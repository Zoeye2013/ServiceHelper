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
 *  Class BluetoothSynchronize
 *
 *  The Activity used for transferring data between two mobile devices by using Bluetooth
 *  A device can act as either a Server or a Client, Client will update files to the Server.
 *
 */


package fi.tol.android.RTDAReceiver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class BluetoothSynchronize extends Activity implements SensorEventListener{

    /** Message types sent from the BluetoothSynchronizeService Handler */
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_INITIATE_UI = 6;
    public static final int MESSAGE_PROGRESS_BAR = 7;
    
    /** Intent request codes */
    private static final int REQUEST_ENABLE_DISCOVERABILITY = 1;

    /** Key names received from the BluetoothSynchronizeService Handler */
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    
    /** Enable Bluetooth on device takes few second, 
	 *  Timer is used to delay the setting of server side */
	private Timer delayServerTimer;
	private TimerTask delayServerTask;
	
	/** Elements on the UI layout */
	private TextView titleTextView;
	private TextView instructionTextView;
	private Button enableDiscoverabilityBtn;

    /** Name of the connected device */
    private String connectedDeviceName = null;

    /** Local Bluetooth adapter */
    private BluetoothAdapter bluetoothAdapter = null;
    /** Member object for the synchronize services */
    private BluetoothSynchronizeService synchronizeService = null;

    
    /** Attributes for Accelerometer sensor */
	private SensorManager acceleroSensor;
	/** Last time fetch the Sensor data */
	private long lastTime = -1;
	
	/** Minimum absolute movement force to consider as a peak value. */
	private static final int MIN_FORCE = 6;
	/** Minimun absolute movement drop after a peak to consider as a valley. */
	private static final int MIN_FORCE_DROP = 5; 
	private float peakPoint = 0;
	private int peakCount = 0;
	private ArrayList<Long> peakTimeList;
	private float currentTotalMovement = 0;
	private float lastX = 0;
	private float lastY = 0;
	private float lastZ = 0;
	long peakTime = 0;
	
	/** Whether this device acts as server */
	boolean isServer = false;
	/** additional part of server's friendly name
	 *  used in recognizing the server that client want to update data to */
	private static final String ADD_SERVER_NAME = "EXCHANGE_DATA_SERVER_TAPPED";
	/** Bluetooth address of this device */
	private String serverAddress;
	
	private boolean findRightServer = false;
	private boolean isFileTransferFinished = false;
	
	/** Mark for information received from server side */
	public static final String INFORM = "I";
	public static final String SYNCHRONIZE_DATA = "S";
	public static final String FILE_END_MARK = "E";
	public static final String FILE_NUMBER_MARK = "N";
	public static final String FILE_TRANSFER_FINISHED = "TRANSFER_FINISHED";
	
	/** Key list of enabled modules in server side */
	private List<String> serverSideEnabledModuelKeys;
	private List<String> filesToUpdate;
	
	/** Preference settings own by specific username 
	 *  here used to get the info about enabled modules */
	private SharedPreferences userPreference;
	/** SharedPreference for account info of all registered users 
	 *  here used to get the info of currently login user */
	private SharedPreferences accountSharedPreference;
	private String username;
	
	/** File read attributes */
	private FileInputStream fileIn;
	private InputStreamReader inReader;
	private BufferedReader bfReader;
	
	/** Text shown on server side after use click the button to enable 
	 *  server's discoverability and before a remote client is connected to the server */
	private String serverWaitingForConnectInfo;

	/** Progress dialog for client side when it's trying to connect to server */
	private ProgressDialog connectingPD;
	/** Progress dialog for both client and server side when they're transferring data */
	private ProgressDialog transferingPD;
	
	private boolean informQuestionUpdate = false;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.exchange_data_main);
        
        /** Get SharedPreference for account info of all registered users */
        accountSharedPreference = getSharedPreferences("Account",Context.MODE_PRIVATE);
        username = accountSharedPreference.getString("last_log_in", null);
        
        /** Get preference settings own by specific user name */
        userPreference = this.getSharedPreferences(username, Context.MODE_PRIVATE);

        /** Get layout elements */
    	titleTextView = (TextView) findViewById(R.id.i_am_server_info);
    	instructionTextView = (TextView) findViewById(R.id.exchange_data_instruction);
    	enableDiscoverabilityBtn = (Button) findViewById(R.id.enable_discoverability_btn);
    	
    	/** Set click listener for synchronizeBtn */
    	enableDiscoverabilityBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				/** Enable the discoverability of the server */
	            ensureDiscoverable();
			}});
        
    	/** Timer to delay the Bluetooth discovery for a few second, 
		 *  to ensure discovery started after Bluetooth is enabled */
    	delayServerTimer = new Timer();
        delayServerTask = new TimerTask()
	    {
	    	public void run() 
	    	{
	            if (synchronizeService != null) {
	                /** Only if the state is STATE_NONE, do we know that we haven't started already */
	                if (synchronizeService.getState() == BluetoothSynchronizeService.STATE_NONE) {
	                  /** Start the Bluetooth synchronize services */
	                 synchronizeService.start();
	                }
	            }
	    	}
	    };

	    /** Get local BT adapter and enable Bluetooth */
        openBTAdapter();

        /** Member object for the synchronize services */
        synchronizeService = new BluetoothSynchronizeService(this, mHandler);
        synchronizeService.setLoginUserName(username);
        synchronizeService.setContext(BluetoothSynchronize.this);
        
        
        /** Timer to delay the Bluetooth discovery for a few second, 
		 *  to ensure discovery started after Bluetooth is enabled */
		delayServerTimer.schedule(delayServerTask, 3000);
		
		/** Initiate Accelerometer sensor and register sensor listener */
		acceleroSensor = (SensorManager) getSystemService(SENSOR_SERVICE);
		acceleroSensor.registerListener(this, acceleroSensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 
				SensorManager.SENSOR_DELAY_NORMAL);
		peakTimeList = new ArrayList<Long>();
		serverSideEnabledModuelKeys = new ArrayList<String>();
		filesToUpdate = new ArrayList<String>();
		
		/** Initiate the bluetooth friendly name, remove all Server Mark */
		String tempName = bluetoothAdapter.getName();
		if(bluetoothAdapter.getName().contains(ADD_SERVER_NAME))
		{
			bluetoothAdapter.setName(bluetoothAdapter.getName().replaceAll(ADD_SERVER_NAME, ""));
		}
		
		serverWaitingForConnectInfo = getString(R.string.waiting_for_client_to_connect);
		
		/** Register for broadcasts when a device is discovered */
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(btReceiver, filter);

        /** Register for broadcasts when discovery has finished */
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(btReceiver, filter);
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
        /** Stop the Bluetooth synchronize services */
        if (synchronizeService != null)
        {
        	synchronizeService.setIsModuleEnded(true);
        	synchronizeService.stop();
        }
       
        
        if (bluetoothAdapter != null) {
        	/** Make sure we're not doing discovery anymore */
        	bluetoothAdapter.cancelDiscovery();
        	if(bluetoothAdapter.getName().contains(ADD_SERVER_NAME))
    		{
    			bluetoothAdapter.setName(bluetoothAdapter.getName().replaceAll(ADD_SERVER_NAME, ""));
    		}
        	/** Turn off Bluetooth */
        	if(bluetoothAdapter.isEnabled() == true)
        		bluetoothAdapter.disable();
        }

        /** Unregister broadcast listeners */
        this.unregisterReceiver(btReceiver);
        
        /** Unregister accelerometer sensor listener */
        acceleroSensor.unregisterListener(this);
    }
    
    /** Enable Server side's Bluetooth discoverability */
    private void ensureDiscoverable() {
    	Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600);
        startActivityForResult(discoverableIntent, REQUEST_ENABLE_DISCOVERABILITY);
    }

    /** Server side will inform the client side how many Modules have been enabled on the server
     *  then client side will prepare corresponding data for those modules. */
    private void serverInformEnabledModules() {
        /** Check that we're actually connected before trying anything */
        if (synchronizeService.getState() != BluetoothSynchronizeService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        else if(isServer == true)
        {
        	/** Prepare the message will be sent to client side
        	 *  Contains INFORM mark, and keys for enabled modules */
        	String enabledKeys = INFORM;
        	if(userPreference.getBoolean(getString(R.string.RTDA_key), false) == true)
    			enabledKeys += "@" + getString(R.string.RTDA_key);
    		if(userPreference.getBoolean(getString(R.string.Ping_I_key), false) == true)
    		{
    			enabledKeys += "@" + getString(R.string.Ping_I_key);
    			informQuestionUpdate = true;
    		}
    		if(userPreference.getBoolean(getString(R.string.task_recorder_key), false) == true)
    			enabledKeys += "@" + getString(R.string.task_recorder_key);
        	byte[] enabledKeysBytes = enabledKeys.getBytes();
        	
        	/** Send the message */
            synchronizeService.write(enabledKeysBytes);
        }
    }
    
    /** After Server side receive the data from client side completely
     *  Server will inform the client that transfer is finished */
    private void serverInformTransferEnd() {
        /**  Check that we're actually connected before trying anything */
        if (synchronizeService.getState() == BluetoothSynchronizeService.STATE_CONNECTED && isServer == true)
        {
        	/** Prepare the message will be sent to client side
        	 *  Contains FILE_TRANSFER_FINISHED mark */
        	String transferEndMsg = FILE_TRANSFER_FINISHED + "@";
        	byte[] transferEndMsgBytes = transferEndMsg.getBytes();
        	
        	/** Send the message */
            synchronizeService.write(transferEndMsgBytes);
        }
    }
    
    /** Client side prepare data to synchronize and send them to Server side */
    private void clientUpdateDataToServer() {
        /** Check that we're actually connected before trying anything */
        if (synchronizeService.getState() != BluetoothSynchronizeService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        else if(isServer == false && serverSideEnabledModuelKeys.size() > 0)
        {
        	String appPath = MainLogin.sdCardPath + "/" + MainLogin.appHomeFolder + "/" + username + "/";
        	File temp;
        	
        	/** For storing the data to be transferred */
        	List<String> synchronizeData = new ArrayList<String>();
        	
        	/** Prepare data of enabled modules */
        	for(int i = 0; i < serverSideEnabledModuelKeys.size(); i++)
        	{
        		/** Server side the RTDA Bluetooth Process Data Acquisition Module is enabled */
        		if(serverSideEnabledModuelKeys.get(i).equals(getString(R.string.RTDA_key)))
        		{
        			/** Get folder path of allowed devices folder,
        			 *  Which is for storing different sets of allowed devices */
        			String RTDAPath = appPath + MainLogin.rtdaSubFolder + "/" + MainLogin.rtdaAllowedFolder;
        			temp = new File(RTDAPath);
        			
        			/** Get all files of allowed devices */
        			String[] siteArray = temp.list();
        			
        			/** If any file of allowed devices exist */
        			if(siteArray.length > 0)
        			{
        				for(int j = 0; j < siteArray.length; j++)
        				{
        					String path = RTDAPath + "/" + siteArray[j];
        					String filePath = MainLogin.rtdaSubFolder + "/" + MainLogin.rtdaAllowedFolder + "/" + siteArray[j];
        					temp = new File(path);
        					if(temp.exists())
        					{
        						filesToUpdate.add(RTDAPath + "/" + siteArray[j]);
        						
        						/** Prepare the message will be sent to client side
        			        	 *  Contains SYNCHRONIZE_DATA mark, the content of the file
        			        	 *  and FILE_END_MARK mark */
        						synchronizeData.add(SYNCHRONIZE_DATA + "@" + filePath + "@" + 
        							readUpdateFile(temp) + "@" + FILE_END_MARK + "@");
        					}
        				}
        			}
        		}
        		/** Server side the Ping I Module is enabled */
        		else if(serverSideEnabledModuelKeys.get(i).equals(getString(R.string.Ping_I_key)))
        		{
        			/** Get the path of questionnaire file */
        			String path = appPath + MainLogin.pingISubFolder + "/" + MainLogin.questionnaireFile;
        			String filePath = MainLogin.pingISubFolder + "/" + MainLogin.questionnaireFile;
        			temp = new File(path);
        			if(temp.exists())
        			{
        				filesToUpdate.add(path);
        				/** Prepare the message will be sent to client side
			        	 *  Contains SYNCHRONIZE_DATA mark, the content of the file
			        	 *  and FILE_END_MARK mark */
        				synchronizeData.add(SYNCHRONIZE_DATA + "@" + filePath + "@" + 
        					readUpdateFile(temp) + "@" + FILE_END_MARK + "@");
        			}
        		}
        		/** Server side the Task Recorder Module is enabled */
        		else if(serverSideEnabledModuelKeys.get(i).equals(getString(R.string.task_recorder_key)))
        		{
        			String path = appPath + MainLogin.tasksSubFolder + "/" + MainLogin.tasksListFile;
        			String filePath = MainLogin.tasksSubFolder + "/" + MainLogin.tasksListFile;
        			temp = new File(path);
        			if(temp.exists())
        			{
        				filesToUpdate.add(path);
        				/** Prepare the message will be sent to client side
			        	 *  Contains SYNCHRONIZE_DATA mark, the content of the file
			        	 *  and FILE_END_MARK mark */
        				synchronizeData.add(SYNCHRONIZE_DATA + "@" + filePath + "@" + 
        					readUpdateFile(temp) + "@" + FILE_END_MARK + "@");
        			}
        		}
        	}
        	if(filesToUpdate.size() > 0)
			{
        		/** Send the files to Server side */
				String headerOfTransfer = FILE_NUMBER_MARK + "@" + filesToUpdate.size() + "@";
				byte[] synchronizeDataBytes = headerOfTransfer.getBytes();
	            synchronizeService.write(synchronizeDataBytes);
	            synchronizeService.setSendFileNum(filesToUpdate.size());
	            for(int j = 0; j < filesToUpdate.size(); j++)
	            {
	            	synchronizeDataBytes = synchronizeData.get(j).getBytes();
	            	synchronizeService.write(synchronizeDataBytes);
	            }
			}
        	/** If there is no file to be updated to Server side, Toast to inform user */
        	if(filesToUpdate.size() <=0)
        	{
        		Toast.makeText(getApplicationContext(),R.string.no_file_to_update, Toast.LENGTH_SHORT).show();
        		return;
        	}
        }
    }
    
    /** Read file content into String for sending to Server side */
	public String readUpdateFile(File file)
    {
		String lineTemp = "";
		String fileData = "";
		try {
			fileIn = new FileInputStream(file);
			inReader = new InputStreamReader(fileIn);
			bfReader = new BufferedReader(inReader);
			while(((lineTemp=bfReader.readLine()) != null)) //Read one line
			{
				fileData += lineTemp + "\n";
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fileData;
   	 }
	
	/** Set up progress bar for transferring data between Server and Client */
	public void setTransferingProgressBar()
	{
		transferingPD = new ProgressDialog(BluetoothSynchronize.this);
		transferingPD.setTitle(R.string.progress_dialog_transfering_tile);
		transferingPD.setMessage(getString(R.string.progress_dialog_transfering_info));
    	transferingPD.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    	transferingPD.setIndeterminate(false);
    	transferingPD.setProgress(100);
    	transferingPD.show();
	}

    /** The Handler that gets information back from the BluetoothSynchronizeService */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            /** After send out a message */
            case MESSAGE_WRITE:
            	/** If this device is currently as Server */
                if(isServer == true)
                {
                	/** Get the message has been sent out */
                	byte[] writeBuf = (byte[]) msg.obj;
                	/** Construct a String of the message from the buffer */
                	String readMessage = new String(writeBuf);
                	
                	/** If the message is not with FILE_TRANSFER_FINISHED mark */
                	if(readMessage.equals(FILE_TRANSFER_FINISHED + "@") == false)
                	{
                		/** Set progress bar shows it's transferring */
                		setTransferingProgressBar();
                		
                		/** Inform Server side background service to re-read questionnaire from file */
                		if(informQuestionUpdate == true)
                		{
                			Intent informQuestionUpdateIntent = new Intent();
                			informQuestionUpdateIntent.setAction(PhoneCallListenerService.RE_LOAD_QUESTIONNAIRE);
    						sendBroadcast(informQuestionUpdateIntent);
    						informQuestionUpdate = false;
                		}
                	}
                }
                break;
            /** After receive a message */
            case MESSAGE_READ:
            	/** Get the recieved message */
                byte[] readBuf = (byte[]) msg.obj;
                /** construct a string of the message from the valid bytes in the buffer */
                String readMessage = new String(readBuf, 0, msg.arg1);
                
                /** If this device is currently as Client */
                if(isServer == false)
                {
                	String[] tempArr = readMessage.split("@");
                	/** Client will first get information from server: which modules is enabled */
                	if(tempArr[0].equals(INFORM) && tempArr.length > 1)
                	{
                		for(int i = 1; i < tempArr.length; i++)
                		{
                			serverSideEnabledModuelKeys.add(tempArr[i]);
                		}
                		/** Set progress bar shows it's transferring */
                		setTransferingProgressBar();
                		
                		/** Client side prepare data to synchronize and send them to Server side */
                		clientUpdateDataToServer();
                	}
                	/** If Server side doesn't enable any modules */
                	else if(tempArr[0].equals(INFORM) && tempArr.length <= 1)
                	{
                		Toast.makeText(getApplicationContext(),R.string.server_no_enabled_modules, Toast.LENGTH_SHORT).show();
                	}
                	/** If it is a FILE_TRANSFER_FINISHED mark */
                	else if(tempArr[0].equals(FILE_TRANSFER_FINISHED))
                	{
                		isFileTransferFinished = true;
                		synchronizeService.setIsTransferFinished(isFileTransferFinished);
                		synchronizeService.stop();
                	}
                }
                break;
            case MESSAGE_DEVICE_NAME:
            	/** When a connection is set up, server will first inform
            	 *  client which modules are enabled in server side */
            	serverInformEnabledModules();
            	
                /** Get the connected device's name */
                connectedDeviceName = msg.getData().getString(DEVICE_NAME);
                if(isServer == true)
                {
                	serverWaitingForConnectInfo = getString(R.string.connect_to_client) + " " + connectedDeviceName + "\n";
                }
                else if(isServer == false)
                {
                	connectingPD.dismiss();
                	instructionTextView.setText(getString(R.string.connect_to_server) + " " + connectedDeviceName);
                }
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_LONG).show();
                break;
            case MESSAGE_INITIATE_UI:
            	discoverableCount.cancel();
        		initiateUI();
            	break;
            case MESSAGE_PROGRESS_BAR:
            	/** The message to dismiss Connecting Progress Bar */
            	if(msg.obj != null && (Boolean)msg.obj == true)
            	{
            		connectingPD.dismiss();
            	}
            	/** Otherwise, messages to update the Transfering Progress Bar */
            	else
            	{
            		if(isServer == true)
                	{
                		int finished = msg.arg1;
                    	transferingPD.setProgress(finished);
                    	if(finished == 100)
                    	{
                    		transferingPD.dismiss();
                    		/** File transfering finished, the server inform client to stop connection */
                    		serverInformTransferEnd();
                    	}
                	}
                	else if(isServer == false)
                	{
                		int finished = msg.arg1;
                		transferingPD.setProgress(finished);
                		if(finished == 100)
                    	{
                    		transferingPD.dismiss();
                    	}
                	}
            	}
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_ENABLE_DISCOVERABILITY:
        	if(resultCode > 0)
        	{
        		/** Once this button is click means current devive
				 *  want to update data to a remote server, so current
				 *  device works as a 'Client' */
				isServer = true;
				synchronizeService.setIsServer(isServer);
				
	            /** As server, after enable discoverability then 
	             *  waiting for client to initiate the connection,
	             *  so will stop accelerometer sensor */
	            acceleroSensor.unregisterListener(BluetoothSynchronize.this);
	            
	            /** Count down timer for discoverable time */
	            discoverableCount.start();
				enableDiscoverabilityBtn.setVisibility(Button.INVISIBLE);
				
        		/** We add addtional part to server name
                 *  will be used for client to recognize server */
        		String name = bluetoothAdapter.getName();
                bluetoothAdapter.setName(name + ADD_SERVER_NAME);
        	}
        	else if (resultCode <= 0) {
                /** User did not enable Bluetooth or an error occurred */
                Toast.makeText(this, R.string.not_discoverable, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /** Connect to a remote Bluetooth device */
    private void connectDevice(boolean secure) {
        /** Get the BluetoothDevice object of the server with the Bluetooth Address */
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(serverAddress);
        /** Attempt to connect to the server */
        synchronizeService.connect(device, secure);
    }

    /** Get local BT adapter and enable Bluetooth */
    public void openBTAdapter()
	{
		Log.i("INFO", "open Bluetooth Device Adapter.");
    	bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    	if (bluetoothAdapter == null)
		{
    		/** If the device doesn't support Bluetooth, the end this module */
    		Toast toast = Toast.makeText(this, R.string.bt_not_available, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER_VERTICAL| Gravity.CENTER_HORIZONTAL,0,0);
			toast.show();
            finish();
            return;
		}    	
		if(!bluetoothAdapter.isEnabled())
		{
			/** Call enable() to enable Bluetooth without request for user permission
			 *  This requires android.permission.BLUETOOTH_ADMIN Permission */
			bluetoothAdapter.enable();
		}
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}

	/** When accelerometer sensor data changed */
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			/** Update when time interval bigger than 100ms */
			long currentTime = System.currentTimeMillis();
			if ((currentTime - lastTime) > 100) 
			{
			    float currentX = event.values[0];
			    float currentY = event.values[1]; 
			    float currentZ = event.values[2];
			    /** We analyze the absolute changes of x, y and z axis */
			    currentTotalMovement = Math.abs(currentX + currentY + currentZ - lastX - lastY - lastZ);

			      
			    /** If there is no tasks are started, we will monitor the movements of user
			    *  to detect "start task" movement */
			    /** If the accelerometer sensor isn't first started */
		    	if(lastX != 0){
		    		
		    		/** If total absolute movement > minimum force, we are going to record peak point */
			    	if(currentTotalMovement > MIN_FORCE){
			    		if(currentTotalMovement > peakPoint){
			    			peakPoint = currentTotalMovement;
			    			peakTime = currentTime;}}
		    		
		    		/** If total absolute movement < minimum force, we are going to record valley point */
			    	else if(currentTotalMovement < peakPoint && (peakPoint - currentTotalMovement) > MIN_FORCE_DROP
		    				  && (currentTime - peakTime) < 500){
			    			peakCount ++;
			    			peakTimeList.add(peakTime);
			    			peakPoint = currentTotalMovement;
		    			  
			    			/** continuous two peak points is considered as a starting of a task */
			    			if(peakCount >= 2){
			    				if(peakTimeList.get(1) - peakTimeList.get(0) < 2000){
			    					peakCount = 0;
			    					peakTimeList.clear();
			    					
			    					/** Progress dialog to show user that it's trying to connect to server */
			    					connectingPD = ProgressDialog.show(BluetoothSynchronize.this,
			    							getString(R.string.progress_dialog_connecting_tile),getString(R.string.progress_dialog_connecting_info));
			    					/** After detect the update action, we will stop 
			    					 *  accelerometer sensor to prevent start another
			    					 *  update action */
			    					acceleroSensor.unregisterListener(this);
			    					titleTextView.setText(R.string.i_am_client_info);
			    					instructionTextView.setText(R.string.waiting_to_connect_to_server);
			    					enableDiscoverabilityBtn.setVisibility(Button.INVISIBLE);
			    					
			    					isServer = false;
			    					synchronizeService.setIsServer(isServer);
			    					
			    					/** If we're already discovering, stop it */
			    			        if (bluetoothAdapter.isDiscovering()) {
			    			            bluetoothAdapter.cancelDiscovery();
			    			        }

			    			        /** Request discover from BluetoothAdapter */
			    			        bluetoothAdapter.startDiscovery();
			    				}
			    				else{
			    					peakCount--;
			    					peakTimeList.remove(0);}}}}
		    	lastX = currentX;
			    lastY = currentY;
			    lastZ = currentZ;
			    }
		   }    	  
		}
	
	/** The BroadcastReceiver that listens for discovered devices and
	 *  update the UI when discovery is finished */
    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            /** When discovery finds a device */
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                /** Get the BluetoothDevice object from the Intent */
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();
                String address = device.getAddress();
                
                /** If it's right server, then try to connect to it */
                if(recognizingServer(name) && address.length() > 0)
                {
                	/** Create the result Intent and include the MAC address */
                	findRightServer = true;
                	serverAddress = address;
                	connectDevice(false);
                }
            /** When discovery is finished, update the UI */
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

            	/** If didn't find the right server to connect, inform user */
				if(findRightServer == false)
				{
					mHandler.obtainMessage(MESSAGE_PROGRESS_BAR, -1, -1, true).sendToTarget();
					initiateUI();
					Toast.makeText(BluetoothSynchronize.this, R.string.cannot_recognize_server, Toast.LENGTH_SHORT).show();
					
				}
				findRightServer = false;
            }
        }
    };
    
    /** Analyze the friendly name of remote Bluetooth device
     *  the right server that current divice want to update
     *  data to will have special addtional string in it's name */
    public boolean recognizingServer(String name){
    	int addtionLength = ADD_SERVER_NAME.length();
    	boolean isRightServer = false;
    	if(name.length() >= addtionLength)
    	{
    		String addtionString = name.substring((name.length() - addtionLength), name.length());
        	if(addtionString.equals(ADD_SERVER_NAME))
        		isRightServer = true;
    	}
    	return isRightServer;
    }
    
    /** A count down timer after server enable it's discoverability */  
    private CountDownTimer discoverableCount = new CountDownTimer(300000, 1000) {     
    
        @Override     
        public void onFinish() {
        	initiateUI();      
        }     
        @Override     
        public void onTick(long millisUntilFinished) {     
        	instructionTextView.setText(serverWaitingForConnectInfo + getString(R.string.discoverable_info) + " " + millisUntilFinished / 1000 + "s");   
        }    
    };
    
    /** When transferring is finished, initiate the UI */
    private void initiateUI()
    {
    	isServer = false;
    	isFileTransferFinished = false;
    	synchronizeService.setIsServer(isServer);
    	synchronizeService.setIsTransferFinished(isFileTransferFinished);
    	synchronizeService.setSendFileNum(0);
    	titleTextView.setText(R.string.i_am_server_info);
    	enableDiscoverabilityBtn.setVisibility(Button.VISIBLE);
    	instructionTextView.setText(R.string.excahnge_data_instruction);
    	serverSideEnabledModuelKeys.clear();
    	filesToUpdate.clear();
    	serverWaitingForConnectInfo = getString(R.string.waiting_for_client_to_connect);
    	acceleroSensor.registerListener(BluetoothSynchronize.this, acceleroSensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 
				SensorManager.SENSOR_DELAY_NORMAL);
    	
    	if(bluetoothAdapter.getName().contains(ADD_SERVER_NAME))
		{
			bluetoothAdapter.setName(bluetoothAdapter.getName().replaceAll(ADD_SERVER_NAME, ""));
		}
    }
}