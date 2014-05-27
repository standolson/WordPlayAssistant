package com.ppp.wordplayadvlib.model;

public class SearchThread extends Thread {

	private SearchObject searchObject;
	private boolean isReconfigured;
	
	public SearchThread(Runnable r, SearchObject o)
	{
		super(r);
		searchObject = o;
		isReconfigured = false;
	}
	
	public void setSearchObject(SearchObject o) { searchObject = o; }
	public SearchObject getSearchObject() { return searchObject; }

	public void setReconfigured(boolean value) { isReconfigured = value; }
	public boolean isReconfigured() { return isReconfigured; }

}
