package cinema.view.ui;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.plaf.FontUIResource;

public class Theme {
    private static boolean installed;
    protected Theme(){}
    public static boolean dark=true;
    public static final int XS=4,SM=8,MD=12,LG=16,XL=24,XXL=32,CONTROL=40,ROW=44,RADIUS=16;
    public static final String FAMILY="Segoe UI",NUMBERS="Consolas";
    private static Color c(String value){return Color.decode(value);}
    public static Color bg(){return c(dark?"#10111B":"#F3F4F8");}
    public static Color surface(){return c(dark?"#191B2A":"#FFFFFF");}
    public static Color raised(){return c(dark?"#222538":"#F7F7FC");}
    public static Color border(){return c(dark?"#3A4058":"#C3C7D3");}
    public static Color text(){return c(dark?"#F2F4FC":"#1B2035");}
    public static Color muted(){return c(dark?"#B0B9D0":"#555F77");}
    public static Color accent(){return c(dark?"#B7A1FF":"#5637AF");}
    public static Color accentInk(){return c(dark?"#181327":"#FFFFFF");}
    public static Color success(){return c(dark?"#7EE2B5":"#146548");}
    public static Color warning(){return c(dark?"#F5CF83":"#785100");}
    public static Color danger(){return c(dark?"#FFADB4":"#A52C3D");}
    public static Color info(){return c(dark?"#9EC4FF":"#254E98");}
    public static Color shadow(){return new Color(0,0,0,dark?50:12);}
    public static Color mix(Color a,Color b,double amount){return new Color((int)(a.getRed()*(1-amount)+b.getRed()*amount),(int)(a.getGreen()*(1-amount)+b.getGreen()*amount),(int)(a.getBlue()*(1-amount)+b.getBlue()*amount));}
    public static Color tone(Color color){return mix(surface(),color,dark?.13:.08);}
    public static Font font(int size){return new Font(FAMILY,Font.PLAIN,s(size));}
    public static Font bold(int size){return new Font(FAMILY,Font.BOLD,s(size));}
    public static Font number(){return number(14);}
    public static Font number(int size){return new Font(NUMBERS,Font.PLAIN,s(size));}
    public static Color semantic(String text){if(text==null)return muted();if(text.contains("hủy")||text.contains("Hủy")||text.contains("Ngừng")||text.contains("Bảo trì")||text.contains("Đã bán")||text.contains("Không khả dụng")||text.contains("Hết hàng"))return danger();if(text.contains("Đang giữ")||text.contains("Chờ")||text.contains("chờ")||text.contains("Hết hạn")||text.contains("Đang thanh toán"))return warning();if(text.contains("Đang chọn"))return accent();if(text.contains("Đã sử dụng")||text.contains("Chưa sử dụng"))return info();return success();}

    public static int s(int value){return com.formdev.flatlaf.util.UIScale.scale(value);}
    private static void lookAndFeel(){com.formdev.flatlaf.FlatLaf.setUseNativeWindowDecorations(false);JFrame.setDefaultLookAndFeelDecorated(false);JDialog.setDefaultLookAndFeelDecorated(false);if(dark)com.formdev.flatlaf.FlatDarkLaf.setup();else com.formdev.flatlaf.FlatLightLaf.setup();}
    public static void install(){if(installed)return;installed=true;lookAndFeel();defaults();Toolkit.getDefaultToolkit().addAWTEventListener(event->{if(event instanceof WindowEvent e&&e.getID()==WindowEvent.WINDOW_OPENED){apply(e.getWindow());if(e.getWindow() instanceof JDialog dialog)modal(dialog);}},AWTEvent.WINDOW_EVENT_MASK);}
    private static void defaults(){
        for(Object key:new ArrayList<>(UIManager.getDefaults().keySet()))if(key.toString().endsWith(".font"))UIManager.put(key,new FontUIResource(Tokens.font(14)));
        for(String key:new String[]{"Panel.background","OptionPane.background","Viewport.background","ScrollPane.background","Table.background","List.background","TextArea.background"})UIManager.put(key,Tokens.surface());
        for(String key:new String[]{"Label.foreground","OptionPane.messageForeground","TextField.foreground","PasswordField.foreground","TextArea.foreground","List.foreground","Table.foreground","ComboBox.foreground","TabbedPane.foreground","CheckBox.foreground","RadioButton.foreground","MenuItem.foreground"})UIManager.put(key,Tokens.text());
        UIManager.put("control",Tokens.surface());UIManager.put("text",Tokens.text());UIManager.put("nimbusBase",Tokens.accent());UIManager.put("ComboBox.background",Tokens.raised());UIManager.put("TextField.background",Tokens.raised());UIManager.put("PasswordField.background",Tokens.raised());UIManager.put("CheckBox.background",Tokens.surface());UIManager.put("RadioButton.background",Tokens.surface());
        UIManager.put("TableUI",Skin.TableUI.class.getName());
        for(String key:new String[]{"ComboBox.buttonBackground","ComboBox.buttonEditableBackground","Spinner.buttonBackground"})UIManager.put(key,Tokens.raised());
        UIManager.put("ComboBox.buttonArrowColor",Tokens.muted());UIManager.put("ComboBox.buttonHoverArrowColor",Tokens.accent());UIManager.put("ComboBox.buttonPressedArrowColor",Tokens.text());
        UIManager.put("Button.minimumHeight",40);UIManager.put("ToggleButton.minimumHeight",40);UIManager.put("ComboBox.minimumHeight",40);UIManager.put("Button.arc",12);UIManager.put("Component.arc",12);UIManager.put("TextComponent.arc",12);UIManager.put("Component.focusWidth",1);UIManager.put("Component.focusColor",Tokens.accent());UIManager.put("Component.arrowType","chevron");UIManager.put("Component.borderColor",Tokens.border());UIManager.put("Component.minimumHeight",40);UIManager.put("Component.minimumWidth",48);UIManager.put("TabbedPane.tabType","card");
        UIManager.put("Table.selectionBackground",Tokens.tone(Tokens.accent()));UIManager.put("Table.selectionForeground",Tokens.text());UIManager.put("Table.gridColor",Tokens.border());UIManager.put("TableHeader.background",Tokens.raised());UIManager.put("TableHeader.foreground",Tokens.muted());UIManager.put("List.selectionBackground",Tokens.tone(Tokens.accent()));UIManager.put("List.selectionForeground",Tokens.accent());UIManager.put("ScrollBar.width",10);UIManager.put("SplitPane.dividerSize",8);UIManager.put("TabbedPane.selected",Tokens.raised());
    }
    public static void toggle(){Tokens.dark=!Tokens.dark;lookAndFeel();defaults();for(Window window:Window.getWindows()){SwingUtilities.updateComponentTreeUI(window);apply(window);window.repaint();}}
    public static JButton toggleButton(){JButton button=new JButton(Tokens.dark?"Giao diện sáng":"Giao diện tối",new Icons("sun"));button.putClientProperty("cinema.themeToggle",true);button.addActionListener(e->toggle());return button;}
    public static boolean reducedMotion(){return Boolean.getBoolean("cinema.ui.reducedMotion")||Boolean.FALSE.equals(Toolkit.getDefaultToolkit().getDesktopProperty("win.clientAreaAnimation"));}
    public static void apply(Component component){
        if(component instanceof JComponent c){c.setForeground(Tokens.text());if(!(c instanceof JPasswordField))c.setFont(Tokens.font(14));
            if(c instanceof JPanel||c instanceof JViewport)c.setBackground("canvas".equals(c.getClientProperty("cinema.background"))?Tokens.bg():Tokens.surface());
            if(c instanceof JPanel panel&&panel.getLayout() instanceof GridLayout grid&&grid.getColumns()==2&&java.util.Arrays.stream(panel.getComponents()).anyMatch(child->child instanceof JTextField||child instanceof JComboBox<?>)){
                Component[] children=panel.getComponents();panel.removeAll();panel.setLayout(new GridBagLayout());panel.putClientProperty("cinema.formLayout",true);for(int i=0;i<children.length;i++){GridBagConstraints g=new GridBagConstraints();g.gridx=i%2;g.gridy=i/2;g.weightx=i%2==0?.3:.7;g.fill=GridBagConstraints.HORIZONTAL;g.anchor=GridBagConstraints.NORTHWEST;g.insets=new Insets(4,4,4,8);panel.add(children[i],g);if(i%2==0&&children[i] instanceof JLabel label&&i+1<children.length)label.setLabelFor(children[i+1]);}
            }
            if(c instanceof JLabel label){if("total".equals(label.getClientProperty("cinema.text"))){label.setFont(Tokens.number(28));label.setForeground(Tokens.accent());}else if("title".equals(label.getClientProperty("cinema.text")))label.setFont(Tokens.bold(28));else if("subtitle".equals(label.getClientProperty("cinema.text"))){label.setForeground(Tokens.muted());label.setFont(Tokens.font(12));}else if("metric".equals(label.getClientProperty("cinema.text")))label.setFont(Tokens.number(28));else if("error".equals(label.getClientProperty("cinema.text"))){label.setFont(Tokens.font(12));label.setForeground(Tokens.danger());}else if("heading".equals(label.getClientProperty("cinema.text")))label.setFont(Tokens.bold(16));}
            if(c instanceof JTextComponent text){text.setForeground(Tokens.text());text.setBackground(Tokens.raised());text.setCaretColor(Tokens.accent());text.setSelectionColor(Tokens.tone(Tokens.accent()));text.setSelectedTextColor(Tokens.text());if("scan".equals(text.getClientProperty("cinema.text")))text.setFont(Tokens.font(20));if(text instanceof JTextField){text.setBorder(new Skin.ControlBorder());text.setMinimumSize(new Dimension(80,40));}}
            if(c instanceof AbstractButton button && !(button instanceof JCheckBox)&&!(button instanceof JRadioButton)){if(button instanceof JButton&&button.getIcon()==null&&!Boolean.TRUE.equals(button.getClientProperty("cinema.noIcon")))button.setIcon(new Icons(Icons.forTitle(button.getText())));button.setFont(Tokens.bold(14));button.setMinimumSize(new Dimension(40,40));String t=button.getText()==null?"":button.getText();if(button.getClientProperty("cinema.role")==null){if(t.startsWith("Xóa")||t.startsWith("Ngừng")||t.equals("Hủy suất chiếu")||t.equals("Hủy giao dịch"))button.putClientProperty("cinema.role","danger");else if(t.startsWith("Thêm")||t.startsWith("Tạo")||t.equals("Lưu")||t.equals("Đăng nhập")||t.equals("Mở chức năng")||t.equals("Hoàn tất giao dịch")||t.startsWith("Thanh toán"))button.putClientProperty("cinema.role","primary");}String role=String.valueOf(button.getClientProperty("cinema.role"));button.setBackground("primary".equals(role)?Tokens.accent():"danger".equals(role)?Tokens.tone(Tokens.danger()):Tokens.raised());button.setForeground("primary".equals(role)?Tokens.accentInk():"danger".equals(role)?Tokens.danger():Tokens.text());button.setMargin(new Insets(8,12,8,12));if(button.getClientProperty("cinema.seatState")!=null)button.setUI(new SeatVisual.SeatUI());if(Boolean.TRUE.equals(button.getClientProperty("cinema.themeToggle")))button.setText(Tokens.dark?"Giao diện sáng":"Giao diện tối");}
            if(c instanceof JComboBox<?> combo){combo.setBackground(Tokens.raised());combo.setBorder(new Skin.ControlBorder());combo.setMinimumSize(new Dimension(80,40));}
            if(c instanceof JTable table){table.setRowHeight(Boolean.TRUE.equals(table.getClientProperty("cinema.posterTable"))?112:Boolean.TRUE.equals(table.getClientProperty("cinema.compactTable"))?32:Tokens.ROW);table.setShowVerticalLines(false);table.setShowHorizontalLines(false);table.setIntercellSpacing(new Dimension(0,0));table.setFillsViewportHeight(true);Skin.Cell cell=new Skin.Cell();for(Class<?> type:new Class<?>[]{Object.class,String.class,Integer.class,Long.class,java.math.BigDecimal.class,java.time.LocalDate.class,java.time.LocalDateTime.class})table.setDefaultRenderer(type,cell);table.getTableHeader().setPreferredSize(new Dimension(0,Boolean.TRUE.equals(table.getClientProperty("cinema.compactTable"))?32:44));table.getTableHeader().setFont(Tokens.bold(12));table.getTableHeader().setReorderingAllowed(false);table.getTableHeader().setDefaultRenderer(new Skin.Header());table.setBackground(Tokens.surface());table.setForeground(Tokens.text());table.setSelectionBackground(Tokens.tone(Tokens.accent()));}
            if(c instanceof JScrollPane scroll){scroll.setBorder(BorderFactory.createEmptyBorder());scroll.getViewport().setBackground(Tokens.surface());scroll.getVerticalScrollBar().setUnitIncrement(24);scroll.getHorizontalScrollBar().setUnitIncrement(24);}
            if(c instanceof JSplitPane split){split.setBorder(BorderFactory.createEmptyBorder());split.setDividerSize(8);split.setContinuousLayout(true);}
            if(c instanceof JTabbedPane tabs){tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);tabs.setBackground(Tokens.bg());}
            if(c instanceof JList<?> list){list.setBackground(Tokens.surface());list.setForeground(Tokens.text());list.setSelectionBackground(Tokens.tone(Tokens.accent()));list.setSelectionForeground(Tokens.accent());list.setFixedCellHeight(Boolean.TRUE.equals(list.getClientProperty("cinema.nav"))?56:44);}
            if(c.getAccessibleContext()!=null&&c.getAccessibleContext().getAccessibleName()==null){if(c instanceof AbstractButton b)c.getAccessibleContext().setAccessibleName(b.getText());if(c instanceof JTextComponent t&&t.getToolTipText()!=null)c.getAccessibleContext().setAccessibleName(t.getToolTipText());}
            if(!Boolean.TRUE.equals(c.getClientProperty("cinema.observed"))){c.putClientProperty("cinema.observed",true);c.addFocusListener(new FocusAdapter(){public void focusGained(FocusEvent e){c.repaint();}public void focusLost(FocusEvent e){c.repaint();}});c.addContainerListener(new ContainerAdapter(){public void componentAdded(ContainerEvent e){apply(e.getChild());}});}
        }
        if(component instanceof JComponent c&&c.getClientProperty("cinema.statusRefresh") instanceof Runnable refresh)refresh.run();
        if(component instanceof Container container)for(Component child:container.getComponents())apply(child);
    }
    private static void modal(JDialog dialog){
        Forms.attach(dialog);
        boolean form=Design.all(dialog).stream().anyMatch(c->c instanceof JComponent p&&Boolean.TRUE.equals(p.getClientProperty("cinema.formLayout")));
        if(form){Dimension screen=Toolkit.getDefaultToolkit().getScreenSize();Dimension preferred=dialog.getPreferredSize();if(preferred.height>screen.height-96){Container old=dialog.getContentPane();JScrollPane scroll=new JScrollPane((Component)old);scroll.setPreferredSize(new Dimension(Math.min(preferred.width,screen.width-96),screen.height-128));dialog.setContentPane(scroll);apply(scroll);}dialog.pack();}fit(dialog);
    }
    public static void fit(Window window){if(!window.isDisplayable())return;Dimension screen=Toolkit.getDefaultToolkit().getScreenSize();int maxW=Math.min(1320,screen.width-48),maxH=Math.min(880,screen.height-64);if(window.getWidth()>maxW||window.getHeight()>maxH)window.setSize(Math.min(window.getWidth(),maxW),Math.min(window.getHeight(),maxH));}
}
