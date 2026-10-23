import java.sql.Timestamp;

public class Ban {
    private int maBan;
    private String tenKhuVuc;
    private String soHieuBan;
    private String trangThai;
    private int sucChua;
    private int soNguoi;
    private double viTriX;
    private double viTriY;
    private double tongTien;
    private Timestamp thoiGianTao;
    private boolean yeuCauThanhToan = false; // Mobile đã gửi yêu cầu thanh toán

    public Ban(int maBan, String tenKhuVuc, String soHieuBan, String trangThai, int sucChua, int soNguoi) {
        this.maBan = maBan;
        this.tenKhuVuc = tenKhuVuc;
        this.soHieuBan = soHieuBan;
        this.trangThai = trangThai;
        this.sucChua = sucChua;
        this.soNguoi = soNguoi;
    }

    public Ban(int maBan, String tenKhuVuc, String soHieuBan, String trangThai, int sucChua, int soNguoi, double viTriX, double viTriY) {
        this(maBan, tenKhuVuc, soHieuBan, trangThai, sucChua, soNguoi);
        this.viTriX = viTriX;
        this.viTriY = viTriY;
    }

    public Ban(int maBan, String tenKhuVuc, String soHieuBan, String trangThai, int sucChua,
               int soNguoi, double viTriX, double viTriY, double tongTien, Timestamp thoiGianTao) {
        this(maBan, tenKhuVuc, soHieuBan, trangThai, sucChua, soNguoi, viTriX, viTriY);
        this.tongTien = tongTien;
        this.thoiGianTao = thoiGianTao;
    }

    public int getMaBan() { return maBan; }
    public String getTenKhuVuc() { return tenKhuVuc; }
    public String getSoHieuBan() { return soHieuBan; }
    public String getTrangThai() { return trangThai; }
    public int getSucChua() { return sucChua; }
    public int getSoNguoi() { return soNguoi; }
    public double getViTriX() { return viTriX; }
    public double getViTriY() { return viTriY; }
    public double getTongTien() { return tongTien; }
    public Timestamp getThoiGianTao() { return thoiGianTao; }
    public boolean isYeuCauThanhToan() { return yeuCauThanhToan; }
    public void setYeuCauThanhToan(boolean v) { this.yeuCauThanhToan = v; }
}
