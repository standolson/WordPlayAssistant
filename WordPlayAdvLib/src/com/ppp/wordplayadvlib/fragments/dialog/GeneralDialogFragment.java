package com.ppp.wordplayadvlib.fragments.dialog;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.ppp.wordplayadvlib.R;

public class GeneralDialogFragment extends DialogFragment {

	public interface GeneralDialogListener {
		public void onGeneralDialogDimissed(String dialogName);
	}

	private View rootView;

	private String dialogName;
	private String listener;

	public static GeneralDialogFragment newInstance(String dialogName,
													String title,
													String message,
													String listener)
	{

		GeneralDialogFragment dialog = new GeneralDialogFragment();

		Bundle args = new Bundle();
		args.putString("dialogName", dialogName);
		args.putString("title", title);
		args.putString("message", message);
		if (listener != null)
			args.putString("altListener", listener);
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
		dialogName = args.getString("dialogName");
		String title = args.getString("title");
		String message = args.getString("message");
		listener = args.getString("altListener");

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

    private void returnToApp()
    {

    	GeneralDialogListener l = null;

    	dismiss();

        if (getActivity() instanceof GeneralDialogListener)
            l = (GeneralDialogListener) getActivity();

        if (listener != null)  {
            Fragment frag = getFragmentManager().findFragmentByTag(listener);
            if (frag instanceof GeneralDialogListener)
                l = (GeneralDialogListener) frag;
        }

        if (l != null)
            l.onGeneralDialogDimissed(dialogName);

    }

}
