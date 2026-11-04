package cinema;

import cinema.controller.auth.DangNhapBUS;
import cinema.dao.TaiKhoanDAO;
import cinema.view.DangNhapWindow;
import javax.swing.SwingUtilities;

public final class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DangNhapWindow(new DangNhapBUS(new TaiKhoanDAO())).setVisible(true));
    }
}
