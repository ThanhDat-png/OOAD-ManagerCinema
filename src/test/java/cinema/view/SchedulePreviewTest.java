package cinema.view;
import cinema.view.ui.*;
import cinema.model.*;
import java.time.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SchedulePreviewTest {
    @Test void previewIncludesCleanupAllowsAdjacentBoundaryAndDoesNotChangeRows()throws Exception{SwingUtilities.invokeAndWait(()->{Theme.install();JDialog dialog=new JDialog();LocalDate date=LocalDate.of(2030,1,1);Phim film=new Phim("P","Phim",90,"Dir",date,0,"","Đang chiếu","2D",List.of());PhongChieu room=new PhongChieu("R",new LoaiPhong(1,"2D","2D"),"Phòng R",15,"Đang hoạt động");JTable table=new JTable(new DefaultTableModel(new Object[][]{{1,"Phim","Phòng R",date.atTime(9,30),date.atTime(11,15),"Ngày thường","Sắp chiếu"}},new String[]{"Mã","Phim","Phòng","Bắt đầu","Trả","Loại","Trạng thái"}));JTextField day=new JTextField(date.toString()),time=new JTextField("10:00");JLabel note=SchedulePreview.create(dialog,table,new JComboBox<>(new Phim[]{film}),new JComboBox<>(new PhongChieu[]{room}),day,time,null);assertTrue(note.getText().contains("trùng"));time.setText("11:15");assertFalse(note.getText().contains("trùng"));assertTrue(note.getText().contains("13:00"));assertTrue(note.getText().contains("15 phút"));assertEquals(1,table.getRowCount());assertEquals(date.atTime(11,15),table.getValueAt(0,4));dialog.dispose();});}
}
