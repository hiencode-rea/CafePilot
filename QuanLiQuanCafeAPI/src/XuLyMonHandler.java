import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** API hàng đợi món cho Bếp và Pha chế. */
public class XuLyMonHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            if ("GET".equalsIgnoreCase(ex.getRequestMethod()) && "/api/xulymon".equals(path)) {
                layDanhSach(ex);
            } else if ("POST".equalsIgnoreCase(ex.getRequestMethod()) && path.endsWith("/trangthai")) {
                capNhatTrangThai(ex);
            } else {
                HttpUtil.guiJson(ex, 404, loi("Đường dẫn không hợp lệ."));
            }
        } catch (Exception e) {
            HttpUtil.guiJson(ex, 500, loi("Lỗi máy chủ: " + e.getMessage()));
        }
    }

    private void layDanhSach(HttpExchange ex) throws Exception {
        String khuVuc = HttpUtil.layQueryParam(ex, "khuVuc");
        if (!"BEP".equalsIgnoreCase(khuVuc) && !"PHA_CHE".equalsIgnoreCase(khuVuc)) {
            HttpUtil.guiJson(ex, 400, loi("khuVuc phải là BEP hoặc PHA_CHE."));
            return;
        }
        String sql = "SELECT ct.MaDH, dh.MaBan, b.SoHieuBan, ct.MaSP, sp.TenSP, ct.SoLuong, " +
                "ISNULL(ct.GhiChu, '') AS GhiChu, ct.TrangThaiMon, dh.ThoiGianTao " +
                "FROM ChiTietDonHang ct " +
                "JOIN DonHang dh ON dh.MaDH = ct.MaDH " +
                "JOIN Ban b ON b.MaBan = dh.MaBan " +
                "JOIN SanPham sp ON sp.MaSP = ct.MaSP " +
                "WHERE dh.TrangThai = N'DangXuLy' AND ct.DaThongBaoBep = 1 " +
                "AND (CASE WHEN NULLIF(LTRIM(RTRIM(sp.LoaiXuLy)), N'') IS NOT NULL THEN sp.LoaiXuLy " +
                "         WHEN LOWER(ISNULL(sp.NhomSP, N'')) LIKE N'%nuoc%' " +
                "           OR LOWER(ISNULL(sp.NhomSP, N'')) LIKE N'%nước%' " +
                "           OR LOWER(ISNULL(sp.NhomSP, N'')) LIKE N'%cafe%' " +
                "           OR LOWER(ISNULL(sp.NhomSP, N'')) LIKE N'%cà phê%' " +
                "           OR LOWER(ISNULL(sp.NhomSP, N'')) LIKE N'%tra%' " +
                "           OR LOWER(ISNULL(sp.NhomSP, N'')) LIKE N'%trà%' THEN N'PHA_CHE' " +
                "         ELSE N'BEP' END) = ? " +
                "AND ct.TrangThaiMon IN (N'CHUA_XU_LY', N'CHO_LAM', N'DANG_LAM') " +
                "ORDER BY dh.ThoiGianTao, ct.MaDH, ct.MaSP";

        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, khuVuc.toUpperCase());
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("maDH", rs.getInt("MaDH"));
                    m.put("maBan", rs.getInt("MaBan"));
                    m.put("soHieuBan", rs.getString("SoHieuBan"));
                    m.put("maSP", rs.getInt("MaSP"));
                    m.put("tenSP", rs.getString("TenSP"));
                    m.put("soLuong", rs.getInt("SoLuong"));
                    m.put("ghiChu", rs.getString("GhiChu"));
                    m.put("trangThaiMon", rs.getString("TrangThaiMon"));
                    m.put("thoiGianTao", rs.getTimestamp("ThoiGianTao"));
                    list.add(m);
                }
            }
        }
        HttpUtil.guiJson(ex, 200, list);
    }

    private void capNhatTrangThai(HttpExchange ex) throws Exception {
        JsonObject req = HttpUtil.GSON.fromJson(HttpUtil.docBody(ex), JsonObject.class);
        int maDH = req.get("maDH").getAsInt();
        int maSP = req.get("maSP").getAsInt();
        String trangThai = req.get("trangThaiMon").getAsString().toUpperCase();
        if (!("CHO_LAM".equals(trangThai) || "DANG_LAM".equals(trangThai)
                || "DA_RA_MON".equals(trangThai) || "HET_MON".equals(trangThai))) {
            HttpUtil.guiJson(ex, 400, loi("Trạng thái không hợp lệ."));
            return;
        }
        String sql = "UPDATE ChiTietDonHang SET TrangThaiMon = ? WHERE MaDH = ? AND MaSP = ? AND DaThongBaoBep = 1";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, trangThai);
            st.setInt(2, maDH);
            st.setInt(3, maSP);
            if (st.executeUpdate() == 0) {
                HttpUtil.guiJson(ex, 404, loi("Không tìm thấy món đã được gửi xử lý."));
                return;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("thanhCong", true);
        result.put("thongBao", "Đã cập nhật trạng thái món thành " + tenTrangThai(trangThai) + ".");
        HttpUtil.guiJson(ex, 200, result);
    }

    private String tenTrangThai(String trangThai) {
        switch (trangThai) {
            case "CHO_LAM": return "Chờ làm";
            case "DANG_LAM": return "Đang làm";
            case "DA_RA_MON": return "Đã ra món";
            case "HET_MON": return "Hết món";
            default: return trangThai;
        }
    }

    private Map<String, Object> loi(String message) {
        Map<String, Object> m = new HashMap<>();
        m.put("thanhCong", false);
        m.put("thongBao", message);
        return m;
    }
}