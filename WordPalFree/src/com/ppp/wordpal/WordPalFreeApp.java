package com.ppp.wordpal;

import com.google.android.gms.ads.AdSize;
import com.ppp.wordplayadvlib.WordPlayApp;
import com.ppp.wordplayadvlib.externalads.AdMobData;

public class WordPalFreeApp extends WordPlayApp {

	@Override
	public AdMobData[] getSearchAdUnitIds()
	{
		return new AdMobData[] {
			new AdMobData("ca-app-pub-8807533815804449/5584675594", new AdSize(320, 100)),
			new AdMobData("ca-app-pub-8807533815804449/7061408794", AdSize.BANNER)
		};
	}

	@Override
	public AdMobData[] getWordJudgeAdUnitIds()
	{
		return new AdMobData[] {
			new AdMobData("ca-app-pub-8807533815804449/8004601597", AdSize.BANNER)
		};
	}

	@Override
	public String getInterstitialAdUnitId()
	{
		return "ca-app-pub-8807533815804449/5730949590";
	}

}
