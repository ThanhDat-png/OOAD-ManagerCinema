package cinema.controller.film;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public final class PosterStore {
    private static final int MAX_BYTES = 10 * 1024 * 1024;
    private final Path directory;
    public PosterStore() { this(Path.of("data", "posters")); }
    public PosterStore(Path directory) { this.directory = directory; }

    private record ImageData(byte[] bytes, String format, BufferedImage image) { }

    private ImageData read(Path source) {
        try (InputStream input = Files.newInputStream(source)) {
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) throw new IOException("Image too large");
            try (ImageInputStream imageInput = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
                if (!readers.hasNext()) throw new IOException("Unknown image");
                ImageReader reader = readers.next();
                try {
                    reader.setInput(imageInput);
                    String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    if (!format.equals("png") && !format.equals("jpeg")) throw new IOException("Unsupported image");
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width <= 0 || height <= 0 || (long) width * height > 16_000_000)
                        throw new IOException("Image dimensions too large");
                    return new ImageData(bytes, format.equals("jpeg") ? "jpg" : "png", reader.read(0));
                } finally { reader.dispose(); }
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Ảnh không hợp lệ. Vui lòng chọn ảnh khác", e);
        }
    }

    public BufferedImage preview(Path source) {
        if (source == null) throw new IllegalArgumentException("Vui lòng chọn poster phim");
        return read(source).image();
    }

    public String importImage(Path source) throws IOException {
        if (source == null) throw new IllegalArgumentException("Vui lòng chọn poster phim");
        ImageData data = read(source);
        Files.createDirectories(directory);
        Path target = directory.resolve(UUID.randomUUID() + "." + data.format());
        Files.write(target, data.bytes(), StandardOpenOption.CREATE_NEW);
        return target.toString().replace(File.separatorChar, '/');
    }

    public BufferedImage load(String stored) {
        Path path = owned(stored);
        return read(path).image();
    }

    public void discard(String stored) throws IOException { Files.deleteIfExists(owned(stored)); }

    private Path owned(String stored) {
        if (stored == null) throw new IllegalArgumentException("Chưa có poster");
        Path target = Path.of(stored).toAbsolutePath().normalize();
        Path base = directory.toAbsolutePath().normalize();
        if (!base.equals(target.getParent()) || !target.getFileName().toString().matches("[a-f0-9-]{36}\\.(png|jpg)"))
            throw new IllegalArgumentException("Đường dẫn poster không hợp lệ");
        return target;
    }
}
