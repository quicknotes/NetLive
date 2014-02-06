package com.richardlucasapps.netlive;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

public class NetworkSpeedWidget extends AppWidgetProvider {

	private PendingIntent service = null; 

    /*This onUpdate is called when the widget is first configured, and also updates over
    the interval defined in the updatePeriodMillis in my widget_details.xml file.
    Though, remember, this can only work every 15 minutes, which is why I must
    start a service, to update every second.
    */


	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);

        /*
        I am unsure what I was doing here.  I think that if there is no WidgetService
        already running, then it will create one.

        So it starts an Alarm Manager that calls the update method of the WidgetService
        every second.


        Note: The Alarm Manager is intended for cases where you want to have your application
        code run at a specific time, even if your application is not currently running. For normal
        timing operations (ticks, timeouts, etc) it is easier and much more efficient to use Handler.
        My Answer: So apparently it is good to stick with an Alarm Manager to update widgets very frequently,
        because it will not wake the device if it is asleep.


        Note: Beginning with API 19 (KITKAT) alarm delivery is inexact: the OS will shift alarms in order to
        minimize wakeups and battery use. There are new APIs to support applications which need strict delivery
        guarantees; see setWindow(int, long, long, PendingIntent) and setExact(int, long, PendingIntent).
        Applications whose targetSdkVersion is earlier than API 19 will continue to see the previous behavior
        in which all alarms are delivered exactly when requested.
        -Don't think this concerns me, because this has to do when the device is asleep.  Though my widget does need
        to update every second, so maybe this is a bit of an issue.

        I got help from here:
        http://www.parallelrealities.co.uk/2011/09/using-alarmmanager-for-updating-android.html
         */

//		final AlarmManager m = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//		final Intent i = new Intent(context, WidgetService.class);
//		if (service == null)
//		{
//			service = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
//		}
//		m.setRepeating(AlarmManager.RTC, System.currentTimeMillis(),1000,service);



		
	}

}	