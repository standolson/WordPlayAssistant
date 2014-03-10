package com.ppp.wordplayadvlib.fragments.hosts;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.fragments.BaseFragment;
import com.ppp.wordplayadvlib.fragments.CrosswordsFragment;

public class CrosswordsHostFragment extends HostFragment {

	@Override
    public BaseFragment getInitialFragment()
	{
		return new CrosswordsFragment();
	}

	@Override
    public int getFragmentHelp() { return R.raw.crosswords_help; }

}
