package com.ppp.wordplayadvlib.activities;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.BackStackEntry;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.ppp.wordplayadvlib.fragments.hosts.HostFragment;

public class HostActivity extends BaseActivity {
    
    long lastBackPressTime;

    protected Fragment lastAdded;
    protected String lastAddedTag = null;

    //
    // Stack Management
    //
    
    protected int getFragmentContainer() { return -1; }
    
    public void onBackPressed()
    {

        // Might have had a rotation, so lastAdded will be null
        if ((lastAddedTag != null) && (lastAdded == null))
            lastAdded = getSupportFragmentManager().findFragmentByTag(lastAddedTag);
		
        if ((lastAdded != null) &&
					(lastAdded.getChildFragmentManager().getBackStackEntryCount() > 0))  
			popBackStackPlus(lastAdded);
        else
        	super.onBackPressed();

    }

	protected void popBackStackPlus(Fragment hostFrag) 
	{

		// If there is a back-stack, check for relevant special cases.
		FragmentManager cfm = hostFrag.getChildFragmentManager();
		BackStackEntry bse = null;

		int stackIndex = cfm.getBackStackEntryCount() - 1;
		if (stackIndex < 0)
			return;

		cfm.popBackStack();

	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        return super.onOptionsItemSelected(item);
    }
    
    public HostFragment switchToFragment(Class<?> cls, boolean restartIfSameFragment)
    {

        // Process the fragment transaction
        boolean fresh = false;
        
        // Might have had a rotation since the last selection, so we should look up the last added fragment.
        if ((lastAddedTag != null) && (lastAdded == null))
            lastAdded = getSupportFragmentManager().findFragmentByTag(lastAddedTag);
        
        HostFragment hostFragment = (HostFragment) getSupportFragmentManager().findFragmentByTag(cls.getName());
        try {
            if (hostFragment == null)  {
                fresh = true;
                hostFragment = (HostFragment) cls.newInstance();
            }
            if (restartIfSameFragment || !lastAdded.getClass().equals(cls))
            	replaceStack(hostFragment, fresh);
            hostFragment.setHostActivity(this);
        }
        catch (InstantiationException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return hostFragment;

    }

    public void replaceStack(Fragment newFragment, boolean freshAdd)
    {

        if (newFragment == null)
        	return;
        
        if (newFragment == lastAdded)  {
        	if (lastAdded.getChildFragmentManager().getBackStackEntryCount() > 0)
	        	lastAdded.getChildFragmentManager().popBackStack(lastAdded.getChildFragmentManager().getBackStackEntryAt(0).getId(),
	        														FragmentManager.POP_BACK_STACK_INCLUSIVE);
        	return;
        }

        // Clear out the back stack
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        ft.setTransition(FragmentTransaction.TRANSIT_NONE);

        if (lastAdded != null)  {
        	Log.e(getClass().getSimpleName(), "replaceStack: detach " + lastAdded);
            ft.detach(lastAdded);
        }

        if (freshAdd)  {
        	Log.e(getClass().getSimpleName(), "replaceStack: add " + newFragment);
            ft.add(getFragmentContainer(), newFragment, newFragment.getClass().getName());
        }
        else {
        	Log.e(getClass().getSimpleName(), "replaceStack: existing " + newFragment);
            ft.attach(newFragment);
        }

        ft.commit();

        lastAdded = newFragment;
        lastAddedTag = newFragment.getClass().getName();

        // Likely the app has just started, set the HostActivity
        if (lastAdded instanceof HostFragment)
        	((HostFragment) lastAdded).setHostActivity(this);
        
    }
  
    public Fragment lastAddedHost()
    {
		
		// Might have had a rotation, so lastAdded will be null
        if ((lastAddedTag != null) && (lastAdded == null))
            lastAdded = getSupportFragmentManager().findFragmentByTag(lastAddedTag);
        
        return lastAdded;
    	
    }
    
	public Fragment currentDisplayFragment(Fragment lastAddedHost)
	{
        
		Fragment f = null;
        BackStackEntry bse = null; 

        if (lastAddedHost != null)  {

        	// Return either the last fragment on the back-stack or the initial fragment
        	if (lastAdded.getChildFragmentManager().getBackStackEntryCount() > 0)  {
	        	FragmentManager cfm = lastAdded.getChildFragmentManager();
	        	bse = cfm.getBackStackEntryAt(cfm.getBackStackEntryCount() - 1);
	        	f = lastAdded.getChildFragmentManager().findFragmentByTag(bse.getName());
	        }
        	else
        		f = ((HostFragment) lastAdded).getInitialFragment();

        }
        
        return f;
		
	}

	public boolean isDrawerOpen() { return false; }

}
