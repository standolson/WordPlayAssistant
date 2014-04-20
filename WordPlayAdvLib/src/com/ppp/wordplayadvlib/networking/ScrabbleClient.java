package com.ppp.wordplayadvlib.networking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ppp.wordplayadvlib.Constants;
import com.ppp.wordplayadvlib.appdata.DictionaryType;
import com.ppp.wordplayadvlib.appdata.ScoredWord;
import com.ppp.wordplayadvlib.appdata.WordSortState;
import com.ppp.wordplayadvlib.exceptions.WifiAuthException;
import com.ppp.wordplayadvlib.exceptions.WordPlayException;

public class ScrabbleClient {
	
	private static final String PP_WEB_SERVICE = "http://portlandportable3.appspot.com/";
	
	public ScrabbleClient() {}
	
	private ArrayList<String> readJSONWordList(StringBuilder sb, WordSortState sort)
		throws JSONException
	{

		ArrayList<String> retval = new ArrayList<String>();
		
		JSONArray a = new JSONArray(sb.toString());
		for (int i = 0; i < a.length(); i += 1)  {
			if (Thread.currentThread().isInterrupted())
				return retval;
			retval.add(a.getString(i));
		}

		sortWordList(retval, sort);
		
		return retval;
		
	}

	private ArrayList<ScoredWord> readJSONScoredWordList(StringBuilder sb, WordSortState sort)
		throws JSONException
	{
	
		ArrayList<ScoredWord> retval = new ArrayList<ScoredWord>();
		String word;
		Integer score;
		ScoredWord sword;

		JSONObject a = new JSONObject(sb.toString());
		Iterator<?> i = a.keys();
		while (i.hasNext())  {
			if (Thread.currentThread().isInterrupted())
				return retval;
			word = (String)i.next();
			score = (Integer)a.get(word);
			sword = new ScoredWord(word, score);
			retval.add(sword);
		}

		sortScoredWordList(retval, sort);
		
		return retval;
		
	}

	private boolean readJSONBoolean(StringBuilder sb) throws WifiAuthException
	{

		if (sb.toString().compareTo("true") == 0)
			return true;
		if (sb.toString().compareTo("false") == 0)
			return false;

		throw new WifiAuthException(sb.toString());
		
	}

	private StringBuilder performHttpGet(String args)
		throws IOException, WifiAuthException, WordPlayException
	{
		
		String line;
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(PP_WEB_SERVICE + "anagrams?" + args);

		// Make sure there is a network out there somewhere
		NetworkUtils.networkAvailable();

		// Execute the GET request
		HttpResponse httpResponse = httpClient.execute(httpGet);

		// Read back the results
		StringBuilder sb = new StringBuilder();
		BufferedReader rd = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()), Constants.BufSize);
		while ((line = rd.readLine()) != null)  {
			if (Thread.currentThread().isInterrupted())
				break;
			sb.append(line);
//			Debug.i("performHttpGet: READ '" + line + "'");
		}
		rd.close();

		checkHttpStatus(httpResponse, sb.toString());

		httpGet = null;

		return sb;
		
	}

	private StringBuilder performHttpPost(String args, String[] data)
		throws IOException, WordPlayException, WifiAuthException
	{

		String line;
		HttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(PP_WEB_SERVICE + "judge?" + args);

		JSONArray obj = new JSONArray();
		for (String str : data)
			if (str.length() > 0)
				obj.put(str);		
		httpPost.setEntity(new StringEntity(obj.toString()));

		// Execute the POST request
		HttpResponse httpResponse = httpClient.execute(httpPost);

		StringBuilder sb = new StringBuilder();
		BufferedReader rd = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()), Constants.BufSize);
		while ((line = rd.readLine()) != null)  {
			if (Thread.currentThread().isInterrupted())
				break;
			sb.append(line);
//			Debug.i("performHttpPost: READ '" + line + "'");
		}		
		rd.close();

		checkHttpStatus(httpResponse, sb.toString());

		httpClient = null;

		return sb;

	}

	private void checkHttpStatus(HttpResponse resp, String respText) throws WifiAuthException
	{


//		Debug.i("HTTP Status Code: " + resp.getStatusLine().getStatusCode());
		int statusCode = resp.getStatusLine().getStatusCode();
		if (statusCode != HttpURLConnection.HTTP_OK)
			throw new WifiAuthException(respText);

	}
		
	public ArrayList<String> getAnagrams(String word,
											DictionaryType dict,
											WordSortState sort)
		throws IOException, WifiAuthException, WordPlayException
	{

		String args = null;
		
		args = URLEncoder.encode("action", "UTF-8") + "=" + URLEncoder.encode("anagram", "UTF-8") + "&";
		args += URLEncoder.encode("letters", "UTF-8") + "=" + URLEncoder.encode(word, "UTF-8") + "&";
		args += URLEncoder.encode("dict", "UTF-8") + "=" + URLEncoder.encode(dict.toString(), "UTF-8");
		
		StringBuilder sb = performHttpGet(args);

		try {
			return readJSONWordList(sb, sort);
		}
		catch (Exception e) {
			throw new WifiAuthException(sb.toString());
		}
		
	}
	
	public ArrayList<ScoredWord> getScoredAnagrams(String word,
													DictionaryType dict,
													WordSortState sort)
		throws IOException, WordPlayException, WifiAuthException
	{

		String args = null;
		
		args = URLEncoder.encode("action", "UTF-8") + "=" + URLEncoder.encode("anagram", "UTF-8") + "&";
		args += URLEncoder.encode("letters", "UTF-8") + "=" + URLEncoder.encode(word, "UTF-8") + "&";
		args += URLEncoder.encode("dict", "UTF-8") + "=" + URLEncoder.encode(dict.toString(), "UTF-8") + "&";
		args += URLEncoder.encode("fmt") + "=" + URLEncoder.encode("scored");
		
		StringBuilder sb = performHttpGet(args);
		
		try {
			return readJSONScoredWordList(sb, sort);
		}
		catch (Exception e) {
			throw new WifiAuthException(sb.toString());
		}

	}

	public ArrayList<String> getWordList(String word,
											DictionaryType dict,
											WordSortState sort)
		throws IOException, WordPlayException, WifiAuthException
	{

		String args = null;

		args = URLEncoder.encode("action", "UTF-8") + "=" + URLEncoder.encode("search", "UTF-8") + "&";
		args += URLEncoder.encode("str", "UTF-8") + "=" + URLEncoder.encode(word, "UTF-8") + "&";
		args += URLEncoder.encode("dict", "UTF-8") + "=" + URLEncoder.encode(dict.toString(), "UTF-8");

		StringBuilder sb = performHttpGet(args);

//		throw(new IOException("Bite me!"));
//		throw new WordPlayException("Bite me!", Globals.HTTP_OK, sb.toString());

		try {
			return readJSONWordList(sb, sort);
		}
		catch (Exception e) {
			throw new WifiAuthException(sb.toString());
		}

	}

	public ArrayList<ScoredWord> getScoredWordList(String word,
													DictionaryType dict,
													WordSortState sort)
		throws IOException, WordPlayException, WifiAuthException
	{

		String args = null;
		
		args = URLEncoder.encode("action", "UTF-8") + "=" + URLEncoder.encode("search", "UTF-8") + "&";
		args += URLEncoder.encode("str", "UTF-8") + "=" + URLEncoder.encode(word, "UTF-8") + "&";
		args += URLEncoder.encode("dict", "UTF-8") + "=" + URLEncoder.encode(dict.toString(), "UTF-8") + "&";
		args += URLEncoder.encode("fmt") + "=" + URLEncoder.encode("scored");
	
		StringBuilder sb = performHttpGet(args);
		
//		throw(new SocketException("Bite me!"));
//		throw new WordPlayException("Bite me!", Globals.HTTP_OK, sb.toString());
	
		try {
			return readJSONScoredWordList(sb, sort);
		}
		catch (Exception e) {
			throw new WifiAuthException(sb.toString());
		}
	
	}

	public boolean judgeWord(String word, DictionaryType dict)
		throws IOException, WordPlayException, WifiAuthException
	{

		String args = null;
		
		args = URLEncoder.encode("action", "UTF-8") + "=" + URLEncoder.encode("lookup", "UTF-8") + "&";
		args += URLEncoder.encode("word", "UTF-8") + "=" + URLEncoder.encode(word, "UTF-8") + "&";
		args += URLEncoder.encode("dict", "UTF-8") + "=" + URLEncoder.encode(dict.toString(), "UTF-8");

		StringBuilder sb = performHttpGet(args);

//		throw(new IOException("Bite me!"));
//		throw new WordPlayException("Bite me!", Globals.HTTP_OK, sb.toString());
		
		return readJSONBoolean(sb);

	}
	
	public boolean judgeWordList(String[] wordlist, DictionaryType dict)
		throws IOException, WordPlayException, WifiAuthException
	{

		String args = null;
		
		args = URLEncoder.encode("dict", "UTF-8") + "=" + URLEncoder.encode(dict.toString(), "UTF-8");
		
		StringBuilder sb = performHttpPost(args, wordlist);
	
		return readJSONBoolean(sb);

	}

	public void sortScoredWordList(ArrayList<ScoredWord> list, WordSortState sort)
	{

		if (sort == WordSortState.WORD_SORT_BY_WORD_SCORE)
			Collections.sort(list, new Comparator<ScoredWord>() {
				public int compare(ScoredWord x, ScoredWord y)
				{
					return y.getScore() - x.getScore();		
				}
			});
		else if (sort == WordSortState.WORD_SORT_BY_WORD_LENGTH)
			Collections.sort(list, new Comparator<ScoredWord>() {
				public int compare(ScoredWord x, ScoredWord y)
				{
					return y.getWord().length() - x.getWord().length();
				}
			});
		else
			Collections.sort(list, new Comparator<ScoredWord>() {
				public int compare(ScoredWord x, ScoredWord y)
				{
					String strx = x.getWord();
					String stry = y.getWord();
					return strx.compareTo(stry);					
				}
			});

	}

	public void sortWordList(ArrayList<String> list, WordSortState sort)
	{

		if (sort == WordSortState.WORD_SORT_BY_WORD_LENGTH)
			Collections.sort(list, new Comparator<String>() {
				public int compare(String x, String y)
				{
					return y.length() - x.length();
				}
			});
		else
			Collections.sort(list, new Comparator<String>() {
				public int compare(String x, String y)
				{
					return x.compareTo(y);					
				}
			});

	}

}
