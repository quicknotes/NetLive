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
import android.util.Log;

public class SettingsFragment extends PreferenceFragment{
	
	ListPreference notificationDrawerUnitOfMeasurePreference;
    CheckBoxPreference disableCheckBoxPreference;
    MyApplication app;
    SharedPreferences.Editor edit;

    SharedPreferences sharedPref;
    boolean syncConnPrefDisbale;

    ListPreference pollRatePreference;
    CheckBoxPreference activeAppPreference;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		
		notificationDrawerUnitOfMeasurePreference = (ListPreference) findPreference("pref_key_measurement_unit");
		notificationDrawerUnitOfMeasurePreference.setOnPreferenceChangeListener(notificationDrawerUnitOfMeasurePreferenceListener);
		notificationDrawerUnitOfMeasurePreference.setSummary(notificationDrawerUnitOfMeasurePreference.getValue().toString());

        pollRatePreference = (ListPreference) findPreference("pref_key_poll_rate");
        pollRatePreference.setOnPreferenceChangeListener(pollRatePreferenceListener);

        activeAppPreference = (CheckBoxPreference) findPreference("pref_key_active_app");
        activeAppPreference.setOnPreferenceChangeListener(activeAppPreferenceListener);

        disableCheckBoxPreference = (CheckBoxPreference) findPreference("pref_key_auto_start");
        disableCheckBoxPreference.setOnPreferenceChangeListener(disableCheckBoxPreferenceListener);

	}
	

	
	private OnPreferenceChangeListener notificationDrawerUnitOfMeasurePreferenceListener = new OnPreferenceChangeListener(){

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {



            ((ListPreference) preference).setValue(newValue.toString());
            preference.setSummary(newValue.toString());
            getActivity().stopService(new Intent(getActivity(), MainService.class));
            getActivity().startService(new Intent(getActivity(), MainService.class));
            return false;
        }


    };


    private OnPreferenceChangeListener pollRatePreferenceListener = new OnPreferenceChangeListener(){

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ((ListPreference) preference).setValue(newValue.toString());
            getActivity().stopService(new Intent(getActivity(), MainService.class));
            getActivity().startService(new Intent(getActivity(), MainService.class));

            return false;
        }


    };

    private OnPreferenceChangeListener activeAppPreferenceListener = new OnPreferenceChangeListener(){

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ((CheckBoxPreference) preference).setChecked((Boolean)newValue);
            getActivity().stopService(new Intent(getActivity(), MainService.class));
            getActivity().startService(new Intent(getActivity(), MainService.class));


            return false;
        }


    };



    private OnPreferenceChangeListener disableCheckBoxPreferenceListener = new OnPreferenceChangeListener(){

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
//
////            app = new MyApplication();
////            sharedPref = PreferenceManager.getDefaultSharedPreferences(app.getInstance());
////            edit = sharedPref.edit();
//
//
//            if(!disableCheckBoxPreference.isChecked()){// So this runs after I check the box //for some reason, when I check the box, it reads it as unchecked.  So if I throw that not (!) there, we are all set
//                Log.d("disableCheckBoxPreferenceListener", "isChecked");
//                edit.putBoolean("pref_key_auto_start",true);
//                edit.commit();
//                return true;
//
//            }
//            //this will run if i uncheck the box
//            edit.putBoolean("pref_key_auto_start",false);
//            edit.commit();
//            Intent intent = new Intent(getActivity(), MainService.class);
//            getActivity().startService(intent);
//            Log.d("disableCheckBoxPreferenceListener", "NotChecked");
//            getActivity().stopService(new Intent(getActivity(), MainService.class));

            ((CheckBoxPreference) preference).setChecked((Boolean)newValue);
            getActivity().stopService(new Intent(getActivity(), MainService.class));
            getActivity().startService(new Intent(getActivity(), MainService.class));


            return false;

        }
    };



}

	

		
