import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class HttpUtil {

    public static final Gson GSON = new Gson();

    // Doc toan bo body cua request (dung cho POST) thanh chuoi
    public static String docBody(HttpExchange ex) throws IOException {
        InputStream is = ex.getRequestBody();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int n;
        while ((n = is.read(data)) != -1) buffer.write(data, 0, n);
        return buffer.toString(StandardCharsets.UTF_8.name());
    }

    // Gui response JSON ve cho client
    public static void guiJson(HttpExchange ex, int statusCode, Object obj) throws IOException {
        String json = GSON.toJson(obj);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    // Lay gia tri 1 tham so tren query string, vd: /api/ban?trangthai=Trong
    public static String layQueryParam(HttpExchange ex, String ten) {
        String query = ex.getRequestURI().getQuery();
        if (query == null) return null;
        for (String cap : query.split("&")) {
            String[] phan = cap.split("=", 2);
            if (phan.length == 2 && phan[0].equals(ten)) {
                return java.net.URLDecoder.decode(phan[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
