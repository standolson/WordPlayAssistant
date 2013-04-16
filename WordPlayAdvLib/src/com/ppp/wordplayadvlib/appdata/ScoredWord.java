package com.ppp.wordplayadvlib.appdata;

public class ScoredWord {
	
	private String word;
	private int score;

	public ScoredWord(String str, int x)
	{
		word = str;
		score = x;
	}
	
	public void setWord(String str)  { word = str; }
	
	public void setScore(int x)  { score = x; }
	
	public String getWord()  { return word; }
	
	public int getScore()  { return score; }
	
	public String toString()  { return word + scoreString(); }

	public String scoreString() { return " (" + score + ")"; }

	public boolean equals(Object o)
	{
		if (!(o instanceof ScoredWord))
			return false;
		ScoredWord w = (ScoredWord)o;
		return w.word.equalsIgnoreCase(word);
	}

	public int hashCode() { return word.hashCode(); }

}
