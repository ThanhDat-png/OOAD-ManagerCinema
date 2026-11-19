package cinema.web.admin;

import cinema.web.db.WebDb;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class AdminUi {
    static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    static final DateTimeFormatter SQL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    record Option(String value, String label) {}
    record Field(String key, String label, String kind, List<Option> options, boolean required) {}
    record Action(String label, String url) {}
    record Column(String key, String label) {}
    record Page(String title, String url, List<Column> columns, List<Field> filters, String create, String createLabel) {}
    static final String FILMS="/catalog/films", ROOMS="/catalog/rooms", EMP="/admin/employees",
            SHOWS="/admin/showtimes", PRICES="/admin/prices", PRODUCTS="/admin/products";
    static final String[] filmStates={"Sắp chiếu","Đang chiếu","Ngừng chiếu"};
    static final String[] roomStates={"Đang hoạt động","Tạm ngừng hoạt động","Bảo trì"};
    static final String[] seatStates={"Sẵn sàng sử dụng","Bảo trì"};
    static final String[] productStates={"Đang kinh doanh","Ngừng kinh doanh"};
    static List<Option> options(String... labels) {
        return Arrays.stream(labels).map(v->new Option(v,v)).toList();
    }
    static List<Option> options(WebDb db,String sql) {
        return db.query(sql).stream().map(r->new Option(r.get("id").toString(),r.get("label").toString())).toList();
    }
    static Field field(String key,String label,String type) {return new Field(key,label,type,List.of(),true);}
    static Field choice(String key,String label,List<Option> opts) {return new Field(key,label,"select",opts,true);}
    static Field optional(Field f){return new Field(f.key,f.label,f.kind,f.options,false);}
    static List<Column> columns(String... pairs) {
        List<Column> cols=new ArrayList<>();
        for(int i=0;i<pairs.length;i+=2) cols.add(new Column(pairs[i],pairs[i+1]));
        return List.copyOf(cols);
    }
    static String val(Map<String,String> params,String key){return params.getOrDefault(key,"").trim();}
    static String required(Map<String,String> params,String key,int max) {
        String value=val(params,key);
        check(!value.isEmpty()&&value.length()<=max,"Vui lòng nhập giá trị hợp lệ cho "+label(key)+" (tối đa "+max+" ký tự).");
        return value;
    }
    static int number(Map<String,String> params,String key,int min) {
        try {
            int n=Integer.parseInt(val(params,key));check(n>=min,label(key)+" phải từ "+min+" trở lên.");return n;
        } catch(NumberFormatException ex){throw new IllegalArgumentException(label(key)+" phải là số nguyên hợp lệ.");}
    }
    static long money(Map<String,String> params,String key) {
        String v=val(params,key);check(v.matches("[0-9]{1,12}")&&Long.parseLong(v)>0,"Số tiền phải là số nguyên dương, tối đa 12 chữ số.");return Long.parseLong(v);
    }
    static String member(Map<String,String> p,String name,String[] values){
        String v=val(p,name);check(Arrays.asList(values).contains(v),"Vui lòng chọn "+label(name)+" hợp lệ.");return v;
    }
    static LocalDate date(Map<String,String> p,String key) {
        try{return LocalDate.parse(val(p,key));}catch(DateTimeException e){throw new IllegalArgumentException(label(key)+" không hợp lệ.");}
    }
    static String id(String prefix){return prefix+UUID.randomUUID().toString().replace("-","").substring(0,10-prefix.length()).toUpperCase(Locale.ROOT);}
    static void check(boolean condition,String message){if(!condition)throw new IllegalArgumentException(message);}
    static Map<String,Object> one(List<Map<String,Object>> rows){if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy dữ liệu");return rows.get(0);}
    static String now(){return SQL_TIME.format(LocalDateTime.now(ZONE));}
    static String label(String k){
        return switch(k){
            case "HoTen"->"Họ tên";case "TenPhim"->"Tên phim";case "TenPhong"->"Tên phòng";case "MaSP"->"Mã sản phẩm";case "TenSP"->"Tên sản phẩm";
            case "DonViTinh"->"Đơn vị tính";case "SoLuongTon"->"Số lượng tồn";case "NgaySinh"->"Ngày sinh";case "SoDienThoai"->"Số điện thoại";case "DiaChi"->"Địa chỉ";case "ChucVu"->"Chức vụ";
            case "DaoDien"->"Đạo diễn";case "ThoiLuong"->"Thời lượng";case "GioiHanTuoi"->"Giới hạn tuổi";case "DinhDangChieu"->"Định dạng chiếu";case "NgayKhoiChieu"->"Ngày khởi chiếu";
            case "TrangThai","TinhTrangKinhDoanh"->"Trạng thái";case "ThoiGianDonPhong"->"Thời gian dọn phòng";case "HangGhe"->"Hàng ghế";case "SoGhe"->"Số ghế";case "date"->"Ngày chiếu";default->k;};
    }
    private AdminUi(){}
}
