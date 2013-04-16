package com.ppp.wordplayadvlib.dialogs;

import com.ppp.wordplayadvlib.Constants;
import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.exceptions.WordPlayException;
import com.ppp.wordplayadvlib.utils.Utils;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

public class AppErrDialog extends AlertDialog {

	private Context context;
	private Exception exception;
	private String app_data;

	public AppErrDialog(Context ctx, Exception e, String data)
	{
		super(ctx);
		context = ctx;
		exception = e;
		app_data = data;
	}
	
	public AppErrDialog(Context ctx) { super(ctx); }
	
	public void show()
	{

		this.setTitle("Application Error");

		if (exception != null)  {
			if (exception instanceof WordPlayException)  {
				WordPlayException e = (WordPlayException)exception;
				app_data += "\n";
				app_data += "HTTP Status Code: " + e.getStatusCode() + "\n";
				app_data += "HTTP Response: " + e.getResponse() + "\n";
			}
			this.setMessage(exception.getMessage());
		}
		else
			this.setMessage("Unspecified error");
		
		this.setButton(AlertDialog.BUTTON1, "Continue", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {}
		});
		if (exception instanceof WordPlayException)  {
			WordPlayException wpe = (WordPlayException)exception;
			if (wpe.getStatusCode() != -1)
				this.setButton(AlertDialog.BUTTON2, "Report Problem", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) { sendProblemReport(); }
				});
		}
		else
			this.setButton(AlertDialog.BUTTON2, "Report Problem", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) { sendProblemReport(); }
			});

		super.show();

	}
	
	public void showMessage(String message)
	{

		this.setTitle("Application Error!!");
		this.setMessage(message);
		this.setButton(AlertDialog.BUTTON1, "Continue", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {}
		});
		this.setButton(AlertDialog.BUTTON2, "Report Problem", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) { sendProblemReport(); }
		});
		super.show();
		
	}
	
    private void sendProblemReport()
    {

    	Intent intent = new Intent(Intent.ACTION_SEND);
    	StackTraceElement[] stackTrace = exception.getStackTrace();
    	StringBuilder buf = new StringBuilder();
    	final String appName = context.getString(R.string.app_name);

    	intent.setType("message/rfc822");
		intent.putExtra(Intent.EXTRA_EMAIL, new String[] { Constants.EmailAddress });
    	intent.putExtra(Intent.EXTRA_SUBJECT,
    			"Problem Report for " + appName + " v" +
    			Constants.AppMajorVersion + "." + Constants.AppMinorVersion);

    	buf.append(exception.toString() + "\n\n");
    	for (StackTraceElement e : stackTrace)
    		buf.append(e.toString() + "\n");
    	buf.append("\n");

    	if ((app_data != null) && (app_data.length() != 0))
    		buf.append(app_data);
    	intent.putExtra(android.content.Intent.EXTRA_TEXT, buf.toString());
    	
    	dismiss();
    	try {
    		context.startActivity(intent);
    	}
    	catch (ActivityNotFoundException exception) {
    		Utils.configureEmailAlert(context);
    	}

    }

}
