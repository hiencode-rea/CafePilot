import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class CaLamViec {
    private final SimpleIntegerProperty maCa;
    private final SimpleIntegerProperty maNV;
    private final SimpleStringProperty tenNV;
    private final SimpleStringProperty ngay;
    private final SimpleStringProperty gioBatDau;
    private final SimpleStringProperty gioKetThuc;

    public CaLamViec(int maCa, int maNV, String tenNV, String ngay, String gioBatDau, String gioKetThuc) {
        this.maCa = new SimpleIntegerProperty(maCa);
        this.maNV = new SimpleIntegerProperty(maNV);
        this.tenNV = new SimpleStringProperty(tenNV);
        this.ngay = new SimpleStringProperty(ngay);
        this.gioBatDau = new SimpleStringProperty(gioBatDau);
        this.gioKetThuc = new SimpleStringProperty(gioKetThuc);
    }

    public int getMaCa() { return maCa.get(); }
    public int getMaNV() { return maNV.get(); }
    public String getTenNV() { return tenNV.get(); }
    public String getNgay() { return ngay.get(); }
    public String getGioBatDau() { return gioBatDau.get(); }
    public String getGioKetThuc() { return gioKetThuc.get(); }

    public SimpleIntegerProperty maCaProperty() { return maCa; }
    public SimpleStringProperty tenNVProperty() { return tenNV; }
    public SimpleStringProperty ngayProperty() { return ngay; }
    public SimpleStringProperty gioBatDauProperty() { return gioBatDau; }
    public SimpleStringProperty gioKetThucProperty() { return gioKetThuc; }
}
