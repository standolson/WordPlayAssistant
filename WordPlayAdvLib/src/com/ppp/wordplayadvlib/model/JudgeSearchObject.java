package com.ppp.wordplayadvlib.model;

import android.os.Parcel;
import android.os.Parcelable;

public class JudgeSearchObject implements Parcelable {

	private String searchString;
	private DictionaryType dictionary;
	
	private boolean result;
	private Exception exception;

	public JudgeSearchObject(String str, DictionaryType dict)
	{
		searchString = str;
		dictionary = dict;
	}
	
	public void setSearchString(String str) { searchString = str; }
	public String getSearchString() { return searchString; }
	
	public void setDictionary(DictionaryType dict) { dictionary = dict; }
	public DictionaryType getDictionary() { return dictionary; }

	public void setResult(Boolean b) { result = b; }
	public boolean getResult() { return result; }

	public void setException(Exception e) { exception = e; }
	public Exception getException() { return exception; }

	//
	// Parcelable
	//

	public static final Parcelable.Creator<JudgeSearchObject> CREATOR = new Parcelable.Creator<JudgeSearchObject>() 
	{
		@Override
		public JudgeSearchObject createFromParcel(Parcel in) { return new JudgeSearchObject(in); }
		@Override
		public JudgeSearchObject[] newArray(int size) { return new JudgeSearchObject[size]; }
	};   

	@Override
	public int describeContents() { return 0; }

	public JudgeSearchObject(Parcel in)
	{
		searchString = in.readString();
		dictionary = DictionaryType.fromInt(in.readInt());
		result = Boolean.parseBoolean(in.readString());
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(searchString);
		dest.writeInt(dictionary.ordinal());
		dest.writeString(Boolean.toString(result));
	}
	
}
