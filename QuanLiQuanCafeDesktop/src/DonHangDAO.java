import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DonHangDAO {

    // Tao don hang moi cho 1 ban, gan voi nhan vien dang dang nhap (theo tenDangNhap)
    // Tra ve MaDH neu thanh cong, -1 neu that bai
    public int taoDonHangMoi(int maBan, String tenDangNhap) {
        return taoDonHangMoi(maBan, tenDangNhap, 0);
    }

    public int taoDonHangMoi(int maBan, String tenDangNhap, int soNguoi) {
        String sqlLayMaNV = "SELECT nv.MaNV FROM NhanVien nv JOIN TaiKhoan tk ON nv.MaTK = tk.MaTK " +
                             "WHERE tk.TenDangNhap = ?";
        String sqlTaoDH = "INSERT INTO DonHang (MaBan, MaNV, SoNguoi, TrangThai, TongTien) VALUES (?, ?, ?, N'DangXuLy', 0)";
        String sqlCapNhatBan = "UPDATE Ban SET TrangThai = N'DangPhucVu' WHERE MaBan = ?";

        try (Connection conn = DBConnection.getConnection()) {
            int maNV;
            try (PreparedStatement s1 = conn.prepareStatement(sqlLayMaNV)) {
                s1.setString(1, tenDangNhap);
                try (ResultSet rs = s1.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Khong tim thay nhan vien tuong ung voi tai khoan.");
                        return -1;
                    }
                    maNV = rs.getInt("MaNV");
                }
            }

            int maDH = -1;
            try (PreparedStatement s2 = conn.prepareStatement(sqlTaoDH, Statement.RETURN_GENERATED_KEYS)) {
                s2.setInt(1, maBan);
                s2.setInt(2, maNV);
                s2.setInt(3, Math.max(0, soNguoi));
                s2.executeUpdate();
                try (ResultSet rs = s2.getGeneratedKeys()) {
                    if (rs.next()) maDH = rs.getInt(1);
                }
            }

            try (PreparedStatement s3 = conn.prepareStatement(sqlCapNhatBan)) {
                s3.setInt(1, maBan);
                s3.executeUpdate();
            }

            return maDH;

        } catch (SQLException e) {
            System.out.println("Loi tao don hang: " + e.getMessage());
            return -1;
        }
    }

    public boolean themChiTiet(int maDH, int maSP, int soLuong, double donGia, String ghiChu) {
        // Tim dong cung MaSP va CHUA thong bao bep (DaThongBaoBep = 0) de cong don
        // Neu tat ca cac dong deu da bao bep -> tu dong tao dong moi
        String sqlKiemTra = "SELECT TOP 1 MaCT, GhiChu FROM ChiTietDonHang WHERE MaDH = ? AND MaSP = ? AND DaThongBaoBep = 0 ORDER BY MaCT ASC";
        String sqlCapNhat = "UPDATE ChiTietDonHang SET SoLuong = SoLuong + ?, GhiChu = ? WHERE MaCT = ?";
        String sqlThem = "INSERT INTO ChiTietDonHang (MaDH, MaSP, SoLuong, DonGia, GhiChu) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            String ghiChuCu = null;
            int maCTTimDuoc = -1;
            boolean daCoDong = false;
            try (PreparedStatement s1 = conn.prepareStatement(sqlKiemTra)) {
                s1.setInt(1, maDH);
                s1.setInt(2, maSP);
                try (ResultSet rs = s1.executeQuery()) {
                    if (rs.next()) {
                        daCoDong = true;
                        maCTTimDuoc = rs.getInt("MaCT");
                        ghiChuCu = rs.getString("GhiChu");
                    }
                }
            }

            if (daCoDong) {
                try (PreparedStatement s2 = conn.prepareStatement(sqlCapNhat)) {
                    s2.setInt(1, soLuong);
                    s2.setString(2, gopGhiChu(ghiChuCu, ghiChu));
                    s2.setInt(3, maCTTimDuoc);
                    return s2.executeUpdate() > 0;
                }
            } else {
                try (PreparedStatement s3 = conn.prepareStatement(sqlThem)) {
                    s3.setInt(1, maDH);
                    s3.setInt(2, maSP);
                    s3.setInt(3, soLuong);
                    s3.setDouble(4, donGia);
                    s3.setString(5, (ghiChu == null || ghiChu.trim().isEmpty()) ? null : ghiChu.trim());
                    return s3.executeUpdate() > 0;
                }
            }

        } catch (SQLException e) {
            System.out.println("Loi them chi tiet don hang: " + e.getMessage());
            return false;
        }
    }

    // Noi 2 ghi chu voi nhau (bo qua ghi chu rong/null)
    private static String gopGhiChu(String cu, String moi) {
        boolean cuRong = cu == null || cu.trim().isEmpty();
        boolean moiRong = moi == null || moi.trim().isEmpty();
        if (moiRong) return cuRong ? null : cu.trim();
        if (cuRong) return moi.trim();
        String noi = cu.trim() + "; " + moi.trim();
        return noi.length() > 255 ? noi.substring(0, 255) : noi;
    }

    // Cap nhat ghi chu cua 1 mon trong don (nhan vien sua truc tiep tu dialog)
    public boolean capNhatGhiChu(int maDH, int maSP, String ghiChu) {
        String sql = "UPDATE ChiTietDonHang SET GhiChu = ? WHERE MaDH = ? AND MaSP = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String gt = (ghiChu == null || ghiChu.trim().isEmpty()) ? null : ghiChu.trim();
            if (gt != null && gt.length() > 255) gt = gt.substring(0, 255);
            stmt.setString(1, gt);
            stmt.setInt(2, maDH);
            stmt.setInt(3, maSP);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Loi cap nhat ghi chu: " + e.getMessage());
            return false;
        }
    }

    public void capNhatTongTien(int maDH) {
        String sql = "UPDATE DonHang SET TongTien = " +
                     "(SELECT ISNULL(SUM(SoLuong * DonGia), 0) FROM ChiTietDonHang WHERE MaDH = ?) " +
                     "WHERE MaDH = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, maDH);
            stmt.setInt(2, maDH);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Loi cap nhat tong tien: " + e.getMessage());
        }
    }

    // Hoan thanh don hang (thanh toan) va tra ban ve trang thai Trong
    public boolean hoanThanhDonHang(int maBan) {
        String sqlTimDH = "SELECT MaDH FROM DonHang WHERE MaBan = ? AND TrangThai = N'DangXuLy'";
        String sqlCapNhatDH = "UPDATE DonHang SET TrangThai = N'HoanThanh', YeuCauThanhToan = 0 " +
                "WHERE MaDH = ? AND NOT EXISTS (SELECT 1 FROM ChiTietDonHang " +
                "WHERE MaDH = ? AND (DaThongBaoBep = 0 OR TrangThaiMon NOT IN (N'DA_RA_MON', N'HET_MON')))";
        String sqlCapNhatBan = "UPDATE Ban SET TrangThai = N'Trong' WHERE MaBan = ?";

        try (Connection conn = DBConnection.getConnection()) {
            int maDH = -1;
            try (PreparedStatement s1 = conn.prepareStatement(sqlTimDH)) {
                s1.setInt(1, maBan);
                try (ResultSet rs = s1.executeQuery()) {
                    if (rs.next()) maDH = rs.getInt("MaDH");
                }
            }
            if (maDH == -1) return false;

            try (PreparedStatement s2 = conn.prepareStatement(sqlCapNhatDH)) {
                s2.setInt(1, maDH);
                s2.setInt(2, maDH);
                if (s2.executeUpdate() == 0) return false;
            }
            try (PreparedStatement s3 = conn.prepareStatement(sqlCapNhatBan)) {
                s3.setInt(1, maBan);
                s3.executeUpdate();
            }
            return true;

        } catch (SQLException e) {
            System.out.println("Loi hoan thanh don hang: " + e.getMessage());
            return false;
        }
    }

    public boolean coMonChuaHoanTat(int maDH) {
        String sql = "SELECT COUNT(*) FROM ChiTietDonHang WHERE MaDH = ? " +
                "AND (DaThongBaoBep = 0 OR TrangThaiMon NOT IN (N'DA_RA_MON', N'HET_MON'))";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, maDH);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("Loi kiem tra mon chua hoan tat: " + e.getMessage());
            return true;
        }
    }

    // Lay thong tin don hang dang mo (chua thanh toan) cua 1 ban: {MaDH, ThoiGianTao, TongTien}
    // Lay TOP 1 theo ThoiGianTao moi nhat, phong truong hop co du lieu du (nhieu don dang mo) do loi cu de lai
    public Object[] layThongTinDonMoCuaBan(int maBan) {
        String sql = "SELECT TOP 1 MaDH, ThoiGianTao, TongTien FROM DonHang " +
                     "WHERE MaBan = ? AND TrangThai = N'DangXuLy' " +
                     "ORDER BY ThoiGianTao DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, maBan);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Object[]{
                            rs.getInt("MaDH"),
                            rs.getTimestamp("ThoiGianTao"),
                            rs.getDouble("TongTien")
                    };
                }
            }
        } catch (SQLException e) {
            System.out.println("Loi lay thong tin don mo: " + e.getMessage());
        }
        return null;
    }

    // Lay chi tiet don hang: moi dong la {TenSP, SoLuong, DonGia, ThanhTien}
    public java.util.List<Object[]> layChiTietDonHang(int maDH) {
        java.util.List<Object[]> list = new java.util.ArrayList<>();
        String sql = "SELECT sp.TenSP, ct.SoLuong, ct.DonGia, (ct.SoLuong * ct.DonGia) AS ThanhTien " +
                     "FROM ChiTietDonHang ct JOIN SanPham sp ON ct.MaSP = sp.MaSP " +
                     "WHERE ct.MaDH = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, maDH);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{
                            rs.getString("TenSP"),
                            rs.getInt("SoLuong"),
                            rs.getDouble("DonGia"),
                            rs.getDouble("ThanhTien")
                    });
                }
            }
        } catch (SQLException e) {
            System.out.println("Loi lay chi tiet don hang: " + e.getMessage());
        }
        return list;
    }

    // Lay chi tiet day du (co MaSP va trang thai bao bep) de phuc vu man hinh Xem chi tiet / Them mon
    // Moi dong: {MaSP, TenSP, SoLuong, DonGia, ThanhTien, DaThongBaoBep, TrangThaiMon}
    public java.util.List<Object[]> layChiTietDayDu(int maDH) {
        java.util.List<Object[]> list = new java.util.ArrayList<>();
        String sql = "SELECT ct.MaSP, sp.TenSP, ct.SoLuong, ct.DonGia, (ct.SoLuong * ct.DonGia) AS ThanhTien, ct.DaThongBaoBep, ct.TrangThaiMon, " +
                     "ISNULL(sp.Size, '') AS Size, ISNULL(sp.MoTa, '') AS MoTa, ISNULL(ct.GhiChu, '') AS GhiChu " +
                     "FROM ChiTietDonHang ct JOIN SanPham sp ON ct.MaSP = sp.MaSP " +
                     "WHERE ct.MaDH = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, maDH);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{
                            rs.getInt("MaSP"),
                            rs.getString("TenSP"),
                            rs.getInt("SoLuong"),
                            rs.getDouble("DonGia"),
                            rs.getDouble("ThanhTien"),
                            rs.getBoolean("DaThongBaoBep"),
                            rs.getString("TrangThaiMon"),
                            rs.getString("Size"),
                            rs.getString("MoTa"),
                            rs.getString("GhiChu")
                    });
                }
            }
        } catch (SQLException e) {
            System.out.println("Loi lay chi tiet day du: " + e.getMessage());
        }
        return list;
    }

    // Danh dau tat ca mon trong 1 don la da bao bep
    public boolean thongBaoBep(int maDH) {
        String sql = "UPDATE ChiTietDonHang SET DaThongBaoBep = 1 WHERE MaDH = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, maDH);
            int soDong = stmt.executeUpdate();
            if (soDong > 0) {
                // Sau khi thong bao bep, gom cac dong trung mon (cung MaSP) lai voi nhau
                gopMonTrung(conn, maDH);
            }
            return soDong >= 0;
        } catch (SQLException e) {
            System.out.println("Loi thong bao bep: " + e.getMessage());
            return false;
        }
    }

    // Kiem tra 1 mon trong don da bao bep chua
    public boolean kiemTraDaThongBao(int maDH, int maSP) {
        String sql = "SELECT DaThongBaoBep FROM ChiTietDonHang WHERE MaDH = ? AND MaSP = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, maDH);
            stmt.setInt(2, maSP);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getBoolean("DaThongBaoBep");
            }
        } catch (SQLException e) {
            System.out.println("Loi kiem tra trang thai bao bep: " + e.getMessage());
        }
        return false;
    }

    // Xoa 1 mon khoi don hang (dung khi mon chua bao bep, hoac Desktop da xac nhan ly do)
    public boolean xoaMonKhoiDon(int maDH, int maSP) {
        String sql = "DELETE FROM ChiTietDonHang WHERE MaDH = ? AND MaSP = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, maDH);
            stmt.setInt(2, maSP);
            boolean ok = stmt.executeUpdate() > 0;
            huyDonNeuRong(conn, maDH);
            return ok;
        } catch (SQLException e) {
            System.out.println("Loi xoa mon khoi don: " + e.getMessage());
            return false;
        }
    }

    // Neu don khong con mon nao thi huy don va tra ban ve Trong (tranh ban ket o trang thai Dang phuc vu)
    private void huyDonNeuRong(Connection conn, int maDH) throws SQLException {
        int soMon = 0;
        try (PreparedStatement s1 = conn.prepareStatement("SELECT COUNT(*) FROM ChiTietDonHang WHERE MaDH = ?")) {
            s1.setInt(1, maDH);
            try (ResultSet rs = s1.executeQuery()) { if (rs.next()) soMon = rs.getInt(1); }
        }
        if (soMon > 0) return;

        try (PreparedStatement s2 = conn.prepareStatement(
                "UPDATE Ban SET TrangThai = N'Trong' WHERE MaBan = (SELECT MaBan FROM DonHang WHERE MaDH = ?)")) {
            s2.setInt(1, maDH);
            s2.executeUpdate();
        }
        try (PreparedStatement s3 = conn.prepareStatement(
                "UPDATE DonHang SET TrangThai = N'DaHuy', TongTien = 0 WHERE MaDH = ?")) {
            s3.setInt(1, maDH);
            s3.executeUpdate();
        }
    }

    // ===== CHUYEN BAN: doi don hang hien tai sang 1 ban khac dang Trong =====
    public boolean chuyenBan(int maDH, int banCu, int banMoi) {
        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement s1 = conn.prepareStatement("UPDATE DonHang SET MaBan = ? WHERE MaDH = ?")) {
                s1.setInt(1, banMoi);
                s1.setInt(2, maDH);
                s1.executeUpdate();
            }
            try (PreparedStatement s2 = conn.prepareStatement("UPDATE Ban SET TrangThai = N'Trong' WHERE MaBan = ?")) {
                s2.setInt(1, banCu);
                s2.executeUpdate();
            }
            try (PreparedStatement s3 = conn.prepareStatement("UPDATE Ban SET TrangThai = N'DangPhucVu' WHERE MaBan = ?")) {
                s3.setInt(1, banMoi);
                s3.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            System.out.println("Loi chuyen ban: " + e.getMessage());
            return false;
        }
    }

    // ===== GOP BAN: gop toan bo don hang cua ban nguon vao don hang dich, dong don nguon =====
    public boolean gopBan(int maDHDich, int banNguon) {
        try (Connection conn = DBConnection.getConnection()) {
            Integer maDHNguon = null;
            try (PreparedStatement s1 = conn.prepareStatement(
                    "SELECT MaDH FROM DonHang WHERE MaBan = ? AND TrangThai = N'DangXuLy'")) {
                s1.setInt(1, banNguon);
                try (ResultSet rs = s1.executeQuery()) { if (rs.next()) maDHNguon = rs.getInt("MaDH"); }
            }
            if (maDHNguon == null) return false;

            // Lay danh sach mon cua don nguon roi gop tung dong vao don dich (cong don neu trung san pham)
            try (PreparedStatement s2 = conn.prepareStatement(
                    "SELECT MaSP, SoLuong, DonGia, GhiChu FROM ChiTietDonHang WHERE MaDH = ?")) {
                s2.setInt(1, maDHNguon);
                try (ResultSet rs = s2.executeQuery()) {
                    while (rs.next()) {
                        int maSP = rs.getInt("MaSP");
                        int soLuong = rs.getInt("SoLuong");
                        double donGia = rs.getDouble("DonGia");
                        String ghiChuNguon = rs.getString("GhiChu");

                        String ghiChuDich = null;
                        String trangThaiDich = null;
                        boolean daThongBaoDich = false;
                        boolean daCoDong;
                        try (PreparedStatement sk = conn.prepareStatement(
                                "SELECT GhiChu, DaThongBaoBep, TrangThaiMon FROM ChiTietDonHang WHERE MaDH=? AND MaSP=?")) {
                            sk.setInt(1, maDHDich); sk.setInt(2, maSP);
                            try (ResultSet rk = sk.executeQuery()) {
                                daCoDong = rk.next();
                                if (daCoDong) {
                                    ghiChuDich = rk.getString("GhiChu");
                                    daThongBaoDich = rk.getBoolean("DaThongBaoBep");
                                    trangThaiDich = rk.getString("TrangThaiMon");
                                }
                            }
                        }

                        if (daCoDong && daThongBaoDich
                                && !("DA_RA_MON".equals(trangThaiDich) || "HET_MON".equals(trangThaiDich))) {
                            System.out.println("Khong the gop ban: mon cung loai o don dich chua ra mon.");
                            return false;
                        }
                        if (daCoDong && "HET_MON".equals(trangThaiDich)) {
                            System.out.println("Khong the gop ban: mon cung loai o don dich da het.");
                            return false;
                        }

                        if (daCoDong) {
                            try (PreparedStatement su = conn.prepareStatement(
                                    "UPDATE ChiTietDonHang SET SoLuong = SoLuong + ?, GhiChu = ? WHERE MaDH=? AND MaSP=?")) {
                                su.setInt(1, soLuong); su.setString(2, gopGhiChu(ghiChuDich, ghiChuNguon)); su.setInt(3, maDHDich); su.setInt(4, maSP);
                                su.executeUpdate();
                            }
                        } else {
                            try (PreparedStatement si = conn.prepareStatement(
                                    "INSERT INTO ChiTietDonHang (MaDH, MaSP, SoLuong, DonGia, GhiChu) VALUES (?,?,?,?,?)")) {
                                si.setInt(1, maDHDich); si.setInt(2, maSP); si.setInt(3, soLuong); si.setDouble(4, donGia);
                                si.setString(5, (ghiChuNguon == null || ghiChuNguon.trim().isEmpty()) ? null : ghiChuNguon.trim());
                                si.executeUpdate();
                            }
                        }
                    }
                }
            }

            // Xoa het chi tiet cua don nguon, dong don nguon, tra ban nguon ve Trong
            try (PreparedStatement s3 = conn.prepareStatement("DELETE FROM ChiTietDonHang WHERE MaDH = ?")) {
                s3.setInt(1, maDHNguon);
                s3.executeUpdate();
            }
            try (PreparedStatement s4 = conn.prepareStatement("UPDATE DonHang SET TrangThai = N'DaHuy' WHERE MaDH = ?")) {
                s4.setInt(1, maDHNguon);
                s4.executeUpdate();
            }
            try (PreparedStatement s5 = conn.prepareStatement("UPDATE Ban SET TrangThai = N'Trong' WHERE MaBan = ?")) {
                s5.setInt(1, banNguon);
                s5.executeUpdate();
            }
            try (PreparedStatement s6 = conn.prepareStatement(
                    "UPDATE DonHang SET TongTien = (SELECT ISNULL(SUM(SoLuong*DonGia),0) FROM ChiTietDonHang WHERE MaDH=?) WHERE MaDH=?")) {
                s6.setInt(1, maDHDich); s6.setInt(2, maDHDich);
                s6.executeUpdate();
            }
            return true;

        } catch (SQLException e) {
            System.out.println("Loi gop ban: " + e.getMessage());
            return false;
        }
    }

    // ===== TACH MON: chuyen 1 phan (hoac toan bo) so luong cua 1 mon sang don hang cua ban khac =====
    public boolean tachMonSangBanKhac(int maDHHienTai, int maSP, int soLuongTach, int banDich, String tenDangNhap) {
        try (Connection conn = DBConnection.getConnection()) {
            int soLuongHienCo;
            double donGia;
            String ghiChuNguon = null;
            try (PreparedStatement s1 = conn.prepareStatement(
                    "SELECT SoLuong, DonGia, GhiChu FROM ChiTietDonHang WHERE MaDH=? AND MaSP=?")) {
                s1.setInt(1, maDHHienTai); s1.setInt(2, maSP);
                try (ResultSet rs = s1.executeQuery()) {
                    if (!rs.next()) return false;
                    soLuongHienCo = rs.getInt("SoLuong");
                    donGia = rs.getDouble("DonGia");
                    ghiChuNguon = rs.getString("GhiChu");
                }
            }
            if (soLuongTach <= 0 || soLuongTach > soLuongHienCo) return false;

            // Tim hoac tao don hang cho ban dich
            Integer maDHDich = null;
            try (PreparedStatement s2 = conn.prepareStatement(
                    "SELECT MaDH FROM DonHang WHERE MaBan=? AND TrangThai=N'DangXuLy'")) {
                s2.setInt(1, banDich);
                try (ResultSet rs = s2.executeQuery()) { if (rs.next()) maDHDich = rs.getInt("MaDH"); }
            }
            if (maDHDich == null) {
                int maNV;
                try (PreparedStatement s3 = conn.prepareStatement(
                        "SELECT nv.MaNV FROM NhanVien nv JOIN TaiKhoan tk ON nv.MaTK=tk.MaTK WHERE tk.TenDangNhap=?")) {
                    s3.setString(1, tenDangNhap);
                    try (ResultSet rs = s3.executeQuery()) { rs.next(); maNV = rs.getInt("MaNV"); }
                }
                try (PreparedStatement s4 = conn.prepareStatement(
                        "INSERT INTO DonHang (MaBan, MaNV, TrangThai, TongTien) VALUES (?,?,N'DangXuLy',0)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    s4.setInt(1, banDich);
                    s4.setInt(2, maNV);
                    s4.executeUpdate();
                    try (ResultSet rs = s4.getGeneratedKeys()) { rs.next(); maDHDich = rs.getInt(1); }
                }
                try (PreparedStatement s5 = conn.prepareStatement("UPDATE Ban SET TrangThai = N'DangPhucVu' WHERE MaBan = ?")) {
                    s5.setInt(1, banDich);
                    s5.executeUpdate();
                }
            }

            // Giam/xoa dong o don hien tai
            if (soLuongTach == soLuongHienCo) {
                try (PreparedStatement s6 = conn.prepareStatement("DELETE FROM ChiTietDonHang WHERE MaDH=? AND MaSP=?")) {
                    s6.setInt(1, maDHHienTai); s6.setInt(2, maSP);
                    s6.executeUpdate();
                }
            } else {
                try (PreparedStatement s6 = conn.prepareStatement(
                        "UPDATE ChiTietDonHang SET SoLuong = SoLuong - ? WHERE MaDH=? AND MaSP=?")) {
                    s6.setInt(1, soLuongTach); s6.setInt(2, maDHHienTai); s6.setInt(3, maSP);
                    s6.executeUpdate();
                }
            }

            // Cong vao don dich (cong don neu da co)
            boolean daCoODich;
            try (PreparedStatement s7 = conn.prepareStatement("SELECT 1 FROM ChiTietDonHang WHERE MaDH=? AND MaSP=?")) {
                s7.setInt(1, maDHDich); s7.setInt(2, maSP);
                try (ResultSet rs = s7.executeQuery()) { daCoODich = rs.next(); }
            }
            if (daCoODich) {
                try (PreparedStatement s8 = conn.prepareStatement(
                        "UPDATE ChiTietDonHang SET SoLuong = SoLuong + ? WHERE MaDH=? AND MaSP=?")) {
                    s8.setInt(1, soLuongTach); s8.setInt(2, maDHDich); s8.setInt(3, maSP);
                    s8.executeUpdate();
                }
            } else {
                try (PreparedStatement s8 = conn.prepareStatement(
                        "INSERT INTO ChiTietDonHang (MaDH, MaSP, SoLuong, DonGia, GhiChu) VALUES (?,?,?,?,?)")) {
                    s8.setInt(1, maDHDich); s8.setInt(2, maSP); s8.setInt(3, soLuongTach); s8.setDouble(4, donGia);
                    s8.setString(5, (ghiChuNguon == null || ghiChuNguon.trim().isEmpty()) ? null : ghiChuNguon.trim());
                    s8.executeUpdate();
                }
            }

            // Neu tach het mon khien don hien tai rong thi huy don va tra ban ve Trong
            huyDonNeuRong(conn, maDHHienTai);

            // Cap nhat tong tien ca 2 don
            for (int maDH : new int[]{maDHHienTai, maDHDich}) {
                try (PreparedStatement s9 = conn.prepareStatement(
                        "UPDATE DonHang SET TongTien = (SELECT ISNULL(SUM(SoLuong*DonGia),0) FROM ChiTietDonHang WHERE MaDH=?) WHERE MaDH=?")) {
                    s9.setInt(1, maDH); s9.setInt(2, maDH);
                    s9.executeUpdate();
                }
            }
            return true;

        } catch (SQLException e) {
            System.out.println("Loi tach mon: " + e.getMessage());
            return false;
        }
    }

    // ===== YEU CAU THANH TOAN (gui tu Mobile) =====

    // Dem so ban dang cho thu ngan xac nhan thanh toan
    public int demYeuCauThanhToan() {
        String sql = "SELECT COUNT(*) FROM DonHang WHERE TrangThai = N'DangXuLy' AND YeuCauThanhToan = 1";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("Loi dem yeu cau thanh toan: " + e.getMessage());
        }
        return 0;
    }

    // Danh sach yeu cau: {MaBan, SoHieuBan, TenKhuVuc, TongTien (tinh lai tu chi tiet), ThoiGianYeuCau}
    public java.util.List<Object[]> layDanhSachYeuCauThanhToan() {
        java.util.List<Object[]> list = new java.util.ArrayList<>();
        String sql = "SELECT b.MaBan, b.SoHieuBan, k.TenKV, d.ThoiGianYeuCau, " +
                     "(SELECT ISNULL(SUM(ct.SoLuong * ct.DonGia), 0) FROM ChiTietDonHang ct WHERE ct.MaDH = d.MaDH) AS Tong " +
                     "FROM DonHang d JOIN Ban b ON d.MaBan = b.MaBan JOIN KhuVuc k ON b.MaKV = k.MaKV " +
                     "WHERE d.TrangThai = N'DangXuLy' AND d.YeuCauThanhToan = 1 " +
                     "ORDER BY d.ThoiGianYeuCau";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Object[]{
                        rs.getInt("MaBan"),
                        rs.getString("SoHieuBan"),
                        rs.getString("TenKV"),
                        rs.getDouble("Tong"),
                        rs.getTimestamp("ThoiGianYeuCau")
                });
            }
        } catch (SQLException e) {
            System.out.println("Loi lay danh sach yeu cau thanh toan: " + e.getMessage());
        }
        return list;
    }

    // Kiem tra 1 don co dang cho xac nhan thanh toan khong
    public boolean coYeuCauThanhToan(int maDH) {
        String sql = "SELECT YeuCauThanhToan FROM DonHang WHERE MaDH = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, maDH);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getBoolean(1);
            }
        } catch (SQLException e) {
            System.out.println("Loi kiem tra yeu cau thanh toan: " + e.getMessage());
        }
        return false;
    }

    // Tu choi / xoa yeu cau thanh toan cua 1 ban
    public boolean tuChoiYeuCauThanhToan(int maBan) {
        String sql = "UPDATE DonHang SET YeuCauThanhToan = 0, ThoiGianYeuCau = NULL " +
                     "WHERE MaBan = ? AND TrangThai = N'DangXuLy'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, maBan);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Loi tu choi yeu cau thanh toan: " + e.getMessage());
            return false;
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
                            java.util.List<Integer> dsCTXoa = new java.util.ArrayList<>();
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
}
