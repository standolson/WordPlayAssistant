package com.ppp.wordplayadvlib.externalads;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.WordPlayApp;
import com.ppp.wordplayadvlib.utils.Debug;
import com.ppp.wordplayadvlib.widgets.TextDrawable;

public class AdMobAd extends SponsoredAd {

	public static boolean useAdMobPlaceholders = false;

	private AdMobData adMobData;
	private InterstitialAd interstitialAd;

	private static View emptyView = null;

	public AdMobAd(Context context, PlacementType placementType, AdMobData adMobData)
	{
		super(context, placementType);
		this.adMobData = adMobData;
	}

	public AdMobAd(Context context, PlacementType placementType, int listPosition, AdMobData adMobData)
	{
		super(context, placementType, listPosition);
		this.adMobData = adMobData;
	}

	public AdMobAd(Context context, AdMobData adMobData)
	{
		super(context);
		this.placementType = PlacementType.Interstitial;
		this.adMobData = adMobData;
	}

	public AdMobData getAdMobData() { return adMobData; }

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

	public InterstitialAd getInterstitialAd() { return interstitialAd; }

	@Override
	public void pause()
	{
		if ((displayView != null) && (displayView != emptyView))  {
			Log.e(getClass().getSimpleName(), "pause: " + super.toString());
			AdView dfpAdView = (AdView) displayView;
			dfpAdView.pause();
		}
		super.pause();
	}

	@Override
	public void resume()
	{
		if ((displayView != null) && (displayView != emptyView))  {
			Log.e(getClass().getSimpleName(), "resume: " + super.toString());
			AdView dfpAdView = (AdView) displayView;
			dfpAdView.resume();
		}
		super.resume();
	}

	@Override
	public void destroy()
	{
		if ((displayView != null) && (displayView != emptyView))  {
			AdView adView = (AdView) displayView;
			destroy(adView);
		}
		super.destroy();
	}

	private void destroy(AdView adView)
	{

		Log.e(getClass().getSimpleName(), "destroy: " + super.toString() + " adUnit: " + adView.toString());

		// Unset the listener and destroy the AdView so
		// nothing is connected back to us and the AdView
		// is completely freed up.
		adView.setAdListener(null);
		adView.destroyDrawingCache();
		adView.pause();
		adView.destroy();

	}

	private AdView getAdMobAdView(AdSize adSize)
	{

//		Debug.e("AdMobAd: creating AdView for '" + adMobData.adUnitId + "'");

		AdView adView = new AdView(context);
		adView.setAdSize(adSize);
		adView.setAdUnitId(adMobData.adUnitId);

		return adView;

	}

	private View getSearchResultView()
	{

		// If Google Play Services isn't working, return a blank view
		if (!WordPlayApp.isGooglePlayServicesOk())  {
			Debug.e("AdMobAd: Google Play Services failure (" + WordPlayApp.getGooglePlayServicesStatusString() + ")");
			return getEmptyView();
		}

		AdSize adSize = AdSize.BANNER;
		final AdView view = getAdMobAdView(adSize);
		if (useAdMobPlaceholders)
			setListLayoutParams(context, view, adSize);

		// Block descendant focusability so we don't have to click twice
		// on the ad
		view.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

		if (view != null)  {

//			Log.e(getClass().getSimpleName(), "AdMob SRP: loading ad for position " + listPosition);

			final AdRequest.Builder builder = getAdBuilder();

			view.setAdListener(new AdListener() {

            	public void onAdLoaded()
            	{
            		Log.d(AdMobAd.class.getSimpleName(), "AdMob SRP: onAdLoaded");
		            isLoaded = true;
		            if (useAdMobPlaceholders)
		            	view.setBackgroundDrawable(null);
					if (eventCallback != null)
						eventCallback.onLoaded(AdMobAd.this);
		            if (adapter != null)
		            	adapter.notifyDataSetChanged();
            	}

            	public void onAdFailedToLoad(int errorCode)
            	{
            		Log.d(AdMobAd.class.getSimpleName(), "AdMob SRP: onAdFailedToLoad; errorCode " + getErrorString(errorCode));
					if (useAdMobPlaceholders)
						view.setBackgroundDrawable(new TextDrawable(context, context.getString(R.string.SponsoredAdFailed), Color.WHITE, 22, 5));
					else {
						destroy(view);
						displayView = getEmptyView();
					}
					if (eventCallback != null)
						eventCallback.onError(AdMobAd.this);
					if (adapter != null)
						adapter.notifyDataSetChanged();
            	}

            	public void onAdOpened()
            	{
            		Log.d(AdMobAd.class.getSimpleName(), "AdMob SRP: onAdOpened");
            		if (eventCallback != null)
            			eventCallback.onOpened(AdMobAd.this);
            	}

            	public void onAdClosed()
            	{
            		Log.d(AdMobAd.class.getSimpleName(), "AdMob SRP: onAdClosed");
            		if (eventCallback != null)
            			eventCallback.onClosed(AdMobAd.this);
            	}

            	public void onAdLeftApplication()
            	{
            		Log.d(AdMobAd.class.getSimpleName(), "AdMob SRP: onAdLeftApplication");
            	}

			});

            if (useAdMobPlaceholders)
    			view.setBackgroundDrawable(new TextDrawable(context, context.getString(R.string.SponsoredAd), Color.WHITE, 22, 5));
			view.loadAd(builder.build());

		}

		return view;

	}

	private View getZsrpResultView()
	{
		return null;
	}

	public void loadInterstitialAd()
	{

		AdRequest.Builder builder = getAdBuilder();

		Log.e(getClass().getSimpleName(), "AdMob INTERSTITIAL: loading ad");

		// Setup the ad
		interstitialAd = new InterstitialAd(context);
		interstitialAd.setAdUnitId(adMobData.adUnitId);
		interstitialAd.setAdListener(new AdListener() {

        	public void onAdLoaded()
        	{
        		Log.d(AdMobAd.class.getSimpleName(), "AdMob INTERSTITIAL: onAdLoaded");
	            isLoaded = true;
				if (eventCallback != null)
					eventCallback.onLoaded(AdMobAd.this);
        	}

        	public void onAdFailedToLoad(int errorCode)
        	{
        		Log.d(AdMobAd.class.getSimpleName(), "AdMob INTERSTITIAL: onAdFailedToLoad; errorCode " + getErrorString(errorCode));
				if (eventCallback != null)
					eventCallback.onError(AdMobAd.this);
        	}

        	public void onAdOpened()
        	{
        		Log.d(AdMobAd.class.getSimpleName(), "AdMob INTERSTITIAL: onAdOpened");
        		if (eventCallback != null)
        			eventCallback.onOpened(AdMobAd.this);
        	}

        	public void onAdClosed()
        	{
        		Log.d(AdMobAd.class.getSimpleName(), "AdMob INTERSTITIAL: onAdClosed");
        		if (eventCallback != null)
        			eventCallback.onClosed(AdMobAd.this);
        	}

        	public void onAdLeftApplication()
        	{
        		Log.d(AdMobAd.class.getSimpleName(), "AdMob INTERSTITIAL: onAdLeftApplication");
        	}
			
		});

		// Create it
		interstitialAd.loadAd(builder.build());

	}

	private void setListLayoutParams(Context context, View view, AdSize adSize)
	{

		Log.e(getClass().getSimpleName(), "adSize " + adSize);

		float density = context.getResources().getDisplayMetrics().density;
		int height = Math.round(adSize.getHeight() * density);

		Log.e(getClass().getSimpleName(), "height " + height);

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

	private AdRequest.Builder getAdBuilder()
	{
		AdRequest.Builder builder = new AdRequest.Builder();
		builder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
		builder.addTestDevice("06B0FEFF5F6B39F3B712494ECD97757A");
		builder.addTestDevice("7D0963E5F7D8F1C546F4A9A66D0ABD32");
		builder.addTestDevice("51AD760CF1B7E77A165A46F95165143C");
		return builder;
	}

	private View getEmptyView()
	{
		if (emptyView == null)  {
			emptyView = new View(context);
			emptyView.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.FILL_PARENT, 0));			
		}
		return emptyView;
	}

}
