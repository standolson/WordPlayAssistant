package com.ppp.wordplayadvlib.externalads;

import com.google.android.gms.ads.AdSize;

public class AdMobParams {

	public AdSize adSize;
	public String adUnitId;

	public AdMobParams(AdSize adSize, String adUnitId)
	{
		this.adSize = adSize;
		this.adUnitId = adUnitId;
	}

}
