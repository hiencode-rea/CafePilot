import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static boolean daBoSungCotSoNguoi = false;
    private static boolean daBoSungCotViTriBan = false;

    // Thong tin ket noi - sua lai neu ban doi ten instance/database/mat khau
    private static final String SERVER_NAME = System.getenv().getOrDefault("CAFE_DB_SERVER", "localhost");
    private static final String PORT = System.getenv().getOrDefault("CAFE_DB_PORT", "1433");
    private static final String DATABASE_NAME = System.getenv().getOrDefault("CAFE_DB_NAME", "QuanLyCafe");
    private static final String USER = System.getenv().getOrDefault("CAFE_DB_USER", "cafe_app");
    private static final String PASSWORD = System.getenv().getOrDefault("CAFE_DB_PASSWORD", "");

    private static final String URL =
        "jdbc:sqlserver://" + SERVER_NAME + ":" + PORT + ";"
        + "databaseName=" + DATABASE_NAME + ";"
        + "encrypt=true;"
        + "trustServerCertificate=true;";

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
        boSungCotSoNguoiNeuCan(conn);
        boSungCotViTriBanNeuCan(conn);
        return conn;
    }

    private static synchronized void boSungCotViTriBanNeuCan(Connection conn) throws SQLException {
        if (daBoSungCotViTriBan) return;
        String sql = "IF COL_LENGTH('dbo.Ban', 'ViTriX') IS NULL " +
                     "BEGIN ALTER TABLE dbo.Ban ADD ViTriX float NULL, ViTriY float NULL END";
        try (java.sql.Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            daBoSungCotViTriBan = true;
        }
    }

    private static synchronized void boSungCotSoNguoiNeuCan(Connection conn) throws SQLException {
        if (daBoSungCotSoNguoi) return;
        String sql = "IF COL_LENGTH('dbo.DonHang', 'SoNguoi') IS NULL " +
                     "BEGIN ALTER TABLE dbo.DonHang ADD SoNguoi int NOT NULL " +
                     "CONSTRAINT DF_DonHang_SoNguoi DEFAULT 0 END";
        try (java.sql.Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            daBoSungCotSoNguoi = true;
        }
    }

    // Ham main de test ket noi rieng, khong lien quan den logic ung dung
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                System.out.println("Ket noi CSDL thanh cong!");
            }
        } catch (SQLException e) {
            System.out.println("Ket noi that bai: " + e.getMessage());
        }
    }
}