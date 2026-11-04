package cinema.controller.staff;

import cinema.controller.auth.PhienDangNhap;
import cinema.dao.NhanVienDAO;
import cinema.model.NhanVien;
import cinema.model.VaiTro;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public final class NhanVienBUS {
    private final PhienDangNhap session;
    private final NhanVienDAO dao;

    public NhanVienBUS(PhienDangNhap session, NhanVienDAO dao) {
        if (session == null) throw new IllegalArgumentException("Vui lòng đăng nhập");
        this.session = session;
        this.dao = dao;
    }

    private void requireAdmin() {
        if (session.vaiTro() != VaiTro.ADMIN) throw new IllegalArgumentException("Bạn không có quyền sử dụng chức năng này");
    }

    public List<NhanVien> search(String keyword) throws SQLException {
        requireAdmin();
        return dao.search(keyword == null ? "" : keyword.trim());
    }

    public NhanVien detail(String id) throws SQLException {
        if (session.vaiTro() != VaiTro.ADMIN && (session.nhanVien() == null
                || !session.nhanVien().maNV().equals(id))) {
            throw new IllegalArgumentException("Bạn không có quyền xem thông tin này");
        }
        return dao.find(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin nhân viên"));
    }

    public NhanVien create(NhanVien input) throws SQLException {
        requireAdmin();
        String id = "NV" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(java.util.Locale.ROOT);
        NhanVien employee = validate(input, id);
        dao.insert(employee);
        return employee;
    }

    public void update(NhanVien input) throws SQLException {
        requireAdmin();
        if (input == null || input.maNV() == null || input.maNV().isBlank() || input.maNV().length() > 10)
            throw new IllegalArgumentException("Mã nhân viên không hợp lệ");
        dao.update(validate(input, input.maNV()));
    }

    public void delete(String id) throws SQLException {
        requireAdmin();
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Vui lòng chọn nhân viên");
        dao.delete(id);
    }

    private NhanVien validate(NhanVien input, String id) {
        if (input == null) throw new IllegalArgumentException("Vui lòng nhập thông tin nhân viên");
        String name = required(input.hoTen(), "Họ tên", 100);
        if (input.ngaySinh() == null || input.ngaySinh().isAfter(LocalDate.now(ZoneId.of("Asia/Bangkok"))))
            throw new IllegalArgumentException("Ngày sinh cần hợp lệ và không ở tương lai");
        String phone = required(input.soDienThoai(), "Số điện thoại", 15);
        if (!phone.matches("\\+?[0-9]{8,15}")) throw new IllegalArgumentException("Số điện thoại không hợp lệ");
        String email = required(input.email(), "Email", 100);
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new IllegalArgumentException("Email không hợp lệ");
        return new NhanVien(id, name, input.ngaySinh(), phone, email,
                required(input.diaChi(), "Địa chỉ", 200), required(input.chucVu(), "Chức vụ", 50));
    }

    private String required(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Vui lòng nhập " + field.toLowerCase());
        String result = value.trim();
        if (result.length() > maxLength) throw new IllegalArgumentException(field + " tối đa " + maxLength + " ký tự");
        return result;
    }
}
