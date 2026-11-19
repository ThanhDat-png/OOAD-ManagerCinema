package cinema.web.staff;

import cinema.controller.admission.DocMaQR;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/staff")
public class CounterPages {
    private final CounterSales sales;
    private final Admissions admissions;
    public CounterPages(CounterSales sales,Admissions admissions) {
        this.sales=sales; this.admissions=admissions;
    }
    private static String employee(HttpSession s) {
        var id=s.getAttribute("employeeId");
        if(id==null) throw new IllegalArgumentException("Tài khoản chưa gắn hồ sơ nhân viên");
        return id.toString();
    }
    private static String cart(HttpSession s) {
        synchronized(s) {
            var id=(String)s.getAttribute("saleId");
            if(id==null) { id=CounterSales.id("G"); s.setAttribute("saleId",id); }
            return id;
        }
    }
    private static void current(HttpSession s,String id) {
        if(!cart(s).equals(id)) throw new IllegalArgumentException("Giao dịch đã thay đổi. Vui lòng kiểm tra giao dịch");
    }
    private static Map<String,Object> optional(CounterSales sales,String id) {
        try { return sales.order(id); } catch(IllegalArgumentException ex) { return null; }
    }
    @GetMapping("/sales")
    public String sales(@RequestParam(defaultValue="") String film,@RequestParam(defaultValue="") String day,
                        @RequestParam(defaultValue="") String time,@RequestParam(required=false) Integer show,
                        HttpSession session,Model model) {
        String id=cart(session);
        Map<String,Object> order=optional(sales,id);
        var held=order==null?List.<Map<String,Object>>of():sales.selected(id);
        if(!held.isEmpty()) show=CounterSales.number(held.get(0),"MaSuat");
        Object selected=null; var groups=new LinkedHashMap<Object,List<Map<String,Object>>>();
        if(show!=null) {
            try {
                selected=held.isEmpty()?sales.sellableShow(show):sales.show(show);
                var seats=sales.seats(show);
                seats.forEach(seat->groups.computeIfAbsent(seat.get("HangGhe"),key->new ArrayList<>()).add(seat));
                if(order!=null) {
                    order=optional(sales,id);
                    held=sales.selected(id);
                }
            } catch(IllegalArgumentException ex) { model.addAttribute("staffError",ex.getMessage()); }
        }
        var byDate=new LinkedHashMap<String,List<Map<String,Object>>>();
        for(var entry:sales.shows(film,day,time)) {
            String display=LocalDate.parse(entry.get("ThoiGianBatDau").toString().substring(0,10))
                    .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy",Locale.forLanguageTag("vi")));
            byDate.computeIfAbsent(display,k->new ArrayList<>()).add(entry);
        }
        model.addAttribute("byDate",byDate);
        model.addAttribute("film",film); model.addAttribute("day",day); model.addAttribute("time",time);
        model.addAttribute("cartId",id); model.addAttribute("order",order); model.addAttribute("slot",selected);
        model.addAttribute("groups",groups); model.addAttribute("heldSeats",held);
        model.addAttribute("soldOut",!groups.isEmpty()&&groups.values().stream().flatMap(Collection::stream)
                .noneMatch(seat->"Trống".equals(seat.get("TrangThai"))&&"Sẵn sàng sử dụng".equals(seat.get("TinhTrang"))));
        model.addAttribute("title","Bán vé tại quầy");
        return "staff/sales";
    }
    @PostMapping("/sales/{action:hold|quote|payment|complete|cancel|new}")
    public String action(@PathVariable String action,@RequestParam String id,@RequestParam(defaultValue="0") int show,
                         @RequestParam(required=false) List<Integer> seats,@RequestParam(defaultValue="") String method,
                         @RequestParam(defaultValue="false") boolean confirmed,HttpSession session,RedirectAttributes flash) {
        try {
            String actor=employee(session);
            current(session,id);
            switch(action) {
                case "hold" -> sales.hold(id,actor,show,seats);
                case "quote" -> sales.quote(id,actor);
                case "payment" -> sales.startPayment(id,actor);
                case "complete" -> {
                    sales.pay(id,actor,method,confirmed);
                    flash.addFlashAttribute("staffNotice","Thanh toán thành công");
                    try { sales.issue(id,actor); }
                    catch(RuntimeException ex) { flash.addFlashAttribute("staffError","Giao dịch đã thanh toán. Vui lòng tiếp tục phát hành vé từ giao dịch này."); }
                    return "redirect:/staff/transactions/"+id;
                }
                case "cancel" -> {
                    sales.cancel(id,actor,confirmed); session.removeAttribute("saleId");
                    flash.addFlashAttribute("staffNotice","Đã hủy giao dịch"); return "redirect:/staff/sales";
                }
                case "new" -> {
                    var order=optional(sales,id);
                    if(order!=null&&Set.of(CounterSales.WAIT,CounterSales.PAYING).contains(order.get("TrangThai")))
                        throw new IllegalArgumentException("Vui lòng xử lý giao dịch hiện tại trước khi bắt đầu giao dịch mới");
                    session.removeAttribute("saleId"); return "redirect:/staff/sales";
                }
                default -> throw new IllegalArgumentException("Thao tác không hợp lệ");
            }
        } catch(IllegalArgumentException ex) { flash.addFlashAttribute("staffError",ex.getMessage()); }
        if(show>0) flash.addAttribute("show",show);
        return "redirect:/staff/sales";
    }
    @GetMapping("/transactions")
    public String transactions(@RequestParam(defaultValue="") String code,@RequestParam(defaultValue="") String from,
                               @RequestParam(defaultValue="") String to,@RequestParam(defaultValue="false") boolean search,Model model) {
        model.addAttribute("title","Kiểm tra giao dịch");
        model.addAttribute("code",code); model.addAttribute("from",from); model.addAttribute("to",to);
        if(search||!code.isBlank()||!from.isBlank()||!to.isBlank()) {
            try { model.addAttribute("results",sales.search(code.strip(),from,to)); }
            catch(IllegalArgumentException ex) { model.addAttribute("staffError",ex.getMessage()); }
        }
        return "staff/transactions";
    }
    @GetMapping("/transactions/{id}")
    public String receipt(@PathVariable String id,HttpSession session,Model model) {
        model.addAttribute("title","Chi tiết giao dịch");
        try {
            var order=sales.order(id);
            model.addAttribute("order",order); model.addAttribute("own",Objects.equals(order.get("MaNV"),session.getAttribute("employeeId")));
            model.addAttribute("tickets",admissions.tickets(id)); model.addAttribute("heldSeats",sales.selected(id));
        } catch(IllegalArgumentException ex) { model.addAttribute("staffError",ex.getMessage()); }
        return "staff/receipt";
    }
    @PostMapping("/transactions/{id}/issue")
    public String issue(@PathVariable String id,HttpSession s,RedirectAttributes flash) {
        try { sales.issue(id,employee(s)); flash.addFlashAttribute("staffNotice","Đã phát hành vé"); }
        catch(IllegalArgumentException ex) { flash.addFlashAttribute("staffError",ex.getMessage()); }
        return "redirect:/staff/transactions/"+id;
    }
    @PostMapping("/transactions/{id}/resume")
    public String resume(@PathVariable String id,HttpSession s,RedirectAttributes flash) {
        try {
            var previous=optional(sales,cart(s)); var order=sales.order(id);
            if(previous!=null && !id.equals(previous.get("MaGiaoDich"))&&Set.of(CounterSales.WAIT,CounterSales.PAYING).contains(previous.get("TrangThai")))
                throw new IllegalArgumentException("Vui lòng xử lý giao dịch đang mở tại quầy trước");
            if(!employee(s).equals(order.get("MaNV"))||!Set.of(CounterSales.WAIT,CounterSales.PAYING).contains(order.get("TrangThai")))
                throw new IllegalArgumentException("Không thể tiếp tục giao dịch này");
            s.setAttribute("saleId",id); return "redirect:/staff/sales";
        } catch(IllegalArgumentException ex) { flash.addFlashAttribute("staffError",ex.getMessage()); return "redirect:/staff/transactions/"+id; }
    }
    @GetMapping(value="/tickets/{id}/qr",produces=MediaType.IMAGE_PNG_VALUE)
    @ResponseBody public ResponseEntity<byte[]> qr(@PathVariable String id) throws IOException {
        try { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(admissions.qr(id)); }
        catch(IllegalArgumentException ex) { return ResponseEntity.notFound().build(); }
    }
    @GetMapping("/admission")
    public String admission(@RequestParam(defaultValue="") String code,@RequestParam(defaultValue="") String room,
                            @RequestParam(required=false) Integer show,@RequestParam(defaultValue="") String day,
                            @RequestParam(defaultValue="") String step,Model model) {
        if(day.isBlank()) day=LocalDate.now(CounterSales.CINEMA_ZONE).toString();
        model.addAttribute("title","Kiểm tra và xác nhận vé");
        model.addAttribute("rooms",admissions.rooms()); model.addAttribute("shows",admissions.shows(day));
        model.addAttribute("code",code); model.addAttribute("room",room); model.addAttribute("show",show);
        model.addAttribute("day",day); model.addAttribute("window",admissions.window);
        if(!step.isBlank()||!code.isBlank()) {
            try {
                var ticket=admissions.lookup(code); model.addAttribute("ticket",ticket);
                if("check".equals(step)) {
                    var reason=admissions.failure(ticket,room,show);
                    if(reason==null) model.addAttribute("valid",true);
                    else model.addAttribute("staffError",reason);
                }
            } catch(IllegalArgumentException ex) { model.addAttribute("staffError",ex.getMessage()); }
        }
        return "staff/admission";
    }
    @PostMapping("/admission/read")
    public String readQr(@RequestParam(required=false) MultipartFile photo,@RequestParam(defaultValue="") String room,
                         @RequestParam(required=false) Integer show,@RequestParam(defaultValue="") String day,RedirectAttributes flash) {
        Path file=null;
        try {
            if(photo==null||photo.isEmpty()||photo.getSize()>10*1024*1024) throw new IllegalArgumentException();
            file=Files.createTempFile("cinema-qr-",".image");
            photo.transferTo(file);
            flash.addAttribute("code",DocMaQR.read(file)); flash.addAttribute("step","lookup");
        } catch(IOException|IllegalArgumentException ex) { flash.addFlashAttribute("staffError","Không đọc được mã QR. Vui lòng quét lại hoặc nhập mã vé thủ công"); }
        finally { if(file!=null) try { Files.deleteIfExists(file); } catch(IOException ignored) { file.toFile().deleteOnExit(); } }
        flash.addAttribute("room",room); flash.addAttribute("show",show); flash.addAttribute("day",day);
        return "redirect:/staff/admission";
    }
    @PostMapping("/admission/confirm")
    public String confirm(@RequestParam String id,@RequestParam String room,@RequestParam int show,
                          @RequestParam String day,RedirectAttributes flash) {
        try { admissions.confirm(id,room,show); flash.addFlashAttribute("staffNotice","Xác nhận vé thành công"); }
        catch(IllegalArgumentException ex) { flash.addFlashAttribute("staffError",ex.getMessage()); }
        flash.addAttribute("code",id); flash.addAttribute("room",room); flash.addAttribute("show",show); flash.addAttribute("day",day);
        return "redirect:/staff/admission";
    }
}
