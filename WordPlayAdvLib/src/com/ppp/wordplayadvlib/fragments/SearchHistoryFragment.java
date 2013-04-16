package com.ppp.wordplayadvlib.fragments;

import java.util.LinkedList;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.appdata.History;
import com.ppp.wordplayadvlib.appdata.HistoryObject;
import com.ppp.wordplayadvlib.appdata.SearchType;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class SearchHistoryFragment extends ListFragment {

//	private AdView adView;
	private HistoryAdapter adapter;

	public SearchHistoryFragment() { super(); }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.search_history_empty, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		
	    super.onActivityCreated(savedInstanceState);

		// This fragment does not have menu items...
		setHasOptionsMenu(false);

		// ...and wants to be retained on reconfiguration
		setRetainInstance(true);

//      if (WordPlayApp.isFreeMode())  {
//          LinearLayout header_layout = (LinearLayout)View.inflate(this, R.layout.admob_listview_footer, null);
//          adView = (AdView)header_layout.findViewById(R.id.listview_ad);
//          adView.loadAd(new AdRequest());
//          ListView list = (ListView)findViewById(android.R.id.list);
//          list.addHeaderView(adView);
//      }

        adapter = new HistoryAdapter(getActivity(), R.layout.search_history, History.getInstance().getHistory());
        setListAdapter(adapter);

    }

//	public void onResume()
//	{
//		super.onResume();
//		if (WordPlayApp.isFreeMode())
//			adView.loadAd(new AdRequest());
//	}

//	public void onDestroy()
//	{
//		if (adView != null)
//			adView.destroy();
//		super.onDestroy();
//	}

	public void onListItemClick(ListView l, View v, int position, long id)
	{

		Intent intent = null;

//		if (WordPlayApp.isFreeMode())
//			position -= 1;

//		HistoryObject elem = History.getInstance().getHistory().get(position);
//		intent = new Intent(getActivity(), SearchResultActivity.class);
//		intent.putExtra("SearchString", elem.getSearchString());
//		if (elem.getSearchType() == SearchType.OPTION_ANAGRAMS)
//			intent.putExtra("BoardString", elem.getBoardString());
//		intent.putExtra("SearchType", elem.getSearchType().ordinal());
//		intent.putExtra("Dictionary", elem.getDictionary().ordinal());
//		intent.putExtra("WordScores", elem.getScoreState().ordinal());
//		intent.putExtra("WordSort", elem.getSortState().ordinal());
//		try {
//			startActivity(intent);
//		}
//		catch (Exception e) {}

	}

	private class HistoryAdapter extends ArrayAdapter<HistoryObject> {
		
		HistoryAdapter(Context ctx, int rowLayoutId, LinkedList<HistoryObject> items)
		{
			super(ctx, rowLayoutId, items);
		}

        public View getView(int position, View convertView, ViewGroup parent)
        {
        	
            View v = convertView;

            if (v == null)  {
                LayoutInflater vi = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.search_history, null);
            }

            HistoryObject elem = getItem(position);
            if (elem != null)  {
            	TextView searchStrView = (TextView)v.findViewById(R.id.sh_search_string);
            	TextView searchTypeView = (TextView)v.findViewById(R.id.sh_search_type);
            	TextView searchDictView = (TextView)v.findViewById(R.id.sh_search_dict);
            	if (searchStrView != null)  {
	            	if (elem.getSearchType() == SearchType.OPTION_ANAGRAMS)  {
	            		if (elem.getBoardString().length() != 0)
	            			searchStrView.setText(elem.getSearchString() +
	            									" (Board: '" + elem.getBoardString() + "')");
	            		else
	            			searchStrView.setText(elem.getSearchString());
	            	}
	            	else
	            		searchStrView.setText(elem.getSearchString());
            	}
            	if (searchTypeView != null)
            		searchTypeView.setText(elem.getSearchTypeString());
            	if (searchDictView != null)
            		searchDictView.setText(elem.getDictionaryString());
            }
            
            return v;
            
        }

	}

}
