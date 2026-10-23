import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

// POST /api/chamcong/vao   Body: { "maCa": 3 }
// POST /api/chamcong/ra    Body: { "maCC": 5 }
public class ChamCongHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            HttpUtil.guiJson(ex, 405, ketQua(false, "Chỉ hỗ trợ POST"));
            return;
        }

        String duong = ex.getRequestURI().getPath(); // /api/chamcong/vao hoac /api/chamcong/ra
        String body = HttpUtil.docBody(ex);
        JsonObject req = HttpUtil.GSON.fromJson(body, JsonObject.class);

        try (Connection conn = DBConnection.getConnection()) {

            if (duong.endsWith("/vao")) {
                int maCa = req.get("maCa").getAsInt();
                String sql = "INSERT INTO ChamCong (MaCa, GioVao) VALUES (?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, maCa);
                    stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                    stmt.executeUpdate();
                }
                HttpUtil.guiJson(ex, 200, ketQua(true, "Chấm công vào thành công"));

            } else if (duong.endsWith("/ra")) {
                int maCC = req.get("maCC").getAsInt();
                String sql = "UPDATE ChamCong SET GioRa = ? WHERE MaCC = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    stmt.setInt(2, maCC);
                    stmt.executeUpdate();
                }
                HttpUtil.guiJson(ex, 200, ketQua(true, "Chấm công ra thành công"));

            } else {
                HttpUtil.guiJson(ex, 404, ketQua(false, "Đường dẫn không hợp lệ"));
            }

        } catch (SQLException e) {
            HttpUtil.guiJson(ex, 500, ketQua(false, "Lỗi CSDL: " + e.getMessage()));
        }
    }

    private Map<String, Object> ketQua(boolean thanhCong, String thongBao) {
        Map<String, Object> m = new HashMap<>();
        m.put("thanhCong", thanhCong);
        m.put("thongBao", thongBao);
        return m;
    }
}
