package cinema.web.admin;

import cinema.controller.revenue.KhoangThongKe;
import cinema.web.db.WebDb;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;
import static cinema.web.admin.AdminUi.*;

@Controller
class Reports {
    private final Catalog catalog;
    private final WebDb db;
    Reports(Catalog catalog,WebDb db){this.catalog=catalog;this.db=db;}
    static final String PAID="""
            WITH seats AS (
                SELECT MaGiaoDich,COUNT(*) sold,SUM(GiaVeChot) amount,
                 SUM(CASE WHEN TrangThai='Đã bán' AND MaGia IS NOT NULL AND GiaVeChot>0 THEN 0 ELSE 1 END) invalid
                 FROM GHE_SUATCHIEU WHERE MaGiaoDich IS NOT NULL GROUP BY MaGiaoDich),
            paid AS (
                SELECT g.MaGiaoDich,g.MaNV,g.TongTien,h.sold,
                       (SELECT count(*) FROM VE v WHERE v.MaGiaoDich=g.MaGiaoDich) issued
                FROM GIAODICH g JOIN THANHTOAN t ON t.MaGiaoDich=g.MaGiaoDich JOIN seats h ON h.MaGiaoDich=g.MaGiaoDich
                WHERE g.TrangThai='Đã thanh toán' AND g.TongTien>0 AND g.TongTien=t.SoTien AND h.amount=g.TongTien AND h.invalid=0
                AND date(t.ThoiGianThanhToan) BETWEEN ? AND ?
                AND NOT EXISTS(SELECT 1 FROM VE v JOIN GHE_SUATCHIEU x ON x.MaSuat=v.MaSuat AND x.MaGhe=v.MaGhe
                   WHERE v.MaGiaoDich=g.MaGiaoDich AND (v.GiaVe<>x.GiaVeChot OR v.MaGia<>x.MaGia)))
            """;
    static final String TOTALS="count(p.MaGiaoDich) Transactions,coalesce(sum(p.sold),0) Sold,coalesce(sum(p.issued),0) Issued,coalesce(sum(p.sold-p.issued),0) Pending,coalesce(sum(p.TongTien),0) Revenue";
    List<Option> tabs(){return List.of(new Option("time","Theo thời gian"),new Option("film","Theo phim"),new Option("employee","Theo nhân viên"),new Option("ticket","Vé"),new Option("stock","Tồn kho F&B"));}
    @GetMapping("/admin/reports")
    String report(@RequestParam Map<String,String> p,Model m){
        String tab=p.getOrDefault("tab","time"),mode=p.getOrDefault("mode","day");
        if(tabs().stream().noneMatch(t->t.value().equals(tab))){
            m.addAttribute("error","Vui lòng chọn loại thống kê.");
            return report(Map.of(),m);
        }
        var fields=new ArrayList<Field>();
        if(tab.equals("time")){
            fields.add(field("day","Ngày thanh toán","date"));fields.add(field("month","Tháng thanh toán","month"));fields.add(field("year","Năm thanh toán","number"));
        }
        if(!tab.equals("stock")){
            String dateLabel=tab.equals("ticket")?" phát hành":" thanh toán";
            fields.add(field("from","Từ ngày"+dateLabel,"date"));fields.add(field("to","Đến ngày"+dateLabel,"date"));
        }
        switch(tab){
            case "film","ticket"->fields.add(optional(choice("film","Phim",catalog.films())));
            case "employee"->fields.add(optional(choice("employee","Nhân viên",options(db,"SELECT MaNV id,HoTen label FROM NHANVIEN ORDER BY HoTen"))));
            case "stock"->{
                fields.add(optional(choice("product","Sản phẩm",options(db,"SELECT MaSP id,TenSP label FROM SANPHAM ORDER BY TenSP"))));
                fields.add(optional(choice("status","Kinh doanh",options(productStates))));fields.add(optional(choice("stock","Tồn kho",options("Còn hàng","Hết hàng"))));
            }
            default->{}
        }
        if(tab.equals("ticket"))fields.add(optional(choice("show","Suất chiếu",options(db,"SELECT s.MaSuat id, s.ThoiGianBatDau||' · '||p.TenPhim||' · '||r.TenPhong label FROM SUATCHIEU s JOIN PHIM p ON p.MaPhim=s.MaPhim JOIN PHONGCHIEU r ON r.MaPhong=s.MaPhong ORDER BY s.ThoiGianBatDau DESC"))));
        m.addAttribute("title","Báo cáo và thống kê");m.addAttribute("tabs",tabs());m.addAttribute("tab",tab);m.addAttribute("mode",mode);m.addAttribute("fields",fields);m.addAttribute("params",p);
        if(!p.containsKey("run"))return "admin/reports";
        try{
            String from="",to="";
            if(!tab.equals("stock")){
                KhoangThongKe range=switch(tab.equals("time")?mode:"range"){
                    case "day"->KhoangThongKe.ngay(date(p,"day"));case "month"->KhoangThongKe.thang(YearMonth.parse(val(p,"month")));
                    case "year"->KhoangThongKe.nam(number(p,"year",1000));case "range"->new KhoangThongKe(date(p,"from"),date(p,"to"));
                    default->throw new IllegalArgumentException("Kỳ thống kê không hợp lệ.");};
                from=range.tuNgay().toString();to=range.denNgay().toString();m.addAttribute("range",from.equals(to)?from:from+" – "+to);
            }
            List<Column> cols;List<Map<String,Object>> result;
            if(tab.equals("stock")){
                String product=val(p,"product"),status=val(p,"status"),stock=val(p,"stock");
                result=db.query("SELECT * FROM SANPHAM WHERE (?='' OR MaSP=?) AND (?='' OR TinhTrangKinhDoanh=?) AND (?='' OR (CASE WHEN SoLuongTon=0 THEN 'Hết hàng' ELSE 'Còn hàng' END)=?) ORDER BY TenSP",product,product,status,status,stock,stock);
                cols=columns("MaSP","Mã","TenSP","Sản phẩm","DonViTinh","Đơn vị","TinhTrangKinhDoanh","Kinh doanh","SoLuongTon","Tồn kho");
            }else if(tab.equals("ticket")){
                String film=val(p,"film"),show=val(p,"show");
                result=db.query("SELECT count(*) Total,count(CASE WHEN v.TrangThai='Đã sử dụng' THEN 1 END) Used,count(CASE WHEN v.TrangThai='Chưa sử dụng' THEN 1 END) Unused FROM VE v JOIN SUATCHIEU s ON s.MaSuat=v.MaSuat WHERE date(v.ThoiGianPhatHanh) BETWEEN ? AND ? AND (?='' OR s.MaPhim=?) AND (?='' OR s.MaSuat=?)",from,to,film,film,show,show);
                cols=columns("Total","Tổng số vé","Used","Đã sử dụng","Unused","Chưa sử dụng");
            }else if(tab.equals("film")){
                String filter=val(p,"film");
                result=db.query(PAID+", total AS (SELECT s.MaPhim,count(*) Tickets,sum(v.GiaVe) Revenue FROM paid p JOIN VE v ON v.MaGiaoDich=p.MaGiaoDich JOIN SUATCHIEU s ON s.MaSuat=v.MaSuat GROUP BY s.MaPhim) SELECT f.MaPhim,f.TenPhim,coalesce(t.Tickets,0) Tickets,coalesce(t.Revenue,0) Revenue FROM PHIM f LEFT JOIN total t ON t.MaPhim=f.MaPhim WHERE (?='' OR f.MaPhim=?) ORDER BY f.TenPhim",from,to,filter,filter);
                cols=columns("MaPhim","Mã phim","TenPhim","Phim","Tickets","Vé đã phát hành","Revenue","Doanh thu (₫)");
            }else{
                var totals=columns("Transactions","Giao dịch","Sold","Vé bán","Issued","Đã phát hành","Pending","Chờ phát hành","Revenue","Doanh thu (₫)");
                if(tab.equals("employee")){
                    String filter=val(p,"employee");
                    result=db.query(PAID+" SELECT n.MaNV,n.HoTen,"+TOTALS+" FROM NHANVIEN n LEFT JOIN paid p ON n.MaNV=p.MaNV WHERE (?='' OR n.MaNV=?) GROUP BY n.MaNV,n.HoTen ORDER BY n.HoTen",from,to,filter,filter);
                    var merged=new ArrayList<>(columns("MaNV","Mã nhân viên","HoTen","Nhân viên"));merged.addAll(totals);cols=merged;
                }else{cols=totals;result=db.query(PAID+" SELECT "+TOTALS+" FROM paid p",from,to);}
            }
            m.addAttribute("columns",cols);m.addAttribute("rows",result);
        }catch(DateTimeException ex){m.addAttribute("error","Thời gian thống kê không hợp lệ.");}
        catch(IllegalArgumentException ex){m.addAttribute("error",ex.getMessage());}
        return "admin/reports";
    }
}
