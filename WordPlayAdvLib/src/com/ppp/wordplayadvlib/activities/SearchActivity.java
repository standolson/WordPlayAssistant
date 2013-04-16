package com.ppp.wordplayadvlib.activities;

import com.ppp.wordplayadvlib.fragments.BaseFragment;
import com.ppp.wordplayadvlib.fragments.SearchFragment;

public class SearchActivity extends BaseActivity {

	@Override
	protected BaseFragment getInitialFragment()
	{
		return new SearchFragment();
	}

}
