package com.mydemo.test31.event;

/**
 * 扫码回调
 */
public class QRScannerEvent {

    /**
     * 扫码结果
     */
    private final String cameraResult;

    public QRScannerEvent(String cameraResult) {
        this.cameraResult = cameraResult;
    }

    public String getCameraResult() {
        return cameraResult;
    }
}
