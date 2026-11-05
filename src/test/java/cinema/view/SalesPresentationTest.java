package cinema.view;
import cinema.view.ui.*;
import cinema.dao.BanVeDAO;
import cinema.model.SuatChieu;
import java.awt.*;
import java.time.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SalesPresentationTest {
    @Test void summaryContainsEveryOriginalFieldAndUpdatesWithoutChangingSource()throws Exception{SwingUtilities.invokeAndWait(()->{Theme.install();JTextArea source=new JTextArea();SalesViews.Details view=new SalesViews.Details(source,"Giao dịch");String text="Giao dịch: G123\nTạo lúc: 2026-11-05T10:00\nNhân viên: NV002\nTrạng thái: Chờ thanh toán\nPhim: Phim thử\nSuất: 1\nPhòng: A\nGhế A1 · Đang giữ · 100000 VND · Giữ đến 2026-11-05T10:05\nTổng tiền: 100000 VND\nThanh toán: Tiền mặt";source.setText(text);String visible=Design.all(view).stream().filter(c->c instanceof JLabel).map(c->((JLabel)c).getText()).reduce("",(a,b)->a+" "+b);for(String value:new String[]{"G123","NV002","Phim thử","A1","10:05","Tiền mặt","100000 VND"})assertTrue(visible.contains(value),value);assertEquals(text,source.getText());source.setText("");assertFalse(Design.all(view).stream().filter(c->c instanceof JLabel).map(c->((JLabel)c).getText()).anyMatch(s->s!=null&&s.contains("G123")));});}
    @Test void showTableAndOriginalSelectionRemainSynchronized()throws Exception{SwingUtilities.invokeAndWait(()->{Theme.install();JComboBox<BanVeDAO.SuatBan> source=new JComboBox<>();SalesViews.Shows view=new SalesViews.Shows(source,()->{});for(int i=1;i<=2;i++)source.addItem(new BanVeDAO.SuatBan(new SuatChieu(i,"P","R",1,LocalDateTime.now(),LocalDateTime.now().plusHours(2),"Sắp chiếu"),"Phim "+i,"Phòng"));JTable table=Design.all(view).stream().filter(c->c instanceof JTable).map(c->(JTable)c).findFirst().orElseThrow();table.setRowSelectionInterval(1,1);assertEquals(1,source.getSelectedIndex());source.setSelectedIndex(0);assertEquals(0,table.getSelectedRow());assertEquals(2,table.getRowCount());});}
    @Test void seatPresentationPreservesAvailabilityAndAccessibleLabels()throws Exception{SwingUtilities.invokeAndWait(()->{for(String state:new String[]{"Trống","Đang giữ","Đã bán","Không khả dụng"}){JToggleButton button=new JToggleButton("A1");button.setEnabled(state.equals("Trống"));SeatVisual.install(button,state,"Ghế VIP",false);Theme.apply(button);assertEquals(state.equals("Trống"),button.isEnabled());assertEquals(state,button.getClientProperty("cinema.seatState"));assertTrue(button.getAccessibleContext().getAccessibleName().contains(state));}});}
}
