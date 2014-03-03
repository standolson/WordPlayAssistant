package com.ppp.wordplayadvlib.activities;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;

public class BaseActivity extends ActionBarActivity {

    //
    // Fragments
    //

	protected int getFragmentContainer() { return android.R.id.content; }
	protected Fragment getInitialFragment() { return null; }

	protected void pushToStack(Fragment newFragment)
	{

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

		// Replace whatever is in the fragment_container view with this fragment
		// and add the transaction to the back stack
		ft.replace(getFragmentContainer(), newFragment, newFragment.getClass().getName());
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		ft.addToBackStack(newFragment.getClass().getName());

		// Commit the transaction
		ft.commit();

	}

	protected void replaceStack(Fragment newFragment)
	{

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

		// Replace without history
		ft.replace(getFragmentContainer(), newFragment, newFragment.getClass().getName());
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.commit();

	}

}
