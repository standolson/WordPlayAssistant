package com.ppp.wordplayadvlib.appdata;

import java.util.ArrayList;
import java.util.Date;

import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;

public class SearchObject implements Parcelable {

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

	//
	// Parcelable
	//

	public static final Parcelable.Creator<SearchObject> CREATOR = new Parcelable.Creator<SearchObject>() 
	{
		@Override
		public SearchObject createFromParcel(Parcel in) { return new SearchObject(in); }
		@Override
		public SearchObject[] newArray(int size) { return new SearchObject[size]; }
	};   

	@Override
	public int describeContents() { return 0; }

	public SearchObject(Parcel in)
	{

		int size;

		searchType = SearchType.fromInt(in.readInt());
		dictionary = DictionaryType.fromInt(in.readInt());
		searchString = in.readString();
		boardString = in.readString();
		wordScores = WordScoreState.fromInt(in.readInt());
		wordSort = WordSortState.fromInt(in.readInt());

		definition = in.readParcelable(WordDefinition.class.getClassLoader());
		size = in.readInt();
		wordList = new ArrayList<String>();
		for (int i = 0; i < size; i += 1)
			wordList.add(in.readString());
		size = in.readInt();
		defnList = new ArrayList<String>();
		for (int i = 0; i < size; i += 1)
			defnList.add(in.readString());
		size = in.readInt();
		scoredWordList = new ArrayList<ScoredWord>();
		for (int i = 0; i < size; i += 1)
			scoredWordList.add((ScoredWord) in.readParcelable(ScoredWord.class.getClassLoader()));
		startTime = new Date(in.readLong());
		endTime = new Date(in.readLong());

	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{

		dest.writeInt(searchType.ordinal());
		dest.writeInt(dictionary.ordinal());
		dest.writeString(searchString);
		dest.writeString(boardString);
		dest.writeInt(wordScores.ordinal());
		dest.writeInt(wordSort.ordinal());

		dest.writeParcelable(definition, flags);
		dest.writeInt(wordList.size());
		for (int i = 0; i < wordList.size(); i += 1)
			dest.writeString(wordList.get(i));
		dest.writeInt(defnList.size());
		for (int i = 0; i < defnList.size(); i += 1)
			dest.writeString(defnList.get(i));
		dest.writeInt(scoredWordList.size());
		for (int i = 0; i < scoredWordList.size(); i += 1)
			dest.writeParcelable(scoredWordList.get(i), flags);
		dest.writeLong(startTime.getTime());
		dest.writeLong(endTime.getTime());

	}

}
