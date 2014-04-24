package com.ppp.wordplayadvlib.fragments;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.WordPlayApp;
import com.ppp.wordplayadvlib.adapters.ScoredWordListAdapter;
import com.ppp.wordplayadvlib.adapters.SponsoredAdAdapter;
import com.ppp.wordplayadvlib.adapters.WordDefinitionsAdapter;
import com.ppp.wordplayadvlib.adapters.WordListAdapter;
import com.ppp.wordplayadvlib.analytics.Analytics;
import com.ppp.wordplayadvlib.appdata.DictionaryType;
import com.ppp.wordplayadvlib.appdata.History;
import com.ppp.wordplayadvlib.appdata.ScoredWord;
import com.ppp.wordplayadvlib.appdata.SearchObject;
import com.ppp.wordplayadvlib.appdata.SearchType;
import com.ppp.wordplayadvlib.appdata.WordDefinition;
import com.ppp.wordplayadvlib.appdata.WordSortState;
import com.ppp.wordplayadvlib.database.ScrabbleDatabaseClient;
import com.ppp.wordplayadvlib.dialogs.AppErrDialog;
import com.ppp.wordplayadvlib.exceptions.WifiAuthException;
import com.ppp.wordplayadvlib.exceptions.WordPlayException;
import com.ppp.wordplayadvlib.fragments.dialog.SearchProgressDialogFragment;
import com.ppp.wordplayadvlib.fragments.dialog.SearchProgressDialogFragment.SearchProgressListener;
import com.ppp.wordplayadvlib.networking.NetworkUtils;
import com.ppp.wordplayadvlib.networking.RFC2229;
import com.ppp.wordplayadvlib.utils.Debug;

public class SearchFragment extends BaseFragment
	implements
		OnItemClickListener,
		SearchProgressListener
{

	private View rootView;
	private ListView searchListView;
	private TextView elapsedTextView;
	private View zeroView;
	private SponsoredAdAdapter adAdapter = null;
	
	private RFC2229 dictServer;	
	private SearchObject searchObject;

	private static AsyncTask<Void, Void, Void> task;
	private LocalBroadcastManager broadcastManager;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{

		super.onCreate(savedInstanceState);

	    // Restore the SearchObject
		if (savedInstanceState != null)
			searchObject = savedInstanceState.getParcelable("searchObject");

	    // Create the new connection to the dictionary server used for this search
    	dictServer = new RFC2229();

    	// Create the broadcast maanger that will receive the completion
    	// and cancel callbacks of the AsyncTasks doing the searches
		broadcastManager = LocalBroadcastManager.getInstance(getActivity());

	}

	@Override
	public void onDestroy()
	{

		super.onDestroy();

		// Stop all AdMob activities
		if (adAdapter != null)
			adAdapter.destroy();

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{

		rootView = inflater.inflate(R.layout.search_fragment, null);

		searchListView = (ListView) rootView.findViewById(R.id.search_result_list);
	    searchListView.setOnItemClickListener(this);

	    zeroView = rootView.findViewById(R.id.zero_results);

	    elapsedTextView = (TextView) rootView.findViewById(R.id.elapsed_time);

		return rootView;

	}

	@Override
	public void onPause()
	{

		super.onPause();

		// Unregister the broadcast receiver
		broadcastManager.unregisterReceiver(searchReceiver);

		// Pause all AdMob activity
		if (adAdapter != null)
			adAdapter.pause();

	}

	@Override
	public void onResume()
	{

		super.onResume();

		// Resume all AdMob activity
		if (adAdapter != null)
			adAdapter.resume();

		// Register the broadcast receiver to receive completed and
		// cancelled notifications
		IntentFilter filter = new IntentFilter();
		filter.addAction(SEARCH_COMPLETED_INTENT);
		filter.addAction(SEARCH_CANCELED_INTENT);
		broadcastManager.registerReceiver(searchReceiver, filter);

		// If we've already done a search, don't do another
		if (searchObject != null)  {
			displayResults(false);
			return;
		}

		// Create the SearchObject
		searchObject = new SearchObject(getArguments());

		// Add this search to the history
		History.getInstance().addHistory(getArguments());

    	// Execute the search
    	switch (searchObject.getSearchType())  {
			case OPTION_DICTIONARY_EXACT_MATCH:
				onExactMatch();
				break;
			case OPTION_DICTIONARY_STARTS_WITH:
    			onStartsWith();
    			break;
    		case OPTION_DICTIONARY_ENDS_WITH:
    			onEndsWith();
    			break;
    		case OPTION_DICTIONARY_CONTAINS:
    			onContains();
    			break;
    		case OPTION_CROSSWORDS:
    			onCrosswords();
    			break;
    		case OPTION_ANAGRAMS:
    			onAnagram();
    			break;
    		case OPTION_THESAURUS:
    			onThesaurus();
    			break;
    		default:
    			break;
    	}

	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState)
	{
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putParcelable("searchObject", searchObject);
	}

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id)
	{

		String word = null;

		if ((searchObject.getSearchType() == SearchType.OPTION_DICTIONARY_EXACT_MATCH) &&
				(searchObject.getDictionary().isNormalDict()))  {

			String defn = searchObject.getDefinition().getDefinitionAt(position);
			Bundle args = new Bundle();
			args.putString("ExactMatchString", searchObject.getSearchString());
			args.putString("ExactMatchResult", defn);
			args.putBoolean("MenusOn", true);

			ExactMatchFragment fragment = new ExactMatchFragment();
			fragment.setArguments(args);
			pushToStack(fragment);

		}
		else {

			// If this is the free version, adjust the position
			if (WordPlayApp.getInstance().isFreeMode())
				position = adAdapter.getRealPosition(position);

			Log.e(getClass().getSimpleName(), "onItemClick: position " + position);

			if (searchObject.getWordScores().isScored())
				word = searchObject.getScoredWordList().get(position).getWord();
			else
				word = searchObject.getWordList().get(position);

			Bundle args = new Bundle();
			args.putString("SearchString", word);
			args.putInt("SearchType", SearchType.OPTION_DICTIONARY_EXACT_MATCH.ordinal());
			if (searchObject.getSearchType() == SearchType.OPTION_ANAGRAMS)
				args.putInt("Dictionary", DictionaryType.DICTIONARY_DICT_DOT_ORG.ordinal());
			else {
				if (searchObject.getDictionary().isScrabbleDict())
					args.putInt("Dictionary", DictionaryType.DICTIONARY_DICT_DOT_ORG.ordinal());
				else
					args.putInt("Dictionary", searchObject.getDictionary().ordinal());
			}

			startNewSearch(args);

		}

	}

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {

    	int[] ids = {
    		R.id.dictionary_menu,
    		R.id.settings_menu,
    		R.id.dictionary_reinstall_menu,
    		R.id.showhelp_menu
    	};

    	for (int id : ids)  {
    		MenuItem item = menu.findItem(id);
	    	if (item != null)
	    		menu.removeItem(item.getItemId());
    	}

    }

	//
	// Search
	//

    private static final String SEARCH_COMPLETED_INTENT = "SearchCompleted";
    private static final String SEARCH_CANCELED_INTENT = "SearchCanceled";

	protected BroadcastReceiver searchReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();
			if (action.equals(SEARCH_COMPLETED_INTENT))  {
				SearchObject searchObject = (SearchObject) intent.getParcelableExtra("searchObject");
				onSearchComplete(searchObject);
			}
			else if (action.equals(SEARCH_CANCELED_INTENT))
				popStack();
		}
		
	};

	@Override
	public void onProgressCancel()
	{

		// Cancel the running task.  The onCancelled handler there will
		// send a message to the broadcast receiver that will get the
		// user back to where they came from.
		task.cancel(true);

	}

	private void onSearchComplete(SearchObject searchObject)
	{

		FragmentManager fm = getFragmentManager();
		SearchProgressDialogFragment dialog = null;

		this.searchObject = searchObject;

		// Find the dialog in the FragmentManager
		if (fm != null)
			dialog =
				(SearchProgressDialogFragment) fm.findFragmentByTag(SearchProgressDialogFragment.class.getName());

		// If the user cancelled, the dialog was already dismissed
		if (dialog != null)  {
			dialog.dismiss();
			displayResults(true);
		}
		
	}

	private void displayResults(boolean showToast)
	{

		if (searchObject.getDefinition() != null)
			showDefinitionList();
		else if (searchObject.getWordList() != null)
			showWordList();
		else if (searchObject.getDefinitionList() != null)
			showDefinitionList();
		else if (searchObject.getScoredWordList() != null)
			showScoredWordList();
		else if (searchObject.getException() != null)
			if (!getActivity().isFinishing())  {
				Exception e = searchObject.getException();
				if (e instanceof WifiAuthException)
					searchObject.setException(new WordPlayException(getString(R.string.wifi_auth_error)));
				showAppErrDialog();
			}

	}
	
    private void showAppErrDialog()
    {

    	StringBuilder appData = new StringBuilder();

    	appData.append("search_string = " + searchObject.getSearchString() + "\n");
    	appData.append("search_type = " + searchObject.getSearchType() + "\n");
    	appData.append("dictionary = " + searchObject.getDictionary().toString() + "\n");
    	appData.append("word_scores = " + searchObject.getWordScores() + "\n");
    	appData.append("word_sort = " + searchObject.getWordSort() + "\n");

    	new AppErrDialog(getActivity(), searchObject.getException(), appData.toString()).show();

    }

    //
    // Search Results
    //

    private boolean zeroResults(int count)
    {
		zeroView.setVisibility(count > 0 ? View.GONE : View.VISIBLE);
		searchListView.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
		elapsedTextView.setVisibility(View.VISIBLE);
		return count == 0;
    }

	private void showDefinitionList()
	{
		
		WordDefinitionsAdapter adapter = null;
		ArrayList<String> defnList = searchObject.getDefinitionList();

		showElapsed(defnList.size(), "definition", "definitions");

		if (zeroResults(defnList.size()))
			return;

		adapter = new WordDefinitionsAdapter(
						getActivity(),
						R.layout.search_result,
						searchObject.getDefinition().getWord(),
						defnList);
		searchListView.setAdapter(adapter);
		registerForContextMenu(searchListView);
		
	}
	
	private void showWordList()
	{
		
		WordListAdapter adapter = null;
		ArrayList<String> wordList = searchObject.getWordList();

		showElapsed(wordList.size(), "word", "words");

		if (zeroResults(wordList.size()))
			return;

		if (searchObject.wordSort == WordSortState.WORD_SORT_BY_ALPHA)
			searchListView.setFastScrollEnabled(true);

		// Create the content adapter
		adapter = new WordListAdapter(
						getActivity(),
						R.layout.word_list,
						wordList,
						searchObject);

		// If this is the free version, create that adapter
		if (WordPlayApp.getInstance().isFreeMode())
			adAdapter = new SponsoredAdAdapter(getActivity(), adapter);

		// Attach the adapter to the ListView
		searchListView.setAdapter(adAdapter != null ? adAdapter : adapter);
		registerForContextMenu(searchListView);
		
	}

	private void showScoredWordList()
	{
		
		ScoredWordListAdapter adapter = null;
		ArrayList<ScoredWord> scoredWordList = searchObject.getScoredWordList();

		showElapsed(scoredWordList.size(), "scored word", "scored words");

		if (zeroResults(scoredWordList.size()))
			return;
		
		if (searchObject.wordSort == WordSortState.WORD_SORT_BY_ALPHA)
			searchListView.setFastScrollEnabled(true);

		// Create the content adapter
		adapter = new ScoredWordListAdapter(
						getActivity(),
						R.layout.word_list,
						scoredWordList,
						searchObject);

		// If this is the free version, create that adapter
		if (WordPlayApp.getInstance().isFreeMode())
			adAdapter = new SponsoredAdAdapter(getActivity(), adapter);

		// Attach the adapter to the ListView
		searchListView.setAdapter(adAdapter != null ? adAdapter : adapter);
		registerForContextMenu(searchListView);
		
	}

	private void showElapsed(int count, String objectName, String objectNamePlural)
	{

		float elapsed = (float) searchObject.getElapsedTime() / 1000;
		String str = getString(R.string.elapsed_time, count, count == 1 ? objectName : objectNamePlural, elapsed);

		Debug.e(str);
		((TextView) rootView.findViewById(R.id.elapsed_time)).setText(str);

	}

	//
	// Search Threads
	//

	private void onExactMatch()
	{

		Analytics.sendEvent(Analytics.SEARCH, Analytics.EXACT_MATCH, searchObject, 0);

		if (searchObject.getDictionary().isScrabbleDict())
			onScrabbleDictExactMatch();
		else
			onDictExactMatch();

	}
	
	private void onScrabbleDictExactMatch()
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
			protected Void doInBackground(Void... args)
			{

				ScrabbleDatabaseClient client = new ScrabbleDatabaseClient();
				String searchString = searchObject.getSearchString();

				try {
					if (searchObject.getWordScores().isScored())
						searchObject.setScoredWordList(
								client.getScoredWordList(searchString,
															searchObject.getDictionary(),
															searchObject.getWordSort()));
					else
						searchObject.setWordList(
								client.getWordList(searchString,
														searchObject.getDictionary(),
														searchObject.getWordSort()));
				}
				catch (Exception e) {
					searchObject.setException(e);
				}

				return null;

			}

			@Override
			protected void onPostExecute(Void result)
			{
				Intent intent = new Intent();
				intent.setAction(SEARCH_COMPLETED_INTENT);
				intent.putExtra("searchObject", searchObject);
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
	
	private void onDictExactMatch()
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
			protected Void doInBackground(Void... args)
			{

				while (!isCancelled())  {
					try {
						StringBuilder resp =
							dictServer.defineWord("^" + searchObject.getSearchString() + "$",
													searchObject.getDictionary());
						if (resp != null)  {
							searchObject.setDefinition(new WordDefinition(searchObject.getSearchString(), resp));
							searchObject.setDefinitionList(searchObject.getDefinition().getDefinitionsList());
						}
						break;
					}
					catch (Exception e)  {
						if (NetworkUtils.isRetryException(e))
							continue;
						searchObject.setException(e);
						break;
					}
				}

				return null;

			}

			@Override
			protected void onPostExecute(Void result)
			{
				Intent intent = new Intent();
				intent.setAction(SEARCH_COMPLETED_INTENT);
				intent.putExtra("searchObject", searchObject);
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

	private void onStartsWith()
	{

		Analytics.sendEvent(Analytics.SEARCH, Analytics.STARTS_WITH, searchObject, 0);

		if (searchObject.getDictionary().isScrabbleDict())
			onScrabbleDictStartsWith();
		else
			onDictStartsWith();

	}
	
	private void onScrabbleDictStartsWith()
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
			protected Void doInBackground(Void... args)
			{

				ScrabbleDatabaseClient client= new ScrabbleDatabaseClient();
				String searchString = searchObject.getSearchString() + "%";

				try {
					if (searchObject.getWordScores().isScored())
						searchObject.setScoredWordList(
								client.getScoredWordList(searchString,
															searchObject.getDictionary(),
															searchObject.getWordSort()));
					else
						searchObject.setWordList(
								client.getWordList(searchString,
															searchObject.getDictionary(),
															searchObject.getWordSort()));
				}
				catch (Exception e) {
					searchObject.setException(e);
				}

				return null;

			}

			@Override
			protected void onPostExecute(Void result)
			{
				Intent intent = new Intent();
				intent.setAction(SEARCH_COMPLETED_INTENT);
				intent.putExtra("searchObject", searchObject);
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
	
	private void onDictStartsWith()
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
			protected Void doInBackground(Void... args)
			{
				
				while (!isCancelled())  {
					try {
						StringBuilder resp =
							dictServer.matchWord("^" + searchObject.getSearchString(),
													searchObject.getDictionary(),
													false);
						searchObject.setWordList(RFC2229.parseWordList(resp));
						break;
					}
					catch (Exception e)  {
						if (NetworkUtils.isRetryException(e))
							continue;
						searchObject.setException(e);
						break;
					}
				}

				return null;

			}

			@Override
			protected void onPostExecute(Void result)
			{
				Intent intent = new Intent();
				intent.setAction(SEARCH_COMPLETED_INTENT);
				intent.putExtra("searchObject", searchObject);
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
	
	private void onContains()
	{

		Analytics.sendEvent(Analytics.SEARCH, Analytics.CONTAINS, searchObject, 0);

		if (searchObject.getDictionary().isScrabbleDict())
			onScrabbleDictContains();
		else
			onDictContains();

	}
	
	private void onScrabbleDictContains()
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
			protected Void doInBackground(Void... args)
			{

				ScrabbleDatabaseClient client = new ScrabbleDatabaseClient();
				String searchString= "%" + searchObject.getSearchString() + "%";

				try {
					if (searchObject.getWordScores().isScored())
						searchObject.setScoredWordList(
							client.getScoredWordList(searchString,
														searchObject.getDictionary(),
														searchObject.getWordSort()));
					else
						searchObject.setWordList(
							client.getWordList(searchString,
														searchObject.getDictionary(),
														searchObject.getWordSort()));
				}
				catch (Exception e) {
					searchObject.setException(e);
				}

				return null;

			}

			@Override
			protected void onPostExecute(Void result)
			{
				Intent intent = new Intent();
				intent.setAction(SEARCH_COMPLETED_INTENT);
				intent.putExtra("searchObject", searchObject);
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
	
	private void onDictContains()
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
			protected Void doInBackground(Void... args)
			{

				while (!isCancelled())  {
					try {
						StringBuilder resp =
							dictServer.matchWord(searchObject.getSearchString(),
													searchObject.getDictionary(), 
													false);
						searchObject.setWordList(RFC2229.parseWordList(resp));
						break;
					}
					catch (Exception e)  {
						if (NetworkUtils.isRetryException(e))
							continue;
						searchObject.setException(e);
						break;
					}
				}

				return null;

			}

			@Override
			protected void onPostExecute(Void result)
			{
				Intent intent = new Intent();
				intent.setAction(SEARCH_COMPLETED_INTENT);
				intent.putExtra("searchObject", searchObject);
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
	
	private void onEndsWith()
	{

		Analytics.sendEvent(Analytics.SEARCH, Analytics.ENDS_WITH, searchObject, 0);

		if (searchObject.getDictionary().isScrabbleDict())
			onScrabbleDictEndsWith();
		else
			onDictEndsWith();

	}
	
	private void onScrabbleDictEndsWith()
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
			protected Void doInBackground(Void... args)
			{

				ScrabbleDatabaseClient client = new ScrabbleDatabaseClient();
				String searchString = "%" + searchObject.getSearchString();

				try {
					if (searchObject.getWordScores().isScored())
						searchObject.setScoredWordList(
							client.getScoredWordList(searchString,
														searchObject.getDictionary(),
														searchObject.getWordSort()));
					else
						searchObject.setWordList(
							client.getWordList(searchString,
														searchObject.getDictionary(),
														searchObject.getWordSort()));
				}
				catch (Exception e) {
					searchObject.setException(e);
				}

				return null;

			}

			@Override
			protected void onPostExecute(Void result)
			{
				Intent intent = new Intent();
				intent.setAction(SEARCH_COMPLETED_INTENT);
				intent.putExtra("searchObject", searchObject);
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
	
	private void onDictEndsWith()
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
			protected Void doInBackground(Void... args)
			{

				while (!isCancelled())  {
					try {
						StringBuilder resp =
							dictServer.matchWord(searchObject.getSearchString() + "$",
													searchObject.getDictionary(),
													false);
						searchObject.setWordList(RFC2229.parseWordList(resp));
						break;
					}
					catch (Exception e)  {
						if (NetworkUtils.isRetryException(e))
							continue;
						searchObject.setException(e);
						break;
					}
				}

				return null;

			}

			@Override
			protected void onPostExecute(Void result)
			{
				Intent intent = new Intent();
				intent.setAction(SEARCH_COMPLETED_INTENT);
				intent.putExtra("searchObject", searchObject);
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

	private void onCrosswords()
	{

		Analytics.sendEvent(Analytics.SEARCH, Analytics.CROSSWORDS, searchObject, 0);

		if (searchObject.getDictionary().isScrabbleDict())
			onScrabbleCrosswords();
		else
			onDictCrosswords();

	}

	private void onDictCrosswords()
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
			protected Void doInBackground(Void... args)
			{

				String newSearchString = searchObject.getSearchString().replace("?", ".");

				while (!isCancelled())  {
					try {
						StringBuilder resp =
							dictServer.matchWord("^" + newSearchString + "$",
													searchObject.getDictionary(),
													false);
						searchObject.setWordList(RFC2229.parseWordList(resp));
						break;
					}
					catch (Exception e)  {
						if (NetworkUtils.isRetryException(e))
							continue;
						searchObject.setException(e);
						break;
					}
				}

				return null;

			}

			@Override
			protected void onPostExecute(Void result)
			{
				Intent intent = new Intent();
				intent.setAction(SEARCH_COMPLETED_INTENT);
				intent.putExtra("searchObject", searchObject);
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

	private void onScrabbleCrosswords()
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
			protected Void doInBackground(Void... args)
			{

				ScrabbleDatabaseClient client = new ScrabbleDatabaseClient();
				String newSearchString = searchObject.getSearchString().replace("?", ".");
				String searchString = newSearchString.replace(".", "_");
 
				try {
					searchObject.setWordList(
						client.getWordList(
								searchString,
								searchObject.getDictionary(),
								searchObject.getWordSort()));
				}
				catch (Exception e) {
					searchObject.setException(e);
				}

				return null;

			}

			@Override
			protected void onPostExecute(Void result)
			{
				Intent intent = new Intent();
				intent.setAction(SEARCH_COMPLETED_INTENT);
				intent.putExtra("searchObject", searchObject);
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

	private void onThesaurus()
	{

		Analytics.sendEvent(Analytics.SEARCH, Analytics.THESAURUS, searchObject, 0);

		task = new AsyncTask<Void, Void, Void>() {

			@Override
			protected void onPreExecute()
			{
				SearchProgressDialogFragment dialog =
					SearchProgressDialogFragment.newInstance(SearchFragment.class.getName());
			    dialog.show(getFragmentManager(), SearchProgressDialogFragment.class.getName());		
			}

			@Override
			protected Void doInBackground(Void... args)
			{

				while (!isCancelled())  {
					try {
						StringBuilder resp = dictServer.thesaurus("^" + searchObject.getSearchString() + "$");
						searchObject.setWordList(RFC2229.parseThesaurusList(resp));
						break;
					}
					catch (Exception e)  {
						if (NetworkUtils.isRetryException(e))
							continue;
						searchObject.setException(e);
						break;
					}
				}

				return null;

			}

			@Override
			protected void onPostExecute(Void result)
			{
				Intent intent = new Intent();
				intent.setAction(SEARCH_COMPLETED_INTENT);
				intent.putExtra("searchObject", searchObject);
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
	
	private void onAnagram()
	{

		Analytics.sendEvent(Analytics.SEARCH, Analytics.ANAGRAM, searchObject, 0);

		if (searchObject.getDictionary().isScrabbleDict())
			onScrabbleDictAnagram();
		else
			Toast.makeText(getActivity(), "onAnagram: Bad Dictionary Selected", Toast.LENGTH_SHORT).show();

	}
	
	private void onScrabbleDictAnagram()
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
			protected Void doInBackground(Void... args)
			{

				ScrabbleDatabaseClient client = new ScrabbleDatabaseClient();
		
				try {
					if (searchObject.getWordScores().isScored())
						searchObject.setScoredWordList(
							client.getScoredAnagrams(searchObject.getSearchString() + searchObject.getBoardString(),
														searchObject.getDictionary(),
														searchObject.getWordSort()));
					else
						searchObject.setWordList(
							client.getAnagrams(searchObject.getSearchString() + searchObject.getBoardString(),
														searchObject.getDictionary(),
														searchObject.getWordSort()));
				}
				catch (Exception e) {
					searchObject.setException(e);
				}

				return null;

			}

			@Override
			protected void onPostExecute(Void result)
			{
				Intent intent = new Intent();
				intent.setAction(SEARCH_COMPLETED_INTENT);
				intent.putExtra("searchObject", searchObject);
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
