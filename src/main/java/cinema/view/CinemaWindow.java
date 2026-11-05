package cinema.view;

import cinema.view.ui.*;

import cinema.controller.auth.PhienDangNhap;
import cinema.model.VaiTro;
import java.awt.BorderLayout;
import javax.swing.*;

public final class CinemaWindow {
    private CinemaWindow() { }

    public static void open(PhienDangNhap session) {
        Theme.install();
        JFrame frame = new JFrame("Quản lý rạp chiếu phim — " + session.vaiTro().getTen());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        String[] functions = session.vaiTro() == VaiTro.ADMIN
                ? new String[]{"Quản lý nhân viên", "Quản lý phim", "Quản lý lịch chiếu",
                    "Quản lý phòng chiếu", "Quản lý giá vé và F&B", "Báo cáo và thống kê"}
                : new String[]{"Xem thông tin nhân viên", "Tra cứu phim", "Tra cứu phòng chiếu",
                    "Kiểm tra, xác nhận vé", "Quản lý bán vé"};
        JList<String> menu = new JList<>(functions);
        menu.setSelectedIndex(0);

        JButton open = new JButton("Mở chức năng");
        open.addActionListener(event -> {
            if (menu.getSelectedIndex() == 1) {
                JFrame films = new JFrame(session.vaiTro() == VaiTro.ADMIN ? "Quản lý phim" : "Tra cứu phim");
                films.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                films.add(new PhimPanel(session));
                films.setSize(1200, 620);
                films.setLocationRelativeTo(frame);
                Shell.attach(films,session,functions,index -> {menu.setSelectedIndex(index);open.doClick();});
                films.setVisible(true);
                return;
            }
            if (session.vaiTro() == VaiTro.ADMIN && menu.getSelectedIndex() == 2) {
                try {
                    JFrame schedule = new JFrame("Quản lý lịch chiếu");
                    schedule.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    schedule.add(new LichChieuPanel(session));
                    schedule.setSize(1280, 620);
                    schedule.setLocationRelativeTo(frame);
                    Shell.attach(schedule,session,functions,index -> {menu.setSelectedIndex(index);open.doClick();});
                schedule.setVisible(true);
                } catch (IllegalArgumentException e) {
                    JOptionPane.showMessageDialog(frame, e.getMessage());
                }
                return;
            }
            if ((session.vaiTro() == VaiTro.ADMIN && menu.getSelectedIndex() == 3)
                    || (session.vaiTro() == VaiTro.NHAN_VIEN && menu.getSelectedIndex() == 2)) {
                JFrame rooms = new JFrame(session.vaiTro() == VaiTro.ADMIN ? "Quản lý phòng chiếu" : "Tra cứu phòng chiếu");
                rooms.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                rooms.add(new PhongChieuPanel(session));
                rooms.setSize(1200, 620);
                rooms.setLocationRelativeTo(frame);
                Shell.attach(rooms,session,functions,index -> {menu.setSelectedIndex(index);open.doClick();});
                rooms.setVisible(true);
                return;
            }
            if (session.vaiTro() == VaiTro.ADMIN && menu.getSelectedIndex() == 4) {
                JFrame pricing = new JFrame("Quản lý giá vé và F&B");
                pricing.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                pricing.add(new GiaVeFnBPanel(session));
                pricing.setSize(1280, 620);
                pricing.setLocationRelativeTo(frame);
                Shell.attach(pricing,session,functions,index -> {menu.setSelectedIndex(index);open.doClick();});
                pricing.setVisible(true);
                return;
            }
            if (session.vaiTro() == VaiTro.NHAN_VIEN && menu.getSelectedIndex() == 3) {
                try {
                    KiemTraVePanel panel = new KiemTraVePanel(session);
                    JFrame admission = new JFrame("Kiểm tra và xác nhận vé");
                    admission.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    admission.add(panel);
                    admission.setSize(1280, 680);
                    admission.setLocationRelativeTo(frame);
                    Shell.attach(admission,session,functions,index -> {menu.setSelectedIndex(index);open.doClick();});
                admission.setVisible(true);
                } catch (IllegalArgumentException e) {
                    JOptionPane.showMessageDialog(frame, e.getMessage());
                }
                return;
            }
            if (session.vaiTro() == VaiTro.NHAN_VIEN && menu.getSelectedIndex() == 4) {
                try {
                    BanVePanel panel=new BanVePanel(session); JFrame sales=new JFrame("Quản lý bán vé");
                    sales.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                    sales.addWindowListener(new java.awt.event.WindowAdapter() {
                        @Override public void windowClosing(java.awt.event.WindowEvent event) { if(panel.requestClose()) sales.dispose(); }
                    });
                    sales.add(panel); sales.setSize(1320,760); sales.setLocationRelativeTo(frame); Shell.attach(sales,session,functions,index -> {menu.setSelectedIndex(index);open.doClick();});
                sales.setVisible(true);
                } catch(IllegalArgumentException e) { JOptionPane.showMessageDialog(frame,e.getMessage()); }
                return;
            }
            if (session.vaiTro() == VaiTro.ADMIN && menu.getSelectedIndex() == 5) {
                try {
                    JFrame reports=new JFrame("Báo cáo và thống kê");reports.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    reports.add(new BaoCaoPanel(session));reports.setSize(1380,760);reports.setLocationRelativeTo(frame);Shell.attach(reports,session,functions,index -> {menu.setSelectedIndex(index);open.doClick();});
                reports.setVisible(true);
                } catch(IllegalArgumentException e){JOptionPane.showMessageDialog(frame,e.getMessage());}
                return;
            }
            if (menu.getSelectedIndex() != 0) {
                JOptionPane.showMessageDialog(frame, "Chức năng đang được hoàn thiện.");
                return;
            }
            JFrame staff = new JFrame(session.vaiTro() == VaiTro.ADMIN ? "Quản lý nhân viên" : "Thông tin cá nhân");
            staff.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            staff.add(new NhanVienPanel(session));
            staff.setSize(1180, 520);
            staff.setLocationRelativeTo(frame);
            Shell.attach(staff,session,functions,index -> {menu.setSelectedIndex(index);open.doClick();});
                staff.setVisible(true);
        });
        frame.setContentPane(new Shell(session,Shell.home(session,functions,index -> {menu.setSelectedIndex(index);open.doClick();},open),menu,index -> {menu.setSelectedIndex(index);open.doClick();},"Điều hướng chức năng"));
        frame.setSize(1320, 800);
        frame.setMinimumSize(new java.awt.Dimension(960,620));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
