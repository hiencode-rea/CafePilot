import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class QuanLyComboView {

    public static class ComboRow {
        private final int maSP;
        private final String tenSP;
        private final double giaBan;
        public ComboRow(int maSP, String tenSP, double giaBan) { this.maSP = maSP; this.tenSP = tenSP; this.giaBan = giaBan; }
        public int getMaSP() { return maSP; }
        public String getTenSP() { return tenSP; }
        public double getGiaBan() { return giaBan; }
    }

    private final ComboDAO comboDAO = new ComboDAO();
    private final SanPhamDAO spDAO = new SanPhamDAO();

    public VBox taoGiaoDien() {
        Label lblTieuDe = new Label("Quản lý combo");
        lblTieuDe.getStyleClass().add("section-title");

        // ----- Bang danh sach combo -----
        ObservableList<ComboRow> danhSachCombo = FXCollections.observableArrayList();
        TableView<ComboRow> table = new TableView<>(danhSachCombo);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Chưa có combo nào."));

        TableColumn<ComboRow, String> colTen = new TableColumn<>("Tên combo");
        colTen.setCellValueFactory(new PropertyValueFactory<>("tenSP"));
        TableColumn<ComboRow, Double> colGia = new TableColumn<>("Giá combo");
        colGia.setCellValueFactory(new PropertyValueFactory<>("giaBan"));
        table.getColumns().addAll(colTen, colGia);
        table.setFixedCellSize(32);
        table.prefHeightProperty().bind(table.fixedCellSizeProperty()
                .multiply(javafx.beans.binding.Bindings.size(danhSachCombo).add(1.4)));
        table.setMinHeight(100);
        table.setMaxHeight(220);

        Label lblThanhPhan = new Label("Thành phần của combo đã chọn:");
        lblThanhPhan.getStyleClass().add("field-label");
        ListView<String> dsThanhPhan = new ListView<>();
        dsThanhPhan.setPrefHeight(100);

        table.getSelectionModel().selectedItemProperty().addListener((obs, cu, moi) -> {
            dsThanhPhan.getItems().clear();
            if (moi != null) {
                for (Object[] tp : comboDAO.layThanhPhanCombo(moi.getMaSP())) {
                    dsThanhPhan.getItems().add(tp[0] + "  x" + tp[1]);
                }
            }
        });

        Runnable[] taiLaiCombo = new Runnable[1];
        taiLaiCombo[0] = () -> {
            danhSachCombo.clear();
            for (Object[] c : comboDAO.layTatCaCombo()) {
                danhSachCombo.add(new ComboRow((int) c[0], (String) c[1], (double) c[2]));
            }
        };
        taiLaiCombo[0].run();

        // ----- Form tao combo moi -----
        Label lblTaoMoi = new Label("Tạo combo mới");
        lblTaoMoi.getStyleClass().add("field-label");

        TextField txtTenCombo = new TextField();
        txtTenCombo.setPromptText("Tên combo, vd: Combo Sáng");
        txtTenCombo.getStyleClass().add("text-field-rounded");

        TextField txtGiaCombo = new TextField();
        txtGiaCombo.setPromptText("Giá combo");
        txtGiaCombo.getStyleClass().add("text-field-rounded");

        List<SanPham> dsSanPhamThuong = new ArrayList<>();
        for (SanPham sp : spDAO.layTatCa()) {
            if (!"Combo".equalsIgnoreCase(sp.getNhomSP())) dsSanPhamThuong.add(sp);
        }
        ComboBox<SanPham> cboThanhPhan = new ComboBox<>(FXCollections.observableArrayList(dsSanPhamThuong));
        cboThanhPhan.setPromptText("Chọn sản phẩm thành phần");
        cboThanhPhan.getStyleClass().add("combo-box-rounded");
        cboThanhPhan.setPrefWidth(200);

        Spinner<Integer> spnSoLuong = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 1));
        spnSoLuong.setEditable(true);
        spnSoLuong.setPrefWidth(70);

        Button btnThemThanhPhan = new Button("+ Thêm thành phần");
        btnThemThanhPhan.getStyleClass().add("btn-success");

        List<int[]> thanhPhanTam = new ArrayList<>();
        ListView<String> dsThanhPhanTam = new ListView<>();
        dsThanhPhanTam.setPrefHeight(100);

        btnThemThanhPhan.setOnAction(e -> {
            SanPham sp = cboThanhPhan.getValue();
            if (sp == null) { canhBao("Vui lòng chọn sản phẩm thành phần."); return; }
            int sl = spnSoLuong.getValue();
            thanhPhanTam.add(new int[]{ sp.getMaSP(), sl });
            dsThanhPhanTam.getItems().add(sp.getTenSP() + "  x" + sl);
        });

        Button btnTaoCombo = new Button("TẠO COMBO");
        btnTaoCombo.getStyleClass().add("btn-update");
        btnTaoCombo.setMaxWidth(Double.MAX_VALUE);

        btnTaoCombo.setOnAction(e -> {
            String ten = txtTenCombo.getText().trim();
            String giaStr = txtGiaCombo.getText().trim();
            if (ten.isEmpty() || giaStr.isEmpty() || thanhPhanTam.isEmpty()) {
                canhBao("Vui lòng nhập tên, giá combo và thêm ít nhất 1 thành phần.");
                return;
            }
            double gia;
            try { gia = Double.parseDouble(giaStr); }
            catch (NumberFormatException ex) { canhBao("Giá combo phải là số."); return; }

            boolean ok = comboDAO.taoCombo(ten, gia, thanhPhanTam);
            if (ok) {
                thongBao("Tạo combo thành công! Combo đã xuất hiện trong danh sách sản phẩm.");
                txtTenCombo.clear();
                txtGiaCombo.clear();
                thanhPhanTam.clear();
                dsThanhPhanTam.getItems().clear();
                taiLaiCombo[0].run();
            } else {
                canhBao("Tạo combo thất bại.");
            }
        });

        Button btnXoaCombo = new Button("Xóa combo đã chọn");
        btnXoaCombo.getStyleClass().add("btn-danger");
        btnXoaCombo.setOnAction(e -> {
            ComboRow chon = table.getSelectionModel().getSelectedItem();
            if (chon == null) { canhBao("Vui lòng chọn 1 combo trong bảng trước."); return; }
            boolean ok = comboDAO.xoaCombo(chon.getMaSP());
            if (ok) { thongBao("Đã xóa combo."); taiLaiCombo[0].run(); dsThanhPhan.getItems().clear(); }
            else canhBao("Không thể xóa (combo có thể đang nằm trong đơn hàng).");
        });

        HBox hangChonThanhPhan = new HBox(10, cboThanhPhan, spnSoLuong, btnThemThanhPhan);
        hangChonThanhPhan.setAlignment(Pos.CENTER_LEFT);

        VBox formTaoMoi = new VBox(10, lblTaoMoi, txtTenCombo, txtGiaCombo, hangChonThanhPhan,
                new Label("Thành phần đã thêm:"), dsThanhPhanTam, btnTaoCombo);

        VBox card = new VBox(18, lblTieuDe, table, lblThanhPhan, dsThanhPhan, btnXoaCombo, formTaoMoi);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(26));

        VBox root = new VBox(card);
        root.setPadding(new Insets(24));
        return root;
    }

    private void thongBao(String noiDung) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(noiDung);
        UI.style(alert);
        alert.showAndWait();
    }

    private void canhBao(String noiDung) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(noiDung);
        UI.style(alert);
        alert.showAndWait();
    }
}
