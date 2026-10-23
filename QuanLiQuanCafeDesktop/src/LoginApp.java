import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginApp extends Application {

    // Vai trò được phép đăng nhập trên bản Desktop (PC).
    // PhucVu, PhaChe dùng ứng dụng Mobile riêng, không đăng nhập ở đây.
    private static final String[] VAI_TRO_DESKTOP = { "QuanLy", "ThuNgan" };

    @Override
    public void start(Stage primaryStage) {

        ImageView logoImage = new ImageView(new Image(
                LoginApp.class.getResourceAsStream("/app-icon.png")));
        logoImage.setFitWidth(64);
        logoImage.setFitHeight(64);
        logoImage.setPreserveRatio(true);
        StackPane lblLogo = new StackPane(logoImage);
        lblLogo.getStyleClass().add("logo");

        Label lblTitle = new Label("Đăng nhập vào hệ thống");
        lblTitle.getStyleClass().add("title");

        Label lblSubtitle = new Label("Quản lý cửa hàng cà phê (Desktop)");
        lblSubtitle.getStyleClass().add("subtitle");

        Label lblUser = new Label("Tên đăng nhập");
        lblUser.getStyleClass().add("field-label");
        TextField txtUser = new TextField();
        txtUser.setPromptText("Nhập tên đăng nhập");
        txtUser.getStyleClass().add("text-field-rounded");

        Label lblPass = new Label("Mật khẩu");
        lblPass.getStyleClass().add("field-label");
        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Nhập mật khẩu");
        txtPass.getStyleClass().add("text-field-rounded");

        Button btnLogin = new Button("Đăng nhập");
        btnLogin.getStyleClass().add("btn-primary");
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setDefaultButton(true);

        Label lblMessage = new Label();
        lblMessage.getStyleClass().add("message-label");
        lblMessage.setWrapText(true);

        Runnable dangNhap = () -> {
            String tenDangNhap = txtUser.getText().trim();
            String matKhau = txtPass.getText().trim();

            if (tenDangNhap.isEmpty() || matKhau.isEmpty()) {
                lblMessage.getStyleClass().setAll("label", "message-label", "message-error");
                lblMessage.setText("Vui lòng nhập đầy đủ thông tin.");
                return;
            }

            String vaiTro = kiemTraDangNhap(tenDangNhap, matKhau);

            if (vaiTro == null) {
                lblMessage.getStyleClass().setAll("label", "message-label", "message-error");
                lblMessage.setText("Sai tên đăng nhập hoặc mật khẩu.");
                return;
            }

            if (!coQuyenDungDesktop(vaiTro)) {
                lblMessage.getStyleClass().setAll("label", "message-label", "message-error");
                lblMessage.setText("Vai trò '" + vaiTro + "' vui lòng dùng ứng dụng Mobile.");
                return;
            }

            lblMessage.getStyleClass().setAll("label", "message-label", "message-success");
            lblMessage.setText("Đăng nhập thành công!");

            MainApp.moManHinhChinh(primaryStage, tenDangNhap, vaiTro);
        };
        btnLogin.setOnAction(e -> dangNhap.run());

        VBox truongUser = new VBox(6, lblUser, txtUser);
        VBox truongPass = new VBox(6, lblPass, txtPass);

        VBox card = new VBox(16, truongUser, truongPass, btnLogin, lblMessage);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(22, 22, 20, 22));
        card.setMaxWidth(340);

        VBox dau = new VBox(8, lblLogo, lblTitle, lblSubtitle);
        dau.setAlignment(Pos.CENTER);

        VBox noiDung = new VBox(24, dau, card);
        noiDung.setAlignment(Pos.CENTER);

        StackPane root = new StackPane(noiDung);
        root.getStyleClass().add("root-bg");

        Scene scene = new Scene(root, 460, 600);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        primaryStage.setTitle("Đăng nhập - Quản lý cửa hàng cà phê");
        Image windowIcon = new Image(LoginApp.class.getResourceAsStream("/app-icon.png"));
        primaryStage.getIcons().setAll(windowIcon);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private boolean coQuyenDungDesktop(String vaiTro) {
        for (String vt : VAI_TRO_DESKTOP) {
            if (vt.equalsIgnoreCase(vaiTro)) return true;
        }
        return false;
    }

    private String kiemTraDangNhap(String tenDangNhap, String matKhau) {
        String sql = "SELECT VaiTro FROM TaiKhoan WHERE TenDangNhap = ? AND MatKhau = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, tenDangNhap);
            stmt.setString(2, matKhau);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("VaiTro");
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi kết nối CSDL: " + e.getMessage());
        }
        return null;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
