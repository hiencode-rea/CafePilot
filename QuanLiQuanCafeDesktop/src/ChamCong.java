import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class ChamCong {
    private final SimpleIntegerProperty maCC;
    private final SimpleStringProperty thongTinCa;
    private final SimpleStringProperty gioVao;
    private final SimpleStringProperty gioRa;

    public ChamCong(int maCC, String thongTinCa, String gioVao, String gioRa) {
        this.maCC = new SimpleIntegerProperty(maCC);
        this.thongTinCa = new SimpleStringProperty(thongTinCa);
        this.gioVao = new SimpleStringProperty(gioVao);
        this.gioRa = new SimpleStringProperty(gioRa);
    }

    public int getMaCC() { return maCC.get(); }
    public String getThongTinCa() { return thongTinCa.get(); }
    public String getGioVao() { return gioVao.get(); }
    public String getGioRa() { return gioRa.get(); }

    public SimpleIntegerProperty maCCProperty() { return maCC; }
    public SimpleStringProperty thongTinCaProperty() { return thongTinCa; }
    public SimpleStringProperty gioVaoProperty() { return gioVao; }
    public SimpleStringProperty gioRaProperty() { return gioRa; }
}
