package com.turingcat.vstclib;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import vstc2.nativecaller.NativeCaller;


/**
 * Created by xwh on 2019/1/7.
 */
public class VstcApi {
	private static final String TAG = "|IPC| ";
	private static boolean isDoing = false;

	public static void startService(Context context) {
		context.startService(new Intent(context, BridgeService.class));
	}

	public static void stopService(Context context) {
		context.stopService(new Intent(context, BridgeService.class));
	}

	/**
	 * 提供的jni NativeCaller没有完整回调，只能通过隔一段时间去检测的方式去获取扫描结果
	 * 注意：在子线程中调用
	 */
	public static void startSearch(final Context context, final OnSearchListener onSearchListener) {
		if (isDoingSomething()) {
			return;
		}
		isDoing = true;
		lastTime = System.currentTimeMillis();

		startService(context);
		final List<VstcCamera> mCameras = new ArrayList<>();

		// 单次结果回调
		BridgeService.setAddCameraInterface(new BridgeService.AddCameraInterface() {
			@Override
			public void callBackSearchResultData(int cameraType, String strMac, String strName, String strDeviceID, String strIpAddr, int port) {
				VstcCamera camera = new VstcCamera();
				camera.deviceId = strDeviceID;
				camera.cameraName = strName;
				camera.cameraType = cameraType;
				camera.ip = strIpAddr;
				if (!mCameras.contains(camera)) {
					mCameras.add(camera);
				}
			}
		});

		NativeCaller.StartSearch();

		final Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			int count;

			@Override
			public void run() {
				count++;
				if (mCameras.size() > 0 || count > 3) {
					isDoing = false;
					timer.cancel();
					NativeCaller.StopSearch();
					BridgeService.setAddCameraInterface(null);
					stopService(context);
					onSearchListener.onResult(mCameras);
				}
				//Log.d("VstcApi", "isDoing : " + count);
			}
		}, 2000, 1000);
	}


	public static void connectCamera(final String strDID, final String devicePass, final OnConnectListener onConnectListener) {
		if (isDoingSomething()) {
			return;
		}
		//NativeCaller.Free();
		isDoing = true;
		lastTime = System.currentTimeMillis();
		Log.d(TAG, "开始连接");
		BridgeService.setIpcamClientInterface(new BridgeService.IpcamClientInterface() {
			@Override
			public void BSMsgNotifyData(String did, int type, int param) {

				Log.d(TAG, "type:" + type + " param:" + param);
				isDoing = false;
				if (type == ContentCommon.PPPP_MSG_TYPE_PPPP_STATUS) {    // 在线
					if (param == ContentCommon.PPPP_STATUS_CONNECTING || param == ContentCommon.PPPP_STATUS_INITIALING){
						Log.d(TAG, "连接中");
					} else if (param == ContentCommon.PPPP_STATUS_ON_LINE) {
						Log.d(TAG, "连接成功");
					} else {
						Log.d(TAG, "连接失败: " + param);
					}

					onConnectListener.onResult(param);
				}


			}

			@Override
			public void BSSnapshotNotify(String did, byte[] bImage, int len) {}

			@Override
			public void callBackUserParams(String did, String user1, String pwd1, String user2, String pwd2, String user3, String pwd3) {}

			@Override
			public void CameraStatus(String did, int status) {}
		});

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		NativeCaller.Init();

		NativeCaller.PPPPInitialOther("ADCBBFAOPPJAHGJGBBGLFLAGDBJJHNJGGMBFBKHIBBNKOKLDHOBHCBOEHOKJJJKJBPMFLGCPPJMJAPDOIPNL");
		//NativeCaller.SetAPPDataPath(context.getApplicationContext().getFilesDir().getAbsolutePath());
		startCameraPPPP(strDID, "admin", devicePass);

	}


	private static void startCameraPPPP(String deviceId, String deviceName, String pwd) {
		try {
			Thread.sleep(100);
		} catch (Exception e) {
		}

		if (deviceId.toLowerCase().startsWith("vsta")) {
			NativeCaller.StartPPPPExt(deviceId, deviceName,
					pwd, 1, "", "EFGFFBBOKAIEGHJAEDHJFEEOHMNGDCNJCDFKAKHLEBJHKEKMCAFCDLLLHAOCJPPMBHMNOMCJKGJEBGGHJHIOMFBDNPKNFEGCEGCBGCALMFOHBCGMFK", 0);
		} else if (deviceId.toLowerCase().startsWith("vstd")) {
			NativeCaller.StartPPPPExt(deviceId, deviceName,
					pwd, 1, "", "HZLXSXIALKHYEIEJHUASLMHWEESUEKAUIHPHSWAOSTEMENSQPDLRLNPAPEPGEPERIBLQLKHXELEHHULOEGIAEEHYEIEK-$$", 1);
		} else if (deviceId.toLowerCase().startsWith("vstf")) {
			NativeCaller.StartPPPPExt(deviceId, deviceName,
					pwd, 1, "", "HZLXEJIALKHYATPCHULNSVLMEELSHWIHPFIBAOHXIDICSQEHENEKPAARSTELERPDLNEPLKEILPHUHXHZEJEEEHEGEM-$$", 1);
		} else if (deviceId.toLowerCase().startsWith("vste")) {
			NativeCaller.StartPPPPExt(deviceId, deviceName,
					pwd, 1, "", "EEGDFHBAKKIOGNJHEGHMFEEDGLNOHJMPHAFPBEDLADILKEKPDLBDDNPOHKKCIFKJBNNNKLCPPPNDBFDL", 0);
		} else if (deviceId.toLowerCase().startsWith("vstg")) {
			NativeCaller.StartPPPPExt(deviceId, deviceName,
					pwd, 1, "", "EEGDFHBOKCIGGFJPECHIFNEBGJNLHOMIHEFJBADPAGJELNKJDKANCBPJGHLAIALAADMDKPDGOENEBECCIK:vstarcam2018", 0);
		} else if (deviceId.toLowerCase().startsWith("vstb") || deviceId.toLowerCase().startsWith("vstc")) {
			NativeCaller.StartPPPPExt(deviceId, deviceName,
					pwd, 1, "", "ADCBBFAOPPJAHGJGBBGLFLAGDBJJHNJGGMBFBKHIBBNKOKLDHOBHCBOEHOKJJJKJBPMFLGCPPJMJAPDOIPNL", 0);
		} else {
			NativeCaller.StartPPPPExt(deviceId, deviceName,
					pwd, 1, "", "", 0);
		}
	}

	private static long lastTime;
	private static boolean isDoingSomething() {
		long now = System.currentTimeMillis();
		if (now - lastTime > 10000) {
			isDoing = false;
		}
		return isDoing;
	}


	public static class VstcCamera {
		public String deviceId;
		public int cameraType;
		public String cameraName;
		public String ip;

		@Override
		public boolean equals(Object obj) {
			return obj instanceof VstcCamera
					&& this.deviceId.equals(((VstcCamera) obj).deviceId);
		}
	}

	public interface OnSearchListener {
		void onResult(List<VstcCamera> cameraList);
	}

	public interface OnConnectListener {
		void onResult(int code);
	}

}
