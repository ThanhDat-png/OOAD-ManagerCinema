package cinema.model;

public record TheLoai(int maTheLoai, String tenTheLoai) {
    @Override public String toString() { return tenTheLoai; }
}
