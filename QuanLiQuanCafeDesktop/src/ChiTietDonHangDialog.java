import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Man hinh gop: vua Them mon vua Xem chi tiet cho 1 ban - dung chung cho ban Trong lan Dang phuc vu.
// Co the dong/mo lai bat cu luc nao, du lieu luon dong bo voi CSDL (moi thao tac luu xuong ngay).
public class ChiTietDonHangDialog {

    public static class DongMon {
        int maSP;
        String tenSP;
        int soLuong;
        double donGia;
        double thanhTien;
        boolean daThongBao;
        String trangThaiMon = "";
        String size = "";
        String moTa = "";
        String ghiChu = "";

        public String getTenSP() { return tenSP; }
        public int getSoLuong() { return soLuong; }
        public double getDonGia() { return donGia; }
        public double getThanhTien() { return thanhTien; }
        public String getSize() { return size; }
        public String getMoTa() { return moTa; }
        public String getGhiChu() { return ghiChu; }
        public String getTrangThai() {
            if (!daThongBao) return "Chưa báo";
            String state = trangThaiMon == null || trangThaiMon.isEmpty() ? "CHO_LAM" : trangThaiMon;
            return "✓ " + tenTrangThai(state);
        }

        private String tenTrangThai(String state) {
            if ("CHO_LAM".equals(state)) return "Chờ làm";
            if ("DANG_LAM".equals(state)) return "Đang làm";
            if ("DA_RA_MON".equals(state)) return "Đã ra món";
            if ("HET_MON".equals(state)) return "Hết món";
            return state;
        }
    }

    public static void moPopup(Ban ban, String tenDangNhap, Runnable onDongPopup) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Bàn " + ban.getSoHieuBan());

        SanPhamDAO spDAO = new SanPhamDAO();
        DonHangDAO dhDAO = new DonHangDAO();
        LichSuHuyMonDAO lsDAO = new LichSuHuyMonDAO();
        List<SanPham> dsSanPham = spDAO.layTatCa();

        // maDH[0] = -1 nghia la ban chua co don nao dang mo (ban Trong, chua thêm mon lan nao)
        final int[] maDH = {-1};
        Object[] donMo = dhDAO.layThongTinDonMoCuaBan(ban.getMaBan());
        if (donMo != null) maDH[0] = (int) donMo[0];

        Label lblTitle = new Label("BÀN " + ban.getSoHieuBan() + " - " + ban.getTenKhuVuc());
        lblTitle.getStyleClass().add("section-title");

        ComboBox<SanPham> cboSanPham = new ComboBox<>(FXCollections.observableArrayList(dsSanPham));
        cboSanPham.setPromptText("Chọn sản phẩm");
        cboSanPham.getStyleClass().add("combo-box-rounded");
        cboSanPham.setPrefWidth(200);

        Spinner<Integer> spnSoLuong = new Spinner<>();
        spnSoLuong.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 1));
        spnSoLuong.setEditable(true);
        spnSoLuong.setPrefWidth(70);

        TextField txtGhiChu = new TextField();
        txtGhiChu.setPromptText("Ghi chú món (VD: 少 đá, ít ngọt...)");
        txtGhiChu.getStyleClass().add("text-field-rounded");
        txtGhiChu.setPrefWidth(160);

        Button btnThem = new Button("+ Thêm món");
        btnThem.getStyleClass().add("btn-success");

        ObservableList<DongMon> danhSach = FXCollections.observableArrayList();
        TableView<DongMon> table = new TableView<>(danhSach);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(220);

        TableColumn<DongMon, String> colTen = new TableColumn<>("Sản phẩm");
        colTen.setCellValueFactory(new PropertyValueFactory<>("tenSP"));
        // Hien thi ten mon (dam) + mo ta SanPham xam nho phia duoi
        colTen.setCellFactory(col -> new TableCell<DongMon, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                VBox box = new VBox(1);
                Label lblTen = new Label(item);
                lblTen.setStyle("-fx-font-weight: bold;");
                box.getChildren().add(lblTen);
                int idx = getIndex();
                if (getTableView() != null && idx >= 0 && idx < getTableView().getItems().size()) {
                    DongMon m = getTableView().getItems().get(idx);
                    if (m != null && m.moTa != null && !m.moTa.isEmpty()) {
                        Label lblMoTa = new Label(m.moTa);
                        lblMoTa.setStyle("-fx-text-fill: #999999; -fx-font-size: 11px;");
                        lblMoTa.setWrapText(true);
                        box.getChildren().add(lblMoTa);
                    }
                }
                setText(null);
                setGraphic(box);
            }
        });
        TableColumn<DongMon, String> colSize = new TableColumn<>("Size");
        colSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        colSize.setPrefWidth(50);
        TableColumn<DongMon, String> colGhiChu = new TableColumn<>("Ghi chú");
        colGhiChu.setCellValueFactory(new PropertyValueFactory<>("ghiChu"));
        colGhiChu.setPrefWidth(130);
        TableColumn<DongMon, Integer> colSL = new TableColumn<>("SL");
        colSL.setCellValueFactory(new PropertyValueFactory<>("soLuong"));
        TableColumn<DongMon, Double> colGia = new TableColumn<>("Đơn giá");
        colGia.setCellValueFactory(new PropertyValueFactory<>("donGia"));
        TableColumn<DongMon, Double> colThanh = new TableColumn<>("Thành tiền");
        colThanh.setCellValueFactory(new PropertyValueFactory<>("thanhTien"));
        TableColumn<DongMon, String> colTrangThai = new TableColumn<>("Trạng thái");
        colTrangThai.setCellValueFactory(new PropertyValueFactory<>("trangThai"));
        colTrangThai.setCellFactory(col -> new TableCell<DongMon, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(item.startsWith("\u2713")
                        ? "-fx-text-fill: #2e9e5b; -fx-font-weight: bold;"
                        : "-fx-text-fill: #999999;");
            }
        });
        table.getColumns().addAll(colTen, colSize, colSL, colGia, colThanh, colGhiChu, colTrangThai);

        Label lblTongTien = new Label("Tổng tiền: 0 VND");
        lblTongTien.getStyleClass().add("card-title");

        Label lblYeuCau = new Label("\uD83D\uDD14 Mobile đã gửi yêu cầu thanh toán cho bàn này. Kiểm tra đơn rồi bấm \"Thanh toán\" để xác nhận.");
        lblYeuCau.getStyleClass().add("yeucau-notice");
        lblYeuCau.setWrapText(true);
        lblYeuCau.setMaxWidth(Double.MAX_VALUE);
        lblYeuCau.setVisible(false);
        lblYeuCau.setManaged(false);

        Runnable[] taiLai = new Runnable[1];
        taiLai[0] = () -> {
            // Dong bo lai ma don dang mo (don co the da bi huy neu vua xoa/tach het mon)
            Object[] donHienTai = dhDAO.layThongTinDonMoCuaBan(ban.getMaBan());
            maDH[0] = (donHienTai == null) ? -1 : (int) donHienTai[0];
            boolean coYeuCau = maDH[0] != -1 && dhDAO.coYeuCauThanhToan(maDH[0]);
            lblYeuCau.setVisible(coYeuCau);
            lblYeuCau.setManaged(coYeuCau);

            danhSach.clear();
            double tong = 0;
            if (maDH[0] != -1) {
                for (Object[] dong : dhDAO.layChiTietDayDu(maDH[0])) {
                    DongMon m = new DongMon();
                    m.maSP = (int) dong[0];
                    m.tenSP = (String) dong[1];
                    m.soLuong = (int) dong[2];
                    m.donGia = (double) dong[3];
                    m.thanhTien = (double) dong[4];
                    m.daThongBao = (boolean) dong[5];
                    m.trangThaiMon = (String) dong[6];
                    m.size = (String) dong[7];
                    m.moTa = (String) dong[8];
                    m.ghiChu = (String) dong[9];
                    danhSach.add(m);
                    tong += m.thanhTien;
                }
            }
            lblTongTien.setText("Tổng tiền: " + tong + " VND");
        };
        taiLai[0].run();

        btnThem.setOnAction(e -> {
            SanPham sp = cboSanPham.getValue();
            if (sp == null) { canhBao("Vui lòng chọn sản phẩm."); return; }
            int soLuong = spnSoLuong.getValue();

            // Neu ban chua co don nao dang mo thi tao moi ngay luc nay
            if (maDH[0] == -1) {
                int soNguoi = ban.getSoNguoi();
                if (soNguoi <= 0) {
                    TextInputDialog nhapSoNguoi = new TextInputDialog("1");
                    nhapSoNguoi.setTitle("Mở bàn");
                    nhapSoNguoi.setHeaderText("Bàn " + ban.getSoHieuBan() + " đang có bao nhiêu người?");
                    nhapSoNguoi.setContentText("Số người (1 - " + ban.getSucChua() + "):");
                    UI.style(nhapSoNguoi);
                    Optional<String> ketQua = nhapSoNguoi.showAndWait();
                    if (!ketQua.isPresent()) return;
                    try {
                        soNguoi = Integer.parseInt(ketQua.get().trim());
                    } catch (NumberFormatException ex) {
                        canhBao("Số người phải là số hợp lệ.");
                        return;
                    }
                    if (soNguoi < 1 || soNguoi > ban.getSucChua()) {
                        canhBao("Số người phải từ 1 đến " + ban.getSucChua() + ".");
                        return;
                    }
                }
                int maDHmoi = dhDAO.taoDonHangMoi(ban.getMaBan(), tenDangNhap, soNguoi);
                if (maDHmoi == -1) { canhBao("Tạo đơn hàng thất bại."); return; }
                maDH[0] = maDHmoi;
            }

            dhDAO.themChiTiet(maDH[0], sp.getMaSP(), soLuong, sp.getGiaBan(), txtGhiChu.getText());
            txtGhiChu.clear();
            dhDAO.capNhatTongTien(maDH[0]);
            taiLai[0].run();
        });

        Button btnThongBaoBep = new Button("\uD83D\uDD14 Thông báo bếp");
        btnThongBaoBep.getStyleClass().add("btn-update");
        btnThongBaoBep.setOnAction(e -> {
            if (maDH[0] == -1 || danhSach.isEmpty()) { canhBao("Chưa có món nào để thông báo."); return; }
            dhDAO.thongBaoBep(maDH[0]);
            taiLai[0].run();
            thongBao("Đã thông báo bếp!");
        });

        Button btnXoaDong = new Button("Xóa dòng đã chọn");
        btnXoaDong.getStyleClass().add("btn-danger");
        btnXoaDong.setOnAction(e -> {
            DongMon chon = table.getSelectionModel().getSelectedItem();
            if (chon == null) { canhBao("Vui lòng chọn 1 dòng trong bảng trước."); return; }

            if (chon.daThongBao) {
                TextInputDialog nhapLyDo = new TextInputDialog();
                nhapLyDo.setTitle("Xóa món đã báo bếp");
                nhapLyDo.setHeaderText("Món '" + chon.tenSP + "' đã được báo bếp.");
                nhapLyDo.setContentText("Nhập lý do xóa:");

                UI.style(nhapLyDo); Optional<String> ketQua = nhapLyDo.showAndWait();
                if (ketQua.isPresent() && !ketQua.get().trim().isEmpty()) {
                    lsDAO.ghiNhan(chon.tenSP, chon.soLuong, ketQua.get().trim(), tenDangNhap);
                    dhDAO.xoaMonKhoiDon(maDH[0], chon.maSP);
                    dhDAO.capNhatTongTien(maDH[0]);
                    taiLai[0].run();
                } else {
                    canhBao("Phải nhập lý do mới xóa được món đã báo bếp.");
                }
            } else {
                dhDAO.xoaMonKhoiDon(maDH[0], chon.maSP);
                dhDAO.capNhatTongTien(maDH[0]);
                taiLai[0].run();
            }
        });

        Button btnThanhToan = new Button("THANH TOÁN - TẠO HÓA ĐƠN");
        btnThanhToan.getStyleClass().add("btn-success");
        btnThanhToan.setMaxWidth(Double.MAX_VALUE);
        btnThanhToan.setOnAction(e -> {
            if (maDH[0] == -1 || danhSach.isEmpty()) { canhBao("Chưa có món nào để thanh toán."); return; }
            if (dhDAO.coMonChuaHoanTat(maDH[0])) {
                canhBao("Chưa thể thanh toán: vẫn còn món chưa ra món hoặc chưa gửi bếp.");
                taiLai[0].run();
                return;
            }
            HoaDonDialog.moPopup(ban, maDH[0], dhDAO);
            if (!dhDAO.hoanThanhDonHang(ban.getMaBan())) {
                canhBao("Không thể hoàn tất đơn. Trạng thái món có thể vừa thay đổi, vui lòng kiểm tra lại.");
                return;
            }
            dialog.close();
            if (onDongPopup != null) onDongPopup.run();
        });

        Button btnDong = new Button("Đóng");
        btnDong.getStyleClass().add("btn-outline");
        btnDong.setOnAction(e -> {
            dialog.close();
            if (onDongPopup != null) onDongPopup.run(); // de luoi ban ben ngoai cap nhat trang thai neu vua tao don moi
        });

        // ===== Chuyen ban / Gop ban / Tach mon =====
        BanDAO banDAO = new BanDAO();

        Button btnChuyenBan = new Button("Chuyển bàn");
        btnChuyenBan.getStyleClass().add("btn-outline");
        btnChuyenBan.setOnAction(e -> {
            if (maDH[0] == -1) { canhBao("Chưa có đơn hàng để chuyển."); return; }
            List<Ban> dsTrong = new ArrayList<>();
            for (Ban b : banDAO.layTatCa()) {
                if ("Trong".equalsIgnoreCase(b.getTrangThai()) && b.getMaBan() != ban.getMaBan()) dsTrong.add(b);
            }
            if (dsTrong.isEmpty()) { canhBao("Không có bàn trống nào để chuyển đến."); return; }

            ChoiceDialog<String> chon = new ChoiceDialog<>(null, moTaDsBan(dsTrong));
            chon.setTitle("Chuyển bàn");
            chon.setHeaderText("Chuyển đơn hàng từ Bàn " + ban.getSoHieuBan() + " sang bàn nào?");
            UI.style(chon); Optional<String> ketQua = chon.showAndWait();
            if (ketQua.isPresent()) {
                int banMoi = layMaBanTuMoTa(ketQua.get(), dsTrong);
                dhDAO.chuyenBan(maDH[0], ban.getMaBan(), banMoi);
                thongBao("Đã chuyển sang bàn mới!");
                dialog.close();
                if (onDongPopup != null) onDongPopup.run();
            }
        });

        Button btnGopBan = new Button("Gộp bàn");
        btnGopBan.getStyleClass().add("btn-outline");
        btnGopBan.setOnAction(e -> {
            if (maDH[0] == -1) { canhBao("Bàn này chưa có đơn hàng để gộp."); return; }
            List<Ban> dsDangPV = new ArrayList<>();
            for (Ban b : banDAO.layTatCa()) {
                if ("DangPhucVu".equalsIgnoreCase(b.getTrangThai()) && b.getMaBan() != ban.getMaBan()) dsDangPV.add(b);
            }
            if (dsDangPV.isEmpty()) { canhBao("Không có bàn đang phục vụ nào khác để gộp."); return; }

            ChoiceDialog<String> chon = new ChoiceDialog<>(null, moTaDsBan(dsDangPV));
            chon.setTitle("Gộp bàn");
            chon.setHeaderText("Gộp đơn hàng từ bàn nào vào Bàn " + ban.getSoHieuBan() + "?");
            UI.style(chon); Optional<String> ketQua = chon.showAndWait();
            if (ketQua.isPresent()) {
                int banNguon = layMaBanTuMoTa(ketQua.get(), dsDangPV);
                boolean ok = dhDAO.gopBan(maDH[0], banNguon);
                if (ok) {
                    thongBao("Đã gộp đơn hàng thành công!");
                    taiLai[0].run();
                } else {
                    canhBao("Gộp bàn thất bại.");
                }
            }
        });

        Button btnTachMon = new Button("Tách món");
        btnTachMon.getStyleClass().add("btn-outline");
        btnTachMon.setOnAction(e -> {
            DongMon chon = table.getSelectionModel().getSelectedItem();
            if (chon == null) { canhBao("Vui lòng chọn 1 món trong bảng trước để tách."); return; }

            List<Ban> dsTrong = new ArrayList<>();
            for (Ban b : banDAO.layTatCa()) {
                if ("Trong".equalsIgnoreCase(b.getTrangThai()) && b.getMaBan() != ban.getMaBan()) dsTrong.add(b);
            }
            if (dsTrong.isEmpty()) { canhBao("Không có bàn trống nào để tách món sang."); return; }

            TextInputDialog nhapSoLuong = new TextInputDialog(String.valueOf(chon.soLuong));
            nhapSoLuong.setTitle("Tách món");
            nhapSoLuong.setHeaderText("Tách bao nhiêu '" + chon.tenSP + "' (tối đa " + chon.soLuong + ")?");
            nhapSoLuong.setContentText("Số lượng:");
            UI.style(nhapSoLuong); Optional<String> slKq = nhapSoLuong.showAndWait();
            if (!slKq.isPresent()) return;

            int soLuongTach;
            try { soLuongTach = Integer.parseInt(slKq.get().trim()); }
            catch (Exception ex) { canhBao("Số lượng không hợp lệ."); return; }
            if (soLuongTach <= 0 || soLuongTach > chon.soLuong) { canhBao("Số lượng không hợp lệ."); return; }

            ChoiceDialog<String> chonBan = new ChoiceDialog<>(null, moTaDsBan(dsTrong));
            chonBan.setTitle("Tách món");
            chonBan.setHeaderText("Tách sang bàn nào?");
            UI.style(chonBan); Optional<String> banKq = chonBan.showAndWait();
            if (banKq.isPresent()) {
                int banDich = layMaBanTuMoTa(banKq.get(), dsTrong);
                boolean ok = dhDAO.tachMonSangBanKhac(maDH[0], chon.maSP, soLuongTach, banDich, tenDangNhap);
                if (ok) { thongBao("Đã tách món sang bàn mới!"); taiLai[0].run(); }
                else canhBao("Tách món thất bại.");
            }
        });

        HBox hangChuyenGopTach = new HBox(10, btnChuyenBan, btnGopBan, btnTachMon);

        HBox hangChon = new HBox(10, cboSanPham, spnSoLuong, txtGhiChu, btnThem);
        hangChon.setAlignment(Pos.CENTER_LEFT);

        // Double-click vao cot Ghi chu de sua ghi chu cua mon do
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() != 2) return;
            javafx.scene.control.TablePosition<?, ?> pos =
                    (javafx.scene.control.TablePosition<?, ?>) table.getSelectionModel().getSelectedCells().stream()
                    .filter(pc -> pc.getColumn() >= 0 && table.getColumns().get(pc.getColumn()) == colGhiChu)
                    .findFirst().orElse(null);
            DongMon chon = table.getSelectionModel().getSelectedItem();
            if (chon == null || maDH[0] == -1 || pos == null) return;

            TextInputDialog nhap = new TextInputDialog(chon.getGhiChu() == null ? "" : chon.getGhiChu());
            nhap.setTitle("Sửa ghi chú");
            nhap.setHeaderText("Ghi chú cho món: " + chon.getTenSP());
            nhap.setContentText("Ghi chú:");
            UI.style(nhap);
            nhap.showAndWait().ifPresent(v -> {
                dhDAO.capNhatGhiChu(maDH[0], chon.maSP, v);
                taiLai[0].run();
            });
        });

        HBox hangNutBep = new HBox(10, btnThongBaoBep, btnXoaDong);

        Button btnSuaSoNguoi = new Button("Sửa số người");
        btnSuaSoNguoi.getStyleClass().add("btn-outline");
        btnSuaSoNguoi.setOnAction(e -> {
            TextInputDialog nhapSoNguoi = new TextInputDialog(String.valueOf(Math.max(1, ban.getSoNguoi())));
            nhapSoNguoi.setTitle(maDH[0] == -1 ? "Mở bàn" : "Cập nhật số người");
            nhapSoNguoi.setHeaderText((maDH[0] == -1 ? "Mở bàn " : "Cập nhật số người tại Bàn ") + ban.getSoHieuBan());
            nhapSoNguoi.setContentText("Số người (1 - " + ban.getSucChua() + "):");
            UI.style(nhapSoNguoi);
            nhapSoNguoi.showAndWait().ifPresent(giaTri -> {
                try {
                    int soNguoiMoi = Integer.parseInt(giaTri.trim());
                    if (soNguoiMoi < 1 || soNguoiMoi > ban.getSucChua()) {
                        canhBao("Số người phải từ 1 đến " + ban.getSucChua() + ".");
                        return;
                    }
                    boolean capNhatThanhCong;
                    if (maDH[0] == -1) {
                        maDH[0] = dhDAO.taoDonHangMoi(ban.getMaBan(), tenDangNhap, soNguoiMoi);
                        capNhatThanhCong = maDH[0] != -1;
                    } else {
                        capNhatThanhCong = banDAO.capNhatSoNguoi(ban.getMaBan(), soNguoiMoi);
                    }
                    if (capNhatThanhCong) {
                        taiLai[0].run();
                        if (onDongPopup != null) onDongPopup.run();
                        thongBao(maDH[0] != -1 ? "Đã cập nhật số người." : "Đã mở bàn.");
                    } else {
                        canhBao("Không thể mở/cập nhật bàn. Vui lòng kiểm tra tài khoản nhân viên và CSDL.");
                    }
                } catch (NumberFormatException ex) {
                    canhBao("Số người phải là số hợp lệ.");
                }
            });
        });

        HBox hangCuoi = new HBox(10, btnThanhToan, btnDong);
        HBox.setHgrow(btnThanhToan, javafx.scene.layout.Priority.ALWAYS);

        VBox root = new VBox(14, lblTitle, hangChon, table, hangNutBep, hangChuyenGopTach, btnSuaSoNguoi, lblTongTien, lblYeuCau, hangCuoi);
        root.getStyleClass().add("content-card");
        root.setPadding(new Insets(24));

        VBox wrapper = new VBox(root);
        wrapper.setPadding(new Insets(20));
        wrapper.getStyleClass().add("root-bg-light");

        Scene scene = new Scene(wrapper, 620, 580);
        scene.getStylesheets().add(ChiTietDonHangDialog.class.getResource("style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private static void canhBao(String noiDung) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(noiDung);
        UI.style(alert);
        alert.showAndWait();
    }

    private static void thongBao(String noiDung) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(noiDung);
        UI.style(alert);
        alert.showAndWait();
    }

    // Tao danh sach mo ta "Ban X - KhuVuc" tu danh sach Ban, dung cho ChoiceDialog
    private static List<String> moTaDsBan(List<Ban> dsBan) {
        List<String> ds = new ArrayList<>();
        for (Ban b : dsBan) ds.add("Bàn " + b.getSoHieuBan() + " - " + b.getTenKhuVuc());
        return ds;
    }

    // Tim lai MaBan tu chuoi mo ta da chon trong ChoiceDialog
    private static int layMaBanTuMoTa(String moTa, List<Ban> dsBan) {
        for (Ban b : dsBan) {
            if (("Bàn " + b.getSoHieuBan() + " - " + b.getTenKhuVuc()).equals(moTa)) return b.getMaBan();
        }
        return -1;
    }
}
