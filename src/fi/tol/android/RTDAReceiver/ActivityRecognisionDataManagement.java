/**
 *  Copyright 2015   PIKESTA, FINLAND
 *
 *
 * 	This file is part of PalveluApu tool.
 * 	PalveluApu is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License version 2 (GPLv2) as published by
 *  the Free Software Foundation.
 * 	PalveluApu is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *  FOR A PARTICULAR PURPOSE.  See the GNU General Public License version 2 for
 *  more details.
 * 	You should have received a copy of the GNU General Public License version 2
 *  along with RTDAReceiver.  If not, see <http://www.gnu.org/licenses/gpl-2.0.html/>.
 */


/**
 *  Class ActivityRecognisionDataManagement
 */


package fi.tol.android.RTDAReceiver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import android.os.Environment;
import android.text.format.Time;
import android.util.Log;

public class ActivityRecognisionDataManagement {

	private Time recordTime;
	private SignalVectorItem signalVector;
	private AccelerometerItem accelerometerData;
	
	private FileOutputStream btFileOut;
	private OutputStreamWriter btOutWriter;
	private BufferedWriter btBufferWriter;
	
	/** Currently login user name */
	private String username;
	/** Number of Bluetooth allowed devices */
	private int numberOfAllowedDevices;
	/** List of allowed devices */
	private static ArrayList<String> allowedDevices;
	private String allowedDevicesFileName;
	
	public ActivityRecognisionDataManagement()
	{
		allowedDevices = new ArrayList<String>();
		/** Initiate some attributes */
        numberOfAllowedDevices = 0;
		allowedDevices.clear();
		/** Get current login user name from RTDA module's main Activity */
        username = RTDAReceiver.username;
		signalVector = new SignalVectorItem();
		accelerometerData = new AccelerometerItem();
		
		initiate();
	}
	
	public void setTime(Time time)
	{
		recordTime = time;
	}
	public Time getTime()
	{
		return recordTime;
	}

	public SignalVectorItem getSignalVector()
	{
		return signalVector;
	}
	
	public void setBluetoothRSSI(short rssi, String btAddress)
	{
		/** Set the remote BT device's RSSI value to correspoding index in the signalVector list */
		for(int index = 0; index < allowedDevices.size(); index++)
		{
			if(btAddress.equalsIgnoreCase(allowedDevices.get(index)))
			{
				signalVector.setSignal(index, rssi);
				break;
			}
		}
	}
	
	public void setAccelerometerData(long time,float x, float y, float z)
	{
		accelerometerData.addSerialValues(time,x, y, z);
	}
	
	/** Output Bluetooth Signal vector and Accelerometer data into file */
	public void outputRecordData()
	{
		String acceRecords = "";
		String btSignalRecords = "";
		String date = recordTime.monthDay + "-" + (recordTime.month + 1) + "-" + recordTime.year;

		/** File for accelerometer data */
		/*File acceFile = new File(Environment.getExternalStorageDirectory() + "/" + MainLogin.appHomeFolder + "/" +
				username + "/" + MainLogin.rtdaSubFolder + "/" + MainLogin.rtdaSignalVectorFolder +
				"/" + allowedDevicesFileName.substring(0, (allowedDevicesFileName.length() - 4)) +
				"_" + date + "_" + "accelerometer.csv");
		/** File for Bluetooth vector data */
		File btSignalVectorFile = new File(Environment.getExternalStorageDirectory() + "/" + MainLogin.appHomeFolder + "/" + 
				username + "/" + MainLogin.rtdaSubFolder + "/" + MainLogin.rtdaSignalVectorFolder +
				"/" + allowedDevicesFileName.substring(0, (allowedDevicesFileName.length() - 4)) +
				"_" + date +  "_" + "btSignalVector.csv");

		/** If accelerometer data file doesn't exist, then write the header of the file */
		/*if(acceFile.exists()==false)
		{
			acceRecords += "Time";
			acceRecords += accelerometerData.getColumns() + "\n";
		}
		/** If BLuetooth signal vector data file doesn't exist, then write the header of the file */
		if(btSignalVectorFile.exists() == false)
		{
			btSignalRecords += "Time,Time";
			for(int i = 0; i < allowedDevices.size(); i ++)
			{
				btSignalRecords += "," + allowedDevices.get(i);
			}
			btSignalRecords += "\n";
		}
		
		/** Prepare the content of record, including time, 
		 *  Bluetooth signal vector and Accelerometer related data */
		ArrayList<Short> signals = signalVector.getSignalVector();

		/*for(int i = 0; i < accelerometerData.getAccelerometerNum(); i ++)
		{
			acceRecords += accelerometerData.getAcceValueTime(i);
			acceRecords += accelerometerData.getAccelerometerValues(i);

		}*/
		
		btSignalRecords += recordTime.toMillis(true)+"," + recordTime.hour + ":" + recordTime.minute + ":" + recordTime.second;
		
		for(int i = 0; i < signals.size(); i ++)
		{
			btSignalRecords += "," + signals.get(i);
		}
		btSignalRecords += "\n";
		/*records += "," + accelerometerData.getAverageX() + "," + accelerometerData.getAverageY() + "," +
			accelerometerData.getAverageZ() + "," + accelerometerData.getAbsAverageX() + "," +
			accelerometerData.getAbsAverageY() + "," + accelerometerData.getAbsAverageZ() + "," +
			accelerometerData.getVariationX() + "," + accelerometerData.getVariationY() + "," +
			accelerometerData.getVariationZ() + "," + accelerometerData.getSumOne() + "," +
			accelerometerData.getAbsSum() + "," + accelerometerData.getsqSum() + "," +
			accelerometerData.getSumTwo() + "," + accelerometerData.getSumThree() + "\n";*/
		try{

			/** Save accelerometer data */
			/*acceFileOut = new FileOutputStream(acceFile, true);
			acceOutWriter = new OutputStreamWriter(acceFileOut);
			acceBufferWriter = new BufferedWriter(acceOutWriter);
			acceBufferWriter.write(acceRecords);
			acceBufferWriter.close();
			
			/** Save Bluetooth Signal Vector data */
			btFileOut = new FileOutputStream(btSignalVectorFile, true);
			btOutWriter = new OutputStreamWriter(btFileOut);
			btBufferWriter = new BufferedWriter(btOutWriter);
			btBufferWriter.write(btSignalRecords);
			btBufferWriter.close();
		}
		catch(FileNotFoundException exception){
			exception.printStackTrace();
		}
		catch(IOException ioException)
		{
			ioException.printStackTrace();
		}
		/** Initiate after recording data into file */
		initiate();
	}
	
	public void initiate()
	{
		signalVector.clearSignalVector();
		accelerometerData.initSerialValues();
	}
	
	public void recordData()
	{
		/** Calculate Accelerometer related data */
		//accelerometerData.calculateAverageValues();
		//accelerometerData.calculateVariations();
		
		/** Output Bluetooth Signal vector and Accelerometer data into file */
		outputRecordData();
	}
	
	/** Get the information of currently selected set of Bluetooth allowed devices */
	public void setAllowedDevice(String selectedSite)
	{
		/** Get selected set of Bluetooth allowed devices from DevicesManaging Class */
		allowedDevices = (ArrayList<String>) DevicesManaging.getInstance().getAllowedDevices();
		numberOfAllowedDevices = allowedDevices.size();
		allowedDevicesFileName = selectedSite;
		signalVector.initSignalVector(numberOfAllowedDevices);
	}
	
	public AccelerometerItem getAccelerometerData()
	{
		 return accelerometerData;
	}
	public String generateOutputFileName(String date)
	{
		String fileName = Environment.getExternalStorageDirectory() + "/" + MainLogin.appHomeFolder + "/" + 
		username + "/" + MainLogin.rtdaSubFolder + "/" + MainLogin.rtdaSignalVectorFolder +
		"/" + allowedDevicesFileName.substring(0, (allowedDevicesFileName.length() - 4)) +
		"_" + date + "_" + "accelerometer.csv";
		return fileName;
	}
	public String generateGPSFileName(String date)
	{
		String fileName = Environment.getExternalStorageDirectory() + "/" + MainLogin.appHomeFolder + "/" + 
		username + "/" + MainLogin.rtdaSubFolder + "/" + MainLogin.rtdaSignalVectorFolder +
		"/" + allowedDevicesFileName.substring(0, (allowedDevicesFileName.length() - 4)) +
		"_" + date + "_" + "gps.csv";
		return fileName;
	}
}
