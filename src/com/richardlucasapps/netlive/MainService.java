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
import android.os.PowerManager;
import android.util.Log;
import android.widget.RemoteViews;

public class MainService extends Service {

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

	private Long bytesSentAndReceivedSinceBoot;
	private Long bytesSentSinceBoot;
	private Long bytesReceivedSinceBoot;
	
	private Long previousBytesSentAndReceivedSinceBoot;
	private Long previousBytesSentSinceBoot;
	private Long previousBytesReceivedSinceBoot;
	
	private Long bytesSentAndReceivedPerSecond;
	private Long bytesSentPerSecond;
	private Long bytesReceivedPerSecond;
    
	double total;
    double sent;
	double received;

	private String totalString;
	private String sentString;
	private String receivedString;
	private String ultimateUnitOfMeasure;
    int appMonitorCounter;
	private String fastApp="";
	List<AppDataUsage> appDataUsageList;
	String appLabel1;
	PowerManager pm;
	int getCurrentInstalledAppsCount;




    //Handler mHandler;

	int mId;

	NotificationCompat.Builder mBuilder;
	NotificationManager mNotifyMgr;
	Notification notification;
	private int timeCount;
	SharedPreferences sharedPref;
	boolean syncConnPrefDisbale;

    String fastAppWithParens;

   long originalPollRate;
   ScheduledFuture beeperHandle;

   //final ScheduledFuture beeperHandle = null;


	@Override
	public void onCreate() {
		
		
		//pm  = ((PowerManager) getSystemService(Context.POWER_SERVICE));

		sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		
    	syncConnPrefDisbale = sharedPref.getBoolean("pref_key_auto_start", false);

        long pollRate = Long.parseLong(sharedPref.getString("pref_key_poll_rate", "1"));
        Log.d("pollRate", String.valueOf(pollRate));
        originalPollRate = pollRate;
    
    	getCurrentInstalledAppsCount = 10;
    
    	
    	//For the onCreate, create a listener on disable, so if someone undisables the app then the activity starts up
    	//probably want to do this in the SettingsFragment java class, and pass an intent to this class to destroy the
    	//notification or reactivate it


        //This line below is janky as hell. I don't want to have to check this every time.  If it's not running, it's not running.
        //Leave it alone, no need to keep checking.
//    	if(syncConnPrefDisbale){
//            Log.d("Sync Con Pref Disable", "looping out before it can run");
//    		return;
//   	    }
    	//mHandler = new Handler();
	        previousBytesSentAndReceivedSinceBoot = 0L;
	        previousBytesSentSinceBoot = 0L;
	        previousBytesReceivedSinceBoot = 0L;
	        appDataUsageList = new ArrayList<AppDataUsage>();
	        fastApp = "";
	        appMonitorCounter = 0;
	        timeCount = 0;

            loadAllAppsIntoAppDataUsageList();
	        
	        //clickIntent notificationIntent = new Intent(this, MainActivity.class);
	        
	        mNotifyMgr = 
	                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	        mId = 1;
	        mBuilder = new NotificationCompat.Builder(this)
			.setSmallIcon(R.drawable.idle) //R.drawable.ic_launcher
		    .setContentTitle("")
		    .setContentText("")
            .setPriority(Notification.PRIORITY_HIGH)
	        .setOngoing(true);

        Intent resultIntent = new Intent(this, MainActivity.class);
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

            //Just commented out this foreground line
	        startForeground(mId, notification);
	        //start();
            beepForAnHour(pollRate);
	        super.onCreate();
	}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void beepForAnHour(long pollRate) {
        final Runnable beeper = new Runnable() {
            public void run() { update(); }
        };
        beeperHandle =
                scheduler.scheduleAtFixedRate(beeper, 1, pollRate, TimeUnit.SECONDS);

    }


	
	private void update(){
    //TODO still need to divide the values by the poll rate to get an estimate


		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        if(originalPollRate!=Long.valueOf(sharedPref.getString("pref_key_poll_rate", "1"))){
            beeperHandle.cancel(true);
            onCreate();
        }

        //TODO remove
		//boolean displayNames = sharedPref.getBoolean("pref_key_transfer_rate_names", true);
		
		boolean showActiveApp = sharedPref.getBoolean("pref_key_active_app", true);
		
		syncConnPrefDisbale = sharedPref.getBoolean("pref_key_auto_start", false);

    	timeCount++;
		setTotalSentReceiveBytesPerSecond();
        convertBytesPerSecondValuesToUnitMeasurement();
        
        sentString = String.format("%.3f", sent/originalPollRate); //TODO this handles the different poll rate
        receivedString = String.format("%.3f", received/originalPollRate);
        totalString = String.format("%.3f", total/originalPollRate);
        
        
        previousBytesSentAndReceivedSinceBoot = bytesSentAndReceivedSinceBoot;
        previousBytesSentSinceBoot = bytesSentSinceBoot;
        previousBytesReceivedSinceBoot = bytesReceivedSinceBoot;
        
        appMonitorCounter+=1;
        if(appMonitorCounter >=60){//TODO change the 10 to something higher, maybe check every 60 seconds
        	if(appDataUsageList.isEmpty()){
                loadAllAppsIntoAppDataUsageList();
                Log.d("loading all", "apps into list");
            }

        	appMonitorCounter = 0;
        }

            fastApp = getCurrentInstalledApps();
            fastAppWithParens = " " + "(" + fastApp + ")";

    	
    	String displayValuesText = "";
        displayValuesText += " Up: " + sentString;
        displayValuesText += " Down: " +  receivedString;

    	
    	String contentTitleText = "";
/*TODO so many times throughout my code, I constantly check to see the settings of things, e.g. I am constantly checking here if showActiveApp is enabled.
      There has got to be a better way to do this.  Maybe get some strategy pattern action involved.
        */
    	if(showActiveApp){
    		contentTitleText = ultimateUnitOfMeasure + " " + fastAppWithParens;
    	}else{
    		contentTitleText = ultimateUnitOfMeasure;
    	}
    	
    	if(syncConnPrefDisbale){
    		stopForeground(true);
    		mNotifyMgr.cancel(mId);
            Log.d("Stopping from posting notification", "Main service line 213");
    		return;
    	}
    	
    	
    	if(bytesSentSinceBoot < 0 || bytesReceivedSinceBoot < 0 || bytesSentAndReceivedSinceBoot < 0){
    		contentTitleText = "Device Unsupported";
    		displayValuesText = "Please email me so I can help";
    	}
    	
        mBuilder.setContentText(displayValuesText);
        mBuilder.setContentTitle(contentTitleText);


        //mBuilder.setPriority(Notification.PRIORITY_MIN);//just added these two
        //mBuilder.setOngoing(true);//just added these two

        if(timeCount >= 500){
        	mBuilder.setWhen(System.currentTimeMillis());
        	timeCount = 0;
        }
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


        checkForAndUpdateWidgets();



        if(bytesSentPerSecond/originalPollRate>13107 && bytesReceivedPerSecond/originalPollRate>13107){//1307 bytes is equal to .1Mbit
            mBuilder.setSmallIcon(R.drawable.both);
            mNotifyMgr.notify(mId, mBuilder.build());
            return;
        }

        if(!(bytesSentPerSecond/originalPollRate>13107) && bytesReceivedPerSecond/originalPollRate>13107){
            mBuilder.setSmallIcon(R.drawable.download);
            mNotifyMgr.notify(mId, mBuilder.build());
            return;

        }

        if(bytesSentPerSecond/originalPollRate<13107 && bytesReceivedPerSecond/originalPollRate<13107){
            mBuilder.setSmallIcon(R.drawable.idle);
            mNotifyMgr.notify(mId, mBuilder.build());
            return;

        }

        if(bytesSentPerSecond/originalPollRate>13107 && bytesReceivedPerSecond/originalPollRate<13107){
            mBuilder.setSmallIcon(R.drawable.upload);
            mNotifyMgr.notify(mId, mBuilder.build());
            return;
        }

        //mNotifyMgr.notify(mId, mBuilder.build());

    }

private void convertBytesPerSecondValuesToUnitMeasurement() {

    //TODO Get rid of all these if statements, use strategy pattern, so create a base class
    //When the options are changed / when app first starts.
    //This base class will hold the transfer rate and whether show active app is enabled.
    //This will be especially helpful with the widget
	SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
	String unitMeasurement = sharedPref.getString("pref_key_measurement_unit", null);
	
	if (unitMeasurement.equals("bps")){
        total = convertBpsTobps(bytesSentAndReceivedPerSecond);
        sent = convertBpsTobps(bytesSentPerSecond);
        received = convertBpsTobps(bytesReceivedPerSecond);
        ultimateUnitOfMeasure = "bps";
        return;
	}
	if (unitMeasurement.equals("Kbps")){
		total = convertBpsToKbps(bytesSentAndReceivedPerSecond);
        sent = convertBpsToKbps(bytesSentPerSecond);
        received = convertBpsToKbps(bytesReceivedPerSecond);
        ultimateUnitOfMeasure = "kbps";
        return;
	}
	if (unitMeasurement.equals("Mbps")){
		total = convertBpsToMbps(bytesSentAndReceivedPerSecond);
        sent = convertBpsToMbps(bytesSentPerSecond);
        received = convertBpsToMbps(bytesReceivedPerSecond);
        ultimateUnitOfMeasure = "Mbps";
        return;
	}
	if (unitMeasurement.equals("Gbps")){
		total = convertBpsToGbps(bytesSentAndReceivedPerSecond);
        sent = convertBpsToGbps(bytesSentPerSecond);
        received = convertBpsToGbps(bytesReceivedPerSecond);
        ultimateUnitOfMeasure = "Gbps";
        return;
	}
	if (unitMeasurement.equals("Bps")){
		total = convertBpsToBps(bytesSentAndReceivedPerSecond);
        sent = convertBpsToBps(bytesSentPerSecond);
        received = convertBpsToBps(bytesReceivedPerSecond);
        ultimateUnitOfMeasure = "Bps";
        return;
	}
	if (unitMeasurement.equals("KBps")){
		total = convertBpsToKBps(bytesSentAndReceivedPerSecond);
        sent = convertBpsToKBps(bytesSentPerSecond);
        received = convertBpsToKBps(bytesReceivedPerSecond);
        ultimateUnitOfMeasure = "kBps";
        return;
	}
	if (unitMeasurement.equals("MBps")){
		total = convertBpsToMBps(bytesSentAndReceivedPerSecond);
        sent = convertBpsToMBps(bytesSentPerSecond);
        received = convertBpsToMBps(bytesReceivedPerSecond);
        ultimateUnitOfMeasure = "MBps";
        return;
	}
	if (unitMeasurement.equals("GBps")){
		total = convertBpsToGBps(bytesSentAndReceivedPerSecond);
        sent = convertBpsToGBps(bytesSentPerSecond);
        received = convertBpsToGBps(bytesReceivedPerSecond);
        ultimateUnitOfMeasure = "GBps";
        return;
	}

		
		
}
	
	


private void setTotalSentReceiveBytesPerSecond() {
	 bytesSentSinceBoot = TrafficStats.getTotalTxBytes();
     bytesReceivedSinceBoot = TrafficStats.getTotalRxBytes();
     bytesSentAndReceivedSinceBoot = bytesSentSinceBoot + bytesReceivedSinceBoot;
     
     bytesSentPerSecond = bytesSentSinceBoot - previousBytesSentSinceBoot;
     bytesReceivedPerSecond = bytesReceivedSinceBoot - previousBytesReceivedSinceBoot;
     bytesSentAndReceivedPerSecond = bytesSentAndReceivedSinceBoot - previousBytesSentAndReceivedSinceBoot;
	
}

private void loadAllAppsIntoAppDataUsageList(){
    PackageManager packageManager=this.getPackageManager();
    List<ApplicationInfo> appList=packageManager.getInstalledApplications(0);

    for (ApplicationInfo appInfo : appList) {
        String appLabel = (String) packageManager.getApplicationLabel(appInfo);
        int uid = appInfo.uid;
        AppDataUsage app = new AppDataUsage(appLabel, uid);
        appDataUsageList.add(app);
        Log.d("Initial Add", appLabel + " " + String.valueOf(uid));

    }
}

private String getCurrentInstalledApps() {


//	PackageManager packageManager=this.getPackageManager();
//    List<ApplicationInfo> appList=packageManager.getInstalledApplications(0);
//
//    for (ApplicationInfo appInfo : appList) {
//    	String appLabel = (String) packageManager.getApplicationLabel(appInfo);
//    	int uid = appInfo.uid;
//    	//AppDataUsage app = new AppDataUsage(appLabel, uid);
//    	//if(!appDataUsageList.contains(app)){
//    	//	appDataUsageList.add(app);
//    	//}
//
//        if(!doesAppDataUsageListContain(appLabel)){
//            AppDataUsage app = new AppDataUsage(appLabel, uid);
//            appDataUsageList.add(app);
//        }
//
//
////        for(AppDataUsage check:appDataUsageList){
////            if(appLabel.equals(check.getAppName())){
////               break;
////            }
////        }
//    }


    
    Long maxDelta = (long) 0;
    Long delta = (long) 0;
    
    for(AppDataUsage currentApp : appDataUsageList){
    	delta = currentApp.getRate();
    	if(!(delta==0)){
    	}
    	if(delta > maxDelta){
    		appLabel1 = currentApp.getAppName();
    		maxDelta = delta;
    	}
    	
    }
    if(appLabel1 == null){
    	return "";
    }
    return appLabel1;
}

private boolean doesAppDataUsageListContain(String label){
    for(AppDataUsage check : appDataUsageList){
        if(check.equals(label)){
            return true;
        }
    }
    return false;
}
	
private double convertBpsTobps(long bytesPerSecond){
	return (bytesPerSecond * 8.0);
}

private double convertBpsToKbps(long bytesPerSecond){
	return   (bytesPerSecond * 8.0) / 1000.0;
	
}

private double convertBpsToMbps(long bytesPerSecond){
	return (bytesPerSecond * 8.0) / 1000000.0;
}

private double convertBpsToGbps(long bytesPerSecond){
	return (bytesPerSecond * 8.0) / 1000000000.0;
}

private double convertBpsToBps(long bytesPerSecond){
	return  bytesPerSecond;
}

private double convertBpsToKBps(long bytesPerSecond){
	return  bytesPerSecond / 1000.0;
}

private double convertBpsToMBps(long bytesPerSecond){
	return  bytesPerSecond / 1000000.0;
}

private double convertBpsToGBps(long bytesPerSecond){
	return  bytesPerSecond / 1000000000.0;
}

    private void checkForAndUpdateWidgets(){
        Context context = getApplicationContext();
        ComponentName name = new ComponentName(context, NetworkSpeedWidget.class);
        int [] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(name);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);

        final int N = ids.length;

        for (int i = 0; i < N; i++){
        int awID = ids[i];


        //boolean bool = sharedPref.getBoolean("pref_key_widget_display_unit_of_measure"+awID, true);

        String unitOfMeasure = sharedPref.getString("pref_key_widget_measurement_unit"+awID, "Mbps");
        boolean displayUnitOfMeasure = true;
        boolean displayTransferRateLabels = true;

        boolean displayActiveApp = sharedPref.getBoolean("pref_key_widget_active_app"+awID, true);
        String colorOfFont = sharedPref.getString("pref_key_widget_font_color"+awID, "Black");
        String sizeOfFont = sharedPref.getString("pref_key_widget_font_size"+awID, "12.0");

        String widgetTextViewLineOneText = "";
        String widgetTextViewLineTwoText = "";
        String widgetTextViewLineThreeText = "";
        String widgetTextViewLineFourText = "";
        String widgetTextViewLineFiveText = "";



        convertBytesPerSecondValuesToUnitMeasurement(unitOfMeasure);

//        appMonitorCounter+=1;  //if several widgets, then one will be added to this more than once per 5 seconds, solved this doing N*5
//        if(appMonitorCounter >= N*5 && displayActiveApp){
//        fastApp = getCurrentInstalledApps();
//        String fastAppWithParens = " " + "(" + fastApp + ")";
//        appMonitorCounter = 0;
//        }

        sentString = String.format("%.3f", sent/originalPollRate);
        receivedString = String.format("%.3f", received/originalPollRate);
        //totalString = String.format("%.3f", total);

        int widgetColor;
        widgetColor = Color.parseColor(colorOfFont);
//
        if(displayActiveApp){
        widgetTextViewLineOneText = fastApp;
        }

        breakMeUnitOfMeasure:if(displayUnitOfMeasure){
        if(widgetTextViewLineOneText.equals("")){
        widgetTextViewLineOneText = ultimateUnitOfMeasure;
        break breakMeUnitOfMeasure;
        }
        if(widgetTextViewLineTwoText.equals("")){
        widgetTextViewLineTwoText = ultimateUnitOfMeasure;
        break breakMeUnitOfMeasure;
        }
        if(widgetTextViewLineThreeText.equals("")){
        widgetTextViewLineThreeText = ultimateUnitOfMeasure;
        break breakMeUnitOfMeasure;
        }
        if(widgetTextViewLineFourText.equals("")){
        widgetTextViewLineFourText = ultimateUnitOfMeasure;
        break breakMeUnitOfMeasure;
        }
        if(widgetTextViewLineFiveText.equals("")){
        widgetTextViewLineFiveText = ultimateUnitOfMeasure;
        break breakMeUnitOfMeasure;
        }
        }



//			breakMeTotalValue:if(true){
//				String totalValueLocal = totalString;
//				if(displayTransferRateLabels){
//					totalValueLocal = "Total: " + totalString;
//				}
//				if(widgetTextViewLineOneText.equals("")){
//					widgetTextViewLineOneText = totalValueLocal;
//					break breakMeTotalValue;
//				}
//				if(widgetTextViewLineTwoText.equals("")){
//					widgetTextViewLineTwoText = totalValueLocal;
//					break breakMeTotalValue;
//				}
//				if(widgetTextViewLineThreeText.equals("")){
//					widgetTextViewLineThreeText = totalValueLocal;
//					break breakMeTotalValue;
//				}
//				if(widgetTextViewLineFourText.equals("")){
//					widgetTextViewLineFourText = totalValueLocal;
//					break breakMeTotalValue;
//				}
//				if(widgetTextViewLineFiveText.equals("")){
//					widgetTextViewLineFiveText = totalValueLocal;
//					break breakMeTotalValue;
//				}
//
//			}





        breakMeUploadValue:if(true){
        String uploadValueLocal = sentString;
        if(displayTransferRateLabels){
        uploadValueLocal = "Up: " + sentString;
        }
        if(widgetTextViewLineOneText.equals("")){
        widgetTextViewLineOneText = uploadValueLocal;
        break breakMeUploadValue;
        }
        if(widgetTextViewLineTwoText.equals("")){
        widgetTextViewLineTwoText = uploadValueLocal;
        break breakMeUploadValue;
        }
        if(widgetTextViewLineThreeText.equals("")){
        widgetTextViewLineThreeText = uploadValueLocal;
        break breakMeUploadValue;
        }
        if(widgetTextViewLineFourText.equals("")){
        widgetTextViewLineFourText = uploadValueLocal;
        break breakMeUploadValue;
        }
        if(widgetTextViewLineFiveText.equals("")){
        widgetTextViewLineFiveText = uploadValueLocal;
        break breakMeUploadValue;
        }

        }


        breakMeDownloadValue:if(true){
        String downloadValueLocal = receivedString;
        if(displayTransferRateLabels){
        downloadValueLocal = "Down: " + receivedString;
        }
        if(widgetTextViewLineOneText.equals("")){
        widgetTextViewLineOneText = downloadValueLocal;
        break breakMeDownloadValue;
        }
        if(widgetTextViewLineTwoText.equals("")){
        widgetTextViewLineTwoText = downloadValueLocal;
        break breakMeDownloadValue;
        }
        if(widgetTextViewLineThreeText.equals("")){
        widgetTextViewLineThreeText = downloadValueLocal;
        break breakMeDownloadValue;
        }
        if(widgetTextViewLineFourText.equals("")){
        widgetTextViewLineFourText = downloadValueLocal;
        break breakMeDownloadValue;
        }
        if(widgetTextViewLineFiveText.equals("")){
        widgetTextViewLineFiveText = downloadValueLocal;
        break breakMeDownloadValue;
        }

        }


        //widgetColor = setColorOfWidget(colorOfFont, customColorOfFont);

        if(bytesSentSinceBoot < 0 || bytesReceivedSinceBoot < 0 || bytesSentAndReceivedSinceBoot < 0){
        widgetTextViewLineOneText = "Device Unsupported, please email me so I can help";
        }



        RemoteViews v = new RemoteViews(getPackageName(), R.layout.widget);

        v.setTextViewText(R.id.widgetTextViewLineOne, widgetTextViewLineOneText);
        v.setTextViewText(R.id.widgetTextViewLineTwo, widgetTextViewLineTwoText);
        v.setTextViewText(R.id.widgetTextViewLineThree, widgetTextViewLineThreeText);
        v.setTextViewText(R.id.widgetTextViewLineFour, widgetTextViewLineFourText);
        //v.setTextViewText(R.id.widgetTextViewLineFive, widgetTextViewLineFiveText);

        v.setTextColor( R.id.widgetTextViewLineOne, widgetColor);
        v.setTextColor( R.id.widgetTextViewLineTwo, widgetColor);
        v.setTextColor( R.id.widgetTextViewLineThree, widgetColor);
        v.setTextColor( R.id.widgetTextViewLineFour, widgetColor);
        //v.setTextColor( R.id.widgetTextViewLineFive, widgetColor);

        Float tempFloat= Float.parseFloat(sizeOfFont);

        v.setFloat(R.id.widgetTextViewLineOne, "setTextSize", tempFloat);
        v.setFloat(R.id.widgetTextViewLineTwo, "setTextSize", tempFloat);
        v.setFloat(R.id.widgetTextViewLineThree, "setTextSize", tempFloat);
        v.setFloat(R.id.widgetTextViewLineFour, "setTextSize", tempFloat);
        //v.setFloat(R.id.widgetTextViewLineFive, "setTextSize", tempFloat);
        manager.updateAppWidget(awID, v);
        widgetTextViewLineOneText = "";

        //HAD THIS BEFORE ABOVE



//        previousBytesSentAndReceivedSinceBoot = bytesSentAndReceivedSinceBoot;
//        previousBytesSentSinceBoot = bytesSentSinceBoot;
//        previousBytesReceivedSinceBoot = bytesReceivedSinceBoot;

        }

        }


    private void convertBytesPerSecondValuesToUnitMeasurement(String unitMeasurement) {

        if (unitMeasurement.equals("bps")){
            total = convertBpsTobps(bytesSentAndReceivedPerSecond);
            sent = convertBpsTobps(bytesSentPerSecond);
            received = convertBpsTobps(bytesReceivedPerSecond);
            ultimateUnitOfMeasure = "bps";
            return;
        }
        if (unitMeasurement.equals("Kbps")){
            total = convertBpsToKbps(bytesSentAndReceivedPerSecond);
            sent = convertBpsToKbps(bytesSentPerSecond);
            received = convertBpsToKbps(bytesReceivedPerSecond);
            ultimateUnitOfMeasure = "kbps";
            return;
        }
        if (unitMeasurement.equals("Mbps")){
            total = convertBpsToMbps(bytesSentAndReceivedPerSecond);
            sent = convertBpsToMbps(bytesSentPerSecond);
            received = convertBpsToMbps(bytesReceivedPerSecond);
            ultimateUnitOfMeasure = "Mbps";
            return;
        }
        if (unitMeasurement.equals("Gbps")){
            total = convertBpsToGbps(bytesSentAndReceivedPerSecond);
            sent = convertBpsToGbps(bytesSentPerSecond);
            received = convertBpsToGbps(bytesReceivedPerSecond);
            ultimateUnitOfMeasure = "Gbps";
            return;
        }
        if (unitMeasurement.equals("Bps")){
            total = convertBpsToBps(bytesSentAndReceivedPerSecond);
            sent = convertBpsToBps(bytesSentPerSecond);
            received = convertBpsToBps(bytesReceivedPerSecond);
            ultimateUnitOfMeasure = "Bps";
            return;
        }
        if (unitMeasurement.equals("KBps")){
            total = convertBpsToKBps(bytesSentAndReceivedPerSecond);
            sent = convertBpsToKBps(bytesSentPerSecond);
            received = convertBpsToKBps(bytesReceivedPerSecond);
            ultimateUnitOfMeasure = "kBps";
            return;
        }
        if (unitMeasurement.equals("MBps")){
            total = convertBpsToMBps(bytesSentAndReceivedPerSecond);
            sent = convertBpsToMBps(bytesSentPerSecond);
            received = convertBpsToMBps(bytesReceivedPerSecond);
            ultimateUnitOfMeasure = "MBps";
            return;
        }
        if (unitMeasurement.equals("GBps")){
            total = convertBpsToGBps(bytesSentAndReceivedPerSecond);
            sent = convertBpsToGBps(bytesSentPerSecond);
            received = convertBpsToGBps(bytesReceivedPerSecond);
            ultimateUnitOfMeasure = "GBps";
            return;
        }

    }

}
