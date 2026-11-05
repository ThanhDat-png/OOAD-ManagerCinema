package cinema.controller.revenue;

import cinema.controller.auth.PhienDangNhap;
import cinema.controller.pricing.SanPhamBUS;
import cinema.dao.*;
import cinema.model.*;
import java.sql.SQLException;
import java.util.*;

public final class BaoCaoBUS {
    private final BaoCaoDAO dao;
    public BaoCaoBUS(PhienDangNhap session,BaoCaoDAO dao) {
        if(session==null || session.vaiTro()==null)throw new IllegalArgumentException("Vui lòng đăng nhập");
        if(session.vaiTro()!=VaiTro.ADMIN)throw new IllegalArgumentException("Chỉ Admin được xem báo cáo và thống kê");
        this.dao=Objects.requireNonNull(dao);
    }
    public BaoCaoDAO.TongHop theoThoiGian(KhoangThongKe range)throws SQLException {return dao.theoThoiGian(required(range));}
    public List<BaoCaoDAO.TheoPhim> theoPhim(KhoangThongKe range,String film)throws SQLException {return dao.theoPhim(required(range),code(film));}
    public List<BaoCaoDAO.TheoNhanVien> theoNhanVien(KhoangThongKe range,String employee)throws SQLException {return dao.theoNhanVien(required(range),code(employee));}
    public BaoCaoDAO.ThongKeVe ve(KhoangThongKe range,String film,Integer show)throws SQLException {
        required(range);if(show!=null && show<=0)throw new IllegalArgumentException("Suất chiếu không hợp lệ");
        return dao.ve(range,code(film),show);
    }
    public List<SanPham> sanPham(String product,String status,Boolean stock)throws SQLException {
        String state=status==null || status.isBlank()?null:status.trim();
        if(state!=null && !SanPhamBUS.TINH_TRANG.contains(state))throw new IllegalArgumentException("Tình trạng kinh doanh không hợp lệ");
        return dao.sanPham(code(product),state,stock);
    }
    public record DanhMuc(List<Phim> phim,List<NhanVien> nhanVien,List<SuatChieu> suat,List<SanPham> sanPham) {
        public DanhMuc {phim=List.copyOf(phim);nhanVien=List.copyOf(nhanVien);suat=List.copyOf(suat);sanPham=List.copyOf(sanPham);}
    }
    public DanhMuc danhMuc()throws SQLException {
        return new DanhMuc(new PhimDAO().search("",null,null),new NhanVienDAO().search(""),new SuatChieuDAO().list(null,null),new SanPhamDAO().search("",null));
    }
    private static KhoangThongKe required(KhoangThongKe range){if(range==null)throw new IllegalArgumentException("Vui lòng chọn thời gian thống kê");return range;}
    private static String code(String value){if(value==null || value.isBlank())return null;String result=value.trim();if(result.length()>10)throw new IllegalArgumentException("Mã lọc tối đa 10 ký tự");return result;}
}
