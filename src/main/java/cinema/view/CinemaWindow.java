package cinema.view;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public final class CinemaWindow {
    public static void open() {
        JFrame frame = new JFrame("Quản lý rạp chiếu phim");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new JLabel("Hệ thống quản lý rạp chiếu phim", SwingConstants.CENTER));
        frame.setSize(480, 240);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
