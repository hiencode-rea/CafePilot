import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BanDAO {

    public List<Ban> layTatCa() {
        List<Ban> list = new ArrayList<>();
        String sql = "SELECT b.MaBan, k.TenKV, b.SoHieuBan, b.TrangThai, b.SucChua, " +
                     "ISNULL(b.ViTriX, -1) AS ViTriX, ISNULL(b.ViTriY, -1) AS ViTriY, " +
                     "ISNULL(x.SoNguoi, 0) AS SoNguoi, ISNULL(x.TongTien, 0) AS TongTien, " +
                     "x.ThoiGianTao, ISNULL(x.YeuCauThanhToan, 0) AS YeuCau " +
                     "FROM Ban b JOIN KhuVuc k ON b.MaKV = k.MaKV " +
                     "OUTER APPLY (SELECT TOP 1 d.SoNguoi, d.TongTien, d.ThoiGianTao, d.YeuCauThanhToan FROM DonHang d " +
                     "             WHERE d.MaBan = b.MaBan AND d.TrangThai = N'DangXuLy' " +
                     "             ORDER BY d.ThoiGianTao DESC) x " +
                     "ORDER BY k.TenKV, b.SoHieuBan";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Ban b = new Ban(
                        rs.getInt("MaBan"),
                        rs.getString("TenKV"),
                        rs.getString("SoHieuBan"),
                        rs.getString("TrangThai"),
                        rs.getInt("SucChua"),
                        rs.getInt("SoNguoi"),
                        rs.getDouble("ViTriX"),
                        rs.getDouble("ViTriY"),
                        rs.getDouble("TongTien"),
                        rs.getTimestamp("ThoiGianTao")
                );
                b.setYeuCauThanhToan(rs.getBoolean("YeuCau"));
                list.add(b);
            }
        } catch (SQLException e) {
            System.out.println("Loi lay danh sach ban: " + e.getMessage());
        }
        return list;
    }

    public boolean themBan(int maKV, String soHieuBan, int sucChua) {
        String sql = "INSERT INTO Ban (MaKV, SoHieuBan, SucChua, ViTriX, ViTriY) VALUES (?, ?, ?, -1, -1)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, maKV);
            stmt.setString(2, soHieuBan);
            stmt.setInt(3, sucChua);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Loi them ban: " + e.getMessage());
            return false;
        }
    }

    public boolean xoaBan(int maBan) {
        String sql = "DELETE FROM Ban WHERE MaBan = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, maBan);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Loi xoa ban: " + e.getMessage());
            return false;
        }
    }

    public boolean themKhuVuc(String tenKV) {
        String sql = "INSERT INTO KhuVuc (TenKV) VALUES (?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tenKV);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Loi them khu vuc: " + e.getMessage());
            return false;
        }
    }

    public int layMaKVTheoTen(String tenKV) {
        String sql = "SELECT MaKV FROM KhuVuc WHERE TenKV = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tenKV);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("MaKV");
            }
        } catch (SQLException e) {
            System.out.println("Loi lay MaKV: " + e.getMessage());
        }
        return -1;
    }

    public List<String> layTatCaKhuVuc() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT TenKV FROM KhuVuc ORDER BY MaKV";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(rs.getString("TenKV"));
            }
        } catch (SQLException e) {
            System.out.println("Loi lay danh sach khu vuc: " + e.getMessage());
        }
        return list;
    }

    public boolean capNhatTrangThai(int maBan, String trangThaiMoi) {
        String sql = "UPDATE Ban SET TrangThai = ? WHERE MaBan = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, trangThaiMoi);
            stmt.setInt(2, maBan);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Loi cap nhat trang thai ban: " + e.getMessage());
            return false;
        }
    }

    public boolean capNhatBan(int maBan, String soHieuBan, int sucChua) {
        String sql = "UPDATE Ban SET SoHieuBan = ?, SucChua = ? WHERE MaBan = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, soHieuBan);
            stmt.setInt(2, sucChua);
            stmt.setInt(3, maBan);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Loi cap nhat ban: " + e.getMessage());
            return false;
        }
    }

    public boolean capNhatViTri(int maBan, double viTriX, double viTriY) {
        String sql = "UPDATE Ban SET ViTriX = ?, ViTriY = ? WHERE MaBan = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, viTriX);
            stmt.setDouble(2, viTriY);
            stmt.setInt(3, maBan);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Loi cap nhat vi tri ban: " + e.getMessage());
            return false;
        }
    }

    public boolean capNhatSoNguoi(int maBan, int soNguoi) {
        String sql = "UPDATE TOP (1) DonHang SET SoNguoi = ? " +
                     "WHERE MaBan = ? AND TrangThai = N'DangXuLy' " +
                     "ORDER BY ThoiGianTao DESC";
        // SQL Server không hỗ trợ ORDER BY trực tiếp trong UPDATE TOP theo cách trên.
        sql = "UPDATE DonHang SET SoNguoi = ? WHERE MaDH = " +
              "(SELECT TOP 1 MaDH FROM DonHang WHERE MaBan = ? AND TrangThai = N'DangXuLy' ORDER BY ThoiGianTao DESC)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, soNguoi);
            stmt.setInt(2, maBan);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Loi cap nhat so nguoi: " + e.getMessage());
            return false;        }
    }
}
