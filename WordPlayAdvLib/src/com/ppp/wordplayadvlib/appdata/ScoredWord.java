package com.ppp.wordplayadvlib.appdata;

import android.os.Parcel;
import android.os.Parcelable;

public class ScoredWord implements Parcelable {
	
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

	//
	// Parcelable
	//

	public static final Parcelable.Creator<ScoredWord> CREATOR = new Parcelable.Creator<ScoredWord>() 
	{
		@Override
		public ScoredWord createFromParcel(Parcel in) { return new ScoredWord(in); }
		@Override
		public ScoredWord[] newArray(int size) { return new ScoredWord[size]; }
	};   

	@Override
	public int describeContents() { return 0; }

	public ScoredWord(Parcel in)
	{
		word = in.readString();
		score = in.readInt();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(word);
		dest.writeInt(score);
	}

}
