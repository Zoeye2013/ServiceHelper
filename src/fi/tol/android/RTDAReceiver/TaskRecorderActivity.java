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
 *  Activity TaskRecorderActivity
 *
 *  Activity responsible for recording tasks and Accelerometer sensor data
 */

package fi.tol.android.RTDAReceiver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;

import android.text.format.Time;
import android.util.Log;


import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class TaskRecorderActivity extends Activity implements SensorEventListener{
	
	private Context context;
	
	/** Elements on main page of Task Recorder page */
	private ListView taskListView;
	private TextView taskStartedText;
	
	private ArrayList<String> tasksListForPresent;
	private TaskListAdapter taskListAdapter;
	public static ArrayList<PhoneCallConsultationQuestion> tasksList;
	
	private AlertDialog alertDialog;
	
	/** File read and write attributes */
	private FileInputStream fileIn;
	private InputStreamReader inReader;
	private BufferedReader bfReader;
	private static FileOutputStream fileOut;
	private static OutputStreamWriter outWriter;
	private static BufferedWriter bufferWriter;
	
	/** Get accounts information from SharedPreferences */
	private SharedPreferences accountPreference;
	private static String username;
	
	private String task;
	private Time beginTime;
	private Time endTime;
	private String date;
	private Boolean isSubTask;
	private int fatherTaskIndex;
	private boolean isATaskStarted = false;
	
	/** FlipperView that contains initial view and task list view, 
	 *  only show one of the view at a time
	 */
	private ViewFlipper viewFlipper;
	
	/** Attributes for Accelerometer sensor */
	private SensorManager acceleroSensor;
	/** Last time fetch the Sensor data */
	private long lastTime = -1;
	
	/** Minimum absolute movement force to consider as a peak value. */
	private static final int MIN_FORCE = 10;
	/** Minimun absolute movement drop after a peak to consider as a valley. */
	private static final int MIN_FORCE_DROP = 8; 
	private float peakPoint = 0;
	private int peakCount = 0;
	private ArrayList<Long> peakTimeList;
	private float currentTotalMovement = 0;
	private float lastX = 0;
	private float lastY = 0;
	private float lastZ = 0;
	
	/** Attributes for recording sensor data after a task is started */
	private ArrayList<SensorDataItem> sensorDataList = new ArrayList<SensorDataItem>();
	private static String UPDATE_BEFORE_TASK_FINISH_TAG = "UPDATE_BEFORE_TASK_FINISH_TAG";
	private static String UPDATE_WHEN_TASK_FINISH_TAG = "UPDATE_WHEN_TASK_FINISH_TAG";
	/** Last time write Sensor data into temporary file*/
	private long lastWriteSensorDataTime = -1;
	
	private Menu taskRecorderMenu = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.task_recorder_main);
		context = this;
		
		/** Initiate FlipperView */
		viewFlipper = (ViewFlipper)findViewById(R.id.tast_recorder_viewflipper);
		viewFlipper.setInAnimation(AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left));
		viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(context, android.R.anim.slide_out_right));
		
		taskListView = (ListView) viewFlipper.getChildAt(1).findViewById(R.id.tasks_list);
		taskStartedText = (TextView) viewFlipper.getChildAt(1).findViewById(R.id.task_started);
		tasksList = new ArrayList<PhoneCallConsultationQuestion>();
		tasksListForPresent = new ArrayList<String>();
		peakTimeList = new ArrayList<Long>();
		
		/** Initiate Accelerometer sensor and register sensor listener */
		acceleroSensor = (SensorManager) getSystemService(SENSOR_SERVICE);
		acceleroSensor.registerListener(this, acceleroSensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 
				SensorManager.SENSOR_DELAY_NORMAL);
		
		/** Get currently login user name from shared preferences */
		accountPreference = context.getSharedPreferences("Account", Context.MODE_PRIVATE);
        username = accountPreference.getString("last_log_in", null);
        
        /** Task list is saved in phone's SDcard, 
         * read task list into the APP when the acticity is started */
		readTasksList();
	}
	
	
	
	@Override
	/** Create Options Menu */
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		taskRecorderMenu = menu;
		taskRecorderMenu.add(R.string.edit_task);
		taskRecorderMenu.add(R.string.back_to_main_page);
		taskRecorderMenu.add(R.string.exit_task_recorder);
		return true;
	}
	
	/** Options Menu click listener */
	public boolean onOptionsItemSelected(MenuItem item) {
		/** User click button to back to application's main page */
		if(item.getTitle().equals(context.getString(R.string.back_to_main_page)))
		{
			Intent mainPageBackIntent = new Intent(context,MainPage.class);
			mainPageBackIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		    context.startActivity(mainPageBackIntent);
		}
		/** User click button to exit the task recorder,
		 *  means the task recorder will be stopped
		 */
		else if(item.getTitle().equals(context.getString(R.string.exit_task_recorder)))
		{
			if(isATaskStarted == false)
			{
				this.finish();
			}
			/** If there's a task is on-going, pop up Alert dialog to confrim with
			 *  whether to stop the on-going task
			 */
			else
			{
				alertDialog = new AlertDialog.Builder(TaskRecorderActivity.this)
                .setTitle(R.string.alert_have_a_task_started)
                .setPositiveButton(R.string.exit_without_saving, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	alertDialog.dismiss();
                    	TaskRecorderActivity.this.finish();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	alertDialog.dismiss();
                    }
                })
                .show();
			}
		}
		/** User click button to start the task editor to edit task list */
		else if(item.getTitle().equals(context.getString(R.string.edit_task)))
		{
			Intent taskEditorIntent = new Intent(context,TaskEditor.class);
			taskEditorIntent.putExtra("username", username);
			taskEditorIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    context.startActivity(taskEditorIntent);
		}
		return true;
	}



	@Override
	protected void onDestroy() {
		acceleroSensor.unregisterListener(this);
		super.onDestroy();
	}

	/** When a task is started, initiate relevant attributes */
	public void initTaskList()
	{
		tasksListForPresent.clear();
		for(int i = 0; i < tasksList.size(); i ++)
		{
			tasksList.get(i).initSelectedIndex();
			tasksListForPresent.add(tasksList.get(i).getQuestionTitle());
		}
		
		isSubTask = false;
		beginTime = new Time();
		beginTime.setToNow();
		lastWriteSensorDataTime = beginTime.toMillis(true);
		endTime = new Time();
		endTime.setToNow();
		taskStartedText.setText(context.getString(R.string.task_started) + 
				beginTime.hour + ":" + beginTime.minute + ":" + beginTime.second);
		ArrayList<HashMap<String, Object>> data = getData(tasksListForPresent); 
        taskListAdapter = new TaskListAdapter(context, data); 
        taskListView.setAdapter(taskListAdapter);
	}
	
	/** Customized List Adapter for task list view 
	 *  that each item includes a textview and a button */
	public class TaskListAdapter extends BaseAdapter{
		private ArrayList<HashMap<String, Object>> data; 
	    private LayoutInflater layoutInflater; 
	    private Context context;
	    public TaskListAdapter(Context context, ArrayList<HashMap<String, Object>> data) { 
	    	this.context = context; 
	    	this.data = data; 
	    	this.layoutInflater = LayoutInflater.from(context);
	    }

	    /** Get the number of tasks */
		public int getCount() {
			return data.size();
		}

		public Object getItem(int position) {
			return data.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		/** Get View of each customized list item */
		public View getView(int position, View convertView, ViewGroup parent) {
	        if (convertView == null) {
	        	
	        	/** Each row of the list contains a Text View presents the task name
	        	 *  and a button for user to click when corresponding task is finished */
	        	convertView = layoutInflater.inflate(R.layout.task_item,null);
	        	Button button = (Button)convertView.findViewById(R.id.task_item_button);
	        	button.setTag(position);
	        	TextView taskText = (TextView)convertView.findViewById(R.id.task_item_text);
	        	
	        	taskText.setText((String) data.get(position).get("task_name"));
		        button.setOnClickListener(new OnClickListener(){

					public void onClick(View v) {
						int taskIndex = Integer.valueOf(v.getTag().toString());
						/** If one task contains several sub-tasks, after click the 'Done'
						 *  button of that task, it still need user to answer 
						 *  what specific sub-task it is */
						if(isSubTask == false)
						{
							endTime.setToNow();
							switch(tasksList.get(taskIndex).getAnswerNum())
							{
							/** The task has no sub-tasks */
							case 0:
								/** Get the task name */
								task = tasksList.get(taskIndex).getQuestionTitle();
								
								/** Stop the task */
								isATaskStarted = false;
								
								/** Return to initial Flipper view */
								viewFlipper.showPrevious();
								
								isSubTask = false;
								
								/** Write task record into file */
								writeTaskRecord(task);
								
								/** Asynchronous task for sensor data file input and output */
								DataForAsyncFileIO dataForOutput = new DataForAsyncFileIO(UPDATE_WHEN_TASK_FINISH_TAG,
			    						  sensorDataList,username,task,beginTime);
								String tempFilePath = Environment.getExternalStorageDirectory() + "/" + MainLogin.appHomeFolder + "/" +
			   					username + "/" + MainLogin.tasksSubFolder + "/" + MainLogin.sensorDataFolder + "/";
								File tempFile = new File(tempFilePath + "tempSensorData.csv");
								tempFile.renameTo(new File(tempFilePath + "asyncTemp.csv"));
								new SensorDataIOTask().execute(dataForOutput);
								sensorDataList.clear();
								break;
							/** If the task has sub-tasks */
							default:
								/** Clear the list of tasks, instead presents the list of sub-tasks of this clicked task */
								tasksListForPresent.clear();
								for(int i = 0; i < tasksList.get(taskIndex).getAnswerNum(); i ++)
								{
									tasksListForPresent.add(tasksList.get(taskIndex).getAnswer(i));
								}
								ArrayList<HashMap<String, Object>> data = getData(tasksListForPresent); 
						        taskListAdapter = new TaskListAdapter(context, data); 
						        taskListView.setAdapter(taskListAdapter);
						        isSubTask = true;
						        fatherTaskIndex = taskIndex;
								break;
							} 
						}
						/** If the task is a sub-task */
						else if(isSubTask == true)
						{
							task = tasksList.get(fatherTaskIndex).getAnswer(taskIndex);
							isATaskStarted = false;
							viewFlipper.showPrevious();
							isSubTask = false;
							
							DataForAsyncFileIO dataForOutput = new DataForAsyncFileIO(UPDATE_WHEN_TASK_FINISH_TAG,
		    						  sensorDataList,username,task,beginTime);
							String tempFilePath = Environment.getExternalStorageDirectory() + "/" + MainLogin.appHomeFolder + "/" +
		   					username + "/" + MainLogin.tasksSubFolder + "/" + MainLogin.sensorDataFolder + "/";
							File tempFile = new File(tempFilePath + "tempSensorData.csv");
							tempFile.renameTo(new File(tempFilePath + "asyncTemp.csv"));
							new SensorDataIOTask().execute(dataForOutput);
							writeTaskRecord(task);
							sensorDataList.clear();
						}
					}});
	        }
	        return convertView;
		}
	}
	
	/** Task list is saved in phone's SDcard, read task list into the APP when the acticity is started */
	public void readTasksList()
    {
   	 if(MainLogin.isFileExist(MainLogin.sdCardPath + "/" + MainLogin.appHomeFolder + "/" + username
	        		+ "/" + MainLogin.tasksSubFolder + "/" + MainLogin.tasksListFile))
   	 {
   		 tasksList.clear();
   		 String lineTemp = "";
   		 String[] tempArr = {};
   		 int line = 1;
   		 try
   		 {
   			 File file = new File(Environment.getExternalStorageDirectory() + "/" + MainLogin.appHomeFolder + "/" + username
   	 	        		+ "/" + MainLogin.tasksSubFolder + "/" + MainLogin.tasksListFile);
   			 fileIn = new FileInputStream(file);
   			 inReader = new InputStreamReader(fileIn);
   			 bfReader = new BufferedReader(inReader);
   			 while(((lineTemp=bfReader.readLine()) != null)) //Read one line
   	 		 {
   				 if(line >1)
   				 {
   					 tempArr = lineTemp.split(",");
       				 PhoneCallConsultationQuestion question = new PhoneCallConsultationQuestion();
       				 question.setQuestionNo(Integer.parseInt(tempArr[0]));
       				 question.setQuestionTitle(tempArr[1]);
       				 question.setAnswerNum(Integer.parseInt(tempArr[2]));
       				 
       				 for(int i = 0; i < question.getAnswerNum(); i++)
       				 {
       					 question.addAnswersList(tempArr[i+3]);
       				 }
       				tasksList.add(question);
   				 }
   				 line ++;
           	 }
   			 bfReader.close();
   		 }catch (FileNotFoundException e1) {
   	 			e1.printStackTrace();
   	 	 } catch (IOException e) {
   	 			e.printStackTrace();
   	 	 }
   	 }
    }
	
	/** Used for customized List Adapter of task list view 
	 *  that each item includes a textview and a button */
	private ArrayList<HashMap<String, Object>> getData(ArrayList<String> contentArray) {
		ArrayList<HashMap<String, Object>> arrayList = new ArrayList<HashMap<String, Object>>();
		for (int i = 0; i <contentArray.size(); i++) { 
			HashMap<String, Object> tempHashMap = new HashMap<String, Object>();
			tempHashMap.put("task_name", contentArray.get(i)); 
			arrayList.add(tempHashMap);
		} 
		return arrayList; 
	}
	
	/** Record Task record into file */
	public void writeTaskRecord(String taskName)
	{
		String tableRecords = "";
		date = beginTime.monthDay + "-" + (beginTime.month + 1) + "-" + beginTime.year;
		File file = new File(Environment.getExternalStorageDirectory() + "/" + MainLogin.appHomeFolder + "/" + 
				username + "/" + MainLogin.tasksSubFolder + "/" + MainLogin.taskRecordsFolder +
				"/" + date +".csv");
		
		if(file.exists()==false)
		{
			tableRecords += "Task,Start Time,End Time\n";
		}
		tableRecords += taskName + "," + beginTime.hour + ":" +beginTime.minute + ":" + beginTime.second + "," +
			endTime.hour + ":" +endTime.minute + ":" + endTime.second +"\n";

		try{
			fileOut = new FileOutputStream(file, true);
			outWriter = new OutputStreamWriter(fileOut);
			bufferWriter = new BufferedWriter(outWriter);
			bufferWriter.write(tableRecords);
			bufferWriter.close();
			/*acceleroSensor.registerListener(this, acceleroSensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 
					SensorManager.SENSOR_DELAY_NORMAL);*/
		}
		catch(FileNotFoundException exception){
			exception.printStackTrace();
		}
		catch(IOException ioException)
		{
			ioException.printStackTrace();
		}
	}
	
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}
	
	/** Fetch Accelerometer sensor data */
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			/** Update when time interval bigger than 100ms */
			long currentTime = System.currentTimeMillis();
			   if ((currentTime - lastTime) > 100) 
			   { 
			      long peakTime = currentTime;
			      lastTime = currentTime;
			      float currentX = event.values[0];
			      float currentY = event.values[1]; 
			      float currentZ = event.values[2];
			      /** We analyze the absolute changes of x, y and z axis */
			      currentTotalMovement = Math.abs(currentX + currentY + currentZ - lastX - lastY - lastZ);

			      
			      /** If there is no tasks are started, we will monitor the movements of user
			       *  to detect "start task" movement */
			      if(isATaskStarted == false){
			    	  
			    	  /** If the accelerometer sensor isn't first started */
			    	  if(lastX != 0){
			    		  
			    		  /** If total absolute movement > minimum force, we are going to record peak point */
				    	  if(currentTotalMovement > MIN_FORCE){
				    		  if(currentTotalMovement > peakPoint){
				    			  peakPoint = currentTotalMovement;
				    			  peakTime = currentTime;}}
				    	  
				    	  /** If total absolute movement < minimum force, we are going to record valley point */
				    	  else if(currentTotalMovement < peakPoint && (peakPoint - currentTotalMovement) > MIN_FORCE_DROP
			    				  && (currentTime - peakTime) < 500){
				    		  peakCount ++;
			    			  peakTimeList.add(peakTime);
			    			  peakPoint = currentTotalMovement;
			    			  
			    			  /** continuous two peak points is considered as a starting of a task */
			    			  if(peakCount >= 2){
			    				  if(peakTimeList.get(1) - peakTimeList.get(0) < 2000){
			    					  isATaskStarted = true;
			    					  sensorDataList.clear();
			    					  peakCount = 0;
		    						  peakTimeList.clear();
		    						  initTaskList();
		    						  
		    						  /** When a task is started, show the Flipper view of task list */
				    				  viewFlipper.showNext();
				    				  }
			    				  else{
			    					  peakCount--;
			    					  peakTimeList.remove(0);}}}}}
			      lastX = currentX;
	    		  lastY = currentY;
	    		  lastZ = currentZ;
	    		  
	    		  /** If a task was started and ongoing, we will record the Accelerometer sensor data into files */
	    		  if(isATaskStarted == true)
				   {
	    			  SensorDataItem sensorData = new SensorDataItem();
	    			  sensorData.setX(currentX);
	    			  sensorData.setY(currentY);
	    			  sensorData.setZ(currentZ);
	    			  sensorData.setTime(currentTime);
	    			  sensorDataList.add(sensorData);
	    			  
	    			  /** Every 10 seconds will update sensor data to file */
	    			  if((currentTime - lastWriteSensorDataTime) > 10000)
	    			  {
	    				  Log.i("Before Asyn", String.valueOf(sensorDataList.size()));
	    				  DataForAsyncFileIO data = new DataForAsyncFileIO(UPDATE_BEFORE_TASK_FINISH_TAG,
	    						  sensorDataList,username,"",beginTime);
	    				  new SensorDataIOTask().execute(data);
	    				  lastWriteSensorDataTime = currentTime;
	    				  sensorDataList.clear();
	    			  }
				   }
			   }
		}		  
			  
	}
	
	/** Asynchronous task for sensor data file input and output */
	private class SensorDataIOTask extends AsyncTask<DataForAsyncFileIO, Void, Void> {
		@Override
		protected Void doInBackground(DataForAsyncFileIO... params) {
			String sensorInfo = "";
			Time time = new Time();
			date = params[0].getTime().monthDay + "-" + (params[0].getTime().month + 1) + "-" + params[0].getTime().year;
			String beginT = params[0].getTime().hour + "h" + params[0].getTime().minute + "m" + params[0].getTime().second + "s";
			String sensorDataFolderPath = Environment.getExternalStorageDirectory() + "/" + MainLogin.appHomeFolder + "/" + 
			params[0].getUser() + "/" + MainLogin.tasksSubFolder + "/" + MainLogin.sensorDataFolder + "/";
			File fileFolder = new File(sensorDataFolderPath + date);
			if(fileFolder.exists() == false)
			{
				fileFolder.mkdir();
			}
			
			File file = new File(sensorDataFolderPath + date +"/" + beginT + ".csv");
			if(params[0].getTag().equals(UPDATE_BEFORE_TASK_FINISH_TAG))
			{
				/*file = new File(Environment.getExternalStorageDirectory() + "/" + MainLogin.appHomeFolder + "/" + 
						params[0].getUser() + "/" + MainLogin.tasksSubFolder + "/" + MainLogin.sensorDataFolder +
						"/tempSensorData.csv");*/
				if(file.exists()==false)
				{
					sensorInfo += "Time,X,Y,Z\n";
				}
				for(int i = 0; i < params[0].getData().size(); i ++)
				{
					time.set(params[0].getData().get(i).getTime());
					sensorInfo += time.hour + ":" + time.minute + ":" + time.second + "," + 
					params[0].getData().get(i).getX() + "," + params[0].getData().get(i).getY() + "," + params[0].getData().get(i).getZ() + "\n";
				}
				try{
					fileOut = new FileOutputStream(file,true);
					outWriter = new OutputStreamWriter(fileOut);
					bufferWriter = new BufferedWriter(outWriter);
					bufferWriter.write(sensorInfo);
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
			else if(params[0].getTag().equals(UPDATE_WHEN_TASK_FINISH_TAG))
			{
				file.renameTo(new File(sensorDataFolderPath + date + "/" + params[0].getTask() + "_" + beginT + ".csv"));
			}
			return null;
		}
		
	}
	
	/** Private Class of Sensor data item */
	private class SensorDataItem
	{
		private float xValue;
		private float yValue;
		private float zValue;
		private long dataTime;
		
		public void setX(float x)
		{
			xValue = x;
		}
		public void setY(float y)
		{
			yValue = y;
		}
		public void setZ(float z)
		{
			zValue = z;
		}
		public void setTime(long time)
		{
			dataTime = time;
		}
		
		public float getX()
		{
			return xValue;
		}
		public float getY()
		{
			return yValue;
		}
		public float getZ()
		{
			return zValue;
		}
		public long getTime()
		{
			return dataTime;
		}
	}
	
	/** Private class of information that will be passed to asynchronous task
	 *  Pass a copy of the data and pass it to asynchronous task ensure
	 *  the correctness of the data that the asynchronous task is going to process*/
	private class DataForAsyncFileIO
	{
		private final String updateTag;
		private final ArrayList<SensorDataItem> sensorDataList;
		private final String username;
		private final String taskname;
		private final Time begintime;
		
		@SuppressWarnings("unchecked")
		public DataForAsyncFileIO(String tag,ArrayList<SensorDataItem> data,String user,String task,Time time)
		{
			updateTag = tag;
			sensorDataList = (ArrayList<SensorDataItem>) data.clone();
			username = user;
			taskname = task;
			begintime = new Time(time);
		}
		
		public String getTag()
		{
			return updateTag;
		}
		public ArrayList<SensorDataItem> getData()
		{
			return sensorDataList;
		}
		public String getUser()
		{
			return username;
		}
		public String getTask()
		{
			return taskname;
		}
		public Time getTime()
		{
			return begintime;
		}
	}
}
