package com.ppp.wordplayadvlib.database.schema;

import android.database.Cursor;

public class DatabaseInfo {

	public static final String TABLE_NAME = "database_info";
	public static final String VERSION_COLUMN_NAME = "VERSION";

	public static final int INVALID_DB_VERSION = -1;
	public static final int CURRENT_DB_VERSION = 2;

	public int version;

	public DatabaseInfo(Cursor c)
	{
		try {
			version = c.getInt(c.getColumnIndexOrThrow(VERSION_COLUMN_NAME));
		}
		catch (Exception e) {
			version = INVALID_DB_VERSION;
		}
	}

	public String toString()
	{
		return "version = " + version;
	}

}
