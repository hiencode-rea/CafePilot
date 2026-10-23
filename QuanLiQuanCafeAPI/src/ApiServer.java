import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ApiServer {

    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/dangnhap", new DangNhapHandler());
        server.createContext("/api/sanpham", new SanPhamHandler());
        server.createContext("/api/ban", new BanHandler());
        server.createContext("/api/donhang", new DonHangHandler());
        server.createContext("/api/chitietdon/capnhatsoinguoi", new ChiTietDonHangHandler());
        server.createContext("/api/calamviec", new CaLamViecHandler());
        server.createContext("/api/chamcongchuara", new ChamCongChuaRaHandler());
        server.createContext("/api/chamcong/vao", new ChamCongHandler());
        server.createContext("/api/chamcong/ra", new ChamCongHandler());
        server.createContext("/api/chitietdon", new ChiTietDonHangHandler());
        server.createContext("/api/chitietdon/them", new ChiTietDonHangHandler());
        server.createContext("/api/chitietdon/thongbaobep", new ChiTietDonHangHandler());
        server.createContext("/api/chitietdon/xoamon", new ChiTietDonHangHandler());
        server.createContext("/api/chitietdon/thanhtoan", new ChiTietDonHangHandler());
        server.createContext("/api/xulymon", new XuLyMonHandler());

        server.setExecutor(null); // dung executor mac dinh
        server.start();

        System.out.println("=================================================");
        System.out.println(" API dang chay tai: http://localhost:" + port);
        System.out.println(" Cac endpoint:");
        System.out.println("   POST /api/dangnhap");
        System.out.println("   GET  /api/sanpham");
        System.out.println("   GET  /api/ban");
        System.out.println("   POST /api/donhang");
        System.out.println("   GET  /api/calamviec?tenDangNhap=...");
        System.out.println("   GET  /api/chamcongchuara?tenDangNhap=...");
        System.out.println("   POST /api/chamcong/vao");
        System.out.println("   POST /api/chamcong/ra");
        System.out.println("   GET  /api/xulymon?khuVuc=BEP|PHA_CHE");
        System.out.println("   POST /api/xulymon/trangthai");
        System.out.println("=================================================");
    }
}
