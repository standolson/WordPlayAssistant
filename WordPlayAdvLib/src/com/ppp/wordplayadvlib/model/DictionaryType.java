package com.ppp.wordplayadvlib.model;

public enum DictionaryType {
	
	DICTIONARY_UNKNOWN,
	DICTIONARY_ENABLE,
	DICTIONARY_SCRABBLE_SOWPODS,
	DICTIONARY_SCRABBLE_TWL06,
	DICTIONARY_SCRABBLE_TWL98,
	DICTIONARY_SCRABBLE_COLLINS_FEB_2007,
	DICTIONARY_SCRABBLE_COLLINS_APR_2007,
	DICTIONARY_DICT_DOT_ORG,
	DICTIONARY_THESAURUS;
	
	public String toString()
	{
		switch (this) {
			case DICTIONARY_UNKNOWN:
				return "Unknown";
			case DICTIONARY_SCRABBLE_SOWPODS:
				return "sowpods";
			case DICTIONARY_SCRABBLE_TWL06:
				return "twl06";
			case DICTIONARY_SCRABBLE_TWL98:
				return "twl98";
			case DICTIONARY_SCRABBLE_COLLINS_FEB_2007:
				return "cwsfeb07";
			case DICTIONARY_SCRABBLE_COLLINS_APR_2007:
				return "cwsapr07";
			case DICTIONARY_ENABLE:
				return "enable";
			case DICTIONARY_DICT_DOT_ORG:
				return "*";
			case DICTIONARY_THESAURUS:
				return "moby-thesaurus";
		}
		return "Unknown";
	}
	
	public static DictionaryType fromString(String str)
	{
		if (str == null)
			return DICTIONARY_UNKNOWN;
		if (str.equals("Unknown"))
			return DICTIONARY_UNKNOWN;
		if (str.equals("sowpods"))
			return DICTIONARY_SCRABBLE_SOWPODS;
		if (str.equals("twl06"))
			return DICTIONARY_SCRABBLE_TWL06;
		if (str.equals("twl98"))
			return DICTIONARY_SCRABBLE_TWL98;
		if (str.equals("cwsfeb07"))
			return DICTIONARY_SCRABBLE_COLLINS_FEB_2007;
		if (str.equals("cwsapr07"))
			return DICTIONARY_SCRABBLE_COLLINS_APR_2007;
		if (str.equals("enable"))
			return DICTIONARY_ENABLE;
		if (str.equals("*"))
			return DICTIONARY_DICT_DOT_ORG;
		if (str.equals("moby-thesaurus"))
			return DICTIONARY_THESAURUS;
		
		return DICTIONARY_UNKNOWN;
	}
	
	public static DictionaryType fromInt(int i)
	{
		for (DictionaryType j : values())
			if (j.ordinal() == i)
				return j;
		return DICTIONARY_UNKNOWN;
	}
	
	public boolean isScrabbleDict()  {
		return this.ordinal() < DICTIONARY_DICT_DOT_ORG.ordinal();
	}
	
	public boolean isNormalDict()  { return !isScrabbleDict(); }
	
	public boolean isThesaurus()  { return this == DICTIONARY_THESAURUS; }

	public int getDictionaryMask()
	{
		switch (this)  {
			case DICTIONARY_UNKNOWN:
				return 0;
			case DICTIONARY_SCRABBLE_SOWPODS:
				return 0x04;
			case DICTIONARY_SCRABBLE_TWL06:
				return 0x01;
			case DICTIONARY_SCRABBLE_TWL98:
				return 0x02;
			case DICTIONARY_SCRABBLE_COLLINS_FEB_2007:
				return 0x08;
			case DICTIONARY_SCRABBLE_COLLINS_APR_2007:
				return 0x10;
			case DICTIONARY_DICT_DOT_ORG:
				return 0;
			case DICTIONARY_THESAURUS:
				return 0;
			case DICTIONARY_ENABLE:
				return 0x20;
		}
		return 0;
	}
	
}

