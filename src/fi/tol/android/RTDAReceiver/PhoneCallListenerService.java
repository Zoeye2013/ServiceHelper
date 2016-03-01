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
 *  Service PhoneCallListenerService
 *
 *  Service for monitoring phone call status changes:Calls incoming, hand-off the phone
 *  Once the PintI module is started, this service will run in background all the time
 *  When phone is rebooted, service will be started after boot is completed
 */

package fi.tol.android.RTDAReceiver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Environment;
import android.os.IBinder;
import android.provider.CallLog.Calls;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import android.util.Log;

public class PhoneCallListenerService extends Service{
	
	private Context serviceContext;
	//protected static final int notificationID =1;
	private static int recordNumber;
	private String username;
	
	/** Managing notification in notification bar */
	//private NotificationManager notificationMag;
	
	/** For listening phone calls */
	private TelephonyManager telephonyMag;
	private PhoneCallStateListener phoneCallStateListener;
	
	/** File read and write attributes */
	private FileOutputStream fileOut;
	private FileInputStream fileIn;
	private InputStreamReader inReader;
	private BufferedReader bfReader;
	
	/** SharedPreference for account info of all registered users */
	private SharedPreferences accountPreference;
	
	/** Preference settings own by specific username */
	private SharedPreferences userPreference;
	private SharedPreferences.Editor userPreferenceEditor;
	
	
	protected static final String UPDATE_PHONECALL_TO_ACTIVITY = "UPDATE_PHONECALL_TO_ACTIVITY";
	
	/** Action strings for preference setting changes */
	protected static final String PINGI_ENABLED = "PINGI_ENABLED";
	protected static final String PINGII_INCOMMING_ENABLED = "PINGII_INCOMMING_ENABLED";
	protected static final String PINGII_OUTGOING_ENABLED = "PINGII_OUTGOING_ENABLED";
	protected static final String RE_LOAD_QUESTIONNAIRE = "RE_LOAD_QUESTIONNAIRE";
	
	/** BroadcastReceiver to listen to changes of user preference setting */
	private BroadcastReceiver preferenceChangeReceiver;
	
	
	/** List for Managing Phone Call records for PingI module*/
	private static ArrayList<PhoneCallRecordItem> consultationRecordMag;
	/** List for Managing Questionnaire */
	private static ArrayList<PhoneCallConsultationQuestion> consultationQuestionList;
	
	/** List for Managing Phone Call records for PingII(incoming) module*/
	private static ArrayList<PhoneCallRecordItem> ping2incomingCallRecordMag;
	/** List for Managing Phone Call records for PingII(outgoing) module*/
	private static ArrayList<PhoneCallRecordItem> ping2outgoingCallRecordMag;
	
	/** For PingI module, record the index of the phone call user just answered */
	private int pingIAnsweredPhoneCallIndex = -1;
	/** For PingII(incoming) module, record the index of the phone call user just answered */
	private int answeredPhoneCallIndex = -1;
	
	/** For PingII(outgoing) module, record the index of the outgoing phone call user just made */
	private int outgoingPhoneCallIndex = -1;
	
	/** Three Ping Module's enable or disable boolean values */
	private boolean pingI = false;
	private boolean pingIIin = false;
	private boolean pingIIout = false;
	private boolean isRinging = false;
	
	/** Time that an outgoing call is ended */
	private Time outgoingCallEndTime = new Time();

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate(){
        super.onCreate();
        Log.i("Service", "service created");
        
        serviceContext = this;
        
        /** For listening phone calls */
        telephonyMag = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
        phoneCallStateListener = new PhoneCallStateListener();
        telephonyMag.listen(phoneCallStateListener,PhoneStateListener.LISTEN_CALL_STATE);
        //notificationMag = (NotificationManager) serviceContext.getSystemService(NOTIFICATION_SERVICE);
        
        /** Get shared preferences after reboot */
        accountPreference = getSharedPreferences("Account",Context.MODE_PRIVATE);
        username = accountPreference.getString("last_log_in", null);
        userPreference = this.getSharedPreferences(username, Context.MODE_PRIVATE);
        userPreferenceEditor = userPreference.edit();
        
    	recordNumber = 0;
    	
    	/** Initiation work based on enabled Ping modules */
        /** PingI is enabled */
        pingI = userPreference.getBoolean(serviceContext.getString(R.string.Ping_I_key), false);
        if( pingI == true)
        {
        	initiateForPingI();
        }
        /** PingII(incoming) is enable */
        pingIIin = userPreference.getBoolean(serviceContext.getString(R.string.Ping_II_incoming_key), false);
        if(pingIIin == true)
        {
        	initiateForPingIIin();
        }
        /** PingII(outgoing) is enable */
        pingIIout = userPreference.getBoolean(serviceContext.getString(R.string.Ping_II_outgoing_key), false);
        if(pingIIout == true)
        {
        	initiateForPingIIout();
        }
        
    	/** BroadcastReceiver to listen to changes of user preference setting */
    	preferenceChangeReceiver = new BroadcastReceiver(){

			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				/** Initiation work based on enabled Ping modules */
		        /** PingI is enabled */
				if(action.equals(PINGI_ENABLED))
				{
					initiateForPingI();
				}
				/** PingII(incoming) is enable */
				else if(action.equals(PINGII_INCOMMING_ENABLED))
				{
					initiateForPingIIin();
				}
				/** PingII(outgoing) is enable */
				else if(action.equals(PINGII_OUTGOING_ENABLED))
				{
					initiateForPingIIout();
				}
				else if(action.equals(RE_LOAD_QUESTIONNAIRE))
				{
					readQuestionnaire(username);
				}
			}
        };
        
        IntentFilter filter = new IntentFilter(PINGI_ENABLED);
        registerReceiver(preferenceChangeReceiver,filter);
        filter = new IntentFilter(PINGII_INCOMMING_ENABLED);
        registerReceiver(preferenceChangeReceiver,filter);
        filter = new IntentFilter(PINGII_OUTGOING_ENABLED);
        registerReceiver(preferenceChangeReceiver,filter);
        filter = new IntentFilter(RE_LOAD_QUESTIONNAIRE);
        registerReceiver(preferenceChangeReceiver,filter);
	}
	
	/** Initiation work based on enabled Ping modules */
    /** PingI is enabled */
	public void initiateForPingI()
	{
		/** Get the number of uncompleted phone call records when user last logout,
         *  from shared preferences*/
        recordNumber = userPreference.getInt("recordNumber", 0);
        
        /** Initiate Phone call records lists for PingI */
    	consultationRecordMag = new ArrayList<PhoneCallRecordItem>();
        consultationQuestionList = new ArrayList<PhoneCallConsultationQuestion>();
        consultationRecordMag.clear();
        
        /** If there are any uncompleted phone call records when user last logout 
         *  Then create notification in notification bar 
         *  and read those records from uncompleted call log csv file */
        if(recordNumber > 0)
        {
        	/*Notification callRecords = new Notification();
			callRecords.icon = R.drawable.notification_icon;
			callRecords.tickerText = PhoneCallListenerService.getPhoneCallNum() + " " + 
			serviceContext.getText(R.string.notification_scroll);
			callRecords.when = System.currentTimeMillis();
			
			CharSequence notificationTitle =serviceContext.getText(R.string.notification_title);
			CharSequence notificationInfo = PhoneCallListenerService.getPhoneCallNum() + " calls";
			Intent phoneintent =new Intent(serviceContext,PhoneCallConsultation.class);
			
			PendingIntent notificationIntent =PendingIntent.getActivity(serviceContext,0, phoneintent,PendingIntent.FLAG_UPDATE_CURRENT);
			callRecords.setLatestEventInfo(serviceContext, notificationTitle, notificationInfo, notificationIntent);
			callRecords.defaults |= Notification.DEFAULT_SOUND;
			callRecords.flags |= Notification.FLAG_AUTO_CANCEL;
			notificationMag.notify(notificationID, callRecords);*/
			
			readUncompletedCallLog();
			PhoneCallListenerService.clearPhoneCallNum();
        }
        
        /** Read questionnaire from csv file in phone's SDcard */
        readQuestionnaire(username);
	}
	/** Initiate Phone call records lists for PingII(incoming) */
	public void initiateForPingIIin()
	{
		ping2incomingCallRecordMag = new ArrayList<PhoneCallRecordItem>();
	    ping2incomingCallRecordMag.clear();
	}
	/** Initiate Phone call records lists for PingII(outgoing) */
	public void initiateForPingIIout()
	{
		ping2outgoingCallRecordMag = new ArrayList<PhoneCallRecordItem>();
		ping2outgoingCallRecordMag.clear();
	}
	
	
	/** When the service is stop, stop listening phone call states,
	 *  and clear the notification in notification bar
	 *  and record uncompleted phone call records into csv file
	 */
     @Override
     public void onDestroy(){
    	 telephonyMag.listen(phoneCallStateListener, PhoneStateListener.LISTEN_NONE);
    	 unregisterReceiver(preferenceChangeReceiver);
    	 if(pingI == true)
    	 {
    		 //notificationMag.cancel(notificationID);
        	 recordUncompletedCallLog();
    	 }
    	 super.onDestroy();
     }
     
     /** Increase incoming phone call number during the period of user
      *  picking up a phone call and ending the same phone call
      *  Because during an answered phone call there maybe other phone call
      *  are trying to get through
      */
     public static void increasePhoneCallNum()
     {
    	 recordNumber ++;
     }
     
     public static void clearPhoneCallNum()
     {
    	 recordNumber = 0;
     }
     
     public static int getPhoneCallNum()
     {
    	 return recordNumber;
     }
     
     /** Get list for Managing Phone Call records in PingI module from background service*/
     public static ArrayList<PhoneCallRecordItem> getConsultationRecordMag()
     {
    	 return consultationRecordMag;
     }
     
     /** Get list for Managing Phone Call records in PingII(incoming) module from background service*/
     public static ArrayList<PhoneCallRecordItem> getPing2incomingCallRecordMag()
     {
    	 return ping2incomingCallRecordMag;
     }
     
     /** Get list for Managing Outgoing Phone Cll records in PingII(outgoing) module from background service */
     public static ArrayList<PhoneCallRecordItem> getPing2outgoingCallRecordMag()
     {
    	 return ping2outgoingCallRecordMag;
     }
     
     /** Get list for Managing questionnaire in PingI module from background service*/
     public static ArrayList<PhoneCallConsultationQuestion> getConsultationQuestionsList()
     {
    	 return consultationQuestionList;
     }
     
     /** Check is questionnaire empty */
     public static boolean isQuestionnaireEmpty()
     {
    	 if(consultationQuestionList.size() > 0)
    		 return false;
    	 else
    		 return true;
     }
     
     /** Search question index in the data list by question No. */
     public static Integer getQuestionIndex(int questionNo)
     {
    	 Integer questionIndex = null;
    	 for(int i = 0; i < PhoneCallListenerService.getConsultationQuestionsList().size(); i ++)
    	 {
    		 if(questionNo == PhoneCallListenerService.getConsultationQuestionsList().get(i).getQuestionNo())
    		 {
    			 questionIndex = i;
    			 break;
    		 }
    	 }
    	 return questionIndex;
     }
     
     /** Record uncompleted phone call records into csv file 
      *  Different from other files in this APP, this file will be saved
      *  in phone's build-in flash memory instead of in SDcard
      *  Because if user shut down the phone and later when user boot the phone
      *  SDcard won't be loaded before the service started*/
     private void recordUncompletedCallLog()
     {
    	 String phoneCalls = "";
    	 ArrayList<PhoneCallRecordItem> recordList = PhoneCallListenerService.getConsultationRecordMag();
    	 for(int i = 0; i <recordList.size(); i++)
    	 {
    		 phoneCalls += recordList.get(i).getType() + "," + recordList.get(i).getContactName() + "," +
    		 	recordList.get(i).getPhoneNumber() + "," + recordList.get(i).getBeginDate() + "," + 
    		 	recordList.get(i).getCallDuration() + "," + recordList.get(i).getTimezone() + "\n";
    	 }
    	 try
    	 { 
    		 fileOut = openFileOutput(MainLogin.uncompletedCallLogFile,Context.MODE_PRIVATE);
    		 fileOut.write(phoneCalls.getBytes());
    		 fileOut.close();
		 }
		 catch (FileNotFoundException e1) {
			e1.printStackTrace();
		 } catch (IOException e) {
			e.printStackTrace();
		 }
		 userPreferenceEditor.putInt("recordNumber", recordList.size());  
		 userPreferenceEditor.commit();
		 PhoneCallListenerService.getConsultationRecordMag().clear();
      }
     
     /** Read uncompleted records from uncompleted call log csv file and load into the APP*/
     public void readUncompletedCallLog()
     {
    	 try {
			fileIn = openFileInput(MainLogin.uncompletedCallLogFile);
			if(fileIn != null)
			{
				PhoneCallListenerService.getConsultationRecordMag().clear();
	    		String lineTemp = "";
	    		String[] tempArr = {};
	    		Time time = new Time();
				inReader = new InputStreamReader(fileIn);
   			 	bfReader = new BufferedReader(inReader);
   			 	while((lineTemp =bfReader.readLine()) != null) //Read one line
   			 	{
   			 		tempArr = lineTemp.split(",");
   			 		PhoneCallRecordItem record = new PhoneCallRecordItem();
					record.setType(Integer.parseInt(tempArr[0]));
					record.setContactName(tempArr[1]);
					record.setPhoneNumber(tempArr[2]);
					record.setBeginDate(Long.parseLong(tempArr[3]));
					record.setTimezone(tempArr[5]);
					time.set(record.getBeginDate());
					record.setDate(time.monthDay + "/" + (time.month+1) + "/" + time.year);
           		 	record.setBeginTime(time.hour + ":" + time.minute + ":" + time.second);
           		 
					if(record.getType() == Calls.MISSED_TYPE)
						record.setPhoneType(getResources().getString(R.string.missed_call));
					else if(record.getType() == Calls.INCOMING_TYPE)
					{
						record.setPhoneType(getResources().getString(R.string.incoming_call));
						record.setCallDuration(Long.parseLong(tempArr[4]));
	            		time.set(record.getBeginDate()+ record.getCallDuration()*1000);
	            		record.setEndTime(time.hour + ":" + time.minute + ":" + time.second);
					}
           		 	consultationRecordMag.add(record);
   			 	}
   			 	bfReader.close();
   			 	deleteFile(MainLogin.uncompletedCallLogFile);
			}
		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
     }
     
     /** Read questionnaire from csv file in phone's SDcard */
     public static void readQuestionnaire(String username)
     {
    	 if(MainLogin.isFileExist(MainLogin.sdCardPath + "/" + MainLogin.appHomeFolder + "/" + username
 	        		+ "/" + MainLogin.pingISubFolder + "/" + MainLogin.questionnaireFile))
    	 {
    		 consultationQuestionList.clear();
    		 String lineTemp = "";
    		 String[] tempArr = {};
    		 int line = 1;
    		 try
    		 {
    			 File file = new File(Environment.getExternalStorageDirectory() + "/" + MainLogin.appHomeFolder + "/" + username
    	 	        		+ "/" + MainLogin.pingISubFolder + "/" + MainLogin.questionnaireFile);
    			 FileInputStream fileIn = new FileInputStream(file);
    			 InputStreamReader inReader = new InputStreamReader(fileIn);
    			 BufferedReader bfReader = new BufferedReader(inReader);
    			 while(((lineTemp=bfReader.readLine()) != null)) //Read one line
    	 		 {
    				 if(line >1)
    				 {
    					 tempArr = lineTemp.split(",");
        				 PhoneCallConsultationQuestion question = new PhoneCallConsultationQuestion();
        				 question.setQuestionNo(Integer.parseInt(tempArr[0]));
        				 question.setQuestionTitle(tempArr[1]);
        				 question.setAnswerNum(Integer.parseInt(tempArr[2]));
        				 
        				 for(int i = 3; i < (3+question.getAnswerNum()*2);)
        				 {
        					 question.addAnswersList(tempArr[i]);
        					 question.addDirectToQuestionList(Integer.parseInt(tempArr[i+1]));
        					 i = i+2;
        				 }
        				 consultationQuestionList.add(question);
    				 }
    				 line ++;
            	 }
    			 bfReader.close();
    		 }catch (FileNotFoundException e1) {
    	 			e1.printStackTrace();
    	 	 } catch (IOException e) {
    	 			e.printStackTrace();
    	 	 }
    	 }
     }
     
     /** Private inner class that monitor call state changes 
      *  After answer a phone call it get Phone Call Log by using Content Provider */
     private class PhoneCallStateListener extends PhoneStateListener{
    	 private Cursor callLogCursor;
    	 
    	 /** Timer to delay the acquiring of call logs, 
 		 *  to ensure CallLog has successfully updated */
    	 private Timer delayLoadCallLogTimer = new Timer();
    	 
    		@Override
    	    public void onCallStateChanged(int state,String incomingNumber)
    	    {
    			super.onCallStateChanged(state, incomingNumber);
    			switch(state)
    			{
    			
    			/** When phone turns to Idle state after answer a phone call
    			 *  get Phone Call Log, use Timer to set a delay to ensure 
    			 *  Phone Call Log is updated before our APP try to get the data from it*/
    			case TelephonyManager.CALL_STATE_IDLE:
    				if(PhoneCallListenerService.getPhoneCallNum() > 0)
    				{
    					outgoingCallEndTime.setToNow();
    					delayLoadCallLogTimer.schedule(new TimerTask(){
    			 	    	public void run() {
    			 	    		getCallLog();}}, 1000);
    				}
    				break;
    			case TelephonyManager.CALL_STATE_OFFHOOK:
    				if(isRinging == false)
    				{
    					PhoneCallListenerService.increasePhoneCallNum();
    				}
    				isRinging = false;
    				break;
    			
    			/** When an incoming call is ringing */
    			case TelephonyManager.CALL_STATE_RINGING:
    				PhoneCallListenerService.increasePhoneCallNum();
    				isRinging = true;
    				break;
    			}
    	    }
    		
    		/** get Phone Call Log by using Content Provider */
    		public void getCallLog()
    	    {
    			/** Get Content Provider object and query the CallLog by using SQL */
    			callLogCursor = getContentResolver().query(
    			        android.provider.CallLog.Calls.CONTENT_URI,
    			        MainPage.strFields,
    			        null,
    			        null,
    			        MainPage.strOrder);
    			Time time = new Time();
    			
    			
    			
    			/** If query result is not null, move to the first record of the results */
    			if (callLogCursor != null && callLogCursor.moveToFirst()) {
    				 
    				 /** Fetch 'recordNumber' records from the CallLog and
    				  *  add these records into consultationRecordMag*/
    				 for(int i = 0; i < PhoneCallListenerService.getPhoneCallNum(); i ++)
	            	 {
    					 int callType = callLogCursor.getInt(callLogCursor.getColumnIndex(Calls.TYPE));
    					 PhoneCallRecordItem record = new PhoneCallRecordItem();
    					 
    					 /** Phone Call Type: answered or missed or outgoing */
    					 record.setType(callType);
    					 /** Incoming phone call number or outgoing phone call number */
	            		 record.setPhoneNumber(callLogCursor.getString(callLogCursor.getColumnIndex(Calls.NUMBER)));
	            		 
	            		 /** Contact name of the incoming phone call or outgoing phone call*/
	            		 if(callLogCursor.getString(callLogCursor.getColumnIndex(Calls.CACHED_NAME))==null)
	            			 record.setContactName(getResources().getString(R.string.unknow_contact));
	            		 else
	            			 record.setContactName(callLogCursor.getString(callLogCursor.getColumnIndex(Calls.CACHED_NAME)));
	            		 
	            		 /** Phone Call date and time */
	            		 record.setBeginDate(callLogCursor.getLong(callLogCursor.getColumnIndex(Calls.DATE)));
	            		 time.set(callLogCursor.getLong(callLogCursor.getColumnIndex(Calls.DATE)));

	            		 record.setDate(time.monthDay + "/" + (time.month+1) + "/" + time.year);
	            		 record.setBeginTime(time.hour + ":" + time.minute + ":" + time.second);
	            		 record.setTimezone(time.timezone);
	            		 
	            		 /** Outgoing Phone call */
	            		 if(callType == Calls.OUTGOING_TYPE)
	            		 {
	            			 record.setCallDuration(callLogCursor.getLong(callLogCursor.getColumnIndex(Calls.DURATION)));
	            			 record.setPhoneType(getResources().getString(R.string.outgoing_call));
	            			 record.setEndTime(outgoingCallEndTime.hour + ":" + outgoingCallEndTime.minute + ":" +
	            					 outgoingCallEndTime.second);
	            			 
	            			 long waitTime = (outgoingCallEndTime.toMillis(true) - time.toMillis(true))/1000  - record.getCallDuration();
	            			 record.setOutgoingWaitingTime(waitTime);
	            			 
	            			 /** For PingII(outgoing) record the index of the outgoing call */
    	            		 pingIIout = userPreference.getBoolean(serviceContext.getString(R.string.Ping_II_outgoing_key), false);
    	            		 if(pingIIout == true)
    	            		 {
    	            			 outgoingPhoneCallIndex = ping2outgoingCallRecordMag.size();
    	            			 ping2outgoingCallRecordMag.add(record);
    	            		 }
	            		 }
	            		 else
	            		 {
	            			 /** Missed Phone call */
	    					 if(callType == Calls.MISSED_TYPE)
	    					 {
	    						 record.setPhoneType(getResources().getString(R.string.missed_call));
	    					 }
	    					 /** Answered Phone call */
		            		 else if(callType == Calls.INCOMING_TYPE)
		            		 {
		            			 record.setCallDuration(callLogCursor.getLong(callLogCursor.getColumnIndex(Calls.DURATION)));
		            			 record.setPhoneType(getResources().getString(R.string.incoming_call));
	    	            		 time.set(callLogCursor.getLong(callLogCursor.getColumnIndex(Calls.DATE))+
	    	            		 	callLogCursor.getLong(callLogCursor.getColumnIndex(Calls.DURATION))*1000);
	    	            		 record.setEndTime(time.hour + ":" + time.minute + ":" + time.second);
	    	            		 
	    	            		 /** For PingII(incoming) record the index of the answered call */
	    	            		 pingIIin = userPreference.getBoolean(serviceContext.getString(R.string.Ping_II_incoming_key), false);
	    	            		 if(pingIIin == true)
	    	            		 {
	    	            			 answeredPhoneCallIndex = ping2incomingCallRecordMag.size();
	    	            			 ping2incomingCallRecordMag.add(record);
	    	            		 }
	    	            		 
	    	            		 /**For PingI record the index of the answered call */
	    	            		 pingIAnsweredPhoneCallIndex = consultationRecordMag.size();
		            		 }
	    					 /** For PingI, it records both missed calls and answered calls */
	    					 pingI = userPreference.getBoolean(serviceContext.getString(R.string.Ping_I_key), false);
	    				     if(pingI == true)
	    				     {
	    				    	 consultationRecordMag.add(record);
	    				     }
	            		 }
	            		 
	            		 /** CallLog cursor move to next record */
	            		 callLogCursor.moveToNext();        
	 		           }
    	 			}
    	 			callLogCursor.close();
    	 			PhoneCallListenerService.clearPhoneCallNum();
    	 			
    	 			/** If PingI module is enable, then will create notification in notification bar
    	 			 *  and inform PhoneCallConsultationTabOne and PhoneCallConsultationTabTwo update the data list */
    	 			pingI = userPreference.getBoolean(serviceContext.getString(R.string.Ping_I_key), false);
    	 			if(pingI == true && consultationRecordMag.size() > 0)
    	 			{
    	 				//Comment out because want to pop up questionnaire right
    	 				//after user answered an incoming phone call istead of
    	 				//giving the notification in the status bar
    	 				
    	 				/** Create notification in notification bar */
        	 			/*Notification callRecords = new Notification();
    					callRecords.icon = R.drawable.notification_icon;
    					callRecords.tickerText = PhoneCallListenerService.getConsultationRecordMag().size() + " " + 
    					serviceContext.getText(R.string.notification_scroll);
    					callRecords.when = System.currentTimeMillis();
    					
    					CharSequence notificationTitle =serviceContext.getText(R.string.notification_title);
    					CharSequence notificationInfo = PhoneCallListenerService.getConsultationRecordMag().size() + " calls";
    					Intent intent =new Intent(serviceContext,PhoneCallConsultation.class);
    					
    					PendingIntent notificationIntent =PendingIntent.getActivity(serviceContext,0, intent,PendingIntent.FLAG_UPDATE_CURRENT);
    					callRecords.setLatestEventInfo(serviceContext, notificationTitle, notificationInfo, notificationIntent);
    					callRecords.defaults |= Notification.DEFAULT_SOUND;
    					callRecords.flags |= Notification.FLAG_AUTO_CANCEL;
    					notificationMag.notify(notificationID, callRecords);*/
    					
    					/** New calls come when Managing the call log list 
    					 *  Inform the BroadcastReceiver in PhoneCallConsultationTabOne
    					 *  and PhoneCallConsultationTabTwo*/
    					/*Intent updateCallIntent = new Intent();
    					updateCallIntent.setAction(UPDATE_PHONECALL_TO_ACTIVITY);
    					sendBroadcast(updateCallIntent);*/
    	 				if(pingIAnsweredPhoneCallIndex != -1)
    	 				{
    	 					if(!PhoneCallListenerService.isQuestionnaireEmpty()){
    	 					String phoneCallInfo = consultationRecordMag.get(pingIAnsweredPhoneCallIndex).getContactName() +
    	 						"\n" + consultationRecordMag.get(pingIAnsweredPhoneCallIndex).getPhoneNumber() + "\n" +
    	 						consultationRecordMag.get(pingIAnsweredPhoneCallIndex).getBeginTime() + " " +
    	 						consultationRecordMag.get(pingIAnsweredPhoneCallIndex).getDate() + ";  " +
    	 						consultationRecordMag.get(pingIAnsweredPhoneCallIndex).getCallDuration() + "s";
    	 					Intent questionnaireDialogIntent = new Intent(PhoneCallListenerService.this,PhoneCallConsultationQuestionDialog.class);
        					questionnaireDialogIntent.putExtra("callInfo", phoneCallInfo);
        					questionnaireDialogIntent.putExtra("is incoming", true);
        					questionnaireDialogIntent.putExtra("call index", pingIAnsweredPhoneCallIndex);
        					questionnaireDialogIntent.putExtra("answerQuestionnaireImmediately", true);
        					questionnaireDialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|
        	 						Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS|Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        					/** When the questionnaire questions are fully completed, 
        					 * it will return 'QUESTION_FINISHED' Tag that defined in this Acticity
        					 */
        					
        					PhoneCallListenerService.this.startActivity(questionnaireDialogIntent);
        					pingIAnsweredPhoneCallIndex = -1;
    	 					}
    	 				}
    	 			}
    	 			
    	 			/** If PingII(incoming) module is enable, then will popup dialog to interact with user */
    	 			pingIIin = userPreference.getBoolean(serviceContext.getString(R.string.Ping_II_incoming_key), false);
    	 			if(pingIIin == true && ping2incomingCallRecordMag.size() > 0)
    	 			{
    	 				if(answeredPhoneCallIndex != -1)
    	 				{
    	 					Intent ping2incomingIntent = new Intent(PhoneCallListenerService.this,PingIIincomingDialog.class);
        	 				ping2incomingIntent.putExtra("answered call index", answeredPhoneCallIndex);
        	 				ping2incomingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	 				PhoneCallListenerService.this.startActivity(ping2incomingIntent);
        	 				answeredPhoneCallIndex = -1;
    	 				}
    	 			}
    	 			
    	 			/** If PingII(outgoing) module is enable, then will popup dialog to interact with user */
    	 			pingIIout = userPreference.getBoolean(serviceContext.getString(R.string.Ping_II_outgoing_key), false);
    	 			if(pingIIout == true && ping2outgoingCallRecordMag.size() > 0)
    	 			{
    	 				if(outgoingPhoneCallIndex != -1)
    	 				{
    	 					double duration = ping2outgoingCallRecordMag.get(outgoingPhoneCallIndex).getCallDuration();
    	 					if(!PhoneCallListenerService.isQuestionnaireEmpty()){
        	 					String phoneCallInfo = ping2outgoingCallRecordMag.get(outgoingPhoneCallIndex).getContactName() +
        	 						"\n" + ping2outgoingCallRecordMag.get(outgoingPhoneCallIndex).getPhoneNumber() + "\n" +
        	 						ping2outgoingCallRecordMag.get(outgoingPhoneCallIndex).getBeginTime() + " " +
        	 						ping2outgoingCallRecordMag.get(outgoingPhoneCallIndex).getDate() + ";  " +
        	 						ping2outgoingCallRecordMag.get(outgoingPhoneCallIndex).getCallDuration() + "s";
        	 					Intent ping2outgoingIntent = new Intent(PhoneCallListenerService.this,PhoneCallConsultationQuestionDialog.class);
        	 					ping2outgoingIntent.putExtra("callInfo", phoneCallInfo);
        	 					ping2outgoingIntent.putExtra("is incoming", false);
        	 					ping2outgoingIntent.putExtra("call index", outgoingPhoneCallIndex);
        	 					ping2outgoingIntent.putExtra("answerQuestionnaireImmediately", true);
        	 					ping2outgoingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            					/** When the questionnaire questions are fully completed, 
            					 * it will return 'QUESTION_FINISHED' Tag that defined in this Acticity
            					 */
            					if(duration > 0)
            						PhoneCallListenerService.this.startActivity(ping2outgoingIntent);
    	 					}

                            outgoingPhoneCallIndex = -1;
            					
    	 					/*Intent ping2outgoingIntent = new Intent(PhoneCallListenerService.this,PingIIOutgoingDialog.class);
        	 				ping2outgoingIntent.putExtra("outgoing call index", outgoingPhoneCallIndex);
        	 				ping2outgoingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	 				PhoneCallListenerService.this.startActivity(ping2outgoingIntent);
        	 				outgoingPhoneCallIndex = -1;*/
    	 				}
    	 			}
    			}
    	}
}