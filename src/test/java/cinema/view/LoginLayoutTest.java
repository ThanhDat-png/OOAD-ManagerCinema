package cinema.view;
import cinema.controller.auth.DangNhapBUS;
import cinema.dao.TaiKhoanDAO;
import cinema.model.TaiKhoan;
import cinema.view.ui.*;
import java.awt.*;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LoginLayoutTest {
    @Test void loginHasReadableFieldsDefaultEnterAndClearFailureInBothThemes()throws Exception {
        DangNhapWindow[] window=new DangNhapWindow[1];SwingUtilities.invokeAndWait(()->{window[0]=new DangNhapWindow(new DangNhapBUS(new TaiKhoanDAO(){@Override public Optional<TaiKhoan> findByUsername(String name){return Optional.empty();}}));window[0].setSize(1024,768);window[0].setVisible(true);for(Component c:Design.all(window[0]))if(c instanceof JPasswordField p)p.setText("wrong");else if(c instanceof JTextField f)f.setText("missing");assertEquals("Đăng nhập",window[0].getRootPane().getDefaultButton().getText());window[0].getRootPane().getDefaultButton().doClick();});
        assertTimeoutPreemptively(Duration.ofSeconds(10),()->{AtomicBoolean done=new AtomicBoolean();while(!done.get()){SwingUtilities.invokeAndWait(()->done.set(window[0].getRootPane().getDefaultButton().isEnabled()));Thread.sleep(20);}});
        SwingUtilities.invokeAndWait(()->{assertTrue(window[0].isShowing());assertTrue(Design.all(window[0]).stream().filter(c->c instanceof JLabel).map(c->((JLabel)c).getText()).anyMatch(s->s!=null&&s.contains("không chính xác")));capture(window[0],"login-dark.png");Theme.toggle();capture(window[0],"login-light.png");for(Component c:Design.all(window[0]))if(c instanceof JTextField field)assertTrue(field.getWidth()>200);window[0].dispose();if(!Tokens.dark)Theme.toggle();});
    }
    private static void capture(Window window,String name){try{var dir=java.nio.file.Path.of("target","ui-redesign","sprint5");java.nio.file.Files.createDirectories(dir);var image=new java.awt.image.BufferedImage(window.getWidth(),window.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();window.paint(g);g.dispose();javax.imageio.ImageIO.write(image,"png",dir.resolve(name).toFile());}catch(java.io.IOException e){throw new RuntimeException(e);}}
}
