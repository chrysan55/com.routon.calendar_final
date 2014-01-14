
package com.routon.calendar;

import android.graphics.Camera;
import android.graphics.Matrix;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;

public class RotateY3dAnimation extends Animation {
    private static final boolean DEBUG = false;
    // 开始角度
    private final float mFromDegrees;
    // 结束角度
    private final float mToDegrees;
    // 中心点
    private final float mCenterX;
    private final float mCenterY;
    private final float mDepthZ;

    // 平移开始坐标
    private final float mFromX;
    private final float mFromY;
    // 平移结束坐标
    private final float mToX;
    private final float mToY;

    // 是否需要扭曲
    private final boolean mReverse;
    // 摄像头
    private Camera mCamera;

    public RotateY3dAnimation(
            float centerX, float centerY,
            float fromDegrees, float toDegrees,
            float fromX, float toX,
            float fromY, float toY,
            float depthZ, boolean reverse) {
        if (DEBUG)
            Log.d("Rotate3dAnimation", "Rotate3dAnimation");
        mCenterX = centerX;
        mCenterY = centerY;
        mFromDegrees = fromDegrees;
        mToDegrees = toDegrees;
        mFromX = fromX;
        mFromY = fromY;
        mToX = toX;
        mToY = toY;
        mDepthZ = depthZ;
        mReverse = reverse;
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        if (DEBUG)
            Log.d("Rotate3dAnimation", "initialize");
        super.initialize(width, height, parentWidth, parentHeight);
        mCamera = new Camera();
        setInterpolator(new DecelerateInterpolator());
    }

    // 生成Transformation
    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        final float centerX = mCenterX;
        final float centerY = mCenterY;
        final Camera camera = mCamera;

        final Matrix matrix = t.getMatrix();

        camera.save();

        if (mReverse) {
            camera.translate(
                    mFromX + (mToX - mFromX) * (1.0f - interpolatedTime),
                    mFromY + (mToY - mFromY) * (1.0f - interpolatedTime),
                    mDepthZ * interpolatedTime);
            camera.rotateZ(90 * interpolatedTime);// 绕z旋转
            // 连乘两次时间是为了在动画开始阶段慢展开
            camera.rotateY(mFromDegrees
                    + ((mToDegrees - mFromDegrees) * (1.0f - interpolatedTime) * (1.0f - interpolatedTime)));// 绕y轴旋转
        } else {
            camera.translate(
                    mFromX + (mToX - mFromX) * (interpolatedTime),
                    mFromY + (mToY - mFromY) * (interpolatedTime),
                    mDepthZ * (1.0f - interpolatedTime));
            camera.rotateZ(90 * (1.0f - interpolatedTime));// 绕z旋转
            camera.rotateY(mFromDegrees
                    + ((mToDegrees - mFromDegrees) * interpolatedTime * interpolatedTime));// 绕y轴旋转
        }

        if (DEBUG)
            Log.d("Rotate3dAnimation", "mDepthZ: " + mDepthZ);//

        // 取得变换后的矩阵
        camera.getMatrix(matrix);
        camera.restore();

        matrix.preTranslate(-centerX, -centerY);
        matrix.postTranslate(centerX, centerY);
    }
}
