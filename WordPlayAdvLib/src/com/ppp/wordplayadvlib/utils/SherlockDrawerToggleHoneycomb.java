package com.ppp.wordplayadvlib.utils;

import java.lang.reflect.Method;

import android.R;
import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

class SherlockDrawerToggleHoneycomb {
    private static final String TAG = "ActionBarDrawerToggleHoneycomb";

    private static final int[] THEME_ATTRS = new int[] {
            R.attr.homeAsUpIndicator
    };

    public static Object setActionBarUpIndicator(Object info, SherlockFragmentActivity activity,
            Drawable drawable, int contentDescRes) {
        if (info == null) {
            info = new SetIndicatorInfo(activity);
        }
        final SetIndicatorInfo sii = (SetIndicatorInfo) info;
        if (sii.setHomeAsUpIndicator != null) {
            try {
                final ActionBar actionBar = activity.getSupportActionBar();
                sii.setHomeAsUpIndicator.invoke(actionBar, drawable);
                sii.setHomeActionContentDescription.invoke(actionBar, contentDescRes);
            } catch (Exception e) {
                Log.w(TAG, "Couldn't set home-as-up indicator via JB-MR2 API", e);
            }
        } else if (sii.upIndicatorView != null) {
            sii.upIndicatorView.setImageDrawable(drawable);
        } else {
            Log.w(TAG, "Couldn't set home-as-up indicator");
        }
        return info;
    }

    public static Object setActionBarDescription(Object info, SherlockFragmentActivity activity,
            int contentDescRes) {
        if (info == null) {
            info = new SetIndicatorInfo(activity);
        }
        final SetIndicatorInfo sii = (SetIndicatorInfo) info;
        if (sii.setHomeAsUpIndicator != null) {
            try {
                final ActionBar actionBar = activity.getSupportActionBar();
                sii.setHomeActionContentDescription.invoke(actionBar, contentDescRes);
            } catch (Exception e) {
                Log.w(TAG, "Couldn't set content description via JB-MR2 API", e);
            }
        }
        return info;
    }

    public static Drawable getThemeUpIndicator(Activity activity) {
        final TypedArray a = activity.obtainStyledAttributes(THEME_ATTRS);
        final Drawable result = a.getDrawable(0);
        a.recycle();
        return result;
    }

    private static class SetIndicatorInfo {
        public Method setHomeAsUpIndicator;
        public Method setHomeActionContentDescription;
        public ImageView upIndicatorView;

        SetIndicatorInfo(Activity activity) {
            try {
                setHomeAsUpIndicator = ActionBar.class.getDeclaredMethod("setHomeAsUpIndicator",
                        Drawable.class);
                setHomeActionContentDescription = ActionBar.class.getDeclaredMethod(
                        "setHomeActionContentDescription", Integer.TYPE);

                // If we got the method we won't need the stuff below.
                return;
            } catch (NoSuchMethodException e) {
                // Oh well. We'll use the other mechanism below instead.
            }

            View home = activity.findViewById(android.R.id.home);
//            if (home == null) {
//                home = activity.findViewById(com.ebay.app.R.id.abs__home);
//            }
            
            if (home == null) {
                // Action bar doesn't have a known configuration, an OEM messed with things.
                return;
            }

            final ViewGroup parent = (ViewGroup) home.getParent();
            final int childCount = parent.getChildCount();
            if (childCount != 2) {
                // No idea which one will be the right one, an OEM messed with things.
                return;
            }

            final View first = parent.getChildAt(0);
            final View second = parent.getChildAt(1);
            final View up = first.getId() == android.R.id.home ? second : first;

            if (up instanceof ImageView) {
                // Jackpot! (Probably...)
                upIndicatorView = (ImageView) up;
            }
        }
    }
}
