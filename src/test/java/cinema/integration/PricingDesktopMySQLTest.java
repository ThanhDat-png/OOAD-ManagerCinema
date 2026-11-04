package cinema.integration;

import cinema.controller.auth.*;
import cinema.controller.pricing.*;
import cinema.dao.*;
import cinema.db.Database;
import cinema.model.*;
import cinema.view.CinemaWindow;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="cinema.mysql.tests",matches="true")
class PricingDesktopMySQLTest {
    private final String key=UUID.randomUUID().toString().replace("-","").substring(0,8),code="P"+key;
    private final int type=100000+ThreadLocalRandom.current().nextInt(1_000_000_000);
    @BeforeEach void setup() throws Exception {
        try(Connection c=Database.connect()) { execute(c,"INSERT INTO LOAIGHE VALUES (?,?)",type,"Ghế thử "+key); execute(c,"INSERT INTO LOAIPHONG VALUES (?,?,'2D')",type,"Phòng thử "+key); }
    }
    @AfterEach void cleanup() throws Exception {
        SwingUtilities.invokeAndWait(() -> { for(Window w:Window.getWindows()) w.dispose(); });
        try(Connection c=Database.connect()) {
            execute(c,"DELETE FROM SANPHAM WHERE MaSP=?",code); execute(c,"DELETE FROM BANGGIAVE WHERE MaLoaiGhe=?",type);
            execute(c,"DELETE FROM LOAIGHE WHERE MaLoaiGhe=?",type); execute(c,"DELETE FROM LOAIPHONG WHERE MaLoaiPhong=?",type);
        }
    }
    private JFrame open() throws Exception {
        var session=new DangNhapBUS(new TaiKhoanDAO()).login("admin_demo","Cinema@123".toCharArray());
        SwingUtilities.invokeAndWait(() -> {
            CinemaWindow.open(session); JFrame home=frame("Quản lý rạp chiếu phim — Admin");
            components(home).stream().filter(c -> c instanceof JList).map(c -> (JList<?>)c).findFirst().orElseThrow().setSelectedIndex(4); click(home,"Mở chức năng");
        });
        await(() -> frame("Quản lý giá vé và F&B")!=null && button(frame("Quản lý giá vé và F&B"),"Thiết lập giá vé").isEnabled());
        return frame("Quản lý giá vé và F&B");
    }

    @Test void createsEditsAndCancelsTicketPriceThroughDesktop() throws Exception {
        JFrame frame=open(); SwingUtilities.invokeLater(() -> click(frame,"Thiết lập giá vé")); await(() -> dialog("Thiết lập giá vé")!=null);
        SwingUtilities.invokeAndWait(() -> {
            JDialog form=dialog("Thiết lập giá vé");
            for(Component c:components(form)) if(c instanceof JComboBox<?> combo) for(int i=0;i<combo.getItemCount();i++) {
                Object item=combo.getItemAt(i);
                if(item instanceof LoaiGhe seat && seat.maLoaiGhe()==type) combo.setSelectedIndex(i);
                if(item instanceof LoaiPhong room && room.maLoaiPhong()==type) combo.setSelectedIndex(i);
                if(item instanceof LoaiThoiDiem period && period.maLoaiThoiDiem()==1) combo.setSelectedIndex(i);
            }
            textFields(form).get(0).setText("123456"); click(form,"Lưu");
        }); dismiss();
        await(() -> ready(frame) && table(frame,5).getRowCount()>0);
        int[] id=new int[1];
        SwingUtilities.invokeAndWait(() -> {
            JTable table=table(frame,5);
            for(int i=0;i<table.getRowCount();i++) if(("Ghế thử "+key).equals(table.getValueAt(i,1))) {
                table.setRowSelectionInterval(i,i); id[0]=(Integer)table.getValueAt(i,0); return;
            }
            fail("Price fixture missing");
        });
        SwingUtilities.invokeLater(() -> click(frame,"Sửa giá vé")); await(() -> dialog("Sửa giá vé")!=null);
        SwingUtilities.invokeAndWait(() -> { textFields(dialog("Sửa giá vé")).get(0).setText("0"); click(dialog("Sửa giá vé"),"Lưu"); });
        dismiss();
        SwingUtilities.invokeAndWait(() -> {
            assertNotNull(dialog("Sửa giá vé")); assertEquals("0",textFields(dialog("Sửa giá vé")).get(0).getText());
            textFields(dialog("Sửa giá vé")).get(0).setText("150000"); click(dialog("Sửa giá vé"),"Lưu");
        }); dismiss(); await(() -> ready(frame));
        assertEquals(new BigDecimal("150000"),new BangGiaVeDAO().find(id[0]).giaVe());
        SwingUtilities.invokeAndWait(() -> selectPrice(frame,id[0]));
        SwingUtilities.invokeLater(() -> click(frame,"Sửa giá vé")); await(() -> dialog("Sửa giá vé")!=null);
        SwingUtilities.invokeAndWait(() -> { textFields(dialog("Sửa giá vé")).get(0).setText("200000"); click(dialog("Sửa giá vé"),"Hủy"); });
        await(() -> ready(frame));
        SwingUtilities.invokeAndWait(() -> screenshot(frame,"target/uc06-prices.png"));
        assertEquals(new BigDecimal("150000"),new BangGiaVeDAO().find(id[0]).giaVe());
    }

    @Test void createsProductEditsStockAndPriceThenFiltersEmptyInventory() throws Exception {
        JFrame frame=open();
        SwingUtilities.invokeAndWait(() -> components(frame).stream().filter(c -> c instanceof JTabbedPane).map(c -> (JTabbedPane)c).findFirst().orElseThrow().setSelectedIndex(1));
        SwingUtilities.invokeLater(() -> click(frame,"Thêm sản phẩm")); await(() -> dialog("Thêm sản phẩm")!=null);
        SwingUtilities.invokeAndWait(() -> {
            JDialog form=dialog("Thêm sản phẩm"); List<JTextField> fields=textFields(form);
            fields.get(0).setText(code); fields.get(1).setText("Bắp thử "+key); fields.get(2).setText("Hộp"); fields.get(3).setText("50000"); fields.get(4).setText("5");
            components(form).stream().filter(c -> c instanceof JTextArea).map(c -> (JTextArea)c).findFirst().orElseThrow().setText("Sản phẩm kiểm thử giao diện");
            click(form,"Lưu");
        }); dismiss(); await(() -> button(frame,"Cập nhật sản phẩm").isEnabled() && table(frame,8).getRowCount()==1);
        SwingUtilities.invokeAndWait(() -> selectProduct(frame));
        SwingUtilities.invokeLater(() -> click(frame,"Cập nhật sản phẩm")); await(() -> dialog("Cập nhật sản phẩm")!=null);
        SwingUtilities.invokeAndWait(() -> {
            List<JTextField> fields=textFields(dialog("Cập nhật sản phẩm"));
            fields.get(0).setText("Bắp đã cập nhật"); fields.get(2).setText("0"); click(dialog("Cập nhật sản phẩm"),"Lưu");
        }); dismiss(); await(() -> button(frame,"Quản lý giá bán").isEnabled());
        assertEquals(0,new SanPhamDAO().find(code).soLuongTon());
        assertEquals(new BigDecimal("50000"),new SanPhamDAO().find(code).giaBan());
        SwingUtilities.invokeAndWait(() -> selectProduct(frame));
        SwingUtilities.invokeLater(() -> click(frame,"Quản lý giá bán")); await(() -> dialog("Quản lý giá bán")!=null);
        SwingUtilities.invokeAndWait(() -> { textFields(dialog("Quản lý giá bán")).get(0).setText("75000"); click(dialog("Quản lý giá bán"),"Lưu"); });
        dismiss(); await(() -> button(frame,"Kiểm tra tồn kho").isEnabled());
        SwingUtilities.invokeAndWait(() -> {
            components(frame).stream().filter(c -> c instanceof JComboBox).map(c -> (JComboBox<?>)c).findFirst().orElseThrow().setSelectedIndex(2);
            click(frame,"Kiểm tra tồn kho");
        });
        await(() -> button(frame,"Kiểm tra tồn kho").isEnabled() && table(frame,8).getRowCount()==1);
        SwingUtilities.invokeAndWait(() -> { assertEquals("Hết hàng",table(frame,8).getValueAt(0,6)); screenshot(frame,"target/uc06-stock.png"); });
        SanPham product=new SanPhamDAO().find(code);
        assertEquals("Bắp đã cập nhật",product.tenSP()); assertEquals(0,product.soLuongTon()); assertEquals(new BigDecimal("75000"),product.giaBan());
    }
    private static boolean ready(JFrame frame) {
        return button(frame,"Thiết lập giá vé").isEnabled() && components(frame).stream()
                .anyMatch(c -> c instanceof JLabel label && label.getText()!=null && label.getText().startsWith("Mức giá vé:"));
    }
    private static void selectPrice(JFrame frame,int id) {
        JTable table=table(frame,5); for(int i=0;i<table.getRowCount();i++) if(Integer.valueOf(id).equals(table.getValueAt(i,0))) { table.setRowSelectionInterval(i,i); return; }
        fail("Missing selected price");
    }
    private void selectProduct(JFrame frame) {
        JTable table=table(frame,8); for(int i=0;i<table.getRowCount();i++) if(code.equals(table.getValueAt(i,0))) { table.setRowSelectionInterval(i,i); return; }
        fail("Missing selected product");
    }
    private static JTable table(Container parent,int columns) { return components(parent).stream().filter(c -> c instanceof JTable t && t.getColumnCount()==columns).map(c -> (JTable)c).findFirst().orElseThrow(); }
    private static void dismiss() throws Exception { await(() -> message()!=null); SwingUtilities.invokeAndWait(() -> message().dispose()); }
    private static JFrame frame(String title) { for(Frame f:JFrame.getFrames()) if(f instanceof JFrame frame && frame.isDisplayable() && title.equals(frame.getTitle())) return frame; return null; }
    private static JDialog dialog(String title) { for(Window w:Window.getWindows()) if(w instanceof JDialog d && d.isShowing() && title.equals(d.getTitle())) return d; return null; }
    private static JDialog message() { for(Window w:Window.getWindows()) if(w instanceof JDialog d && d.isShowing() && components(d).stream().anyMatch(c -> c instanceof JOptionPane)) return d; return null; }
    private static List<Component> components(Container parent) { List<Component> result=new ArrayList<>(); for(Component c:parent.getComponents()) { result.add(c); if(c instanceof Container child) result.addAll(components(child)); } return result; }
    private static List<JTextField> textFields(Container parent) { return components(parent).stream().filter(c -> c instanceof JTextField).map(c -> (JTextField)c).toList(); }
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
