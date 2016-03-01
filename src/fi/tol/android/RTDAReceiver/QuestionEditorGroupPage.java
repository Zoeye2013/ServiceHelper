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
 *  Activity QuestionEditorGroupPage
 *
 *  Group-level(Question) Editing Page
 *  For Editing questions: Edit question title, Delete the question, Add answers to the question
 */

package fi.tol.android.RTDAReceiver;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class QuestionEditorGroupPage extends Activity {
	
	/** Question index */
	private int groupPosition;
	private String groupTitle;
	
	/** Elements on question page */
	private EditText questionTitleEdit;
	private EditText answerEdit;
	private EditText directToQuestionEdit;
	private ImageButton delQuestionBtn;
	private ImageButton addAnswerBtn;
	private TextView emptyQuestionWarning;
	private TextView answerWarning;
	private TextView directToWarning;
	private Button saveBtn;
	private Button cancelBtn;
	
	/** sub-tasks list view */
	private ListView answerListView;
	/** list of answers */
	private ArrayList<String> answerList;
	/** list of corresponding answer direct to the question's No. */
	private ArrayList<Integer> directToList;
	/** answers list view adapter */
	private ArrayAdapter<String> answerAdapter;
	/** sub tasks list for list adapter */
	private ArrayList<String> answerForPresentList;
	
	private Activity activity;
	
	/** Tag used to distinguish whether this page is popup
	 *  by 'adding a new question' or 'editing existing question information' */
	private String titleTag;
	
	@SuppressWarnings("unchecked")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.question_editor_group_page);
		activity = this;
		titleTag = this.getIntent().getStringExtra("title tag");
		questionTitleEdit = (EditText)findViewById(R.id.question_title_edit);
		answerEdit = (EditText)findViewById(R.id.answer_title_edit);
		directToQuestionEdit = (EditText)findViewById(R.id.answer_to_question_no_edit);
		directToQuestionEdit.setText("0");
		delQuestionBtn = (ImageButton)findViewById(R.id.delete_question_button);
		addAnswerBtn = (ImageButton)findViewById(R.id.add_answer_button);
		saveBtn = (Button)findViewById(R.id.save_question_change_button);
		cancelBtn = (Button)findViewById(R.id.cancel_question_change_button);
		emptyQuestionWarning = (TextView)findViewById(R.id.empty_question_warning);
		answerWarning = (TextView)findViewById(R.id.answer_title_text);
		directToWarning = (TextView)findViewById(R.id.direct_to_warning);
		answerListView = (ListView)findViewById(R.id.answer_list);
		answerForPresentList = new ArrayList<String>();
		
		/** if this page is popup by 'adding a new question'
		 *  this question item doesn't exsit in the data list yet
		 *  so no group position for this item yet */
		if(titleTag.equals(QuestionEditor.ADD_NEW_TAG))
		{
			answerList = new ArrayList<String>();
			directToList = new ArrayList<Integer>();
			groupTitle = this.getIntent().getStringExtra("group title");
			groupPosition = PhoneCallListenerService.getConsultationQuestionsList().size();
			this.setTitle(R.string.question_editor_title_add_answers);
			delQuestionBtn.setVisibility(Button.INVISIBLE);
		}
		
		/** if this page is popup by 'editing existing question information'
		 *  which means user click one question in the list of QuestionEditor Activity
		 *  this question item exsit in the data list,
		 *  so the group position will be passed from QuestionEditor Activity */
		else if(titleTag.equals(QuestionEditor.EDIT_TAG))
		{
			groupPosition = this.getIntent().getIntExtra("group_position", -1);
			groupTitle = PhoneCallListenerService.getConsultationQuestionsList().get(groupPosition).getQuestionTitle();
			answerList = (ArrayList<String>) PhoneCallListenerService.getConsultationQuestionsList().get(groupPosition).getAnswersList().clone();
			directToList = (ArrayList<Integer>) PhoneCallListenerService.getConsultationQuestionsList().get(groupPosition).getDirectToList().clone();
			this.setTitle(R.string.question_editor_title);
			delQuestionBtn.setVisibility(Button.VISIBLE);
		}
		
		questionTitleEdit.setText(groupTitle);
		
		/** Provide the content to present in answers list in the question editing page */
		getAnswerForPresentList();
		
		answerAdapter = new ArrayAdapter<String>(this,R.layout.device_item,answerForPresentList);
		answerListView.setAdapter(answerAdapter);
		
		/** Add answer items */
		addAnswerBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				String newAnswerTitle = answerEdit.getText().toString();

				/** Verify that user input (both answer text & direct to question's No.) is not empty */
				if(newAnswerTitle.length() > 0 && directToQuestionEdit.getText().toString().length() > 0)
				{
					int directToQuestion = Integer.parseInt(directToQuestionEdit.getText().toString());
					answerWarning.setTextColor(Color.WHITE);
					answerWarning.setText(R.string.answer_title);
					directToWarning.setTextColor(Color.WHITE);
					directToWarning.setText(R.string.direct_to_question_no);
					
					answerList.add(newAnswerTitle);
					directToList.add(directToQuestion);
					getAnswerForPresentList();
					answerAdapter.notifyDataSetChanged();
					answerEdit.setText("");
					directToQuestionEdit.setText("0");
					Toast toast = Toast.makeText(activity, R.string.answer_added, Toast.LENGTH_SHORT);
	  				toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,0,0);
	  				toast.show();
				}
				else
				{
					if(newAnswerTitle.length() <= 0)
					{
						answerWarning.setTextColor(Color.RED);
						answerWarning.setText(R.string.empty_answer_warning);
					}
					if(directToQuestionEdit.getText().toString().length() <= 0)
					{
						directToWarning.setTextColor(Color.RED);
						directToWarning.setText(R.string.empty_direct_to_warning);
					}
				}
			}
		});
		
		/** Close the question editing page */
		cancelBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				activity.finish();
			}
			
		});
		
		/** Save the changes */
		saveBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				groupTitle = questionTitleEdit.getText().toString();
				
				/** Verify that user input(question title) is not empty */
				if(groupTitle.length() <= 0)
				{
					emptyQuestionWarning.setTextColor(Color.RED);
					emptyQuestionWarning.setText(R.string.empty_question_warning);
				}
				else
				{
					emptyQuestionWarning.setText("");
					
					/** If the tag is ADD_NEW_TAG so need one extra step to add
					 *   all the info of this question into data lists */
					if(titleTag.equals(QuestionEditor.ADD_NEW_TAG))
					{
						PhoneCallConsultationQuestion question = new PhoneCallConsultationQuestion();
		  				question.setQuestionNo(groupPosition + 1);
		  				PhoneCallListenerService.getConsultationQuestionsList().add(question);
					}
					PhoneCallListenerService.getConsultationQuestionsList().get(groupPosition).setQuestionTitle(groupTitle);
					PhoneCallListenerService.getConsultationQuestionsList().get(groupPosition).getAnswersList().clear();
					PhoneCallListenerService.getConsultationQuestionsList().get(groupPosition).getDirectToList().clear();
					PhoneCallListenerService.getConsultationQuestionsList().get(groupPosition).setAnswerNum(0);
					for(int i = 0; i < answerList.size(); i++)
					{
						PhoneCallListenerService.getConsultationQuestionsList().get(groupPosition).addAnswersList(answerList.get(i));
						PhoneCallListenerService.getConsultationQuestionsList().get(groupPosition).addDirectToQuestionList(directToList.get(i));
						PhoneCallListenerService.getConsultationQuestionsList().get(groupPosition).answerNumAdd();
					}
					
					/** Inform BroadcastReceiver in QuestionEditor Activity update the expandable list */
					Intent updateExpandableListIntent = new Intent();
					updateExpandableListIntent.setAction(QuestionEditor.UPDATE_QUESTIONLIST_ADAPTER_ACTION);
					sendBroadcast(updateExpandableListIntent);
					activity.finish();
				}
			}	
		});
		
		/** Delete the question, this will also delete all the answers of this question */
		delQuestionBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				
				/** Confirm the delete with user with AlertDialog before delete the question item */
				new AlertDialog.Builder(QuestionEditorGroupPage.this)
				.setTitle(R.string.sure_to_delete_question)
				
				/** User confirm to delete the question item */
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int which) {
				            // do something here..I end this Prograss..
				        	PhoneCallListenerService.getConsultationQuestionsList().remove(groupPosition);
				        	Toast toast = Toast.makeText(activity, R.string.question_deleted, Toast.LENGTH_SHORT);
			  				toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,0,0);
			  				toast.show();
			  				
			  				/** Inform BroadcastReceiver in QuestionEditor Activity update the expandable list */
				        	Intent updateExpandableListIntent = new Intent();
							updateExpandableListIntent.setAction(QuestionEditor.UPDATE_QUESTIONLIST_ADAPTER_ACTION);
							sendBroadcast(updateExpandableListIntent);
							activity.finish();
				    }})
				    
				/** User cancel delete the question item */
				.setNegativeButton("Cancel",
				    new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int which) {
				            dialog.cancel();
				    }}
				).show();
			}
		});
		
	}
	
	/** Provide the content to present in answers list in the question editing page */
	public ArrayList<String> getAnswerForPresentList()
	{
		answerForPresentList.clear();
		for(int i = 0; i < answerList.size(); i++)
		{
			String answerInfo = answerList.get(i);
			
			/** The content presented to user include the answer text and direct to question's No. */
			if(directToList.get(i) != 0)
			{
				answerInfo += "        -> Q:" + directToList.get(i);
			}
			answerForPresentList.add(answerInfo);
		}
		return answerForPresentList;
	}
}
