package com.richardlucasapps.netlive;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;


public class NetworkSpeedWidget extends AppWidgetProvider {

    /*This onUpdate is called when the widget is first configured, and also updates over
    the interval defined in the updatePeriodMillis in my widget_details.xml file.
    Though, remember, this can only work every 15 minutes, which is why I must
    start a service, to update every second.
    */


	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
        MainService.setWidgetExist(true);
		super.onUpdate(context, appWidgetManager, appWidgetIds);



		
	}

}	