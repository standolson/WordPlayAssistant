package com.ppp.wordplayadvlib.database;

import java.math.BigInteger;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.ppp.wordplayadvlib.WordPlayApp;
import com.ppp.wordplayadvlib.model.DictionaryType;
import com.ppp.wordplayadvlib.model.ScoredWord;
import com.ppp.wordplayadvlib.model.WordSortState;
import com.ppp.wordplayadvlib.networking.ScrabbleClient;

public class ScrabbleDatabaseClient extends ScrabbleClient {

	private static final String[] dictPrimes =
	{
		"7", "59", "29", "31", "2",		// a, b, c, d, e
		"67", "47", "53", "5", "101",	// f, g, h, i, j
		"73", "23", "43", "13", "17",	// k, l, m, n, o
		"41", "97", "11", "3", "19",	// p, q, r, s, t
		"37", "71", "79", "89", "61",	// u, v, w, x, y
		"83"							// z
	};

	private BigInteger getPrimeValue(String word)
	{

		BigInteger retval = new BigInteger("1");
		CharacterIterator iter = new StringCharacterIterator(word);

		for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next())  {
			BigInteger val = new BigInteger(dictPrimes[c - 'a']);
			retval = retval.multiply(val);
		}

		return retval;

	}

	public ArrayList<String> getAnagrams(String word,
											DictionaryType dict,
											WordSortState sort)
	{

		ArrayList<String> retval = new ArrayList<String>();
		BigInteger primeVal;
		WordlistDatabase db =
			(WordlistDatabase) new WordlistDatabase(WordPlayApp.getInstance()).openReadOnly();

		// If there is a wildcard in the word, scan all letters
		if (word.contains(".") || word.contains("?"))  {
			ArrayList<String> sublist = new ArrayList<String>();
			for (char c = 'a'; c <= 'z'; c += 1)  {
				if (Thread.currentThread().isInterrupted())
					return retval;
				String newWord = word.replace('.', c);
				newWord = newWord.replace('?', c);
				primeVal = getPrimeValue(newWord);
				sublist = db.getAnagrams(primeVal, dict);
				retval.addAll(sublist);
			}
			Set<String> mySet = new HashSet<String>(retval);
			retval.clear();
			retval.addAll(mySet);
		}
		else {
			primeVal = getPrimeValue(word);
			retval = db.getAnagrams(primeVal, dict);
		}

		// Sort the result
		sortWordList(retval, sort);

		db.close();

		return retval;

	}

	public ArrayList<ScoredWord> getScoredAnagrams(String word,
													DictionaryType dict,
													WordSortState sort)
	{

		ArrayList<ScoredWord> retval = new ArrayList<ScoredWord>();
		BigInteger primeVal;
		WordlistDatabase db =
			(WordlistDatabase) new WordlistDatabase(WordPlayApp.getInstance()).openReadOnly();

		// If there is a wildcard in the word, scan all letters
		if (word.contains(".") || word.contains("?"))  {
			ArrayList<ScoredWord> sublist = new ArrayList<ScoredWord>();
			for (char c = 'a'; c <= 'z'; c += 1)  {
//				Log.e(getClass().getSimpleName(), "getScoredAnagrams: wildcard '" + c + "'");
				if (Thread.currentThread().isInterrupted())  {
//					Log.e(getClass().getSimpleName(), "getScoredAnagrams: interrupted");
					return retval;
				}
				String newWord = word.replace('.', c);
				newWord = newWord.replace('?', c);
				primeVal = getPrimeValue(newWord);
				sublist = db.getScoredAnagrams(primeVal, dict);
				retval.addAll(sublist);
			}
			Set<ScoredWord> mySet = new HashSet<ScoredWord>(retval);
			retval.clear();
			retval.addAll(mySet);
		}
		else {
			primeVal = getPrimeValue(word);
			retval = db.getScoredAnagrams(primeVal, dict);
		}

		// Sort the result
		sortScoredWordList(retval, sort);

		db.close();

		return retval;

	}

	public ArrayList<String> getWordList(String word,
											DictionaryType dict,
											WordSortState sort)
	{

		ArrayList<String> retval;
		WordlistDatabase db =
			(WordlistDatabase) new WordlistDatabase(WordPlayApp.getInstance()).openReadOnly();

		retval = db.getWordList(word, dict);
		sortWordList(retval, sort);

		db.close();

		return retval;

	}

	public ArrayList<ScoredWord> getScoredWordList(String word,
													DictionaryType dict,
													WordSortState sort)
	{

		ArrayList<ScoredWord> retval;
		WordlistDatabase db =
			(WordlistDatabase) new WordlistDatabase(WordPlayApp.getInstance()).openReadOnly();

		retval = db.getScoredWordList(word, dict);
		sortScoredWordList(retval, sort);

		db.close();

		return retval;

	}

	public boolean judgeWord(String word, DictionaryType dict)
	{
		WordlistDatabase db =
			(WordlistDatabase) new WordlistDatabase(WordPlayApp.getInstance()).openReadOnly();
		boolean retval = db.judgeWord(word, dict);
		db.close();
		return retval;
	}

	public boolean judgeWordList(String[] wordlist, DictionaryType dict)
	{
		WordlistDatabase db =
			(WordlistDatabase) new WordlistDatabase(WordPlayApp.getInstance()).openReadOnly();
		boolean retval = db.judgeWordList(wordlist, dict);
		db.close();
		return retval;
	}

}
