package cinema.web.admin;

import cinema.web.db.WebDb;
import java.nio.file.*;
import java.io.IOException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminBrowserFlowsTest {
    @DynamicPropertySource static void database(DynamicPropertyRegistry props) throws IOException {
        var path=Files.createTempDirectory("cinema-admin-test-").resolve("cinema.sqlite");
        props.add("CINEMA_SQLITE_PATH",path::toString);
        props.add("CINEMA_DEMO_DATA",()->true);
    }
    @Autowired MockMvc mvc;
    @Autowired WebDb db;
    static final Map<String,Object> ADMIN=Map.of("role","ADMIN","employeeId","NV001","name","Admin");

    private ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.with(user("admin").roles("ADMIN")).sessionAttrs(ADMIN));
    }

    private MockHttpServletRequestBuilder form(String op, String id) {
        return post("/admin/form/" + op).with(csrf()).param("id", id);
    }

    private void saved(MockHttpServletRequestBuilder request) throws Exception {
        asAdmin(request).andExpect(status().is3xxRedirection());
    }

    private void rejected(MockHttpServletRequestBuilder request) throws Exception {
        asAdmin(request).andExpect(status().isOk()).andExpect(model().attributeExists("error"));
    }

    private String value(String sql, String column, Object... args) {
        return db.query(sql, args).get(0).get(column).toString();
    }

    private MockHttpServletRequestBuilder employee(String op, String id, String name, String email) {
        return form(op, id).param("HoTen", name).param("NgaySinh", "1996-05-08")
                .param("SoDienThoai", "0912345678").param("Email", email)
                .param("DiaChi", "TP.HCM").param("ChucVu", "Nhân viên bán vé");
    }

    @Test void employeesCanBeCreatedEditedSearchedAndDeleted() throws Exception {
        rejected(employee("employee-new", "", "Hồ sơ nghiệm thu", "wrong"));
        assertThat(db.query("SELECT * FROM NHANVIEN WHERE HoTen='Hồ sơ nghiệm thu'")).isEmpty();
        saved(employee("employee-new", "", "Hồ sơ nghiệm thu", "test@example.com"));
        String id = value("SELECT MaNV FROM NHANVIEN WHERE HoTen='Hồ sơ nghiệm thu'", "MaNV");
        asAdmin(get("/admin/form/employee-edit").param("id", id))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Hồ sơ nghiệm thu")));
        saved(employee("employee-edit", id, "Hồ sơ đã sửa", "updated@example.com"));
        asAdmin(get("/admin/employees").param("q", "0912345678"))
                .andExpect(content().string(containsString("Hồ sơ đã sửa")));
        asAdmin(get("/admin/employees/detail").param("id", id))
                .andExpect(content().string(containsString("updated@example.com")));
        asAdmin(get("/admin/employees").param("q", "DOES-NOT-EXIST"))
                .andExpect(content().string(containsString("Không tìm thấy nhân viên phù hợp")));
        asAdmin(get("/admin/form/employee-delete").param("id", id)).andExpect(status().isOk());
        assertThat(db.query("SELECT * FROM NHANVIEN WHERE MaNV=?", id)).hasSize(1);
        saved(form("employee-delete", id));
        asAdmin(get("/admin/employees/detail").param("id", id))
                .andExpect(redirectedUrl("/admin/employees"))
                .andExpect(flash().attribute("adminError", "Không tìm thấy thông tin nhân viên"));
        mvc.perform(get("/profile").param("id", "NV001").with(user("staff").roles("STAFF"))
                        .sessionAttrs(Map.of("role", "STAFF", "employeeId", "NV002")))
                .andExpect(status().isForbidden());
    }

    @Test void filmCreationEditsAndStopPersistAndSearch() throws Exception {
        var image = new java.awt.image.BufferedImage(4, 6, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", out);
        var poster = new MockMultipartFile("upload", "poster.png", "image/png", out.toByteArray());
        saved(multipart("/admin/form/film-new").file(poster).with(csrf())
                .param("TenPhim", "Phim nghiệm thu").param("DaoDien", "Đạo diễn nghiệm thu")
                .param("ThoiLuong", "90").param("NgayKhoiChieu", "2026-01-01").param("GioiHanTuoi", "0")
                .param("DinhDangChieu", "2D").param("TrangThai", "Đang chiếu").param("genres", "3", "5"));
        String id = value("SELECT MaPhim FROM PHIM WHERE TenPhim='Phim nghiệm thu'", "MaPhim");
        rejected(form("film-name", id).param("TenPhim", ""));
        saved(form("film-name", id).param("TenPhim", "Phim đã sửa"));
        rejected(form("film-genres", id));
        saved(form("film-genres", id).param("genres", "5"));
        assertThat(db.query("SELECT MaTheLoai FROM PHIM_THELOAI WHERE MaPhim=?", id))
                .containsExactly(Map.of("MaTheLoai", 5));
        var invalid = new MockMultipartFile("upload", "poster.png", "image/png", new byte[]{1});
        rejected(multipart("/admin/form/film-poster").file(invalid).with(csrf()).param("id", id));
        rejected(form("film-stop", "PH001"));
        saved(form("film-stop", id));
        rejected(form("film-stop", id));
        asAdmin(get("/catalog/films").param("q", "Phim đã sửa").param("genre", "5").param("status", "Ngừng chiếu"))
                .andExpect(content().string(containsString("Phim đã sửa")));
    }

    @Test void roomAndSeatEditsPersistWithDuplicateAndAvailabilityChecks() throws Exception {
        var create = form("room-new", "").param("TenPhong", "Phòng nghiệm thu").param("MaLoaiPhong", "1")
                .param("ThoiGianDonPhong", "15").param("TrangThai", "Bảo trì");
        saved(create);
        rejected(create);
        String room = value("SELECT MaPhong FROM PHONGCHIEU WHERE TenPhong='Phòng nghiệm thu'", "MaPhong");
        var activate = form("room-state", room).param("TrangThai", "Đang hoạt động");
        rejected(activate);
        var addSeat = form("seat-new", room).param("HangGhe", "Z").param("SoGhe", "2")
                .param("TrangThai", "Sẵn sàng sử dụng").param("MaLoaiGhe", "1");
        saved(addSeat);
        rejected(addSeat);
        String seat = value("SELECT MaGhe FROM GHE WHERE MaPhong=?", "MaGhe", room);
        saved(form("seat-edit", seat).param("HangGhe", "Y").param("SoGhe", "3").param("TrangThai", "Sẵn sàng sử dụng"));
        rejected(form("seat-type", seat).param("MaLoaiGhe", "999"));
        saved(form("seat-type", seat).param("MaLoaiGhe", "2"));
        assertThat(db.query("SELECT HangGhe,SoGhe,MaLoaiGhe FROM GHE WHERE MaGhe=?", seat))
                .containsExactly(Map.of("HangGhe", "Y", "SoGhe", 3, "MaLoaiGhe", 2));
        saved(activate);
        rejected(form("room-state", "PC01").param("TrangThai", "Bảo trì"));
        rejected(form("room-edit", "PC01").param("TenPhong", "Phòng 01").param("MaLoaiPhong", "2").param("ThoiGianDonPhong", "15"));
        saved(form("room-edit", room).param("TenPhong", "Phòng đã sửa").param("MaLoaiPhong", "2").param("ThoiGianDonPhong", "20"));
        asAdmin(get("/catalog/rooms").param("q", room).param("type", "2").param("status", "Đang hoạt động"))
                .andExpect(content().string(containsString("Phòng đã sửa")));
        db.write(tx -> {
            tx.update("INSERT INTO SUATCHIEU (MaPhim,MaPhong,MaLoaiThoiDiem,ThoiGianBatDau,ThoiGianTraPhong,TrangThai) VALUES ('PH001',?,1,'2020-01-01 10:00:00','2020-01-01 12:00:00','Sắp chiếu')", room);
            String pastShow = tx.query("SELECT last_insert_rowid() id").get(0).get("id").toString();
            tx.update("INSERT INTO GIAODICH VALUES ('PAST-PAY','NV002','2020-01-01 09:50:00',65000,'Đang thanh toán')");
            tx.update("INSERT INTO GHE_SUATCHIEU (MaSuat,MaGhe,MaGiaoDich,TrangThai,MaGia,GiaVeChot) VALUES (?,?,'PAST-PAY','Đang giữ',1,65000)", pastShow, seat);
            return null;
        });
        try {
            rejected(form("room-state", room).param("TrangThai", "Bảo trì"));
            rejected(form("seat-edit", seat).param("HangGhe", "Y").param("SoGhe", "3").param("TrangThai", "Bảo trì"));
        } finally {
            db.write(tx -> {
                tx.update("DELETE FROM GHE_SUATCHIEU WHERE MaGiaoDich='PAST-PAY'");
                tx.update("DELETE FROM GIAODICH WHERE MaGiaoDich='PAST-PAY'");
                tx.update("DELETE FROM SUATCHIEU WHERE MaPhong=? AND ThoiGianBatDau='2020-01-01 10:00:00'", room);
                return null;
            });
        }
    }

    @Test void ticketPriceCanBeCreatedAndEdited() throws Exception {
        db.write(tx -> {
            tx.update("INSERT INTO LOAIGHE VALUES (99,'Loại ghế nghiệm thu')");
            return null;
        });
        var create = form("price-new", "").param("MaLoaiGhe", "99").param("MaLoaiPhong", "1")
                .param("MaLoaiThoiDiem", "1").param("GiaVe", "72000");
        saved(create);
        rejected(create);
        String price = value("SELECT MaGia FROM BANGGIAVE WHERE MaLoaiGhe=99", "MaGia");
        rejected(form("price-edit", price).param("GiaVe", "0"));
        saved(form("price-edit", price).param("GiaVe", "73000"));
        assertThat(value("SELECT GiaVe FROM BANGGIAVE WHERE MaGia=?", "GiaVe", price)).isEqualTo("73000");
    }
    @Test void catalogAndReportsRenderIncludingDetailsAndForms() throws Exception {
        for(String path:List.of("/admin/showtimes","/catalog/films","/catalog/rooms","/admin/prices","/admin/products","/admin/reports","/admin/employees","/profile","/admin/form/employee-new","/admin/form/film-new","/admin/form/room-new","/admin/form/show-new","/admin/form/product-new")){
            mvc.perform(get(path).with(user("admin").roles("ADMIN")).sessionAttrs(ADMIN)).andExpect(status().isOk());
        }
        for(var item:Map.of("/catalog/films/detail","PH001","/catalog/rooms/detail","PC01","/admin/products/detail","SP001","/admin/employees/detail","NV001","/admin/showtimes/detail","1","/admin/prices/detail","1","/admin/form/film-genres","PH001","/admin/form/seat-type","1","/admin/form/price-edit","1").entrySet()){
            mvc.perform(get(item.getKey()).param("id",item.getValue()).with(user("admin").roles("ADMIN")).sessionAttrs(ADMIN)).andExpect(status().isOk());
        }
        for(String tab:List.of("time","film","employee","ticket","stock"))mvc.perform(get("/admin/reports").param("tab",tab).param("run","1").param("mode","range").param("from","2020-01-01").param("to","2040-01-01").with(user("admin").roles("ADMIN")).sessionAttrs(ADMIN)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Kết quả thống kê"))).andExpect(model().attributeExists("kpis","chartRows"));
        mvc.perform(get("/admin/employees").with(user("staff").roles("STAFF"))).andExpect(status().isForbidden());
        mvc.perform(get("/catalog/films/detail").param("id","PH001").with(user("staff").roles("STAFF")).sessionAttrs(Map.of("role","STAFF","employeeId","NV002"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Sửa tên phim"))));
    }
    @Test void productFormsKeepStockAndPriceIndependent() throws Exception {
        mvc.perform(post("/admin/form/product-new").with(user("admin").roles("ADMIN")).with(csrf()).sessionAttrs(ADMIN).param("MaSP","TEST").param("TenSP","Trà chanh").param("DonViTinh","Ly").param("SoLuongTon","10").param("GiaBan","20000").param("TinhTrangKinhDoanh","Đang kinh doanh")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/form/product-price").with(user("admin").roles("ADMIN")).with(csrf()).sessionAttrs(ADMIN).param("id","TEST").param("GiaBan","0")).andExpect(status().isOk()).andExpect(model().attributeExists("error"));
        mvc.perform(post("/admin/form/product-price").with(user("admin").roles("ADMIN")).with(csrf()).sessionAttrs(ADMIN).param("id","TEST").param("GiaBan","25000")).andExpect(status().is3xxRedirection());
        var item=db.query("SELECT GiaBan,SoLuongTon FROM SANPHAM WHERE MaSP='TEST'").get(0);
        assertThat(item.get("GiaBan").toString()).isEqualTo("25000");assertThat(item.get("SoLuongTon").toString()).isEqualTo("10");
        rejected(form("product-edit", "TEST").param("TenSP", "Trà mới").param("DonViTinh", "Ly")
                .param("SoLuongTon", "-1").param("TinhTrangKinhDoanh", "Đang kinh doanh"));
        saved(form("product-edit", "TEST").param("TenSP", "Trà mới").param("DonViTinh", "Ly")
                .param("SoLuongTon", "0").param("TinhTrangKinhDoanh", "Đang kinh doanh"));
        assertThat(value("SELECT GiaBan FROM SANPHAM WHERE MaSP='TEST'", "GiaBan")).isEqualTo("25000");
        asAdmin(get("/admin/products").param("stock", "Hết hàng").param("q", "Trà mới"))
                .andExpect(content().string(containsString("Trà mới")));
    }
    @Test void scheduleTracksSeatChangesAndConflictsRollback() throws Exception {
        String day=LocalDate.now(AdminUi.ZONE).plusYears(1).toString();
        var create=post("/admin/form/show-new").with(user("admin").roles("ADMIN")).with(csrf()).sessionAttrs(ADMIN).param("MaPhim","PH001").param("MaPhong","PC01").param("date",day).param("time","12:00");
        mvc.perform(create).andExpect(status().is3xxRedirection());
        var id=db.query("SELECT MaSuat FROM SUATCHIEU WHERE ThoiGianBatDau=?",day+" 12:00:00").get(0).get("MaSuat").toString();
        mvc.perform(create).andExpect(status().isOk()).andExpect(model().attributeExists("error"));
        rejected(form("show-date", id).param("date", "2000-01-01"));
        saved(form("show-film", id).param("MaPhim", "PH002"));
        String newDay = LocalDate.parse(day).plusDays(2).toString();
        saved(form("show-date", id).param("date", newDay));
        rejected(form("show-time", id).param("time", "invalid"));
        saved(form("show-time", id).param("time", "13:00"));
        assertThat(value("SELECT ThoiGianBatDau FROM SUATCHIEU WHERE MaSuat=?", "ThoiGianBatDau", id))
                .isEqualTo(newDay + " 13:00:00");
        assertThat(value("SELECT MaPhim FROM SUATCHIEU WHERE MaSuat=?", "MaPhim", id)).isEqualTo("PH002");
        mvc.perform(post("/admin/form/show-room").with(user("admin").roles("ADMIN")).with(csrf()).sessionAttrs(ADMIN).param("id",id).param("MaPhong","PC02")).andExpect(status().is3xxRedirection());
        assertThat(db.query("SELECT DISTINCT MaPhong FROM GHE_SUATCHIEU h JOIN GHE g ON g.MaGhe=h.MaGhe WHERE MaSuat=?",id)).containsExactly(Map.of("MaPhong","PC02"));
        mvc.perform(post("/admin/form/show-cancel").with(user("admin").roles("ADMIN")).with(csrf()).sessionAttrs(ADMIN).param("id",id)).andExpect(status().is3xxRedirection());
        assertThat(db.query("SELECT DISTINCT TrangThai FROM GHE_SUATCHIEU WHERE MaSuat=?",id)).containsExactly(Map.of("TrangThai","Không khả dụng"));
        rejected(form("show-cancel", id));
    }
    @Test void posterReloadsFromDatabaseAfterUpload() throws Exception {
        var image=new java.awt.image.BufferedImage(4,6,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",out);
        var poster=new MockMultipartFile("upload","poster.png","image/png",out.toByteArray());
        mvc.perform(multipart("/admin/form/film-poster").file(poster).param("id","PH002").with(user("admin").roles("ADMIN")).with(csrf()).sessionAttrs(ADMIN)).andExpect(status().is3xxRedirection());
        mvc.perform(get("/catalog/films/poster").param("id","PH002").with(user("staff").roles("STAFF"))).andExpect(status().isOk()).andExpect(content().bytes(out.toByteArray()));
    }

    private MockHttpServletRequestBuilder report(String tab, String from, String to) {
        return get("/admin/reports").param("tab", tab).param("run", "1")
                .param("mode", "range").param("from", from).param("to", to);
    }

    @Test void reportsHandleInvalidCriteriaZeroCountsAndPaidSnapshots() throws Exception {
        asAdmin(report("time", "2020-02-01", "2020-01-01")).andExpect(model().attributeExists("error"));
        asAdmin(report("film", "", "")).andExpect(model().attributeExists("error"));
        asAdmin(report("stock", "", "").param("product", "MISSING"))
                .andExpect(content().string(containsString("Không có sản phẩm phù hợp")));
        asAdmin(report("ticket", "2000-01-01", "2000-01-01"))
                .andExpect(model().attribute("rows", List.of(Map.of("Total", 0, "Used", 0, "Unused", 0))));
        String timestamp = "2020-01-02 12:00:00";
        String seat = value("SELECT MaGhe FROM GHE_SUATCHIEU WHERE MaSuat=1 ORDER BY MaGhe LIMIT 1", "MaGhe");
        db.write(tx -> {
            tx.update("INSERT INTO GIAODICH VALUES ('REPORT-TEST','NV002',?,88888,'Đã thanh toán')", timestamp);
            tx.update("INSERT INTO THANHTOAN (MaGiaoDich,PhuongThuc,SoTien,ThoiGianThanhToan) VALUES ('REPORT-TEST','Tiền mặt',88888,?)", timestamp);
            tx.update("UPDATE GHE_SUATCHIEU SET MaGiaoDich='REPORT-TEST',TrangThai='Đã bán',MaGia=1,GiaVeChot=88888 WHERE MaSuat=1 AND MaGhe=?", seat);
            tx.update("""
                    INSERT INTO VE VALUES ('REPORT-TICKET','REPORT-TEST',1,?,1,'REPORT-QR',88888,
                    'Chưa sử dụng',?,'A',1,'Thường')
                    """, seat, timestamp);
            return null;
        });
        try {
            rejected(form("employee-delete", "NV002"));
            String day = "2020-01-02";
            for (String tab : List.of("time", "film", "employee")) {
                var request = report(tab, day, day);
                if (tab.equals("film")) request.param("film", value("SELECT MaPhim FROM SUATCHIEU WHERE MaSuat=1", "MaPhim"));
                if (tab.equals("employee")) request.param("employee", "NV002");
                asAdmin(request).andExpect(content().string(containsString("88.888")));
            }
            asAdmin(report("ticket", day, day)).andExpect(model().attribute("rows",
                    List.of(Map.of("Total", 1, "Used", 0, "Unused", 1))));
        } finally {
            db.write(tx -> {
                tx.update("DELETE FROM VE WHERE MaVe='REPORT-TICKET'");
                tx.update("DELETE FROM THANHTOAN WHERE MaGiaoDich='REPORT-TEST'");
                tx.update("UPDATE GHE_SUATCHIEU SET MaGiaoDich=NULL,TrangThai='Trống',MaGia=NULL,GiaVeChot=NULL WHERE MaGiaoDich='REPORT-TEST'");
                tx.update("DELETE FROM GIAODICH WHERE MaGiaoDich='REPORT-TEST'");
                return null;
            });
        }
    }
}
