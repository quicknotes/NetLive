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

    private String activeApp = "";
    List<AppDataUsage> appDataUsageList;
    int appMonitorCounter;

    int mId;

    NotificationCompat.Builder mBuilder;
    NotificationManager mNotifyMgr;
    Notification notification;
    SharedPreferences sharedPref;

    ScheduledFuture updateHandler;
    Intent resultIntent;

    Context context;
    ComponentName name;

    UnitConverter converter;
    long pollRate;
    NotificationContentTitleSetter titleSetter;

    String displayValuesText = "";
    String unitMeasurement;
    boolean showActiveApp;
    String contentTitleText = "";

    ActiveAppGetter activeAppGetter;

    PowerManager pm;
    boolean autoStart;

    List<UnitConverter> widgetUnitMeasurementConverters;
    List<RemoteViews> widgetRemoteViews;

    int[] ids;
    AppWidgetManager manager;
    int N;

    /*
    So this guys is interesting.  Here is my problem.  When the user turns their screen on, the service will pick up reporting where it left off.
    But since it has no notion of how long it has been so it actually updated, upon the first update the numbers will be artificially high-
    reporting your transfer rate much higher than it actually is.  So this variable will keep track of this down time, and I will use it
    to prevent a spike if the user looks at NetLive's reporting as soon as they turn on their screen.
     */
    int updatesMissed;

    @Override
    public void onCreate() {
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        autoStart = !(sharedPref.getBoolean("pref_key_auto_start", false));
        boolean widgetExist = sharedPref.getBoolean("widget_exists", false);

        if (!autoStart && !widgetExist) {
            this.onDestroy();
            return;
        }

        updatesMissed = 0;

        unitMeasurement = sharedPref.getString("pref_key_measurement_unit", "Mbps");
        pollRate = Long.parseLong(sharedPref.getString("pref_key_poll_rate", "1"));
        showActiveApp = sharedPref.getBoolean("pref_key_active_app", true);

        converter = getUnitConverter(unitMeasurement);
        titleSetter = getNotificationContentTitleSetter(showActiveApp);
        context = getApplicationContext();

        appMonitorCounter = 0;

        previousBytesSentSinceBoot = TrafficStats.getTotalTxBytes();//i dont initialize these to 0, because if i do, when app first reports, the rate will be crazy high
        previousBytesReceivedSinceBoot = TrafficStats.getTotalRxBytes();
        appDataUsageList = new ArrayList<AppDataUsage>();

        loadAllAppsIntoAppDataUsageList();
        activeAppGetter = determineUidDataGatherMethod();

        if (widgetExist) {
            setupWidgets();
        }

        if (autoStart) {
            mNotifyMgr =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mId = 1;
            mBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.idle)
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
        }
        if (autoStart && !widgetExist) {
            startOnlyNotifcationEnabledService(pollRate);
        }

        if (autoStart && widgetExist) {
            startNotifcationAndWidgetEnabledService(pollRate);
        }

        if (!autoStart && widgetExist) {
            startOnlyWidgetEnabledService(pollRate);
        }
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onDestroy() {
        try {
            updateHandler.cancel(true);
        } catch (NullPointerException e) {
            //The only way there will be a null pointer, is if the disabled preference is checked.  Because if it is, onDestory() is called right away, without creating the updateHandler
        }
        this.stopSelf();
        super.onDestroy();

    }

    private ActiveAppGetter determineUidDataGatherMethod() {
        //TODO I think I will have to maintain that useTrafficStatsAPI boolean from this class.  And simply separtate the methods within app data usage and call them accordingly

        long bytesTransferred = 0L;

        for (AppDataUsage currentApp : appDataUsageList) {
            int uid = currentApp.getUid();
            bytesTransferred = TrafficStats.getUidRxBytes(uid) + TrafficStats.getUidTxBytes(uid);
            if (bytesTransferred > 0) {
                return setUidDataGatherMethodToTrafficStatsAPI(true);

            }
        }

        return setUidDataGatherMethodToTrafficStatsAPI(false);

    }

    private ActiveAppGetter setUidDataGatherMethodToTrafficStatsAPI(boolean b) {
        if (b) {

            return (new ActiveAppGetter() {
                @Override
                public String getActiveApp() {

                    long maxDelta = 0L;
                    long delta = 0L;
                    String appLabel = "";

                    for (AppDataUsage currentApp : appDataUsageList) {
                        delta = currentApp.getRateWithTrafficStatsAPI();

                        if (delta > maxDelta) {
                            appLabel = currentApp.getAppName();
                            maxDelta = delta;
                        }
                    }
                    if (appLabel.equals("")) {
                        return "...";
                    }
                    return appLabel;

                }
            });
        } else {

            return (new ActiveAppGetter() {
                @Override
                public String getActiveApp() {

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
                    if (appLabel.equals("")) {
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

    public void startOnlyNotifcationEnabledService(long pollRate) {
        final Runnable beeper = new Runnable() {
            public void run() {
                updateOnlyNotificationEnabled();
            }
        };
        updateHandler =
                scheduler.scheduleAtFixedRate(beeper, 1, pollRate, TimeUnit.SECONDS);
    }

    public void startNotifcationAndWidgetEnabledService(long pollRate) {
        final Runnable beeper = new Runnable() {
            public void run() {
                updateNotificationAndWidgetEnabled();
            }
        };
        updateHandler =
                scheduler.scheduleAtFixedRate(beeper, 1, pollRate, TimeUnit.SECONDS);
    }

    public void startOnlyWidgetEnabledService(long pollRate) {
        final Runnable beeper = new Runnable() {
            public void run() {
                updateOnlyWidgetEnabled();
            }
        };
        updateHandler =
                scheduler.scheduleAtFixedRate(beeper, 1, pollRate, TimeUnit.SECONDS);
    }


    private void updateOnlyNotificationEnabled() {
        if (!pm.isScreenOn()) {//TODO a snazier thing might be to do a broadcast receiver that pauses the schedule executor service when screen is off, and renables when screen on.
            updatesMissed+=1;
            return;          //I don't think cancelling the service all together would be a good idea when screen is off, I don't want to keep calling onCreate when the user turns their screen on
        }

        long correctedPollRate;
        if(updatesMissed!=0){
            correctedPollRate = pollRate * updatesMissed;
        }else{
            correctedPollRate = pollRate;
        }
        updatesMissed = 0;


        bytesSentSinceBoot = TrafficStats.getTotalTxBytes();
        bytesReceivedSinceBoot = TrafficStats.getTotalRxBytes();

        bytesSentPerSecond = bytesSentSinceBoot - previousBytesSentSinceBoot;
        bytesReceivedPerSecond = bytesReceivedSinceBoot - previousBytesReceivedSinceBoot;

        sentString = String.format("%.3f", converter.convert(bytesSentPerSecond / correctedPollRate));
        receivedString = String.format("%.3f", converter.convert(bytesReceivedPerSecond / correctedPollRate));

        previousBytesSentSinceBoot = bytesSentSinceBoot;
        previousBytesReceivedSinceBoot = bytesReceivedSinceBoot;

        appMonitorCounter += 1;
        if (appMonitorCounter >= (500 / pollRate)) {//divide by pollRate so that if you have a pollRate of 10, that will end up being 500 seconds, not 5000
            loadAllAppsIntoAppDataUsageList();
            appMonitorCounter = 0;
        }

        displayValuesText = " Up: " + sentString + " Down: " + receivedString;
        activeApp = titleSetter.set();
        contentTitleText = unitMeasurement + " " + activeApp;

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


        mBuilder.setWhen(System.currentTimeMillis());



        if (bytesSentPerSecond / correctedPollRate < 13107 && bytesReceivedPerSecond / correctedPollRate < 13107) {
            mBuilder.setSmallIcon(R.drawable.idle);
            mNotifyMgr.notify(mId, mBuilder.build());
            return;
        }

        if (!(bytesSentPerSecond / correctedPollRate > 13107) && bytesReceivedPerSecond / correctedPollRate > 13107) {
            mBuilder.setSmallIcon(R.drawable.download);
            mNotifyMgr.notify(mId, mBuilder.build());
            return;
        }

        if (bytesSentPerSecond / correctedPollRate > 13107 && bytesReceivedPerSecond / correctedPollRate < 13107) {
            mBuilder.setSmallIcon(R.drawable.upload);
            mNotifyMgr.notify(mId, mBuilder.build());
            return;
        }

        if (bytesSentPerSecond / correctedPollRate > 13107 && bytesReceivedPerSecond / correctedPollRate > 13107) {//1307 bytes is equal to .1Mbit
            mBuilder.setSmallIcon(R.drawable.both);
            mNotifyMgr.notify(mId, mBuilder.build());
        }

    }

    private void updateOnlyWidgetEnabled() {
        updateWidgets();
    }

    private void updateNotificationAndWidgetEnabled() {
        if (!pm.isScreenOn()) {//TODO a snazier thing might be to do a broadcast receiver that pauses the schedule executor service when screen is off, and renables when screen on.
            updatesMissed+=1;
            return;          //I don't think cancelling the service all together would be a good idea when screen is off, I don't want to keep calling onCreate when the user turns their screen on
        }

        long correctedPollRate;
        if(updatesMissed!=0){
            correctedPollRate = pollRate * updatesMissed;
        }else{
            correctedPollRate = pollRate;
        }
        updatesMissed = 0;

        bytesSentSinceBoot = TrafficStats.getTotalTxBytes();
        bytesReceivedSinceBoot = TrafficStats.getTotalRxBytes();

        bytesSentPerSecond = bytesSentSinceBoot - previousBytesSentSinceBoot;
        bytesReceivedPerSecond = bytesReceivedSinceBoot - previousBytesReceivedSinceBoot;

        sentString = String.format("%.3f", converter.convert(bytesSentPerSecond) / correctedPollRate);
        receivedString = String.format("%.3f", converter.convert(bytesReceivedPerSecond) / correctedPollRate);

        previousBytesSentSinceBoot = bytesSentSinceBoot;
        previousBytesReceivedSinceBoot = bytesReceivedSinceBoot;

        appMonitorCounter += 1;
        if (appMonitorCounter >= 500 / pollRate) {//divide by pollRate so that if you have a pollRate of 10, that will end up being 500 seconds, not 5000
            loadAllAppsIntoAppDataUsageList();
            appMonitorCounter = 0;
        }

        displayValuesText = " Up: " + sentString + " Down: " + receivedString;
        activeApp = titleSetter.set();
        contentTitleText = unitMeasurement + " " + activeApp;

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


        mBuilder.setWhen(System.currentTimeMillis());


        for (int i = 0; i < N; i++) {
            int awID = ids[i];

            boolean displayActiveApp = sharedPref.getBoolean("pref_key_widget_active_app" + awID, true);

            String widgetTextViewLineOneText = "";

            if (displayActiveApp) {
                widgetTextViewLineOneText = activeApp + "\n";
            }

            UnitConverter c = widgetUnitMeasurementConverters.get(i);

            String sentString = String.format("%.3f", c.convert(bytesSentPerSecond) / correctedPollRate);
            String receivedString = String.format("%.3f", c.convert(bytesReceivedPerSecond) / correctedPollRate);

            widgetTextViewLineOneText += unitMeasurement + "\n";
            widgetTextViewLineOneText += "Up: " + sentString + "\n";
            widgetTextViewLineOneText += "Down: " + receivedString + "\n";

            RemoteViews v = widgetRemoteViews.get(i);
            v.setTextViewText(R.id.widgetTextViewLineOne, widgetTextViewLineOneText);
            manager.updateAppWidget(awID, v);

        }



        if (bytesSentPerSecond / correctedPollRate < 13107 && bytesReceivedPerSecond / correctedPollRate < 13107) {
            mBuilder.setSmallIcon(R.drawable.idle);
            mNotifyMgr.notify(mId, mBuilder.build());
            return;
        }

        if (!(bytesSentPerSecond / correctedPollRate > 13107) && bytesReceivedPerSecond / correctedPollRate > 13107) {
            mBuilder.setSmallIcon(R.drawable.download);
            mNotifyMgr.notify(mId, mBuilder.build());
            return;
        }

        if (bytesSentPerSecond / correctedPollRate > 13107 && bytesReceivedPerSecond / correctedPollRate < 13107) {
            mBuilder.setSmallIcon(R.drawable.upload);
            mNotifyMgr.notify(mId, mBuilder.build());
            return;
        }

        if (bytesSentPerSecond / correctedPollRate > 13107 && bytesReceivedPerSecond / correctedPollRate > 13107) {//1307 bytes is equal to .1Mbit
            mBuilder.setSmallIcon(R.drawable.both);
            mNotifyMgr.notify(mId, mBuilder.build());
        }

    }

    private void setupWidgets() {

        name = new ComponentName(context, NetworkSpeedWidget.class);
        ids = AppWidgetManager.getInstance(context).getAppWidgetIds(name);
        manager = AppWidgetManager.getInstance(this);


        widgetUnitMeasurementConverters = new ArrayList<UnitConverter>();
        widgetRemoteViews = new ArrayList<RemoteViews>();

        N = ids.length;

        for (int i = 0; i < N; i++) {
            int awID = ids[i];

            String colorOfFont = sharedPref.getString("pref_key_widget_font_color" + awID, "Black");
            String sizeOfFont = sharedPref.getString("pref_key_widget_font_size" + awID, "12.0");
            String measurementUnit = sharedPref.getString("pref_key_widget_measurement_unit" + awID, "Mbps");

            int widgetColor;
            widgetColor = Color.parseColor(colorOfFont);


            RemoteViews v = new RemoteViews(getPackageName(), R.layout.widget);
            v.setTextColor(R.id.widgetTextViewLineOne, widgetColor);
            Float tempFloat = Float.parseFloat(sizeOfFont);
            v.setFloat(R.id.widgetTextViewLineOne, "setTextSize", tempFloat);

            widgetRemoteViews.add(v);
            UnitConverter converter = getUnitConverter(measurementUnit);
            widgetUnitMeasurementConverters.add(converter);


        }
    }


    private void loadAllAppsIntoAppDataUsageList() {
        PackageManager packageManager = this.getPackageManager();
        List<ApplicationInfo> appList = packageManager.getInstalledApplications(0);

        for (ApplicationInfo appInfo : appList) {
            String appLabel = (String) packageManager.getApplicationLabel(appInfo);
            int uid = appInfo.uid;
            AppDataUsage app = new AppDataUsage(appLabel, uid);
            appDataUsageList.add(app);

        }
    }


    private void updateWidgets() {

        if (!pm.isScreenOn()) {//TODO a snazier thing might be to do a broadcast receiver that pauses the schedule executor service when screen is off, and renables when screen on.
            updatesMissed+=1;
            return;          //I don't think cancelling the service all together would be a good idea when screen is off, I don't want to keep calling onCreate when the user turns their screen on
        }

        long correctedPollRate;
        if(updatesMissed!=0){
            correctedPollRate = pollRate * updatesMissed;
        }else{
            correctedPollRate = pollRate;
        }
        updatesMissed = 0;

        bytesSentSinceBoot = TrafficStats.getTotalTxBytes();
        bytesReceivedSinceBoot = TrafficStats.getTotalRxBytes();

        bytesSentPerSecond = bytesSentSinceBoot - previousBytesSentSinceBoot;
        bytesReceivedPerSecond = bytesReceivedSinceBoot - previousBytesReceivedSinceBoot;

        previousBytesSentSinceBoot = bytesSentSinceBoot;
        previousBytesReceivedSinceBoot = bytesReceivedSinceBoot;

        appMonitorCounter += 1;
        if (appMonitorCounter >= 500 / pollRate) {//divide by pollRate so that if you have a pollRate of 10, that will end up being 500 seconds, not 5000
            loadAllAppsIntoAppDataUsageList();
            appMonitorCounter = 0;
        }

        activeApp = titleSetter.set();

        for (int i = 0; i < N; i++) {
            int awID = ids[i];

            boolean displayActiveApp = sharedPref.getBoolean("pref_key_widget_active_app" + awID, true);


            String widgetTextViewLineOneText = "";

            if (displayActiveApp) {
                widgetTextViewLineOneText = activeApp + "\n";
            }

            UnitConverter c = widgetUnitMeasurementConverters.get(i);

            String sentString = String.format("%.3f", c.convert(bytesSentPerSecond) / correctedPollRate);
            String receivedString = String.format("%.3f", c.convert(bytesReceivedPerSecond) / correctedPollRate);


            widgetTextViewLineOneText += unitMeasurement + "\n";
            widgetTextViewLineOneText += "Up: " + sentString + "\n";
            widgetTextViewLineOneText += "Down: " + receivedString + "\n";

            RemoteViews v = widgetRemoteViews.get(i);
            v.setTextViewText(R.id.widgetTextViewLineOne, widgetTextViewLineOneText);
            manager.updateAppWidget(awID, v);

        }

    }

}
