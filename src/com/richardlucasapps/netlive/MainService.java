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
import android.content.Intent;
import android.content.SharedPreferences;
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
	private String fastApp;
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
	        
	        //clickIntent notificationIntent = new Intent(this, MainActivity.class);
	        
	        mNotifyMgr = 
	                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	        mId = 1;
	        mBuilder = new NotificationCompat.Builder(this)
			.setSmallIcon(R.drawable.idle) //R.drawable.ic_launcher
		    .setContentTitle("")
		    .setContentText("")
            //.setPriority(Notification.PRIORITY_MIN)
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

//        boolean firstRunSinceUpdate = getSharedPreferences("delete_data", MODE_PRIVATE).getBoolean("firstRunSinceUpdate", true);
//        if (firstRunSinceUpdate){
//            Log.d("TAG123456", "made it here12");
//            MyApplication.getInstance().clearApplicationData();//This will delete the data from the previous installation
//            //getSharedPreferences("START_UP_PREFERENCE", MODE_PRIVATE).getBoolean("firstRunSinceUpdateV1ToV1Point1", false);
//            getSharedPreferences("delete_data", MODE_PRIVATE)
//                    .edit()
//                    .putBoolean("firstRunSinceUpdate", false)
//                    .commit();
//        }

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
        if(showActiveApp && appMonitorCounter >=10){//TODO change the 10 to something higher, maybe check every 60 seconds
        	if(appDataUsageList.isEmpty()){
                loadAllAppsIntoAppDataUsageList();
                Log.d("loading all", "apps into list");
            }
            fastApp = getCurrentInstalledApps();
        	appMonitorCounter = 0;
        }
       

    	
    	String displayValuesText = "";
        displayValuesText += " Up: " + sentString;
        displayValuesText += " Down: " +  receivedString;

    	
    	String contentTitleText = "";
/*TODO so many times throughout my code, I constantly check to see the settings of things, e.g. I am constantly checking here if showActiveApp is enabled.
      There has got to be a better way to do this.  Maybe get some strategy pattern action involved.
        */
    	if(showActiveApp){
    		contentTitleText = ultimateUnitOfMeasure + " " + fastApp;
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
    		appLabel1 = " " + "(" + currentApp.getAppName() + ")";
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


}
