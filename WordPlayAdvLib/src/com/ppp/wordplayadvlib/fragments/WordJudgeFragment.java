package com.ppp.wordplayadvlib.fragments;

import java.util.LinkedList;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
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
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.AdView;
import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.WordPlayApp;
import com.ppp.wordplayadvlib.appdata.DictionaryType;
import com.ppp.wordplayadvlib.appdata.JudgeHistory;
import com.ppp.wordplayadvlib.appdata.JudgeHistoryObject;
import com.ppp.wordplayadvlib.appdata.JudgeSearch;
import com.ppp.wordplayadvlib.appdata.SearchType;
import com.ppp.wordplayadvlib.utils.Debug;

public class WordJudgeFragment extends BaseFragment implements View.OnClickListener {

	private RelativeLayout rootView;
	private Button wjButton = null;
	private Spinner wjSpinner = null;
	private EditText wjText = null;
	private AdView wordJudgeAdView;
	private static ListView wjListview = null;
	private static WordJudgeAdapter wjAdapter = null;
	private JudgeSearch wjSearchObj = null;

	//
	// Activity Methods
	//

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		rootView = (RelativeLayout)inflater.inflate(R.layout.word_judge_fragment, container, false);
		setupWordJudgeTab();
		return rootView;
	}

	@Override
    public void onClick(View v)
    {
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
    	startWordJudgeSearch();
    }

    //
    // UI Setup
    //

	private void setupWordJudgeTab()
	{

        wjButton = (Button)rootView.findViewById(R.id.WordJudgeButton);
        wjButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) { startWordJudgeSearch(); }
        });

        wjText = (EditText)rootView.findViewById(R.id.WordJudgeText);
        wjText.setFilters(new InputFilter[] { commaFilter });
        wjText.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if (checkForEnterKey(v, keyCode, event))  {
					startWordJudgeSearch();
					return true;
				}
				return false;
			}
        });
		
        wjListview = (ListView)rootView.findViewById(R.id.wordjudge_listview);
        wjListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View v, int position, long id)
            {
        		startJudgeHistorySearch(position);
            }
        });

        final Button wjClearButton = (Button)rootView.findViewById(R.id.WordJudgeTextClear);
        wjClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) { wjText.setText(""); }
		});

    	wjSpinner = (Spinner)rootView.findViewById(R.id.wordjudge_dict_spinner);
    	wjSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
    		public void onItemSelected(AdapterView<?> parent, View view, int i, long l) {}
    		public void onNothingSelected(AdapterView<?> parent) {}
    	});

        if (WordPlayApp.getInstance().isFreeMode())  {
        	LinearLayout headerLayout = (LinearLayout)View.inflate(getActivity(), R.layout.admob_listview_footer, null);
        	wordJudgeAdView = (AdView)headerLayout.findViewById(R.id.listview_ad);
//			wordJudgeAdView.loadAd(new AdRequest());
            wjListview.addHeaderView(headerLayout);
        }

        updateJudgeHistoryAdapter();

	}

	//
	// Search Activity Support
	//

    private void startWordJudgeSearch()
    {

    	String searchString = "";
    	DictionaryType dictionary = DictionaryType.DICTIONARY_UNKNOWN;

		searchString = wjText.getText().toString();
		dictionary = DictionaryType.fromInt((int)wjSpinner.getSelectedItemId() + 1);

		if (!validateString(searchString, dictionary, false))
			return;

		searchCount += 1;
		Debug.v("SEARCH_COUNT " + searchCount);
		if (!hasNagged && ((searchCount % nagFrequency) == 0))  {
			searchBundle = null;
			showDialog(NagDialog);
			hasNagged = true;
		}

		wjSearchObj = new JudgeSearch();
		wjSearchObj.execute(this, searchString, dictionary);
		wjText.setText("");

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

    private void startJudgeHistorySearch(int position)
    {

    	if (WordPlayApp.getInstance().isFreeMode())
    		position -= 1;

    	JudgeHistoryObject elem = JudgeHistory.getInstance().getJudgeHistory().get(position);

    	if (elem.getState())  {

    		if (elem.getWord().contains(","))
    			return;

    		Bundle args = new Bundle();
			args.putString("SearchString", elem.getWord());
			args.putInt("SearchType", SearchType.OPTION_DICTIONARY_EXACT_MATCH.ordinal());
			args.putInt("Dictionary", DictionaryType.DICTIONARY_DICT_DOT_ORG.ordinal());

    		startSearchActivity(args);

    	}
    	else
    		Toast.makeText(getActivity(), "Cannot search for unknown words", Toast.LENGTH_SHORT).show();

    }

}
