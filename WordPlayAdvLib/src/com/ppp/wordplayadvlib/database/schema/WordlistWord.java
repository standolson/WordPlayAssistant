package com.ppp.wordplayadvlib.database.schema;

import java.math.BigInteger;

import android.database.Cursor;

public class WordlistWord {

	public static final String TABLE_NAME = "wordlist";
	public static final String WORD_COLUMN_NAME = "WORD";
	public static final String DICTS_COLUMN_NAME = "DICTS";
	public static final String PRIMEVALUE_COLUMN_NAME = "PRIMEVALUE";
	public static final String SCORE_COLUMN_NAME = "SCORE";

	public String word;
	public int dicts;
	public BigInteger primeValue;
	public int score;

	public WordlistWord(Cursor c)
	{
		word = c.getString(c.getColumnIndexOrThrow(WORD_COLUMN_NAME));
		dicts = c.getInt(c.getColumnIndexOrThrow(DICTS_COLUMN_NAME));
		primeValue = new BigInteger(c.getString(c.getColumnIndexOrThrow(PRIMEVALUE_COLUMN_NAME)));
		score = c.getInt(c.getColumnIndexOrThrow(SCORE_COLUMN_NAME));
	}

	public String toString()
	{
		return
			"word: " + word +
			" dicts: " + dicts +
			" primeValue: " + primeValue +
			" score: " + score;
	}

}
