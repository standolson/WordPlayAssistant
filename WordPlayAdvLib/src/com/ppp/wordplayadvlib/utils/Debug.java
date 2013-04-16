package com.ppp.wordplayadvlib.utils;

import android.util.Log;

import static junit.framework.Assert.*;

public class Debug {

	private static final String LOG_TAG = "WordPlay";

	//
	// DEBUG OUTPUT SECTION
	//

	public enum DebugLevel {

		DebugLevelNone,
		DebugLevelWarning,
		DebugLevelInfo,
		DebugLevelDebug,
		DebugLevelVerbose;

		public String toString()
		{
			switch (this)  {
				case DebugLevelNone:
					return "None";
				case DebugLevelWarning:
					return "Warning";
				case DebugLevelInfo:
					return "Info";
				case DebugLevelDebug:
					return "Debug";
				case DebugLevelVerbose:
					return "Verbose";
			}
			return "Unknown";
		}
	}

	// The current debug level.  We always generate logging for errors.
	private static DebugLevel currentLevel = DebugLevel.DebugLevelNone;

	public static void setLogLevel(DebugLevel level)
	{
		currentLevel = level;
	}

	public static boolean isLogLevel(DebugLevel level)
	{
		return currentLevel.ordinal() >= level.ordinal();
	}

	public static void error(String message)
	{
		Log.e(LOG_TAG, message);
	}
	public static void e(String message) { error(message); }

	public static void warn(String message)
	{
		if (currentLevel.ordinal() >= DebugLevel.DebugLevelWarning.ordinal())
			Log.w(LOG_TAG, message);
	}
	public static void w(String message) { warn(message); }

	public static void info(String message)
	{
		if (currentLevel.ordinal() >= DebugLevel.DebugLevelInfo.ordinal())
			Log.i(LOG_TAG, message);
	}
	public static void i(String message) { info(message); }

	public static void verbose(String message)
	{
		if (currentLevel.ordinal() >= DebugLevel.DebugLevelVerbose.ordinal())
			Log.v(LOG_TAG, message);
	}
	public static void v(String message) { verbose(message); }

	public static void debug(String message)
	{
		if (currentLevel.ordinal() >= DebugLevel.DebugLevelDebug.ordinal())
			Log.d(LOG_TAG, message);
	}
	public static void d(String message) { debug(message); }

	//
	// ASSERTION SECTION
	//

	public static boolean assertState = false;

	public static void enableAsserts() { assertState = true; }
	public static void disableAsserts() { assertState = false; }

	public static void trueAssert(String message, boolean value)
	{
		if (assertState)
			assertTrue(message, value);
	}

	//
	// STACK TRACE SECTION
	//

	public static void printStackTrace(Exception e)
	{

		// Dump the stack trace to the system log
		e.printStackTrace();

		// Now dump it to LogCat
		Debug.e("EXCEPTION: " + e);
		for (StackTraceElement elem : e.getStackTrace())
			Debug.e("at " + elem.getClassName() + "." + elem.getMethodName() +
					"(" + elem.getFileName() + ":" + elem.getLineNumber() + ")");

	}

}
