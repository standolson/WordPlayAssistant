package com.ppp.wordplayadvlib.externalads;

import android.os.Bundle;

public class AdMobData {

	public String adUnitId;
	public Bundle args;

	public AdMobData(String adUnitId)
	{
		this.adUnitId = adUnitId;
	}

	public AdMobData(String adUnitId, Bundle args)
	{
		this.adUnitId = adUnitId;
		this.args = args;
	}

}
