package com.routon.calendar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Region;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.Scroller;
import android.widget.Toast;

public class MyFrameLayout extends FrameLayout {
	private Bitmap mBitmap = null;
	private Canvas mCanvas = null;
	private Bitmap mPreBitmap = null;
	private Bitmap mCurBitmap = null;

	private int mPreViewId = -1;

	private int mCornerX = 0; // 拖拽点对应的页脚
	private int mCornerY = 0;
	private Path mPath0;
	private Path mPath1;

	PointF mTouch = new PointF(); // 拖拽点
	PointF mBezierStart1 = new PointF(); // 贝塞尔曲线起始点
	PointF mBezierControl1 = new PointF(); // 贝塞尔曲线控制点
	PointF mBeziervertex1 = new PointF(); // 贝塞尔曲线顶点
	PointF mBezierEnd1 = new PointF(); // 贝塞尔曲线结束点

	PointF mBezierStart2 = new PointF(); // 另一条贝塞尔曲线
	PointF mBezierControl2 = new PointF();
	PointF mBeziervertex2 = new PointF();
	PointF mBezierEnd2 = new PointF();

	float mMiddleX;
	float mMiddleY;
	float mDegrees;
	float mTouchToCornerDis;
	ColorMatrixColorFilter mColorMatrixFilter;
	Matrix mMatrix;
	float[] mMatrixArray = { 0, 0, 0, 0, 0, 0, 0, 0, 1.0f };

	boolean mIsRTandLB; // 是否属于右上左下
	float mMaxLength = 0;
	int[] mBackShadowColors;
	int[] mFrontShadowColors;
	GradientDrawable mBackShadowDrawableLR;
	GradientDrawable mBackShadowDrawableRL;
	GradientDrawable mFolderShadowDrawableLR;
	GradientDrawable mFolderShadowDrawableRL;

	GradientDrawable mFrontShadowDrawableHBT;
	GradientDrawable mFrontShadowDrawableHTB;
	GradientDrawable mFrontShadowDrawableVLR;
	GradientDrawable mFrontShadowDrawableVRL;

	Paint mPaint;

	Scroller mScroller;
	private boolean mIsAnimating = false;// Indicating whether it is animating
	private boolean mReverse = false;// 用来指示翻页动画是指示旧fragment是以翻页的方式卷出
										// 还是新fragment以翻页的方式卷进
										// 默认
	private boolean mSnapshot = false;// 是否要取得快照
	private Context mContext = null;

	private boolean needAnimation = false;

	public MyFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		mContext = context;
		viewInit();
	}

	public MyFrameLayout(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		mContext = context;
		viewInit();
	}

	private void viewInit() {
		mPath0 = new Path();
		mPath1 = new Path();
		createDrawable();

		mPaint = new Paint();
		mPaint.setStyle(Paint.Style.FILL);

		ColorMatrix cm = new ColorMatrix();
		float array[] = { // 这个矩阵用来作为颜色过滤的参数,即将bitmap中的每个像素的[r g b
				// a]与这个矩阵相乘,得到新的颜色信息
				0.55f, 0, 0, 0, 100.0f, 0, 0.55f, 0, 0, 100.0f, 0, 0, 0.55f, 0,
				80.0f, 0, 0, 0, 1.0f, 0 };
		// 第一行决定红色,第二行决定绿色,第三行决定蓝色,第四行决定透明度,第五列是颜色的偏移量
		cm.set(array);
		mColorMatrixFilter = new ColorMatrixColorFilter(cm);
		mMatrix = new Matrix();

		AccelerateDecelerateInterpolator bi = new AccelerateDecelerateInterpolator();
		mScroller = new Scroller(getContext(), bi);

		mTouch.x = 0.01f; // 不让x,y为0,否则在点计算时会有问题
		mTouch.y = 0.01f;

		// 关闭硬件加速,硬件加速情况下不支持clipPath方法
		this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
	}

	// @Override
	// protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	// // TODO Auto-generated method stub
	// super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	// }

	// public void needAnimate(boolean need) {
	// ViewGroup child = (ViewGroup) this.getChildAt(0);
	// assert (child instanceof ViewGroup);//
	// 因为从配置文件可以保证child是一个ViewGroup,为了提高效果,直接使用断言
	//
	// int id = ((ViewGroup) child).getChildAt(0).getId();
	// if (mPreViewId == -1) {
	// mPreViewId = id;
	// return false;
	// }
	// if (mPreViewId == id) {
	// return false;
	// }
	// if (mPreViewId != id) {
	// mPreViewId = id;
	// return true;
	// }
	// assert (false);
	// needAnimation = need;
	// }

	static long first = -1, second = -1;

	@Override
	protected void dispatchDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		// //////////////先创建一个mBitmap/////////////
		// /////以下的创建不能移到init()函数中去,因为在构造函数中getWidth()与getHeight()都为0
		if (mBitmap == null) {
			mBitmap = Bitmap.createBitmap(getWidth(), getHeight(),
					Bitmap.Config.ARGB_8888);
		}
		if (mCanvas == null) {
			mCanvas = new Canvas(mBitmap);
			mCanvas.setBitmap(mBitmap);
		}
		// System.out.println("当前系统时间 in dispatchDraw: =====>" +
		// System.currentTimeMillis());
		// 进入此函数的间隔时间
		if (first == -1)
			first = System.currentTimeMillis();
		else {
			second = System.currentTimeMillis();
			// System.out.println("两次进入dispatchDraw的时间间隔 =====>" + (second -
			// first));
			first = second;
		}

		long begin = System.currentTimeMillis(); // 测试起始时间
													// currentTimeMillis()返回以毫秒为单位的当前时间
		long t1 = 0, t2 = 0, t3 = 0, t4 = 0, t5 = 0, t6 = 0, t7 = 0, t8 = 0;
		t7 = System.currentTimeMillis();
		if (mIsAnimating == false) {
			t1 = System.currentTimeMillis();
			if (mSnapshot == true)// 在动画前需要一张当前页的快照
			{
				super.dispatchDraw(mCanvas);
				// canvas.drawBitmap(mBitmap, 0, 0, null);
				destroyBitmap(mPreBitmap);
				Log.i("TAG", "FLAG : preBitmap");
				mPreBitmap = mBitmap.copy(Bitmap.Config.ARGB_8888, false);
				mSnapshot = false;
			}
			t2 = System.currentTimeMillis();
			mBitmap.eraseColor(Color.TRANSPARENT);
			if (needAnimation == false) {
				t3 = System.currentTimeMillis();
				super.dispatchDraw(canvas);
				t4 = System.currentTimeMillis();
			} else {
				needAnimation = false;
				t5 = System.currentTimeMillis();
				// 将下一页保存到mCurBitmap上
				super.dispatchDraw(mCanvas);
				destroyBitmap(mCurBitmap);
				mCurBitmap = mBitmap.copy(Bitmap.Config.ARGB_8888, false);
				// 启动动画
				startPageTurnAnimation(1300);
				t6 = System.currentTimeMillis();
			}
		}
		t8 = System.currentTimeMillis();
		// System.out.println("mIsAnimating == false =====>" + (t8 - t7));
		// System.out.println("\tmSnapshot == true =====>" + (t2 - t1));
		// System.out.println("\tneedAnimate() == false =====>" + (t4 - t3));
		// System.out.println("\tneedAnimate() == true =====>" + (t6 - t5));

		t1 = System.currentTimeMillis();
		if (mIsAnimating == true) {
			t3 = System.currentTimeMillis();
			calcPoints();
			t4 = System.currentTimeMillis();
			if (mReverse == false) {
				t5 = System.currentTimeMillis();
				drawCurrentPageArea(canvas, mPreBitmap, mPath0);
				drawNextPageAreaAndShadow(canvas, mCurBitmap);
				drawCurrentPageShadow(canvas);
				drawCurrentBackArea(canvas, mPreBitmap);
				t6 = System.currentTimeMillis();
			} else {
				t5 = System.currentTimeMillis();
				drawCurrentPageArea(canvas, mCurBitmap, mPath0);
				drawNextPageAreaAndShadow(canvas, mPreBitmap);
				drawCurrentPageShadow(canvas);
				drawCurrentBackArea(canvas, mCurBitmap);
				t6 = System.currentTimeMillis();
			}
			invalidate();
		}
		t2 = System.currentTimeMillis();
		// System.out.println("mIsAnimating == true =====>" + (t2 - t1));
		// System.out.println("\t计算各点坐标	=====>" + (t4 - t3));
		// System.out.println("\t绘制动画 =====>" + (t6 - t5));
		long end = System.currentTimeMillis(); // 测试起始时间
		// System.out.println("整个dispatchDraw耗时 =====>" + (end - begin));
	}

	private void destroyBitmap(Bitmap bitmap) {
		if (bitmap != null && bitmap.isRecycled() == false) {
			bitmap.recycle();
			bitmap = null;
		}
	}

	/**
	 * 计算拖拽点对应的拖拽脚
	 */
	public void calcCornerXY(float x, float y) {
		if (x <= getWidth() / 2)
			mCornerX = 0;
		else
			mCornerX = getWidth();
		if (y <= getHeight() / 2)
			mCornerY = 0;
		else
			mCornerY = getHeight();
		if ((mCornerX == 0 && mCornerY == getHeight())
				|| (mCornerX == getWidth() && mCornerY == 0))
			mIsRTandLB = true;
		else
			mIsRTandLB = false;
	}

	/**
	 * 求解直线P1P2和直线P3P4的交点坐标
	 */
	public PointF getCross(PointF P1, PointF P2, PointF P3, PointF P4) {
		PointF CrossP = new PointF();
		// 二元函数通式： y=ax+b
		float a1 = (P2.y - P1.y) / (P2.x - P1.x);
		float b1 = ((P1.x * P2.y) - (P2.x * P1.y)) / (P1.x - P2.x);

		float a2 = (P4.y - P3.y) / (P4.x - P3.x);
		float b2 = ((P3.x * P4.y) - (P4.x * P3.y)) / (P3.x - P4.x);
		CrossP.x = (b2 - b1) / (a1 - a2);
		CrossP.y = a1 * CrossP.x + b1;
		return CrossP;
	}

	/*
	 * 计算两条贝塞尔曲线的各个点坐标 这个函数要注意,一但有贝塞曲线的控制点的计算值发生溢出,那么整个贝塞尔曲线的计算就不正确
	 * 简单的限制一下,不会影响最终结果
	 */
	private void calcPoints() {

		mMaxLength = (float) Math.hypot(getWidth(), getHeight());

		mMiddleX = (mTouch.x + mCornerX) / 2;
		mMiddleY = (mTouch.y + mCornerY) / 2;
		mBezierControl1.x = mMiddleX - (mCornerY - mMiddleY)
				* (mCornerY - mMiddleY) / (mCornerX - mMiddleX);
		mBezierControl1.y = mCornerY;
		mBezierControl2.x = mCornerX;
		mBezierControl2.y = Math.min(Math.max(mMiddleY - (mCornerX - mMiddleX)
				* (mCornerX - mMiddleX) / (mCornerY - mMiddleY), -80000.0f),
				80000.0f);

		// Log.i("hmg", "mTouchX  " + mTouch.x + "  mTouchY  " + mTouch.y);
		// Log.i("hmg", "mBezierControl1.x  " + mBezierControl1.x
		// + "  mBezierControl1.y  " + mBezierControl1.y);
		// Log.i("hmg", "mBezierControl2.x  " + mBezierControl2.x
		// + "  mBezierControl2.y  " + mBezierControl2.y);

		mBezierStart1.x = mBezierControl1.x - (mCornerX - mBezierControl1.x)
				/ 2;
		mBezierStart1.y = mCornerY;

		// 当mBezierStart1.x < 0或者mBezierStart1.x > 480时
		// 如果继续翻页，会出现BUG故在此限制
		// 但是由于要改造成向上翻的情况,而不是原来的向左右翻的情况,所以去掉此限制.
		if (false && mTouch.x > 0 && mTouch.x < getWidth()) {
			if (mBezierStart1.x < 0 || mBezierStart1.x > getWidth()) {
				if (mBezierStart1.x < 0)
					mBezierStart1.x = getWidth() - mBezierStart1.x;

				float f1 = Math.abs(mCornerX - mTouch.x);
				float f2 = getWidth() * f1 / mBezierStart1.x;
				mTouch.x = Math.abs(mCornerX - f2);

				float f3 = Math.abs(mCornerX - mTouch.x)
						* Math.abs(mCornerY - mTouch.y) / f1;
				mTouch.y = Math.abs(mCornerY - f3);

				mMiddleX = (mTouch.x + mCornerX) / 2;
				mMiddleY = (mTouch.y + mCornerY) / 2;

				mBezierControl1.x = mMiddleX - (mCornerY - mMiddleY)
						* (mCornerY - mMiddleY) / (mCornerX - mMiddleX);
				mBezierControl1.y = mCornerY;

				mBezierControl2.x = mCornerX;
				mBezierControl2.y = Math.max(mMiddleY - (mCornerX - mMiddleX)
						* (mCornerX - mMiddleX) / (mCornerY - mMiddleY),
						-80000.0f);

				// Log.i("hmg", "mTouchX --> " + mTouch.x + "  mTouchY-->  "
				// + mTouch.y);
				// Log.i("hmg", "mBezierControl1.x--  " + mBezierControl1.x
				// + "  mBezierControl1.y -- " + mBezierControl1.y);
				// Log.i("hmg", "mBezierControl2.x -- " + mBezierControl2.x
				// + "  mBezierControl2.y -- " + mBezierControl2.y);
				mBezierStart1.x = mBezierControl1.x
						- (mCornerX - mBezierControl1.x) / 2;
			}
		}
		mBezierStart2.x = mCornerX;
		mBezierStart2.y = mBezierControl2.y - (mCornerY - mBezierControl2.y)
				/ 2;

		mTouchToCornerDis = (float) Math.hypot((mTouch.x - mCornerX),
				(mTouch.y - mCornerY));

		mBezierEnd1 = getCross(mTouch, mBezierControl1, mBezierStart1,
				mBezierStart2);
		mBezierEnd2 = getCross(mTouch, mBezierControl2, mBezierStart1,
				mBezierStart2);

		mBeziervertex1.x = (mBezierStart1.x + 2 * mBezierControl1.x + mBezierEnd1.x) / 4;
		mBeziervertex1.y = (2 * mBezierControl1.y + mBezierStart1.y + mBezierEnd1.y) / 4;
		mBeziervertex2.x = (mBezierStart2.x + 2 * mBezierControl2.x + mBezierEnd2.x) / 4;
		mBeziervertex2.y = Math.max(
				(2 * mBezierControl2.y + mBezierStart2.y + mBezierEnd2.y) / 4,
				-80000.0f);

		// System.out.println("贝塞尔曲线:");
		// System.out.println(mBeziervertex1.x);
		// System.out.println(mBeziervertex1.y);
		// System.out.println(mBeziervertex2.x);
		// System.out.println(mBeziervertex2.y);
	}

	private void drawCurrentPageArea(Canvas canvas, Bitmap bitmap, Path path) {
		// mPath0的区域包括当前页的背面以及被它遮挡的页露出的部分
		mPath0.reset();
		mPath0.moveTo(mBezierStart1.x, mBezierStart1.y);
		mPath0.quadTo(mBezierControl1.x, mBezierControl1.y, mBezierEnd1.x,
				mBezierEnd1.y);
		mPath0.lineTo(mTouch.x, mTouch.y);
		mPath0.lineTo(mBezierEnd2.x, mBezierEnd2.y);
		mPath0.quadTo(mBezierControl2.x, mBezierControl2.y, mBezierStart2.x,
				mBezierStart2.y);
		mPath0.lineTo(mCornerX, mCornerY);
		mPath0.close();

		canvas.save();
		canvas.clipPath(path, Region.Op.XOR);// 当前页能看到的部分,即将path包围的区域剪切掉而剩下的区域
		canvas.drawBitmap(bitmap, 0, 0, null);
		canvas.restore();
	}

	private void drawNextPageAreaAndShadow(Canvas canvas, Bitmap bitmap) {
		mPath1.reset();
		mPath1.moveTo(mBezierStart1.x, mBezierStart1.y);
		mPath1.lineTo(mBeziervertex1.x, mBeziervertex1.y);
		mPath1.lineTo(mBeziervertex2.x, mBeziervertex2.y);
		mPath1.lineTo(mBezierStart2.x, mBezierStart2.y);
		mPath1.lineTo(mCornerX, mCornerY);
		mPath1.close();

		mDegrees = (float) Math.toDegrees(Math.atan2(mBezierControl1.x
				- mCornerX, mBezierControl2.y - mCornerY));
		int leftx;
		int rightx;
		GradientDrawable mBackShadowDrawable;
		if (mIsRTandLB) {
			leftx = (int) (mBezierStart1.x);
			rightx = (int) (mBezierStart1.x + mTouchToCornerDis / 4);
			mBackShadowDrawable = mBackShadowDrawableLR;
		} else {
			leftx = (int) (mBezierStart1.x - mTouchToCornerDis / 4);
			rightx = (int) mBezierStart1.x;
			mBackShadowDrawable = mBackShadowDrawableRL;
		}
		canvas.save();
		canvas.clipPath(mPath0);
		canvas.clipPath(mPath1, Region.Op.INTERSECT);// 前掉与mPath相交的部分,只剩下被遮挡的下一页中能显示的部分
		canvas.drawBitmap(bitmap, 0, 0, null);
		canvas.rotate(mDegrees, mBezierStart1.x, mBezierStart1.y);
		mBackShadowDrawable.setBounds(
				leftx,
				(int) mBezierStart1.y,
				rightx,
				(int) (mBezierStart1.y + Math.hypot(mBezierStart1.x
						- mBezierStart2.x, mBezierStart1.y - mBezierStart2.y)));
		// mMaxLength 对角线的长度
		// System.out.println("mMaxLength:" + mMaxLength);
		// System.out.println("drawNextPageAreaAndShadow: " + leftx + "\t" +
		// mBezierStart1.y + "\t" + rightx + "\t" + (int) (mMaxLength +
		// mBezierStart1.y));
		mBackShadowDrawable.draw(canvas);
		canvas.restore();
	}

	public void setScreen(int w, int h) {
	}

	/**
	 * 创建阴影的GradientDrawable
	 */
	private void createDrawable() {
		int[] color = { 0x333333, 0xf2f2f2f2, 0x333333 };
		mFolderShadowDrawableRL = new GradientDrawable(
				GradientDrawable.Orientation.RIGHT_LEFT, color);
		mFolderShadowDrawableRL
				.setGradientType(GradientDrawable.LINEAR_GRADIENT);

		mFolderShadowDrawableLR = new GradientDrawable(
				GradientDrawable.Orientation.LEFT_RIGHT, color);
		mFolderShadowDrawableLR
				.setGradientType(GradientDrawable.LINEAR_GRADIENT);

		mBackShadowColors = new int[] { 0xff111111, 0x111111 };
		mBackShadowDrawableRL = new GradientDrawable(
				GradientDrawable.Orientation.RIGHT_LEFT, mBackShadowColors);
		mBackShadowDrawableRL.setGradientType(GradientDrawable.LINEAR_GRADIENT);

		mBackShadowDrawableLR = new GradientDrawable(
				GradientDrawable.Orientation.LEFT_RIGHT, mBackShadowColors);
		mBackShadowDrawableLR.setGradientType(GradientDrawable.LINEAR_GRADIENT);

		mFrontShadowColors = new int[] { 0x80111111, 0x111111 };
		mFrontShadowDrawableVLR = new GradientDrawable(
				GradientDrawable.Orientation.LEFT_RIGHT, mFrontShadowColors);
		mFrontShadowDrawableVLR
				.setGradientType(GradientDrawable.LINEAR_GRADIENT);
		mFrontShadowDrawableVRL = new GradientDrawable(
				GradientDrawable.Orientation.RIGHT_LEFT, mFrontShadowColors);
		mFrontShadowDrawableVRL
				.setGradientType(GradientDrawable.LINEAR_GRADIENT);

		mFrontShadowDrawableHTB = new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM, mFrontShadowColors);
		mFrontShadowDrawableHTB
				.setGradientType(GradientDrawable.LINEAR_GRADIENT);

		mFrontShadowDrawableHBT = new GradientDrawable(
				GradientDrawable.Orientation.BOTTOM_TOP, mFrontShadowColors);
		mFrontShadowDrawableHBT
				.setGradientType(GradientDrawable.LINEAR_GRADIENT);
	}

	/**
	 * 绘制翻起页的阴影
	 */
	private void drawCurrentPageShadow(Canvas canvas) {
		double degree;
		if (mIsRTandLB) {
			degree = Math.PI
					/ 4
					- Math.atan2(mBezierControl1.y - mTouch.y, mTouch.x
							- mBezierControl1.x);
		} else {
			degree = Math.PI
					/ 4
					- Math.atan2(mTouch.y - mBezierControl1.y, mTouch.x
							- mBezierControl1.x);
		}
		// 翻起页阴影顶点与touch点的距离
		double d1 = (float) 25 * 1.414 * Math.cos(degree);
		double d2 = (float) 25 * 1.414 * Math.sin(degree);
		float x = (float) (mTouch.x + d1);
		float y;
		if (mIsRTandLB) {
			y = (float) (mTouch.y + d2);
		} else {
			y = (float) (mTouch.y - d2);
		}
		mPath1.reset();
		mPath1.moveTo(x, y);
		mPath1.lineTo(mTouch.x, mTouch.y);
		mPath1.lineTo(mBezierControl1.x, mBezierControl1.y);
		mPath1.lineTo(mBezierStart1.x, mBezierStart1.y);
		mPath1.close();
		float rotateDegrees;
		canvas.save();

		canvas.clipPath(mPath0, Region.Op.XOR);
		canvas.clipPath(mPath1, Region.Op.INTERSECT);
		int leftx;
		int rightx;
		GradientDrawable mCurrentPageShadow;
		if (mIsRTandLB) {
			leftx = (int) (mBezierControl1.x);
			rightx = (int) mBezierControl1.x + 25;
			mCurrentPageShadow = mFrontShadowDrawableVLR;
		} else {
			leftx = (int) (mBezierControl1.x - 25);
			rightx = (int) mBezierControl1.x + 1;
			mCurrentPageShadow = mFrontShadowDrawableVRL;
		}

		rotateDegrees = (float) Math.toDegrees(Math.atan2(mTouch.x
				- mBezierControl1.x, mBezierControl1.y - mTouch.y));
		canvas.rotate(rotateDegrees, mBezierControl1.x, mBezierControl1.y);
		mCurrentPageShadow.setBounds(
				leftx,
				(int) (mBezierControl1.y - Math.hypot(x - mBezierControl1.x, y
						- mBezierControl1.y)), rightx,
				(int) (mBezierControl1.y + 100));
		mCurrentPageShadow.draw(canvas);
		canvas.restore();

		mPath1.reset();
		mPath1.moveTo(x, y);
		mPath1.lineTo(mTouch.x, mTouch.y);
		mPath1.lineTo(mBezierControl2.x, mBezierControl2.y);
		mPath1.lineTo(mBezierStart2.x, mBezierStart2.y);
		mPath1.close();
		canvas.save();
		canvas.clipPath(mPath0, Region.Op.XOR);
		canvas.clipPath(mPath1, Region.Op.INTERSECT);
		if (mIsRTandLB) {
			leftx = (int) (mBezierControl2.y);
			rightx = (int) (mBezierControl2.y + 25);
			mCurrentPageShadow = mFrontShadowDrawableHTB;
		} else {
			leftx = (int) (mBezierControl2.y - 25);
			rightx = (int) (mBezierControl2.y + 1);
			mCurrentPageShadow = mFrontShadowDrawableHBT;
		}
		rotateDegrees = (float) Math.toDegrees(Math.atan2(mBezierControl2.y
				- mTouch.y, mBezierControl2.x - mTouch.x));
		canvas.rotate(rotateDegrees, mBezierControl2.x, mBezierControl2.y);
		float temp;
		if (mBezierControl2.y < 0)
			temp = mBezierControl2.y - getHeight();
		else
			temp = mBezierControl2.y;

		int hmg = (int) Math.hypot(mBezierControl2.x, temp);
		if (hmg > mMaxLength)
			mCurrentPageShadow
					.setBounds((int) (mBezierControl2.x - 25) - hmg, leftx,
							(int) (mBezierControl2.x + mMaxLength) - hmg,
							rightx);
		else
			mCurrentPageShadow.setBounds(
					(int) (mBezierControl2.x - mMaxLength), leftx,
					(int) (mBezierControl2.x), rightx);

		// System.out.println("hmg" + "mBezierControl2.x   " + mBezierControl2.x
		// + "  mBezierControl2.y  " + mBezierControl2.y);
		mCurrentPageShadow.draw(canvas);
		canvas.restore();
	}

	/**
	 * 绘制翻起页背面
	 */
	private void drawCurrentBackArea(Canvas canvas, Bitmap bitmap) {
		int i = (int) (mBezierStart1.x + mBezierControl1.x) / 2;
		float f1 = Math.abs(i - mBezierControl1.x);
		int i1 = (int) (mBezierStart2.y + mBezierControl2.y) / 2;
		float f2 = Math.abs(i1 - mBezierControl2.y);
		float f3 = Math.min(f1, f2);
		mPath1.reset();
		mPath1.moveTo(mBeziervertex2.x, mBeziervertex2.y);
		mPath1.lineTo(mBeziervertex1.x, mBeziervertex1.y);
		mPath1.lineTo(mBezierEnd1.x, mBezierEnd1.y);
		mPath1.lineTo(mTouch.x, mTouch.y);
		mPath1.lineTo(mBezierEnd2.x, mBezierEnd2.y);
		mPath1.close();
		GradientDrawable mFolderShadowDrawable;
		int left;
		int right;
		if (mIsRTandLB) {
			left = (int) (mBezierStart1.x - 1);
			right = (int) (mBezierStart1.x + f3 + 1);
			mFolderShadowDrawable = mFolderShadowDrawableLR;
		} else {
			left = (int) (mBezierStart1.x - f3 - 1);
			right = (int) (mBezierStart1.x + 1);
			assert (false);
			mFolderShadowDrawable = mFolderShadowDrawableRL;
		}
		canvas.save();
		canvas.clipPath(mPath0);// 先裁剪掉当前页能显示的部分
		canvas.clipPath(mPath1, Region.Op.INTERSECT);// 然后与mPath1取交集,即只剩下mPath1的区域

		mPaint.setColorFilter(mColorMatrixFilter);

		// Math.hypot(x, y): 返回两数平方和的平方根
		float dis = (float) Math.hypot(mCornerX - mBezierControl1.x,
				mBezierControl2.y - mCornerY);
		float f8 = (mCornerX - mBezierControl1.x) / dis;
		float f9 = (mBezierControl2.y - mCornerY) / dis;
		mMatrixArray[0] = 1 - 2 * f9 * f9;
		mMatrixArray[1] = 2 * f8 * f9;
		mMatrixArray[3] = mMatrixArray[1];
		mMatrixArray[4] = 1 - 2 * f8 * f8;
		mMatrix.reset();
		mMatrix.setValues(mMatrixArray);
		mMatrix.preTranslate(-mBezierControl1.x, -mBezierControl1.y);
		mMatrix.postTranslate(mBezierControl1.x, mBezierControl1.y);
		canvas.drawBitmap(bitmap, mMatrix, mPaint);
		mPaint.setColorFilter(null);
		canvas.rotate(mDegrees, mBezierStart1.x, mBezierStart1.y);

		mFolderShadowDrawable.setBounds(
				left,
				(int) (mBezierStart1.y),
				right,
				(int) (mBezierStart1.y + Math.hypot(mBezierStart2.x
						- mBezierStart1.x, mBezierStart2.y - mBezierStart1.y)));
		mFolderShadowDrawable.draw(canvas);
		canvas.restore();
	}

	/*
	 * 计算当前的的滚动值
	 */
	public void computeScroll() {
		super.computeScroll();
		if (mScroller.computeScrollOffset()) {
			float x = mScroller.getCurrX();
			float y = mScroller.getCurrY();
			mTouch.x = x;
			mTouch.y = y;
			postInvalidate();
		} else {
			mIsAnimating = false;
		}
	}

	/*
	 * 开始动画 delayMillis:动画持续时间 mReverse为真,表示向下翻 public void startScroll (int
	 * startX, int startY, int dx, int dy, int duration) dx 水平方向滑动的距离，负值会使滚动向左滚动
	 * dy 垂直方向滑动的距离，负值会使滚动向上滚动
	 * 
	 * 向上翻时:只需将上面的一页不显示即行 向上翻时:要注意 startX + dx == getWidth() startY + dy ==
	 * getHeight()
	 */
	public void startPageTurnAnimation(int delayMillis) {
		if (mReverse) {
			mTouch.x = getWidth() * 3 / 4;
			mTouch.y = -getHeight();
			mCornerX = getWidth();
			mCornerY = getHeight();
		} else {
			mTouch.x = getWidth() - 0.1f;
			mTouch.y = getHeight() - 0.1f;
			mCornerX = getWidth();
			mCornerY = getHeight();
		}

		if (mReverse) {
			mScroller.startScroll((int) mTouch.x, (int) mTouch.y,
					(int) Math.abs(mTouch.x - getWidth()),
					(int) Math.abs(mTouch.y - getHeight()), delayMillis);
		} else {
			mScroller.startScroll((int) mTouch.x, (int) mTouch.y,
					-(int) mTouch.x / 4, -(int) getHeight() * 2, delayMillis);
		}

		mIsAnimating = true;

	}

	/*
	 * 准备动画 作用是取得当前面的一个快照,以便用于接下的动画 参数reverse:指明动画的模式,false:表示卷起动画,true:表示展开动画
	 */
	public void prepareAnimation(boolean reverse) {
		mReverse = reverse;

		if(mIsAnimating == true)
			abortAnimation();
		mPreBitmap = view2Bitmap((View) this);
		if (mPreBitmap != null)
			needAnimation = true;
	}

	/*
	 * 中止动画过程 结果:会让Scroller停止动画过程直接进入最终结果状态
	 */
	public void abortAnimation() {
		if (!mScroller.isFinished()) {
			mScroller.abortAnimation();
			mIsAnimating = false;
		}
	}

	/*
	 * 测试是否能够拖拽 条件:点击点到卷曲角的距离大于宽度的十分之一
	 */
	public boolean canDragOver() {
		if (mTouchToCornerDis > getWidth() / 10)
			return true;
		return false;
	}

	/*
	 * 将View转为bitmap
	 */
	public Bitmap view2Bitmap(View v) {
		v.setDrawingCacheEnabled(true);
		v.buildDrawingCache();

		Bitmap bitmap = Bitmap.createBitmap(v.getDrawingCache());

		v.setDrawingCacheEnabled(false);
		if (bitmap != null) {
			return bitmap;
		} else {
			Log.i("TAG", "...........view to bitmap is null.............");
			return null;
		}
		
	}
}
