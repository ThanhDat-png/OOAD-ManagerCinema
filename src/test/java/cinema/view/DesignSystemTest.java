package cinema.view;
import cinema.view.ui.*;
import cinema.controller.auth.PhienDangNhap;
import cinema.model.VaiTro;
import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
class DesignSystemTest {
    @AfterEach void close()throws Exception{SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())w.dispose();if(!Tokens.dark)Theme.toggle();});}
    @Test void bothPalettesMeetAaForTextAndSemanticColors(){for(boolean dark:new boolean[]{true,false}){Tokens.dark=dark;for(Color bg:new Color[]{Tokens.bg(),Tokens.surface(),Tokens.raised()}){assertTrue(ratio(Tokens.text(),bg)>=4.5,"body");assertTrue(ratio(Tokens.muted(),bg)>=4.5,"secondary");}assertTrue(ratio(Tokens.accent(),Tokens.accentInk())>=4.5);for(Color color:new Color[]{Tokens.success(),Tokens.warning(),Tokens.danger(),Tokens.info(),Tokens.accent()})assertTrue(ratio(color,Tokens.tone(color))>=4.5,"semantic");}Tokens.dark=true;}
    @Test void togglingThemePreservesModelsSelectionsAndText()throws Exception{SwingUtilities.invokeAndWait(()->{Theme.install();JFrame frame=new JFrame();JPanel body=new JPanel();JTextField input=new JTextField("Giữ nguyên");JTable table=new JTable(new DefaultTableModel(new Object[][]{{"A",123}},new Object[]{"Ghế","Tiền"}));table.setRowSelectionInterval(0,0);body.add(input);body.add(new JScrollPane(table));frame.add(body);frame.pack();frame.setVisible(true);var model=table.getModel();Theme.toggle();capture(frame,"foundation-light.png");assertSame(model,table.getModel());assertEquals(0,table.getSelectedRow());assertEquals("Giữ nguyên",input.getText());Theme.toggle();capture(frame,"foundation-dark.png");assertEquals(123,table.getValueAt(0,1));});}
    @Test void shellKeepsRoleMenuAndKeyboardSearchAtTabletWidth()throws Exception{SwingUtilities.invokeAndWait(()->{Theme.install();JFrame frame=new JFrame();JList<String> menu=new JList<>(new String[]{"Bán vé","Kiểm tra vé"});JPanel content=new JPanel();JButton action=new JButton("Thao tác");content.add(action);Shell shell=new Shell(new PhienDangNhap(2,"staff",VaiTro.NHAN_VIEN,null),content,menu,i->{},"Quầy vé");frame.add(shell);frame.setSize(1024,768);frame.setVisible(true);assertEquals(2,menu.getModel().getSize());assertTrue(action.isFocusable());assertNotNull(shell.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(KeyStroke.getKeyStroke("ctrl K")));assertTrue(menu.getWidth()>0);capture(frame,"foundation-tablet.png");});}
    private static void capture(Window window,String name){try{java.nio.file.Path dir=java.nio.file.Path.of("target","ui-redesign","sprint1");java.nio.file.Files.createDirectories(dir);var image=new java.awt.image.BufferedImage(window.getWidth(),window.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();window.paint(g);g.dispose();javax.imageio.ImageIO.write(image,"png",dir.resolve(name).toFile());}catch(java.io.IOException e){throw new RuntimeException(e);}}
    private static double linear(double value){value/=255;return value<=.04045?value/12.92:Math.pow((value+.055)/1.055,2.4);}
    private static double luminance(Color c){return .2126*linear(c.getRed())+.7152*linear(c.getGreen())+.0722*linear(c.getBlue());}
    private static double ratio(Color a,Color b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
}
