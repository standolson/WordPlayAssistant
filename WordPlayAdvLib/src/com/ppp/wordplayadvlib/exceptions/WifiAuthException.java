package com.ppp.wordplayadvlib.exceptions;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.WordPlayApp;

public class WifiAuthException extends Exception {

	private static final long serialVersionUID = 1L;

	private String htmlText;

	public WifiAuthException(String s) { htmlText = s; }

	public String getHtmlText() { return htmlText; }

	public String getMessage() { return WordPlayApp.getInstance().getString(R.string.wifi_auth_error); }

}
