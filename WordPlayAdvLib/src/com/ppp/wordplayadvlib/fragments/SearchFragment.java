package com.ppp.wordplayadvlib.fragments;

import java.util.ArrayList;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.WordPlayApp;
import com.ppp.wordplayadvlib.adapters.ScoredWordListAdapter;
import com.ppp.wordplayadvlib.adapters.WordDefinitionsAdapter;
import com.ppp.wordplayadvlib.adapters.WordListAdapter;
import com.ppp.wordplayadvlib.appdata.DictionaryType;
import com.ppp.wordplayadvlib.appdata.History;
import com.ppp.wordplayadvlib.appdata.ScoredWord;
import com.ppp.wordplayadvlib.appdata.SearchObject;
import com.ppp.wordplayadvlib.appdata.SearchThread;
import com.ppp.wordplayadvlib.appdata.SearchType;
import com.ppp.wordplayadvlib.appdata.WordDefinition;
import com.ppp.wordplayadvlib.appdata.WordScoreState;
import com.ppp.wordplayadvlib.appdata.WordSortState;
import com.ppp.wordplayadvlib.database.ScrabbleDatabaseClient;
import com.ppp.wordplayadvlib.dialogs.AppErrDialog;
import com.ppp.wordplayadvlib.exceptions.WifiAuthException;
import com.ppp.wordplayadvlib.exceptions.WordPlayException;
import com.ppp.wordplayadvlib.fragments.dialog.SearchProgressDialogFragment;
import com.ppp.wordplayadvlib.fragments.dialog.SearchProgressDialogFragment.SearchProgressListener;
import com.ppp.wordplayadvlib.networking.NetworkUtils;
import com.ppp.wordplayadvlib.networking.RFC2229;
import com.ppp.wordplayadvlib.networking.ScrabbleClient;
import com.ppp.wordplayadvlib.utils.Debug;

public class SearchFragment extends BaseFragment
	implements
		OnItemClickListener,
		SearchProgressListener
{
	
	RFC2229 dictServer;
	
	private SearchObject searchObject;

	private SearchType searchType;
	private DictionaryType dictionary = DictionaryType.DICTIONARY_DICT_DOT_ORG;
	private String searchString;
	private String boardString;
	private WordScoreState wordScore;
	private WordSortState wordSort;
	
	private SearchThread searchThread;

	private View rootView;
	private ListView searchListView;
	private Handler searchHandler;
	private ProgressDialog progressDialog;
	private boolean cancel = false;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{

		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

	    // Get the arguments
		if (savedInstanceState != null)
			searchObject = savedInstanceState.getParcelable("searchObject");

		// If no SearchObject available, make one
		if (searchObject == null)  {

		    Bundle args = getArguments();
		    searchType = SearchType.fromInt(args.getInt("SearchType"));
		    searchString = args.getString("SearchString").toLowerCase();
		    if (searchType == SearchType.OPTION_ANAGRAMS)
			    boardString = args.getString("BoardString").toLowerCase();
		    else
		    	boardString = "";
			dictionary = DictionaryType.fromInt((int)args.getInt("Dictionary"));
			if (dictionary == DictionaryType.DICTIONARY_UNKNOWN)
				dictionary = DictionaryType.DICTIONARY_DICT_DOT_ORG;
			wordScore = WordScoreState.fromInt(args.getInt("WordScores"));
			wordSort = WordSortState.fromInt(args.getInt("WordSort"));

		    Debug.i("SearchString: '" + searchString + "'");
			Debug.i("BoardString: '" + boardString + "'");
		    Debug.i("SearchType: " + searchType);
			Debug.i("Dictionary: " + dictionary);
			Debug.i("WordScores: " + wordScore);
			Debug.i("SortByScore: " + wordSort);

		}

	    // Create the new connection to the dictionary server used for this search
    	dictServer = new RFC2229();

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{

		rootView = inflater.inflate(R.layout.search_fragment, null);

		searchListView = (ListView) rootView.findViewById(R.id.search_result_list);
	    searchListView.setOnItemClickListener(this);

		return rootView;

	}

	@Override
	public void onResume()
	{

		super.onResume();

		// If a search is already running, reattach to it
//		if (searchThread != null)  {
//	    	searchThread.setReconfigured(false);
//	    	searchObject = searchThread.getSearchObject();
//	    	searchHandler = searchObject.getSearchHandler();
//	    	if (searchThread.isAlive())  {
//	    		Debug.v("SearchResult Reconfiguration: THREAD ALIVE!");
//	    		openProgressDialog();
//	    		try {
//	    			searchThread.join();
//	    		}
//	    		catch (Exception e) {
//	    			searchObject.setException(e);
//	    		}
//	    		displayResults(true);
//	    		return;
//	    	}
//	    	else {
//				Debug.v("SearchResult Reconfiguration: DISPLAY RESULTS");
//	    		displayResults(false);
//	    		return;
//	    	}
//	    }

		// If we've already done a search, don't do another
		if (searchObject != null)  {
			displayResults(true);
			return;
		}

		// Create the SearchObject
		searchObject = new SearchObject(getArguments());

		// Add this search to the history
		History.getInstance().addHistory(searchString,
											boardString,
											searchType,
											dictionary,
											wordScore,
											wordSort);

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

//	@Override
//	public void onDetach()
//	{
//
//		super.onDetach();
//
//		// If the user is reorienting the device, close the progress dialog
//		// and reset the search handler
//		if (progressDialog != null)
//			closeProgressDialog();
//		if (searchThread != null)  {
//			if (searchThread.getSearchObject() != null)
//				searchThread.getSearchObject().setSearchHandler(null);
//			searchThread.setReconfigured(true);
//		}
//
//	}

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

			startSearchActivity(args);

		}

	}

	//
	// Search
	//

	@Override
	public void onProgressCancel()
	{
		Log.e(getClass().getSimpleName(), "onProgressCancel");
		popStack();
	}

	private void startBackgroundSearch(Runnable r, boolean isAnagrams)
	{

		searchThread = new SearchThread(r, searchObject);

		searchHandler = new Handler() {
	        public void handleMessage(Message msg)
	        {
//	        	if (searchThread != null)  {
//	        		try {
//	        			searchThread.join();
//	        		}
//	        		catch (Exception e) {}
//	        	}
	        	displayResults(true);
	        }
		};
		
		searchObject.setSearchHandler(searchHandler);
		searchThread.start();
		
		if (!isAnagrams)
			openProgressDialog();
	
	}

	public void openProgressDialog()
	{

		cancel = false;

		progressDialog = new ProgressDialog(getActivity());
		progressDialog.setTitle("Please wait...");
		progressDialog.setMessage("Retrieving data...");
		progressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE,
								getString(android.R.string.cancel),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which)
									{
										if (searchThread != null)
											searchThread.interrupt();
										cancel = true;
									}
								});
		progressDialog.show();

	}
	
	private void closeProgressDialog()
	{

		if (progressDialog != null)  {
			if (!getActivity().isFinishing())  {
				try {
					progressDialog.dismiss();
				}
				catch (Exception e) {}
			}
		}
		progressDialog = null;

	}
	
	private void displayResults(Boolean showToast)
	{

//		if (progressDialog != null)
//			closeProgressDialog();

//		if (cancel)  {
//			popStack();
//			return;
//		}

		if (searchObject.getDefinition() != null)
			showDefinitionList(showToast);
		else if (searchObject.getWordList() != null)
			showWordList(showToast);
		else if (searchObject.getDefinitionList() != null)
			showDefinitionList(showToast);
		else if (searchObject.getScoredWordList() != null)
			showScoredWordList(showToast);
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

    	StringBuilder app_data = new StringBuilder();

    	app_data.append("search_string = " + searchObject.getSearchString() + "\n");
    	app_data.append("search_type = " + searchObject.getSearchType() + "\n");
    	app_data.append("dictionary = " + searchObject.getDictionary().toString() + "\n");
    	app_data.append("word_scores = " + searchObject.getWordScores() + "\n");
    	app_data.append("word_sort = " + searchObject.getWordSort() + "\n");

    	new AppErrDialog(getActivity(), searchObject.getException(), app_data.toString()).show();

    }

    //
    // Search Results
    //

	private void showDefinitionList(boolean showToast)
	{
		
		WordDefinitionsAdapter adapter = null;
		ArrayList<String> defnList = searchObject.getDefinitionList();

		Debug.d("found " + defnList.size() + " definitions in " + getElapsedTime() + " seconds");
		if (showToast && !cancel)
			Toast.makeText(getActivity(),
							defnList.size() + " definition" + ((defnList.size() == 1) ? "" : "s") + " found in " + getElapsedTime() + " seconds",
							Toast.LENGTH_SHORT).show();

		adapter = new WordDefinitionsAdapter(
						getActivity(),
						R.layout.search_result,
						searchObject.getDefinition().getWord(),
						defnList);
		searchListView.setAdapter(adapter);
		registerForContextMenu(searchListView);
		
	}
	
	private void showWordList(Boolean showToast)
	{
		
		WordListAdapter adapter = null;
		ArrayList<String> wordList = searchObject.getWordList();

		Debug.d("found " + wordList.size() + " words in " + getElapsedTime() + " seconds");
		if (showToast && !cancel)
			Toast.makeText(getActivity(),
							wordList.size() + " word" +
								((wordList.size() == 1) ? "" : "s") + " found in " + getElapsedTime() + " seconds",
							Toast.LENGTH_SHORT).show();

		if (wordSort == WordSortState.WORD_SORT_BY_ALPHA)
			searchListView.setFastScrollEnabled(true);

		adapter = new WordListAdapter(
						getActivity(),
						R.layout.word_list,
						wordList,
						wordSort,
						boardString,
						searchObject);
		searchListView.setAdapter(adapter);
		registerForContextMenu(searchListView);
		
	}

	private void showScoredWordList(Boolean showToast)
	{
		
		ScoredWordListAdapter adapter = null;
		ArrayList<ScoredWord> scoredWordList = searchObject.getScoredWordList();

		Debug.d("found " + scoredWordList.size() + " scored words in " + getElapsedTime() + " seconds");
		if (showToast && !cancel)
			Toast.makeText(getActivity(),
							scoredWordList.size() + " word" + ((scoredWordList.size() == 1) ? "" : "s") + " found in " + getElapsedTime() + " seconds",
							Toast.LENGTH_SHORT).show();
		
		if (wordSort == WordSortState.WORD_SORT_BY_ALPHA)
			searchListView.setFastScrollEnabled(true);

		adapter = new ScoredWordListAdapter(
						getActivity(),
						R.layout.word_list,
						scoredWordList,
						searchObject.wordSort,
						searchObject.boardString,
						searchObject);
		searchListView.setAdapter(adapter);
		registerForContextMenu(searchListView);
		
	}

	private String getElapsedTime()
	{
		float elapsed = (float)searchObject.getElapsedTime() / 1000;
		return String.format("%.3f", elapsed);
	}

	//
	// Search Threads
	//

	private void onExactMatch()
	{

		if (searchObject.getDictionary().isScrabbleDict())
			onScrabbleDictExactMatch();
		else
			onDictExactMatch();
		
	}
	
	private void onScrabbleDictExactMatch()
	{
		
		Runnable r = new Runnable() {
			public void run()
			{

				ScrabbleClient client;
				String searchString;

				if (WordPlayApp.getInstance().getUseGoogleAppEngine())  {
					client = new ScrabbleClient();
					searchString = "^" + searchObject.getSearchString() + "$";
				}
				else {
					client = new ScrabbleDatabaseClient();
					searchString = searchObject.getSearchString();
				}

				while (!cancel)  {
					try {
						if (searchObject.getWordScores().isScored())
							searchObject.setScoredWordList(
									client.getScoredWordList(
											searchString,
											searchObject.getDictionary(),
											searchObject.getWordSort()));
						else
							searchObject.setWordList(
									client.getWordList(
											searchString,
											searchObject.getDictionary(),
											searchObject.getWordSort()));
						break;
					}
					catch (Exception e) {
						if (NetworkUtils.isRetryException(e))
							continue;
						searchObject.setException(e);
						break;
					}
				}

				if ((searchObject != null) && (searchObject.getSearchHandler() != null))
					searchObject.getSearchHandler().sendEmptyMessage(0);
				
			}
		};
		
		startBackgroundSearch(r, false);
		
	}
	
	private void onDictExactMatch()
	{
		
		Runnable r = new Runnable() {
			public void run()
			{
				
				while (!cancel)  {
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
				
				if ((searchObject != null) && (searchObject.getSearchHandler() != null))
					searchObject.getSearchHandler().sendEmptyMessage(0);

			}
		};
		
		startBackgroundSearch(r, false);

	}

	private void onStartsWith()
	{
		
		if (searchObject.getDictionary().isScrabbleDict())
			onScrabbleDictStartsWith();
		else
			onDictStartsWith();
		
	}
	
	private void onScrabbleDictStartsWith()
	{
		
		Runnable r = new Runnable() {
			public void run()
			{

				ScrabbleClient client;
				String searchString;

				if (WordPlayApp.getInstance().getUseGoogleAppEngine())  {
					client = new ScrabbleClient();
					searchString = "^" + searchObject.getSearchString();
				}
				else {
					client = new ScrabbleDatabaseClient();
					searchString = searchObject.getSearchString() + "%";
				}

				while (!cancel)  {
					try {
						if (searchObject.getWordScores().isScored())
							searchObject.setScoredWordList(
									client.getScoredWordList(
											searchString,
											searchObject.getDictionary(),
											searchObject.getWordSort()));
						else
							searchObject.setWordList(
									client.getWordList(
											searchString,
											searchObject.getDictionary(),
											searchObject.getWordSort()));
						break;
					}
					catch (Exception e) {
						if (NetworkUtils.isRetryException(e))
							continue;
						searchObject.setException(e);
						break;
					}
				}
				
				if ((searchObject != null) && (searchObject.getSearchHandler() != null))
					searchObject.getSearchHandler().sendEmptyMessage(0);

			}
		};
		
		startBackgroundSearch(r, false);
		
	}
	
	private void onDictStartsWith()
	{
		
		Runnable r = new Runnable() {
			public void run()
			{
				
				while (!cancel)  {
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
				
				if ((searchObject != null) && (searchObject.getSearchHandler() != null))
					searchObject.getSearchHandler().sendEmptyMessage(0);
				
			}
		};
		
		startBackgroundSearch(r, false);
		
	}
	
	private void onContains()
	{
		
		if (searchObject.getDictionary().isScrabbleDict())
			onScrabbleDictContains();
		else
			onDictContains();
		
	}
	
	private void onScrabbleDictContains()
	{
		
		Runnable r = new Runnable() {
			public void run()
			{

				ScrabbleClient client;
				String searchString;

				if (WordPlayApp.getInstance().getUseGoogleAppEngine())  {
					client = new ScrabbleClient();
					searchString = searchObject.getSearchString();
				}
				else {
					client = new ScrabbleDatabaseClient();
					searchString = "%" + searchObject.getSearchString() + "%";
				}

				while (!cancel)  {
					try {
						if (searchObject.getWordScores().isScored())
							searchObject.setScoredWordList(
									client.getScoredWordList(
											searchString,
											searchObject.getDictionary(),
											searchObject.getWordSort()));
						else
							searchObject.setWordList(
									client.getWordList(
											searchString,
											searchObject.getDictionary(),
											searchObject.getWordSort()));
						break;
					}
					catch (Exception e) {
						if (NetworkUtils.isRetryException(e))
							continue;
						searchObject.setException(e);
						break;
					}
				}

				if ((searchObject != null) && (searchObject.getSearchHandler() != null))
					searchObject.getSearchHandler().sendEmptyMessage(0);
				
			}
		};
		
		startBackgroundSearch(r, false);

	}
	
	private void onDictContains()
	{
		
		Runnable r = new Runnable() {
			public void run()
			{
				
				while (!cancel)  {
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

				if ((searchObject != null) && (searchObject.getSearchHandler() != null))
					searchObject.getSearchHandler().sendEmptyMessage(0);
				
			}
		};
		
		startBackgroundSearch(r, false);
		
	}
	
	private void onEndsWith()
	{
		
		if (searchObject.getDictionary().isScrabbleDict())
			onScrabbleDictEndsWith();
		else
			onDictEndsWith();
		
	}
	
	private void onScrabbleDictEndsWith()
	{
		
		Runnable r = new Runnable() {
			public void run()
			{

				ScrabbleClient client;
				String searchString;

				if (WordPlayApp.getInstance().getUseGoogleAppEngine())  {
					client = new ScrabbleClient();
					searchString = searchObject.getSearchString() + "$";
				}
				else {
					client = new ScrabbleDatabaseClient();
					searchString = "%" + searchObject.getSearchString();
				}

				while (!cancel)  {
					try {
						if (searchObject.getWordScores().isScored())
							searchObject.setScoredWordList(
									client.getScoredWordList(
											searchString,
											searchObject.getDictionary(),
											searchObject.getWordSort()));
						else
							searchObject.setWordList(
									client.getWordList(
											searchString,
											searchObject.getDictionary(),
											searchObject.getWordSort()));
						break;
					}
					catch (Exception e) {
						if (NetworkUtils.isRetryException(e))
							continue;
						searchObject.setException(e);
						break;
					}
				}
				
				if ((searchObject != null) && (searchObject.getSearchHandler() != null))
					searchObject.getSearchHandler().sendEmptyMessage(0);

			}
		};
		
		startBackgroundSearch(r, false);
		
	}
	
	private void onDictEndsWith()
	{
		
		Runnable r = new Runnable() {
			public void run()
			{
				
				while (!cancel)  {
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
				
				if ((searchObject != null) && (searchObject.getSearchHandler() != null))
					searchObject.getSearchHandler().sendEmptyMessage(0);
				
			}
		};

		startBackgroundSearch(r, false);
		
	}

	private void onCrosswords()
	{

		if (searchObject.getDictionary().isScrabbleDict())
			onScrabbleCrosswords();
		else
			onDictCrosswords();

	}

	private void onDictCrosswords()
	{

		Runnable r = new Runnable() {
			public void run()
			{

				String newSearchString = searchObject.getSearchString().replace("?", ".");

				while (!cancel)  {
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
				
				if ((searchObject != null) && (searchObject.getSearchHandler() != null))
					searchObject.getSearchHandler().sendEmptyMessage(0);

			}
		};

		startBackgroundSearch(r, false);
		
	}

	private void onScrabbleCrosswords()
	{

		Runnable r = new Runnable() {
			public void run()
			{

				ScrabbleClient client;
				String searchString;
				String newSearchString = searchObject.getSearchString().replace("?", ".");

				if (WordPlayApp.getInstance().getUseGoogleAppEngine())  {
					client = new ScrabbleClient();
					searchString = "^" + newSearchString + "$";
				}
				else {
					client = new ScrabbleDatabaseClient();
					searchString = newSearchString.replace(".", "_");
				}

				while (!cancel)  {
					try {
						searchObject.setWordList(
								client.getWordList(
										searchString,
										searchObject.getDictionary(),
										searchObject.getWordSort()));
						break;
					}
					catch (Exception e) {
						if (NetworkUtils.isRetryException(e))
							continue;
						searchObject.setException(e);
						break;
					}
				}

				if ((searchObject != null) && (searchObject.getSearchHandler() != null))
					searchObject.getSearchHandler().sendEmptyMessage(0);
				
			}
		};
		
		startBackgroundSearch(r, false);

	}

	private void onThesaurus()
	{
		
		Runnable r = new Runnable() {
			public void run()
			{
				
				while (!cancel)  {
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
				
				if ((searchObject != null) && (searchObject.getSearchHandler() != null))
					searchObject.getSearchHandler().sendEmptyMessage(0);

			}
		};
		
		startBackgroundSearch(r, false);
		
	}
	
	private void onAnagram()
	{

		if (searchObject.getDictionary().isScrabbleDict())
			onScrabbleDictAnagram();
		else
			Toast.makeText(getActivity(), "onAnagram: Bad Dictionary Selected", Toast.LENGTH_SHORT).show();
		
	}
	
	private void onScrabbleDictAnagram()
	{

		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

			@Override
			protected void onPreExecute()
			{
				SearchProgressDialogFragment dialog =
					SearchProgressDialogFragment.newInstance(SearchFragment.class.getName(), this);
			    dialog.show(getFragmentManager(), SearchProgressDialogFragment.class.getName());		
			}

			@Override
			protected Void doInBackground(Void... args)
			{

				ScrabbleClient client = null;

				if (WordPlayApp.getInstance().getUseGoogleAppEngine())
					client = new ScrabbleClient();
				else
					client = new ScrabbleDatabaseClient();
		
				try {
					if (searchObject.getWordScores().isScored())
						searchObject.setScoredWordList(
								client.getScoredAnagrams(
										searchObject.getSearchString() + searchObject.getBoardString(),
										searchObject.getDictionary(),
										searchObject.getWordSort()));
					else
						searchObject.setWordList(
								client.getAnagrams(
										searchObject.getSearchString() + searchObject.getBoardString(),
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

				FragmentManager fm = getFragmentManager();
				SearchProgressDialogFragment dialog = null;

				// Do nothing if we've been cancelled
				if (isCancelled())
					return;

				// Find the dialog in the FragmentManager
				if (fm != null)
					dialog =
						(SearchProgressDialogFragment) fm.findFragmentByTag(SearchProgressDialogFragment.class.getName());

				// Dismiss the dialog and display the results
				if (dialog != null)  {
					dialog.dismiss();
					displayResults(true);
				}

			}

			@Override
			protected void onCancelled(Void result) { popStack(); }

			@Override
			protected void onCancelled() { popStack(); }

		};

		task.execute();
		
	}

}
