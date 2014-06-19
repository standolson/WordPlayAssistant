package com.ppp.wordplayadvlib.exceptions;

public class WordPlayException extends Exception {

	private int status_code;
	private String response;

	private static final long serialVersionUID = -3843457052376896154L;

	public WordPlayException(String s)
	{
		super(s);
		status_code = -1;
		response = null;
	}
	
	public int getStatusCode() { return status_code; }
	
	public String getResponse() { return response; }
	
}
