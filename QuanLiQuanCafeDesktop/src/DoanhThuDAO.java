import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DoanhThuDAO {

    // Doanh thu tung ngay trong khoang [tuNgay, denNgay], chi tinh don da Hoan thanh
    public java.util.List<Object[]> layDoanhThuTheoNgay(String tuNgay, String denNgay) {
        java.util.List<Object[]> list = new java.util.ArrayList<>();
        String sql = "SELECT CAST(ThoiGianTao AS DATE) AS Ngay, SUM(TongTien) AS Tong, COUNT(*) AS SoDon " +
                     "FROM DonHang WHERE TrangThai = N'HoanThanh' " +
                     "AND CAST(ThoiGianTao AS DATE) BETWEEN ? AND ? " +
                     "GROUP BY CAST(ThoiGianTao AS DATE) ORDER BY Ngay";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(tuNgay));
            stmt.setDate(2, Date.valueOf(denNgay));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{
                            rs.getDate("Ngay").toString(),
                            rs.getDouble("Tong"),
                            rs.getInt("SoDon")
                    });
                }
            }
        } catch (SQLException e) {
            System.out.println("Loi lay doanh thu theo ngay: " + e.getMessage());
        }
        return list;
    }

    // Doanh thu tung thang trong 1 nam
    public java.util.List<Object[]> layDoanhThuTheoThang(int nam) {
        java.util.List<Object[]> list = new java.util.ArrayList<>();
        String sql = "SELECT MONTH(ThoiGianTao) AS Thang, SUM(TongTien) AS Tong, COUNT(*) AS SoDon " +
                     "FROM DonHang WHERE TrangThai = N'HoanThanh' AND YEAR(ThoiGianTao) = ? " +
                     "GROUP BY MONTH(ThoiGianTao) ORDER BY Thang";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, nam);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{
                            "Thang " + rs.getInt("Thang"),
                            rs.getDouble("Tong"),
                            rs.getInt("SoDon")
                    });
                }
            }
        } catch (SQLException e) {
            System.out.println("Loi lay doanh thu theo thang: " + e.getMessage());
        }
        return list;
    }

    // Tong doanh thu + tong so don trong 1 ngay cu the (dung cho "theo ca/ngay")
    public Object[] layTongTheoNgay(String ngay) {
        String sql = "SELECT ISNULL(SUM(TongTien),0) AS Tong, COUNT(*) AS SoDon " +
                     "FROM DonHang WHERE TrangThai = N'HoanThanh' AND CAST(ThoiGianTao AS DATE) = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(ngay));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Object[]{ rs.getDouble("Tong"), rs.getInt("SoDon") };
                }
            }
        } catch (SQLException e) {
            System.out.println("Loi lay tong doanh thu ngay: " + e.getMessage());
        }
        return new Object[]{ 0.0, 0 };
    }
}
