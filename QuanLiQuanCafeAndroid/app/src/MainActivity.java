package com.example.nlcsn2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ListView dsBan;
    private final List<JSONObject> danhSachBan = new ArrayList<>();
    private ArrayAdapter<JSONObject> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sp = getSharedPreferences("PhienDangNhap", MODE_PRIVATE);
        String hoTen = sp.getString("hoTen", "");
        String vaiTro = sp.getString("vaiTro", "");

        TextView lblChao = findViewById(R.id.lblChao);
        lblChao.setText("Xin chào, " + hoTen + " (" + vaiTro + ")");

        Button btnChamCong = findViewById(R.id.btnChamCong);
        btnChamCong.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ChamCongActivity.class)));

        dsBan = findViewById(R.id.dsBan);

        adapter = new ArrayAdapter<JSONObject>(this, 0, danhSachBan) {
            @NonNull
            @Override
            public View getView(int vitri, View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_ban, parent, false);
                }
                JSONObject ban = getItem(vitri);

                TextView lblTen = convertView.findViewById(R.id.lblTenBan);
                TextView lblKhuVuc = convertView.findViewById(R.id.lblKhuVuc);
                TextView lblTrangThai = convertView.findViewById(R.id.lblTrangThai);
                View vachMau = convertView.findViewById(R.id.vachMau);

                try {
                    String soHieuBan = ban.getString("soHieuBan");
                    String tenKhuVuc = ban.getString("tenKhuVuc");
                    String trangThai = ban.getString("trangThai");
                    boolean trong = trangThai.equalsIgnoreCase("Trong");

                    lblTen.setText("Bàn " + soHieuBan);
                    lblKhuVuc.setText(tenKhuVuc);
                    lblTrangThai.setText(trong ? "Trống" : "Đang PV");
                    lblTrangThai.setTextColor(trong ? 0xFF3FB950 : 0xFFF85149);
                    lblTrangThai.setBackgroundResource(trong ? R.drawable.bg_badge_green : R.drawable.bg_badge_red);
                    vachMau.setBackgroundColor(trong ? 0xFF3FB950 : 0xFFF85149);

                } catch (Exception e) {
                    lblTen.setText("Lỗi dữ liệu");
                }
                return convertView;
            }
        };

        dsBan.setAdapter(adapter);

        dsBan.setOnItemClickListener((parent, view, vitri, id) -> {
            JSONObject ban = danhSachBan.get(vitri);
            try {
                Intent intent = new Intent(MainActivity.this, ChiTietDonHangActivity.class);
                intent.putExtra("maBan", ban.getInt("maBan"));
                intent.putExtra("soHieuBan", ban.getString("soHieuBan"));
                startActivity(intent);
            } catch (Exception ignored) {}
        });

        taiDanhSachBan();
    }

    @Override
    protected void onResume() {
        super.onResume();
        taiDanhSachBan();
    }

    private void taiDanhSachBan() {
        new Thread(() -> {
            try {
                String phanHoi = ApiClient.get("/api/ban");
                JSONArray mang = new JSONArray(phanHoi);

                List<JSONObject> ketQua = new ArrayList<>();
                for (int i = 0; i < mang.length(); i++) {
                    ketQua.add(mang.getJSONObject(i));
                }

                runOnUiThread(() -> {
                    danhSachBan.clear();
                    danhSachBan.addAll(ketQua);
                    adapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi tải danh sách bàn: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
