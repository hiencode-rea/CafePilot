import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SanPhamDAO {

    public List<SanPham> layTatCa() {
        List<SanPham> list = new ArrayList<>();
        String sql = "SELECT MaSP, TenSP, NhomSP, GiaBan, Size, MoTa FROM SanPham ORDER BY MaSP";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new SanPham(
                        rs.getInt("MaSP"),
                        rs.getString("TenSP"),
                        rs.getString("NhomSP"),
                        rs.getDouble("GiaBan"),
                        rs.getString("Size"),
                        rs.getString("MoTa")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Loi lay danh sach san pham: " + e.getMessage());
        }
        return list;
    }

    public boolean them(String tenSP, String nhomSP, double giaBan, String size, String moTa) {
        if (giaBan <= 0) {
            System.out.println("Gia ban phai lon hon 0.");
            return false;
        }
        String sql = "INSERT INTO SanPham (TenSP, NhomSP, GiaBan, Size, MoTa) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, tenSP);
            stmt.setString(2, nhomSP);
            stmt.setDouble(3, giaBan);
            stmt.setString(4, size);
            stmt.setString(5, moTa);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Loi them san pham: " + e.getMessage());
            return false;
        }
    }

    public boolean capNhat(int maSP, String tenSP, String nhomSP, double giaBan, String size, String moTa) {
        if (giaBan <= 0) {
            System.out.println("Gia ban phai lon hon 0.");
            return false;
        }
        String sql = "UPDATE SanPham SET TenSP = ?, NhomSP = ?, GiaBan = ?, Size = ?, MoTa = ? WHERE MaSP = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, tenSP);
            stmt.setString(2, nhomSP);
            stmt.setDouble(3, giaBan);
            stmt.setString(4, size);
            stmt.setString(5, moTa);
            stmt.setInt(6, maSP);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Loi cap nhat san pham: " + e.getMessage());
            return false;
        }
    }

    public boolean xoa(int maSP) {
        String sql = "DELETE FROM SanPham WHERE MaSP = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, maSP);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Loi xoa san pham (co the dang duoc dung trong don hang): " + e.getMessage());
            return false;
        }
    }

    public boolean capNhatTheoTenVaSize(String tenCu, String tenMoi, String nhom, double gia, String size, String moTa) {
        String sql = "UPDATE SanPham SET TenSP=?, NhomSP=?, GiaBan=?, MoTa=? WHERE TenSP=? AND Size=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tenMoi);
            stmt.setString(2, nhom);
            stmt.setDouble(3, gia);
            stmt.setString(4, moTa);
            stmt.setString(5, tenCu);
            stmt.setString(6, size);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Loi cap nhat theo ten va size: " + e.getMessage());
            return false;
        }
    }

    public boolean xoaTheoTen(String tenSP) {
        String sql = "DELETE FROM SanPham WHERE TenSP = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tenSP);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Loi xoa theo ten: " + e.getMessage());
            return false;
        }
    }

    public boolean xoaTheoTenVaSize(String tenSP, String size) {
        String sql = "DELETE FROM SanPham WHERE TenSP = ? AND Size = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tenSP);
            stmt.setString(2, size);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Loi xoa theo ten va size: " + e.getMessage());
            return false;
        }
    }

    public boolean xoaTheoTenKhongSize(String tenSP) {
        String sql = "DELETE FROM SanPham WHERE TenSP = ? AND Size IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tenSP);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Loi xoa san pham khong size: " + e.getMessage());
            return false;
        }
    }
}
