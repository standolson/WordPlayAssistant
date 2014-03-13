package com.ppp.wordplayadvlib.fragments.hosts;

import com.ppp.wordplayadvlib.fragments.BaseFragment;
import com.ppp.wordplayadvlib.fragments.HistoryFragment;

public class HistoryHostFragment extends HostFragment {

	@Override
    public BaseFragment getInitialFragment()
	{
		return new HistoryFragment();
	}

}
