package com.ppp.wordplayadvlib.fragments.hosts;

import com.ppp.wordplayadvlib.fragments.AboutFragment;
import com.ppp.wordplayadvlib.fragments.BaseFragment;

public class AboutHostFragment extends HostFragment {

	@Override
    public BaseFragment getInitialFragment()
	{
		return new AboutFragment();
	}

}
