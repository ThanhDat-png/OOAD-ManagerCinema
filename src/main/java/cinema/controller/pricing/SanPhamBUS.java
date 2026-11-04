package cinema.controller.pricing;

import cinema.controller.auth.PhienDangNhap;
import cinema.dao.SanPhamDAO;
import cinema.model.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public final class SanPhamBUS {
    public static final List<String> TINH_TRANG=List.of("Đang kinh doanh","Ngừng kinh doanh");
    private final PhienDangNhap session;
    private final SanPhamDAO dao;
    public SanPhamBUS(PhienDangNhap session,SanPhamDAO dao) {
        if(session==null || session.vaiTro()==null) throw new IllegalArgumentException("Vui lòng đăng nhập");
        this.session=session; this.dao=dao;
    }
    private void admin() { if(session.vaiTro()!=VaiTro.ADMIN) throw new IllegalArgumentException("Bạn không có quyền quản lý F&B"); }
    public List<SanPham> search(String text,Boolean inStock) throws SQLException { admin(); return dao.search(text==null?"":text.trim(),inStock); }
    public SanPham detail(String id) throws SQLException { admin(); return dao.find(id); }
    public void create(SanPham input) throws SQLException {
        admin(); SanPham product=validate(input);
        dao.insert(new SanPham(product.maSP(),product.tenSP(),product.moTa(),product.donViTinh(),
                MoneyRules.positive(input.giaBan(),"Vui lòng nhập giá bán hợp lệ"),product.soLuongTon(),product.tinhTrangKinhDoanh()));
    }
    public void updateInfo(SanPham input) throws SQLException { admin(); dao.updateInfo(validate(input)); }
    public void updatePrice(String id,BigDecimal amount) throws SQLException { admin(); dao.updatePrice(id,MoneyRules.positive(amount,"Vui lòng nhập giá bán hợp lệ")); }
    private SanPham validate(SanPham input) {
        if(input==null) throw new IllegalArgumentException("Vui lòng nhập thông tin sản phẩm");
        String id=required(input.maSP(),"mã sản phẩm",10),name=required(input.tenSP(),"tên sản phẩm",100);
        String unit=required(input.donViTinh(),"đơn vị tính",20);
        String description=input.moTa()==null?"":input.moTa().trim();
        if(description.length()>255) throw new IllegalArgumentException("Mô tả tối đa 255 ký tự");
        if(input.soLuongTon()<0) throw new IllegalArgumentException("Số lượng tồn kho không hợp lệ");
        if(input.tinhTrangKinhDoanh()==null || !TINH_TRANG.contains(input.tinhTrangKinhDoanh())) throw new IllegalArgumentException("Tình trạng kinh doanh không hợp lệ");
        return new SanPham(id,name,description,unit,input.giaBan(),input.soLuongTon(),input.tinhTrangKinhDoanh());
    }
    private String required(String value,String label,int length) {
        if(value==null || value.isBlank()) throw new IllegalArgumentException("Vui lòng nhập "+label);
        String text=value.trim(); if(text.length()>length) throw new IllegalArgumentException("Thông tin "+label+" tối đa "+length+" ký tự"); return text;
    }
}
