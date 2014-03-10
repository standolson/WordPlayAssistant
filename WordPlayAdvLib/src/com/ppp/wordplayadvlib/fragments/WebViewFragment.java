package com.ppp.wordplayadvlib.fragments;

import android.R;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class WebViewFragment extends BaseFragment {

	private WebView rootView;

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

}
