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
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.preference.PreferenceManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import android.util.Log;
import android.widget.RemoteViews;

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


    @Override
    public void onDestroy(){
        try {
            beeperHandle.cancel(true);
        } catch (NullPointerException e) {
            //The only way there will be a null pointer, is if the disabled preference is checked.  Because if it is, onDestory is called right away, without creating the beeperHandle
        }
        super.onDestroy();
        //what I gotta do is kill the schedule Executor service
    }

	@Override
	public void onCreate() {
        sharedPref                      = PreferenceManager.getDefaultSharedPreferences(this);

        if(sharedPref.getBoolean("pref_key_auto_start", false)){
            onDestroy();
            return;
        }

        unitMeasurement                 = sharedPref.getString("pref_key_measurement_unit", "Mbps");
        pollRate                        = Long.parseLong(sharedPref.getString("pref_key_poll_rate", "1"));
        showActiveApp                   = sharedPref.getBoolean("pref_key_active_app", true);

        converter                       = getUnitConverter(unitMeasurement);
        titleSetter                     = getNotificationContentTitleSetter(showActiveApp);
        context                         = getApplicationContext();
        name                            = new ComponentName(context, NetworkSpeedWidget.class);

        appMonitorCounter               = 0;


        previousBytesSentSinceBoot = 0L;
        previousBytesReceivedSinceBoot = 0L;
        appDataUsageList = new ArrayList<AppDataUsage>();

        loadAllAppsIntoAppDataUsageList();


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

    private UnitConverter getUnitConverter(String unitMeasurement){

        if (unitMeasurement.equals("bps")){
            return (new UnitConverter() {
                @Override
                public double convert(long bytesPerSecond) {
                    return (bytesPerSecond * 8.0);
                }
            });
        }
        if (unitMeasurement.equals("Kbps")){
            return (new UnitConverter() {
                @Override
                public double convert(long bytesPerSecond) {
                    return (bytesPerSecond * 8.0) / 1000.0;
                }
            });
        }
        if (unitMeasurement.equals("Mbps")){
            return (new UnitConverter() {
                @Override
                public double convert(long bytesPerSecond) {
                    return (bytesPerSecond * 8.0) / 1000000.0;
                }
            });
        }
        if (unitMeasurement.equals("Gbps")){
            return (new UnitConverter() {
                @Override
                public double convert(long bytesPerSecond) {
                    return (bytesPerSecond * 8.0) / 1000000000.0;
                }
            });
        }
        if (unitMeasurement.equals("Bps")){
            return (new UnitConverter() {
                @Override
                public double convert(long bytesPerSecond) {
                    return bytesPerSecond;
                }
            });
        }
        if (unitMeasurement.equals("KBps")){
            return (new UnitConverter() {
                @Override
                public double convert(long bytesPerSecond) {
                    return bytesPerSecond / 1000.0;
                }
            });
        }
        if (unitMeasurement.equals("MBps")){
            return (new UnitConverter() {
                @Override
                public double convert(long bytesPerSecond) {
                    return bytesPerSecond / 1000000.0;
                }
            });
        }
        if (unitMeasurement.equals("GBps")){
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

    private NotificationContentTitleSetter getNotificationContentTitleSetter(boolean showActiveApp){

        if (showActiveApp){
            return (new NotificationContentTitleSetter() {
                @Override
                public String set() {
                    return " " + "("+ getActiveApp()+")";
                }
            });
        }
        else{
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
        if(appMonitorCounter >=500){//TODO change the 10 to something higher, maybe check every 60 seconds
            loadAllAppsIntoAppDataUsageList();
        	appMonitorCounter = 0;
        }

        displayValuesText               = " Up: " + sentString +  " Down: " +  receivedString;
        activeApp                       = titleSetter.set();
    	contentTitleText                = unitMeasurement + activeApp;
    	
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


//        checkForAndUpdateWidgets();



        if(bytesSentPerSecond/pollRate>13107 && bytesReceivedPerSecond/pollRate>13107){//1307 bytes is equal to .1Mbit
            mBuilder.setSmallIcon(R.drawable.both);
            mNotifyMgr.notify(mId, mBuilder.build());
            return;
        }

        if(!(bytesSentPerSecond/pollRate>13107) && bytesReceivedPerSecond/pollRate>13107){
            mBuilder.setSmallIcon(R.drawable.download);
            mNotifyMgr.notify(mId, mBuilder.build());
            return;

        }

        if(bytesSentPerSecond/pollRate<13107 && bytesReceivedPerSecond/pollRate<13107){
            mBuilder.setSmallIcon(R.drawable.idle);
            mNotifyMgr.notify(mId, mBuilder.build());
            return;

        }

        if(bytesSentPerSecond/pollRate>13107 && bytesReceivedPerSecond/pollRate<13107){
            mBuilder.setSmallIcon(R.drawable.upload);
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

private String getActiveApp() {

    
    long maxDelta   = 0L;
    long delta      = 0L;
    String appLabel = "";
    
    for(AppDataUsage currentApp : appDataUsageList){
    	delta = currentApp.getRate();
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


//    private void checkForAndUpdateWidgets(){
////        Context context = getApplicationContext();
////        ComponentName name = new ComponentName(context, NetworkSpeedWidget.class);
//        int [] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(name);
//        AppWidgetManager manager = AppWidgetManager.getInstance(this);
//
//        final int N = ids.length;
//
//        Log.d("Before", "For Loop");
//        for (int i = 0; i < N; i++){
//            Log.d("In", "for loop");
//        int awID = ids[i];
//
//
//        //boolean bool = sharedPref.getBoolean("pref_key_widget_display_unit_of_measure"+awID, true);
//
//        String unitOfMeasure = sharedPref.getString("pref_key_widget_measurement_unit"+awID, "Mbps");
//        boolean displayUnitOfMeasure = true;
//        boolean displayTransferRateLabels = true;
//
//        boolean displayActiveApp = sharedPref.getBoolean("pref_key_widget_active_app"+awID, true);
//        String colorOfFont = sharedPref.getString("pref_key_widget_font_color"+awID, "Black");
//        String sizeOfFont = sharedPref.getString("pref_key_widget_font_size"+awID, "12.0");
//
//        String widgetTextViewLineOneText = "";
//        String widgetTextViewLineTwoText = "";
//        String widgetTextViewLineThreeText = "";
//        String widgetTextViewLineFourText = "";
//        String widgetTextViewLineFiveText = "";
//
//
//
//        convertBytesPerSecondValuesToUnitMeasurement(unitOfMeasure);
//
////        appMonitorCounter+=1;  //if several widgets, then one will be added to this more than once per 5 seconds, solved this doing N*5
////        if(appMonitorCounter >= N*5 && displayActiveApp){
////        activeApp = getActiveApp();
////        String fastAppWithParens = " " + "(" + activeApp + ")";
////        appMonitorCounter = 0;
////        }
//
//        sentString = String.format("%.3f", sent/originalPollRate);
//        receivedString = String.format("%.3f", received/originalPollRate);
//        //totalString = String.format("%.3f", total);
//
//        int widgetColor;
//        widgetColor = Color.parseColor(colorOfFont);
////
//        if(displayActiveApp){
//        widgetTextViewLineOneText = activeApp;
//        }
//
//        breakMeUnitOfMeasure:if(displayUnitOfMeasure){
//        if(widgetTextViewLineOneText.equals("")){
//        widgetTextViewLineOneText = ultimateUnitOfMeasure;
//        break breakMeUnitOfMeasure;
//        }
//        if(widgetTextViewLineTwoText.equals("")){
//        widgetTextViewLineTwoText = ultimateUnitOfMeasure;
//        break breakMeUnitOfMeasure;
//        }
//        if(widgetTextViewLineThreeText.equals("")){
//        widgetTextViewLineThreeText = ultimateUnitOfMeasure;
//        break breakMeUnitOfMeasure;
//        }
//        if(widgetTextViewLineFourText.equals("")){
//        widgetTextViewLineFourText = ultimateUnitOfMeasure;
//        break breakMeUnitOfMeasure;
//        }
//        if(widgetTextViewLineFiveText.equals("")){
//        widgetTextViewLineFiveText = ultimateUnitOfMeasure;
//        break breakMeUnitOfMeasure;
//        }
//        }
//
//
//
//
//
//
//        breakMeUploadValue:if(true){
//        String uploadValueLocal = sentString;
//        if(displayTransferRateLabels){
//        uploadValueLocal = "Up: " + sentString;
//        }
//        if(widgetTextViewLineOneText.equals("")){
//        widgetTextViewLineOneText = uploadValueLocal;
//        break breakMeUploadValue;
//        }
//        if(widgetTextViewLineTwoText.equals("")){
//        widgetTextViewLineTwoText = uploadValueLocal;
//        break breakMeUploadValue;
//        }
//        if(widgetTextViewLineThreeText.equals("")){
//        widgetTextViewLineThreeText = uploadValueLocal;
//        break breakMeUploadValue;
//        }
//        if(widgetTextViewLineFourText.equals("")){
//        widgetTextViewLineFourText = uploadValueLocal;
//        break breakMeUploadValue;
//        }
//        if(widgetTextViewLineFiveText.equals("")){
//        widgetTextViewLineFiveText = uploadValueLocal;
//        break breakMeUploadValue;
//        }
//
//        }
//
//
//        breakMeDownloadValue:if(true){
//        String downloadValueLocal = receivedString;
//        if(displayTransferRateLabels){
//        downloadValueLocal = "Down: " + receivedString;
//        }
//        if(widgetTextViewLineOneText.equals("")){
//        widgetTextViewLineOneText = downloadValueLocal;
//        break breakMeDownloadValue;
//        }
//        if(widgetTextViewLineTwoText.equals("")){
//        widgetTextViewLineTwoText = downloadValueLocal;
//        break breakMeDownloadValue;
//        }
//        if(widgetTextViewLineThreeText.equals("")){
//        widgetTextViewLineThreeText = downloadValueLocal;
//        break breakMeDownloadValue;
//        }
//        if(widgetTextViewLineFourText.equals("")){
//        widgetTextViewLineFourText = downloadValueLocal;
//        break breakMeDownloadValue;
//        }
//        if(widgetTextViewLineFiveText.equals("")){
//        widgetTextViewLineFiveText = downloadValueLocal;
//        break breakMeDownloadValue;
//        }
//
//        }
//
//
//        //widgetColor = setColorOfWidget(colorOfFont, customColorOfFont);
//
//        if(bytesSentSinceBoot < 0 || bytesReceivedSinceBoot < 0 || bytesSentAndReceivedSinceBoot < 0){
//        widgetTextViewLineOneText = "Device Unsupported, please email me so I can help";
//        }
//
//
//
//        RemoteViews v = new RemoteViews(getPackageName(), R.layout.widget);
//
//        v.setTextViewText(R.id.widgetTextViewLineOne, widgetTextViewLineOneText);
//        v.setTextViewText(R.id.widgetTextViewLineTwo, widgetTextViewLineTwoText);
//        v.setTextViewText(R.id.widgetTextViewLineThree, widgetTextViewLineThreeText);
//        v.setTextViewText(R.id.widgetTextViewLineFour, widgetTextViewLineFourText);
//        //v.setTextViewText(R.id.widgetTextViewLineFive, widgetTextViewLineFiveText);
//
//        v.setTextColor( R.id.widgetTextViewLineOne, widgetColor);
//        v.setTextColor( R.id.widgetTextViewLineTwo, widgetColor);
//        v.setTextColor( R.id.widgetTextViewLineThree, widgetColor);
//        v.setTextColor( R.id.widgetTextViewLineFour, widgetColor);
//        //v.setTextColor( R.id.widgetTextViewLineFive, widgetColor);
//
//        Float tempFloat= Float.parseFloat(sizeOfFont);
//
//        v.setFloat(R.id.widgetTextViewLineOne, "setTextSize", tempFloat);
//        v.setFloat(R.id.widgetTextViewLineTwo, "setTextSize", tempFloat);
//        v.setFloat(R.id.widgetTextViewLineThree, "setTextSize", tempFloat);
//        v.setFloat(R.id.widgetTextViewLineFour, "setTextSize", tempFloat);
//        //v.setFloat(R.id.widgetTextViewLineFive, "setTextSize", tempFloat);
//        manager.updateAppWidget(awID, v);
//        widgetTextViewLineOneText = "";
//
//        //HAD THIS BEFORE ABOVE
//
//
//
////        previousBytesSentAndReceivedSinceBoot = bytesSentAndReceivedSinceBoot;
////        previousBytesSentSinceBoot = bytesSentSinceBoot;
////        previousBytesReceivedSinceBoot = bytesReceivedSinceBoot;
//
//        }
//
//        }


//    private void convertBytesPerSecondValuesToUnitMeasurement(String unitMeasurement) {
//
//        if (unitMeasurement.equals("bps")){
//            total = convertBpsTobps(bytesSentAndReceivedPerSecond);
//            sent = convertBpsTobps(bytesSentPerSecond);
//            received = convertBpsTobps(bytesReceivedPerSecond);
//            ultimateUnitOfMeasure = "bps";
//            return;
//        }
//        if (unitMeasurement.equals("Kbps")){
//            total = convertBpsToKbps(bytesSentAndReceivedPerSecond);
//            sent = convertBpsToKbps(bytesSentPerSecond);
//            received = convertBpsToKbps(bytesReceivedPerSecond);
//            ultimateUnitOfMeasure = "kbps";
//            return;
//        }
//        if (unitMeasurement.equals("Mbps")){
//            total = convertBpsToMbps(bytesSentAndReceivedPerSecond);
//            sent = convertBpsToMbps(bytesSentPerSecond);
//            received = convertBpsToMbps(bytesReceivedPerSecond);
//            ultimateUnitOfMeasure = "Mbps";
//            return;
//        }
//        if (unitMeasurement.equals("Gbps")){
//            total = convertBpsToGbps(bytesSentAndReceivedPerSecond);
//            sent = convertBpsToGbps(bytesSentPerSecond);
//            received = convertBpsToGbps(bytesReceivedPerSecond);
//            ultimateUnitOfMeasure = "Gbps";
//            return;
//        }
//        if (unitMeasurement.equals("Bps")){
//            total = convertBpsToBps(bytesSentAndReceivedPerSecond);
//            sent = convertBpsToBps(bytesSentPerSecond);
//            received = convertBpsToBps(bytesReceivedPerSecond);
//            ultimateUnitOfMeasure = "Bps";
//            return;
//        }
//        if (unitMeasurement.equals("KBps")){
//            total = convertBpsToKBps(bytesSentAndReceivedPerSecond);
//            sent = convertBpsToKBps(bytesSentPerSecond);
//            received = convertBpsToKBps(bytesReceivedPerSecond);
//            ultimateUnitOfMeasure = "kBps";
//            return;
//        }
//        if (unitMeasurement.equals("MBps")){
//            total = convertBpsToMBps(bytesSentAndReceivedPerSecond);
//            sent = convertBpsToMBps(bytesSentPerSecond);
//            received = convertBpsToMBps(bytesReceivedPerSecond);
//            ultimateUnitOfMeasure = "MBps";
//            return;
//        }
//        if (unitMeasurement.equals("GBps")){
//            total = convertBpsToGBps(bytesSentAndReceivedPerSecond);
//            sent = convertBpsToGBps(bytesSentPerSecond);
//            received = convertBpsToGBps(bytesReceivedPerSecond);
//            ultimateUnitOfMeasure = "GBps";
//            return;
//        }
//
//    }

}
