package com.ppp.wordplayadvlib.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.ppp.wordplayadvlib.R;

public class WordDefinitionsAdapter extends ArrayAdapter<String> {
	
	private String word;

	private LayoutInflater inflater;
	
	public WordDefinitionsAdapter(Context ctx, int rowLayoutId, String word, ArrayList<String> defns)
	{

		super(ctx, rowLayoutId, defns);

		this.word = word;

		this.inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

	}

    public View getView(int position, View convertView, ViewGroup parent)
    {
    	
        View v = convertView;

        if (v == null)
            v = inflater.inflate(R.layout.search_result, parent, false);

        String defn = getItem(position);
        if (defn != null)  {
        	TextView wordView = (TextView)v.findViewById(R.id.sr_word);
        	TextView defnView = (TextView)v.findViewById(R.id.sr_definition);
        	if (wordView != null)
        		wordView.setText(word);
        	if (defnView != null)  {
        		int index = defn.indexOf('\n');
        		if (index != -1)
        			defnView.setText(defn.substring(0, defn.indexOf('\n')));
        		else
        			defnView.setText(defn);
        	}
        }
        
        return v;
        
    }

}
