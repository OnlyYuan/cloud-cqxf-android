package com.xycm.cqxf.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View;

/**
 * 自定义白色圆形遮罩视图
 */
public class WhiteCircleMaskView extends View {

    private Paint whitePaint;
    private Paint clearPaint;
    private Path clipPath;
    private int circleRadius = 140; // dp
    private int maskColor = 0xFFFFFFFF; // 白色

    private int radiusPx; // 像素单位的圆半径
    private float density; // 屏幕密度

    public WhiteCircleMaskView(Context context) {
        super(context);
        init();
    }

    public WhiteCircleMaskView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 白色画笔
        whitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whitePaint.setColor(maskColor);
        whitePaint.setStyle(Paint.Style.FILL);

        // 透明画笔（用于挖空）
        clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        clearPaint.setColor(Color.TRANSPARENT);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        clipPath = new Path();

        // 启用硬件加速
        setLayerType(LAYER_TYPE_HARDWARE, null);

        // 获取屏幕密度，仅计算一次
        density = getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // 计算圆的半径（像素单位）
        radiusPx = (int) (circleRadius * density);

        // 创建圆形裁剪路径
        clipPath.reset();
        int centerX = w / 2;
        int centerY = h / 2;
        clipPath.addCircle(centerX, centerY, radiusPx, Path.Direction.CW);
        clipPath.setFillType(Path.FillType.INVERSE_WINDING);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 方法1：使用离屏缓冲（更稳定）
        int saveCount = canvas.saveLayer(0, 0, getWidth(), getHeight(), null, Canvas.ALL_SAVE_FLAG);

        // 绘制全屏白色
        canvas.drawColor(maskColor);

        // 在中心挖一个圆形
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // 挖空圆形
        canvas.drawCircle(centerX, centerY, radiusPx, clearPaint);

        canvas.restoreToCount(saveCount);
    }

    /**
     * 设置圆形半径
     *
     * @param radiusDp 圆的半径，单位 dp
     */
    public void setCircleRadius(int radiusDp) {
        if (this.circleRadius != radiusDp) {
            this.circleRadius = radiusDp;
            // 计算像素值
            radiusPx = (int) (circleRadius * density);
            invalidate(); // 只有当值改变时才触发重绘
        }
    }

    /**
     * 设置遮罩颜色
     *
     * @param color 颜色值
     */
    public void setMaskColor(int color) {
        if (this.maskColor != color) {
            this.maskColor = color;
            whitePaint.setColor(color);
            invalidate(); // 只有当颜色改变时才触发重绘
        }
    }
}
