package com.ppp.wordplayadvlib.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.analytics.Analytics;
import com.ppp.wordplayadvlib.database.ScrabbleDatabaseClient;
import com.ppp.wordplayadvlib.fragments.dialog.SearchProgressDialogFragment;
import com.ppp.wordplayadvlib.fragments.dialog.SearchProgressDialogFragment.SearchProgressListener;
import com.ppp.wordplayadvlib.fragments.tablet.WordJudgeAdapterFragment;
import com.ppp.wordplayadvlib.model.DictionaryType;
import com.ppp.wordplayadvlib.model.JudgeHistory;
import com.ppp.wordplayadvlib.model.JudgeSearchObject;
import com.ppp.wordplayadvlib.utils.Debug;

public class WordJudgeFragment extends BaseFragment
	implements
		SearchProgressListener
{

	private View rootView;

	private View definitionsView;
	private WordJudgeAdapterFragment adapterFragment;
	private Button wjButton = null;
	private EditText wjText = null;

	private JudgeSearchObject judgeSearchObject = null;

	private static AsyncTask<Void, Void, Void> task;
	private LocalBroadcastManager broadcastManager;

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

			// Clear the history, update the list of words, and if
			// running on a tablet, remove any existing SearchFragment
			JudgeHistory.getInstance().clearJudgeHistory(getActivity());
			adapterFragment.updateJudgeAdapter();
			removeExistingFragment(SearchFragment.class.getName());

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

        // Find the adapter fragment
        String tag = WordJudgeAdapterFragment.class.getName();
		adapterFragment =
			(WordJudgeAdapterFragment) getChildFragmentManager().findFragmentByTag(tag);

		// If we currently don't have an WordJudgeAdapterFragment, create one and
		// add it to our container
		if (adapterFragment == null)  {

			// Create a transaction and the fragment we're adding
			FragmentTransaction ft = getChildFragmentManager().beginTransaction();
			adapterFragment =
				(WordJudgeAdapterFragment) Fragment.instantiate(getActivity(), tag);

			// Add the fragment and commit the transaction
			ft.add(R.id.word_judge_adapter_container, adapterFragment, adapterFragment.getClass().getName());
			ft.commit();

		}

		// Find the container that will hold the exact match search.  On tablets,
		// this is used when the user clicks a word from the adapter fragment.
		definitionsView = rootView.findViewById(R.id.word_judge_definitions_container);

	}

	//
	// Tablet Support
	//

	public void removeExistingFragment(String tag)
	{
		SearchFragment oldFragment = (SearchFragment) getChildFragmentManager().findFragmentByTag(tag);
		if (isTablet() && (oldFragment != null))  {
			FragmentTransaction ft = getChildFragmentManager().beginTransaction();
			ft.remove(oldFragment);
			ft.commit();
		}
	}

	@Override
	public boolean isTablet() { return definitionsView != null; }

	@Override
	public void startNewSearch(Bundle args)
	{

		if (isTablet())  {

			String tag = SearchFragment.class.getName();

			// See if we already have a SearchFragment attached and if so
			// detach it
			removeExistingFragment(tag);

			// Create the new SearchFragment
			SearchFragment fragment = (SearchFragment) SearchFragment.instantiate(getActivity(), tag);
			fragment.setArguments(args);

			// Add it
			FragmentTransaction ft = getChildFragmentManager().beginTransaction();
			ft.add(R.id.word_judge_definitions_container, fragment, fragment.getClass().getName());
			ft.commit();

		}
		else
			startNewSearch(args);
		
	}

	//
	// Search Activity Support
	//

    private void startWordJudgeSearch()
    {

		String searchString = wjText.getText().toString().toLowerCase();
		DictionaryType dictionary = DictionaryType.fromInt(getSelectedDictionary() + 1);

		// Get rid of any existing SearchFragment when in tablet mode
    	removeExistingFragment(SearchFragment.class.getName());

    	// Start a new search
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
		adapterFragment.updateJudgeAdapter();

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
