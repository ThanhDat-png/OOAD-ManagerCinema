package cinema.view;
import cinema.view.ui.*;
import java.awt.*;
import java.math.BigDecimal;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ReportPresentationTest {
    @Test void kpisUseOriginalMoneyAndClearWhenCriteriaChangeWithoutMutatingModel()throws Exception{SwingUtilities.invokeAndWait(()->{Theme.install();DefaultTableModel model=new DefaultTableModel(new Object[][]{{3L,5L,4L,1L,new BigDecimal("500000")}},new String[]{"Giao dịch","Bán","Đã phát","Chờ","Doanh thu"});JTable table=new JTable(model);JLabel status=new JLabel("1456-02");ReportVisuals view=new ReportVisuals(table,status,"time");assertTrue(labels(view).contains("500.000"));assertEquals(new BigDecimal("500000"),model.getValueAt(0,4));status.setText("Tiêu chí đã thay đổi");assertFalse(labels(view).contains("500.000"));assertEquals(1,model.getRowCount());});}
    @Test void stockKpisCountProductsInsteadOfAddingDifferentUnits()throws Exception{SwingUtilities.invokeAndWait(()->{DefaultTableModel model=new DefaultTableModel(new Object[][]{{"A","Nước","Ly","Đang kinh doanh",7},{"B","Bắp","Hộp","Đang kinh doanh",0}},new String[]{"Mã","Tên","Đơn vị","Trạng thái","Tồn"});ReportVisuals view=new ReportVisuals(new JTable(model),new JLabel("2 sản phẩm"),"stock");String labels=labels(view);assertTrue(labels.contains("Sản phẩm theo bộ lọc"));assertTrue(labels.contains("Hết hàng"));assertFalse(labels.contains("Tổng tồn kho"));assertEquals(7,model.getValueAt(0,4));});}
    private static String labels(Container root){return Design.all(root).stream().filter(c->c instanceof JLabel).map(c->((JLabel)c).getText()).reduce("",(a,b)->a+" "+b);}
}
