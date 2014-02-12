package com.richardlucasapps.netlive;

import android.net.TrafficStats;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class AppDataUsage {
	
	private String appName;
	private int uId;
	private long previousTotalData;

//    private File uidFileDir;
//
//    private File dir;
//	  private File uidActualFileReceived;
//	  private File uidActualFileSent;
//
//	  private static BufferedReader brReceived;
//    private static BufferedReader brSent;
////    private static String textReceived;
////	private static String textSent;
////	private static String receivedLine;
////    private static String sentLine;

    private static final String uidStatPath = "/proc/uid_stat/";
    private static final String uidRcv = "tcp_rcv";
    private static final String uidSnd = "tcp_snd";


	public AppDataUsage(String appName1, int uid1) {
		this.appName = appName1;
		this.uId = uid1;
		this.previousTotalData = getTotalBytesManual();

//        this.dir = new File(uidStatPath);
//        this.uidFileDir = new File(uidStatPath+String.valueOf(this.uId));
//        this.uidActualFileReceived = new File(uidFileDir,uidRcv);
//        this.uidActualFileSent = new File(uidFileDir,uidSnd);
//
//        try{
//        brReceived = new BufferedReader(new FileReader(uidActualFileReceived));
//        brSent = new BufferedReader(new FileReader(uidActualFileSent));
//
//        }catch(Exception e){
//
//        }

	}

    public int getUid(){
        return this.uId;
    }

    /*
    http://developer.android.com/reference/android/net/TrafficStats.html#getUidRxPackets(int)
    I need this manual function because "Before JELLY_BEAN_MR2, this may return UNSUPPORTED on devices where statistics aren't available."
     */
	public Long getTotalBytesManual(){

        File dir                    = new File(uidStatPath);
        File uidFileDir             = new File(uidStatPath+String.valueOf(this.uId));
        File uidActualFileReceived  = new File(uidFileDir,uidRcv);
        File uidActualFileSent      = new File(uidFileDir,uidSnd);

        String[] children = dir.list();
        if (!Arrays.asList(children).contains(String.valueOf(this.uId))) {
            return 0L;
        }

        String textReceived ="0";
        String textSent     ="0";

        try{
            BufferedReader brReceived   = new BufferedReader(new FileReader(uidActualFileReceived));
            BufferedReader brSent       = new BufferedReader(new FileReader(uidActualFileSent));
            textReceived         = brReceived.readLine();
            textSent             = brSent.readLine();
        }catch(FileNotFoundException e){

        }catch(IOException e){

        }
        return Long.valueOf(textReceived).longValue() + Long.valueOf(textSent).longValue();

    }

   public Long getStatsWithAPI(){
        return Long.valueOf(TrafficStats.getUidRxBytes(this.uId)) + Long.valueOf(TrafficStats.getUidTxBytes(this.uId));

    }

    public Long getRateWithTrafficStatsAPI(){
		long currentTotalData = getStatsWithAPI();
		long rate = currentTotalData - previousTotalData;
		previousTotalData = currentTotalData;
		return rate;
	}

    public Long getRateManual(){
        long currentTotalData = getTotalBytesManual();
        long rate = currentTotalData - previousTotalData;
        previousTotalData = currentTotalData;
        return rate;

    }

	public String getAppName() {
		return appName;
	}

	@Override
	public String toString(){
		return String.valueOf(uId) + String.valueOf(previousTotalData); 
		
	}
}
