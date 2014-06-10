package com.ppp.wordpal;

import com.ppp.wordplayadvlib.WordPlayApp;

public class WordPalFreeApp extends WordPlayApp {

	@Override
	public String[] getSearchAdUnitIds()
	{
		return new String[] {
			"ca-app-pub-8807533815804449/5584675594",
			"ca-app-pub-8807533815804449/7061408794"
		};
	}

	@Override
	public String[] getWordJudgeAdUnitIds()
	{
		return new String[] {
			"ca-app-pub-8807533815804449/8004601597"
		};
	}

	@Override
	public String getInterstitialAdUnitId()
	{
		return "ca-app-pub-8807533815804449/5730949590";
	}

}
