
package com.alios.rtsp.server;

import java.util.Locale;

import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.rtsp.RTSPService;
import net.majorkernelpanic.streaming.video.VideoQuality;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mundoglass.worldglass.R;

public class CameraServerActivity extends Activity {

	public final static String TAG = "CameraActivity";

	private RelativeLayout mRelativeLayout;
	private SurfaceView mSurfaceView;
	private PowerManager.WakeLock mWakeLock;
	private Boolean recording = false;

	private TextView mTextView;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);

		// Getting layout
		mRelativeLayout = (RelativeLayout) findViewById(R.id.camera_activity);
		// Create gesture detector
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		mSurfaceView = (SurfaceView) findViewById(R.id.surface);

		mTextView = (TextView) findViewById(R.id.server_info);

		// Configures the SessionBuilder
		SessionBuilder.getInstance()
		        .setContext(getApplicationContext())
		        .setSurfaceHolder(mSurfaceView.getHolder())
		        .setContext(getApplicationContext())
		        .setVideoQuality(VideoQuality.DEFAULT_VIDEO_QUALITY)
		        .setAudioEncoder(SessionBuilder.AUDIO_AAC)
		        .setVideoEncoder(SessionBuilder.VIDEO_H264);

		// Starts the service of the RTSP server
		this.startService(new Intent(this, CustomRTSPService.class));

		// Prevents the phone from going to sleep mode
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK,
		        "net.majorkernelpanic.example3.wakelock");

		mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {

			@Override
			public void surfaceCreated(SurfaceHolder holder) {
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				Log.i(TAG, "surfaceChanged()");
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				Log.i(TAG, "surfaceDestroyed()");
			}

		});

	}

	@Override
	public void onStart() {
		super.onStart();
		// Lock screen
		mWakeLock.acquire();
		bindService(new Intent(this, CustomRTSPService.class), mRTSPServiceConnection,
		        Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onStop() {
		Log.i(TAG, "onStop()");
		// Unlock screen
		if (mWakeLock.isHeld())
			mWakeLock.release();
		// Setting recording state to disabled
		recording = false;

		if (mRtspServer != null)
			mRtspServer.removeCallbackListener(mRtspCallbackListener);
		unbindService(mRTSPServiceConnection);
		this.stopService(new Intent(this, CustomRTSPService.class));
		super.onStop();
	}

	@Override
	protected void onPause() {
		// Unlock screen
		if (mWakeLock.isHeld())
			mWakeLock.release();
		// Setting recording state to disabled
		recording = false;
		super.onPause();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean result = super.onTouchEvent(event);
		mRelativeLayout.playSoundEffect(SoundEffectConstants.CLICK);
		if (recording == false) {
			if (mRtspServer != null) {
				mRtspServer.start();
			}
			recording = true;
		} else {
			if (mRtspServer != null) {
				mRtspServer.stop();
			}
			recording = false;
		}
		return result;
	}

	private RTSPService mRtspServer;

	private void info() {
		WifiManager wifiManager = (WifiManager) getApplicationContext()
		        .getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifiManager.getConnectionInfo();
		if (info != null && info.getNetworkId() > -1) {
			int i = info.getIpAddress();
			String ip = String.format(Locale.ENGLISH, "%d.%d.%d.%d", i & 0xff, i >> 8 & 0xff,
			        i >> 16 & 0xff, i >> 24 & 0xff);
			mTextView.setText("rtsp://");
			mTextView.append(ip);
			mTextView.append(":" + mRtspServer.getPort());
		}
	}

	private ServiceConnection mRTSPServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mRtspServer = (CustomRTSPService) ((RTSPService.LocalBinder) service).getService();
			mRtspServer.addCallbackListener(mRtspCallbackListener);
			mRtspServer.start();
			recording = true;
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					info();
				}
			});
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
		}

	};

	private RTSPService.CallbackListener mRtspCallbackListener = new RTSPService.CallbackListener() {

		@Override
		public void onError(RTSPService server, Exception e, int error) {
			// We alert the user that the port is already used by another app.
			if (error == RTSPService.ERROR_BIND_FAILED) {
				Log.e(TAG, "=========== ERROR_BIND_FAILED =======");
			}
		}

		@Override
		public void onMessage(RTSPService server, int message) {
			if (message == RTSPService.MESSAGE_STREAMING_STARTED) {
				Log.e(TAG, "=========== MESSAGE_STREAMING_STARTED =======");
			} else if (message == RTSPService.MESSAGE_STREAMING_STOPPED) {
				Log.e(TAG, "=========== MESSAGE_STREAMING_STOPPED =======");
			}
		}

	};

}
