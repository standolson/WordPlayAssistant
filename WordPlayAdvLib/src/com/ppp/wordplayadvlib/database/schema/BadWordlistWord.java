package com.ppp.wordplayadvlib.database.schema;

import android.database.Cursor;

public class BadWordlistWord {

	public static final String TABLE_NAME = "wordlist";
	public static final String WORD_COLUMN_NAME = "WORD";
	public static final String DICTS_COLUMN_NAME = "DICTS";
	public static final String PRIMEVALUE_COLUMN_NAME = "PRIMEVALUE";
	public static final String SCORE_COLUMN_NAME = "SCORE";

	public String word;
	public int dicts;
	public String primeValue;
	public int score;

	public BadWordlistWord(Cursor c)
	{
		word = c.getString(c.getColumnIndexOrThrow(WORD_COLUMN_NAME));
		dicts = c.getInt(c.getColumnIndexOrThrow(DICTS_COLUMN_NAME));
		primeValue = c.getString(c.getColumnIndexOrThrow(PRIMEVALUE_COLUMN_NAME));
		score = c.getInt(c.getColumnIndexOrThrow(SCORE_COLUMN_NAME));
	}

	public String toString()
	{
		return
			"word: " + word +
			" dicts: " + dicts +
			" primeValue: '" + primeValue + "'" +
			" score: " + score;
	}

}
