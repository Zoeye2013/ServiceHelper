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
 *  ExpandableListActivity QuestionEditor
 *
 *  Responsible for editing task list
 *  A two-level list includes group-level(question title) and child-level(answers list) list items
 *  Also includes a header layout for the list.
 */

package fi.tol.android.RTDAReceiver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import android.app.AlertDialog;
import android.app.ExpandableListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;

public class QuestionEditor extends ExpandableListActivity {
	
	private EditText addNewQuestionEdit;
	private Button addNewQuestionBtn;
	private TextView emptyTitleWarningText;
	private RelativeLayout headerLayout;
	private String username;
	/** Expandable List view adapter */
	private QuestionExpandableListAdapter expandableListAdapter;
	/** Listen to changes of data set and update the expandable list */
	private BroadcastReceiver expandableListUpdateReceiver;
	/** Action name */
	public static String UPDATE_QUESTIONLIST_ADAPTER_ACTION = "UPDATE_QUESTIONLIST_ADAPTER_ACTION";
	
	/** Add a new question Tag, question is added from the header of the expandable list */
	public static String ADD_NEW_TAG = "ADD_NEW_TAG";
	/** Edit existing question information Tag */
	public static String EDIT_TAG ="EDIT_TAG";
	
	/** File output attributes */
	private FileOutputStream fileOut;
	private OutputStreamWriter outWriter;
	private BufferedWriter bfWriter;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.task_editor);
		
		/** Inflate header view from task_ditor_footer.xml file */
		headerLayout = (RelativeLayout) LayoutInflater.from(this).inflate( 
				R.layout.task_editor_footer, null); 
		this.getExpandableListView().addHeaderView(headerLayout);
		
		username = this.getIntent().getStringExtra("username");
		expandableListAdapter = new QuestionExpandableListAdapter(this);
		setListAdapter(expandableListAdapter);
		addNewQuestionEdit = (EditText)headerLayout.findViewById(R.id.new_task_edittext);
		addNewQuestionEdit.setText(R.string.add_question_instruction);
		emptyTitleWarningText = (TextView)headerLayout.findViewById(R.id.empty_title_warning_text);
		addNewQuestionBtn = (Button)headerLayout.findViewById(R.id.button_add);
		
		/** Add a new question from the EditText in header
		 *  This will popup group-level editing page that contains question information*/ 
		addNewQuestionBtn.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				String newQuestionTitle = addNewQuestionEdit.getText().toString();
				if(newQuestionTitle.length() <= 0)
				{
					emptyTitleWarningText.setTextColor(Color.RED);
					emptyTitleWarningText.setText(R.string.empty_question_warning);
				}
				else
				{
					emptyTitleWarningText.setText("");
					addNewQuestionEdit.setText("");
					Intent questionModifyIntent = new Intent(QuestionEditor.this,QuestionEditorGroupPage.class);
					questionModifyIntent.putExtra("title tag", ADD_NEW_TAG);
					questionModifyIntent.putExtra("group title", newQuestionTitle);
					questionModifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					QuestionEditor.this.startActivity(questionModifyIntent);
				}
				
			}
			
		});
		
		/** Expand the List by default */
		expandGroup();
		
		/** Group-level Click listener, will pop up group-level editing page */
		getExpandableListView().setOnGroupClickListener(new OnGroupClickListener(){

			public boolean onGroupClick(ExpandableListView parent, View v,
					int groupPosition, long id) {
				Intent questionModifyIntent = new Intent(QuestionEditor.this,QuestionEditorGroupPage.class);
				questionModifyIntent.putExtra("title tag", EDIT_TAG);
				questionModifyIntent.putExtra("group_position", groupPosition);
				questionModifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				QuestionEditor.this.startActivity(questionModifyIntent);
				return true;
			}
		});
		
		/** Child-level Click listener, will pop up child-level editing page */
		getExpandableListView().setOnChildClickListener(new OnChildClickListener(){
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				Intent childIntent = new Intent(QuestionEditor.this,QuestionEditorChildPage.class);
				childIntent.putExtra("group_position", groupPosition);
				childIntent.putExtra("child_position", childPosition);
				childIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				QuestionEditor.this.startActivity(childIntent);
				return true;
			}
			
		});
		
		/** BroadcastReceiver to listen to changes of data set and update the expandable list */
		expandableListUpdateReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (action.equals(UPDATE_QUESTIONLIST_ADAPTER_ACTION))
				{
					expandableListAdapter.notifyDataSetChanged();
					expandGroup();
				}
			}
        };
        IntentFilter filter = new IntentFilter(UPDATE_QUESTIONLIST_ADAPTER_ACTION);
		registerReceiver(expandableListUpdateReceiver, filter);
	}
	
	
	
	@Override
	protected void onDestroy() {
		unregisterReceiver(expandableListUpdateReceiver);
		super.onDestroy();
	}




	/** Catch the press of 'back' key in the phone */
	public void onBackPressed() {
		/** Check the reasonability of the questionnaire
		 *  Whether some answers are defined to direct to null questions */
		checkDirectToNull();
    }
	
	/** Customized Expandable List Adapter for question list view*/
	public class QuestionExpandableListAdapter extends BaseExpandableListAdapter{
		private Context context;
		
		public QuestionExpandableListAdapter(Context c)
		{
			context = c;
		}

		public Object getChild(int groupPositioin, int childPosition) {
			return PhoneCallListenerService.getConsultationQuestionsList().get(groupPositioin).getAnswer(childPosition);
		}

		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		/** Get View of every answer in child-level */
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			TextView text = null;
			if (convertView == null) {
				text = new TextView(context);
			} else {
				text = (TextView) convertView;
			}
			String answerInfo = (String) PhoneCallListenerService.getConsultationQuestionsList().get(groupPosition).getAnswer(childPosition);
			if(PhoneCallListenerService.getConsultationQuestionsList().get(groupPosition).getDirectToQuestionNo(childPosition) != 0)
			{
				answerInfo += "        -> Q:" + 
				PhoneCallListenerService.getConsultationQuestionsList().get(groupPosition).getDirectToQuestionNo(childPosition);
			}
			AbsListView.LayoutParams lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 50);
			text.setLayoutParams(lp);
			text.setTextSize(16);
			text.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			text.setPadding(60, 0, 0, 0);
			text.setText(answerInfo);
			return text;
		}

		public int getChildrenCount(int groupPosition) {
			return PhoneCallListenerService.getConsultationQuestionsList().get(groupPosition).getAnswerNum();
		}

		public Object getGroup(int groupPosition) {
			return PhoneCallListenerService.getConsultationQuestionsList().get(groupPosition);
		}

		public int getGroupCount() {
			return PhoneCallListenerService.getConsultationQuestionsList().size();
		}

		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		/** Get View of each question in group-level */
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			TextView text = null;
			if (convertView == null) {
				text = new TextView(context);
			} else {
				text = (TextView) convertView;
			}
			String questionInfo = "Q" + PhoneCallListenerService.getConsultationQuestionsList().get(groupPosition).getQuestionNo() + " " + PhoneCallListenerService.getConsultationQuestionsList().get(groupPosition).getQuestionTitle();
			AbsListView.LayoutParams lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 50);
			text.setLayoutParams(lp);
			text.setTextSize(18);
			text.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			text.setPadding(50, 0, 0, 0);
			text.setText(questionInfo);
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
		int groupCount = expandableListAdapter.getGroupCount();
		for (int i=0; i<groupCount; i++) {
			if(expandableListAdapter.getChildrenCount(i) > 0)
			{
		      getExpandableListView().expandGroup(i);
			}
		}
	}
	
	/** Check the reasonability of the questionnaire
	 *  Whether some answers are defined to direct to null questions */
	public void checkDirectToNull()
	{
		String questionNotExist = "";
		int questionListSize = PhoneCallListenerService.getConsultationQuestionsList().size();
		int answerNum = 0;
		int directToNo = 0;
		for(int i = 0; i < questionListSize; i++)
		{
			answerNum = PhoneCallListenerService.getConsultationQuestionsList().get(i).getAnswerNum();
			for(int j = 0; j < answerNum; j ++)
			{
				directToNo = PhoneCallListenerService.getConsultationQuestionsList().get(i).getDirectToQuestionNo(j);
				if( directToNo < 0 || directToNo > questionListSize)
				{
					questionNotExist += " Q." + PhoneCallListenerService.getConsultationQuestionsList().get(i).getDirectToQuestionNo(j);
				}
			}
		}
		if(questionNotExist.length() > 0)
		{
			new AlertDialog.Builder(QuestionEditor.this)
			.setTitle(getResources().getString(R.string.check_null_direct_alert) + questionNotExist)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) {
					    dialog.cancel();
			    }}).show();
		}
		else
		{
			wirteQuestionnaireFile();
			
			/** Inform Server side background service to re-read questionnaire from file */
			Intent informQuestionUpdateIntent = new Intent();
			informQuestionUpdateIntent.setAction(PhoneCallListenerService.RE_LOAD_QUESTIONNAIRE);
			sendBroadcast(informQuestionUpdateIntent);
			
			/** Inform User questionnaire is saved */
			Toast toast = Toast.makeText(QuestionEditor.this, R.string.questionnaire_is_saved, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL,0,0);
			toast.show();
			QuestionEditor.this.finish();
		}
	}
	
	/** Write the questionnaire into csv file in phone's SDcard */
	public void wirteQuestionnaireFile()
	{
		String tasksInfo = "Question No, Question Title, Number of Answers, Answer, Direct To Question No\n";
		for(int i = 0; i <PhoneCallListenerService.getConsultationQuestionsList().size(); i++)
	   	{
			tasksInfo += PhoneCallListenerService.getConsultationQuestionsList().get(i).getQuestionNo() + "," +
			PhoneCallListenerService.getConsultationQuestionsList().get(i).getQuestionTitle() + "," +
			PhoneCallListenerService.getConsultationQuestionsList().get(i).getAnswerNum();
			
			for(int j = 0; j < PhoneCallListenerService.getConsultationQuestionsList().get(i).getAnswerNum(); j ++)
			{
				tasksInfo += "," + PhoneCallListenerService.getConsultationQuestionsList().get(i).getAnswer(j) +
				"," + PhoneCallListenerService.getConsultationQuestionsList().get(i).getDirectToQuestionNo(j);
			}
			tasksInfo += "\n";
	   	 }
   	 	try
   	 	{
			 File file = new File(MainLogin.sdCardPath + "/" + MainLogin.appHomeFolder + "/" + username
	        		+ "/" + MainLogin.pingISubFolder + "/" + MainLogin.questionnaireFile);
			 
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
