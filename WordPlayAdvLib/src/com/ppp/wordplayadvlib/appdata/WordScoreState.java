package com.ppp.wordplayadvlib.appdata;

public enum WordScoreState {

	WORD_SCORE_UNKNOWN,
	WORD_SCORE_STATE_OFF,
	WORD_SCORE_STATE_ON;
	
	public String toString()
	{
		switch (this) {
			case WORD_SCORE_UNKNOWN:
				return "Word Scores Unknown";
			case WORD_SCORE_STATE_OFF:
				return "Word Scores Off";
			case WORD_SCORE_STATE_ON:
				return "Word Scores On";
		}
		return null;
	}
	
	public static WordScoreState fromInt(int i)
	{
		for (WordScoreState j : values())
			if (j.ordinal() == i)
				return j;
		return WORD_SCORE_UNKNOWN;
	}
	
	public boolean isScored() { return this == WORD_SCORE_STATE_ON; }
	
	public boolean isUnscored() { return this == WORD_SCORE_STATE_OFF; }
	
}

