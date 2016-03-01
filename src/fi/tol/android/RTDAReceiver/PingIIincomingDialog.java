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
 *  Activity PintIIincomingDialog
 *
 *  For PingII(incoming) module
 *  This activity is a popup dialog for collecting phone call information
 *  after user answered a phone call
 *  Is Flipper view, which includes three views:initial view ask question'is it relevant call'
 *  Is relevant, then show relevant_call_view
 *  otherwise, show irrelevant_call_view
 */

package fi.tol.android.RTDAReceiver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.Time;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class PingIIincomingDialog extends Activity {

	private ViewFlipper viewFlipper;
	private Context context;
	private Activity activity;
	
	/** Attribute for getting currently login user name */
	private String username;
	/** Get accounts information from SharedPreferences */
	private SharedPreferences accountPreference;
	
	/** Elements from initial question view */
	private RadioButton yesRadioBtn;
	private RadioButton noRadioBtn;
	private Button recordLaterBtn;
	private TextView callInfoTextOne;
	
	/** Elements from relevant_call view */
	private TextView callTimeText;
	private TextView callDurationText;
	private TextView callerNumText;
	private EditText notesEditOne;
	private Button saveRelevantCallBtn;
	/** Record's index in the data list of background service */
	private int relevantCallIndex;
	private PhoneCallRecordItem recordItem;
	
	/** Elements from irrelevant_call view */
	private EditText callerEdit;
	private EditText whereToCallEdit;
	private EditText notesEditTwo;
	private Button saveIrrelevantCallBtn;
	private TextView callInfoTextTwo;
	
	/** File output attributes */
	private static FileOutputStream fileOut;
	private static OutputStreamWriter outWriter;
	private static BufferedWriter bufferWriter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		context = this;
		activity = this;
		setContentView(R.layout.ping2_incoming_dialog_main);
		
		/** Get relevant call index in the data list of background service */
		relevantCallIndex = this.getIntent().getIntExtra("answered call index", -1);
		/** Get corresponding phone call info from background service */
		recordItem = PhoneCallListenerService.getPing2incomingCallRecordMag().get(relevantCallIndex);
		
		/** Get currently login user name from shared preferences */
        accountPreference = context.getSharedPreferences("Account", Context.MODE_PRIVATE);
        username = accountPreference.getString("last_log_in", null);
		
		/** Initiate FlipperView */
		this.setTitle("");
		viewFlipper = (ViewFlipper)findViewById(R.id.ping2_popup_dialog);
		viewFlipper.setInAnimation(AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left));
		viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(context, android.R.anim.slide_out_right));
		
		
		/** Find elements of init_question view */
		yesRadioBtn = (RadioButton) viewFlipper.getChildAt(0).findViewById(R.id.yes_radio_btn);
		noRadioBtn = (RadioButton) viewFlipper.getChildAt(0).findViewById(R.id.no_radio_btn);
		recordLaterBtn = (Button) viewFlipper.getChildAt(0).findViewById(R.id.record_later_btn);
		callInfoTextOne = (TextView) viewFlipper.getChildAt(0).findViewById(R.id.call_info);
		callInfoTextOne.setText("Phone Call at: " + recordItem.getBeginTime() + "\nFrom: " + 
				recordItem.getPhoneNumber() + "(" + recordItem.getContactName() + ")");
		
		/** Find elements of relevant_call view */
		callTimeText = (TextView) viewFlipper.getChildAt(1).findViewById(R.id.time);
		callDurationText = (TextView) viewFlipper.getChildAt(1).findViewById(R.id.duration);
		callerNumText = (TextView) viewFlipper.getChildAt(1).findViewById(R.id.phone_number);
		notesEditOne = (EditText) viewFlipper.getChildAt(1).findViewById(R.id.enter_note_edit);
		saveRelevantCallBtn = (Button) viewFlipper.getChildAt(1).findViewById(R.id.save_note_btn);
		
		/** Relevant call view save button click listener */
		saveRelevantCallBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				// TODO Auto-generated method stub
				
				/** Save the phone call as relevant phone call record */
				writePhoneCallRecord(true);
				/** Remove the record call from data list in the background service */
				PhoneCallListenerService.getPing2incomingCallRecordMag().remove(relevantCallIndex);
				
				/** Use Toast to inform user the record is saved successfully */
				Toast toast = Toast.makeText(PingIIincomingDialog.this, R.string.relevant_call_saved, Toast.LENGTH_SHORT);
  				toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,0,0);
  				toast.show();
  				
  				/** Set result return to PingIIMainPage as QUESTION_FINISHED */
  				setResult(PingIIincomingMainPage.RECORD_COMPLETED);
  				/** Close the dialog after save the record */
  				activity.finish();
			}
			
		});
		
		/** Find elements of irrelevant_call view */
		callerEdit = (EditText) viewFlipper.getChildAt(2).findViewById(R.id.caller_edit);
		whereToCallEdit = (EditText) viewFlipper.getChildAt(2).findViewById(R.id.where_to_call_edit);
		notesEditTwo = (EditText) viewFlipper.getChildAt(2).findViewById(R.id.note_edit);
		saveIrrelevantCallBtn = (Button) viewFlipper.getChildAt(2).findViewById(R.id.save_btn);
		callInfoTextTwo = (TextView) viewFlipper.getChildAt(2).findViewById(R.id.call_info);
		saveIrrelevantCallBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				// TODO Auto-generated method stub
				/** Save the phone call as irrelevant phone call record */
				writePhoneCallRecord(false);
				/** Remove the record call from data list in the background service */
				PhoneCallListenerService.getPing2incomingCallRecordMag().remove(relevantCallIndex);
				
				/** Use Toast to inform user the record is saved successfully */
				Toast toast = Toast.makeText(PingIIincomingDialog.this, R.string.irrelevant_call_saved, Toast.LENGTH_SHORT);
  				toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,0,0);
  				toast.show();
  				
  				/** Set result return to PingIIMainPage as QUESTION_FINISHED */
  				setResult(PingIIincomingMainPage.RECORD_COMPLETED);
  				/** Close the dialog after save the record */
  				activity.finish();
			}
			
		});
		
		/** Yes Radio button click listener */
		yesRadioBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				// TODO Auto-generated method stub
				activity.setTitle(R.string.incoming_call_info_title);
				
				/** Initiate content for relevant_call view */
				callTimeText.setText("Time: " + recordItem.getBeginTime());
				callDurationText.setText("Duration: " + recordItem.getCallDuration() + "s");
				callerNumText.setText("Number: " + recordItem.getPhoneNumber() + "(" + recordItem.getContactName() + ")");
				viewFlipper.showNext();
			}
		});
		
		/** No Radio button click listener */
		noRadioBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				// TODO Auto-generated method stub
				activity.setTitle(R.string.irrelevant_call_title);
				
				/** Initiate content for irrelevant_call view */
				callerEdit.setText(recordItem.getContactName());
				callInfoTextTwo.setText(recordItem.getBeginTime() + "  " + recordItem.getPhoneNumber());
				viewFlipper.showNext();
				viewFlipper.showNext();
			}
			
		});
		
		recordLaterBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				// TODO Auto-generated method stub
				/** When user choose to record current incoming call later
				 *  send broast to PingIIMainPage to update the list view */
				Intent updateCallIntent = new Intent();
				updateCallIntent.setAction(PingIIincomingMainPage.UPDATE_UNCOMPLETED_PHONE_CALL_TO_ACTIVITY);
				sendBroadcast(updateCallIntent);
				
				/** Close the dialog after save the record */
  				activity.finish();
			}
			
		});
	}



	/** Write phone call records into csv file in phone's SDcard*/
	public void writePhoneCallRecord(boolean isRelevant)
	{
		Time time = new Time();
		time.set(recordItem.getBeginDate());
		String date = time.monthDay + "_" + (time.month + 1) + "_" + time.year;
		String phoneCallRecordInfo ="";
		String folderPath = Environment.getExternalStorageDirectory() + "/" + MainLogin.appHomeFolder + "/" + 
		username + "/" + MainLogin.pingIIincomingFolder + "/" + MainLogin.pingIIincomingCallRecordsFolder + "/";
		File file = new File(folderPath + date +".csv");
		
		/** If file doesn't exist yet, new created file need to add header line */
		if(file.exists()==false)
		{
			phoneCallRecordInfo += "Relevant,Time,Phone Num,Duration,Who,Where should call,Notes\n";
		}
		
		/** Prepare the string content for saving into csv file
		 *  If call is relevant, some field are different from irrelevant calls */
		if(isRelevant == true)
		{
			/** In csv format, column item is sepearted by ","
			 *  So here replace the ',' in notes with " " */
			String note = notesEditOne.getText().toString();
			note.replace(",", " ");
			phoneCallRecordInfo += "Yes" + "," + recordItem.getBeginTime() + "," + recordItem.getPhoneNumber() + ","
			+ recordItem.getCallDuration() + "," + recordItem.getContactName() + "," + "," + note + "\n";
		}
		else
		{
			/** In csv format, column item is sepearted by ","
			 *  So here replace the ',' in notes with " " */
			String note = notesEditTwo.getText().toString();
			note.replace(",", " ");
			phoneCallRecordInfo += "No" + "," + recordItem.getBeginTime() + "," + recordItem.getPhoneNumber() + ","
			+ recordItem.getCallDuration() + "," + callerEdit.getText().toString() + "," + whereToCallEdit.getText().toString() + ","
				+ note + "\n";
		}
		
		/** Write String content into given name file */
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
