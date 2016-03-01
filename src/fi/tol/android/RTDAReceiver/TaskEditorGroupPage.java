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
 *  Activity TaskEditorGroupPage
 *
 *  Group-level Editing Page
 *  For Editing group-level tasks: Edit task name, Delete the task, Add sub-tasks
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

public class TaskEditorGroupPage extends Activity {
	
	private int groupPosition;
	private String groupTitle;
	
	/** Elements on task page */
	private EditText taskTitleEdit;
	private EditText subTaskEdit;
	private ImageButton delTaskBtn;
	private ImageButton addSubTaskBtn;
	private TextView renameTaskWarning;
	private TextView subtaskWarning;
	private Button saveBtn;
	private Button cancelBtn;
	
	/** sub-tasks list view */
	private ListView subTaskListView;
	/** sub tasks list for list adapter */
	private ArrayList<String> subTaskList;
	/** sub-tasks list view adapter */
	private ArrayAdapter<String> subTaskAdapter;
	
	
	private Activity activity;
	
	@SuppressWarnings("unchecked")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.task_editor_group_page);
		activity = this;
		taskTitleEdit = (EditText)findViewById(R.id.task_title_edit);
		subTaskEdit = (EditText)findViewById(R.id.subtask_title_edit);
		delTaskBtn = (ImageButton)findViewById(R.id.delete_task_button);
		addSubTaskBtn = (ImageButton)findViewById(R.id.add_childtask_button);
		saveBtn = (Button)findViewById(R.id.save_group_change_button);
		renameTaskWarning = (TextView)findViewById(R.id.empty_rename_task_warning);
		subtaskWarning = (TextView)findViewById(R.id.empty_subtask_warning);
		cancelBtn = (Button)findViewById(R.id.cancel_group_change_button);
		subTaskListView = (ListView)findViewById(R.id.subtask_list);
		groupPosition = this.getIntent().getIntExtra("group_position", -1);
		subTaskList = (ArrayList<String>) TaskRecorderActivity.tasksList.get(groupPosition).getAnswersList().clone();
		subTaskAdapter = new ArrayAdapter<String>(this,R.layout.device_item,subTaskList);
		subTaskListView.setAdapter(subTaskAdapter);
		
		/** Add sub-tasks */
		addSubTaskBtn.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				String newSubTaskTitle = subTaskEdit.getText().toString();
				
				/** Verify that user input is not empty */
				if(newSubTaskTitle.length() <= 0)
				{
					subtaskWarning.setTextColor(Color.RED);
					subtaskWarning.setText(R.string.empty_title_warning);
				}
				else
				{
					subtaskWarning.setText("");
					subTaskList.add(newSubTaskTitle);
					subTaskAdapter.notifyDataSetChanged();
					subTaskEdit.setText("");
					Toast toast = Toast.makeText(activity, R.string.subtask_added, Toast.LENGTH_SHORT);
					toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,0,0);
					toast.show();
				}
			}
			
		});
		
		/** Close the group-level editing page */
		cancelBtn.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				activity.finish();
			}
			
		});
		
		/** Save the changes */
		saveBtn.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				groupTitle = taskTitleEdit.getText().toString();
				if(groupTitle.length() <= 0)
				{
					renameTaskWarning.setTextColor(Color.RED);
					renameTaskWarning.setText(R.string.empty_title_warning);
				}
				else
				{
					renameTaskWarning.setText("");
					TaskRecorderActivity.tasksList.get(groupPosition).setQuestionTitle(groupTitle);
					TaskRecorderActivity.tasksList.get(groupPosition).getAnswersList().clear();
					TaskRecorderActivity.tasksList.get(groupPosition).setAnswerNum(0);
					for(int i = 0; i < subTaskList.size(); i++)
					{
						TaskRecorderActivity.tasksList.get(groupPosition).addAnswersList(subTaskList.get(i));
						TaskRecorderActivity.tasksList.get(groupPosition).answerNumAdd();
					}
					Intent updateExpandableListIntent = new Intent();
					updateExpandableListIntent.setAction(TaskEditor.UPDATE_TASKLIST_ADAPTER_ACTION);
					sendBroadcast(updateExpandableListIntent);
					activity.finish();
				}
			}
			
		});
		
		/** Delete the group-level task, this will also delete all the sub-tasks under this group-level task */
		delTaskBtn.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				
				/** Confirm the delete with user with AlertDialog before delete the task item */
				new AlertDialog.Builder(TaskEditorGroupPage.this)
				.setTitle(R.string.sure_to_delete)
				
				/** User confirm to delete the task item */
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int which) {
				            // do something here..I end this Prograss..
				        	TaskRecorderActivity.tasksList.remove(groupPosition);
				        	Toast toast = Toast.makeText(activity, R.string.task_deleted, Toast.LENGTH_SHORT);
			  				toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,0,0);
			  				toast.show();
			  				
			  				/** Inform BroadcastReceiver in TaskEditor Activity update the expandable list */
				        	Intent updateExpandableListIntent = new Intent();
							updateExpandableListIntent.setAction(TaskEditor.UPDATE_TASKLIST_ADAPTER_ACTION);
							sendBroadcast(updateExpandableListIntent);
							activity.finish();
				    }})
				    
				/** User cancel delete the task item */
				.setNegativeButton("Cancel",
				    new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int which) {
				            dialog.cancel();
				    }}
				).show();
			}
			
		});		
		if(groupPosition >= 0)
		{
			groupTitle = TaskRecorderActivity.tasksList.get(groupPosition).getQuestionTitle();
			taskTitleEdit.setText(groupTitle);
			this.setTitle(R.string.group_page_title);
		}
		
	}
}
