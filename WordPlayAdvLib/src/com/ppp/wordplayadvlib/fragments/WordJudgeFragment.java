package com.ppp.wordplayadvlib.fragments;

import java.util.TreeSet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputFilter;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.WordPlayApp;
import com.ppp.wordplayadvlib.adapters.SponsoredAdAdapter;
import com.ppp.wordplayadvlib.adapters.WordJudgeAdapter;
import com.ppp.wordplayadvlib.analytics.Analytics;
import com.ppp.wordplayadvlib.database.ScrabbleDatabaseClient;
import com.ppp.wordplayadvlib.externalads.SponsoredAd;
import com.ppp.wordplayadvlib.fragments.dialog.SearchProgressDialogFragment;
import com.ppp.wordplayadvlib.fragments.dialog.SearchProgressDialogFragment.SearchProgressListener;
import com.ppp.wordplayadvlib.model.DictionaryType;
import com.ppp.wordplayadvlib.model.JudgeHistory;
import com.ppp.wordplayadvlib.model.JudgeHistoryObject;
import com.ppp.wordplayadvlib.model.JudgeSearchObject;
import com.ppp.wordplayadvlib.model.SearchType;
import com.ppp.wordplayadvlib.utils.Debug;

public class WordJudgeFragment extends BaseFragment
	implements
		View.OnClickListener,
		OnItemClickListener,
		SearchProgressListener
{

	private View rootView;
	private Button wjButton = null;
	private EditText wjText = null;
	private ListView wjListview = null;
	private SponsoredAdAdapter wjAdAdapter = null;
	private WordJudgeAdapter wjAdapter = null;

	private JudgeSearchObject judgeSearchObject = null;

	private static AsyncTask<Void, Void, Void> task;
	private LocalBroadcastManager broadcastManager;

    private TreeSet<Integer> sponsoredAdPositions = new TreeSet<Integer>();
    private SparseArray<SponsoredAd> sponsoredAdListAds = new SparseArray<SponsoredAd>();

	//
	// Activity Methods
	//

    @Override
    public void onCreate(Bundle savedInstanceState)
    {

    	super.onCreate(savedInstanceState);

    	// Create the broadcast maanger that will receive the completion
    	// and cancel callbacks of the AsyncTasks doing the searches
		broadcastManager = LocalBroadcastManager.getInstance(getActivity());

    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{

		rootView = inflater.inflate(R.layout.word_judge_fragment, container, false);

		if (savedInstanceState != null)  {
			int[] positions = savedInstanceState.getIntArray("sponsoredAdListPositions");
			sponsoredAdPositions = SponsoredAd.intArrayToTreeSet(positions);
		}

		setupWordJudgeTab();

		return rootView;
	}

	@Override
	public void onPause()
	{

		super.onPause();

		// Unregister the broadcast receiver
		broadcastManager.unregisterReceiver(wordJudgeReceiver);

	}

	@Override
	public void onResume()
	{

		super.onResume();

		Analytics.screenView(Analytics.WORD_JUDGE_SCREEN);

		// Register the broadcast receiver to receive completed and
		// cancelled notifications
		IntentFilter filter = new IntentFilter();
		filter.addAction(SEARCH_COMPLETED_INTENT);
		filter.addAction(SEARCH_CANCELED_INTENT);
		broadcastManager.registerReceiver(wordJudgeReceiver, filter);

		setButtonState();

	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState)
	{
		super.onSaveInstanceState(savedInstanceState);
    	savedInstanceState.putIntArray("sponsoredAdListPositions", SponsoredAd.treeSetToIntArray(sponsoredAdPositions));		
	}

	@Override
    public void onClick(View v)
    {
        InputMethodManager imm =
        	(InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(wjText.getWindowToken(), 0);
		getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    	startWordJudgeSearch();
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

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
    	MenuItem item = menu.findItem(R.id.clearhistory_menu);
    	if (item != null)
    		item.setVisible(true);
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{

		// Handle clearing history
		if (item.getItemId() == R.id.clearhistory_menu)  {

			Analytics.sendEvent(Analytics.HISTORY, Analytics.CLEAR_JUDGE_HISTORY, "", 0);

			JudgeHistory.getInstance().clearJudgeHistory(getActivity());
			wjAdapter.notifyDataSetChanged();
			return true;

		}

		return false;
			
	}

	//
	// Menu Support
	//

	@Override
	public String[] getDictionaryNames()
	{
		return getResources().getStringArray(R.array.word_list_names);
	}

	@Override
	public int getSelectedDictionary()
	{
		SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
    	int wjDict = prefs.getInt("wordjudgeDict", DictionaryType.DICTIONARY_ENABLE.ordinal());
    	return wjDict - 1;
	}

	@Override
	public void onSelection(int selection)
	{

		SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		DictionaryType dict = DictionaryType.fromInt(selection + 1);

		editor.putInt("wordjudgeDict", dict.ordinal());
		Debug.v("SAVE wordjudgeDict = " + dict.ordinal());
		editor.commit();

	}

    //
    // UI Setup
    //

	private void setupWordJudgeTab()
	{

        wjButton = (Button)rootView.findViewById(R.id.WordJudgeButton);
        wjButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) { startWordJudgeSearch(); }
        });

        wjText = (EditText)rootView.findViewById(R.id.WordJudgeText);
        wjText.setFilters(new InputFilter[] { commaFilter });
        wjText.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if (checkForEnterKey(v, keyCode, event))  {
					startWordJudgeSearch();
					return true;
				}
				return false;
			}
        });
        wjText.addTextChangedListener(buttonTextWatcher);

        final Button wjClearButton = (Button)rootView.findViewById(R.id.WordJudgeTextClear);
        wjClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) { wjText.setText(""); }
		});
		
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

	//
	// Search Activity Support
	//

    private void startWordJudgeSearch()
    {

		String searchString = wjText.getText().toString().toLowerCase();
		DictionaryType dictionary = DictionaryType.fromInt(getSelectedDictionary() + 1);

		judgeSearchObject = new JudgeSearchObject(searchString, dictionary);
		doWordJudgeSearch();

		wjText.setText("");

    }

    @Override
    protected void setButtonState()
    {

		String searchString = wjText.getText().toString();
		DictionaryType dictionary = DictionaryType.fromInt(getSelectedDictionary() + 1);

		wjButton.setEnabled(validateString(searchString, dictionary, false, false));
    	
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

    		startNewSearch(args);

    	}
    	else
    		Toast.makeText(getActivity(), "Cannot search for unknown words", Toast.LENGTH_SHORT).show();

    }

    private static final String SEARCH_COMPLETED_INTENT = "WordJudgeSearchCompleted";
    private static final String SEARCH_CANCELED_INTENT = "WordJudgeSearchCanceled";

	protected BroadcastReceiver wordJudgeReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();
			if (action.equals(SEARCH_COMPLETED_INTENT))  {
				JudgeSearchObject searchObject = (JudgeSearchObject) intent.getParcelableExtra("judgeSearchObject");
				onSearchComplete(searchObject);
			}
			else if (action.equals(SEARCH_CANCELED_INTENT))
				;
		}
		
	};

	private void onSearchComplete(JudgeSearchObject judgeSearchObject)
	{

		FragmentManager fm = getFragmentManager();
		SearchProgressDialogFragment dialog = null;

		this.judgeSearchObject = judgeSearchObject;

		// Find the dialog in the FragmentManager
		if (fm != null)
			dialog =
				(SearchProgressDialogFragment) fm.findFragmentByTag(SearchProgressDialogFragment.class.getName());

		// If the user cancelled, the dialog was already dismissed
		if (dialog == null)
			return;

		dialog.dismiss();

		// Update the history
		JudgeHistory.getInstance().addJudgeHistory(judgeSearchObject.getSearchString(),
													judgeSearchObject.getResult());
		JudgeHistory.getInstance().saveJudgeHistory(getActivity());

		// Update the adapter
		if (WordPlayApp.getInstance().isFreeMode())
			wjAdAdapter.notifyDataSetChanged();
		else
			wjAdapter.notifyDataSetChanged();

	}

	@Override
	public void onProgressCancel()
	{

		// Cancel the running task.  The onCancelled handler there will
		// send a message to the broadcast receiver that will get the
		// user back to where they came from.
		task.cancel(true);

	}

    private void doWordJudgeSearch()
    {

		task = new AsyncTask<Void, Void, Void>() {

			@Override
			protected void onPreExecute()
			{
				SearchProgressDialogFragment dialog =
					SearchProgressDialogFragment.newInstance(SearchFragment.class.getName());
			    dialog.show(getFragmentManager(), SearchProgressDialogFragment.class.getName());		
			}

			@Override
			protected Void doInBackground(Void... params)
			{

    			ScrabbleDatabaseClient client = new ScrabbleDatabaseClient();
    			String[] words = judgeSearchObject.getSearchString().split(",");

				try {
    				if (words.length > 1)  {
    					judgeSearchObject.setResult(
    							client.judgeWordList(
    									words,
    									judgeSearchObject.getDictionary()));
    				}
    				else
    					judgeSearchObject.setResult(
    							client.judgeWord(
    									judgeSearchObject.getSearchString().replace(",", ""),
    									judgeSearchObject.getDictionary()));
    			}
    			catch (Exception e) {
    				judgeSearchObject.setException(e);
    			}

				return null;

			}

			@Override
			protected void onPostExecute(Void result)
			{
				Intent intent = new Intent();
				intent.setAction(SEARCH_COMPLETED_INTENT);
				intent.putExtra("judgeSearchObject", judgeSearchObject);
				broadcastManager.sendBroadcast(intent);
			}

			@Override
			protected void onCancelled(Void result)
			{
				Intent intent = new Intent();
				intent.setAction(SEARCH_CANCELED_INTENT);
				broadcastManager.sendBroadcast(intent);
			}

			@Override
			protected void onCancelled()
			{
				Intent intent = new Intent();
				intent.setAction(SEARCH_CANCELED_INTENT);
				broadcastManager.sendBroadcast(intent);
			}

		};

		task.execute();

    }

}
