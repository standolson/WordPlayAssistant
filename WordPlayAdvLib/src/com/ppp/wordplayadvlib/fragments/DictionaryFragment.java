package com.ppp.wordplayadvlib.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.analytics.Analytics;
import com.ppp.wordplayadvlib.appdata.DictionaryType;
import com.ppp.wordplayadvlib.appdata.SearchType;
import com.ppp.wordplayadvlib.appdata.WordScoreState;
import com.ppp.wordplayadvlib.appdata.WordSortState;
import com.ppp.wordplayadvlib.utils.Debug;
import com.ppp.wordplayadvlib.widgets.MultiStateButton;

public class DictionaryFragment extends BaseFragment
	implements
		View.OnClickListener
{

	private View rootView;
	private Button dictButton = null;
	private MultiStateButton searchTypeToggle = null;
	private MultiStateButton dictScoreToggle = null;
	private MultiStateButton dictSortToggle = null;

	//
	// Activity Methods
	//

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		rootView = inflater.inflate(R.layout.dictionary_fragment, container, false);
		setupDictionaryTab();
		return rootView;
	}

	@Override
	public void onResume()
	{
		super.onResume();
		Analytics.screenView(Analytics.DICTIONARY_SCREEN);
		setButtonState();
	}

	@Override
    public void onClick(View v)
    {
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
    	startSearchFragment(v);
    }

	//
	// Menu Support
	//

	@Override
	public String[] getDictionaryNames()
	{
		return getResources().getStringArray(R.array.dictionary_names);
	}

	@Override
	public int getSelectedDictionary()
	{
		SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
    	int dictionaryDict = prefs.getInt("dictionaryDict", DictionaryType.DICTIONARY_ENABLE.ordinal());
    	return dictionaryDict - 1;
	}

	@Override
	public void onSelection(int selection)
	{

		SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		DictionaryType dict = DictionaryType.fromInt(selection + 1);

		int visible =
			dict != DictionaryType.DICTIONARY_DICT_DOT_ORG ? View.VISIBLE : View.GONE;
		rootView.findViewById(R.id.DictionaryToggleButtons).setVisibility(visible);

		editor.putInt("dictionaryDict", dict.ordinal());
		Debug.v("SAVE dictionaryDict = " + dict.ordinal());
		editor.commit();

	}

    //
    // UI Setup
    //

	private void setupDictionaryTab()
	{

		SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);

        dictButton = (Button)rootView.findViewById(R.id.DictionaryButton);
        dictButton.setOnClickListener(this);

        final EditText dictText = (EditText)rootView.findViewById(R.id.DictionaryText);
        String dictStr = prefs.getString("dictStr", "");
		Debug.v("LOAD dictStr = " + dictStr);
        dictText.setText(dictStr);
        dictText.setFilters(new InputFilter[] { alphaFilter });
        dictText.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if (checkForEnterKey(v, keyCode, event))  {
					startSearchFragment(v);
					return true;
				}
				return false;
			}
        });
        dictText.addTextChangedListener(buttonTextWatcher);

        final Button dictClearButton = (Button)rootView.findViewById(R.id.DictionaryTextClear);
        dictClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) { dictText.setText(""); }
		});

        searchTypeToggle = (MultiStateButton)rootView.findViewById(R.id.SearchTypeButton);
        searchTypeToggle.setStateNames(getResources().getStringArray(R.array.search_types));
        int searchType =
        	prefs.getInt("searchType", SearchType.OPTION_DICTIONARY_EXACT_MATCH.ordinal());
        Debug.v("LOAD searchType = " + searchType);
        searchTypeToggle.setState(searchType);

        dictScoreToggle = (MultiStateButton)rootView.findViewById(R.id.WordScoreButton);
        dictScoreToggle.setStateNames(getResources().getStringArray(R.array.word_score_toggle_states));
		int dictScore =
			prefs.getInt("dictScore", WordScoreState.WORD_SCORE_STATE_ON.ordinal() - 1);
		Debug.v("LOAD dictScore = " + dictScore);
		dictScoreToggle.setState(dictScore);

        dictSortToggle = (MultiStateButton)rootView.findViewById(R.id.SortOrderButton);
        dictSortToggle.setStateNames(getResources().getStringArray(R.array.sort_order_toggle_states));
		int dictSort =
			prefs.getInt("dictSort", WordSortState.WORD_SORT_BY_WORD_SCORE.ordinal() - 1);
		Debug.v("LOAD dictSort = " + dictSort);
		dictSortToggle.setState(dictSort);
        dictScoreToggle.setOnChangeListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		MultiStateButton button = (MultiStateButton)v;
        		WordScoreState score = WordScoreState.fromInt(button.getState() + 1);
        		boolean button_state = true;
        		if (score == WordScoreState.WORD_SCORE_STATE_OFF)
        			button_state = false;
        		dictSortToggle.setButtonState(WordSortState.WORD_SORT_BY_WORD_SCORE.ordinal() - 1, button_state);
        	}
        });

	}

	//
	// Search Activity Support
	//

    private void startSearchFragment(View v)
    {

		SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();

    	String searchString = "";
    	String boardString = "";
    	WordScoreState wordScores = WordScoreState.WORD_SCORE_UNKNOWN;
    	WordSortState wordSort = WordSortState.WORD_SORT_UNKNOWN;
    	DictionaryType dictionary = DictionaryType.DICTIONARY_UNKNOWN;
    	SearchType searchType = SearchType.OPTION_UNKNOWN;

		final EditText dictText = (EditText)rootView.findViewById(R.id.DictionaryText);

		searchType = SearchType.fromInt(searchTypeToggle.getState());
		searchString = dictText.getText().toString();
		dictionary = DictionaryType.fromInt(getSelectedDictionary() + 1);
		if (dictionary.isScrabbleDict())  {
    		wordScores = WordScoreState.fromInt(dictScoreToggle.getState() + 1);
    		wordSort = WordSortState.fromInt(dictSortToggle.getState() + 1);
		}
		else {
			wordScores = WordScoreState.WORD_SCORE_UNKNOWN;
			wordSort = WordSortState.WORD_SORT_UNKNOWN;
		}

		// Save state
		editor.putString("dictStr", (searchString == null) ? "" : searchString);
		Debug.v("SAVE dictStr " + searchString);
		editor.putInt("dictScore", dictScoreToggle.getState());
		Debug.v("SAVE searchType " + searchType);
		editor.putInt("searchType", searchTypeToggle.getState());
		Debug.v("SAVE dictScore = " + dictScoreToggle.getState());
		editor.putInt("dictSort", dictSortToggle.getState());
		Debug.v("SAVE dictSort = " + dictSortToggle.getState());
		editor.commit();

		Bundle args = new Bundle();
		args.putInt("SearchType", searchType.ordinal());
		args.putString("SearchString", searchString);
		args.putString("BoardString", boardString);
		args.putInt("Dictionary", dictionary.ordinal());
		args.putInt("WordScores", wordScores.ordinal());
		args.putInt("WordSort", wordSort.ordinal());

		BaseFragment fragment = new SearchFragment();
		fragment.setArguments(args);
		pushToStack(fragment);

    }

    @Override
    protected void setButtonState()
    {

    	final EditText dictText = (EditText)rootView.findViewById(R.id.DictionaryText);
    	String searchString = dictText.getText().toString();
		DictionaryType dictionary = DictionaryType.fromInt(getSelectedDictionary() + 1);

		dictButton.setEnabled(validateString(searchString, dictionary, false, false));

    }

}
