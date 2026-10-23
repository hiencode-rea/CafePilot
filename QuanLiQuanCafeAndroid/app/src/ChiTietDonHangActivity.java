package com.example.nlcsn2;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
    private Integer maDH = null; // null nghia la ban chua co don dang mo

    private Spinner spSanPham;
    private final List<JSONObject> danhSachSanPham = new ArrayList<>();

    private ListView dsMon;
    private final List<JSONObject> danhSachMon = new ArrayList<>();
    private ArrayAdapter<String> adapterMon;

    private TextView lblTongTien;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chi_tiet_don_hang);

        maBan = getIntent().getIntExtra("maBan", -1);
        soHieuBan = getIntent().getStringExtra("soHieuBan");

        SharedPreferences sp = getSharedPreferences("PhienDangNhap", MODE_PRIVATE);
        tenDangNhap = sp.getString("tenDangNhap", "");

        TextView lblTieuDe = findViewById(R.id.lblTieuDe);
        lblTieuDe.setText("Bàn " + soHieuBan);

        spSanPham = findViewById(R.id.spSanPham);
        dsMon = findViewById(R.id.dsMon);
        lblTongTien = findViewById(R.id.lblTongTien);
        EditText edtSoLuong = findViewById(R.id.edtSoLuong);
        Button btnThem = findViewById(R.id.btnThem);
        Button btnThongBaoBep = findViewById(R.id.btnThongBaoBep);
        Button btnThanhToan = findViewById(R.id.btnThanhToan);

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
        btnThanhToan.setOnClickListener(v -> thanhToan());
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
                JSONArray mangMon = ketQua.getJSONArray("danhSachMon");

                List<JSONObject> ds = new ArrayList<>();
                for (int i = 0; i < mangMon.length(); i++) ds.add(mangMon.getJSONObject(i));

                runOnUiThread(() -> {
                    maDH = maDHmoi;
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
                String danhDau = mon.getBoolean("daThongBaoBep") ? "  \u2705 Đã báo" : "";
                dongHienThi.add(mon.getString("tenSP") + "  x" + mon.getInt("soLuong") + "  =  " + thanhTien + "d" + danhDau);
            }
        } catch (Exception ignored) {}

        adapterMon.clear();
        adapterMon.addAll(dongHienThi);
        adapterMon.notifyDataSetChanged();
        lblTongTien.setText("Tổng tiền: " + tongTien + " VND");
    }

    private void themMon(int maSP, int soLuong) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("maBan", maBan);
                body.put("tenDangNhap", tenDangNhap);
                body.put("maSP", maSP);
                body.put("soLuong", soLuong);

                ApiClient.post("/api/chitietdon/them", body.toString());
                runOnUiThread(this::taiChiTiet);

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi thêm món: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
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
                ApiClient.post("/api/chitietdon/thongbaobep", body.toString());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Đã thông báo bếp!", Toast.LENGTH_SHORT).show();
                    taiChiTiet();
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

    private void thanhToan() {
        if (maDH == null || danhSachMon.isEmpty()) {
            Toast.makeText(this, "Chưa có món nào để thanh toán.", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("maBan", maBan);

                String phanHoi = ApiClient.post("/api/chitietdon/thanhtoan", body.toString());
                JSONObject ketQua = new JSONObject(phanHoi);
                boolean thanhCong = ketQua.getBoolean("thanhCong");
                String thongBao = ketQua.optString("thongBao", "");

                runOnUiThread(() -> {
                    Toast.makeText(this, thongBao, Toast.LENGTH_LONG).show();
                    if (thanhCong) finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi thanh toán: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
