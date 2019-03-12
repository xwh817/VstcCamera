package xwh.demo.ipcamera;

import android.app.Activity;
import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.turingcat.vstclib.AudioPlayer;
import com.turingcat.vstclib.BridgeService;
import com.turingcat.vstclib.ContentCommon;
import com.turingcat.vstclib.CustomBuffer;
import com.turingcat.vstclib.CustomBufferData;
import com.turingcat.vstclib.MyRender;
import com.turingcat.vstclib.VstcApi;

import vstc2.nativecaller.NativeCaller;

public class PlayActivity extends Activity implements BridgeService.PlayInterface {

	private static final String TAG = "IPCamera";
	private static final int AUDIO_BUFFER_START_CODE = 0xff00ff;
	//surfaceView控件
	private GLSurfaceView mSurfaceView = null;

	int mVideoWidth;
	int mVideoHeight;

	private String strDID = "VSTF050962LSAZZ";

	private boolean bDisplayFinished = true;
	private CustomBuffer mAudioBuffer = null;
	private AudioPlayer mAudioPlayer = null;
	private MyRender myRender;

	private Handler msgHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				Log.d(TAG, "断线了");
				finish();
			}
		}
	};

	private void setSurfaceSize(int width, int height) {

		mVideoWidth = width;
		mVideoHeight = height;
		int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
		int screenHeight = getWindowManager().getDefaultDisplay().getHeight();

		int surfaceWidth;
		int surfaceHeight;
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			surfaceWidth = screenWidth;
			surfaceHeight = screenWidth * height / width;
		} else {
			surfaceWidth = screenHeight;
			surfaceHeight = screenHeight * width / height;
		}
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
				surfaceWidth, surfaceHeight);
		lp.gravity = Gravity.CENTER;
		mSurfaceView.setLayoutParams(lp);

		Log.d(TAG, "setSurfaceSize: " + surfaceWidth + ", " + surfaceHeight);
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.layout_player);

		mSurfaceView = findViewById(R.id.mysurfaceview);
		myRender = new MyRender(mSurfaceView);
		mAudioBuffer = new CustomBuffer();
		mAudioPlayer = new AudioPlayer(mAudioBuffer);

		VstcApi.startService(this);
		BridgeService.setPlayInterface(this);

		new Thread(() -> {
			VstcApi.connectCamera("VSTF050962LSAZZ", "123456aa", code -> {
				if (code == ContentCommon.PPPP_STATUS_ON_LINE) {    // 在线
					Log.d(TAG, "连接成功，开始播放");
					NativeCaller.StartPPPPLivestream(strDID, 10, 1);//确保不能重复start
					// 播放声音
					StartAudio();
				}
			});
		}).start();

		// 手势
		this.gestureDetector = new GestureDetector(this, this.onGestureListener);


		mSurfaceView.setOnTouchListener((v, event) -> {
			return this.gestureDetector.onTouchEvent(event);
		});
	}


	private final int MINLEN = 80;//最小间距
	private GestureDetector gestureDetector;

	// 手势事件处理
	private OnGestureListener onGestureListener = new OnGestureListener() {

		@Override
		public boolean onDown(MotionEvent e) {
			return false;
		}

		@Override
		public void onShowPress(MotionEvent e) {

		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			return false;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			return false;
		}

		@Override
		public void onLongPress(MotionEvent e) {

		}

		/**
		 * 快速滑动
		 */
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			if (e1 == null || e2 == null) {
				return false;
			}

			float x1 = e1.getX();
			float x2 = e2.getX();
			float y1 = e1.getY();
			float y2 = e2.getY();

			float dx = x1 > x2 ? x1 - x2 : x2 - x1;
			float dy = y1 > y2 ? y1 - y2 : y2 - y1;

			if (dx > dy) {
				if ((x1 > x2) && (dx > MINLEN)) {// right
					cameraMove(ContentCommon.CMD_PTZ_RIGHT);
				} else if ((x1 < x2) && (dx > MINLEN)) {// left
					cameraMove(ContentCommon.CMD_PTZ_LEFT);
				}
			} else {
				if ((y1 > y2) && (dy > MINLEN)) {// down
					cameraMove(ContentCommon.CMD_PTZ_DOWN);
				} else if ((y1 < y2) && (dy > MINLEN)) {// up
					cameraMove(ContentCommon.CMD_PTZ_UP);
				}
			}
			return false;
		}
	};


	private int controlOrientation;

	private void cameraMove(int orientation) {
		this.controlOrientation = orientation;

		new Thread(() -> {
			try {
				NativeCaller.PPPPPTZControl(strDID, controlOrientation);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
		;
	}

	@Override
	protected void onDestroy() {
		NativeCaller.StopPPPPLivestream(strDID);
		StopAudio();

		BridgeService.setPlayInterface(null);
		VstcApi.stopService(this);
		NativeCaller.Free();

		super.onDestroy();
	}

	/***
	 * BridgeService callback 视频参数回调
	 *
	 * **/
	@Override
	public void callBackCameraParamNotify(String did, int resolution,
	                                      int brightness, int contrast, int hue, int saturation, int flip, int mode) {
		Log.e(TAG, "设备返回的参数" + resolution + "," + brightness + "," + contrast + "," + hue + "," + saturation + "," + flip + "," + mode);
		//bInitCameraParam = true;
	}

	/***
	 * BridgeService callback 视频数据流回调
	 *
	 * **/
	@Override
	public void callBackVideoData(byte[] videobuf, int h264Data, int len, int width, int height) {
		//Log.d(TAG, "底层返回数据" + "videobuf:" + videobuf + "--" + "h264Data" + h264Data + "len" + len + "width" + width + "height" + height);
		if (!bDisplayFinished) {
			return;
		}

		if (h264Data == 1) {
			bDisplayFinished = false;
			if (width != mVideoWidth || height != mVideoHeight) { // 如果发生变化才改变
				msgHandler.post(() -> {
					setSurfaceSize(width, height);
				});
			}
			myRender.writeSample(videobuf, width, height);
			bDisplayFinished = true;
		}

	}

	/***
	 * BridgeService callback
	 *
	 * **/
	@Override
	public void callBackMessageNotify(String did, int msgType, int param) {
		Log.d(TAG, "MessageNotify did: " + did + " msgType: " + msgType + " param: " + param);

		if (msgType == ContentCommon.PPPP_MSG_TYPE_STREAM) {
			return;
		}

		if (msgType != ContentCommon.PPPP_MSG_TYPE_PPPP_STATUS) {
			return;
		}

		if (!did.equals(strDID)) {
			return;
		}

		Message msg = new Message();
		msg.what = 1;
		msgHandler.sendMessage(msg);
	}


	/***
	 * BridgeService callback
	 *
	 * **/
	@Override
	public void callBackAudioData(byte[] pcm, int len) {
		//Log.d(TAG, "AudioData: len :+ " + len);
		if (!mAudioPlayer.isAudioPlaying()) {    // 如果静音，不处理
			return;
		}

		CustomBufferData data = mAudioPlayer.obtainData();
		data.length = len;
		data.data = pcm;
		mAudioBuffer.addData(data);
	}

	/***
	 * BridgeService callback
	 *
	 * **/
	@Override
	public void callBackH264Data(byte[] h264, int type, int size) {
		Log.d(TAG, "CallBack_H264Data" + " type:" + type + " size:" + size);
	}

	// 播放声音
	private void StartAudio() {
		synchronized (this) {
			mAudioBuffer.ClearAll();
			mAudioPlayer.AudioPlayStart();
			NativeCaller.PPPPStartAudio(strDID);
		}
	}

	// 静音
	private void StopAudio() {
		synchronized (this) {
			mAudioPlayer.AudioPlayStop();
			mAudioBuffer.ClearAll();
			NativeCaller.PPPPStopAudio(strDID);
		}
	}

}
