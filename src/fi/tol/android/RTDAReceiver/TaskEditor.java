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
 *  ExpandableListActivity TaskEditor
 *
 *  Responsible for editing task list
 *  A two-level list includes group-level and child-level list items
 *  Also includes a header layout for the list.
 */

package fi.tol.android.RTDAReceiver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import android.app.ExpandableListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class TaskEditor extends ExpandableListActivity{
	private EditText addNewTaskEdit;
	private Button addNewTaskBtn;
	private TextView emptyTitleWarningText;
	private RelativeLayout headerLayout;
	/** Expandable List view adapter */
	private TaskExpandableListAdapter adapter;
	private String username;
	private static String NEWGROUPTASK_EDIT_TAG = "new group task";
	
	/** Listen to changes of data set and update the expandable list */
	private BroadcastReceiver expandableListUpdateReceiver;
	/** Action name */
	public static String UPDATE_TASKLIST_ADAPTER_ACTION = "UPDATE_TASKLIST_ADAPTER_ACTION";
	
	/** File output attributes */
	private FileOutputStream fileOut;
	private OutputStreamWriter outWriter;
	private BufferedWriter bfWriter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.task_editor);
		/** Inflate header view from task_ditor_footer.xml file */
		headerLayout = (RelativeLayout) LayoutInflater.from(this).inflate( 
				R.layout.task_editor_footer, null); 
		this.getExpandableListView().addHeaderView(headerLayout);
		username = this.getIntent().getStringExtra("username");
		adapter = new TaskExpandableListAdapter(this);
		setListAdapter(adapter);
		addNewTaskEdit = (EditText)headerLayout.findViewById(R.id.new_task_edittext);
		addNewTaskEdit.setTag(NEWGROUPTASK_EDIT_TAG);
		emptyTitleWarningText = (TextView)headerLayout.findViewById(R.id.empty_title_warning_text);
		addNewTaskBtn = (Button)headerLayout.findViewById(R.id.button_add);
		
		/** Add a new Group-level from the EditText in header */ 
		addNewTaskBtn.setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) {
				String newTaskTitle = addNewTaskEdit.getText().toString();
				if(newTaskTitle.length() <= 0)
				{
					emptyTitleWarningText.setTextColor(Color.RED);
					emptyTitleWarningText.setText(R.string.empty_title_warning);
				}
				else
				{
					emptyTitleWarningText.setText("");
					PhoneCallConsultationQuestion task = new PhoneCallConsultationQuestion();
	  				task.setQuestionNo(adapter.getGroupCount() + 1);
	  				task.setQuestionTitle(newTaskTitle);
	  				TaskRecorderActivity.tasksList.add(task);
	  				
	  				
	  				Toast toast = Toast.makeText(TaskEditor.this, R.string.task_added, Toast.LENGTH_SHORT);
	  				toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,0,0);
	  				toast.show();
	  				addNewTaskEdit.setText("");
	  				adapter.notifyDataSetChanged();
				}
			}
		});
		
		/** Expand the List by default */
		expandGroup();
		
		/** Group-level Click listener, will pop up group-level editing page */
		getExpandableListView().setOnGroupClickListener(new OnGroupClickListener(){

			public boolean onGroupClick(ExpandableListView parent, View v,
					int groupPosition, long id) {
				Intent groupIntent =  new Intent(TaskEditor.this, TaskEditorGroupPage.class);
				groupIntent.putExtra("group_position", groupPosition);
				groupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				TaskEditor.this.startActivity(groupIntent);
				return true;
			}
		});
		
		/** Child-level Click listener, will pop up child-level editing page */
		getExpandableListView().setOnChildClickListener(new OnChildClickListener(){

			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				Intent childIntent = new Intent(TaskEditor.this,TaskEditorChildPage.class);
				childIntent.putExtra("group_position", groupPosition);
				childIntent.putExtra("child_position", childPosition);
				childIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				TaskEditor.this.startActivity(childIntent);
				return true;
			}
		});
		
		/** BroadcastReceiver to listen to changes of data set and update the expandable list */
		expandableListUpdateReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (action.equals(UPDATE_TASKLIST_ADAPTER_ACTION))
				{
					adapter.notifyDataSetChanged();
					expandGroup();
				}
			}
        };
        IntentFilter filter = new IntentFilter(UPDATE_TASKLIST_ADAPTER_ACTION);
		registerReceiver(expandableListUpdateReceiver, filter);
	}
	
	
	
	@Override
	protected void onDestroy() {
		unregisterReceiver(expandableListUpdateReceiver);
		wirteTaskListFile();
		Toast toast = Toast.makeText(TaskEditor.this, R.string.tasklist_is_saved, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL,0,0);
		toast.show();
		super.onDestroy();
	}


	/** Customized Expandable List Adapter for task list view*/
	public class TaskExpandableListAdapter extends BaseExpandableListAdapter{
		private Context context;
		
		public TaskExpandableListAdapter(Context c)
		{
			context = c;
		}

		public Object getChild(int groupPositioin, int childPosition) {
			return TaskRecorderActivity.tasksList.get(groupPositioin).getAnswer(childPosition);
		}

		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		/** Get View of list item in child-level */
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			TextView text = null;
			if (convertView == null) {
				text = new TextView(context);
			} else {
				text = (TextView) convertView;
			}
			String name = (String) TaskRecorderActivity.tasksList.get(groupPosition).getAnswer(childPosition);
			AbsListView.LayoutParams lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 50);
			text.setLayoutParams(lp);
			text.setTextSize(16);
			text.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			text.setPadding(60, 0, 0, 0);
			text.setText(name);
			return text;
		}

		public int getChildrenCount(int groupPosition) {
			return TaskRecorderActivity.tasksList.get(groupPosition).getAnswerNum();
		}

		public Object getGroup(int groupPosition) {
			return TaskRecorderActivity.tasksList.get(groupPosition);
		}

		public int getGroupCount() {
			return TaskRecorderActivity.tasksList.size();
		}

		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		/** Get View of list item in group-level */
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			TextView text = null;
			if (convertView == null) {
				text = new TextView(context);
			} else {
				text = (TextView) convertView;
			}
			String name = (String) TaskRecorderActivity.tasksList.get(groupPosition).getQuestionTitle();
			AbsListView.LayoutParams lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 50);
			text.setLayoutParams(lp);
			text.setTextSize(18);
			text.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			text.setPadding(50, 0, 0, 0);
			text.setText(name);
			return text;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
		
	}
	
	/** Expand the child-level of the list */
	public void expandGroup()
	{
		int groupCount = adapter.getGroupCount();
		for (int i=0; i<groupCount; i++) {
			if(adapter.getChildrenCount(i) > 0)
			{
		      getExpandableListView().expandGroup(i);
			}
		}
	}
	
	/** Write the task list into csv file in phone's SDcard */
	public void wirteTaskListFile()
	{
		String tasksInfo = "Task No, Task Title, Sub tasts Number, Sub task\n";
		for(int i = 0; i <TaskRecorderActivity.tasksList.size(); i++)
	   	{
			tasksInfo += TaskRecorderActivity.tasksList.get(i).getQuestionNo() + "," +
			TaskRecorderActivity.tasksList.get(i).getQuestionTitle() + "," +
			TaskRecorderActivity.tasksList.get(i).getAnswerNum();
			
			for(int j = 0; j < TaskRecorderActivity.tasksList.get(i).getAnswerNum(); j ++)
			{
				tasksInfo += "," + TaskRecorderActivity.tasksList.get(i).getAnswer(j);
			}
			tasksInfo += "\n";
	   	 }
   	 	try
   	 	{
			 File file = new File(MainLogin.sdCardPath + "/" + MainLogin.appHomeFolder + "/" + username
	        		+ "/" + MainLogin.tasksSubFolder + "/" + MainLogin.tasksListFile);
			 
			 fileOut = new FileOutputStream(file);
			 outWriter = new OutputStreamWriter(fileOut);
			 bfWriter = new BufferedWriter(outWriter);
			 bfWriter.write(tasksInfo);
			 bfWriter.close();
		 }
		 catch (FileNotFoundException e1) {
			e1.printStackTrace();
		 } catch (IOException e) {
			e.printStackTrace();
		 }
	}
}
