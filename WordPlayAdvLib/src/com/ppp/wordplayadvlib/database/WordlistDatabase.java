package com.ppp.wordplayadvlib.database;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;

import com.ppp.wordplayadvlib.database.schema.DatabaseInfo;
import com.ppp.wordplayadvlib.database.schema.WordlistWord;
import com.ppp.wordplayadvlib.model.DictionaryType;
import com.ppp.wordplayadvlib.model.ScoredWord;

public class WordlistDatabase extends ApplicationDatabase {

    // Sub-directory of the external storage directory for this app's databases
    private static String DB_PATH = "/%s/databases/";

	public static final String DB_FILE_NAME = "Wordlist";
	public static final int DB_VERSION = 1;

	public WordlistDatabase(Context ctx)
	{
		super(ctx, getDatabaseFilePath(ctx, DB_FILE_NAME) + DB_FILE_NAME, true);
	}

	public static boolean dbInstallsOnExternalStorage()
	{
		return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}

	public static String getDatabaseFilePath(Context context, String dbName)
	{

		// Get the app name
		String appName = context.getPackageName();

    	// If the media is mounted read/write and they paid for the
    	// honor, we can use the SDcard
    	if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
    		return Environment.getExternalStorageDirectory() + String.format(DB_PATH, appName);
    	else
    		return context.getDatabasePath(dbName).getParent() + "/";

	}

	public static void createDatabaseFile(Context context) throws Exception
	{
		try {
			new WordlistDatabase(context).createDatabase();
		}
		catch (Exception e) { throw e; }
	}

	public static void deleteDatabaseFile(Context context)
	{
		String path = getDatabaseFilePath(context, DB_FILE_NAME);
    	File f = new File(path + DB_FILE_NAME);
    	f.delete();		
	}

	public int getDatabaseVersion()
	{

		String queryStr;
		Cursor c;
		int version = DatabaseInfo.INVALID_DB_VERSION;

		if (getDb() == null)
			return version;

		// Create the query string and execute it
		queryStr = "SELECT * FROM " + DatabaseInfo.TABLE_NAME;
		try {
			c = getDb().rawQuery(queryStr, new String[] {});
		}
		catch (Exception e) {
			return version;
		}

		if (c != null)  {
			c.moveToFirst();
			DatabaseInfo info = new DatabaseInfo(c);
			version = info.version;
			c.close();
		}

		return version;

	}

	public ArrayList<ScoredWord> getScoredAnagrams(BigInteger primeValue, DictionaryType dict)
	{

		String queryStr;
		Cursor c;
		int dictMask = dict.getDictionaryMask();
		ArrayList<ScoredWord> retval = new ArrayList<ScoredWord>();

		if (getDb() == null)
			return retval;

		// Create the query string and execute it
		queryStr = "SELECT * FROM " + WordlistWord.TABLE_NAME +
					" WHERE (" + primeValue + " % " + WordlistWord.PRIMEVALUE_COLUMN_NAME + " = 0) AND " +
					"(" + WordlistWord.DICTS_COLUMN_NAME + " & " + dictMask + " = " + dictMask + ")";
		c = getDb().rawQuery(queryStr, new String[] {});

		// Iterate over the results
		c.moveToFirst();
		while (!c.isAfterLast())  {
			if (Thread.currentThread().isInterrupted())
				return retval;
			WordlistWord word = new WordlistWord(c);
			ScoredWord scoredWord = new ScoredWord(word.word, word.score);
			retval.add(scoredWord);
			c.moveToNext();
		}
		c.close();

		return retval;

	}

	public ArrayList<String> getAnagrams(BigInteger primeValue, DictionaryType dict)
	{

		String queryStr;
		Cursor c;
		int dictMask = dict.getDictionaryMask();
		ArrayList<String> retval = new ArrayList<String>();

		if (getDb() == null)
			return retval;

		// Create the query string and execute it
		queryStr = "SELECT * FROM " + WordlistWord.TABLE_NAME +
					" WHERE (" + primeValue + " % " + WordlistWord.PRIMEVALUE_COLUMN_NAME + " = 0) AND " +
					"(" + WordlistWord.DICTS_COLUMN_NAME + " & " + dictMask + " = " + dictMask + ")";
		c = getDb().rawQuery(queryStr, new String[] {});

		// Iterate over the results
		c.moveToFirst();
		while (!c.isAfterLast())  {
			if (Thread.currentThread().isInterrupted())
				return retval;
			WordlistWord word = new WordlistWord(c);
			retval.add(word.word);
			c.moveToNext();
		}
		c.close();

		return retval;

	}

	public boolean judgeWord(String word, DictionaryType dict)
	{

		String queryStr;
		Cursor c;
		int dictMask = dict.getDictionaryMask();
		boolean retval = false;

		if (getDb() == null)
			return retval;

		// Create the query string and execute it
		queryStr = "SELECT * FROM " + WordlistWord.TABLE_NAME +
					" WHERE (" + WordlistWord.WORD_COLUMN_NAME + " = '" + word + "') AND " +
					"(" + WordlistWord.DICTS_COLUMN_NAME + " & " + dictMask + " = " + dictMask + ")";
		c = getDb().rawQuery(queryStr, new String[] {});

		// If there is one row, we found the word
		if (c.getCount() == 1)
			retval = true;
		c.close();

		return retval;

	}

	public boolean judgeWordList(String[] words, DictionaryType dict)
	{

		String queryStr;
		Cursor c;
		int dictMask = dict.getDictionaryMask();
		boolean retval = false;

		if (getDb() == null)
			return retval;

		// Create the query string and execute it
		queryStr = "SELECT * FROM " + WordlistWord.TABLE_NAME + " WHERE (";
		for (int i = 0; i < words.length; i += 1)  {
			queryStr += "(" + WordlistWord.WORD_COLUMN_NAME + " = '" + words[i] + "')";
			if ((words.length > 1) && (i < words.length - 1))
				queryStr += " OR ";
		}
		queryStr += ") AND ";
		queryStr += "(" + WordlistWord.DICTS_COLUMN_NAME + " & " + dictMask + " = " + dictMask + ")";
		c = getDb().rawQuery(queryStr, new String[] {});

		// If there are exactly as many rows as there were input words,
		// we found it
		if (c.getCount() == words.length)
			retval = true;
		c.close();

		return retval;

	}

	public ArrayList<ScoredWord> getScoredWordList(String word, DictionaryType dict)
	{

		Cursor c;
		int dictMask = dict.getDictionaryMask();
		ArrayList<ScoredWord> retval = new ArrayList<ScoredWord>();
		String queryStr;

		if (getDb() == null)
			return retval;

		// Create the query string
		queryStr = "SELECT * FROM " + WordlistWord.TABLE_NAME +
					" WHERE (" + WordlistWord.WORD_COLUMN_NAME + " like '" + word + "')" +
					" AND (" + WordlistWord.DICTS_COLUMN_NAME + " & " + dictMask + " = " + dictMask + ")";
		c = getDb().rawQuery(queryStr, new String[] {});

		// Iterate over the results
		c.moveToFirst();
		while (!c.isAfterLast())  {
			if (Thread.currentThread().isInterrupted())
				return retval;
			WordlistWord w = new WordlistWord(c);
			ScoredWord scoredWord = new ScoredWord(w.word, w.score);
			retval.add(scoredWord);
			c.moveToNext();
		}
		c.close();

		return retval;

	}

	public ArrayList<String> getWordList(String word, DictionaryType dict)
	{

		Cursor c;
		int dictMask = dict.getDictionaryMask();
		ArrayList<String> retval = new ArrayList<String>();
		String queryStr;

		if (getDb() == null)
			return retval;

		// Create the query string
		queryStr = "SELECT * FROM " + WordlistWord.TABLE_NAME +
					" WHERE (" + WordlistWord.WORD_COLUMN_NAME + " like '" + word + "')" +
					" AND (" + WordlistWord.DICTS_COLUMN_NAME + " & " + dictMask + " = " + dictMask + ")";
		c = getDb().rawQuery(queryStr, new String[] {});

		// Iterate over the results
		c.moveToFirst();
		while (!c.isAfterLast())  {
			if (Thread.currentThread().isInterrupted())
				return retval;
			WordlistWord w = new WordlistWord(c);
			retval.add(w.word);
			c.moveToNext();
		}
		c.close();

		return retval;

	}

}
