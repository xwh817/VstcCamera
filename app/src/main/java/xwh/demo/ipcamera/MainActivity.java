package xwh.demo.ipcamera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.turingcat.vstclib.ContentCommon;
import com.turingcat.vstclib.VstcApi;

import vstc2.nativecaller.NativeCaller;


public class MainActivity extends Activity {

	private TextView mTextInfo;

	private int connectCode = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mTextInfo = this.findViewById(R.id.text_info);

		this.findViewById(R.id.bt_search).setOnClickListener(v -> {
			mTextInfo.setText("正在扫描摄像头……");
			new Thread(() -> {
				VstcApi.startSearch(this, cameraList -> {
					mTextInfo.post(() -> {
						mTextInfo.setText("扫描结果：");
						for (VstcApi.VstcCamera result : cameraList) {
							mTextInfo.append("\n" + result.deviceId + "  " + result.ip);
						}
					});
				});
			}).start();
		});

		this.findViewById(R.id.bt_connect).setOnClickListener(v -> {
			mTextInfo.setText("正在连接……");

			VstcApi.startService(this);

			new Thread(() -> {
				VstcApi.connectCamera("VSTF050962LSAZZ", "123456aa", code -> {
					connectCode = code;

						mTextInfo.post(() -> {
							if (code == ContentCommon.PPPP_STATUS_CONNECTING || code == ContentCommon.PPPP_STATUS_INITIALING){
								mTextInfo.append("\n连接中 " + code);
							} else if (code == ContentCommon.PPPP_STATUS_ON_LINE) {
								mTextInfo.append("\n连接成功");
							} else if (code == ContentCommon.PPPP_STATUS_INVALID_ID || code == ContentCommon.PPPP_STATUS_CONNECT_ERRER) {
								mTextInfo.append("\n密码错误");
							} else {
								mTextInfo.append("\n连接失败 " + code);
							}
						});

						if (code >= ContentCommon.PPPP_STATUS_ON_LINE) {
							VstcApi.stopService(this);
							NativeCaller.Free();
						}
					});
			}).start();

			mTextInfo.postDelayed(() -> {
				if (connectCode == -1) {
					mTextInfo.append("\n连接失败");
					VstcApi.stopService(this);
					NativeCaller.Free();
				}
			}, 5000);
		});


		this.findViewById(R.id.bt_play).setOnClickListener(v -> {
			startActivity(new Intent(MainActivity.this, PlayActivity.class));
		});

	}

}
