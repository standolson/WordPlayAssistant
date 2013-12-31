package com.ppp.wordplayadvlib.utils;

import java.io.File;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.StatFs;
import android.text.SpannableString;
import android.text.format.Formatter;
import android.text.style.ForegroundColorSpan;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.appdata.SearchObject;
import com.ppp.wordplayadvlib.appdata.SearchType;

public class Utils {

	public static void configureEmailAlert(Context context)
	{
		AlertDialog alertDialog = new AlertDialog.Builder(context).create();
		alertDialog.setTitle(context.getString(R.string.error_dialog_title));
		alertDialog.setMessage(context.getResources().getString(R.string.email_not_configured));
		alertDialog.setButton(context.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {}
		});
		alertDialog.show();
	}

	public static String getFreeSpaceForFile(Context context, File file)
	{
		StatFs stat = new StatFs(file.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		return Formatter.formatFileSize(context, availableBlocks * blockSize);
	}

	public static SpannableString convertToBoardString(String str,
														String word,
														String boardString,
														SearchObject searchObject)
	{

		SpannableString ss = new SpannableString(str);
		int start = 0;
		int end = 0;
		boolean outerFound = false;

		// Find all of the board letters and make them RED
		if ((boardString != null) && (boardString.length() > 0))  {

			StringBuffer inner = new StringBuffer(boardString);

			CharacterIterator outer = new StringCharacterIterator(str);
			for (char co = outer.first(); co != CharacterIterator.DONE; co = outer.next())  {
	
				boolean innerFound = false;
	
				// If we didn't have match on the previous character, reset the
				// start of the span
				if (!outerFound)
					start = outer.getIndex();
	
				// Look at each of the characters in the board letter string and if we
				// find one, add all of the character styles
				for (int i = 0; i < inner.length(); i += 1)  {

					char ci = inner.charAt(i);
	
					// If we have a match, set a span over the range and remove
					// the letter we found from the list of board letters
					if (ci == co)  {
						innerFound = true;
						end = outer.getIndex() + 1;
//						Debug.e("setting span on '" + str + "' start " + start + " end " + end);
						ss.setSpan(new ForegroundColorSpan(Color.RED), start, end, 0);
						inner.deleteCharAt(i);
						break;
					}
	
				}

				// If there are no more letters in the board string, quit
				if (inner.length() == 0)
					break;

				// Reset the flag so that we pick up a new start location
				// on the next iteration
				outerFound = innerFound;
	
			}

		}

		// Now if there were any wildcard characters in an anagram lookup,
		// we want to find the characters that were used as wildcards and
		// highlight them in GREEN
		String letters = searchObject.getSearchString() + searchObject.getBoardString();
		if ( (searchObject.getSearchType() == SearchType.OPTION_ANAGRAMS) &&
				(letters.contains(".") || letters.contains("?")) )  {

			StringBuffer wordBuffer = new StringBuffer(word);

			// Remove all of the wildcard characters from the set of letters
			letters = letters.replace(".", "").replace("?", "");

			// Iterate over all of the letters removing each from the word
			CharacterIterator iter = new StringCharacterIterator(letters);
			for (char ci = iter.first(); ci != CharacterIterator.DONE; ci = iter.next())  {
				int index = wordBuffer.indexOf(Character.toString(ci));
				if (index == -1)  {
//					Debug.e("wildcard: cannot find '" + ci + "' in '" + word + "'");
					continue;
				}
				wordBuffer.deleteCharAt(index);
			}

			// If there are no wildcards, we can safely return now
			if (wordBuffer.length() == 0)  {
//				Debug.e("wildcard: word '" + word + "' has no wildcards");
				return ss;
			}

//			Debug.e("wildcard: word '" + word + "' possible wildcards: '" + wordBuffer.toString() + "'");

			// Now iterate over the characters we think are wildcards and find
			// one that doesn't have a span on it already and set its span to
			// make it GREEN
			for (int i = 0; i < wordBuffer.length(); i += 1)  {
				
				int index = -1;
				char c = wordBuffer.charAt(i);

				while (true)  {

					// Find the next instance of the character
					index = word.indexOf(c, index + 1);
					if (index == -1)  {
//						Debug.e("wildcard: word '" + word + "' has no letter '" + c + "'");
						break;
					}
//					Debug.e("wildcard: word '" + word + "' found letter '" + c + "' at position " + index);

					// Get the span for that location.  If there are no spans on
					// this location, we can safely add a new span.
					ForegroundColorSpan[] spans = ss.getSpans(index, index, ForegroundColorSpan.class);
					if (spans.length > 0)
						continue;
					ss.setSpan(new ForegroundColorSpan(Color.GREEN), index, index + 1, 0);
					break;

				}

				// If we didn't find the letter, report
//				if (index == -1)
//					Debug.e("wildcard: word '" + word + "' didn't find letter '" + c + "'");

			}

		}

		return ss;

	}

}
