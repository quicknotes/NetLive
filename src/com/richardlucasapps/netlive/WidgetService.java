package com.richardlucasapps.netlive;

import java.util.LinkedList;
import java.util.List;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.TrafficStats;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

public class WidgetService extends Service
{
	
    Long bytesSentAndReceivedSinceBoot;
	Long bytesSentSinceBoot;
    Long bytesReceivedSinceBoot;

    Long previousBytesSentAndReceivedSinceBoot;
    Long previousBytesSentSinceBoot;
    Long previousBytesReceivedSinceBoot;

    Long bytesSentAndReceivedPerSecond;
    Long bytesSentPerSecond;
    Long bytesReceivedPerSecond;

	double total;
	double sent;
	double received;

    String totalString;
    String sentString;
    String receivedString;

	String ultimateUnitOfMeasure;

    int appMonitorCounter;
    boolean bool;
	SharedPreferences sharedPref;
    String fastApp;
    List<AppDataUsage> appDataUsageList;
    String appLabel1;
    PowerManager pm;
    int getCurrentInstalledAppsCount;


    AppWidgetConfigurePreferencesFragment widgetConfigure;

    @Override
    public void onCreate()
    {
    	sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    	//startService(new Intent(this, MainService.class));
        super.onCreate();

        //test
        previousBytesSentAndReceivedSinceBoot = 0L;
        previousBytesSentSinceBoot = 0L;
        previousBytesReceivedSinceBoot = 0L;
        appMonitorCounter = 0;
        appDataUsageList = new LinkedList();
        fastApp = "";
        getCurrentInstalledAppsCount = 5;
        //pm  = ((PowerManager) getSystemService(Context.POWER_SERVICE));
        widgetConfigure = new AppWidgetConfigurePreferencesFragment();
       
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
         buildUpdate();

        return super.onStartCommand(intent, flags, startId);
    }

    private void buildUpdate()
    {
//    	boolean isScreenOn = pm.isScreenOn();
//    	//Log.d("isScreenOn", String.valueOf(isScreenOn));
//    	if(!isScreenOn ){//|| widgetConfigure.isWidgetPreferencePaneOpen()){
//    		return;
//    	}
    	//DO NOT BUILD UPDATE IF SCREEN OFF, LIKE IN MAIN SERVICE
    	
    	setTotalSentReceiveBytesPerSecond();
    	

	
		
		AppWidgetManager manager = AppWidgetManager.getInstance(this);
		ComponentName thisWidget = new ComponentName(this, NetworkSpeedWidget.class);
        
		int[]ids = manager.getAppWidgetIds(thisWidget);
        final int N = ids.length;
        for (int i = 0; i < N; i++){
        	int awID = ids[i];

            //How can I make this so I don't have to check and recheck every damn time?
            bool = sharedPref.getBoolean("pref_key_widget_display_unit_of_measure"+awID, true);
       	    String unitOfMeasure = sharedPref.getString("pref_key_widget_measurement_unit"+awID, "Mbps");
            boolean displayUnitOfMeasure = true;
			//boolean displayUnitOfMeasure = sharedPref.getBoolean("pref_key_widget_display_unit_of_measure"+awID, true);
            boolean displayTransferRateLabels = true;
            //boolean displayTransferRateLabels = sharedPref.getBoolean("pref_key_widget_transfer_rate_names"+awID, true);
			boolean displayActiveApp = sharedPref.getBoolean("pref_key_widget_active_app"+awID, true);
			String colorOfFont = sharedPref.getString("pref_key_widget_font_color"+awID, "Black");
			String sizeOfFont = sharedPref.getString("pref_key_widget_font_size"+awID, "12.0");
			
			
			//CURRENTLY HERE, INCORPORATE NUMBEROFLINESOFWISGET TO DYNAMICALLY CHANGE WIDGET SIZE
			//ALSO, IF NUMBER OF LINES IS 0, THEN DONT EVEN DISPLAY THE WIDGET, DO A TOAST LIKE "NOTHING DISPLAYED"
			
			String widgetTextViewLineOneText = "";
	    	String widgetTextViewLineTwoText = "";
	    	String widgetTextViewLineThreeText = "";
	    	String widgetTextViewLineFourText = "";
	    	String widgetTextViewLineFiveText = "";
			

			
			convertBytesPerSecondValuesToUnitMeasurement(unitOfMeasure);
			
			 appMonitorCounter+=1;  //if several widgets, then one will be added to this more than once per 5 seconds, solved this doing N*5
		        if(appMonitorCounter >= N*5 && displayActiveApp){
		        	fastApp = getCurrentInstalledApps();
		        	appMonitorCounter = 0;
		        }
		        
		     sentString = String.format("%.3f", sent);
		     receivedString = String.format("%.3f", received);
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
   	 		
   	 	
   	 		
   	 		previousBytesSentAndReceivedSinceBoot = bytesSentAndReceivedSinceBoot;
   	 		previousBytesSentSinceBoot = bytesSentSinceBoot;
   	 		previousBytesReceivedSinceBoot = bytesReceivedSinceBoot;
       	 	
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

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
    
    //Reconstruction, make this shit independent
    
    private void setTotalSentReceiveBytesPerSecond() {
   	 	bytesSentSinceBoot = TrafficStats.getTotalTxBytes();
        bytesReceivedSinceBoot = TrafficStats.getTotalRxBytes();
        bytesSentAndReceivedSinceBoot = bytesSentSinceBoot + bytesReceivedSinceBoot;
        
        bytesSentPerSecond = bytesSentSinceBoot - previousBytesSentSinceBoot;
        bytesReceivedPerSecond = bytesReceivedSinceBoot - previousBytesReceivedSinceBoot;
        bytesSentAndReceivedPerSecond = bytesSentAndReceivedSinceBoot - previousBytesSentAndReceivedSinceBoot;
   	
   }
    
    private String getCurrentInstalledApps() {
    	
    	getCurrentInstalledAppsCount+=1;
    	if(getCurrentInstalledAppsCount>5){
    	PackageManager packageManager=this.getPackageManager();
        List<ApplicationInfo> appList=packageManager.getInstalledApplications(0);
     
        for (ApplicationInfo appInfo : appList) {
        	String appLabel = (String) packageManager.getApplicationLabel(appInfo);
        	//Log.d("asdf", appLabel);
        	int uid = appInfo.uid;
        	//Log.d("data", String.valueOf(android.net.TrafficStats.getUidRxBytes(uid) + android.net.TrafficStats.getUidTxBytes(uid)));
        	AppDataUsage app = new AppDataUsage(appLabel, uid);
        	if(!appDataUsageList.contains(app)){
        		//Log.d("asdfad", "got here");
        		appDataUsageList.add(app);
        	}
        }
        getCurrentInstalledAppsCount = 0;
    	}
        
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
    
}
