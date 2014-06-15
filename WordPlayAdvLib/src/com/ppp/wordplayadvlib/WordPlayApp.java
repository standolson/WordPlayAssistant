package com.ppp.wordplayadvlib;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.ppp.wordplayadvlib.externalads.AdMobData;
import com.ppp.wordplayadvlib.utils.Debug;

public class WordPlayApp extends Application
{

	public static String GOOGLE_PLAY_SERVICES_PACKAGE = "com.google.android.gms";

	private static WordPlayApp singleton = null;

	// App version code and name
	public static int appVersionCode = 0;
	public static String appVersionName = "Unknown";

	private boolean freeMode = true;
	private boolean useGoogleAppEngine = false;

	private static int googlePlayStatus = -1;
	private static boolean showPlayServicesWarning = false;

	public void onCreate()
	{

		super.onCreate();

        singleton = this;

		// Set logging level (at release, this should be set to something
		// unobtrusive) and assertion state (at release, this should be set
		// to disabled)
		Debug.setLogLevel(Debug.DebugLevel.DebugLevelVerbose);
        Debug.enableAsserts();

        // Set the free/paid mode based on the name of the package
        freeMode = getPackageName().contains("free");

        // Initialize the version
        initVersionInfo(this);

        // Initialize Google Play Services
        initGooglePlayServices(this);

	}

	public void onLowMemory()
	{
		super.onLowMemory();
	}

	public void onTerminate()
	{
		super.onTerminate();
	}

	public void onConfigurationChanged(Configuration config)
	{
		super.onConfigurationChanged(config);
	}

	public static WordPlayApp getInstance() { return singleton; }

	private static void initVersionInfo(WordPlayApp app)
	{

		// Skip if the version is set
		if (appVersionCode != 0)
			return;

        // Get the application version we use later in reseting the nag count
        // which will eventually show the nag dialog.  This must happen before
		// history is loaded as we also use the manifest version to determine
		// when history needs upgrading.
        try {
        	PackageInfo pInfo =
        		app.getPackageManager().getPackageInfo(app.getPackageName(), PackageManager.GET_META_DATA);
        	appVersionCode = pInfo.versionCode;
        	appVersionName = pInfo.versionName;
        	Debug.v("MANIFEST VERISON CODE = " + appVersionCode);
        	Debug.v("MANIFEST VERSION NAME = " + appVersionName);
        }
        catch (NameNotFoundException e) {}

	}

	public static int getAppManifestVersion() { return appVersionCode; }

	public void setFreeMode() { freeMode = true; }
	public void setPaidMode() { freeMode = false; }

	public boolean isFreeMode() { return freeMode; }
	public boolean isPaidMode() { return !freeMode; }

	public boolean getUseGoogleAppEngine() { return useGoogleAppEngine; }
	public void setUseGoogleAppEngine(boolean b) { useGoogleAppEngine = b; }

	//
	// Google Play Services
	//

	public static void initGooglePlayServices(Context context)
	{

		// Get the state of Play Services
		googlePlayStatus = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
//		googlePlayStatus = ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED;

		// Reset the warning state
		showPlayServicesWarning = false;

	}

	//
	// Google Play Services
	//

	public static int getGooglePlayServicesStatus() { return googlePlayStatus; }
	public static boolean isGooglePlayServicesOk() { return googlePlayStatus == ConnectionResult.SUCCESS; }

	public static boolean shouldShowPlayServicesWarning() { return showPlayServicesWarning; }
	public static void showPlayServicesWarning(boolean b) { showPlayServicesWarning = b; }

	public static String getGooglePlayServicesStatusString()
	{
		return GooglePlayServicesUtil.getErrorString(googlePlayStatus);
	}

	public static boolean isGooglePlayStatusRecoverable()
	{
		// In testing, discovered that SERVICE_INVALID is user recoverable
		// and we don't want that
		return
			GooglePlayServicesUtil.isUserRecoverableError(googlePlayStatus) &&
			(googlePlayStatus != ConnectionResult.SERVICE_INVALID);
	}

	public static String getGooglePlayServicesVersionName(Context context)
	{
		try {
			PackageManager pm = context.getPackageManager();
			PackageInfo info = pm.getPackageInfo(GOOGLE_PLAY_SERVICES_PACKAGE, 0);
			return info.versionName;
		}
		catch (Exception e) {
			return "UNKNOWN";
		}
	}

	public static int getGooglePlayServicesVersionCode(Context context)
	{
		try {
			PackageManager pm = context.getPackageManager();
			PackageInfo info = pm.getPackageInfo(GOOGLE_PLAY_SERVICES_PACKAGE, 0);
			return info.versionCode;
		}
		catch (Exception e) {
			return 0;
		}
	}

	//
	// AdMob
	//

	public AdMobData[] getSearchAdUnitIds() { return null; }

	public AdMobData[] getWordJudgeAdUnitIds() { return null; }

	public String getInterstitialAdUnitId() { return null; }

	public long getInterstitialInterval() { return 5; }

}
