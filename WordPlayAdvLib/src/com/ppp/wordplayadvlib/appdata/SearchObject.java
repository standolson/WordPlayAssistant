package com.ppp.wordplayadvlib.appdata;

import java.util.ArrayList;
import java.util.Date;

import android.os.Bundle;
import android.os.Handler;

public class SearchObject {

	// input
	public SearchType searchType;
	public DictionaryType dictionary = DictionaryType.DICTIONARY_DICT_DOT_ORG;
	public String searchString;
	public String boardString;
	public WordScoreState wordScores;
	public WordSortState wordSort;
	public Handler searchHandler;

	// output
	private WordDefinition definition;
	private ArrayList<String> wordList;
	private ArrayList<String> defnList;
	private ArrayList<ScoredWord> scoredWordList;
	private Exception exception;

	private Date startTime;
	private Date endTime;
	
	public SearchObject(Bundle b)
	{

		searchType = SearchType.fromInt(b.getInt("SearchType"));
		searchString = b.getString("SearchString").toLowerCase();
	    if (searchType == SearchType.OPTION_ANAGRAMS)
	    	boardString = b.getString("BoardString").toLowerCase();
		dictionary = DictionaryType.fromInt((int)b.getInt("Dictionary"));
		if (dictionary == DictionaryType.DICTIONARY_UNKNOWN)
			dictionary = DictionaryType.DICTIONARY_DICT_DOT_ORG;
		wordScores = WordScoreState.fromInt(b.getInt("WordScores"));
		wordSort = WordSortState.fromInt(b.getInt("WordSort"));

		startTime = new Date();

	}

	public SearchType getSearchType() { return searchType; }
	public DictionaryType getDictionary() { return dictionary; }
	public String getSearchString() { return searchString; }
	public String getBoardString() { return boardString; }
	public WordScoreState getWordScores() { return wordScores; }
	public WordSortState getWordSort() { return wordSort; }
	
	public void setDefinition(WordDefinition d)
	{
		definition = d;
		endTime = new Date();
	}
	public WordDefinition getDefinition() { return definition; }
	
	public void setWordList(ArrayList<String> l)
	{
		wordList = l;
		endTime = new Date();
	}
	public ArrayList<String> getWordList() { return wordList; }
	
	public void setDefinitionList(ArrayList<String> l)
	{
		defnList = l;
		endTime = new Date();
	}
	public ArrayList<String> getDefinitionList() { return defnList; }
	
	public void setScoredWordList(ArrayList<ScoredWord> l)
	{
		scoredWordList = l;
		endTime = new Date();
	}
	public ArrayList<ScoredWord> getScoredWordList() { return scoredWordList; }
	
	public void setException(Exception e)
	{
		exception = e;
		endTime = new Date();
	}
	public Exception getException() { return exception; }
	
	public void setSearchHandler(Handler h) { searchHandler = h; }
	public Handler getSearchHandler() { return searchHandler; }

	public long getElapsedTime() { return endTime.getTime() - startTime.getTime(); }

}
