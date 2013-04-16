package com.ppp.wordplayadvlib.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import com.ppp.wordplayadvlib.fragments.BaseFragment;

public class BaseActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{

		super.onCreate(savedInstanceState);

		if (savedInstanceState == null)  {
			BaseFragment fragment = getInitialFragment();
			fragment.setArguments(getIntent().getExtras());
			replaceStack(fragment);
		}

	}

    //
    // Fragments
    //

	protected int getFragmentContainer() { return android.R.id.content; }
	protected BaseFragment getInitialFragment() { return null; }

	protected void pushToStack(BaseFragment newFragment)
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

	protected void replaceStack(BaseFragment newFragment)
	{

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

		// Replace without history
		ft.replace(getFragmentContainer(), newFragment, newFragment.getClass().getName());
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.commit();

	}

}
