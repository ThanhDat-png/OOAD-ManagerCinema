package cinema.controller.customer;

import cinema.dao.KhachHangDAO;
import cinema.model.ChonGheKhach;
import java.sql.SQLException;
import java.util.*;

public final class KhachHangBUS {
    private final KhachHangDAO dao;

    public KhachHangBUS() {
        this(new KhachHangDAO());
    }

    public KhachHangBUS(KhachHangDAO dao) {
        if (dao == null) throw new IllegalArgumentException("Thiếu DAO khách hàng");
        this.dao = dao;
    }

    public static boolean hopLeMaChonGhe(String ma) {
        return ma != null && ma.strip().matches("(?i)[a-f0-9]{24}");
    }

    public List<ChonGheKhach.PhimChieu> danhSachPhim() throws SQLException {
        return dao.danhSachPhim();
    }

    public List<ChonGheKhach.PhimChieu> phimDangChieu() throws SQLException {
        return dao.danhSachPhim().stream()
                .filter(p -> "Đang chiếu".equals(p.phim().trangThai()))
                .toList();
    }

    public List<ChonGheKhach.PhimChieu> phimSapChieu() throws SQLException {
        return dao.danhSachPhim().stream()
                .filter(p -> "Sắp chiếu".equals(p.phim().trangThai()))
                .toList();
    }

    public ChonGheKhach.PhimChieu chiTietPhim(String maPhim) throws SQLException {
        if (maPhim == null || maPhim.isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn phim");
        }
        return dao.chiTietPhim(maPhim.trim())
                .orElseThrow(() -> new IllegalArgumentException("Phim không tồn tại hoặc không còn chiếu"));
    }

    public List<ChonGheKhach.SuatKhach> suatChieuCuaPhim(String maPhim) throws SQLException {
        if (maPhim == null || maPhim.isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn phim");
        }
        return dao.suatChieuCuaPhim(maPhim.trim());
    }

    public ChonGheKhach.SuatKhach chiTietSuat(int maSuat) throws SQLException {
        if (maSuat <= 0) throw new IllegalArgumentException("Suất chiếu không hợp lệ");
        return dao.chiTietSuat(maSuat)
                .orElseThrow(() -> new IllegalArgumentException("Suất chiếu không còn nhận đặt vé."));
    }

    public List<ChonGheKhach.GheSanh> soDoGhe(int maSuat) throws SQLException {
        if (maSuat <= 0) throw new IllegalArgumentException("Suất chiếu không hợp lệ");
        return dao.soDoGhe(maSuat);
    }

    public String taoMaChonGhe(int maSuat, Collection<Integer> dsMaGhe) throws SQLException {
        if (maSuat <= 0) throw new IllegalArgumentException("Suất chiếu không hợp lệ");
        if (dsMaGhe == null || dsMaGhe.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ghế còn trống.");
        }
        Set<Integer> unique = new LinkedHashSet<>();
        for (Integer id : dsMaGhe) {
            if (id == null || id <= 0 || !unique.add(id)) {
                throw new IllegalArgumentException("Vui lòng chọn ghế còn trống.");
            }
        }
        return dao.taoMaChonGhe(maSuat, unique);
    }

    public Optional<ChonGheKhach> traCuuMa(String maChonGhe) throws SQLException {
        if (!hopLeMaChonGhe(maChonGhe)) return Optional.empty();
        return dao.traCuuMa(maChonGhe.strip().toUpperCase(Locale.ROOT));
    }
}
