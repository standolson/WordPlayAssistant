package com.ppp.wordplay;

import com.crittercism.app.Crittercism;
import com.ppp.wordplayadvlib.activities.WordPlayActivity;
import com.ppp.wordplayadvlib.WordPlayApp;

import android.os.Bundle;

public class WordPlayFree extends WordPlayActivity {

    public void onCreate(Bundle savedInstanceState)
    {

    	// Tell the application we are the free app
    	WordPlayApp.getInstance().setFreeMode();

    	// Do the normal onCreate stuff
        super.onCreate(savedInstanceState);

        // Initialize Crittercism
        Crittercism.initialize(getApplicationContext(), "531e2cdff7b7da0b4e000001");

    }

}