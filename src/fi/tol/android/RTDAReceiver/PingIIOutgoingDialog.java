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
 *  Activity PingIIOutgoingDialog
 *
 *  For PingII(outgoing) module
 *  This activity is a popup dialog for collecting phone call information
 *  after user made an outgoing phone call
 *  Is Flipper view, which includes two views: first view ask the type of outgoing phone call
 *  then shows the second view, and the contents of second view have slightly differences
 *  depends on the type of outgoing phone call.
 *
 *
 *  NOTE!!!!  In this version this activity is not used, according to Lääkäriliitto requirement
 *            Same questionnaire dialog as PhoneCallConsultationQuestionDialog will be used instead
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

public class PingIIOutgoingDialog extends Activity {

	private ViewFlipper viewFlipper;
	private Context context;
	private Activity activity;
	
	/** Attribute for getting currently login user name */
	private String username;
	/** Get accounts information from SharedPreferences */
	private SharedPreferences accountPreference;
	
	/** Elements from PingII(outgoing) first view */
	private TextView callBeginTimeText;
	private TextView callEndTimeText;
	private TextView waitingTimeText;
	private TextView callDurationText;
	private TextView callerNumText;
	private RadioButton reportCallRadioBtn;
	private RadioButton emergencyCallRadioBtn;
	private RadioButton continuumCallRadioBtn;
	private RadioButton otherCallRadioBtn;
	private Button recordLaterBtn;
	
	/** Elements from PingII(outgoing) second view */
	private RadioButton yesSolvedRadioBtn;
	private RadioButton notSolvedRadioBtn;
	private TextView patientText;
	private EditText patientEdit;
	private EditText callNotesEdit;
	private Button saveBtn;
	private TextView callInfoText;
	
	/** Record's index in the data list of background service */
	private int outgoingCallIndex;
	private PhoneCallRecordItem recordItem;
	
	/** Strings used when write call records into csv file */
	private static String REPORT_CALL = "REPORT_CALL";
	private static String EMERGENCY_CALL = "EMERGENCY_CALL";
	private static String CONTINUUM_CALL = "CONTINUUM_CALL";
	private static String OTHER_CALL = "OTHER_CALL";
	private String callType;
	private String isProblemSolved;
	
	
	/** File output attributes */
	private static FileOutputStream fileOut;
	private static OutputStreamWriter outWriter;
	private static BufferedWriter bufferWriter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;
		activity = this;
		setContentView(R.layout.ping2_outgoing_dialog_main);
		
		/** Get outgoing call index in the data list of background service */
		outgoingCallIndex = this.getIntent().getIntExtra("outgoing call index", -1);
		/** Get corresponding phone call info from background service */
		recordItem = PhoneCallListenerService.getPing2outgoingCallRecordMag().get(outgoingCallIndex);
		
		/** Get currently login user name from shared preferences */
        accountPreference = context.getSharedPreferences("Account", Context.MODE_PRIVATE);
        username = accountPreference.getString("last_log_in", null);
		
		/** Initiate FlipperView */
		this.setTitle(R.string.outgoing_call_info_title);
		viewFlipper = (ViewFlipper)findViewById(R.id.ping2_outgoing_dialog);
		viewFlipper.setInAnimation(AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left));
		viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(context, android.R.anim.slide_out_right));
		
		
		/** Find elements of first view */
		callBeginTimeText = (TextView) viewFlipper.getChildAt(0).findViewById(R.id.begin_time);
		callEndTimeText = (TextView) viewFlipper.getChildAt(0).findViewById(R.id.end_time);
		waitingTimeText = (TextView) viewFlipper.getChildAt(0).findViewById(R.id.waiting_time_text);
		callDurationText = (TextView) viewFlipper.getChildAt(0).findViewById(R.id.duration);
		callerNumText = (TextView) viewFlipper.getChildAt(0).findViewById(R.id.phone_number);
		reportCallRadioBtn = (RadioButton) viewFlipper.getChildAt(0).findViewById(R.id.report_call_btn);
		emergencyCallRadioBtn = (RadioButton) viewFlipper.getChildAt(0).findViewById(R.id.emergency_call_btn);
		continuumCallRadioBtn = (RadioButton) viewFlipper.getChildAt(0).findViewById(R.id.continuum_call_btn);
		otherCallRadioBtn = (RadioButton) viewFlipper.getChildAt(0).findViewById(R.id.other_call_btn);
		recordLaterBtn = (Button) viewFlipper.getChildAt(0).findViewById(R.id.record_later_btn);
		
		/** Find elements of second view */
		yesSolvedRadioBtn = (RadioButton) viewFlipper.getChildAt(1).findViewById(R.id.yes_solved_btn);
		notSolvedRadioBtn = (RadioButton) viewFlipper.getChildAt(1).findViewById(R.id.not_solved_btn);
		patientText = (TextView) viewFlipper.getChildAt(1).findViewById(R.id.patient_text);
		patientEdit = (EditText) viewFlipper.getChildAt(1).findViewById(R.id.patient_edit);
		callNotesEdit = (EditText) viewFlipper.getChildAt(1).findViewById(R.id.enter_call_note_edit);
		saveBtn = (Button) viewFlipper.getChildAt(1).findViewById(R.id.save_btn);
		callInfoText = (TextView) viewFlipper.getChildAt(1).findViewById(R.id.call_info);
		
		
		/** Click listeners for Buttons in the first and second Flipper views */
		setButtonListeners();
		
		/** Set phone call info that to be presented on pupup dialog
		 *  after user finished an outgoing phone call */
		setOutgoingCallInfo();
	}

	/** Write phone call records into csv file in phone's SDcard*/
	public void writePhoneCallRecord()
	{
		Time time = new Time();
		time.set(recordItem.getBeginDate());
		String date = time.monthDay + "_" + (time.month + 1) + "_" + time.year;
		String phoneCallRecordInfo ="";
		String folderPath = Environment.getExternalStorageDirectory() + "/" + MainLogin.appHomeFolder + "/" + 
		username + "/" + MainLogin.pingIIoutgoingFolder + "/" + MainLogin.pingIIoutgoingCallRecordsFolder + "/";
		File file = new File(folderPath + date +".csv");
		
		/** If file doesn't exist yet, new created file need to add header line */
		if(file.exists()==false)
		{
			phoneCallRecordInfo += "Begin Time,Phone Num,Call Type,End Time,Waiting Time," +
					"Duration,Problem Solved,To whom,Notes\n";
		}
		
		/** Prepare the string content for saving into csv file
		 *  Content differs depends on the type of outgoing call */
		/** In csv format, column item is sepearted by "," so here replace the ',' in notes with " " */
		String note = callNotesEdit.getText().toString();
		note.replace(',', ' ');
		phoneCallRecordInfo += recordItem.getBeginTime() + "," + recordItem.getPhoneNumber() + ",";
		
		if(callType.equals(REPORT_CALL))
		{
			phoneCallRecordInfo += REPORT_CALL + "," + recordItem.getEndTime() + "," + recordItem.getOutgoingWaitingTime() +
				"," + recordItem.getCallDuration() + "," + isProblemSolved + "," +
				patientEdit.getText().toString() + "," + note + "\n";
		}
		else if(callType.equals(EMERGENCY_CALL))
		{
			phoneCallRecordInfo += EMERGENCY_CALL + "," + recordItem.getEndTime() + "," + recordItem.getOutgoingWaitingTime() +
			"," + recordItem.getCallDuration() + "," + isProblemSolved + "," +
			patientEdit.getText().toString() + "," + note + "\n";
		}
		else if(callType.equals(CONTINUUM_CALL))
		{
			phoneCallRecordInfo += CONTINUUM_CALL + "," + recordItem.getEndTime() + "," + recordItem.getOutgoingWaitingTime() +
			"," + recordItem.getCallDuration() + "," + isProblemSolved + "," +
			patientEdit.getText().toString() + "(Patient)," + note + "\n";
		}
		else if(callType.equals(OTHER_CALL))
		{
			phoneCallRecordInfo += OTHER_CALL + "," + recordItem.getEndTime() + "," + recordItem.getOutgoingWaitingTime() +
			"," + recordItem.getCallDuration()+ "\n";
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
	
	/** Click listeners for Radio Buttons and 'Record Later' Button in the first Flipper view
	 *  and 'Save' Button in the second Flipper view */
	public void setButtonListeners()
	{
		/** Is Report Call */
		reportCallRadioBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				/** Set call type */
				callType = REPORT_CALL;
				
				patientText.setText(R.string.to_whom_text);
				
				/** Show the second Flipper view */
				viewFlipper.showNext();
			}
		});
		
		/** Is Emergency Call */
		emergencyCallRadioBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {

				/** Set call type */
				callType = EMERGENCY_CALL;
				
				patientText.setText(R.string.to_whom_text);
				
				/** Show the second Flipper view */
				viewFlipper.showNext();
			}
		});
		/** Is Check Continuum Treatment Call */
		continuumCallRadioBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {

				/** Set call type */
				callType = CONTINUUM_CALL;
				
				/** Show patient text & edit*/
				patientText.setText(R.string.patient_text);
				
				/** Show the second Flipper view */
				viewFlipper.showNext();
			}
		});
		/** Is Other types of Calls */
		otherCallRadioBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				/** Set call type */
				callType = OTHER_CALL;
				savInfoWhenFinishDialog();
			}
		});
		
		recordLaterBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				/** When user choose to record current incoming call later
				 *  send broast to PingIIMainPage to update the list view */
				Intent updateCallIntent = new Intent();
				updateCallIntent.setAction(PingIIOutgoingMainPage.UPDATE_UNCOMPLETED_OUTGOING_CALL_TO_ACTIVITY);
				sendBroadcast(updateCallIntent);
				
				/** Close the dialog after save the record */
  				activity.finish();
			}
		});
		
		/** Problem is solved */
		yesSolvedRadioBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				isProblemSolved = "Yes";
			}
		});
		/** Problem isn't solved */
		notSolvedRadioBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				isProblemSolved = "No";
			}
		});
		saveBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				savInfoWhenFinishDialog();
			}
			
		});
	}
	
	/** Set phone call info that to be presented on pupup dialog
	 *  after user finished an outgoing phone call */
	public void setOutgoingCallInfo()
	{
		/** Set call info for presenting in first Flipper view */
		callBeginTimeText.setText("Begin Time: " + recordItem.getBeginTime());
		callEndTimeText.setText("End Time: " + recordItem.getEndTime());
		waitingTimeText.setText("Waiting Time: " + recordItem.getOutgoingWaitingTime() +"s");
		callDurationText.setText("Duration: " + recordItem.getCallDuration() + "s");
		callerNumText.setText("Number: " + recordItem.getPhoneNumber() + "(" + recordItem.getContactName() + ")");
		
		/** Set call info for presenting in second Flipper view */
		callInfoText.setText(recordItem.getBeginTime() + "  " + recordItem.getPhoneNumber());
		patientEdit.setText(recordItem.getContactName());
		callNotesEdit.setLines(4);
		
		/** Set problem is solved by default */
		yesSolvedRadioBtn.setChecked(true);
		isProblemSolved = "Yes";
	}
	
	/** Save the phone call info when the dialog is supposed to be finished 
	 *  When user click 'Save' Button or when it's an 'Other' Type of phone call */
	public void savInfoWhenFinishDialog()
	{
		/** Save the phone call as relevant phone call record */
		writePhoneCallRecord();
		
		/** Remove the record call from data list in the background service */
		PhoneCallListenerService.getPing2outgoingCallRecordMag().remove(outgoingCallIndex);
		
		/** Use Toast to inform user the record is saved successfully */
		Toast toast = Toast.makeText(PingIIOutgoingDialog.this, R.string.outgoing_call_saved, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,0,0);
			toast.show();
			
			/** Set result return to PingIIOutgoingMainPage as QUESTION_FINISHED */
			setResult(PingIIOutgoingMainPage.RECORD_COMPLETED);
			/** Close the dialog after save the record */
			activity.finish();
	}
}
