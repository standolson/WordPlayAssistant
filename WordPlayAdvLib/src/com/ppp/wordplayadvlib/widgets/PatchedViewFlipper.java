package com.ppp.wordplayadvlib.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ViewFlipper;

public class PatchedViewFlipper extends ViewFlipper {

	public PatchedViewFlipper(Context context) { super(context); }

	public PatchedViewFlipper(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}


	@Override
	protected void onDetachedFromWindow()
	{
	    try {
	        super.onDetachedFromWindow();
	    }
	    catch (IllegalArgumentException e) {
	        stopFlipping();
	    }
	}

}
