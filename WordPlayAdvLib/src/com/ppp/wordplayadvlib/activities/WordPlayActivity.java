package com.ppp.wordplayadvlib.activities;

import java.io.File;
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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.ppp.wordplayadvlib.Constants;
import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.WordPlayApp;
import com.ppp.wordplayadvlib.appdata.History;
import com.ppp.wordplayadvlib.appdata.JudgeHistory;
import com.ppp.wordplayadvlib.database.WordlistDatabase;
import com.ppp.wordplayadvlib.database.schema.DatabaseInfo;
import com.ppp.wordplayadvlib.dialogs.AppErrDialog;
import com.ppp.wordplayadvlib.fragments.WebViewFragment;
import com.ppp.wordplayadvlib.fragments.hosts.AboutHostFragment;
import com.ppp.wordplayadvlib.fragments.hosts.AnagramsHostFragment;
import com.ppp.wordplayadvlib.fragments.hosts.CrosswordsHostFragment;
import com.ppp.wordplayadvlib.fragments.hosts.DictionaryHostFragment;
import com.ppp.wordplayadvlib.fragments.hosts.HistoryHostFragment;
import com.ppp.wordplayadvlib.fragments.hosts.HostFragment;
import com.ppp.wordplayadvlib.fragments.hosts.ThesaurusHostFragment;
import com.ppp.wordplayadvlib.fragments.hosts.WordJudgeHostFragment;
import com.ppp.wordplayadvlib.utils.Debug;
import com.ppp.wordplayadvlib.utils.Utils;

@SuppressLint("ValidFragment")
public class WordPlayActivity extends HostActivity
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
    private ActionBarDrawerToggle drawerToggle;
	private ListView menuListView;
	private MenuAdapter menuAdapter;

    private String lastItemTitle = null;
    private boolean drawerSeen = false;
    private List<DrawerMenuItem> menuItems;

	private static boolean notificationIconEnabled = false;

	//
	// Activity Methods
	//

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{

		super.onCreate(savedInstanceState);

	    setContentView(R.layout.menu_drawer);

	    ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(getString(R.string.app_name));

        menuItems = new ArrayList<DrawerMenuItem>(5);
        DrawerMenuItem anagramsItem =
        	new DrawerMenuItem(getString(R.string.Anagrams), R.drawable.ic_tab_anagrams, AnagramsHostFragment.class);
        menuItems.add(anagramsItem);
        menuItems.add(new DrawerMenuItem(getString(R.string.WordJudge), R.drawable.ic_tab_wordjudge, WordJudgeHostFragment.class));
        menuItems.add(new DrawerMenuItem(getString(R.string.Dictionary), R.drawable.ic_tab_dictionary, DictionaryHostFragment.class));
        menuItems.add(new DrawerMenuItem(getString(R.string.Thesaurus), R.drawable.ic_tab_thesaurus, ThesaurusHostFragment.class));
        menuItems.add(new DrawerMenuItem(getString(R.string.Crosswords), R.drawable.ic_tab_crosswords, CrosswordsHostFragment.class));
        menuItems.add(new DrawerMenuItem(Constants.BLANK, 0, null));
        menuItems.add(new DrawerMenuItem(getString(R.string.showhistory_menu_str), 0, HistoryHostFragment.class));
        menuItems.add(new DrawerMenuItem(getString(R.string.showabout_menu_str), 0, AboutHostFragment.class));

        // Clear and load history
        History.getInstance().loadHistory(this);
        JudgeHistory.getInstance().loadJudgeHistory(this);

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
        menuAdapter = new MenuAdapter(menuItems);
        menuListView.setAdapter(menuAdapter);
        menuListView.setScrollingCacheEnabled(false);
        menuListView.setOnItemClickListener(this);

        // Create the drawer toggle so that we can do special shit
        // like show the normal drawer icon instead of the ActionBar
        // back icon
        drawerToggle = new ActionBarDrawerToggle(this, menuDrawer, R.drawable.ic_drawer, R.string.app_name, R.string.app_name) {      

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
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }
    
    @Override
    protected void onResume()
    {

        super.onResume();

        initUi();
        refreshHomeIcon();

        // If the drawer has not been seen, show it
        if (!drawerSeen)
            menuDrawer.postDelayed(new Runnable() {
                @Override
                public void run() { menuDrawer.openDrawer(menuListView); }
            }, 500);

    }

    @Override
	public void onStop()
    {

    	super.onStop();

    	History.getInstance().saveHistory(this);
    	JudgeHistory.getInstance().saveJudgeHistory(this);

    }

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState)
	{
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putBoolean("drawerSeen", drawerSeen);
		savedInstanceState.putString("lastItemTitle", lastItemTitle);
	}

    @Override
    public void onBackPressed()
    {

        if (menuDrawer.isDrawerOpen(menuListView))
            menuDrawer.closeDrawer(menuListView);
        else
            super.onBackPressed();
        
        refreshHomeIcon();

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

        if (it.itemClass != null)  {
	        lastItemTitle = it.title;
	        parent.setSelection(position);
	        switchToFragment(it.itemClass, true);
	        menuListView.setItemChecked(position, true);
	        menuDrawer.closeDrawer(menuListView);
			getSupportActionBar().setSubtitle(it.title);
        }

    }

	protected void initUi() 
	{

		// App may have been killed while in the background.
        if ((lastAddedTag != null) && (lastAdded == null))
        	lastAdded = getSupportFragmentManager().findFragmentByTag(lastAddedTag);
		
        // As a last-resort, default to Browse.
		if (lastAdded == null)
			switchToFragment(AnagramsHostFragment.class, true);

        // Set the subtitle to the currently selected tab item
		getSupportActionBar().setSubtitle(lastItemTitle);

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
    	getMenuInflater().inflate(R.menu.search_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {

    	MenuItem item = null;
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

    	// Close the drawer
    	if (isDrawerOpen())
    		menuDrawer.closeDrawer(menuListView);

    	// If there is no help, don't show it
    	if (lastAdded != null)  {
        	HostFragment host = (HostFragment) lastAdded;
        	if (host.getFragmentHelp() == 0)
        		menu.findItem(R.id.showhelp_menu).setVisible(false);
    	}

    	// Let child fragments turn on the "Clear History" item
    	item = menu.findItem(R.id.clearhistory_menu);
    	item.setVisible(false);

    	// If the notification bar is turned off, don't show "Exit"
    	item = menu.findItem(R.id.exit_menu);
    	if (item != null)
    		item.setVisible(prefs.getBoolean("notification_bar", false));

    	return super.onPrepareOptionsMenu(menu);

    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item)
	{

    	switch (item.getItemId())  {

			// Respond to the action bar's Up/Home button
		    case android.R.id.home:

		    	if (isDrawerOpen() && !drawerToggle.isDrawerIndicatorEnabled())
		    		menuDrawer.closeDrawer(menuListView);
		    	
		    	else if (!drawerToggle.isDrawerIndicatorEnabled()) 
		    		if (lastAdded.getChildFragmentManager() != null)
		    			popBackStackPlus(lastAdded);
		    	
		    	refreshHomeIcon();

		}

        // Menu drawer
    	if (drawerToggle.onOptionsItemSelected(item))
            return true;
    	
		// Help
		else if (item.getItemId() == R.id.showhelp_menu)
			showHelp();
	
		// Clear history
		else if (item.getItemId() == R.id.clearhistory_menu)  {
//			History.getInstance().clearHistory();
//			JudgeHistory.getInstance().clearJudgeHistory();
		}
	
		// Dictionaries
		else if (item.getItemId() == R.id.dictionary_menu)
			showDictionaries();

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
	
		// Exit
		else if (item.getItemId() == R.id.exit_menu)  {
			removeNotification();
			finish();
		}
		
		return super.onOptionsItemSelected(item);
		
	}

    //
    // Menu Helpers
    //

    private void showHelp()
    {

    	if (lastAdded == null)
    		return;

    	HostFragment host = (HostFragment) lastAdded;
    	int helpId = host.getFragmentHelp();
    	if (helpId != 0)  {

    		Bundle args = new Bundle();
    		args.putString("content", Utils.getHelpText(this, "Release Notes", helpId));

    		WebViewFragment fragment = new WebViewFragment();
    		fragment.setArguments(args);
    		host.pushToStack(fragment);

    	}

    }

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
        	
        	TextView copyrightText = (TextView)layout.findViewById(R.id.about_dialog_copyright);
        	copyrightText.setText(getString(R.string.copyright));
        	
        	TextView companyNameText = (TextView)layout.findViewById(R.id.about_dialog_company_name);
        	companyNameText.setText(getString(R.string.company_name));
        		
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
        								WordPlayApp.appVersionName);
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
        			String str = Utils.getHelpText(WordPlayActivity.this, "Release Notes", R.raw.release_notes);
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

    private DrawerMenuItem findDrawerItemByTag(String tag)
    {
        for (DrawerMenuItem item : menuItems)
            if (item.itemClass != null && tag.equals(item.itemClass.getName()))
                return item;
        return null;
    }

    private DrawerMenuItem findDrawerItemByTitle(String title)
    {
        for (DrawerMenuItem item : menuItems)
            if (title.equals(item.title))
                return item;
        return null;
    }

    private DrawerMenuItem findDrawerItemByClass(Class<?> cls)
    {
    	for (DrawerMenuItem item : menuItems)
    		if (cls.equals(item.itemClass))
    			return item;
    	return null;
    }

	@Override
	public boolean isDrawerOpen()
	{
		if ((menuDrawer != null) && (menuListView != null))
			return menuDrawer.isDrawerOpen(menuListView);
		return false;
	}
	
	public void closeDrawer()
	{
		if ((menuDrawer != null) && (menuListView != null))
			menuDrawer.closeDrawer(menuListView);
	}

	private void showDrawer()
	{
        menuDrawer.postDelayed(new Runnable() {
            @Override
            public void run()
            {
                menuDrawer.openDrawer(menuListView);   
            }
        }, 500);
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

    protected int getFragmentContainer() { return R.id.content; }

    protected Fragment getInitialFragment() { return new AnagramsHostFragment(); }
    
    public ActionBarDrawerToggle getDrawerToggle() { return drawerToggle; }
    public DrawerLayout getDrawerLayout() { return menuDrawer; }
    
    //
    // Home Icon
    //

    private void refreshHomeIcon()
    {
    	menuDrawer.postDelayed(new Runnable() {
            @Override
            public void run()
            {
            	if (lastAdded != null) {
            		
                	if (lastAdded.getChildFragmentManager().getBackStackEntryCount() > 0) 
                		drawerToggle.setDrawerIndicatorEnabled(false);
                	else 
                		drawerToggle.setDrawerIndicatorEnabled(true);
                	
                }
            }
        }, 500);
    }

}
