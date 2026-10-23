import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

// Danh sách các bàn mà Mobile đã gửi yêu cầu thanh toán, chờ thu ngân xác nhận trên Desktop
public class YeuCauThanhToanDialog {

    public static class DongYeuCau {
        private final int maBan;
        private final String banHienThi;
        private final String khuVuc;
        private final String tongTien;
        private final String thoiGian;

        public DongYeuCau(int maBan, String soHieuBan, String khuVuc, double tong, Timestamp tg) {
            this.maBan = maBan;
            this.banHienThi = "Bàn " + soHieuBan;
            this.khuVuc = khuVuc;
            this.tongTien = tong + " VND";
            this.thoiGian = tg == null ? "-" : tg.toLocalDateTime().format(DateTimeFormatter.ofPattern("HH:mm dd/MM"));
        }
        public int getMaBan() { return maBan; }
        public String getBanHienThi() { return banHienThi; }
        public String getKhuVuc() { return khuVuc; }
        public String getTongTien() { return tongTien; }
        public String getThoiGian() { return thoiGian; }
    }

    public static void moPopup(String tenDangNhap, Runnable onDong) {
        DonHangDAO dhDAO = new DonHangDAO();
        BanDAO banDAO = new BanDAO();

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Yêu cầu thanh toán từ Mobile");

        Label lblTitle = new Label("\uD83D\uDD14 Yêu cầu thanh toán từ Mobile");
        lblTitle.getStyleClass().add("section-title");
        Label lblMoTa = new Label("Nhân viên phục vụ đã gửi yêu cầu. Thu ngân kiểm tra đơn rồi xác nhận thanh toán.");
        lblMoTa.getStyleClass().add("card-subtitle");

        ObservableList<DongYeuCau> ds = FXCollections.observableArrayList();
        TableView<DongYeuCau> table = new TableView<>(ds);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Không có yêu cầu thanh toán nào."));
        table.setPrefHeight(260);

        TableColumn<DongYeuCau, String> colBan = new TableColumn<>("Bàn");
        colBan.setCellValueFactory(new PropertyValueFactory<>("banHienThi"));
        TableColumn<DongYeuCau, String> colKV = new TableColumn<>("Khu vực");
        colKV.setCellValueFactory(new PropertyValueFactory<>("khuVuc"));
        TableColumn<DongYeuCau, String> colTong = new TableColumn<>("Tổng tiền");
        colTong.setCellValueFactory(new PropertyValueFactory<>("tongTien"));
        TableColumn<DongYeuCau, String> colTG = new TableColumn<>("Gửi lúc");
        colTG.setCellValueFactory(new PropertyValueFactory<>("thoiGian"));
        table.getColumns().addAll(colBan, colKV, colTong, colTG);

        Runnable[] taiLai = new Runnable[1];
        taiLai[0] = () -> {
            ds.clear();
            for (Object[] r : dhDAO.layDanhSachYeuCauThanhToan()) {
                ds.add(new DongYeuCau((int) r[0], (String) r[1], (String) r[2], (double) r[3], (Timestamp) r[4]));
            }
        };
        taiLai[0].run();

        Button btnMo = new Button("Xem đơn & thanh toán");
        btnMo.getStyleClass().add("btn-success");
        btnMo.setOnAction(e -> {
            DongYeuCau chon = table.getSelectionModel().getSelectedItem();
            if (chon == null) { canhBao("Vui lòng chọn 1 yêu cầu trong bảng."); return; }
            Ban ban = null;
            for (Ban b : banDAO.layTatCa()) if (b.getMaBan() == chon.getMaBan()) { ban = b; break; }
            if (ban == null) { canhBao("Không tìm thấy bàn."); return; }
            ChiTietDonHangDialog.moPopup(ban, tenDangNhap, null);
            taiLai[0].run();
        });

        Button btnTuChoi = new Button("Từ chối yêu cầu");
        btnTuChoi.getStyleClass().add("btn-danger");
        btnTuChoi.setOnAction(e -> {
            DongYeuCau chon = table.getSelectionModel().getSelectedItem();
            if (chon == null) { canhBao("Vui lòng chọn 1 yêu cầu trong bảng."); return; }
            dhDAO.tuChoiYeuCauThanhToan(chon.getMaBan());
            taiLai[0].run();
        });

        Button btnLamMoi = new Button("Làm mới");
        btnLamMoi.getStyleClass().add("btn-outline");
        btnLamMoi.setOnAction(e -> taiLai[0].run());

        Button btnDong = new Button("Đóng");
        btnDong.getStyleClass().add("btn-outline");
        btnDong.setOnAction(e -> {
            dialog.close();
            if (onDong != null) onDong.run();
        });

        HBox hangNut = new HBox(10, btnMo, btnTuChoi, btnLamMoi, btnDong);
        hangNut.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(14, lblTitle, lblMoTa, table, hangNut);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(24));

        VBox wrapper = new VBox(card);
        wrapper.setPadding(new Insets(20));
        wrapper.getStyleClass().add("root-bg-light");

        Scene scene = new Scene(wrapper, 640, 440);
        scene.getStylesheets().add(YeuCauThanhToanDialog.class.getResource("style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.setOnHidden(e -> { if (onDong != null) onDong.run(); });
        dialog.showAndWait();
    }

    private static void canhBao(String noiDung) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(noiDung);
        UI.style(alert);
        alert.showAndWait();
    }
}
