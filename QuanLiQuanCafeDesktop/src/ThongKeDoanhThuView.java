import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

public class ThongKeDoanhThuView {

    private final DoanhThuDAO dao = new DoanhThuDAO();

    public VBox taoGiaoDien() {
        Label lblTieuDe = new Label("Thống kê doanh thu");
        lblTieuDe.getStyleClass().add("section-title");

        ComboBox<String> cboKieu = new ComboBox<>(FXCollections.observableArrayList(
                "Theo ngày (chọn khoảng ngày)", "Theo tháng (chọn năm)"
        ));
        cboKieu.setValue("Theo ngày (chọn khoảng ngày)");
        cboKieu.getStyleClass().add("combo-box-rounded");

        TextField txtTuNgay = new TextField(LocalDate.now().minusDays(6).toString());
        txtTuNgay.setPromptText("YYYY-MM-DD");
        txtTuNgay.getStyleClass().add("text-field-rounded");

        TextField txtDenNgay = new TextField(LocalDate.now().toString());
        txtDenNgay.setPromptText("YYYY-MM-DD");
        txtDenNgay.getStyleClass().add("text-field-rounded");

        TextField txtNam = new TextField(String.valueOf(LocalDate.now().getYear()));
        txtNam.getStyleClass().add("text-field-rounded");
        txtNam.setPrefWidth(90);

        HBox khoiNgay = new HBox(16, truong("Từ ngày", txtTuNgay), truong("Đến ngày", txtDenNgay));
        HBox khoiThang = new HBox(16, truong("Năm", txtNam));
        khoiThang.setVisible(false);
        khoiThang.setManaged(false);

        cboKieu.setOnAction(e -> {
            boolean theoNgay = cboKieu.getValue().startsWith("Theo ngày");
            khoiNgay.setVisible(theoNgay); khoiNgay.setManaged(theoNgay);
            khoiThang.setVisible(!theoNgay); khoiThang.setManaged(!theoNgay);
        });

        Button btnXem = new Button("Xem thống kê");
        btnXem.getStyleClass().add("btn-update");

        Label lblTongQuan = new Label("Tổng doanh thu: - | Tổng số đơn: -");
        lblTongQuan.getStyleClass().add("card-title");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Doanh thu (VND)");
        BarChart<String, Number> bieuDo = new BarChart<>(xAxis, yAxis);
        bieuDo.setLegendVisible(false);
        bieuDo.setPrefHeight(300);

        TableView<Object[]> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Chưa có dữ liệu."));

        TableColumn<Object[], String> colMoc = new TableColumn<>("Mốc thời gian");
        colMoc.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(data.getValue()[0])));

        TableColumn<Object[], String> colTong = new TableColumn<>("Doanh thu (VND)");
        colTong.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(data.getValue()[1])));

        TableColumn<Object[], String> colSoDon = new TableColumn<>("Số đơn");
        colSoDon.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(data.getValue()[2])));

        table.getColumns().addAll(colMoc, colTong, colSoDon);
        table.setFixedCellSize(32);
        table.setMinHeight(100);
        table.setMaxHeight(260);

        btnXem.setOnAction(e -> {
            List<Object[]> ketQua;
            if (cboKieu.getValue().startsWith("Theo ngày")) {
                ketQua = dao.layDoanhThuTheoNgay(txtTuNgay.getText().trim(), txtDenNgay.getText().trim());
            } else {
                ketQua = dao.layDoanhThuTheoThang(Integer.parseInt(txtNam.getText().trim()));
            }

            table.getItems().setAll(ketQua);

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            double tongDoanhThu = 0;
            int tongSoDon = 0;
            for (Object[] dong : ketQua) {
                series.getData().add(new XYChart.Data<>((String) dong[0], (Double) dong[1]));
                tongDoanhThu += (Double) dong[1];
                tongSoDon += (Integer) dong[2];
            }
            bieuDo.getData().setAll(series);
            lblTongQuan.setText("Tổng doanh thu: " + tongDoanhThu + " VND | Tổng số đơn: " + tongSoDon);
        });

        HBox hangChon = new HBox(16, truong("Kiểu thống kê", cboKieu), khoiNgay, khoiThang);
        hangChon.setAlignment(Pos.CENTER_LEFT);

        HBox hopNut = new HBox(btnXem);
        hopNut.setPadding(new Insets(6, 0, 0, 0));

        VBox card = new VBox(16, lblTieuDe, hangChon, hopNut, lblTongQuan, bieuDo, table);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(26));

        VBox root = new VBox(card);
        root.setPadding(new Insets(24));
        return root;
    }

    private VBox truong(String nhan, javafx.scene.control.Control o) {
        Label lbl = new Label(nhan);
        lbl.getStyleClass().add("field-label");
        return new VBox(4, lbl, o);
    }
}
