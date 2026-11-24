package cinema.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
class WebPages {
    @GetMapping("/")
    String home(HttpSession session) {
        Object role = session.getAttribute("role");
        if ("ADMIN".equals(role)) return "redirect:/admin/showtimes";
        if ("STAFF".equals(role)) return "redirect:/staff/sales";
        return "redirect:/cinema";
    }

    @GetMapping("/login")
    String login(HttpSession session, Model model) {
        if (session.getAttribute("role") != null) return "redirect:/";
        model.addAttribute("loginError", session.getAttribute("loginError"));
        session.removeAttribute("loginError");
        return "login";
    }

    @GetMapping("/health")
    @ResponseBody String health() { return "ok"; }
}

@ControllerAdvice
class Navigation {
    record Link(String path, String label, String icon) {}
    private static final List<Link> ADMIN = List.of(
            new Link("/admin/showtimes", "Lịch chiếu", "calendar"),
            new Link("/catalog/films", "Thư viện phim", "film"),
            new Link("/catalog/rooms", "Phòng chiếu", "rooms"),
            new Link("/admin/prices", "Giá vé", "ticket"),
            new Link("/admin/products", "Sản phẩm F&B", "cup"),
            new Link("/admin/employees", "Nhân viên", "people"),
            new Link("/admin/reports", "Báo cáo", "reports"));
    private static final List<Link> STAFF = List.of(
            new Link("/staff/sales", "Quầy bán vé", "ticket"),
            new Link("/staff/admission", "Kiểm tra vé", "qr"),
            new Link("/staff/transactions", "Giao dịch", "receipt"),
            new Link("/catalog/films", "Tra cứu phim", "film"),
            new Link("/catalog/rooms", "Tra cứu phòng", "rooms"),
            new Link("/profile", "Hồ sơ cá nhân", "person"));
    @ModelAttribute
    void navigation(HttpServletRequest request, Model model) {
        var session = request.getSession(false);
        boolean admin = session != null && "ADMIN".equals(session.getAttribute("role"));
        var links = admin ? ADMIN : STAFF;
        String path = request.getRequestURI();
        var current = links.stream().filter(link -> path.equals(link.path()) || path.startsWith(link.path() + "/"))
                .findFirst().orElse(null);
        model.addAttribute("navigation", links);
        model.addAttribute("currentPath", current == null ? "" : current.path());
        model.addAttribute("sectionTitle", current == null ? "Cinema Workspace" : current.label());
        model.addAttribute("roleLabel", admin ? "Quản trị viên" : "Nhân viên");
    }
}
