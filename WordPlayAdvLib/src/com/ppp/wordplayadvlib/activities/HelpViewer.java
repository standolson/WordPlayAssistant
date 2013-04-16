package com.ppp.wordplayadvlib.activities;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class HelpViewer extends Activity {
	
    public void onCreate(Bundle savedInstanceState) 
    {

    	WebView webView = new WebView(this);
    	
        super.onCreate(savedInstanceState);
        setContentView(webView);
        
        Bundle extras = getIntent().getExtras();

        // If this something from the help system, show it as white
        // text on black
        String str = extras.getString("HelpText");
        if (str != null)  {
	        str = "<font color=\"white\">" + str + "</font>";
	    	webView.setBackgroundColor(android.R.color.black);
	        webView.loadData(str, "text/html", "utf-8");
	        return;
        }

        // If this is a WifiAuthException, then we want to render
        // the HTML that the user got back
        str = extras.getString("WifiAuthHtml");
        if (str != null)  {
        	webView.loadData(str, "text/html", "utf-8");
        	return;
        }

    }

}
