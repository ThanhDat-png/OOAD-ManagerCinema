package cinema.web.admin;

import cinema.web.db.WebDb;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import static cinema.web.admin.AdminUi.*;

@Controller
public class AdminForms {
    final Catalog catalog;
    final AdminWrites writes;
    AdminForms(Catalog catalog,AdminWrites writes){this.catalog=catalog;this.writes=writes;}
    record Form(String title,String back,String sql,List<Field> fields,String warning) {}
    static Field input(String key,String kind){return field(key,label(key),kind);}
    static List<Field> append(List<Field> a,Field... fields){List<Field> result=new ArrayList<>(a);result.addAll(List.of(fields));return result;}
    Form form(String op) {
        List<Field> employee=List.of(input("HoTen","text"),input("NgaySinh","date"),input("SoDienThoai","tel"),input("Email","email"),input("DiaChi","text"),input("ChucVu","text"));
        Field poster=field("upload","Poster (PNG hoặc JPEG, tối đa 10 MiB)","file");
        String person="SELECT * FROM NHANVIEN WHERE MaNV=?",film="SELECT * FROM PHIM WHERE MaPhim=?",room="SELECT * FROM PHONGCHIEU WHERE MaPhong=?",seat="SELECT * FROM GHE WHERE MaGhe=?",show="SELECT *,substr(ThoiGianBatDau,1,10) date,substr(ThoiGianBatDau,12,5) time FROM SUATCHIEU WHERE MaSuat=?",product="SELECT * FROM SANPHAM WHERE MaSP=?";
        return switch(op) {
            case "employee-new"->new Form("Thêm nhân viên",EMP,null,employee,null);
            case "employee-edit"->new Form("Sửa hồ sơ nhân viên",EMP,person,employee,null);
            case "employee-delete"->new Form("Xóa nhân viên",EMP,person,List.of(),"Xác nhận xóa hồ sơ và tài khoản liên kết? Nhân viên đã thực hiện giao dịch phải được giữ lại.");
            case "film-new"->new Form("Thêm phim",FILMS,null,List.of(input("TenPhim","text"),input("DaoDien","text"),input("ThoiLuong","number"),input("NgayKhoiChieu","date"),input("GioiHanTuoi","number"),input("DinhDangChieu","text"),choice("TrangThai","Trạng thái",options(filmStates)),new Field("genres","Thể loại","multi",catalog.genres(),true),poster),null);
            case "film-name"->new Form("Sửa tên phim",FILMS,film,List.of(input("TenPhim","text")),null);
            case "film-genres"->new Form("Cập nhật thể loại",FILMS,film,List.of(new Field("genres","Thể loại","multi",catalog.genres(),true)),null);
            case "film-poster"->new Form("Thay poster",FILMS,film,List.of(poster),null);
            case "film-stop"->new Form("Ngừng chiếu phim",FILMS,film,List.of(),"Xác nhận ngừng chiếu phim này? Lịch sử suất chiếu và vé sẽ được lưu.");
            case "room-new","room-edit"->{
                var fields=List.of(input("TenPhong","text"),choice("MaLoaiPhong","Loại phòng",catalog.roomTypes()),input("ThoiGianDonPhong","number"));
                yield op.endsWith("new")?new Form("Thêm phòng chiếu",ROOMS,null,append(fields,choice("TrangThai","Trạng thái",options(roomStates))),null):new Form("Sửa phòng chiếu",ROOMS,room,fields,null);
            }
            case "room-state"->new Form("Đổi trạng thái phòng",ROOMS,room,List.of(choice("TrangThai","Trạng thái",options(roomStates))),null);
            case "seat-new","seat-edit"->{
                var fields=List.of(input("HangGhe","text"),input("SoGhe","number"),choice("TrangThai","Trạng thái",options(seatStates)));
                yield op.endsWith("new")?new Form("Thêm ghế",ROOMS,room,append(fields,choice("MaLoaiGhe","Loại ghế",catalog.seats())),null):new Form("Sửa vị trí và trạng thái ghế",ROOMS,seat,fields,null);
            }
            case "seat-type"->new Form("Phân loại ghế",ROOMS,seat,List.of(choice("MaLoaiGhe","Loại ghế",catalog.seats())),null);
            case "show-new","show-film","show-room","show-date","show-time"->{
                List<Field> fields=new ArrayList<>();
                boolean create=op.equals("show-new");
                if(create||op.equals("show-film"))fields.add(choice("MaPhim","Phim",catalog.films()));
                if(create||op.equals("show-room"))fields.add(choice("MaPhong","Phòng chiếu",catalog.rooms()));
                if(create||op.equals("show-date"))fields.add(field("date","Ngày chiếu","date"));
                if(create||op.equals("show-time"))fields.add(field("time","Giờ bắt đầu","time"));
                String title=switch(op){case "show-film"->"Đổi phim";case "show-room"->"Đổi phòng";case "show-date"->"Đổi ngày chiếu";case "show-time"->"Đổi giờ chiếu";default->"Tạo suất chiếu";};
                yield new Form(title,SHOWS,create?null:show,fields,create?null:"Thay đổi lịch sẽ giải phóng các lượt giữ ghế chưa thanh toán của suất này.");
            }
            case "show-cancel"->new Form("Hủy suất chiếu",SHOWS,show,List.of(),"Xác nhận hủy suất chiếu và giải phóng các lượt giữ ghế chưa thanh toán?");
            case "price-new"->new Form("Thiết lập giá vé",PRICES,null,List.of(choice("MaLoaiGhe","Loại ghế",catalog.seats()),choice("MaLoaiPhong","Loại phòng",catalog.roomTypes()),choice("MaLoaiThoiDiem","Loại thời điểm",catalog.periods()),field("GiaVe","Giá vé (₫)","number")),null);
            case "price-edit"->new Form("Sửa giá vé",PRICES,Catalog.PRICE_SQL+" WHERE b.MaGia=?",List.of(field("GiaVe","Giá vé mới (₫)","number")),null);
            case "product-new","product-edit"->{
                List<Field> fields=new ArrayList<>();
                if(op.endsWith("new"))fields.add(input("MaSP","text"));
                fields.addAll(List.of(input("TenSP","text"),optional(field("MoTa","Mô tả","textarea")),input("DonViTinh","text"),input("SoLuongTon","number"),choice("TinhTrangKinhDoanh","Tình trạng kinh doanh",options(productStates))));
                if(op.endsWith("new"))fields.add(field("GiaBan","Giá bán (₫)","number"));
                yield new Form(op.endsWith("new")?"Thêm sản phẩm":"Cập nhật sản phẩm và tồn kho",PRODUCTS,op.endsWith("new")?null:product,fields,null);
            }
            case "product-price"->new Form("Cập nhật giá bán",PRODUCTS,product,List.of(field("GiaBan","Giá bán mới (₫)","number")),null);
            default->throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        };
    }
    String render(String op,String id,Form f,Map<String,?> values,List<String> selected,Model m){
        m.addAttribute("form",f);m.addAttribute("title",f.title());m.addAttribute("op",op);m.addAttribute("id",id);m.addAttribute("values",values);m.addAttribute("selected",selected);
        String back=f.back();
        if(!id.isBlank()&&!op.equals("seat-edit")&&!op.equals("seat-type"))back=Catalog.url(back+"/detail","id",id);
        if(op.equals("seat-edit")||op.equals("seat-type"))back=Catalog.url(ROOMS+"/detail","id",one(catalog.db.query("SELECT MaPhong FROM GHE WHERE MaGhe=?",id)).get("MaPhong"));
        m.addAttribute("back",back);
        return "admin/form";
    }
    @GetMapping("/admin/form/{op}")
    String get(@PathVariable String op,@RequestParam(defaultValue="") String id,Model m){
        var f=form(op);var row=f.sql()==null?Map.<String,Object>of():one(catalog.db.query(f.sql(),id));
        List<String> selected=op.equals("film-genres")?catalog.db.query("SELECT MaTheLoai FROM PHIM_THELOAI WHERE MaPhim=?",id).stream().map(r->r.get("MaTheLoai").toString()).toList():List.of();
        if(op.startsWith("show-")&&!op.equals("show-new")){
            try{writes.validateShow(id);}catch(IllegalArgumentException ex){m.addAttribute("blocked",true);m.addAttribute("error",ex.getMessage());}
        }
        return render(op,id,f,row,selected,m);
    }
    @PostMapping("/admin/form/{op}")
    String post(@PathVariable String op,@RequestParam(defaultValue="") String id,@RequestParam MultiValueMap<String,String> params,@RequestParam(name="upload",required=false) MultipartFile file,Model m,RedirectAttributes flash){
        Form f=form(op);
        var values=params.toSingleValueMap();
        var genres=params.getOrDefault("genres",List.of());
        try{
            String location=writes.save(op,id,values,genres,file,f);
            flash.addFlashAttribute("adminSuccess",switch(op){
                case "employee-new"->"Thêm nhân viên thành công";
                case "employee-edit"->"Cập nhật nhân viên thành công";
                default->"Đã lưu thay đổi.";
            });
            return "redirect:"+location;
        }catch(IllegalArgumentException e){
            m.addAttribute("error",e.getMessage());
            return render(op,id,f,values,genres,m);
        }
    }
}
