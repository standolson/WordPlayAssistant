package com.ppp.wordplayadvlib.fragments;

import java.util.ArrayList;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.adapters.ScoredWordListAdapter;
import com.ppp.wordplayadvlib.adapters.WordDefinitionsAdapter;
import com.ppp.wordplayadvlib.adapters.WordListAdapter;
import com.ppp.wordplayadvlib.appdata.DictionaryType;
import com.ppp.wordplayadvlib.appdata.History;
import com.ppp.wordplayadvlib.appdata.ScoredWord;
import com.ppp.wordplayadvlib.appdata.SearchObject;
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
import com.ppp.wordplayadvlib.utils.Debug;

public class SearchFragment extends BaseFragment
	implements
		OnItemClickListener,
		SearchProgressListener
{

	private View rootView;
	private ListView searchListView;

	private SearchType searchType;
	private DictionaryType dictionary = DictionaryType.DICTIONARY_DICT_DOT_ORG;
	private String searchString;
	private String boardString;
	private WordScoreState wordScore;
	private WordSortState wordSort;
	private boolean cancel = false;
	
	private RFC2229 dictServer;	
	private SearchObject searchObject;

	private static AsyncTask<Void, Void, Void> task;
	private static SearchFragment taskListener;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{

		super.onCreate(savedInstanceState);

		taskListener = this;

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

		// If we've already done a search, don't do another
		if (searchObject != null)  {
			displayResults(searchObject, true);
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

			startNewSearch(args);

		}

	}

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
    	MenuItem item = menu.findItem(R.id.dictionary_menu);
    	if (item != null)
    		item.setVisible(false);
    }

	//
	// Search
	//

	@Override
	public void onProgressCancel()
	{
		Log.e(getClass().getSimpleName(), "onProgressCancel");
		task.cancel(true);
	}

	private void onSearchComplete(SearchObject searchObject)
	{

		FragmentManager fm = getFragmentManager();
		SearchProgressDialogFragment dialog = null;

		// Do nothing if we've been cancelled
		if (task.isCancelled())  {
			popStack();
			return;
		}

		// Find the dialog in the FragmentManager
		if (fm != null)
			dialog =
				(SearchProgressDialogFragment) fm.findFragmentByTag(SearchProgressDialogFragment.class.getName());

		// Dismiss the dialog and display the results
		if (dialog != null)  {
			dialog.dismiss();
			displayResults(searchObject, true);
		}
		
	}

	private void onSearchCancelled(SearchObject so)
	{
		popStack();
	}

	private void displayResults(SearchObject so, boolean showToast)
	{

		if (task.isCancelled())  {
			popStack();
			return;
		}

		searchObject = so;

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
				taskListener.onSearchComplete(searchObject);
			}

			@Override
			protected void onCancelled(Void result)
			{
				taskListener.onSearchCancelled(searchObject);
			}

			@Override
			protected void onCancelled()
			{
				taskListener.onSearchCancelled(searchObject);
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
				taskListener.onSearchComplete(searchObject);
			}

			@Override
			protected void onCancelled(Void result)
			{
				taskListener.onSearchCancelled(searchObject);
			}

			@Override
			protected void onCancelled()
			{
				taskListener.onSearchCancelled(searchObject);
			}

		};

		task.execute();

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
				taskListener.onSearchComplete(searchObject);
			}

			@Override
			protected void onCancelled(Void result)
			{
				taskListener.onSearchCancelled(searchObject);
			}

			@Override
			protected void onCancelled()
			{
				taskListener.onSearchCancelled(searchObject);
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
				taskListener.onSearchComplete(searchObject);
			}

			@Override
			protected void onCancelled(Void result)
			{
				taskListener.onSearchCancelled(searchObject);
			}

			@Override
			protected void onCancelled()
			{
				taskListener.onSearchCancelled(searchObject);
			}

		};

		task.execute();
		
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
				taskListener.onSearchComplete(searchObject);
			}

			@Override
			protected void onCancelled(Void result)
			{
				taskListener.onSearchCancelled(searchObject);
			}

			@Override
			protected void onCancelled()
			{
				taskListener.onSearchCancelled(searchObject);
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
				taskListener.onSearchComplete(searchObject);
			}

			@Override
			protected void onCancelled(Void result)
			{
				taskListener.onSearchCancelled(searchObject);
			}

			@Override
			protected void onCancelled()
			{
				taskListener.onSearchCancelled(searchObject);
			}

		};

		task.execute();
		
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
				taskListener.onSearchComplete(searchObject);
			}

			@Override
			protected void onCancelled(Void result)
			{
				taskListener.onSearchCancelled(searchObject);
			}

			@Override
			protected void onCancelled()
			{
				taskListener.onSearchCancelled(searchObject);
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
				taskListener.onSearchComplete(searchObject);
			}

			@Override
			protected void onCancelled(Void result)
			{
				taskListener.onSearchCancelled(searchObject);
			}

			@Override
			protected void onCancelled()
			{
				taskListener.onSearchCancelled(searchObject);
			}

		};

		task.execute();
		
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
				taskListener.onSearchComplete(searchObject);
			}

			@Override
			protected void onCancelled(Void result)
			{
				taskListener.onSearchCancelled(searchObject);
			}

			@Override
			protected void onCancelled()
			{
				taskListener.onSearchCancelled(searchObject);
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
				taskListener.onSearchComplete(searchObject);
			}

			@Override
			protected void onCancelled(Void result)
			{
				taskListener.onSearchCancelled(searchObject);
			}

			@Override
			protected void onCancelled()
			{
				taskListener.onSearchCancelled(searchObject);
			}

		};

		task.execute();

	}

	private void onThesaurus()
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
				taskListener.onSearchComplete(searchObject);
			}

			@Override
			protected void onCancelled(Void result)
			{
				taskListener.onSearchCancelled(searchObject);
			}

			@Override
			protected void onCancelled()
			{
				taskListener.onSearchCancelled(searchObject);
			}

		};

		task.execute();
		
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
				taskListener.onSearchComplete(searchObject);
			}

			@Override
			protected void onCancelled(Void result)
			{
				taskListener.onSearchCancelled(searchObject);
			}

			@Override
			protected void onCancelled()
			{
				taskListener.onSearchCancelled(searchObject);
			}

		};

		task.execute();
		
	}

}
