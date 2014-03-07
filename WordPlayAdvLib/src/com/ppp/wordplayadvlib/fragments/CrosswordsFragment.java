package com.ppp.wordplayadvlib.fragments;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.appdata.DictionaryType;
import com.ppp.wordplayadvlib.appdata.SearchType;
import com.ppp.wordplayadvlib.appdata.WordScoreState;
import com.ppp.wordplayadvlib.appdata.WordSortState;

import android.content.Context;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;

public class CrosswordsFragment extends BaseFragment implements View.OnClickListener {

	private RelativeLayout rootView;
	private Button crosswordsButton = null;
	private EditText crosswordsText = null;
	private Spinner crosswordsSpinner = null;

	//
	// Activity Methods
	//

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		rootView = (RelativeLayout)inflater.inflate(R.layout.crosswords_fragment, container, false);
		setupCrosswordsTab();
		return rootView;
	}

	@Override
    public void onClick(View v)
    {
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
    	startSearchFragment(v);
    }

    //
    // UI Setup
    //

	private void setupCrosswordsTab()
	{

		crosswordsButton = (Button)rootView.findViewById(R.id.CrosswordsButton);
        crosswordsButton.setOnClickListener(this);

        crosswordsText = (EditText)rootView.findViewById(R.id.CrosswordsText);
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

        final Button crosswordsClearButton = (Button)rootView.findViewById(R.id.CrosswordsTextClear);
        crosswordsClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) { crosswordsText.setText(""); }
		});

    	crosswordsSpinner = (Spinner)rootView.findViewById(R.id.crosswords_dict_spinner);
    	crosswordsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
    		public void onItemSelected(AdapterView<?> parent, View view, int i, long l) {}
    		public void onNothingSelected(AdapterView<?> parent) {}
		});

	}

	//
	// Search Activity Support
	//

    private void startSearchFragment(View v)
    {

    	String searchString = "";
    	String boardString = "";
    	WordScoreState wordScores = WordScoreState.WORD_SCORE_UNKNOWN;
    	WordSortState wordSort = WordSortState.WORD_SORT_UNKNOWN;
    	DictionaryType dictionary = DictionaryType.DICTIONARY_UNKNOWN;

		searchString = crosswordsText.getText().toString();
		dictionary = DictionaryType.fromInt((int)crosswordsSpinner.getSelectedItemId() + 1);
		wordScores = WordScoreState.WORD_SCORE_STATE_OFF;
		wordSort = WordSortState.WORD_SORT_BY_ALPHA;

		Bundle args = new Bundle();
		args.putInt("SearchType", SearchType.OPTION_CROSSWORDS.ordinal());
		args.putString("SearchString", searchString);
		args.putString("BoardString", boardString);
		args.putInt("Dictionary", dictionary.ordinal());
		args.putInt("WordScores", wordScores.ordinal());
		args.putInt("WordSort", wordSort.ordinal());

		BaseFragment fragment = new SearchFragment();
		fragment.setArguments(args);
		pushToStack(fragment);

    }

}
