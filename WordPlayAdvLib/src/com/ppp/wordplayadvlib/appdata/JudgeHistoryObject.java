package com.ppp.wordplayadvlib.appdata;

public class JudgeHistoryObject {
	
	private String word;
	private boolean state;
	
	public JudgeHistoryObject(String str, boolean s)
	{
		word = str;
		state = s;
	}

	public JudgeHistoryObject(String csvRecord)
	{
		String[] parts = csvRecord.split(":");
		word = parts[0];
		state = Boolean.parseBoolean(parts[1]);
	}
	
	public String getWord() { return word; }
	
	public boolean getState() { return state; }

}
