package com.ppp.wordplayadvlib.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.appdata.DictionaryType;
import com.ppp.wordplayadvlib.appdata.SearchType;
import com.ppp.wordplayadvlib.appdata.WordScoreState;
import com.ppp.wordplayadvlib.appdata.WordSortState;
import com.ppp.wordplayadvlib.widgets.MultiStateButton;

public class AnagramsFragment extends BaseFragment implements View.OnClickListener {

	private RelativeLayout rootView;
	private Button anagramButton = null;
	private MultiStateButton anagramScoreToggle = null;
	private MultiStateButton anagramSortToggle = null;
	private Spinner anagramSpinner = null;

	//
	// Activity Methods
	//

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{

		rootView = (RelativeLayout)inflater.inflate(R.layout.anagrams_fragment, container, false);

		setupAnagramTab();

		return rootView;

	}

	@Override
	public void onResume()
	{
		super.onResume();
		setActionBarTitle(getString(R.string.Anagrams));
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

	private void setupAnagramTab()
	{

        anagramButton = (Button)rootView.findViewById(R.id.AnagramsButton);
        anagramButton.setOnClickListener(this);

        final EditText anagramsTrayText = (EditText)rootView.findViewById(R.id.AnagramsTrayText);
        anagramsTrayText.setFilters(new InputFilter[] { searchFilter });
        anagramsTrayText.setOnKeyListener(new OnKeyListener() {
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

        final EditText anagramsBoardText = (EditText)rootView.findViewById(R.id.AnagramsBoardText);
        anagramsBoardText.setFilters(new InputFilter[] { alphaFilter });
        anagramsBoardText.setOnKeyListener(new OnKeyListener() {
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

        final Button anagramsTrayClearButton = (Button)rootView.findViewById(R.id.AnagramsTrayTextClear);
        anagramsTrayClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) { anagramsTrayText.setText(""); }
        });

        final Button anagramsBoardClearButton = (Button)rootView.findViewById(R.id.AnagramsBoardTextClear);
        anagramsBoardClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) { anagramsBoardText.setText(""); }
		});

    	anagramSortToggle = (MultiStateButton)rootView.findViewById(R.id.AnagramsSortOrder);
    	anagramSortToggle.setStateNames(getResources().getStringArray(R.array.sort_order_toggle_states));
 
    	anagramScoreToggle = (MultiStateButton)rootView.findViewById(R.id.AnagramsWordScores);
    	anagramScoreToggle.setStateNames(getResources().getStringArray(R.array.word_score_toggle_states));
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

    	anagramSpinner = (Spinner)rootView.findViewById(R.id.anagrams_dict_spinner);
    	anagramSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
    	SearchType searchType = SearchType.OPTION_UNKNOWN;

		final EditText anagramsTrayText = (EditText)rootView.findViewById(R.id.AnagramsTrayText);
		final EditText anagramsBoardText = (EditText)rootView.findViewById(R.id.AnagramsBoardText);

		searchType = SearchType.OPTION_ANAGRAMS;
		searchString = anagramsTrayText.getText().toString();
		boardString = anagramsBoardText.getText().toString();
		dictionary = DictionaryType.fromInt((int)anagramSpinner.getSelectedItemId() + 1);
		wordScores = WordScoreState.fromInt(anagramScoreToggle.getState() + 1);
		wordSort = WordSortState.fromInt(anagramSortToggle.getState() + 1);

		if (!validateString(searchString + boardString, dictionary, true))
			return;

		startSearchActivity(searchType,
							searchString,
							boardString,
							dictionary,
							wordScores,
							wordSort);

    }

}
