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
import com.ppp.wordplayadvlib.model.DictionaryType;
import com.ppp.wordplayadvlib.model.SearchType;
import com.ppp.wordplayadvlib.model.WordScoreState;
import com.ppp.wordplayadvlib.model.WordSortState;
import com.ppp.wordplayadvlib.utils.Debug;

public class CrosswordsFragment extends BaseFragment
	implements
		View.OnClickListener
{

	private View rootView;
	private Button crosswordsButton = null;
	private EditText crosswordsText = null;

	//
	// Activity Methods
	//

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		rootView = inflater.inflate(R.layout.crosswords_fragment, container, false);
		setupCrosswordsTab();
		return rootView;
	}

	@Override
	public void onResume()
	{
		super.onResume();
		Analytics.screenView(Analytics.CROSSWORDS_SCREEN);
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
    	int crosswordsDict = prefs.getInt("crosswordsDict", DictionaryType.DICTIONARY_ENABLE.ordinal());
    	return crosswordsDict - 1;
	}

	@Override
	public void onSelection(int selection)
	{

		SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		DictionaryType dict = DictionaryType.fromInt(selection + 1);

		editor.putInt("crosswordsDict", dict.ordinal());
		Debug.v("SAVE crosswordsDict = " + dict.ordinal());
		editor.commit();

	}

    //
    // UI Setup
    //

	private void setupCrosswordsTab()
	{

		SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);

		crosswordsButton = (Button)rootView.findViewById(R.id.CrosswordsButton);
        crosswordsButton.setOnClickListener(this);

        crosswordsText = (EditText)rootView.findViewById(R.id.CrosswordsText);
        String crosswordsStr = prefs.getString("crosswordsStr", "");
        Debug.v("LOAD crosswordsStr = " + crosswordsStr);
        crosswordsText.setText(crosswordsStr);
        crosswordsText.setFilters(new InputFilter[] { searchFilter });
        crosswordsText.setOnKeyListener(new OnKeyListener() {
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
        crosswordsText.addTextChangedListener(buttonTextWatcher);

        final Button crosswordsClearButton = (Button)rootView.findViewById(R.id.CrosswordsTextClear);
        crosswordsClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) { crosswordsText.setText(""); }
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

		searchString = crosswordsText.getText().toString().toLowerCase();
		dictionary = DictionaryType.fromInt(getSelectedDictionary() + 1);
		wordScores = WordScoreState.WORD_SCORE_STATE_OFF;
		wordSort = WordSortState.WORD_SORT_BY_ALPHA;

		// Save state
		editor.putString("crosswordsStr", (searchString == null) ? "" : searchString);
		editor.commit();

		Bundle args = new Bundle();
		args.putInt("SearchType", SearchType.OPTION_CROSSWORDS.ordinal());
		args.putString("SearchString", searchString);
		args.putString("BoardString", boardString);
		args.putInt("Dictionary", dictionary.ordinal());
		args.putInt("WordScores", wordScores.ordinal());
		args.putInt("WordSort", wordSort.ordinal());
		args.putBoolean("isTopLevelSearch", true);

		BaseFragment fragment = new SearchFragment();
		fragment.setArguments(args);
		pushToStack(fragment);

    }

    @Override
    protected void setButtonState()
    {

		String searchString = crosswordsText.getText().toString();
		DictionaryType dictionary = DictionaryType.fromInt(getSelectedDictionary() + 1);

		crosswordsButton.setEnabled(validateString(searchString, dictionary, false, true));
    	
    }

}
