import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class NhanVienDAO {

    public List<NhanVien> layTatCa() {
        List<NhanVien> list = new ArrayList<>();
        String sql = "SELECT nv.MaNV, nv.HoTen, nv.SoDienThoai, nv.LoaiHopDong, " +
                     "tk.TenDangNhap, tk.VaiTro " +
                     "FROM NhanVien nv JOIN TaiKhoan tk ON nv.MaTK = tk.MaTK " +
                     "ORDER BY nv.MaNV";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new NhanVien(
                        rs.getInt("MaNV"),
                        rs.getString("HoTen"),
                        rs.getString("SoDienThoai"),
                        rs.getString("LoaiHopDong"),
                        rs.getString("TenDangNhap"),
                        rs.getString("VaiTro")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Loi lay danh sach nhan vien: " + e.getMessage());
        }
        return list;
    }

    // Tao tai khoan + nhan vien cung luc. Tra ve true neu thanh cong
    public boolean them(String hoTen, String soDienThoai, String loaiHopDong,
                         String tenDangNhap, String matKhau, String vaiTro) {
        // Validate SDT: 10 so, bat dau bang 0
        if (!soDienThoai.matches("^0\\d{9}$")) {
            System.out.println("So dien thoai khong hop le (can 10 so, bat dau bang 0).");
            return false;
        }
        // Validate Mat khau: toi thieu 6 ky tu
        if (matKhau == null || matKhau.length() < 6) {
            System.out.println("Mat khau phai co it nhat 6 ky tu.");
            return false;
        }
        // Kiem tra trung SDT
        String sqlKiemTraSDT = "SELECT COUNT(*) FROM NhanVien WHERE SoDienThoai = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement sCheck = conn.prepareStatement(sqlKiemTraSDT)) {
            sCheck.setString(1, soDienThoai);
            try (ResultSet rs = sCheck.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("So dien thoai da duoc su dung.");
                    return false;
                }
            }
        } catch (SQLException e) {
            System.out.println("Loi kiem tra SDT: " + e.getMessage());
            return false;
        }

        String sqlTaoTK = "INSERT INTO TaiKhoan (TenDangNhap, MatKhau, VaiTro) VALUES (?, ?, ?)";
        String sqlTaoNV = "INSERT INTO NhanVien (HoTen, SoDienThoai, MaTK, LoaiHopDong) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            int maTK;
            try (PreparedStatement s1 = conn.prepareStatement(sqlTaoTK, Statement.RETURN_GENERATED_KEYS)) {
                s1.setString(1, tenDangNhap);
                s1.setString(2, matKhau);
                s1.setString(3, vaiTro);
                s1.executeUpdate();
                try (ResultSet rs = s1.getGeneratedKeys()) {
                    if (!rs.next()) return false;
                    maTK = rs.getInt(1);
                }
            }
            try (PreparedStatement s2 = conn.prepareStatement(sqlTaoNV)) {
                s2.setString(1, hoTen);
                s2.setString(2, soDienThoai);
                s2.setInt(3, maTK);
                s2.setString(4, loaiHopDong);
                s2.executeUpdate();
            }
            return true;

        } catch (SQLException e) {
            System.out.println("Loi them nhan vien (co the trung ten dang nhap): " + e.getMessage());
            return false;
        }
    }

    // Cap nhat thong tin nhan vien va vai tro tai khoan (khong doi mat khau o day)
    public boolean capNhat(int maNV, String hoTen, String soDienThoai, String loaiHopDong, String vaiTroMoi) {
        // Validate SDT
        if (!soDienThoai.matches("^0\\d{9}$")) {
            System.out.println("So dien thoai khong hop le.");
            return false;
        }
        // Kiem tra trung SDT (tru chinh no ra)
        String sqlKiemTraSDT = "SELECT COUNT(*) FROM NhanVien WHERE SoDienThoai = ? AND MaNV <> ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement sCheck = conn.prepareStatement(sqlKiemTraSDT)) {
            sCheck.setString(1, soDienThoai);
            sCheck.setInt(2, maNV);
            try (ResultSet rs = sCheck.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("So dien thoai da duoc su dung boi nhan vien khac.");
                    return false;
                }
            }
        } catch (SQLException e) {
            System.out.println("Loi kiem tra SDT: " + e.getMessage());
            return false;
        }

        String sqlNV = "UPDATE NhanVien SET HoTen = ?, SoDienThoai = ?, LoaiHopDong = ? WHERE MaNV = ?";
        String sqlTK = "UPDATE TaiKhoan SET VaiTro = ? WHERE MaTK = (SELECT MaTK FROM NhanVien WHERE MaNV = ?)";

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement s1 = conn.prepareStatement(sqlNV)) {
                s1.setString(1, hoTen);
                s1.setString(2, soDienThoai);
                s1.setString(3, loaiHopDong);
                s1.setInt(4, maNV);
                s1.executeUpdate();
            }
            try (PreparedStatement s2 = conn.prepareStatement(sqlTK)) {
                s2.setString(1, vaiTroMoi);
                s2.setInt(2, maNV);
                s2.executeUpdate();
            }
            return true;

        } catch (SQLException e) {
            System.out.println("Loi cap nhat nhan vien: " + e.getMessage());
            return false;
        }
    }

    // Xoa nhan vien va tai khoan lien ket
    public boolean xoa(int maNV) {
        String sqlLayMaTK = "SELECT MaTK FROM NhanVien WHERE MaNV = ?";
        String sqlXoaNV = "DELETE FROM NhanVien WHERE MaNV = ?";
        String sqlXoaTK = "DELETE FROM TaiKhoan WHERE MaTK = ?";

        try (Connection conn = DBConnection.getConnection()) {
            int maTK = -1;
            try (PreparedStatement s1 = conn.prepareStatement(sqlLayMaTK)) {
                s1.setInt(1, maNV);
                try (ResultSet rs = s1.executeQuery()) {
                    if (rs.next()) maTK = rs.getInt("MaTK");
                }
            }
            try (PreparedStatement s2 = conn.prepareStatement(sqlXoaNV)) {
                s2.setInt(1, maNV);
                s2.executeUpdate();
            }
            if (maTK != -1) {
                try (PreparedStatement s3 = conn.prepareStatement(sqlXoaTK)) {
                    s3.setInt(1, maTK);
                    s3.executeUpdate();
                }
            }
            return true;

        } catch (SQLException e) {
            System.out.println("Loi xoa nhan vien (co the dang co du lieu lien quan nhu don hang/ca lam viec): " + e.getMessage());
            return false;
        }
    }
}
