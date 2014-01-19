package com.richardlucasapps.netlive;

import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.richardlucasapps.netlive.R;

public class AppWidgetConfigurePreferencesFragment extends PreferenceFragment{
	
	PreferenceScreen addWidgetPreference;
	AppWidgetConfigure appWidgetConfigure;
	EditTextPreference enterCustomColorPreference;
	int mAppWidgetId;
	boolean bool;
	SharedPreferences sharedPref;
	SharedPreferences.Editor edit;
	
	private static boolean widgetPreferencePaneOpen;

	ListPreference widgetUnitOfMeasure;
	ListPreference widgetFontStyle;
	ListPreference widgetFontColor;
	ListPreference widgetFontSize;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		widgetPreferencePaneOpen = true;
		
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.widget_preference);

		addWidgetPreference = (PreferenceScreen) findPreference("pref_key_widget_add_widget_preference_screen");
		addWidgetPreference.setOnPreferenceClickListener(addWidgetPreferenceListener);
		appWidgetConfigure = new AppWidgetConfigure();
		mAppWidgetId = appWidgetConfigure.getmAppWidgetId();
		
		sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		edit = sharedPref.edit();
		bool = sharedPref.getBoolean("pref_key_widget_display_unit_of_measure", false);
		
		widgetUnitOfMeasure = (ListPreference) findPreference("pref_key_widget_measurement_unit");
		widgetUnitOfMeasure.setOnPreferenceChangeListener(widgetUnitOfMeasureListener);
		widgetUnitOfMeasure.setSummary(widgetUnitOfMeasure.getValue().toString());
		
		widgetFontColor = (ListPreference) findPreference("pref_key_widget_font_color");
		widgetFontColor.setOnPreferenceChangeListener(widgetFontColorListener);
		widgetFontColor.setSummary(widgetFontColor.getEntry().toString());
		
		widgetFontSize = (ListPreference) findPreference("pref_key_widget_font_size");
		widgetFontSize.setOnPreferenceChangeListener(widgetFontSizeListener);
		widgetFontSize.setSummary(widgetFontSize.getEntry().toString());
		
	}
	
	public static boolean isWidgetPreferencePaneOpen() {
		return widgetPreferencePaneOpen;
	}
	
	private OnPreferenceChangeListener widgetFontSizeListener = new OnPreferenceChangeListener(){

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			((ListPreference) preference).setValue(newValue.toString());
			String fontSizeEntry = (String) ((ListPreference) preference).getEntry();
			preference.setSummary(fontSizeEntry);
			return false;
		}
		
		
	};
	
	private OnPreferenceChangeListener widgetUnitOfMeasureListener = new OnPreferenceChangeListener(){

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			((ListPreference) preference).setValue(newValue.toString());
			preference.setSummary(newValue.toString());
			return false;
		}
		
		
	};
	

	
	private OnPreferenceChangeListener widgetFontColorListener = new OnPreferenceChangeListener(){

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			((ListPreference) preference).setValue(newValue.toString());
			String fontColorEntry = (String) ((ListPreference) preference).getEntry();
			preference.setSummary(fontColorEntry);
			return false;
		}
		
		
	};

	
	private OnPreferenceClickListener addWidgetPreferenceListener = new OnPreferenceClickListener(){

		@Override
		public boolean onPreferenceClick(Preference arg0) {
			
			String unitOfMeasure = sharedPref.getString("pref_key_widget_measurement_unit", "Auto (bps, Kbps, Mbps, Gbps)");
			//boolean displayUnitOfMeasure = sharedPref.getBoolean("pref_key_widget_display_unit_of_measure", true);
//			boolean displayTotalValue = sharedPref.getBoolean("pref_key_widget_displayed_values_total", true);
//			boolean displayUploadValue = sharedPref.getBoolean("pref_key_widget_displayed_values_upload", true);
//			boolean displayDownloadValue = sharedPref.getBoolean("pref_key_widget_displayed_values_download", true);
			boolean displayTransferRateLabels = sharedPref.getBoolean("pref_key_widget_transfer_rate_names", true);
			boolean displayActiveApp = sharedPref.getBoolean("pref_key_widget_active_app", true);
			String styleOfFont = sharedPref.getString("pref_key_widget_font_style", null);
			String sizeOfFont = sharedPref.getString("pref_key_widget_font_size", null);
			String colorOfFont = sharedPref.getString("pref_key_widget_font_color", null);
			
			
			edit.putString("pref_key_widget_measurement_unit"+mAppWidgetId, unitOfMeasure);
			//edit.putBoolean("pref_key_widget_display_unit_of_measure"+mAppWidgetId, displayUnitOfMeasure);
			
//			edit.putBoolean("pref_key_widget_displayed_values_total"+mAppWidgetId, displayTotalValue);
//			edit.putBoolean("pref_key_widget_displayed_values_upload"+mAppWidgetId, displayUploadValue);
//			edit.putBoolean("pref_key_widget_displayed_values_download"+mAppWidgetId, displayDownloadValue);
			
			edit.putBoolean("pref_key_widget_transfer_rate_names"+mAppWidgetId, displayTransferRateLabels);
			edit.putBoolean("pref_key_widget_active_app"+mAppWidgetId, displayActiveApp);
			
			edit.putString("pref_key_widget_font_style"+mAppWidgetId, styleOfFont);
			edit.putString("pref_key_widget_font_size"+mAppWidgetId, sizeOfFont);
			edit.putString("pref_key_widget_font_color"+mAppWidgetId, colorOfFont);
			//edit.putString("pref_key_widget_font_color_edit_text"+mAppWidgetId, customColorOfFont);
			
			
			edit.commit();
			Intent result = new Intent();
			result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
			getActivity().setResult(getActivity().RESULT_OK, result);
			getActivity().finish();
			widgetPreferencePaneOpen = false;
			return false;
		}

		
		
	};
	
	public static void initializeActionBar(PreferenceScreen preferenceScreen) {
	    final Dialog dialog = preferenceScreen.getDialog();

	    if (dialog != null) {
	        // Inialize the action bar
	        dialog.getActionBar().setDisplayHomeAsUpEnabled(true);

	        // Apply custom home button area click listener to close the PreferenceScreen because PreferenceScreens are dialogs which swallow
	        // events instead of passing to the activity
	        // Related Issue: https://code.google.com/p/android/issues/detail?id=4611
	        View homeBtn = dialog.findViewById(android.R.id.home);

	        if (homeBtn != null) {
	            OnClickListener dismissDialogClickListener = new OnClickListener() {
	                @Override
	                public void onClick(View v) {
	                    dialog.dismiss();
	                }
	            };

	            // Prepare yourselves for some hacky programming
	            ViewParent homeBtnContainer = homeBtn.getParent();

	            // The home button is an ImageView inside a FrameLayout
	            if (homeBtnContainer instanceof FrameLayout) {
	                ViewGroup containerParent = (ViewGroup) homeBtnContainer.getParent();

	                if (containerParent instanceof LinearLayout) {
	                    // This view also contains the title text, set the whole view as clickable
	                    ((LinearLayout) containerParent).setOnClickListener(dismissDialogClickListener);
	                } else {
	                    // Just set it on the home button
	                    ((FrameLayout) homeBtnContainer).setOnClickListener(dismissDialogClickListener);
	                }
	            } else {
	                // The 'If all else fails' default case
	                homeBtn.setOnClickListener(dismissDialogClickListener);
	            }
	        }    
	    }
	}
	
	
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
	    super.onPreferenceTreeClick(preferenceScreen, preference);

	    // If the user has clicked on a preference screen, set up the action bar
	    if (preference instanceof PreferenceScreen) {
	        initializeActionBar((PreferenceScreen) preference);
	    }

	    return false;
	}

	
	
	
}