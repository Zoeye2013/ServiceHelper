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
 *  Avtivity PhoneCallConsultationTabTwo
 *
 *  Activity of Tab Two that presents missed phone calls list
 *  Items in list is not clickable, only can clear all the missed call records
 *  and if clear button is clicked, all those missed call records will be saved to csv file
 */

package fi.tol.android.RTDAReceiver;

import java.util.ArrayList;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.CallLog.Calls;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class PhoneCallConsultationTabTwo extends Activity {
	
	private TextView tabTwoTitleInfo;
	/** List view shows missed phone calls */
	private ListView missedCallList;
	private Button callBtn;
	private Button clearBtn;
	
	/** Broadcast receiver listen to Call Log update request 
	 *  and update the list adapter*/
	private BroadcastReceiver callRecordUpdateReceiver;
	private NotificationManager notificationMag;
	
	/** SQL inquery fields*/
	String[] strFields = {
	        android.provider.CallLog.Calls.NUMBER, 
	        android.provider.CallLog.Calls.TYPE,
	        android.provider.CallLog.Calls.CACHED_NAME,
	        android.provider.CallLog.Calls.DATE
	        };
	private ArrayList<PhoneCallRecordItem> missedRecordMag;
	/** ArrayList provides content for adapter */
	private ArrayList<String> contentForPresentation;
	/** List view adapter */
	private ArrayAdapter<String> phoneRecordsAdapter;
	
	
    @Override
    /** Called when the activity is first created. */  
    public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);   
         setContentView(R.layout.missed_calls_list);
         tabTwoTitleInfo = (TextView)findViewById(R.id.missed_call_list_info);
         missedCallList = (ListView)findViewById(R.id.missed_phone_call_list);
         callBtn = (Button)findViewById(R.id.button_call);
         callBtn.setVisibility(Button.INVISIBLE);
         clearBtn = (Button)findViewById(R.id.button_clear);
         
         missedRecordMag = new ArrayList<PhoneCallRecordItem>();
         contentForPresentation = new ArrayList<String>();
         notificationMag = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
         
         /** Access to all phone call records that saved in PhoneCallListenerService and
     	 *  read those missed phone call records into the list attribute of this Acticity */
         getCallLog();
         
         phoneRecordsAdapter = new ArrayAdapter<String>(this, R.layout.call_item,contentForPresentation);
         missedCallList.setAdapter(phoneRecordsAdapter);
         
         /** When user click 'Clear' button, all the missed call records
          *  will be cleared from the display list and removed from data lists
          *  and write into csv file
          */
         clearBtn.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				int num = PhoneCallListenerService.getConsultationRecordMag().size();
				for(int i = num; i > 0; i --)
				{
					PhoneCallRecordItem item = PhoneCallListenerService.getConsultationRecordMag().get(i-1);
					if(item.getType() == Calls.MISSED_TYPE)
					{
						PhoneCallConsultation.writePhoneCallRecord(item);
						PhoneCallListenerService.getConsultationRecordMag().remove(i-1);
					}
				}
				missedRecordMag.clear();
				contentForPresentation.clear();
				if(contentForPresentation.size() <= 0)
				{
					tabTwoTitleInfo.setText(R.string.no_call_records);
				}
				phoneRecordsAdapter.notifyDataSetChanged();
				//notificationMag.cancel(PhoneCallListenerService.notificationID);
			}});
         
         /** Broadcast receiver listen to Call Log update request 
     	 *  and update the list adapter*/
         callRecordUpdateReceiver = new BroadcastReceiver(){
 			@Override
 			public void onReceive(Context context, Intent intent) {
 				String action = intent.getAction();
 				if (action.equals(PhoneCallListenerService.UPDATE_PHONECALL_TO_ACTIVITY))
 				{
 					getCallLog();
 					//notificationMag.cancel(PhoneCallListenerService.notificationID);
 					phoneRecordsAdapter.notifyDataSetChanged();
 				}
 			}
         };
         IntentFilter filter = new IntentFilter(PhoneCallListenerService.UPDATE_PHONECALL_TO_ACTIVITY);
         registerReceiver(callRecordUpdateReceiver, filter);
    }  
    
    public void onDestroy()
    {
    	unregisterReceiver(callRecordUpdateReceiver);
    	super.onDestroy();
    }
    
    /** Access to all phone call records that saved in PhoneCallListenerService and
 	 *  read those missed phone call records into the list attribute of this Acticity */
    private void getCallLog()
    {
    	missedRecordMag.clear();
		contentForPresentation.clear();
		String presentationString ="";
		ArrayList<PhoneCallRecordItem> recordList = PhoneCallListenerService.getConsultationRecordMag();
		for(int i = 0; i < recordList.size(); i ++)
		{
			int type = recordList.get(i).getType();
			PhoneCallRecordItem record = new PhoneCallRecordItem();
			switch (type) 
            {
            case Calls.MISSED_TYPE:
            	record.setType(recordList.get(i).getType());
            	record.setPhoneType(recordList.get(i).getPhoneType());
            	record.setPhoneNumber(recordList.get(i).getPhoneNumber());
            	record.setContactName(recordList.get(i).getContactName());
            	record.setBeginDate(recordList.get(i).getBeginDate());
            	record.setBeginDate(recordList.get(i).getBeginDate());
            	record.setDate(recordList.get(i).getDate());
            	record.setBeginTime(recordList.get(i).getBeginTime());
            	record.setTimezone(recordList.get(i).getTimezone());
           	 
            	presentationString = recordList.get(i).getContactName() + "\n" + recordList.get(i).getPhoneNumber() + "\n" +
            		recordList.get(i).getBeginTime() + " " + recordList.get(i).getDate();
    			 contentForPresentation.add(presentationString);
    			 missedRecordMag.add(record);
                break;
            }
		}
		if(missedRecordMag.size() <= 0)
			tabTwoTitleInfo.setText(R.string.no_call_records);
		else
			tabTwoTitleInfo.setText(R.string.phone_call_list_title);
    }
}
