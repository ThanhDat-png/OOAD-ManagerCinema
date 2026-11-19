package cinema.web;

import java.io.IOException;
import java.nio.file.Files;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WebLoginTest {
    @TempDir static Path temp;
    @DynamicPropertySource
    static void db(DynamicPropertyRegistry registry) {
        registry.add("CINEMA_SQLITE_PATH", () -> temp.resolve("test.db").toString());
        registry.add("TURSO_DATABASE_URL", () -> "");
        registry.add("TURSO_AUTH_TOKEN", () -> "");
        registry.add("CINEMA_DEMO_DATA", () -> "true");
    }
    @Autowired MockMvc browser;
    @Test void roleIsolationAndCsrf() throws Exception {
        browser.perform(get("/login")).andExpect(status().isOk());
        browser.perform(get("/admin/employees")).andExpect(status().is3xxRedirection());
        browser.perform(post("/login").param("username","staff_demo").param("password","Cinema@123"))
                .andExpect(status().isForbidden());
        var staff = login("staff_demo", "/staff/sales");
        assertEquals("NV002",staff.getAttribute("employeeId"));
        browser.perform(get("/admin/employees").session(staff)).andExpect(status().isForbidden());
        browser.perform(post("/logout").session(staff).with(csrf())).andExpect(redirectedUrl("/login"));
        assertTrue(staff.isInvalid());
        var admin = login("admin_demo", "/admin/showtimes");
        browser.perform(get("/staff/sales").session(admin)).andExpect(status().isForbidden());
    }
    @Test void rejectWrongPassword() throws Exception {
        var result=browser.perform(post("/login").with(csrf()).param("username","staff_demo").param("password","incorrect"))
                .andExpect(redirectedUrl("/login?error")).andReturn();
        assertNull(result.getRequest().getSession().getAttribute("employeeId"));
    }
    private MockHttpSession login(String name,String landing) throws Exception {
        var response=browser.perform(post("/login").with(csrf()).param("username",name).param("password","Cinema@123"))
                .andExpect(redirectedUrl(landing)).andReturn();
        return (MockHttpSession)response.getRequest().getSession();
    }
}
