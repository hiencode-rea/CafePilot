import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ComboDAO {

    // Danh sach combo hien co: {MaSP, TenSP, GiaBan}
    public List<Object[]> layTatCaCombo() {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT MaSP, TenSP, GiaBan FROM SanPham WHERE NhomSP = N'Combo' ORDER BY MaSP";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Object[]{ rs.getInt("MaSP"), rs.getString("TenSP"), rs.getDouble("GiaBan") });
            }
        } catch (SQLException e) {
            System.out.println("Loi lay danh sach combo: " + e.getMessage());
        }
        return list;
    }

    // Thanh phan cua 1 combo: {TenSPThanhPhan, SoLuong}
    public List<Object[]> layThanhPhanCombo(int maSPCombo) {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT sp.TenSP, ct.SoLuong FROM ChiTietCombo ct " +
                     "JOIN SanPham sp ON ct.MaSPThanhPhan = sp.MaSP WHERE ct.MaSP = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, maSPCombo);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{ rs.getString("TenSP"), rs.getInt("SoLuong") });
                }
            }
        } catch (SQLException e) {
            System.out.println("Loi lay thanh phan combo: " + e.getMessage());
        }
        return list;
    }

    // Tao combo moi: tao 1 dong SanPham (NhomSP = Combo) roi them cac dong thanh phan
    public boolean taoCombo(String tenCombo, double giaCombo, List<int[]> thanhPhan) {
        if (giaCombo <= 0) {
            System.out.println("Gia combo phai lon hon 0.");
            return false;
        }
        // Kiem tra trung ten combo
        String sqlKiemTraTen = "SELECT COUNT(*) FROM SanPham WHERE TenSP = ? AND NhomSP = N'Combo'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement sCheck = conn.prepareStatement(sqlKiemTraTen)) {
            sCheck.setString(1, tenCombo);
            try (ResultSet rs = sCheck.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("Ten combo da ton tai.");
                    return false;
                }
            }
        } catch (SQLException e) {
            System.out.println("Loi kiem tra ten combo: " + e.getMessage());
            return false;
        }

        // thanhPhan: danh sach [maSPThanhPhan, soLuong]
        String sqlTaoSP = "INSERT INTO SanPham (TenSP, NhomSP, GiaBan) VALUES (?, N'Combo', ?)";
        String sqlThemThanhPhan = "INSERT INTO ChiTietCombo (MaSP, MaSPThanhPhan, SoLuong) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            int maSPCombo;
            try (PreparedStatement s1 = conn.prepareStatement(sqlTaoSP, Statement.RETURN_GENERATED_KEYS)) {
                s1.setString(1, tenCombo);
                s1.setDouble(2, giaCombo);
                s1.executeUpdate();
                try (ResultSet rs = s1.getGeneratedKeys()) {
                    rs.next();
                    maSPCombo = rs.getInt(1);
                }
            }
            for (int[] tp : thanhPhan) {
                try (PreparedStatement s2 = conn.prepareStatement(sqlThemThanhPhan)) {
                    s2.setInt(1, maSPCombo);
                    s2.setInt(2, tp[0]);
                    s2.setInt(3, tp[1]);
                    s2.executeUpdate();
                }
            }
            return true;

        } catch (SQLException e) {
            System.out.println("Loi tao combo: " + e.getMessage());
            return false;
        }
    }

    // Xoa combo: xoa thanh phan truoc, roi xoa dong SanPham dai dien
    public boolean xoaCombo(int maSPCombo) {
        String sqlXoaThanhPhan = "DELETE FROM ChiTietCombo WHERE MaSP = ?";
        String sqlXoaSP = "DELETE FROM SanPham WHERE MaSP = ?";

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement s1 = conn.prepareStatement(sqlXoaThanhPhan)) {
                s1.setInt(1, maSPCombo);
                s1.executeUpdate();
            }
            try (PreparedStatement s2 = conn.prepareStatement(sqlXoaSP)) {
                s2.setInt(1, maSPCombo);
                return s2.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("Loi xoa combo (co the dang duoc dung trong don hang): " + e.getMessage());
            return false;
        }
    }
}
