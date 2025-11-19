package com.mydemo.test31.util;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import com.mpttpnas.pnaslibraryapi.https.SSLSocketClient;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * GlideModule注解，实现glideModule的自动实例化和自动注册
 */
@GlideModule
public class OkHttpGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
    }

    @Override
    public void registerComponents(Context context, Glide glide, @NonNull Registry registry) {
        Log.d("OkHttpGlideModule", "registerComponents.");
        OkHttpClient httpClient = SSLSocketClient.setSSLSocketFactory(
                new OkHttpClient.Builder()
                        .connectTimeout(5, TimeUnit.SECONDS)
                        .writeTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
        ).build();
        registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(httpClient));
    }
}
