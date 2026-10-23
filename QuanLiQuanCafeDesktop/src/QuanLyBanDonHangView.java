import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Group;
import javafx.scene.shape.Rectangle;
import javafx.scene.input.ScrollEvent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.text.DecimalFormat;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QuanLyBanDonHangView {

    private final BanDAO banDAO = new BanDAO();
    private final DonHangDAO donHangDAO = new DonHangDAO();
    private final String tenDangNhap;
    private final String vaiTro;
    private final boolean laQuanLy;

    private final FlowPane luoiThe = new FlowPane();
    private final Label lblSoTrang = new Label();

    private String locKhuVuc = "Tất cả";
    private String locTrangThai = "Tất cả";
    private String tuKhoaTim = "";
    private boolean dangChinhSuaSoDo = false;
    private Timeline dongHoCapNhat;
    private static final DecimalFormat DINH_DANG_TIEN = new DecimalFormat("#,##0");

    public QuanLyBanDonHangView(String tenDangNhap, String vaiTro) {
        this.tenDangNhap = tenDangNhap;
        this.vaiTro = vaiTro == null ? "" : vaiTro;
        this.laQuanLy = "QuanLy".equalsIgnoreCase(this.vaiTro);
    }

    public VBox taoGiaoDien() {
        Label lblTieuDe = new Label("Quản lý bàn / Đơn hàng");
        lblTieuDe.getStyleClass().add("section-title");

        // ----- Thanh loc -----
        List<String> dsKhuVuc = new ArrayList<>();
        dsKhuVuc.add("Tất cả");
        dsKhuVuc.addAll(banDAO.layTatCa().stream().map(Ban::getTenKhuVuc).distinct().collect(Collectors.toList()));

        ComboBox<String> cboKhuVuc = new ComboBox<>(FXCollections.observableArrayList(dsKhuVuc));
        cboKhuVuc.setValue("Tất cả");
        cboKhuVuc.getStyleClass().add("combo-box-rounded");

        ComboBox<String> cboTrangThai = new ComboBox<>(FXCollections.observableArrayList(
                "Tất cả", "Trống", "Đang phục vụ"
        ));
        cboTrangThai.setValue("Tất cả");
        cboTrangThai.getStyleClass().add("combo-box-rounded");

        TextField txtTim = new TextField();
        txtTim.setPromptText("Tìm số bàn...");
        txtTim.getStyleClass().add("text-field-rounded");
        txtTim.setPrefWidth(140);

        cboKhuVuc.setOnAction(e -> { locKhuVuc = cboKhuVuc.getValue(); veLaiLuoi(); });
        cboTrangThai.setOnAction(e -> { locTrangThai = "Trống".equals(cboTrangThai.getValue()) ? "Trong" : ("Đang phục vụ".equals(cboTrangThai.getValue()) ? "DangPhucVu" : "Tất cả"); veLaiLuoi(); });
        txtTim.textProperty().addListener((obs, cu, moi) -> { tuKhoaTim = moi.trim(); veLaiLuoi(); });

        Button btnThemBan = new Button("Thêm bàn");
        btnThemBan.getStyleClass().add("btn-primary");
        btnThemBan.setOnAction(e -> xuLyThemBan());

        Button btnThemKhuVuc = new Button("Thêm khu vực");
        btnThemKhuVuc.getStyleClass().add("btn-outline");
        btnThemKhuVuc.setOnAction(e -> xuLyThemKhuVuc());

        Button btnChinhSuaSoDo = new Button("Chỉnh sửa sơ đồ");
        btnChinhSuaSoDo.getStyleClass().add("btn-outline");
        btnChinhSuaSoDo.setOnAction(e -> {
            if (!laQuanLy) {
                thongBaoKhongDuQuyen();
                return;
            }
            dangChinhSuaSoDo = !dangChinhSuaSoDo;
            btnChinhSuaSoDo.setText(dangChinhSuaSoDo ? "Hoàn tất sắp xếp" : "Chỉnh sửa sơ đồ");
            btnChinhSuaSoDo.getStyleClass().removeAll("btn-primary", "btn-outline");
            btnChinhSuaSoDo.getStyleClass().add(dangChinhSuaSoDo ? "btn-primary" : "btn-outline");
            veLaiLuoi();
        });
        HBox thanhLoc = new HBox(14,
                nhanVaO("Khu vực:", cboKhuVuc),
                nhanVaO("Trạng thái:", cboTrangThai),
                nhanVaO("Tìm bàn:", txtTim),
                taoNutLamMoi(),
                taoNhomNutQuanLy(btnThemBan),
                taoNhomNutQuanLy(btnThemKhuVuc),
                taoNhomNutQuanLy(btnChinhSuaSoDo)
        );
        thanhLoc.setAlignment(Pos.CENTER_LEFT);
        thanhLoc.getStyleClass().add("filter-bar");
        thanhLoc.setPadding(new Insets(14));

        // ----- So do mat bang mini-map -----
        luoiThe.setHgap(18);
        luoiThe.setVgap(18);
        luoiThe.setPadding(new Insets(16, 0, 0, 0));
        luoiThe.setPrefWrapLength(980);
        veLaiLuoi();
        dongHoCapNhat = new Timeline(new KeyFrame(Duration.seconds(30), e -> veLaiLuoi()));
        dongHoCapNhat.setCycleCount(Timeline.INDEFINITE);
        dongHoCapNhat.play();

        VBox root = new VBox(16, lblTieuDe, thanhLoc, luoiThe);
        root.setPadding(new Insets(24));
        return root;
    }

    private VBox nhanVaO(String nhan, javafx.scene.control.Control o) {
        Label lbl = new Label(nhan);
        lbl.getStyleClass().add("field-label");
        return new VBox(4, lbl, o);
    }

    private VBox taoNhomNut(Button nut) {
        Label dongCanh = new Label(" ");
        dongCanh.getStyleClass().add("field-label");
        return new VBox(4, dongCanh, nut);
    }

    private VBox taoNhomNutQuanLy(Button nut) {
        VBox nhom = taoNhomNut(nut);
        nhom.setVisible(laQuanLy);
        nhom.setManaged(laQuanLy);
        return nhom;
    }

    private void veLaiLuoi() {
        luoiThe.getChildren().clear();

        List<Ban> tatCa = banDAO.layTatCa();
        List<Ban> daLoc = tatCa.stream()
                .filter(b -> locKhuVuc.equals("Tất cả") || b.getTenKhuVuc().equals(locKhuVuc))
                .filter(b -> locTrangThai.equals("Tất cả") || b.getTrangThai().equalsIgnoreCase(locTrangThai))
                .filter(b -> tuKhoaTim.isEmpty() || b.getSoHieuBan().contains(tuKhoaTim))
                .collect(Collectors.toList());

        Map<String, List<Ban>> theoKhuVuc = new LinkedHashMap<>();
        for (Ban ban : daLoc) {
            theoKhuVuc.computeIfAbsent(ban.getTenKhuVuc(), k -> new ArrayList<>()).add(ban);
        }

        int thuTuKhuVuc = 0;
        for (Map.Entry<String, List<Ban>> entry : theoKhuVuc.entrySet()) {
            luoiThe.getChildren().add(taoKhuVucMiniMap(entry.getKey(), entry.getValue(), thuTuKhuVuc++));
        }

        if (daLoc.isEmpty()) {
            Label rong = new Label("Không có bàn phù hợp với bộ lọc hiện tại.");
            rong.getStyleClass().add("empty-state-text");
            VBox khungRong = new VBox(rong);
            khungRong.getStyleClass().add("empty-state");
            luoiThe.getChildren().add(khungRong);
        }

        lblSoTrang.setText("Sơ đồ mặt bằng  •  " + daLoc.size() + " bàn  •  " + theoKhuVuc.size() + " khu vực");
    }

    private VBox taoKhuVucMiniMap(String tenKhuVuc, List<Ban> dsBan, int thuTu) {
        Label tieuDe = new Label("▦  " + tenKhuVuc);
        tieuDe.getStyleClass().add("mini-map-room-title");

        Label chuThich = new Label(dsBan.size() + " bàn");
        chuThich.getStyleClass().add("card-subtitle");
        Region gian = new Region();
        HBox.setHgrow(gian, Priority.ALWAYS);
        HBox dauPhong = new HBox(10, tieuDe, gian, chuThich);
        dauPhong.setAlignment(Pos.CENTER_LEFT);

        Pane luoiPhong = new Pane();
        luoiPhong.getStyleClass().add("mini-map-grid");
        luoiPhong.setPrefSize(800, 640);
        luoiPhong.setMinSize(800, 640);

        Group noiDungMiniMap = new Group(luoiPhong);
        StackPane khungMiniMap = new StackPane(noiDungMiniMap);
        khungMiniMap.setPrefSize(800, 640);
        khungMiniMap.setMinSize(800, 640);
        khungMiniMap.setMaxSize(800, 640);
        Rectangle clipMiniMap = new Rectangle(800, 640);
        clipMiniMap.setArcWidth(16);
        clipMiniMap.setArcHeight(16);
        khungMiniMap.setClip(clipMiniMap);
        final double[] mucThuPhong = {1.0};
        luoiPhong.addEventFilter(ScrollEvent.SCROLL, e -> {
            double buoc = e.getDeltaY() > 0 ? 0.1 : -0.1;
            mucThuPhong[0] = Math.max(0.6, Math.min(1.8, mucThuPhong[0] + buoc));
            noiDungMiniMap.setScaleX(mucThuPhong[0]);
            noiDungMiniMap.setScaleY(mucThuPhong[0]);
            e.consume();
        });
        luoiPhong.setOnMouseEntered(e -> luoiPhong.setCursor(javafx.scene.Cursor.OPEN_HAND));
        luoiPhong.setOnMouseExited(e -> luoiPhong.setCursor(javafx.scene.Cursor.DEFAULT));

        double[][] viTriBan = taoViTriBan(dsBan.size());
        for (int i = 0; i < dsBan.size(); i++) {
            Ban ban = dsBan.get(i);
            VBox theBan = taoTheBan(ban);
            double x = ban.getViTriX() >= 0 ? ban.getViTriX() : viTriBan[i][0];
            double y = ban.getViTriY() >= 0 ? ban.getViTriY() : viTriBan[i][1];
            ganSuKienKeoTha(theBan, ban, luoiPhong, x, y);
            theBan.setLayoutX(x);
            theBan.setLayoutY(y);
            luoiPhong.getChildren().add(theBan);
        }

        VBox phong = new VBox(14, dauPhong, khungMiniMap);
        phong.getStyleClass().addAll("mini-map-room", thuTu % 2 == 0 ? "mini-map-room-a" : "mini-map-room-b");
        phong.setPrefWidth(820);
        phong.setMinWidth(820);
        phong.setPadding(new Insets(16));
        return phong;
    }

    private double[][] taoViTriBan(int soLuong) {
        double[][] mau = {
                {24, 24}, {300, 24}, {576, 24},
                {150, 250}, {450, 250},
                {24, 476}, {300, 476}, {576, 476}
        };
        double[][] ketQua = new double[soLuong][2];
        for (int i = 0; i < soLuong; i++) {
            if (i < mau.length) {
                ketQua[i][0] = mau[i][0];
                ketQua[i][1] = mau[i][1];
            } else {
                int cot = (i - mau.length) % 3;
                int hang = (i - mau.length) / 3;
                ketQua[i][0] = 24 + cot * 276;
                ketQua[i][1] = 24 + hang * 226;
            }
        }
        return ketQua;
    }

    private VBox taoTheBan(Ban ban) {
        boolean trong = "Trong".equalsIgnoreCase(ban.getTrangThai());
        int soNguoi = Math.max(0, Math.min(ban.getSoNguoi(), ban.getSucChua()));

        Button btnXemChiTiet = new Button("Xem chi tiết");
        btnXemChiTiet.getStyleClass().add("btn-card-action");
        btnXemChiTiet.setOnAction(e -> {
            ChiTietDonHangDialog.moPopup(ban, tenDangNhap, this::veLaiLuoi);
        });

        Button btnThemMon = new Button("Thêm món");
        btnThemMon.getStyleClass().add("btn-success");
        btnThemMon.setOnAction(e -> ChiTietDonHangDialog.moPopup(ban, tenDangNhap, this::veLaiLuoi));

        Button btnSua = new Button("Sửa bàn");
        btnSua.getStyleClass().add("btn-update");
        btnSua.setOnAction(e -> {
            xuLySuaBan(ban);
            e.consume();
        });

        Button btnXoa = new Button("Xóa bàn");
        btnXoa.getStyleClass().add("btn-danger");
        btnXoa.setOnAction(e -> {
            xuLyXoaBan(ban.getMaBan(), ban.getSoHieuBan());
            e.consume();
        });

        StackPane soDoBan = taoSoDoBan(ban, soNguoi);

        VBox thaoTacDonHang = new VBox(5, btnXemChiTiet, btnThemMon);
        thaoTacDonHang.getStyleClass().add("table-hover-actions");
        thaoTacDonHang.setAlignment(Pos.CENTER);
        thaoTacDonHang.setMouseTransparent(dangChinhSuaSoDo);

        HBox thaoTacQuanTri = new HBox(6, btnSua, btnXoa);
        thaoTacQuanTri.getStyleClass().add("table-edit-actions");
        thaoTacQuanTri.setAlignment(Pos.CENTER);
        thaoTacQuanTri.setVisible(dangChinhSuaSoDo);
        thaoTacQuanTri.setManaged(dangChinhSuaSoDo);
        thaoTacQuanTri.setOnMousePressed(e -> e.consume());

        VBox lopThaoTac = new VBox(6, thaoTacDonHang, thaoTacQuanTri);
        lopThaoTac.setAlignment(Pos.CENTER);
        lopThaoTac.setMouseTransparent(false);

        StackPane lopBan = new StackPane(soDoBan, lopThaoTac);
        StackPane.setAlignment(lopThaoTac, Pos.CENTER);

        VBox the = new VBox(lopBan);
        the.getStyleClass().addAll("mini-map-table",
                trong ? "mini-map-table-empty" : "mini-map-table-serving");
        the.setPrefWidth(150);
        the.setMinWidth(150);
        the.setMaxWidth(150);
        return the;
    }

    private void ganSuKienKeoTha(VBox theBan, Ban ban, Pane luoiPhong, double viTriX, double viTriY) {
        final double[] diemBatDau = new double[2];
        final double[] viTriBatDau = new double[2];

        theBan.setOnMousePressed(e -> {
            if (!laQuanLy || !dangChinhSuaSoDo) return;
            diemBatDau[0] = e.getSceneX();
            diemBatDau[1] = e.getSceneY();
            viTriBatDau[0] = theBan.getLayoutX();
            viTriBatDau[1] = theBan.getLayoutY();
            theBan.setCursor(javafx.scene.Cursor.CLOSED_HAND);
            e.consume();
        });

        theBan.setOnMouseDragged(e -> {
            if (!laQuanLy || !dangChinhSuaSoDo) return;
            double tiLe = Math.max(0.01, luoiPhong.getParent() == null ? 1.0 : luoiPhong.getParent().getScaleX());
            double x = viTriBatDau[0] + (e.getSceneX() - diemBatDau[0]) / tiLe;
            double y = viTriBatDau[1] + (e.getSceneY() - diemBatDau[1]) / tiLe;
            x = Math.max(8, Math.min(642, x));
            y = Math.max(8, Math.min(542, y));
            theBan.setLayoutX(x);
            theBan.setLayoutY(y);
            e.consume();
        });

        theBan.setOnMouseReleased(e -> {
            if (!laQuanLy || !dangChinhSuaSoDo) return;
            theBan.setCursor(javafx.scene.Cursor.OPEN_HAND);
            double x = theBan.getLayoutX();
            double y = theBan.getLayoutY();
            if (!banDAO.capNhatViTri(ban.getMaBan(), x, y)) {
                thongBao("Không thể lưu vị trí bàn. Vui lòng kiểm tra kết nối CSDL.");
            }
            e.consume();
        });

        theBan.setOnMouseEntered(e -> {
            if (laQuanLy && dangChinhSuaSoDo) theBan.setCursor(javafx.scene.Cursor.OPEN_HAND);
        });
    }

    private void xuLyThemKhuVuc() {
        if (!laQuanLy) {
            thongBaoKhongDuQuyen();
            return;
        }
        TextField txtTenKV = new TextField();
        txtTenKV.setPromptText("Tên khu vực mới");
        txtTenKV.getStyleClass().add("text-field-rounded");

        VBox content = new VBox(10, new Label("Tên khu vực:"), txtTenKV);
        content.setPadding(new Insets(10));

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Thêm khu vực");
        alert.setHeaderText("Tạo khu vực mới");
        alert.getDialogPane().setContent(content);
        UI.style(alert);

        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                String tenKV = txtTenKV.getText().trim();
                if (tenKV.isEmpty()) {
                    thongBao("Tên khu vực không được để trống.");
                    return;
                }
                if (banDAO.themKhuVuc(tenKV)) {
                    locKhuVuc = tenKV;
                    thongBao("Thêm khu vực thành công!");
                    veLaiLuoi();
                } else {
                    thongBao("Thêm khu vực thất bại. Có thể tên khu vực đã tồn tại.");
                }
            }
        });
    }

    private StackPane taoSoDoBan(Ban ban, int soNguoi) {
        StackPane khung = new StackPane();
        khung.getStyleClass().add("table-layout");
        boolean trong = "Trong".equalsIgnoreCase(ban.getTrangThai());

        int sucChua = Math.max(1, ban.getSucChua());
        int soGheHangTren = (sucChua + 1) / 2;
        int soGheHangDuoi = sucChua / 2;
        int soCot = Math.max(1, soGheHangTren);
        double chieuRongMatBan = Math.max(76, soCot * 30.0 + 16);

        HBox hangTren = taoHangGhe(soGheHangTren, 0, soNguoi);
        HBox hangDuoi = taoHangGhe(soGheHangDuoi, soGheHangTren, soNguoi);

        Label lblNguoi = new Label("Người: " + soNguoi + "/" + ban.getSucChua());
        Label lblTongTien = new Label("Tổng: " + DINH_DANG_TIEN.format(ban.getTongTien()) + " VND");
        Label lblThoiGian = new Label("Đã order: " + dinhDangThoiGian(ban.getThoiGianTao()));
        VBox matBan = new VBox(1,
                new Label("BÀN " + ban.getSoHieuBan()),
                lblNguoi,
                lblTongTien,
                lblThoiGian);
        matBan.setAlignment(Pos.CENTER);
        matBan.getStyleClass().addAll("table-surface", trong ? "table-surface-empty" : "table-surface-serving");
        matBan.getChildren().get(0).getStyleClass().add("table-label");
        lblNguoi.getStyleClass().add("table-person-count");
        lblTongTien.getStyleClass().add("table-order-info");
        lblThoiGian.getStyleClass().add("table-order-info");
        matBan.setPrefSize(chieuRongMatBan, 70);
        matBan.setMinWidth(chieuRongMatBan);
        matBan.setMaxWidth(chieuRongMatBan);

        VBox soDoHangDai = new VBox(6, hangTren, matBan, hangDuoi);
        soDoHangDai.setAlignment(Pos.CENTER);

        khung.getChildren().add(soDoHangDai);
        return khung;
    }

    private String dinhDangThoiGian(Timestamp thoiGianTao) {
        if (thoiGianTao == null) return "-";
        long soPhut = ChronoUnit.MINUTES.between(thoiGianTao.toLocalDateTime(), LocalDateTime.now());
        if (soPhut < 0) soPhut = 0;
        long gio = soPhut / 60;
        long phut = soPhut % 60;
        return gio > 0 ? gio + " giờ " + phut + " phút" : phut + " phút";
    }

    private HBox taoHangGhe(int soGhe, int chiSoBatDau, int soNguoi) {
        HBox hang = new HBox(6);
        hang.setAlignment(Pos.CENTER);

        for (int i = 0; i < soGhe; i++) {
            Region ghe = new Region();
            ghe.getStyleClass().add("chair");
            if (chiSoBatDau + i < soNguoi) ghe.getStyleClass().add("chair-occupied");
            ghe.setPrefSize(24, 24);
            ghe.setMinSize(24, 24);
            ghe.setMaxSize(24, 24);
            hang.getChildren().add(ghe);
        }
        return hang;
    }

    private VBox taoNutLamMoi() {
        Button btn = new Button("Làm mới");
        btn.getStyleClass().add("btn-outline");
        btn.setOnAction(e -> veLaiLuoi());
        Label trong = new Label(" ");
        trong.getStyleClass().add("field-label");
        return new VBox(4, trong, btn);
    }

    // ----- Xu ly them / sua / xoa ban -----

    private void xuLyThemBan() {
        if (!laQuanLy) {
            thongBaoKhongDuQuyen();
            return;
        }
        List<String> dsKV = banDAO.layTatCaKhuVuc();
        if (dsKV.isEmpty()) {
            thongBao("Chưa có khu vực nào. Vui lòng thêm khu vực trước.");
            return;
        }

        ComboBox<String> cboKV = new ComboBox<>(FXCollections.observableArrayList(dsKV));
        cboKV.setValue(dsKV.get(0));
        cboKV.getStyleClass().add("combo-box-rounded");

        TextField txtSoHieu = new TextField();
        txtSoHieu.setPromptText("Số hiệu bàn (A1, 1...)");
        txtSoHieu.getStyleClass().add("text-field-rounded");

        TextField txtSucChua = new TextField();
        txtSucChua.setPromptText("Sức chứa");
        txtSucChua.getStyleClass().add("text-field-rounded");

        VBox content = new VBox(10,
                new Label("Khu vực:"), cboKV,
                new Label("Số hiệu bàn:"), txtSoHieu,
                new Label("Sức chứa:"), txtSucChua);
        content.setPadding(new javafx.geometry.Insets(10));

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Thêm bàn");
        alert.setHeaderText("Thêm bàn mới");
        alert.getDialogPane().setContent(content);
        UI.style(alert);

        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                String soHieu = txtSoHieu.getText().trim();
                String sucChua = txtSucChua.getText().trim();
                if (soHieu.isEmpty() || sucChua.isEmpty()) {
                    thongBao("Vui lòng nhập đầy đủ thông tin.");
                    return;
                }
                int sc;
                try {
                    sc = Integer.parseInt(sucChua);
                } catch (NumberFormatException ex) {
                    thongBao("Sức chứa phải là số!");
                    return;
                }
                int maKV = banDAO.layMaKVTheoTen(cboKV.getValue());
                if (maKV == -1) {
                    thongBao("Không tìm thấy mã khu vực.");
                    return;
                }
                if (banDAO.themBan(maKV, soHieu, sc)) {
                    thongBao("Thêm bàn thành công!");
                    veLaiLuoi();
                } else {
                    thongBao("Thêm bàn thất bại.");
                }
            }
        });
    }

    private void xuLySuaBan(Ban ban) {
        if (!laQuanLy) {
            thongBaoKhongDuQuyen();
            return;
        }
        TextField txtSoHieu = new TextField(ban.getSoHieuBan());
        txtSoHieu.setPromptText("Số hiệu bàn");
        txtSoHieu.getStyleClass().add("text-field-rounded");

        TextField txtSucChua = new TextField(String.valueOf(ban.getSucChua()));
        txtSucChua.setPromptText("Sức chứa");
        txtSucChua.getStyleClass().add("text-field-rounded");

        VBox content = new VBox(10,
                new Label("Số hiệu:"), txtSoHieu,
                new Label("Sức chứa:"), txtSucChua);
        content.setPadding(new javafx.geometry.Insets(10));

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Sửa bàn");
        alert.setHeaderText("Cập nhật thông tin bàn: " + ban.getSoHieuBan());
        alert.getDialogPane().setContent(content);
        UI.style(alert);

        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                String soHieuMoi = txtSoHieu.getText().trim();
                if (soHieuMoi.isEmpty()) {
                    thongBao("Số hiệu bàn không được để trống.");
                    return;
                }
                int sc;
                try {
                    sc = Integer.parseInt(txtSucChua.getText().trim());
                } catch (NumberFormatException ex) {
                    thongBao("Sức chứa phải là số!");
                    return;
                }
                if (banDAO.capNhatBan(ban.getMaBan(), soHieuMoi, sc)) {
                    thongBao("Cập nhật thành công!");
                    veLaiLuoi();
                } else {
                    thongBao("Cập nhật thất bại.");
                }
            }
        });
    }

    private void xuLyXoaBan(int maBan, String soHieuBan) {
        if (!laQuanLy) {
            thongBaoKhongDuQuyen();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Bạn có chắc chắn muốn xóa bàn " + soHieuBan + "?");
        confirm.setContentText("Hành động này không thể hoàn tác.");
        UI.style(confirm);

        confirm.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                if (banDAO.xoaBan(maBan)) {
                    thongBao("Đã xóa bàn " + soHieuBan + " thành công.");
                    veLaiLuoi();
                } else {
                    thongBao("Không thể xóa bàn. Có thể bàn đang có đơn hàng hoặc lỗi CSDL.");
                }
            }
        });
    }

    private void thongBao(String noiDung) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(noiDung);
        UI.style(alert);
        alert.showAndWait();
    }

    private void thongBaoKhongDuQuyen() {
        thongBao("Chỉ tài khoản Quản lý mới được thêm, sửa, xóa bàn hoặc thêm khu vực.");
    }
}
