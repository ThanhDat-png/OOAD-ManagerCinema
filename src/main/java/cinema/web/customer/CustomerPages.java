package cinema.web.customer;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cinema")
class CustomerPages {
    private static final DateTimeFormatter DISPLAY_EXPIRY = DateTimeFormatter.ofPattern("HH:mm · dd/MM/uuuu");
    private final CustomerSelections customer;

    CustomerPages(CustomerSelections customer) {
        this.customer = customer;
    }

    @GetMapping
    String home(Model model) {
        var films = customer.films();
        model.addAttribute("films", films);
        model.addAttribute("nowShowing", films.stream().filter(film -> "Đang chiếu".equals(film.get("TrangThai"))).toList());
        model.addAttribute("upcoming", films.stream().filter(film -> "Sắp chiếu".equals(film.get("TrangThai"))).toList());
        return "public/index";
    }

    @GetMapping("/films/{id}")
    String film(@PathVariable String id, @RequestParam(defaultValue="") String date, Model model) {
        var film = customer.film(id);
        if (film == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var shows = customer.shows(id);
        var dates = shows.stream().map(CustomerPages::showDate).distinct().toList();
        String selectedDate = dates.contains(date) ? date : dates.stream().findFirst().orElse("");
        model.addAttribute("film", film);
        model.addAttribute("dates", dates);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("shows", shows.stream().filter(show -> showDate(show).equals(selectedDate)).toList());
        return "public/film";
    }

    private static String showDate(Map<String,Object> show) {
        return show.get("ThoiGianBatDau").toString().substring(0, 10);
    }

    @GetMapping("/films/{id}/poster")
    ResponseEntity<byte[]> poster(@PathVariable String id) {
        var poster = customer.poster(id);
        if (poster == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().cacheControl(CacheControl.noCache())
                .contentType(MediaType.parseMediaType(poster.get("Mime").toString()))
                .body((byte[]) poster.get("Data"));
    }

    @GetMapping("/showtimes/{id}")
    String seats(@PathVariable int id, Model model, HttpServletResponse response) {
        var show = customer.show(id);
        if (show == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("error", "Suất chiếu không còn nhận đặt vé.");
        }
        var seats = show == null ? List.<Map<String,Object>>of() : customer.seats(id);
        model.addAttribute("show", show);
        model.addAttribute("seats", seats);
        model.addAttribute("groups", seats.stream().collect(Collectors.groupingBy(
                seat -> seat.get("HangGhe").toString(), LinkedHashMap::new, Collectors.toList())));
        return "public/seats";
    }

    @PostMapping("/showtimes/{id}")
    String submit(@PathVariable int id, @RequestParam(required=false) List<Integer> seats,
                  Model model, HttpServletResponse response) {
        try {
            return "redirect:/cinema/selection/" + customer.create(id, seats);
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            model.addAttribute("error", ex.getMessage());
            return seats(id, model, response);
        }
    }

    @GetMapping("/selection/{reference}")
    String selection(@PathVariable String reference, Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
        var selection = customer.find(reference);
        model.addAttribute("selection", selection);
        if (selection == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("error", "Mã đặt vé không tồn tại hoặc đã hết hạn.");
        } else {
            model.addAttribute("reference", selection.reference());
            model.addAttribute("show", selection.show());
            model.addAttribute("seats", selection.seats());
            model.addAttribute("total", selection.total());
            model.addAttribute("expiresAt", selection.expiresAt().format(DISPLAY_EXPIRY));
            model.addAttribute("claimed", selection.claimed());
            model.addAttribute("paid", selection.paid());
            model.addAttribute("showChanged", selection.showChanged());
        }
        return "public/selection";
    }
}
