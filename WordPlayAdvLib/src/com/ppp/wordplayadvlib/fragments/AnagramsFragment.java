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
import android.widget.RelativeLayout;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.appdata.DictionaryType;
import com.ppp.wordplayadvlib.appdata.SearchType;
import com.ppp.wordplayadvlib.appdata.WordScoreState;
import com.ppp.wordplayadvlib.appdata.WordSortState;
import com.ppp.wordplayadvlib.utils.Debug;
import com.ppp.wordplayadvlib.widgets.MultiStateButton;

public class AnagramsFragment extends BaseFragment
	implements
		View.OnClickListener
{

	private RelativeLayout rootView;
	private Button anagramButton = null;
	private MultiStateButton anagramScoreToggle = null;
	private MultiStateButton anagramSortToggle = null;

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
		setButtonState();
	}

	@Override
    public void onClick(View v)
    {
        InputMethodManager imm =
        	(InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
    	startSearchFragment();
    }

	//
	// Menu Support
	//

	@Override
	public String[] getDictionaryNames()
	{
		return getResources().getStringArray(R.array.word_list_names);
	}

	@Override
	public int getSelectedDictionary()
	{
		SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
		int anagramsDict = prefs.getInt("anagramsDict", DictionaryType.DICTIONARY_ENABLE.ordinal());
		return anagramsDict - 1;
	}

	@Override
	public void onSelection(int selection)
	{

		SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		DictionaryType dict = DictionaryType.fromInt(selection + 1);

		editor.putInt("anagramsDict", dict.ordinal());
		Debug.v("SAVE anagramsDict = " + dict.ordinal());
		editor.commit();

	}

    //
    // UI Setup
    //

	private void setupAnagramTab()
	{

		SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);

        anagramButton = (Button)rootView.findViewById(R.id.AnagramsButton);
        anagramButton.setOnClickListener(this);

        final EditText anagramsTrayText = (EditText)rootView.findViewById(R.id.AnagramsTrayText);
        String anagramsStr = prefs.getString("anagramsStr", "");
        Debug.v("LOAD anagramsStr = " + anagramsStr);
        anagramsTrayText.setText(anagramsStr);
        anagramsTrayText.setFilters(new InputFilter[] { searchFilter });
        anagramsTrayText.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if (checkForEnterKey(v, keyCode, event))  {
					startSearchFragment();
					return true;
				}
				return false;
			}
        });
        anagramsTrayText.addTextChangedListener(buttonTextWatcher);

        final EditText anagramsBoardText = (EditText)rootView.findViewById(R.id.AnagramsBoardText);
        String anagramsBoard = prefs.getString("anagramsBoard", "");
        Debug.v("LOAD anagramsBoard = " + anagramsBoard);
        anagramsBoardText.setText(anagramsBoard);
        anagramsBoardText.setFilters(new InputFilter[] { alphaFilter });
        anagramsBoardText.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if (checkForEnterKey(v, keyCode, event))  {
					startSearchFragment();
					return true;
				}
				return false;
			}
        });
        anagramsBoardText.addTextChangedListener(buttonTextWatcher);

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
 
    	anagramScoreToggle = (MultiStateButton)rootView.findViewById(R.id.WordScoreButton);
    	anagramScoreToggle.setStateNames(getResources().getStringArray(R.array.word_score_toggle_states));
		int anagramsScore =
			prefs.getInt("anagramsScore", WordSortState.WORD_SORT_BY_WORD_SCORE.ordinal() - 1);
		Debug.v("LOAD anagrmasScore = " + anagramsScore);
		anagramScoreToggle.setState(anagramsScore);
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

    	anagramSortToggle = (MultiStateButton)rootView.findViewById(R.id.SortOrderButton);
    	anagramSortToggle.setStateNames(getResources().getStringArray(R.array.sort_order_toggle_states));
		int anagramsSort =
			prefs.getInt("anagramsSort", WordScoreState.WORD_SCORE_STATE_ON.ordinal() - 1);
		Debug.v("LOAD anagramsSort = " + anagramsSort);
		anagramSortToggle.setState(anagramsSort);

	}

	//
	// Search Activity Support
	//

    private void startSearchFragment()
    {

		SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();

    	String searchString = "";
    	String boardString = "";
    	WordScoreState wordScores = WordScoreState.WORD_SCORE_UNKNOWN;
    	WordSortState wordSort = WordSortState.WORD_SORT_UNKNOWN;
    	DictionaryType dictionary = DictionaryType.DICTIONARY_UNKNOWN;

		final EditText anagramsTrayText = (EditText)rootView.findViewById(R.id.AnagramsTrayText);
		final EditText anagramsBoardText = (EditText)rootView.findViewById(R.id.AnagramsBoardText);

		searchString = anagramsTrayText.getText().toString();
		boardString = anagramsBoardText.getText().toString();
		dictionary = DictionaryType.fromInt(getSelectedDictionary() + 1);
		wordScores = WordScoreState.fromInt(anagramScoreToggle.getState() + 1);
		wordSort = WordSortState.fromInt(anagramSortToggle.getState() + 1);

		// Save state
		editor.putString("anagramsStr", (searchString == null) ? "" : searchString);
		Debug.v("SAVE anagramsStr = " + searchString);
		editor.putString("anagramsBoard", (boardString == null) ? "" : boardString);
		Debug.v("SAVE anagramsBoard = " + boardString);
		editor.putInt("anagramsScore", anagramScoreToggle.getState());
		Debug.v("SAVE anagramsScore = " + anagramScoreToggle.getState());
		editor.putInt("anagramsSort", anagramSortToggle.getState());
		Debug.v("SAVE anagramsSort = " + anagramSortToggle.getState());
		editor.commit();

		Bundle args = new Bundle();
		args.putInt("SearchType", SearchType.OPTION_ANAGRAMS.ordinal());
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
 
		final EditText anagramsTrayText = (EditText)rootView.findViewById(R.id.AnagramsTrayText);
		final EditText anagramsBoardText = (EditText)rootView.findViewById(R.id.AnagramsBoardText);
		String searchString = anagramsTrayText.getText().toString();
		String boardString = anagramsBoardText.getText().toString();
		DictionaryType dictionary = DictionaryType.fromInt(getSelectedDictionary() + 1);

		anagramButton.setEnabled(validateString(searchString + boardString, dictionary, true, false));

    }

}
