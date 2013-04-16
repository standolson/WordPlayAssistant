package com.ppp.wordplayadvlib.appdata;

public enum SearchType {

	OPTION_DICTIONARY_EXACT_MATCH,
	OPTION_DICTIONARY_STARTS_WITH,
	OPTION_DICTIONARY_CONTAINS,
	OPTION_DICTIONARY_ENDS_WITH,
	OPTION_CROSSWORDS,
	OPTION_THESAURUS,
	OPTION_ANAGRAMS,
	OPTION_WORD_JUDGE,
	OPTION_UNKNOWN;

	public String toString()
	{
		switch (this) {
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
		}
		return "Unknown";
	}

	public static SearchType fromString(String str)
	{
		if (str.equals("Exact Match"))
			return OPTION_DICTIONARY_EXACT_MATCH;
		if (str.equals("Starts With"))
			return OPTION_DICTIONARY_STARTS_WITH;
		if (str.equals("Contains"))
			return OPTION_DICTIONARY_CONTAINS;
		if (str.equals("Ends With"))
			return OPTION_DICTIONARY_ENDS_WITH;
		if (str.equals("Crossword"))
			return OPTION_CROSSWORDS;
		if (str.equals("Thesaurus"))
			return OPTION_THESAURUS;
		if (str.equals("Anagram"))
			return OPTION_ANAGRAMS;
		return OPTION_UNKNOWN;
	}

	public static SearchType fromInt(int i)
	{
		for (SearchType j : values())
			if (j.ordinal() == i)
				return j;
		return OPTION_UNKNOWN;
	}

}
