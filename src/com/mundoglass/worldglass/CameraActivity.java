
package com.mundoglass.worldglass;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.rtsp.RTSPClient;
import net.majorkernelpanic.streaming.video.VideoQuality;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.RelativeLayout;
import android.widget.Toast;

/**
 * @author ander.martinez@mundoglass.es based on
 *         https://github.com/fyhertz/libstreaming
 * @see www.mundoglass.es
 */
public class CameraActivity extends Activity {

	public final static String TAG = "CameraActivity";

	private final static VideoQuality QUALITY_GLASS = new VideoQuality(352, 288, 60, 384000); // wifi
	String user = "username";
	String password = "password";
	String url = "rtsp://opendev.mundoglass.es:1935/live/test.sdp";

	private VideoQuality mQuality = QUALITY_GLASS;

	private RelativeLayout mRelativeLayout;
	private SurfaceView mSurfaceView;
	private Session mSession;
	private PowerManager.WakeLock mWakeLock;
	private RTSPClient mClient;
	private Boolean recording = false;

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

		// Configures the SessionBuilder
		SessionBuilder sBuilder = SessionBuilder.getInstance()
		        .setContext(getApplicationContext())
		        .setSurfaceHolder(mSurfaceView.getHolder())
		        .setContext(getApplicationContext())
		        .setVideoQuality(QUALITY_GLASS)
		        .setAudioEncoder(SessionBuilder.AUDIO_AAC)
		        .setVideoEncoder(SessionBuilder.VIDEO_H264);

		// Configures the RTSP client
		mClient = new RTSPClient();

		// Creates the Session
		try {
			mSession = sBuilder.build();
			mClient.setSession(mSession);
		} catch (Exception e) {
			logError(e.getMessage());
			e.printStackTrace();
		}

		// Prevents the phone from going to sleep mode
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK,
		        "net.majorkernelpanic.example3.wakelock");

		mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {

			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				if (mSession != null) {
					try {
						if (mSession.getVideoTrack() != null) {
							mSession.getVideoTrack().setVideoQuality(mQuality);

							// Start streaming
							new ToggleStreamAsyncTask().execute();

						}
					} catch (RuntimeException e) {
						logError(e.getMessage());
						e.printStackTrace();
					}
				}
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
	}

	@Override
	public void onStop() {
		Log.i(TAG, "onStop()");
		// Unlock screen
		if (mWakeLock.isHeld())
			mWakeLock.release();
		// Setting recording state to disabled
		recording = false;
		mSession.flush();

		// Stops the stream and disconnects from the RTSP server
		mClient.stopStream();

		super.onStop();
	}

	@Override
	protected void onPause() {

		// Stops the stream and disconnects from the RTSP server
		mClient.stopStream();

		// Unlock screen
		if (mWakeLock.isHeld())
			mWakeLock.release();
		// Setting recording state to disabled
		recording = false;

		mSession.flush();

		super.onPause();
	}

	// Connects/disconnects to the RTSP server and starts/stops the stream
	private class ToggleStreamAsyncTask extends AsyncTask<Void, Void, Integer> {

		private final int START_SUCCEEDED = 0x00;
		private final int START_FAILED = 0x01;
		private final int STOP = 0x02;

		@Override
		protected Integer doInBackground(Void... params) {
			if (!mClient.isStreaming()) {
				String ip, port, path;
				try {
					// We parse the URI written in the Editext
					Pattern uri = Pattern.compile("rtsp://(.+):(\\d+)/(.+)");
					Matcher m = uri.matcher(url);
					m.find();

					ip = m.group(1);
					port = m.group(2);
					path = m.group(3);

					// Connection to the RTSP server
					if (mSession.getVideoTrack() != null) {
						mSession.getVideoTrack().setVideoQuality(mQuality);
					}
					mClient.setCredentials(user, password);
					mClient.setServerAddress(ip, Integer.parseInt(port));
					mClient.setStreamPath("/" + path);
					mClient.startStream(1);

					// Init recording flag
					recording = true;

					return START_SUCCEEDED;
				} catch (Exception e) {
					Log.e(TAG, "Error starting streaiming.", e);
					Toast.makeText(getApplicationContext(), e.getLocalizedMessage(),
					        Toast.LENGTH_SHORT).show();
					return START_FAILED;
				}
			} else {
				// Stops the stream and disconnects from the RTSP server
				mClient.stopStream();
				// Setting recording state to disabled
				recording = false;
				Log.i(TAG, "*** Recording stopStream()");
				finish();
			}
			return STOP;
		}

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean result = super.onTouchEvent(event);
		mRelativeLayout.playSoundEffect(SoundEffectConstants.CLICK);
		if (recording == false) {
			new ToggleStreamAsyncTask().execute();
			recording = true;
		} else {
			new StopStreamAsyncTask().execute();
			recording = false;
			finish();
		}
		return result;
	}

	// Disconnects from the RTSP server and stops the stream
	private class StopStreamAsyncTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			mClient.stopStream();
			return null;
		}
	}

	private void logError(String msg) {
		final String error = (msg == null) ? "Error unknown" : msg;
		Log.e(TAG, error);
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(CameraActivity.this, error, Toast.LENGTH_SHORT).show();
			}
		});
	}

}
