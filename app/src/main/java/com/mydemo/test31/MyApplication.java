package com.mydemo.test31;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.AMapLocationQualityReport;
import com.mpttpnas.pnaslibraryapi.PnasApplicationUtil;
import com.mpttpnas.pnaslibraryapi.PnasGisUtil;
import com.mydemo.test31.util.Utils;

public class MyApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = "MyApplication";

    private static int activityCount = 0;
    private static boolean isInBackground = false;

    private AMapLocationClient locationClient = null;
    private AMapLocationClientOption locationOption = null;

    private static MyApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.i(TAG, "onCreate() called");
        PnasApplicationUtil.getInstance().initApplication(this);
        registerActivityLifecycleCallbacks(this);
        try {
            // 初始化定位
            locationClient = new AMapLocationClient(getApplicationContext());
            // 设置定位回调监听
            locationClient.setLocationListener(mLocationListener);
            locationOption = getDefaultOption();
            // 设置定位场景，目前支持三种场景（签到、出行、运动，默认无场景）
            locationOption.setLocationPurpose(AMapLocationClientOption.AMapLocationPurpose.Sport);
            if (null != locationClient) {
                locationClient.setLocationOption(locationOption);
                // 设置场景模式后最好调用一次stop，再调用start以保证场景模式生效
                locationClient.stopLocation();
                locationClient.startLocation();
            }
        } catch (Exception e) {
            Log.e(TAG, "onCreate() called", e);
        }
    }

    public static MyApplication getInstance() {
        return instance;
    }

    /**
     * 声明定位回调监听器: 上报Gis
     *
     * @param lon 经度
     * @param lat 纬度
     * @param height 海拔高度
     * @param dir 方位
     * @param speed 速度
     * @param isCacheGis 是否缓存
     */
    public AMapLocationListener mLocationListener = location -> {
        if (null != location) {
            StringBuilder sb = new StringBuilder();
            // errCode等于0代表定位成功，其他的为定位失败，具体的可以参照官网定位错误码说明
            if (location.getErrorCode() == 0) {
                sb.append("定位成功" + "\n");
                sb.append("定位类型: ").append(location.getLocationType()).append("\n");
                sb.append("经    度    : ").append(location.getLongitude()).append("\n");
                sb.append("纬    度    : ").append(location.getLatitude()).append("\n");
                sb.append("精    度    : ").append(location.getAccuracy()).append("米").append("\n");
                sb.append("提供者    : ").append(location.getProvider()).append("\n");

                sb.append("速    度    : ").append(location.getSpeed()).append("米/秒").append("\n");
                sb.append("角    度    : ").append(location.getBearing()).append("\n");
                // 获取当前提供定位服务的卫星个数
                sb.append("星    数    : ").append(location.getSatellites()).append("\n");
                sb.append("国    家    : ").append(location.getCountry()).append("\n");
                sb.append("省            : ").append(location.getProvince()).append("\n");
                sb.append("市            : ").append(location.getCity()).append("\n");
                sb.append("城市编码 : ").append(location.getCityCode()).append("\n");
                sb.append("区            : ").append(location.getDistrict()).append("\n");
                sb.append("区域 码   : ").append(location.getAdCode()).append("\n");
                sb.append("地    址    : ").append(location.getAddress()).append("\n");
                sb.append("兴趣点    : ").append(location.getPoiName()).append("\n");
                //定位完成的时间
                sb.append("定位时间: ").append(Utils.formatUTC(location.getTime(), "yyyy-MM-dd HH:mm:ss")).append("\n");
                // 位置上传
                if (PnasGisUtil.getInstance().isGisEnable()) {
                    PnasGisUtil.getInstance().uploadGis(location.getLongitude(), location.getLatitude(),
                            location.getAccuracy(), location.getLocationType(), location.getSpeed(), false);
                }
            } else {
                //定位失败
                sb.append("定位失败" + "\n");
                sb.append("错误码:").append(location.getErrorCode()).append("\n");
                sb.append("错误信息:").append(location.getErrorInfo()).append("\n");
                sb.append("错误描述:").append(location.getLocationDetail()).append("\n");
            }
            sb.append("***定位质量报告***").append("\n");
            sb.append("* WIFI开关：").append(location.getLocationQualityReport().isWifiAble() ? "开启" : "关闭").append("\n");
            sb.append("* GPS状态：").append(getGPSStatusString(location.getLocationQualityReport().getGPSStatus())).append("\n");
            sb.append("* GPS星数：").append(location.getLocationQualityReport().getGPSSatellites()).append("\n");
            sb.append("* 网络类型：").append(location.getLocationQualityReport().getNetworkType()).append("\n");
            sb.append("* 网络耗时：").append(location.getLocationQualityReport().getNetUseTime()).append("\n");
            sb.append("****************").append("\n");
            // 定位之后的回调时间
            sb.append("回调时间: ").append(Utils.formatUTC(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss")).append("\n");
            // 解析定位结果，
            String result = sb.toString();
        } else {
            System.out.println("定位失败！");
        }
    };

    /**
     * 获取GPS状态的字符串
     *
     * @param statusCode GPS状态码
     */
    private String getGPSStatusString(int statusCode) {
        String str = "";
        switch (statusCode) {
            case AMapLocationQualityReport.GPS_STATUS_OK:
                str = "GPS状态正常";
                break;
            case AMapLocationQualityReport.GPS_STATUS_NOGPSPROVIDER:
                str = "手机中没有GPS Provider，无法进行GPS定位";
                break;
            case AMapLocationQualityReport.GPS_STATUS_OFF:
                str = "GPS关闭，建议开启GPS，提高定位质量";
                break;
            case AMapLocationQualityReport.GPS_STATUS_MODE_SAVING:
                str = "选择的定位模式中不包含GPS定位，建议选择包含GPS定位的模式，提高定位质量";
                break;
            case AMapLocationQualityReport.GPS_STATUS_NOGPSPERMISSION:
                str = "没有GPS定位权限，建议开启gps定位权限";
                break;
        }
        return str;
    }

    /**
     * 默认的定位参数
     *
     * @author hongming.wang
     * @since 2.8.0
     *
     */
    private AMapLocationClientOption getDefaultOption() {
        AMapLocationClientOption mOption = new AMapLocationClientOption();
        // 可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
        mOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        // 可选，设置是否gps优先，只在高精度模式下有效。默认关闭
        mOption.setGpsFirst(true);
        // 可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
        mOption.setHttpTimeOut(30000);
        //  可选，设置定位间隔。默认为2秒
        mOption.setInterval(2000);
        //可选，设置是否返回逆地理地址信息。默认是true
        mOption.setNeedAddress(true);
        // 可选，设置是否单次定位。默认是false
        mOption.setOnceLocation(false);
        // 可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
        mOption.setOnceLocationLatest(false);
        // 可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
        AMapLocationClientOption.setLocationProtocol(AMapLocationClientOption.AMapLocationProtocol.HTTP);
        // 可选，设置是否使用传感器。默认是false
        mOption.setSensorEnable(true);
        // 可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
        mOption.setWifiScan(true);
        // 可选，设置是否使用缓存定位，默认为true
        mOption.setLocationCacheEnable(false);
        // 可选，设置逆地理信息的语言，默认值为默认语言（根据所在地区选择语言）
        mOption.setGeoLanguage(AMapLocationClientOption.GeoLanguage.ZH);
        return mOption;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        activityCount++;
        if (isInBackground) {
            // 应用从后台回到前台
            isInBackground = false;
            // 可以发送事件或者回调
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        activityCount--;
        if (activityCount == 0) {
            // 应用进入后台
            isInBackground = true;
            // 可以发送事件或者回调
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }

    public static boolean isAppInBackground() {
        return isInBackground;
    }
}
