package com.ppp.wordplayadvlib.fragments.hosts;

import com.ppp.wordplayadvlib.fragments.BaseFragment;
import com.ppp.wordplayadvlib.fragments.ThesaurusFragment;

public class ThesaurusHostFragment extends HostFragment {

	@Override
    public BaseFragment getInitialFragment()
	{
		return new ThesaurusFragment();
	}

}
