package com.example.nlcsn2;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

public abstract class XuLyMonActivity extends AppCompatActivity {
    private LinearLayout danhSach;
    private String khuVuc;

    protected abstract String tenKhuVuc();
    protected abstract String tenHienThi();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xu_ly_mon);
        khuVuc = tenKhuVuc();
        SharedPreferences sp = getSharedPreferences("PhienDangNhap", MODE_PRIVATE);
        ((TextView) findViewById(R.id.lblChaoXuLy)).setText("Xin chào, " + sp.getString("hoTen", ""));
        ((TextView) findViewById(R.id.lblTieuDeXuLy)).setText("Yêu cầu " + tenHienThi());
        danhSach = findViewById(R.id.dsYeuCauXuLy);
        findViewById(R.id.btnLamMoiXuLy).setOnClickListener(v -> taiDanhSach());
        findViewById(R.id.btnDangXuatXuLy).setOnClickListener(v -> {
            sp.edit().clear().apply();
            finish();
        });
        taiDanhSach();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (danhSach != null) taiDanhSach();
    }

    private void taiDanhSach() {
        new Thread(() -> {
            try {
                JSONArray arr = new JSONArray(ApiClient.get("/api/xulymon?khuVuc=" + khuVuc));
                runOnUiThread(() -> hienThi(arr));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi tải yêu cầu: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void hienThi(JSONArray arr) {
        danhSach.removeAllViews();
        if (arr.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("Chưa có yêu cầu đang chờ.");
            empty.setTextColor(getColor(R.color.text_secondary));
            empty.setTextSize(15);
            empty.setPadding(dp(16), dp(24), dp(16), dp(24));
            danhSach.addView(empty);
            return;
        }
        for (int i = 0; i < arr.length(); i++) {
            try { taoThe(arr.getJSONObject(i)); } catch (Exception ignored) { }
        }
    }

    private void taoThe(JSONObject mon) throws Exception {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        GradientDrawable cardBackground = new GradientDrawable();
        cardBackground.setColor(getColor(R.color.surface_card));
        cardBackground.setCornerRadius(dp(18));
        cardBackground.setStroke(dp(1), getColor(R.color.border_subtle));
        card.setBackground(cardBackground);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cp);

        TextView info = new TextView(this);
        String state = mon.optString("trangThaiMon", "CHO_LAM");
        info.setText("Bàn " + mon.optString("soHieuBan", "?") + "  •  Đơn #" + mon.optInt("maDH") +
                "\n" + mon.optString("tenSP") + " x" + mon.optInt("soLuong") +
                "\nGhi chú: " + mon.optString("ghiChu", "-") +
                "\nTrạng thái: " + tenTrangThai(state));
        info.setTextColor(getColor(R.color.text_primary));
        info.setTextSize(15);
        info.setLineSpacing(dp(3), 1f);
        card.addView(info);

        Button action = new Button(this);
        if ("DANG_LAM".equals(state)) {
            action.setText("Đã ra món");
            action.setOnClickListener(v -> capNhat(mon, "DA_RA_MON"));
        } else if ("DA_RA_MON".equals(state) || "HET_MON".equals(state)) {
            action.setText("Đã xử lý");
            action.setEnabled(false);
        } else {
            action.setText("Nhận làm món");
            action.setOnClickListener(v -> capNhat(mon, "DANG_LAM"));
        }
        action.setAllCaps(false);
        action.setTextColor(getColor(R.color.white));
        action.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(
                "DANG_LAM".equals(state) ? R.color.status_success : R.color.purple_500)));
        action.setTypeface(null, Typeface.BOLD);
        card.addView(action);
        if (!"DA_RA_MON".equals(state) && !"HET_MON".equals(state)) {
            Button hetMon = new Button(this);
            hetMon.setText("Hết món");
            hetMon.setAllCaps(false);
            hetMon.setTextColor(getColor(R.color.status_danger));
            hetMon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.status_danger_surface)));
            hetMon.setOnClickListener(v -> capNhat(mon, "HET_MON"));
            card.addView(hetMon);
        }
        danhSach.addView(card);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String tenTrangThai(String state) {
        if ("CHO_LAM".equals(state)) return "Chờ làm";
        if ("DANG_LAM".equals(state)) return "Đang làm";
        if ("DA_RA_MON".equals(state)) return "Đã ra món";
        if ("HET_MON".equals(state)) return "Hết món";
        return state;
    }

    private void capNhat(JSONObject mon, String state) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("maDH", mon.getInt("maDH"));
                body.put("maSP", mon.getInt("maSP"));
                body.put("trangThaiMon", state);
                JSONObject result = new JSONObject(ApiClient.post("/api/xulymon/trangthai", body.toString()));
                runOnUiThread(() -> {
                    if (!result.optBoolean("thanhCong", false)) {
                        Toast.makeText(this, result.optString("thongBao", "Cập nhật thất bại"), Toast.LENGTH_LONG).show();
                        return;
                    }
                    Toast.makeText(this, result.optString("thongBao", "Đã cập nhật"), Toast.LENGTH_SHORT).show();
                    taiDanhSach();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}