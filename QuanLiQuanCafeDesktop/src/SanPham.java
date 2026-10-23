import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class SanPham {
    private final SimpleIntegerProperty maSP;
    private final SimpleStringProperty tenSP;
    private final SimpleStringProperty nhomSP;
    private final SimpleDoubleProperty giaBan;
    private final SimpleStringProperty size;
    private final SimpleStringProperty moTa;

    public SanPham(int maSP, String tenSP, String nhomSP, double giaBan, String size, String moTa) {
        this.maSP = new SimpleIntegerProperty(maSP);
        this.tenSP = new SimpleStringProperty(tenSP);
        this.nhomSP = new SimpleStringProperty(nhomSP);
        this.giaBan = new SimpleDoubleProperty(giaBan);
        this.size = new SimpleStringProperty(size != null ? size : "");
        this.moTa = new SimpleStringProperty(moTa != null ? moTa : "");
    }

    public int getMaSP() { return maSP.get(); }
    public String getTenSP() { return tenSP.get(); }
    public String getNhomSP() { return nhomSP.get(); }
    public double getGiaBan() { return giaBan.get(); }
    public String getSize() { return size.get(); }
    public String getMoTa() { return moTa.get(); }

    public SimpleIntegerProperty maSPProperty() { return maSP; }
    public SimpleStringProperty tenSPProperty() { return tenSP; }
    public SimpleStringProperty nhomSPProperty() { return nhomSP; }
    public SimpleDoubleProperty giaBanProperty() { return giaBan; }
    public SimpleStringProperty sizeProperty() { return size; }
    public SimpleStringProperty moTaProperty() { return moTa; }

    // Hien thi ten san pham (keo size) trong ComboBox
    @Override
    public String toString() {
        String kq = tenSP.get();
        if (size.get() != null && !size.get().isEmpty()) {
            kq += " (" + size.get() + ")";
        }
        return kq;
    }
}
