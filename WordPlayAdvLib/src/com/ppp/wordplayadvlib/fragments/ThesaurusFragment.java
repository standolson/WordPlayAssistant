package com.ppp.wordplayadvlib.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.appdata.DictionaryType;
import com.ppp.wordplayadvlib.appdata.SearchType;
import com.ppp.wordplayadvlib.appdata.WordScoreState;
import com.ppp.wordplayadvlib.appdata.WordSortState;
import com.ppp.wordplayadvlib.utils.Debug;

public class ThesaurusFragment extends BaseFragment implements View.OnClickListener {

	private View rootView;
	private Button thesaurusButton = null;

	//
	// Activity Methods
	//

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		rootView = inflater.inflate(R.layout.thesaurus_fragment, container, false);
		setupThesaurusTab();
		return rootView;
	}

	@Override
	public void onResume()
	{
		super.onResume();
		setButtonState();
	}

	@Override
    public void onClick(View v)
    {
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
    	startSearchFragment(v);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
    	MenuItem item = menu.findItem(R.id.dictionary_menu);
    	if (item != null)
    		item.setVisible(false);
    }

    //
    // UI Setup
    //

	private void setupThesaurusTab()
	{

		SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);

        thesaurusButton = (Button)rootView.findViewById(R.id.DictionaryButton);
        thesaurusButton.setOnClickListener(this);
        		 
        final EditText dictText = (EditText)rootView.findViewById(R.id.DictionaryText);
        String thesaurusStr = prefs.getString("thesaurusStr", "");
        Debug.v("LOAD thesaurusStr = " + thesaurusStr);
        dictText.setText(thesaurusStr);
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
		dictText.addTextChangedListener(buttonTextWatcher);

        final Button dictClearButton = (Button)rootView.findViewById(R.id.DictionaryTextClear);
        dictClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) { dictText.setText(""); }
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
    	DictionaryType dictionary = DictionaryType.DICTIONARY_THESAURUS;

		final EditText dictText = (EditText)rootView.findViewById(R.id.DictionaryText);

		searchString = dictText.getText().toString();
		dictionary = DictionaryType.DICTIONARY_DICT_DOT_ORG;
		wordScores = WordScoreState.WORD_SCORE_UNKNOWN;
		wordSort = WordSortState.WORD_SORT_UNKNOWN;

		// Save state
		editor.putString("thesaurusStr", (searchString == null) ? "" : searchString);
		Debug.v("SAVE thesaurusStr = " + searchString);
		editor.commit();

		Bundle args = new Bundle();
		args.putInt("SearchType", SearchType.OPTION_THESAURUS.ordinal());
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

		thesaurusButton.setEnabled(validateString(searchString, DictionaryType.DICTIONARY_THESAURUS, false, false));
   	
    }

}
