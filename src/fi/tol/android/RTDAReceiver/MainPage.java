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
 *  Activity MainPage
 *
 *  Main page of this application
 *  A grid view, each item represents one module
 *  Modules can be enabled or disabled in the "preference setting" item on the main page
 */

package fi.tol.android.RTDAReceiver;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class MainPage extends Activity{
	
	private String username;
	private static Context context;
	private Activity activity;
	
	/** Default preference settings from MainPreferenceActivity */
	private SharedPreferences sharedPreference;
	private SharedPreferences.Editor sharedPreferenceEditor;
	
	/** Preference settings own by specific username */
	private SharedPreferences userPreference;
	private SharedPreferences.Editor userPreferenceEditor;
	
	/** SharedPreference for account info of all registered users */
	private SharedPreferences accountSharedPreference;
	private SharedPreferences.Editor accountSharedPreferenceEditor;
	
	/** GridView for the MainPage, each item in the GridView is an image and text*/
	private GridView mainPageGridView;
	/** Image adapter for each item shown in the grid */
	private ImageAdapter mainPageIconsAdapter;
	/** The number of items in the grid */
	private int itemsNum;
	/** The list of the items, the value means the Module it correspond to */
	private ArrayList<Integer> itemsList;
	/** Values to represent different Modules */
	private static final int RTDA = 1;
	private static final int PING_I = 2;
	private static final int PING_II_INCOMING = 3;
	private static final int PING_II_OUTGOING = 4;
	private static final int MEETING_TIME_RECORDER = 5;
	private static final int EXCHANGE_BY_KNOCK = 6;
	private static final int TASK_RECORDER = 7;
	
	/** BroadcastReceiver to listen to changes of user preference setting */
	protected static final String UPDATE_USER_PREFERENCE_SETTING = "UPDATE_USER_PREFERENCE_SETTING";
	private BroadcastReceiver userPreferenceUpdateReceiver;
	
	/** Phone call listener service Name */
	public static final String PhoneCallListenerServiceName = "fi.tol.android.RTDAReceiver.PhoneCallListenerService";
	
	private Menu mainPageMenu = null;
	
	/** For manage notification in notification bar */
	private NotificationManager notificationMag;
	
	/** Attributes for CallLog inquiry usage */
	public static final String strOrder = android.provider.CallLog.Calls.DATE + " DESC";
	public static String[] strFields = {
        android.provider.CallLog.Calls.NUMBER, 
        android.provider.CallLog.Calls.TYPE,
        android.provider.CallLog.Calls.CACHED_NAME,
        android.provider.CallLog.Calls.DATE,
        android.provider.CallLog.Calls.DURATION
        };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_page);
		context = this;
		activity = this;
		
		mainPageGridView = (GridView)findViewById(R.id.main_page_grid);
		
		/** Get default preference settings that is saved by MainPreferenceActivity */
		sharedPreference = PreferenceManager.getDefaultSharedPreferences(context);
		sharedPreferenceEditor = sharedPreference.edit();
		
		/** Get SharedPreference for account info of all registered users */
        accountSharedPreference = getSharedPreferences("Account",Context.MODE_PRIVATE);
        accountSharedPreferenceEditor = accountSharedPreference.edit();
        
        notificationMag = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        
        username = accountSharedPreference.getString("last_log_in", null);
        
        this.setTitle("Tervetuloa " + username);
        itemsList = new ArrayList<Integer>();
        
        /** Get preference settings own by specific user name */
        userPreference = activity.getSharedPreferences(username, Context.MODE_PRIVATE);
        userPreferenceEditor = userPreference.edit();
		
        /** Read user preference settings into the default SharedPreference, 
         * 	which will be used by MainPreferenceActivity to present to user */
		loadUserPreference();
		/** Get the items of enabled modules from user preference setting */
        getItemsNum();
        
        /** Set image adapter for Grid View */
		mainPageIconsAdapter = new ImageAdapter();
		mainPageGridView.setAdapter(mainPageIconsAdapter);
		
		
		/** Gridview item click listener, means user start the module which is represented by the item*/
		mainPageGridView.setOnItemClickListener(new OnItemClickListener() {
	        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
	        	/** Start Preference Setting Page */
	        	if(position == (itemsNum-2))
	            {
		            Intent preferenceIntent = new Intent(context, MainPreferenceActivity.class);
		            preferenceIntent.putExtra("username", username);
		            preferenceIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					
		            activity.startActivity(preferenceIntent);
	            }
	        	/** Open Account Setting Page */
	            else if(position == (itemsNum -1))
	            {
	            	Intent accountIntent = new Intent(context, AccountInfoActivity.class);
	            	accountIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	            	activity.startActivity(accountIntent);
	            }
	        	/** Start other modules */
	        	if((itemsList.size()!=0) && (position != (itemsNum - 2)) && (position != (itemsNum -1)))
            	{
            		switch(itemsList.get(position))
            		{
            		/** Start RTDA Bluetooth Process Data Acquisition Module */
            		case MainPage.RTDA:
            			Intent rtdaIntent = new Intent(context, RTDAReceiver.class);
            			rtdaIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            			activity.startActivity(rtdaIntent);
            			break;
            		/** Start PingI App for doctor to record emergency phone calls */
            		case MainPage.PING_I:
            			Intent pingIntent = new Intent(context,PhoneCallConsultation.class);
            			pingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            			
            			PhoneCallListenerService.readQuestionnaire(username);
            			activity.startActivity(pingIntent);
            			break;
            		/** Start PingII(incoming) for nurse to record incoming phone calls */
            		case MainPage.PING_II_INCOMING:
            			Intent pingIIinIntent = new Intent(context,PingIIincomingMainPage.class);
            			pingIIinIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            			activity.startActivity(pingIIinIntent);
            			break;
            		/** Start PingII(outgoing) for nurse to record outgoing phone calls */
            		case MainPage.PING_II_OUTGOING:
            			Intent pingIIoutIntent = new Intent(context,PingIIOutgoingMainPage.class);
            			pingIIoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            			activity.startActivity(pingIIoutIntent);
            			break;
            		case MainPage.MEETING_TIME_RECORDER:
            			Intent meetingRecorderIntent = new Intent(context,MeetingTimeRecorderActivity.class);
            			meetingRecorderIntent.putExtra("username", username);
            			meetingRecorderIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            			activity.startActivity(meetingRecorderIntent);
            			break;
            		case MainPage.EXCHANGE_BY_KNOCK:
            			Intent knockIntent = new Intent(context,BluetoothSynchronize.class);
            			//Intent knockIntent = new Intent(context,AccelerometerKnockActivity.class);
            			knockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            			activity.startActivity(knockIntent);
            			break;
            		/** Start Task recorder */
            		case MainPage.TASK_RECORDER:
            			Intent taskIntent = new Intent(context,TaskRecorderActivity.class);
            			taskIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            			//taskIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            			activity.startActivity(taskIntent);
            			break;
            		}
            	}
	        }
	    });
        
		/** BroadcastReceiver to listen to changes of user preference setting */
        userPreferenceUpdateReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if(action.equals(MainPage.UPDATE_USER_PREFERENCE_SETTING))
				{
					/** Write the preference setting of MainPreferenceActivity to 
					 *  currently login user's preference setting file*/
					writeUserPreference();
					/** reload the user preference setting to update main page's grid view */
					getItemsNum();
					
					/** Doesn't use adapter notify data set change method to avoid grid items disorder problem */
					mainPageIconsAdapter = new ImageAdapter();
					mainPageGridView.setAdapter(mainPageIconsAdapter);
					
				}
			}
        	
        };
        IntentFilter filter = new IntentFilter(MainPage.UPDATE_USER_PREFERENCE_SETTING);
        registerReceiver(userPreferenceUpdateReceiver,filter);
	}
	
	@Override
	/** Create Options Menu */
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		mainPageMenu = menu;
		mainPageMenu.add(R.string.menu_log_out);
		return true;
		
	}

	@Override
	/** Options Menu click listener */
	public boolean onOptionsItemSelected(MenuItem item) {
		/** When the user logout */
		if (item.getItemId() == 0) {
			accountSharedPreferenceEditor.remove("last_log_in");
			accountSharedPreferenceEditor.commit();

	    	/** When logout stop the background service if it's running 
	    	 *  and dismiss the icon in the notification bar */
			if(isServiceRunning(context) == true)
			{
				context.stopService(new Intent(context,PhoneCallListenerService.class));
				 //notificationMag.cancel(PhoneCallListenerService.notificationID);
			}
	    	
	    	/** When logout return to Login Page */
			Intent loginIntent = new Intent(context,MainLogin.class);
			loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			activity.startActivity(loginIntent);
			activity.finish();
        }  
		return true;
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(userPreferenceUpdateReceiver);
		super.onDestroy();
	}

	/** Write the preference setting of MainPreferenceActivity to 
	 *  currently login user's preference file
	 *  and create corresponding folders for different modules that are enabled */
	public void writeUserPreference()
	{
        boolean boolTemp = false;
        String subFolderPath;
        
        /** Read default preference setting whether the module is enabled 
         *  Write boolean value whether the module is enable into user's own SharedPreference
         *  If the module is enable, need to create corresponding module folder for it*/
        
        
        boolTemp = sharedPreference.getBoolean(getString(R.string.RTDA_key), false);
        userPreferenceEditor.putBoolean(getString(R.string.RTDA_key), boolTemp);
        /** Bluetooth process data acquisition module is enabled */
        if(boolTemp == true)
        {
        	subFolderPath = Environment.getExternalStorageDirectory() + "/" + MainLogin.appHomeFolder + "/" + username
        		+ "/" + MainLogin.rtdaSubFolder;
        	if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && !MainLogin.isFileExist(subFolderPath))
			{
					File rtdaDir = new File(subFolderPath);
					rtdaDir.mkdir();
					
					File allowedDir = new File(subFolderPath + "/" + MainLogin.rtdaAllowedFolder);
					allowedDir.mkdir();
					
					File signalVectorDir = new File(subFolderPath + "/" + MainLogin.rtdaSignalVectorFolder);
					signalVectorDir.mkdir();
			}
        }
        
        boolTemp = sharedPreference.getBoolean(getString(R.string.Ping_I_key), false);
        userPreferenceEditor.putBoolean(getString(R.string.Ping_I_key), boolTemp);
        /** PingI module is enabled */
        if(boolTemp == true)
        {
        	subFolderPath = Environment.getExternalStorageDirectory() + "/" + MainLogin.appHomeFolder + "/" + username
        		+ "/" + MainLogin.pingISubFolder;
        	if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && !MainLogin.isFileExist(subFolderPath))
			{
					File pingDir = new File(subFolderPath);
					pingDir.mkdir();
					
					File phoneConsultationDir = new File(subFolderPath + "/" + MainLogin.pingICallConsultationFolder);
					phoneConsultationDir.mkdir();
					
					/** This module requires questionnaire, so after user enable this module
					 *  a dialog will pop up ask user to edit the questionnaire now or later
					 */
					new AlertDialog.Builder(MainPage.this)
					.setTitle(R.string.whether_edit_questionnaire)
					
					/** If user want to immediately, then direct to QuestionEditor page */
					.setPositiveButton("Kyllä", new DialogInterface.OnClickListener() {
					        public void onClick(DialogInterface dialog, int which) {
					        	Intent editQuestionnaireIntent = new Intent(context,QuestionEditor.class);
					        	editQuestionnaireIntent.putExtra("username", username);
					        	editQuestionnaireIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					        	MainPage.this.startActivity(editQuestionnaireIntent);
							    dialog.cancel();
					    }})
					    
					/** If user want to edit the questionnaire later
					 *  so just close the AlertDialog
					 *  and user can edit the questionnaire later through Ping module page's option menu */
					.setNegativeButton("Myöhemmin", new DialogInterface.OnClickListener() {
					        public void onClick(DialogInterface dialog, int which) {
					            dialog.cancel();
					    }}
					).show();
			}
        	
        }

        
        boolTemp = sharedPreference.getBoolean(getString(R.string.Ping_II_incoming_key), false);
        userPreferenceEditor.putBoolean(getString(R.string.Ping_II_incoming_key), boolTemp);
        /** PingII(incoming) module is enabled */
        if(boolTemp == true)
        {
        	subFolderPath = Environment.getExternalStorageDirectory() + "/" + MainLogin.appHomeFolder + "/" + username
        		+ "/" + MainLogin.pingIIincomingFolder;
        	if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && !MainLogin.isFileExist(subFolderPath))
			{
					File pingIIinDir = new File(subFolderPath);
					pingIIinDir.mkdir();
					
					File phoneConsultationDir = new File(subFolderPath + "/" + MainLogin.pingIIincomingCallRecordsFolder);
					phoneConsultationDir.mkdir();
			}
        }
        	
        boolTemp = sharedPreference.getBoolean(getString(R.string.Ping_II_outgoing_key), false);
        userPreferenceEditor.putBoolean(getString(R.string.Ping_II_outgoing_key), boolTemp);
        /** PingII(outgoing) module is enabled */
        if(boolTemp == true)
        {
        	subFolderPath = Environment.getExternalStorageDirectory() + "/" + MainLogin.appHomeFolder + "/" + username
        		+ "/" + MainLogin.pingIIoutgoingFolder;
        	if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && !MainLogin.isFileExist(subFolderPath))
			{
					File pingIIoutDir = new File(subFolderPath);
					pingIIoutDir.mkdir();
					
					File outgoingRecordsDir = new File(subFolderPath + "/" + MainLogin.pingIIoutgoingCallRecordsFolder);
					outgoingRecordsDir.mkdir();
			}
        }

        

        boolTemp = sharedPreference.getBoolean(getString(R.string.meeting_time_recorder_key), false);
        userPreferenceEditor.putBoolean(getString(R.string.meeting_time_recorder_key), boolTemp);
        /** Meeting time recorder module is enabled */
        if(boolTemp == true)
        {
        	subFolderPath = Environment.getExternalStorageDirectory() + "/" + MainLogin.appHomeFolder + "/" + username
        		+ "/" + MainLogin.meetingTimeSubFolder;
        	if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && !MainLogin.isFileExist(subFolderPath))
			{
					File meetingTimerRecorderDir = new File(subFolderPath);
					meetingTimerRecorderDir.mkdir();
			}
        }
        
        boolTemp = sharedPreference.getBoolean(getString(R.string.exchange_by_knock_key), false);
        userPreferenceEditor.putBoolean(getString(R.string.exchange_by_knock_key), boolTemp);
        
        boolTemp = sharedPreference.getBoolean(getString(R.string.task_recorder_key), false);
        userPreferenceEditor.putBoolean(getString(R.string.task_recorder_key), boolTemp);
        /** Task recorder module is enabled */
        if(boolTemp == true)
        {
        	subFolderPath = Environment.getExternalStorageDirectory() + "/" + MainLogin.appHomeFolder + "/" + username
        		+ "/" + MainLogin.tasksSubFolder;
        	if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && !MainLogin.isFileExist(subFolderPath))
			{
					File taskDir = new File(subFolderPath);
					taskDir.mkdir();
					
					File taskRecordsDir = new File(subFolderPath + "/" + MainLogin.taskRecordsFolder);
					taskRecordsDir.mkdir();
					
					File sensorDataDir = new File(subFolderPath + "/" + MainLogin.sensorDataFolder);
					sensorDataDir.mkdir();
			}
        }
        
        /** Submit user's SharedPreference setting */
        userPreferenceEditor.commit();
	}
	
	/** Get the items of enabled modules from user preference setting */
	public void getItemsNum()
	{
		boolean pingI;
		boolean pingIIin;
		boolean pingIIout;
		itemsList.clear();
		itemsNum = 0;
		if(userPreference.getBoolean(activity.getString(R.string.RTDA_key), false) == true)
			itemsList.add(MainPage.RTDA);
		pingI = userPreference.getBoolean(activity.getString(R.string.Ping_I_key), false);
		if(pingI == true)
			itemsList.add(MainPage.PING_I);
		pingIIin = userPreference.getBoolean(activity.getString(R.string.Ping_II_incoming_key), false);
		if(pingIIin == true)
			itemsList.add(MainPage.PING_II_INCOMING);
		pingIIout = userPreference.getBoolean(activity.getString(R.string.Ping_II_outgoing_key), false);
		if(pingIIout == true)
			itemsList.add(MainPage.PING_II_OUTGOING);
		if(userPreference.getBoolean(activity.getString(R.string.meeting_time_recorder_key), false) == true)
			itemsList.add(MainPage.MEETING_TIME_RECORDER);
		if(userPreference.getBoolean(activity.getString(R.string.exchange_by_knock_key), false) == true)
			itemsList.add(MainPage.EXCHANGE_BY_KNOCK);
		if(userPreference.getBoolean(activity.getString(R.string.task_recorder_key), false) == true)
			itemsList.add(MainPage.TASK_RECORDER);
		
		/** Check weather need to start Phone call listener service */
		toStartService(pingI,pingIIin,pingIIout);
		
		itemsNum = itemsList.size() + 2;
	}
	
	/** Is user preference setting exists, then read those settings into
	 *  the default SharedPreference, which will be used by
	 *  MainPreferenceActivity to present to user */
	public void loadUserPreference()
	{
		String key;
        boolean boolTemp = false;
        
		key = activity.getString(R.string.RTDA_key);
		boolTemp = userPreference.getBoolean(key, false);
		sharedPreferenceEditor.putBoolean(key, boolTemp);
        
        key = activity.getString(R.string.Ping_I_key);
        boolTemp = userPreference.getBoolean(key, false);
        sharedPreferenceEditor.putBoolean(key, boolTemp);
        
        key = activity.getString(R.string.Ping_II_incoming_key);
        boolTemp = userPreference.getBoolean(key, false);
        sharedPreferenceEditor.putBoolean(key, boolTemp);
        
        key = activity.getString(R.string.Ping_II_outgoing_key);
        boolTemp = userPreference.getBoolean(key, false);
        sharedPreferenceEditor.putBoolean(key, boolTemp);
        
        key = activity.getString(R.string.meeting_time_recorder_key);
        boolTemp = userPreference.getBoolean(key, false);
        sharedPreferenceEditor.putBoolean(key, boolTemp);
        
        key = activity.getString(R.string.task_recorder_key);
        boolTemp = userPreference.getBoolean(key, false);
        sharedPreferenceEditor.putBoolean(key, boolTemp);
        
        sharedPreferenceEditor.commit();
	}
	
	/** Check whether PhoneCalllistnerService is running */
	public static boolean isServiceRunning(Context context)
	{
		boolean isRunning = false;
		/** Used to interact with the overall activities running in the system */
        ActivityManager activityManager = (ActivityManager)context.getSystemService(ACTIVITY_SERVICE); 
        /** Get the list of currently running services */
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(Integer.MAX_VALUE);
        
        /** If no service is running, return false */
        if (!(serviceList.size()>0)) 
        {
            return false;
        }
        
        /** Check is our own PhoneCallListenerService running */
        for (int i=0; i<serviceList.size(); i++) 
        {
        	if (serviceList.get(i).service.getClassName().equals(PhoneCallListenerServiceName) == true) 
        	{
                isRunning = true;
                break;
            }
        }
        return isRunning;
	}
	
	/** Ping Apps requires a background running service, so
	 *  here when one of this group of modules is enabled, the service will be started
	 *  and when none of this group of modules is enabled, the service will be stopped */
	public static void toStartService(boolean pingI, boolean pingIIin, boolean pingIIout)
	{
		/** Service will be started if the service isn't running and when at least one module is enabled */
		if(isServiceRunning(context) == false && (pingI == true || pingIIin == true || pingIIout == true))
		{
			context.startService(new Intent(context,PhoneCallListenerService.class));
		}
		/** When service is running and none of the Ping Apps is enable, background service will be stopped */
		else if(isServiceRunning(context) == true && (pingI == false && pingIIin == false && pingIIout == false))
		{
			context.stopService(new Intent(context,PhoneCallListenerService.class));
		}
	}
	
	/** Inner Class ImageAdapter for items of the Main page grid view */
	public class ImageAdapter extends BaseAdapter{
	    /** Get number of items */
		public int getCount() {
			return itemsNum;
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}

		/** Get the view of the grid item: an image and text */
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
	         
	         if ( convertView == null )
	         {
	            /** Inflate the layout of the grid item */
	            LayoutInflater layoutInflater = getLayoutInflater();
	            view = layoutInflater.inflate(R.layout.grid_item, null);
	            
	            TextView text = (TextView)view.findViewById(R.id.grid_item_text);
	            ImageView image = (ImageView)view.findViewById(R.id.grid_item_image);
	            
	            /** In Main page two default grid items are 'Preference' and 'Account'
	             *  They are presented always the last two items in the grid view */
	            if(position == (itemsNum-2))
	            {
		            text.setText(R.string.preference);
		            image.setImageResource(R.drawable.preference_icon);
	            }
	            else if(position == (itemsNum -1))
	            {
	            	text.setText(R.string.account);
		            image.setImageResource(R.drawable.account_icon);
	            }
	            
	            /** If user enable any modules, then the main page will shows one icon in
	             *  the grid view for each module */
	            if(itemsList.size() != 0)
	            {
	            	if((position != (itemsNum - 2)) && (position != (itemsNum -1)))
	            	{
	            		switch(itemsList.get(position))
	            		{
	            		/** Bluetooth Process Data Acquisition Moudle, then show RTDA icon */
	            		case MainPage.RTDA:
	            			text.setText(R.string.RTDA_icon);
	            			image.setImageResource(R.drawable.rtda_icon);
	            			break;
	            		case MainPage.PING_I:
	            			text.setText(R.string.Ping_I_icon);
	            			image.setImageResource(R.drawable.ping_1_icon);
	            			break;
	            		case MainPage.PING_II_INCOMING:
	            			text.setText(R.string.Ping_II_incoming_icon);
	            			image.setImageResource(R.drawable.ping_2_incoming_icon);
	            			break;
	            		case MainPage.PING_II_OUTGOING:
	            			text.setText(R.string.Ping_II_outgoing_icon);
	            			image.setImageResource(R.drawable.ping_2_outgoing_icon);
	            			break;
	            		case MainPage.MEETING_TIME_RECORDER:
	            			text.setText(R.string.meeting_time_recorder_icon);
	            			image.setImageResource(R.drawable.meeting_time_recorder_icon);
	            			break;
	            		case MainPage.EXCHANGE_BY_KNOCK:
	            			text.setText(R.string.exchange_by_knock_icon);
	            			image.setImageResource(R.drawable.exchange_by_knock);
	            			break;
	            		case MainPage.TASK_RECORDER:
	            			text.setText(R.string.task_recorder_icon);
	            			image.setImageResource(R.drawable.task_recorder);
	            			break;
	            		}
	            	}
	            }
	         }
	         return view;
		}
	}
}
