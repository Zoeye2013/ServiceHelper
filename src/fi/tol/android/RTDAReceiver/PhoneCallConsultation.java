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
 *  TabActivity PhoneCallConsultation
 *
 *  Main Activity for PingI module, includes two tabs: answered calls list & missed calls list
 *  Also responsible for recording phone call records into files
 */

package fi.tol.android.RTDAReceiver;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.provider.CallLog.Calls;
import android.text.format.Time;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;

public class PhoneCallConsultation extends TabActivity  {  
	
	//protected static final String UPDATE_PHONECALL_LIST = "UPDATE_PHONECALL_LIST";
	
	/** Notification manager that responsible for notifying user
	 *  there are uncompleted incoming emergency phone calls */
	private NotificationManager notificationMag;
	
	private Context context;
	
	/** File output attributes */
	private static FileOutputStream fileOut;
	private static OutputStreamWriter outWriter;
	private static BufferedWriter bufferWriter;
	
	private static String date;
	
	/** Get accounts information from SharedPreferences */
	private SharedPreferences accountPreference;
	
	public static String username;
	private Menu PhoneCallConsultationMenu = null;
	
    public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);  
        setContentView(R.layout.call_consultation);
        this.setTitle(R.string.Ping_I_icon);
        context = this;
        
        /** Get currently login user name from shared preferences */
        accountPreference = context.getSharedPreferences("Account", Context.MODE_PRIVATE);
        username = accountPreference.getString("last_log_in", null);
        
        /** Resource object to get Drawables */
        Resources res = getResources();
        /** The activity TabHost */
        TabHost tabHost = getTabHost();
        TabHost.TabSpec tempTab;
        Intent intent;
  
        /** Intent to launch an Activity for tab one(answered calls list tab) */ 
        intent = new Intent().setClass(context, PhoneCallConsultationTabOne.class);
        intent.putExtra("username", username);
  
        /** Initialize Tab one and add it to the TabHost */
        tempTab = tabHost.newTabSpec("answered").setIndicator("Vastattu",  
                          res.getDrawable(R.drawable.tab_one))  
                      .setContent(intent);  
        tabHost.addTab(tempTab);
  
        /** Intent to launch an Activity for tab two(missed calls list tab) */  
        intent = new Intent().setClass(context, PhoneCallConsultationTabTwo.class);
        intent.putExtra("username", username);
        
        /** Initialize Tab two and add it to the TabHost */
        tempTab = tabHost.newTabSpec("missed").setIndicator("Vastaamaton",  
                          res.getDrawable(R.drawable.tab_two))  
                      .setContent(intent);  
        tabHost.addTab(tempTab);    
  
        tabHost.setCurrentTab(0);
        notificationMag = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
    }
    
    /** When user close this Tab Activity, 
     *  if there still are any uncompleted call records, 
     *  we will create a notification in the notification bar */
    public void onDestroy()
    {
		if(PhoneCallListenerService.getConsultationRecordMag().size() > 0)
		{
			/** Send notification */
			Notification callRecords = new Notification();
			callRecords.icon = R.drawable.notification_icon; 
			callRecords.tickerText = PhoneCallListenerService.getConsultationRecordMag().size() + " " + 
			context.getText(R.string.notification_scroll);
			callRecords.when = System.currentTimeMillis();
			
			CharSequence notificationTitle =context.getText(R.string.notification_title);
			CharSequence notificationInfo = PhoneCallListenerService.getConsultationRecordMag().size() + " calls";
			Intent notificationIntent =new Intent(context,PhoneCallConsultation.class);
			
			PendingIntent notificationPendingIntent =PendingIntent.getActivity(context,0, notificationIntent,PendingIntent.FLAG_UPDATE_CURRENT);
			callRecords.setLatestEventInfo(context, notificationTitle, notificationInfo, notificationPendingIntent);
			callRecords.defaults |= Notification.DEFAULT_SOUND;
			callRecords.flags |= Notification.FLAG_AUTO_CANCEL;
			//notificationMag.notify(PhoneCallListenerService.notificationID, callRecords);
			
			
			/** set shared preference for reboot */
			SharedPreferences userPreferences = context.getSharedPreferences(username, MODE_PRIVATE); 
			SharedPreferences.Editor userPreferenceEditor = userPreferences.edit();
			userPreferenceEditor.putInt("recordNumber", PhoneCallListenerService.getConsultationRecordMag().size());  
			userPreferenceEditor.commit();
		}
    	super.onDestroy();
    }
    
    
    @Override
    /** Create Options Menu */
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		PhoneCallConsultationMenu = menu;
		PhoneCallConsultationMenu.add(R.string.edit_questionnaire_menu);
		return true;
	}
    
    /** Options Menu click listener */
    public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getTitle().equals(context.getString(R.string.edit_questionnaire_menu)))
		{
			Intent editQuestionnaireIntent = new Intent(context,QuestionEditor.class);
			editQuestionnaireIntent.putExtra("username", username);
			editQuestionnaireIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    context.startActivity(editQuestionnaireIntent);
		}
		return true;
	}
    
    /** Write phone call header line into csv file in phone's SDcard*/
	public static void writeRecordHeader()
	{
		Time currentTime = new Time();
		currentTime.setToNow();
		date = currentTime.monthDay + "-" + (currentTime.month + 1) + "-" + currentTime.year;
		File file = new File(Environment.getExternalStorageDirectory() + "/" + MainLogin.appHomeFolder + "/" + 
				username + "/" + MainLogin.pingISubFolder + "/" + MainLogin.pingICallConsultationFolder +
				"/" + date +".csv");
		
		if(file.exists()==false)
		{
			ArrayList<PhoneCallConsultationQuestion> questionList = PhoneCallListenerService.getConsultationQuestionsList();
			
			/** Basic Phone call records information */
			String tableHead = "Date;Phone Call Type;Contact Name;Incoming Phone Number;Begin Time;End Time;Call Duration;Timezone";
			String lastQuestionTitle = "";
			
			/** Additional Phone call records information based on the questions in the customizable questionnaire */
			for(int i = 0; i < questionList.size(); i ++)
			{
				
				/** There are question items in the questionnaire file,
				 *  which shared same question title, but with different sets of answers
				 *  when answer the question only one of these question will popup
				 *  depends on the answer to previous answer
				 *  So when record it into files, should only have one item in the record for those questions
				 */
				if(questionList.get(i).getQuestionTitle().equals(lastQuestionTitle) == false)
				{
					tableHead += ";" + questionList.get(i).getQuestionTitle();
					lastQuestionTitle = questionList.get(i).getQuestionTitle();
				}
			}
			tableHead += "\n";
			
			try{
				fileOut = new FileOutputStream(file, true);
				outWriter = new OutputStreamWriter(fileOut);
				bufferWriter = new BufferedWriter(outWriter);
				bufferWriter.write(tableHead);
				bufferWriter.close();
				}
			catch(FileNotFoundException exception){
				exception.printStackTrace();
				} 
			catch (IOException e) {
				e.printStackTrace();
				}
		}
	}
	
	/** Write phone call records into csv file in phone's SDcard*/
	public static void writePhoneCallRecord(PhoneCallRecordItem record)
	{
		/** Write phone call header line into csv file in phone's SDcard*/
		writeRecordHeader();
		
		String phoneCallRecordInfo ="";
		ArrayList<PhoneCallConsultationQuestion> questionList = PhoneCallListenerService.getConsultationQuestionsList();

		switch(record.getType())
		{
		/** record information of an answered phone call */
		case Calls.INCOMING_TYPE:
		case Calls.OUTGOING_TYPE:
			phoneCallRecordInfo = record.getDate() + ";" + record.getPhoneType() + ";" + 
			record.getContactName() + ";" + record.getPhoneNumber() + ";" + record.getBeginTime() +
			";" + record.getEndTime() + ";" + record.getCallDuration() + ";" + record.getTimezone();
			
			for(int i = 0; i < questionList.size(); i ++)
			{
				Integer selectedAnswerIndex = questionList.get(i).getSelectedAnswerIndex();
				
				if(selectedAnswerIndex != null)
				{
					phoneCallRecordInfo += ";";
					phoneCallRecordInfo += questionList.get(i).getAnswersList().get(selectedAnswerIndex);
				}
			}
			phoneCallRecordInfo += "\n";
			break;
			
		/** record information of a missed phone call */
		case Calls.MISSED_TYPE:
			phoneCallRecordInfo = record.getDate() + ";" + record.getPhoneType() + ";" + 
			record.getContactName() + ";" + record.getPhoneNumber() + ";" + record.getBeginTime() +"\n";
			break;
		}
		
		String phoneCallFileName = MainLogin.sdCardPath + "/" + MainLogin.appHomeFolder + "/" + 
			username + "/" + MainLogin.pingISubFolder + "/" + MainLogin.pingICallConsultationFolder + "/" + date + ".csv";
		
		File file = new File(phoneCallFileName);
		try{
			fileOut = new FileOutputStream(file, true);
			outWriter = new OutputStreamWriter(fileOut);
			bufferWriter = new BufferedWriter(outWriter);
			bufferWriter.write(phoneCallRecordInfo);
			bufferWriter.close();
		}
		catch(FileNotFoundException exception){
			exception.printStackTrace();
		}
		catch(IOException ioException)
		{
			ioException.printStackTrace();
		}
	}
}
