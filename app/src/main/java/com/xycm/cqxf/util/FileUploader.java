package com.xycm.cqxf.util;

import android.util.Log;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FileUploader {
    private static final String TAG = "FileUploader";
    private static final Gson gson = new Gson();

    // 单例
    private static FileUploader instance;
    private final OkHttpClient client;

    private FileUploader() {
        // 使用现有的OkHttp 3.10.0配置
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS) // 文件上传需要更长时间
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static FileUploader getInstance() {
        if (instance == null) {
            instance = new FileUploader();
        }
        return instance;
    }

    /**
     * 上传文件
     *
     * @param filePath  文件路径
     * @param uploadUrl 上传URL
     * @param token     认证token
     * @param callback  回调
     */
    public void upload(String filePath, String uploadUrl, String token, UploadCallback callback) {
        File file = new File(filePath);
        if (!file.exists()) {
            callback.onError("文件不存在");
            return;
        }

        Log.d(TAG, "上传文件: " + filePath + ", 大小: " + file.length());

        // 创建请求体
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(MediaType.parse("image/*"), file))
                .addFormDataPart("type", "face")
                .build();

        // 创建请求
        Request.Builder requestBuilder = new Request.Builder()
                .url(uploadUrl)
                .post(body);

        // 添加token
        if (token != null && !token.isEmpty()) {
            String authToken = token.startsWith("Bearer ") ? token : "Bearer " + token;
            requestBuilder.header("Authorization", authToken);
        }

        // 执行请求
        client.newCall(requestBuilder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("网络错误: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body().string();
                Log.d(TAG, "上传响应: " + result);
                if (response.isSuccessful()) {
                    try {
                        callback.onSuccess(result);
                    } catch (Exception e) {
                        callback.onError("解析响应失败");
                    }
                } else {
                    callback.onError("服务器错误: " + response.code());
                }
            }
        });
    }

    public interface UploadCallback {
        void onSuccess(String filePath);

        void onError(String error);
    }
}