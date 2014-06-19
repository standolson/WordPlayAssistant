package com.ppp.wordplayadvlib.widgets;

import com.ppp.wordplayadvlib.R;
import com.ppp.wordplayadvlib.utils.Utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RelativeLayout;

public class ActionBarSpinner implements OnItemClickListener {

	private Context context;
	private View anchorView;
	private ActionBarSpinnerCallback callback;
	private String[] items;
	private int selection;

	private LayoutInflater inflater;
	private PopupWindow popup;
	private ListView listView;
	private OptionsAdapter optionAdapter;

	public interface ActionBarSpinnerCallback {
		public void onSelection(int selection);
	}

	public ActionBarSpinner(Context context, View anchorView, ActionBarSpinnerCallback callback, String[] items)
	{

		this.context = context;
		this.anchorView = anchorView;
		this.callback = callback;
		this.items = items;

		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

	}

	public void setSelection(int position) { selection = position; }

	public void show()
	{

		// If there are no items to display, do nothing
		if (items == null)
			return;

		// Create the PopupWindow that will hold the view
		popup = new PopupWindow(context);

		// Inflate the ListView the window will show
		listView = (ListView) inflater.inflate(R.layout.popup_spinner_layout, null);

		// Create the adapter for the ListView
		optionAdapter = new OptionsAdapter(items);
		optionAdapter.setSelectedPosition(selection);
		listView.setAdapter(optionAdapter);
		listView.setOnItemClickListener(this);

		// Set the content of the window
		popup.setContentView(listView);
		popup.setWidth(Utils.getScreenIndependantPixels(context, 200));
		popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
		popup.setFocusable(true);

		// Show it
		popup.showAsDropDown(anchorView);

	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{

		selection = position;
		optionAdapter.setSelectedPosition(selection);

		view.postDelayed(new Runnable() {
			@Override
			public void run()
			{
				popup.dismiss();
				if (callback != null)
					callback.onSelection(selection);
			}
		}, 200);

	}

	private class OptionsAdapter extends BaseAdapter {
		
		private String[] list;
		private int selectedPosition;
		
		public OptionsAdapter(String[] list)
		{
			this.list = list;
		}
		
		@Override
		public int getCount() { return list.length; }
	
		@Override
		public Object getItem(int position) { return list[position]; }
	
		@Override
		public long getItemId(int position) { return position; }
	
		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent)
		{

			RelativeLayout layout =
				(RelativeLayout) inflater.inflate(R.layout.popup_spinner_layout_item, null);
			RadioButton rb = (RadioButton)layout.findViewById(R.id.listItem);

			rb.setText(list[position]);

			if (position == selectedPosition)
				rb.setChecked(true);
			else
				rb.setChecked(false);

			return layout;

		}

		public void setSelectedPosition(int index)
		{ 
			selectedPosition = index;
			notifyDataSetChanged();
		}
		
	}

}
