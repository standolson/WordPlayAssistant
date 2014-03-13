package com.ppp.wordplayadvlib.fragments;

import java.util.LinkedList;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.appdata.History;
import com.ppp.wordplayadvlib.appdata.HistoryObject;
import com.ppp.wordplayadvlib.appdata.SearchType;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class HistoryFragment extends BaseFragment
	implements OnItemClickListener
{

	private View rootView;
	private ListView listView;
	private HistoryAdapter adapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{

		rootView = inflater.inflate(R.layout.history_fragment, container, false);

		listView = (ListView) rootView.findViewById(R.id.history_list);
		listView.setOnItemClickListener(this);
        adapter = new HistoryAdapter(getActivity(), R.layout.search_history, History.getInstance().getHistory());
        listView.setAdapter(adapter);

		return rootView;

	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{

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
