package com.example.admin.a2018iotkorea.ble;

public class AppController {
	public String clickerBleDeviceAddress=null;
	public String clickerProfileName=null;
	public int clickerProfileId=-1;
	public boolean bleDeviceServiceConnected=false;
	public boolean clickerAudibleOn=false;
	public boolean fallDectionOn=true;   
	public String loggerTimeStamp=null;
	public String logFilePath;

	public String polarBleDeviceAddress="00:22:D0:9C:FA:7A";
	public boolean polarBleDeviceServiceConnected=false;

	private static AppController instance = new AppController();
	public static AppController getInstance() {
		return instance;
	}	
	

}

