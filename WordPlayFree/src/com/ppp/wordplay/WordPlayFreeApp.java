package com.ppp.wordplay;

import com.ppp.wordplayadvlib.WordPlayApp;

public class WordPlayFreeApp extends WordPlayApp {

	@Override
	public String[] getSearchAdUnitIds()
	{
		return new String[] {
			"ca-app-pub-8807533815804449/5100324396",
			"ca-app-pub-8807533815804449/5100324397"	
		};
	}

	@Override
	public String[] getWordJudgeAdUnitIds()
	{
		return new String[] {
			"ca-app-pub-8807533815804449/6527868399"
		};
	}

	@Override
	public String getInterstitialAdUnitId()
	{
		return "ca-app-pub-8807533815804449/9140244397";
	}

}
