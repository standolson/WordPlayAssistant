package com.ppp.wordplayadvlib.fragments.tablet;

import java.util.TreeSet;

import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.WordPlayApp;
import com.ppp.wordplayadvlib.adapters.SponsoredAdAdapter;
import com.ppp.wordplayadvlib.adapters.WordJudgeAdapter;
import com.ppp.wordplayadvlib.externalads.SponsoredAd;
import com.ppp.wordplayadvlib.fragments.BaseFragment;
import com.ppp.wordplayadvlib.fragments.SearchFragment;
import com.ppp.wordplayadvlib.fragments.WordJudgeFragment;
import com.ppp.wordplayadvlib.model.DictionaryType;
import com.ppp.wordplayadvlib.model.JudgeHistory;
import com.ppp.wordplayadvlib.model.JudgeHistoryObject;
import com.ppp.wordplayadvlib.model.SearchType;

public class WordJudgeAdapterFragment extends BaseFragment
	implements
		OnItemClickListener
{

	private View rootView;

	private WordJudgeFragment wjFragment;

	private ListView wjListview = null;
	private SponsoredAdAdapter wjAdAdapter = null;
	private WordJudgeAdapter wjAdapter = null;

    private TreeSet<Integer> sponsoredAdPositions = new TreeSet<Integer>();
    private SparseArray<SponsoredAd> sponsoredAdListAds = new SparseArray<SponsoredAd>();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {

    	super.onCreate(savedInstanceState);

    	// Find the WordJudgeFragment that owns us
    	if ((wjFragment == null) && (getParentFragment() != null) && getParentFragment() instanceof WordJudgeFragment)
    		wjFragment = (WordJudgeFragment) getParentFragment();

    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{

		rootView = inflater.inflate(R.layout.word_judge_adapter_fragment, container, false);

		if (savedInstanceState != null)  {
			int[] positions = savedInstanceState.getIntArray("sponsoredAdListPositions");
			sponsoredAdPositions = SponsoredAd.intArrayToTreeSet(positions);
		}

		setupWordJudgeAdapterFragment();

		return rootView;

	}

	@Override
	public void onPause()
	{

		super.onPause();

		// Pause all AdMob activity
		if (wjAdAdapter != null)
			wjAdAdapter.pause();

	}

	@Override
	public void onResume()
	{

		super.onResume();

		// Resume all AdMob activity
		if (wjAdAdapter != null)
			wjAdAdapter.resume();

	}

	@Override
	public void onDestroy()
	{

		super.onDestroy();

		// Stop all AdMob activity
		if (wjAdAdapter != null)
			wjAdAdapter.destroy();

	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState)
	{
		super.onSaveInstanceState(savedInstanceState);
    	savedInstanceState.putIntArray("sponsoredAdListPositions", SponsoredAd.treeSetToIntArray(sponsoredAdPositions));		
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id)
    {

		// If we're showing ads, we need to do some special stuff
		if (wjAdAdapter != null)  {

			// If the user is trying to click on the sponsored ad,
			// pass that click off to that view and be done.  If
			// not, adjust the position according to the number of
			// sponsored ads seen at the location.
			if (wjAdAdapter.isSponsoredAd(position))  {
				wjAdAdapter.performClick(position);
				return;
			}
			else
				position -= wjAdAdapter.sponsoredAdCountAt(position);

		}

		startJudgeHistorySearch(position);

    }

	//
	// UI Setup
	//

	private void setupWordJudgeAdapterFragment()
	{

        wjListview = (ListView)rootView.findViewById(R.id.wordjudge_listview);

        // Create the content adapter
        wjAdapter =
			new WordJudgeAdapter(getActivity(),
									R.layout.judge_history,
									JudgeHistory.getInstance().getJudgeHistory());

		// If this is the free version, create that adapter
		if (WordPlayApp.getInstance().isFreeMode())
			wjAdAdapter = new SponsoredAdAdapter(getActivity(),
													wjAdapter,
													WordPlayApp.getInstance().getWordJudgeAdUnitIds(),
													sponsoredAdPositions,
													sponsoredAdListAds);

		// Attach the adapter to the ListView
        wjListview.setAdapter(wjAdAdapter != null ? wjAdAdapter : wjAdapter);
        wjListview.setOnItemClickListener(this);

	}

	@Override
	public boolean isTablet()
	{
		return wjFragment != null ? wjFragment.isTablet() : false;
	}

	//
	// Fragment IPC
	//

	public void updateJudgeAdapter()
	{
		if (wjAdAdapter != null)
			wjAdAdapter.notifyDataSetChanged();
		else
			wjAdapter.notifyDataSetChanged();
	}

    //
    // Word Judge Search
    //

    private void startJudgeHistorySearch(int position)
    {

    	JudgeHistoryObject elem = JudgeHistory.getInstance().getJudgeHistory().get(position);

    	if (elem.getState())  {

    		if (elem.getWord().contains(","))
    			return;

    		Bundle args = new Bundle();
			args.putString("SearchString", elem.getWord());
			args.putInt("SearchType", SearchType.OPTION_DICTIONARY_EXACT_MATCH.ordinal());
			args.putInt("Dictionary", DictionaryType.DICTIONARY_DICT_DOT_ORG.ordinal());

			if (wjFragment != null)
				wjFragment.startNewSearch(args);
			else
				startNewSearch(args);

    	}
    	else {
    		if (wjFragment != null)
    			wjFragment.removeExistingFragment(SearchFragment.class.getName());
    		Toast.makeText(getActivity(), getString(R.string.WordJudgeInvalidSearch), Toast.LENGTH_SHORT).show();
    	}

    }

}
