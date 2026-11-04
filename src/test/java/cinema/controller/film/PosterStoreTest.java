package cinema.controller.film;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PosterStoreTest {
    @TempDir Path temporary;

    @Test void copiesReadablePngAndJpegAndLoadsIndependentOfSource() throws Exception {
        PosterStore store = new PosterStore(temporary.resolve("owned"));
        for (String format : new String[]{"png", "jpeg"}) {
            Path source = temporary.resolve("source." + format);
            ImageIO.write(new BufferedImage(30, 45, BufferedImage.TYPE_INT_RGB), format, source.toFile());
            String imported = store.importImage(source);
            Files.delete(source);
            assertEquals(30, store.load(imported).getWidth());
            store.discard(imported);
            assertFalse(Files.exists(Path.of(imported)));
        }
    }

    @Test void rejectsTextUnsupportedAndOversizedImagesWithoutWritingPoster() throws Exception {
        PosterStore store = new PosterStore(temporary.resolve("owned"));
        Path text = temporary.resolve("fake.png"); Files.writeString(text, "not an image");
        assertThrows(IllegalArgumentException.class, () -> store.importImage(text));
        Path gif = temporary.resolve("unsupported.gif");
        ImageIO.write(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), "gif", gif.toFile());
        assertThrows(IllegalArgumentException.class, () -> store.preview(gif));
        Path huge = temporary.resolve("huge.png"); Files.write(huge, new byte[10 * 1024 * 1024 + 1]);
        assertThrows(IllegalArgumentException.class, () -> store.importImage(huge));
        assertFalse(Files.exists(temporary.resolve("owned")));
    }

    @Test void rejectsPathsOutsideOwnedDirectory() throws Exception {
        PosterStore store = new PosterStore(temporary.resolve("owned"));
        Path unrelated = temporary.resolve("keep.txt"); Files.writeString(unrelated, "keep");
        assertThrows(IllegalArgumentException.class, () -> store.discard(unrelated.toString()));
        assertThrows(IllegalArgumentException.class, () -> store.load("../outside.png"));
        assertTrue(Files.exists(unrelated));
    }
}
