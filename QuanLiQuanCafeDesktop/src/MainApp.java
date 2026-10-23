import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class MainApp {

    private static final List<Button> dsNutMenu = new ArrayList<>();
    private static Timeline dongHo; // kiểm tra yêu cầu thanh toán từ Mobile định kỳ

    // Mở màn hình chính: menu bên trái thay đổi theo vai trò, trang chủ kiểu bảng điều khiển
    public static void moManHinhChinh(Stage stage, String tenDangNhap, String vaiTro) {
        dsNutMenu.clear();
        String hoTen = layHoTen(tenDangNhap);
        boolean laQuanLy = vaiTro.equalsIgnoreCase("QuanLy");
        boolean duocXemDoanhThu = laQuanLy || vaiTro.equalsIgnoreCase("ThuNgan");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-bg-light");

        // ===== Thanh trên cùng =====
        ImageView logoImage = new ImageView(new Image(
                MainApp.class.getResourceAsStream("/app-icon.png")));
        logoImage.setFitWidth(30);
        logoImage.setFitHeight(30);
        logoImage.setPreserveRatio(true);
        StackPane logoContainer = new StackPane(logoImage);
        Label lblLogo = new Label();
        lblLogo.setGraphic(logoContainer);
        lblLogo.getStyleClass().add("top-bar-title");
        Label lblApp = new Label("Quản lý cửa hàng cà phê");
        lblApp.getStyleClass().add("top-bar-title");

        Label avatar = new Label(hoTen.isEmpty() ? "?" : hoTen.substring(0, 1).toUpperCase());
        avatar.getStyleClass().add("avatar");
        Label lblUser = new Label(hoTen);
        lblUser.getStyleClass().add("top-bar-user");
        Label lblVaiTro = new Label(vaiTro);
        lblVaiTro.getStyleClass().add("role-badge");

        Button btnDangXuat = new Button("Đăng xuất");
        btnDangXuat.getStyleClass().add("btn-outline-light");
        btnDangXuat.setOnAction(e -> {
            dungDongHo();
            try {
                new LoginApp().start(stage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Chuông: số yêu cầu thanh toán Mobile đang chờ thu ngân xác nhận
        Button btnChuong = new Button("\uD83D\uDD14 Yêu cầu thanh toán");
        btnChuong.getStyleClass().add("btn-outline-light");
        btnChuong.setOnAction(e -> YeuCauThanhToanDialog.moPopup(tenDangNhap, () -> capNhatChuong(btnChuong)));
        capNhatChuong(btnChuong);
        batDauDongHo(btnChuong);

        HBox topBar = new HBox(10, lblLogo, lblApp, spacer, btnChuong, avatar, lblUser, lblVaiTro, btnDangXuat);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(12, 24, 12, 24));

        // ===== Menu bên trái =====
        VBox menu = new VBox(4);
        menu.getStyleClass().add("menu-bar");
        menu.setPadding(new Insets(16, 12, 16, 12));
        menu.setPrefWidth(230);

        Map<String, Button> nut = new LinkedHashMap<>();

        Button btnTrangChu = taoNut("\u2302  Trang chủ");
        menu.getChildren().add(btnTrangChu);

        Button btnBan = taoNut("\u25A6  Bàn / Đơn hàng");
        nut.put("Bàn / Đơn hàng", btnBan);
        menu.getChildren().add(btnBan);

        if (laQuanLy) {
            Label nhomQuanTri = new Label("QUẢN TRỊ");
            nhomQuanTri.getStyleClass().add("menu-group");
            menu.getChildren().add(nhomQuanTri);

            Button btnSanPham = taoNut("\u2615  Sản phẩm");
            Button btnCombo = taoNut("\u2630  Combo");
            Button btnNhanVien = taoNut("\u263A  Nhân viên");
            Button btnCa = taoNut("\u23F1  Ca làm việc / Chấm công");
            Button btnLuong = taoNut("\u2756  Tính lương");

            nut.put("Sản phẩm", btnSanPham);
            nut.put("Combo", btnCombo);
            nut.put("Nhân viên", btnNhanVien);
            nut.put("Chấm công", btnCa);
            nut.put("Tính lương", btnLuong);

            menu.getChildren().addAll(btnSanPham, btnCombo, btnNhanVien, btnCa, btnLuong);

            gan(root, btnSanPham, () -> new QuanLySanPhamView().taoGiaoDien());
            gan(root, btnCombo, () -> new QuanLyComboView().taoGiaoDien());
            gan(root, btnNhanVien, () -> new QuanLyNhanVienView().taoGiaoDien());
            gan(root, btnCa, () -> new QuanLyCaChamCongView().taoGiaoDien());
            gan(root, btnLuong, () -> new TinhLuongView().taoGiaoDien());
        }

        if (duocXemDoanhThu) {
            Button btnDoanhThu = taoNut("\u2197  Thống kê doanh thu");
            nut.put("Doanh thu", btnDoanhThu);
            menu.getChildren().add(btnDoanhThu);
            gan(root, btnDoanhThu, () -> new ThongKeDoanhThuView().taoGiaoDien());
        }

        gan(root, btnBan, () -> new QuanLyBanDonHangView(tenDangNhap, vaiTro).taoGiaoDien());
        gan(root, btnTrangChu, () -> taoTrangChu(hoTen, nut));

        // Hiện trang chủ ngay khi vào
        btnTrangChu.fire();

        root.setTop(topBar);
        root.setLeft(menu);

        Scene scene = new Scene(root, 1180, 740);
        scene.getStylesheets().add(MainApp.class.getResource("style.css").toExternalForm());

        stage.setTitle("Quản lý cửa hàng cà phê - " + vaiTro);
        Image windowIcon = new Image(MainApp.class.getResourceAsStream("/app-icon.png"));
        stage.getIcons().setAll(windowIcon);
        stage.setScene(scene);
        stage.show();
    }

    private static void capNhatChuong(Button btn) {
        datChuong(btn, new DonHangDAO().demYeuCauThanhToan());
    }

    private static void datChuong(Button btn, int soYeuCau) {
        btn.getStyleClass().remove("bell-active");
        if (soYeuCau > 0) {
            btn.setText("\uD83D\uDD14 Yêu cầu thanh toán (" + soYeuCau + ")");
            btn.getStyleClass().add("bell-active");
        } else {
            btn.setText("\uD83D\uDD14 Yêu cầu thanh toán");
        }
    }

    private static void batDauDongHo(Button btn) {
        dungDongHo();
        dongHo = new Timeline(new KeyFrame(Duration.seconds(4), e -> {
            Thread t = new Thread(() -> {
                int n = new DonHangDAO().demYeuCauThanhToan();
                Platform.runLater(() -> datChuong(btn, n));
            });
            t.setDaemon(true);
            t.start();
        }));
        dongHo.setCycleCount(Timeline.INDEFINITE);
        dongHo.play();
    }

    private static void dungDongHo() {
        if (dongHo != null) dongHo.stop();
    }

    // Gắn hành động chọn mục menu: đánh dấu mục đang chọn + đổi nội dung bên phải
    private static void gan(BorderPane root, Button btn, Supplier<Node> taoNoiDung) {
        btn.setOnAction(e -> {
            for (Button b : dsNutMenu) b.getStyleClass().remove("btn-menu-selected");
            btn.getStyleClass().add("btn-menu-selected");
            Node noiDung = taoNoiDung.get();
            ScrollPane cuon = new ScrollPane(noiDung);
            cuon.setFitToWidth(true);
            cuon.getStyleClass().add("board-scroll");
            root.setCenter(cuon);
        });
    }

    private static Button taoNut(String ten) {
        Button btn = new Button(ten);
        btn.getStyleClass().add("btn-menu");
        btn.setMaxWidth(Double.MAX_VALUE);
        dsNutMenu.add(btn);
        return btn;
    }

    // ===== Trang chủ: lời chào + lối tắt + thống kê nhanh + bàn đang phục vụ =====
    private static Node taoTrangChu(String hoTen, Map<String, Button> nut) {
        int gio = LocalTime.now().getHour();
        String loiChao = gio < 12 ? "Chào buổi sáng" : (gio < 18 ? "Chào buổi chiều" : "Chào buổi tối");

        ImageView homeIcon = new ImageView(new Image(
                MainApp.class.getResourceAsStream("/app-icon.png")));
        homeIcon.setFitWidth(48);
        homeIcon.setFitHeight(48);
        homeIcon.setPreserveRatio(true);
        StackPane lblIcon = new StackPane(homeIcon);
        Label lblGreeting = new Label(loiChao + ", " + hoTen + "!");
        lblGreeting.getStyleClass().add("greeting");
        Label lblNgay = new Label("Hôm nay là " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lblNgay.getStyleClass().add("greeting-sub");

        HBox dau = new HBox(14, lblIcon, new VBox(2, lblGreeting, lblNgay));
        dau.setAlignment(Pos.CENTER_LEFT);

        // Lối tắt kiểu "chip"
        FlowPane chips = new FlowPane(10, 10);
        for (Map.Entry<String, Button> m : nut.entrySet()) {
            Button chip = new Button(m.getKey());
            chip.getStyleClass().add("chip");
            chip.setOnAction(e -> m.getValue().fire());
            chips.getChildren().add(chip);
        }

        // Thống kê nhanh
        BanDAO banDAO = new BanDAO();
        List<Ban> tatCaBan = banDAO.layTatCa();
        List<Ban> banDangPV = new ArrayList<>();
        for (Ban b : tatCaBan) {
            if ("DangPhucVu".equalsIgnoreCase(b.getTrangThai())) banDangPV.add(b);
        }
        Object[] homNay = new DoanhThuDAO().layTongTheoNgay(LocalDate.now().toString());
        double doanhThu = (double) homNay[0];
        int soDon = (int) homNay[1];

        HBox hangThongKe = new HBox(14,
                taoTheThongKe(banDangPV.size() + " / " + tatCaBan.size(), "Bàn đang phục vụ"),
                taoTheThongKe(dinhDangTien(doanhThu) + " VND", "Doanh thu hôm nay"),
                taoTheThongKe(String.valueOf(soDon), "Đơn đã thanh toán hôm nay"));
        for (Node n : hangThongKe.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        // Danh sách bàn đang phục vụ
        Label lblBanPV = new Label("Bàn đang phục vụ");
        lblBanPV.getStyleClass().add("home-section-title");

        VBox khungBanPV = new VBox();
        if (banDangPV.isEmpty()) {
            Label trong = new Label("Hiện không có bàn nào đang phục vụ.");
            trong.getStyleClass().add("empty-state-text");
            VBox o = new VBox(trong);
            o.getStyleClass().add("empty-state");
            o.setAlignment(Pos.CENTER);
            khungBanPV.getChildren().add(o);
        } else {
            int dem = 0;
            for (Ban b : banDangPV) {
                if (dem++ >= 8) break;
                Label ten = new Label("Bàn " + b.getSoHieuBan());
                ten.getStyleClass().add("card-title");
                Label kv = new Label(b.getTenKhuVuc());
                kv.getStyleClass().add("card-subtitle");
                Label badge = new Label("Đang phục vụ");
                badge.getStyleClass().add("badge-dangpv");
                Region gian = new Region();
                HBox.setHgrow(gian, Priority.ALWAYS);
                HBox dong = new HBox(12, ten, kv, gian, badge);
                dong.setAlignment(Pos.CENTER_LEFT);
                dong.getStyleClass().add("home-row");
                khungBanPV.getChildren().add(dong);
            }
        }

        VBox trang = new VBox(22, dau, chips, hangThongKe, lblBanPV, khungBanPV);
        trang.setPadding(new Insets(32, 40, 32, 40));
        trang.setMaxWidth(980);

        HBox canGiua = new HBox(trang);
        canGiua.setAlignment(Pos.TOP_CENTER);
        return canGiua;
    }

    private static VBox taoTheThongKe(String giaTri, String nhan) {
        Label v = new Label(giaTri);
        v.getStyleClass().add("stat-value");
        Label l = new Label(nhan);
        l.getStyleClass().add("stat-label");
        VBox the = new VBox(4, v, l);
        the.getStyleClass().add("stat-card");
        return the;
    }

    private static String dinhDangTien(double so) {
        return String.format("%,.0f", so).replace(',', '.');
    }

    private static String layHoTen(String tenDangNhap) {
        try {
            for (NhanVien nv : new NhanVienDAO().layTatCa()) {
                if (tenDangNhap.equals(nv.getTenDangNhap())) return nv.getHoTen();
            }
        } catch (Exception ignored) { }
        return tenDangNhap;
    }
}
