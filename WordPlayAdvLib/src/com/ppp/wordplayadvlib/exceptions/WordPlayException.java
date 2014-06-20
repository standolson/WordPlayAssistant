package com.ppp.wordplayadvlib.exceptions;

public class WordPlayException extends Exception {

	private int statusCode;
	private String response;

	private static final long serialVersionUID = -3843457052376896154L;

	public WordPlayException(String s)
	{
		super(s);
		statusCode = -1;
		response = null;
	}
	
	public int getStatusCode() { return statusCode; }
	
	public String getResponse() { return response; }
	
}
