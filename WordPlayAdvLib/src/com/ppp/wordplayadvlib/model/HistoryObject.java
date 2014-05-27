package com.ppp.wordplayadvlib.model;

import android.os.Bundle;

public class HistoryObject {
	
	private SearchType searchType;
	private String searchString;
	private String boardString;
	private DictionaryType searchDict;
	private WordScoreState wordScore;
	private WordSortState wordSort;
	
	public HistoryObject(String searchString,
							String boardString,
							SearchType searchType,
							DictionaryType dictionary,
							WordScoreState wordScore,
							WordSortState wordSort)
	{
		this.searchType = searchType;
		this.searchString = searchString;
		this.boardString = boardString;
		this.searchDict = dictionary;
		this.wordScore = wordScore;
		this.wordSort = wordSort;
	}

	public HistoryObject(Bundle bundle)
	{
		this.searchType = SearchType.fromInt(bundle.getInt("SearchType"));
		this.searchString = bundle.getString("SearchString").toLowerCase();
		if (searchType == SearchType.OPTION_ANAGRAMS)
			this.boardString = bundle.getString("BoardString").toLowerCase();
		else
			this.boardString = "";
		this.searchDict = DictionaryType.fromInt((int) bundle.getInt("Dictionary"));
		if (searchDict == DictionaryType.DICTIONARY_UNKNOWN)
			this.searchDict = DictionaryType.DICTIONARY_DICT_DOT_ORG;
		this.wordScore = WordScoreState.fromInt(bundle.getInt("WordScores"));
		this.wordSort = WordSortState.fromInt(bundle.getInt("WordSort"));
	}

	public HistoryObject(String csvRecord)
	{
		String[] parts = csvRecord.split(":");
		searchString = parts[0];
		boardString = parts[1];
		searchType = SearchType.fromString(parts[2]);
		searchDict = DictionaryType.fromString(parts[3]);
		wordScore = WordScoreState.fromInt(Integer.parseInt(parts[4]));
		wordSort = WordSortState.fromInt(Integer.parseInt(parts[5]));
	}

	public void setSearchType(SearchType type) { searchType = type; }
	public SearchType getSearchType() { return searchType; }
	
	public void setSearchString(String str) { searchString = str; }
	public String getSearchString() { return searchString; }

	public void setBoardString(String str) { boardString = str; }
	public String getBoardString() { return boardString; }
	
	public void setDictionary(int dict) { searchDict = DictionaryType.fromInt(dict); }
	public DictionaryType getDictionary() { return searchDict; }
	
	public void setScoreState(WordScoreState score) { wordScore = score; }
	public WordScoreState getScoreState() { return wordScore; }
	
	public void setSortState(WordSortState sort) { wordSort = sort; }
	public WordSortState getSortState() { return wordSort; }
	
	public String getSearchTypeString()
	{

		if (searchType == null)
			return "Unknown";

		switch (searchType) {
			case OPTION_UNKNOWN:
				return "Unknown";
			case OPTION_DICTIONARY_EXACT_MATCH:
				return "Exact Match";
			case OPTION_DICTIONARY_STARTS_WITH:
				return "Starts With";
			case OPTION_DICTIONARY_CONTAINS:
				return "Contains";
			case OPTION_DICTIONARY_ENDS_WITH:
				return "Ends With";
			case OPTION_CROSSWORDS:
				return "Crossword";
			case OPTION_THESAURUS:
				return "Thesaurus";
			case OPTION_ANAGRAMS:
				return "Anagram";
			case OPTION_WORD_JUDGE:
				return "Unknown";
			default:
				return "Unknown";
		}

	}

	public String getDictionaryString()
	{

		if (searchDict == null)
			return "Unknown Dictionary";

		switch (searchDict)  {
			case DICTIONARY_UNKNOWN:
				return "Unknown Dictionary";
			case DICTIONARY_SCRABBLE_SOWPODS:
				return "SOWPODS" + getScoreAndSortString();
			case DICTIONARY_SCRABBLE_TWL06:
				return "TWL06" + getScoreAndSortString();
			case DICTIONARY_SCRABBLE_TWL98:
				return "TWL98" + getScoreAndSortString();
			case DICTIONARY_SCRABBLE_COLLINS_FEB_2007:
				return "Collins (2/2007)" + getScoreAndSortString();
			case DICTIONARY_SCRABBLE_COLLINS_APR_2007:
				return "Collins (4/2007)" + getScoreAndSortString();
			case DICTIONARY_ENABLE:
				return "ENABLE" + getScoreAndSortString();
			case DICTIONARY_DICT_DOT_ORG:
				return "All DICT.ORG Dictionaries";
			case DICTIONARY_THESAURUS:
				return "Moby II Thesaurus";
		}

		return "Unknown";

	}

	public String getScoreAndSortString()
	{
		
		String retval;
		
		if (wordScore == WordScoreState.WORD_SCORE_STATE_ON)
			retval = " (Scored, ";
		else if (wordScore == WordScoreState.WORD_SCORE_STATE_OFF)
			retval = " (Unscored, ";
		else
			retval = " (Unknown scoring, ";
		
		if (wordSort == WordSortState.WORD_SORT_BY_ALPHA)
			retval += "Sorted alphabetically)";
		else if (wordSort == WordSortState.WORD_SORT_BY_WORD_LENGTH)
			retval += "Sorted by word length)";
		else if (wordSort == WordSortState.WORD_SORT_BY_WORD_SCORE)
			retval += "Sorted by word score)";
		else
			retval += "Unknown sorting)";

		return retval;
		
	}

	public boolean equalTo(HistoryObject elem)
	{
		if ((elem.getSearchString().equals(getSearchString())) &&
			(elem.getBoardString().equals(getBoardString())) &&
			(elem.getSearchType() == getSearchType()) &&
			(elem.getDictionary() == getDictionary()) &&
			(elem.getScoreState() == getScoreState()) &&
			(elem.getSortState() == getSortState()))
			return true;
		return false;
	}

}
