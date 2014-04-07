package com.ppp.wordplayadvlib.fragments;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ppp.wordplayadvlib.Constants;
import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.WordPlayApp;
import com.ppp.wordplayadvlib.activities.HelpViewer;
import com.ppp.wordplayadvlib.appdata.DictionaryType;
import com.ppp.wordplayadvlib.appdata.History;
import com.ppp.wordplayadvlib.appdata.SearchType;
import com.ppp.wordplayadvlib.appdata.WordScoreState;
import com.ppp.wordplayadvlib.appdata.WordSortState;
import com.ppp.wordplayadvlib.database.WordlistDatabase;
import com.ppp.wordplayadvlib.database.schema.DatabaseInfo;
import com.ppp.wordplayadvlib.dialogs.AppErrDialog;
import com.ppp.wordplayadvlib.utils.Debug;
import com.ppp.wordplayadvlib.utils.Utils;

@SuppressLint("ValidFragment")
public class BaseFragment extends Fragment {

	private static final int RestartNotificationId = 1;

	protected static final int InstallDbDialog = 1;
	protected static final int FreeDialog = 2;
	protected static final int UpgradeDbDialog = 3;
	protected static final int AboutDialog = 4;
	protected static final int NagDialog = 5;

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

    protected void showDialog(int id)
    {

    	DialogFragment newFragment = null;

    	switch (id) {

	    	case InstallDbDialog:
//	    	    newFragment = new DbInstallDialog(this, false);
//	    	    newFragment.setCancelable(false);
//	    	    newFragment.show(getFragmentManager(), "InstallDbDialog");
	    		break;

	    	case FreeDialog:
	    		newFragment = new FreeDialog(this);
	    		newFragment.setCancelable(false);
	    		newFragment.show(getFragmentManager(), "FreeDialog");
	    		break;

	    	case UpgradeDbDialog:
//	    	    newFragment = new DbInstallDialog(this, true);
//	    	    newFragment.setCancelable(false);
//	    	    newFragment.show(getFragmentManager(), "UpgradeDbDialog");
	    		break;

	    	case AboutDialog:
	    		newFragment = new AboutDialog(this);
	    		newFragment.show(getFragmentManager(), "AboutDialog");
	    		break;

	    	case NagDialog:
	    		newFragment = new NagDialog(this);
	    		newFragment.show(getFragmentManager(), "NagDialog");
	    		break;

    	}

    }
    
//	@Override
//	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
//	{
//	    inflater.inflate(R.menu.search_menu, menu);
//	}

//	@Override
//	public void onPrepareOptionsMenu(Menu menu)
//	{
//	
//		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
//	
//		// If the notification bar is turned off, don't show "Exit"
//		MenuItem item = menu.findItem(R.id.exit_menu);
//		item.setVisible(prefs.getBoolean("notification_bar", false));
//	
//	}
    
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item)
//	{
//	
//		Activity activity = getActivity();
//	
//		// Preferences
//		if (item.getItemId() == R.id.prefs_menu)  {
//			Intent intent = new Intent(activity, UserPreferenceActivity.class);
//			try {
//				startActivityForResult(intent, UserPrefsActivity);
//			}
//			catch (Exception e) {
//				Debug.e("User Prefs Startup Failed: " + e);
//			}
//		}
//	
//		// History
//		else if (item.getItemId() == R.id.showhistory_menu)  {
//			showHistory();
//		}
//	
//		// Clear history
//		else if (item.getItemId() == R.id.clearhistory_menu)  {
//	//			if (currentTab == WordJudgeTab)  {
//	//				clearJudgeHistory(true);
//	//				updateJudgeHistoryAdapter();
//	//				wjAdapter.notifyDataSetChanged();
//	//			}
//	//			else
//	//				clearHistory(true);
//		}
//	
//		// Dictionaries
//		else if (item.getItemId() == R.id.dictionary_menu)
//			showDictionaries();
//	
//		// Help
//		else if (item.getItemId() == R.id.showhelp_menu)
//			showHelp();
//	
//		// About
//		else if (item.getItemId() == R.id.showabout_menu)
//			showDialog(AboutDialog);
//	
//		// Exit
//		else if (item.getItemId() == R.id.exit_menu)  {
//			removeNotification();
//			activity.finish();
//		}
//	
//		// Reinstall Dictionary
//		else if (item.getItemId() == R.id.dictionary_reinstall_menu)  {
//			WordlistDatabase.deleteDatabaseFile(getActivity());
//			try {
//				WordlistDatabase.createDatabaseFile(getActivity());
//			}
//			catch (Exception e) {
//				createDatabaseExceptionDialog(getActivity(), e);
//			}
//		}
//		
//		return true;
//		
//	}

	//
	// Search Activity Support
	//

	public void startSearchActivity(SearchType searchType,
									String searchString,
									String boardString,
									DictionaryType dictionary,
									WordScoreState wordScores,
									WordSortState wordSort)
	{

		if (searchString != null)
			searchString = searchString.toLowerCase();
		if (boardString != null)
			boardString = boardString.toLowerCase();

		Bundle args = new Bundle();
		args.putString("SearchString", searchString);
		args.putString("BoardString", boardString);
		args.putInt("SearchType", searchType.ordinal());
		args.putInt("Dictionary", dictionary.ordinal());
		args.putInt("WordScores", wordScores.ordinal());
		args.putInt("WordSort", wordSort.ordinal());

		// Only start a search if the search string exists and has a length
		if ((searchString != null) && (searchString.length() != 0))  {
 
			// Make sure it isn't zero length after removing spaces
			searchString = searchString.replace(" ", "");
			if (searchString.length() == 0)  {
				Toast.makeText(getActivity(), "Please enter a word or search string", Toast.LENGTH_SHORT).show();
				return;
			}

			// Add this search to the history
			History.getInstance().addHistory(searchString,
    											boardString,
    											searchType,
    											dictionary,
    											wordScores,
    											wordSort);

			// Update the search count and perform a nag dialog if required
			searchCount += 1;
			Debug.v("SEARCH_COUNT " + searchCount);
			if (!hasNagged && ((searchCount % nagFrequency) == 0))  {
				searchBundle = args;
				showDialog(NagDialog);
				hasNagged = true;
			}
			else
				startNewSearch(args);

    	}
    	else {
			Toast.makeText(getActivity(), "Please enter a word or search string", Toast.LENGTH_SHORT).show();
			return;
    	}
		
	}

	public void startNewSearch(Bundle args)
	{
		SearchFragment fragment = new SearchFragment();
		fragment.setArguments(args);
		pushToStack(fragment);
	}

    //
    // Menu Helpers
    //

    private String getHelpText(String whichHelp, int id)
    {

    	BufferedReader rd =
    		new BufferedReader(new InputStreamReader(getResources().openRawResource(id)), Constants.BufSize);
    	String retval = "";
    	String line;
    	
    	try {
    		while ((line = rd.readLine()) != null)  {
    			if (line.length() == 0)
    				continue;
    			retval += line.replace('\n', ' ');
    		}
    		rd.close();
    	}
    	catch (IOException e) {
    		return "Unable to view '" + whichHelp + "' help at this time.";
    	}
    	
    	return retval;
    	
    }

    //
    // Dialogs
    //

    public static class AboutDialog extends DialogFragment {

    	BaseFragment fragment;

		public AboutDialog(BaseFragment f)
    	{
    		super();
    		fragment = f;
    	}

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {

        	AlertDialog.Builder builder;
        	final AlertDialog dialog;

        	LayoutInflater inflater =
        		(LayoutInflater)getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        	final View layout =
        		inflater.inflate(R.layout.about_dialog,
        							(ViewGroup)getActivity().findViewById(R.id.about_dialog_layout));

        	builder = new AlertDialog.Builder(getActivity());
        	builder.setView(layout);
        	dialog = builder.create();

        	layout.setOnClickListener(new View.OnClickListener() {
        		public void onClick(View v)
        		{
        			if (!getActivity().isFinishing())
        				dismiss();
        		}
        	});

        	ImageView iconImage = (ImageView)layout.findViewById(R.id.about_dialog_image);
        	iconImage.setOnClickListener(new View.OnClickListener() {
        		public void onClick(View v)
        		{
        			Intent myIntent =
        				new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.WebAddress));
        			if (!getActivity().isFinishing())  {
        				dismiss();
    	    			try {
    	    				if (!getActivity().isFinishing())
    	    					startActivity(myIntent);
    	    			}
    	    			catch (Exception e) {}
        			}
        		}
        	});
        	    	
        	final String appName = getString(R.string.app_name);
        	TextView versionText = (TextView)layout.findViewById(R.id.about_dialog_version);
        	versionText.setText(appName + " v" + WordPlayApp.appVersionName);
        	
        	TextView copyrightText = (TextView)layout.findViewById(R.id.about_dialog_copyright);
        	copyrightText.setText(getString(R.string.copyright));
        	
        	TextView companyNameText = (TextView)layout.findViewById(R.id.about_dialog_company_name);
        	companyNameText.setText(getString(R.string.company_name));
        	
        	Button contactButton = (Button)layout.findViewById(R.id.contact_us);
        	contactButton.setOnClickListener(new View.OnClickListener() {
        		public void onClick(View v)
        		{
    	    		Intent intent = new Intent(Intent.ACTION_SEND);
    	    		intent.setType("message/rfc822");
    	    		intent.putExtra(Intent.EXTRA_EMAIL, new String[] { Constants.EmailAddress });
    	    		intent.putExtra(Intent.EXTRA_SUBJECT,
    	    				"Comments on " + appName + " v" + WordPlayApp.appVersionName);
                	intent.putExtra(android.content.Intent.EXTRA_TEXT, "");
                	if (!getActivity().isFinishing())  {
                		dismiss();
    	            	try {
    	            		if (!getActivity().isFinishing())
    	            			startActivity(intent);
    	            	}
    	            	catch (ActivityNotFoundException exception) {
    	            		Utils.configureEmailAlert(getActivity());
    	            	}
                	}
        		}
        	});
        	
        	Button releaseNotesButton = (Button)layout.findViewById(R.id.release_notes);
        	releaseNotesButton.setOnClickListener(new View.OnClickListener()  {
        		public void onClick(View v)
        		{
        			String str = fragment.getHelpText("Release Notes", R.raw.release_notes);
        			Intent intent = new Intent(getActivity(), HelpViewer.class);
        			intent.putExtra("HelpText", str);
        			if (!getActivity().isFinishing())  {
        				dismiss();
    	    			try {
    	    				if (!getActivity().isFinishing())
    	    					startActivity(intent);
    	    			}
    	    			catch (Exception e) {}
        			}
        		}
        	});

        	Button buyItButton = (Button)layout.findViewById(R.id.buy_it);
//        	fragment.setMarketButton(dialog, buyItButton, true);
        		
        	ImageView dictOrgImage = (ImageView)layout.findViewById(R.id.powered_by_image);
        	dictOrgImage.setOnClickListener(new View.OnClickListener() {
        		public void onClick(View v)
        		{
        			Intent myIntent =
        				new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.DictOrgWebAddress));
        			if (!getActivity().isFinishing())  {
    	    			dismiss();
    	    			try {
    	    				if (!getActivity().isFinishing())
    	    					startActivity(myIntent);
    	    			}
    	    			catch (Exception e) {}
        			}
        		}
        	});
        	
        	return dialog;

        }

    }

    public class NagDialog extends DialogFragment {

    	BaseFragment fragment;

    	public NagDialog(BaseFragment f)
    	{
    		super();
    		fragment = f;
    	}

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {

        	AlertDialog.Builder builder;
        	final AlertDialog dialog;
        	
        	LayoutInflater inflater =
        		(LayoutInflater)getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        	final View layout =
        		inflater.inflate(R.layout.nag_dialog,
        							(ViewGroup)getActivity().findViewById(R.id.nag_dialog_layout));

        	builder = new AlertDialog.Builder(getActivity());
        	builder.setView(layout);
        	dialog = builder.create();
        	
        	Button contactButton = (Button)layout.findViewById(R.id.nag_button);
        	contactButton.setOnClickListener(new View.OnClickListener() {
        		public void onClick(View v)
        		{

    	    		Intent intent = new Intent(Intent.ACTION_SEND);
    	        	String appName = getString(R.string.app_name);

    	    		intent.setType("message/rfc822");
    	    		intent.putExtra(Intent.EXTRA_EMAIL, new String[] { Constants.EmailAddress });
    	    		intent.putExtra(Intent.EXTRA_SUBJECT,
    	    				"Comments on " + appName + " v" + WordPlayApp.appVersionName);
                	intent.putExtra(android.content.Intent.EXTRA_TEXT, "");
                	if (!getActivity().isFinishing())  {
    	            	dismiss();
    	            	BaseFragment.savedSearchBundle = fragment.searchBundle;
    	            	try {
    	            		if (!getActivity().isFinishing())
    	            			startActivityForResult(intent, EmailActivity);
    	            	}
    	            	catch (Exception e) {
    	            		startNewSearch(BaseFragment.savedSearchBundle);
    	            	}
                	}

        		}
        	});

        	return dialog;

        }

        @Override
        public void onCancel(DialogInterface dialog)
        {
			if (!getActivity().isFinishing())  {
				dismiss();
				if (fragment.searchBundle != null)  {
					try {
						if (!getActivity().isFinishing())
							startNewSearch(searchBundle);
					}
					catch (Exception e) {}
				}
			}
        }

    }

//    public static class DbInstallDialog extends DialogFragment {
//
//    	BaseFragment fragment;
//    	boolean isUpgrade = false;
//
//    	public DbInstallDialog(BaseFragment fragment, boolean isUpgrade)
//    	{
//    		this.fragment = fragment;
//    		this.isUpgrade = isUpgrade;
//            Bundle args = new Bundle();
//            args.putBoolean("isUpgrade", isUpgrade);
//            setArguments(args);
//    	}
//
//    	@Override
//    	public void onSaveInstanceState(Bundle savedInstanceState)
//    	{
//    		savedInstanceState.putBoolean("isUpgrade", isUpgrade);
//    	}
//
//        @Override
//        public Dialog onCreateDialog(Bundle savedInstanceState)
//        {
//
//        	AlertDialog.Builder builder;
//        	final AlertDialog dialog;
//        	boolean isUpgrade = getArguments().getBoolean("isUpgrade");
//
//        	if (savedInstanceState != null)
//        		isUpgrade = savedInstanceState.getBoolean("isUpgrade");
//
//        	LayoutInflater inflater =
//        		(LayoutInflater)getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
//        	final View layout =
//        		inflater.inflate(R.layout.dictionary_install_dialog,
//        							(ViewGroup)getActivity().findViewById(R.id.dictionary_install_layout));
//
//        	builder = new AlertDialog.Builder(getActivity());
//        	builder.setView(layout);
//        	dialog = builder.create();
//
//        	TextView textView = (TextView)layout.findViewById(R.id.dictionary_mode_text);
//        	String text = String.format(isUpgrade ?
//        									getString(R.string.dictionary_upgrade_dialog_text) :
//        									getString(R.string.dictionary_install_dialog_text),
//        								WordPlayApp.getInstance().isFreeMode() ?
//        									" Free" : "",
//        								WordPlayApp.appVersionName);
//        	textView.setText(text);
//
//        	Button okButton = (Button)layout.findViewById(R.id.dictionary_ok_button);
//        	okButton.setOnClickListener(new View.OnClickListener() {
//    			@Override
//    			public void onClick(View v)
//    			{
//    				fragment.startDatabaseInstallation(getActivity(), DbInstallDialog.this);
//    			}
//    		});
//
//        	return dialog;
//
//        }
//
//    }

    public static class FreeDialog extends DialogFragment {

    	BaseFragment fragment;

    	public FreeDialog(BaseFragment f)
    	{
    		super();
    		fragment = f;
    	}

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {

        	AlertDialog.Builder builder;
        	final AlertDialog dialog;

        	LayoutInflater inflater =
        		(LayoutInflater)getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        	final View layout =
        		inflater.inflate(R.layout.free_dialog,
        							(ViewGroup)getActivity().findViewById(R.id.free_mode_layout));

        	builder = new AlertDialog.Builder(getActivity());
        	builder.setView(layout);
        	dialog = builder.create();

        	Button showRelNotesButton = (Button)layout.findViewById(R.id.free_mode_relnotes_button);
        	showRelNotesButton.setOnClickListener(new View.OnClickListener() {
        		public void onClick(View v) {
        			String str =
        				fragment.getHelpText("Release Notes", R.raw.release_notes);
        			Intent intent = new Intent(getActivity(), HelpViewer.class);
        			intent.putExtra("HelpText", str);
        			if (!getActivity().isFinishing())  {
    	    			dismiss();
    	    			try {
    	    				if (!getActivity().isFinishing())  {
    	    					try {
    	    						fragment.startActivityForResult(intent, HelpViewerActivity);
    	    					}
    	    					catch (Exception e) {}
    	    				}
    	    			}
    	    			catch (Exception e) {}
        			}
        		}
        	});

        	Button continueButton = (Button)layout.findViewById(R.id.free_mode_continue_button);
        	continueButton.setOnClickListener(new View.OnClickListener() {
    			@Override
    			public void onClick(View v)
    			{
    				dismiss();
    				fragment.createDatabaseIfMissing();
    			}
    		});

        	return dialog;

        }

    }

    //
    // Database Installation
    //

    private void startDatabaseInstallation(Context context, DialogFragment dialog)
    {
		new DatabaseWaitTask(context, dialog).execute();    	
    }

    private void createDatabaseIfMissing()
    {

    	WordlistDatabase db =
    		(WordlistDatabase) new WordlistDatabase(getActivity()).openReadOnly();

    	// If the database is old or missing, the version will be -1
    	int dbVersion = db.getDatabaseVersion();
		if (dbVersion == DatabaseInfo.INVALID_DB_VERSION)  {
			Debug.e("bad db version " + dbVersion);
			showDialog(InstallDbDialog);
		}
		else if (dbVersion != DatabaseInfo.CURRENT_DB_VERSION)  {
			Debug.e("old db version " + dbVersion);
			showDialog(UpgradeDbDialog);
		}

		db.close();

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
			showDialog(FreeDialog);

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
