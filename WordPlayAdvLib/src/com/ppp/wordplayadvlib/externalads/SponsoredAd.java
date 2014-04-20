package com.ppp.wordplayadvlib.externalads;

import android.content.Context;
import android.view.View;

import com.ppp.wordplayadvlib.adapters.SponsoredAdAdapter;

public class SponsoredAd {

	public enum PlacementType {
		Unknown,
		ListSearchResult,
		ZeroSearchResult,
	}

	public interface EventCallback {
		public void onLoaded(SponsoredAd ad);
		public void onError(SponsoredAd ad);
	}

	protected Context context;
	protected PlacementType placementType;
	protected View displayView;
	protected int listPosition;
	protected boolean isLoaded;

	protected SponsoredAdAdapter adapter;
	protected EventCallback eventCallback;
	protected int gridWidth;

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

}
