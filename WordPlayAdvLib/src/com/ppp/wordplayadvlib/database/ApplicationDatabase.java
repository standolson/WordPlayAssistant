package com.ppp.wordplayadvlib.database;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.ppp.wordplayadvlib.utils.Debug;

public class ApplicationDatabase {

    private SQLiteDatabase db; 
    private Context context;
    private String dbPath;
    private boolean copyFromAssets;

	public ApplicationDatabase(Context context, String path, boolean copyFromAssets)
	{
		this.context = context;
		this.dbPath = path;
		this.copyFromAssets = copyFromAssets;
	}

    public SQLiteDatabase getDb() { return db; }

    public ApplicationDatabase openReadOnly()
    {

    	try {
	    	db = SQLiteDatabase.openDatabase(dbPath, null,
	    										SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
    	}
    	catch (Exception e) {
    		db = null;
    	}

    	return this;

    }

    public void close()
    {
    	if (db != null)
    		db.close();
    	db = null;
    }

    public void createDatabase() throws Exception
    {
 
    	boolean dbExist = checkDatabase();
 
    	// If the database does not exist or has other issues, then we'll create a new
    	// copy of it from the source database in the APK.
    	if (!dbExist)  {
 
    		Debug.d("Creating database '" + dbPath + "'");
 
        	// If we are copying from an existing database in the application's
        	// assets, do so here.
        	if (copyFromAssets)  {
		    	try {
					copyDatabaseFromAssets();
				}
		    	catch (IOException e)  {
		    		deleteDatabase();
		    		throw new Exception("Error copying database: " + e.getMessage());
		    	}
        	}

    	}
 
    }

    private void deleteDatabase()
    {

    	// Don't delete if the database is still open
    	if ((db != null) && (db.isOpen()))
    		return;

    	// Create the new file object and then delete the file
    	Debug.d("Deleting database '" + dbPath + "'");
    	File f = new File(dbPath);
    	f.delete();
    	
    }

    protected boolean checkDatabase()
    {
 
    	SQLiteDatabase cdb = null;

    	// Try to open the application's database.  If we get a handle
    	// back, the DB is there and can be mounted.
    	try {
    		Debug.d("checking database '" + dbPath + "'");
    		cdb = SQLiteDatabase.openDatabase(dbPath,
    											null,
    											SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
    	}
    	catch (SQLiteException e) {}

    	// If we opened, close it
    	if (cdb != null)
    		cdb.close();
 
    	return cdb != null ? true : false;
    	
    }

    private void copyDatabaseFromAssets() throws IOException
    {

    	// Without a context, we cannot open the source copy
    	if (context == null)
    		return;

		Debug.d("Copying database '" + dbPath + "'");

		// Make sure that the full path to the database exists by
		// creating all of the subdirectories if necessary
		File file = new File(dbPath);
		File dir = new File(file.getParent());
		if (file.exists())  {
			if (!dir.isDirectory())
				throw new IOException("Database path exists and is already a file");
		}
		else {
			if (!dir.exists() && !dir.mkdirs())
				throw new IOException("Unable to create database file path: '" + dbPath + "'");
		}

    	// Open the database included in the APK as the input stream.
    	//
        // The name of the input database from the application's asset directory
    	// must have an extension that the APK packager will not compress.
    	// If this file is compressed, the file cannot be read and we cannot
    	// make a local copy.
		String dbName = new File(dbPath).getName();
    	InputStream myInput = context.getAssets().open(dbName + ".mp3");
 
    	// Create the destination database
    	OutputStream myOutput = new FileOutputStream(dbPath);
 
    	// Copy the file
    	byte[] buffer = new byte[65536];
    	int length;
    	int total = 0;
    	while ((length = myInput.read(buffer)) > 0)  {
    		total += length;
    		myOutput.write(buffer, 0, length);
    	}

    	Debug.d("Copied " + total + " bytes to '" + dbPath + "'");

    	myOutput.flush();
    	myOutput.close();
    	myInput.close();
 
    }

}
