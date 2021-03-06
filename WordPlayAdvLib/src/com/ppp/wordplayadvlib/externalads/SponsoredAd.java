package com.ppp.wordplayadvlib.externalads;

import java.util.TreeSet;

import android.content.Context;
import android.view.View;

import com.ppp.wordplayadvlib.adapters.SponsoredAdAdapter;

public class SponsoredAd {

	public enum PlacementType {
		Unknown,
		ListSearchResult,
		ZeroSearchResult,
		Interstitial
	}

	public interface EventCallback {
		public void onLoaded(SponsoredAd ad);
		public void onError(SponsoredAd ad);
		public void onOpened(SponsoredAd ad);
		public void onClosed(SponsoredAd ad);
	}

	protected Context context;
	protected PlacementType placementType;
	protected View displayView;
	protected int listPosition;
	protected boolean isLoaded;

	protected SponsoredAdAdapter adapter;
	protected EventCallback eventCallback;
	protected int gridWidth;

	public SponsoredAd(Context context)
	{
		this.context = context;
	}

	public SponsoredAd(Context context, PlacementType placementType)
	{
		this.context = context;
		this.placementType = placementType;
		this.isLoaded = false;
	}

	public SponsoredAd(Context context, PlacementType placementType, int listPosition)
	{
		this.context = context;
		this.placementType = placementType;
		this.listPosition = listPosition;
		this.isLoaded = false;
	}

	public PlacementType getPlacementType() { return placementType; }

	public void setView(View v) { displayView = v; }
	public View getView() { return displayView; }

	public int getListPosition() { return listPosition; }

	public boolean isLoaded() { return isLoaded; }

	public void setEventCallback(EventCallback callback) { eventCallback = callback; }

	public void setSponsoredAdAdapter(SponsoredAdAdapter adapter) { this.adapter = adapter; }

	public void pause() {}

	public void resume() {}

	public void destroy()
	{
		adapter = null;
		displayView = null;
	}

	public String toString()
	{
		return "placement: " + placementType + " position: " + listPosition + " loaded: " + isLoaded;
	}

    //
    // Sponsored Ad Support
    //

    public static int[] treeSetToIntArray(TreeSet<Integer> set)
    {

    	if (set == null)
    		return null;

		int[] retval = new int[set.size()];
		int i = 0;
        for (Integer aSet : set) {
            retval[i] = aSet;
            i += 1;
        }

    	return retval;

    }

    public static TreeSet<Integer> intArrayToTreeSet(int[] ints)
    {
    	TreeSet<Integer> retval = new TreeSet<Integer>();
    	if (ints == null)
    		return retval;
    	for (int i : ints)
    		retval.add(i);
    	return retval;
    }

}
