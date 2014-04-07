package com.ppp.wordplayadvlib.fragments;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.ppp.wordplayadvlib.Constants;
import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.WordPlayApp;
import com.ppp.wordplayadvlib.activities.HelpViewer;
import com.ppp.wordplayadvlib.activities.SearchHistoryActivity;
import com.ppp.wordplayadvlib.activities.UserPreferenceActivity;
import com.ppp.wordplayadvlib.appdata.DictionaryType;
import com.ppp.wordplayadvlib.appdata.History;
import com.ppp.wordplayadvlib.appdata.JudgeHistory;
import com.ppp.wordplayadvlib.appdata.JudgeHistoryObject;
import com.ppp.wordplayadvlib.appdata.JudgeSearch;
import com.ppp.wordplayadvlib.appdata.JudgeSearch.JudgeSearchObject;
import com.ppp.wordplayadvlib.appdata.WordScoreState;
import com.ppp.wordplayadvlib.appdata.WordSortState;
import com.ppp.wordplayadvlib.database.WordlistDatabase;
import com.ppp.wordplayadvlib.database.schema.DatabaseInfo;
import com.ppp.wordplayadvlib.dialogs.AppErrDialog;
import com.ppp.wordplayadvlib.utils.Debug;
import com.ppp.wordplayadvlib.utils.Utils;
import com.ppp.wordplayadvlib.widgets.MultiStateButton;

@SuppressLint("ValidFragment")
public class WordPlayFragment extends Fragment implements View.OnClickListener
{

	private static final int RestartNotificationId = 1;

	private static final int AnagramTab = 0;
	private static final int WordJudgeTab = 1;
	private static final int DictionaryTab = 2;
	private static final int CrosswordTab = 3;

	private static final int InstallDbDialog = 1;
	private static final int FreeDialog = 2;
	private static final int UpgradeDbDialog = 3;
	private static final int AboutDialog = 4;
	private static final int NagDialog = 5;

	private static final int EmailActivity = 1;
	private static final int HelpViewerActivity = 2;
	private static final int UserPrefsActivity = 3;

	private ViewFlipper flipper = null;

	private Button dictButton = null;
	private Button wjButton = null;
	private Button anagramButton = null;
	private Button crosswordsButton = null;
	
	private Spinner dictSpinner = null;
	private Spinner wjSpinner = null;
	private Spinner anagramSpinner = null;
	private Spinner crosswordsSpinner = null;
	
	private MultiStateButton dictScoreToggle = null;
	private MultiStateButton dictSortToggle = null;
	private MultiStateButton anagramScoreToggle = null;
	private MultiStateButton anagramSortToggle = null;
	
	private static ListView wjListview = null;
	private static WordJudgeAdapter wjAdapter = null;
	private JudgeSearch wjSearchObj = null;
	
	private Intent searchIntent = null;
	private Intent savedSearchIntent = null;
	
	private static int searchCount = 0;
	private static int nagFrequency = Constants.NagDialogFrequency;
	private static Boolean hasNagged = false;
	private static boolean notificationIconEnabled = false;

	private int currentTab = AnagramTab;

	public WordPlayFragment() { super(); }

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		if (savedInstanceState != null)
			currentTab = savedInstanceState.getInt("currentTab", AnagramTab);
		return inflater.inflate(R.layout.wordplay_fragment, container, false);
	}

	@Override
    public void onActivityCreated(Bundle savedInstanceState) 
    {
    	
		super.onActivityCreated(savedInstanceState);

		// This fragment has menu items...
		setHasOptionsMenu(true);

		// ...and wants to be retained on reconfiguration
		setRetainInstance(true);

        // Setup the tabs
		setupAnagramTab();
		setupWordJudgeTab();
		setupDictionaryTab();
		setupCrosswordsTab();

        // Get important view elements
        flipper = (ViewFlipper)getActivity().findViewById(R.id.view_flipper);
        flipper.setDisplayedChild(currentTab);

        // Load the history
		loadHistory();

        // Create the restart notification if the preference is set
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
    	notificationIconEnabled = prefs.getBoolean("notification_bar", false);
		if (notificationIconEnabled)  {
			Intent intent = new Intent(getActivity(), getActivity().getClass());
			addRestartNotification(intent);
		}

        // If the device was reoriented, then reconnect to the word judge
        // search.  In these cases, the JudgeSearch object is retained because
		// we called setRetainInstance(true) above.
		if (wjSearchObj != null)  {
	        JudgeSearch.JudgeThread searchThread = wjSearchObj.getSearchThread();
			if (searchThread != null)  {
				JudgeSearchObject searchObject = searchThread.getSearchObject();
				if (searchThread.isAlive())  {
					Debug.v("WordPlay Reconfiguration: THREAD ALIVE!");
					searchObject.getJudgeObject().openProgressDialog(getActivity());
					try {
						searchThread.join();
					}
					catch (Exception e) {
						searchObject.setException(e);
					}
					searchObject.getJudgeObject().displayResults();
					return;
				}
				else {
					Debug.v("WordPlay Reconfiguration: DISPLAY RESULTS");
					searchObject.getJudgeObject().displayResults();
					return;
				}
			}
		}

        // For the free mode, see if we've shown the free dialog
        // and if we haven't, show it.  If we show it, when we're
        // done, the database will get installed.
        //
        // For the paid mode, make sure we've got a database.
		if (WordPlayApp.getInstance().isFreeMode())
			freeDialogCheck();
		else
			createDatabaseIfMissing();

    }

	//
	// Activity Methods
	//

	public void onSaveInstanceState(Bundle savedInstanceState)
	{
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putInt("currentTab", currentTab);
	}

	public void onDetach()
	{

		super.onDetach();

		// On device reorientation, we need close the progress dialog
		// for any open WordJudge search and reset the search handler
		// before allowing reorientation to happen.
		//
		// The search object is retained because the fragment is retained
		// during orientation.
		if (wjSearchObj != null)  {
			Debug.v("WordPlay: onDetach executing");
			if (wjSearchObj.getProgressDialog() != null)
				wjSearchObj.closeProgressDialog();
			wjSearchObj.getSearchThread().getSearchObject().setSearchHandler(null);
		}

	}

	public void onStop()
    {
    	super.onStop();
    	saveHistory();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {

    	switch (requestCode)  {
	
	    	case EmailActivity:
	
	    		// When returning from email sent from the nag dialog, finish
	    		// the search the user started
	    		if (savedSearchIntent != null)  {
	    			try {
	    				startActivity(savedSearchIntent);
	    			}
	    			catch (Exception e) {}
	    		}
	    		break;
	
	    	case HelpViewerActivity:
	 
	    		// We're returning from showing the release notes from the
	    		// free app installed dialog.  Proceed to creating the database
	    		// if that is required.
	    		createDatabaseIfMissing();
	    		break;
	
	    	case UserPrefsActivity:
	
	    		// We've returned from setting preferences.  Apply the only one we
	    		// know about now by adding or removing the notification icon.
	    		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
	        	boolean newNotificationSetting = prefs.getBoolean("notification_bar", false);
	        	if (newNotificationSetting != notificationIconEnabled)  {
	        		if (newNotificationSetting)  {
	        	    	Intent intent = new Intent(getActivity(), getActivity().getClass());
	        	    	addRestartNotification(intent);
	        		}
	        		else
	        			removeNotification();
	        		notificationIconEnabled = newNotificationSetting;
	        	}
	        	break;

    	}
    	
    }

    private void showDialog(int id)
    {

    	DialogFragment newFragment = null;

    	switch (id) {

	    	case InstallDbDialog:
//	    	    newFragment = new DbInstallDialog(false);
//	    	    newFragment.setCancelable(false);
//	    	    newFragment.show(getFragmentManager(), "InstallDbDialog");
	    		break;

	    	case FreeDialog:
	    		newFragment = new FreeDialog();
	    		newFragment.setCancelable(false);
	    		newFragment.show(getFragmentManager(), "FreeDialog");
	    		break;

	    	case UpgradeDbDialog:
//	    	    newFragment = new DbInstallDialog(true);
//	    	    newFragment.setCancelable(false);
//	    	    newFragment.show(getFragmentManager(), "UpgradeDbDialog");
	    		break;

	    	case AboutDialog:
	    		newFragment = new AboutDialog();
	    		newFragment.show(getFragmentManager(), "AboutDialog");
	    		break;

	    	case NagDialog:
	    		newFragment = new NagDialog();
	    		newFragment.show(getFragmentManager(), "NagDialog");
	    		break;

    	}

    }

    public void onClick(View v)
    {
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
    	startSearchIntent(v);
    }
    
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.search_menu, menu);
    }

    public void onPrepareOptionsMenu(Menu menu)
    {

    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

    	// If the notification bar is turned off, don't show "Exit"
    	MenuItem item = menu.findItem(R.id.exit_menu);
    	item.setVisible(prefs.getBoolean("notification_bar", false));

    }
    
    public boolean onOptionsItemSelected(MenuItem item)
    {

    	Activity activity = getActivity();

    	// Preferences
    	if (item.getItemId() == R.id.settings_menu)  {
			Intent intent = new Intent(activity, UserPreferenceActivity.class);
			try {
				startActivityForResult(intent, UserPrefsActivity);
			}
			catch (Exception e) {
				Debug.e("User Prefs Startup Failed: " + e);
			}
		}

    	// Clear history
    	else if (item.getItemId() == R.id.clearhistory_menu)  {
			if (currentTab == WordJudgeTab)  {
				clearJudgeHistory(true);
				updateJudgeHistoryAdapter();
				wjAdapter.notifyDataSetChanged();
			}
			else
				clearHistory(true);
		}

    	// Dictionaries
    	else if (item.getItemId() == R.id.dictionary_menu)
			showDictionaries();

    	// Help
    	else if (item.getItemId() == R.id.showhelp_menu)
			showHelp();

    	// Exit
    	else if (item.getItemId() == R.id.exit_menu)  {
			removeNotification();
			activity.finish();
		}

    	// Reinstall Dictionary
    	else if (item.getItemId() == R.id.dictionary_reinstall_menu)  {
			WordlistDatabase.deleteDatabaseFile(getActivity());
			try {
				WordlistDatabase.createDatabaseFile(getActivity());
			}
			catch (Exception e) {
				createDatabaseExceptionDialog(getActivity(), e);
			}
		}
    	
    	return true;
    	
    }

	//
	// Filters and Listeners
	//

    // A filter for input of one or more alphabetic characters
    InputFilter alphaFilter = new InputFilter() {
    	public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend)
    	{
    		if (end > start)  {
    			String destText = dest.toString();
    			String resultText =
    				destText.substring(0, dstart) + source.subSequence(start, end) + destText.substring(dend);
    			if (!resultText.matches("[a-zA-Z]*"))  {
    				if (source instanceof Spanned)
    					return new SpannableString("");
    				else
    					return "";
    			}
    		}
    		return null;
    	}
    };

    // A filter for input of a list of words
    InputFilter commaFilter = new InputFilter() {
    	public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend)
    	{
    		if (end > start)  {
    			String destText = dest.toString();
    			String resultText =
    				destText.substring(0, dstart) + source.subSequence(start, end) + destText.substring(dend);
    			if (!resultText.matches("([a-zA-Z]+,?)*"))  {
    				if (source instanceof Spanned)
    					return new SpannableString("");
    				else
    					return "";
    			}
    		}
    		return null;
    	}
    };

    // A filter for a wildcarded text entry field
    InputFilter searchFilter = new InputFilter() {
    	public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend)
    	{
    		if (end > start)  {
    			String destText = dest.toString();
    			String resultText =
    				destText.substring(0, dstart) + source.subSequence(start, end) + destText.substring(dend);
    			if (!resultText.matches("[a-zA-Z?.]*"))  {
    				if (source instanceof Spanned)
    					return new SpannableString("");
    				else
    					return "";
    			}
    		}
    		return null;
    	}
    };
    
    // A key listener which allows ENTER to be used like clicking the
    // "Search" button
    OnKeyListener keyListener = new OnKeyListener() {
    	public boolean onKey(View v, int keyCode, KeyEvent event)
    	{

    		// Only operate on the ENTER key when pressed down
    		if (event.getAction() == KeyEvent.ACTION_UP)
    			return false;
    		if (event.getKeyCode() != KeyEvent.KEYCODE_ENTER)
    			return false;

    		// Dismiss the soft keyboard
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);

            // Perform the search
    		startSearchIntent(v);
    		
    		return true;
    		
    	}
    };

    //
    // UI Setup
    //

	private void setupAnagramTab()
	{

		Activity activity = getActivity();

        anagramButton = (Button)activity.findViewById(R.id.AnagramsButton);
        anagramButton.setOnClickListener(this);

        Button anagramTabButton = (Button)activity.findViewById(R.id.anagram_tab_button);
        anagramTabButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) { changeTab(v); }
		});

        final EditText anagramsTrayText = (EditText)activity.findViewById(R.id.AnagramsTrayText);
        anagramsTrayText.setFilters(new InputFilter[] { searchFilter });
        anagramsTrayText.setOnKeyListener(keyListener);
        final EditText anagramsBoardText = (EditText)activity.findViewById(R.id.AnagramsBoardText);
        anagramsBoardText.setFilters(new InputFilter[] { alphaFilter });
        anagramsBoardText.setOnKeyListener(keyListener);

        final Button anagramsTrayClearButton = (Button)activity.findViewById(R.id.AnagramsTrayTextClear);
        anagramsTrayClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) { anagramsTrayText.setText(""); }
        });
        final Button anagramsBoardClearButton = (Button)activity.findViewById(R.id.AnagramsBoardTextClear);
        anagramsBoardClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) { anagramsBoardText.setText(""); }
		});

    	anagramScoreToggle = (MultiStateButton)activity.findViewById(R.id.AnagramsWordScores);
    	anagramScoreToggle.setStateNames(getResources().getStringArray(R.array.word_score_toggle_states));
    	anagramSortToggle = (MultiStateButton)activity.findViewById(R.id.AnagramsSortOrder);
    	anagramSortToggle.setStateNames(getResources().getStringArray(R.array.sort_order_toggle_states));
    	anagramScoreToggle.setOnChangeListener(new View.OnClickListener() {
    		public void onClick(View v)
    		{
    			MultiStateButton button = (MultiStateButton)v;
    			WordScoreState score = WordScoreState.fromInt(button.getState() + 1);
    			boolean button_state = true;
    			if (score == WordScoreState.WORD_SCORE_STATE_OFF)
    				button_state = false;
    			anagramSortToggle.setButtonState(
    									WordSortState.WORD_SORT_BY_WORD_SCORE.ordinal() - 1,
    									button_state);
    		}
    	});

    	anagramSpinner = (Spinner)activity.findViewById(R.id.anagrams_dict_spinner);
    	anagramSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
    		public void onItemSelected(AdapterView<?> parent, View view, int i, long l) {}
    		public void onNothingSelected(AdapterView<?> parent) {}
    	});

	}

	private void setupWordJudgeTab()
	{

		Activity activity = getActivity();

        wjButton = (Button)activity.findViewById(R.id.WordJudgeButton);
        wjButton.setOnClickListener(this);

        Button wjTabButton = (Button)activity.findViewById(R.id.wj_tab_button);
        wjTabButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) { changeTab(v); }
		});

        final EditText wjText = (EditText)activity.findViewById(R.id.WordJudgeText);
        wjText.setFilters(new InputFilter[] { commaFilter });
        wjText.setOnKeyListener(keyListener);
		
        wjListview = (ListView)activity.findViewById(R.id.wordjudge_listview);
        wjListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View v, int position, long id)
            {
        		startJudgeHistorySearch(position);
            }
        });

        final Button wjClearButton = (Button)activity.findViewById(R.id.WordJudgeTextClear);
        wjClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) { wjText.setText(""); }
		});

    	wjSpinner = (Spinner)activity.findViewById(R.id.wordjudge_dict_spinner);
    	wjSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
    		public void onItemSelected(AdapterView<?> parent, View view, int i, long l) {}
    		public void onNothingSelected(AdapterView<?> parent) {}
    	});

        if (WordPlayApp.getInstance().isFreeMode())  {
        	LinearLayout headerLayout = (LinearLayout)View.inflate(activity, R.layout.admob_listview_footer, null);
            wjListview.addHeaderView(headerLayout);
        }
        updateJudgeHistoryAdapter();

	}

	private void setupDictionaryTab()
	{

		final Activity activity = getActivity();

		if (WordPlayApp.getInstance().isFreeMode())
			activity.findViewById(R.id.dictionary_tab_button).setVisibility(View.GONE);

        dictButton = (Button)activity.findViewById(R.id.DictionaryButton);
        dictButton.setOnClickListener(this);

        Button dictionaryTabButton = (Button)activity.findViewById(R.id.dictionary_tab_button);
        dictionaryTabButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) { changeTab(v); }
		});

        final EditText dictText = (EditText)activity.findViewById(R.id.DictionaryText);
        dictText.setFilters(new InputFilter[] { alphaFilter });
        dictText.setOnKeyListener(keyListener);

        final Button dictClearButton = (Button)activity.findViewById(R.id.DictionaryTextClear);
        dictClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) { dictText.setText(""); }
		});

        dictScoreToggle = (MultiStateButton)activity.findViewById(R.id.DictionaryWordScores);
        dictScoreToggle.setStateNames(getResources().getStringArray(R.array.word_score_toggle_states));
        dictSortToggle = (MultiStateButton)activity.findViewById(R.id.DictionarySortOrder);
        dictSortToggle.setStateNames(getResources().getStringArray(R.array.sort_order_toggle_states));
        dictScoreToggle.setOnChangeListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		MultiStateButton button = (MultiStateButton)v;
        		WordScoreState score = WordScoreState.fromInt(button.getState() + 1);
        		boolean button_state = true;
        		if (score == WordScoreState.WORD_SCORE_STATE_OFF)
        			button_state = false;
        		dictSortToggle.setButtonState(
        									WordSortState.WORD_SORT_BY_WORD_SCORE.ordinal() - 1,
        									button_state);
        	}
        });
    	
    	dictSpinner = (Spinner)activity.findViewById(R.id.dictionary_dict_spinner);
    	dictSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
    		
    		public void onItemSelected(AdapterView<?> parent, View view, int i, long l)
    		{

    			DictionaryType dict = DictionaryType.fromInt(i + 1);
	    		final Spinner spinner = (Spinner)activity.findViewById(R.id.DictionarySpinner);

    			if (dict.isScrabbleDict())  {
      				dictScoreToggle.setVisibility(View.VISIBLE);
      				dictSortToggle.setVisibility(View.VISIBLE);
      				spinner.setVisibility(View.VISIBLE);
    			}
    			else {
    	    		if (dict.isThesaurus())
    	    			spinner.setVisibility(View.GONE);
    	    		else
    	    			spinner.setVisibility(View.VISIBLE);
    				dictScoreToggle.setVisibility(View.GONE);
    				dictSortToggle.setVisibility(View.GONE);
    			}
    			setDictionaryTabMode();

    		}
    		
    		public void onNothingSelected(AdapterView<?> parent)  { }
    		
    	});

	}

	private void setupCrosswordsTab()
	{

		Activity activity = getActivity();

		if (WordPlayApp.getInstance().isFreeMode())
			activity.findViewById(R.id.crosswords_tab_button).setVisibility(View.GONE);

		crosswordsButton = (Button)activity.findViewById(R.id.CrosswordsButton);
        crosswordsButton.setOnClickListener(this);

        Button crosswordsTabButton = (Button)activity.findViewById(R.id.crosswords_tab_button);
        crosswordsTabButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) { changeTab(v); }
		});

        final EditText crosswordsText = (EditText)activity.findViewById(R.id.CrosswordsText);
        crosswordsText.setFilters(new InputFilter[] { searchFilter });
        crosswordsText.setOnKeyListener(keyListener);

        final Button crosswordsClearButton = (Button)activity.findViewById(R.id.CrosswordsTextClear);
        crosswordsClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) { crosswordsText.setText(""); }
		});

    	crosswordsSpinner = (Spinner)activity.findViewById(R.id.crosswords_dict_spinner);
    	crosswordsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
    		public void onItemSelected(AdapterView<?> parent, View view, int i, long l) {}
    		public void onNothingSelected(AdapterView<?> parent) {}
		});

	}

	private void changeTab(View v)
	{

		// Map view ID to ViewFlipper child
		if (v.getId() == R.id.anagram_tab_button)
			currentTab = AnagramTab;
		else if (v.getId() == R.id.wj_tab_button)
			currentTab = WordJudgeTab;
		else if (v.getId() == R.id.dictionary_tab_button)  {
			currentTab = DictionaryTab;
			setDictionaryTabMode();
		}
		else if (v.getId() == R.id.crosswords_tab_button)
			currentTab = CrosswordTab;

		// Change the view
		flipper.setDisplayedChild(currentTab);

	}

	//
	// Search Activity Support
	//

    private void startSearchIntent(View v)
    {
    	
//    	int id = v.getId();
//    	Activity activity = getActivity();
//    	Intent intent = new Intent(getActivity(), SearchResultActivity.class);
//    	String searchString = "";
//    	String boardString = "";
//    	WordScoreState wordScores = WordScoreState.WORD_SCORE_UNKNOWN;
//    	WordSortState wordSort = WordSortState.WORD_SORT_UNKNOWN;
//    	DictionaryType dictionary = DictionaryType.DICTIONARY_UNKNOWN;
//    	SearchType searchType = SearchType.OPTION_UNKNOWN;
//
//    	// Process the DICTIONARY tab
//    	if ((id == R.id.DictionaryButton) || (id == R.id.DictionaryText))  {
//    		
//    		final EditText dictText = (EditText)activity.findViewById(R.id.DictionaryText);
//       	final Spinner spinner = (Spinner)activity.findViewById(R.id.DictionarySpinner);
//
//    		searchString = dictText.getText().toString();	    
//    		dictionary = DictionaryType.fromInt((int)dictSpinner.getSelectedItemId() + 1);
//
//    		if (dictionary.isThesaurus())  {
//    			searchType = SearchType.OPTION_THESAURUS;
//				dictionary = DictionaryType.DICTIONARY_DICT_DOT_ORG;
//			}
//			else {
//				searchType = SearchType.fromInt((int)spinner.getSelectedItemId());
//			}
//
//    		if (dictionary.isScrabbleDict())  {
//        		wordScores = WordScoreState.fromInt(dictScoreToggle.getState() + 1);
//        		wordSort = WordSortState.fromInt(dictSortToggle.getState() + 1);
//    		}
//    		else {
//    			wordScores = WordScoreState.WORD_SCORE_UNKNOWN;
//    			wordSort = WordSortState.WORD_SORT_UNKNOWN;
//    		}
//   		
//    	}
//
//    	// Process the WORD JUDGE tab
//    	else if ((id == R.id.WordJudgeButton) || (id == R.id.WordJudgeText))  {
//
//    		final EditText wjText = (EditText)activity.findViewById(R.id.WordJudgeText);
//    		searchString = wjText.getText().toString();
//    		dictionary = DictionaryType.fromInt((int)wjSpinner.getSelectedItemId() + 1);
//
//    		if (!validateString(searchString, dictionary, false))
//    			return;
//
//    		searchCount += 1;
//    		Debug.v("SEARCH_COUNT " + searchCount);
//    		if (!hasNagged && ((searchCount % nagFrequency) == 0))  {
//    			searchIntent = null;
//    			showDialog(NagDialog);
//    			hasNagged = true;
//    		}
//
//    		wjSearchObj = new JudgeSearch();
//    		wjSearchObj.execute(this, searchString, dictionary);
//    		wjText.setText("");
//
//    		return;
//
//    	}
//
//    	// Process the ANAGRAMS tab
//    	else if ((id == R.id.AnagramsButton) || (id == R.id.AnagramsTrayText) || (id == R.id.AnagramsBoardText))   {
//
//    		final EditText anagramsTrayText = (EditText)activity.findViewById(R.id.AnagramsTrayText);
//    		final EditText anagramsBoardText = (EditText)activity.findViewById(R.id.AnagramsBoardText);
// 
//    		searchType = SearchType.OPTION_ANAGRAMS;
//    		searchString = anagramsTrayText.getText().toString();
//    		boardString = anagramsBoardText.getText().toString();
//    		dictionary = DictionaryType.fromInt((int)anagramSpinner.getSelectedItemId() + 1);
//    		wordScores = WordScoreState.fromInt(anagramScoreToggle.getState() + 1);
//    		wordSort = WordSortState.fromInt(anagramSortToggle.getState() + 1);
//
//    		if (searchString != null)
//    			searchString = searchString.toLowerCase();
//    		if (boardString != null)
//    			boardString = boardString.toLowerCase();
//
//    		if (!validateString(searchString + boardString, dictionary, true))
//    			return;
//
//    	}
//
//    	// Process the CROSSWORDS tab
//    	else if ((id == R.id.CrosswordsButton) || (id == R.id.CrosswordsText))   {
//
//    		final EditText crosswordsText = (EditText)activity.findViewById(R.id.CrosswordsText);
//    		searchString = crosswordsText.getText().toString();
//    		searchType = SearchType.OPTION_CROSSWORDS;
//    		dictionary = DictionaryType.fromInt((int)crosswordsSpinner.getSelectedItemId() + 1);
//    		wordScores = WordScoreState.WORD_SCORE_STATE_OFF;
//    		wordSort = WordSortState.WORD_SORT_BY_ALPHA;
//    		
//    	}
//
//		intent.putExtra("SearchString", searchString);
//		intent.putExtra("BoardString", boardString);
//		intent.putExtra("SearchType", searchType.ordinal());
//		intent.putExtra("Dictionary", dictionary.ordinal());
//		intent.putExtra("WordScores", wordScores.ordinal());
//		intent.putExtra("WordSort", wordSort.ordinal());
//
//		// Only start a search if the search string exists and has a length
//		if ((searchString != null) && (searchString.length() != 0))  {
// 
//			// Make sure it isn't zero length after removing spaces
//			searchString = searchString.replace(" ", "");
//			if (searchString.length() == 0)  {
//				Toast.makeText(activity, "Please enter a word or search string", Toast.LENGTH_SHORT).show();
//				return;
//			}
//
//			// Add this search to the history
//			History.getInstance().addHistory(searchString,
//    											boardString,
//    											searchType,
//    											dictionary,
//    											wordScores,
//    											wordSort);
//
//			// Update the search count and perform a nag dialog if required
//			searchCount += 1;
//			Debug.v("SEARCH_COUNT " + searchCount);
//			if (!hasNagged && ((searchCount % nagFrequency) == 0))  {
//				searchIntent = intent;
//				showDialog(NagDialog);
//				hasNagged = true;
//			}
//			else {
//				try {
//					startActivity(intent);
//				}
//				catch (Exception e) {}
//			}
//
//    	}
//    	else {
//			Toast.makeText(activity, "Please enter a word or search string", Toast.LENGTH_SHORT).show();
//			return;
//    	}

    }

    //
    // Menu Helpers
    //

    private void showDictionaries()
    {
    	if (currentTab == DictionaryTab)
    		dictSpinner.performClick();
    	else if (currentTab == WordJudgeTab)
    		wjSpinner.performClick();
    	else if (currentTab == AnagramTab)
    		anagramSpinner.performClick();
    	else if (currentTab == CrosswordTab)
    		crosswordsSpinner.performClick();
    }
    
    private void showHistory()
    {

    	Intent intent = new Intent(getActivity(), SearchHistoryActivity.class);
    	try {
    		startActivity(intent);
    	}
    	catch (Exception e) {}
    }

    private void showHelp()
    {
    	
    	String str = null;
    	Intent intent = null;
    	
    	if (currentTab == DictionaryTab)  {
    		DictionaryType dict =
    			DictionaryType.fromInt((int)dictSpinner.getSelectedItemId() + 1);
    		if (dict.isThesaurus())
    			str = getHelpText("Thesaurus", R.raw.thesaurus_help);
    		else
    			str = getHelpText("Dictionary", R.raw.dictionary_help);
    	}
    	else if (currentTab == WordJudgeTab)
    		str = getHelpText("Word Judge", R.raw.wordjudge_help);
    	else if (currentTab == AnagramTab)
    		str = getHelpText("Anagrams", R.raw.anagrams_help);
    	else if (currentTab == CrosswordTab)
    		str = getHelpText("Crosswords", R.raw.crosswords_help);
    	
		intent = new Intent(getActivity(), HelpViewer.class);
		intent.putExtra("HelpText", str);
		try {
			startActivity(intent);
		}
		catch (Exception e) {}
    	
    }
 
    private String getHelpText(String whichHelp, int id)
    {

    	BufferedReader rd =
    		new BufferedReader(new InputStreamReader(getResources().openRawResource(id)), Constants.BufSize);
    	String retval = "";
    	String line;
    	
    	try {
    		while ((line = rd.readLine()) != null)  {
    			if (line.length() == 0)
    				continue;
    			retval += line.replace('\n', ' ');
    		}
    		rd.close();
    	}
    	catch (IOException e) {
    		return "Unable to view '" + whichHelp + "' help at this time.";
    	}
    	
    	return retval;
    	
    }

    //
    // Dialogs
    //

    public static class AboutDialog extends DialogFragment {

    	WordPlayFragment fragment;

    	public AboutDialog() { super(); }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {

        	AlertDialog.Builder builder;
        	final AlertDialog dialog;

        	fragment =
        		(WordPlayFragment)getFragmentManager().findFragmentById(R.id.wordplay_activity_fragment);

        	LayoutInflater inflater =
        		(LayoutInflater)getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        	final View layout =
        		inflater.inflate(R.layout.about_dialog,
        							(ViewGroup)getActivity().findViewById(R.id.about_dialog_layout));

        	builder = new AlertDialog.Builder(getActivity());
        	builder.setView(layout);
        	dialog = builder.create();

        	layout.setOnClickListener(new View.OnClickListener() {
        		public void onClick(View v)
        		{
        			if (!getActivity().isFinishing())
        				dismiss();
        		}
        	});

        	ImageView iconImage = (ImageView)layout.findViewById(R.id.about_dialog_image);
        	iconImage.setOnClickListener(new View.OnClickListener() {
        		public void onClick(View v)
        		{
        			Intent myIntent =
        				new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.WebAddress));
        			if (!getActivity().isFinishing())  {
        				dismiss();
    	    			try {
    	    				if (!getActivity().isFinishing())
    	    					startActivity(myIntent);
    	    			}
    	    			catch (Exception e) {}
        			}
        		}
        	});
        	    	
//        	final String appName = getString(R.string.app_name);
//        	TextView versionText = (TextView)layout.findViewById(R.id.about_dialog_version);
//        	versionText.setText(appName + " v" + Constants.AppMajorVersion + "." + Constants.AppMinorVersion);
        	
        	TextView copyrightText = (TextView)layout.findViewById(R.id.about_dialog_copyright);
        	copyrightText.setText(getString(R.string.copyright));
        	
        	TextView companyNameText = (TextView)layout.findViewById(R.id.about_dialog_company_name);
        	companyNameText.setText(getString(R.string.company_name));
        	
        	Button contactButton = (Button)layout.findViewById(R.id.contact_us);
//        	contactButton.setOnClickListener(new View.OnClickListener() {
//        		public void onClick(View v)
//        		{
//    	    		Intent intent = new Intent(Intent.ACTION_SEND);
//    	    		intent.setType("message/rfc822");
//    	    		intent.putExtra(Intent.EXTRA_EMAIL, new String[] { Constants.EmailAddress });
//    	    		intent.putExtra(Intent.EXTRA_SUBJECT,
//    	    				"Comments on " + appName + " v" +
//    	    				Constants.AppMajorVersion + "." + Constants.AppMinorVersion);
//                	intent.putExtra(android.content.Intent.EXTRA_TEXT, "");
//                	if (!getActivity().isFinishing())  {
//                		dismiss();
//    	            	try {
//    	            		if (!getActivity().isFinishing())
//    	            			startActivity(intent);
//    	            	}
//    	            	catch (ActivityNotFoundException exception) {
//    	            		Utils.configureEmailAlert(getActivity());
//    	            	}
//                	}
//        		}
//        	});
        	
        	Button releaseNotesButton = (Button)layout.findViewById(R.id.release_notes);
        	releaseNotesButton.setOnClickListener(new View.OnClickListener()  {
        		public void onClick(View v)
        		{
        			String str = fragment.getHelpText("Release Notes", R.raw.release_notes);
        			Intent intent = new Intent(getActivity(), HelpViewer.class);
        			intent.putExtra("HelpText", str);
        			if (!getActivity().isFinishing())  {
        				dismiss();
    	    			try {
    	    				if (!getActivity().isFinishing())
    	    					startActivity(intent);
    	    			}
    	    			catch (Exception e) {}
        			}
        		}
        	});

        	Button buyItButton = (Button)layout.findViewById(R.id.buy_it);
        	fragment.setMarketButton(dialog, buyItButton, true);
        		
        	ImageView dictOrgImage = (ImageView)layout.findViewById(R.id.powered_by_image);
        	dictOrgImage.setOnClickListener(new View.OnClickListener() {
        		public void onClick(View v)
        		{
        			Intent myIntent =
        				new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.DictOrgWebAddress));
        			if (!getActivity().isFinishing())  {
    	    			dismiss();
    	    			try {
    	    				if (!getActivity().isFinishing())
    	    					startActivity(myIntent);
    	    			}
    	    			catch (Exception e) {}
        			}
        		}
        	});
        	
        	return dialog;

        }

    }

    public static class NagDialog extends DialogFragment {

    	WordPlayFragment fragment;

    	public NagDialog() { super(); }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {

        	AlertDialog.Builder builder;
        	final AlertDialog dialog;

        	fragment =
        		(WordPlayFragment)getFragmentManager().findFragmentById(R.id.wordplay_activity_fragment);
        	
        	LayoutInflater inflater =
        		(LayoutInflater)getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        	final View layout =
        		inflater.inflate(R.layout.nag_dialog,
        							(ViewGroup)getActivity().findViewById(R.id.nag_dialog_layout));

        	builder = new AlertDialog.Builder(getActivity());
        	builder.setView(layout);
        	dialog = builder.create();
        	
//        	Button contactButton = (Button)layout.findViewById(R.id.nag_button);
//        	contactButton.setOnClickListener(new View.OnClickListener() {
//        		public void onClick(View v)
//        		{
//
//    	    		Intent intent = new Intent(Intent.ACTION_SEND);
//    	        	String appName = getString(R.string.app_name);
//
//    	    		intent.setType("message/rfc822");
//    	    		intent.putExtra(Intent.EXTRA_EMAIL, new String[] { Constants.EmailAddress });
//    	    		intent.putExtra(Intent.EXTRA_SUBJECT,
//    	    				"Comments on " + appName + " v" + Constants.AppMajorVersion + "." + Constants.AppMinorVersion);
//                	intent.putExtra(android.content.Intent.EXTRA_TEXT, "");
//                	if (!getActivity().isFinishing())  {
//    	            	dismiss();
//    	            	fragment.savedSearchIntent = fragment.searchIntent;
//    	            	try {
//    	            		if (!getActivity().isFinishing())
//    	            			startActivityForResult(intent, EmailActivity);
//    	            	}
//    	            	catch (Exception e) {
//    	    	    		startActivity(fragment.savedSearchIntent);
//    	            	}
//                	}
//
//        		}
//        	});

        	return dialog;

        }

        @Override
        public void onCancel(DialogInterface dialog)
        {
			if (!getActivity().isFinishing())  {
				dismiss();
				if (fragment.searchIntent != null)  {
					try {
						if (!getActivity().isFinishing())
							startActivity(fragment.searchIntent);
					}
					catch (Exception e) {}
				}
			}
        }

    }

//    public static class DbInstallDialog extends DialogFragment {
//
//    	WordPlayFragment fragment;
//    	boolean isUpgrade = false;
//
//    	public DbInstallDialog() { super(); }
//
//    	public DbInstallDialog(boolean isUpgrade)
//    	{
//    		super();
//    		this.isUpgrade = isUpgrade;
//            Bundle args = new Bundle();
//            args.putBoolean("isUpgrade", isUpgrade);
//            setArguments(args);
//    	}
//
//    	@Override
//    	public void onSaveInstanceState(Bundle savedInstanceState)
//    	{
//    		savedInstanceState.putBoolean("isUpgrade", isUpgrade);
//    	}
//
//        @Override
//        public Dialog onCreateDialog(Bundle savedInstanceState)
//        {
//
//        	AlertDialog.Builder builder;
//        	final AlertDialog dialog;
//        	boolean isUpgrade = getArguments().getBoolean("isUpgrade");
//
//        	fragment =
//        		(WordPlayFragment)getFragmentManager().findFragmentById(R.id.wordplay_activity_fragment);
//
//        	if (savedInstanceState != null)
//        		isUpgrade = savedInstanceState.getBoolean("isUpgrade");
//
//        	LayoutInflater inflater =
//        		(LayoutInflater)getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
//        	final View layout =
//        		inflater.inflate(R.layout.dictionary_install_dialog,
//        							(ViewGroup)getActivity().findViewById(R.id.dictionary_install_layout));
//
//        	builder = new AlertDialog.Builder(getActivity());
//        	builder.setView(layout);
//        	dialog = builder.create();
//
//        	TextView textView = (TextView)layout.findViewById(R.id.dictionary_mode_text);
////        	String text = String.format(isUpgrade ?
////        									getString(R.string.dictionary_upgrade_dialog_text) :
////        									getString(R.string.dictionary_install_dialog_text),
////        								WordPlayApp.getInstance().isFreeMode() ?
////        									" Free" : "",
////        								Constants.AppMajorVersion, Constants.AppMinorVersion);
////        	textView.setText(text);
//
//        	Button okButton = (Button)layout.findViewById(R.id.dictionary_ok_button);
//        	okButton.setOnClickListener(new View.OnClickListener() {
//    			@Override
//    			public void onClick(View v)
//    			{
//    				fragment.startDatabaseInstallation(getActivity(), DbInstallDialog.this);
//    			}
//    		});
//
//        	return dialog;
//
//        }
//
//    }

    public static class FreeDialog extends DialogFragment {

    	WordPlayFragment fragment;

    	public FreeDialog() { super(); }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {

        	AlertDialog.Builder builder;
        	final AlertDialog dialog;

        	fragment =
        		(WordPlayFragment)getFragmentManager().findFragmentById(R.id.wordplay_activity_fragment);

        	LayoutInflater inflater =
        		(LayoutInflater)getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        	final View layout =
        		inflater.inflate(R.layout.free_dialog,
        							(ViewGroup)getActivity().findViewById(R.id.free_mode_layout));

        	builder = new AlertDialog.Builder(getActivity());
        	builder.setView(layout);
        	dialog = builder.create();

        	Button showRelNotesButton = (Button)layout.findViewById(R.id.free_mode_relnotes_button);
        	showRelNotesButton.setOnClickListener(new View.OnClickListener() {
        		public void onClick(View v) {
        			String str =
        				fragment.getHelpText("Release Notes", R.raw.release_notes);
        			Intent intent = new Intent(getActivity(), HelpViewer.class);
        			intent.putExtra("HelpText", str);
        			if (!getActivity().isFinishing())  {
    	    			dismiss();
    	    			try {
    	    				if (!getActivity().isFinishing())  {
    	    					try {
    	    						fragment.startActivityForResult(intent, HelpViewerActivity);
    	    					}
    	    					catch (Exception e) {}
    	    				}
    	    			}
    	    			catch (Exception e) {}
        			}
        		}
        	});

        	Button continueButton = (Button)layout.findViewById(R.id.free_mode_continue_button);
        	continueButton.setOnClickListener(new View.OnClickListener() {
    			@Override
    			public void onClick(View v)
    			{
    				dismiss();
    				fragment.createDatabaseIfMissing();
    			}
    		});

        	return dialog;

        }

    }

    //
    // Database Installation
    //

    private void startDatabaseInstallation(Context context, DialogFragment dialog)
    {
		new DatabaseWaitTask(context, dialog).execute();    	
    }

    private void createDatabaseIfMissing()
    {

    	WordlistDatabase db =
    		(WordlistDatabase) new WordlistDatabase(getActivity()).openReadOnly();

    	// If the database is old or missing, the version will be -1
    	int dbVersion = db.getDatabaseVersion();
		if (dbVersion == DatabaseInfo.INVALID_DB_VERSION)  {
			Debug.e("bad db version " + dbVersion);
			showDialog(InstallDbDialog);
		}
		else if (dbVersion != DatabaseInfo.CURRENT_DB_VERSION)  {
			Debug.e("old db version " + dbVersion);
			showDialog(UpgradeDbDialog);
		}

		db.close();

    }

    private class DatabaseWaitTask extends AsyncTask<Void, Void, Void> {

    	private Context context = null;
    	private DialogFragment dialogFragment = null;

    	private Exception exception = null;
    	private ProgressDialog progressDialog = null;

    	public DatabaseWaitTask(Context ctx, DialogFragment dialog)
    	{
    		context = ctx;
    		dialogFragment = dialog;
    	}

    	protected void onPreExecute()
    	{

    		if (dialogFragment != null)
    			if (!getActivity().isFinishing())
    				dialogFragment.dismiss();

    		if (!getActivity().isFinishing())  {
    			progressDialog = new ProgressDialog(context);
    			String installLocStr = WordlistDatabase.dbInstallsOnExternalStorage() ?
						getString(R.string.dictionary_on_external_storage) :
						getString(R.string.dictionary_on_internal_storage);
				String message = String.format(getString(R.string.dictionary_progress_dialog_text), installLocStr);
				progressDialog.setMessage(message);
    			progressDialog.setCancelable(false);
    			progressDialog.show();
    		}

    	}

		@Override
		protected Void doInBackground(Void... params)
		{
			try {
				WordlistDatabase.deleteDatabaseFile(getActivity());
				WordlistDatabase.createDatabaseFile(getActivity());
			}
			catch (Exception e) { exception = e; }
			return null;
		}

		protected void onPostExecute(Void result)
		{

			// Reset the dictionaries to ENABLE in preferences
			// and in the spinners
			SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = prefs.edit();
			int defaultDict = DictionaryType.DICTIONARY_ENABLE.ordinal();
			editor.putInt("dictionary_dict", defaultDict - 1);
			dictSpinner.setSelection(defaultDict - 1);
			editor.putInt("anagrams_dict", defaultDict - 1);
			anagramSpinner.setSelection(defaultDict - 1);
			editor.putInt("wordjudge_dict", defaultDict - 1);
			wjSpinner.setSelection(defaultDict - 1);
			editor.putInt("crosswords_dict", defaultDict - 1);
			crosswordsSpinner.setSelection(defaultDict - 1);
			editor.commit();

			// Dismiss the dialog
			if (progressDialog != null)
				if (!getActivity().isFinishing())
					progressDialog.dismiss();

			// If there was an exception during install,
			// report it
			if (exception != null)
				createDatabaseExceptionDialog(context, exception);

		}

    }

    private void createDatabaseExceptionDialog(Context context, Exception exception)
    {

		StringBuilder builder = new StringBuilder();

		File file = Environment.getDataDirectory();
		builder.append(file.getPath());
		builder.append(" ");
		builder.append(Utils.getFreeSpaceForFile(context, file));
		builder.append("\n");

		file = Environment.getExternalStorageDirectory();
		builder.append(file.getPath());
		builder.append(" ");
		builder.append(Utils.getFreeSpaceForFile(context, file));
		builder.append("\n");

		builder.append("Installs on " +
						(WordlistDatabase.dbInstallsOnExternalStorage() ?
								getString(R.string.dictionary_on_external_storage) :
									getString(R.string.dictionary_on_internal_storage)) + " storage");
		builder.append("\n");

		if (!getActivity().isFinishing())
			new AppErrDialog(context, exception, builder.toString()).show();

    }

    //
    // Notification Bar Icon Support
    //

    public void addRestartNotification(Intent startIntent)
    {

    	int icon = 0;
    	Activity activity = getActivity();
    	NotificationManager manager = (NotificationManager)activity.getSystemService(Context.NOTIFICATION_SERVICE);

    	// Get the notification parameters
    	if (WordPlayApp.getInstance().isFreeMode())
			icon = R.drawable.ic_launcher_wordplay_assistant_free;
		else
			icon = R.drawable.ic_launcher_wordplay_assistant;
		String tickerText = getString(R.string.notify_ticker_text);
		String title = getString(R.string.app_name);
		String content = getString(R.string.notify_description);

		// Set the flags required to restart the app in the Intent we
		// received
		startIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

		// Create the PendingIntent that is fired when the notification is selected
		// by the user
		PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, startIntent, 0);

		// Create the notification
		Notification notification = new Notification(icon, tickerText, System.currentTimeMillis());
		notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;   
		notification.setLatestEventInfo(activity, title, content, pendingIntent);

		// Give it to the notification manager for display
		manager.notify(RestartNotificationId, notification);

    }

    private void removeNotification()
    {
    	NotificationManager manager = (NotificationManager)getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
    	manager.cancel(RestartNotificationId);
    }

    //
    // Word Judge & Adapter
    //

	public void setWordJudgeObject(JudgeSearch o) { wjSearchObj = o; }
	
	public WordJudgeAdapter getWordJudgeAdapter() { return wjAdapter; }

	public void updateJudgeHistoryAdapter()
	{
		wjAdapter = new WordJudgeAdapter(getActivity(), R.layout.judge_history, JudgeHistory.getInstance().getJudgeHistory());
        wjListview.setAdapter(wjAdapter);	
	}

	public class WordJudgeAdapter extends ArrayAdapter<JudgeHistoryObject> {

		private LinkedList<JudgeHistoryObject> history;
		
		WordJudgeAdapter(Context ctx, int rowLayoutId, LinkedList<JudgeHistoryObject> items)
		{
			super(ctx, rowLayoutId, items);
			this.history = items;
		}

		public void updateHistory() { history = JudgeHistory.getInstance().getJudgeHistory(); }
		
        public View getView(int position, View convertView, ViewGroup parent)
        {
        	
            View v = convertView;

            if (v == null)  {
                LayoutInflater vi = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.judge_history, null);
            }

            JudgeHistoryObject item = history.get(position);
            if (item != null)  {

            	ImageView imageView = (ImageView)v.findViewById(R.id.jh_state_image);
            	if (imageView != null)
            		if (item.getState())
            			imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_listitem_thumbsup));
            		else
            			imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_listitem_thumbsdown));

            	TextView wordView = (TextView)v.findViewById(R.id.jh_word);
            	if (wordView != null)  {
            		if (item.getState())
            			wordView.setTextColor(Color.GREEN);
            		else
            			wordView.setTextColor(Color.RED);
            		wordView.setText(item.getWord());

            	}

            }
            
            return v;
            
        }
		
	}

    public void startJudgeHistorySearch(int position)
    {

//    	if (WordPlayApp.getInstance().isFreeMode())
//    		position -= 1;
//
//    	Intent intent = null;
//    	JudgeHistoryObject elem = JudgeHistory.getInstance().getJudgeHistory().get(position);
//
//    	if (elem.getState())  {
//    		if (elem.getWord().contains(","))
//    			return;
//			intent = new Intent(getActivity(), SearchResultActivity.class);
//			intent.putExtra("SearchString", elem.getWord());
//			intent.putExtra("SearchType", SearchType.OPTION_DICTIONARY_EXACT_MATCH);
//			intent.putExtra("Dictionary", DictionaryType.DICTIONARY_DICT_DOT_ORG.ordinal());
//			try {
//				if (!getActivity().isFinishing())
//					startActivity(intent);
//			}
//			catch (Exception e) {}
//    	}
//    	else
//    		Toast.makeText(getActivity(), "Cannot search for unknown words", Toast.LENGTH_SHORT).show();
    	
    }

	//
	// History
	//

	public void clearHistory(boolean doToast)
	{
		
		SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("history", "");
		editor.commit();
		
		History.getInstance().clearHistory(getActivity());

		Debug.v("clearHistory: history cleared");
		if (doToast)
			Toast.makeText(getActivity(), "History Cleared", Toast.LENGTH_SHORT).show();
		
	}

	private void clearJudgeHistory(boolean doToast)
	{

		SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("wordjudge_history", "");
		editor.commit();
		
		JudgeHistory.getInstance().clearJudgeHistory(getActivity());

		if (doToast)
			Toast.makeText(getActivity(), "History Cleared", Toast.LENGTH_SHORT).show();
		
	}

	private void saveHistory()
	{

		Activity activity = getActivity();
		SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();

		History.getInstance().saveHistory(getActivity());
		JudgeHistory.getInstance().saveJudgeHistory(getActivity());

		// Save versioning information
		editor.putInt("history_version", Constants.CurrentHistoryVersion);
		Debug.v("SAVE HISTORY_VERSION = " + Constants.CurrentHistoryVersion);
		editor.putInt("application_version", WordPlayApp.getAppManifestVersion());
		Debug.v("SAVE APPLICATION_VERSION = " + WordPlayApp.getAppManifestVersion());
		
		// Save the search count
		editor.putInt("search_count", searchCount);
		Debug.v("SAVE SEARCH_COUNT = " + searchCount);

		// Save the "nag" flag
		editor.putBoolean("has_nagged", hasNagged);
		Debug.v("SAVE HAS_NAGGED = " + hasNagged);

		// Save the user's dictionary selections
		editor.putInt("dictionary_dict", dictSpinner.getSelectedItemPosition());
		Debug.v("SAVE DICTIONARY_DICT = " + dictSpinner.getSelectedItemPosition());
		editor.putInt("anagrams_dict", anagramSpinner.getSelectedItemPosition());
		Debug.v("SAVE ANAGRAMS_DICT = " + anagramSpinner.getSelectedItemPosition());
		editor.putInt("wordjudge_dict", wjSpinner.getSelectedItemPosition());
		Debug.v("SAVE WORDJUDGE_DICT = " + wjSpinner.getSelectedItemPosition());
		editor.putInt("crosswords_dict", crosswordsSpinner.getSelectedItemPosition());
		Debug.v("SAVE CROSSWORDS_DICT = " + crosswordsSpinner.getSelectedItemPosition());

		// Save the user's score/sort options
		editor.putInt("dictionary_score", dictScoreToggle.getState());
		Debug.v("SAVE DICTIONARY_SCORE = " + dictScoreToggle.getState());
		editor.putInt("dictionary_sort", dictSortToggle.getState());
		Debug.v("SAVE DICTIONARY_SORT = " + dictSortToggle.getState());
		editor.putInt("anagrams_score", anagramScoreToggle.getState());
		Debug.v("SAVE ANAGRAMS_SCORE = " + anagramScoreToggle.getState());
		editor.putInt("anagrams_sort", anagramSortToggle.getState());
		Debug.v("SAVE ANAGRAMS_SORT = " + anagramSortToggle.getState());

		String str;

		// Save what the user currently has typed into each of the text
		// boxes
		str = ((EditText)activity.findViewById(R.id.DictionaryText)).getText().toString();
		editor.putString("dictionary_text", (str == null ? "" : str));
		Debug.v("SAVE DICTIONARY_TEXT = " + (str == null ? "" : str));
		str = ((EditText)activity.findViewById(R.id.AnagramsTrayText)).getText().toString();
		editor.putString("anagrams_tray_text", (str == null ? "" : str));
		Debug.v("SAVE ANAGRAMS_TRAY_TEXT = " + (str == null ? "" : str));
		str = ((EditText)activity.findViewById(R.id.AnagramsBoardText)).getText().toString();
		editor.putString("anagrams_board_text", (str == null ? "" : str));
		Debug.v("SAVE ANAGRAMS_BOARD_TEXT = " + (str == null ? "" : str));
		str = ((EditText)activity.findViewById(R.id.CrosswordsText)).getText().toString();
		editor.putString("crosswords_text", (str == null ? "" : str));
		Debug.v("SAVE CROSSWORDS_TEXT = " + (str == null ? "" : str));

		editor.commit();

	}

	private void loadHistory()
	{

		int historyVersion;
		int lastAppVersion = 0;
		SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
		
		// Anything prior to VERSION 3 is now deprecated.  If we find this,
		// destroy all of the preferences from those versions.
		historyVersion = prefs.getInt("history_version", Constants.CurrentHistoryVersion);
		if (historyVersion != Constants.CurrentHistoryVersion)
			deleteAllHistory();
		loadHistory_v4();

		// Detect new version installation and do any needed maintenance
		lastAppVersion = prefs.getInt("application_version", 0);
		Debug.v("APPLICATION_VERSION = " + lastAppVersion);
		if (lastAppVersion != WordPlayApp.getAppManifestVersion())  {
			
			// Reset the nag counter and flag so the customer gets nagged anew.
			Debug.v("RESET NAG COUNT/FLAG");
			hasNagged = false;
			searchCount = 0;

			// Reset the flag in the preferences that tells us to show the
			// free dialog
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean("free_dialog_shown", false);
			editor.commit();

		}

	}

	private void loadHistory_v4()
	{

		Activity activity = getActivity();
		SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);
		boolean buttonState;
		
		// Get the search count and nag flag set by the count
		searchCount = prefs.getInt("search_count", 0);
		Debug.v("LOAD SEARCH_COUNT = " + searchCount);
		hasNagged = prefs.getBoolean("has_nagged", false);
		Debug.v("LOAD HAS_NAGGED = " + hasNagged);

		// Get the dictionary settings for each of the tabs
		int dictionary_dict =
			prefs.getInt("dictionary_dict", DictionaryType.DICTIONARY_ENABLE.ordinal() - 1);
		dictSpinner.setSelection(dictionary_dict);
		Debug.v("LOAD DICTIONARY_DICT = " + dictionary_dict);
		int anagrams_dict =
			prefs.getInt("anagrams_dict", DictionaryType.DICTIONARY_ENABLE.ordinal() - 1);
		anagramSpinner.setSelection(anagrams_dict);
		Debug.v("LOAD ANAGRAMS_DICT = " + anagrams_dict);
		int wordjudge_dict =
			prefs.getInt("wordjudge_dict", DictionaryType.DICTIONARY_ENABLE.ordinal() - 1);
		wjSpinner.setSelection(wordjudge_dict);
		Debug.v("LOAD WORDJUDGE_DICT = " + wordjudge_dict);
		int crosswords_dict =
			prefs.getInt("crosswords_dict", DictionaryType.DICTIONARY_ENABLE.ordinal() - 1);
		crosswordsSpinner.setSelection(crosswords_dict);
		Debug.v("LOAD CROSSWORDS_DICT = " + crosswords_dict);
		
		// Set the dictionary tab to either "Dictionary" or "Thesaurus" mode
		setDictionaryTabMode();
		
		// Get and set the dictionary tab's score and sort button states
		int dictionary_score =
			prefs.getInt("dictionary_score", WordScoreState.WORD_SCORE_STATE_ON.ordinal() - 1);
		Debug.v("LOAD DICTIONARY_SCORE = " + dictionary_score);
		dictScoreToggle.setState(dictionary_score);
		int dictionary_sort =
			prefs.getInt("dictionary_sort", WordSortState.WORD_SORT_BY_WORD_SCORE.ordinal() - 1);
		Debug.v("LOAD DICTIONARY_SORT = " + dictionary_sort);
		dictSortToggle.setState(dictionary_sort);

		// Turn off the "sort by score" option if word score state is off
		WordScoreState dictionary_tmp_score =
			WordScoreState.fromInt(dictScoreToggle.getState() + 1);
		buttonState = true;
		if (dictionary_tmp_score == WordScoreState.WORD_SCORE_STATE_OFF)
			buttonState = false;
		dictSortToggle.setButtonState(
									WordSortState.WORD_SORT_BY_WORD_SCORE.ordinal() - 1,
									buttonState);

		// Get and set the anagram tab's score and sort button states
		int anagrams_score =
			prefs.getInt("anagrams_score", WordScoreState.WORD_SCORE_STATE_ON.ordinal() - 1);
		Debug.v("LOAD ANAGRAMS_SCORE = " + anagrams_score);
		anagramScoreToggle.setState(anagrams_score);

		int anagrams_sort =
				prefs.getInt("anagrams_sort", WordSortState.WORD_SORT_BY_WORD_SCORE.ordinal() - 1);
		Debug.v("LOAD ANAGRAMS_SORT = " + anagrams_sort);	
		anagramSortToggle.setState(anagrams_sort);

		// Turn off the "sort by score" option if word score state is off
		WordScoreState anagrams_tmp_score =
			WordScoreState.fromInt(anagramScoreToggle.getState() + 1);
		buttonState = true;
		if (anagrams_tmp_score == WordScoreState.WORD_SCORE_STATE_OFF)
			buttonState = false;
		anagramSortToggle.setButtonState(
									WordSortState.WORD_SORT_BY_WORD_SCORE.ordinal() - 1,
									buttonState);

		String str;

		// Restore what the user had previously entered into the text entries
		str = prefs.getString("dictionary_text", "");
		Debug.v("LOAD DICTIONARY_TEXT = " + str);
		((EditText)activity.findViewById(R.id.DictionaryText)).setText(str);
		str = prefs.getString("anagrams_tray_text", "");
		Debug.v("LOAD ANAGRAMS_TRAY_TEXT = " + str);
		((EditText)activity.findViewById(R.id.AnagramsTrayText)).setText(str);
		str = prefs.getString("anagrams_board_text", "");
		Debug.v("LOAD ANAGRAMS_BOARD_TEXT = " + str);
		((EditText)activity.findViewById(R.id.AnagramsBoardText)).setText(str);
		str = prefs.getString("crosswords_text", "");
		((EditText)activity.findViewById(R.id.CrosswordsText)).setText(str);

		// Read in the regular history
		History.getInstance().loadHistory(getActivity());

		// Read in the WordJudge history
		JudgeHistory.getInstance().loadJudgeHistory(getActivity());		
		wjAdapter.updateHistory();
		updateJudgeHistoryAdapter();
		
	}

	private void deleteAllHistory()
	{

		SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();

		Debug.v("deleteOldHistory: deleting all preferences");
		editor.clear();
		editor.commit();

	}

	//
	// Miscellaneous Support
	//

    private void setMarketButton(final AlertDialog dialog, Button button, boolean freeModeOnly)
    {
    	if (WordPlayApp.getInstance().isPaidMode() && freeModeOnly)
    		button.setVisibility(View.GONE);
    	else
    		button.setOnClickListener(new View.OnClickListener()  {
				public void onClick(View v)
				{
					Intent myIntent =
						new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.MarketPaidWebAddress));
					if (!getActivity().isFinishing())  {
						dialog.dismiss();
						try {
							if (!getActivity().isFinishing())
								startActivity(myIntent);
						}
						catch (Exception e) {}
					}
				}
    		});
    }

    private void freeDialogCheck()
    {

		SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);

		// Have we show the dialog already?  It is shown only on the
		// very first run.
		boolean hasShown = prefs.getBoolean("free_dialog_shown", false);
		if (!hasShown)  {

			// Update the preferences to mark that its been shown
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean("free_dialog_shown", true);
			editor.commit();

			// Show the dialog
			showDialog(FreeDialog);

		}
		else
			createDatabaseIfMissing();

    }

    private boolean validateString(String searchString, DictionaryType dictionary, boolean isAnagram)
    {

		int wildcardCount = 0;

		if (!isAnagram && (searchString.length() < Constants.MinAnagramLength))  {
			AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
			alertDialog.setTitle("Error");
			alertDialog.setMessage("Please enter more than one letter.");
			alertDialog.setButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {}
			});
			alertDialog.show();
			return false;
		}

		if (isAnagram &&
				(searchString.length() < Constants.MinAnagramLength) ||
				(searchString.length() > Constants.MaxAnagramLength))  {
			AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
			alertDialog.setTitle("Error");
			alertDialog.setMessage("There must be more than 1 and not more than 20 letters total between the tray and board letters.");
			alertDialog.setButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {}
			});
			alertDialog.show();
			return false;
		}

		if (dictionary.isScrabbleDict())  {
			for (int i = 0; i < searchString.length(); i += 1)
				if ((searchString.charAt(i) == '.') || (searchString.charAt(i) == '?'))
					wildcardCount += 1;
			if (wildcardCount > 1)  {
				AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
				alertDialog.setTitle("Error");
				alertDialog.setMessage("Only one wildcard character allowed using the current dictionary.");
				alertDialog.setButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {}
				});
				alertDialog.show();
				return false;
			}
		}
		else {
			if (searchString.contains(".") || searchString.contains("?"))  {
				AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
				alertDialog.setTitle("Error");
				alertDialog.setMessage("No wildcards are allowed when finding anagrams with the current dictionary.");
				alertDialog.setButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {}
				});
				alertDialog.show();
				return false;
			}
		}
		
		return true;
		
    }
   
    private void setDictionaryTabMode()
    {

    	if (WordPlayApp.getInstance().isFreeMode())
    		return;

    	ViewGroup buttonArea = (ViewGroup)getActivity().findViewById(R.id.button_area);
    	Button button = (Button)buttonArea.getChildAt(DictionaryTab);
		EditText textEntry = (EditText)getActivity().findViewById(R.id.DictionaryText);
		
		int dictionary = (int)dictSpinner.getSelectedItemId() + 1;
		if (dictionary == DictionaryType.DICTIONARY_THESAURUS.ordinal())  {
			button.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_tab_thesaurus, 0, 0);
			button.setText("Thesaurus");
			textEntry.setHint(R.string.thesaurus_edit_hint);
		}
		else {
			button.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_tab_dictionary, 0, 0);
			button.setText("Dictionary");
			textEntry.setHint(R.string.dictionary_edit_hint);
		}
    	
    }

}
