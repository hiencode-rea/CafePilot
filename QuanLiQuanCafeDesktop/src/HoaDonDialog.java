import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HoaDonDialog {

    public static void moPopup(Ban ban, int maDH, DonHangDAO dhDAO) {
        List<Object[]> chiTiet = dhDAO.layChiTietDonHang(maDH);
        String ngayIn = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Hóa đơn - Bàn " + ban.getSoHieuBan());

        Label lblCuaHang = new Label("CỬA HÀNG CÀ PHÊ");
        lblCuaHang.getStyleClass().add("section-title");

        Label lblHoaDon = new Label("HÓA ĐƠN THANH TOÁN");
        lblHoaDon.getStyleClass().add("card-title");

        Label lblMaDH = new Label("Mã đơn hàng: " + maDH);
        Label lblBan = new Label("Bàn: " + ban.getSoHieuBan() + " - " + ban.getTenKhuVuc());
        Label lblNgay = new Label("Ngày in: " + ngayIn);
        for (Label l : new Label[]{lblMaDH, lblBan, lblNgay}) l.getStyleClass().add("card-subtitle");

        VBox danhSachDong = new VBox(6);
        danhSachDong.setPadding(new Insets(10, 0, 10, 0));

        HBox tieuDeDong = taoDong("Sản phẩm", "SL", "Đơn giá", "Thành tiền", true);
        danhSachDong.getChildren().add(tieuDeDong);
        danhSachDong.getChildren().add(new Separator());

        double tongCong = 0;
        for (Object[] dong : chiTiet) {
            String tenSP = (String) dong[0];
            int soLuong = (int) dong[1];
            double donGia = (double) dong[2];
            double thanhTien = (double) dong[3];
            tongCong += thanhTien;

            danhSachDong.getChildren().add(taoDong(tenSP, String.valueOf(soLuong),
                    String.valueOf(donGia), String.valueOf(thanhTien), false));
        }
        final double tongCongCuoi = tongCong;

        Separator gachCuoi = new Separator();
        Label lblTongCong = new Label("TỔNG CỘNG: " + tongCong + " VND");
        lblTongCong.getStyleClass().add("section-title");

        Label lblCamOn = new Label("Cảm ơn quý khách - Hẹn gặp lại!");
        lblCamOn.getStyleClass().add("card-subtitle");

        Button btnInPdf = new Button("\uD83D\uDDA8 In hóa đơn (PDF)");
        btnInPdf.getStyleClass().add("btn-success");
        btnInPdf.setMaxWidth(Double.MAX_VALUE);
        btnInPdf.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Lưu hóa đơn PDF");
            fc.setInitialFileName("HoaDon_" + maDH + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
            File file = fc.showSaveDialog(dialog);
            if (file != null) {
                boolean ok = xuatPdf(file, ban, maDH, ngayIn, chiTiet, tongCongCuoi);
                if (ok) {
                    thongBao("Đã xuất hóa đơn PDF thành công!\n" + file.getAbsolutePath());
                } else {
                    thongBao("Xuất PDF thất bại. Kiểm tra lại thư viện pdfbox-app.jar trong lib.");
                }
            }
        });

        Button btnDong = new Button("Đóng");
        btnDong.getStyleClass().add("btn-update");
        btnDong.setMaxWidth(Double.MAX_VALUE);
        btnDong.setOnAction(e -> dialog.close());

        VBox noiDung = new VBox(8, lblCuaHang, lblHoaDon, lblMaDH, lblBan, lblNgay,
                danhSachDong, gachCuoi, lblTongCong, lblCamOn, btnInPdf, btnDong);
        noiDung.getStyleClass().add("content-card");
        noiDung.setPadding(new Insets(26));
        noiDung.setAlignment(Pos.CENTER);

        VBox wrapper = new VBox(noiDung);
        wrapper.setPadding(new Insets(20));
        wrapper.getStyleClass().add("root-bg-light");

        Scene scene = new Scene(wrapper, 420, 560);
        scene.getStylesheets().add(HoaDonDialog.class.getResource("style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // Xuat noi dung hoa don ra file PDF that su, dung thu vien Apache PDFBox
    private static boolean xuatPdf(File file, Ban ban, int maDH, String ngayIn,
                                    List<Object[]> chiTiet, double tongCong) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A5);
            doc.addPage(page);

            // Ưu tiên font Arial của Windows (hỗ trợ tiếng Việt có dấu). Không có thì dùng font chuẩn và bỏ dấu.
            PDFont fontThuong;
            PDFont fontDam;
            coUnicode = false;
            try {
                String thuMucFont = (System.getenv("WINDIR") != null ? System.getenv("WINDIR") : "C:\\Windows") + "\\Fonts\\";
                File ttfThuong = new File(thuMucFont + "arial.ttf");
                File ttfDam = new File(thuMucFont + "arialbd.ttf");
                if (!ttfThuong.exists() || !ttfDam.exists()) throw new IOException("Không tìm thấy font Arial");
                fontThuong = PDType0Font.load(doc, ttfThuong);
                fontDam = PDType0Font.load(doc, ttfDam);
                coUnicode = true;
            } catch (Exception ex) {
                fontThuong = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                fontDam = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            }

            float le = 40;
            float y = page.getMediaBox().getHeight() - 50;
            float rongTrang = page.getMediaBox().getWidth() - 2 * le;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                cs.beginText();
                cs.setFont(fontDam, 16);
                cs.newLineAtOffset(le, y);
                cs.showText(vt("CỬA HÀNG CÀ PHÊ"));
                cs.endText();
                y -= 22;

                cs.beginText();
                cs.setFont(fontDam, 13);
                cs.newLineAtOffset(le, y);
                cs.showText(vt("HÓA ĐƠN THANH TOÁN"));
                cs.endText();
                y -= 22;

                cs.setFont(fontThuong, 10.5f);
                String[] thongTin = {
                        "Mã đơn hàng: " + maDH,
                        "Bàn: " + ban.getSoHieuBan() + " - " + ban.getTenKhuVuc(),
                        "Ngày in: " + ngayIn
                };
                for (String dong : thongTin) {
                    cs.beginText();
                    cs.newLineAtOffset(le, y);
                    cs.showText(vt(dong));
                    cs.endText();
                    y -= 16;
                }
                y -= 6;

                // Duong ke tren bang
                cs.moveTo(le, y);
                cs.lineTo(le + rongTrang, y);
                cs.stroke();
                y -= 16;

                // Header bang: San pham | SL | Don gia | Thanh tien
                float xTen = le, xSL = le + 190, xGia = le + 240, xThanh = le + 330;
                cs.setFont(fontDam, 10);
                cs.beginText(); cs.newLineAtOffset(xTen, y); cs.showText(vt("Sản phẩm")); cs.endText();
                cs.beginText(); cs.newLineAtOffset(xSL, y); cs.showText(vt("SL")); cs.endText();
                cs.beginText(); cs.newLineAtOffset(xGia, y); cs.showText(vt("Đơn giá")); cs.endText();
                cs.beginText(); cs.newLineAtOffset(xThanh, y); cs.showText(vt("Thành tiền")); cs.endText();
                y -= 10;

                cs.moveTo(le, y);
                cs.lineTo(le + rongTrang, y);
                cs.stroke();
                y -= 16;

                cs.setFont(fontThuong, 10);
                for (Object[] dong : chiTiet) {
                    String tenSP = (String) dong[0];
                    int soLuong = (int) dong[1];
                    double donGia = (double) dong[2];
                    double thanhTien = (double) dong[3];

                    cs.beginText(); cs.newLineAtOffset(xTen, y); cs.showText(vt(cat(tenSP, 24))); cs.endText();
                    cs.beginText(); cs.newLineAtOffset(xSL, y); cs.showText(vt(String.valueOf(soLuong))); cs.endText();
                    cs.beginText(); cs.newLineAtOffset(xGia, y); cs.showText(vt(String.valueOf(donGia))); cs.endText();
                    cs.beginText(); cs.newLineAtOffset(xThanh, y); cs.showText(vt(String.valueOf(thanhTien))); cs.endText();
                    y -= 16;

                    if (y < 80) break; // tranh tran trang (du lieu niên luận it dong nen hiem khi xay ra)
                }

                y -= 6;
                cs.moveTo(le, y);
                cs.lineTo(le + rongTrang, y);
                cs.stroke();
                y -= 20;

                cs.beginText();
                cs.setFont(fontDam, 13);
                cs.newLineAtOffset(le, y);
                cs.showText(vt("TỔNG CỘNG: " + tongCong + " VND"));
                cs.endText();
                y -= 26;

                cs.beginText();
                cs.setFont(fontThuong, 10);
                cs.newLineAtOffset(le, y);
                cs.showText(vt("Cảm ơn quý khách - Hẹn gặp lại!"));
                cs.endText();
            }

            doc.save(file);
            return true;

        } catch (Exception e) {
            System.out.println("Loi xuat PDF: " + e.getMessage());
            return false;
        }
    }

    private static boolean coUnicode = false;

    // Nếu không có font Unicode thì bỏ dấu để font chuẩn của PDF hiển thị được
    private static String vt(String s) {
        if (coUnicode) return s;
        String t = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return t.replace('đ', 'd').replace('Đ', 'D');
    }

    // Cat bot ten san pham neu qua dai de khong tran cot trong PDF
    private static String cat(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "." : s;
    }

    private static HBox taoDong(String ten, String sl, String gia, String thanh, boolean laTieuDe) {
        Label lblTen = new Label(ten);
        Label lblSL = new Label(sl);
        Label lblGia = new Label(gia);
        Label lblThanh = new Label(thanh);

        lblTen.setPrefWidth(140);
        lblSL.setPrefWidth(40);
        lblGia.setPrefWidth(90);
        lblThanh.setPrefWidth(100);

        if (laTieuDe) {
            for (Label l : new Label[]{lblTen, lblSL, lblGia, lblThanh}) {
                l.getStyleClass().add("field-label");
            }
        } else {
            for (Label l : new Label[]{lblTen, lblSL, lblGia, lblThanh}) {
                l.setStyle("-fx-font-size: 12px;");
            }
        }

        HBox hang = new HBox(lblTen, lblSL, lblGia, lblThanh);
        hang.setAlignment(Pos.CENTER_LEFT);
        return hang;
    }

    private static void thongBao(String noiDung) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(noiDung);
        UI.style(alert);
        alert.showAndWait();
    }
}
