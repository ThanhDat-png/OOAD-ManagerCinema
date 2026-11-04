package cinema.integration;

import cinema.controller.auth.*;
import cinema.controller.schedule.LichChieuBUS;
import cinema.dao.*;
import cinema.db.Database;
import cinema.model.*;
import cinema.view.CinemaWindow;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="cinema.mysql.tests",matches="true")
class AdmissionDesktopMySQLTest {
    @TempDir Path temporary;
    private final String key=UUID.randomUUID().toString().replace("-","").substring(0,8),film="F"+key,room="R"+key,tx="G"+key;
    private final String ticket="V"+UUID.randomUUID().toString().replace("-","").substring(0,19),qr="Qr"+key;
    private final int type=100000+ThreadLocalRandom.current().nextInt(1_000_000_000);
    private int show;

    @BeforeEach void setup() throws Exception {
        LocalDate date=LocalDate.now(ZoneId.of("Asia/Bangkok")).plusDays(30);
        try(Connection c=Database.connect()) {
            execute(c,"INSERT INTO LOAIGHE VALUES (?,?)",type,"Seat "+key); execute(c,"INSERT INTO LOAIPHONG VALUES (?,?,'2D')",type,"Type "+key);
            execute(c,"INSERT INTO PHIM VALUES (?,?,90,'Test',?,0,'test','Đang chiếu','2D')",film,"Phim kiểm vé "+key,java.sql.Date.valueOf(date));
            execute(c,"INSERT INTO PHONGCHIEU VALUES (?,?,?,15,'Đang hoạt động')",room,type,"Gate "+key);
            execute(c,"INSERT INTO GHE (MaPhong,MaLoaiGhe,HangGhe,SoGhe,TrangThai) VALUES (?,?,'A',1,'Sẵn sàng sử dụng')",room,type);
            execute(c,"INSERT INTO BANGGIAVE (MaLoaiGhe,MaLoaiPhong,MaLoaiThoiDiem,GiaVe) VALUES (?,?,1,100000)",type,type);
        }
        show=new LichChieuBUS(new PhienDangNhap(1,"admin",VaiTro.ADMIN,null),new SuatChieuDAO(),Set.of()).create(film,room,date.atTime(10,0)).maSuat();
        try(Connection c=Database.connect()) {
            execute(c,"UPDATE SUATCHIEU SET ThoiGianBatDau=NOW(),ThoiGianTraPhong=TIMESTAMPADD(MINUTE,105,NOW()) WHERE MaSuat=?",show);
            execute(c,"INSERT INTO GIAODICH VALUES (?,'NV002',NOW(),100000,'Đã thanh toán')",tx);
            execute(c,"UPDATE GHE_SUATCHIEU SET MaGiaoDich=?,TrangThai='Đã bán',GiaVeChot=100000,MaGia=(SELECT MaGia FROM BANGGIAVE WHERE MaLoaiGhe=? LIMIT 1) WHERE MaSuat=?",tx,type,show);
            execute(c,"INSERT INTO THANHTOAN (MaGiaoDich,PhuongThuc,SoTien,ThoiGianThanhToan) VALUES (?,'Tiền mặt',100000,NOW())",tx);
            execute(c,"INSERT INTO VE (MaVe,MaGiaoDich,MaSuat,MaGhe,MaGia,MaQR,GiaVe,TrangThai,ThoiGianPhatHanh) SELECT ?,MaGiaoDich,MaSuat,MaGhe,MaGia,?,GiaVeChot,'Chưa sử dụng',NOW() FROM GHE_SUATCHIEU WHERE MaSuat=?",ticket,qr,show);
        }
    }
    @AfterEach void cleanup() throws Exception {
        SwingUtilities.invokeAndWait(() -> { for(Window window:Window.getWindows()) window.dispose(); });
        try(Connection c=Database.connect()) {
            execute(c,"DELETE FROM VE WHERE MaVe=?",ticket); execute(c,"DELETE FROM GHE_SUATCHIEU WHERE MaSuat=?",show); execute(c,"DELETE FROM SUATCHIEU WHERE MaSuat=?",show);
            execute(c,"DELETE FROM THANHTOAN WHERE MaGiaoDich=?",tx); execute(c,"DELETE FROM GIAODICH WHERE MaGiaoDich=?",tx);
            execute(c,"DELETE FROM GHE WHERE MaPhong=?",room); execute(c,"DELETE FROM PHONGCHIEU WHERE MaPhong=?",room);
            execute(c,"DELETE FROM BANGGIAVE WHERE MaLoaiGhe=?",type); execute(c,"DELETE FROM LOAIGHE WHERE MaLoaiGhe=?",type);
            execute(c,"DELETE FROM LOAIPHONG WHERE MaLoaiPhong=?",type); execute(c,"DELETE FROM PHIM WHERE MaPhim=?",film);
        }
    }
    private JFrame open() throws Exception {
        var session=new DangNhapBUS(new TaiKhoanDAO()).login("staff_demo","Cinema@123".toCharArray());
        SwingUtilities.invokeAndWait(() -> {
            CinemaWindow.open(session); JFrame home=frame("Quản lý rạp chiếu phim — Nhân viên");
            components(home).stream().filter(c -> c instanceof JList).map(c -> (JList<?>)c).findFirst().orElseThrow().setSelectedIndex(3); click(home,"Mở chức năng");
        });
        await(() -> frame("Kiểm tra và xác nhận vé")!=null && ready(frame("Kiểm tra và xác nhận vé")));
        JFrame frame=frame("Kiểm tra và xác nhận vé");
        SwingUtilities.invokeAndWait(() -> {
            JComboBox<?> rooms=roomBox(frame);
            for(int i=0;i<rooms.getItemCount();i++) if(rooms.getItemAt(i) instanceof PhongChieu r && room.equals(r.maPhong())) {
                if(rooms.getSelectedIndex()!=i) rooms.setSelectedIndex(i); return;
            }
            fail("Missing gate room");
        });
        await(() -> ready(frame) && components(frame).stream().anyMatch(c -> c instanceof JComboBox<?> combo && combo.getSelectedItem() instanceof SuatChieu s && s.maSuat()==show));
        return frame;
    }
    private void check(JFrame frame) throws Exception {
        SwingUtilities.invokeAndWait(() -> click(frame,"Kiểm tra tính hợp lệ"));
        await(() -> ready(frame) && button(frame,"Xác nhận").isEnabled());
    }

    @Test void employeeLooksUpCancelsAndConfirmsWithFreshPaymentCheck() throws Exception {
        JFrame frame=open();
        SwingUtilities.invokeAndWait(() -> { codeField(frame).setText(ticket); codeField(frame).postActionEvent(); });
        await(() -> ready(frame) && detail(frame).getText().contains(ticket));
        SwingUtilities.invokeAndWait(() -> assertFalse(button(frame,"Xác nhận").isEnabled()));
        check(frame);
        SwingUtilities.invokeLater(() -> click(frame,"Xác nhận")); await(() -> message()!=null);
        SwingUtilities.invokeAndWait(() -> pane(message()).setValue(JOptionPane.NO_OPTION));
        await(() -> ready(frame) && hasMessage(frame,"Đã hủy xác nhận. Trạng thái vé được giữ nguyên")); assertEquals("Chưa sử dụng",state());
        SwingUtilities.invokeAndWait(() -> { codeField(frame).setText("missing"); assertFalse(button(frame,"Xác nhận").isEnabled()); codeField(frame).setText(ticket); });
        check(frame);
        try(Connection c=Database.connect()) { execute(c,"UPDATE GIAODICH SET TrangThai='Chờ thanh toán' WHERE MaGiaoDich=?",tx); }
        SwingUtilities.invokeLater(() -> click(frame,"Xác nhận")); await(() -> message()!=null);
        SwingUtilities.invokeAndWait(() -> pane(message()).setValue(JOptionPane.YES_OPTION));
        await(() -> ready(frame) && hasMessage(frame,"Vé chưa có thanh toán hợp lệ")); assertEquals("Chưa sử dụng",state());
        try(Connection c=Database.connect()) { execute(c,"UPDATE GIAODICH SET TrangThai='Đã thanh toán' WHERE MaGiaoDich=?",tx); }
        check(frame); SwingUtilities.invokeLater(() -> click(frame,"Xác nhận")); await(() -> message()!=null);
        SwingUtilities.invokeAndWait(() -> pane(message()).setValue(JOptionPane.YES_OPTION));
        await(() -> ready(frame) && hasMessage(frame,"Xác nhận vé thành công"));
        assertEquals("Đã sử dụng",state());
        SwingUtilities.invokeAndWait(() -> { assertFalse(button(frame,"Xác nhận").isEnabled()); screenshot(frame,"target/uc07-confirmed.png"); click(frame,"Kiểm tra tính hợp lệ"); });
        await(() -> ready(frame) && hasMessage(frame,"Vé đã được sử dụng"));
        SwingUtilities.invokeAndWait(() -> assertFalse(button(frame,"Xác nhận").isEnabled()));
    }

    @Test void qrImageLookupAndContextChangesNeverReuseEarlierCheck() throws Exception {
        Path image=temporary.resolve("ticket-qr.png"); MatrixToImageWriter.writeToPath(new QRCodeWriter().encode(qr,BarcodeFormat.QR_CODE,320,320),"PNG",image);
        JFrame frame=open(); SwingUtilities.invokeLater(() -> click(frame,"Đọc QR từ ảnh"));
        await(() -> chooser()!=null);
        SwingUtilities.invokeAndWait(() -> { JFileChooser chooser=chooser(); chooser.setSelectedFile(image.toFile()); chooser.approveSelection(); });
        await(() -> ready(frame) && detail(frame).getText().contains(ticket));
        SwingUtilities.invokeAndWait(() -> { assertEquals(qr,codeField(frame).getText()); assertFalse(button(frame,"Xác nhận").isEnabled()); });
        check(frame);
        SwingUtilities.invokeAndWait(() -> {
            screenshot(frame,"target/uc07-qr.png"); JComboBox<?> rooms=roomBox(frame);
            for(int i=0;i<rooms.getItemCount();i++) if(rooms.getItemAt(i) instanceof PhongChieu r && !room.equals(r.maPhong())) { rooms.setSelectedIndex(i); break; }
            assertFalse(button(frame,"Xác nhận").isEnabled());
        });
        await(() -> ready(frame)); assertEquals("Chưa sử dụng",state());
    }

    private String state() throws SQLException { return new VeDAO().lookup(ticket,VeDAO.LoaiMa.MA_VE).orElseThrow().ve().trangThai(); }
    private static boolean ready(JFrame frame) { return button(frame,"Tra cứu vé").isEnabled() && !hasMessage(frame,"Đang xử lý..."); }
    private static boolean hasMessage(Container parent,String value) { return components(parent).stream().anyMatch(c -> c instanceof JLabel label && value.equals(label.getText())); }
    private static JTextField codeField(Container parent) { return components(parent).stream().filter(c -> c instanceof JTextField field && field.getColumns()==30).map(c -> (JTextField)c).findFirst().orElseThrow(); }
    private static JTextArea detail(Container parent) { return components(parent).stream().filter(c -> c instanceof JTextArea).map(c -> (JTextArea)c).findFirst().orElseThrow(); }
    private static JComboBox<?> roomBox(Container parent) { return components(parent).stream().filter(c -> c instanceof JComboBox<?> combo && combo.getItemCount()>0 && combo.getItemAt(0) instanceof PhongChieu).map(c -> (JComboBox<?>)c).findFirst().orElseThrow(); }
    private static JFileChooser chooser() { for(Window w:Window.getWindows()) if(w.isShowing()) for(Component c:components(w)) if(c instanceof JFileChooser chooser) return chooser; return null; }
    private static JFrame frame(String title) { for(Frame f:JFrame.getFrames()) if(f instanceof JFrame frame && frame.isDisplayable() && title.equals(frame.getTitle())) return frame; return null; }
    private static JDialog message() { for(Window w:Window.getWindows()) if(w instanceof JDialog d && d.isShowing() && components(d).stream().anyMatch(c -> c instanceof JOptionPane)) return d; return null; }
    private static JOptionPane pane(Container parent) { return components(parent).stream().filter(c -> c instanceof JOptionPane).map(c -> (JOptionPane)c).findFirst().orElseThrow(); }
    private static List<Component> components(Container parent) { List<Component> result=new ArrayList<>(); for(Component c:parent.getComponents()) { result.add(c); if(c instanceof Container child) result.addAll(components(child)); } return result; }
    private static JButton button(Container parent,String title) { return components(parent).stream().filter(c -> c instanceof JButton b && title.equals(b.getText())).map(c -> (JButton)c).findFirst().orElseThrow(); }
    private static void click(Container parent,String title) { button(parent,title).doClick(); }
    private static void screenshot(Window window,String name) {
        BufferedImage image=new BufferedImage(window.getWidth(),window.getHeight(),BufferedImage.TYPE_INT_RGB); Graphics2D graphics=image.createGraphics(); window.paint(graphics); graphics.dispose();
        try { ImageIO.write(image,"png",Path.of(name).toFile()); } catch(java.io.IOException e) { throw new RuntimeException(e); }
    }
    private static void await(java.util.function.BooleanSupplier condition) {
        assertTimeoutPreemptively(Duration.ofSeconds(15),() -> { AtomicBoolean done=new AtomicBoolean(); while(!done.get()) { SwingUtilities.invokeAndWait(() -> done.set(condition.getAsBoolean())); Thread.sleep(20); } });
    }
    private static void execute(Connection c,String sql,Object... args) throws SQLException { try(PreparedStatement ps=c.prepareStatement(sql)) { for(int i=0;i<args.length;i++) ps.setObject(i+1,args[i]); ps.executeUpdate(); } }
}
