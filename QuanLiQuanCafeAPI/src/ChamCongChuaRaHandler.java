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

// GET /api/chamcongchuara?tenDangNhap=nv01
// Tra ve danh sach ban ghi cham cong DA VAO nhung CHUA RA cua nhan vien do (de chon cham cong RA)
public class ChamCongChuaRaHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            HttpUtil.guiJson(ex, 405, "Chỉ hỗ trợ GET");
            return;
        }

        String tenDangNhap = HttpUtil.layQueryParam(ex, "tenDangNhap");
        if (tenDangNhap == null) {
            HttpUtil.guiJson(ex, 400, "Thiếu tham số tenDangNhap");
            return;
        }

        String sql = "SELECT cc.MaCC, c.Ngay, c.GioBatDau, c.GioKetThuc " +
                     "FROM ChamCong cc " +
                     "JOIN CaLamViec c ON cc.MaCa = c.MaCa " +
                     "JOIN NhanVien nv ON c.MaNV = nv.MaNV " +
                     "JOIN TaiKhoan tk ON nv.MaTK = tk.MaTK " +
                     "WHERE tk.TenDangNhap = ? AND cc.GioRa IS NULL " +
                     "ORDER BY c.Ngay DESC";

        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, tenDangNhap);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> cc = new HashMap<>();
                    cc.put("maCC", rs.getInt("MaCC"));
                    cc.put("ngay", rs.getDate("Ngay").toString());
                    cc.put("gioBatDau", rs.getTime("GioBatDau").toString().substring(0, 5));
                    cc.put("gioKetThuc", rs.getTime("GioKetThuc").toString().substring(0, 5));
                    list.add(cc);
                }
            }
            HttpUtil.guiJson(ex, 200, list);

        } catch (SQLException e) {
            HttpUtil.guiJson(ex, 500, "Lỗi CSDL: " + e.getMessage());
        }
    }
}
