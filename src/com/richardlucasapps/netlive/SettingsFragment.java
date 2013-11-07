package com.richardlucasapps.netlive;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.richardlucasapps.netlive.R;

public class SettingsFragment extends PreferenceFragment{
	
	ListPreference notificationDrawerUnitOfMeasurePreference;
	//ListPreference notificationAreaUnitOfMeasurePreference;
	CheckBoxPreference generalDisablePreference;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		//added for the HTC One
		//getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
		
		notificationDrawerUnitOfMeasurePreference = (ListPreference) findPreference("pref_key_measurement_unit");
		notificationDrawerUnitOfMeasurePreference.setOnPreferenceChangeListener(notificationDrawerUnitOfMeasurePreferenceListener);
		notificationDrawerUnitOfMeasurePreference.setSummary(notificationDrawerUnitOfMeasurePreference.getValue().toString());
		
		//generalDisablePreference.setOnPreferenceChangeListener(generalDisablePreferenceListener);
	}
	
	
//	private OnPreferenceChangeListener generalDisablePreferenceListener = new OnPreferenceChangeListener(){
//		
//	}
	
	/** Sets up the action bar for an {@link PreferenceScreen} */
//	public static void initializeActionBar(PreferenceScreen preferenceScreen) {
//	    final Dialog dialog = preferenceScreen.getDialog();
//
//	    if (dialog != null) {
//	        // Inialize the action bar
//	        dialog.getActionBar().setDisplayHomeAsUpEnabled(true);
//
//	        // Apply custom home button area click listener to close the PreferenceScreen because PreferenceScreens are dialogs which swallow
//	        // events instead of passing to the activity
//	        // Related Issue: https://code.google.com/p/android/issues/detail?id=4611
//	        View homeBtn = dialog.findViewById(android.R.id.home);
//
//	        if (homeBtn != null) {
//	            OnClickListener dismissDialogClickListener = new OnClickListener() {
//	                @Override
//	                public void onClick(View v) {
//	                    dialog.dismiss();
//	                }
//	            };
//
//	            // Prepare yourselves for some hacky programming
//	            ViewParent homeBtnContainer = homeBtn.getParent();
//
//	            // The home button is an ImageView inside a FrameLayout
//	            if (homeBtnContainer instanceof FrameLayout) {
//	                ViewGroup containerParent = (ViewGroup) homeBtnContainer.getParent();
//
//	                if (containerParent instanceof LinearLayout) {
//	                    // This view also contains the title text, set the whole view as clickable
//	                    ((LinearLayout) containerParent).setOnClickListener(dismissDialogClickListener);
//	                } else {
//	                    // Just set it on the home button
//	                    ((FrameLayout) homeBtnContainer).setOnClickListener(dismissDialogClickListener);
//	                }
//	            } else {
//	                // The 'If all else fails' default case
//	                homeBtn.setOnClickListener(dismissDialogClickListener);
//	            }
//	        }    
//	    }
//	}
	
//	 public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference)
//	    {
//	        super.onPreferenceTreeClick(preferenceScreen, preference);
//	        if (preference!=null)
//	            if (preference instanceof PreferenceScreen)
//	                if (((PreferenceScreen)preference).getDialog()!=null)
//	                    ((PreferenceScreen)preference).getDialog().getActionBar().setDisplayHomeAsUpEnabled(true);
//	        return false;
//	    }
	
//	@Override
//	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
//	    super.onPreferenceTreeClick(preferenceScreen, preference);
//
//	    // If the user has clicked on a preference screen, set up the action bar
//	    if (preference instanceof PreferenceScreen) {
//	        initializeActionBar((PreferenceScreen) preference);
//	    }
//
//	    return false;
//	}
	
	private OnPreferenceChangeListener notificationDrawerUnitOfMeasurePreferenceListener = new OnPreferenceChangeListener(){

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			
//			Intent intent = new Intent(MyApplication.getAppContext(), MainService.class);
//			MyApplication.getAppContext().startService(intent);
			getActivity().startService(new Intent(getActivity(),MainService.class));//for HTC One
			((ListPreference) preference).setValue(newValue.toString());
			preference.setSummary(newValue.toString());
			return false;
		}
		
		
	};
	
	
}
	
//	ListPreference listPreference = (ListPreference) findPreference("pref_key_measurement_unit");
////    if(listPreference.getValue()==null) {
////        // to ensure we don't get a null value
////        // set first value by default
////        listPreference.setValueIndex(0);
////    }
//    //listPreference.setSummary(listPreference.getValue().toString());
//    listPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
//       
//		@Override
//		public boolean onPreferenceChange(Preference preference, Object arg1) {
//			preference.setSummary(((ListPreference) preference).getValue());
//			return false;
//		}
//    });
//}
		
