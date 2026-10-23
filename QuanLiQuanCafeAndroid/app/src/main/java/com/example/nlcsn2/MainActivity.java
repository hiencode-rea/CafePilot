package com.example.nlcsn2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class MainActivity extends AppCompatActivity {

    private LinearLayout dsKhuVuc;
    private final List<JSONObject> danhSachBan = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tuDongLamMoi = new Runnable() {
        @Override
        public void run() {
            taiDanhSachBan();
            handler.postDelayed(this, 30_000);
        }
    };
    private static final DecimalFormat DINH_DANG_TIEN = new DecimalFormat("#,##0");
    private static final DateTimeFormatter DINH_DANG_THOI_GIAN = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

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

        dsKhuVuc = findViewById(R.id.dsKhuVuc);

        taiDanhSachBan();
        handler.postDelayed(tuDongLamMoi, 30_000);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(tuDongLamMoi);
        super.onDestroy();
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
                    veSoDoBan();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (e.getMessage() != null && e.getMessage().contains("Failed to connect")) {
                        Toast.makeText(this, "Không thể kết nối đến máy chủ. Vui lòng kiểm tra mạng.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Lỗi tải danh sách bàn: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private void veSoDoBan() {
        dsKhuVuc.removeAllViews();
        Map<String, List<JSONObject>> theoKhuVuc = new LinkedHashMap<>();
        for (JSONObject ban : danhSachBan) {
            String khuVuc = ban.optString("tenKhuVuc", "Không phân khu");
            theoKhuVuc.computeIfAbsent(khuVuc, k -> new ArrayList<>()).add(ban);
        }

        for (Map.Entry<String, List<JSONObject>> entry : theoKhuVuc.entrySet()) {
            TextView tieuDe = new TextView(this);
            tieuDe.setText("▦  " + entry.getKey() + "  •  " + entry.getValue().size() + " bàn");
            tieuDe.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_primary));
            tieuDe.setTextSize(18);
            tieuDe.setTypeface(null, android.graphics.Typeface.BOLD);
            tieuDe.setPadding(dp(8), dp(18), dp(8), dp(10));
            dsKhuVuc.addView(tieuDe);

            HorizontalScrollView cuonNgang = new HorizontalScrollView(this);
            cuonNgang.setFillViewport(false);
            cuonNgang.setClipToPadding(true);

            FrameLayout matBang = new FrameLayout(this);
            matBang.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.surface_page));
            matBang.setMinimumWidth(dp(800));
            matBang.setMinimumHeight(dp(640));
            matBang.setPadding(dp(8), dp(8), dp(8), dp(8));

            double[][] viTriMacDinh = {{24, 24}, {300, 24}, {576, 24}, {150, 250}, {450, 250}, {24, 476}, {300, 476}, {576, 476}};
            for (int i = 0; i < entry.getValue().size(); i++) {
                JSONObject ban = entry.getValue().get(i);
                View theBan = taoTheBan(ban);
                double x = ban.optDouble("viTriX", -1);
                double y = ban.optDouble("viTriY", -1);
                if (x < 0 || y < 0) {
                    if (i < viTriMacDinh.length) {
                        x = viTriMacDinh[i][0];
                        y = viTriMacDinh[i][1];
                    } else {
                        x = 24 + (i % 3) * 276;
                        y = 24 + (i / 3) * 226;
                    }
                }
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(190), dp(142));
                params.leftMargin = dp((int) x);
                params.topMargin = dp((int) y);
                matBang.addView(theBan, params);
            }

            cuonNgang.addView(matBang, new ViewGroup.LayoutParams(dp(800), dp(640)));
            LinearLayout.LayoutParams roomParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(650));
            roomParams.bottomMargin = dp(12);
            dsKhuVuc.addView(cuonNgang, roomParams);
        }
    }

    private View taoTheBan(JSONObject ban) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_ban, dsKhuVuc, false);
        TextView lblTen = view.findViewById(R.id.lblTenBan);
        TextView lblKhuVuc = view.findViewById(R.id.lblKhuVuc);
        TextView lblTrangThai = view.findViewById(R.id.lblTrangThai);
        TextView lblSoNguoi = view.findViewById(R.id.lblSoNguoi);
        TextView lblTongTien = view.findViewById(R.id.lblTongTien);
        TextView lblThoiGian = view.findViewById(R.id.lblThoiGian);
        View vachMau = view.findViewById(R.id.vachMau);

        String soHieuBan = ban.optString("soHieuBan", "?");
        String tenKhuVuc = ban.optString("tenKhuVuc", "");
        boolean trong = "Trong".equalsIgnoreCase(ban.optString("trangThai", ""));
        lblTen.setText("Bàn " + soHieuBan);
        lblKhuVuc.setText(tenKhuVuc);
        lblTrangThai.setText(trong ? "Trống" : "Đang PV");
        lblTrangThai.setTextColor(androidx.core.content.ContextCompat.getColor(this,
                trong ? R.color.status_success : R.color.status_danger));
        lblTrangThai.setBackgroundResource(trong ? R.drawable.bg_badge_green : R.drawable.bg_badge_red);
        vachMau.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this,
                trong ? R.color.status_success : R.color.status_danger));
        lblSoNguoi.setText("Người: " + ban.optInt("soNguoi", 0) + "/" + ban.optInt("sucChua", 0));
        lblTongTien.setText("Tổng: " + DINH_DANG_TIEN.format(ban.optDouble("tongTien", 0)) + " VND");
        lblThoiGian.setText("Đã order: " + dinhDangThoiGian(ban.optString("thoiGianTao", null)));

        view.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChiTietDonHangActivity.class);
            intent.putExtra("maBan", ban.optInt("maBan", -1));
            intent.putExtra("soHieuBan", soHieuBan);
            intent.putExtra("sucChua", ban.optInt("sucChua", 1));
            startActivity(intent);
        });
        return view;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String dinhDangThoiGian(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty() || "null".equalsIgnoreCase(giaTri)) return "-";
        try {
            long phut = Math.max(0, Duration.between(
                    LocalDateTime.parse(giaTri, DINH_DANG_THOI_GIAN), LocalDateTime.now()).toMinutes());
            long gio = phut / 60;
            return gio > 0 ? gio + " giờ " + (phut % 60) + " phút" : phut + " phút";
        } catch (DateTimeParseException e) {
            return "-";
        }
    }
}
