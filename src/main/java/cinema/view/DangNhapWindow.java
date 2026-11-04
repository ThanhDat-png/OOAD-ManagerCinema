package cinema.view;

import cinema.controller.auth.DangNhapBUS;
import cinema.controller.auth.PhienDangNhap;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import javax.swing.*;

public final class DangNhapWindow extends JFrame {
    private final JTextField username = new JTextField(22);
    private final JPasswordField password = new JPasswordField(22);
    private final JButton submit = new JButton("Đăng nhập");
    private final JLabel message = new JLabel("Nhập tài khoản đã được cấp để đăng nhập.");

    public DangNhapWindow(DangNhapBUS service) {
        super("Đăng nhập — Quản lý rạp chiếu phim");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel fields = new JPanel(new GridLayout(0, 1, 6, 6));
        fields.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        fields.add(new JLabel("Tên đăng nhập"));
        fields.add(username);
        fields.add(new JLabel("Mật khẩu"));
        fields.add(password);
        fields.add(submit);
        add(fields, BorderLayout.CENTER);
        add(message, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(submit);
        submit.addActionListener(event -> authenticate(service));
        setSize(640, 310);
        setLocationRelativeTo(null);
    }

    private void authenticate(DangNhapBUS service) {
        String name = username.getText();
        char[] secret = password.getPassword();
        submit.setEnabled(false);
        username.setEnabled(false);
        password.setEnabled(false);
        message.setText("Đang kiểm tra thông tin đăng nhập...");
        new SwingWorker<PhienDangNhap, Void>() {
            @Override
            protected PhienDangNhap doInBackground() throws Exception {
                try { return service.login(name, secret); }
                finally { Arrays.fill(secret, '\0'); }
            }

            @Override
            protected void done() {
                try {
                    PhienDangNhap session = get();
                    CinemaWindow.open(session);
                    dispose();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    message.setText("Đăng nhập bị gián đoạn. Vui lòng thử lại.");
                } catch (ExecutionException e) {
                    message.setText(e.getCause() instanceof IllegalArgumentException
                            ? e.getCause().getMessage()
                            : "Không thể đăng nhập. Vui lòng kiểm tra cấu hình và kết nối CSDL.");
                } finally {
                    password.setText("");
                    submit.setEnabled(true);
                    username.setEnabled(true);
                    password.setEnabled(true);
                }
            }
        }.execute();
    }
}
