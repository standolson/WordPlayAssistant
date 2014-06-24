package com.ppp.wordplay;

import android.os.Bundle;

import com.crittercism.app.Crittercism;
import com.ppp.wordplayadvlib.WordPlayApp;
import com.ppp.wordplayadvlib.activities.WordPlayActivity;

public class WordPlayPaid extends WordPlayActivity {

    public void onCreate(Bundle savedInstanceState)
    {

    	// Tell the application this is the paid version
    	WordPlayApp.getInstance().setPaidMode();

    	// Do the normal onCreate stuff
        super.onCreate(savedInstanceState);

        // Initialize Crittercism
        if (!WordPlayApp.getInstance().getDebugFlag())
        	Crittercism.initialize(getApplicationContext(), "531e144040ec925d0e000002");

        // Initialiaze Google Analytics
        initGoogleAnalytics("UA-50341453-1");

    }

}