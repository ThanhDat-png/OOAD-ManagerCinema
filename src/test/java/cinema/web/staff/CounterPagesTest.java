package cinema.web.staff;

import cinema.web.db.WebDb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CounterPagesTest {
    @TempDir static Path files;
    @DynamicPropertySource static void db(DynamicPropertyRegistry properties) {
        properties.add("CINEMA_SQLITE_PATH",()->files.resolve("counter-pages.sqlite").toString());
        properties.add("CINEMA_DEMO_DATA",()->"true");
    }
    @Autowired MockMvc mvc;
    @Autowired WebDb db;
    @Autowired CounterSales sales;

    @Test void switchingShowsReleasesHeldSeatsAndExpiredCartsOfferRestart() throws Exception {
        var session=new MockHttpSession();
        session.setAttribute("employeeId","NV002");
        session.setAttribute("saleId","Gswitch");
        var staff=user("staff_demo").roles("STAFF");
        int show=CounterSales.number(sales.shows("","","").get(0),"MaSuat");
        int seat=CounterSales.number(sales.seats(show).stream()
                .filter(row->"Trống".equals(row.get("TrangThai"))&&"Sẵn sàng sử dụng".equals(row.get("TinhTrang")))
                .findFirst().orElseThrow(),"MaGhe");
        sales.hold("Gswitch","NV002",show,List.of(seat));
        mvc.perform(get("/staff/sales").session(session).with(staff)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Hủy giữ ghế và đổi suất")));
        mvc.perform(post("/staff/sales/cancel").param("id","Gswitch").session(session).with(staff).with(csrf()))
                .andExpect(redirectedUrl("/staff/sales"));
        assertEquals(List.of(),sales.selected("Gswitch"));

        session.setAttribute("saleId","Gexpired");
        sales.hold("Gexpired","NV002",show,List.of(seat));
        db.update("UPDATE GHE_SUATCHIEU SET ThoiGianHetHanGiu='2000-01-01 00:00:00' WHERE MaGiaoDich='Gexpired'");
        mvc.perform(get("/staff/sales").session(session).with(staff)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Giao dịch mới")));
        assertEquals("Hết hạn",sales.order("Gexpired").get("TrangThai"));
        assertEquals(List.of(),sales.selected("Gexpired"));
    }

    @Test void counterCanSellThenRecoverReceiptAndScanTicket() throws Exception {
        var session=new MockHttpSession();
        session.setAttribute("employeeId","NV002");
        session.setAttribute("name","Nhân viên");
        session.setAttribute("role","STAFF");
        var staff=user("staff_demo").roles("STAFF");
        var slot=sales.shows("","","").get(0);
        String show=slot.get("MaSuat").toString();
        String seat=sales.seats(Integer.parseInt(show)).get(0).get("MaGhe").toString();
        mvc.perform(get("/staff/sales").session(session).with(staff)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Tìm suất chiếu")));
        mvc.perform(get("/staff/sales").param("show",show).session(session).with(staff)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Màn hình")));
        String id=(String)session.getAttribute("saleId");
        for(String action:new String[]{"hold","quote","payment","complete"}) {
            mvc.perform(post("/staff/sales/"+action).session(session).with(staff).with(csrf())
                    .param("id",id).param("show",show).param("seats",seat).param("method","Tiền mặt").param("confirmed","true"))
                    .andExpect(status().is3xxRedirection());
            mvc.perform(get(action.equals("complete")?"/staff/transactions/"+id:"/staff/sales").session(session).with(staff))
                    .andExpect(status().isOk());
        }
        String ticket=db.query("SELECT MaVe FROM VE WHERE MaGiaoDich=?",id).get(0).get("MaVe").toString();
        mvc.perform(get("/staff/transactions").param("code",ticket).session(session).with(staff)).andExpect(content().string(containsString(id)));
        mvc.perform(get("/staff/admission").param("code",ticket).session(session).with(staff)).andExpect(content().string(containsString(ticket)));
        mvc.perform(get("/staff/tickets/"+ticket+"/qr").session(session).with(staff)).andExpect(content().contentType("image/png"));
    }
}
