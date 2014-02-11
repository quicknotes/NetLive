package com.richardlucasapps.netlive;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.TrafficStats;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.preference.PreferenceManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import android.util.Log;
import android.widget.RemoteViews;


/*TODO perhaps disable when screen is off using a broadcast receiver, depending on how much battery this ish uses, though may be a good idea to do this regardless

*/
public class MainService extends Service {

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

	private Long bytesSentSinceBoot;
	private Long bytesReceivedSinceBoot;

	private Long previousBytesSentSinceBoot;
	private Long previousBytesReceivedSinceBoot;

	private Long bytesSentPerSecond;
	private Long bytesReceivedPerSecond;

    private String sentString;
	private String receivedString;

	private String activeApp ="";
	List<AppDataUsage> appDataUsageList;
	int appMonitorCounter;

    private static boolean widgetExist;





    //Handler mHandler;

	int mId;

	NotificationCompat.Builder mBuilder;
	NotificationManager mNotifyMgr;
	Notification notification;
	SharedPreferences sharedPref;


    ScheduledFuture beeperHandle;
    Intent resultIntent;

    Context context;
    ComponentName name;

   //final ScheduledFuture beeperHandle = null;

    UnitConverter converter;
    long pollRate;
    NotificationContentTitleSetter titleSetter;

    String displayValuesText = "";
    String unitMeasurement;
    boolean showActiveApp;
    String contentTitleText="";

    ActiveAppGetter activeAppGetter;

    PowerManager pm;
    //UidDataGetter uidDataGetter;


    @Override
    public void onDestroy(){
        try {
            beeperHandle.cancel(true);
        } catch (NullPointerException e) {
            //The only way there will be a null pointer, is if the disabled preference is checked.  Because if it is, onDestory is called right away, without creating the beeperHandle
        }
        super.onDestroy();

    }

	@Override
	public void onCreate() {
        pm                              = (PowerManager) getSystemService(Context.POWER_SERVICE);
        sharedPref                      = PreferenceManager.getDefaultSharedPreferences(this);

        if(sharedPref.getBoolean("pref_key_auto_start", false)){
            onDestroy();
            return;
        }

        widgetExist                     = checkIfWidgetExist();



        unitMeasurement                 = sharedPref.getString("pref_key_measurement_unit", "Mbps");
        pollRate                        = Long.parseLong(sharedPref.getString("pref_key_poll_rate", "1"));
        showActiveApp                   = sharedPref.getBoolean("pref_key_active_app", true);

        converter                       = getUnitConverter(unitMeasurement);
        titleSetter                     = getNotificationContentTitleSetter(showActiveApp);
        context                         = getApplicationContext();
        name                            = new ComponentName(context, NetworkSpeedWidget.class);

        appMonitorCounter               = 0;


        previousBytesSentSinceBoot      = 0L;
        previousBytesReceivedSinceBoot  = 0L;
        appDataUsageList                = new ArrayList<AppDataUsage>();

        loadAllAppsIntoAppDataUsageList();
        activeAppGetter                 = determineUidDataGatherMethod();


        mNotifyMgr =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mId = 1;
        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.idle) //R.drawable.ic_launcher
                .setContentTitle("")
                .setContentText("")
                .setPriority(Notification.PRIORITY_HIGH)
                .setOngoing(true);

        resultIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);


        notification = mBuilder.build();


        mNotifyMgr.notify(
                mId,
                notification);

        startForeground(mId, notification);

        beepForAnHour(pollRate);
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private ActiveAppGetter determineUidDataGatherMethod(){
        //TODO I think I will have to maintain that useTrafficStatsAPI boolean from this class.  And simply separtate the methods within app data usage and call them accordingly

        long bytesTransferred      = 0L;

        for(AppDataUsage currentApp : appDataUsageList){
            int uid = currentApp.getUid();
            bytesTransferred = TrafficStats.getUidRxBytes(uid) + TrafficStats.getUidTxBytes(uid);
            if(bytesTransferred > 0){
                return setUidDataGatherMethodToTrafficStatsAPI(true);//TODO Change to True

            }
        }

        return setUidDataGatherMethodToTrafficStatsAPI(false);

    }

    private ActiveAppGetter setUidDataGatherMethodToTrafficStatsAPI(boolean b){
        if(b){

            return (new ActiveAppGetter() {
                @Override
                public String getActiveApp() {
                    Log.d("setUidDataGatherMethodToTrafficStatsAPI", "traffic stats API");
                    long maxDelta   = 0L;
                    long delta      = 0L;
                    String appLabel = "";

                    for(AppDataUsage currentApp : appDataUsageList){
                        delta = currentApp.getRateWithTrafficStatsAPI();

                        if(delta > maxDelta){
                            appLabel = currentApp.getAppName();
                            maxDelta = delta;
                        }
                    }
                    if(appLabel == ""){
                        return "...";
                    }
                    return appLabel;

                }
            });
        }else{

            return (new ActiveAppGetter() {
                @Override
                public String getActiveApp() {
                    Log.d("setUidDataGatherMethodToTrafficStatsAPI", "MANUAL");
                    long maxDelta = 0L;
                    long delta = 0L;
                    String appLabel = "";

                    for (AppDataUsage currentApp : appDataUsageList) {
                        delta = currentApp.getRateManual();
                        if (delta > maxDelta) {
                            appLabel = currentApp.getAppName();
                            maxDelta = delta;
                        }
                    }
                    if (appLabel == "") {
                        return "...";
                    }
                    return appLabel;
                }
            });
        }
    }


    private UnitConverter getUnitConverter(String unitMeasurement) {

        if (unitMeasurement.equals("bps")) {
            return (new UnitConverter() {
                @Override
                public double convert(long bytesPerSecond) {
                    return (bytesPerSecond * 8.0);
                }
            });
        }
        if (unitMeasurement.equals("Kbps")) {
            return (new UnitConverter() {
                @Override
                public double convert(long bytesPerSecond) {
                    return (bytesPerSecond * 8.0) / 1000.0;
                }
            });
        }
        if (unitMeasurement.equals("Mbps")) {
            return (new UnitConverter() {
                @Override
                public double convert(long bytesPerSecond) {
                    return (bytesPerSecond * 8.0) / 1000000.0;
                }
            });
        }
        if (unitMeasurement.equals("Gbps")) {
            return (new UnitConverter() {
                @Override
                public double convert(long bytesPerSecond) {
                    return (bytesPerSecond * 8.0) / 1000000000.0;
                }
            });
        }
        if (unitMeasurement.equals("Bps")) {
            return (new UnitConverter() {
                @Override
                public double convert(long bytesPerSecond) {
                    return bytesPerSecond;
                }
            });
        }
        if (unitMeasurement.equals("KBps")) {
            return (new UnitConverter() {
                @Override
                public double convert(long bytesPerSecond) {
                    return bytesPerSecond / 1000.0;
                }
            });
        }
        if (unitMeasurement.equals("MBps")) {
            return (new UnitConverter() {
                @Override
                public double convert(long bytesPerSecond) {
                    return bytesPerSecond / 1000000.0;
                }
            });
        }
        if (unitMeasurement.equals("GBps")) {
            return (new UnitConverter() {
                @Override
                public double convert(long bytesPerSecond) {
                    return bytesPerSecond / 1000000000.0;
                }
            });
        }

        return (new UnitConverter() {
            @Override
            public double convert(long bytesPerSecond) {
                return (bytesPerSecond * 8.0) / 1000000.0;
            }
        });


    }

    private NotificationContentTitleSetter getNotificationContentTitleSetter(boolean showActiveApp) {

        if (showActiveApp) {
            return (new NotificationContentTitleSetter() {
                @Override
                public String set() {
                    return "(" + activeAppGetter.getActiveApp() + ")";
                }
            });
        } else {
            return (new NotificationContentTitleSetter() {
                @Override
                public String set() {
                    return "";
                }
            });

        }

    }

    public void beepForAnHour(long pollRate) {
        final Runnable beeper = new Runnable() {
            public void run() { update(); }
        };
        beeperHandle =
                scheduler.scheduleAtFixedRate(beeper, 1, pollRate, TimeUnit.SECONDS);

    }


	
	private void update(){
        if(!pm.isScreenOn()){//TODO a snazier thing might be to do a broadcast receiver that pauses the schedule executor service when screen is off, and renables when screen on.
            return;          //I don't think cancelling the service all together would be a good idea when screen is off, I don't want to keep calling onCreate when the user turns their screen on
        }

        Log.d("Update", "Run");


        bytesSentSinceBoot              = TrafficStats.getTotalTxBytes();
        bytesReceivedSinceBoot          = TrafficStats.getTotalRxBytes();

        bytesSentPerSecond              = bytesSentSinceBoot - previousBytesSentSinceBoot;
        bytesReceivedPerSecond          = bytesReceivedSinceBoot - previousBytesReceivedSinceBoot;

        sentString                      = String.format("%.3f", converter.convert(bytesSentPerSecond)/pollRate);
        receivedString                  = String.format("%.3f", converter.convert(bytesReceivedPerSecond)/pollRate);

        previousBytesSentSinceBoot      = bytesSentSinceBoot;
        previousBytesReceivedSinceBoot  = bytesReceivedSinceBoot;
        
        appMonitorCounter+=1;
        if(appMonitorCounter >=500/pollRate){//divide by pollRate so that if you have a pollRate of 10, that will end up being 500 seconds, not 5000
            loadAllAppsIntoAppDataUsageList();
        	appMonitorCounter = 0;
        }

        displayValuesText               = " Up: " + sentString +  " Down: " +  receivedString;
        activeApp                       = titleSetter.set();
    	contentTitleText                = unitMeasurement + " " + activeApp;
    	
        mBuilder.setContentText(displayValuesText);
        mBuilder.setContentTitle(contentTitleText);


        resultIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);


        //mBuilder.setPriority(Notification.PRIORITY_MIN);//just added these two
        //mBuilder.setOngoing(true);//just added these two


       mBuilder.setWhen(System.currentTimeMillis());
//        Intent resultIntent = new Intent(this, MainActivity.class);
//        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
//        stackBuilder.addParentStack(MainActivity.class);
//        stackBuilder.addNextIntent(resultIntent);
//        PendingIntent resultPendingIntent =
//                stackBuilder.getPendingIntent(
//                    0,
//                    PendingIntent.FLAG_UPDATE_CURRENT
//                );
//        mBuilder.setContentIntent(resultPendingIntent);


        if(widgetExist){
            updateWidgets();
        }


        if(bytesSentPerSecond/pollRate<13107 && bytesReceivedPerSecond/pollRate<13107){
            mBuilder.setSmallIcon(R.drawable.idle);
            mNotifyMgr.notify(mId, mBuilder.build());
            return;
        }

        if(!(bytesSentPerSecond/pollRate>13107) && bytesReceivedPerSecond/pollRate>13107){
            mBuilder.setSmallIcon(R.drawable.download);
            mNotifyMgr.notify(mId, mBuilder.build());
            return;
        }

        if(bytesSentPerSecond/pollRate>13107 && bytesReceivedPerSecond/pollRate<13107){
            mBuilder.setSmallIcon(R.drawable.upload);
            mNotifyMgr.notify(mId, mBuilder.build());
            return;
        }

        if(bytesSentPerSecond/pollRate>13107 && bytesReceivedPerSecond/pollRate>13107){//1307 bytes is equal to .1Mbit
            mBuilder.setSmallIcon(R.drawable.both);
            mNotifyMgr.notify(mId, mBuilder.build());
            return;
        }




        //mNotifyMgr.notify(mId, mBuilder.build());

    }


private void loadAllAppsIntoAppDataUsageList(){
    PackageManager packageManager=this.getPackageManager();
    List<ApplicationInfo> appList=packageManager.getInstalledApplications(0);

    for (ApplicationInfo appInfo : appList) {
        String appLabel = (String) packageManager.getApplicationLabel(appInfo);
        int uid = appInfo.uid;
        AppDataUsage app = new AppDataUsage(appLabel, uid);
        appDataUsageList.add(app);

    }
}


/*TODO
    Talk to commonsware dude in fireside chat.  Ask what the most efficient way to update widgets is.  There could be several of them and I keep doing shit like setcolor each update, even though it doesn't change
*/

    //TODO I AM AN IDIOT.  Wait, maybe not.  I have these four bullshit text views.  Maybe I can simply use '\n', so still have only one text view, just append lines that way

    public static void setWidgetExist(boolean b){
        widgetExist = b;
    }

    private boolean checkIfWidgetExist(){
        try {
            int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(name);
        } catch (NullPointerException e) {
            return false;
        }
        int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(name);
        if (ids.length > 0) {
            return true;
        }
        return false;

    }

    private void updateWidgets(){
        int [] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(name);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);

        final int N = ids.length;

        Log.d("Before", "For Loop");
        for (int i = 0; i < N; i++){
            Log.d("In", "for loop");
            int awID = ids[i];

            boolean displayActiveApp = sharedPref.getBoolean("pref_key_widget_active_app" + awID, true);
            String colorOfFont = sharedPref.getString("pref_key_widget_font_color" + awID, "Black");
            String sizeOfFont = sharedPref.getString("pref_key_widget_font_size" + awID, "12.0");

            String widgetTextViewLineOneText = "";

            int widgetColor;
            widgetColor = Color.parseColor(colorOfFont);

            if (displayActiveApp) {
                widgetTextViewLineOneText = activeApp + "\n";
            }


            widgetTextViewLineOneText+= unitMeasurement+"\n";
            widgetTextViewLineOneText+= "Up: "+sentString +"\n";
            widgetTextViewLineOneText+= "Down: "+receivedString +"\n";



            RemoteViews v = new RemoteViews(getPackageName(), R.layout.widget);

            v.setTextViewText(R.id.widgetTextViewLineOne, widgetTextViewLineOneText);

            v.setTextColor(R.id.widgetTextViewLineOne, widgetColor);


            Float tempFloat = Float.parseFloat(sizeOfFont);

            v.setFloat(R.id.widgetTextViewLineOne, "setTextSize", tempFloat);

            manager.updateAppWidget(awID, v);

        }

    }

}
