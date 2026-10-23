import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

public class Luong {
    private final SimpleStringProperty tenNV;
    private final SimpleStringProperty loaiHopDong;
    private final SimpleDoubleProperty tongGioLam;
    private final SimpleDoubleProperty phuCap;
    private final SimpleDoubleProperty thuong;
    private final SimpleDoubleProperty phat;
    private final SimpleDoubleProperty thucLinh;

    public Luong(String tenNV, String loaiHopDong, double tongGioLam, double phuCap,
                 double thuong, double phat, double thucLinh) {
        this.tenNV = new SimpleStringProperty(tenNV);
        this.loaiHopDong = new SimpleStringProperty(loaiHopDong);
        this.tongGioLam = new SimpleDoubleProperty(tongGioLam);
        this.phuCap = new SimpleDoubleProperty(phuCap);
        this.thuong = new SimpleDoubleProperty(thuong);
        this.phat = new SimpleDoubleProperty(phat);
        this.thucLinh = new SimpleDoubleProperty(thucLinh);
    }

    public String getTenNV() { return tenNV.get(); }
    public String getLoaiHopDong() { return loaiHopDong.get(); }
    public double getTongGioLam() { return tongGioLam.get(); }
    public double getPhuCap() { return phuCap.get(); }
    public double getThuong() { return thuong.get(); }
    public double getPhat() { return phat.get(); }
    public double getThucLinh() { return thucLinh.get(); }

    public SimpleStringProperty tenNVProperty() { return tenNV; }
    public SimpleStringProperty loaiHopDongProperty() { return loaiHopDong; }
    public SimpleDoubleProperty tongGioLamProperty() { return tongGioLam; }
    public SimpleDoubleProperty phuCapProperty() { return phuCap; }
    public SimpleDoubleProperty thuongProperty() { return thuong; }
    public SimpleDoubleProperty phatProperty() { return phat; }
    public SimpleDoubleProperty thucLinhProperty() { return thucLinh; }
}
