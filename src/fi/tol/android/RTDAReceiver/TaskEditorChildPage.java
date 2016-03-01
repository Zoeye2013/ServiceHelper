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
 *  Activity TaskEditorChildPage
 *
 *  Child-level Editing Page
 *  For Editing child-level tasks: Edit sub-task name, Delete the sub-task
 */

package fi.tol.android.RTDAReceiver;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class TaskEditorChildPage extends Activity {
	
	/** Elements on the sub-task page */
	private ImageButton delTaskBtn;
	private Button saveBtn;
	private Button cancelBtn;
	private TextView subtaskWarning;
	private EditText subTaskTitleEdit;
	
	/** The index of the task */
	private int groupPosition;
	/** The index of the sub-task */
	private int childPosition;
	
	private Activity activity;
	private String subTaskTitle;
	
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.task_editor_child_page);
		activity = this;
		groupPosition = this.getIntent().getIntExtra("group_position", -1);
		childPosition = this.getIntent().getIntExtra("child_position", -1);
		delTaskBtn = (ImageButton)findViewById(R.id.delete_subtask_button);
		saveBtn = (Button)findViewById(R.id.save_child_change_button);
		cancelBtn = (Button)findViewById(R.id.cancel_child_change_button);
		subTaskTitleEdit = (EditText)findViewById(R.id.subtask_title_edit);
		subtaskWarning =(TextView)findViewById(R.id.subtask_warning);
		subTaskTitle = TaskRecorderActivity.tasksList.get(groupPosition).getAnswer(childPosition);
		subTaskTitleEdit.setText(subTaskTitle);
		this.setTitle(R.string.group_page_title);
		
		/** Close the child-level editing page */
		cancelBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				activity.finish();
			}
			
		});
		
		/** Save the changes */
		saveBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				subTaskTitle = subTaskTitleEdit.getText().toString();
				if(subTaskTitle.length() <= 0)
				{
					subtaskWarning.setTextColor(Color.RED);
					subtaskWarning.setText(R.string.empty_title_warning);
				}
				else
				{
					subtaskWarning.setText("");
					TaskRecorderActivity.tasksList.get(groupPosition).setAnswer(childPosition, subTaskTitle);
					
					/** Inform BroadcastReceiver in TaskEditor Activity update the expandable list */
					Intent updateExpandableListIntent = new Intent();
					updateExpandableListIntent.setAction(TaskEditor.UPDATE_TASKLIST_ADAPTER_ACTION);
					sendBroadcast(updateExpandableListIntent);
					activity.finish();
				}
				
			}
			
		});
		
		/** Delete the sub-task */
		delTaskBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				
				/** Confirm the delete with user with AlertDialog before delete the answer item */
				new AlertDialog.Builder(TaskEditorChildPage.this)
				.setTitle(R.string.sure_to_delete)
				
				/** User confirm to delete the sub-task item */
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int which) {
				            // do something here..I end this Prograss..
				        	TaskRecorderActivity.tasksList.get(groupPosition).getAnswersList().remove(childPosition);
				        	TaskRecorderActivity.tasksList.get(groupPosition).answerNumDel();
				        	Toast toast = Toast.makeText(activity, R.string.task_deleted, Toast.LENGTH_SHORT);
			  				toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,0,0);
			  				toast.show();
				        	
			  				/** Inform BroadcastReceiver in TaskEditor Activity update the expandable list */
				        	Intent updateExpandableListIntent = new Intent();
							updateExpandableListIntent.setAction(TaskEditor.UPDATE_TASKLIST_ADAPTER_ACTION);
							sendBroadcast(updateExpandableListIntent);
							activity.finish();
				    }})
				    
				/** User cancel delete the sub-task item */
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
