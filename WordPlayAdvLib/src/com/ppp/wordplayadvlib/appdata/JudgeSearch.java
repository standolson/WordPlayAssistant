package com.ppp.wordplayadvlib.appdata;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.WordPlayApp;
import com.ppp.wordplayadvlib.database.ScrabbleDatabaseClient;
import com.ppp.wordplayadvlib.dialogs.AppErrDialog;
import com.ppp.wordplayadvlib.exceptions.WifiAuthException;
import com.ppp.wordplayadvlib.exceptions.WordPlayException;
import com.ppp.wordplayadvlib.fragments.WordJudgeFragment;
import com.ppp.wordplayadvlib.networking.NetworkUtils;
import com.ppp.wordplayadvlib.networking.ScrabbleClient;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.widget.Toast;

public class JudgeSearch {

	private JudgeSearchObject searchObject;
	private JudgeThread searchThread;
	private Handler searchHandler;
	private ProgressDialog progressDialog;
	private boolean cancel = false;

	public class JudgeSearchObject {

		private Fragment fragment;
		private String searchString;
		private DictionaryType dictionary;
		private JudgeSearch judgeObject;
		private Handler handler;
		
		private boolean result;
		private Exception exception;

		private JudgeSearchObject(Fragment f, String str, DictionaryType dict, JudgeSearch j)
		{
			fragment = f;
			searchString = str;
			dictionary = dict;
			judgeObject = j;
		}
		
		public void setFragment(Fragment f) { fragment = f; }
		public Fragment getFragment() { return fragment; }
		
		public void setSearchString(String str) { searchString = str; }
		public String getSearchString() { return searchString; }
		
		public void setDictionary(DictionaryType dict) { dictionary = dict; }
		public DictionaryType getDictionary() { return dictionary; }
		
		public void setJudgeObject(JudgeSearch j) { judgeObject = j; }
		public JudgeSearch getJudgeObject() { return judgeObject; }
		
		public void setSearchHandler(Handler h) { handler = h; }
		public Handler getSearchHandler() { return handler; }

		public void setResult(Boolean b) { result = b; }
		public boolean getResult() { return result; }

		public void setException(Exception e) { exception = e; }
		public Exception getException() { return exception; }
		
	}

	public class JudgeThread extends Thread {

		private JudgeSearchObject judgeThreadSearchObject;
		
		JudgeThread(Runnable r, JudgeSearchObject obj)
		{
			super(r);
			judgeThreadSearchObject = obj;
		}

		public void setSearchObject(JudgeSearchObject obj) { judgeThreadSearchObject = obj; }
		public JudgeSearchObject getSearchObject() { return judgeThreadSearchObject; }
		
	}
	
	public JudgeSearch() {}

	public JudgeThread getSearchThread() { return searchThread; }
	
	private void startBackgroundSearch(Runnable r,
										Fragment f,
										String str,
										DictionaryType dict,
										JudgeSearch j)
	{

		searchObject = new JudgeSearchObject(f, str, dict, j);
		
		searchThread = new JudgeThread(r, searchObject);

		searchHandler = new Handler() {
	        public void handleMessage(Message msg) { displayResults(); }
		};
		
		searchObject.setSearchHandler(searchHandler);
		searchThread.start();
		
		openProgressDialog(searchObject.getFragment().getActivity());
	
	}

	public void displayResults()
	{

		if (progressDialog != null)
			closeProgressDialog();

		if (cancel)
			return;

		((WordJudgeFragment)searchObject.getFragment()).setWordJudgeObject(null);

		if (searchObject.getException() != null)  {
			Exception e = searchObject.getException();
//			if (e instanceof WifiAuthException)
//				Globals.startWifiAuthWebview(search_object.getContext(), (WifiAuthException)e);
//			else
//				showAppErrDialog();
			if (e instanceof WifiAuthException)
				searchObject.setException(
					new WordPlayException(WordPlayApp.getInstance().getString(R.string.wifi_auth_error)));
		}
		else {
			JudgeHistory.getInstance().addJudgeHistory(
											searchObject.getSearchString(),
											searchObject.getResult());
			((WordJudgeFragment)searchObject.getFragment()).updateJudgeHistoryAdapter();
			((WordJudgeFragment)searchObject.getFragment()).getWordJudgeAdapter().notifyDataSetChanged();
		}

	}

	public void execute(Fragment fragment, String search_string, DictionaryType dictionary)
    {

		if (search_string.length() == 0)  {
			Toast.makeText(fragment.getActivity(), "Please enter a word or search string", Toast.LENGTH_SHORT).show();
			return;
		}
    	
    	Runnable r = new Runnable() {
    		public void run()
    		{

    			ScrabbleClient client = null;
    			String[] words = searchObject.getSearchString().split(",");

				if (WordPlayApp.getInstance().getUseGoogleAppEngine())
					client = new ScrabbleClient();
				else
					client = new ScrabbleDatabaseClient();

				while (!cancel)  {
					try {
	    				if (words.length > 1)  {
	    					searchObject.setResult(
	    							client.judgeWordList(
	    									words,
	    									searchObject.getDictionary()));
	    				}
	    				else
	    					searchObject.setResult(
	    							client.judgeWord(
	    									searchObject.getSearchString().replace(",", ""),
	    									searchObject.getDictionary()));
	    				break;
	    			}
	    			catch (Exception e) {
						if (NetworkUtils.isRetryException(e))
							continue;
	    				searchObject.setException(e);
	    				break;
	    			}
				}
    			
				if (searchObject.getSearchHandler() != null)
					searchObject.getSearchHandler().sendEmptyMessage(0);

    		}    			
    	};
		
    	startBackgroundSearch(r, fragment, search_string, dictionary, this);

    }

	public ProgressDialog getProgressDialog() { return progressDialog; }
	
    public void openProgressDialog(Context context)
    {

		cancel = false;

		progressDialog = new ProgressDialog(context);
		progressDialog.setTitle("Please wait...");
		progressDialog.setMessage("Retrieving data...");
		progressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE,
								context.getString(android.R.string.cancel),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which)
									{
										searchThread.interrupt();
										cancel = true;
									}
								});
		progressDialog.show();

    }
    
    public void closeProgressDialog()
    {
    	
    	if (progressDialog != null)
    		if (!searchObject.getFragment().getActivity().isFinishing())
    			progressDialog.dismiss();
    	progressDialog = null;

    }
   
    public void showAppErrDialog()
    {

    	StringBuilder app_data = new StringBuilder();
    	
    	app_data.append("search_string = '" + searchObject.getSearchString() + "'");
    	
    	if (!searchObject.getFragment().getActivity().isFinishing())
    		new AppErrDialog(
	    			searchObject.getFragment().getActivity(),
	    			searchObject.getException(),
	    			app_data.toString()).show();

    }

}
