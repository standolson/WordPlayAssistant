package com.ppp.wordplayadvlib.fragments;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.ClipboardManager;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.WordPlayApp;
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

	private LinearLayout rootView;
	private ListView searchListView;
	private Handler searchHandler;
	private ProgressDialog progressDialog;
	private boolean cancel = false;

//	private AdView adView;

	public SearchFragment() { super(); }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		rootView = (LinearLayout)inflater.inflate(R.layout.search_fragment, null);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		
	    super.onActivityCreated(savedInstanceState);

	    // Get a handle on the ListView
	    searchListView = (ListView)rootView.findViewById(R.id.search_result_list);
	    searchListView.setOnItemClickListener(this);

		// This fragment has menu items...
		setHasOptionsMenu(true);

		// ...and wants to be retained on reconfiguration
		setRetainInstance(true);

//		if (WordPlayApp.isFreeMode())  {
//			LinearLayout header_layout = (LinearLayout)View.inflate(this, R.layout.admob_listview_footer, null);
//			adView = (AdView)header_layout.findViewById(R.id.listview_ad);
//			adView.loadAd(new AdRequest());
//			ListView list = (ListView)findViewById(android.R.id.list); 
//			list.addHeaderView(adView);
//		}

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

        // If the device was reoriented, then reconnect to the word judge
        // search.  In these cases, the JudgeSearch object is retained because
		// we called setRetainInstance(true) above.
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
		
	    // Create the new SearchObject used for this search
	    searchObject = new SearchObject(args);
    
	    Debug.i("SearchString: '" + searchString + "'");
		Debug.i("BoardString: '" + boardString + "'");
	    Debug.i("SearchType: " + searchType);
		Debug.i("Dictionary: " + dictionary);
		Debug.i("WordScores: " + wordScore);
		Debug.i("SortByScore: " + wordSort);

    	dictServer = new RFC2229();
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

	//
	// Activity Methods
	//

	@Override
	public void onDestroy()
	{

//		if (adView != null)
//			adView.destroy();

		super.onDestroy();

		// If we are not being reconfigured and are really going away
		// for good, free the memory being held by the result list.
		if ((searchThread != null) && !searchThread.isReconfigured())  {
			Debug.d("onDestroy: freeing memory");
			ArrayAdapter<?> adapter = (ArrayAdapter<?>)searchListView.getAdapter();
			if (adapter != null)
				adapter.clear();
			System.gc();
		}

	}

//	@Override
	//	public void onResume()
//	{
//		super.onResume();
//		if (WordPlayApp.isFreeMode())
//			adView.loadAd(new AdRequest());
//	}

	@Override
	public void onDetach()
	{

		super.onDetach();

		// If the user is reorienting the device, close the progress dialog
		// and reset the search handler
		Debug.v("SearchResult: onDetach executing");
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
			getActivity().finish();
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
						wordList);
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
						scoredWordList);
		searchListView.setAdapter(adapter);
		registerForContextMenu(searchListView);
		
	}

	//
	// Adapters and Utilities
	//

	private class WordListAdapter extends ArrayAdapter<String> implements SectionIndexer {

		private HashMap<String, Integer> indexer;
		private String[] sections = new String[0];

		WordListAdapter(Context ctx, int rowLayoutId, ArrayList<String> items)
		{

			super(ctx, rowLayoutId, items);

			if (wordSort == WordSortState.WORD_SORT_BY_ALPHA)  {

				// Create a map of first letters to array positions
				indexer = new HashMap<String, Integer>();
				for (int i = items.size() - 1; i >= 0; i -= 1)  {
					String word = items.get(i);
					String firstChar = (word.charAt(0) + "").toUpperCase();
					if ((firstChar.charAt(0) < 'A') || (firstChar.charAt(0) > 'Z'))
						firstChar = "@";
					indexer.put(firstChar, i);
				}

				// Now get all of the first letters we found and
				// create an ordered array for section names
				Set<String> keys = indexer.keySet();
				Iterator<String> it = keys.iterator();
				ArrayList<String> keyList = new ArrayList<String>();
				while (it.hasNext())
				    keyList.add(it.next());
				Collections.sort(keyList);
				sections = new String[keyList.size()];
				keyList.toArray(sections);

			}

		}
		
        public View getView(int position, View convertView, ViewGroup parent)
        {

            View v = convertView;

            if (v == null)  {
                LayoutInflater vi =
                	(LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.word_list, null);
            }

            SpannableString word = convertToBoardString(getItem(position), getItem(position));
            if (word != null)  {
            	TextView wordView = (TextView)v.findViewById(R.id.wl_word);
            	if (wordView != null)
            		wordView.setText(word);
            }
            
            return v;
            
        }

		public int getPositionForSection(int section)
		{
			if (wordSort != WordSortState.WORD_SORT_BY_ALPHA)
				return 0;
			if (section < 0)
				section = 0;
			else if (section >= sections.length)
				section = sections.length - 1;
			String letter = sections[section];
			return indexer.get(letter);
		}

		public int getSectionForPosition(int position)
		{
			if (wordSort != WordSortState.WORD_SORT_BY_ALPHA)
				return 0;
			int prevIndex = 0;
			for (int i = 0; i < sections.length; i += 1)  {
				if ((position < getPositionForSection(i)) && (position >= prevIndex))  {
			        prevIndex = i;
			        break;
			    }
			    prevIndex = i;
			}
			return prevIndex;
		}

		public Object[] getSections() { return sections; }
        
	}

	private class ScoredWordListAdapter extends ArrayAdapter<ScoredWord> implements SectionIndexer {
		
		private HashMap<String, Integer> indexer;
		private String[] sections = new String[0];
		
		ScoredWordListAdapter(Context ctx, int rowLayoutId, ArrayList<ScoredWord> items)
		{

			super(ctx, rowLayoutId, items);

			if (wordSort == WordSortState.WORD_SORT_BY_ALPHA)  {

				// Create a map of first letters to array positions
				indexer = new HashMap<String, Integer>();
				for (int i = items.size() - 1; i >= 0; i -= 1)  {
					String word = items.get(i).getWord();
					String firstChar = word.substring(0, 1).toUpperCase();
					if ((firstChar.charAt(0) < 'A') || (firstChar.charAt(0) > 'Z'))
						firstChar = "@";
					indexer.put(firstChar, i);
				}

				// Now get all of the first letters we found and
				// create an ordered array for section names
				Set<String> keys = indexer.keySet();
				Iterator<String> it = keys.iterator();
				ArrayList<String> keyList = new ArrayList<String>();
				while (it.hasNext())
				    keyList.add(it.next());
				Collections.sort(keyList);
				sections = new String[keyList.size()];
				keyList.toArray(sections);

			}

		}
		
        public View getView(int position, View convertView, ViewGroup parent)
        {
        	
            View v = convertView;

            if (v == null)  {
                LayoutInflater vi =
                	(LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.word_list, null);
            }

            ScoredWord word = getItem(position);
            if (word != null)  {
            	TextView wordView = (TextView)v.findViewById(R.id.wl_word);
            	if (wordView != null)
            		wordView.setText(convertToBoardString(word.toString(), word.getWord()));
            }
            
            return v;
            
        }

		public int getPositionForSection(int section)
		{
			if (wordSort != WordSortState.WORD_SORT_BY_ALPHA)
				return 0;
			if (section < 0)
				section = 0;
			else if (section >= sections.length)
				section = sections.length - 1;
			String letter = sections[section];
			return indexer.get(letter);
		}

		public int getSectionForPosition(int position)
		{
			if (wordSort != WordSortState.WORD_SORT_BY_ALPHA)
				return 0;
			int prevIndex = 0;
			for (int i = 0; i < sections.length; i += 1)  {
				if ((position < getPositionForSection(i)) && (position >= prevIndex))  {
			        prevIndex = i;
			        break;
			    }
			    prevIndex = i;
			}
			return prevIndex;
		}

		public Object[] getSections() { return sections; }

	}
	
	private class WordDefinitionsAdapter extends ArrayAdapter<String> {
		
		private String word;
		private ArrayList<String> defns;
		
		WordDefinitionsAdapter(Context ctx, int rowLayoutId, String w, ArrayList<String> items)
		{
			super(ctx, rowLayoutId, items);
			word = w;
			defns = items;
		}

        public View getView(int position, View convertView, ViewGroup parent)
        {
        	
            View v = convertView;

            if (v == null)  {
                LayoutInflater vi =
                	(LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.search_result, null);
            }

            String defn = defns.get(position);
            if (defn != null)  {
            	TextView wordView = (TextView)v.findViewById(R.id.sr_word);
            	TextView defnView = (TextView)v.findViewById(R.id.sr_definition);
            	if (wordView != null)
            		wordView.setText(word);
            	if (defnView != null)  {
            		int index = defn.indexOf('\n');
            		if (index != -1)
            			defnView.setText(defn.substring(0, defn.indexOf('\n')));
            		else
            			defnView.setText(defn);
            	}
            }
            
            return v;
            
        }

	}

	private SpannableString convertToBoardString(String str, String word)
	{

		SpannableString ss = new SpannableString(str);
		int start = 0;
		int end = 0;
		boolean outerFound = false;

		// Find all of the board letters and make them RED
		if ((boardString != null) && (boardString.length() > 0))  {

			StringBuffer inner = new StringBuffer(boardString);

			CharacterIterator outer = new StringCharacterIterator(str);
			for (char co = outer.first(); co != CharacterIterator.DONE; co = outer.next())  {
	
				boolean innerFound = false;
	
				// If we didn't have match on the previous character, reset the
				// start of the span
				if (!outerFound)
					start = outer.getIndex();
	
				// Look at each of the characters in the board letter string and if we
				// find one, add all of the character styles
				for (int i = 0; i < inner.length(); i += 1)  {

					char ci = inner.charAt(i);
	
					// If we have a match, set a span over the range and remove
					// the letter we found from the list of board letters
					if (ci == co)  {
						innerFound = true;
						end = outer.getIndex() + 1;
//						Debug.e("setting span on '" + str + "' start " + start + " end " + end);
						ss.setSpan(new ForegroundColorSpan(Color.RED), start, end, 0);
						inner.deleteCharAt(i);
						break;
					}
	
				}

				// If there are no more letters in the board string, quit
				if (inner.length() == 0)
					break;

				// Reset the flag so that we pick up a new start location
				// on the next iteration
				outerFound = innerFound;
	
			}

		}

		// Now if there were any wildcard characters in an anagram lookup,
		// we want to find the characters that were used as wildcards and
		// highlight them in GREEN
		String letters = searchObject.getSearchString() + searchObject.getBoardString();
		if ( (searchObject.getSearchType() == SearchType.OPTION_ANAGRAMS) &&
				(letters.contains(".") || letters.contains("?")) )  {

			StringBuffer wordBuffer = new StringBuffer(word);

			// Remove all of the wildcard characters from the set of letters
			letters = letters.replace(".", "").replace("?", "");

			// Iterate over all of the letters removing each from the word
			CharacterIterator iter = new StringCharacterIterator(letters);
			for (char ci = iter.first(); ci != CharacterIterator.DONE; ci = iter.next())  {
				int index = wordBuffer.indexOf(Character.toString(ci));
				if (index == -1)  {
//					Debug.e("wildcard: cannot find '" + ci + "' in '" + word + "'");
					continue;
				}
				wordBuffer.deleteCharAt(index);
			}

			// If there are no wildcards, we can safely return now
			if (wordBuffer.length() == 0)  {
//				Debug.e("wildcard: word '" + word + "' has no wildcards");
				return ss;
			}

//			Debug.e("wildcard: word '" + word + "' possible wildcards: '" + wordBuffer.toString() + "'");

			// Now iterate over the characters we think are wildcards and find
			// one that doesn't have a span on it already and set its span to
			// make it GREEN
			for (int i = 0; i < wordBuffer.length(); i += 1)  {
				
				int index = -1;
				char c = wordBuffer.charAt(i);

				while (true)  {

					// Find the next instance of the character
					index = word.indexOf(c, index + 1);
					if (index == -1)  {
//						Debug.e("wildcard: word '" + word + "' has no letter '" + c + "'");
						break;
					}
//					Debug.e("wildcard: word '" + word + "' found letter '" + c + "' at position " + index);

					// Get the span for that location.  If there are no spans on
					// this location, we can safely add a new span.
					ForegroundColorSpan[] spans = ss.getSpans(index, index, ForegroundColorSpan.class);
					if (spans.length > 0)
						continue;
					ss.setSpan(new ForegroundColorSpan(Color.GREEN), index, index + 1, 0);
					break;

				}

				// If we didn't find the letter, report
//				if (index == -1)
//					Debug.e("wildcard: word '" + word + "' didn't find letter '" + c + "'");

			}

		}

		return ss;

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
						StringBuilder resp = dictServer.defineWord(
								"^" + searchObject.getSearchString() + "$",
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
						StringBuilder resp = dictServer.matchWord(
								"^" + searchObject.getSearchString(),
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
						StringBuilder resp = dictServer.matchWord(
								searchObject.getSearchString(),
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
						StringBuilder resp = dictServer.matchWord(
								searchObject.getSearchString() + "$",
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
						StringBuilder resp = dictServer.matchWord(
								"^" + newSearchString + "$",
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
