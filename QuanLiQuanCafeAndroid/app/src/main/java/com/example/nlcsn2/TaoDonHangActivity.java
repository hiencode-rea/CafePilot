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

public class TaoDonHangActivity extends AppCompatActivity {

    // Dong trong gio hang, luu tam trong bo nho truoc khi luu xuong CSDL
    static class DongDatMon {
        int maSP;
        String tenSP;
        int soLuong;
        double donGia;
        boolean daThongBao = false; // da bao bep hay chua

        double thanhTien() { return soLuong * donGia; }

        String hienThi() {
            String danhDau = daThongBao ? "  \u2705 Da bao" : "";
            return tenSP + "  x" + soLuong + "  =  " + thanhTien() + "d" + danhDau;
        }
    }

    private int maBan;
    private String soHieuBan;
    private String tenDangNhap;

    private Spinner spSanPham;
    private final List<JSONObject> danhSachSanPham = new ArrayList<>();

    private ListView dsGioHang;
    private final List<DongDatMon> gioHang = new ArrayList<>();
    private ArrayAdapter<String> adapterGioHang;

    private TextView lblTongTien;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tao_don_hang);

        maBan = getIntent().getIntExtra("maBan", -1);
        soHieuBan = getIntent().getStringExtra("soHieuBan");

        SharedPreferences sp = getSharedPreferences("PhienDangNhap", MODE_PRIVATE);
        tenDangNhap = sp.getString("tenDangNhap", "");

        TextView lblTieuDe = findViewById(R.id.lblTieuDe);
        lblTieuDe.setText("Tao don hang - Ban " + soHieuBan);

        spSanPham = findViewById(R.id.spSanPham);
        dsGioHang = findViewById(R.id.dsGioHang);
        lblTongTien = findViewById(R.id.lblTongTien);
        EditText edtSoLuong = findViewById(R.id.edtSoLuong);
        Button btnThem = findViewById(R.id.btnThem);
        Button btnThongBaoBep = findViewById(R.id.btnThongBaoBep);
        Button btnLuuDon = findViewById(R.id.btnLuuDon);

        adapterGioHang = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<>()) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                TextView row = (TextView) super.getView(position, convertView, parent);
                row.setTextColor(getColor(R.color.text_primary));
                row.setTextSize(15);
                row.setPadding(dp(16), dp(12), dp(16), dp(12));
                return row;
            }
        };
        dsGioHang.setAdapter(adapterGioHang);

        taiDanhSachSanPham();

        btnThem.setOnClickListener(v -> {
            int viTri = spSanPham.getSelectedItemPosition();
            if (viTri < 0 || viTri >= danhSachSanPham.size()) {
                Toast.makeText(this, "Vui long chon san pham.", Toast.LENGTH_SHORT).show();
                return;
            }
            int soLuongThem;
            try {
                soLuongThem = Integer.parseInt(edtSoLuong.getText().toString().trim());
                if (soLuongThem <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "So luong khong hop le.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                JSONObject spChon = danhSachSanPham.get(viTri);
                int maSP = spChon.getInt("maSP");

                // Neu san pham nay da co trong gio hang thi cong don so luong
                DongDatMon daCo = null;
                for (DongDatMon d : gioHang) {
                    if (d.maSP == maSP) { daCo = d; break; }
                }

                if (daCo != null) {
                    daCo.soLuong += soLuongThem;
                } else {
                    DongDatMon dong = new DongDatMon();
                    dong.maSP = maSP;
                    dong.tenSP = spChon.getString("tenSP");
                    dong.soLuong = soLuongThem;
                    dong.donGia = spChon.getDouble("giaBan");
                    gioHang.add(dong);
                }

                capNhatGioHang();

            } catch (Exception e) {
                Toast.makeText(this, "Loi them san pham.", Toast.LENGTH_SHORT).show();
            }
        });

        // Bam vao 1 dong trong gio hang de xoa - chi cho phep neu CHUA bao bep
        dsGioHang.setOnItemClickListener((parent, view, viTri, id) -> {
            DongDatMon dong = gioHang.get(viTri);
            if (dong.daThongBao) {
                Toast.makeText(this, "Mon nay da bao bep, khong the xoa.", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Xoa mon")
                    .setMessage("Xoa '" + dong.tenSP + "' khoi gio hang?")
                    .setPositiveButton("Xoa", (dlg, which) -> {
                        gioHang.remove(viTri);
                        capNhatGioHang();
                    })
                    .setNegativeButton("Huy", null)
                    .show();
        });

        btnThongBaoBep.setOnClickListener(v -> {
            if (gioHang.isEmpty()) {
                Toast.makeText(this, "Chua co mon nao trong gio hang.", Toast.LENGTH_SHORT).show();
                return;
            }
            for (DongDatMon d : gioHang) d.daThongBao = true;
            capNhatGioHang();
            Toast.makeText(this, "Da thong bao bep!", Toast.LENGTH_SHORT).show();
        });

        btnLuuDon.setOnClickListener(v -> luuDonHang());
    }

    private void taiDanhSachSanPham() {
        new Thread(() -> {
            try {
                String phanHoi = ApiClient.get("/api/sanpham");
                JSONArray mang = new JSONArray(phanHoi);

                List<JSONObject> ds = new ArrayList<>();
                List<String> tenHienThi = new ArrayList<>();
                for (int i = 0; i < mang.length(); i++) {
                    JSONObject sp = mang.getJSONObject(i);
                    ds.add(sp);
                    tenHienThi.add(sp.getString("tenSP") + " - " + sp.getDouble("giaBan") + "d");
                }

                runOnUiThread(() -> {
                    danhSachSanPham.clear();
                    danhSachSanPham.addAll(ds);

                    ArrayAdapter<String> adapterSp = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_dropdown_item, tenHienThi);
                    spSanPham.setAdapter(adapterSp);
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Loi tai san pham: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void capNhatGioHang() {
        List<String> dongHienThi = new ArrayList<>();
        double tong = 0;
        for (DongDatMon d : gioHang) {
            dongHienThi.add(d.hienThi());
            tong += d.thanhTien();
        }
        adapterGioHang.clear();
        adapterGioHang.addAll(dongHienThi);
        adapterGioHang.notifyDataSetChanged();

        lblTongTien.setText("Tong tien: " + tong + " VND");
    }

    private void luuDonHang() {
        if (gioHang.isEmpty()) {
            Toast.makeText(this, "Gio hang dang trong.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("maBan", maBan);
                body.put("tenDangNhap", tenDangNhap);

                JSONArray chiTiet = new JSONArray();
                for (DongDatMon d : gioHang) {
                    JSONObject dong = new JSONObject();
                    dong.put("maSP", d.maSP);
                    dong.put("soLuong", d.soLuong);
                    chiTiet.put(dong);
                }
                body.put("chiTiet", chiTiet);

                String phanHoi = ApiClient.post("/api/donhang", body.toString());
                JSONObject ketQua = new JSONObject(phanHoi);
                boolean thanhCong = ketQua.getBoolean("thanhCong");
                String thongBao = ketQua.optString("thongBao", "");

                runOnUiThread(() -> {
                    Toast.makeText(this, thongBao, Toast.LENGTH_LONG).show();
                    if (thanhCong) finish();
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Loi luu don hang: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
