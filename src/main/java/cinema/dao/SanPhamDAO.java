package cinema.dao;

import cinema.db.Database;
import cinema.model.SanPham;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SanPhamDAO {
    public List<SanPham> search(String keyword,Boolean inStock) throws SQLException {
        String pattern="%"+keyword.replace("=","==").replace("%","=%").replace("_","=_")+"%";
        try(Connection c=Database.connect();PreparedStatement ps=c.prepareStatement(
                "SELECT * FROM SANPHAM WHERE (MaSP LIKE ? ESCAPE '=' OR TenSP LIKE ? ESCAPE '=')"
                        +" AND (? IS NULL OR (?=TRUE AND SoLuongTon>0) OR (?=FALSE AND SoLuongTon=0)) ORDER BY MaSP")) {
            ps.setString(1,pattern); ps.setString(2,pattern);
            ps.setObject(3,inStock,Types.BOOLEAN); ps.setObject(4,inStock,Types.BOOLEAN); ps.setObject(5,inStock,Types.BOOLEAN);
            try(ResultSet rs=ps.executeQuery()) {
                List<SanPham> result=new ArrayList<>(); while(rs.next()) result.add(read(rs)); return result;
            }
        }
    }
    public SanPham find(String id) throws SQLException {
        try(Connection c=Database.connect();PreparedStatement ps=c.prepareStatement("SELECT * FROM SANPHAM WHERE MaSP=?")) {
            ps.setString(1,id);
            try(ResultSet rs=ps.executeQuery()) {
                if(!rs.next()) throw new IllegalArgumentException("Không tìm thấy sản phẩm"); return read(rs);
            }
        }
    }
    public void insert(SanPham product) throws SQLException {
        try(Connection c=Database.connect();PreparedStatement ps=c.prepareStatement("INSERT INTO SANPHAM (MaSP,TenSP,MoTa,DonViTinh,GiaBan,SoLuongTon,TinhTrangKinhDoanh) VALUES (?,?,?,?,?,?,?)")) {
            ps.setString(1,product.maSP()); ps.setString(2,product.tenSP()); ps.setString(3,product.moTa());
            ps.setString(4,product.donViTinh()); ps.setBigDecimal(5,product.giaBan()); ps.setInt(6,product.soLuongTon()); ps.setString(7,product.tinhTrangKinhDoanh());
            try { ps.executeUpdate(); }
            catch(SQLException e) {
                if(e.getErrorCode()==1062) throw new IllegalArgumentException("Mã sản phẩm đã tồn tại. Vui lòng nhập mã khác",e); throw e;
            }
        }
    }
    public void updateInfo(SanPham product) throws SQLException {
        try(Connection c=Database.connect();PreparedStatement ps=c.prepareStatement(
                "UPDATE SANPHAM SET TenSP=?,MoTa=?,DonViTinh=?,SoLuongTon=?,TinhTrangKinhDoanh=? WHERE MaSP=?")) {
            ps.setString(1,product.tenSP()); ps.setString(2,product.moTa()); ps.setString(3,product.donViTinh());
            ps.setInt(4,product.soLuongTon()); ps.setString(5,product.tinhTrangKinhDoanh()); ps.setString(6,product.maSP());
            if(ps.executeUpdate()!=1) throw new IllegalArgumentException("Không tìm thấy sản phẩm");
        }
    }
    public void updatePrice(String id,BigDecimal price) throws SQLException {
        try(Connection c=Database.connect();PreparedStatement ps=c.prepareStatement("UPDATE SANPHAM SET GiaBan=? WHERE MaSP=?")) {
            ps.setBigDecimal(1,price); ps.setString(2,id);
            if(ps.executeUpdate()!=1) throw new IllegalArgumentException("Không tìm thấy sản phẩm");
        }
    }
    private static SanPham read(ResultSet rs) throws SQLException {
        return new SanPham(rs.getString("MaSP"),rs.getString("TenSP"),rs.getString("MoTa"),rs.getString("DonViTinh"),
                rs.getBigDecimal("GiaBan"),rs.getInt("SoLuongTon"),rs.getString("TinhTrangKinhDoanh"));
    }
}
