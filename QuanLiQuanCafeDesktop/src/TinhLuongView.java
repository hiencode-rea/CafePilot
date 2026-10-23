import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.Map;

public class TinhLuongView {

    private final LuongDAO dao = new LuongDAO();
    private Map<String, Integer> mapNV;
    private final ObservableList<Luong> danhSachLuong = FXCollections.observableArrayList();

    public VBox taoGiaoDien() {
        Label lblTieuDe = new Label("Tính lương");
        lblTieuDe.getStyleClass().add("section-title");

        ComboBox<Integer> cboThang = new ComboBox<>();
        for (int i = 1; i <= 12; i++) cboThang.getItems().add(i);
        cboThang.setValue(LocalDate.now().getMonthValue());
        cboThang.getStyleClass().add("combo-box-rounded");

        TextField txtNam = new TextField(String.valueOf(LocalDate.now().getYear()));
        txtNam.getStyleClass().add("text-field-rounded");
        txtNam.setPrefWidth(90);

        mapNV = dao.layDanhSachNhanVien();
        ComboBox<String> cboNV = new ComboBox<>(FXCollections.observableArrayList(mapNV.keySet()));
        cboNV.setPromptText("Chọn nhân viên");
        cboNV.getStyleClass().add("combo-box-rounded");
        cboNV.setPrefWidth(220);

        Label lblTongGio = new Label("Tổng giờ làm: -");
        lblTongGio.getStyleClass().add("card-subtitle");

        Label lblDonGiaGio = new Label("Đơn giá / giờ (PartTime)");
        lblDonGiaGio.getStyleClass().add("field-label");
        TextField txtDonGiaGio = new TextField();
        txtDonGiaGio.setPromptText("VD: 25000");
        txtDonGiaGio.getStyleClass().add("text-field-rounded");
        VBox khoiDonGiaGio = new VBox(4, lblDonGiaGio, txtDonGiaGio);

        Label lblLuongCoBan = new Label("Lương cơ bản (FullTime)");
        lblLuongCoBan.getStyleClass().add("field-label");
        TextField txtLuongCoBan = new TextField();
        txtLuongCoBan.setPromptText("VD: 6000000");
        txtLuongCoBan.getStyleClass().add("text-field-rounded");
        VBox khoiLuongCoBan = new VBox(4, lblLuongCoBan, txtLuongCoBan);
        khoiLuongCoBan.setVisible(false);
        khoiLuongCoBan.setManaged(false);

        TextField txtPhuCap = new TextField("0");
        txtPhuCap.getStyleClass().add("text-field-rounded");
        TextField txtThuong = new TextField("0");
        txtThuong.getStyleClass().add("text-field-rounded");
        TextField txtPhat = new TextField("0");
        txtPhat.getStyleClass().add("text-field-rounded");

        HBox khoiRieng = new HBox(16, khoiDonGiaGio, khoiLuongCoBan);

        cboNV.setOnAction(e -> {
            if (cboNV.getValue() == null) return;
            int maNV = mapNV.get(cboNV.getValue());
            String loai = dao.layLoaiHopDong(maNV);
            boolean laPartTime = "PartTime".equalsIgnoreCase(loai);
            khoiDonGiaGio.setVisible(laPartTime);
            khoiDonGiaGio.setManaged(laPartTime);
            khoiLuongCoBan.setVisible(!laPartTime);
            khoiLuongCoBan.setManaged(!laPartTime);

            double tongGio = dao.tinhTongGioLam(maNV, cboThang.getValue(), layNam(txtNam));
            lblTongGio.setText("Tổng giờ làm: " + tongGio + " giờ (tính từ dữ liệu chấm công)");
        });

        HBox hangChon = new HBox(16,
                truong("Tháng", cboThang), truong("Năm", txtNam), truong("Nhân viên", cboNV)
        );
        hangChon.setAlignment(Pos.CENTER_LEFT);

        HBox hangPhu = new HBox(16,
                truong("Phụ cấp", txtPhuCap), truong("Thưởng", txtThuong), truong("Phạt", txtPhat)
        );
        hangPhu.setAlignment(Pos.CENTER_LEFT);

        Label lblKetQua = new Label("Thực lĩnh: -");
        lblKetQua.getStyleClass().add("card-title");

        Button btnTinh = new Button("Tính lương");
        btnTinh.getStyleClass().add("btn-update");
        Button btnLuu = new Button("Lưu kết quả");
        btnLuu.getStyleClass().add("btn-success");

        final double[] thucLinhTinhDuoc = {0};
        final double[] tongGioDaTinh = {0};

        btnTinh.setOnAction(e -> {
            if (cboNV.getValue() == null) { canhBao("Vui lòng chọn nhân viên."); return; }
            try {
                int maNV = mapNV.get(cboNV.getValue());
                int thang = cboThang.getValue();
                int nam = layNam(txtNam);
                String loai = dao.layLoaiHopDong(maNV);

                double tongGio = dao.tinhTongGioLam(maNV, thang, nam);
                tongGioDaTinh[0] = tongGio;
                lblTongGio.setText("Tổng giờ làm: " + tongGio + " giờ (tính từ dữ liệu chấm công)");

                double phuCap = layso(txtPhuCap);
                double thuong = layso(txtThuong);
                double phat = layso(txtPhat);
                double thucLinh;

                if ("PartTime".equalsIgnoreCase(loai)) {
                    double donGiaGio = layso(txtDonGiaGio);
                    thucLinh = tongGio * donGiaGio + phuCap + thuong - phat;
                } else {
                    double luongCoBan = layso(txtLuongCoBan);
                    thucLinh = luongCoBan + phuCap + thuong - phat;
                }

                thucLinhTinhDuoc[0] = thucLinh;
                lblKetQua.setText("Thực lĩnh: " + thucLinh + " VND");

            } catch (NumberFormatException ex) {
                canhBao("Vui lòng nhập số hợp lệ cho các ô Đơn giá/Lương cơ bản/Phụ cấp/Thưởng/Phạt.");
            }
        });

        btnLuu.setOnAction(e -> {
            if (cboNV.getValue() == null) { canhBao("Vui lòng chọn nhân viên và bấm Tính lương trước."); return; }
            int maNV = mapNV.get(cboNV.getValue());
            int thang = cboThang.getValue();
            int nam = layNam(txtNam);
            String loai = dao.layLoaiHopDong(maNV);

            double donGiaGio = "PartTime".equalsIgnoreCase(loai) ? layso(txtDonGiaGio) : 0;
            double luongCoBan = "FullTime".equalsIgnoreCase(loai) ? layso(txtLuongCoBan) : 0;

            boolean ok = dao.luuLuong(maNV, thang, nam, tongGioDaTinh[0], donGiaGio, luongCoBan,
                    layso(txtPhuCap), layso(txtThuong), layso(txtPhat), thucLinhTinhDuoc[0]);

            if (ok) {
                thongBao("Đã lưu bảng lương tháng " + thang + "/" + nam + "!");
                danhSachLuong.setAll(dao.layBangLuong(thang, nam));
            } else {
                canhBao("Lưu thất bại.");
            }
        });

        HBox hopNut = new HBox(12, btnTinh, btnLuu);
        hopNut.setPadding(new Insets(6, 0, 0, 0));

        Label lblBangLuong = new Label("Bảng lương đã lưu");
        lblBangLuong.getStyleClass().add("field-label");

        TableView<Luong> table = new TableView<>(danhSachLuong);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Chưa có dữ liệu lương cho tháng/năm này."));

        TableColumn<Luong, String> colTen = new TableColumn<>("Nhân viên");
        colTen.setCellValueFactory(new PropertyValueFactory<>("tenNV"));
        TableColumn<Luong, String> colLoai = new TableColumn<>("Loại HĐ");
        colLoai.setCellValueFactory(new PropertyValueFactory<>("loaiHopDong"));
        TableColumn<Luong, Double> colGio = new TableColumn<>("Giờ làm");
        colGio.setCellValueFactory(new PropertyValueFactory<>("tongGioLam"));
        TableColumn<Luong, Double> colThucLinh = new TableColumn<>("Thực lĩnh");
        colThucLinh.setCellValueFactory(new PropertyValueFactory<>("thucLinh"));

        table.getColumns().addAll(colTen, colLoai, colGio, colThucLinh);
        table.setFixedCellSize(32);
        table.prefHeightProperty().bind(table.fixedCellSizeProperty()
                .multiply(javafx.beans.binding.Bindings.size(danhSachLuong).add(1.4)));
        table.setMinHeight(100);
        table.setMaxHeight(280);

        cboThang.setOnAction(e -> danhSachLuong.setAll(dao.layBangLuong(cboThang.getValue(), layNam(txtNam))));
        txtNam.textProperty().addListener((obs, cu, moi) -> {
            try { danhSachLuong.setAll(dao.layBangLuong(cboThang.getValue(), Integer.parseInt(moi.trim()))); }
            catch (Exception ex) { /* dang go do, bo qua */ }
        });

        danhSachLuong.setAll(dao.layBangLuong(cboThang.getValue(), layNam(txtNam)));

        VBox card = new VBox(16, lblTieuDe, hangChon, lblTongGio, khoiRieng, hangPhu,
                lblKetQua, hopNut, lblBangLuong, table);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(26));

        VBox root = new VBox(card);
        root.setPadding(new Insets(24));
        return root;
    }

    private int layNam(TextField txtNam) {
        try { return Integer.parseInt(txtNam.getText().trim()); }
        catch (Exception e) { return LocalDate.now().getYear(); }
    }

    private double layso(TextField tf) {
        String s = tf.getText().trim();
        return s.isEmpty() ? 0 : Double.parseDouble(s);
    }

    private VBox truong(String nhan, javafx.scene.control.Control o) {
        Label lbl = new Label(nhan);
        lbl.getStyleClass().add("field-label");
        return new VBox(4, lbl, o);
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
