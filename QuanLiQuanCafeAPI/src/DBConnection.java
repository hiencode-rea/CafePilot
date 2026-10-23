import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // Dùng cùng endpoint SQL Server với ứng dụng Desktop để hai ứng dụng
    // chắc chắn đọc/ghi chung một database QuanLyCafe.
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
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
