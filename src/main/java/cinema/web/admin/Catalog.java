package cinema.web.admin;

import cinema.web.db.WebDb;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.*;
import static cinema.web.admin.AdminUi.*;

@Controller
public class Catalog {
    final WebDb db;
    Catalog(WebDb db){this.db=db;}
    static final String FILM_SQL="""
        SELECT p.*, (SELECT group_concat(t.TenTheLoai, ', ') FROM PHIM_THELOAI x JOIN THELOAI t ON t.MaTheLoai=x.MaTheLoai WHERE x.MaPhim=p.MaPhim) AS TheLoai FROM PHIM p
        """;
    static final String ROOM_SQL="""
        SELECT p.*, l.TenLoaiPhong,l.DinhDangChieu, (SELECT count(*) FROM GHE g WHERE g.MaPhong=p.MaPhong) AS SoGhe FROM PHONGCHIEU p JOIN LOAIPHONG l ON l.MaLoaiPhong=p.MaLoaiPhong
        """;
    static final String SHOW_SQL="""
        SELECT s.*,p.TenPhim,p.Poster,p.DinhDangChieu,p.ThoiLuong,r.TenPhong,k.TenLoaiThoiDiem FROM SUATCHIEU s JOIN PHIM p ON p.MaPhim=s.MaPhim JOIN PHONGCHIEU r ON r.MaPhong=s.MaPhong JOIN LOAITHOIDIEM k ON k.MaLoaiThoiDiem=s.MaLoaiThoiDiem
        """;
    static final String PRICE_SQL="""
        SELECT b.*,g.TenLoaiGhe,r.TenLoaiPhong,t.TenLoaiThoiDiem FROM BANGGIAVE b JOIN LOAIGHE g ON g.MaLoaiGhe=b.MaLoaiGhe JOIN LOAIPHONG r ON r.MaLoaiPhong=b.MaLoaiPhong JOIN LOAITHOIDIEM t ON t.MaLoaiThoiDiem=b.MaLoaiThoiDiem
        """;
    List<Option> genres(){return options(db,"SELECT MaTheLoai id,TenTheLoai label FROM THELOAI ORDER BY TenTheLoai");}
    List<Option> roomTypes(){return options(db,"SELECT MaLoaiPhong id,TenLoaiPhong label FROM LOAIPHONG ORDER BY id");}
    List<Option> seats(){return options(db,"SELECT MaLoaiGhe id,TenLoaiGhe label FROM LOAIGHE ORDER BY id");}
    List<Option> periods(){return options(db,"SELECT MaLoaiThoiDiem id,TenLoaiThoiDiem label FROM LOAITHOIDIEM ORDER BY id");}
    List<Option> films(){return options(db,"SELECT MaPhim id,TenPhim label FROM PHIM ORDER BY TenPhim");}
    List<Option> rooms(){return options(db,"SELECT MaPhong id,TenPhong label FROM PHONGCHIEU ORDER BY TenPhong");}

    static String url(String base,String key,Object value){
        return UriComponentsBuilder.fromPath(base).queryParam(key,"{value}").encode().buildAndExpand(value).toUriString();
    }
    static Field search(String label){return optional(field("q",label,"search"));}
    static boolean contains(Object value,String term){
        return value!=null&&value.toString().toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT));
    }
    static boolean matches(Map<String,Object> row,String col,String v){return v.isBlank()||Objects.toString(row.get(col),"").equals(v);}
    static boolean admin(HttpSession session){return "ADMIN".equalsIgnoreCase(Objects.toString(session.getAttribute("role"),""));}
    static String list(Model m,Page page,List<Map<String,Object>> rows,String key,String kind,Map<String,String> params){
        m.addAttribute("page",page);m.addAttribute("title",page.title());m.addAttribute("rows",rows);m.addAttribute("key",key);
        m.addAttribute("kind",kind);m.addAttribute("params",params);
        m.addAttribute("emptyMessage",switch(page.url()){
            case EMP->"Không tìm thấy nhân viên phù hợp";
            case FILMS->"Không tìm thấy phim phù hợp";
            case ROOMS->"Không tìm thấy phòng chiếu phù hợp";
            case PRODUCTS->"Không tìm thấy sản phẩm phù hợp";
            default->"Không tìm thấy dữ liệu phù hợp";
        });
        return "admin/list";
    }
    @GetMapping(EMP)
    String employees(@RequestParam Map<String,String> p,Model m){
        String q=val(p,"q");
        var rows=db.query("SELECT * FROM NHANVIEN ORDER BY HoTen").stream().filter(r->List.of("MaNV","HoTen","SoDienThoai").stream().anyMatch(k->contains(r.get(k),q))).toList();
        return list(m,new Page("Quản lý nhân viên",EMP,columns("MaNV","Mã nhân viên","HoTen","Họ tên","ChucVu","Chức vụ","SoDienThoai","Điện thoại"),List.of(search("Mã, tên hoặc số điện thoại")),"employee-new","Thêm nhân viên"),rows,"MaNV","table",p);
    }
    @GetMapping(FILMS)
    String filmList(@RequestParam Map<String,String> p,HttpSession session,Model m){
        String q=val(p,"q"),g=val(p,"genre"),s=val(p,"status");
        var rows=db.query(FILM_SQL+" WHERE (?='' OR EXISTS(SELECT 1 FROM PHIM_THELOAI x WHERE x.MaPhim=p.MaPhim AND x.MaTheLoai=?)) ORDER BY p.TenPhim",g,g)
                .stream().filter(r->contains(r.get("TenPhim"),q)&&matches(r,"TrangThai",s)).toList();
        boolean manage=admin(session);
        return list(m,new Page(manage?"Quản lý phim":"Tra cứu phim",FILMS,List.of(),List.of(search("Tên phim"),optional(choice("genre","Thể loại",genres())),optional(choice("status","Trạng thái",options(filmStates)))),manage?"film-new":null,"Thêm phim"),rows,"MaPhim","films",p);
    }
    @GetMapping(ROOMS)
    String roomList(@RequestParam Map<String,String> p,HttpSession session,Model m){
        String q=val(p,"q");
        var rows=db.query(ROOM_SQL+" ORDER BY p.TenPhong").stream().filter(r->(contains(r.get("MaPhong"),q)||contains(r.get("TenPhong"),q))&&matches(r,"TrangThai",val(p,"status"))&&matches(r,"MaLoaiPhong",val(p,"type"))).toList();
        boolean manage=admin(session);
        return list(m,new Page(manage?"Quản lý phòng chiếu":"Tra cứu phòng chiếu",ROOMS,columns("TenPhong","Phòng chiếu","TenLoaiPhong","Loại phòng","SoGhe","Số ghế","ThoiGianDonPhong","Dọn phòng (phút)","TrangThai","Trạng thái"),List.of(search("Mã hoặc tên phòng"),optional(choice("type","Loại phòng",roomTypes())),optional(choice("status","Trạng thái",options(roomStates)))),manage?"room-new":null,"Thêm phòng"),rows,"MaPhong","table",p);
    }
    @GetMapping(SHOWS)
    String showList(@RequestParam Map<String,String> p,Model m){
        var today=java.time.LocalDate.now(ZONE);
        var filters=new HashMap<>(p);
        filters.putIfAbsent("date",today.toString());
        List<Option> days=new ArrayList<>();
        for(int i=0;i<7;i++){
            var d=today.plusDays(i);
            String label=i==0?"Hôm nay":d.getDayOfWeek().getValue()==7?"Chủ nhật":"Thứ "+(d.getDayOfWeek().getValue()+1);
            days.add(new Option(d.toString(),label));
        }
        m.addAttribute("dates",days);
        var rows=db.query(SHOW_SQL+" WHERE (?='' OR date(s.ThoiGianBatDau)=?) AND (?='' OR s.MaPhong=?) ORDER BY s.ThoiGianBatDau",val(filters,"date"),val(filters,"date"),val(filters,"room"),val(filters,"room"));
        return list(m,new Page("Quản lý lịch chiếu",SHOWS,columns("TenPhim","Phim","TenPhong","Phòng","ThoiGianBatDau","Bắt đầu","ThoiGianTraPhong","Trả phòng","TrangThai","Trạng thái"),List.of(optional(field("date","Ngày chiếu","date")),optional(choice("room","Phòng chiếu",rooms()))),"show-new","Tạo suất chiếu"),rows,"MaSuat","table",filters);
    }
    @GetMapping(PRICES)
    String priceList(@RequestParam Map<String,String> p,Model m){
        return list(m,new Page("Quản lý giá vé",PRICES,columns("TenLoaiGhe","Loại ghế","TenLoaiPhong","Loại phòng","TenLoaiThoiDiem","Thời điểm","GiaVe","Giá vé (₫)"),List.of(),"price-new","Thiết lập giá"),db.query(PRICE_SQL+" ORDER BY g.TenLoaiGhe,r.TenLoaiPhong,t.TenLoaiThoiDiem"),"MaGia","table",p);
    }
    @GetMapping(PRODUCTS)
    String productList(@RequestParam Map<String,String> p,Model m){
        String q=val(p,"q"),stock=val(p,"stock");
        var rows=db.query("SELECT * FROM SANPHAM ORDER BY TenSP").stream().filter(r->(contains(r.get("MaSP"),q)||contains(r.get("TenSP"),q))&&(stock.isEmpty()||stock.equals(((Number)r.get("SoLuongTon")).intValue()==0?"Hết hàng":"Còn hàng"))).toList();
        return list(m,new Page("Sản phẩm F&B và tồn kho",PRODUCTS,columns("MaSP","Mã sản phẩm","TenSP","Sản phẩm","DonViTinh","Đơn vị","GiaBan","Giá bán (₫)","SoLuongTon","Tồn kho","TinhTrangKinhDoanh","Kinh doanh"),List.of(search("Mã hoặc tên sản phẩm"),optional(choice("stock","Tồn kho",options("Còn hàng","Hết hàng")))),"product-new","Thêm sản phẩm"),rows,"MaSP","table",p);
    }
    static Action edit(String label,String op,Object id){return new Action(label,url("/admin/form/"+op,"id",id));}
    static String detail(Model m,String title,String back,Map<String,Object> item,List<Column> cols,List<Action> actions){
        m.addAttribute("title",title);m.addAttribute("back",back);m.addAttribute("item",item);m.addAttribute("columns",cols);m.addAttribute("actions",actions);return "admin/detail";
    }
    @GetMapping({"/profile",EMP+"/detail"})
    String employee(@RequestParam(defaultValue="") String id,HttpSession session,jakarta.servlet.http.HttpServletRequest req,Model m,RedirectAttributes flash){
        boolean own=req.getServletPath().equals("/profile")||req.getRequestURI().endsWith("/profile");
        Object who=own?session.getAttribute("employeeId"):id;
        if(own&&!id.isBlank()&&!id.equals(who))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Bạn không có quyền xem thông tin này");
        var rows=db.query("SELECT * FROM NHANVIEN WHERE MaNV=?",who);
        if(rows.isEmpty()){
            String message="Không tìm thấy thông tin nhân viên";
            if(own)throw new ResponseStatusException(HttpStatus.NOT_FOUND,message);
            flash.addFlashAttribute("adminError",message);
            return "redirect:"+EMP;
        }
        var r=rows.get(0);
        return detail(m,own?"Hồ sơ cá nhân":r.get("HoTen").toString(),own?null:EMP,r,columns("MaNV","Mã nhân viên","HoTen","Họ tên","NgaySinh","Ngày sinh","ChucVu","Chức vụ","SoDienThoai","Điện thoại","Email","Email","DiaChi","Địa chỉ"),own?List.of():List.of(edit("Sửa hồ sơ","employee-edit",id),edit("Xóa nhân viên","employee-delete",id)));
    }
    @GetMapping(FILMS+"/detail")
    String film(@RequestParam String id,HttpSession session,Model m){
        var r=one(db.query(FILM_SQL+" WHERE p.MaPhim=?",id)); m.addAttribute("film",true);
        return detail(m,r.get("TenPhim").toString(),FILMS,r,columns("MaPhim","Mã phim","DaoDien","Đạo diễn","TheLoai","Thể loại","ThoiLuong","Thời lượng (phút)","NgayKhoiChieu","Khởi chiếu","GioiHanTuoi","Giới hạn tuổi","DinhDangChieu","Định dạng","TrangThai","Trạng thái"),admin(session)?List.of(edit("Sửa tên phim","film-name",id),edit("Thay poster","film-poster",id),edit("Sửa thể loại","film-genres",id),edit("Ngừng chiếu","film-stop",id)):List.of());
    }
    @GetMapping(ROOMS+"/detail")
    String room(@RequestParam String id,HttpSession session,Model m){
        var r=one(db.query(ROOM_SQL+" WHERE p.MaPhong=?",id));m.addAttribute("roomId",id);m.addAttribute("manageSeats",admin(session));
        m.addAttribute("seatRows",db.query("SELECT g.*,l.TenLoaiGhe FROM GHE g JOIN LOAIGHE l ON l.MaLoaiGhe=g.MaLoaiGhe WHERE MaPhong=? ORDER BY HangGhe,SoGhe",id).stream().collect(java.util.stream.Collectors.groupingBy(x->x.get("HangGhe").toString(),LinkedHashMap::new,java.util.stream.Collectors.toList())));
        return detail(m,r.get("TenPhong").toString(),ROOMS,r,columns("MaPhong","Mã phòng","TenLoaiPhong","Loại phòng","SoGhe","Số ghế","ThoiGianDonPhong","Dọn phòng (phút)","TrangThai","Trạng thái"),admin(session)?List.of(edit("Sửa thông tin","room-edit",id),edit("Đổi trạng thái","room-state",id),edit("Thêm ghế","seat-new",id)):List.of());
    }
    @GetMapping(SHOWS+"/detail")
    String show(@RequestParam String id,Model m){
        var r=one(db.query(SHOW_SQL+" WHERE s.MaSuat=?",id));
        return detail(m,"Suất chiếu "+id,SHOWS,r,columns("TenPhim","Phim","TenPhong","Phòng","ThoiGianBatDau","Bắt đầu","ThoiGianTraPhong","Trả phòng","TenLoaiThoiDiem","Loại thời điểm","TrangThai","Trạng thái"),List.of(edit("Đổi phim","show-film",id),edit("Đổi phòng","show-room",id),edit("Đổi ngày","show-date",id),edit("Đổi giờ","show-time",id),edit("Hủy suất chiếu","show-cancel",id)));
    }
    @GetMapping(PRICES+"/detail")
    String price(@RequestParam String id,Model m){
        return detail(m,"Chi tiết giá vé",PRICES,one(db.query(PRICE_SQL+" WHERE b.MaGia=?",id)),columns("TenLoaiGhe","Loại ghế","TenLoaiPhong","Loại phòng","TenLoaiThoiDiem","Thời điểm","GiaVe","Giá vé (₫)"),List.of(edit("Sửa giá vé","price-edit",id)));
    }
    @GetMapping(PRODUCTS+"/detail")
    String product(@RequestParam String id,Model m){
        var r=one(db.query("SELECT * FROM SANPHAM WHERE MaSP=?",id));
        return detail(m,r.get("TenSP").toString(),PRODUCTS,r,columns("MaSP","Mã sản phẩm","MoTa","Mô tả","DonViTinh","Đơn vị tính","GiaBan","Giá bán (₫)","SoLuongTon","Tồn kho","TinhTrangKinhDoanh","Kinh doanh"),List.of(edit("Sửa thông tin và tồn kho","product-edit",id),edit("Đổi giá bán","product-price",id)));
    }
    @GetMapping(FILMS+"/poster")
    ResponseEntity<byte[]> poster(@RequestParam String id){
        var r=db.query("SELECT w.Mime,w.Data FROM WEB_POSTER w JOIN PHIM p ON p.Poster=w.Id WHERE p.MaPhim=?",id);
        if(r.isEmpty())return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(r.get(0).get("Mime").toString())).body((byte[])r.get(0).get("Data"));
    }
}
