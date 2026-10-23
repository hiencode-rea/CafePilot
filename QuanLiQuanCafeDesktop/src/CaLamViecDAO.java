import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CaLamViecDAO {

    public List<CaLamViec> layTatCa() {
        List<CaLamViec> list = new ArrayList<>();
        String sql = "SELECT c.MaCa, c.MaNV, nv.HoTen, c.Ngay, c.GioBatDau, c.GioKetThuc " +
                     "FROM CaLamViec c JOIN NhanVien nv ON c.MaNV = nv.MaNV " +
                     "ORDER BY c.Ngay DESC, c.GioBatDau";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new CaLamViec(
                        rs.getInt("MaCa"),
                        rs.getInt("MaNV"),
                        rs.getString("HoTen"),
                        rs.getDate("Ngay").toString(),
                        rs.getTime("GioBatDau").toString().substring(0, 5),
                        rs.getTime("GioKetThuc").toString().substring(0, 5)
                ));
            }
        } catch (SQLException e) {
            System.out.println("Loi lay danh sach ca lam viec: " + e.getMessage());
        }
        return list;
    }

    // Tra ve Map<TenNV, MaNV> de do vao ComboBox chon nhan vien
    public Map<String, Integer> layDanhSachNhanVien() {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT MaNV, HoTen FROM NhanVien ORDER BY HoTen";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                map.put(rs.getString("HoTen") + " (NV" + rs.getInt("MaNV") + ")", rs.getInt("MaNV"));
            }
        } catch (SQLException e) {
            System.out.println("Loi lay danh sach nhan vien: " + e.getMessage());
        }
        return map;
    }

    public boolean them(int maNV, String ngay, String gioBatDau, String gioKetThuc) {
        String sql = "INSERT INTO CaLamViec (MaNV, Ngay, GioBatDau, GioKetThuc) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, maNV);
            stmt.setDate(2, Date.valueOf(ngay));
            stmt.setTime(3, Time.valueOf(gioBatDau + ":00"));
            stmt.setTime(4, Time.valueOf(gioKetThuc + ":00"));
            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Loi them ca lam viec: " + e.getMessage());
            return false;
        }
    }

    public boolean capNhat(int maCa, int maNV, String ngay, String gioBatDau, String gioKetThuc) {
        String sql = "UPDATE CaLamViec SET MaNV = ?, Ngay = ?, GioBatDau = ?, GioKetThuc = ? WHERE MaCa = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, maNV);
            stmt.setDate(2, Date.valueOf(ngay));
            stmt.setTime(3, Time.valueOf(gioBatDau + ":00"));
            stmt.setTime(4, Time.valueOf(gioKetThuc + ":00"));
            stmt.setInt(5, maCa);
            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Loi cap nhat ca lam viec: " + e.getMessage());
            return false;
        }
    }

    public boolean xoa(int maCa) {
        String sql = "DELETE FROM CaLamViec WHERE MaCa = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, maCa);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Loi xoa ca lam viec (co the da co du lieu cham cong): " + e.getMessage());
            return false;
        }
    }
}
