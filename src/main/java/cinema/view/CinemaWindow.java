package cinema.view;

import cinema.view.ui.*;
import cinema.controller.auth.PhienDangNhap;
import cinema.model.VaiTro;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public final class CinemaWindow {
    private CinemaWindow() { }

    public static void open(PhienDangNhap session) {
        Theme.install();
        new Workspace(session).show();
    }

    /** A session owns one frame and one shell; changing pages never closes a sale. */
    private static final class Workspace {
        private final PhienDangNhap session;
        private final JFrame frame;
        private final String[] functions;
        private final JList<String> menu;
        private final CardLayout layout=new CardLayout();
        private final JPanel pages=new JPanel(layout);
        private final Shell shell;
        private JPanel current;
        private BanVePanel sales;
        private int selected=-1;

        Workspace(PhienDangNhap session) {
            this.session=session;
            boolean admin=session.vaiTro()==VaiTro.ADMIN;
            functions=admin
                    ?new String[]{"Quản lý nhân viên","Quản lý phim","Quản lý lịch chiếu",
                        "Quản lý phòng chiếu","Quản lý giá vé và F&B","Báo cáo và thống kê"}
                    :new String[]{"Hồ sơ cá nhân","Tra cứu phim","Tra cứu phòng chiếu",
                        "Soát vé","Bán vé tại quầy"};
            frame=new JFrame("Quản lý rạp chiếu phim — "+session.vaiTro().getTen());
            menu=new JList<>(functions);
            menu.setSelectedIndex(0);
            JButton open=new JButton("Mở chức năng");
            open.addActionListener(event->navigate(menu.getSelectedIndex()));
            pages.add(Shell.home(session,functions,this::navigate,open),"home");
            shell=new Shell(session,pages,menu,this::navigate,"Điều hướng chức năng");
            frame.setContentPane(shell);
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent event) {
                    if(sales!=null&&!sales.requestClose())return;
                    frame.dispose();
                }
            });
            Dimension screen=Toolkit.getDefaultToolkit().getScreenSize();
            frame.setSize(Math.min(1366,screen.width-40),Math.min(820,screen.height-56));
            frame.setMinimumSize(new Dimension(960,620));
            frame.setLocationRelativeTo(null);
        }

        void show() { frame.setVisible(true); }

        private void navigate(int index) {
            if(index<0||index>=functions.length)return;
            if(index==selected){menu.setSelectedIndex(index);return;}
            JPanel next;
            try { next=page(index); }
            catch(IllegalArgumentException error) {
                menu.setSelectedIndex(selected<0?0:selected);
                JOptionPane.showMessageDialog(frame,error.getMessage());
                return;
            }
            // Reopen read/manage screens to refresh their existing catalogue queries.
            // Keep the sale attached even while hidden: its timer/state/lifecycle stay intact.
            if(current!=null&&current!=sales)pages.remove(current);
            String key=next==sales?"sales":"current";
            if(next.getParent()!=pages)pages.add(next,key);
            layout.show(pages,key);
            current=next;
            selected=index;
            menu.setSelectedIndex(index);
            shell.setPageTitle(functions[index]);
            shell.putClientProperty("cinema.activePage",next);
            frame.setTitle(windowTitle(index));
            pages.revalidate();
            pages.repaint();
        }

        private JPanel page(int index) {
            if(session.vaiTro()==VaiTro.ADMIN)return switch(index) {
                case 0->new NhanVienPanel(session);
                case 1->new PhimPanel(session);
                case 2->new LichChieuPanel(session);
                case 3->new PhongChieuPanel(session);
                case 4->new GiaVeFnBPanel(session);
                case 5->new BaoCaoPanel(session);
                default->throw new IllegalArgumentException("Chức năng không hợp lệ");
            };
            return switch(index) {
                case 0->new NhanVienPanel(session);
                case 1->new PhimPanel(session);
                case 2->new PhongChieuPanel(session);
                case 3->new KiemTraVePanel(session);
                case 4->{if(sales==null)sales=new BanVePanel(session);yield sales;}
                default->throw new IllegalArgumentException("Chức năng không hợp lệ");
            };
        }

        private String windowTitle(int index) {
            if(session.vaiTro()==VaiTro.ADMIN)return functions[index];
            return switch(index) {
                case 0->"Thông tin cá nhân";
                case 3->"Kiểm tra và xác nhận vé";
                case 4->"Quản lý bán vé";
                default->functions[index];
            };
        }
    }
}
