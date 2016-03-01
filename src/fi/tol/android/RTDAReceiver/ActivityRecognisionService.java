/**
 * 	This file is part of RTDAReceiver.
 * 	RTDAReceiver is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License version 2 (GPLv2) as published by
 *  the Free Software Foundation.
 * 	RTDAReceiver is distributed in the hope that it will be useful, but WITHOUT 
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 *  FOR A PARTICULAR PURPOSE.  See the GNU General Public License version 2 for 
 *  more details.
 * 	You should have received a copy of the GNU General Public License version 2 
 *  along with RTDAReceiver.  If not, see <http://www.gnu.org/licenses/gpl-2.0.html/>.
 *  Copyright 2013 Ye Zhang
 */
package fi.tol.android.RTDAReceiver;

/** Open Source Software By Elinkeinoel�m�n Tutkimuslaitos (ETLA), Finland
 *  Coding by Ye Zhang
 *  
 *  A service for RTDA(Bluetooth Process Data Acquisition) module
 *  To record Bluetooth RSSI value of a specific set of 
 *  Bluetooth allowed devices during process measuring
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;
import android.location.GpsStatus;

public class ActivityRecognisionService extends Service implements SensorEventListener,LocationListener{
	
	
	
	/** One signal vector is a list of Bluetooth RSSI values */
	//private ArrayList<Short> signalVector;
	
	/** Broadcast receiver listen to Bluetooth discovering results
     *  focus on getting Bluetooth RSSI values */
	private BroadcastReceiver rssiReceiver;
	/** Accelerometer sensor */
	private SensorManager acceleroSensor;
	/** Last time fetch the Sensor data */
	private long lastTime = -1;
	private ActivityRecognisionDataManagement activityRecognisionMag;
	
	private Time time;
	
	/** This is the object that receives interactions from clients*/
    private final IBinder signalVectorServiceBinder = new SignalVectorLocalBinder();
    
    /** File output attributes */
	private FileOutputStream acceFileOut;
	private OutputStreamWriter acceOutWriter;
	private BufferedWriter acceBufferWriter;
	private final int ACCELEROMETER_DATA=1;
	private final int GPS_DATA=2;
    
	private LocationListener locationListener;
	
	/** Location manager */
	private LocationManager locationManager;
	private BroadcastReceiver measurementStartReceiver;
    
	public void onCreate() {
        super.onCreate();

        //signalVector = new ArrayList<Short>();
        locationListener = this;
        activityRecognisionMag = new ActivityRecognisionDataManagement();
        time = new Time();
        
        /** Initiate Accelerometer sensor and register sensor listener */
		acceleroSensor = (SensorManager) getSystemService(SENSOR_SERVICE);
		
        /** Broadcast receiver listen to Bluetooth discoverying results
         *  focus on getting Bluetooth RSSI values */
        rssiReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				/** Only when RTDA module is on RUN_STATE, we will record RSSI vaules
				 *  won't record RSSI in either on OPEN_STATE or TRAINING_STATE */
				if(DevicesManaging.getInstance().getCanvasState() == DevicesManaging.ON_RUN_STATE)
				{
					String action = intent.getAction();
					/** Catch remote Bluetooth device found action */
					if (BluetoothDevice.ACTION_FOUND.equals(action))
					{
						/** Get BluetoothDevice object of the found remote BT device */
						BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
						/** Get Bluetooth RSSI value of the found remote BT device */
						short btRSSI = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
						
						activityRecognisionMag.setBluetoothRSSI(btRSSI, device.getAddress());
					}
					/** Catch Bluetooth discovery finished action 
					 *  record one round discovery result into corresponding file*/
					else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
					{
						recordData();
					}
				}
			}
        	
        };
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(rssiReceiver, filter);
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(rssiReceiver, filter);
		
		/** Broadcast Receiver to  receive when the measurement is started */
		measurementStartReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (action.equals(SelectMeasureSiteDialog.MEASUREMENT_SITE_SELECTED))
				{
					locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
					int version = Build.VERSION.SDK_INT;
					String provider = Settings.Secure.getString(getContentResolver(), 
			    	Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
					
					requestLocationUpdates();

					if (Build.VERSION.SDK_INT > 13) {
						if(provider.contains("gps") == false)
						{
							Intent enableGPSIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
							enableGPSIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							
							Toast toast = Toast.makeText(getApplicationContext(), R.string.to_enable_gps, Toast.LENGTH_LONG);
							toast.setGravity(Gravity.CENTER_VERTICAL| Gravity.CENTER_HORIZONTAL,0,0);
							toast.show();
							
					        startActivity(enableGPSIntent);
						}
					}
					else
					{
						toggleGPS(true);
					}
					
					
				}
				else if(action.equals(RTDAReceiver.MEASUREMENT_IS_STOPPED))
				{
					if(locationManager != null)
					{
						locationManager.removeUpdates(locationListener);
					}
					if(Build.VERSION.SDK_INT <= 13)
						toggleGPS(false);
				}
			}
		};
		filter = new IntentFilter(SelectMeasureSiteDialog.MEASUREMENT_SITE_SELECTED);
		registerReceiver(measurementStartReceiver,filter);
		filter = new IntentFilter(RTDAReceiver.MEASUREMENT_IS_STOPPED);
		registerReceiver(measurementStartReceiver,filter);
    }
	
	

	@Override
	public void onDestroy() {
		/** Unregister Broadcast receiver */
		unregisterReceiver(rssiReceiver);
		acceleroSensor.unregisterListener(this);
		unregisterReceiver(measurementStartReceiver);
		if(locationManager != null)
		{
			locationManager.removeUpdates(this);
		}
		if(Build.VERSION.SDK_INT <= 13)
		{
			toggleGPS(false);
		}
		
		super.onDestroy();
	}



	@Override
	/** Return the communication channel to the service.
	 *  return null if clients can not bind to the service. */
	public IBinder onBind(Intent intent) {
		return signalVectorServiceBinder;
	}
	
	/** Get the information of currently selected set of Bluetooth allowed devices */
	public void setAllowedDevice(String selectedSite)
	{
		activityRecognisionMag.setAllowedDevice(selectedSite);
		
		/** Only start listen to the accelerometer after the measurement is started */
		acceleroSensor.registerListener(this, acceleroSensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 
				SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	public void recordData()
	{
		time.setToNow();
		activityRecognisionMag.setTime(time);	
		activityRecognisionMag.recordData();
	}
	
	/** Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC. */
	public class SignalVectorLocalBinder extends Binder {
	    
		ActivityRecognisionService getService() {
	        return ActivityRecognisionService.this;
	    }
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}



	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			/** Update when time interval bigger than 100ms */
			long currentTime = System.currentTimeMillis();
			  // if ((currentTime - lastTime) > 20) 
			   //{ 
			      float currentX = event.values[0];
			      float currentY = event.values[1]; 
			      float currentZ = event.values[2];
			      activityRecognisionMag.setAccelerometerData(currentTime,currentX, currentY, currentZ);
			      lastTime = currentTime;
			      
			    //test
			      if(DevicesManaging.getInstance().getCanvasState() == DevicesManaging.ON_RUN_STATE)
			      {
			      Time testT = new Time();
					testT.set(currentTime);
			      String test = currentTime + "," + testT.hour + ":" + testT.minute + ":" +  testT.second + "," +
			      currentX + "," + currentY + "," + currentZ + "\n";
			      
			      String date = testT.monthDay + "-" + (testT.month + 1) + "-" + testT.year;
			      String file = activityRecognisionMag.generateOutputFileName(date);
			      
			      outputData(file,test,ACCELEROMETER_DATA);
			      }
		}
	}
	
	//Test
	public void outputData(String fileName,String records,int dataType)
	{
		//Test
		String acceRecords = "";
		
		/** File for accelerometer data */
		File file = new File(fileName);
		if(file.exists()==false)
		{
			switch(dataType)
			{
			case ACCELEROMETER_DATA:
				/** If accelerometer data file doesn't exist, then write the header of the file */
				acceRecords += "Time,Time";
				acceRecords += activityRecognisionMag.getAccelerometerData().getColumns() + "\n";
				break;
			case GPS_DATA:
				acceRecords += "Time,Time,Provider,Latitude,Longitude,Accuracy,Altitude\n";
				break;
			}
		}
		acceRecords += records;
		
		try{
			
			/** Save accelerometer data */
			acceFileOut = new FileOutputStream(file, true);
			acceOutWriter = new OutputStreamWriter(acceFileOut);
			acceBufferWriter = new BufferedWriter(acceOutWriter);
			acceBufferWriter.write(acceRecords);
			acceBufferWriter.close();
		}
		catch(FileNotFoundException exception){
			exception.printStackTrace();
		}
		catch(IOException ioException)
		{
			ioException.printStackTrace();
		}
	}



	/** Call back methods of LocationListener */
	public void onLocationChanged(Location location) {
		Time time = new Time();
		time.set(location.getTime());
		String date = time.monthDay + "-" + (time.month + 1) + "-" + time.year;
		String file = activityRecognisionMag.generateGPSFileName(date);
		
		String gpsInfo = location.getTime() + "," + 
			time.hour + ":" + time.minute + ":" +  time.second + "," + location.getProvider() + "," +
			location.getLatitude() + "," + location.getLongitude() + "," + 
			location.getAccuracy() + "," + location.getAltitude() + "\n";
		outputData(file,gpsInfo,GPS_DATA);
	}

	public void onProviderEnabled(String provider) {
		Log.i("LOCATION", "Provider " + provider + " is now enabled.");
	}

	public void onProviderDisabled(String provider) {
		Log.i("LOCATION", "Provider " + provider + " is now disabled.");
	}



	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.i("LOCATION", "Provider " + provider + " has changed status to " + status);
	}
	
	
	private void toggleGPS(boolean enable) {
	    String provider = Settings.Secure.getString(getContentResolver(), 
	        Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

	    if(provider.contains("gps") == enable) {
	        return; // the GPS is already in the requested state
	    }

	    final Intent poke = new Intent();
	    poke.setClassName("com.android.settings", 
	        "com.android.settings.widget.SettingsAppWidgetProvider");
	    poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
	    poke.setData(Uri.parse("3"));
	    getApplicationContext().sendBroadcast(poke);
	}
	
	private void requestLocationUpdates() {
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60 * 1000, 0, this);
		if(checkWifi() == true || checkMobileNetwork() ==  true)
		{
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,30*1000,0,this);
		}
	}
	
	public boolean checkWifi() 
	{
	    ConnectivityManager connec = (ConnectivityManager) getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);
	    NetworkInfo wifi = connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

	    if (wifi.isConnected()) {
	        return true;
	    }
	    return false;
	}
	public boolean checkMobileNetwork() 
	{
	    ConnectivityManager connec = (ConnectivityManager) getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);
	    NetworkInfo mobile = connec.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

	    if (mobile.isConnected()) {
	        return true;
	    }
	    return false;
	}
}
