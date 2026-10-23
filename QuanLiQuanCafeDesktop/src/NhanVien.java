import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class NhanVien {
    private final SimpleIntegerProperty maNV;
    private final SimpleStringProperty hoTen;
    private final SimpleStringProperty soDienThoai;
    private final SimpleStringProperty loaiHopDong;
    private final SimpleStringProperty tenDangNhap;
    private final SimpleStringProperty vaiTro;

    public NhanVien(int maNV, String hoTen, String soDienThoai, String loaiHopDong,
                     String tenDangNhap, String vaiTro) {
        this.maNV = new SimpleIntegerProperty(maNV);
        this.hoTen = new SimpleStringProperty(hoTen);
        this.soDienThoai = new SimpleStringProperty(soDienThoai);
        this.loaiHopDong = new SimpleStringProperty(loaiHopDong);
        this.tenDangNhap = new SimpleStringProperty(tenDangNhap);
        this.vaiTro = new SimpleStringProperty(vaiTro);
    }

    public int getMaNV() { return maNV.get(); }
    public String getHoTen() { return hoTen.get(); }
    public String getSoDienThoai() { return soDienThoai.get(); }
    public String getLoaiHopDong() { return loaiHopDong.get(); }
    public String getTenDangNhap() { return tenDangNhap.get(); }
    public String getVaiTro() { return vaiTro.get(); }

    public SimpleIntegerProperty maNVProperty() { return maNV; }
    public SimpleStringProperty hoTenProperty() { return hoTen; }
    public SimpleStringProperty soDienThoaiProperty() { return soDienThoai; }
    public SimpleStringProperty loaiHopDongProperty() { return loaiHopDong; }
    public SimpleStringProperty tenDangNhapProperty() { return tenDangNhap; }
    public SimpleStringProperty vaiTroProperty() { return vaiTro; }
}
