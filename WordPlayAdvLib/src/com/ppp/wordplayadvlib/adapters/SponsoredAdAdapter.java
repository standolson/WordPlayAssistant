package com.ppp.wordplayadvlib.adapters;

import java.util.SortedSet;
import java.util.TreeSet;

import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;

import com.ppp.wordplayadvlib.WordPlayApp;
import com.ppp.wordplayadvlib.externalads.AdMobAd;
import com.ppp.wordplayadvlib.externalads.AdMobData;
import com.ppp.wordplayadvlib.externalads.SponsoredAd;
import com.ppp.wordplayadvlib.externalads.SponsoredAd.PlacementType;

public class SponsoredAdAdapter extends BaseAdapter implements SectionIndexer {

	private final DataSetObservable observers = new DataSetObservable();

	private Context context;
	private BaseAdapter delegate;
	protected TreeSet<Integer> sponsoredAdPositions;
	private SparseArray<SponsoredAd> sponsoredAds;

	public SponsoredAdAdapter(Context context,
								BaseAdapter delegate,
								TreeSet<Integer> sponsoredAdPositions,
								SparseArray<SponsoredAd> sponsoredAds)
	{

		this.context = context;
		this.delegate = delegate;
		this.sponsoredAdPositions = sponsoredAdPositions;
		this.sponsoredAds = sponsoredAds;

		sponsoredAds = new SparseArray<SponsoredAd>();

	}

	@Override
	public int getCount()
	{

		// No delegate equals no items
		if (delegate == null)
			return 0;

		int trueCount = delegate.getCount();
		int adCount = sponsoredAdPositions.size();

		// If no results, then show no ads
		if (trueCount == 0)
			return trueCount;

		Log.e(getClass().getSimpleName(), "getCount: " + (trueCount + adCount));

		return trueCount + adCount;

	}

	@Override
	public Object getItem(int position)
	{
		Object obj = null;
		if (delegate == null)  {
			Log.e(getClass().getSimpleName(), "getItem: delegate == null");
			return null;
		}
		if (isSponsoredAd(position))  {
			Log.e(getClass().getSimpleName(), "getItem: position " + position + " = isSponsoredAd");
			return null;
		}
		obj = delegate.getItem(getRealPosition(position));
		Log.e(getClass().getSimpleName(), "getItem: position " + position + " = " + obj);
		return obj;
	}

	@Override
	public long getItemId(int position) { return position; }

	@Override
	public int getItemViewType(int position)
	{
		int type = -1;
		if (delegate != null)  {
			if (isSponsoredAd(position))
				type = delegate.getViewTypeCount();
			else
				type = delegate.getItemViewType(getRealPosition(position));
		}
		else
			type = 0;
		Log.e(getClass().getSimpleName(), "getItemViewType: position " + position + " = " + type);
		return type;
	}

	@Override
	public int getViewTypeCount()
	{
		if (delegate == null)
			return 1;
		int delegateCount = delegate.getViewTypeCount();
		Log.e(getClass().getSimpleName(), "getViewTypeCount: " + (delegateCount + 1));
		return delegateCount + 1;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{

		String[] adUnitIds = WordPlayApp.getInstance().getAdMobAdUnitIds();

		// Check for no delegate
		if (delegate == null)
			return getEmptyView();

		if (isSponsoredAd(position))  {

			Log.e(getClass().getSimpleName(), "isSponsoredAd: " + position);

			// Add this position to the list of positions
			sponsoredAdPositions.add(position);

			// Do we already have a SponsoredAd for this position?
			SponsoredAd ad = sponsoredAds.get(position);
			if ((ad == null) || (ad.getView() == null))  {

				// If we have no ad units to show or don't have enough,
				// return an empty view
				if ((adUnitIds == null) || (adUnitIds.length < 2))
					return getEmptyView();

				// Create a new ad if we haven't loaded the maximum number
				// of AdMob ads.  If we haven't, then we use one of the ads
				// we already have.
				AdMobData adMobData = new AdMobData(adUnitIds[sponsoredAds.size() % adUnitIds.length]);
				AdMobAd adMobAd = new AdMobAd(context, PlacementType.ListSearchResult, position, adMobData);
				adMobAd.setSponsoredAdAdapter(this);

				// Either load the ad or get the view we already have
				View view = adMobAd.getView();

				// Save it for later
				sponsoredAds.put(position, adMobAd);

				// Notify all observers as by adding another sponsored ad,
				// we've increased the size of what the adapter is showing.
				notifyDataSetChanged();

				// Return a zero height view to hide where the ad is going
				// to go until it is loaded
				if (((ad != null) && ad.isLoaded()) || AdMobAd.useAdMobPlaceholders)
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

		// This is a regular item...just return it now
		else {
			Log.e(getClass().getSimpleName(), "NOT isSponsoredAd: " + position);
			return delegate.getView(getRealPosition(position), convertView, parent);
		}

	}

	public int getRealPosition(int position)
	{
		int sponsored = sponsoredAdCountAt(position);
		return position - sponsored;
	}

    public void pause()
    {
    	for (int i = 0; i < sponsoredAds.size(); i += 1)  {
    		int key = sponsoredAds.keyAt(i);
    		SponsoredAd ad = sponsoredAds.get(key);
    		ad.pause();
    	}
    }

    public void resume()
    {
    	for (int i = 0; i < sponsoredAds.size(); i += 1)  {
    		int key = sponsoredAds.keyAt(i);
    		SponsoredAd ad = sponsoredAds.get(key);
    		ad.resume();
    	}
    }

    public void destroy()
    {
    	for (int i = 0; i < sponsoredAds.size(); i += 1)  {
    		int key = sponsoredAds.keyAt(i);
    		SponsoredAd ad = sponsoredAds.get(key);
    		ad.destroy();
    	}
    	delegate = null;
    }

	public boolean isSponsoredAd(int position)
	{

		// If we already know this position is and ad, quit now
		if (sponsoredAdPositions.contains(position))
			return true;

		// Find out how far away we are from the last sponsored ad
		int[] sponsorInfo = lastSponsoredAdInfo(position);
		int sponsorPosition = sponsorInfo[0];
		int sponsorDist = sponsorInfo[1];

		// If we've never seen an ad, we have now
		if (sponsorPosition == -1)
			return true;

		// Are we 10 away from the last one?
		if (sponsorDist - 1 == 10)
			return true;

		return false;

	}

	public void performClick(int position)
	{

		// If not a sponsored ad at this position, skip it
		if (!sponsoredAdPositions.contains(position))
			return;

		// Get the SponsoredAd for this position and pass the click
		// on to its view (if it has one)
		SponsoredAd ad = sponsoredAds.get(position);
		if (ad != null)  {
			Log.e(getClass().getSimpleName(), "performClick: position " + position);
			View v = ad.getView();
			if (v != null)
				v.performClick();
		}

	}

	public int sponsoredAdCountAt(int position)
	{

		// Get the ordered set of the positions of sponsored ads whose
		// location is less than the given position
		SortedSet<Integer> positions = sponsoredAdPositions.headSet(position);

		// Return the size of that set
		int count = (positions == null) ? 0 : positions.size();
//		Log.e(getClass().getSimpleName(), "sponsoredAdCountAt: position " + position + " count " + count);
		return count;

	}

	private int[] lastSponsoredAdInfo(int position)
	{

		// Get the ordered set of all sponsored ads and return an array
		// containing the position in the pseudo-list of the nearest
		// sponsored ad to the given position and the distance to that
		// ad from the given position.
		try {
			int[] info = new int[2];
			SortedSet<Integer> positions = sponsoredAdPositions.headSet(position);
			int closest = (positions == null) ? 0 : position - positions.last();
//			Log.e(getClass().getSimpleName(), "distanceFromLastSponsoredAd: position " + position + " last " + positions.last() + " closest " + closest);
			info[0] = positions.last();
			info[1] = closest;
			return info;
		}
		catch (Exception e) {
			return new int[] { -1, 0 };
		}

	}

	private View getEmptyView()
	{
		View emptyView = new View(context);
		emptyView.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.FILL_PARENT, 0));
		return emptyView;
	}

	@Override
	public Object[] getSections()
	{
		Log.e(getClass().getSimpleName(), "getSections");
		if (delegate instanceof SectionIndexer)
			return ((SectionIndexer) delegate).getSections();
		else
			return null;
	}

	@Override
	public int getPositionForSection(int section)
	{
		Log.e(getClass().getSimpleName(), "getPositionForSection");
		if (delegate instanceof SectionIndexer)
			return ((SectionIndexer) delegate).getPositionForSection(section);
		else
			return 0;
	}

	@Override
	public int getSectionForPosition(int position)
	{
		Log.e(getClass().getSimpleName(), "getSectionForPosition");
		if (delegate instanceof SectionIndexer)
			return ((SectionIndexer) delegate).getSectionForPosition(position);
		else
			return 0;
	}

	@Override
	public boolean hasStableIds() { return true; }

	@Override
	public boolean isEnabled(int position) { return true; }

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent)
	{
		return getView(position, convertView, parent);
	}

	@Override
	public boolean isEmpty() { return getCount() == 0; }

	@Override
	public void notifyDataSetChanged()
	{
		Log.e(getClass().getSimpleName(), "notifyDataSetChanged");
		observers.notifyChanged();
	}

	@Override
	public void notifyDataSetInvalidated()
	{
		Log.e(getClass().getSimpleName(), "notifyDataSetInvalidated");
		observers.notifyInvalidated();
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer)
	{
		Log.e(getClass().getSimpleName(), "registerDataSetObserver: " + observer);
		observers.registerObserver(observer);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer)
	{
		Log.e(getClass().getSimpleName(), "unregisterDataSetObserver: " + observer);
		observers.unregisterObserver(observer);
	}

}
