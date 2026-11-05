package cinema.view;
import cinema.view.ui.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ProfilePresentationTest {
    @Test void profileShowsFreshlyLoadedEmployeeWhileRowSorterIsUpdating()throws Exception{
        SwingUtilities.invokeAndWait(()->{Theme.install();DefaultTableModel model=new DefaultTableModel(new String[]{"Mã nhân viên","Họ tên"},0);JTable source=new JTable(model);source.setAutoCreateRowSorter(true);JPanel profile=ReadOnlyViews.profile(source);model.addRow(new Object[]{"NV_TEST","Nguyễn Văn An"});assertTrue(Design.all(profile).stream().anyMatch(c->c instanceof JLabel l&&l.getText().contains("Nguyễn Văn An")));model.setRowCount(0);assertFalse(Design.all(profile).stream().anyMatch(c->c instanceof JLabel l&&l.getText().contains("Nguyễn Văn An")));assertSame(model,source.getModel());});
    }
}
