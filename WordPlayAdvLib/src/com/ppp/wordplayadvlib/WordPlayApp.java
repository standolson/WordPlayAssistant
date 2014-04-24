package com.ppp.wordplayadvlib;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;

import com.ppp.wordplayadvlib.analytics.Analytics;
import com.ppp.wordplayadvlib.utils.Debug;

public class WordPlayApp extends Application
{

	private static WordPlayApp singleton = null;

	// App version code and name
	public static int appVersionCode = 0;
	public static String appVersionName = "Unknown";

	private boolean freeMode = true;
	private boolean useGoogleAppEngine = false;

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
        freeMode = getPackageName().equals(Constants.FreeAppName);

        // Initialize the version
        initVersionInfo(this);

        // Initialize Google Analytics
        initGoogleAnalytics();

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
        		app.getPackageManager().getPackageInfo(getInstance().isFreeMode() ?
        													Constants.FreeAppName : Constants.PaidAppName,
        												PackageManager.GET_META_DATA);
        	appVersionCode = pInfo.versionCode;
        	appVersionName = pInfo.versionName;
        	Debug.v("MANIFEST VERISON CODE = " + appVersionCode);
        	Debug.v("MANIFEST VERSION NAME = " + appVersionName);
        }
        catch (NameNotFoundException e) {}

	}

	private void initGoogleAnalytics()
	{

		// Initialize the global Tracker object
		String trackingId = isPaidMode() ? "UA-50341453-1" : "UA-50341453-2";
		new Analytics(getApplicationContext(), trackingId);

	}

	public static int getAppManifestVersion() { return appVersionCode; }

	public void setFreeMode() { freeMode = true; }
	public void setPaidMode() { freeMode = false; }

	public boolean isFreeMode() { return freeMode; }
	public boolean isPaidMode() { return !freeMode; }

	public boolean getUseGoogleAppEngine() { return useGoogleAppEngine; }
	public void setUseGoogleAppEngine(boolean b) { useGoogleAppEngine = b; }

}
