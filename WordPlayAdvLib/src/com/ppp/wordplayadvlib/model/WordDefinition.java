package com.ppp.wordplayadvlib.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Parcel;
import android.os.Parcelable;

import com.ppp.wordplayadvlib.Constants;
import com.ppp.wordplayadvlib.utils.Debug;

public class WordDefinition implements Parcelable {
	
	public String word;
	public String[] definitions;
	private int currentIndex;
	private int maxIndex;
	
	public WordDefinition(String w, int definition_count)
	{
		word = w;
		definitions = new String[definition_count];
		maxIndex = definition_count;
	}
	
	public WordDefinition(String w, StringBuilder resp) throws IOException
	{

		BufferedReader in = new BufferedReader(new StringReader(resp.toString()), Constants.BufSize);
		String data = null;
		String tag = null;
		
		word = w;
		
		while ((data = in.readLine()) != null)  {
			
//			Debug.i("WordDefinition: " + data);
			tag = data.substring(0, 3);
			
			// Number of definitions - Get the number of definitions and instantiate
			// the new WordDefinition object.
			if (tag.equals("150") == true)  {
				
				String p = "^150 (\\d+) definitions retrieved";
				int defn_count;
				
				Pattern pattern = Pattern.compile(p);
				Matcher matcher = pattern.matcher(data);
				if (matcher.find())  {
					defn_count = Integer.parseInt(matcher.group(1));
					definitions = new String[defn_count];
					maxIndex = defn_count;
//					Debug.i("150: " + defn_count + " definitions");
				}
				else
					Debug.i("150: No count found");
				
			}

			// Definition - Read until we find a "." in column #1 indicating the end
			// of the definition.
			else if (tag.equals("151") == true)  {
				
				StringBuilder defn = new StringBuilder();
				
				while ((data = in.readLine()) != null)  {
					try {
						tag = data.substring(0, 1);
						if (tag.equals(".") == true)
							break;
						defn.append(data).append("\n");
					}
					catch (StringIndexOutOfBoundsException e) {
						defn.append("\n");
					}
				}
//				Debug.i("DEFINITION: " + defn.toString());
				if (currentIndex < maxIndex)
					definitions[currentIndex++] = defn.toString();
				else
					Debug.e("Unable to add more definitions");
				
			}
			
			// Unknown response tag
			else {
				Debug.e("WordDefinition: unknown response tag");
			}
			
		}
		
	}
	
	public String getWord() { return word; }
	
	public String[] getDefinitions() { return definitions; }
	
	public String getDefinitionAt(int index) { return definitions[index]; }
	
	public ArrayList<String> getDefinitionsList()
	{
		ArrayList<String> retval = new ArrayList<String>();
		for (int i = 0; i < currentIndex; i += 1)
			retval.add(definitions[i]);		
		return retval;
	}
	
	public void addDefinition(String definition)
	{
		if (currentIndex < maxIndex)
			definitions[currentIndex++] = definition;
	}
	
	public int size() { return currentIndex; }

	//
	// Parcelable
	//

	public static final Parcelable.Creator<WordDefinition> CREATOR = new Parcelable.Creator<WordDefinition>() 
	{
		@Override
		public WordDefinition createFromParcel(Parcel in) { return new WordDefinition(in); }
		@Override
		public WordDefinition[] newArray(int size) { return new WordDefinition[size]; }
	};   

	@Override
	public int describeContents() { return 0; }

	public WordDefinition(Parcel in)
	{
		word = in.readString();
		int len = in.readInt();
		if (len > 0)
			definitions = new String[len];
		in.readStringArray(definitions);
		currentIndex = in.readInt();
		maxIndex = in.readInt();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(word);
		dest.writeInt(definitions != null ? definitions.length : 0);
		if (definitions != null)
			dest.writeStringArray(definitions);
		dest.writeInt(currentIndex);
		dest.writeInt(maxIndex);
	}

}

