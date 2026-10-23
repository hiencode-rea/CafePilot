package com.example.nlcsn2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ApiClient {

    // 10.0.2.2 la dia chi dac biet de may ao Android tro ve localhost cua may tinh that
    public static final String BASE_URL = "http://192.168.1.238 :8080";

    // Goi GET, tra ve chuoi JSON. PHAI goi tu thread nen (khong goi tu main thread).
    public static String get(String duong) throws Exception {
        URL url = new URL(BASE_URL + duong);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        return docPhanHoi(conn);
    }

    // Goi POST voi body JSON (dang chuoi), tra ve chuoi JSON phan hoi. PHAI goi tu thread nen.
    public static String post(String duong, String jsonBody) throws Exception {
        URL url = new URL(BASE_URL + duong);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        return docPhanHoi(conn);
    }

    private static String docPhanHoi(HttpURLConnection conn) throws Exception {
        int maTrangThai = conn.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                maTrangThai >= 200 && maTrangThai < 300 ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        String dong;
        while ((dong = reader.readLine()) != null) sb.append(dong);
        reader.close();

        return sb.toString();
    }
}