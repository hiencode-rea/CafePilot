import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BanHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            HttpUtil.guiJson(ex, 405, "Chỉ hỗ trợ GET");
            return;
        }

        // Query param tuy chon: /api/ban?trangthai=Trong
        String locTrangThai = HttpUtil.layQueryParam(ex, "trangthai");

        String sql = "SELECT b.MaBan, k.TenKV, b.SoHieuBan, b.TrangThai, b.SucChua, " +
                     "ISNULL(b.ViTriX, -1) AS ViTriX, ISNULL(b.ViTriY, -1) AS ViTriY, " +
                     "ISNULL(d.SoNguoi, 0) AS SoNguoi, ISNULL(d.TongTien, 0) AS TongTien, d.ThoiGianTao " +
                     "FROM Ban b JOIN KhuVuc k ON b.MaKV = k.MaKV " +
                     "OUTER APPLY (SELECT TOP 1 SoNguoi, TongTien, ThoiGianTao FROM DonHang " +
                     "             WHERE MaBan = b.MaBan AND TrangThai = N'DangXuLy' " +
                     "             ORDER BY ThoiGianTao DESC) d " +
                     (locTrangThai != null ? "WHERE b.TrangThai = ? " : "") +
                     "ORDER BY k.TenKV, b.SoHieuBan";

        List<Map<String, Object>> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (locTrangThai != null) stmt.setString(1, locTrangThai);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> ban = new HashMap<>();
                    ban.put("maBan", rs.getInt("MaBan"));
                    ban.put("tenKhuVuc", rs.getString("TenKV"));
                    ban.put("soHieuBan", rs.getString("SoHieuBan"));
                    ban.put("trangThai", rs.getString("TrangThai"));
                    ban.put("sucChua", rs.getInt("SucChua"));
                    ban.put("viTriX", rs.getDouble("ViTriX"));
                    ban.put("viTriY", rs.getDouble("ViTriY"));
                    ban.put("soNguoi", rs.getInt("SoNguoi"));
                    ban.put("tongTien", rs.getDouble("TongTien"));
                    java.sql.Timestamp thoiGian = rs.getTimestamp("ThoiGianTao");
                    ban.put("thoiGianTao", thoiGian == null ? null :
                            new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).format(thoiGian));
                    list.add(ban);
                }
            }
            HttpUtil.guiJson(ex, 200, list);

        } catch (SQLException e) {
            HttpUtil.guiJson(ex, 500, "Lỗi CSDL: " + e.getMessage());
        }
    }
}
