import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.stream.Collectors;

public class QuanLyBanView {

    private final BanDAO dao = new BanDAO();
    private final ScrollPane scroll = new ScrollPane();

    // Controls for management
    private final ComboBox<String> cboKhuVuc = new ComboBox<>();
    private final TextField txtSoHieuBan = new TextField();
    private final TextField txtSucChua = new TextField();
    private final TextField txtTenKV = new TextField();

    // Danh sach mau vach the, xoay vong theo thu tu de nhin nhu Trello
    private static final String[] MAU_VACH = {
            "accent-blue", "accent-pink", "accent-teal", "accent-purple", "accent-yellow"
    };

    public VBox taoGiaoDien() {
        Label lblTieuDe = new Label("Sơ đồ trạng thái Bàn");
        lblTieuDe.getStyleClass().add("section-title");

        // ----- Thanh cong cu quan ly -----
        Label lblQL = new Label("Quản lý nhanh:");
        lblQL.getStyleClass().add("card-subtitle");

        cboKhuVuc.setPromptText("Chọn khu vực");
        cboKhuVuc.getStyleClass().add("combo-box-rounded");

        txtSoHieuBan.setPromptText("Số hiệu bàn (A1, 1...)");
        txtSoHieuBan.getStyleClass().add("text-field-rounded");
        txtSoHieuBan.setPrefWidth(120);

        txtSucChua.setPromptText("Sức chứa");
        txtSucChua.getStyleClass().add("text-field-rounded");
        txtSucChua.setPrefWidth(80);

        Button btnThemBan = new Button("Thêm bàn");
        btnThemBan.getStyleClass().add("btn-primary");

        Button btnXoaBan = new Button("Xóa bàn đã chọn");
        btnXoaBan.getStyleClass().add("btn-danger");

        Label lblKV = new Label("Khu vực mới:");
        txtTenKV.setPromptText("Tên khu vực");
        txtTenKV.getStyleClass().add("text-field-rounded");
        txtTenKV.setPrefWidth(150);
        Button btnThemKV = new Button("Thêm KV");
        btnThemKV.getStyleClass().add("btn-outline");
        
        Button btnLamMoi = new Button("Làm mới");
        btnLamMoi.getStyleClass().add("btn-outline");

        HBox hangQL1 = new HBox(10, lblQL, cboKhuVuc, txtSoHieuBan, txtSucChua, btnThemBan, btnXoaBan);
        hangQL1.setAlignment(Pos.CENTER_LEFT);
        
        HBox hangQL2 = new HBox(10, lblKV, txtTenKV, btnThemKV, new Region(), btnLamMoi);
        HBox.setHgrow(hangQL2.getChildren().get(3), Priority.ALWAYS); // Spacer
        hangQL2.setAlignment(Pos.CENTER_LEFT);

        VBox panelQL = new VBox(10, hangQL1, hangQL2);
        panelQL.getStyleClass().add("content-card");
        panelQL.setPadding(new Insets(16));

        // Su kien
        btnThemBan.setOnAction(e -> xuLyThemBan());
        btnThemKV.setOnAction(e -> xuLyThemKhuVuc());
        btnLamMoi.setOnAction(e -> {
            capNhatDSKhuVuc();
            veLaiBang();
        });
        // Nút xóa chung đã được thay thế bằng nút xóa trên từng thẻ bàn
        btnXoaBan.setDisable(true);
        btnXoaBan.setText("Xóa (Chọn trên thẻ)");

        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("board-scroll");
        veLaiBang();
        capNhatDSKhuVuc();

        VBox root = new VBox(16, lblTieuDe, panelQL, scroll);
        root.setPadding(new Insets(24));
        return root;
    }

    private void xuLyThemBan() {
        if (cboKhuVuc.getValue() == null || txtSoHieuBan.getText().trim().isEmpty() || txtSucChua.getText().trim().isEmpty()) {
            thongBao("Vui lòng nhập đầy đủ thông tin.");
            return;
        }
        try {
            int sc = Integer.parseInt(txtSucChua.getText().trim());
            int maKV = dao.layMaKVTheoTen(cboKhuVuc.getValue());
            if (maKV != -1) {
                if (dao.themBan(maKV, txtSoHieuBan.getText().trim(), sc)) {
                    thongBao("Thêm bàn thành công!");
                    txtSoHieuBan.clear();
                    txtSucChua.clear();
                    veLaiBang();
                } else {
                    thongBao("Thêm bàn thất bại.");
                }
            } else {
                thongBao("Không tìm thấy mã khu vực.");
            }
        } catch (Exception ex) {
            thongBao("Sức chứa phải là số.");
        }
    }

    private void xuLyThemKhuVuc() {
        if (txtTenKV.getText().trim().isEmpty()) {
            thongBao("Vui lòng nhập tên khu vực.");
            return;
        }
        if (dao.themKhuVuc(txtTenKV.getText().trim())) {
            thongBao("Thêm khu vực thành công!");
            txtTenKV.clear();
            capNhatDSKhuVuc();
        } else {
            thongBao("Thêm khu vực thất bại.");
        }
    }

    private void xuLyXoaBan(int maBan, String soHieuBan) {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Bạn có chắc chắn muốn xóa bàn " + soHieuBan + "?");
        confirm.setContentText("Hành động này không thể hoàn tác.");
        UI.style(confirm);
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                if (dao.xoaBan(maBan)) {
                    thongBao("Đã xóa bàn " + soHieuBan + " thành công.");
                    veLaiBang();
                } else {
                    thongBao("Không thể xóa bàn. Có thể bàn đang có đơn hàng hoặc lỗi CSDL.");
                }
            }
        });
    }

    private void capNhatDSKhuVuc() {
        // Load the available areas from the DAO.
        // ở đây ta dùng tạm một danh sách mẫu hoặc query trực tiếp nếu cần.
        // Để đơn giản, ta sẽ thêm hàm layTatCaKhuVuc vào BanDAO sau nếu cần thiết.
        // Tạm thời dùng hardcode hoặc query nhanh.
        List<String> kvList = dao.layTatCaKhuVuc(); 
        cboKhuVuc.setItems(FXCollections.observableArrayList(kvList));
    }

    private void veLaiBang() {
        HBox board = new HBox(20);
        board.setPadding(new Insets(6, 0, 0, 0));

        List<Ban> danhSach = dao.layTatCa();

        VBox cotTrong = taoCot("Ban trong", "col-trong");
        VBox cotDangPV = taoCot("Dang phuc vu", "col-dangpv");

        for (int i = 0; i < danhSach.size(); i++) {
            Ban ban = danhSach.get(i);
            String mauVach = MAU_VACH[i % MAU_VACH.length];
            VBox the = taoTheBan(ban, mauVach);

            if ("Trong".equalsIgnoreCase(ban.getTrangThai())) {
                cotTrong.getChildren().add(the);
            } else {
                cotDangPV.getChildren().add(the);
            }
        }

        board.getChildren().addAll(cotTrong, cotDangPV);
        scroll.setContent(board);
    }

    private VBox taoCot(String tieuDe, String styleClass) {
        Label lbl = new Label(tieuDe);
        lbl.getStyleClass().add("column-title");

        VBox cot = new VBox(12, lbl);
        cot.getStyleClass().addAll("kanban-column", styleClass);
        cot.setPrefWidth(300);
        cot.setMinWidth(280);
        cot.setPadding(new Insets(14));
        return cot;
    }

    private VBox taoTheBan(Ban ban, String mauVach) {
        Region vach = new Region();
        vach.getStyleClass().addAll("card-accent-bar", mauVach);
        vach.setPrefHeight(6);

        Label lblBan = new Label("Ban " + ban.getSoHieuBan());
        lblBan.getStyleClass().add("card-title");

        Label lblKhuVuc = new Label(ban.getTenKhuVuc());
        lblKhuVuc.getStyleClass().add("card-subtitle");
        
        Label lblSucChua = new Label("Sức chứa: " + ban.getSucChua());
        lblSucChua.getStyleClass().add("card-subtitle");

        Button btnDoiTrangThai = new Button(
                "Trong".equalsIgnoreCase(ban.getTrangThai()) ? "Bắt đầu phục vụ" : "Trả bàn (Trống)"
        );
        btnDoiTrangThai.getStyleClass().add("btn-card-action");
        btnDoiTrangThai.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDoiTrangThai, Priority.ALWAYS);
        btnDoiTrangThai.setOnAction(e -> {
            String moi = "Trong".equalsIgnoreCase(ban.getTrangThai()) ? "DangPhucVu" : "Trong";
            dao.capNhatTrangThai(ban.getMaBan(), moi);
            veLaiBang();
        });

        Button btnSuaThe = new Button("Sua");
        btnSuaThe.getStyleClass().add("btn-update");
        btnSuaThe.setPrefWidth(80);
        btnSuaThe.setMinHeight(32);
        btnSuaThe.setMaxHeight(32);

        btnSuaThe.setOnAction(e -> xuLySuaBan(ban));

        Button btnXoaThe = new Button("Xoa");
        btnXoaThe.getStyleClass().add("btn-danger");
        btnXoaThe.setPrefWidth(80);
        btnXoaThe.setMinHeight(32);
        btnXoaThe.setMaxHeight(32);

        btnXoaThe.setOnAction(e -> xuLyXoaBan(ban.getMaBan(), ban.getSoHieuBan()));

        HBox hangSuaXoa = new HBox(8, btnSuaThe, btnXoaThe);
        hangSuaXoa.setAlignment(Pos.CENTER_RIGHT);

        VBox noiDung = new VBox(8, lblBan, lblKhuVuc, lblSucChua, btnDoiTrangThai, hangSuaXoa);
        noiDung.setPadding(new Insets(12, 14, 14, 14));

        VBox the = new VBox(vach, noiDung);
        the.getStyleClass().add("kanban-card");
        return the;
    }

    private void xuLySuaBan(Ban ban) {
        TextField txtSoHieu = new TextField(ban.getSoHieuBan());
        txtSoHieu.setPromptText("Số hiệu bàn");
        TextField txtSucChua = new TextField(String.valueOf(ban.getSucChua()));
        txtSucChua.setPromptText("Sức chứa");
        
        VBox content = new VBox(10, new Label("Số hiệu:"), txtSoHieu, new Label("Sức chứa:"), txtSucChua);
        content.setPadding(new Insets(10));
        
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Sửa bàn");
        alert.setHeaderText("Cập nhật thông tin bàn: " + ban.getSoHieuBan());
        alert.getDialogPane().setContent(content);
        UI.style(alert);
        
        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                String soHieuMoi = txtSoHieu.getText().trim();
                int sucChuaMoi;
                try {
                    sucChuaMoi = Integer.parseInt(txtSucChua.getText().trim());
                } catch (NumberFormatException ex) {
                    thongBao("Sức chứa phải là số!");
                    return;
                }
                
                boolean ok = dao.capNhatBan(ban.getMaBan(), soHieuMoi, sucChuaMoi);
                if (ok) {
                    thongBao("Cập nhật thành công!");
                    veLaiBang();
                } else {
                    thongBao("Cập nhật thất bại.");
                }
            }
        });
    }

    private void thongBao(String noiDung) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(noiDung);
        UI.style(alert);
        alert.showAndWait();
    }
}
