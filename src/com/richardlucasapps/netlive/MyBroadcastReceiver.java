package com.richardlucasapps.netlive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class MyBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
    	SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    	boolean syncConnPref = sharedPref.getBoolean("pref_key_auto_start", false);
    	
    	if(syncConnPref == false){
    		 Intent startServiceIntent = new Intent(context, MainService.class);
    	     context.startService(startServiceIntent);
    	}
    }
}
