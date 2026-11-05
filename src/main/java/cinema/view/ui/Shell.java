package cinema.view.ui;
import cinema.controller.auth.*;
import cinema.dao.TaiKhoanDAO;
import cinema.model.VaiTro;
import cinema.view.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.IntConsumer;
import javax.swing.*;

public final class Shell extends JPanel {
    private final JPanel sidebar=new JPanel(new BorderLayout(8,16));
    private final JLabel account;
    public Shell(PhienDangNhap session,JComponent content,JList<String> menu,IntConsumer navigate,String title){
        super(new BorderLayout(0,0));Theme.install();
        sidebar.setBorder(BorderFactory.createEmptyBorder(Theme.s(Theme.XL),Theme.s(Theme.LG),20,Theme.s(Theme.LG)));sidebar.setPreferredSize(new Dimension(Theme.s(236),0));
        JLabel logo=Design.text("CINEMA", "title");JLabel caption=Design.text("WORKSPACE / NỘI BỘ","subtitle");
        String name=session.nhanVien()==null?session.tenDangNhap():session.nhanVien().hoTen();
        account=Design.text(name,"heading");JLabel role=Design.text(session.vaiTro().getTen(),"subtitle");
        JPanel identity=Design.stack(logo,caption,Box.createVerticalStrut(16),account,role);sidebar.add(identity,BorderLayout.NORTH);
        menu.putClientProperty("cinema.nav",true);menu.setBorder(BorderFactory.createEmptyBorder());menu.setFixedCellHeight(Theme.s(56));menu.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        menu.setCellRenderer(new DefaultListCellRenderer(){@Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus){JLabel label=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focus);label.setIcon(new Icons(Icons.forTitle(String.valueOf(value))));String caption=String.valueOf(value).replace("&","&amp;");if(sidebar.getWidth()<Theme.s(220))caption=caption.replace("Quản lý ","Quản lý<br>").replace("Tra cứu ","Tra cứu<br>").replace("Báo cáo và ","Báo cáo và<br>").replace("Hồ sơ ","Hồ sơ<br>").replace("Bán vé ","Bán vé<br>");label.setText("<html>"+caption+"</html>");label.setToolTipText(String.valueOf(value));label.setIconTextGap(12);label.setFont(Tokens.bold(14));label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,selected?3:0,0,0,selected?Tokens.accent():Tokens.surface()),BorderFactory.createEmptyBorder(Theme.s(Theme.SM),Theme.s(Theme.MD),Theme.s(Theme.SM),Theme.s(Theme.SM))));label.setBackground(selected?Tokens.tone(Tokens.accent()):Tokens.surface());label.setForeground(selected?Tokens.accent():Tokens.muted());return label;}});
        menu.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){int index=menu.locationToIndex(e.getPoint());if(index>=0)navigate.accept(index);}});
        menu.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0),"cinema.open");menu.getActionMap().put("cinema.open",new AbstractAction(){public void actionPerformed(ActionEvent e){if(menu.getSelectedIndex()>=0)navigate.accept(menu.getSelectedIndex());}});
        JScrollPane navigation=new JScrollPane(menu);navigation.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);sidebar.add(navigation,BorderLayout.CENTER);
        JButton theme=Theme.toggleButton();JButton exit=new JButton("Đăng xuất",new Icons("exit"));exit.addActionListener(e->logout());sidebar.add(Design.stack(theme,exit),BorderLayout.SOUTH);add(sidebar,BorderLayout.WEST);
        JPanel workspace=new JPanel(new BorderLayout());JPanel bar=new JPanel(new BorderLayout(16,0));bar.setBorder(BorderFactory.createEmptyBorder(Theme.s(Theme.LG),Theme.s(Theme.XL),Theme.s(Theme.LG),Theme.s(Theme.XL)));
        bar.add(Design.text(title,"heading"),BorderLayout.WEST);JTextField search=new Design.SearchField(22,"Tìm chức năng · Ctrl+K");search.setToolTipText("Tìm chức năng được phép · Ctrl+K");search.getAccessibleContext().setAccessibleName("Tìm chức năng");search.setText("");search.setBorder(new Skin.ControlBorder());search.addActionListener(e->{String text=search.getText().trim().toLowerCase();for(int i=0;i<menu.getModel().getSize();i++)if(menu.getModel().getElementAt(i).toLowerCase().contains(text)){menu.setSelectedIndex(i);navigate.accept(i);break;}});bar.add(search,BorderLayout.EAST);
        JScrollPane pageScroll=new JScrollPane(new AdaptivePage(content));pageScroll.setBorder(BorderFactory.createEmptyBorder());workspace.add(pageScroll,BorderLayout.CENTER);workspace.add(bar,BorderLayout.NORTH);add(workspace,BorderLayout.CENTER);
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_K,java.awt.event.InputEvent.CTRL_DOWN_MASK),"cinema.search");getActionMap().put("cinema.search",new AbstractAction(){public void actionPerformed(ActionEvent e){search.requestFocusInWindow();}});
        addComponentListener(new ComponentAdapter(){public void componentResized(ComponentEvent e){int width=getWidth()<Theme.s(1120)?Theme.s(190):Theme.s(236);sidebar.setPreferredSize(new Dimension(width,0));revalidate();}});
        Theme.apply(this);
    }
    public static void attach(JFrame frame,PhienDangNhap session,String[] functions,IntConsumer navigate){
        Container old=frame.getContentPane();JList<String> menu=new JList<>(functions);for(int i=0;i<functions.length;i++)if(frame.getTitle().equals(functions[i])||frame.getTitle().equals("Quản lý bán vé")&&functions[i].equals("Bán vé tại quầy")||frame.getTitle().equals("Kiểm tra và xác nhận vé")&&functions[i].equals("Soát vé")||frame.getTitle().equals("Thông tin cá nhân")&&functions[i].equals("Hồ sơ cá nhân"))menu.setSelectedIndex(i);
        frame.setContentPane(new Shell(session,(JComponent)old,menu,navigate,frame.getTitle()));Dimension screen=Toolkit.getDefaultToolkit().getScreenSize();frame.setSize(Math.min(1366,screen.width-40),Math.min(820,screen.height-56));frame.setMinimumSize(new Dimension(960,620));Theme.apply(frame);
    }
    public static JPanel home(PhienDangNhap session,String[] functions,IntConsumer navigate,JButton open){
        JPanel page=new JPanel(new BorderLayout(16,24));page.setBorder(BorderFactory.createEmptyBorder(Theme.s(Theme.XL),Theme.s(Theme.XL),Theme.s(Theme.XL),Theme.s(Theme.XL)));
        page.add(Design.stack(Design.text(session.vaiTro()==VaiTro.ADMIN?"Không gian quản trị":"Không gian làm việc","title"),Design.text("Chọn chức năng để bắt đầu. Dữ liệu và quyền truy cập theo phiên đăng nhập.","subtitle")),BorderLayout.NORTH);
        JPanel grid=new JPanel(new GridLayout(0,2,16,16));for(int i=0;i<functions.length;i++){final int index=i;Design.Card card=new Design.Card(new BorderLayout(12,12));JLabel name=Design.text(functions[i],"heading");name.setIcon(new Icons(Icons.forTitle(functions[i]),24));name.setIconTextGap(12);card.add(name,BorderLayout.NORTH);String hint=switch(i){case 0->"Hồ sơ, thông tin và quyền thao tác";case 1->"Danh mục, bộ lọc và thông tin chi tiết";case 2->"Không gian chiếu và thông tin vận hành";default->"Mở khu vực thao tác theo vai trò";};card.add(Design.text(hint,"subtitle"));JButton go=new JButton("Mở "+functions[i].toLowerCase());go.addActionListener(e->navigate.accept(index));card.add(go,BorderLayout.SOUTH);grid.add(card);}page.add(grid,BorderLayout.CENTER);JPanel footer=new JPanel(new FlowLayout(FlowLayout.RIGHT));footer.add(open);page.add(footer,BorderLayout.SOUTH);Theme.apply(page);page.setBackground(Tokens.bg());return page;
    }
    /** Scroll only when the available logical area cannot contain usable controls. */
    private static final class AdaptivePage extends JPanel implements Scrollable {
        AdaptivePage(JComponent content){super(new BorderLayout());add(content);}
        public Dimension getPreferredSize(){return new Dimension(Theme.s(900),Math.max(Theme.s(580),super.getPreferredSize().height));}
        public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
        public int getScrollableUnitIncrement(Rectangle r,int orientation,int direction){return Theme.s(Theme.XL);}
        public int getScrollableBlockIncrement(Rectangle r,int orientation,int direction){return Math.max(Theme.s(Theme.XL),(orientation==SwingConstants.VERTICAL?r.height:r.width)-Theme.s(Theme.XL));}
        public boolean getScrollableTracksViewportWidth(){return getParent()!=null&&getParent().getWidth()>=Theme.s(760);}
        public boolean getScrollableTracksViewportHeight(){return getParent()!=null&&getParent().getHeight()>=Theme.s(580);}
    }
    private void logout(){
        for(Window w:Window.getWindows())if(w.isShowing()&&w instanceof JFrame frame)for(Component c:Design.all(frame))if(c instanceof BanVePanel sale&&!sale.requestClose())return;
        if(JOptionPane.showConfirmDialog(this,"Đăng xuất khỏi phiên làm việc hiện tại?","Đăng xuất",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        for(Window w:Window.getWindows())if(w.isDisplayable())w.dispose();new DangNhapWindow(new DangNhapBUS(new TaiKhoanDAO())).setVisible(true);
    }
}
