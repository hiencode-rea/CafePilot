package com.example.nlcsn2;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChamCongActivity extends AppCompatActivity {

    private String tenDangNhap;

    private Spinner spCaVao, spCaRa;
    private final Map<String, Integer> mapCaVao = new LinkedHashMap<>();
    private final Map<String, Integer> mapCaRa = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cham_cong);

        SharedPreferences sp = getSharedPreferences("PhienDangNhap", MODE_PRIVATE);
        tenDangNhap = sp.getString("tenDangNhap", "");

        spCaVao = findViewById(R.id.spCaVao);
        spCaRa = findViewById(R.id.spCaRa);
        Button btnVao = findViewById(R.id.btnChamCongVao);
        Button btnRa = findViewById(R.id.btnChamCongRa);

        taiDanhSach();

        btnVao.setOnClickListener(v -> {
            String chon = (String) spCaVao.getSelectedItem();
            if (chon == null || !mapCaVao.containsKey(chon)) {
                Toast.makeText(this, "Không có ca nào để chấm công vào.", Toast.LENGTH_SHORT).show();
                return;
            }
            chamCongVao(mapCaVao.get(chon));
        });

        btnRa.setOnClickListener(v -> {
            String chon = (String) spCaRa.getSelectedItem();
            if (chon == null || !mapCaRa.containsKey(chon)) {
                Toast.makeText(this, "Không có bản ghi nào để chấm công ra.", Toast.LENGTH_SHORT).show();
                return;
            }
            chamCongRa(mapCaRa.get(chon));
        });
    }

    private void taiDanhSach() {
        new Thread(() -> {
            try {
                String phanHoiVao = ApiClient.get("/api/calamviec?tenDangNhap=" + tenDangNhap);
                JSONArray mangVao = new JSONArray(phanHoiVao);

                Map<String, Integer> tamVao = new LinkedHashMap<>();
                for (int i = 0; i < mangVao.length(); i++) {
                    JSONObject ca = mangVao.getJSONObject(i);
                    String moTa = ca.getString("ngay") + "  (" + ca.getString("gioBatDau") + "-" + ca.getString("gioKetThuc") + ")";
                    tamVao.put(moTa, ca.getInt("maCa"));
                }

                String phanHoiRa = ApiClient.get("/api/chamcongchuara?tenDangNhap=" + tenDangNhap);
                JSONArray mangRa = new JSONArray(phanHoiRa);

                Map<String, Integer> tamRa = new LinkedHashMap<>();
                for (int i = 0; i < mangRa.length(); i++) {
                    JSONObject cc = mangRa.getJSONObject(i);
                    String moTa = cc.getString("ngay") + "  (" + cc.getString("gioBatDau") + "-" + cc.getString("gioKetThuc") + ")";
                    tamRa.put(moTa, cc.getInt("maCC"));
                }

                runOnUiThread(() -> {
                    mapCaVao.clear();
                    mapCaVao.putAll(tamVao);
                    ArrayAdapter<String> adapterVao = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_dropdown_item, new ArrayList<>(mapCaVao.keySet()));
                    spCaVao.setAdapter(adapterVao);

                    mapCaRa.clear();
                    mapCaRa.putAll(tamRa);
                    ArrayAdapter<String> adapterRa = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_dropdown_item, new ArrayList<>(mapCaRa.keySet()));
                    spCaRa.setAdapter(adapterRa);
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi tải danh sách: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void chamCongVao(int maCa) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("maCa", maCa);
                String phanHoi = ApiClient.post("/api/chamcong/vao", body.toString());
                JSONObject ketQua = new JSONObject(phanHoi);
                String thongBao = ketQua.optString("thongBao", "");

                runOnUiThread(() -> {
                    Toast.makeText(this, thongBao, Toast.LENGTH_SHORT).show();
                    taiDanhSach();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void chamCongRa(int maCC) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("maCC", maCC);
                String phanHoi = ApiClient.post("/api/chamcong/ra", body.toString());
                JSONObject ketQua = new JSONObject(phanHoi);
                String thongBao = ketQua.optString("thongBao", "");

                runOnUiThread(() -> {
                    Toast.makeText(this, thongBao, Toast.LENGTH_SHORT).show();
                    taiDanhSach();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
