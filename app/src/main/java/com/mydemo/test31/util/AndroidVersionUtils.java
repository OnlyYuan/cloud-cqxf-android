package com.mydemo.test31.util;

import android.os.Build;
import android.text.TextUtils;
import java.util.Locale;

/**
 * Android 版本信息工具类
 * 提供设备版本、品牌、型号等信息的获取和判断
 */
public class AndroidVersionUtils {

    /**
     * 获取完整的 Android 版本信息
     */
    public static String getFullAndroidVersion() {
        return String.format(Locale.getDefault(), 
            "Android %s (API %d)", Build.VERSION.RELEASE, Build.VERSION.SDK_INT);
    }

    /**
     * 获取 API Level
     */
    public static int getApiLevel() {
        return Build.VERSION.SDK_INT;
    }

    /**
     * 获取 Android 版本名称 (如 "11", "12")
     */
    public static String getVersionRelease() {
        return Build.VERSION.RELEASE;
    }

    /**
     * 获取设备品牌
     */
    public static String getDeviceBrand() {
        return Build.BRAND;
    }

    /**
     * 获取设备型号
     */
    public static String getDeviceModel() {
        return Build.MODEL;
    }

    /**
     * 获取设备制造商
     */
    public static String getDeviceManufacturer() {
        return Build.MANUFACTURER;
    }

    /**
     * 获取完整的设备信息
     */
    public static String getFullDeviceInfo() {
        return String.format(Locale.getDefault(), 
            "%s %s (%s)", getDeviceManufacturer(), getDeviceModel(), getDeviceBrand());
    }

    /**
     * 获取 Android 版本代号（如 "Android 12"）
     */
    public static String getVersionCodeName() {
        switch (Build.VERSION.SDK_INT) {
            case Build.VERSION_CODES.BASE: return "Android 1.0";
            case Build.VERSION_CODES.BASE_1_1: return "Android 1.1";
            case Build.VERSION_CODES.CUPCAKE: return "Cupcake (1.5)";
            case Build.VERSION_CODES.DONUT: return "Donut (1.6)";
            case Build.VERSION_CODES.ECLAIR: return "Eclair (2.0)";
            case Build.VERSION_CODES.ECLAIR_0_1: return "Eclair (2.0.1)";
            case Build.VERSION_CODES.ECLAIR_MR1: return "Eclair (2.1)";
            case Build.VERSION_CODES.FROYO: return "Froyo (2.2)";
            case Build.VERSION_CODES.GINGERBREAD: return "Gingerbread (2.3)";
            case Build.VERSION_CODES.GINGERBREAD_MR1: return "Gingerbread (2.3.3)";
            case Build.VERSION_CODES.HONEYCOMB: return "Honeycomb (3.0)";
            case Build.VERSION_CODES.HONEYCOMB_MR1: return "Honeycomb (3.1)";
            case Build.VERSION_CODES.HONEYCOMB_MR2: return "Honeycomb (3.2)";
            case Build.VERSION_CODES.ICE_CREAM_SANDWICH: return "Ice Cream Sandwich (4.0)";
            case Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1: return "Ice Cream Sandwich (4.0.3)";
            case Build.VERSION_CODES.JELLY_BEAN: return "Jelly Bean (4.1)";
            case Build.VERSION_CODES.JELLY_BEAN_MR1: return "Jelly Bean (4.2)";
            case Build.VERSION_CODES.JELLY_BEAN_MR2: return "Jelly Bean (4.3)";
            case Build.VERSION_CODES.KITKAT: return "KitKat (4.4)";
            case Build.VERSION_CODES.KITKAT_WATCH: return "KitKat Watch (4.4W)";
            case Build.VERSION_CODES.LOLLIPOP: return "Lollipop (5.0)";
            case Build.VERSION_CODES.LOLLIPOP_MR1: return "Lollipop (5.1)";
            case Build.VERSION_CODES.M: return "Marshmallow (6.0)";
            case Build.VERSION_CODES.N: return "Nougat (7.0)";
            case Build.VERSION_CODES.N_MR1: return "Nougat (7.1)";
            case Build.VERSION_CODES.O: return "Oreo (8.0)";
            case Build.VERSION_CODES.O_MR1: return "Oreo (8.1)";
            case Build.VERSION_CODES.P: return "Pie (9.0)";
            case Build.VERSION_CODES.Q: return "Android 10";
            case Build.VERSION_CODES.R: return "Android 11";
            // case Build.VERSION_CODES.S: return "Android 12";
            // case Build.VERSION_CODES.S_V2: return "Android 12L";
            // case Build.VERSION_CODES.TIRAMISU: return "Android 13";
            // case Build.VERSION_CODES.UPSIDE_DOWN_CAKE: return "Android 14";
            // case Build.VERSION_CODES.VANILLA_ICE_CREAM: return "Android 15";
            default:
                // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                //     return "Android " + (Build.VERSION.SDK_INT - 20); // 估算版本
                // }
                return "Unknown Android Version";
        }
    }

    /**
     * 检查是否是特定版本及以上
     */
    public static boolean isAtLeastVersion(int apiLevel) {
        return Build.VERSION.SDK_INT >= apiLevel;
    }

    /**
     * 检查是否是特定版本以下
     */
    public static boolean isBelowVersion(int apiLevel) {
        return Build.VERSION.SDK_INT < apiLevel;
    }

    /**
     * 检查版本范围
     */
    public static boolean isVersionBetween(int minApiLevel, int maxApiLevel) {
        return Build.VERSION.SDK_INT >= minApiLevel && Build.VERSION.SDK_INT <= maxApiLevel;
    }

    /**
     * 获取基带版本
     */
    public static String getBasebandVersion() {
        return Build.getRadioVersion();
    }

    /**
     * 获取构建信息
     */
    public static String getBuildInfo() {
        return String.format(Locale.getDefault(),
            "Device: %s\n" +
            "Model: %s\n" +
            "Manufacturer: %s\n" +
            "Brand: %s\n" +
            "Android: %s (API %d)\n" +
            "Build ID: %s\n" +
            "Fingerprint: %s",
            Build.DEVICE,
            Build.MODEL,
            Build.MANUFACTURER,
            Build.BRAND,
            Build.VERSION.RELEASE,
            Build.VERSION.SDK_INT,
            Build.DISPLAY,
            Build.FINGERPRINT
        );
    }

    /**
     * 判断是否是华为/荣耀设备
     */
    public static boolean isHuaweiDevice() {
        String manufacturer = getDeviceManufacturer().toLowerCase();
        String brand = getDeviceBrand().toLowerCase();
        return manufacturer.contains("huawei") || manufacturer.contains("honor") ||
               brand.contains("huawei") || brand.contains("honor");
    }

    /**
     * 判断是否是小米设备
     */
    public static boolean isXiaomiDevice() {
        String manufacturer = getDeviceManufacturer().toLowerCase();
        String brand = getDeviceBrand().toLowerCase();
        return manufacturer.contains("xiaomi") || manufacturer.contains("redmi") ||
               brand.contains("xiaomi") || brand.contains("redmi");
    }

    /**
     * 判断是否是 OPPO/VIVO 设备
     */
    public static boolean isOppoVivoDevice() {
        String manufacturer = getDeviceManufacturer().toLowerCase();
        String brand = getDeviceBrand().toLowerCase();
        return manufacturer.contains("oppo") || manufacturer.contains("vivo") ||
               manufacturer.contains("realme") || brand.contains("oppo") ||
               brand.contains("vivo") || brand.contains("realme");
    }

    /**
     * 判断是否是三星设备
     */
    public static boolean isSamsungDevice() {
        String manufacturer = getDeviceManufacturer().toLowerCase();
        String brand = getDeviceBrand().toLowerCase();
        return manufacturer.contains("samsung") || brand.contains("samsung");
    }

    /**
     * 获取设备简化的品牌名称
     */
    public static String getSimplifiedBrand() {
        String brand = getDeviceBrand().toLowerCase();
        if (brand.contains("huawei") || brand.contains("honor")) return "Huawei";
        if (brand.contains("xiaomi") || brand.contains("redmi")) return "Xiaomi";
        if (brand.contains("oppo")) return "OPPO";
        if (brand.contains("vivo")) return "VIVO";
        if (brand.contains("samsung")) return "Samsung";
        if (brand.contains("oneplus")) return "OnePlus";
        if (brand.contains("meizu")) return "Meizu";
        if (brand.contains("lenovo")) return "Lenovo";
        if (brand.contains("zte")) return "ZTE";
        if (brand.contains("nokia")) return "Nokia";
        if (brand.contains("sony")) return "Sony";
        return getDeviceBrand();
    }

    /**
     * 获取版本兼容性信息
     */
    public static String getCompatibilityInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("当前设备: ").append(getFullDeviceInfo()).append("\n");
        sb.append("系统版本: ").append(getFullAndroidVersion()).append("\n");
        sb.append("版本代号: ").append(getVersionCodeName()).append("\n");
        
        // 添加兼容性提示
        if (isBelowVersion(Build.VERSION_CODES.M)) {
            sb.append("⚠️ 设备运行较旧版本 Android，部分功能可能不可用");
        }
        // else if (isAtLeastVersion(Build.VERSION_CODES.TIRAMISU)) {
        //     sb.append("✅ 设备运行最新版本 Android");
        // }
        else {
            sb.append("✅ 设备运行兼容版本 Android");
        }
        
        return sb.toString();
    }
}