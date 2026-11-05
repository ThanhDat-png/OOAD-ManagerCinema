package cinema.view.ui;
import java.awt.*;
import java.awt.event.*;
import java.text.Normalizer;
import java.time.*;
import javax.swing.*;
import javax.swing.event.*;
/** Inline input hints only. Existing save handlers and BUS remain the final authority. */
public final class Forms {
    private Forms(){}
    private static String normalized(String s){return Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase();}
    public static String issue(String label,String value){String name=normalized(label),text=value==null?"":value.trim();if(text.isEmpty()){if(name.contains("mo ta"))return null;return "Vui lòng nhập "+label.toLowerCase()+".";}try{
        if(name.contains("ngay")||name.contains("khoi chieu")){LocalDate.parse(text);return null;}
        if(name.contains("gio (")||name.equals("gio")){LocalTime.parse(text);return null;}
        if(name.contains("email")&&!text.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))return "Nhập email dạng ten@mien.vn.";
        if(name.contains("dien thoai")&&!text.matches("\\+?[0-9]{8,15}"))return "Nhập 8–15 chữ số, có thể bắt đầu bằng +.";
        if(name.contains("gia ")||name.contains("so tien")){if(!text.matches("[0-9]{1,12}")||new java.math.BigDecimal(text).signum()<=0)return "Nhập tiền VND bằng số nguyên dương, tối đa 12 chữ số.";}
        if(name.contains("so ghe")||name.contains("thoi luong")||name.contains("don phong")||name.contains("tuoi")||name.contains("so luong")){int n=Integer.parseInt(text);boolean positive=name.contains("so ghe")||name.contains("thoi luong");if(n<(positive?1:0))return positive?"Nhập số nguyên lớn hơn 0.":"Nhập số nguyên không âm.";}
        int max=name.contains("ten phim")?200:name.contains("ten phong")?50:name.contains("ho ten")||name.contains("ten san pham")?100:name.contains("ma san pham")?10:Integer.MAX_VALUE;if(text.length()>max)return "Nhập tối đa "+max+" ký tự.";
    }catch(java.time.format.DateTimeParseException e){return name.contains("gio")?"Chọn giờ hoặc nhập HH:mm.":"Chọn ngày hoặc nhập yyyy-MM-dd hợp lệ.";}catch(NumberFormatException e){return "Nhập một số nguyên hợp lệ.";}return null;}
    public static void attach(Container root){for(Component c:Design.all(root))if(c instanceof JLabel label&&label.getLabelFor() instanceof JTextField field&&!(field instanceof JPasswordField))bind(field,label.getText());}
    public static void bind(JTextField field,String name){if(field.getClientProperty("cinema.validation")!=null)return;field.putClientProperty("cinema.validation",true);field.getAccessibleContext().setAccessibleName(name);String key=normalized(name);if(key.contains("ngay")||key.contains("khoi chieu"))DateInputs.attach(field,false,false);else if(key.contains("gio (")||key.equals("gio"))DateInputs.attach(field,true,false);
        JLabel error=Design.text(" ","error");error.setVisible(false);Container parent=field.getParent();if(parent!=null&&parent.getLayout() instanceof GridBagLayout grid){GridBagConstraints constraints=grid.getConstraints(field);parent.remove(field);JPanel wrapper=new JPanel(new BorderLayout(0,4));wrapper.add(field);wrapper.add(error,BorderLayout.SOUTH);parent.add(wrapper,constraints);}
        Runnable check=()->{String message=issue(name,field.getText());field.putClientProperty("cinema.invalid",message!=null);field.putClientProperty("JComponent.outline",message==null?null:"error");error.setText(message==null?" ":message);error.setVisible(message!=null);error.setForeground(Theme.danger());field.setToolTipText(message==null?name:message);field.repaint();};field.addFocusListener(new FocusAdapter(){public void focusLost(FocusEvent e){check.run();}});field.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){check.run();}public void removeUpdate(DocumentEvent e){check.run();}public void changedUpdate(DocumentEvent e){check.run();}});
    }
}
