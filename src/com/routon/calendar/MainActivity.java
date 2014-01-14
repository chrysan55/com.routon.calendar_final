package com.routon.calendar;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlPullParserException;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationSet;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.routon.utils.SoundEffect;

public class MainActivity extends Activity {
	private static final boolean DEBUG = false;
	GridView gridview;
	GridView gridview_new;
	FrameLayout gridLayout;
	FrameLayout gridLayout_new;
	FrameLayout windowLayout;
	MyFrameLayout almanacFrameLayout;
	private List<GridInfo> list;
	private List<GridInfo> listOld;
	private DaysAdapter adapter;
	private DaysAdapter adapterOld;
	// private MyHandler myHandler;
	private CalendarOriDataRunnable m = new CalendarOriDataRunnable();
	private Calendar curCal = null;
	private YMD curymd, preymd, nexymd;
	private Map<String, Integer> lunarMonMap = new HashMap<String, Integer>();
	private List<Day> preMonth = new ArrayList<Day>();
	private List<Day> curMonth = new ArrayList<Day>();
	private List<Day> nexMonth = new ArrayList<Day>();
	private FocusXY focXY = null;
	private CalendarUpdateRunnable CUR = new CalendarUpdateRunnable();
	private CalendarUpdateRunnableBack CURB = new CalendarUpdateRunnableBack();
	private HandlerOnUpdateMonth HOUM;
	private boolean processBusy = true;
	private int curDayPosition = -1;

	private CalendarTimerRunnable CTR = new CalendarTimerRunnable();
	private ImageView timer1;
	private ImageView timer2;
	private ImageView timer3;
	private ImageView timer4;
	private Time te = new Time("Asia/Shanghai");

	private HandlerTimer HT;

	private ViewGroup mContainer1;
	private RotateY3dAnimation rotation1;
	private ViewGroup mContainer2;
	private RotateY3dAnimation rotation2;
	private AnimationSet as;
	private static final int duration_start = 1000;
	private static final int duration_finish = 700;

	AnimatorSet aniSetUpdateMonth;
	ObjectAnimator gridAnimator;
	ObjectAnimator gridNewAnimator;

	private boolean animation_completed = true;
	private Day lunarDayShow = null;
	private int moveMonth = 0;
	boolean error = true;
	private boolean focusChange = false;
	private boolean openAnimation = true;

	/*
	 * Five days arrays as list of months, each month has up to 31 days Using
	 * LinkedList and ArrayList
	 */
	private LinkedList<List<Day>> months = new LinkedList<List<Day>>();
	private int curMonInd = 0;

	public void onDestroy() {
		// System.out.println("onDestroy");
		// System.exit(0);
		android.os.Process.killProcess(android.os.Process.myPid());
		super.onDestroy();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {// 当activity显示完成(此时还是透明的)就开始作动画
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			if (focusChange == false && openAnimation == true) {
				mContainer1.setAlpha(1.0f);
				mContainer2.setAlpha(1.0f);
				animationAtStart();
				focusChange = true;
			}
		}
	}

	@Override
	public void onBackPressed() {// 拦截退出事件，作完动画再退出
		if (animation_completed == true)
			if (openAnimation == true) {
				animationAtFinish();
			} else {
				overridePendingTransition(R.anim.scalesmall_alpha_in, R.anim.scalesmall_alpha_out);
				super.onBackPressed();
			}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i("tag", "~~~~~~~~~~~~~~~~onCreate");
		super.onCreate(savedInstanceState);
		// Full screen
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		MapRes();

		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			if (bundle.containsKey("cmd_args"))
				if (bundle.getString("cmd_args").equals("NoAnimation"))
					openAnimation = false;
		}
		setContentView(R.layout.activity_main);

		gridLayout = (FrameLayout) findViewById(R.id.gridlayout);
		gridLayout_new = (FrameLayout) View.inflate(this, R.layout.gridlayout_new, null);
		gridview = (GridView) findViewById(R.id.gridview);
		gridview_new = (GridView) gridLayout_new.findViewById(R.id.gridview_new);
		windowLayout = (FrameLayout) findViewById(R.id.windowlayout);

		// 画格子
		FrameLayout gridHorizontalLayout = (FrameLayout) findViewById(R.id.grid_horizontal_lines_layout);
		LayoutParams lp;
		ImageView horizontalLine;
		for (int i = 1; i <= 5; i++) {
			horizontalLine = new ImageView(this);
			horizontalLine.setImageDrawable(getResources().getDrawable(R.drawable.horizontal_line));
			lp = new LayoutParams(LayoutParams.MATCH_PARENT, (int) getResources().getDimension(
					R.dimen.grid_divider_width));
			horizontalLine.setLayoutParams(lp);
			gridHorizontalLayout.addView(horizontalLine);
			horizontalLine.setY(i * getResources().getDimension(R.dimen.grid_divider_horizontal_gap));
		}
		FrameLayout gridVerticalLayout = (FrameLayout) findViewById(R.id.grid_vertical_lines_layout);
		ImageView verticalLine;
		for (int i = 1; i <= 6; i++) {
			verticalLine = new ImageView(this);
			verticalLine.setImageDrawable(getResources().getDrawable(R.drawable.vertical_line));
			lp = new LayoutParams((int) getResources().getDimension(R.dimen.grid_divider_width),
					LayoutParams.MATCH_PARENT);
			verticalLine.setLayoutParams(lp);
			gridVerticalLayout.addView(verticalLine);
			verticalLine.setX(i * getResources().getDimension(R.dimen.grid_divider_vertical_gap));
		}

		timer1 = (ImageView) findViewById(R.id.timer1);
		timer2 = (ImageView) findViewById(R.id.timer2);
		timer3 = (ImageView) findViewById(R.id.timer3);
		timer4 = (ImageView) findViewById(R.id.timer4);

		list = new ArrayList<GridInfo>();
		listOld = new ArrayList<GridInfo>();
		for (int i = 0; i < 42; i++) {
			list.add(new GridInfo());
		}
		adapter = new DaysAdapter(this);
		adapterOld = new DaysAdapter(this);
		adapter.setList(list);
		gridview.setAdapter(adapter);
		adapterOld.setList(listOld);
		gridview_new.setAdapter(adapterOld);

		gridview.setFocusable(false);

		gridview.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				// GridInfo item = (GridInfo) arg0.getItemAtPosition(arg2);
				// Log.i(item.getGC(), item.getLC());
				/* Get the day */
				if (curMonth.isEmpty()) {
					return;
				}
				if (lunarDayShow != null) {
					int i = Integer.parseInt(curMonth.get(0).week);
					boolean reverse;
					Day day = new Day();
					if (arg2 < i) {
						day = preMonth.get(preMonth.size() - i + arg2);
					} else if (arg2 - i > curMonth.size() - 1) {
						day = nexMonth.get(arg2 - i - curMonth.size());
					} else {
						day = curMonth.get(arg2 - i);
					}

					int dayChange = compareDays(lunarDayShow, day);
					Log.i("tag", "dayChange = " + dayChange + " , day = " + day.lunarday + " " + day.lunarmonth + " "
							+ day.lunaryear);
					if (dayChange != 0) {
						if (dayChange < 0)
							reverse = false;
						else
							reverse = true;

						Log.i("TAG", "FLAG : day:" + day.lunaryear + day.lunarmonth + day.lunarday + day.number);
						almanacFrameLayout = (MyFrameLayout) findViewById(R.id.lunar);
						almanacFrameLayout.prepareAnimation(reverse);
						almanacFrameLayout.invalidate();
						/* update the lunar */
						lunarDayShow = day;
						updateLunar(day);
					}
				}

			}

		});

		gridview.setOnKeyListener(new OnKeyListener() {

			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// System.out.println(keyCode);
				focXY.X = gridview.getSelectedItemPosition() % 7 + 1;
				focXY.Y = gridview.getSelectedItemPosition() / 7 + 1;
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					if (processBusy == true) {
						return false;
					}
					processBusy = true;
					switch (keyCode) {
					case KeyEvent.KEYCODE_BACK:
					case KeyEvent.KEYCODE_ESCAPE:
						SoundEffect.makeEscSound();
						break;
					case KeyEvent.KEYCODE_MEDIA_REWIND:
						SoundEffect.makeMoveSound();
						if (animation_completed == true) {
							// load prev month
							curymd.Year = preymd.Year;
							curymd.Month = preymd.Month;
							updateYMD(curymd);
							new Thread(CURB).start();
						}
						break;
					case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
						SoundEffect.makeMoveSound();
						if (animation_completed == true) {
							// load next month
							/* update YMD */
							curymd.Year = nexymd.Year;
							curymd.Month = nexymd.Month;
							updateYMD(curymd);
							new Thread(CUR).start();
						}
						break;
					case KeyEvent.KEYCODE_DPAD_RIGHT:
						SoundEffect.makeMoveSound();
						if (focXY == null) {
							processBusy = false;
							break;
						}
						if (focXY.X == 7) {
							if (focXY.Y == 6) {
								if (animation_completed == true) {
									// load next month
									/* update YMD */
									curymd.Year = nexymd.Year;
									curymd.Month = nexymd.Month;
									updateYMD(curymd);
									new Thread(CUR).start();
								}
							} else {
								// focXY.Y++;
								// focXY.X = 1;
								gridview.setSelection((focXY.Y) * 7);
								processBusy = false;
							}
						} else {
							// focXY.X++;
							processBusy = false;
						}

						break;
					case KeyEvent.KEYCODE_DPAD_DOWN:
						SoundEffect.makeMoveSound();
						if (focXY == null) {
							processBusy = false;
							break;
						}
						if (focXY.Y == 6) {
							if (animation_completed == true) {
								// load next month
								curymd.Year = nexymd.Year;
								curymd.Month = nexymd.Month;
								updateYMD(curymd);
								new Thread(CUR).start();
							}
						} else {
							// focXY.Y++;
							processBusy = false;
						}

						break;
					case KeyEvent.KEYCODE_DPAD_UP:
						SoundEffect.makeMoveSound();
						if (focXY == null) {
							processBusy = false;
							break;
						}
						if (focXY.Y == 1) {
							if (animation_completed == true) {
								// load prev month
								curymd.Year = preymd.Year;
								curymd.Month = preymd.Month;
								updateYMD(curymd);
								new Thread(CURB).start();
							}

						} else {
							// focXY.Y--;
							processBusy = false;
						}

						break;
					case KeyEvent.KEYCODE_DPAD_LEFT:
						SoundEffect.makeMoveSound();
						if (focXY == null) {
							processBusy = false;
							break;
						}
						if (focXY.X == 1) {
							if (focXY.Y == 1) {
								if (animation_completed == true) {
									// load prev month
									curymd.Year = preymd.Year;
									curymd.Month = preymd.Month;
									updateYMD(curymd);
									new Thread(CURB).start();
								}
							} else {
								// focXY.Y--;
								// focXY.X = 7;
								gridview.setSelection((focXY.Y - 2) * 7 + 6);
								processBusy = false;
							}
						} else {
							// focXY.X--;
							processBusy = false;
						}

						break;
					default:
						processBusy = false;
					}
				}
				return false;
			}
		});
		HOUM = new HandlerOnUpdateMonth();
		HT = new HandlerTimer();

		mContainer1 = (ViewGroup) findViewById(R.id.left);
		mContainer2 = (ViewGroup) findViewById(R.id.right);
		if (openAnimation == true) {
			mContainer1.setAlpha(0);// 先隐藏
			mContainer2.setAlpha(0);
		} else {
			new Thread(m).start();
			new Thread(CTR).start();
		}
	}

	public class CalendarTimerRunnable implements Runnable {

		public void run() {

			// Bundle b=new Bundle();
			while (true) {
				Message msg = new Message();
				// b.putString("timer", "timer");
				// msg.setData(b);
				MainActivity.this.HT.sendMessage(msg);
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}

	}

	public class HandlerTimer extends Handler {
		public HandlerTimer() {

		}

		public HandlerTimer(Looper looper) {
			super(looper);
		}

		public void handleMessage(Message msg) {
			/* 消息处理 */
			// super.handleMessage(msg);
			te.setToNow();
			int hours = te.hour;
			int minutes = te.minute;
			timer1.setImageResource(Minute[hours / 10]);
			timer2.setImageResource(Minute[hours % 10]);
			timer3.setImageResource(Minute[minutes / 10]);
			timer4.setImageResource(Minute[minutes % 10]);
		}
	}

	public class HandlerOnUpdateMonth extends Handler {
		public HandlerOnUpdateMonth() {

		}

		public HandlerOnUpdateMonth(Looper looper) {
			super(looper);
		}

		public void handleMessage(Message msg) {
			/* 消息处理 */
			super.handleMessage(msg);
			Bundle b = msg.getData();
			int state = b.getInt("state");

			if (state == 1 || state == 2) {
				animation_completed = false;
				/* 先将之前的月信息保存在一个gridview中 */
				if (listOld.size() != 0)
					listOld.clear();
				Log.i("TAG", "++++++++++++++++++ " + listOld.size());
				for (int i = 0; i < 42; i++) {
					listOld.add(list.get(i).clone());
					Log.i("TAG", "----------------" + listOld.get(i).getGC());
				}
				gridview_new.invalidateViews();
			}
			/* 将本月信息更新到grid视图 */
			int position = updateMonth(updatePreCurNex(state));
			if (position == -1) {// months中没有数据，无法进行内容更新
				processBusy = false;
				return;
			}
			if (state == 0) {
				curDayPosition = position;
				/* 更新当天黄历 */
				if (error == false)
					gridview.setFocusable(true);
				if (lunarDayShow == null)
					lunarDayShow = curMonth.get(curymd.Day - 1);
				updateLunar(curMonth.get(curymd.Day - 1));
			} else if (state == 1) {
				moveMonth++;
				position = (focXY.X == 7) ? 0 : (focXY.X - 1);
				gridAnimator = null;
				gridNewAnimator = null;
				windowLayout.addView(gridLayout_new);
				gridAnimator = ObjectAnimator.ofFloat(gridLayout, "x", gridLayout.getWidth(), 0);
				gridNewAnimator = ObjectAnimator.ofFloat(gridLayout_new, "x", 0, -gridLayout.getWidth());
				gridAnimator.setDuration(500);
				gridNewAnimator.setDuration(500);
			} else if (state == 2) {
				moveMonth--;
				position = (focXY.X == 1) ? 41 : (34 + focXY.X);
				gridAnimator = null;
				gridNewAnimator = null;
				windowLayout.addView(gridLayout_new);
				gridAnimator = ObjectAnimator.ofFloat(gridLayout, "x", -gridLayout.getWidth(), 0);
				gridNewAnimator = ObjectAnimator.ofFloat(gridLayout_new, "x", 0, gridLayout.getWidth());
				gridAnimator.setDuration(500);
				gridNewAnimator.setDuration(500);
			}
			gridview.setSelection(position);
			if (focXY == null) {
				focXY = new FocusXY();
			}
			focXY.Y = position / 7 + 1;
			focXY.X = position % 7 + 1;

			/* 设置当前日期高亮 */
			if (curymd.Month == curCal.get(Calendar.MONTH) && curymd.Year == curCal.get(Calendar.YEAR)
					&& gridview.isFocusable()) {
				setCurDayHightLight(curDayPosition);
			} else {
				unsetCurDayHightLight(curDayPosition);
			}

			/* 刷新gridview */
			gridview.invalidateViews();

			/* 设置顶部年月标题 */
			TextView tagYearMonth = (TextView) findViewById(R.id.TagYearMonth);
			String textCurYearMonth = Integer.toString(curymd.Year) + "年" + Integer.toString(curymd.Month + 1) + "月";
			tagYearMonth.setText(textCurYearMonth);

			/* 移动动画 */
			if (state == 1 || state == 2) {
				if (aniSetUpdateMonth != null) {
					aniSetUpdateMonth.cancel();
					aniSetUpdateMonth = null;
				}
				aniSetUpdateMonth = new AnimatorSet();
				aniSetUpdateMonth.playTogether(gridNewAnimator, gridAnimator);
				aniSetUpdateMonth.start();

				aniSetUpdateMonth.addListener(new AnimatorListenerAdapter() {

					public void onAnimationStart(Animator animation) {
						// TODO Auto-generated method stub

					}

					public void onAnimationRepeat(Animator animation) {
						// TODO Auto-generated method stub

					}

					public void onAnimationEnd(Animator animation) {
						// TODO Auto-generated method stub
						windowLayout.removeView(gridLayout_new);
						animation_completed = true;
						processBusy = false;
					}

					public void onAnimationCancel(Animator animation) {
						// TODO Auto-generated method stub

					}
				});
			} else {
				processBusy = false;
			}
		}
	}

	private InputStream downloadUrl(String urlString) throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000 /* milliseconds */);
		conn.setConnectTimeout(15000 /* milliseconds */);
		conn.setRequestMethod("GET");
		conn.setDoInput(true);
		// Starts the query
		conn.connect();
		InputStream stream = conn.getInputStream();
		return stream;
	}

	private class CalendarOriDataRunnable implements Runnable {
		private CalendarXmlParser xmlParser = new CalendarXmlParser();

		public void run() {
			curCal = Calendar.getInstance();
			curymd = new YMD(curCal.get(Calendar.YEAR), curCal.get(Calendar.MONTH), curCal.get(Calendar.DAY_OF_MONTH));
			preymd = new YMD();
			nexymd = new YMD();
			updateYMD(curymd);
			getXml(preymd.Year, preymd.Month);
			getXml(curymd.Year, curymd.Month);
			getXml(nexymd.Year, nexymd.Month);

			Message msg = new Message();
			Bundle b = new Bundle();
			b.putString("textStr", "xml obtained");
			b.putInt("state", 0); // 0 means initialize
			msg.setData(b);
			MainActivity.this.HOUM.sendMessage(msg);
		}

		private void getXml(int year, int month) {
			// 获得的数据
			InputStream in = null;
			List<Day> list = null;
			try {
				error = false;
				String httpUrl = getUrl(year, month);
				in = downloadUrl(httpUrl);
				list = xmlParser.parse(in);
				in.close();
			} catch (IOException e) {// 无法从网络取得数据，生成“N/A”链表
				error = true;
				e.printStackTrace();
			} catch (XmlPullParserException e) {
				error = true;
				e.printStackTrace();
			}
			if (error && list == null) {
				list = xmlParser.parse(year, month);
			}
			addList(list);
			// urlConn.disconnect();
		}

		private void addList(List<Day> list) {
			months.addLast(list);
		}
	}

	public class CalendarUpdateRunnable implements Runnable {
		private CalendarXmlParser xmlParser = new CalendarXmlParser();

		public void run() {
			if (months.size() > 0 && curMonInd + 1 >= months.size() - 1) {
				getXml(nexymd.Year, nexymd.Month);
			}
			Message msg = new Message();
			Bundle b = new Bundle();
			b.putString("textStr", "xml obtained");
			b.putInt("state", 1); // 1 means forward
			msg.setData(b);
			MainActivity.this.HOUM.sendMessage(msg);
		}

		private void getXml(int year, int month) {
			// 获得的数据
			InputStream in = null;
			List<Day> list = null;
			boolean error = false;
			try {
				String httpUrl = getUrl(year, month);
				in = downloadUrl(httpUrl);
				list = xmlParser.parse(in);
				in.close();
			} catch (XmlPullParserException e) {
				error = true;
				e.printStackTrace();
			} catch (IOException e) {
				error = true;
				e.printStackTrace();
			}
			if (error && list == null) {
				list = xmlParser.parse(year, month);
			}
			addList(list);
		}

		private void addList(List<Day> list) {
			months.addLast(list);
			if (months.size() > 5) {
				months.removeFirst();
				curMonInd--;
			}
		}
	}

	public class CalendarUpdateRunnableBack implements Runnable {
		private CalendarXmlParser xmlParser = new CalendarXmlParser();

		public void run() {
			if (curMonInd - 1 <= 0) {
				getXml(preymd.Year, preymd.Month);
			}
			Message msg = new Message();
			Bundle b = new Bundle();
			b.putString("textStr", "xml obtained");
			b.putInt("state", 2); // 2 means backward
			msg.setData(b);
			MainActivity.this.HOUM.sendMessage(msg);
		}

		private void getXml(int year, int month) {

			// 获得的数据
			InputStream in = null;
			List<Day> list = null;
			boolean error = false;
			try {
				String httpUrl = getUrl(year, month);
				in = downloadUrl(httpUrl);
				list = xmlParser.parse(in);

				in.close();
			} catch (XmlPullParserException e) {
				error = true;
				e.printStackTrace();
			} catch (IOException e) {
				error = true;
				e.printStackTrace();
			}
			// urlConn.disconnect();
			if (error && list == null) {
				list = xmlParser.parse(year, month);
			}
			months.addFirst(list);
			curMonInd++;
			if (months.size() > 5) {
				months.removeLast();
			}
		}
	}

	private String getUrl(int year, int month) {
		String httpUrl = "http://www.grandes.com.cn/calendar/" + year;
		httpUrl += "/";
		httpUrl += month + 1;
		httpUrl += ".xml";
		return httpUrl;
	}

	private void updateYMD(YMD curymd) {
		if ((preymd.Month = curymd.Month - 1) < 0) {
			preymd.Month = 11;
			preymd.Year = curymd.Year - 1;
		} else {
			preymd.Year = curymd.Year;
		}
		if ((nexymd.Month = curymd.Month + 1) > 11) {
			nexymd.Month = 0;
			nexymd.Year = curymd.Year + 1;
		} else {
			nexymd.Year = curymd.Year;
		}
	}

	/* 更新黄历 */
	private void updateLunar(Day day) {
		/* update the month, year, day */
		ImageView picMonth1 = (ImageView) findViewById(R.id.picMonth1);
		ImageView picMonth2 = (ImageView) findViewById(R.id.picMonth2);
		ImageView picYear = (ImageView) findViewById(R.id.picYear);
		ImageView picDay1 = (ImageView) findViewById(R.id.picDay1);
		ImageView picDay2 = (ImageView) findViewById(R.id.picDay2);
		/* 设置月份 */
		if (lunarMonMap.containsKey(day.lunarmonth)) {
			int i = (Integer) lunarMonMap.get(day.lunarmonth);
			Log.i("TAG", "FLAG : updateLunar");
			if (i == 1) {
				picMonth1.setImageResource(android.R.color.transparent);
				picMonth2.setImageResource((Integer) otherPic[5]);
			} else if (i != 11) {
				picMonth1.setImageResource(android.R.color.transparent);
				picMonth2.setImageResource((Integer) lunarNumPic[i]);
			} else {
				picMonth1.setImageResource(R.drawable.cten);
				picMonth2.setImageResource(R.drawable.cone);
			}
		}
		/* 设置生肖 */
		if (lunarMonMap.containsKey(day.shengxiao)) {
			picYear.setImageResource((Integer) lunarMonMap.get(day.shengxiao));
		} else {
			Log.i("updateLunar", "sheng xiao error");
		}
		/* 设置农历日期 */
		if (lunarMonMap.containsKey(day.lunarday)) {
			int i = (Integer) lunarMonMap.get(day.lunarday);
			System.out.println(i);
			if (i <= 10) {
				picDay1.setImageResource(otherPic[1]);
				picDay2.setImageResource(lunarNumPic[i]);
			} else if (i < 20) {
				picDay1.setImageResource(lunarNumPic[10]);
				picDay2.setImageResource(lunarNumPic[i % 10]);
			} else if (i < 30) {
				picDay1.setImageResource(otherPic[0]);
				picDay2.setImageResource(lunarNumPic[i % 10]);
			} else {
				picDay1.setImageResource(lunarNumPic[i / 10]);
				picDay2.setImageResource(lunarNumPic[10]);
			}
		} else {
			Log.i("updateLunar", "day error");
		}

		/* 设置宜忌 */
		TextView textSuit = (TextView) findViewById(R.id.textSuit);
		textSuit.setText(day.suit);
		TextView textAvoid = (TextView) findViewById(R.id.textAvoid);
		textAvoid.setText(day.avoid);

		/* 设置公历日期 */
		ImageView picDayNum1 = (ImageView) findViewById(R.id.picDayNum1);
		ImageView picDayNum2 = (ImageView) findViewById(R.id.picDayNum2);
		if (day.number.equals("")) {
			gridview.setFocusable(false);
		} else {
			int numDay = Integer.parseInt(day.number);
			picDayNum1.setImageResource(BigNum[numDay / 10]);
			picDayNum2.setImageResource(BigNum[numDay % 10]);
		}

		/* 设置节日 */
		TextView textJieRi = (TextView) findViewById(R.id.textJieRi);
		StringBuilder sb = new StringBuilder();
		if (!day.lunarfestival.equals(""))
			sb.append(day.lunarfestival);
		if (!day.solarterm.equals(""))
			sb.append(" ").append(day.solarterm);
		if (!day.solarfestival.equals(""))
			sb.append(" ").append(day.solarfestival);
		textJieRi.setText(sb.toString());

		/* 设置天干 */
		TextView textTGMonth = (TextView) findViewById(R.id.textTGMonth);
		TextView textTGYear = (TextView) findViewById(R.id.textTGYear);
		TextView textTGDay = (TextView) findViewById(R.id.textTGDay);
		textTGMonth.setText(day.lunarmonthganzhi + "月");
		textTGYear.setText(day.lunaryearganzhi + "年");
		textTGDay.setText(day.lunardayganzhi + "日");

	}

	private int updateMonth(int curMonStarPos) {
		// System.out.println(curMonStarPos);
		if (curMonStarPos == -1) {
			return -1;
		}
		int i = curMonStarPos;

		Iterator<Day> preiterm = preMonth.iterator();
		Iterator<Day> curiterm = curMonth.iterator();
		Iterator<Day> nexiterm = nexMonth.iterator();

		Iterator<GridInfo> ilist = list.iterator();
		/* 跳过上月不显示内容 */
		for (int j = 0; j < preMonth.size() - i && preiterm.hasNext(); j++) {
			preiterm.next();
		}
		/* 设置上月显示内容 */
		for (int j = 0; j < i && ilist.hasNext() && preiterm.hasNext(); j++) {
			GridInfo gi = (GridInfo) ilist.next();
			Day day = (Day) preiterm.next();
			gi.setGC(day.number);
			gi.setLC(day.lunarday);
			if (!day.lunarfestival.equals("")) {
				gi.setMD(day.lunarfestival.split(" ")[0]);
			} else if (!day.solarterm.equals("")) {
				gi.setMD(day.solarterm.split(" ")[0]);
			} else {
				gi.setMD(day.solarfestival.split(" ")[0]);
			}

			if (Integer.parseInt(day.holiday) == 1) {
				gi.setColor(Color.RED);
			} else {
				gi.setColor(Color.GRAY);
			}
		}

		/* 设置本月显示内容 */
		while (curiterm.hasNext() && ilist.hasNext()) {
			GridInfo gi = (GridInfo) ilist.next();
			Day day = (Day) curiterm.next();
			gi.setGC(day.number);
			gi.setLC(day.lunarday);
			if (!day.lunarfestival.equals("")) {
				gi.setMD(day.lunarfestival.split(" ")[0]);
			} else if (!day.solarterm.equals("")) {
				gi.setMD(day.solarterm.split(" ")[0]);
			} else {
				gi.setMD(day.solarfestival.split(" ")[0]);
			}
			if (Integer.parseInt(day.holiday) == 1) {
				// Log.i("", "setColor");
				gi.setColor(Color.RED);
			} else {
				gi.setColor(Color.WHITE);
			}
		}
		/* 设置下月显示内容 */
		while (ilist.hasNext() && nexiterm.hasNext()) {
			GridInfo gi = (GridInfo) ilist.next();
			Day day = (Day) nexiterm.next();
			gi.setGC(day.number);
			gi.setLC(day.lunarday);
			if (!day.lunarfestival.equals("")) {
				gi.setMD(day.lunarfestival.split(" ")[0]);
			} else if (!day.solarterm.equals("")) {
				gi.setMD(day.solarterm.split(" ")[0]);
			} else {
				gi.setMD(day.solarfestival.split(" ")[0]);
			}
			if (Integer.parseInt(day.holiday) == 1) {
				gi.setColor(Color.RED);
			} else {
				gi.setColor(Color.GRAY);
			}
		}
		return i + curymd.Day - 1;
	}

	private synchronized int updatePreCurNex(int state) {
		// System.out.println(months.size());
		if (months.size() <= 0) {// 若moths中没有数据，返回-1
			return -1;
		}
		try {
			switch (state) {
			case 0:
				preMonth = months.get(0);
				curMonth = months.get(1);
				nexMonth = months.get(2);
				curMonInd = 1;
				break;
			case 1:// 向前移动一个月
				curMonInd++;
				preMonth = months.get(curMonInd - 1);
				curMonth = months.get(curMonInd);
				nexMonth = months.get(curMonInd + 1);
				break;
			case 2:// 向后移动一个月
				curMonInd--;
				preMonth = months.get(curMonInd - 1);
				curMonth = months.get(curMonInd);
				nexMonth = months.get(curMonInd + 1);
				break;
			}
		} catch (IndexOutOfBoundsException e) {
			Log.i("IndexExcep", String.valueOf(state));
		}

		// Day firstDay = curMonth.get(0);
		int i = Integer.parseInt(curMonth.get(0).week);
		return i;
	}

	private void setCurDayHightLight(int index) {
		ImageView spot = (ImageView) ((ViewGroup) gridview.getChildAt(index)).getChildAt(0);
		spot.setImageResource(R.drawable.spot);
		// TextView GC = (TextView) ((ViewGroup) ((ViewGroup)
		// gridview.getChildAt(index)).getChildAt(1)).getChildAt(0);
		// GC.setTextColor(0xFF00FFFF);
		GridInfo gi = list.get(index);
		gi.setColor(Color.YELLOW);
	}

	private void unsetCurDayHightLight(int index) {
		ImageView spot = (ImageView) ((ViewGroup) gridview.getChildAt(index)).getChildAt(0);
		spot.setImageResource(android.R.color.transparent);
		// TextView GC = (TextView) ((ViewGroup) ((ViewGroup)
		// gridview.getChildAt(index)).getChildAt(1)).getChildAt(0);
		// GC.setTextColor(Color.WHITE);
		GridInfo gi = list.get(index);
		gi.setColor(Color.WHITE);
	}

	/*
	 * @Override public boolean onCreateOptionsMenu(Menu menu) {
	 * getMenuInflater().inflate(R.menu.activity_main, menu); return true; }
	 */

	/*
	 * current year, month, day in integer value
	 */
	public class YMD {
		public int Year;
		public int Month;
		public int Day;

		public YMD() {

		}

		public YMD(int curY, int curM, int curD) {
			Year = curY;
			Month = curM;
			Day = curD;
		}
	}

	private class FocusXY {
		int X;
		int Y;
	}

	private void MapRes() {
		lunarMonMap.put("一", 1);
		lunarMonMap.put("二", 2);
		lunarMonMap.put("三", 3);
		lunarMonMap.put("四", 4);
		lunarMonMap.put("五", 5);
		lunarMonMap.put("六", 6);
		lunarMonMap.put("七", 7);
		lunarMonMap.put("八", 8);
		lunarMonMap.put("九", 9);
		lunarMonMap.put("十", 10);
		lunarMonMap.put("腊", 12);
		lunarMonMap.put("廿", 20);// nian 四声
		lunarMonMap.put("初", 13);
		lunarMonMap.put("十一", 11);
		lunarMonMap.put("十二", 12);
		lunarMonMap.put("十三", 13);
		lunarMonMap.put("十四", 14);
		lunarMonMap.put("十五", 15);
		lunarMonMap.put("十六", 16);
		lunarMonMap.put("十七", 17);
		lunarMonMap.put("十八", 18);
		lunarMonMap.put("十九", 19);
		lunarMonMap.put("二十", 20);
		lunarMonMap.put("廿一", 21);
		lunarMonMap.put("廿二", 22);
		lunarMonMap.put("廿三", 23);
		lunarMonMap.put("廿四", 24);
		lunarMonMap.put("廿五", 25);
		lunarMonMap.put("廿六", 26);
		lunarMonMap.put("廿七", 27);
		lunarMonMap.put("廿八", 28);
		lunarMonMap.put("廿九", 29);
		lunarMonMap.put("三十", 30);
		lunarMonMap.put("初一", 1);
		lunarMonMap.put("初二", 2);
		lunarMonMap.put("初三", 3);
		lunarMonMap.put("初四", 4);
		lunarMonMap.put("初五", 5);
		lunarMonMap.put("初六", 6);
		lunarMonMap.put("初七", 7);
		lunarMonMap.put("初八", 8);
		lunarMonMap.put("初九", 9);
		lunarMonMap.put("初十", 10);

		lunarMonMap.put("鼠", R.drawable.mouse);
		lunarMonMap.put("龙", R.drawable.dragon);
		lunarMonMap.put("牛", R.drawable.ox);
		lunarMonMap.put("虎", R.drawable.tiger);
		lunarMonMap.put("兔", R.drawable.rabbit);
		lunarMonMap.put("蛇", R.drawable.snake);
		lunarMonMap.put("马", R.drawable.horse);
		lunarMonMap.put("羊", R.drawable.sheep);
		lunarMonMap.put("猴", R.drawable.monkey);
		lunarMonMap.put("鸡", R.drawable.chicken);
		lunarMonMap.put("狗", R.drawable.dog);
		lunarMonMap.put("猪", R.drawable.pig);

		lunarMonMap.put("周", 2);
		lunarMonMap.put("冬", 3);
		lunarMonMap.put("小", 4);
		lunarMonMap.put("正", 1);
	}

	public int[] lunarNumPic = { 0, R.drawable.cone, R.drawable.ctwo, R.drawable.cthree, R.drawable.cfour,
			R.drawable.cfive, R.drawable.csix, R.drawable.cseven, R.drawable.ceight, R.drawable.cnine, R.drawable.cten,
			0, R.drawable.la, };

	public int[] otherPic = { R.drawable.twenty, R.drawable.ori, R.drawable.week, R.drawable.winter, R.drawable.xiao,
			R.drawable.zheng };

	public int[] BigNum = { R.drawable.bzero, R.drawable.bone, R.drawable.btwo, R.drawable.bthree, R.drawable.bfour,
			R.drawable.bfive, R.drawable.bsix, R.drawable.bseven, R.drawable.beight, R.drawable.bnine };
	public int[] Minute = { R.drawable.zero, R.drawable.one, R.drawable.two, R.drawable.three, R.drawable.four,
			R.drawable.five, R.drawable.six, R.drawable.seven, R.drawable.eight, R.drawable.nine };

	/*
	 * 需要额外定义一个属性动画的原因是因为如果只应用自定义的Rotation3DAnimation， 那么两个view不会更新 原因待查
	 */
	private void animationAtStart() {
		if (true) {
			animation_completed = false;
			AnimatorSet set1 = new AnimatorSet();
			set1.playTogether(ObjectAnimator.ofFloat(mContainer1, "alpha", 0.6f, 1.0f),
					ObjectAnimator.ofFloat(mContainer2, "alpha", 0.6f, 1.0f));
			set1.setDuration(duration_start);
			set1.start();
			set1.addListener(new AnimatorListenerAdapter() {

				public void onAnimationStart(Animator animation) {
					// TODO Auto-generated method stub

				}

				public void onAnimationRepeat(Animator animation) {
					// TODO Auto-generated method stub

				}

				public void onAnimationEnd(Animator animation) {
					// TODO Auto-generated method stub
					animation_completed = true;
					new Thread(m).start();// 将日历的更新放到动画启动的后面是为了加快界面的显示和动画的启动
					new Thread(CTR).start();
				}

				public void onAnimationCancel(Animator animation) {
					// TODO Auto-generated method stub

				}
			});
		}
		applyRotation(false);
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		super.finish();
		overridePendingTransition(R.anim.scalesmall_alpha_in, R.anim.scalesmall_alpha_out);
	}

	private void animationAtFinish() {
		if (true) {
			animation_completed = false;
			AnimatorSet set1 = new AnimatorSet();
			set1.playTogether(ObjectAnimator.ofFloat(mContainer1, "alpha", 1.0f, 0.5f),
					ObjectAnimator.ofFloat(mContainer2, "alpha", 1.0f, 0.5f));
			set1.setDuration(duration_finish);

			set1.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					// TODO Auto-generated method stub
					// android.os.Process.killProcess(android.os.Process.myPid());
					finish();// 结束activity
				}
			});
			set1.start();
		}
		applyRotation(true);
	}

	private void applyRotation(boolean reverse) {
		if (as != null) {
			as.cancel();
			as = null;
		}
		if (rotation1 != null) {
			rotation1.cancel();
			rotation1 = null;
		}
		if (rotation2 != null) {
			rotation2.cancel();
			rotation2 = null;
		}
		long duration = reverse ? duration_finish : duration_start;

		// 计算中心点
		final float centerX = mContainer1.getWidth();
		final float centerY = mContainer1.getHeight() / 2.0f;
		final float fromX = getResources().getDimension(R.dimen.rotate_animation_from_x);
		final float fromY = getResources().getDimension(R.dimen.rotate_animation_from_y);

		// 注意，这里的x,y的起点终点移动距离与屏幕像素坐标不一样，因为有深度改变，所以x, y的值要偏大,而且完全靠感觉调整

		rotation1 = new RotateY3dAnimation(centerX, centerY, 0, 0, fromX, 0, fromY, 0, 5000, reverse);
		rotation1.setDuration(duration);
		rotation1.setFillAfter(true);
		mContainer1.setAnimation(rotation1);

		rotation2 = new RotateY3dAnimation(0, centerY, -180, 0, fromX, 0, fromY, 0, 5000, reverse);

		rotation2.setDuration(duration);
		rotation2.setFillAfter(true);
		mContainer2.setAnimation(rotation2);

		as = new AnimationSet(true);
		as.addAnimation(rotation2);
		as.addAnimation(rotation1);
		as.setDuration(duration);

		if (DEBUG)
			Log.d("Camera-MainActivity", "启动动画");
		as.startNow();
	}

	private int compareDays(Day firstDay, Day secondDay) {
		int year1, year2, month1, month2, day1, day2;

		if (firstDay.lunaryear.equals(""))
			return 0;
		else
			year1 = Integer.parseInt(firstDay.lunaryear);
		if (secondDay.lunaryear.equals(""))
			return 0;
		else
			year2 = Integer.parseInt(secondDay.lunaryear);
		if (firstDay.lunarmonth.equals(""))
			return 0;
		else
			month1 = (Integer) lunarMonMap.get(firstDay.lunarmonth);
		if (secondDay.lunarmonth.equals(""))
			return 0;
		else
			month2 = (Integer) lunarMonMap.get(secondDay.lunarmonth);
		if (secondDay.lunarday.equals(""))
			return 0;
		else
			day1 = (Integer) lunarMonMap.get(firstDay.lunarday);
		if (secondDay.lunarday.equals(""))
			return 0;
		else
			day2 = (Integer) lunarMonMap.get(secondDay.lunarday);

		if (year1 != year2)
			return year1 - year2;
		else if (month1 != month2)
			return month1 - month2;
		else
			return day1 - day2;
	}
}
