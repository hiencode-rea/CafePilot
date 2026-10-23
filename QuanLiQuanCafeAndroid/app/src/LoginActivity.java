package com.example.nlcsn2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText edtTenDangNhap, edtMatKhau;
    private TextView lblThongBao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtTenDangNhap = findViewById(R.id.edtTenDangNhap);
        edtMatKhau = findViewById(R.id.edtMatKhau);
        lblThongBao = findViewById(R.id.lblThongBao);
        Button btnDangNhap = findViewById(R.id.btnDangNhap);

        btnDangNhap.setOnClickListener(v -> dangNhap());
    }

    private void dangNhap() {
        String tenDangNhap = edtTenDangNhap.getText().toString().trim();
        String matKhau = edtMatKhau.getText().toString().trim();

        if (tenDangNhap.isEmpty() || matKhau.isEmpty()) {
            lblThongBao.setText("Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        lblThongBao.setText("Đang đăng nhập...");

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("tenDangNhap", tenDangNhap);
                body.put("matKhau", matKhau);

                String phanHoi = ApiClient.post("/api/dangnhap", body.toString());
                JSONObject ketQua = new JSONObject(phanHoi);

                boolean thanhCong = ketQua.getBoolean("thanhCong");
                String thongBao = ketQua.optString("thongBao", "");

                runOnUiThread(() -> {
                    if (thanhCong) {
                        String vaiTro = ketQua.optString("vaiTro", "");
                        String hoTen = ketQua.optString("hoTen", "");
                        int maNV = ketQua.optInt("maNV", -1);

                        // Luu thong tin dang nhap de dung o cac man hinh khac
                        SharedPreferences sp = getSharedPreferences("PhienDangNhap", MODE_PRIVATE);
                        sp.edit()
                                .putString("tenDangNhap", tenDangNhap)
                                .putString("vaiTro", vaiTro)
                                .putString("hoTen", hoTen)
                                .putInt("maNV", maNV)
                                .apply();

                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        lblThongBao.setText(thongBao);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> lblThongBao.setText("Lỗi kết nối API: " + e.getMessage()));
            }
        }).start();
    }
}
