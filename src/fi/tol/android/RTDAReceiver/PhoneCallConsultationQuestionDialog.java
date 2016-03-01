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
 *  Activity PhoneCallConsultationQuestionDialog
 *
 *  Activity provides questionnaire dialog
 *  responsible for collection information of those questions in the questionnaire
 */

package fi.tol.android.RTDAReceiver;


import java.util.ArrayList;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PhoneCallConsultationQuestionDialog extends Activity {
	
	/** Text for showing the phone call information */
	private TextView callInfo;
	
	/** List view shows answers for the question */
	private ListView answersListView;
	
	/** Back to previous question button */
	private Button backButton;
	/** Answer later button */
	private Button answerLaterButton;
	/** Record question answer route, facilitate 'go back' action */
	private ArrayList<Integer> questionRoute;
	
	/** ArrayList provides content for adapter and its list view adapter */
	private ArrayAdapter<String> answersListAdapter;
	private ArrayList<String> answersList;
	
	/** The index of the question that is currently presented to user */
	private int questionIndex;
	
	/** Record's index in the data list of background service */
	private boolean isIncomingCall;
	private int relevantCallIndex;
	private boolean answerQuestionnaireImmediate;
	
	/** List of questions in the questionnaire */
	private ArrayList<PhoneCallConsultationQuestion> questionList;
	
	private Activity activity;
	
	/** Managing notification in notification bar */
	private NotificationManager notificationMag;
	
	/** Get accounts information from SharedPreferences */
	private SharedPreferences accountPreference;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.call_consultation_dialog);
		activity = this;
		
		/** Get currently login user name from shared preferences */
        accountPreference = this.getSharedPreferences("Account", Context.MODE_PRIVATE);
        PhoneCallConsultation.username = accountPreference.getString("last_log_in", null);
        
		notificationMag = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
		
		/** Get basic information of the phone call from intent passed from PhoneCallConsultationTabOne */
		String call = this.getIntent().getStringExtra("callInfo");
		
		/** Get relevant call index in the data list of background service */
		isIncomingCall = this.getIntent().getBooleanExtra("is incoming", true);
		relevantCallIndex = this.getIntent().getIntExtra("call index", -1);
		answerQuestionnaireImmediate = this.getIntent().getBooleanExtra("answerQuestionnaireImmediately", false);
		
		
		
		questionRoute = new ArrayList<Integer>();
		questionIndex = 0;
		
		/** Get question list from background service */
		questionList = PhoneCallListenerService.getConsultationQuestionsList();
		
		/** Initiate selected answer index of every question */
		for(int i = 0; i < questionList.size(); i ++)
		{
			questionList.get(i).initSelectedIndex();
		}
		
		/** Set Dialog title as the question title */
		this.setTitle(questionList.get(questionIndex).getQuestionTitle());
		
		answersListView = (ListView) findViewById(R.id.singular_choice_list);
		answersList = new ArrayList<String>();
		answersList = questionList.get(questionIndex).getAnswersList();
		
		answersListAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_single_choice,answersList);
		answersListView.setAdapter(answersListAdapter);
		answersListView.setItemsCanFocus(false);
		answersListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		
		/** Monitoring when user choose one answer from the list */
		listenSingleChoiceClick();
		
		callInfo = (TextView)findViewById(R.id.call_record_info);
		callInfo.setText(call);
		backButton = (Button) findViewById(R.id.button_back);
		answerLaterButton = (Button) findViewById(R.id.button_answer_later);
		if(!isIncomingCall)
			answerLaterButton.setVisibility(Button.INVISIBLE);
		else
			answerLaterButton.setVisibility(Button.VISIBLE);
		listenButtonsClick();
		
		/** If current question is the first question, then don't have back to previous question button */
		if(questionRoute.size() == 0)
		{
			backButton.setVisibility(Button.INVISIBLE);
		}
		else
		{
			backButton.setVisibility(Button.VISIBLE);
			answerLaterButton.setVisibility(Button.INVISIBLE);
		}
	}

	
	/** Monitoring when user choose one answer from the list */
	public void listenSingleChoiceClick()
	{
		answersListView.setOnItemClickListener(new OnItemClickListener()
        {
			public void onItemClick(AdapterView<?> parent, View view, int answerIndex, long id) {
				questionList.get(questionIndex).setSelectedAnswerIndex(answerIndex);
				
				/** Depend on the No. of the question that will be directed to
				 *  If value = 0 means it's the last question, won't have anymore questions
				 *  Otherwise, we will save the selected answer info and load next question for user*/
				switch(questionList.get(questionIndex).getDirectToQuestionNo(answerIndex))
				{
				case 0:
					/** After the questionnaire is fully completed, 
					 *  this dialog activity is going to be destroyed
					 *  then it will return with 'QUESTION_FINISHED' Tag to father Activity -- PhoneCallConsultationTabOne
					 */
					if(answerQuestionnaireImmediate == true)
					{
						if(isIncomingCall)
						{
							PhoneCallConsultation.writePhoneCallRecord(
									PhoneCallListenerService.getConsultationRecordMag().get(relevantCallIndex));
							PhoneCallListenerService.getConsultationRecordMag().remove(relevantCallIndex);
						}else if(!isIncomingCall)
						{
							PhoneCallConsultation.writePhoneCallRecord(
									PhoneCallListenerService.getPing2outgoingCallRecordMag().get(relevantCallIndex));
							PhoneCallListenerService.getPing2outgoingCallRecordMag().remove(relevantCallIndex);
						}
						
					}
					else if(answerQuestionnaireImmediate == false)
					{
						setResult(PhoneCallConsultationTabOne.QUESTION_FINISHED);
					}
					showToastWithImage("Puhelun kysely on tallennettu!");
					activity.finish();
					break;
				default:
					questionRoute.add(questionIndex);
					questionIndex = PhoneCallListenerService.getQuestionIndex(questionList.get(questionIndex).getDirectToQuestionNo(answerIndex));
					activity.setTitle(questionList.get(questionIndex).getQuestionTitle());
					answersList = questionList.get(questionIndex).getAnswersList();
					answersListAdapter = new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_single_choice,answersList);
					answersListView.setAdapter(answersListAdapter);
					if(questionRoute.size() == 0)
					{
						backButton.setVisibility(Button.INVISIBLE);
						answerLaterButton.setVisibility(Button.VISIBLE);
					}
					else
					{
						backButton.setVisibility(Button.VISIBLE);
						answerLaterButton.setVisibility(Button.INVISIBLE);
					}
					 break;
				}
			}
		});	
	}
	
	/** Set onClick listeners for buttons */
	public void listenButtonsClick()
	{
		/** Answer the the questionnaire of the answered incoming phone call later */
		answerLaterButton.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				/** Create notification in notification bar */
	 			Notification callRecords = new Notification();
				callRecords.icon = R.drawable.notification_icon;
				callRecords.tickerText = PhoneCallListenerService.getConsultationRecordMag().size() + " " + 
				getApplicationContext().getText(R.string.notification_scroll);
				callRecords.when = System.currentTimeMillis();
				
				CharSequence notificationTitle =getApplicationContext().getText(R.string.notification_title);
				CharSequence notificationInfo = PhoneCallListenerService.getConsultationRecordMag().size() + " calls";
				Intent intent =new Intent(getApplicationContext(),PhoneCallConsultation.class);
				
				PendingIntent notificationIntent =PendingIntent.getActivity(getApplicationContext(),0, intent,PendingIntent.FLAG_UPDATE_CURRENT);
				callRecords.setLatestEventInfo(getApplicationContext(), notificationTitle, notificationInfo, notificationIntent);
				callRecords.defaults |= Notification.DEFAULT_SOUND;
				callRecords.flags |= Notification.FLAG_AUTO_CANCEL;
				//notificationMag.notify(PhoneCallListenerService.notificationID, callRecords);
				
				/** New calls come when Managing the call log list 
				 *  Inform the BroadcastReceiver in PhoneCallConsultationTabOne
				 *  and PhoneCallConsultationTabTwo*/
				Intent updateCallIntent = new Intent();
				updateCallIntent.setAction(PhoneCallListenerService.UPDATE_PHONECALL_TO_ACTIVITY);
				sendBroadcast(updateCallIntent);
				
				/** Close the dialog after save the record */
				showToastWithImage("Vastaa kysymyksiä myöhemmin.");
  				activity.finish();
			}
			
		});
		
		/** Return to previous question with the help of questionRoute ArrayList, 
		 *  which records the route of user's answer */
		backButton.setOnClickListener(new OnClickListener (){

			public void onClick(View v) {
				questionIndex = questionRoute.get(questionRoute.size()-1);
				activity.setTitle(questionList.get(questionIndex).getQuestionTitle());
				answersList = questionList.get(questionIndex).getAnswersList();
				answersListAdapter = new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_single_choice,answersList);
				answersListView.setAdapter(answersListAdapter);
				questionRoute.remove(questionRoute.size() - 1);
				if(questionRoute.size() == 0)
				{
					backButton.setVisibility(Button.INVISIBLE);
					answerLaterButton.setVisibility(Button.VISIBLE);
				}
				else
				{
					backButton.setVisibility(Button.VISIBLE);
					answerLaterButton.setVisibility(Button.INVISIBLE);
				}
			}
		});
	}
	
	private void showToastWithImage(String message){
		Toast toast = Toast.makeText(PhoneCallConsultationQuestionDialog.this,   
                message, Toast.LENGTH_LONG);  
        ImageView imageView = new ImageView(PhoneCallConsultationQuestionDialog.this);  
        imageView.setImageResource(R.drawable.smile);  
 
        View toastView = toast.getView();  
        LinearLayout linearLayout = new LinearLayout(PhoneCallConsultationQuestionDialog.this);  
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);  
           
        linearLayout.addView(imageView);  
        linearLayout.addView(toastView);  
        
        toast.setView(linearLayout);  
        toast.show();  
	}
			
}
