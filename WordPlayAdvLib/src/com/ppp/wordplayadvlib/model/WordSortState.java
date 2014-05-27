package com.ppp.wordplayadvlib.model;

public enum WordSortState {
	
	WORD_SORT_UNKNOWN,
	WORD_SORT_BY_WORD_LENGTH,
	WORD_SORT_BY_WORD_SCORE,
	WORD_SORT_BY_ALPHA;
	
	public String toString()
	{
		switch (this) {
			case WORD_SORT_UNKNOWN:
				return "Sort Method Unknown";
			case WORD_SORT_BY_WORD_LENGTH:
				return "Sort By Word Length";
			case WORD_SORT_BY_WORD_SCORE:
				return "Sort By Word Score";
			case WORD_SORT_BY_ALPHA:
				return "Sort Alphabetically";
		}
		return null;
	}
	
	
	public static WordSortState fromInt(int i)
	{
		for (WordSortState j : values())
			if (j.ordinal() == i)
				return j;
		return WORD_SORT_UNKNOWN;
	}

}

