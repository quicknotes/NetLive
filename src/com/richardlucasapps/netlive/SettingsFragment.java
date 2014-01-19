package com.richardlucasapps.netlive;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.richardlucasapps.netlive.R;

public class SettingsFragment extends PreferenceFragment{
	
	ListPreference notificationDrawerUnitOfMeasurePreference;
    CheckBoxPreference disableCheckBoxPreference;
    MyApplication app;
    SharedPreferences.Editor edit;

    SharedPreferences sharedPref;
    boolean syncConnPrefDisbale;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		
		notificationDrawerUnitOfMeasurePreference = (ListPreference) findPreference("pref_key_measurement_unit");
		notificationDrawerUnitOfMeasurePreference.setOnPreferenceChangeListener(notificationDrawerUnitOfMeasurePreferenceListener);
		notificationDrawerUnitOfMeasurePreference.setSummary(notificationDrawerUnitOfMeasurePreference.getValue().toString());

        disableCheckBoxPreference = (CheckBoxPreference) findPreference("pref_key_auto_start");
        disableCheckBoxPreference.setOnPreferenceChangeListener(disableCheckBoxPreferenceListener);

	}
	

	
	private OnPreferenceChangeListener notificationDrawerUnitOfMeasurePreferenceListener = new OnPreferenceChangeListener(){

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			

			getActivity().startService(new Intent(getActivity(),MainService.class));//for HTC One, pretty sure this line doesn't do anything
			((ListPreference) preference).setValue(newValue.toString());
			preference.setSummary(newValue.toString());
			return false;
		}
		
		
	};

    public OnPreferenceChangeListener disableCheckBoxPreferenceListener = new OnPreferenceChangeListener(){

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            //SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            //SharedPreferences.Editor edit =
            //SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

            //boolean syncConnPrefDisbale = sharedPref.getBoolean("pref_key_auto_start", false);

            app = new MyApplication();
            sharedPref = PreferenceManager.getDefaultSharedPreferences(app.getInstance());
            edit = sharedPref.edit();


            if(!disableCheckBoxPreference.isChecked()){// So this runs after I check the box //for some reason, when I check the box, it reads it as unchecked.  So if I throw that not (!) there, we are all set
                Log.d("disableCheckBoxPreferenceListener", "isChecked");
                edit.putBoolean("pref_key_auto_start",true);
                edit.commit();
                return true;

            }
            //this will run if i uncheck the box
            edit.putBoolean("pref_key_auto_start",false);
            edit.commit();
            Intent intent = new Intent(getActivity(), MainService.class);
            getActivity().startService(intent);
            Log.d("disableCheckBoxPreferenceListener", "NotChecked");

            return true;

        }
    };



}

	

		
