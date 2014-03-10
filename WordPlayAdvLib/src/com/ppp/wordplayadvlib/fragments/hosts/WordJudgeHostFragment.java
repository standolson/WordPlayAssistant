package com.ppp.wordplayadvlib.fragments.hosts;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.fragments.BaseFragment;
import com.ppp.wordplayadvlib.fragments.WordJudgeFragment;

public class WordJudgeHostFragment extends HostFragment {

	@Override
    public BaseFragment getInitialFragment()
	{
		return new WordJudgeFragment();
	}

	@Override
    public int getFragmentHelp() { return R.raw.wordjudge_help; }

}
