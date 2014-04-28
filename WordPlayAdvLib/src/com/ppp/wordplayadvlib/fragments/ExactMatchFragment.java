package com.ppp.wordplayadvlib.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.analytics.Analytics;

public class ExactMatchFragment extends BaseFragment {

	private View rootView;

	private String word;

	public ExactMatchFragment() { super(); }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{

		rootView = inflater.inflate(R.layout.exact_match_fragment, container, false);

		Bundle args = getArguments();
	    String contents = args.getString("ExactMatchResult");
	    word = args.getString("ExactMatchString");

	    TextView text = (TextView) rootView.findViewById(R.id.em_textview);
	    text.setText(contents);

		return rootView;

	}

	@Override
	public void onResume()
	{
		super.onResume();
		Analytics.screenView(Analytics.EXACT_MATCH_SCREEN);
	}

}
