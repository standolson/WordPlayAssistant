package com.ppp.wordplayadvlib.fragments;

import android.content.Context;
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
import android.widget.RelativeLayout;
import android.widget.Spinner;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.appdata.DictionaryType;
import com.ppp.wordplayadvlib.appdata.SearchType;
import com.ppp.wordplayadvlib.appdata.WordScoreState;
import com.ppp.wordplayadvlib.appdata.WordSortState;
import com.ppp.wordplayadvlib.widgets.MultiStateButton;

public class ThesaurusFragment extends BaseFragment implements View.OnClickListener {

	private RelativeLayout rootView;
	private Button dictButton = null;
	private MultiStateButton dictScoreToggle = null;
	private MultiStateButton dictSortToggle = null;
	private Spinner dictSpinner = null;

	//
	// Activity Methods
	//

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{

		rootView = (RelativeLayout)inflater.inflate(R.layout.dictionary_fragment, container, false);

		setupThesaurusTab();

		return rootView;

	}

	@Override
	public void onResume()
	{
		super.onResume();
		setActionBarTitle(getString(R.string.Thesaurus));
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

	private void setupThesaurusTab()
	{

        dictButton = (Button)rootView.findViewById(R.id.DictionaryButton);
        dictButton.setOnClickListener(this);

        final EditText dictText = (EditText)rootView.findViewById(R.id.DictionaryText);
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
		dictText.setHint(R.string.thesaurus_edit_hint);

        final Button dictClearButton = (Button)rootView.findViewById(R.id.DictionaryTextClear);
        dictClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) { dictText.setText(""); }
		});

        dictScoreToggle = (MultiStateButton)rootView.findViewById(R.id.DictionaryWordScores);
		dictScoreToggle.setVisibility(View.GONE);

        dictSortToggle = (MultiStateButton)rootView.findViewById(R.id.DictionarySortOrder);
		dictSortToggle.setVisibility(View.GONE);
    	
    	dictSpinner = (Spinner)rootView.findViewById(R.id.dictionary_dict_spinner);
		dictSpinner.setVisibility(View.GONE);

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

		final EditText dictText = (EditText)rootView.findViewById(R.id.DictionaryText);

		searchString = dictText.getText().toString();	    
		searchType = SearchType.OPTION_THESAURUS;
		dictionary = DictionaryType.DICTIONARY_DICT_DOT_ORG;
		wordScores = WordScoreState.WORD_SCORE_UNKNOWN;
		wordSort = WordSortState.WORD_SORT_UNKNOWN;

		startSearchActivity(searchType,
							searchString,
							boardString,
							dictionary,
							wordScores,
							wordSort);

    }

}
