package com.richardlucasapps.netlive;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class AppDataUsage {
	
	private String appName;
	private int uId;
	private long previousTotalData;
	private File dir;
	private File uidFileDir;
	private File uidActualFileReceived;
	private File uidActualFileSent;
	private BufferedReader brReceived;
    private BufferedReader brSent;
    private String textReceived;
	private String textSent;
	private String receivedLine;
    private String sentLine;
    
    private final String uidStatPath = "/proc/uid_stat/";
    private final String uidRcv = "tcp_rcv";
    private final String uidSnd = "tcp_snd";

	@Override
	public boolean equals(Object obj) {

	    final AppDataUsage other = (AppDataUsage) obj;
	    if(this.appName.equals(other.appName)){
	    	return true;
	    }
		return false;
	}

	public AppDataUsage(String appName1, int uid1) {
		this.appName = appName1;
		this.uId = uid1;
		this.previousTotalData = getTotalBytesManual(uid1);
		
	}
	
	private Long getTotalBytesManual(int localUid){
	
	dir = new File(uidStatPath);
	String[] children = dir.list();
	if(!Arrays.asList(children).contains(String.valueOf(localUid))){
		return 0L;
	}
	uidFileDir = new File(uidStatPath+String.valueOf(localUid));
	uidActualFileReceived = new File(uidFileDir,uidRcv);
	uidActualFileSent = new File(uidFileDir,uidSnd);
	
	 textReceived = "0";
	 textSent = "0";
	 
	 try {
	        brReceived = new BufferedReader(new FileReader(uidActualFileReceived));
	        brSent = new BufferedReader(new FileReader(uidActualFileSent));

	        if ((receivedLine = brReceived.readLine()) != null) {
	        	textReceived = receivedLine;
	        }
	        if ((sentLine = brSent.readLine()) != null) {
	        	textSent = sentLine;
	        }
	        
	    }
	    catch (IOException e) {
	    	
	    }
	 return Long.valueOf(textReceived).longValue() + Long.valueOf(textSent).longValue();
	 
	}
	
	public Long getRate(){
		Long currentTotalData = getTotalBytesManual(uId);
		Long rate = currentTotalData - previousTotalData;
		previousTotalData = currentTotalData;
		return rate;
	}
	
	public int getuId() {
		return uId;
	}

	public void setuId(int uId) {
		this.uId = uId;
	}
	
	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	@Override
	public String toString(){
		return String.valueOf(uId) + String.valueOf(previousTotalData); 
		
	}
}
