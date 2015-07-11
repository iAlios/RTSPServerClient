
package com.alios.rtsp.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.microedition.khronos.opengles.GL10;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

public class DeviceUtils {

	public static final String UNKNOW = "Unknown";

	private final static String P2P_INT = "p2p0";

	private static Process mLogcatProc = null;

	private static ArrayList<String> mListOfLogLines;

	private static final String CHAR_ARRAY = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

	private static final Random RANDOM = new Random();

	public static final void startApp(Context context, String action, String packageName,
	        String className) {
		try {
			Intent intent = new Intent();
			if (!TextUtils.isEmpty(action)) {
				intent.setAction(action);
			}
			if (!TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(className)) {
				intent.setClassName(packageName, className);
			}
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);
		} catch (Throwable e) {
			throw new IllegalArgumentException("illegal argument exception");
		}
	}

	public static final void startMainActivity(Context context, String packageName,
	        String className) {
		try {
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			if (!TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(className)) {
				intent.setClassName(packageName, className);
			}
			intent.setFlags(
			        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			context.startActivity(intent);
		} catch (Throwable e) {
			throw new IllegalArgumentException("illegal argument exception");
		}
	}

	public static void broadcast(Context context, String action, String packageName,
	        String className) {
		try {
			Intent intent = new Intent(action);
			if (!TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(className)) {
				intent.setClassName(packageName, className);
			}
			context.sendBroadcast(intent);
		} catch (Throwable e) {
			throw new IllegalArgumentException("illegal argument exception");
		}
	}

	/**
	 * 模拟按键操作
	 * 
	 * @param context
	 * @param keyCodes
	 */
	public static final void sendKey(final Context context, Integer... keyCodes) {
		new AsyncTask<Integer, Void, Void>() {
			@Override
			protected Void doInBackground(Integer... keyCodes1) {
				Instrumentation cInstrumentation = new Instrumentation();

				for (Integer keyCode : keyCodes1) {
					cInstrumentation.sendKeyDownUpSync(keyCode);
				}
				return null;
			}

			protected void onPostExecute(Void result) {

			};

		}.execute(keyCodes);
	}

	/**
	 * 判断是否具有相关权限
	 * 
	 * @param context
	 * @param premission 权限名称
	 * @return
	 */
	public static final boolean checkPermission(Context context,
	        String premission) {
		PackageManager localPackageManager = context.getPackageManager();
		if (localPackageManager.checkPermission(premission,
		        context.getPackageName()) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
			return false;
		}
		return true;
	}

	public static final String randomString(int len) {
		StringBuffer sBuffer = new StringBuffer();
		int rang = CHAR_ARRAY.length();
		for (int index = 0; index < len; index++) {
			sBuffer.append(CHAR_ARRAY.charAt(RANDOM.nextInt(rang)));
		}
		return sBuffer.toString();
	}

	/**
	 * 获取GPU相关信息
	 * 
	 * @param gl10
	 * @return
	 */
	public static final String[] getGPUInfo(GL10 gl10) {
		try {
			String[] result = new String[3];
			result[0] = gl10.glGetString(GL10.GL_RENDERER);
			result[1] = gl10.glGetString(GL10.GL_VENDOR);
			result[2] = gl10.glGetString(GL10.GL_VERSION);
			return result;
		} catch (Exception localException) {
		}
		return new String[0];
	}

	public static String getIPFromMac(String MAC) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("/proc/net/arp"));
			String line;
			while ((line = br.readLine()) != null) {
				String[] splitted = line.split(" +");
				if (splitted != null && splitted.length >= 4) {
					String device = splitted[5];
					if (device.matches(".*" + P2P_INT + ".*")) {
						String mac = splitted[3];
						if (mac.matches(MAC)) {
							return splitted[0];
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static String getLocalIPAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
			        .hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
				        .hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();

					String iface = intf.getName();
					if (iface.matches(".*" + P2P_INT + ".*")) {
						if (inetAddress instanceof Inet4Address) {
							return getDottedDecimalIP(inetAddress.getAddress());
						}
					}
				}
			}
		} catch (SocketException ex) {
			Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
		} catch (NullPointerException ex) {
			Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
		}
		return null;
	}

	private static String getDottedDecimalIP(byte[] ipAddr) {
		String ipAddrStr = "";
		for (int i = 0; i < ipAddr.length; i++) {
			if (i > 0) {
				ipAddrStr += ".";
			}
			ipAddrStr += ipAddr[i] & 0xFF;
		}
		return ipAddrStr;
	}

	/**
	 * 获取CPU相关信息
	 * 
	 * @return
	 */
	public static final String getCPUInfo() {
		String str = null;
		try {
			FileReader localFileReader = new FileReader("/proc/cpuinfo");
			if (localFileReader != null) {
				BufferedReader localBufferedReader = new BufferedReader(
				        localFileReader, 1024);
				str = localBufferedReader.readLine();
				localBufferedReader.close();
				localFileReader.close();
			}
		} catch (IOException localFileNotFoundException) {
		}
		if (!TextUtils.isEmpty(str)) {
			return str.substring(str.indexOf(':') + 1).trim();
		}
		return str;
	}

	/**
	 * 获取IMEI号，GSM等唯一标示
	 * 
	 * @param context
	 * @return
	 */
	public static final String getIMEI(Context context) {
		if (checkPermission(context, "android.permission.READ_PHONE_STATE")) {
			TelephonyManager localTelephonyManager = (TelephonyManager) context
			        .getSystemService(Context.TELEPHONY_SERVICE);
			if (localTelephonyManager == null) {
				return UNKNOW;
			} else {
				return localTelephonyManager.getDeviceId();
			}
		}
		return UNKNOW;
	}

	/**
	 * 获取系统SessionId
	 * 
	 * @param context
	 * @return
	 */
	public static final String getSessionId(Context context) {
		return Settings.Secure.getString(context.getContentResolver(),
		        Secure.ANDROID_ID);
	}

	/**
	 * 生产UUID
	 * 
	 * @param pString
	 * @return
	 */
	public static final String getUUID(String pString) {
		UUID cUUID;
		try {
			cUUID = UUID.nameUUIDFromBytes(pString.getBytes("utf-8"));
		} catch (UnsupportedEncodingException e) {
			cUUID = UUID.nameUUIDFromBytes(pString.getBytes());
		}
		return cUUID.toString();
	}

	@SuppressLint("InlinedApi")
	public static final String getUUIDKey(Context context) {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(getSessionId(context));
		stringBuffer.append("-");
		stringBuffer.append(getCPUInfo());
		stringBuffer.append("-");
		stringBuffer.append(getMAC(context));
		stringBuffer.append("-");
		try {
			stringBuffer.append(Build.HARDWARE);
		} catch (Throwable e) {
		}
		if (checkPermission(context, "android.permission.READ_PHONE_STATE")) {
			stringBuffer.append("-");
			stringBuffer.append(getIMEI(context));
		}
		return stringBuffer.toString();
	}

	public static boolean isMonkeyRunning() {
		return ActivityManager.isUserAMonkey();
	}

	@SuppressLint("InlinedApi")
	public static void setDebuggable(Context context, boolean debuggable) {
		Settings.Global.putInt(context.getContentResolver(),
		        Settings.Global.ADB_ENABLED, debuggable ? 1 : 0);
		Settings.Global.putInt(context.getContentResolver(),
		        Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, debuggable ? 1
		                : 0);
	}

	@SuppressWarnings("deprecation")
	public static final String phoneTotalCapacity(Context context) {
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSizeLong();
		long totalBlocks = stat.getBlockCount();
		return Formatter.formatFileSize(context, blockSize * totalBlocks);
	}

	@SuppressWarnings("deprecation")
	public static final String phoneAvailableCapacity(Context context) {
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		return Formatter.formatFileSize(context, blockSize * availableBlocks);
	}

	@SuppressWarnings("deprecation")
	public static final String sdcardTotalCapacity(Context context) {
		File path = Environment.getExternalStorageDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long totalBlocks = stat.getBlockCount();
		return Formatter.formatFileSize(context, blockSize * totalBlocks);
	}

	@SuppressWarnings("deprecation")
	public static final String sdcardAvailableCapacity(Context context) {
		File path = Environment.getExternalStorageDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		return Formatter.formatFileSize(context, blockSize * availableBlocks);
	}

	@SuppressWarnings("deprecation")
	public static final float sdcardTotalCapacityMB() {
		File path = Environment.getExternalStorageDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long totalBlocks = stat.getBlockCount();
		return formatFileSize(blockSize * totalBlocks);
	}

	@SuppressWarnings("deprecation")
	public static final float sdcardAvailableCapacityMB() {
		File path = Environment.getExternalStorageDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		return formatFileSize(blockSize * availableBlocks);
	}

	private static float formatFileSize(double result) {
		result = result / 1024;
		result = result / 1024;
		BigDecimal bBigDecimal = new BigDecimal(result);
		bBigDecimal = bBigDecimal.setScale(2, 4);
		return bBigDecimal.floatValue();
	}

	/**
	 * 获取屏幕大小
	 * 
	 * @param context
	 * @return
	 */
	public static final String getScreenSize(Context context) {
		try {
			DisplayMetrics localDisplayMetrics = new DisplayMetrics();
			WindowManager localWindowManager = (WindowManager) context
			        .getSystemService(Context.WINDOW_SERVICE);
			localWindowManager.getDefaultDisplay().getMetrics(
			        localDisplayMetrics);
			return String.format("%dx%d", localDisplayMetrics.widthPixels,
			        localDisplayMetrics.heightPixels);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return UNKNOW;
	}

	/**
	 * 获取联网方式
	 * 
	 * @param context
	 * @return
	 */
	public static final String[] getNetworkType(Context context) {
		String[] result = {
		        UNKNOW, UNKNOW
		};
		if (checkPermission(context, "android.permission.ACCESS_NETWORK_STATE")) {
			return result;
		}
		ConnectivityManager cConnectivityManager = (ConnectivityManager) context
		        .getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cConnectivityManager == null) {
			return result;
		}
		NetworkInfo cNetworkInfo = cConnectivityManager
		        .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (cNetworkInfo.getState() == NetworkInfo.State.CONNECTED) {
			result[0] = "Wi-Fi";
			return result;
		}
		cNetworkInfo = cConnectivityManager
		        .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		if (cNetworkInfo.getState() == NetworkInfo.State.CONNECTED) {
			if (cNetworkInfo.getSubtype() == TelephonyManager.NETWORK_TYPE_GPRS
			        || cNetworkInfo.getSubtype() == TelephonyManager.NETWORK_TYPE_CDMA
			        || cNetworkInfo.getSubtype() == TelephonyManager.NETWORK_TYPE_EDGE) {
				result[0] = "2G";
				return result;
			} else {
				result[0] = "3G";
			}
			result[1] = cNetworkInfo.getSubtypeName();
			return result;
		}
		return result;
	}

	/**
	 * 获取 wifi 下的 ip 地址 <uses-permission
	 * android:name="android.permission.ACCESS_WIFI_STATE"></uses-permission>
	 * <uses-permission
	 * android:name="android.permission.CHANGE_WIFI_STATE"></uses-permission>
	 * 
	 * @param context
	 * @return
	 */
	public static final String getLocalIP(Context context) {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (wifiManager.isWifiEnabled()) {
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			int ipAddress = wifiInfo.getIpAddress();
			return (ipAddress & 0xFF) + "." +
			        ((ipAddress >> 8) & 0xFF) + "." +
			        ((ipAddress >> 16) & 0xFF) + "." +
			        (ipAddress >> 24 & 0xFF);
		}
		return null;
	}

	/**
	 * 获取上网的ip地址 <br/>
	 * 需要 <uses-permission
	 * android:name="android.permission.INTERNET"></uses-permission>
	 * 
	 * @param context
	 * @return
	 */
	public static final String getNetIP(Context context) {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
			        .hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
				        .hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {
		}
		return null;
	}

	/**
	 * 当前是否已联网
	 * 
	 * @param context
	 * @return
	 */
	public static final boolean hasNetWork(Context context) {
		try {
			ConnectivityManager cConnectivityManager = (ConnectivityManager) context
			        .getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo cNetworkInfo = cConnectivityManager
			        .getActiveNetworkInfo();
			if (cNetworkInfo != null) {
				return cNetworkInfo.isConnectedOrConnecting();
			}
			return false;
		} catch (Exception localException) {
		}
		return true;
	}

	public static final String[] getDefaultLang(Context context) {
		String[] result = {
		        UNKNOW, UNKNOW
		};
		Locale cLocale = getDefaultLocale(context);
		if (cLocale != null) {
			result[0] = cLocale.getCountry();
			result[1] = cLocale.getLanguage();
		}
		return result;
	}

	public static final Locale getDefaultLocale(Context context) {
		Locale result = null;
		try {
			Configuration localConfiguration = new Configuration();
			Settings.System.getConfiguration(context.getContentResolver(),
			        localConfiguration);
			if (localConfiguration != null) {
				result = localConfiguration.locale;
			}
		} catch (Exception localException) {
		}

		if (result == null) {
			result = Locale.getDefault();
		}

		return result;
	}

	/**
	 * 获取网卡的物理地址
	 * 
	 * @param context
	 * @return
	 */
	public static final String getMAC(Context context) {
		try {
			WifiManager cWifiManager = (WifiManager) context
			        .getSystemService(Context.WIFI_SERVICE);
			if (checkPermission(context, "android.permission.ACCESS_WIFI_STATE")) {
				WifiInfo localWifiInfo = cWifiManager.getConnectionInfo();
				return TextUtils.isEmpty(localWifiInfo.getMacAddress()) ? UNKNOW
				        : localWifiInfo.getMacAddress();
			}
		} catch (Exception localException) {
		}
		return UNKNOW;
	}

	public static final boolean isDebuggable(Context context) {
		return (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
	}

	/**
	 * 获取当前地址
	 * 
	 * @param context
	 * @return
	 */
	public static final Location getCurrentLocation(Context context) {
		try {
			LocationManager cLocationManager = (LocationManager) context
			        .getSystemService(Context.LOCATION_SERVICE);
			Location cLocation;
			if (checkPermission(context,
			        "android.permission.ACCESS_FINE_LOCATION")) {
				cLocation = cLocationManager.getLastKnownLocation("gps");
				if (cLocation != null) {
					return cLocation;
				}
			}
			if (checkPermission(context,
			        "android.permission.ACCESS_COARSE_LOCATION")) {
				cLocation = cLocationManager.getLastKnownLocation("network");
				if (cLocation != null) {
					return cLocation;
				}
			}
			return null;
		} catch (Exception localException) {
		}
		return null;
	}

	public static final void startLog(Context context) throws IOException {
		if (checkPermission(context, "android.permission.READ_LOGS")) {
			return;
		}
		BufferedReader reader = null;
		mLogcatProc = Runtime.getRuntime().exec(
		        new String[] {
		                "logcat", "-d", "AndroidRuntime:E BDtN:V *:S"
		        });
		reader = new BufferedReader(new InputStreamReader(
		        mLogcatProc.getInputStream()));
		String line;
		if (mListOfLogLines == null) {
			mListOfLogLines = new ArrayList<String>();
		}
		while ((line = reader.readLine()) != null) {
			mListOfLogLines.add(line);
		}
	}

	public static final void stopLog(Context context) {
		if (mLogcatProc == null) {
			return;
		}
		mLogcatProc.destroy();
		mListOfLogLines.clear();
		mLogcatProc = null;
	}

	/**
	 * 获取联网时候的代理地址
	 * 
	 * @param context
	 * @return
	 */
	public static final String getProxy(Context context) {
		if (checkPermission(context, "android.permission.ACCESS_NETWORK_STATE")) {
			return null;
		}
		try {
			ConnectivityManager cConnectivityManager = (ConnectivityManager) context
			        .getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo cNetworkInfo = cConnectivityManager
			        .getActiveNetworkInfo();
			if (cNetworkInfo == null) {
				return null;
			}
			if (cNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
				return null;
			}
			String str = cNetworkInfo.getExtraInfo();
			if (str == null)
				return null;
			if ((str.equals("cmwap")) || (str.equals("3gwap"))
			        || (str.equals("uniwap"))) {
				return "10.0.0.172";
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getNowTime(String formatString) {
		long time = System.currentTimeMillis();
		SimpleDateFormat format = new SimpleDateFormat(formatString);
		Date date = new Date(time);
		String res = format.format(date);
		return res;
	}

	/**
	 * 判断是否为手机号
	 */
	public static boolean isPhoneNumber(String str) {
		Pattern pattern = Pattern.compile("1[0-9]{10}");
		Matcher matcher = pattern.matcher(str);
		if (matcher.matches()) {
			return true;
		} else {
			return false;
		}
	}

	public static void dump() {
		new Throwable("Dump Stack").printStackTrace();
	}

}
