import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class QuanLyNhanVienView {

    private final NhanVienDAO dao = new NhanVienDAO();
    private final ObservableList<NhanVien> danhSach = FXCollections.observableArrayList();
    private final TableView<NhanVien> table = new TableView<>();

    private final TextField txtHoTen = new TextField();
    private final TextField txtSDT = new TextField();
    private final ComboBox<String> cboLoaiHopDong = new ComboBox<>();
    private final TextField txtTenDangNhap = new TextField();
    private final PasswordField txtMatKhau = new PasswordField();
    private final ComboBox<String> cboVaiTro = new ComboBox<>();

    private NhanVien dangChon = null;

    public VBox taoGiaoDien() {
        Label lblTieuDe = new Label("Quản lý nhân viên");
        lblTieuDe.getStyleClass().add("section-title");

        // ----- Bang -----
        TableColumn<NhanVien, Integer> colMa = new TableColumn<>("Mã NV");
        colMa.setCellValueFactory(new PropertyValueFactory<>("maNV"));

        TableColumn<NhanVien, String> colHoTen = new TableColumn<>("Họ tên");
        colHoTen.setCellValueFactory(new PropertyValueFactory<>("hoTen"));

        TableColumn<NhanVien, String> colSDT = new TableColumn<>("SĐT");
        colSDT.setCellValueFactory(new PropertyValueFactory<>("soDienThoai"));

        TableColumn<NhanVien, String> colHopDong = new TableColumn<>("Loại hợp đồng");
        colHopDong.setCellValueFactory(new PropertyValueFactory<>("loaiHopDong"));

        TableColumn<NhanVien, String> colTenDangNhap = new TableColumn<>("Tên đăng nhập");
        colTenDangNhap.setCellValueFactory(new PropertyValueFactory<>("tenDangNhap"));

        TableColumn<NhanVien, String> colVaiTro = new TableColumn<>("Vai trò");
        colVaiTro.setCellValueFactory(new PropertyValueFactory<>("vaiTro"));

        table.getColumns().addAll(colMa, colHoTen, colSDT, colHopDong, colTenDangNhap, colVaiTro);
        table.setItems(danhSach);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Chưa có nhân viên nào."));
        table.setFixedCellSize(32);
        table.prefHeightProperty().bind(
                table.fixedCellSizeProperty().multiply(
                        javafx.beans.binding.Bindings.size(danhSach).add(1.4)
                )
        );
        table.setMinHeight(120);
        table.setMaxHeight(360);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                dangChon = newVal;
                txtHoTen.setText(newVal.getHoTen());
                txtSDT.setText(newVal.getSoDienThoai());
                cboLoaiHopDong.setValue(newVal.getLoaiHopDong());
                txtTenDangNhap.setText(newVal.getTenDangNhap());
                txtTenDangNhap.setDisable(true); // khong doi ten dang nhap khi sua
                txtMatKhau.clear();
                txtMatKhau.setDisable(true); // khong doi mat khau o man hinh nay
                cboVaiTro.setValue(newVal.getVaiTro());
            }
        });

        // ----- Form -----
        cboLoaiHopDong.setItems(FXCollections.observableArrayList("PartTime", "FullTime"));
        cboLoaiHopDong.setPromptText("Chọn loại");
        cboLoaiHopDong.getStyleClass().add("combo-box-rounded");

        cboVaiTro.setItems(FXCollections.observableArrayList("QuanLy", "ThuNgan", "PhaChe", "PhucVu", "Bep"));
        cboVaiTro.setPromptText("Chọn vai trò");
        cboVaiTro.getStyleClass().add("combo-box-rounded");

        txtHoTen.setPromptText("Họ tên");
        txtSDT.setPromptText("Số điện thoại");
        txtTenDangNhap.setPromptText("Tên đăng nhập");
        txtMatKhau.setPromptText("Mật khẩu (chỉ khi thêm mới)");

        for (TextField tf : new TextField[]{txtHoTen, txtSDT, txtTenDangNhap}) {
            tf.getStyleClass().add("text-field-rounded");
        }
        txtMatKhau.getStyleClass().add("text-field-rounded");

        HBox hang1 = new HBox(16,
                truong("Họ tên", txtHoTen),
                truong("SĐT", txtSDT),
                truong("Loại hợp đồng", cboLoaiHopDong)
        );
        HBox hang2 = new HBox(16,
                truong("Tên đăng nhập", txtTenDangNhap),
                truong("Mật khẩu", txtMatKhau),
                truong("Vai trò", cboVaiTro)
        );
        hang1.setAlignment(Pos.CENTER_LEFT);
        hang2.setAlignment(Pos.CENTER_LEFT);

        // ----- Nut -----
        Button btnThem = new Button("+  Thêm mới");
        Button btnSua = new Button("Cập nhật");
        Button btnXoa = new Button("Xóa");
        Button btnLamMoi = new Button("Làm mới");

        btnThem.getStyleClass().add("btn-success");
        btnSua.getStyleClass().add("btn-update");
        btnXoa.getStyleClass().add("btn-danger");
        btnLamMoi.getStyleClass().add("btn-outline");

        btnThem.setOnAction(e -> {
            if (!kiemTraHopLeThem()) return;
            boolean ok = dao.them(
                    txtHoTen.getText().trim(), txtSDT.getText().trim(), cboLoaiHopDong.getValue(),
                    txtTenDangNhap.getText().trim(), txtMatKhau.getText().trim(), cboVaiTro.getValue()
            );
            if (ok) {
                thongBao("Thêm nhân viên thành công!");
                lamMoiForm();
                taiLaiDuLieu();
            } else {
                thongBao("Thêm thất bại (tên đăng nhập có thể đã tồn tại).");
            }
        });

        btnSua.setOnAction(e -> {
            if (dangChon == null) { thongBao("Vui lòng chọn 1 nhân viên trong bảng trước."); return; }
            if (txtHoTen.getText().trim().isEmpty() || cboLoaiHopDong.getValue() == null || cboVaiTro.getValue() == null) {
                thongBao("Vui lòng nhập đầy đủ Họ tên, Loại hợp đồng, Vai trò.");
                return;
            }
            boolean ok = dao.capNhat(dangChon.getMaNV(), txtHoTen.getText().trim(), txtSDT.getText().trim(),
                    cboLoaiHopDong.getValue(), cboVaiTro.getValue());
            if (ok) {
                thongBao("Cập nhật thành công!");
                lamMoiForm();
                taiLaiDuLieu();
            } else {
                thongBao("Cập nhật thất bại.");
            }
        });

        btnXoa.setOnAction(e -> {
            if (dangChon == null) { thongBao("Vui lòng chọn 1 nhân viên trong bảng trước."); return; }
            boolean ok = dao.xoa(dangChon.getMaNV());
            if (ok) {
                thongBao("Đã xóa nhân viên.");
                lamMoiForm();
                taiLaiDuLieu();
            } else {
                thongBao("Không thể xóa (nhân viên có thể đang có dữ liệu liên quan).");
            }
        });

        btnLamMoi.setOnAction(e -> lamMoiForm());

        HBox hopNut = new HBox(12, btnThem, btnSua, btnXoa, btnLamMoi);
        hopNut.setPadding(new Insets(6, 0, 0, 0));

        VBox card = new VBox(18, lblTieuDe, table, hang1, hang2, hopNut);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(26));

        VBox root = new VBox(card);
        root.setPadding(new Insets(24));

        taiLaiDuLieu();
        return root;
    }

    private VBox truong(String nhan, javafx.scene.control.Control o) {
        Label lbl = new Label(nhan);
        lbl.getStyleClass().add("field-label");
        return new VBox(4, lbl, o);
    }

    private void taiLaiDuLieu() {
        danhSach.setAll(dao.layTatCa());
    }

    private void lamMoiForm() {
        dangChon = null;
        table.getSelectionModel().clearSelection();
        txtHoTen.clear();
        txtSDT.clear();
        cboLoaiHopDong.setValue(null);
        txtTenDangNhap.clear();
        txtTenDangNhap.setDisable(false);
        txtMatKhau.clear();
        txtMatKhau.setDisable(false);
        cboVaiTro.setValue(null);
    }

    private boolean kiemTraHopLeThem() {
        if (txtHoTen.getText().trim().isEmpty() || cboLoaiHopDong.getValue() == null ||
            txtTenDangNhap.getText().trim().isEmpty() || txtMatKhau.getText().trim().isEmpty() ||
            cboVaiTro.getValue() == null) {
            thongBao("Vui lòng nhập đầy đủ thông tin (bao gồm tên đăng nhập và mật khẩu).");
            return false;
        }
        return true;
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
