package com.ppp.wordplayadvlib.fragments;

import java.util.LinkedList;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdSize;
import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.WordPlayApp;
import com.ppp.wordplayadvlib.analytics.Analytics;
import com.ppp.wordplayadvlib.externalads.AdMobAd;
import com.ppp.wordplayadvlib.externalads.AdMobData;
import com.ppp.wordplayadvlib.externalads.SponsoredAd;
import com.ppp.wordplayadvlib.externalads.SponsoredAd.EventCallback;
import com.ppp.wordplayadvlib.externalads.SponsoredAd.PlacementType;
import com.ppp.wordplayadvlib.model.DictionaryType;
import com.ppp.wordplayadvlib.model.JudgeHistory;
import com.ppp.wordplayadvlib.model.JudgeHistoryObject;
import com.ppp.wordplayadvlib.model.JudgeSearch;
import com.ppp.wordplayadvlib.model.SearchType;
import com.ppp.wordplayadvlib.utils.Debug;

public class WordJudgeFragment extends BaseFragment
	implements
		View.OnClickListener,
		EventCallback
{

	private View rootView;
	private Button wjButton = null;
	private EditText wjText = null;
	private static ListView wjListview = null;
	private static WordJudgeAdapter wjAdapter = null;
	private LinearLayout adView = null;

	private JudgeSearch wjSearchObj = null;
	private AdMobAd adMobAd = null;

	//
	// Activity Methods
	//

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		rootView = inflater.inflate(R.layout.word_judge_fragment, container, false);
		setupWordJudgeTab();
		return rootView;
	}

	@Override
	public void onPause()
	{

		super.onPause();

		if (adMobAd != null)
			adMobAd.pause();

	}

	@Override
	public void onResume()
	{

		super.onResume();

		Analytics.screenView(Analytics.WORD_JUDGE_SCREEN);

		setButtonState();

		if (adMobAd != null)
			adMobAd.resume();

	}

	@Override
	public void onDestroy()
	{

		super.onDestroy();

		if (adMobAd != null)
			adMobAd.destroy();

	}

	@Override
    public void onClick(View v)
    {
        InputMethodManager imm =
        	(InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(wjText.getWindowToken(), 0);
		getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    	startWordJudgeSearch();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
    	MenuItem item = menu.findItem(R.id.clearhistory_menu);
    	if (item != null)
    		item.setVisible(true);
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{

		// Handle clearing history
		if (item.getItemId() == R.id.clearhistory_menu)  {

			Analytics.sendEvent(Analytics.HISTORY, Analytics.CLEAR_JUDGE_HISTORY, "", 0);

			JudgeHistory.getInstance().clearJudgeHistory(getActivity());
			wjAdapter.notifyDataSetChanged();
			return true;

		}

		return false;
			
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
    	int wjDict = prefs.getInt("wordjudgeDict", DictionaryType.DICTIONARY_ENABLE.ordinal());
    	return wjDict - 1;
	}

	@Override
	public void onSelection(int selection)
	{

		SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		DictionaryType dict = DictionaryType.fromInt(selection + 1);

		editor.putInt("wordjudgeDict", dict.ordinal());
		Debug.v("SAVE wordjudgeDict = " + dict.ordinal());
		editor.commit();

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
        wjText.addTextChangedListener(buttonTextWatcher);
		
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

        // Load an ad into the AdView if we don't already have one.
        // If we have one and it is loaded, place it into the view.
    	adView = (LinearLayout) rootView.findViewById(R.id.WordJudgeAdView);
        if (WordPlayApp.getInstance().isFreeMode())  {
        	setupAdView();
        	if (adMobAd == null)
        		loadAdMobAd();
        	else {
        		if ((adMobAd.getView() != null) && adMobAd.isLoaded())
        			showAdMobAd(adMobAd);
        	}
        }

        updateJudgeHistoryAdapter();

	}

	//
	// Search Activity Support
	//

    private void startWordJudgeSearch()
    {

		String searchString = wjText.getText().toString().toLowerCase();
		DictionaryType dictionary = DictionaryType.fromInt(getSelectedDictionary() + 1);

		wjSearchObj = new JudgeSearch();
		wjSearchObj.execute(this, searchString, dictionary);
		wjText.setText("");

    }

    @Override
    protected void setButtonState()
    {

		String searchString = wjText.getText().toString();
		DictionaryType dictionary = DictionaryType.fromInt(getSelectedDictionary() + 1);

		wjButton.setEnabled(validateString(searchString, dictionary, false, false));
    	
    }

    //
    // Word Judge & Adapter
    //

	public void setWordJudgeObject(JudgeSearch o) { wjSearchObj = o; }
	
	public WordJudgeAdapter getWordJudgeAdapter() { return wjAdapter; }

	public void updateJudgeHistoryAdapter()
	{
		wjAdapter =
			new WordJudgeAdapter(getActivity(), R.layout.judge_history, JudgeHistory.getInstance().getJudgeHistory());
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

    	JudgeHistoryObject elem = JudgeHistory.getInstance().getJudgeHistory().get(position);

    	if (elem.getState())  {

    		if (elem.getWord().contains(","))
    			return;

    		Bundle args = new Bundle();
			args.putString("SearchString", elem.getWord());
			args.putInt("SearchType", SearchType.OPTION_DICTIONARY_EXACT_MATCH.ordinal());
			args.putInt("Dictionary", DictionaryType.DICTIONARY_DICT_DOT_ORG.ordinal());

    		startNewSearch(args);

    	}
    	else
    		Toast.makeText(getActivity(), "Cannot search for unknown words", Toast.LENGTH_SHORT).show();

    }

    //
    // AdView
    //

    private void setupAdView()
    {

		float density = getResources().getDisplayMetrics().density;
//		int width = Math.round(AdSize.BANNER.getWidth() * density);
		DisplayMetrics metrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int width = metrics.widthPixels;
		int height = Math.round(AdSize.BANNER.getHeight() * density);

		Log.e(getClass().getSimpleName(), "width " + width);
		Log.e(getClass().getSimpleName(), "height " + height);

		LinearLayout.LayoutParams params =
			new LinearLayout.LayoutParams(width, height);
		adView.setLayoutParams(params);

    }

    private void loadAdMobAd()
    {

    	// Create the ad
    	String adUnitId = WordPlayApp.getInstance().getWordJudgeAdUnitId();
    	AdMobData adMobData = new AdMobData(adUnitId);
    	adMobAd = new AdMobAd(getActivity(), PlacementType.ListSearchResult, adMobData);

    	// Load it
    	adMobAd.setEventCallback(this);
    	adMobAd.getView();

    }

    private void showAdMobAd(SponsoredAd ad)
    {
    	if ((ad.getView() != null) && (ad.getView().getParent() != null))  {
    		LinearLayout parent = (LinearLayout) ad.getView().getParent();
    		parent.removeAllViews();
    	}
		adView.addView(ad.getView());    	
    }

	@Override
	public void onLoaded(SponsoredAd ad)
	{
		Log.d(getClass().getSimpleName(), "AdMob WordJudge: onLoaded");
		showAdMobAd(ad);
	}

	@Override
	public void onError(SponsoredAd ad)
	{
		Log.d(getClass().getSimpleName(), "AdMob WordJudge: onError");
	}

	@Override
	public void onOpened(SponsoredAd ad)
	{
		Log.d(getClass().getSimpleName(), "AdMob WordJudge: onOpened");		
	}

	@Override
	public void onClosed(SponsoredAd ad)
	{
		Log.d(getClass().getSimpleName(), "AdMob WordJudge: onClosed");
	}

}
