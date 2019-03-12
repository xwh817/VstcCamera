package com.turingcat.vstclib;


import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class AudioPlayer {

	private final static String LOG_TAG = "AudioPlayer" ;

	CustomBuffer audioBuffer;
	CustomBuffer audioReused;
	private boolean bAudioPlaying = false;
	private Thread audioThread = null;
	private AudioTrack m_AudioTrack = null;

	public AudioPlayer(CustomBuffer buffer) {
		// TODO Auto-generated constructor stub
		audioBuffer = buffer;
		audioReused = new CustomBuffer();
	}

	public boolean isAudioPlaying() {
		return bAudioPlaying;
	}

	public boolean AudioPlayStart() {
		synchronized (this) {
			if (bAudioPlaying) {
				return true;
			}
			bAudioPlaying = true;
			audioThread = new Thread(new AudioPlayThread());
			audioThread.start();
		}
		return true;
	}

	public void AudioPlayStop() {
		synchronized (this) {
			if (!bAudioPlaying || audioThread == null) {
				return;
			}

			bAudioPlaying = false;
			try {
				audioThread.join();
			} catch (Exception e) {
				// TODO: handle exception
			}
			audioThread = null;
		}
	}

	@SuppressWarnings("deprecation")
	public boolean initAudioDev() {

		int channelConfig;
		int audioFormat = 2;
		int mMinBufSize = 0;

		// channelConfig = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
		channelConfig = AudioFormat.CHANNEL_OUT_MONO;
		audioFormat = AudioFormat.ENCODING_PCM_16BIT;
		mMinBufSize = AudioTrack.getMinBufferSize(8000, channelConfig,
				audioFormat);
		System.out.println("--audio, mMinBufSize=" + mMinBufSize);

		if (mMinBufSize == AudioTrack.ERROR_BAD_VALUE
				|| mMinBufSize == AudioTrack.ERROR) {
			System.out.println("初始化失败");
			return false;

		}

		try {
			m_AudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 8000,
					channelConfig, audioFormat, mMinBufSize * 2,
					AudioTrack.MODE_STREAM);
		} catch (IllegalArgumentException iae) {
			iae.printStackTrace();
			return false;
		}

		m_AudioTrack.play();
		return true;
	}

	class AudioPlayThread implements Runnable {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if (!initAudioDev()) {
				return;
			}

			while (bAudioPlaying) {
				CustomBufferData data = audioBuffer.RemoveData();
				if (data == null) {
					try {
						Thread.sleep(10);
						continue;
					} catch (Exception e) {
						// TODO: handle exception
						m_AudioTrack.stop();
						return;
					}
				}
				Log.d(LOG_TAG, "audioBuffer:" + audioBuffer.getSize() + ", audioReused:" + audioReused.getSize());
				m_AudioTrack.write(data.data, 0, data.length);
				data.data = null;
				audioReused.addData(data);  // 用完的数据先存下来后面复用，不要每次都去new新的会造成很多垃圾。
			}

			m_AudioTrack.stop();
			m_AudioTrack.release();
			audioReused.ClearAll();
			m_AudioTrack = null;
		}

	}

	public CustomBufferData obtainData() {
		CustomBufferData bufferData = audioReused.RemoveData();
		if (bufferData == null) {
			bufferData = new CustomBufferData();
		}
		return bufferData;
	}
}