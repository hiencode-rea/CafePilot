package com.example.nlcsn2;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

// Man hinh gop: vua them mon vua xem chi tiet cho 1 ban - dung chung cho ban Trong lan Dang phuc vu.
// Co the dong/mo lai bat cu luc nao, du lieu luon lay tu server (da luu ngay xuong CSDL).
public class ChiTietDonHangActivity extends AppCompatActivity {

    private int maBan;
    private String soHieuBan;
    private String tenDangNhap;
    private int sucChua;
    private Integer maDH = null; // null nghia la ban chua co don dang mo
    private boolean daYeuCau = false; // da gui yeu cau thanh toan sang Desktop chua
    private Button btnThanhToan;
    private TextView lblYeuCau;

    private Spinner spSanPham;
    private final List<JSONObject> danhSachSanPham = new ArrayList<>();

    private ListView dsMon;
    private final List<JSONObject> danhSachMon = new ArrayList<>();
    private ArrayAdapter<String> adapterMon;

    private TextView lblTongTien;
    private TextView lblThongTinDon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chi_tiet_don_hang);

        maBan = getIntent().getIntExtra("maBan", -1);
        soHieuBan = getIntent().getStringExtra("soHieuBan");
        sucChua = getIntent().getIntExtra("sucChua", 1);

        SharedPreferences sp = getSharedPreferences("PhienDangNhap", MODE_PRIVATE);
        tenDangNhap = sp.getString("tenDangNhap", "");

        TextView lblTieuDe = findViewById(R.id.lblTieuDe);
        lblTieuDe.setText("Bàn " + soHieuBan);

        spSanPham = findViewById(R.id.spSanPham);
        dsMon = findViewById(R.id.dsMon);
        lblTongTien = findViewById(R.id.lblTongTien);
        lblThongTinDon = findViewById(R.id.lblThongTinDon);
        Button btnSuaSoNguoi = findViewById(R.id.btnSuaSoNguoi);
        EditText edtSoLuong = findViewById(R.id.edtSoLuong);
        Button btnThem = findViewById(R.id.btnThem);
        Button btnThongBaoBep = findViewById(R.id.btnThongBaoBep);
        btnThanhToan = findViewById(R.id.btnThanhToan);
        lblYeuCau = findViewById(R.id.lblYeuCau);

        btnSuaSoNguoi.setOnClickListener(v -> hienThiSuaSoNguoi());

        adapterMon = new ArrayAdapter<>(this, R.layout.item_text, R.id.lblNoiDung, new ArrayList<>());
        dsMon.setAdapter(adapterMon);

        taiDanhSachSanPham();
        taiChiTiet();

        btnThem.setOnClickListener(v -> {
            int viTri = spSanPham.getSelectedItemPosition();
            if (viTri < 0 || viTri >= danhSachSanPham.size()) {
                Toast.makeText(this, "Vui lòng chọn sản phẩm.", Toast.LENGTH_SHORT).show();
                return;
            }
            int soLuong;
            try {
                soLuong = Integer.parseInt(edtSoLuong.getText().toString().trim());
                if (soLuong <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Số lượng không hợp lệ.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int maSP = danhSachSanPham.get(viTri).getInt("maSP");
                themMon(maSP, soLuong);
            } catch (Exception e) {
                Toast.makeText(this, "Lỗi thêm món.", Toast.LENGTH_SHORT).show();
            }
        });

        // Bam giu 1 dong de xoa - chi cho phep neu CHUA bao bep
        dsMon.setOnItemLongClickListener((parent, view, viTri, id) -> {
            try {
                JSONObject mon = danhSachMon.get(viTri);
                if ("HET_MON".equals(mon.optString("trangThaiMon", ""))) {
                    hienThiDoiMon(mon);
                    return true;
                }
                if (mon.getBoolean("daThongBaoBep")) {
                    Toast.makeText(this, "Món này đã báo bếp, không thể xóa.", Toast.LENGTH_SHORT).show();
                    return true;
                }
                new AlertDialog.Builder(this)
                        .setTitle("Xóa món")
                        .setMessage("Xóa '" + mon.getString("tenSP") + "' khỏi đơn?")
                        .setPositiveButton("Xóa", (dlg, which) -> xoaMon(mon))
                        .setNegativeButton("Hủy", null)
                        .show();
            } catch (Exception ignored) {}
            return true;
        });

        btnThongBaoBep.setOnClickListener(v -> thongBaoBep());
        btnThanhToan.setOnClickListener(v -> { if (daYeuCau) huyYeuCau(); else guiYeuCau(); });
    }

    private void taiDanhSachSanPham() {
        new Thread(() -> {
            try {
                String phanHoi = ApiClient.get("/api/sanpham");
                JSONArray mang = new JSONArray(phanHoi);

                List<JSONObject> ds = new ArrayList<>();
                List<String> tenHienThi = new ArrayList<>();
                for (int i = 0; i < mang.length(); i++) {
                    JSONObject spct = mang.getJSONObject(i);
                    ds.add(spct);
                    tenHienThi.add(spct.getString("tenSP") + " - " + spct.getDouble("giaBan") + "d");
                }

                runOnUiThread(() -> {
                    danhSachSanPham.clear();
                    danhSachSanPham.addAll(ds);
                    ArrayAdapter<String> adapterSp = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_dropdown_item, tenHienThi);
                    spSanPham.setAdapter(adapterSp);
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi tải sản phẩm: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void taiChiTiet() {
        new Thread(() -> {
            try {
                String phanHoi = ApiClient.get("/api/chitietdon?maBan=" + maBan);
                JSONObject ketQua = new JSONObject(phanHoi);

                Integer maDHmoi = ketQua.isNull("maDH") ? null : ketQua.getInt("maDH");
                double tongTien = ketQua.optDouble("tongTien", 0);
                int soNguoiMoi = ketQua.optInt("soNguoi", 0);
                String thoiGianTao = ketQua.optString("thoiGianTao", "");
                boolean yeuCauMoi = ketQua.optBoolean("yeuCauThanhToan", false);
                JSONArray mangMon = ketQua.getJSONArray("danhSachMon");

                List<JSONObject> ds = new ArrayList<>();
                for (int i = 0; i < mangMon.length(); i++) ds.add(mangMon.getJSONObject(i));

                runOnUiThread(() -> {
                    maDH = maDHmoi;
                    capNhatThongTinDon(soNguoiMoi, thoiGianTao);
                    daYeuCau = yeuCauMoi;
                    danhSachMon.clear();
                    danhSachMon.addAll(ds);
                    capNhatGiaoDien(tongTien);
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi tải chi tiết: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void capNhatGiaoDien(double tongTien) {
        List<String> dongHienThi = new ArrayList<>();
        try {
            for (JSONObject mon : danhSachMon) {
                double thanhTien = mon.getInt("soLuong") * mon.getDouble("donGia");
                String danhDau = mon.getBoolean("daThongBaoBep")
                        ? "  • " + tenTrangThai(mon.optString("trangThaiMon", "CHO_LAM"))
                        : "  • Chưa gửi";
                dongHienThi.add(mon.getString("tenSP") + "  x" + mon.getInt("soLuong") + "  =  " + thanhTien + "d" + danhDau);
            }
        } catch (Exception ignored) {}

        adapterMon.clear();
        adapterMon.addAll(dongHienThi);
        adapterMon.notifyDataSetChanged();
        lblTongTien.setText("Tổng tiền: " + tongTien + " VND");

        // Nút thanh toán trên Mobile chỉ GỬI YÊU CẦU; việc thanh toán do thu ngân xác nhận trên Desktop
        if (daYeuCau) {
            btnThanhToan.setText("Hủy yêu cầu thanh toán");
            btnThanhToan.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.status_warning)));
            lblYeuCau.setVisibility(android.view.View.VISIBLE);
        } else {
            btnThanhToan.setText("Gửi yêu cầu thanh toán");
            btnThanhToan.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.status_success)));
            lblYeuCau.setVisibility(android.view.View.GONE);
        }
    }

    private String tenTrangThai(String state) {
        if ("CHO_LAM".equals(state)) return "Chờ làm";
        if ("DANG_LAM".equals(state)) return "Đang làm";
        if ("DA_RA_MON".equals(state)) return "Đã ra món";
        if ("HET_MON".equals(state)) return "Hết món";
        return state;
    }

    private void capNhatThongTinDon(int soNguoi, String thoiGianTao) {
        if (maDH == null) {
            lblThongTinDon.setText("Chưa có đơn mở • Sức chứa: " + sucChua);
            return;
        }
        lblThongTinDon.setText("Người: " + soNguoi + "/" + sucChua + " • Đã order: " + dinhDangThoiGian(thoiGianTao));
    }

    private String dinhDangThoiGian(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty() || "null".equalsIgnoreCase(giaTri)) return "-";
        try {
            java.time.LocalDateTime tao = java.time.LocalDateTime.parse(giaTri,
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            long phut = Math.max(0, java.time.Duration.between(tao, java.time.LocalDateTime.now()).toMinutes());
            long gio = phut / 60;
            return gio > 0 ? gio + " giờ " + (phut % 60) + " phút" : phut + " phút";
        } catch (java.time.format.DateTimeParseException e) {
            return "-";
        }
    }

    private void hienThiSuaSoNguoi() {
        EditText nhap = new EditText(this);
        nhap.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        nhap.setHint("Số người");
        nhap.setText("1");
        nhap.setSelectAllOnFocus(true);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        nhap.setPadding(padding, padding / 2, padding, padding / 2);

        new AlertDialog.Builder(this)
                .setTitle(maDH == null ? "Mở bàn" : "Sửa số người")
                .setMessage("Số người (1 - " + sucChua + ")")
                .setView(nhap)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    try {
                        int soNguoi = Integer.parseInt(nhap.getText().toString().trim());
                        if (soNguoi < 1 || soNguoi > sucChua) {
                            Toast.makeText(this, "Số người phải từ 1 đến " + sucChua + ".", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        capNhatSoNguoi(soNguoi);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số người không hợp lệ.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void capNhatSoNguoi(int soNguoi) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("maBan", maBan);
                body.put("soNguoi", soNguoi);
                body.put("tenDangNhap", tenDangNhap);
                JSONObject ketQua = new JSONObject(ApiClient.post("/api/chitietdon/capnhatsoinguoi", body.toString()));
                boolean thanhCong = ketQua.optBoolean("thanhCong", false);
                String thongBao = ketQua.optString("thongBao", "");
                runOnUiThread(() -> {
                    Toast.makeText(this, thongBao, Toast.LENGTH_SHORT).show();
                    if (thanhCong) taiChiTiet();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi cập nhật số người: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void themMon(int maSP, int soLuong) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("maBan", maBan);
                body.put("tenDangNhap", tenDangNhap);
                body.put("maSP", maSP);
                body.put("soLuong", soLuong);
                body.put("soNguoi", 1);

                String phanHoi = ApiClient.post("/api/chitietdon/them", body.toString());
                JSONObject ketQua = new JSONObject(phanHoi);
                boolean thanhCong = ketQua.optBoolean("thanhCong", true);
                String thongBao = ketQua.optString("thongBao", "");
                runOnUiThread(() -> {
                    if (!thanhCong) Toast.makeText(this, thongBao, Toast.LENGTH_LONG).show();
                    taiChiTiet();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (e.getMessage() != null && e.getMessage().contains("Failed to connect")) {
                        Toast.makeText(this, "Mất kết nối mạng. Không thể thêm món.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Lỗi thêm món: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private void hienThiDoiMon(JSONObject monHetMon) {
        if (danhSachSanPham.isEmpty()) {
            Toast.makeText(this, "Chưa tải được danh sách món thay thế.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<JSONObject> sanPhamThayThe = new ArrayList<>();
        List<String> tenMon = new ArrayList<>();
        int maSPHetMon = monHetMon.optInt("maSP", -1);
        for (JSONObject sanPham : danhSachSanPham) {
            int maSP = sanPham.optInt("maSP", -1);
            if (maSP == maSPHetMon || daCoMonDaGuiHoacHet(maSP)) continue;
            sanPhamThayThe.add(sanPham);
            tenMon.add(sanPham.optString("tenSP", "Món không tên") + " - "
                    + sanPham.optDouble("giaBan", 0) + "d");
        }
        if (sanPhamThayThe.isEmpty()) {
            Toast.makeText(this, "Không còn món khác để thay thế.", Toast.LENGTH_SHORT).show();
            return;
        }

        Spinner chonMon = new Spinner(this);
        chonMon.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, tenMon));

        EditText soLuong = new EditText(this);
        soLuong.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        soLuong.setHint("Số lượng món thay thế");
        soLuong.setText(String.valueOf(Math.max(1, monHetMon.optInt("soLuong", 1))));
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        soLuong.setPadding(padding, padding / 2, padding, padding / 2);

        LinearLayout noiDung = new LinearLayout(this);
        noiDung.setOrientation(LinearLayout.VERTICAL);
        noiDung.setPadding(padding, 0, padding, 0);
        noiDung.addView(chonMon);
        noiDung.addView(soLuong);

        new AlertDialog.Builder(this)
                .setTitle("Đổi món hết hàng")
                .setMessage("Món '" + monHetMon.optString("tenSP", "")
                        + "' đã hết. Chọn món thay thế để gửi bếp duyệt.")
                .setView(noiDung)
                .setPositiveButton("Thêm món thay thế", (dialog, which) -> {
                    try {
                        int soLuongMoi = Integer.parseInt(soLuong.getText().toString().trim());
                        if (soLuongMoi <= 0) throw new NumberFormatException();
                        int viTri = chonMon.getSelectedItemPosition();
                        int maSPMoi = sanPhamThayThe.get(viTri).getInt("maSP");
                        themMon(maSPMoi, soLuongMoi);
                        Toast.makeText(this,
                                "Đã thêm món thay thế. Hãy bấm Thông báo bếp để gửi duyệt.",
                                Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Số lượng món thay thế không hợp lệ.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private boolean daCoMonDaGuiHoacHet(int maSP) {
        for (JSONObject mon : danhSachMon) {
            if (mon.optInt("maSP", -1) != maSP) continue;
            if (mon.optBoolean("daThongBaoBep", false)
                    || "HET_MON".equals(mon.optString("trangThaiMon", ""))) return true;
        }
        return false;
    }

    private void thongBaoBep() {
        if (maDH == null || danhSachMon.isEmpty()) {
            Toast.makeText(this, "Chưa có món nào để thông báo.", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("maDH", maDH);
                String phanHoi = ApiClient.post("/api/chitietdon/thongbaobep", body.toString());
                JSONObject ketQua = new JSONObject(phanHoi);
                boolean thanhCong = ketQua.optBoolean("thanhCong", false);
                String thongBao = ketQua.optString("thongBao", "Không có phản hồi từ máy chủ.");
                runOnUiThread(() -> {
                    Toast.makeText(this, thongBao, Toast.LENGTH_SHORT).show();
                    if (thanhCong) taiChiTiet();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void xoaMon(JSONObject mon) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("maDH", maDH);
                body.put("maSP", mon.getInt("maSP"));

                String phanHoi = ApiClient.post("/api/chitietdon/xoamon", body.toString());
                JSONObject ketQua = new JSONObject(phanHoi);
                String thongBao = ketQua.optString("thongBao", "");

                runOnUiThread(() -> {
                    Toast.makeText(this, thongBao, Toast.LENGTH_SHORT).show();
                    taiChiTiet();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi xóa món: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void guiYeuCau() {
        if (maDH == null || danhSachMon.isEmpty()) {
            Toast.makeText(this, "Chưa có món nào để yêu cầu thanh toán.", Toast.LENGTH_SHORT).show();
            return;
        }
        guiLenhYeuCau("/api/chitietdon/yeucauthanhtoan");
    }

    private void huyYeuCau() {
        guiLenhYeuCau("/api/chitietdon/huyyeucau");
    }

    private void guiLenhYeuCau(String duong) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("maBan", maBan);

                String phanHoi = ApiClient.post(duong, body.toString());
                JSONObject ketQua = new JSONObject(phanHoi);
                String thongBao = ketQua.optString("thongBao", "");

                runOnUiThread(() -> {
                    Toast.makeText(this, thongBao, Toast.LENGTH_LONG).show();
                    taiChiTiet();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
