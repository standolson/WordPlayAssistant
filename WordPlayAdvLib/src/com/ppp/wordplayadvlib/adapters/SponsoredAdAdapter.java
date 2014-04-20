package com.ppp.wordplayadvlib.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.BaseAdapter;

import com.ppp.wordplayadvlib.externalads.AdMobAd;
import com.ppp.wordplayadvlib.externalads.SponsoredAd;
import com.ppp.wordplayadvlib.externalads.SponsoredAd.PlacementType;

public class SponsoredAdAdapter extends BaseAdapter {

	private static final int MAX_ADMOB_ADS = 2;

	private Context context;
	private BaseAdapter delegate;

	private SparseArray<SponsoredAd> sponsoredAds;
	private ArrayList<SponsoredAd> availableAds;

	public SponsoredAdAdapter(Context context, BaseAdapter delegate)
	{

		this.context = context;
		this.delegate = delegate;

		sponsoredAds = new SparseArray<SponsoredAd>();
		availableAds = new ArrayList<SponsoredAd>(2);

	}

	@Override
	public int getCount()
	{

		int trueCount = delegate.getCount();
		int adCount = trueCount / 10 + 1;

		// If no results, then show no ads
		if (trueCount == 0)
			return trueCount;

		return trueCount + adCount;

	}

	@Override
	public Object getItem(int position)
	{
		if (isSponsoredAd(position))
			return null;
		return delegate.getItem(getRealPosition(position));
	}

	@Override
	public long getItemId(int position) { return delegate.getItemId(position); }

	@Override
	public int getItemViewType(int position)
	{
		if (isSponsoredAd(position))
			return delegate.getViewTypeCount();
		else
			return delegate.getItemViewType(getRealPosition(position));
	}

	@Override
	public int getViewTypeCount() { return delegate.getViewTypeCount() + 1; }

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{

		// If this is a regular item, just return it now
		if (!isSponsoredAd(position))
			return delegate.getView(getRealPosition(position), convertView, parent);

		// Do we already have a SponsoredAd for this position?
		SponsoredAd ad = sponsoredAds.get(position);
		if ((ad == null) || (ad.getView() == null))  {

			// Create a new ad if we haven't loaded the maximum number
			// of AdMob ads.  If we haven't, then we use one of the ads
			// we already have.
			AdMobAd adMobAd = null;
			if (availableAds.size() < MAX_ADMOB_ADS)  {
				adMobAd = new AdMobAd(context, PlacementType.ListSearchResult, position);
				adMobAd.setSponsoredAdAdapter(this);
				availableAds.add(adMobAd);
			}
			else
				adMobAd = (AdMobAd) availableAds.get(sponsoredAds.size() % 2);
			View view = adMobAd.getView();

			// Save it for later
			sponsoredAds.put(position, adMobAd);

			// Notify all observers as by adding another sponsored ad,
			// we've increased the size of what the adapter is showing.
			notifyDataSetChanged();

			// Return a zero height view to hide where the ad is going
			// to go until it is loaded
			if (AdMobAd.useAdMobPlaceholders)
				return view;
			else
				return getEmptyView();

		}
		else {

			// We have a cached SponsoredAd.  Return its view
			// if the ad is loaded otherwise return a zero height
			// view.
			if (ad.isLoaded() || AdMobAd.useAdMobPlaceholders)
				return ad.getView();
			else
				return getEmptyView();

		}

	}

    public void pause()
    {

    	if ((availableAds == null) || (availableAds.size() == 0))
    		return;

    	for (SponsoredAd ad : availableAds)
    		if (ad != null)
    			ad.pause();

    }

    public void resume()
    {

    	if ((availableAds == null) || (availableAds.size() == 0))
    		return;

    	for (SponsoredAd ad : availableAds)
    		if (ad != null)
    			ad.resume();

    }

    public void destroy()
    {

    	if ((availableAds == null) || (availableAds.size() == 0))
    		return;

    	for (SponsoredAd ad : availableAds)
    		if (ad != null)
    			ad.destroy();

    	delegate = null;

    }

	private boolean isSponsoredAd(int position) { return position % 10 == 0; }

	public int getRealPosition(int position)
	{
		int sponsored = sponsoredAdCountAt(position);
		return position - sponsored;
	}

	public int sponsoredAdCountAt(int position)
	{
		int adCount = position / 10 + 1;
		return adCount;
	}

	private View getEmptyView()
	{
		View empty = new View(context);
		empty.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.FILL_PARENT, 0));
		return empty;
	}

}
