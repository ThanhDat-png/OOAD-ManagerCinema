package cinema.view;
import cinema.view.ui.*;
import java.awt.*;
import java.time.*;
import java.util.Date;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DatePickerPresentationTest {
    @Test void pickersKeepExistingDayMonthYearTimeFormatsAndAllowOptionalBlank()throws Exception{
        SwingUtilities.invokeAndWait(()->{Theme.install();JFrame frame=new JFrame();JTextField field=new JTextField(20);JComboBox<String> period=new JComboBox<>(new String[]{"Ngày","Tháng","Năm","Khoảng"});field.putClientProperty("cinema.periodSelector",period);DateInputs.attach(field,false,true);frame.add(field);frame.pack();frame.setVisible(true);try{
            for(int index=0;index<3;index++){period.setSelectedIndex(index);field.setText("");JPopupMenu popup=open(field);JSpinner spinner=spinner(popup);spinner.setValue(index==2?2027:Date.from(LocalDateTime.of(2027,4,18,12,0).atZone(ZoneId.systemDefault()).toInstant()));button(popup,"Áp dụng").doClick();assertEquals(new String[]{"2027-04-18","2027-04","2027"}[index],field.getText());}
            button(open(field),"Để trống").doClick();assertEquals("",field.getText());
            JTextField time=new JTextField(10);frame.setContentPane(new JPanel());frame.getContentPane().add(time);DateInputs.attach(time,true,true);frame.pack();JPopupMenu popup=open(time);spinner(popup).setValue(Date.from(LocalDateTime.of(2027,4,18,9,35).atZone(ZoneId.systemDefault()).toInstant()));button(popup,"Áp dụng").doClick();assertEquals("09:35",time.getText());
        }finally{MenuSelectionManager.defaultManager().clearSelectedPath();frame.dispose();}});
    }
    private static JPopupMenu open(JTextField field){((JButton)field.getClientProperty("JTextField.trailingComponent")).doClick();for(MenuElement element:MenuSelectionManager.defaultManager().getSelectedPath())if(element instanceof JPopupMenu menu)return menu;throw new AssertionError("Không mở được bộ chọn");}
    private static JSpinner spinner(JPopupMenu popup){return Design.all(popup).stream().filter(c->c instanceof JSpinner).map(c->(JSpinner)c).findFirst().orElseThrow();}
    private static JButton button(JPopupMenu popup,String name){return Design.all(popup).stream().filter(c->c instanceof JButton b&&name.equals(b.getText())).map(c->(JButton)c).findFirst().orElseThrow();}
}
