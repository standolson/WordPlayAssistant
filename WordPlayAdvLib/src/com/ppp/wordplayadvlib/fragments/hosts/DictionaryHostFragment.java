package com.ppp.wordplayadvlib.fragments.hosts;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.fragments.BaseFragment;
import com.ppp.wordplayadvlib.fragments.DictionaryFragment;

public class DictionaryHostFragment extends HostFragment {

	@Override
    public BaseFragment getInitialFragment()
	{
		return new DictionaryFragment();
	}

	@Override
    public int getFragmentHelp() { return R.raw.dictionary_help; }

}
