package cinema.controller.admission;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.nio.file.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public final class DocMaQR {
    private DocMaQR() { }
    public static String read(Path file) {
        try {
            if(file==null || Files.size(file)>10*1024*1024) throw new IllegalArgumentException();
            try(ImageInputStream input=ImageIO.createImageInputStream(file.toFile())) {
                Iterator<ImageReader> readers=ImageIO.getImageReaders(input);
                if(!readers.hasNext()) throw new IllegalArgumentException();
                ImageReader reader=readers.next();
                try {
                    reader.setInput(input);
                    if((long)reader.getWidth(0)*reader.getHeight(0)>16_000_000) throw new IllegalArgumentException();
                    var bitmap=new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(reader.read(0))));
                    String result=new MultiFormatReader().decode(bitmap,Map.of(DecodeHintType.POSSIBLE_FORMATS,List.of(BarcodeFormat.QR_CODE),DecodeHintType.TRY_HARDER,true)).getText();
                    if(result==null || result.isBlank() || result.length()>100) throw new IllegalArgumentException();
                    return result;
                } finally { reader.dispose(); }
            }
        } catch(Exception e) { throw new IllegalArgumentException("Không đọc được mã QR. Vui lòng quét lại hoặc nhập mã vé thủ công",e); }
    }
}
