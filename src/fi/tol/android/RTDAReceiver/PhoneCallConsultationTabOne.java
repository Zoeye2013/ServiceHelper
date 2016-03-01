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
 *  Activity PhoneCallConsultationTabOne
 *
 *  Activity of Tab One that presents answered phone calls list
 *  Click one item in the list will popup a questionnaire dialog for question information collecting
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

public class PhoneCallConsultationTabOne extends Activity{
	
	private TextView phoneCallTitle;
	/** List view shows answered phone calls */
	private ListView phoneCallList;
	/** List view adapter */
	private ArrayAdapter<String> phoneRecordsAdapter;
	/** ArrayList provides content for adapter */
	private ArrayList<String> contentForPresentation = new ArrayList<String>();
	
	private Context context;
	private Activity activity;
	/** Phone call records list */
	private ArrayList<PhoneCallRecordItem> answeredRecordMag;
	/** Index of the item in list that is clicked by user */
	private int selectedCallRecordIndex;
	
	/** Tag for a full completion of the questionnaire for the record item that user clicked */
	public static final int QUESTION_FINISHED = 2;
	
	/** Broadcast receiver listen to Call Log update request 
	 *  and update the list adapter*/
	private BroadcastReceiver callRecordUpdateReceiver;
	
	/** Manage notification in notification bar */
	private NotificationManager notificationMag;
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
        setContentView(R.layout.answered_calls_list);
        context = this;
        activity = this;
        
        phoneCallTitle = (TextView)findViewById(R.id.answered_call_list_info);  
        phoneCallList = (ListView)findViewById(R.id.phone_call_list);
        answeredRecordMag = new ArrayList<PhoneCallRecordItem>();
        notificationMag = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        
        /** Access to all phone call records that saved in PhoneCallListenerService and
    	 *  read those answered phone call records into the list attribute of this Acticity */
		getCallLog();
        phoneRecordsAdapter = new ArrayAdapter<String>(context, R.layout.call_item,contentForPresentation);
        
        phoneCallList.setAdapter(phoneRecordsAdapter);
        
        /** Monitoring when user click one item of the list */
        phoneCallList.setOnItemClickListener(new OnItemClickListener()
        {
			public void onItemClick(AdapterView<?> parent, View view, int position,long id) {
				
				/** When there is no questionnaire, 
				 *  clicked phone call record will be saved into file automatically
				 *  without writing any questionnaire field
				 */
				if(PhoneCallListenerService.getConsultationQuestionsList().size() <= 0)
				{
					Toast toast = Toast.makeText(context, R.string.no_questionnaire, Toast.LENGTH_LONG);
					toast.show();
					PhoneCallConsultation.writePhoneCallRecord(answeredRecordMag.get(selectedCallRecordIndex));
					/** Update phone call lists in this Activity and in PhoneCallListenerService
					 *  Remove the phone call record that already be saved into csv file
					 */
					updataLists(selectedCallRecordIndex);
				}
				
				/** When there is a questionnaire,
				 *  Popup a dialog and instruct user to complete the questionnaire questions
				 */
				else
				{
					selectedCallRecordIndex = position;
					Intent questionnaireDialogIntent = new Intent(context,PhoneCallConsultationQuestionDialog.class);
					questionnaireDialogIntent.putExtra("callInfo", contentForPresentation.get(position));
					/** When the questionnaire questions are fully completed, 
					 * it will return 'QUESTION_FINISHED' Tag that defined in this Acticity
					 */
					activity.startActivityForResult(questionnaireDialogIntent, 0);
				}
			}
        	
        });
        
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
	
	@Override
	/** When user fully complete the questions in questionnaire
	 *  it returns 'QUESTION_FINISHED' Tag, 
	 *  then in this Activity we will record corresponding record into csv file and update data lists*/
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == PhoneCallConsultationTabOne.QUESTION_FINISHED)
		{
			PhoneCallConsultation.writePhoneCallRecord(answeredRecordMag.get(selectedCallRecordIndex));
			updataLists(selectedCallRecordIndex);
		}
	}

	@Override
	public void onDestroy()
    {	
		unregisterReceiver(callRecordUpdateReceiver);
    	super.onDestroy();
    }
	
	/** Update phone call lists in this Activity and in PhoneCallListenerService
	 *  Remove the phone call record that already be saved into csv file
	 */
	public void updataLists(int index)
	{
		for(int i = 0; i < PhoneCallListenerService.getConsultationRecordMag().size(); i ++)
		{
			PhoneCallRecordItem item = PhoneCallListenerService.getConsultationRecordMag().get(i);
			if((item.getType() == answeredRecordMag.get(index).getType()) && 
					(item.getBeginDate() == answeredRecordMag.get(index).getBeginDate()) && 
					(item.getPhoneNumber().equalsIgnoreCase(answeredRecordMag.get(index).getPhoneNumber()))){
				PhoneCallListenerService.getConsultationRecordMag().remove(i);
				break;
			}
		}
		answeredRecordMag.remove(index);
		contentForPresentation.remove(index);
		
		if(answeredRecordMag.size() <= 0)
		{
			phoneCallTitle.setText(R.string.no_call_records);
		}
		phoneRecordsAdapter.notifyDataSetChanged();
		//notificationMag.cancel(PhoneCallListenerService.notificationID);
	}
	
	/** Access to all phone call records that saved in PhoneCallListenerService and
	 *  read those answered phone call records into the list attribute of this Acticity
	 */
	private void getCallLog()
    {
		answeredRecordMag.clear();
		contentForPresentation.clear();
		String presentationString ="";
		ArrayList<PhoneCallRecordItem> recordList = PhoneCallListenerService.getConsultationRecordMag();
		for(int i = 0; i < recordList.size(); i ++)
		{
			int type = recordList.get(i).getType();
			PhoneCallRecordItem record = new PhoneCallRecordItem();
			switch (type) 
            {
            case Calls.INCOMING_TYPE:
            	record.setType(recordList.get(i).getType());
            	record.setPhoneType(recordList.get(i).getPhoneType());
            	record.setPhoneNumber(recordList.get(i).getPhoneNumber());
            	record.setContactName(recordList.get(i).getContactName());
            	record.setBeginDate(recordList.get(i).getBeginDate());
            	record.setCallDuration(recordList.get(i).getCallDuration());
            	record.setBeginDate(recordList.get(i).getBeginDate());
            	record.setDate(recordList.get(i).getDate());
            	record.setBeginTime(recordList.get(i).getBeginTime());
            	record.setEndTime(recordList.get(i).getEndTime());
            	record.setTimezone(recordList.get(i).getTimezone());
            	presentationString = recordList.get(i).getContactName() + "\n" + recordList.get(i).getPhoneNumber() + "\n" +
            		recordList.get(i).getBeginTime() + " " + recordList.get(i).getDate() + ";  " + 
            		recordList.get(i).getCallDuration() + "s";
    			 contentForPresentation.add(presentationString);
    			 answeredRecordMag.add(record);
                break;
            }
		}
		if(answeredRecordMag.size() <= 0)
			phoneCallTitle.setText(R.string.no_call_records);
		else
			phoneCallTitle.setText(R.string.phone_call_list_title);
    }
}
