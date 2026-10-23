import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class QuanLySanPhamView {

    private final SanPhamDAO dao = new SanPhamDAO();
    private final ObservableList<SanPham> danhSach = FXCollections.observableArrayList();
    private final TableView<SanPham> table = new TableView<>();

    private final TextField txtTen = new TextField();
    private final ComboBox<String> cboNhom = new ComboBox<>();
    private final TextField txtMoTa = new TextField();

    private final CheckBox chkS = new CheckBox("S");
    private final TextField txtGiaS = new TextField();
    private final CheckBox chkM = new CheckBox("M");
    private final TextField txtGiaM = new TextField();
    private final CheckBox chkL = new CheckBox("L");
    private final TextField txtGiaL = new TextField();
    private final CheckBox chkKhongSize = new CheckBox("Không size");
    private final TextField txtGiaKhongSize = new TextField();

    private SanPham dangChon = null;

    public VBox taoGiaoDien() {
        Label lblTieuDe = new Label("Quản lý sản phẩm");
        lblTieuDe.getStyleClass().add("section-title");

        // ----- Bang danh sach -----
        TableColumn<SanPham, Integer> colMa = new TableColumn<>("Mã SP");
        colMa.setCellValueFactory(new PropertyValueFactory<>("maSP"));
        colMa.setPrefWidth(60);

        TableColumn<SanPham, String> colTen = new TableColumn<>("Tên sản phẩm");
        colTen.setCellValueFactory(new PropertyValueFactory<>("tenSP"));
        colTen.setPrefWidth(200);

        TableColumn<SanPham, String> colNhom = new TableColumn<>("Nhóm");
        colNhom.setCellValueFactory(new PropertyValueFactory<>("nhomSP"));
        colNhom.setPrefWidth(100);

        TableColumn<SanPham, Double> colGia = new TableColumn<>("Giá bán");
        colGia.setCellValueFactory(new PropertyValueFactory<>("giaBan"));
        colGia.setPrefWidth(100);

        TableColumn<SanPham, String> colSize = new TableColumn<>("Size");
        colSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        colSize.setPrefWidth(80);

        TableColumn<SanPham, String> colMoTa = new TableColumn<>("Mô tả");
        colMoTa.setCellValueFactory(new PropertyValueFactory<>("moTa"));
        colMoTa.setPrefWidth(200);

        table.getColumns().addAll(colMa, colTen, colNhom, colGia, colSize, colMoTa);
        table.setItems(danhSach);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Chưa có sản phẩm nào."));

        // Tu co chieu cao bang theo so dong du lieu, khong hien dong trang thua
        table.setFixedCellSize(32);
        table.prefHeightProperty().bind(
                table.fixedCellSizeProperty().multiply(
                        javafx.beans.binding.Bindings.size(danhSach).add(1.4)
                )
        );
        table.setMinHeight(120);
        table.setMaxHeight(400);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                dangChon = newVal;
                txtTen.setText(newVal.getTenSP());
                cboNhom.setValue(newVal.getNhomSP());
                txtMoTa.setText(newVal.getMoTa());
                chkS.setSelected(false); txtGiaS.clear();
                chkM.setSelected(false); txtGiaM.clear();
                chkL.setSelected(false); txtGiaL.clear();
                chkKhongSize.setSelected(false); txtGiaKhongSize.clear();
                for (SanPham sp : danhSach) {
                    if (sp.getTenSP().equalsIgnoreCase(newVal.getTenSP())) {
                        if ("S".equalsIgnoreCase(sp.getSize())) { chkS.setSelected(true); txtGiaS.setText(String.valueOf(sp.getGiaBan())); }
                        if ("M".equalsIgnoreCase(sp.getSize())) { chkM.setSelected(true); txtGiaM.setText(String.valueOf(sp.getGiaBan())); }
                        if ("L".equalsIgnoreCase(sp.getSize())) { chkL.setSelected(true); txtGiaL.setText(String.valueOf(sp.getGiaBan())); }
                        if (sp.getSize() == null || sp.getSize().trim().isEmpty()) {
                            chkKhongSize.setSelected(true);
                            txtGiaKhongSize.setText(String.valueOf(sp.getGiaBan()));
                        }
                    }
                }
            }
        });

        // ----- Form nhap -----
        cboNhom.setItems(FXCollections.observableArrayList(
                "Ca phe", "Nuoc", "An vat", "An sang", "Combo"
        ));
        cboNhom.setPromptText("Chon nhom");
        cboNhom.getStyleClass().add("combo-box-rounded");

        txtTen.setPromptText("Ten san pham");
        txtTen.getStyleClass().add("text-field-rounded");

        txtMoTa.setPromptText("Mo ta ngan");
        txtMoTa.getStyleClass().add("text-field-rounded");

        txtGiaS.setPromptText("Gia S");
        txtGiaS.getStyleClass().add("text-field-rounded");
        txtGiaS.setPrefWidth(90);
        txtGiaS.disableProperty().bind(chkS.selectedProperty().not());

        txtGiaM.setPromptText("Gia M");
        txtGiaM.getStyleClass().add("text-field-rounded");
        txtGiaM.setPrefWidth(90);
        txtGiaM.disableProperty().bind(chkM.selectedProperty().not());

        txtGiaL.setPromptText("Gia L");
        txtGiaL.getStyleClass().add("text-field-rounded");
        txtGiaL.setPrefWidth(90);
        txtGiaL.disableProperty().bind(chkL.selectedProperty().not());

        txtGiaKhongSize.setPromptText("Giá bán");
        txtGiaKhongSize.getStyleClass().add("text-field-rounded");
        txtGiaKhongSize.setPrefWidth(100);
        txtGiaKhongSize.disableProperty().bind(chkKhongSize.selectedProperty().not());

        chkKhongSize.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                chkS.setSelected(false);
                chkM.setSelected(false);
                chkL.setSelected(false);
            }
        });
        chkS.selectedProperty().addListener((obs, oldVal, newVal) -> { if (newVal) chkKhongSize.setSelected(false); });
        chkM.selectedProperty().addListener((obs, oldVal, newVal) -> { if (newVal) chkKhongSize.setSelected(false); });
        chkL.selectedProperty().addListener((obs, oldVal, newVal) -> { if (newVal) chkKhongSize.setSelected(false); });

        HBox boxSizeCoKichThuoc = new HBox(10, chkS, txtGiaS, chkM, txtGiaM, chkL, txtGiaL);
        boxSizeCoKichThuoc.setAlignment(Pos.CENTER_LEFT);

        HBox boxKhongSize = new HBox(10, chkKhongSize, txtGiaKhongSize);
        boxKhongSize.setAlignment(Pos.CENTER_LEFT);

        VBox boxSize = new VBox(8, boxSizeCoKichThuoc, boxKhongSize);

        VBox colTenField = new VBox(4, new Label("Ten san pham"), txtTen);
        VBox colNhomField = new VBox(4, new Label("Nhom"), cboNhom);
        VBox colSizeField = new VBox(4, new Label("Size & Giá"), boxSize);
        VBox colMoTaField = new VBox(4, new Label("Mo ta"), txtMoTa);
        colTenField.getChildren().get(0).getStyleClass().add("field-label");
        colNhomField.getChildren().get(0).getStyleClass().add("field-label");
        colSizeField.getChildren().get(0).getStyleClass().add("field-label");
        colMoTaField.getChildren().get(0).getStyleClass().add("field-label");

        HBox formRow1 = new HBox(16, colTenField, colNhomField);
        HBox formRow2 = new HBox(16, colSizeField, colMoTaField);
        formRow1.setAlignment(Pos.CENTER_LEFT);
        formRow2.setAlignment(Pos.CENTER_LEFT);
        VBox form = new VBox(10, formRow1, formRow2);

        // ----- Nut chuc nang -----
        Button btnThem = new Button("+  Thêm mới");
        Button btnSua = new Button("Cập nhật");
        Button btnXoa = new Button("Xóa");
        Button btnLamMoi = new Button("Làm mới");

        btnThem.getStyleClass().add("btn-success");
        btnSua.getStyleClass().add("btn-update");
        btnXoa.getStyleClass().add("btn-danger");
        btnLamMoi.getStyleClass().add("btn-outline");

        btnThem.setOnAction(e -> {
            if (!kiemTraHopLe()) return;
            boolean ok = luuSanPham(false);
            if (ok) {
                thongBao("Them san pham thanh cong!");
                lamMoiForm();
                taiLaiDuLieu();
            } else {
                thongBao("Them that bai. Kiem tra lai du lieu.");
            }
        });

        btnSua.setOnAction(e -> {
            if (dangChon == null) {
                thongBao("Vui long chon 1 san pham trong bang truoc.");
                return;
            }
            if (!kiemTraHopLe()) return;
            boolean ok = luuSanPham(true);
            if (ok) {
                thongBao("Cap nhat thanh cong!");
                lamMoiForm();
                taiLaiDuLieu();
            } else {
                thongBao("Cap nhat that bai.");
            }
        });

        btnXoa.setOnAction(e -> {
            if (dangChon == null) {
                thongBao("Vui long chon 1 san pham trong bang truoc.");
                return;
            }
            boolean ok = dao.xoaTheoTen(dangChon.getTenSP());
            if (ok) {
                thongBao("Da xoa san pham.");
                lamMoiForm();
                taiLaiDuLieu();
            } else {
                thongBao("Khong the xoa (san pham co the dang nam trong don hang).");
            }
        });

        btnLamMoi.setOnAction(e -> lamMoiForm());

        HBox hopNut = new HBox(12, btnThem, btnSua, btnXoa, btnLamMoi);
        hopNut.setPadding(new Insets(6, 0, 0, 0));

        VBox card = new VBox(18, lblTieuDe, table, form, hopNut);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(26));

        VBox root = new VBox(card);
        root.setPadding(new Insets(24));

        taiLaiDuLieu();
        return root;
    }

    private boolean luuSanPham(boolean laCapNhat) {
        String ten = txtTen.getText().trim();
        String tenCu = laCapNhat ? dangChon.getTenSP() : ten;
        String nhom = cboNhom.getValue();
        String moTa = txtMoTa.getText().trim();
        boolean success = true;

        if (chkKhongSize.isSelected()) {
            double gia = parseGia(txtGiaKhongSize.getText());
            if (gia < 0) return false;
            if (laCapNhat) {
                success = dao.capNhat(dangChon.getMaSP(), ten, nhom, gia, null, moTa);
                if (success) {
                    dao.xoaTheoTenVaSize(tenCu, "S");
                    dao.xoaTheoTenVaSize(tenCu, "M");
                    dao.xoaTheoTenVaSize(tenCu, "L");
                }
            } else {
                success = dao.them(ten, nhom, gia, null, moTa);
            }
            return success;
        }

        if (chkS.isSelected()) {
            double gia = parseGia(txtGiaS.getText());
            if (gia < 0) return false;
            if (laCapNhat) {
                if (!dao.capNhatTheoTenVaSize(dangChon.getTenSP(), ten, nhom, gia, "S", moTa)) {
                    dao.them(ten, nhom, gia, "S", moTa);
                }
            } else {
                success &= dao.them(ten, nhom, gia, "S", moTa);
            }
        }
        if (chkM.isSelected()) {
            double gia = parseGia(txtGiaM.getText());
            if (gia < 0) return false;
            if (laCapNhat) {
                if (!dao.capNhatTheoTenVaSize(dangChon.getTenSP(), ten, nhom, gia, "M", moTa)) {
                    dao.them(ten, nhom, gia, "M", moTa);
                }
            } else {
                success &= dao.them(ten, nhom, gia, "M", moTa);
            }
        }
        if (chkL.isSelected()) {
            double gia = parseGia(txtGiaL.getText());
            if (gia < 0) return false;
            if (laCapNhat) {
                if (!dao.capNhatTheoTenVaSize(dangChon.getTenSP(), ten, nhom, gia, "L", moTa)) {
                    dao.them(ten, nhom, gia, "L", moTa);
                }
            } else {
                success &= dao.them(ten, nhom, gia, "L", moTa);
            }
        }
        // Xoa cac size khong con duoc chon khi cap nhat
        if (laCapNhat) {
            dao.xoaTheoTenKhongSize(tenCu);
            if (!chkS.isSelected()) dao.xoaTheoTenVaSize(tenCu, "S");
            if (!chkM.isSelected()) dao.xoaTheoTenVaSize(tenCu, "M");
            if (!chkL.isSelected()) dao.xoaTheoTenVaSize(tenCu, "L");
        }
        return success;
    }

    private double parseGia(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            thongBao("Gia ban phai la so.");
            return -1;
        }
    }

    private void taiLaiDuLieu() {
        danhSach.setAll(dao.layTatCa());
    }

    private void lamMoiForm() {
        dangChon = null;
        table.getSelectionModel().clearSelection();
        txtTen.clear();
        cboNhom.setValue(null);
        txtMoTa.clear();
        chkS.setSelected(false); txtGiaS.clear();
        chkM.setSelected(false); txtGiaM.clear();
        chkL.setSelected(false); txtGiaL.clear();
        chkKhongSize.setSelected(false); txtGiaKhongSize.clear();
    }

    private boolean kiemTraHopLe() {
        if (txtTen.getText().trim().isEmpty() || cboNhom.getValue() == null) {
            thongBao("Vui long nhap day du: Ten SP, Nhom.");
            return false;
        }
        if (!chkKhongSize.isSelected() && !chkS.isSelected() && !chkM.isSelected() && !chkL.isSelected()) {
            thongBao("Vui lòng chọn size S/M/L hoặc chọn Không size.");
            return false;
        }
        if (chkKhongSize.isSelected() && txtGiaKhongSize.getText().trim().isEmpty()) {
            thongBao("Vui lòng nhập giá bán cho sản phẩm không có size.");
            return false;
        }
        if (chkS.isSelected() && txtGiaS.getText().trim().isEmpty()) {
            thongBao("Vui long nhap gia cho size S.");
            return false;
        }
        if (chkM.isSelected() && txtGiaM.getText().trim().isEmpty()) {
            thongBao("Vui long nhap gia cho size M.");
            return false;
        }
        if (chkL.isSelected() && txtGiaL.getText().trim().isEmpty()) {
            thongBao("Vui long nhap gia cho size L.");
            return false;
        }
        return true;
    }

    private void thongBao(String noiDung) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Thong bao");
        alert.setHeaderText(null);
        alert.setContentText(noiDung);
        UI.style(alert);
        alert.showAndWait();
    }
}
