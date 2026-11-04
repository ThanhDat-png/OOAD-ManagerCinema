package cinema.controller.admission;

import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class DocMaQRTest {
    @TempDir Path temporary;
    @Test void decodesQrPayloadWithoutChangingCaseOrContent() throws Exception {
        Path file=temporary.resolve("qr.png");
        MatrixToImageWriter.writeToPath(new QRCodeWriter().encode("Qr_Demo_ABC123",BarcodeFormat.QR_CODE,320,320),"PNG",file);
        assertEquals("Qr_Demo_ABC123",DocMaQR.read(file));
    }
    @Test void rejectsImageWithoutQrAndUnreadableFiles() throws Exception {
        Path plain=temporary.resolve("plain.png"); ImageIO.write(new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB),"png",plain.toFile());
        assertThrows(IllegalArgumentException.class,() -> DocMaQR.read(plain));
        Path text=temporary.resolve("fake.png"); Files.writeString(text,"not an image");
        assertThrows(IllegalArgumentException.class,() -> DocMaQR.read(text));
        assertThrows(IllegalArgumentException.class,() -> DocMaQR.read(temporary.resolve("missing.png")));
    }
    @Test void rejectsOversizedPayloadAndFile() throws Exception {
        Path qr=temporary.resolve("large-payload.png"); MatrixToImageWriter.writeToPath(new QRCodeWriter().encode("x".repeat(101),BarcodeFormat.QR_CODE,400,400),"PNG",qr);
        assertThrows(IllegalArgumentException.class,() -> DocMaQR.read(qr));
        Path huge=temporary.resolve("huge.png"); Files.write(huge,new byte[10*1024*1024+1]);
        assertThrows(IllegalArgumentException.class,() -> DocMaQR.read(huge));
    }
}
