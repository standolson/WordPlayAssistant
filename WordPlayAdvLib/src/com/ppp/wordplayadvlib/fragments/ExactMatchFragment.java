package com.ppp.wordplayadvlib.fragments;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.utils.Utils;

public class ExactMatchFragment extends BaseFragment {

	private boolean menusOn = true;
	private String word;

	public ExactMatchFragment() { super(); }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.exact_match_fragment, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		
	    super.onActivityCreated(savedInstanceState);

		// This fragment has menu items...
		setHasOptionsMenu(true);

		// ...and wants to be retained on reconfiguration
		setRetainInstance(true);
       
        Bundle args = getArguments();
	    String contents = args.getString("ExactMatchResult");
	    menusOn = args.getBoolean("MenusOn");
	    word = args.getString("ExactMatchString");
	    TextView text = (TextView)getActivity().findViewById(R.id.em_textview);
	    text.setText(contents);
	    
        if (menusOn)
        	registerForContextMenu(text);

    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {

    	super.onCreateContextMenu(menu, v, menuInfo);
    	
    	if (v.getId() == R.id.em_textview)  {
    		getActivity().getMenuInflater().inflate(R.menu.exact_match_context, menu);
    		menu.setHeaderTitle("Options");
    	}
    	
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
    	
    	ClipboardManager clippy;
    	Intent intent;
    	TextView text = (TextView)getActivity().findViewById(R.id.em_textview);

    	if (item.getItemId() == R.id.exact_match_copy)  {
			clippy = (ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
			clippy.setText(text.getText());
			return true;
		}
    	else if (item.getItemId() == R.id.exact_match_email)  {
			intent = new Intent(Intent.ACTION_SEND);
			intent.setType("message/rfc822");
			intent.putExtra(Intent.EXTRA_SUBJECT, "Definition of '" + word + "'");
			intent.putExtra(Intent.EXTRA_TEXT, text.getText());
			try {
				startActivity(intent);
			}
			catch (ActivityNotFoundException exception) {
				Utils.configureEmailAlert(getActivity());
			}
			return true;
		}
    	
    	return super.onOptionsItemSelected(item);
    	
    }
    
//	@Override
//	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
//	{
//	    if (menusOn)
//	    	inflater.inflate(R.menu.exact_match_context, menu);
//	}
    
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item)
//	{
//	
//		ClipboardManager clippy;
//		Intent intent;
//		TextView text = (TextView)getActivity().findViewById(R.id.em_textview);
//		
//		if (item.getItemId() == R.id.exact_match_copy)  {
//			clippy = (ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
//			clippy.setText(text.getText());
//		}
//		else if (item.getItemId() == R.id.exact_match_email)  {
//			intent = new Intent(Intent.ACTION_SEND);
//			intent.setType("message/rfc822");
//			intent.putExtra(Intent.EXTRA_SUBJECT, "Definition of '" + word + "'");
//			intent.putExtra(android.content.Intent.EXTRA_TEXT, text.getText());
//			try {
//				startActivity(intent);
//			}
//			catch (ActivityNotFoundException exception) {
//				Utils.configureEmailAlert(getActivity());
//			}
//		}
//		
//		return true;
//		
//	}

}
