package com.ppp.wordplayadvlib.fragments;

import java.util.ArrayList;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.ClipboardManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.WordPlayApp;
import com.ppp.wordplayadvlib.adapters.ScoredWordListAdapter;
import com.ppp.wordplayadvlib.adapters.WordDefinitionsAdapter;
import com.ppp.wordplayadvlib.adapters.WordListAdapter;
import com.ppp.wordplayadvlib.appdata.DictionaryType;
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
import com.ppp.wordplayadvlib.networking.NetworkUtils;
import com.ppp.wordplayadvlib.networking.RFC2229;
import com.ppp.wordplayadvlib.networking.ScrabbleClient;
import com.ppp.wordplayadvlib.utils.Debug;
import com.ppp.wordplayadvlib.utils.Utils;

public class SearchFragment extends BaseFragment implements OnItemClickListener {
	
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
		if (searchThread != null)  {
	    	searchThread.setReconfigured(false);
	    	searchObject = searchThread.getSearchObject();
	    	searchHandler = searchObject.getSearchHandler();
	    	if (searchThread.isAlive())  {
	    		Debug.v("SearchResult Reconfiguration: THREAD ALIVE!");
	    		openProgressDialog();
	    		try {
	    			searchThread.join();
	    		}
	    		catch (Exception e) {
	    			searchObject.setException(e);
	    		}
	    		displayResults(true);
	    		return;
	    	}
	    	else {
				Debug.v("SearchResult Reconfiguration: DISPLAY RESULTS");
	    		displayResults(false);
	    		return;
	    	}
	    }

		// If we've already done a search, don't do another
		if (searchObject != null)
			return;

	    // Create the new SearchObject and connection to the dictionary
		// server used for this search
	    searchObject = new SearchObject(getArguments());
    	dictServer = new RFC2229();

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
	public void onDetach()
	{

		super.onDetach();

		// If the user is reorienting the device, close the progress dialog
		// and reset the search handler
		if (progressDialog != null)
			closeProgressDialog();
		if (searchThread != null)  {
			if (searchThread.getSearchObject() != null)
				searchThread.getSearchObject().setSearchHandler(null);
			searchThread.setReconfigured(true);
		}

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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {

    	super.onCreateContextMenu(menu, v, menuInfo);
    	getActivity().getMenuInflater().inflate(R.menu.exact_match_context, menu);
    	
    	if (searchObject.getSearchType() == SearchType.OPTION_DICTIONARY_EXACT_MATCH)
    		menu.setHeaderTitle("Definitions");
    	else
    		menu.setHeaderTitle("Words");
    	
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
    	
    	ClipboardManager clippy;
    	Intent intent;
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
    	String text;

		if (searchObject.getSearchType() == SearchType.OPTION_DICTIONARY_EXACT_MATCH)
			text = searchObject.getDefinition().getDefinitionAt((int)info.id);
		else
			if (searchObject.getWordScores().isScored())
				text = searchObject.getScoredWordList().get((int)info.id).getWord();
			else
				text = searchObject.getWordList().get((int)info.id);

    	if (item.getItemId() == R.id.exact_match_copy)  {
			clippy = (ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
			clippy.setText(text);
			return true;
		}
    	else if (item.getItemId() == R.id.exact_match_email)  {
			intent = new Intent(Intent.ACTION_SEND);
			intent.setType("message/rfc822");
			intent.putExtra(Intent.EXTRA_SUBJECT, getMessageSubject(true));
			intent.putExtra(android.content.Intent.EXTRA_TEXT, text);
			try {
				startActivity(intent);
			}
			catch (ActivityNotFoundException exception) {
				Utils.configureEmailAlert(getActivity());
			}
			return true;
		}
    	
    	return super.onOptionsItemSelected(item);
    	
    }

//	@Override
//	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
//	{
//	
//		// No search object?
//		if (searchObject == null)
//	    	return;
//	
//		// If we did a regular dictionary search and there aren't any
//		// results, show no menu
//		if (searchObject.getSearchType() == SearchType.OPTION_DICTIONARY_EXACT_MATCH)  {
//	    	if (searchObject.getDictionary().isNormalDict() &&
//	    		(searchObject.getDefinition().size() == 0))
//	    		return;
//	    }
//	    else {
//	
//	    	// For all other searches, for no results, show no
//	    	// menu
//	    	if (searchObject.getWordScores().isScored())  {
//	    		if (searchObject.getScoredWordList().size() == 0)
//	    			return;
//	    	}
//	    	else
//	    		if (searchObject.getWordList().size() == 0)
//	    			return;
//	
//	    }
//	    
//	    inflater.inflate(R.menu.exact_match_context, menu);
//	    return;
//	    
//	}
    
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item)
//	{
//	
//		ClipboardManager clippy;
//		Intent intent;
//		String text = "";
//		
//		if (searchObject.getSearchType() == SearchType.OPTION_DICTIONARY_EXACT_MATCH)  {
//			if (searchObject.getDictionary().isNormalDict())  {
//				WordDefinition d = searchObject.getDefinition();
//				ArrayList<String> defns = d.getDefinitionsList();
//				for (int i = 0; i < defns.size(); i += 1)
//					text += d.getDefinitionAt(i) + "\n";
//			}
//			else {
//				if (searchObject.getWordScores().isScored())
//					for (ScoredWord w : searchObject.getScoredWordList())
//						text += w.getWord() + " (" + w.getScore() + ")\n";
//				else
//					for (String w : searchObject.getWordList())
//						text += w + "\n";			
//			}
//		}
//		else {
//			if (searchObject.getWordScores().isScored())
//				for (ScoredWord w : searchObject.getScoredWordList())
//					text += w.getWord() + " (" + w.getScore() + ")";
//			else
//				for (String w : searchObject.getWordList())
//					text += w + "\n";
//		}
//		
//		if (item.getItemId() == R.id.exact_match_copy)  {
//			clippy = (ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
//			clippy.setText(text);
//		}
//		else if (item.getItemId() == R.id.exact_match_email)  {
//			intent = new Intent(Intent.ACTION_SEND);
//			intent.setType("message/rfc822");
//			intent.putExtra(Intent.EXTRA_SUBJECT, getMessageSubject(false));
//			intent.putExtra(android.content.Intent.EXTRA_TEXT, text);
//			try {
//				startActivity(intent);
//			}
//			catch (ActivityNotFoundException exception) {
//				Utils.configureEmailAlert(getActivity());
//			}
//		}
//		
//		return true;
//		
//	}

	public String getMessageSubject(boolean isContext)
	{

		String str = "Unknown Subject";
		String ss = searchObject.getSearchString();
		
		switch (searchObject.getSearchType()) {
			case OPTION_DICTIONARY_EXACT_MATCH:
				str = (isContext ? "A definition" : "Definitions") + " of '" + ss + "'";
				break;
			case OPTION_DICTIONARY_STARTS_WITH:
				str = (isContext ? "A word " : "Words") + " starting with '" + ss + "'";
				break;
			case OPTION_DICTIONARY_ENDS_WITH:
				str = (isContext ? "A word" : "Words") + " ending with '" + ss + "'";
				break;
			case OPTION_DICTIONARY_CONTAINS:
				str = (isContext ? "A word" : "Words") + " containing '" + ss + "'";
				break;
			case OPTION_ANAGRAMS:
				str = (isContext ? "An anagram" : "Anagrams") + " of '" + ss + "'";
				break;
			case OPTION_THESAURUS:
				str = (isContext ? "A synonym" : "Synonyms") + " of '" + ss + "'";
				break;
			case OPTION_CROSSWORDS:
				str = (isContext ? "A word" : "Words") + " matching crossword pattern of '" + ss + "'";
				break;
		}
		
		return str;
		
	}

	//
	// Search
	//

	private void startBackgroundSearch(Runnable r, boolean isAnagrams)
	{

		searchThread = new SearchThread(r, searchObject);

		searchHandler = new Handler() {
	        public void handleMessage(Message msg)
	        {
	        	if (searchThread != null)  {
	        		try {
	        			searchThread.join();
	        		}
	        		catch (Exception e) {}
	        	}
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

		if (progressDialog != null)
			closeProgressDialog();

		if (cancel)  {
			popStack();
			return;
		}

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
						wordSort,
						boardString,
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
		
		Runnable r = new Runnable() {
			public void run()
			{

				ScrabbleClient client = null;

				if (WordPlayApp.getInstance().getUseGoogleAppEngine())
					client = new ScrabbleClient();
				else
					client = new ScrabbleDatabaseClient();

				while (!cancel)  {
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

}
