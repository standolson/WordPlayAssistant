package com.ppp.wordplayadvlib.externalads;

import android.os.Bundle;

import com.google.android.gms.ads.AdSize;

public class AdMobData {

	public String adUnitId;
	public AdSize adSize;
	public Bundle args;

	public AdMobData(String adUnitId)
	{
		this.adUnitId = adUnitId;
	}

	public AdMobData(String adUnitId, AdSize adSize)
	{
		this.adUnitId = adUnitId;
		this.adSize = adSize;
	}

}
