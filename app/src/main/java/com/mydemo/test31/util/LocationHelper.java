package com.mydemo.test31.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.List;

public class LocationHelper {
    private static final String TAG = "LocationHelper";
    private static final long MIN_TIME_MS = 3000; // 3秒
    private static final float MIN_DISTANCE_M = 0; // 0米，获取所有更新
    private static final long TIMEOUT_MS = 20000; // 20秒超时
    private static final long LOCATION_CACHE_TIME = 10000; // 10秒缓存

    private final Context context;
    private final LocationManager locationManager;
    private LocationListener locationListener;
    private final Handler timeoutHandler;
    private OnLocationResultListener locationResultListener;
    private Location lastKnownLocation;
    private long lastLocationTime = 0;
    private boolean isLocationUpdateStarted = false;
    private int locationUpdateCount = 0;

    public interface OnLocationResultListener {
        void onLocationSuccess(double longitude, double latitude, double altitude, float bearing, float speed);
        void onLocationFailed(String error);
    }

    public LocationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.timeoutHandler = new Handler(Looper.getMainLooper());
    }

    public void getCurrentLocation(OnLocationResultListener listener) {
        this.locationResultListener = listener;

        // 检查权限
        if (!hasLocationPermission()) {
            if (listener != null) {
                listener.onLocationFailed("没有位置权限");
            }
            return;
        }

        // 检查位置服务是否开启
        if (!isLocationEnabled()) {
            if (listener != null) {
                listener.onLocationFailed("位置服务未开启");
            }
            return;
        }

        // 尝试使用缓存位置
        if (canUseCachedLocation()) {
            return;
        }

        // 启动实时位置更新
        startLocationUpdates();

        // 设置超时
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_MS);
    }

    private boolean canUseCachedLocation() {
        if (lastKnownLocation != null &&
                System.currentTimeMillis() - lastLocationTime < LOCATION_CACHE_TIME) {
            Log.d(TAG, "使用缓存位置");
            if (locationResultListener != null) {
                locationResultListener.onLocationSuccess(
                        lastKnownLocation.getLongitude(),
                        lastKnownLocation.getLatitude(),
                        lastKnownLocation.hasAltitude() ? lastKnownLocation.getAltitude() : 0,
                        lastKnownLocation.hasBearing() ? lastKnownLocation.getBearing() : 0,
                        lastKnownLocation.hasSpeed() ? lastKnownLocation.getSpeed() : 0
                );
                locationResultListener = null;
            }
            return true;
        }
        return false;
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isLocationEnabled() {
        if (locationManager == null) {
            return false;
        }
        try {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            Log.e(TAG, "检查位置服务状态失败", e);
            return false;
        }
    }

    private void startLocationUpdates() {
        if (isLocationUpdateStarted) {
            Log.d(TAG, "位置更新已启动，跳过重复启动");
            return;
        }

        isLocationUpdateStarted = true;
        locationUpdateCount = 0;

        Log.d(TAG, "开始位置更新监听");

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                locationUpdateCount++;
                Log.d(TAG, "位置更新 #" + locationUpdateCount + ": " +
                        location.getLatitude() + ", " + location.getLongitude() +
                        ", 精度: " + location.getAccuracy() + "m, 来源: " + location.getProvider());

                // 更新缓存
                updateLocationCache(location);

                // 停止监听并返回结果
                stopLocationUpdates();
                timeoutHandler.removeCallbacks(timeoutRunnable);

                if (locationResultListener != null) {
                    locationResultListener.onLocationSuccess(
                            location.getLongitude(),
                            location.getLatitude(),
                            location.hasAltitude() ? location.getAltitude() : 0,
                            location.hasBearing() ? location.getBearing() : 0,
                            location.hasSpeed() ? location.getSpeed() : 0
                    );
                    locationResultListener = null;
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d(TAG, "位置状态变化: " + provider + " - " + status);
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d(TAG, "位置提供者启用: " + provider);
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.w(TAG, "位置提供者禁用: " + provider);
            }
        };

        try {
            // 获取所有可用的位置提供者
            List<String> providers = locationManager.getProviders(true);
            Log.d(TAG, "可用的位置提供者: " + providers);

            boolean hasActiveProvider = false;

            for (String provider : providers) {
                if (LocationManager.GPS_PROVIDER.equals(provider) ||
                        LocationManager.NETWORK_PROVIDER.equals(provider)) {

                    Log.d(TAG, "注册位置监听: " + provider);
                    locationManager.requestLocationUpdates(
                            provider,
                            MIN_TIME_MS,
                            MIN_DISTANCE_M,
                            locationListener
                    );
                    hasActiveProvider = true;

                    // 立即尝试获取该提供者的最后一次位置
                    try {
                        Location lastLocation = locationManager.getLastKnownLocation(provider);
                        if (lastLocation != null) {
                            Log.d(TAG, "获取到最后一次已知位置: " + provider +
                                    " - " + lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
                            // 立即使用这个位置
                            locationListener.onLocationChanged(lastLocation);
                            break;
                        } else {
                            Log.d(TAG, "最后一次已知位置为空: " + provider);
                        }
                    } catch (SecurityException e) {
                        Log.e(TAG, "获取最后一次位置权限异常: " + provider, e);
                    }
                }
            }

            if (!hasActiveProvider) {
                Log.e(TAG, "没有可用的位置提供者");
                handleLocationFailure("没有可用的位置提供者");
            }

        } catch (SecurityException e) {
            Log.e(TAG, "位置权限异常", e);
            handleLocationFailure("位置权限异常: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "启动位置更新异常", e);
            handleLocationFailure("启动位置更新异常: " + e.getMessage());
        }
    }

    private final Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (isLocationUpdateStarted) {
                Log.w(TAG, "位置获取超时，已尝试 " + locationUpdateCount + " 次更新");
                stopLocationUpdates();

                if (locationResultListener != null) {
                    // 超时时返回缓存位置（如果有）
                    if (lastKnownLocation != null) {
                        Log.d(TAG, "超时但返回缓存位置");
                        locationResultListener.onLocationSuccess(
                                lastKnownLocation.getLongitude(),
                                lastKnownLocation.getLatitude(),
                                lastKnownLocation.hasAltitude() ? lastKnownLocation.getAltitude() : 0,
                                lastKnownLocation.hasBearing() ? lastKnownLocation.getBearing() : 0,
                                lastKnownLocation.hasSpeed() ? lastKnownLocation.getSpeed() : 0
                        );
                    } else {
                        locationResultListener.onLocationFailed("获取位置超时，请检查GPS信号或网络连接");
                    }
                    locationResultListener = null;
                }
            }
        }
    };

    private void handleLocationFailure(String error) {
        stopLocationUpdates();
        timeoutHandler.removeCallbacks(timeoutRunnable);

        if (locationResultListener != null) {
            locationResultListener.onLocationFailed(error);
            locationResultListener = null;
        }
    }

    private void updateLocationCache(Location location) {
        lastKnownLocation = location;
        lastLocationTime = System.currentTimeMillis();
    }

    private void stopLocationUpdates() {
        if (locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
                Log.d(TAG, "停止位置更新监听");
            } catch (SecurityException e) {
                Log.e(TAG, "停止位置更新权限异常", e);
            } catch (Exception e) {
                Log.e(TAG, "停止位置更新异常", e);
            }
            locationListener = null;
        }
        isLocationUpdateStarted = false;
    }

    public void release() {
        stopLocationUpdates();
        timeoutHandler.removeCallbacksAndMessages(null);
        locationResultListener = null;
        Log.d(TAG, "LocationHelper资源已释放");
    }

    /**
     * 主动设置一个位置（用于测试或模拟）
     */
    public void setMockLocation(double longitude, double latitude, double altitude) {
        Location mockLocation = new Location("mock");
        mockLocation.setLongitude(longitude);
        mockLocation.setLatitude(latitude);
        mockLocation.setAltitude(altitude);
        mockLocation.setAccuracy(10f);
        mockLocation.setTime(System.currentTimeMillis());

        updateLocationCache(mockLocation);
        Log.d(TAG, "设置模拟位置: " + longitude + ", " + latitude);
    }
}