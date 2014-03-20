package com.ppp.wordplayadvlib.fragments.dialog;

import com.ppp.wordplayadvlib.R;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

public class SearchProgressDialogFragment extends DialogFragment
	implements
		OnClickListener
{

	public interface SearchProgressListener {
		public void onProgressCancel();
	}

	private View rootView;

	private String listener;
	private AsyncTask<?, ?, ?> task;

	public static SearchProgressDialogFragment newInstance(String listener, AsyncTask<?, ?, ?> task)
	{

		SearchProgressDialogFragment dialog = new SearchProgressDialogFragment();

		dialog.task = task;

		Bundle args = new Bundle();
		args.putString("listener", listener);
		dialog.setArguments(args);

		return dialog;

	}

	public SearchProgressDialogFragment() { super(); }

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

		rootView = (LinearLayout) inflater.inflate(R.layout.progress_dialog, null);

		Bundle args = getArguments();
		listener = args.getString("listener");

		Button cancelButton = (Button) rootView.findViewById(R.id.button);
		cancelButton.setText(R.string.Cancel);
		cancelButton.setOnClickListener(this);

		return rootView;

	}

	@Override
	public void onClick(View v)
	{
		if (v.getId() == R.id.button)
			performCancel();
	}

    private void performCancel()
    {

        SearchProgressListener l = null;
        
        dismiss();

        if (getActivity() instanceof SearchProgressListener)
            l = (SearchProgressListener) getActivity();

        if ((listener != null) && (l == null))  {
            Fragment frag = getFragmentManager().findFragmentByTag(listener);
            if (frag instanceof SearchProgressListener)
                l = (SearchProgressListener) frag;
        }

        if (l != null)
            l.onProgressCancel();
 
        if (task != null)  {
        	Log.e(getClass().getSimpleName(), "cancelling task");
        	task.cancel(true);
        }

    }

}
