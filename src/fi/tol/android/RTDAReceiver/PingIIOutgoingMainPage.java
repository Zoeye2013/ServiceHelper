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
 *  Activity PingIIOutgoingMainPage
 *
 *  Main Activity for PingII(outgoing) module
 *  Started from the grid icon on Application's MainPage
 *  List view presents uncompleted outgoing phone call records
 *  which user choose 'record later' on the popup dialog after made an outgoing call
 */

package fi.tol.android.RTDAReceiver;

import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class PingIIOutgoingMainPage extends Activity{
	
	private TextView phoneCallTitle;
	/** List view shows uncompleted phone call records */
	private ListView phoneCallList;
	/** List view adapter */
	private ArrayAdapter<String> phoneRecordsAdapter;
	/** ArrayList provides content for adapter */
	private ArrayList<String> contentForPresentation = new ArrayList<String>();
	
	private Context context;
	private Activity activity;
	/** Outgoing phone call records list */
	private ArrayList<PhoneCallRecordItem> outgoingRecordMag;
	/** Index of the item in list that is clicked by user */
	private int selectedCallRecordIndex;
	
	/** Tag for successful recording the item that user clicked */
	public static final int RECORD_COMPLETED = 2;
	
	/** An Action to update list view in PingIIOutgoingMainPage when
	 *  User choose to record an outgoing call later */
	public static final String UPDATE_UNCOMPLETED_OUTGOING_CALL_TO_ACTIVITY = "UPDATE_UNCOMPLETED_OUTGOING_CALL_TO_ACTIVITY";
	/** Broadcast receiver listen to the action and update the list adapter*/
	private BroadcastReceiver callRecordUpdateReceiver;
	
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
        setContentView(R.layout.answered_calls_list);
        context = this;
        activity = this;
        
        phoneCallTitle = (TextView)findViewById(R.id.answered_call_list_info);  
        phoneCallList = (ListView)findViewById(R.id.phone_call_list);
        
        /** Get uncompleted phone call records from background service */
        outgoingRecordMag = PhoneCallListenerService.getPing2outgoingCallRecordMag();
        setContentForPresent();
        phoneRecordsAdapter = new ArrayAdapter<String>(context, R.layout.call_item,contentForPresentation);
        phoneCallList.setAdapter(phoneRecordsAdapter);
        
        /** Monitoring when user click one item of the list */
        phoneCallList.setOnItemClickListener(new OnItemClickListener()
        {
			public void onItemClick(AdapterView<?> parent, View view, int position,long id) {
				
				/** When user click one item in the list view
				 *  Popup the PintIIPopupDialog to instruct user complete the call record */
				selectedCallRecordIndex = position;
				
				Intent pingIIOutgoingDialogIntent = new Intent(context,PingIIOutgoingDialog.class);
				pingIIOutgoingDialogIntent.putExtra("outgoing call index", selectedCallRecordIndex);
				/** Start the activity for result, when user click save button in the popup dialog
				 * 	it will return the RECORD_COMPLETED result to this main activity*/
				activity.startActivityForResult(pingIIOutgoingDialogIntent, 0);
			}
        	
        });
        
        /** Broadcast receiver listen to the action and update the list adapter*/
        callRecordUpdateReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (action.equals(UPDATE_UNCOMPLETED_OUTGOING_CALL_TO_ACTIVITY))
				{
					setContentForPresent();
					phoneRecordsAdapter.notifyDataSetChanged();
				}
			}
        };
        
        IntentFilter filter = new IntentFilter(UPDATE_UNCOMPLETED_OUTGOING_CALL_TO_ACTIVITY);
		registerReceiver(callRecordUpdateReceiver, filter);
	}
	
	@Override
	public void onDestroy()
    {	
		unregisterReceiver(callRecordUpdateReceiver);
    	super.onDestroy();
    }
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		/** When this activity's popup dialog finished, it will return a result to this activity 
		 *  When user in popup dialog save the outgoing call record, then the result is RECORD_COMPLETED */
		
		/** Update the content for present in list view */
		setContentForPresent();
		if(resultCode == PingIIOutgoingMainPage.RECORD_COMPLETED)
		{
			/** Update the content of the list view that is presented to user */
			if(outgoingRecordMag.size() <= 0)
			{
				phoneCallTitle.setText(R.string.no_call_records);
			}
			phoneRecordsAdapter.notifyDataSetChanged();
		}
	}
	
	
	/** Prepare content for presented in list view */
	private void setContentForPresent()
    {
		contentForPresentation.clear();
		String presentationString ="";
		for(int i = 0; i < outgoingRecordMag.size(); i ++)
		{
			presentationString = outgoingRecordMag.get(i).getContactName() + "\n" + 
			outgoingRecordMag.get(i).getPhoneNumber() + "\n" +
			outgoingRecordMag.get(i).getBeginTime() + " " + outgoingRecordMag.get(i).getDate() + ";  " + 
			outgoingRecordMag.get(i).getCallDuration() + "s";
			contentForPresentation.add(presentationString);
		}
		if(outgoingRecordMag.size() <= 0)
			phoneCallTitle.setText(R.string.no_call_records);
		else
			phoneCallTitle.setText(R.string.phone_call_list_title);
    }
}
