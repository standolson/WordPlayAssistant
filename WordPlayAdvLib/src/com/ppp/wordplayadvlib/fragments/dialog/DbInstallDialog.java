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
import com.ppp.wordplayadvlib.WordPlayApp;

public class DbInstallDialog extends DialogFragment {

	public interface DbInstallDialogListener {
		public void startDatabaseInstall();
	}

	private View rootView;
	private DbInstallDialogListener listener;

	public static DbInstallDialog newInstance(boolean isUpgrade)
	{

		DbInstallDialog dialog = new DbInstallDialog();

        Bundle args = new Bundle();
        args.putBoolean("isUpgrade", isUpgrade);
        dialog.setArguments(args);

        return dialog;

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

    	boolean isUpgrade = getArguments().getBoolean("isUpgrade");

    	rootView = inflater.inflate(R.layout.dictionary_install_dialog, null);

    	TextView textView = (TextView) rootView.findViewById(R.id.dictionary_mode_text);
    	String text = String.format(isUpgrade ?
    									getString(R.string.dictionary_upgrade_dialog_text) :
    									getString(R.string.dictionary_install_dialog_text),
    								WordPlayApp.getInstance().isFreeMode() ?
    									" Free" : "",
    								WordPlayApp.appVersionName);
    	textView.setText(text);

    	Button okButton = (Button) rootView.findViewById(R.id.button);
    	okButton.setText(android.R.string.ok);
    	okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v)
			{
				startDbInstall();
			}
		});

    	return rootView;

    }

    @Override
    public void onAttach(Activity activity)
    {
    	super.onAttach(activity);

    	if (activity instanceof DbInstallDialogListener)
    		listener = (DbInstallDialogListener) activity;
    	else
    		listener = null;

    }

    @Override
    public void onDetach()
    {
    	super.onDetach();
    	listener = null;
    }

    private void startDbInstall()
    {
    	dismiss();
    	if (listener != null)
    		listener.startDatabaseInstall();
    }

}
