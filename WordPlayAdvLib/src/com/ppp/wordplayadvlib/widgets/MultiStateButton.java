package com.ppp.wordplayadvlib.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MultiStateButton extends Button implements OnClickListener {
	
	private static final int IllegalState = -1;

	private String[] stateNames;
	private boolean[] buttonStates;
	private int currentState = IllegalState;
	private View.OnClickListener clickListener = null;
	
	public MultiStateButton(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setOnClickListener(this);
		currentState = IllegalState;
	}

	public void setStateNames(String[] states)
	{
		stateNames = states;
		buttonStates = new boolean[states.length];
		for (int i = 0; i < states.length; i += 1)
			buttonStates[i] = true;
		if (states.length > 0)  {
			currentState = 0;
			setText(stateNames[0]);
		}
	}
	
	public void setButtonState(int button, boolean state)
	{
		
		if (button > stateNames.length)
			return;
		buttonStates[button] = state;
		
		// Count the number of enabled states
		int enabled_count = 0;
		for (int i = 0; i < stateNames.length; i += 1)
			if (buttonStates[i])  {
				enabled_count += 1;
				continue;
			}
		
		// If no states are enabled, disable the button
		if (enabled_count == 0)  {
			setEnabled(false);
			return;
		}
		else
			setEnabled(true);
		
		// Move to the next button state if the button
		// mentioned is being disabled and we are currently
		// on that state.  There is at least one button
		// state currently enabled here.
		if (currentState == button)  {
			while (true)  {
				currentState += 1;
				if (currentState >= stateNames.length)
					currentState = 0;
				if (buttonStates[currentState])  {
					setText(stateNames[currentState]);
					break;
				}
			}
		}

	}
	
	public void onClick(View v)
	{

		while (true)  {
			currentState += 1;
			if (currentState >= stateNames.length)
				currentState = 0;
			if (buttonStates[currentState])
				break;
		}
		
		setText(stateNames[currentState]);
		if (clickListener != null)
			clickListener.onClick(this);
		
	}

	public void setState(int state)
	{

		if (state >= stateNames.length)
			return;
		currentState = state;
		setText(stateNames[currentState]);

	}

	public int getState() { return currentState; }
	
	public void setOnChangeListener(View.OnClickListener listener) { clickListener = listener; }

}
