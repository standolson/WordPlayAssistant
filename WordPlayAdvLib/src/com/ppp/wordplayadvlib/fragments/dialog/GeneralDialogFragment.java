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

public class GeneralDialogFragment extends DialogFragment {

	public interface GeneralDialogListener {
		public void onGeneralDialogDimissed();
	}

	private View rootView;
	private GeneralDialogListener listener;

	public static GeneralDialogFragment newInstance(String title, String message, String listener)
	{

		GeneralDialogFragment dialog = new GeneralDialogFragment();

		Bundle args = new Bundle();
		args.putString("title", title);
		args.putString("message", message);
		if (listener != null)
			args.putString("listener", listener);
		dialog.setArguments(args);

		return dialog;

	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{

		super.onCreate(savedInstanceState);

		// No standard dialog title bar...we've got our own
		setStyle(STYLE_NO_TITLE, R.style.DialogStyle);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{

		rootView = inflater.inflate(R.layout.general_dialog_fragment, null);

		Bundle args = getArguments();
		String title = args.getString("title");
		String message = args.getString("message");

		((TextView) rootView.findViewById(R.id.title)).setText(title);
		((TextView) rootView.findViewById(R.id.dialog_text)).setText(message);

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

    	if (activity instanceof GeneralDialogListener)
    		listener = (GeneralDialogListener) activity;
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
    		listener.onGeneralDialogDimissed();
    }

}
