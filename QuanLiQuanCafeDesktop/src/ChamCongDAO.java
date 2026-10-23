import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChamCongDAO {

    public List<ChamCong> layTatCa() {
        List<ChamCong> list = new ArrayList<>();
        String sql = "SELECT cc.MaCC, nv.HoTen, c.Ngay, c.GioBatDau, c.GioKetThuc, cc.GioVao, cc.GioRa " +
                     "FROM ChamCong cc " +
                     "JOIN CaLamViec c ON cc.MaCa = c.MaCa " +
                     "JOIN NhanVien nv ON c.MaNV = nv.MaNV " +
                     "ORDER BY c.Ngay DESC, c.GioBatDau";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String thongTinCa = rs.getString("HoTen") + " - " + rs.getDate("Ngay") +
                        " (" + rs.getTime("GioBatDau").toString().substring(0, 5) +
                        "-" + rs.getTime("GioKetThuc").toString().substring(0, 5) + ")";

                Timestamp vao = rs.getTimestamp("GioVao");
                Timestamp ra = rs.getTimestamp("GioRa");

                list.add(new ChamCong(
                        rs.getInt("MaCC"),
                        thongTinCa,
                        vao != null ? vao.toString() : "-",
                        ra != null ? ra.toString() : "-"
                ));
            }
        } catch (SQLException e) {
            System.out.println("Loi lay danh sach cham cong: " + e.getMessage());
        }
        return list;
    }

    // Tra ve Map<Mo ta ca, MaCa> cho cac ca CHUA co ban ghi cham cong, de chon cham cong vao
    public Map<String, Integer> layCaChuaChamCongVao() {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT c.MaCa, nv.HoTen, c.Ngay, c.GioBatDau, c.GioKetThuc " +
                     "FROM CaLamViec c JOIN NhanVien nv ON c.MaNV = nv.MaNV " +
                     "WHERE c.MaCa NOT IN (SELECT MaCa FROM ChamCong) " +
                     "ORDER BY c.Ngay DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String moTa = rs.getString("HoTen") + " - " + rs.getDate("Ngay") +
                        " (" + rs.getTime("GioBatDau").toString().substring(0, 5) +
                        "-" + rs.getTime("GioKetThuc").toString().substring(0, 5) + ")";
                map.put(moTa, rs.getInt("MaCa"));
            }
        } catch (SQLException e) {
            System.out.println("Loi lay danh sach ca chua cham cong: " + e.getMessage());
        }
        return map;
    }

    // Tra ve Map<Mo ta, MaCC> cho cac ban ghi da vao nhung CHUA ra, de chon cham cong ra
    public Map<String, Integer> layChamCongChuaRa() {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT cc.MaCC, nv.HoTen, c.Ngay " +
                     "FROM ChamCong cc " +
                     "JOIN CaLamViec c ON cc.MaCa = c.MaCa " +
                     "JOIN NhanVien nv ON c.MaNV = nv.MaNV " +
                     "WHERE cc.GioRa IS NULL " +
                     "ORDER BY c.Ngay DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String moTa = rs.getString("HoTen") + " - " + rs.getDate("Ngay");
                map.put(moTa, rs.getInt("MaCC"));
            }
        } catch (SQLException e) {
            System.out.println("Loi lay danh sach cham cong chua ra: " + e.getMessage());
        }
        return map;
    }

    public boolean chamCongVao(int maCa) {
        String sql = "INSERT INTO ChamCong (MaCa, GioVao) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, maCa);
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Loi cham cong vao: " + e.getMessage());
            return false;
        }
    }

    public boolean chamCongRa(int maCC) {
        String sql = "UPDATE ChamCong SET GioRa = ? WHERE MaCC = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            stmt.setInt(2, maCC);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Loi cham cong ra: " + e.getMessage());
            return false;
        }
    }
}
