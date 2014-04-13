package com.ppp.wordplayadvlib.externalads;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.mediation.admob.AdMobExtras;
import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.widgets.TextDrawable;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

public class AdMobAd extends SponsoredAd {

	public static boolean useAdMobPlaceholders = true;

	public AdMobAd(Context context, PlacementType placementType)
	{
		super(context, placementType);
	}

	public AdMobAd(Context context, PlacementType placementType, int listPosition)
	{
		super(context, placementType, listPosition);
	}

	public View getView()
	{

		// If this ad already has been given a view, return it
		if (displayView != null)
			return displayView;

		// Return the correct kind of ad
		switch (placementType)  {
			case ListSearchResult:
				displayView = getSearchResultView();
				break;
			case ZeroSearchResult:
				displayView = getZsrpResultView();
				break;
			default:
				displayView = null;
				break;
		}

		return displayView;

	}

	public void shutdown()
	{

		Log.e(getClass().getSimpleName(), "shutdown: " + super.toString());

		// Unset the listener and destroy the DfpAdView so
		// nothing is connected back to us and the DfpAdView
		// is completely freed up.
		if (displayView != null)  {
			AdView adView = (AdView) displayView;
			adView.setAdListener(null);
//			dfpAdView.stopLoading();
			adView.destroyDrawingCache();
			adView.destroy();
		}

		super.shutdown();

	}

	private AdView getAdMobAdView(AdSize adSize)
	{
		AdView adView = new AdView(context);
		adView.setAdSize(adSize);
		adView.setAdUnitId("MY_AD_UNIT_ID");
		return adView;
	}

	private View getSearchResultView()
	{

		AdSize adSize = AdSize.BANNER;
		final AdView view = getAdMobAdView(adSize);
		if (useAdMobPlaceholders)
			setListLayoutParams(context, view, adSize);

		// Block descendant focusability so we don't have to click twice
		// on the ad (ANDREBAYCLASS-2971).
		view.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

		if (view != null)  {

			final AdRequest.Builder builder = new AdRequest.Builder();
			builder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);

			view.setAdListener(new AdListener() {

            	public void onAdLoaded()
            	{
            		Log.d(AdMobAd.class.getSimpleName(), "AdMob: onAdLoaded");
            	}

            	public void onAdFailedToLoad(int errorCode)
            	{
            		Log.d(AdMobAd.class.getSimpleName(), "AdMob: onAdFailedToLoad; errorCode " + getErrorString(errorCode));
            	}

            	public void onAdOpened()
            	{
            		Log.d(AdMobAd.class.getSimpleName(), "AdMobAd: onAdOpened");       		
            	}

            	public void onAdClosed()
            	{
            		Log.d(AdMobAd.class.getSimpleName(), "AdSense: onAdClosed");
            	}

            	public void onAdLeftApplication()
            	{
            		Log.d(AdMobAd.class.getSimpleName(), "AdSense: onAdLeftApplication");
            	}

			});

            if (useAdMobPlaceholders)
    			view.setBackgroundDrawable(new TextDrawable(context, context.getString(R.string.SponsoredAd), Color.BLACK, 22, 5));
            view.post(new Runnable() {
				@Override
				public void run() { view.loadAd(builder.build()); }
            });

		}

		return view;

	}

	private View getZsrpResultView()
	{
		return null;
	}

	private void setListLayoutParams(Context context, View view, AdSize adSize)
	{

		float density = context.getResources().getDisplayMetrics().density;
		int height = Math.round(adSize.getHeight() * density);

		AbsListView.LayoutParams params;
		params = new AbsListView.LayoutParams(AbsListView.LayoutParams.FILL_PARENT, height);
		view.setLayoutParams(params);

	}

	public static String getErrorString(int errorCode)
	{
		switch (errorCode)  {
			case AdRequest.ERROR_CODE_INTERNAL_ERROR:
				return "ERROR_CODE_INTERNAL_ERROR";
			case AdRequest.ERROR_CODE_INVALID_REQUEST:
				return "ERROR_CODE_INVALID_REQUEST";
			case AdRequest.ERROR_CODE_NETWORK_ERROR:
				return "ERROR_CODE_NETWORK_ERROR";
			case AdRequest.ERROR_CODE_NO_FILL:
				return "ERROR_CODE_NO_FILL";
			default:
				return "Unknown error";
		}
	}

}
