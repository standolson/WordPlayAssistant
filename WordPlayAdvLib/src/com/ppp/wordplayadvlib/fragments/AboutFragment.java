package com.ppp.wordplayadvlib.fragments;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.utils.Debug;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

public class AboutFragment extends BaseFragment
	implements OnClickListener
{

	private View rootView;

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{

		rootView = inflater.inflate(R.layout.about_fragment, null);

		TextView releaseNotes = (TextView) rootView.findViewById(R.id.release_notes);
		releaseNotes.setOnClickListener(this);
		TextView contactUs = (TextView) rootView.findViewById(R.id.contact_us);
		contactUs.setOnClickListener(this);

		return rootView;
		
	}

	@Override
	public void onClick(View v)
	{

		int id = v.getId();

		if (id == R.id.release_notes)
			Debug.e("Release Notes");

		else if (id == R.id.contact_us)
			Debug.e("Contact Us");

	}

}
