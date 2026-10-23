import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LuongDAO {

    // Tra ve Map<"Ten (LoaiHopDong)", MaNV> de do vao ComboBox
    public Map<String, Integer> layDanhSachNhanVien() {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT MaNV, HoTen, LoaiHopDong FROM NhanVien ORDER BY HoTen";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                map.put(rs.getString("HoTen") + " (" + rs.getString("LoaiHopDong") + ")", rs.getInt("MaNV"));
            }
        } catch (SQLException e) {
            System.out.println("Loi lay danh sach nhan vien: " + e.getMessage());
        }
        return map;
    }

    public String layLoaiHopDong(int maNV) {
        String sql = "SELECT LoaiHopDong FROM NhanVien WHERE MaNV = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, maNV);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("LoaiHopDong");
            }
        } catch (SQLException e) {
            System.out.println("Loi lay loai hop dong: " + e.getMessage());
        }
        return null;
    }

    // Tinh tong gio lam thuc te trong thang/nam dua tren du lieu Cham cong (chi tinh ca da co GioRa)
    public double tinhTongGioLam(int maNV, int thang, int nam) {
        String sql = "SELECT SUM(DATEDIFF(MINUTE, cc.GioVao, cc.GioRa)) AS TongPhut " +
                     "FROM ChamCong cc JOIN CaLamViec c ON cc.MaCa = c.MaCa " +
                     "WHERE c.MaNV = ? AND MONTH(c.Ngay) = ? AND YEAR(c.Ngay) = ? " +
                     "AND cc.GioRa IS NOT NULL";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, maNV);
            stmt.setInt(2, thang);
            stmt.setInt(3, nam);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int phut = rs.getInt("TongPhut");
                    return Math.round((phut / 60.0) * 100.0) / 100.0;
                }
            }
        } catch (SQLException e) {
            System.out.println("Loi tinh tong gio lam: " + e.getMessage());
        }
        return 0;
    }

    // Luu ket qua tinh luong: neu da co ban ghi cho NV/thang/nam nay thi cap nhat, chua co thi them moi
    public boolean luuLuong(int maNV, int thang, int nam, double tongGioLam, double donGiaGio,
                             double luongCoBan, double phuCap, double thuong, double phat, double thucLinh) {
        String sqlKiemTra = "SELECT MaLuong FROM Luong WHERE MaNV = ? AND Thang = ? AND Nam = ?";
        String sqlCapNhat = "UPDATE Luong SET TongGioLam=?, DonGiaGio=?, LuongCoBan=?, PhuCap=?, Thuong=?, Phat=?, ThucLinh=? " +
                             "WHERE MaLuong = ?";
        String sqlThem = "INSERT INTO Luong (MaNV, Thang, Nam, TongGioLam, DonGiaGio, LuongCoBan, PhuCap, Thuong, Phat, ThucLinh) " +
                          "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            Integer maLuong = null;
            try (PreparedStatement s1 = conn.prepareStatement(sqlKiemTra)) {
                s1.setInt(1, maNV);
                s1.setInt(2, thang);
                s1.setInt(3, nam);
                try (ResultSet rs = s1.executeQuery()) {
                    if (rs.next()) maLuong = rs.getInt("MaLuong");
                }
            }

            if (maLuong != null) {
                try (PreparedStatement s2 = conn.prepareStatement(sqlCapNhat)) {
                    s2.setDouble(1, tongGioLam);
                    s2.setDouble(2, donGiaGio);
                    s2.setDouble(3, luongCoBan);
                    s2.setDouble(4, phuCap);
                    s2.setDouble(5, thuong);
                    s2.setDouble(6, phat);
                    s2.setDouble(7, thucLinh);
                    s2.setInt(8, maLuong);
                    return s2.executeUpdate() > 0;
                }
            } else {
                try (PreparedStatement s3 = conn.prepareStatement(sqlThem)) {
                    s3.setInt(1, maNV);
                    s3.setInt(2, thang);
                    s3.setInt(3, nam);
                    s3.setDouble(4, tongGioLam);
                    s3.setDouble(5, donGiaGio);
                    s3.setDouble(6, luongCoBan);
                    s3.setDouble(7, phuCap);
                    s3.setDouble(8, thuong);
                    s3.setDouble(9, phat);
                    s3.setDouble(10, thucLinh);
                    return s3.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            System.out.println("Loi luu luong: " + e.getMessage());
            return false;
        }
    }

    public List<Luong> layBangLuong(int thang, int nam) {
        List<Luong> list = new ArrayList<>();
        String sql = "SELECT nv.HoTen, nv.LoaiHopDong, l.TongGioLam, l.PhuCap, l.Thuong, l.Phat, l.ThucLinh " +
                     "FROM Luong l JOIN NhanVien nv ON l.MaNV = nv.MaNV " +
                     "WHERE l.Thang = ? AND l.Nam = ? ORDER BY nv.HoTen";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, thang);
            stmt.setInt(2, nam);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Luong(
                            rs.getString("HoTen"),
                            rs.getString("LoaiHopDong"),
                            rs.getDouble("TongGioLam"),
                            rs.getDouble("PhuCap"),
                            rs.getDouble("Thuong"),
                            rs.getDouble("Phat"),
                            rs.getDouble("ThucLinh")
                    ));
                }
            }
        } catch (SQLException e) {
            System.out.println("Loi lay bang luong: " + e.getMessage());
        }
        return list;
    }
}
