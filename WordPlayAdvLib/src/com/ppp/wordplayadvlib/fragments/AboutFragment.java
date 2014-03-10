package com.ppp.wordplayadvlib.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ppp.wordplayadvlib.Constants;
import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.WordPlayApp;
import com.ppp.wordplayadvlib.utils.Utils;

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
    	ImageView dictOrgImage = (ImageView) rootView.findViewById(R.id.powered_by_image);
    	dictOrgImage.setOnClickListener(this);
    	TextView version = (TextView) rootView.findViewById(R.id.version);
    	version.setText(getString(R.string.version, WordPlayApp.appVersionName));

    	if (WordPlayApp.getInstance().isFreeMode())  {
    		TextView buyIt = (TextView) rootView.findViewById(R.id.buy_it);
    		buyIt.setOnClickListener(this);
    	}
    	else
    		rootView.findViewById(R.id.buy_it_layout).setVisibility(View.GONE);

		return rootView;
		
	}

	@Override
	public void onClick(View v)
	{

		int id = v.getId();

		if (id == R.id.release_notes)
			showReleaseNotes();

		else if (id == R.id.contact_us)
			contactUs();

		else if (id == R.id.powered_by_image)
			gotoDictDotOrg();

		else if (id == R.id.buy_it)
			buyIt();

	}

	private void showReleaseNotes()
	{

		Bundle args = new Bundle();
		args.putString("content", Utils.getHelpText(getActivity(), "Release Notes", R.raw.release_notes));

		WebViewFragment fragment = new WebViewFragment();
		fragment.setArguments(args);
		pushToStack(fragment);

	}

	private void contactUs()
	{

		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("message/rfc822");
		intent.putExtra(Intent.EXTRA_EMAIL, new String[] { Constants.EmailAddress });
		intent.putExtra(Intent.EXTRA_SUBJECT,
			"Comments on " + getString(R.string.app_name) + " v" + WordPlayApp.appVersionName);
    	intent.putExtra(android.content.Intent.EXTRA_TEXT, "");

    	try {
    		startActivity(intent);
    	}
    	catch (ActivityNotFoundException exception) {
    		Utils.configureEmailAlert(getActivity());
    	}

	}

	private void gotoDictDotOrg()
	{

		Intent myIntent =
			new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.DictOrgWebAddress));

		try {
			startActivity(myIntent);
		}
		catch (Exception e) {}
		
	}

	private void buyIt()
	{

		Intent myIntent =
			new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.MarketPaidWebAddress));

		try {
			startActivity(myIntent);
		}
		catch (Exception e) {}
		
	}

}
