package cinema.dao;

import cinema.controller.sales.BanVeBUS;
import cinema.db.Database;
import cinema.model.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class KhachHangDAO {
    private static final String VISIBLE = "p.TrangThai IN ('Sắp chiếu','Đang chiếu')";
    private static final String FILM_SELECT = """
            SELECT p.*,
                   (SELECT GROUP_CONCAT(t.TenTheLoai ORDER BY t.TenTheLoai SEPARATOR ', ')
                    FROM PHIM_THELOAI pt JOIN THELOAI t ON t.MaTheLoai=pt.MaTheLoai
                    WHERE pt.MaPhim=p.MaPhim) AS TheLoai
            FROM PHIM p WHERE\s""";
    private static final String SHOW_SELECT = """
            SELECT s.*, p.TenPhim, p.ThoiLuong, p.DaoDien, p.NgayKhoiChieu, p.GioiHanTuoi,
                   p.Poster, p.TrangThai AS TrangThaiPhim, p.DinhDangChieu, r.TenPhong
            FROM SUATCHIEU s
            JOIN PHIM p ON p.MaPhim=s.MaPhim
            JOIN PHONGCHIEU r ON r.MaPhong=s.MaPhong
            WHERE s.TrangThai<>'Đã hủy' AND s.ThoiGianBatDau>NOW()
              AND r.TrangThai='Đang hoạt động' AND\s""";
    private static final String SEATS_SELECT = """
            SELECT g.MaGhe, g.HangGhe, g.SoGhe, CONCAT(TRIM(g.HangGhe), g.SoGhe) AS ViTri,
                   l.TenLoaiGhe, b.GiaVe,
                   CASE WHEN g.TrangThai='Sẵn sàng sử dụng' AND b.GiaVe IS NOT NULL
                        AND (h.TrangThai='Trống' OR (h.TrangThai='Đang giữ'
                        AND gd.TrangThai='Chờ thanh toán' AND h.ThoiGianHetHanGiu<=NOW()))
                        THEN 1 ELSE 0 END AS KhaDung
            FROM GHE_SUATCHIEU h
            JOIN GHE g ON g.MaGhe=h.MaGhe
            JOIN LOAIGHE l ON l.MaLoaiGhe=g.MaLoaiGhe
            JOIN SUATCHIEU s ON s.MaSuat=h.MaSuat AND s.MaPhong=g.MaPhong
            JOIN PHONGCHIEU r ON r.MaPhong=s.MaPhong
            LEFT JOIN BANGGIAVE b ON b.MaLoaiGhe=g.MaLoaiGhe
                AND b.MaLoaiPhong=r.MaLoaiPhong AND b.MaLoaiThoiDiem=s.MaLoaiThoiDiem
            LEFT JOIN GIAODICH gd ON gd.MaGiaoDich=h.MaGiaoDich
            WHERE h.MaSuat=?
            ORDER BY g.HangGhe, g.SoGhe
            """;

    public List<ChonGheKhach.PhimChieu> danhSachPhim() throws SQLException {
        String sql = FILM_SELECT + VISIBLE + " ORDER BY FIELD(p.TrangThai,'Đang chiếu','Sắp chiếu'), p.TenPhim";
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<ChonGheKhach.PhimChieu> result = new ArrayList<>();
            while (rs.next()) result.add(readFilm(rs));
            return result;
        }
    }

    public Optional<ChonGheKhach.PhimChieu> chiTietPhim(String maPhim) throws SQLException {
        String sql = FILM_SELECT + VISIBLE + " AND p.MaPhim=?";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhim);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(readFilm(rs)) : Optional.empty();
            }
        }
    }

    public List<ChonGheKhach.SuatKhach> suatChieuCuaPhim(String maPhim) throws SQLException {
        String sql = SHOW_SELECT + VISIBLE + " AND p.MaPhim=? ORDER BY s.ThoiGianBatDau, s.MaSuat";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhim);
            try (ResultSet rs = ps.executeQuery()) {
                List<ChonGheKhach.SuatKhach> result = new ArrayList<>();
                while (rs.next()) result.add(readShow(rs));
                return result;
            }
        }
    }

    public Optional<ChonGheKhach.SuatKhach> chiTietSuat(int maSuat) throws SQLException {
        String sql = SHOW_SELECT + VISIBLE + " AND s.MaSuat=?";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, maSuat);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(readShow(rs)) : Optional.empty();
            }
        }
    }

    public List<ChonGheKhach.GheSanh> soDoGhe(int maSuat) throws SQLException {
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(SEATS_SELECT)) {
            ps.setInt(1, maSuat);
            try (ResultSet rs = ps.executeQuery()) {
                List<ChonGheKhach.GheSanh> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(new ChonGheKhach.GheSanh(
                            rs.getInt("MaGhe"),
                            rs.getString("HangGhe").trim(),
                            rs.getInt("SoGhe"),
                            rs.getString("ViTri"),
                            rs.getString("TenLoaiGhe"),
                            rs.getBigDecimal("GiaVe"),
                            rs.getInt("KhaDung") == 1));
                }
                return result;
            }
        }
    }

    public String taoMaChonGhe(int maSuat, Set<Integer> maGhe) throws SQLException {
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase(Locale.ROOT);
        try (Connection c = Database.connect()) {
            PhongChieuDAO.begin(c);
            try {
                ChonGheKhach.SuatKhach slot;
                try (PreparedStatement ps = c.prepareStatement(SHOW_SELECT + VISIBLE + " AND s.MaSuat=? FOR SHARE")) {
                    ps.setInt(1, maSuat);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Suất chiếu không còn nhận đặt vé.");
                        slot = readShow(rs);
                    }
                }
                List<ChonGheKhach.GheSanh> chosen = new ArrayList<>();
                try (PreparedStatement ps = c.prepareStatement(SEATS_SELECT)) {
                    ps.setInt(1, maSuat);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int id = rs.getInt("MaGhe");
                            if (maGhe.contains(id) && rs.getInt("KhaDung") == 1) {
                                chosen.add(new ChonGheKhach.GheSanh(
                                        id, rs.getString("HangGhe").trim(), rs.getInt("SoGhe"),
                                        rs.getString("ViTri"), rs.getString("TenLoaiGhe"),
                                        rs.getBigDecimal("GiaVe"), true));
                            }
                        }
                    }
                }
                if (chosen.size() != maGhe.size()) {
                    throw new IllegalArgumentException("Có ghế không còn khả dụng. Vui lòng cập nhật lựa chọn.");
                }
                try (PreparedStatement del = c.prepareStatement(
                        "DELETE FROM CHONGHE_KHACH WHERE MaGiaoDich IS NULL AND ThoiGianHetHan<=NOW()")) {
                    del.executeUpdate();
                }
                try (PreparedStatement ins = c.prepareStatement("""
                        INSERT INTO CHONGHE_KHACH
                        (MaChonGhe, MaSuat, MaPhim, MaPhong, ThoiGianBatDau, ThoiGianHetHan, ThoiGianTao)
                        VALUES (?, ?, ?, ?, ?, LEAST(DATE_ADD(NOW(), INTERVAL 30 MINUTE), ?), NOW())
                        """)) {
                    Timestamp start = Timestamp.valueOf(slot.suat().thoiGianBatDau());
                    ins.setString(1, code);
                    ins.setInt(2, maSuat);
                    ins.setString(3, slot.suat().maPhim());
                    ins.setString(4, slot.suat().maPhong());
                    ins.setTimestamp(5, start);
                    ins.setTimestamp(6, start);
                    ins.executeUpdate();
                }
                try (PreparedStatement seatIns = c.prepareStatement("""
                        INSERT INTO CHONGHE_KHACH_GHE (MaChonGhe, MaGhe, ViTriGhe, TenLoaiGhe, GiaTamTinh)
                        VALUES (?, ?, ?, ?, ?)
                        """)) {
                    for (ChonGheKhach.GheSanh s : chosen) {
                        seatIns.setString(1, code);
                        seatIns.setInt(2, s.maGhe());
                        seatIns.setString(3, s.viTri());
                        seatIns.setString(4, s.tenLoaiGhe());
                        seatIns.setBigDecimal(5, s.giaTamTinh());
                        seatIns.addBatch();
                    }
                    seatIns.executeBatch();
                }
                c.commit();
                return code;
            } catch (SQLException e) {
                PhongChieuDAO.rollback(c, e);
                throw translateMissingSchema(e);
            } catch (RuntimeException e) {
                PhongChieuDAO.rollback(c, e);
                throw e;
            }
        }
    }

    public Optional<ChonGheKhach> traCuuMa(String maChonGhe) throws SQLException {
        String sql = """
                SELECT p.MaPhim, p.TenPhim, p.TrangThai AS FilmState,
                       r.TenPhong, r.TrangThai AS RoomState,
                       w.MaSuat, w.MaPhong, w.ThoiGianBatDau, w.ThoiGianHetHan, w.MaGiaoDich,
                       s.MaPhim AS LiveFilm, s.MaPhong AS LiveRoom, s.ThoiGianBatDau AS LiveStart,
                       s.TrangThai AS LiveShowState, gd.TrangThai AS OrderState,
                       (SELECT MAX(h.ThoiGianHetHanGiu) FROM GHE_SUATCHIEU h
                        WHERE h.MaGiaoDich=w.MaGiaoDich AND h.TrangThai='Đang giữ') AS HoldEnd,
                       NOW() AS DbNow
                FROM CHONGHE_KHACH w
                JOIN PHIM p ON p.MaPhim=w.MaPhim
                JOIN PHONGCHIEU r ON r.MaPhong=w.MaPhong
                LEFT JOIN SUATCHIEU s ON s.MaSuat=w.MaSuat
                LEFT JOIN GIAODICH gd ON gd.MaGiaoDich=w.MaGiaoDich
                WHERE w.MaChonGhe=?
                  AND (w.ThoiGianHetHan>NOW() OR gd.TrangThai IN ('Chờ thanh toán','Đang thanh toán','Đã thanh toán'))
                """;
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maChonGhe);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                LocalDateTime now = rs.getTimestamp("DbNow").toLocalDateTime();
                LocalDateTime start = rs.getTimestamp("ThoiGianBatDau").toLocalDateTime();
                LocalDateTime expires = rs.getTimestamp("ThoiGianHetHan").toLocalDateTime();
                String orderState = rs.getString("OrderState");
                Timestamp holdTs = rs.getTimestamp("HoldEnd");
                LocalDateTime holdEnd = holdTs == null ? null : holdTs.toLocalDateTime();
                boolean paid = "Đã thanh toán".equals(orderState);
                boolean claimed = paid || "Đang thanh toán".equals(orderState)
                        || ("Chờ thanh toán".equals(orderState) && holdEnd != null && holdEnd.isAfter(now));
                if (!claimed && !expires.isAfter(now)) return Optional.empty();

                Timestamp liveStartTs = rs.getTimestamp("LiveStart");
                LocalDateTime liveStart = liveStartTs == null ? null : liveStartTs.toLocalDateTime();
                String liveState = rs.getString("LiveShowState");
                boolean showChanged = !paid && (
                        liveState == null
                        || "Đã hủy".equals(liveState)
                        || !Objects.equals(rs.getString("MaPhim"), rs.getString("LiveFilm"))
                        || !Objects.equals(rs.getString("MaPhong"), rs.getString("LiveRoom"))
                        || !Objects.equals(start, liveStart)
                        || !"Đang hoạt động".equals(rs.getString("RoomState"))
                        || !Set.of("Sắp chiếu", "Đang chiếu").contains(rs.getString("FilmState")));

                List<ChonGheKhach.GheChon> seats = new ArrayList<>();
                BigDecimal total = BigDecimal.ZERO;
                try (PreparedStatement sps = c.prepareStatement(
                        "SELECT MaGhe, ViTriGhe, TenLoaiGhe, GiaTamTinh FROM CHONGHE_KHACH_GHE WHERE MaChonGhe=? ORDER BY MaGhe")) {
                    sps.setString(1, maChonGhe);
                    try (ResultSet srs = sps.executeQuery()) {
                        while (srs.next()) {
                            BigDecimal price = srs.getBigDecimal("GiaTamTinh");
                            total = total.add(price);
                            seats.add(new ChonGheKhach.GheChon(
                                    srs.getInt("MaGhe"),
                                    srs.getString("ViTriGhe"),
                                    srs.getString("TenLoaiGhe"),
                                    price));
                        }
                    }
                }
                return Optional.of(new ChonGheKhach(
                        maChonGhe, rs.getInt("MaSuat"), rs.getString("MaPhim"), rs.getString("TenPhim"),
                        rs.getString("MaPhong"), rs.getString("TenPhong"), start, expires,
                        rs.getString("MaGiaoDich"), seats, total, claimed, paid, showChanged));
            }
        } catch (SQLException e) {
            throw translateMissingSchema(e);
        }
    }

    public List<ChonGheKhach.YeuCauCho> danhSachChoTiepNhan() throws SQLException {
        String sql = """
                SELECT w.MaChonGhe, w.MaSuat, p.TenPhim, r.TenPhong, w.ThoiGianBatDau,
                       (SELECT GROUP_CONCAT(cg.ViTriGhe ORDER BY cg.MaGhe SEPARATOR ', ')
                        FROM CHONGHE_KHACH_GHE cg WHERE cg.MaChonGhe=w.MaChonGhe) AS Seats
                FROM CHONGHE_KHACH w
                JOIN SUATCHIEU s ON s.MaSuat=w.MaSuat AND s.MaPhim=w.MaPhim
                    AND s.MaPhong=w.MaPhong AND s.ThoiGianBatDau=w.ThoiGianBatDau
                JOIN PHIM p ON p.MaPhim=s.MaPhim
                JOIN PHONGCHIEU r ON r.MaPhong=s.MaPhong
                WHERE (w.MaGiaoDich IS NULL
                       OR EXISTS (SELECT 1 FROM GIAODICH gd WHERE gd.MaGiaoDich=w.MaGiaoDich
                                  AND gd.TrangThai IN ('Hết hạn','Đã hủy'))
                       OR (EXISTS (SELECT 1 FROM GIAODICH gd WHERE gd.MaGiaoDich=w.MaGiaoDich AND gd.TrangThai='Chờ thanh toán')
                           AND NOT EXISTS (SELECT 1 FROM GHE_SUATCHIEU h WHERE h.MaGiaoDich=w.MaGiaoDich
                                           AND (h.TrangThai<>'Đang giữ' OR h.ThoiGianHetHanGiu>NOW()))))
                  AND w.ThoiGianHetHan>NOW()
                  AND s.TrangThai<>'Đã hủy' AND s.ThoiGianBatDau>NOW()
                  AND p.TrangThai IN ('Sắp chiếu','Đang chiếu')
                  AND r.TrangThai='Đang hoạt động'
                ORDER BY w.ThoiGianTao DESC, w.MaChonGhe DESC
                LIMIT 15
                """;
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<ChonGheKhach.YeuCauCho> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new ChonGheKhach.YeuCauCho(
                        rs.getString("MaChonGhe"),
                        rs.getInt("MaSuat"),
                        rs.getString("TenPhim"),
                        rs.getString("TenPhong"),
                        rs.getTimestamp("ThoiGianBatDau").toLocalDateTime(),
                        Objects.toString(rs.getString("Seats"), "")));
            }
            return result;
        } catch (SQLException e) {
            throw translateMissingSchema(e);
        }
    }

    public ChonGheKhach.KetQuaTiepNhan tiepNhanMa(
            String maChonGhe, String maNV, String maGiaoDichHienTai, int holdMinutes) throws SQLException {
        BanVeDAO banVeDao = new BanVeDAO();
        int maSuat;
        String maPhim;
        String maPhong;
        LocalDateTime showStart;
        LocalDateTime expiresAt;
        String existingOrder;
        LocalDateTime now;
        List<ChonGheKhach.GheChon> requested = new ArrayList<>();

        try (Connection c = Database.connect()) {
            PhongChieuDAO.begin(c);
            try {
                c.prepareStatement("""
                        UPDATE CHONGHE_KHACH SET MaGiaoDich=NULL
                        WHERE MaGiaoDich IN (SELECT MaGiaoDich FROM GIAODICH WHERE TrangThai IN ('Hết hạn','Đã hủy'))
                        """).executeUpdate();

                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT *, NOW() AS DbNow FROM CHONGHE_KHACH WHERE MaChonGhe=? FOR UPDATE")) {
                    ps.setString(1, maChonGhe);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Mã đặt vé không tồn tại hoặc đã hết hạn");
                        maSuat = rs.getInt("MaSuat");
                        maPhim = rs.getString("MaPhim");
                        maPhong = rs.getString("MaPhong");
                        showStart = rs.getTimestamp("ThoiGianBatDau").toLocalDateTime();
                        expiresAt = rs.getTimestamp("ThoiGianHetHan").toLocalDateTime();
                        existingOrder = rs.getString("MaGiaoDich");
                        now = rs.getTimestamp("DbNow").toLocalDateTime();
                    }
                }

                if (existingOrder != null) {
                    c.commit();
                } else {
                    if (!expiresAt.isAfter(now)) {
                        throw new IllegalArgumentException("Mã đặt vé đã hết hạn");
                    }
                    ensureNoActiveCurrentOrder(c, maGiaoDichHienTai, null, maNV);
                    try (PreparedStatement ps = c.prepareStatement(
                            "SELECT MaPhim, MaPhong, ThoiGianBatDau, TrangThai FROM SUATCHIEU WHERE MaSuat=?")) {
                        ps.setInt(1, maSuat);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next() || "Đã hủy".equals(rs.getString("TrangThai"))) {
                                throw new IllegalArgumentException("Suất chiếu không còn được phép bán vé");
                            }
                            if (!Objects.equals(maPhim, rs.getString("MaPhim"))
                                    || !Objects.equals(maPhong, rs.getString("MaPhong"))
                                    || !Objects.equals(showStart, rs.getTimestamp("ThoiGianBatDau").toLocalDateTime())) {
                                throw new IllegalArgumentException("Lịch chiếu đã thay đổi. Vui lòng chọn suất chiếu lại");
                            }
                        }
                    }
                    try (PreparedStatement ps = c.prepareStatement(
                            "SELECT MaGhe, ViTriGhe, TenLoaiGhe, GiaTamTinh FROM CHONGHE_KHACH_GHE WHERE MaChonGhe=? ORDER BY MaGhe")) {
                        ps.setString(1, maChonGhe);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                requested.add(new ChonGheKhach.GheChon(
                                        rs.getInt("MaGhe"),
                                        rs.getString("ViTriGhe"),
                                        rs.getString("TenLoaiGhe"),
                                        rs.getBigDecimal("GiaTamTinh")));
                            }
                        }
                    }
                    if (requested.isEmpty()) throw new IllegalArgumentException("Mã đặt vé không còn ghế hợp lệ");
                    c.commit();
                }
            } catch (SQLException e) {
                PhongChieuDAO.rollback(c, e);
                throw translateMissingSchema(e);
            } catch (RuntimeException e) {
                PhongChieuDAO.rollback(c, e);
                throw e;
            }
        }

        if (existingOrder != null) {
            banVeDao.map(maSuat);
            BanVeDAO.GioHang cart = banVeDao.inspect(existingOrder);
            if (!cart.giaoDich().maNV().equalsIgnoreCase(maNV)) {
                throw new IllegalArgumentException("Mã đặt vé đã được nhân viên khác tiếp nhận");
            }
            String state = cart.giaoDich().trangThai();
            if ("Hết hạn".equals(state) || "Đã hủy".equals(state)) {
                try (Connection c = Database.connect();
                     PreparedStatement ps = c.prepareStatement(
                             "UPDATE CHONGHE_KHACH SET MaGiaoDich=NULL WHERE MaChonGhe=? AND MaGiaoDich=?")) {
                    ps.setString(1, maChonGhe);
                    ps.setString(2, existingOrder);
                    ps.executeUpdate();
                }
                return tiepNhanMa(maChonGhe, maNV, maGiaoDichHienTai, holdMinutes);
            }
            if (!Set.of("Chờ thanh toán", "Đang thanh toán", "Đã thanh toán").contains(state)) {
                throw new IllegalArgumentException("Giao dịch của mã đặt vé đã kết thúc");
            }
            if (!"Đã thanh toán".equals(state)) {
                try (Connection c = Database.connect()) {
                    ensureNoActiveCurrentOrder(c, maGiaoDichHienTai, existingOrder, maNV);
                }
            }
            return new ChonGheKhach.KetQuaTiepNhan(cart, maSuat, "Đã thanh toán".equals(state));
        }

        Set<Integer> seatIds = new LinkedHashSet<>();
        for (ChonGheKhach.GheChon s : requested) seatIds.add(s.maGhe());
        String newOrderId = BanVeBUS.newId();
        BanVeDAO.GioHang held = banVeDao.hold(newOrderId, maNV, maSuat, seatIds, holdMinutes);

        Map<Integer, GheSuatChieu> heldById = new HashMap<>();
        for (GheSuatChieu gsc : held.ghe()) heldById.put(gsc.ghe().maGhe(), gsc);
        for (ChonGheKhach.GheChon req : requested) {
            GheSuatChieu live = heldById.get(req.maGhe());
            if (live == null
                    || !req.viTri().equals(live.ghe().hangGhe().trim() + live.ghe().soGhe())
                    || !req.tenLoaiGhe().equals(live.ghe().loaiGhe().tenLoaiGhe())) {
                banVeDao.cancel(newOrderId, maNV, false);
                throw new IllegalArgumentException("Thông tin ghế đã thay đổi. Vui lòng chọn ghế lại");
            }
        }

        BanVeDAO.GioHang quoted = banVeDao.quote(newOrderId, maNV);
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE CHONGHE_KHACH SET MaGiaoDich=? WHERE MaChonGhe=? AND MaGiaoDich IS NULL")) {
            ps.setString(1, newOrderId);
            ps.setString(2, maChonGhe);
            if (ps.executeUpdate() != 1) {
                banVeDao.cancel(newOrderId, maNV, false);
                throw new IllegalArgumentException("Mã đặt vé đã được nhân viên khác tiếp nhận");
            }
        }
        return new ChonGheKhach.KetQuaTiepNhan(quoted, maSuat, false);
    }

    public static void releaseByTransaction(Connection c, String maGiaoDich) throws SQLException {
        if (maGiaoDich == null) return;
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE CHONGHE_KHACH SET MaGiaoDich=NULL WHERE MaGiaoDich=?")) {
            ps.setString(1, maGiaoDich);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() != 1146) throw e;
        }
    }

    private static void ensureNoActiveCurrentOrder(
            Connection c, String current, String incoming, String employee) throws SQLException {
        if (current == null || current.isBlank() || Objects.equals(current, incoming)) return;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM GIAODICH WHERE MaGiaoDich=? AND MaNV=? AND TrangThai IN ('Chờ thanh toán','Đang thanh toán')")) {
            ps.setString(1, current.trim());
            ps.setString(2, employee);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    throw new IllegalArgumentException("Vui lòng xử lý giao dịch đang mở tại quầy trước");
                }
            }
        }
    }

    private static SQLException translateMissingSchema(SQLException e) {
        if (e.getErrorCode() == 1146) {
            throw new IllegalArgumentException(
                    "CSDL chưa có bảng chọn ghế khách. Hãy chạy thêm Database/012_chon_ghe_khach.sql", e);
        }
        return e;
    }

    private static ChonGheKhach.PhimChieu readFilm(ResultSet rs) throws SQLException {
        Phim phim = new Phim(
                rs.getString("MaPhim"),
                rs.getString("TenPhim"),
                rs.getInt("ThoiLuong"),
                rs.getString("DaoDien"),
                rs.getDate("NgayKhoiChieu").toLocalDate(),
                rs.getInt("GioiHanTuoi"),
                rs.getString("Poster"),
                rs.getString("TrangThai"),
                rs.getString("DinhDangChieu"),
                List.of());
        return new ChonGheKhach.PhimChieu(phim, Objects.toString(rs.getString("TheLoai"), ""));
    }

    private static ChonGheKhach.SuatKhach readShow(ResultSet rs) throws SQLException {
        SuatChieu suat = new SuatChieu(
                rs.getInt("MaSuat"),
                rs.getString("MaPhim"),
                rs.getString("MaPhong"),
                rs.getInt("MaLoaiThoiDiem"),
                rs.getTimestamp("ThoiGianBatDau").toLocalDateTime(),
                rs.getTimestamp("ThoiGianTraPhong").toLocalDateTime(),
                rs.getString("TrangThai"));
        Phim phim = new Phim(
                rs.getString("MaPhim"),
                rs.getString("TenPhim"),
                rs.getInt("ThoiLuong"),
                rs.getString("DaoDien"),
                rs.getDate("NgayKhoiChieu").toLocalDate(),
                rs.getInt("GioiHanTuoi"),
                rs.getString("Poster"),
                rs.getString("TrangThaiPhim"),
                rs.getString("DinhDangChieu"),
                List.of());
        return new ChonGheKhach.SuatKhach(suat, phim, rs.getString("TenPhong"));
    }
}
