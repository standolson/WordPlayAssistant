package com.ppp.wordplayadvlib.activities;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.ppp.wordplayadvlib.R;

public class UserPreferenceActivity extends PreferenceActivity
{

	public void onCreate(Bundle savedInstanceState)
	{

		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.user_preference_activity);

	}

}
