package com.ppp.wordplayadvlib.analytics;

import java.util.Locale;

import android.content.Context;
import android.os.Build;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.ppp.wordplayadvlib.WordPlayApp;
import com.ppp.wordplayadvlib.model.SearchObject;
import com.ppp.wordplayadvlib.utils.Debug;

public class Analytics {

	private static Context context;
	private static Tracker tracker;

	// Categories

	public static final String SEARCH = "Search";
	public static final String DATABASE = "Database";
	public static final String HISTORY = "History";
	public static final String ABOUT = "About";

	// Actions

	public static final String EXACT_MATCH = "ExactMatch";
	public static final String STARTS_WITH = "StartsWith";
	public static final String CONTAINS = "Contains";
	public static final String ENDS_WITH = "EndsWith";
	public static final String CROSSWORDS = "Crosswords";
	public static final String THESAURUS = "Thesaurus";
	public static final String ANAGRAM = "Anagram";
	public static final String DATABASE_INSTALL_INTERNAL = "DatabaseInstallInternal";
	public static final String DATABASE_INSTALL_EXTERNAL = "DatabaseInstallExternal";
	public static final String DATABASE_ERROR = "DatabaseError";
	public static final String CLEAR_HISTORY = "ClearHistory";
	public static final String CLEAR_JUDGE_HISTORY = "ClearJudgeHistory";
	public static final String RELEASE_NOTES = "ReleaseNotes";
	public static final String CONTACT_US = "ContactUs";
	public static final String UPGRADE = "Upgrade";
	public static final String DICT_DOT_ORG = "DictDotOrg";

	// Screens

	public static final String ANAGRAMS_SCREEN = "AnagramsScreen";
	public static final String WORD_JUDGE_SCREEN = "WordJudgeScreen";
	public static final String CROSSWORDS_SCREEN = "CrosswordsScreen";
	public static final String DICTIONARY_SCREEN = "DictionaryScreen";
	public static final String THESAURUS_SCREEN = "ThesaurusScreen";
	public static final String ABOUT_SCREEN = "AboutScreen";
	public static final String EXACT_MATCH_SCREEN = "ExactMatchScreen";
	public static final String HISTORY_SCREEN = "HistoryScreen";
	public static final String HELP_SCREEN = "HelpScreen";

	public Analytics(Context c, String trackerId)
	{
		if (WordPlayApp.isGooglePlayServicesOk())  {
			context = c;
			tracker = GoogleAnalytics.getInstance(c).newTracker(trackerId);
		}
		else
			Debug.e("Analytics: Google Play Services failure (" + WordPlayApp.getGooglePlayServicesStatusString() + ")");
	}

	public static void screenView(String screenName)
	{

		// No tracker?
		if (tracker == null)  {
			Debug.e("attempt to send screen view without Analytics initialization");
			return;
		}

		Debug.i("screenView: screen '" + screenName + "'");

		HitBuilders.EventBuilder event = new HitBuilders.EventBuilder();
		tracker.setScreenName(screenName);
		tracker.send(event.build());

		GoogleAnalytics.getInstance(context).dispatchLocalHits();

	}

	public static void sendEvent(String category, String action, String label, long value)
	{

		// No tracker?
		if (tracker == null)  {
			Debug.e("attempt to send tracking event without Analytics initialization");
			return;
		}

		Debug.i("sendEvent: category '" + category + "' action '" + action + "'");
		if ((label != null) && (label.length() > 0))
			Debug.i("           label: '" + label + "'");
		if (value != 0)
			Debug.i("           value: " + value);

		HitBuilders.EventBuilder event = new HitBuilders.EventBuilder();
		event.setCategory(category);
		event.setAction(action);
		event.setLabel(label);
		event.setValue(value);
		addBasicCustomDimensions(event);
		tracker.send(event.build());

		GoogleAnalytics.getInstance(context).dispatchLocalHits();

	}

	public static void sendEvent(String category, String action, SearchObject searchObject, long value)
	{

		// No tracker?
		if (tracker == null)  {
			Debug.e("attempt to send tracking event without Analytics initialization");
			return;
		}

		Debug.i("sendEvent: category '" + category + "' action '" + action + "'");
		if (value != 0)
			Debug.i("           value: " + value);

		HitBuilders.EventBuilder event = new HitBuilders.EventBuilder();
		event.setCategory(category);
		event.setAction(action);
		event.setLabel("EVENT");
		event.setValue(value);
		addBasicCustomDimensions(event);
		addCustomDimensions(event, searchObject);
		tracker.send(event.build());

		GoogleAnalytics.getInstance(context).dispatchLocalHits();

	}

	private static void addBasicCustomDimensions(HitBuilders.EventBuilder event)
	{

		String device = Build.MANUFACTURER + " " + Build.MODEL;
		String osVersion = Build.VERSION.RELEASE;
		String locale = Locale.getDefault().toString();

		event.setCustomDimension(1, device);
		event.setCustomDimension(2, osVersion);
		event.setCustomDimension(3, locale);

	}

	private static void addCustomDimensions(HitBuilders.EventBuilder event, SearchObject searchObject)
	{
		event.setCustomDimension(4, searchObject.searchString);
		event.setCustomDimension(5, searchObject.boardString);
		event.setCustomDimension(6, searchObject.dictionary.toString());
		event.setCustomDimension(7, searchObject.wordScores.toString());
		event.setCustomDimension(8, searchObject.wordSort.toString());
	}

}
