package cinema.view;
import cinema.view.ui.Forms;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FormPresentationTest {
    @Test void dateTimeHintsPreserveOriginalFormats(){assertNull(Forms.issue("Ngày (yyyy-MM-dd)","2024-02-29"));assertNotNull(Forms.issue("Ngày sinh","2026-02-30"));assertNull(Forms.issue("Giờ (HH:mm)","10:30"));assertNotNull(Forms.issue("Giờ (HH:mm)","25:00"));}
    @Test void numericAndContactHintsAreActionable(){assertNotNull(Forms.issue("Số ghế","0"));assertNull(Forms.issue("Dọn phòng (phút)","0"));assertNotNull(Forms.issue("Giá vé (VND)","100.000"));assertNull(Forms.issue("Giá vé (VND)","100000"));assertNotNull(Forms.issue("Email","a@"));assertNull(Forms.issue("Mô tả",""));}
}
