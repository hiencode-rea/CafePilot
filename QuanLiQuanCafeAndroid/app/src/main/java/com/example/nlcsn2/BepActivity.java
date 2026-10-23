package com.example.nlcsn2;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Màn hình riêng cho tài khoản Bep. Nghiệp vụ hàng đợi món sẽ bổ sung ở bước tiếp theo. */
public class BepActivity extends XuLyMonActivity {
    @Override protected String tenKhuVuc() { return "BEP"; }
    @Override protected String tenHienThi() { return "làm món bếp"; }
}