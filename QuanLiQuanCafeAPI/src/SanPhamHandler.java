import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SanPhamHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            HttpUtil.guiJson(ex, 405, "Chỉ hỗ trợ GET");
            return;
        }

        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT MaSP, TenSP, NhomSP, GiaBan, LoaiXuLy FROM SanPham ORDER BY MaSP";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> sp = new HashMap<>();
                sp.put("maSP", rs.getInt("MaSP"));
                sp.put("tenSP", rs.getString("TenSP"));
                sp.put("nhomSP", rs.getString("NhomSP"));
                sp.put("giaBan", rs.getDouble("GiaBan"));
                sp.put("loaiXuLy", rs.getString("LoaiXuLy"));
                list.add(sp);
            }
            HttpUtil.guiJson(ex, 200, list);

        } catch (SQLException e) {
            HttpUtil.guiJson(ex, 500, "Lỗi CSDL: " + e.getMessage());
        }
    }
}
