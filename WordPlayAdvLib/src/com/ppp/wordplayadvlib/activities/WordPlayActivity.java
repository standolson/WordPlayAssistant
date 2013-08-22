package com.ppp.wordplayadvlib.activities;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import com.ppp.wordplayadvlib.Constants;
import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.WordPlayApp;
import com.ppp.wordplayadvlib.appdata.DictionaryType;
import com.ppp.wordplayadvlib.database.WordlistDatabase;
import com.ppp.wordplayadvlib.database.schema.DatabaseInfo;
import com.ppp.wordplayadvlib.dialogs.AppErrDialog;
import com.ppp.wordplayadvlib.fragments.AnagramsFragment;
import com.ppp.wordplayadvlib.fragments.CrosswordsFragment;
import com.ppp.wordplayadvlib.fragments.DictionaryFragment;
import com.ppp.wordplayadvlib.fragments.WordJudgeFragment;
import com.ppp.wordplayadvlib.utils.Debug;
import com.ppp.wordplayadvlib.utils.SherlockBarDrawerToggle;
import com.ppp.wordplayadvlib.utils.Utils;

@SuppressLint("ValidFragment")
public class WordPlayActivity extends SherlockFragmentActivity
	implements
		OnItemClickListener
{

	private static final int RestartNotificationId = 1;

	private static final int EmailActivity = 1;
	private static final int HelpViewerActivity = 2;
	private static final int UserPrefsActivity = 3;

	private static final int InstallDbDialog = 1;
	private static final int FreeDialog = 2;
	private static final int UpgradeDbDialog = 3;
	private static final int AboutDialog = 4;
	private static final int NagDialog = 5;

    private DrawerLayout menuDrawer;
    private SherlockBarDrawerToggle drawerToggle;
	private ListView menuListView;
	private MenuAdapter menuAdapter;

    private String lastAddedTag = null;
    private String lastItemTitle = null;
    private Fragment lastAdded = null;
    private boolean drawerSeen = false;

	private static boolean notificationIconEnabled = false;

	//
	// Activity Methods
	//

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{

		super.onCreate(savedInstanceState);

//		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

	    setContentView(R.layout.menu_drawer);

	    ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(getString(R.string.app_name));

        List<DrawerMenuItem> items = new ArrayList<DrawerMenuItem>(5);
        DrawerMenuItem anagramsItem =
        	new DrawerMenuItem(getString(R.string.Anagrams), R.drawable.ic_tab_anagrams, AnagramsFragment.class);
        items.add(anagramsItem);
        items.add(new DrawerMenuItem(getString(R.string.WordJudge), R.drawable.ic_tab_wordjudge, WordJudgeFragment.class));
        items.add(new DrawerMenuItem(getString(R.string.Dictionary), R.drawable.ic_tab_dictionary, DictionaryFragment.class));
        items.add(new DrawerMenuItem(getString(R.string.Thesaurus), R.drawable.ic_tab_thesaurus, DictionaryFragment.class));
        items.add(new DrawerMenuItem(getString(R.string.Crosswords), R.drawable.ic_tab_crosswords, CrosswordsFragment.class));

        // Create and show the initial fragment or the last
        // fragment seen
        if (savedInstanceState == null)  {
            lastItemTitle = anagramsItem.title;
            replaceStack(getInitialFragment(), true);
        }
        else
            lastItemTitle = savedInstanceState.getString("lastItemTitle");
 
        // Get the DrawerLayout
        menuDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        menuDrawer.setDrawerShadow(R.drawable.drawer_shadow, Gravity.LEFT);

        // Get the ListView for the DrawerLayout and populate it with
        // the adapter of DrawerMenuItems
        menuListView = (ListView) findViewById(R.id.left_drawer);
        menuAdapter = new MenuAdapter(items);
        menuListView.setAdapter(menuAdapter);
        menuListView.setScrollingCacheEnabled(false);
        menuListView.setOnItemClickListener(this);

        // Create the drawer toggle so that we can do special shit
        // like show the normal drawer icon instead of the ActionBar
        // back icon
        drawerToggle = new SherlockBarDrawerToggle(this, menuDrawer, R.drawable.ic_drawer, R.string.app_name, R.string.app_name) {      

            public void onDrawerOpened(View drawerView) {}
            
            public void onDrawerClosed(View drawerView)
            {
                if (!drawerSeen)  {
                    drawerSeen = true;
                    SharedPreferences.Editor edit = getPreferences(Context.MODE_PRIVATE).edit();
                    edit.putBoolean("drawerSeen", true);
                    edit.commit();
                }
            }

        };
        menuDrawer.setDrawerListener(drawerToggle);

        // Initialize the drawer seen state
        drawerSeen = getPreferences(Context.MODE_PRIVATE).getBoolean("drawerSeen", false);

    }
    
    @Override
    protected void onResume()
    {

        super.onResume();

        // If the drawer has not been seen, show it
        if (!drawerSeen)
            menuDrawer.postDelayed(new Runnable() {
                @Override
                public void run() { menuDrawer.openDrawer(menuListView); }
            }, 500);

    }

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState)
	{
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putBoolean("drawerSeen", drawerSeen);
	}

    @Override
    public void onBackPressed()
    {
        if (menuDrawer.isDrawerOpen(menuListView))
            menuDrawer.closeDrawer(menuListView);
        else
            super.onBackPressed();
    }

	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {

    	switch (requestCode)  {
	
	    	case EmailActivity:
	
	    		// When returning from email sent from the nag dialog, finish
	    		// the search the user started
//	    		if (savedSearchIntent != null)  {
//	    			try {
//	    				startActivity(savedSearchIntent);
//	    			}
//	    			catch (Exception e) {}
//	    		}
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
	    		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	        	boolean newNotificationSetting = prefs.getBoolean("notification_bar", false);
	        	if (newNotificationSetting != notificationIconEnabled)  {
	        		if (newNotificationSetting)  {
	        	    	Intent intent = new Intent(this, getClass());
	        	    	addRestartNotification(intent);
	        		}
	        		else
	        			removeNotification();
	        		notificationIconEnabled = newNotificationSetting;
	        	}
	        	break;

    	}
    	
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,	long id)
    {
        DrawerMenuItem it = (DrawerMenuItem) parent.getAdapter().getItem(position);
        lastItemTitle = it.title;
        parent.setSelection(position);
        switchToFragment(it.itemClass);
        menuListView.setItemChecked(position, true);
        menuDrawer.closeDrawer(menuListView);
    }

    private void displayDialog(int id)
    {

    	DialogFragment newFragment = null;

    	switch (id) {

	    	case InstallDbDialog:
	    	    newFragment = new DbInstallDialog(false);
	    	    newFragment.setCancelable(false);
	    	    newFragment.show(getSupportFragmentManager(), "InstallDbDialog");
	    		break;

	    	case FreeDialog:
	    		newFragment = new FreeDialog();
	    		newFragment.setCancelable(false);
	    		newFragment.show(getSupportFragmentManager(), "FreeDialog");
	    		break;

	    	case UpgradeDbDialog:
	    	    newFragment = new DbInstallDialog(true);
	    	    newFragment.setCancelable(false);
	    	    newFragment.show(getSupportFragmentManager(), "UpgradeDbDialog");
	    		break;

	    	case AboutDialog:
	    		newFragment = new AboutDialog();
	    		newFragment.show(getSupportFragmentManager(), "AboutDialog");
	    		break;

	    	case NagDialog:
	    		newFragment = new NagDialog();
	    		newFragment.show(getSupportFragmentManager(), "NagDialog");
	    		break;

    	}

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.search_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {

    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

    	// If the notification bar is turned off, don't show "Exit"
    	MenuItem item = menu.findItem(R.id.exit_menu);
    	item.setVisible(prefs.getBoolean("notification_bar", false));

    	return true;

    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item)
	{

    	// Home menu
        if (drawerToggle.onOptionsItemSelected(item))
            return true;

		// Preferences
    	else if (item.getItemId() == R.id.prefs_menu)  {
			Intent intent = new Intent(this, UserPreferenceActivity.class);
			try {
				startActivityForResult(intent, UserPrefsActivity);
			}
			catch (Exception e) {
				Debug.e("User Prefs Startup Failed: " + e);
			}
		}
	
		// History
		else if (item.getItemId() == R.id.showhistory_menu)
			showHistory();
	
		// Clear history
		else if (item.getItemId() == R.id.clearhistory_menu)  {
//			if (currentTab == WordJudgeTab)  {
//				clearJudgeHistory(true);
//				updateJudgeHistoryAdapter();
//				wjAdapter.notifyDataSetChanged();
//			}
//			else
//				clearHistory(true);
		}
	
		// Dictionaries
		else if (item.getItemId() == R.id.dictionary_menu)
			showDictionaries();
	
		// Help
		else if (item.getItemId() == R.id.showhelp_menu)
			showHelp();
	
		// About
		else if (item.getItemId() == R.id.showabout_menu)
			displayDialog(AboutDialog);
	
		// Exit
		else if (item.getItemId() == R.id.exit_menu)  {
//			removeNotification();
			finish();
		}
	
		// Reinstall Dictionary
		else if (item.getItemId() == R.id.dictionary_reinstall_menu)  {
			WordlistDatabase.deleteDatabaseFile(this);
			try {
				WordlistDatabase.createDatabaseFile(this);
			}
			catch (Exception e) {
				createDatabaseExceptionDialog(this, e);
			}
		}
		
		return true;
		
	}

    //
    // Menu Helpers
    //

    private void showDictionaries()
    {
//    	if (currentTab == DictionaryTab)
//    		dictSpinner.performClick();
//    	else if (currentTab == WordJudgeTab)
//    		wjSpinner.performClick();
//    	else if (currentTab == AnagramTab)
//    		anagramSpinner.performClick();
//    	else if (currentTab == CrosswordTab)
//    		crosswordsSpinner.performClick();
    }
    
    private void showHistory()
    {
    	Intent intent = new Intent(this, SearchHistoryActivity.class);
    	try {
    		startActivity(intent);
    	}
    	catch (Exception e) {}
    }

    private void showHelp()
    {
    	
//    	String str = null;
//    	Intent intent = null;
//    	
//    	if (currentTab == DictionaryTab)  {
//    		DictionaryType dict =
//    			DictionaryType.fromInt((int)dictSpinner.getSelectedItemId() + 1);
//    		if (dict.isThesaurus())
//    			str = getHelpText("Thesaurus", R.raw.thesaurus_help);
//    		else
//    			str = getHelpText("Dictionary", R.raw.dictionary_help);
//    	}
//    	else if (currentTab == WordJudgeTab)
//    		str = getHelpText("Word Judge", R.raw.wordjudge_help);
//    	else if (currentTab == AnagramTab)
//    		str = getHelpText("Anagrams", R.raw.anagrams_help);
//    	else if (currentTab == CrosswordTab)
//    		str = getHelpText("Crosswords", R.raw.crosswords_help);
//    	
//		intent = new Intent(this, HelpViewer.class);
//		intent.putExtra("HelpText", str);
//		try {
//			startActivity(intent);
//		}
//		catch (Exception e) {}
    	
    }
    
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

	public void setDictionaryTabMode(int dictionary, EditText textEntry)
	{

		if (dictionary == DictionaryType.DICTIONARY_THESAURUS.ordinal())
			textEntry.setHint(R.string.thesaurus_edit_hint);
		else
			textEntry.setHint(R.string.dictionary_edit_hint);

	}


    //
    // Dialogs
    //

    private class AboutDialog extends DialogFragment {

    	public AboutDialog() { super(); }

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
        	versionText.setText(appName + " v" + Constants.AppMajorVersion + "." + Constants.AppMinorVersion);
        	
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
    	    				"Comments on " + appName + " v" +
    	    				Constants.AppMajorVersion + "." + Constants.AppMinorVersion);
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
        			String str = getHelpText("Release Notes", R.raw.release_notes);
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

    private class NagDialog extends DialogFragment {

    	public NagDialog() { super(); }

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
    	    				"Comments on " + appName + " v" + Constants.AppMajorVersion + "." + Constants.AppMinorVersion);
                	intent.putExtra(android.content.Intent.EXTRA_TEXT, "");
                	if (!getActivity().isFinishing())  {
    	            	dismiss();
//    	            	fragment.savedSearchIntent = fragment.searchIntent;
    	            	try {
    	            		if (!getActivity().isFinishing())
    	            			startActivityForResult(intent, EmailActivity);
    	            	}
    	            	catch (Exception e) {
//    	    	    		startActivity(fragment.savedSearchIntent);
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
//				if (fragment.searchIntent != null)  {
//					try {
//						if (!this.isFinishing())
//							startActivity(fragment.searchIntent);
//					}
//					catch (Exception e) {}
//				}
			}
        }

    }

    private class DbInstallDialog extends DialogFragment {

    	boolean isUpgrade = false;

    	public DbInstallDialog() { super(); }

    	public DbInstallDialog(boolean isUpgrade)
    	{
    		super();
    		this.isUpgrade = isUpgrade;
            Bundle args = new Bundle();
            args.putBoolean("isUpgrade", isUpgrade);
            setArguments(args);
    	}

    	@Override
    	public void onSaveInstanceState(Bundle savedInstanceState)
    	{
    		savedInstanceState.putBoolean("isUpgrade", isUpgrade);
    	}

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {

        	AlertDialog.Builder builder;
        	final AlertDialog dialog;
        	boolean isUpgrade = getArguments().getBoolean("isUpgrade");

        	if (savedInstanceState != null)
        		isUpgrade = savedInstanceState.getBoolean("isUpgrade");

        	LayoutInflater inflater =
        		(LayoutInflater)getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        	final View layout =
        		inflater.inflate(R.layout.dictionary_install_dialog,
        							(ViewGroup)getActivity().findViewById(R.id.dictionary_install_layout));

        	builder = new AlertDialog.Builder(getActivity());
        	builder.setView(layout);
        	dialog = builder.create();

        	TextView textView = (TextView)layout.findViewById(R.id.dictionary_mode_text);
        	String text = String.format(isUpgrade ?
        									getString(R.string.dictionary_upgrade_dialog_text) :
        									getString(R.string.dictionary_install_dialog_text),
        								WordPlayApp.getInstance().isFreeMode() ?
        									" Free" : "",
        								Constants.AppMajorVersion, Constants.AppMinorVersion);
        	textView.setText(text);

        	Button okButton = (Button)layout.findViewById(R.id.dictionary_ok_button);
        	okButton.setOnClickListener(new View.OnClickListener() {
    			@Override
    			public void onClick(View v)
    			{
    				startDatabaseInstallation(getActivity(), DbInstallDialog.this);
    			}
    		});

        	return dialog;

        }

    }

    private class FreeDialog extends DialogFragment {

    	public FreeDialog() { super(); }

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
        			String str = getHelpText("Release Notes", R.raw.release_notes);
        			Intent intent = new Intent(getActivity(), HelpViewer.class);
        			intent.putExtra("HelpText", str);
        			if (!getActivity().isFinishing())  {
    	    			dismiss();
    	    			try {
    	    				if (!getActivity().isFinishing())  {
    	    					try {
    	    						startActivityForResult(intent, HelpViewerActivity);
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
    				createDatabaseIfMissing();
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
    		(WordlistDatabase) new WordlistDatabase(this).openReadOnly();

    	// If the database is old or missing, the version will be -1
    	int dbVersion = db.getDatabaseVersion();
		if (dbVersion == DatabaseInfo.INVALID_DB_VERSION)  {
			Debug.e("bad db version " + dbVersion);
			displayDialog(InstallDbDialog);
		}
		else if (dbVersion != DatabaseInfo.CURRENT_DB_VERSION)  {
			Debug.e("old db version " + dbVersion);
			displayDialog(UpgradeDbDialog);
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
    			if (!isFinishing())
    				dialogFragment.dismiss();

    		if (!isFinishing())  {
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
				WordlistDatabase.deleteDatabaseFile(WordPlayActivity.this);
				WordlistDatabase.createDatabaseFile(WordPlayActivity.this);
			}
			catch (Exception e) { exception = e; }
			return null;
		}

		protected void onPostExecute(Void result)
		{

			// Reset the dictionaries to ENABLE in preferences
			// and in the spinners
//			SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
//			SharedPreferences.Editor editor = prefs.edit();
//			int defaultDict = DictionaryType.DICTIONARY_ENABLE.ordinal();
//			editor.putInt("dictionary_dict", defaultDict - 1);
//			dictSpinner.setSelection(defaultDict - 1);
//			editor.putInt("anagrams_dict", defaultDict - 1);
//			anagramSpinner.setSelection(defaultDict - 1);
//			editor.putInt("wordjudge_dict", defaultDict - 1);
//			wjSpinner.setSelection(defaultDict - 1);
//			editor.putInt("crosswords_dict", defaultDict - 1);
//			crosswordsSpinner.setSelection(defaultDict - 1);
//			editor.commit();

			// Dismiss the dialog
			if (progressDialog != null)
				if (!isFinishing())
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

		if (!isFinishing())
			new AppErrDialog(context, exception, builder.toString()).show();

    }

    //
    // Notification Bar Icon Support
    //

    public void addRestartNotification(Intent startIntent)
    {

    	int icon = 0;
    	Activity activity = this;
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
    	NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
    	manager.cancel(RestartNotificationId);
    }

    //
    // Tab Support Classes
    //

    private static final class DrawerMenuItem {

        public String title;
        public int iconResource;
        public Class<?> itemClass;

        DrawerMenuItem(String title, int iconResource, Class<?> itemClass)
        {
            this.title = title;
            this.iconResource = iconResource;
            this.itemClass = itemClass;
        }
 
    }

    private class MenuAdapter extends BaseAdapter {

        private List<DrawerMenuItem> items;

        public MenuAdapter(List<DrawerMenuItem> items)
        {
            this.items = items;
        }

        @Override
        public int getCount()
        {
            if (items != null)
                return items.size();
            return 0;
        }

        @Override
        public Object getItem(int position) { return items.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {

            DrawerMenuItem item = items.get(position);

            if (convertView == null)
                convertView = getLayoutInflater().inflate(R.layout.menu_row_item, null);

            TextView tv = (TextView) convertView.findViewById(R.id.title);
            ImageView iv = (ImageView) convertView.findViewById(R.id.icon);

            tv.setText(item.title);
            if (item.iconResource != 0)  {
                iv.setVisibility(View.VISIBLE);
                iv.setImageResource(item.iconResource);
            }
            else
                iv.setVisibility(View.INVISIBLE);

            return convertView;

        }

    }

    //
    // Fragment Management
    //

    private int getFragmentContainer() { return R.id.content; }

    protected Fragment getInitialFragment() { return new AnagramsFragment(); }

    private void switchToFragment(Class<?> cls)
    {
        
        // Process the fragment transaction
        boolean fresh = false;
        
        // Might have had a rotation since the last selection, so we should look up the last added fragment.
        if ((lastAddedTag != null) && (lastAdded == null))
            lastAdded = getSupportFragmentManager().findFragmentByTag(lastAddedTag);
        
        Fragment f = (Fragment) getSupportFragmentManager().findFragmentByTag(cls.getName());
        try {
            if (f == null)  {
                fresh = true;
                f = (Fragment) cls.newInstance();
            }
            replaceStack(f, fresh);
        }
        catch (InstantiationException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    private void replaceStack(Fragment newFragment, boolean freshAdd)
    {

        if ((newFragment == null) || (newFragment == lastAdded))
            return;

        // Clear out the back stack
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_NONE);

        if (lastAdded != null)
            ft.detach(lastAdded);

        if (freshAdd)
            ft.add(getFragmentContainer(), newFragment, newFragment.getClass().getName());
        else
            ft.attach(newFragment);

        ft.commit();

        lastAdded = newFragment;
        lastAddedTag = newFragment.getClass().getName();

    }

}
