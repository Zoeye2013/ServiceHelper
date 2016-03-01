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
 *  Activity QuestionEditorChildPage
 *
 *  Child-level(Answer) Editing Page
 *  For Editing answers: Edit the text of answer, Delete the answer
 */

package fi.tol.android.RTDAReceiver;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

public class QuestionEditorChildPage extends Activity {
	
	/** Elements on the answer page */
	private ImageButton delAnswerBtn;
	private Button saveBtn;
	private Button cancelBtn;
	private EditText answerTitleEdit;
	private EditText directToEdit;
	
	/** The index of the question */
	private int groupPosition;
	/** The index of the answer */
	private int childPosition;
	/** What is the answer */
	private String answerTitle;
	/** What will be next question */
	private int directToQNo;
	private Activity activity;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.question_editor_child_page);
		activity = this;
		groupPosition = this.getIntent().getIntExtra("group_position", -1);
		childPosition = this.getIntent().getIntExtra("child_position", -1);
		delAnswerBtn = (ImageButton)findViewById(R.id.delete_answer_button);
		saveBtn = (Button)findViewById(R.id.save_answer_change_button);
		cancelBtn = (Button)findViewById(R.id.cancel_answer_change_button);
		answerTitleEdit = (EditText)findViewById(R.id.answer_title_edit);
		directToEdit = (EditText)findViewById(R.id.answer_to_question_edit);
		answerTitle = PhoneCallListenerService.getConsultationQuestionsList().get(groupPosition).getAnswer(childPosition);
		directToQNo = PhoneCallListenerService.getConsultationQuestionsList().get(groupPosition).getDirectToQuestionNo(childPosition);
		answerTitleEdit.setText(answerTitle);
		directToEdit.setText(String.valueOf(directToQNo));
		this.setTitle(R.string.child_page_title);
		
		/** Close the child-level editing page */
		cancelBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				activity.finish();
			}
			
		});
		
		/** Save the changes */
		saveBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				answerTitle = answerTitleEdit.getText().toString();
				directToQNo = Integer.parseInt(directToEdit.getText().toString());
				PhoneCallListenerService.getConsultationQuestionsList().get(groupPosition).setAnswer(childPosition, answerTitle);
				PhoneCallListenerService.getConsultationQuestionsList().get(groupPosition).setDirectToQuestionList(childPosition, directToQNo);
				
				/** Inform BroadcastReceiver in QuestionEditor Activity update the expandable list */
				Intent updateQuestionListIntent = new Intent();
				updateQuestionListIntent.setAction(QuestionEditor.UPDATE_QUESTIONLIST_ADAPTER_ACTION);
				sendBroadcast(updateQuestionListIntent);
				activity.finish();
			}
			
		});
		
		/** Delete the answer */
		delAnswerBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				
				/** Confirm the delete with user with AlertDialog before delete the answer item */
				new AlertDialog.Builder(QuestionEditorChildPage.this)
				.setTitle(R.string.sure_to_delete)
				
				/** User confirm to delete the answer item */
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int which) {
				        	PhoneCallListenerService.getConsultationQuestionsList().get(groupPosition).getAnswersList().remove(childPosition);
				        	PhoneCallListenerService.getConsultationQuestionsList().get(groupPosition).getDirectToList().remove(childPosition);
				        	PhoneCallListenerService.getConsultationQuestionsList().get(groupPosition).answerNumDel();
				        	Toast toast = Toast.makeText(activity, R.string.answer_deleted, Toast.LENGTH_SHORT);
			  				toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,0,0);
			  				toast.show();
				        	
			  				/** Inform BroadcastReceiver in QuestionEditor Activity update the expandable list */
				        	Intent updateQuestionListIntent = new Intent();
							updateQuestionListIntent.setAction(QuestionEditor.UPDATE_QUESTIONLIST_ADAPTER_ACTION);
							sendBroadcast(updateQuestionListIntent);
							activity.finish();
				    }})
				    
				/** User cancel delete the answer item */
				.setNegativeButton("Cancel",
				    new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int which) {
				            dialog.cancel();
				    }}
				).show();
			}
		});
	}

}
