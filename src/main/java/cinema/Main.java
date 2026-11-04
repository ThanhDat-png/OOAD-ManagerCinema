package cinema;

import cinema.view.CinemaWindow;
import javax.swing.SwingUtilities;

public final class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(CinemaWindow::open);
    }
}
