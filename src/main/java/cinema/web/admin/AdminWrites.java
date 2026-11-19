package cinema.web.admin;

import cinema.controller.film.PosterStore;
import cinema.web.db.WebDb;
import cinema.web.db.WebDb.Tx;
import java.nio.file.*;
import java.io.IOException;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import static cinema.web.admin.AdminUi.*;

@Service
class AdminWrites {
    private final WebDb db;
    private final ScheduleEdits schedules;
    AdminWrites(WebDb db,ScheduleEdits schedules){this.db=db;this.schedules=schedules;}
    record Poster(String id,String mime,byte[] bytes){}
    private static Poster read(MultipartFile file) {
        check(file!=null&&!file.isEmpty(),"Vui lòng chọn poster phim.");
        check(file.getSize()<=10*1024*1024,"Poster vượt quá 10 MiB.");
        try{
            Path temp=Files.createTempFile("cinema-poster-",".upload");
            try {
                file.transferTo(temp); new PosterStore().preview(temp);
                byte[] bytes=Files.readAllBytes(temp);
                return new Poster(UUID.randomUUID().toString(),bytes[0]==(byte)0x89?"image/png":"image/jpeg",bytes);
            }finally{Files.deleteIfExists(temp);}
        }catch(IOException e){throw new IllegalArgumentException("Không đọc được poster. Vui lòng chọn lại tệp.",e);}
    }
    void validateShow(String id){db.write(tx->{schedules.editable(tx,id);return null;});}
    String save(String op,String id,Map<String,String> p,List<String> genres,MultipartFile file,AdminForms.Form form){
        Poster poster=Set.of("film-new","film-poster").contains(op)?read(file):null;
        return db.write(tx->{
            if(form.sql()!=null)one(tx.query(form.sql(),id));
            if(poster!=null)tx.update("INSERT INTO WEB_POSTER (Id,Mime,Data) VALUES (?,?,?)",poster.id(),poster.mime(),poster.bytes());
            String saved=switch(op.split("-")[0]){
                case "employee"->employee(tx,op,id,p);
                case "film"->film(tx,op,id,p,genres,poster);
                case "room"->schedules.room(tx,op,id,p);
                case "seat"->schedules.seat(tx,op,id,p);
                case "show"->schedules.show(tx,op,id,p);
                case "price"->price(tx,op,id,p);
                case "product"->product(tx,op,id,p);
                default->throw new IllegalArgumentException("Thao tác không hợp lệ.");
            };
            return saved==null?form.back():Catalog.url(form.back()+"/detail","id",saved);
        });
    }
    private String employee(Tx tx,String op,String id,Map<String,String> p){
        if(op.equals("employee-delete")){
            check(tx.query("SELECT 1 FROM GIAODICH WHERE MaNV=? LIMIT 1",id).isEmpty(),"Không thể xóa nhân viên vì có dữ liệu giao dịch liên quan");
            tx.update("DELETE FROM TAIKHOAN WHERE MaNV=?",id);tx.update("DELETE FROM NHANVIEN WHERE MaNV=?",id);return null;
        }
        String name=required(p,"HoTen",100),phone=required(p,"SoDienThoai",15),email=required(p,"Email",100),address=required(p,"DiaChi",200),position=required(p,"ChucVu",50);
        LocalDate dob=date(p,"NgaySinh");check(!dob.isAfter(LocalDate.now(ZONE)),"Ngày sinh không thể ở tương lai.");
        check(phone.matches("\\+?[0-9]{8,15}"),"Số điện thoại không hợp lệ.");check(email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"),"Email không hợp lệ.");
        if(op.equals("employee-new")){
            id=id("NV");tx.update("INSERT INTO NHANVIEN (HoTen,NgaySinh,SoDienThoai,Email,DiaChi,ChucVu,MaNV) VALUES (?,?,?,?,?,?,?)",name,dob.toString(),phone,email,address,position,id);
        }else tx.update("UPDATE NHANVIEN SET HoTen=?,NgaySinh=?,SoDienThoai=?,Email=?,DiaChi=?,ChucVu=? WHERE MaNV=?",name,dob.toString(),phone,email,address,position,id);
        return id;
    }
    private String film(Tx tx,String op,String id,Map<String,String> p,List<String> genres,Poster poster){
        switch(op){
            case "film-new"->{
                id=id("P");tx.update("INSERT INTO PHIM (MaPhim,TenPhim,ThoiLuong,DaoDien,NgayKhoiChieu,GioiHanTuoi,Poster,TrangThai,DinhDangChieu) VALUES (?,?,?,?,?,?,?,?,?)",id,required(p,"TenPhim",200),number(p,"ThoiLuong",1),required(p,"DaoDien",100),date(p,"NgayKhoiChieu").toString(),number(p,"GioiHanTuoi",0),poster.id(),member(p,"TrangThai",filmStates),required(p,"DinhDangChieu",20));
            }
            case "film-name"->tx.update("UPDATE PHIM SET TenPhim=? WHERE MaPhim=?",required(p,"TenPhim",200),id);
            case "film-poster"->tx.update("UPDATE PHIM SET Poster=? WHERE MaPhim=?",poster.id(),id);
            case "film-stop"->{
                check(tx.query("SELECT 1 FROM SUATCHIEU WHERE MaPhim=? AND TrangThai<>'Đã hủy' AND ThoiGianTraPhong>? LIMIT 1",id,now()).isEmpty(),"Phim vẫn còn suất chiếu chưa kết thúc.");
                tx.update("UPDATE PHIM SET TrangThai='Ngừng chiếu' WHERE MaPhim=?",id);
            }
            default->{}
        }
        if(op.equals("film-new")||op.equals("film-genres")){
            var unique=new LinkedHashSet<>(genres);check(!unique.isEmpty(),"Vui lòng chọn ít nhất một thể loại.");
            tx.update("DELETE FROM PHIM_THELOAI WHERE MaPhim=?",id);
            for(String g:unique){
                check(!tx.query("SELECT 1 FROM THELOAI WHERE MaTheLoai=?",g).isEmpty(),"Thể loại không hợp lệ.");
                tx.update("INSERT INTO PHIM_THELOAI (MaPhim,MaTheLoai) VALUES (?,?)",id,g);
            }
        }return id;
    }
    static int reference(Tx tx,Map<String,String> p,String key,String table) {
        int n=number(p,key,1);check(!tx.query("SELECT 1 FROM "+table+" WHERE "+key+"=?",n).isEmpty(),"Danh mục đã chọn không hợp lệ.");return n;
    }
    private String price(Tx tx,String op,String id,Map<String,String> p){
        long amount=money(p,"GiaVe");
        if(op.equals("price-edit"))tx.update("UPDATE BANGGIAVE SET GiaVe=? WHERE MaGia=?",amount,id);
        else{
            int seat=reference(tx,p,"MaLoaiGhe","LOAIGHE"),room=reference(tx,p,"MaLoaiPhong","LOAIPHONG"),period=reference(tx,p,"MaLoaiThoiDiem","LOAITHOIDIEM");
            check(tx.query("SELECT 1 FROM BANGGIAVE WHERE MaLoaiGhe=? AND MaLoaiPhong=? AND MaLoaiThoiDiem=?",seat,room,period).isEmpty(),"Giá vé cho điều kiện này đã tồn tại.");
            tx.update("INSERT INTO BANGGIAVE (MaLoaiGhe,MaLoaiPhong,MaLoaiThoiDiem,GiaVe) VALUES (?,?,?,?)",seat,room,period,amount);
            id=one(tx.query("SELECT last_insert_rowid() id")).get("id").toString();
        }return id;
    }
    private String product(Tx tx,String op,String id,Map<String,String> p){
        if(op.equals("product-price")){tx.update("UPDATE SANPHAM SET GiaBan=? WHERE MaSP=?",money(p,"GiaBan"),id);return id;}
        String name=required(p,"TenSP",100),unit=required(p,"DonViTinh",20),description=val(p,"MoTa"),state=member(p,"TinhTrangKinhDoanh",productStates);
        int stock=number(p,"SoLuongTon",0);check(description.length()<=255,"Mô tả tối đa 255 ký tự.");
        if(op.equals("product-new")){
            id=required(p,"MaSP",10);check(tx.query("SELECT 1 FROM SANPHAM WHERE MaSP=?",id).isEmpty(),"Mã sản phẩm đã tồn tại.");
            tx.update("INSERT INTO SANPHAM (MaSP,TenSP,MoTa,DonViTinh,SoLuongTon,TinhTrangKinhDoanh,GiaBan) VALUES (?,?,?,?,?,?,?)",id,name,description,unit,stock,state,money(p,"GiaBan"));
        }else tx.update("UPDATE SANPHAM SET TenSP=?,MoTa=?,DonViTinh=?,SoLuongTon=?,TinhTrangKinhDoanh=? WHERE MaSP=?",name,description,unit,stock,state,id);
        return id;
    }
}
