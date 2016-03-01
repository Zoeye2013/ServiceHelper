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
 *  Class DevicesManaging
 *
 *  Class for managing Bluetooth devices list (including useless devices,
 *  allowed devices, current in-range devices) and for managing file I/O
 */
package fi.tol.android.RTDAReceiver;


import java.io.*;
import java.util.ArrayList;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Environment;
import android.text.format.Time;
import android.util.Log;

public class DevicesManaging {
	
	/** List to record useless, allowed and currently in-range devices */
	private ArrayList<DeviceItem> devicesList;
	private BluetoothAdapter localAdapter;
	
	/** File read and write attributes */
	private FileInputStream fileIn;
	private InputStreamReader inReader;
	private BufferedReader bfReader;
	private FileOutputStream fileOut;
	private OutputStreamWriter outWriter;
	private BufferedWriter bfWriter;
	
	private String lineTemp;
	private int canvasState;
	private int allowedDeviceNo = 0;
	private int deviceClass;
	
	/** List of allowed devices */
	private ArrayList<String> allowedDevices;
	
	/** An static instance of the class itself */
	private static DevicesManaging deviceMagInstance;
	
	/** App folder path in phone SDcard*/
	private String sdPath;
	
	private Time time = new Time();
	
	public static final int ON_OPEN_STATE = 1;
	public static final int ON_START_STATE = 2;
	public static final int ON_RUN_STATE = 3;
	public static final int ON_SHARE_STATE = 4;
	private String username;
	
	
	/** Constructor, initial members*/
	private DevicesManaging()
	{
		devicesList = new ArrayList<DeviceItem>();
		allowedDevices = new ArrayList<String>();
		localAdapter = BluetoothAdapter.getDefaultAdapter();
		canvasState = ON_OPEN_STATE;
		
		sdPath = Environment.getExternalStorageDirectory() + "/";
		username = RTDAReceiver.username;
		
		time.setToNow();
	}
	
	/** Read allowed devices info from .csv file into allowedDevices list*/
	public void readAllowedDevicesFile(String allowedDevicesFile)
	{
		lineTemp = "";
		String[] tempArr = {};//Split temp
		String address = "";

		clearAllowedDevices();
		
		try
		{
			/** Open the allowed devices file */
			File file = new File(sdPath + MainLogin.appHomeFolder + "/" + username + "/" + MainLogin.rtdaSubFolder + 
					"/" + MainLogin.rtdaAllowedFolder + "/" + allowedDevicesFile);
			fileIn = new FileInputStream(file);
			inReader = new InputStreamReader(fileIn);
			bfReader = new BufferedReader(inReader);
			
			int line = 1;
			/** Read the file line by line */
			while((lineTemp=bfReader.readLine()) != null) //Read one line
			{
				if (line > 1)
				{
					/** Split the line with "," */
					tempArr = lineTemp.split(",");
					/** Get BT address info */
					address = tempArr[(tempArr.length-1)];
					/** Get BT device type info */
					deviceClass = Integer.parseInt(tempArr[(tempArr.length -2)]);
					allowedDeviceNo = Integer.parseInt(tempArr[0]);
					/** Get BluetoothDevice according to the address */
					BluetoothDevice deviceItem = localAdapter.getRemoteDevice(address);
					
					/** Add into allowed device list */
					allowedDevices.add(address);
					
					/** Update the devicesList */
					addAllowedDevice(deviceItem);
				}
				line ++;
			}
			bfReader.close(); //close the file
			
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/** Write allowed devices info into .csv file when stop teaching BT device*/
	public void writeAllowedDevicesFile(String allowedFileName)
	{
		String allowedDeviceInfo = "";
		try
		{
			File file = new File(sdPath + MainLogin.appHomeFolder + "/" + username + "/" + MainLogin.rtdaSubFolder + 
					"/" + MainLogin.rtdaAllowedFolder + "/" + allowedFileName + ".csv");
			fileOut = new FileOutputStream(file); //only allow this App access it, overwrite mode
			outWriter = new OutputStreamWriter(fileOut);
			bfWriter = new BufferedWriter(outWriter);
			
			allowedDeviceInfo += "Device Number,Device Name,Device Class,Address\n";
			
			for(int i = 0; i < devicesList.size(); i++)
			{
				if(devicesList.get(i).getIsAllowed())
				{
					allowedDeviceInfo += devicesList.get(i).getBTDeviceNo() + "," 
						+ devicesList.get(i).getBluetoothDevice().getName() + ","
						+ devicesList.get(i).getBluetoothDevice().getBluetoothClass().getMajorDeviceClass()
						+ "," + devicesList.get(i).getBluetoothDevice().getAddress() + "\n";
				}
			}
			bfWriter.write(allowedDeviceInfo);
			bfWriter.close();
			
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	/** Add an item to devicesList marked as allowed device */
	public void addAllowedDevice(BluetoothDevice device)
	{
		/** If this allowed device doesn't exist in the devicesList, 
		 *  then add it to the devicesList */
		if(!isExist(device.getAddress()))
		{
			DeviceItem deviceItem = new DeviceItem();
			deviceItem.setBluetoothDevice(device);
			deviceItem.setBTdeviceNo(allowedDeviceNo);
			deviceItem.setIsAllowed(true);
			deviceItem.setIsUseless(false);
			if(canvasState == ON_RUN_STATE)
			{
				deviceItem.setBTMajorDeviceClass(deviceClass);
				deviceItem.setIsCurrent(false);
				deviceItem.setIsInScope(false);
			}
			else if(canvasState == ON_START_STATE)
			{
				deviceItem.setBTMajorDeviceClass(device.getBluetoothClass().getMajorDeviceClass());
				deviceItem.setIsCurrent(true);
				deviceItem.setIsInScope(true);
				allowedDeviceNo ++;
			}
			devicesList.add(deviceItem);
		}
		/** If this allowed device already existed in the devicesList,
		 *  then just update corresponding field of the record */
		else
		{
			for(int i = 0; i < devicesList.size(); i++)
			{
				if(devicesList.get(i).getBluetoothDevice().getAddress().equalsIgnoreCase(device.getAddress()))
				{
					if(canvasState == ON_RUN_STATE)
					{
						devicesList.get(i).setIsAllowed(true);
						devicesList.get(i).setIsUseless(false);
						devicesList.get(i).setBTdeviceNo(allowedDeviceNo);
						devicesList.get(i).setIsCurrent(false);
						devicesList.get(i).setIsInScope(false);
					}
					else if (!devicesList.get(i).getIsUseless())
					{
						devicesList.get(i).setIsCurrent(true);
						devicesList.get(i).setIsInScope(true);
					}
				}
			}
		}
	}
	
	/** In open state, the devices found will be marked as useless devices */
	public void addUselessDevice(BluetoothDevice device)
	{
		/** If this useless device doesn't exist in the devicesList, 
		 *  then add it to the devicesList */
		if(!isExist(device.getAddress()))
		{
			DeviceItem deviceItem = new DeviceItem();
			deviceItem.setBluetoothDevice(device);
			deviceItem.setBTMajorDeviceClass(device.getBluetoothClass().getMajorDeviceClass());
			deviceItem.setIsAllowed(false);
			deviceItem.setIsUseless(true);
			deviceItem.setIsCurrent(true);
			deviceItem.setIsInScope(true);
			devicesList.add(deviceItem);
		}
		/** If this allowed device already existed in the devicesList,
		 *  then just update corresponding field of the record */
		else
		{
			for(int i = 0; i < devicesList.size(); i++)
			{
				if(devicesList.get(i).getBluetoothDevice().getAddress().
						equalsIgnoreCase(device.getAddress()))
				{
					devicesList.get(i).setIsCurrent(true);
					devicesList.get(i).setIsInScope(true);
				}
			}
		}
	}
	
	/** Clear allowed devices list preparing for training new set of allowed devices */ 
	public void clearAllowedDevices()
	{
		Log.i("RTDA", "clear list");
		
		allowedDeviceNo = 1;
		@SuppressWarnings("unchecked")
		ArrayList<DeviceItem> temp = (ArrayList<DeviceItem>) devicesList.clone();
		devicesList.clear();
		allowedDevices.clear();
		for(int i = 0; i < temp.size(); i ++)
		{
			if(!temp.get(i).getIsAllowed())
			{
				devicesList.add(temp.get(i));
			}
		}
	}
	
	/** Get allowed devices list*/
	public ArrayList<DeviceItem> returnDevicesList()
	{
		return devicesList;
	}
	
	/** Initiate the field 'isCurrent' to false */
	public void setIsCurrentFalse()
	{
		for(int i = 0; i < devicesList.size(); i++)
		{
			devicesList.get(i).setIsCurrent(false);
		}
	}
	
	/** Initiate the field 'isInScope' to false */
	public void setIsInScopeFalse()
	{
		for(int i = 0; i < devicesList.size(); i++)
		{
			devicesList.get(i).setIsInScope(false);
		}
	}
	
	
	public void recordEnterTime(BluetoothDevice device)
	{
		time.setToNow();
		for(int i = 0; i < devicesList.size(); i++)
		{
			if(devicesList.get(i).getBluetoothDevice().getAddress().equalsIgnoreCase(device.getAddress())
					&& devicesList.get(i).getIsAllowed())
			{
				if(!devicesList.get(i).getIsInScope())//new discovered device
				{
					devicesList.get(i).setIsCurrent(true);
					devicesList.get(i).setIsInScope(true);
				}
				else if(devicesList.get(i).getIsInScope() && !devicesList.get(i).getIsCurrent())
				{
					devicesList.get(i).setIsCurrent(true);
					devicesList.get(i).setIsInScope(true);
				}
			}
		}
	}
	
	/** Check whether an allowed device is still in range */
	public void checkIsInScope()
	{
		for(int i = 0; i < devicesList.size(); i++)
		{
			if(devicesList.get(i).getIsInScope() && !devicesList.get(i).getIsCurrent())
			{
				devicesList.get(i).setIsInScope(false);
			}
		}
	}
	
	/** Check whether there are any set of allowed devices */
	public boolean isAnyAllowedDevices()
	{
		boolean exist = false;
		File siteDir = new File(sdPath + MainLogin.appHomeFolder + "/" + username + "/" + MainLogin.rtdaSubFolder + 
				"/" + MainLogin.rtdaAllowedFolder);
		if (siteDir.list().length > 0)
		{
			exist = true;
		}
		return exist;
	}
	
	/** Check is a BT device already exist in list*/
	public boolean isExist(String btAddress)
	{
		boolean exist = false;
		for(int i = 0; i < devicesList.size(); i++)
		{
			if(devicesList.get(i).getBluetoothDevice().getAddress().equalsIgnoreCase(btAddress))
			{
				exist = true;
				break;
			}
		}
		return exist;
	}
	
	public void setCanvasState(int state)
    {
    	canvasState = state;
    }
	
	/** Translate time object in String format */
	public String setTimeString(Time time)
	{
		String timeStr = time.monthDay + "-" + (time.month + 1) + "-" + time.year+ " " +
			time.hour + ":" + time.minute + ":" + time.second + "(" + time.timezone + ")";
		return timeStr;
	}
	
	/** Get the list of currently in-range devices */
	public String[] getCurrentBTDevices()
	{
		ArrayList<BluetoothDevice> currentDevicesList = new ArrayList<BluetoothDevice>();
		for(int i = 0; i < devicesList.size(); i++)
		{
			if(devicesList.get(i).getIsCurrent() == true)
			{
				currentDevicesList.add(i, devicesList.get(i).getBluetoothDevice());
			}
		}
		String[] receivers = new String[currentDevicesList.size()];
		for(int j = 0; j < currentDevicesList.size(); j++)
		{
			receivers[j] = currentDevicesList.get(j).getName();
		}
		return receivers;
	}
	
	/** Get the list of allowed devices */
	public int getNumberOfAllowedDevices()
	{
		return allowedDeviceNo;
	}
	
	/** To control only one instance of this device is exist */
	public static synchronized DevicesManaging getInstance(){
	    if (deviceMagInstance == null) {
	    	deviceMagInstance = new DevicesManaging();
	    }
	    return deviceMagInstance;
	}
	
	public int getCanvasState()
	{
		return canvasState;
	}
	public ArrayList<String> getAllowedDevices()
	{
		return allowedDevices;
	}
	public void clearDevicesMag()
	{
		devicesList.clear();
	}
}
