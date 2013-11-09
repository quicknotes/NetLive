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
	CheckBoxPreference generalDisablePreference;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		
		notificationDrawerUnitOfMeasurePreference = (ListPreference) findPreference("pref_key_measurement_unit");
		notificationDrawerUnitOfMeasurePreference.setOnPreferenceChangeListener(notificationDrawerUnitOfMeasurePreferenceListener);
		notificationDrawerUnitOfMeasurePreference.setSummary(notificationDrawerUnitOfMeasurePreference.getValue().toString());
		
		;
	}
	

	
	private OnPreferenceChangeListener notificationDrawerUnitOfMeasurePreferenceListener = new OnPreferenceChangeListener(){

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			

			getActivity().startService(new Intent(getActivity(),MainService.class));//for HTC One
			((ListPreference) preference).setValue(newValue.toString());
			preference.setSummary(newValue.toString());
			return false;
		}
		
		
	};
	
	
}
	

		
