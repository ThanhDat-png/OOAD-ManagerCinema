package cinema.controller.admission;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;

public record ThoiGianVaoPhong(int truocPhut,int sauPhut) {
    public ThoiGianVaoPhong {
        if(truocPhut<0 || sauPhut<0 || truocPhut>1440 || sauPhut>1440)
            throw new IllegalArgumentException("Cấu hình vào phòng cần từ 0 đến 1440 phút mỗi phía");
    }
    public static ThoiGianVaoPhong load() {
        Path path=Path.of("config","admission.properties");
        if(!Files.exists(path)) return new ThoiGianVaoPhong(15,15);
        Properties values=new Properties();
        try(Reader reader=Files.newBufferedReader(path,StandardCharsets.UTF_8)) {
            values.load(reader);
            return new ThoiGianVaoPhong(Integer.parseInt(values.getProperty("beforeMinutes","15").trim()),Integer.parseInt(values.getProperty("afterMinutes","15").trim()));
        } catch(IOException|NumberFormatException e) { throw new IllegalArgumentException("Cấu hình vào phòng không hợp lệ",e); }
    }
}
