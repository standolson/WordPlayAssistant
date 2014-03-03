package com.ppp.wordplayadvlib.fragments.hosts;

import com.ppp.wordplayadvlib.fragments.AnagramsFragment;
import com.ppp.wordplayadvlib.fragments.BaseFragment;

public class AnagramsHostFragment extends HostFragment {

	@Override
    public BaseFragment getInitialFragment()
	{
		return new AnagramsFragment();
	}

}
