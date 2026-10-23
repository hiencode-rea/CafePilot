import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

// POST /api/donhang
// Body: { "maBan": 1, "tenDangNhap": "nv01", "chiTiet": [ {"maSP":1,"soLuong":2}, ... ] }
public class DonHangHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            HttpUtil.guiJson(ex, 405, loi("Chi ho tro POST"));
            return;
        }

        String body = HttpUtil.docBody(ex);
        JsonObject req = HttpUtil.GSON.fromJson(body, JsonObject.class);
        int maBan = req.get("maBan").getAsInt();
        String tenDangNhap = req.get("tenDangNhap").getAsString();
        JsonArray chiTiet = req.getAsJsonArray("chiTiet");

        if (chiTiet == null || chiTiet.size() == 0) {
            HttpUtil.guiJson(ex, 200, loi("Don hang chua co san pham nao."));
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {

            // 1. Lay MaNV tu tenDangNhap
            int maNV;
            String sqlNV = "SELECT nv.MaNV FROM NhanVien nv JOIN TaiKhoan tk ON nv.MaTK = tk.MaTK WHERE tk.TenDangNhap = ?";
            try (PreparedStatement s1 = conn.prepareStatement(sqlNV)) {
                s1.setString(1, tenDangNhap);
                try (ResultSet rs = s1.executeQuery()) {
                    if (!rs.next()) { HttpUtil.guiJson(ex, 200, loi("Khong tim thay nhan vien.")); return; }
                    maNV = rs.getInt("MaNV");
                }
            }

            // 2. Tao don hang
            int maDH;
            String sqlDH = "INSERT INTO DonHang (MaBan, MaNV, TrangThai, TongTien) VALUES (?, ?, N'DangXuLy', 0)";
            try (PreparedStatement s2 = conn.prepareStatement(sqlDH, Statement.RETURN_GENERATED_KEYS)) {
                s2.setInt(1, maBan);
                s2.setInt(2, maNV);
                s2.executeUpdate();
                try (ResultSet rs = s2.getGeneratedKeys()) {
                    rs.next();
                    maDH = rs.getInt(1);
                }
            }

            // 3. Them chi tiet don hang (lay don gia hien tai cua san pham)
            String sqlGiaSP = "SELECT GiaBan FROM SanPham WHERE MaSP = ?";
            String sqlKiemTraDaCo = "SELECT SoLuong FROM ChiTietDonHang WHERE MaDH = ? AND MaSP = ?";
            String sqlCapNhatSoLuong = "UPDATE ChiTietDonHang SET SoLuong = SoLuong + ? WHERE MaDH = ? AND MaSP = ?";
            String sqlChiTiet = "INSERT INTO ChiTietDonHang (MaDH, MaSP, SoLuong, DonGia) VALUES (?, ?, ?, ?)";

            for (int i = 0; i < chiTiet.size(); i++) {
                JsonObject dong = chiTiet.get(i).getAsJsonObject();
                int maSP = dong.get("maSP").getAsInt();
                int soLuong = dong.get("soLuong").getAsInt();

                double donGia;
                try (PreparedStatement sg = conn.prepareStatement(sqlGiaSP)) {
                    sg.setInt(1, maSP);
                    try (ResultSet rs = sg.executeQuery()) {
                        rs.next();
                        donGia = rs.getDouble("GiaBan");
                    }
                }

                // Neu san pham nay da co dong trong don hang thi cong don so luong,
                // tranh loi trung khoa chinh (MaDH, MaSP) khi client gui trung san pham
                boolean daCoDong;
                try (PreparedStatement sk = conn.prepareStatement(sqlKiemTraDaCo)) {
                    sk.setInt(1, maDH);
                    sk.setInt(2, maSP);
                    try (ResultSet rs = sk.executeQuery()) {
                        daCoDong = rs.next();
                    }
                }

                if (daCoDong) {
                    try (PreparedStatement sc = conn.prepareStatement(sqlCapNhatSoLuong)) {
                        sc.setInt(1, soLuong);
                        sc.setInt(2, maDH);
                        sc.setInt(3, maSP);
                        sc.executeUpdate();
                    }
                } else {
                    try (PreparedStatement sc = conn.prepareStatement(sqlChiTiet)) {
                        sc.setInt(1, maDH);
                        sc.setInt(2, maSP);
                        sc.setInt(3, soLuong);
                        sc.setDouble(4, donGia);
                        sc.executeUpdate();
                    }
                }
            }

            // 4. Cap nhat tong tien don hang
            try (PreparedStatement s4 = conn.prepareStatement(
                    "UPDATE DonHang SET TongTien = (SELECT ISNULL(SUM(SoLuong*DonGia),0) FROM ChiTietDonHang WHERE MaDH=?) WHERE MaDH=?")) {
                s4.setInt(1, maDH);
                s4.setInt(2, maDH);
                s4.executeUpdate();
            }

            // 5. Cap nhat trang thai ban
            try (PreparedStatement s5 = conn.prepareStatement("UPDATE Ban SET TrangThai = N'DangPhucVu' WHERE MaBan = ?")) {
                s5.setInt(1, maBan);
                s5.executeUpdate();
            }

            Map<String, Object> ketQua = new HashMap<>();
            ketQua.put("thanhCong", true);
            ketQua.put("thongBao", "Tao don hang thanh cong");
            ketQua.put("maDH", maDH);
            HttpUtil.guiJson(ex, 200, ketQua);

        } catch (SQLException e) {
            HttpUtil.guiJson(ex, 500, loi("Loi CSDL: " + e.getMessage()));
        }
    }

    private Map<String, Object> loi(String noiDung) {
        Map<String, Object> m = new HashMap<>();
        m.put("thanhCong", false);
        m.put("thongBao", noiDung);
        return m;
    }
}
