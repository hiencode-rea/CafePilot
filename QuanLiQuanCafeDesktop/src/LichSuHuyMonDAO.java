import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LichSuHuyMonDAO {

    public boolean ghiNhan(String tenSP, int soLuong, String lyDo, String nguoiThucHien) {
        String sql = "INSERT INTO LichSuHuyMon (TenSP, SoLuong, LyDo, NguoiThucHien) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, tenSP);
            stmt.setInt(2, soLuong);
            stmt.setString(3, lyDo);
            stmt.setString(4, nguoiThucHien);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Loi ghi nhan lich su huy mon: " + e.getMessage());
            return false;
        }
    }
}
