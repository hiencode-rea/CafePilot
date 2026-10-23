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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Gom cac endpoint lien quan man hinh "gop them mon + xem chi tiet" cho Mobile:
//   GET  /api/chitietdon?maBan=1              -> lay thong tin don dang mo + danh sach mon
//   POST /api/chitietdon/them                 -> them 1 mon vao don (tu tao don neu chua co)
//   POST /api/chitietdon/thongbaobep          -> danh dau tat ca mon la da bao bep
//   POST /api/chitietdon/xoamon               -> xoa 1 mon (chi khi CHUA bao bep)
//   POST /api/chitietdon/thanhtoan            -> hoan thanh don, tra ban ve Trong
public class ChiTietDonHangHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String duong = ex.getRequestURI().getPath();
        String phuongThuc = ex.getRequestMethod();

        try {
            if (duong.equals("/api/chitietdon") && "GET".equalsIgnoreCase(phuongThuc)) {
                xemChiTiet(ex);
            } else if (duong.endsWith("/them") && "POST".equalsIgnoreCase(phuongThuc)) {
                themMon(ex);
            } else if (duong.endsWith("/thongbaobep") && "POST".equalsIgnoreCase(phuongThuc)) {
                thongBaoBep(ex);
            } else if (duong.endsWith("/xoamon") && "POST".equalsIgnoreCase(phuongThuc)) {
                xoaMon(ex);
            } else if (duong.endsWith("/yeucauthanhtoan") && "POST".equalsIgnoreCase(phuongThuc)) {
                guiYeuCauThanhToan(ex);
            } else if (duong.endsWith("/huyyeucau") && "POST".equalsIgnoreCase(phuongThuc)) {
                huyYeuCau(ex);
            } else if (duong.endsWith("/capnhatsoinguoi") && "POST".equalsIgnoreCase(phuongThuc)) {
                capNhatSoNguoi(ex);
            } else if (duong.endsWith("/thanhtoan") && "POST".equalsIgnoreCase(phuongThuc)) {
                // Mobile KHÔNG được tự thanh toán: chỉ thu ngân/quản lý xác nhận trên Desktop
                HttpUtil.guiJson(ex, 200, loi("Mobile không được thanh toán trực tiếp. Hãy gửi yêu cầu thanh toán để thu ngân xác nhận trên Desktop."));
            } else {
                HttpUtil.guiJson(ex, 404, loi("Đường dẫn không hợp lệ"));
            }
        } catch (Exception e) {
            HttpUtil.guiJson(ex, 500, loi("Lỗi máy chủ: " + e.getMessage()));
        }
    }

    // ===== GET /api/chitietdon?maBan=1 =====
    private void xemChiTiet(HttpExchange ex) throws Exception {
        int maBan = Integer.parseInt(HttpUtil.layQueryParam(ex, "maBan"));

        try (Connection conn = DBConnection.getConnection()) {
            Integer maDH = null;
            double tongTien = 0;

            boolean yeuCau = false;
            String sqlDon = "SELECT MaDH, SoNguoi, TongTien, ThoiGianTao, YeuCauThanhToan FROM DonHang WHERE MaBan = ? AND TrangThai = N'DangXuLy' ORDER BY ThoiGianTao DESC";
            int soNguoi = 0;
            java.sql.Timestamp thoiGianTao = null;
            try (PreparedStatement s1 = conn.prepareStatement(sqlDon)) {
                s1.setInt(1, maBan);
                try (ResultSet rs = s1.executeQuery()) {
                    if (rs.next()) {
                        maDH = rs.getInt("MaDH");
                        soNguoi = rs.getInt("SoNguoi");
                        tongTien = rs.getDouble("TongTien");
                        thoiGianTao = rs.getTimestamp("ThoiGianTao");
                        yeuCau = rs.getBoolean("YeuCauThanhToan");
                    }
                }
            }

            List<Map<String, Object>> danhSachMon = new ArrayList<>();
            if (maDH != null) {
                String sqlCT = "SELECT ct.MaSP, sp.TenSP, ct.SoLuong, ct.DonGia, ct.DaThongBaoBep " +
                               ", ct.TrangThaiMon, sp.LoaiXuLy FROM ChiTietDonHang ct JOIN SanPham sp ON ct.MaSP = sp.MaSP WHERE ct.MaDH = ?";
                try (PreparedStatement s2 = conn.prepareStatement(sqlCT)) {
                    s2.setInt(1, maDH);
                    try (ResultSet rs = s2.executeQuery()) {
                        while (rs.next()) {
                            Map<String, Object> mon = new HashMap<>();
                            mon.put("maSP", rs.getInt("MaSP"));
                            mon.put("tenSP", rs.getString("TenSP"));
                            mon.put("soLuong", rs.getInt("SoLuong"));
                            mon.put("donGia", rs.getDouble("DonGia"));
                            mon.put("daThongBaoBep", rs.getBoolean("DaThongBaoBep"));
                            mon.put("trangThaiMon", rs.getString("TrangThaiMon"));
                            mon.put("loaiXuLy", rs.getString("LoaiXuLy"));
                            danhSachMon.add(mon);
                        }
                    }
                }
            }

            Map<String, Object> ketQua = new HashMap<>();
            ketQua.put("maDH", maDH);
            ketQua.put("soNguoi", soNguoi);
            ketQua.put("tongTien", tongTien);
            ketQua.put("thoiGianTao", thoiGianTao == null ? null :
                    new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).format(thoiGianTao));
            ketQua.put("yeuCauThanhToan", yeuCau);
            ketQua.put("danhSachMon", danhSachMon);
            HttpUtil.guiJson(ex, 200, ketQua);
        }
    }

    // ===== POST /api/chitietdon/capnhatsoinguoi Body: {maBan, soNguoi} =====
    private void capNhatSoNguoi(HttpExchange ex) throws Exception {
        JsonObject req = HttpUtil.GSON.fromJson(HttpUtil.docBody(ex), JsonObject.class);
        int maBan = req.get("maBan").getAsInt();
        int soNguoi = req.get("soNguoi").getAsInt();
        String tenDangNhap = req.has("tenDangNhap") ? req.get("tenDangNhap").getAsString() : null;
        if (soNguoi < 1) {
            HttpUtil.guiJson(ex, 200, loi("Số người phải lớn hơn 0."));
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            int sucChua;
            try (PreparedStatement st = conn.prepareStatement("SELECT SucChua FROM Ban WHERE MaBan = ?")) {
                st.setInt(1, maBan);
                try (ResultSet rs = st.executeQuery()) {
                    if (!rs.next()) { HttpUtil.guiJson(ex, 200, loi("Không tìm thấy bàn.")); return; }
                    sucChua = rs.getInt(1);
                }
            }
            if (soNguoi > sucChua) {
                HttpUtil.guiJson(ex, 200, loi("Số người không được vượt quá sức chứa " + sucChua + "."));
                return;
            }
            try (PreparedStatement st = conn.prepareStatement(
                    "UPDATE DonHang SET SoNguoi = ? WHERE MaDH = (SELECT TOP 1 MaDH FROM DonHang WHERE MaBan = ? AND TrangThai = N'DangXuLy' ORDER BY ThoiGianTao DESC)")) {
                st.setInt(1, soNguoi);
                st.setInt(2, maBan);
                if (st.executeUpdate() == 0) {
                    if (tenDangNhap == null || tenDangNhap.trim().isEmpty()) {
                        HttpUtil.guiJson(ex, 200, loi("Bàn chưa có đơn đang mở."));
                        return;
                    }
                    int maNV;
                    try (PreparedStatement nv = conn.prepareStatement(
                            "SELECT nv.MaNV FROM NhanVien nv JOIN TaiKhoan tk ON nv.MaTK = tk.MaTK WHERE tk.TenDangNhap = ?")) {
                        nv.setString(1, tenDangNhap);
                        try (ResultSet rs = nv.executeQuery()) {
                            if (!rs.next()) { HttpUtil.guiJson(ex, 200, loi("Không tìm thấy nhân viên.")); return; }
                            maNV = rs.getInt(1);
                        }
                    }
                    try (PreparedStatement mo = conn.prepareStatement(
                            "INSERT INTO DonHang (MaBan, MaNV, SoNguoi, TrangThai, TongTien) VALUES (?, ?, ?, N'DangXuLy', 0)")) {
                        mo.setInt(1, maBan);
                        mo.setInt(2, maNV);
                        mo.setInt(3, soNguoi);
                        mo.executeUpdate();
                    }
                    try (PreparedStatement ban = conn.prepareStatement("UPDATE Ban SET TrangThai = N'DangPhucVu' WHERE MaBan = ?")) {
                        ban.setInt(1, maBan);
                        ban.executeUpdate();
                    }
                }
            }
            HttpUtil.guiJson(ex, 200, ketQua(true, "Đã cập nhật số người", null));
        }
    }

    // ===== POST /api/chitietdon/them  Body: {maBan, tenDangNhap, maSP, soLuong} =====
    private void themMon(HttpExchange ex) throws Exception {
        JsonObject req = HttpUtil.GSON.fromJson(HttpUtil.docBody(ex), JsonObject.class);
        int maBan = req.get("maBan").getAsInt();
        String tenDangNhap = req.get("tenDangNhap").getAsString();
        int maSP = req.get("maSP").getAsInt();
        int soLuong = req.get("soLuong").getAsInt();
        String ghiChu = req.has("ghiChu") && !req.get("ghiChu").isJsonNull()
                ? req.get("ghiChu").getAsString() : null;
        int soNguoi = req.has("soNguoi") ? req.get("soNguoi").getAsInt() : 1;

        if (soLuong <= 0) {
            HttpUtil.guiJson(ex, 200, loi("Số lượng phải lớn hơn 0."));
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            double donGia;
            try (PreparedStatement s0 = conn.prepareStatement("SELECT GiaBan FROM SanPham WHERE MaSP=?")) {
                s0.setInt(1, maSP);
                try (ResultSet rs = s0.executeQuery()) {
                    if (!rs.next()) {
                        HttpUtil.guiJson(ex, 200, loi("Không tìm thấy sản phẩm."));
                        return;
                    }
                    donGia = rs.getDouble("GiaBan");
                }
            }

            Integer maDH = null;
            try (PreparedStatement s1 = conn.prepareStatement(
                    "SELECT MaDH FROM DonHang WHERE MaBan = ? AND TrangThai = N'DangXuLy'")) {
                s1.setInt(1, maBan);
                try (ResultSet rs = s1.executeQuery()) { if (rs.next()) maDH = rs.getInt("MaDH"); }
            }

            if (maDH != null && daGuiYeuCau(conn, maDH)) {
                HttpUtil.guiJson(ex, 200, loi("Đơn đã gửi yêu cầu thanh toán, không thể thêm món trên Mobile. Hãy hủy yêu cầu trước."));
                return;
            }

            if (maDH == null) {
                int maNV;
                try (PreparedStatement s2 = conn.prepareStatement(
                        "SELECT nv.MaNV FROM NhanVien nv JOIN TaiKhoan tk ON nv.MaTK=tk.MaTK WHERE tk.TenDangNhap=?")) {
                    s2.setString(1, tenDangNhap);
                    try (ResultSet rs = s2.executeQuery()) {
                        if (!rs.next()) { HttpUtil.guiJson(ex, 200, loi("Không tìm thấy nhân viên.")); return; }
                        maNV = rs.getInt("MaNV");
                    }
                }
                try (PreparedStatement s3 = conn.prepareStatement(
                        "INSERT INTO DonHang (MaBan, MaNV, SoNguoi, TrangThai, TongTien) VALUES (?,?,?,N'DangXuLy',0)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    s3.setInt(1, maBan);
                    s3.setInt(2, maNV);
                    s3.setInt(3, Math.max(1, soNguoi));
                    s3.executeUpdate();
                    try (ResultSet rs = s3.getGeneratedKeys()) { rs.next(); maDH = rs.getInt(1); }
                }
                try (PreparedStatement s4 = conn.prepareStatement("UPDATE Ban SET TrangThai = N'DangPhucVu' WHERE MaBan = ?")) {
                    s4.setInt(1, maBan);
                    s4.executeUpdate();
                }
            }

            // Tim dong cung MaSP va CHUA thong bao bep (DaThongBaoBep = 0) de cong don
            // Neu tat ca cac dong deu da bao bep -> tu dong tao dong moi
            boolean daCoDong = false;
            String ghiChuCu = null;
            int maCTTimDuoc = -1;
            try (PreparedStatement s6 = conn.prepareStatement(
                    "SELECT TOP 1 MaCT, GhiChu FROM ChiTietDonHang WHERE MaDH=? AND MaSP=? AND DaThongBaoBep=0 ORDER BY MaCT ASC")) {
                s6.setInt(1, maDH); s6.setInt(2, maSP);
                try (ResultSet rs = s6.executeQuery()) {
                    if (rs.next()) {
                        daCoDong = true;
                        maCTTimDuoc = rs.getInt("MaCT");
                        ghiChuCu = rs.getString("GhiChu");
                    }
                }
            }
            try (PreparedStatement sCheckHet = conn.prepareStatement(
                    "SELECT TOP 1 TrangThaiMon FROM ChiTietDonHang WHERE MaDH=? AND MaSP=? AND TrangThaiMon=N'HET_MON'")) {
                sCheckHet.setInt(1, maDH); sCheckHet.setInt(2, maSP);
                try (ResultSet rsHet = sCheckHet.executeQuery()) {
                    if (rsHet.next()) {
                        HttpUtil.guiJson(ex, 200, loi("M\u00f3n n\u00e0y \u0111\u00e3 h\u1ebft, vui l\u00f2ng ch\u1ecdn m\u00f3n thay th\u1ebf."));
                        return;
                    }
                }
            }
            if (daCoDong) {
                try (PreparedStatement s7 = conn.prepareStatement("UPDATE ChiTietDonHang SET SoLuong = SoLuong + ?, GhiChu = ? WHERE MaCT=?")) {
                    s7.setInt(1, soLuong);
                    s7.setString(2, gopGhiChu(ghiChuCu, ghiChu));
                    s7.setInt(3, maCTTimDuoc);
                    s7.executeUpdate();
                }
            } else {
                try (PreparedStatement s7 = conn.prepareStatement("INSERT INTO ChiTietDonHang (MaDH, MaSP, SoLuong, DonGia, GhiChu) VALUES (?,?,?,?,?)")) {
                    s7.setInt(1, maDH); s7.setInt(2, maSP); s7.setInt(3, soLuong); s7.setDouble(4, donGia);
                    s7.setString(5, (ghiChu == null || ghiChu.trim().isEmpty()) ? null : ghiChu.trim());
                    s7.executeUpdate();
                }
            }

            try (PreparedStatement s8 = conn.prepareStatement(
                    "UPDATE DonHang SET TongTien = (SELECT ISNULL(SUM(SoLuong*DonGia),0) FROM ChiTietDonHang WHERE MaDH=?) WHERE MaDH=?")) {
                s8.setInt(1, maDH); s8.setInt(2, maDH);
                s8.executeUpdate();
            }

            HttpUtil.guiJson(ex, 200, ketQua(true, "Đã thêm món", maDH));
        }
    }

    // ===== POST /api/chitietdon/thongbaobep  Body: {maDH} =====
    private void thongBaoBep(HttpExchange ex) throws Exception {
        JsonObject req = HttpUtil.GSON.fromJson(HttpUtil.docBody(ex), JsonObject.class);
        int maDH = req.get("maDH").getAsInt();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE ChiTietDonHang SET DaThongBaoBep = 1, TrangThaiMon = CASE WHEN TrangThaiMon IS NULL OR TrangThaiMon = N'CHUA_XU_LY' THEN N'CHO_LAM' ELSE TrangThaiMon END WHERE MaDH = ?")) {
            stmt.setInt(1, maDH);
            int soDong = stmt.executeUpdate();
            if (soDong == 0) {
                HttpUtil.guiJson(ex, 200, loi("Đơn chưa có món hoặc không tìm thấy đơn."));
                return;
            }
            // Sau khi thong bao bep, gom cac dong trung mon (cung MaSP) lai voi nhau neu chung deu da thong bao
            gopMonTrung(conn, maDH);

            Map<String, Object> result = ketQua(true, "Đã thông báo bếp", maDH);
            result.put("soDong", soDong);
            HttpUtil.guiJson(ex, 200, result);
        }
    }

    // ===== POST /api/chitietdon/xoamon  Body: {maDH, maSP} - chi cho phep neu CHUA bao bep =====
    private void xoaMon(HttpExchange ex) throws Exception {
        JsonObject req = HttpUtil.GSON.fromJson(HttpUtil.docBody(ex), JsonObject.class);
        int maDH = req.get("maDH").getAsInt();
        int maSP = req.get("maSP").getAsInt();

        try (Connection conn = DBConnection.getConnection()) {
            if (daGuiYeuCau(conn, maDH)) {
                HttpUtil.guiJson(ex, 200, loi("Đơn đã gửi yêu cầu thanh toán, không thể xóa món trên Mobile. Hãy hủy yêu cầu trước."));
                return;
            }
            boolean daBao;
            try (PreparedStatement s1 = conn.prepareStatement("SELECT DaThongBaoBep FROM ChiTietDonHang WHERE MaDH=? AND MaSP=?")) {
                s1.setInt(1, maDH); s1.setInt(2, maSP);
                try (ResultSet rs = s1.executeQuery()) {
                    if (!rs.next()) { HttpUtil.guiJson(ex, 200, loi("Không tìm thấy món.")); return; }
                    daBao = rs.getBoolean("DaThongBaoBep");
                }
            }

            if (daBao) {
                HttpUtil.guiJson(ex, 200, loi("Món đã báo bếp, không thể xóa trên Mobile."));
                return;
            }

            try (PreparedStatement s2 = conn.prepareStatement("DELETE FROM ChiTietDonHang WHERE MaDH=? AND MaSP=?")) {
                s2.setInt(1, maDH); s2.setInt(2, maSP);
                s2.executeUpdate();
            }
            try (PreparedStatement s3 = conn.prepareStatement(
                    "UPDATE DonHang SET TongTien = (SELECT ISNULL(SUM(SoLuong*DonGia),0) FROM ChiTietDonHang WHERE MaDH=?) WHERE MaDH=?")) {
                s3.setInt(1, maDH); s3.setInt(2, maDH);
                s3.executeUpdate();
            }

            // Neu don khong con mon nao: huy don va tra ban ve Trong (tranh ban ket o Dang phuc vu)
            int soMonConLai;
            try (PreparedStatement s4 = conn.prepareStatement("SELECT COUNT(*) FROM ChiTietDonHang WHERE MaDH=?")) {
                s4.setInt(1, maDH);
                try (ResultSet rs = s4.executeQuery()) { rs.next(); soMonConLai = rs.getInt(1); }
            }
            if (soMonConLai == 0) {
                try (PreparedStatement s5 = conn.prepareStatement(
                        "UPDATE Ban SET TrangThai = N'Trong' WHERE MaBan = (SELECT MaBan FROM DonHang WHERE MaDH = ?)")) {
                    s5.setInt(1, maDH);
                    s5.executeUpdate();
                }
                try (PreparedStatement s6 = conn.prepareStatement("UPDATE DonHang SET TrangThai = N'DaHuy' WHERE MaDH = ?")) {
                    s6.setInt(1, maDH);
                    s6.executeUpdate();
                }
            }
            HttpUtil.guiJson(ex, 200, ketQua(true, "Đã xóa món", maDH));
        }
    }

// Gom cac dong chi tiet cung MaSP va CUNG TrangThaiMon trong 1 don hang
    // Dieu kien: ca 2 dong deu da thong bao bep (DaThongBaoBep = 1)
    // Logic: lay dong dau tien lam goc, cong don SoLuong cua cac dong sau vao, roi xoa cac dong sau
    private void gopMonTrung(Connection conn, int maDH) throws SQLException {
        String sqlLayMon = "SELECT MaSP, ISNULL(TrangThaiMon, '') AS TTMon FROM ChiTietDonHang WHERE MaDH = ? GROUP BY MaSP, ISNULL(TrangThaiMon, '') HAVING COUNT(*) > 1";
        try (PreparedStatement ps = conn.prepareStatement(sqlLayMon)) {
            ps.setInt(1, maDH);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.List<Object[]> dsNhom = new java.util.ArrayList<>();
                while (rs.next()) dsNhom.add(new Object[]{rs.getInt("MaSP"), rs.getString("TTMon")});

                for (Object[] nhom : dsNhom) {
                    int maSP = (Integer) nhom[0];
                    String ttMon = (String) nhom[1];
                    // Lay tat ca dong cua mon nay co CUNG TrangThaiMon
                    String sqlChiTiet = "SELECT MaCT, SoLuong, GhiChu FROM ChiTietDonHang WHERE MaDH = ? AND MaSP = ? AND ISNULL(TrangThaiMon, '') = ? ORDER BY MaCT ASC";
                    try (PreparedStatement ps2 = conn.prepareStatement(sqlChiTiet)) {
                        ps2.setInt(1, maDH);
                        ps2.setInt(2, maSP);
                        ps2.setString(3, ttMon);
                        try (ResultSet rs2 = ps2.executeQuery()) {
                            int maCTGoc = -1;
                            int tongSoLuong = 0;
                            StringBuilder ghiChuGop = new StringBuilder();
                            List<Integer> dsCTXoa = new ArrayList<>();
                            int index = 0;

                            while (rs2.next()) {
                                if (index == 0) {
                                    maCTGoc = rs2.getInt("MaCT");
                                    tongSoLuong = rs2.getInt("SoLuong");
                                    String gc = rs2.getString("GhiChu");
                                    if (gc != null && !gc.isEmpty()) ghiChuGop.append(gc);
                                } else {
                                    tongSoLuong += rs2.getInt("SoLuong");
                                    String gc = rs2.getString("GhiChu");
                                    if (gc != null && !gc.isEmpty()) {
                                        if (ghiChuGop.length() > 0) ghiChuGop.append(" | ");
                                        ghiChuGop.append(gc);
                                    }
                                    dsCTXoa.add(rs2.getInt("MaCT"));
                                }
                                index++;
                            }

                            if (maCTGoc != -1 && !dsCTXoa.isEmpty()) {
                                // Cap nhat so luong va ghi chu cho dong goc
                                String sqlUpdate = "UPDATE ChiTietDonHang SET SoLuong = ?, GhiChu = ? WHERE MaCT = ?";
                                try (PreparedStatement psUpd = conn.prepareStatement(sqlUpdate)) {
                                    psUpd.setInt(1, tongSoLuong);
                                    psUpd.setString(2, ghiChuGop.toString());
                                    psUpd.setInt(3, maCTGoc);
                                    psUpd.executeUpdate();
                                }

                                // Xoa cac dong phu
                                for (int ctx : dsCTXoa) {
                                    String sqlXoa = "DELETE FROM ChiTietDonHang WHERE MaCT = ?";
                                    try (PreparedStatement psDel = conn.prepareStatement(sqlXoa)) {
                                        psDel.setInt(1, ctx);
                                        psDel.executeUpdate();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        // Noi 2 ghi chu voi nhau (bo qua ghi chu rong/null)
    private String gopGhiChu(String cu, String moi) {
        boolean cuRong = cu == null || cu.trim().isEmpty();
        boolean moiRong = moi == null || moi.trim().isEmpty();
        if (cuRong && moiRong) return "";
        if (cuRong) return moi;
        if (moiRong) return cu;
        if (cu.equals(moi)) return cu;
        return cu + " | " + moi;
    }

    // Kiểm tra đơn đã có yêu cầu thanh toán chưa
    private boolean daGuiYeuCau(Connection conn, int maDH) throws SQLException {
        try (PreparedStatement st = conn.prepareStatement("SELECT YeuCauThanhToan FROM DonHang WHERE MaDH = ?")) {
            st.setInt(1, maDH);
            try (ResultSet rs = st.executeQuery()) { return rs.next() && rs.getBoolean(1); }
        }
    }

    // ===== POST /api/chitietdon/yeucauthanhtoan  Body: {maBan} - gửi yêu cầu sang Desktop =====
    private void guiYeuCauThanhToan(HttpExchange ex) throws Exception {
        JsonObject req = HttpUtil.GSON.fromJson(HttpUtil.docBody(ex), JsonObject.class);
        int maBan = req.get("maBan").getAsInt();

        try (Connection conn = DBConnection.getConnection()) {
            Integer maDH = null;
            try (PreparedStatement s1 = conn.prepareStatement("SELECT MaDH FROM DonHang WHERE MaBan=? AND TrangThai=N'DangXuLy'")) {
                s1.setInt(1, maBan);
                try (ResultSet rs = s1.executeQuery()) { if (rs.next()) maDH = rs.getInt("MaDH"); }
            }
            if (maDH == null) { HttpUtil.guiJson(ex, 200, loi("Không có đơn đang mở cho bàn này.")); return; }

            try (PreparedStatement pending = conn.prepareStatement(
                    "SELECT COUNT(*) FROM ChiTietDonHang " +
                    "WHERE MaDH = ? AND (DaThongBaoBep = 0 OR TrangThaiMon NOT IN (N'DA_RA_MON', N'HET_MON'))")) {
                pending.setInt(1, maDH);
                try (ResultSet rs = pending.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        HttpUtil.guiJson(ex, 200, loi("Chưa thể yêu cầu thanh toán: vẫn còn món chưa ra món hoặc chưa gửi bếp."));
                        return;
                    }
                }
            }

            try (PreparedStatement s2 = conn.prepareStatement(
                    "UPDATE DonHang SET YeuCauThanhToan = 1, ThoiGianYeuCau = GETDATE() WHERE MaDH = ?")) {
                s2.setInt(1, maDH);
                s2.executeUpdate();
            }
            HttpUtil.guiJson(ex, 200, ketQua(true, "Đã gửi yêu cầu thanh toán đến thu ngân", maDH));
        }
    }

    // ===== POST /api/chitietdon/huyyeucau  Body: {maBan} - rút lại yêu cầu =====
    private void huyYeuCau(HttpExchange ex) throws Exception {
        JsonObject req = HttpUtil.GSON.fromJson(HttpUtil.docBody(ex), JsonObject.class);
        int maBan = req.get("maBan").getAsInt();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE DonHang SET YeuCauThanhToan = 0, ThoiGianYeuCau = NULL WHERE MaBan = ? AND TrangThai = N'DangXuLy'")) {
            stmt.setInt(1, maBan);
            stmt.executeUpdate();
            HttpUtil.guiJson(ex, 200, ketQua(true, "Đã hủy yêu cầu thanh toán", null));
        }
    }

    private Map<String, Object> ketQua(boolean thanhCong, String thongBao, Integer maDH) {
        Map<String, Object> m = new HashMap<>();
        m.put("thanhCong", thanhCong);
        m.put("thongBao", thongBao);
        if (maDH != null) m.put("maDH", maDH);
        return m;
    }

    private Map<String, Object> loi(String noiDung) {
        Map<String, Object> m = new HashMap<>();
        m.put("thanhCong", false);
        m.put("thongBao", noiDung);
        return m;
    }
}
