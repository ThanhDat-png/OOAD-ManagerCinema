package cinema.controller.sales;

import cinema.dao.VeDAO;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Printing renders existing tickets; it never writes transaction/ticket data. */
public final class InVe implements Printable {
    private final List<VeDAO.ThongTin> tickets;
    public InVe(List<VeDAO.ThongTin> tickets) { this.tickets=List.copyOf(tickets); }
    public static BufferedImage preview(VeDAO.ThongTin info) {
        BufferedImage measure=new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB); Graphics2D metrics=measure.createGraphics();
        int height; try { height=Math.max(560,326+18*wrapped(metrics,info).size()); } finally { metrics.dispose(); }
        BufferedImage page=new BufferedImage(450,height,BufferedImage.TYPE_INT_RGB); Graphics2D g=page.createGraphics();
        g.setColor(Color.WHITE); g.fillRect(0,0,450,height);
        try { draw(g,info); } finally { g.dispose(); } return page;
    }
    private static java.util.List<String> wrapped(Graphics2D g,VeDAO.ThongTin info) {
        java.util.List<String> result=new java.util.ArrayList<>();
        g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,13));
        var ticket=info.ve();
        String[] lines={"Mã vé: "+ticket.maVe(),"Phim: "+info.tenPhim(),"Phòng: "+info.tenPhong(),
                "Suất: "+ticket.maSuat()+" · "+info.batDau().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                "Ghế: "+ticket.hangGheLucPhatHanh()+ticket.soGheLucPhatHanh()+" · "+ticket.tenLoaiGheLucPhatHanh(),
                "Giá: "+ticket.giaVe().toPlainString()+" VND", "Trạng thái: "+ticket.trangThai()};

        for(String line:lines) {
            String remaining=line;
            while(g.getFontMetrics().stringWidth(remaining)>410) {
                int end=remaining.length()-1;
                while(end>0 && g.getFontMetrics().stringWidth(remaining.substring(0,end))>410) end--;
                result.add(remaining.substring(0,end)); remaining=remaining.substring(end);
            }
            result.add(remaining);
        }
        return result;
    }
    private static void draw(Graphics2D g,VeDAO.ThongTin info) {
        g.setColor(Color.BLACK); g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,18)); g.drawString("VÉ XEM PHIM",20,30);
        var lines=wrapped(g,info); int y=58;
        for(String line:lines) { g.drawString(line,20,y); y+=18; }
        try {
            BufferedImage qr=MatrixToImageWriter.toBufferedImage(new QRCodeWriter().encode(info.ve().maQR(),BarcodeFormat.QR_CODE,240,240));
            g.drawImage(qr,105,Math.max(y+8,260),null);
        } catch(WriterException e) { throw new IllegalArgumentException("Không tạo được ảnh QR của vé đã lưu",e); }
    }
    @Override public int print(Graphics graphics,PageFormat format,int page) throws PrinterException {
        if(page<0 || page>=tickets.size()) return NO_SUCH_PAGE;
        Graphics2D g=(Graphics2D)graphics.create();
        try {
            BufferedImage image=preview(tickets.get(page));
            g.translate(format.getImageableX(),format.getImageableY()); double scale=Math.min(format.getImageableWidth()/450,format.getImageableHeight()/image.getHeight());
            g.scale(scale,scale); g.drawImage(image,0,0,null); return PAGE_EXISTS;
        } catch(RuntimeException e) { throw new PrinterException("Không kết xuất được vé: "+e.getMessage()); }
        finally { g.dispose(); }
    }
}
