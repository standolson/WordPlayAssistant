package com.ppp.wordplayadvlib.fragments.dialog;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.ppp.wordplayadvlib.R;

public class FreeDialog extends DialogFragment {

	public interface FreeDialogListener {
		public void onFreeDialogDimissed();
	}

	private View rootView;
	private FreeDialogListener listener;

	public static FreeDialog newInstance()
	{
		return new FreeDialog();
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{

		super.onCreate(savedInstanceState);

		// No standard dialog title bar...we've got our own
		setStyle(STYLE_NO_TITLE, R.style.DialogStyle);

		// Not cancelable
		setCancelable(false);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{

		rootView = inflater.inflate(R.layout.free_dialog, null);

		TextView tv = (TextView) rootView.findViewById(R.id.dialog_text);
		tv.setText(getString(R.string.free_mode_text, getString(R.string.app_name)));

		Button okButton = (Button) rootView.findViewById(R.id.button);
    	okButton.setText(android.R.string.ok);
    	okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v)
			{
				returnToApp();
			}
		});

		return rootView;

	}

    @Override
    public void onAttach(Activity activity)
    {
    	super.onAttach(activity);

    	if (activity instanceof FreeDialogListener)
    		listener = (FreeDialogListener) activity;
    	else
    		listener = null;

    }

    @Override
    public void onDetach()
    {
    	super.onDetach();
    	listener = null;
    }

    private void returnToApp()
    {
    	dismiss();
    	if (listener != null)
    		listener.onFreeDialogDimissed();
    }

}
