package com.ppp.wordplayadvlib.fragments;

import com.ppp.wordplayadvlib.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class WebViewFragment extends BaseFragment {

	private WebView rootView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{

		rootView = (WebView) new WebView(getActivity());

        // If this something from the help system, show it as white
        // text on black
        String content = getArguments().getString("content");
        if (content != null)  {
	        content = "<font color=\"white\">" + content + "</font>";
	    	rootView.setBackgroundColor(getResources().getColor(R.color.black));
	        rootView.loadData(content, "text/html", "utf-8");
        }

		return rootView;

	}

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {

    	int[] ids = {
    		R.id.dictionary_menu,
    		R.id.settings_menu,
    		R.id.dictionary_reinstall_menu,
    		R.id.showhelp_menu
    	};

    	for (int id : ids)  {
    		MenuItem item = menu.findItem(id);
	    	if (item != null)
	    		menu.removeItem(item.getItemId());
    	}

    }

}
