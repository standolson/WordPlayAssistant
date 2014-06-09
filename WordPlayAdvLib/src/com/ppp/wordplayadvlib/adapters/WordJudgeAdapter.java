package com.ppp.wordplayadvlib.adapters;

import java.util.LinkedList;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.model.JudgeHistory;
import com.ppp.wordplayadvlib.model.JudgeHistoryObject;

public class WordJudgeAdapter extends ArrayAdapter<JudgeHistoryObject> {

	private LinkedList<JudgeHistoryObject> history;
	
	public WordJudgeAdapter(Context ctx, int rowLayoutId, LinkedList<JudgeHistoryObject> items)
	{
		super(ctx, rowLayoutId, items);
		this.history = items;
	}

	public void updateHistory() { history = JudgeHistory.getInstance().getJudgeHistory(); }
	
    public View getView(int position, View convertView, ViewGroup parent)
    {
    	
        View v = convertView;

        if (v == null)  {
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.judge_history, parent, false);
        }

        JudgeHistoryObject item = history.get(position);
        if (item != null)  {

        	ImageView imageView = (ImageView)v.findViewById(R.id.jh_state_image);
        	if (imageView != null)
        		if (item.getState())
        			imageView.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_listitem_thumbsup));
        		else
        			imageView.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_listitem_thumbsdown));

        	TextView wordView = (TextView)v.findViewById(R.id.jh_word);
        	if (wordView != null)  {
        		if (item.getState())
        			wordView.setTextColor(Color.GREEN);
        		else
        			wordView.setTextColor(Color.RED);
        		wordView.setText(item.getWord());
        	}

        }
        
        return v;
        
    }
	
}
