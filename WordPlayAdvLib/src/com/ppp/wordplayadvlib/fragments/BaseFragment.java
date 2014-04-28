package com.ppp.wordplayadvlib.fragments;

import java.io.File;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.ppp.wordplayadvlib.Constants;
import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.WordPlayApp;
import com.ppp.wordplayadvlib.appdata.DictionaryType;
import com.ppp.wordplayadvlib.database.WordlistDatabase;
import com.ppp.wordplayadvlib.dialogs.AppErrDialog;
import com.ppp.wordplayadvlib.utils.Debug;
import com.ppp.wordplayadvlib.utils.Utils;
import com.ppp.wordplayadvlib.widgets.ActionBarSpinner.ActionBarSpinnerCallback;

@SuppressLint("ValidFragment")
public class BaseFragment extends Fragment
	implements
		ActionBarSpinnerCallback
{

	private static final int RestartNotificationId = 1;

	protected static final int EmailActivity = 1;
	protected static final int HelpViewerActivity = 2;
	protected static final int UserPrefsActivity = 3;

	protected static int searchCount = 0;
	protected static int nagFrequency = Constants.NagDialogFrequency;
	protected static Boolean hasNagged = false;
	private static boolean notificationIconEnabled = false;

	protected Bundle searchBundle = null;
	protected static Bundle savedSearchBundle = null;

	protected HostFragmentInterface hostFragment;

	//
	// Fragment Methods
	//

	public BaseFragment() { super(); }

	@Override
	public void onCreate(Bundle savedInstanceState)
	{

		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

	}

	@Override
	public void onAttach(Activity activity)
	{

		super.onAttach(activity);

		if (activity instanceof HostFragmentInterface)
			setHostFragment((HostFragmentInterface) activity);
		else if ((getParentFragment() != null) &&
					getParentFragment() instanceof HostFragmentInterface)
			setHostFragment((HostFragmentInterface) getParentFragment());
		else if ((getParentFragment() != null) && (getParentFragment().getParentFragment() != null) &&
					getParentFragment().getParentFragment() instanceof HostFragmentInterface)
		    setHostFragment((HostFragmentInterface) (getParentFragment().getParentFragment()));

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{

		super.onActivityCreated(savedInstanceState);

        // For the free mode, see if we've shown the free dialog
        // and if we haven't, show it.  If we show it, when we're
        // done, the database will get installed.
        //
        // For the paid mode, make sure we've got a database.
		if (WordPlayApp.getInstance().isFreeMode())
			freeDialogCheck();
		else
			createDatabaseIfMissing();

	}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {

    	switch (requestCode)  {
	
	    	case EmailActivity:
	
	    		// When returning from email sent from the nag dialog, finish
	    		// the search the user started
	    		if (savedSearchBundle != null)  {
	    			try {
//	    				startActivity(savedSearchIntent);
	    			}
	    			catch (Exception e) {}
	    		}
	    		break;
	
	    	case HelpViewerActivity:
	 
	    		// We're returning from showing the release notes from the
	    		// free app installed dialog.  Proceed to creating the database
	    		// if that is required.
	    		createDatabaseIfMissing();
	    		break;
	
	    	case UserPrefsActivity:
	
	    		// We've returned from setting preferences.  Apply the only one we
	    		// know about now by adding or removing the notification icon.
	    		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
	        	boolean newNotificationSetting = prefs.getBoolean("notification_bar", false);
	        	if (newNotificationSetting != notificationIconEnabled)  {
	        		if (newNotificationSetting)  {
	        	    	Intent intent = new Intent(getActivity(), getActivity().getClass());
	        	    	addRestartNotification(intent);
	        		}
	        		else
	        			removeNotification();
	        		notificationIconEnabled = newNotificationSetting;
	        	}
	        	break;

    	}
    	
    }

	//
	// Search Support
	//

	public void startNewSearch(Bundle args)
	{
		SearchFragment fragment = new SearchFragment();
		fragment.setArguments(args);
		pushToStack(fragment);
	}

    //
    // Menu Helpers
    //

	@Override
	public void onSelection(int selection)
	{
		Debug.e("onSelection: called from BaseFragment");
	}

	public String[] getDictionaryNames() { return null; }

	public int getSelectedDictionary() { return 0; }

    //
    // Database Installation
    //

    private void startDatabaseInstallation(Context context, DialogFragment dialog)
    {
		new DatabaseWaitTask(context, dialog).execute();    	
    }

    private void createDatabaseIfMissing()
    {

//    	WordlistDatabase db =
//    		(WordlistDatabase) new WordlistDatabase(getActivity()).openReadOnly();
//
//    	// If the database is old or missing, the version will be -1
//    	int dbVersion = db.getDatabaseVersion();
//		if (dbVersion == DatabaseInfo.INVALID_DB_VERSION)  {
//			Debug.e("bad db version " + dbVersion);
//			showDialog(InstallDbDialog);
//		}
//		else if (dbVersion != DatabaseInfo.CURRENT_DB_VERSION)  {
//			Debug.e("old db version " + dbVersion);
//			showDialog(UpgradeDbDialog);
//		}
//
//		db.close();

    }

    private class DatabaseWaitTask extends AsyncTask<Void, Void, Void> {

    	private Context context = null;
    	private DialogFragment dialogFragment = null;

    	private Exception exception = null;
    	private ProgressDialog progressDialog = null;

    	public DatabaseWaitTask(Context ctx, DialogFragment dialog)
    	{
    		context = ctx;
    		dialogFragment = dialog;
    	}

    	protected void onPreExecute()
    	{

    		if (dialogFragment != null)
    			if (!getActivity().isFinishing())
    				dialogFragment.dismiss();

    		if (!getActivity().isFinishing())  {
    			progressDialog = new ProgressDialog(context);
    			String installLocStr = WordlistDatabase.dbInstallsOnExternalStorage() ?
						getString(R.string.dictionary_on_external_storage) :
						getString(R.string.dictionary_on_internal_storage);
				String message = String.format(getString(R.string.dictionary_progress_dialog_text), installLocStr);
				progressDialog.setMessage(message);
    			progressDialog.setCancelable(false);
    			progressDialog.show();
    		}

    	}

		@Override
		protected Void doInBackground(Void... params)
		{
			try {
				WordlistDatabase.deleteDatabaseFile(getActivity());
				WordlistDatabase.createDatabaseFile(getActivity());
			}
			catch (Exception e) { exception = e; }
			return null;
		}

		protected void onPostExecute(Void result)
		{

			// Reset the dictionaries to ENABLE in preferences
			// and in the spinners
			SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = prefs.edit();
			int defaultDict = DictionaryType.DICTIONARY_ENABLE.ordinal();
			editor.putInt("dictionary_dict", defaultDict - 1);
			editor.putInt("anagrams_dict", defaultDict - 1);
			editor.putInt("wordjudge_dict", defaultDict - 1);
			editor.putInt("crosswords_dict", defaultDict - 1);
			editor.commit();

			// Dismiss the dialog
			if (progressDialog != null)
				if (!getActivity().isFinishing())
					progressDialog.dismiss();

			// If there was an exception during install,
			// report it
			if (exception != null)
				createDatabaseExceptionDialog(context, exception);

		}

    }

    private void createDatabaseExceptionDialog(Context context, Exception exception)
    {

		StringBuilder builder = new StringBuilder();

		File file = Environment.getDataDirectory();
		builder.append(file.getPath());
		builder.append(" ");
		builder.append(Utils.getFreeSpaceForFile(context, file));
		builder.append("\n");

		file = Environment.getExternalStorageDirectory();
		builder.append(file.getPath());
		builder.append(" ");
		builder.append(Utils.getFreeSpaceForFile(context, file));
		builder.append("\n");

		builder.append("Installs on " +
						(WordlistDatabase.dbInstallsOnExternalStorage() ?
								getString(R.string.dictionary_on_external_storage) :
									getString(R.string.dictionary_on_internal_storage)) + " storage");
		builder.append("\n");

		if (!getActivity().isFinishing())
			new AppErrDialog(context, exception, builder.toString()).show();

    }

    //
    // Notification Bar Icon Support
    //

    public void addRestartNotification(Intent startIntent)
    {

    	int icon = 0;
    	Activity activity = getActivity();
    	NotificationManager manager = (NotificationManager)activity.getSystemService(Context.NOTIFICATION_SERVICE);

    	// Get the notification parameters
    	if (WordPlayApp.getInstance().isFreeMode())
			icon = R.drawable.ic_launcher_wordplay_assistant_free;
		else
			icon = R.drawable.ic_launcher_wordplay_assistant;
		String tickerText = getString(R.string.notify_ticker_text);
		String title = getString(R.string.app_name);
		String content = getString(R.string.notify_description);

		// Set the flags required to restart the app in the Intent we
		// received
		startIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

		// Create the PendingIntent that is fired when the notification is selected
		// by the user
		PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, startIntent, 0);

		// Create the notification
		Notification notification = new Notification(icon, tickerText, System.currentTimeMillis());
		notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;   
		notification.setLatestEventInfo(activity, title, content, pendingIntent);

		// Give it to the notification manager for display
		manager.notify(RestartNotificationId, notification);

    }

    private void removeNotification()
    {
    	NotificationManager manager = (NotificationManager)getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
    	manager.cancel(RestartNotificationId);
    }

	//
	// Miscellaneous Support
	//

    private void freeDialogCheck()
    {

		SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);

		// Have we show the dialog already?  It is shown only on the
		// very first run.
		boolean hasShown = prefs.getBoolean("free_dialog_shown", false);
		if (!hasShown)  {

			// Update the preferences to mark that its been shown
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean("free_dialog_shown", true);
			editor.commit();

			// Show the dialog
//			showDialog(FreeDialog);

		}
		else
			createDatabaseIfMissing();

    }

    protected boolean validateString(String searchString,
    									DictionaryType dictionary,
    									boolean isAnagram,
    									boolean isCrossword)
    {

		int wildcardCount = 0;

		if (!isAnagram && (searchString.length() < Constants.MinAnagramLength))
			return false;

		if (isAnagram &&
				(searchString.length() < Constants.MinAnagramLength) ||
				(searchString.length() > Constants.MaxAnagramLength))
			return false;

		if (!isCrossword)  {
			if (dictionary.isScrabbleDict())  {
				for (int i = 0; i < searchString.length(); i += 1)
					if ((searchString.charAt(i) == '.') || (searchString.charAt(i) == '?'))
						wildcardCount += 1;
				if (wildcardCount > 1)
					return false;
			}
			else {
				if (searchString.contains(".") || searchString.contains("?"))
					return false;
			}
		}
		
		return true;
		
    }

	//
	// Fragment Manipulation
	//

	public interface HostFragmentInterface {
		public void pushToStack(BaseFragment newFragment);
		public void replaceStack(BaseFragment newFragment);
		public void clearStack();
		public void clearStackToFragment(String fragmentName);
		public void popStack();
	}

	public void setHostFragment(HostFragmentInterface nTH) { hostFragment = nTH; }
	public HostFragmentInterface getHostFragment() { return hostFragment; }

	protected void pushToStack(BaseFragment newFragment)
	{
		if (hostFragment != null)
			hostFragment.pushToStack(newFragment);
	}

	protected void replaceStack(BaseFragment newFragment)
	{
		if (hostFragment != null)
			hostFragment.replaceStack(newFragment);
	}

	protected void clearStack()
	{
		if (hostFragment != null)
			hostFragment.clearStack();
	}

	protected void clearStackToFragment(String fragmentName)
	{
		if (hostFragment != null)
			hostFragment.clearStackToFragment(fragmentName);
	}
	
	protected void popStack()
	{
		if (hostFragment != null)
			hostFragment.popStack();
	}

	//
	// Filters and Listeners
	//

    // A filter for input of one or more alphabetic characters
    protected InputFilter alphaFilter = new InputFilter() {
    	public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend)
    	{
    		if (end > start)  {
    			String destText = dest.toString();
    			String resultText =
    				destText.substring(0, dstart) + source.subSequence(start, end) + destText.substring(dend);
    			if (!resultText.matches("[a-zA-Z]*"))  {
    				if (source instanceof Spanned)
    					return new SpannableString("");
    				else
    					return "";
    			}
    		}
    		return null;
    	}
    };

    // A filter for input of a list of words
    protected InputFilter commaFilter = new InputFilter() {
    	public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend)
    	{
    		if (end > start)  {
    			String destText = dest.toString();
    			String resultText =
    				destText.substring(0, dstart) + source.subSequence(start, end) + destText.substring(dend);
    			if (!resultText.matches("([a-zA-Z]+,?)*"))  {
    				if (source instanceof Spanned)
    					return new SpannableString("");
    				else
    					return "";
    			}
    		}
    		return null;
    	}
    };

    // A filter for a wild-carded text entry field
    protected InputFilter searchFilter = new InputFilter() {
    	public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend)
    	{
    		if (end > start)  {
    			String destText = dest.toString();
    			String resultText =
    				destText.substring(0, dstart) + source.subSequence(start, end) + destText.substring(dend);
    			if (!resultText.matches("[a-zA-Z?.]*"))  {
    				if (source instanceof Spanned)
    					return new SpannableString("");
    				else
    					return "";
    			}
    		}
    		return null;
    	}
    };

    // A TextWatcher for updating the query submit button state
	public TextWatcher buttonTextWatcher = new TextWatcher() {

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {}

		@Override
		public void afterTextChanged(Editable s)
		{
			setButtonState();
		}
    	
    };

    protected void setButtonState() {}

	protected boolean checkForEnterKey(View v, int keyCode, KeyEvent event)
	{

		// Only operate on the ENTER key when pressed down
		if (event.getAction() == KeyEvent.ACTION_UP)
			return false;
		if (event.getKeyCode() != KeyEvent.KEYCODE_ENTER)
			return false;

		// Dismiss the soft keyboard
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
		
		return true;
		
	}

}
