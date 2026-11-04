package cinema.model;
public record LoaiGhe(int maLoaiGhe,String tenLoaiGhe) {
    @Override public String toString() { return tenLoaiGhe; }
}
