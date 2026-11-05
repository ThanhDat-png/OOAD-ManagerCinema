package cinema.view;

import cinema.view.ui.*;

import cinema.controller.auth.DangNhapBUS;
import cinema.controller.auth.PhienDangNhap;
import java.awt.*;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import javax.swing.*;

public final class DangNhapWindow extends JFrame {
    static { Theme.install(); }
    private final JTextField username = new JTextField(22);
    private final JPasswordField password = new JPasswordField(22);
    private final JButton submit = new JButton("Đăng nhập");
    private final JLabel message = new Design.WrappingLabel("Nhập tài khoản đã được cấp để đăng nhập.");

    public DangNhapWindow(DangNhapBUS service) {
        super("Đăng nhập — Quản lý rạp chiếu phim");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel stage=new JPanel(new BorderLayout());stage.putClientProperty("cinema.background","canvas");JPanel tools=new JPanel(new FlowLayout(FlowLayout.RIGHT));tools.putClientProperty("cinema.background","canvas");tools.setBorder(BorderFactory.createEmptyBorder(16,24,0,24));tools.add(Theme.toggleButton());stage.add(tools,BorderLayout.NORTH);
        JPanel center=new JPanel(new GridBagLayout());center.putClientProperty("cinema.background","canvas");Design.Card card=new Design.Card(new BorderLayout(16,24));card.setPreferredSize(new Dimension(480,470));JLabel brand=Design.text("CINEMA WORKSPACE","heading");brand.setIcon(new Icons("film",24));brand.setIconTextGap(12);card.add(Design.stack(brand,Design.text("Đăng nhập","title"),Design.text("Sử dụng tài khoản nội bộ đã được cấp.","subtitle")),BorderLayout.NORTH);
        JPanel fields=new JPanel(new GridBagLayout());GridBagConstraints c=new GridBagConstraints();c.gridx=0;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;c.anchor=GridBagConstraints.WEST;c.insets=new Insets(4,0,8,0);
        JLabel userLabel=Design.text("Tên đăng nhập","subtitle");userLabel.setLabelFor(username);c.gridy=0;fields.add(userLabel,c);c.gridy=1;fields.add(username,c);JLabel passwordLabel=Design.text("Mật khẩu","subtitle");passwordLabel.setLabelFor(password);c.gridy=2;fields.add(passwordLabel,c);JPanel secret=new JPanel(new BorderLayout(0,4));secret.add(password);JLabel required=Design.text("Vui lòng nhập mật khẩu.","error");required.setVisible(false);secret.add(required,BorderLayout.SOUTH);c.gridy=3;fields.add(secret,c);Forms.bind(username,"Tên đăng nhập");Forms.password(password,required);
        card.add(fields);submit.setMaximumSize(new Dimension(Integer.MAX_VALUE,Theme.s(40)));Design.status(message);card.add(Design.stack(message,submit),BorderLayout.SOUTH);center.add(card);stage.add(center);setContentPane(stage);Theme.apply(stage);SwingUtilities.invokeLater(()->username.requestFocusInWindow());
        getRootPane().setDefaultButton(submit);
        submit.addActionListener(event -> authenticate(service));
        setSize(960, 680);
        setMinimumSize(new Dimension(640,600));
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
