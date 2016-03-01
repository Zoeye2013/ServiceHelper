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
 *  Activity RTDAReceiver
 *
 *  Activity for Bluetooth Process Data Acquisition Module, including a self-defined
 *  canvas to present Bluetooth inquiring result or to virtualize the measurement result,
 *  buttons for several tasks, such as training the application to recognize allowed BT devices,
 *  measuring activity process
 */

package fi.tol.android.RTDAReceiver;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class RTDAReceiver extends Activity{

	private Context context;
	private Activity activity;
	
	/** Elements on the UI */
	private Button btnStartTraining;
	private Button btnStartMeasurement;
	//private Button btnRunInBackground;
	private boolean isTrainingStarted = false;
	private boolean isMeasurementStarted = false;
	private TextView textWelcomeInfo;
	private TextView stateInfo;
	
	/** Self defined Canvas component to draw BT devices */
	private BTDeviceCanvas btDevicesCanvas;
	
	/** Option menu */
	private Menu rtdaReceiverMenu = null;
	
	/** System Time*/
	private Time time = new Time();
	/** The site name user chose to measurement next */
	private String selectedSite;
    
    /** Broadcast receiver listener to the asynchronous Bluetooth discovering results*/
	private BTBroadcastReceiver btReceiver;
	/** Local Bluetooth adapter, the basis of all Bluetooth communication */
	protected BluetoothAdapter localBTAdapter;
	
	/** Broadcast receiver listener to user's selection of measurement sites */
	private BroadcastReceiver siteSelectedReceiver;
	
	public static final String MEASUREMENT_IS_STOPPED="MEASUREMENT_IS_STOPPED";
	
	
	/** Enable Bluetooth on device takes few second, Timer is used to delay the
	 *  Discovering Bluetooth devices work for a few second, 
	 *  to ensure discovery started after Bluetooth is enabled */
	private Timer btDiscoveryDelayTimer = new Timer();
	private TimerTask task;
	
	/** Boolean idicate whether executing Bluetooth discovery periodically */
	private boolean continueInquiry = true;
    
	private DevicesManaging deviceMag;
    private ServiceConnection signalVectorServiceConnection;
    private ActivityRecognisionService signalVectorService;
    
    /** Get accounts information from SharedPreferences */
    private SharedPreferences accountPreference;
    public static String username;
	
	
	/** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rtda_receiver);
        Log.i("RTDA", "On Create");
        this.setTitle(R.string.RTDA_icon);
        accountPreference = getSharedPreferences("Account",Context.MODE_PRIVATE);
        username = accountPreference.getString("last_log_in", null);
        /** Initial members and UI components*/
        initComponents();
		
        /** Register Broadcast receiver listener to the asynchronous Bluetooth discovering results*/
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(btReceiver, filter);
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(btReceiver, filter);
		
		/** Register Broadcast receiver listener to user's selection of measurement sites */
		filter = new IntentFilter(SelectMeasureSiteDialog.MEASUREMENT_SITE_SELECTED);
		registerReceiver(siteSelectedReceiver, filter);
		
		/** Timer to delay the Bluetooth discovery for a few second, 
		 *  to ensure discovery started after Bluetooth is enabled */
		btDiscoveryDelayTimer.schedule(task, 5000);
		
		/** Bind service that responsible for recording Bluetooth RSSI value on RUN_STATE */
        Intent signalVectorServiceIntent = new Intent(this, ActivityRecognisionService.class);
        bindService(signalVectorServiceIntent, signalVectorServiceConnection, Context.BIND_AUTO_CREATE);
    }
    
    public void onDestroy()
    {
    	
    	Log.i("RTDA", "On Destroy");
    	DevicesManaging.getInstance().clearDevicesMag();
    	if(localBTAdapter != null)
		{
			localBTAdapter.cancelDiscovery();
		}
    	btDiscoveryDelayTimer.cancel();
    	unregisterReceiver(btReceiver);
    	unregisterReceiver(siteSelectedReceiver);
    	localBTAdapter.disable();
    	unbindService(signalVectorServiceConnection);
    	super.onDestroy();
    }
    
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		super.onCreateOptionsMenu(menu);
		rtdaReceiverMenu = menu;
		rtdaReceiverMenu.add(R.string.run_in_background);
		return true;
	}
    
    

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		super.onOptionsItemSelected(item);
		// User click button to go back to application's main page
		if(item.getItemId() == 0)
		{
			Intent mainPageIntent = new Intent(context,MainPage.class);
			mainPageIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			activity.startActivity(mainPageIntent);
		}
		return true;
	}

	/** Initial members and UI components*/
    public void initComponents()
	{
    	Log.i("RTDA", "call initComponents() function");
    	context = this;
    	activity = this;
    	
    	/**Get local BT adapter and enable Bluetooth*/
    	openBTAdapter();
    	
    	time.setToNow();
    	
		textWelcomeInfo = (TextView) findViewById(R.id.welcome_info);
		stateInfo = (TextView) findViewById(R.id.state_info);
		btDevicesCanvas = (BTDeviceCanvas) findViewById(R.id.bt_device_canvas);
		DevicesManaging.getInstance().setCanvasState(DevicesManaging.ON_OPEN_STATE);
		stateInfo.setText(R.string.state_info_nothing);
		setWelcomeInfo();
		
		/** Defines callbacks for service binding, passed to bindService() */
		signalVectorServiceConnection = new ServiceConnection() {

			/** When the connection with the service has been established */
			public void onServiceConnected(ComponentName className, IBinder service) {
				
				/** Get the service object we can use to interact with the service */
				signalVectorService = ((ActivityRecognisionService.SignalVectorLocalBinder) service).getService();
			}

			public void onServiceDisconnected(ComponentName arg0) { }
	      };
		
		/**Click to start/stop teaching the application to recognize allowed BT devices*/
		btnStartTraining = (Button) findViewById(R.id.button_start);
		this.btnStartTraining.setOnClickListener(new OnClickListener(){
			public void onClick(final View v)
			{
				/** StartTraining button is clicked */
				if(isTrainingStarted == false)
				{
					DevicesManaging.getInstance().setCanvasState(DevicesManaging.ON_START_STATE);
					stateInfo.setText(R.string.state_info_training);
					if(continueInquiry == false)
					{
						/** Will keep inquiring remote BT devices */
						continueInquiry = true;
						localBTAdapter.startDiscovery();
					}
					isTrainingStarted = true;
					btnStartTraining.setText(R.string.stop_training);
					btnStartMeasurement.setEnabled(false);
					
					/** Clear the allowed BT device list, preparing for training new set of allowed devices */
					DevicesManaging.getInstance().clearAllowedDevices();
					localBTAdapter.cancelDiscovery();
				}
				/** StopTraining button is clicked */
				else if(isTrainingStarted)
				{
					DevicesManaging.getInstance().setCanvasState(DevicesManaging.ON_OPEN_STATE);
					stateInfo.setText(R.string.state_info_nothing);
					btnStartTraining.setText(R.string.start_training);
					isTrainingStarted = false;
					/** Stop Bluetooth inquiring */
					continueInquiry = false;
					localBTAdapter.cancelDiscovery();
					if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
					{
						Intent saveDialogIntent = new Intent(context, SaveAllowedDevicesDialog.class);
						saveDialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						
						context.startActivity(saveDialogIntent);
						btnStartMeasurement.setEnabled(true);
						
					}
				}
			}
		});
		
		/**Click to recording activities */
		btnStartMeasurement = (Button) findViewById(R.id.button_run);
		this.btnStartMeasurement.setOnClickListener(new OnClickListener(){
			public void onClick(final View v)
			{
				/** Start measurement button is clicked */
				if(isMeasurementStarted == false)
				{
					//btnStartTraining.setEnabled(false);
					
					Intent selectSiteDialogIntent = new Intent(context, SelectMeasureSiteDialog.class);
					selectSiteDialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					activity.startActivity(selectSiteDialogIntent);
			
					//DevicesManaging.getInstance().setIsCurrentFalse();
					//DevicesManaging.getInstance().setIsInScopeFalse();
				}
				/** Stop measurement button is clicked */
				else if(isMeasurementStarted)
				{
					DevicesManaging.getInstance().setCanvasState(DevicesManaging.ON_OPEN_STATE);
					stateInfo.setText(R.string.state_info_nothing);
					btnStartTraining.setEnabled(true);
					isMeasurementStarted = false;
					//btnRunInBackground.setEnabled(false);
					btnStartMeasurement.setText(R.string.start_measurment);
					/** Record data when user stop the measurement */
					signalVectorService.recordData();
					
					/** Inform to stop GPS */
					Intent intent=new Intent();
					intent.setAction(MEASUREMENT_IS_STOPPED);
					sendBroadcast(intent);
					
					updateCanvas();
				}
			}
		});
		/**Click to put the measurement to background */
		/* btnRunInBackground = (Button) findViewById(R.id.button_do_in_background);
		this.btnRunInBackground.setOnClickListener(new OnClickListener(){

			public void onClick(View arg0) {
				Intent mainPageIntent = new Intent(context,MainPage.class);
				mainPageIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				activity.startActivity(mainPageIntent);
			}
			
		});
		btnRunInBackground.setEnabled(false);*/
		 
		//devicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_item);
		deviceMag = DevicesManaging.getInstance();
		
		/** Bluetooth BroadcastReceiver*/
		btReceiver = new BTBroadcastReceiver();
		
		/** Before user start measurement, user need to choose which set of Bluetooth allowed devices
		 *  are going to be used in measurement, it's a broadcast receiver to receiver the site selected
		 *  by user */
		siteSelectedReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {

				String action = intent.getAction();
				if (action.equals(SelectMeasureSiteDialog.MEASUREMENT_SITE_SELECTED))
				{
					DevicesManaging.getInstance().setIsCurrentFalse();
					DevicesManaging.getInstance().setIsInScopeFalse();
					selectedSite = intent.getStringExtra("SelectedSite");
					/** Load corresponding set of BT allowed devices into the application */
					signalVectorService.setAllowedDevice(selectedSite);
			    	if(continueInquiry == false)
					{
			    		continueInquiry = true;
						localBTAdapter.startDiscovery();
					}
			    	btnStartTraining.setEnabled(false);
					isMeasurementStarted = true;
					stateInfo.setText(R.string.state_info_measuring);
					btnStartMeasurement.setText(R.string.stop_measurment);
					
					updateCanvas();
					//btnRunInBackground.setEnabled(true);
				}
			}
		};
		
		/** Timer task to start discovering Bluetooth devices */
		task = new TimerTask()
	    {
	    	public void run() 
	    	{
	    		localBTAdapter.startDiscovery();
	    	}
	    };
	}
    
    /**Set welcome information in the UI*/
    public void setWelcomeInfo()
	{
    	Log.i("RTDA", "set welcome information");
    	String welInfo = "Welcome, Today is: " + time.monthDay + "/" +
    		(time.month + 1) + "/" + time.year + "!";
		textWelcomeInfo.setText(welInfo);
	}
    
    /**Get local BT adapter and enable Bluetooth*/
    public void openBTAdapter()
	{
		Log.i("INFO", "open Bluetooth Device Adapter.");
    	localBTAdapter = BluetoothAdapter.getDefaultAdapter();
    	if (localBTAdapter == null)
		{
    		/** If the device doesn't support Bluetooth, the end this module */
    		Toast toast = Toast.makeText(this, R.string.bt_not_available, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER_VERTICAL| Gravity.CENTER_HORIZONTAL,0,0);
			toast.show();
            finish();
            return;
		}    	
		if(!localBTAdapter.isEnabled())
		{
			/** Call enable() to enable Bluetooth without request for user permission
			 *  This requires android.permission.BLUETOOTH_ADMIN Permission */
			localBTAdapter.enable();
		}
	}
    
    public void updateCanvas()
    {
    	/** Set the list of Bluetooth devices that are going to be presented on canvas */
		btDevicesCanvas.setBTDevicesList(deviceMag.returnDevicesList());
		/** Refresh the canvas */
		btDevicesCanvas.invalidate();
    }
    
    /** Inner class Broadcast receiver listen to Bluetooth discovering results */
    public class BTBroadcastReceiver extends BroadcastReceiver{
    	
    	@Override
    	public void onReceive(Context context, Intent intent) {
    		
    		if(deviceMag.getCanvasState() != DevicesManaging.ON_SHARE_STATE)
    		{
    			String action = intent.getAction();
    			
    			/** Catch remote Bluetooth device found action */
    			if (BluetoothDevice.ACTION_FOUND.equals(action))
    			{
    				/** Get BluetoothDevice object of the found remote BT device */
    				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					
    				/** Depends on the state of the application will do different processes */
					switch(DevicesManaging.getInstance().getCanvasState())
					{
					/** When the application is just run, all discovered devices will be recorded as useless devices */
					case DevicesManaging.ON_OPEN_STATE: //Open the application, it's in open state
						deviceMag.addUselessDevice(device);
						break;
					/** When use starts training allowed devices sets, 
					 *  newly discovered devices will be recorded as allowed devices */
					case DevicesManaging.ON_START_STATE: //Start teaching BT device
						deviceMag.addAllowedDevice(device);
						break;
					/** When user start measuring, the application starts recording activity records */
					case DevicesManaging.ON_RUN_STATE: //Recording routes
						deviceMag.recordEnterTime(device);
						break;
					}
					
					updateCanvas();
    			}
    			/** Catch Bluetooth discovery finished action */
    			else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
    			{
    				/** Check is previous discovered BT devices still in scope */
    				deviceMag.checkIsInScope();
    				
    				updateCanvas();
    				
    				deviceMag.setIsCurrentFalse();
    				
    				/** If continuously discovering is true then start next round of discovery */
    				if(continueInquiry == true)
    				{
    					localBTAdapter.startDiscovery();
    				}
    			}	
    		}
    	}
    }
}