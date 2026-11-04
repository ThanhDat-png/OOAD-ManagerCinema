package cinema.controller.film;

import cinema.controller.auth.PhienDangNhap;
import cinema.dao.PhimDAO;
import cinema.dao.TheLoaiDAO;
import cinema.model.*;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;

public final class PhimBUS {
    public static final List<String> TRANG_THAI = List.of("Sắp chiếu", "Đang chiếu", "Ngừng chiếu");
    private final PhienDangNhap session;
    private final PhimDAO dao;
    private final TheLoaiDAO genres;
    private final PosterStore posters;

    public PhimBUS(PhienDangNhap session, PhimDAO dao, TheLoaiDAO genres, PosterStore posters) {
        if (session == null || session.vaiTro() == null) throw new IllegalArgumentException("Vui lòng đăng nhập");
        this.session = session; this.dao = dao; this.genres = genres; this.posters = posters;
    }

    private void requireAdmin() {
        if (session.vaiTro() != VaiTro.ADMIN) throw new IllegalArgumentException("Bạn không có quyền sử dụng chức năng này");
    }

    public List<TheLoai> genres() throws SQLException { return genres.all(); }

    public List<Phim> search(String name, Integer genre, String status) throws SQLException {
        if (status != null && !TRANG_THAI.contains(status)) throw new IllegalArgumentException("Trạng thái phim không hợp lệ");
        return dao.search(name == null ? "" : name.trim(), genre, status);
    }

    public Phim detail(String id) throws SQLException {
        return dao.find(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim"));
    }

    public Phim create(Phim input, Path image) throws SQLException, IOException {
        requireAdmin();
        if (input == null) throw new IllegalArgumentException("Vui lòng nhập thông tin phim");
        String name = name(input.tenPhim());
        if (input.thoiLuong() <= 0) throw new IllegalArgumentException("Thời lượng phải lớn hơn 0 phút");
        String director = required(input.daoDien(), "đạo diễn", 100);
        if (input.ngayKhoiChieu() == null) throw new IllegalArgumentException("Ngày khởi chiếu không hợp lệ");
        if (input.gioiHanTuoi() < 0) throw new IllegalArgumentException("Giới hạn tuổi không được âm");
        if (!TRANG_THAI.contains(input.trangThai())) throw new IllegalArgumentException("Trạng thái phim không hợp lệ");
        String format = required(input.dinhDangChieu(), "định dạng chiếu", 20);
        List<TheLoai> selected = validateGenres(input.theLoai());
        String poster = posters.importImage(image);
        Phim film = new Phim("P" + UUID.randomUUID().toString().replace("-", "").substring(0, 9).toUpperCase(Locale.ROOT),
                name, input.thoiLuong(), director, input.ngayKhoiChieu(), input.gioiHanTuoi(),
                poster, input.trangThai(), format, selected);
        try { dao.insert(film); return film; }
        catch (SQLException | RuntimeException e) { cleanup(poster, e); throw e; }
    }

    public void rename(String id, String name) throws SQLException {
        requireAdmin(); dao.rename(id, name(name));
    }

    public void updatePoster(String id, Path image) throws SQLException, IOException {
        requireAdmin(); detail(id);
        String poster = posters.importImage(image);
        try { dao.updatePoster(id, poster); }
        catch (SQLException | RuntimeException e) { cleanup(poster, e); throw e; }
    }

    public void updateGenres(String id, List<TheLoai> selected) throws SQLException {
        requireAdmin(); dao.updateGenres(id, validateGenres(selected));
    }

    public void stop(String id) throws SQLException { requireAdmin(); dao.stop(id); }

    private String name(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Vui lòng nhập tên phim");
        return required(value, "tên phim", 200);
    }

    private String required(String value, String label, int max) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Vui lòng nhập " + label);
        String text = value.trim();
        if (text.length() > max) throw new IllegalArgumentException("Thông tin " + label + " tối đa " + max + " ký tự");
        return text;
    }

    private List<TheLoai> validateGenres(List<TheLoai> selected) {
        if (selected == null || selected.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn thể loại phim");
        Set<Integer> ids = new HashSet<>();
        for (TheLoai genre : selected)
            if (genre == null || genre.maTheLoai() <= 0 || !ids.add(genre.maTheLoai()))
                throw new IllegalArgumentException("Thể loại phim không hợp lệ hoặc bị trùng");
        return List.copyOf(selected);
    }

    private void cleanup(String poster, Exception error) {
        try { posters.discard(poster); } catch (IOException | RuntimeException e) { error.addSuppressed(e); }
    }
}
