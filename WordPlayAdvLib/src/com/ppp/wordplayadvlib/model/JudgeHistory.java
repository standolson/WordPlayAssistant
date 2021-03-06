package com.ppp.wordplayadvlib.model;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.ListIterator;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.ppp.wordplayadvlib.Constants;
import com.ppp.wordplayadvlib.utils.Debug;

public class JudgeHistory {

	private static JudgeHistory instance;

	// Word Judge History
	private static LinkedList<JudgeHistoryObject> judge_history = new LinkedList<JudgeHistoryObject>();

	private JudgeHistory() {}

	public static JudgeHistory getInstance()
	{
		if (instance == null)
			instance = new JudgeHistory();
		return instance;
	}

	public LinkedList<JudgeHistoryObject> getJudgeHistory() { return judge_history; }

	public void clearJudgeHistory(Activity activity)
	{
		Debug.d("clearJudgeHistory: history cleared");
		judge_history.clear();
		saveJudgeHistory(activity);
	}

	public void saveJudgeHistory(Activity activity)
	{

		SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		StringBuilder judgeBuf = new StringBuilder("");

		ListIterator<JudgeHistoryObject> iterator = getJudgeHistory().listIterator(judge_history.size());
		while (iterator.hasPrevious())  {
			JudgeHistoryObject elem = iterator.previous();
			judgeBuf.append(elem.getWord()).append(":");
			judgeBuf.append(elem.getState());
			judgeBuf.append("\n");
		}

		editor.putString("wordjudgeHistory", judgeBuf.toString().trim());
		Debug.v("SAVE JUDGE_HISTORY = '" + judgeBuf.toString().trim() + "'");

		editor.commit();

	}

	public boolean loadJudgeHistory(Activity activity)
	{

		SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);
		String judgeHistoryStr = prefs.getString("wordjudgeHistory", "");

		Debug.v("LOAD JUDGE_HISTORY = '" + judgeHistoryStr + "'");

		judge_history.clear();

		BufferedReader judgeBuf = new BufferedReader(new StringReader(judgeHistoryStr), Constants.BufSize);
		try {

			String input;
			while ((input = judgeBuf.readLine()) != null)  {
				Log.e(getClass().getSimpleName(), "input = '" + input + "'");
				input = input.trim();
				if ((input == null) || (input.length() == 0) || !input.contains(":"))
					continue;
				if (input.split(":").length != JudgeHistoryObject.JUDGE_HISTORY_ITEM_LEN)
					continue;
				JudgeHistoryObject history = new JudgeHistoryObject(input);
				addJudgeHistory(history);
			}

			return true;

		}
		catch (Exception e) {}

		return false;

	}

	public void addJudgeHistory(JudgeHistoryObject newHistory)
	{
		if (getJudgeHistory().size() >= Constants.MaxJudgeHistory)
			getJudgeHistory().removeLast();
		getJudgeHistory().addFirst(newHistory);
	}

	public void addJudgeHistory(String word, boolean state)
	{	
		JudgeHistoryObject elem = new JudgeHistoryObject(word, state);
		addJudgeHistory(elem);
	}

}
