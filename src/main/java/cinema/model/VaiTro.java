package cinema.model;

public enum VaiTro {
    ADMIN("Admin"), NHAN_VIEN("Nhân viên");

    private final String ten;
    VaiTro(String ten) { this.ten = ten; }
    public String getTen() { return ten; }

    public static VaiTro fromDatabase(String ten) {
        for (VaiTro vaiTro : values()) {
            if (vaiTro.ten.equals(ten)) return vaiTro;
        }
        throw new IllegalArgumentException("Vai trò không được hỗ trợ");
    }
}
