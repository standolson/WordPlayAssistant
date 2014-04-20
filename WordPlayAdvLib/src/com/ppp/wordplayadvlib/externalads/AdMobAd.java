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
import com.google.android.gms.ads.doubleclick.PublisherAdView;
import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.widgets.TextDrawable;

public class AdMobAd extends SponsoredAd {

	private static View emptyView = null;
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


	@Override
	public void pause()
	{
		if ((displayView != null) && (displayView != emptyView))  {
			Log.e(getClass().getSimpleName(), "pause: " + super.toString());
			PublisherAdView dfpAdView = (PublisherAdView) displayView;
			dfpAdView.pause();
		}
		super.pause();
	}

	@Override
	public void resume()
	{
		if ((displayView != null) && (displayView != emptyView))  {
			Log.e(getClass().getSimpleName(), "resume: " + super.toString());
			PublisherAdView dfpAdView = (PublisherAdView) displayView;
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
		AdView adView = new AdView(context);
		adView.setAdSize(adSize);
		adView.setAdUnitId("a14d57787867bd1");
		return adView;
	}

	private View getSearchResultView()
	{

		AdSize adSize = AdSize.BANNER;
		final AdView view = getAdMobAdView(adSize);
		if (useAdMobPlaceholders)
			setListLayoutParams(context, view, adSize);

		// Block descendant focusability so we don't have to click twice
		// on the ad
		view.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

		if (view != null)  {

			Log.e(getClass().getSimpleName(), "AdMob: loading ad for position " + listPosition);

			final AdRequest.Builder builder = new AdRequest.Builder();
			builder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
//			builder.addTestDevice("06B0FEFF5F6B39F3B712494ECD97757A");

			view.setAdListener(new AdListener() {

            	public void onAdLoaded()
            	{
            		Log.d(AdMobAd.class.getSimpleName(), "AdMob: onAdLoaded");
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
            		Log.d(AdMobAd.class.getSimpleName(), "AdMob: onAdFailedToLoad; errorCode " + getErrorString(errorCode));
					if (useAdMobPlaceholders)
						view.setBackgroundDrawable(new TextDrawable(context, context.getString(R.string.SponsoredAdFailed), Color.BLACK, 22, 5));
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
    			view.setBackgroundDrawable(new TextDrawable(context, context.getString(R.string.SponsoredAd), Color.WHITE, 22, 5));
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

	private View getEmptyView()
	{
		if (emptyView == null)  {
			emptyView = new View(context);
			emptyView.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.FILL_PARENT, 0));			
		}
		return emptyView;
	}

}
