import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.Map;

public class QuanLyCaChamCongView {

    private final CaLamViecDAO caDAO = new CaLamViecDAO();
    private final ChamCongDAO ccDAO = new ChamCongDAO();

    public VBox taoGiaoDien() {
        Label lblTieuDe = new Label("Ca làm việc / Chấm công");
        lblTieuDe.getStyleClass().add("section-title");

        TabPane tabPane = new TabPane();
        Tab tabCa = new Tab("Ca làm việc", taoTabCaLamViec());
        Tab tabCC = new Tab("Chấm công", taoTabChamCong());
        tabCa.setClosable(false);
        tabCC.setClosable(false);
        tabPane.getTabs().addAll(tabCa, tabCC);

        VBox card = new VBox(16, lblTieuDe, tabPane);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(26));

        VBox root = new VBox(card);
        root.setPadding(new Insets(24));
        return root;
    }

    // ================= TAB 1: CA LAM VIEC =================
    private VBox taoTabCaLamViec() {
        ObservableList<CaLamViec> danhSach = FXCollections.observableArrayList();
        TableView<CaLamViec> table = new TableView<>();

        TableColumn<CaLamViec, Integer> colMa = new TableColumn<>("Mã ca");
        colMa.setCellValueFactory(new PropertyValueFactory<>("maCa"));

        TableColumn<CaLamViec, String> colTen = new TableColumn<>("Nhân viên");
        colTen.setCellValueFactory(new PropertyValueFactory<>("tenNV"));

        TableColumn<CaLamViec, String> colNgay = new TableColumn<>("Ngày");
        colNgay.setCellValueFactory(new PropertyValueFactory<>("ngay"));

        TableColumn<CaLamViec, String> colBatDau = new TableColumn<>("Giờ bắt đầu");
        colBatDau.setCellValueFactory(new PropertyValueFactory<>("gioBatDau"));

        TableColumn<CaLamViec, String> colKetThuc = new TableColumn<>("Giờ kết thúc");
        colKetThuc.setCellValueFactory(new PropertyValueFactory<>("gioKetThuc"));

        table.getColumns().addAll(colMa, colTen, colNgay, colBatDau, colKetThuc);
        table.setItems(danhSach);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Chưa có ca làm việc nào."));
        table.setFixedCellSize(32);
        table.prefHeightProperty().bind(table.fixedCellSizeProperty()
                .multiply(javafx.beans.binding.Bindings.size(danhSach).add(1.4)));
        table.setMinHeight(100);
        table.setMaxHeight(300);

        Map<String, Integer> mapNV = caDAO.layDanhSachNhanVien();
        ComboBox<String> cboNV = new ComboBox<>(FXCollections.observableArrayList(mapNV.keySet()));
        cboNV.setPromptText("Chọn nhân viên");
        cboNV.getStyleClass().add("combo-box-rounded");

        DatePicker dpNgay = new DatePicker(LocalDate.now());
        dpNgay.getStyleClass().add("text-field-rounded");

        TextField txtGioBatDau = new TextField();
        txtGioBatDau.setPromptText("VD: 08:00");
        txtGioBatDau.getStyleClass().add("text-field-rounded");

        TextField txtGioKetThuc = new TextField();
        txtGioKetThuc.setPromptText("VD: 12:00");
        txtGioKetThuc.getStyleClass().add("text-field-rounded");

        final CaLamViec[] dangChon = {null};

        table.getSelectionModel().selectedItemProperty().addListener((obs, cu, moi) -> {
            if (moi != null) {
                dangChon[0] = moi;
                dpNgay.setValue(LocalDate.parse(moi.getNgay()));
                txtGioBatDau.setText(moi.getGioBatDau());
                txtGioKetThuc.setText(moi.getGioKetThuc());
                for (String key : mapNV.keySet()) {
                    if (mapNV.get(key) == moi.getMaNV()) { cboNV.setValue(key); break; }
                }
            }
        });

        HBox form = new HBox(16,
                truong("Nhân viên", cboNV),
                truong("Ngày", dpNgay),
                truong("Giờ bắt đầu", txtGioBatDau),
                truong("Giờ kết thúc", txtGioKetThuc)
        );
        form.setAlignment(Pos.CENTER_LEFT);

        Button btnThem = new Button("+  Thêm ca");
        Button btnSua = new Button("Cập nhật");
        Button btnXoa = new Button("Xóa");
        Button btnLamMoi = new Button("Làm mới");
        btnThem.getStyleClass().add("btn-success");
        btnSua.getStyleClass().add("btn-update");
        btnXoa.getStyleClass().add("btn-danger");
        btnLamMoi.getStyleClass().add("btn-outline");

        Runnable[] taiLai = new Runnable[1];
        taiLai[0] = () -> danhSach.setAll(caDAO.layTatCa());

        Runnable lamMoiForm = () -> {
            dangChon[0] = null;
            table.getSelectionModel().clearSelection();
            cboNV.setValue(null);
            dpNgay.setValue(LocalDate.now());
            txtGioBatDau.clear();
            txtGioKetThuc.clear();
        };

        btnThem.setOnAction(e -> {
            if (cboNV.getValue() == null || txtGioBatDau.getText().trim().isEmpty() || txtGioKetThuc.getText().trim().isEmpty()) {
                canhBao("Vui lòng nhập đầy đủ thông tin.");
                return;
            }
            boolean ok = caDAO.them(mapNV.get(cboNV.getValue()), dpNgay.getValue().toString(),
                    txtGioBatDau.getText().trim(), txtGioKetThuc.getText().trim());
            if (ok) { thongBao("Thêm ca làm việc thành công!"); lamMoiForm.run(); taiLai[0].run(); }
            else canhBao("Thêm thất bại. Kiểm tra định dạng giờ (HH:mm).");
        });

        btnSua.setOnAction(e -> {
            if (dangChon[0] == null) { canhBao("Vui lòng chọn 1 ca trong bảng trước."); return; }
            boolean ok = caDAO.capNhat(dangChon[0].getMaCa(), mapNV.get(cboNV.getValue()),
                    dpNgay.getValue().toString(), txtGioBatDau.getText().trim(), txtGioKetThuc.getText().trim());
            if (ok) { thongBao("Cập nhật thành công!"); lamMoiForm.run(); taiLai[0].run(); }
            else canhBao("Cập nhật thất bại.");
        });

        btnXoa.setOnAction(e -> {
            if (dangChon[0] == null) { canhBao("Vui lòng chọn 1 ca trong bảng trước."); return; }
            boolean ok = caDAO.xoa(dangChon[0].getMaCa());
            if (ok) { thongBao("Đã xóa ca làm việc."); lamMoiForm.run(); taiLai[0].run(); }
            else canhBao("Không thể xóa (ca này có thể đã có dữ liệu chấm công).");
        });

        btnLamMoi.setOnAction(e -> lamMoiForm.run());

        HBox hopNut = new HBox(12, btnThem, btnSua, btnXoa, btnLamMoi);
        hopNut.setPadding(new Insets(10, 0, 0, 0));

        taiLai[0].run();

        VBox root = new VBox(16, table, form, hopNut);
        root.setPadding(new Insets(16, 0, 0, 0));
        return root;
    }

    // ================= TAB 2: CHAM CONG =================
    private VBox taoTabChamCong() {
        ObservableList<ChamCong> danhSach = FXCollections.observableArrayList();
        TableView<ChamCong> table = new TableView<>();

        TableColumn<ChamCong, String> colCa = new TableColumn<>("Ca làm việc");
        colCa.setCellValueFactory(new PropertyValueFactory<>("thongTinCa"));
        colCa.setPrefWidth(260);

        TableColumn<ChamCong, String> colVao = new TableColumn<>("Giờ vào");
        colVao.setCellValueFactory(new PropertyValueFactory<>("gioVao"));

        TableColumn<ChamCong, String> colRa = new TableColumn<>("Giờ ra");
        colRa.setCellValueFactory(new PropertyValueFactory<>("gioRa"));

        table.getColumns().addAll(colCa, colVao, colRa);
        table.setItems(danhSach);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Chưa có dữ liệu chấm công."));
        table.setFixedCellSize(32);
        table.prefHeightProperty().bind(table.fixedCellSizeProperty()
                .multiply(javafx.beans.binding.Bindings.size(danhSach).add(1.4)));
        table.setMinHeight(100);
        table.setMaxHeight(300);

        ComboBox<String> cboCaVao = new ComboBox<>();
        cboCaVao.setPromptText("Chọn ca để chấm công VÀO");
        cboCaVao.getStyleClass().add("combo-box-rounded");
        cboCaVao.setPrefWidth(260);

        ComboBox<String> cboCaRa = new ComboBox<>();
        cboCaRa.setPromptText("Chọn ca để chấm công RA");
        cboCaRa.getStyleClass().add("combo-box-rounded");
        cboCaRa.setPrefWidth(260);

        Button btnVao = new Button("Chấm công VÀO");
        Button btnRa = new Button("Chấm công RA");
        btnVao.getStyleClass().add("btn-success");
        btnRa.getStyleClass().add("btn-danger");

        Runnable[] taiLai = new Runnable[1];
        Map<String, Integer>[] mapVao = new Map[1];
        Map<String, Integer>[] mapRa = new Map[1];

        taiLai[0] = () -> {
            danhSach.setAll(ccDAO.layTatCa());
            mapVao[0] = ccDAO.layCaChuaChamCongVao();
            cboCaVao.setItems(FXCollections.observableArrayList(mapVao[0].keySet()));
            mapRa[0] = ccDAO.layChamCongChuaRa();
            cboCaRa.setItems(FXCollections.observableArrayList(mapRa[0].keySet()));
        };

        btnVao.setOnAction(e -> {
            if (cboCaVao.getValue() == null) { canhBao("Vui lòng chọn ca làm việc."); return; }
            boolean ok = ccDAO.chamCongVao(mapVao[0].get(cboCaVao.getValue()));
            if (ok) { thongBao("Chấm công vào thành công!"); cboCaVao.setValue(null); taiLai[0].run(); }
            else canhBao("Chấm công thất bại.");
        });

        btnRa.setOnAction(e -> {
            if (cboCaRa.getValue() == null) { canhBao("Vui lòng chọn bản ghi chấm công."); return; }
            boolean ok = ccDAO.chamCongRa(mapRa[0].get(cboCaRa.getValue()));
            if (ok) { thongBao("Chấm công ra thành công!"); cboCaRa.setValue(null); taiLai[0].run(); }
            else canhBao("Chấm công thất bại.");
        });

        HBox form = new HBox(16,
                truong("Chấm công VÀO", cboCaVao),
                truong("Chấm công RA", cboCaRa)
        );
        form.setAlignment(Pos.CENTER_LEFT);

        HBox hopNut = new HBox(12, btnVao, btnRa);
        hopNut.setPadding(new Insets(10, 0, 0, 0));

        taiLai[0].run();

        VBox root = new VBox(16, table, form, hopNut);
        root.setPadding(new Insets(16, 0, 0, 0));
        return root;
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
