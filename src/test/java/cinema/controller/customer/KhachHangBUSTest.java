package cinema.controller.customer;

import cinema.controller.auth.PhienDangNhap;
import cinema.controller.sales.BanVeBUS;
import cinema.dao.BanVeDAO;
import cinema.dao.KhachHangDAO;
import cinema.model.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KhachHangBUSTest {
    private static final Phim PHIM_DANG_CHIEU = new Phim(
            "PH001", "Mắt biếc", 117, "Victor Vũ", LocalDate.of(2026, 11, 1),
            13, "", "Đang chiếu", "2D", List.of());
    private static final Phim PHIM_SAP_CHIEU = new Phim(
            "PH002", "Đất rừng phương Nam", 110, "Nguyễn Quang Dũng", LocalDate.of(2026, 12, 1),
            0, "", "Sắp chiếu", "2D", List.of());

    @Test
    void splitsNowShowingAndUpcomingAndValidatesSelectionCreation() throws Exception {
        String code = "ABCDEF0123456789ABCDEF01";
        KhachHangDAO fakeDao = new KhachHangDAO() {
            @Override public List<ChonGheKhach.PhimChieu> danhSachPhim() {
                return List.of(
                        new ChonGheKhach.PhimChieu(PHIM_DANG_CHIEU, "Tình cảm, Chính kịch"),
                        new ChonGheKhach.PhimChieu(PHIM_SAP_CHIEU, "Phiêu lưu"));
            }
            @Override public String taoMaChonGhe(int maSuat, Set<Integer> maGhe) {
                assertEquals(10, maSuat);
                assertEquals(Set.of(1, 2), maGhe);
                return code;
            }
            @Override public Optional<ChonGheKhach> traCuuMa(String maChonGhe) {
                if (!code.equals(maChonGhe)) return Optional.empty();
                return Optional.of(new ChonGheKhach(
                        code, 10, "PH001", "Mắt biếc", "PHONG01", "Phòng chiếu 1",
                        LocalDateTime.of(2026, 11, 25, 19, 30),
                        LocalDateTime.of(2026, 11, 24, 17, 30),
                        null,
                        List.of(
                                new ChonGheKhach.GheChon(1, "A1", "Ghế thường", new BigDecimal("75000")),
                                new ChonGheKhach.GheChon(2, "A2", "Ghế thường", new BigDecimal("75000"))),
                        new BigDecimal("150000"), false, false, false));
            }
        };

        KhachHangBUS bus = new KhachHangBUS(fakeDao);
        assertEquals(1, bus.phimDangChieu().size());
        assertEquals("PH001", bus.phimDangChieu().get(0).phim().maPhim());
        assertEquals(1, bus.phimSapChieu().size());
        assertEquals("PH002", bus.phimSapChieu().get(0).phim().maPhim());

        assertThrows(IllegalArgumentException.class, () -> bus.taoMaChonGhe(0, List.of(1)));
        assertThrows(IllegalArgumentException.class, () -> bus.taoMaChonGhe(10, List.of()));
        assertThrows(IllegalArgumentException.class, () -> bus.taoMaChonGhe(10, List.of(1, 1)));
        assertThrows(IllegalArgumentException.class, () -> bus.taoMaChonGhe(10, List.of(-1)));

        String created = bus.taoMaChonGhe(10, List.of(1, 2));
        assertEquals(code, created);
        assertTrue(bus.traCuuMa("  abcdef0123456789abcdef01 ").isPresent());
        assertTrue(bus.traCuuMa("khong-hop-le").isEmpty());
    }

    @Test
    void counterClaimEnforcesStaffRoleAndNormalizesReferenceCode() throws Exception {
        String code = "1234567890ABCDEF12345678";
        NhanVien nv = new NhanVien("NV002", "Trần Thị Bình", LocalDate.of(1999, 5, 10),
                "0901234567", "binh@cinema.local", "TP.HCM", "Nhân viên bán vé");
        PhienDangNhap staffSession = new PhienDangNhap(2, "staff_demo", VaiTro.NHAN_VIEN, nv);
        PhienDangNhap adminSession = new PhienDangNhap(1, "admin_demo", VaiTro.ADMIN, null);

        GiaoDich gd = new GiaoDich("G1234567890123456789", "NV002",
                LocalDateTime.now(), new BigDecimal("90000"), "Chờ thanh toán");
        SuatChieu suat = new SuatChieu(7, "PH001", "PHONG01", 1,
                LocalDateTime.now().plusHours(3), LocalDateTime.now().plusHours(5), "Sắp chiếu");
        PhongChieu phong = new PhongChieu("PHONG01", new LoaiPhong(1, "Phòng 2D", "2D"),
                "Phòng chiếu 1", 15, "Đang hoạt động");
        Ghe ghe = new Ghe(11, "PHONG01", new LoaiGhe(1, "Ghế thường"), "A", 1, "Sẵn sàng sử dụng");
        GheSuatChieu gsc = new GheSuatChieu(7, ghe, gd.maGiaoDich(), "Đang giữ",
                LocalDateTime.now().plusMinutes(5), 1, new BigDecimal("90000"));
        BanVeDAO.GioHang cart = new BanVeDAO.GioHang(gd, suat, phong, "Mắt biếc", List.of(gsc), null);

        KhachHangDAO fakeCustomerDao = new KhachHangDAO() {
            @Override public List<ChonGheKhach.YeuCauCho> danhSachChoTiepNhan() {
                return List.of(new ChonGheKhach.YeuCauCho(
                        code, 7, "Mắt biếc", "Phòng chiếu 1", suat.thoiGianBatDau(), "A1"));
            }
            @Override public ChonGheKhach.KetQuaTiepNhan tiepNhanMa(
                    String maChonGhe, String maNV, String maGiaoDichHienTai, int holdMinutes) throws SQLException {
                assertEquals(code, maChonGhe);
                assertEquals("NV002", maNV);
                assertEquals("G_CURRENT", maGiaoDichHienTai);
                assertEquals(5, holdMinutes);
                return new ChonGheKhach.KetQuaTiepNhan(cart, 7, false);
            }
        };

        BanVeBUS adminBus = new BanVeBUS(adminSession, new BanVeDAO(), fakeCustomerDao, 5);
        assertThrows(IllegalArgumentException.class, adminBus::yeuCauChonGheKhach);

        BanVeBUS staffBus = new BanVeBUS(staffSession, new BanVeDAO(), fakeCustomerDao, 5);
        assertEquals(1, staffBus.yeuCauChonGheKhach().size());
        assertThrows(IllegalArgumentException.class, () -> staffBus.nhanMaChonGhe("sai-ma", "G_CURRENT"));

        ChonGheKhach.KetQuaTiepNhan claimed = staffBus.nhanMaChonGhe(" 1234567890abcdef12345678 ", "G_CURRENT");
        assertEquals(7, claimed.maSuat());
        assertEquals("G1234567890123456789", claimed.gioHang().giaoDich().maGiaoDich());
    }
}
