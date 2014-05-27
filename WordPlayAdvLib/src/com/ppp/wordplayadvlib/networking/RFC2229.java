package com.ppp.wordplayadvlib.networking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ppp.wordplayadvlib.Constants;
import com.ppp.wordplayadvlib.exceptions.WordPlayException;
import com.ppp.wordplayadvlib.model.DictionaryType;
import com.ppp.wordplayadvlib.utils.Debug;

public class RFC2229 {
	
	Socket s = null;
	PrintWriter out = null;
	BufferedReader in = null;
	boolean connected = false;

	//
	// Public Entry Points
	//

	public StringBuilder defineWord(String word, DictionaryType dict) throws IOException, WordPlayException
	{
		
		StringBuilder buf = new StringBuilder();
		
		hello();
		buf = sendAndRecv("define " + dict.toString() + " \"" + word + "\"");
		Debug.i(dict.toString() + " REPLY: " + buf.toString());
		quit();
		
		return buf;

	}
		
	public StringBuilder matchWord(String word, DictionaryType dict, boolean callerConnected)
		throws IOException, WordPlayException
	{
		
		StringBuilder buf = new StringBuilder();
		String newWord = word.replace("?", ".");
		
		if (!callerConnected)
			hello();
		buf = sendAndRecv("match " + dict.toString() + " re \"" + newWord + "\"");
		Debug.i(dict.toString() + " REPLY: " + buf.toString());
		if (!callerConnected)
			quit();

		return buf;

	}
	
	public StringBuilder thesaurus(String word) throws IOException, WordPlayException
	{
		
		StringBuilder buf = new StringBuilder();
		
		hello();
		buf = sendAndRecv("define moby-thes \"" + word + "\"");
		Debug.i("MOBY-THES REPLY: " + buf.toString());
		quit();
		
		return buf;
	
	}

	//
	// Protocol
	//

	private void connect() throws IOException, WordPlayException
	{

		// Make sure there is a network we can talk to
		NetworkUtils.networkAvailable();

		try {
			Debug.i("CONNECTING");
			s = new Socket("dict.org", 2628);
			s.setSoTimeout(5000);
			connected = true;
			Debug.i("CONNECTED");
		}
		catch (UnknownHostException e) {
			e.printStackTrace();
			throw new IOException();
		} 
		
		if (connected == true)  {
			try	{
				out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"), true);		
				in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"), Constants.BufSize);
			}
			catch (UnsupportedEncodingException e) {
				throw new IOException();
			}
		}
		
	}
	
	private void disconnect() throws IOException
	{
		
		if ((s != null) && (connected == true))  {
			Debug.i("DISCONNECTING");
			s.close();
			connected = false;
		}
		
	}
	
	private StringBuilder hello() throws IOException, WordPlayException
	{
		
		StringBuilder buf = new StringBuilder();
		
		if (connected == false)
			connect();
		
		buf = recv(); // server banner
		Debug.i("LOGIN REPLY: " + buf.toString());
		buf = sendAndRecv("client wordplay/1.0");
		Debug.i("CLIENT ID REPLY: " + buf.toString());	

		return buf;
		
	}
	
	private void quit() throws IOException, WordPlayException
	{
		
		send("quit");
		disconnect();
		
	}
	
	
	private void send(String s) throws IOException, WordPlayException
	{
		
		if (connected == false)
			connect();
		
		Debug.i("SEND: " + s);
		out.println(s);
		
	}

	
	private StringBuilder sendAndRecv(String s) throws IOException, WordPlayException
	{
		
		send(s);
		return recv();
		
	}
	
	
	private StringBuilder recv() throws IOException
	{
		
		String data = null;
		StringBuilder buf = new StringBuilder();
		String tag = null;
		
		while ((data = in.readLine()) != null)  {

			Debug.i("RECV: " + data);
			
			try {
				
				tag = data.substring(0, 3);
				//Debug.i("RESP: '" + data + "'");
				
				// Login response
				if (tag.equals("220") == true)  {
					buf.append(data).append("\n");
					break;
				}
				
				// Number of definitions
				else if (tag.equals("150") == true)  {
					buf.append(data).append("\n");
					continue;
				}
				
				// Number of words
				else if (tag.equals("152") == true)  {
					buf.append(data).append("\n");
					continue;
				}
				
				// OK - done
				else if (tag.equals("250") == true)  {
					break;
				}
				
				// No such word found
				else if (tag.equals("552") == true)  {
					buf.append("No matching words found");
					break;
				}
				
				else {
					buf.append(data).append("\n");
				}
				
			}
			catch (StringIndexOutOfBoundsException e) {
				// Ignore as we want to look for valid response codes.
				// These lines are just appended to the result.
				buf.append(data).append("\n");
			}

		}
		
		return buf;
	
	}
		
	//
	// Response Parsing
	//

	public static ArrayList<String> parseWordList(StringBuilder resp) throws IOException
	{

		ArrayList<String> retval = new ArrayList<String>();
		String data = null;
		String tag = null;
		String p = null;

		if (resp == null)
			return retval;

		BufferedReader in = new BufferedReader(new StringReader(resp.toString()), Constants.BufSize);
		while ((data = in.readLine()) != null)  {
			
			Pattern pattern = null;
			Matcher matcher = null;

			if (Thread.currentThread().isInterrupted())  {
				in.close();
				return retval;
			}

//			Debug.i("parseWordList: " + data);
			tag = data.substring(0, 3);
			
			// Number of words
			if (tag.equals("152") == true)  {
				
				p = "^152 (\\d+) matches found";
				
				pattern = Pattern.compile(p);
				matcher = pattern.matcher(data);
				if (matcher.find())  {
//					int word_count = Integer.parseInt(matcher.group(1));
//					Debug.i("parseWordList: " + word_count + " words");
				}
				else {
//					Debug.e("parseWordList: No count found");
					break;
				}
				
				while ((data = in.readLine()) != null)  {
					
					// Look for a "." in column one indicating end of word list
					if (data.equals(".") == true)
						break;
//					Debug.i("parseWordList: " + data);
					
					// Get the word out of the result line
//					p = search_object.getDictionary().toString() + " \"(.+)\"";
					p = ".+ \"(.+)\"";
					pattern = Pattern.compile(p);
					matcher = pattern.matcher(data);
					if (matcher.find())  {
						String word = matcher.group(1);
						retval.add(word);
//						Debug.i("parseWordList WORD: " + word);
					}
					else {
//						Debug.e("parseWordList WORD: No match found");
					}
					
				}
				
			}
			
			// Unknown tag
			else {
//				Debug.e("parseWordList: invalid tag");
				break;
			}
			
		}

		// Close the reader
		in.close();
		
        // Sort the list
        Collections.sort(retval);
        
        // Eliminate duplicates
        for (int i = 0; i < retval.size() - 1;)
        	if (retval.get(i).equals(retval.get(i + 1)) == true)
        		retval.remove(i);
        	else
        		i += 1;
        
		return retval;
		
	}	
	
	public static ArrayList<String> parseThesaurusList(StringBuilder resp) throws IOException
	{

		ArrayList<String> retval = new ArrayList<String>();
		String data = null;
		String tag = null;
		String p = null;

		if (resp == null)
			return retval;

		BufferedReader in = new BufferedReader(new StringReader(resp.toString()), Constants.BufSize);
		while ((data = in.readLine()) != null)  {
			
			Pattern pattern = null;
			Matcher matcher = null;

			if (Thread.currentThread().isInterrupted())  {
				in.close();
				return retval;
			}

//			Debug.i("parseThesaurusList: " + data);
			tag = data.substring(0, 3);
			
			// Number of words
			if (tag.equals("150") == true)  {
				
				p = "^150 (\\d+) definitions retrieved";
				
				pattern = Pattern.compile(p);
				matcher = pattern.matcher(data);
				if (matcher.find())  {
//					int word_count = Integer.parseInt(matcher.group(1));
//					Debug.i("parseThesaurusList: " + word_count + " words");
				}
				else {
//					Debug.e("parseThesaurusList: No count found");
					break;
				}
				
			}
			
			// Main thesaurus response - get word count
			else if (tag.equals("151") == true)  {
				
				data = in.readLine();
				p = "(\\d+) Moby Thesaurus words for";
				pattern = Pattern.compile(p);
				matcher = pattern.matcher(data);
				if (matcher.find())  {
//					int word_count = Integer.parseInt(matcher.group(1));
//					Debug.i("parseThesaurusList: " + word_count + " words found");
				}
				else {
//					Debug.e("parseThesaurusList: No count found");
					break;
				}

				while ((data = in.readLine()) != null)  {
					
					// Look for a "." in column one indicating end of word list
					if (data.equals(".") == true)
						break;

					// Clean up the line and split it into words
					int pos = 0;
					int orig_pos = 0;
					String word_list = data.replace("   ", "").replace(", ", ",");
					while ((pos = word_list.indexOf(",", orig_pos)) != -1)  {
						String word = word_list.substring(orig_pos, pos);
						orig_pos = pos + 1;
//						Debug.i("parseThesaurusList: " + word);
						retval.add(word);
					}

				}
				
			}
			
			// Unknown tag
			else {
//				Debug.e("parseThesaurusList: invalid tag");
				break;
			}
			
		}

		// Close the reader
		in.close();

		return retval;
		
	}
	
}
