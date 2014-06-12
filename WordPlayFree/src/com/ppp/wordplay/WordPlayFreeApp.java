package com.ppp.wordplay;

import com.google.android.gms.ads.AdSize;
import com.ppp.wordplayadvlib.WordPlayApp;
import com.ppp.wordplayadvlib.externalads.AdMobData;

public class WordPlayFreeApp extends WordPlayApp {

	@Override
	public AdMobData[] getSearchAdUnitIds()
	{
		return new AdMobData[] {
			new AdMobData("ca-app-pub-8807533815804449/5100324396", new AdSize(320, 100)),
			new AdMobData("ca-app-pub-8807533815804449/5100324397", AdSize.BANNER)
		};
	}

	@Override
	public AdMobData[] getWordJudgeAdUnitIds()
	{
		return new AdMobData[] {
			new AdMobData("ca-app-pub-8807533815804449/6527868399", AdSize.BANNER)
		};
	}

	@Override
	public String getInterstitialAdUnitId()
	{
		return "ca-app-pub-8807533815804449/9140244397";
	}

}
