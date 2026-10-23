import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DangNhapHandler implements HttpHandler {

    // Vai tro duoc phep dung app Mobile (Desktop la QuanLy/ThuNgan)
    private static final String[] VAI_TRO_MOBILE = { "PhaChe", "PhucVu", "Bep" };

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            HttpUtil.guiJson(ex, 405, loi("Chỉ hỗ trợ POST"));
            return;
        }

        String body = HttpUtil.docBody(ex);
        JsonObject req = HttpUtil.GSON.fromJson(body, JsonObject.class);
        String tenDangNhap = req.get("tenDangNhap").getAsString();
        String matKhau = req.get("matKhau").getAsString();

        String sql = "SELECT tk.VaiTro, nv.MaNV, nv.HoTen " +
                     "FROM TaiKhoan tk JOIN NhanVien nv ON nv.MaTK = tk.MaTK " +
                     "WHERE tk.TenDangNhap = ? AND tk.MatKhau = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, tenDangNhap);
            stmt.setString(2, matKhau);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    HttpUtil.guiJson(ex, 200, ketQua(false, "Sai tên đăng nhập hoặc mật khẩu.", null, null, null));
                    return;
                }

                String vaiTro = rs.getString("VaiTro");
                if (!coQuyenMobile(vaiTro)) {
                    HttpUtil.guiJson(ex, 200, ketQua(false, "Vai trò '" + vaiTro + "' vui lòng dùng ứng dụng Desktop.", null, null, null));
                    return;
                }

                HttpUtil.guiJson(ex, 200, ketQua(true, "Đăng nhập thành công", vaiTro, rs.getInt("MaNV"), rs.getString("HoTen")));
            }

        } catch (SQLException e) {
            HttpUtil.guiJson(ex, 500, loi("Lỗi CSDL: " + e.getMessage()));
        }
    }

    private boolean coQuyenMobile(String vaiTro) {
        for (String vt : VAI_TRO_MOBILE) if (vt.equalsIgnoreCase(vaiTro)) return true;
        return false;
    }

    private Map<String, Object> ketQua(boolean thanhCong, String thongBao, String vaiTro, Integer maNV, String hoTen) {
        Map<String, Object> m = new HashMap<>();
        m.put("thanhCong", thanhCong);
        m.put("thongBao", thongBao);
        if (vaiTro != null) m.put("vaiTro", vaiTro);
        if (maNV != null) m.put("maNV", maNV);
        if (hoTen != null) m.put("hoTen", hoTen);
        return m;
    }

    private Map<String, Object> loi(String noiDung) {
        Map<String, Object> m = new HashMap<>();
        m.put("thanhCong", false);
        m.put("thongBao", noiDung);
        return m;
    }
}
