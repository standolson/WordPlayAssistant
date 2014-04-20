package com.ppp.wordplayadvlib.networking;

import java.io.IOException;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.WordPlayApp;
import com.ppp.wordplayadvlib.exceptions.WordPlayException;
import com.ppp.wordplayadvlib.utils.Debug;

public class NetworkUtils {

	public static void networkAvailable() throws WordPlayException
	{

		int networkCount = 0;
		ConnectivityManager mgr =
			(ConnectivityManager)WordPlayApp.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);

		// Iterate over all of the available networks
		for (NetworkInfo info : mgr.getAllNetworkInfo())  {
			Debug.e("networkAvailable:" +
					" network " + info.getTypeName() +
					" subtype " + info.getSubtypeName() +
					" state " + info.getState());
			NetworkInfo.State state = info.getState();
			if ((state == NetworkInfo.State.CONNECTED) || (state == NetworkInfo.State.CONNECTING))
				networkCount += 1;
		}

		Debug.e("networkAvailable: " + networkCount + " networks active");

		if (networkCount == 0)
			throw new WordPlayException(WordPlayApp.getInstance().getString(R.string.no_network_available));

	}

	public static boolean getAirplaneMode()
	{

		int airplaneMode =
			Settings.System.getInt(WordPlayApp.getInstance().getContentResolver(),
									Settings.System.AIRPLANE_MODE_ON, 0);

		if (airplaneMode != 0)  {
			Debug.e("getAirplaneMode: airplane mode is on");
			return true;
		}

		return false;

	}

	public static boolean isRetryException(Exception e)
	{
		return (e instanceof IOException);
	}

}
