package com.ppp.wordplayadvlib.utils;

import java.io.File;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.StatFs;
import android.text.format.Formatter;

import com.ppp.wordplayadvlib.R;

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

}
