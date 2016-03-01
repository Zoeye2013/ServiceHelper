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
 *  Activity MeetingTimeRecorderActivity
 *
 *  Main Page for Meeting Time Recorder Module
 *  provides a timer for use to record the meeting time with patient's families.
 *
 */

package fi.tol.android.RTDAReceiver;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.format.Time;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Toast;
import android.widget.Chronometer.OnChronometerTickListener;
import android.widget.TextView;

public class MeetingTimeRecorderActivity extends Activity {
	
	/** Elements of the Meeting Timer Recorder Activity */
	private TextView dateInfoText;
	private TextView meetingStartInfo;
	private Chronometer timerChronometer;
	private Button startBtn;
	private Button pauseBtn;
	private Button resetBtn;
	
	/** Timer's count up value */
	private long countUp;
	
	/** To set the format of numbers of the Timer */
	private NumberFormat numberFormatter;
	
	/** Currently Login user's name */
	private String username;
	
	/** Whether the meeting time is stopped or paused */
	private boolean isStop;
	private boolean isPause;
    
    /** Used to get current time, and show date and time info in TextView */
    private Time currentTime;
    private Time meetingBeginTime;
    
    /** File output attributes */
	private FileOutputStream fileOut;
	private OutputStreamWriter outWriter;
	private BufferedWriter bfWriter;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.meeting_time_recorder_main);
		
		/** Get currently login user name */
		username = getIntent().getStringExtra("username");
		
		/** Get the elements of this activity */
		dateInfoText = (TextView)findViewById(R.id.date_info);
		meetingStartInfo = (TextView)findViewById(R.id.start_meeting_info);
		timerChronometer = (Chronometer)findViewById(R.id.timer); 
		startBtn = (Button)findViewById(R.id.start_btn);
		pauseBtn = (Button)findViewById(R.id.pause_btn);
		resetBtn = (Button)findViewById(R.id.reset_btn);
		
		/** Initiate the buttons' visibilities and texts */
		initButtons();
		
		/** To set the format of numbers of the Timer */
		numberFormatter = new DecimalFormat("00");
		
		/** Show current date and time */
		setDateAndTimeInfo();
		
		/** Set Chronometer Tick Listener */
		timerChronometer.setOnChronometerTickListener(new OnChronometerTickListener(){

			public void onChronometerTick(Chronometer chronometer) {
				/** Timer's count up value */
				countUp = (SystemClock.elapsedRealtime() - chronometer.getBase()) / 1000;
				
				/** The string of the value of the Timer, in 'HH:MM:SS' format */
		        String timerText = numberFormatter.format(countUp / 3600) + ":" + 
		        	numberFormatter.format((countUp % 3600)/60 ) + ":" + numberFormatter.format((countUp % 3600) % 60);
		        /** Show the value on the screen */
		        timerChronometer.setFormat(timerText);
			}
			
		});
		
		/** When user to start/stop the timer */
		startBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				/** Start button is clicked */
				if(isStop == true)
				{
					isStop = false;
					/** Set the begin time of the Timer as '00:00:00' */
					timerChronometer.setBase(SystemClock.elapsedRealtime());
					
					/** Record meeting begin time */
					currentTime.setToNow();
					meetingBeginTime = new Time();
					meetingBeginTime.setToNow();
					
					/** Change the text of start button to 'Stop' */
					startBtn.setText(R.string.stop_btn);
					
					/** Text on the screen shows user that a new meeting is begin */
					meetingStartInfo.setText(R.string.meeting_started);
					pauseBtn.setEnabled(true);
					resetBtn.setEnabled(true);
					/** Start the Timer to count up */
					timerChronometer.start();
				}
				/** Stop button is clicked */
				else if(isStop == false)
				{
					/** Initiate the buttons' visibilities and texts */
					initButtons();
					
					/** Text on the screen shows user that the meeting is finished */
					meetingStartInfo.setText(R.string.meeting_finished);
					
					/** Stop the timer and save the meeting time record into .csv file*/
					timerChronometer.stop();
					recordMeetingToFile();
				}
			}
		});
		
		/** When user to pause/resume the timer */ 
		pauseBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				/** Pause Button is clicked */
				if(isPause == false)
				{
					isPause = true;
					/** Change the text of pause button to 'Resume' */
					pauseBtn.setText(R.string.resume_btn);
					/** Text on the screen shows user that the meeting has been paused */
					meetingStartInfo.setText(R.string.meeting_paused);
					startBtn.setEnabled(false);
					resetBtn.setEnabled(false);
					/** Pause the timer */
					timerChronometer.stop();
				}
				/** Resume button is clicked */
				else if(isPause == true)
				{
					isPause = false;
					/** Change the text of resume button to 'Pause' */
					pauseBtn.setText(R.string.pause_btn);
					/** Text on the screen shows user that the meeting is continue */
					meetingStartInfo.setText(R.string.meeting_continue);
					startBtn.setEnabled(true);
					resetBtn.setEnabled(true);
					/** Resume the timer */
					timerChronometer.start();
				}
			}
			
		});
		/** When user to reset the timer to 00:00:00 */
		resetBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				timerChronometer.stop();
				initButtons();
				timerChronometer.setBase(SystemClock.elapsedRealtime());
			}
		});
	}

	/** Show current date and time */
	public void setDateAndTimeInfo()
	{
		currentTime = new Time();
		currentTime.setToNow();
		
		String date = currentTime.monthDay + "/" + (currentTime.month + 1) + "/" + currentTime.year;
		dateInfoText.setText(date);
	}
	
	/** Initiate the buttons' visibilities and texts */
	public void initButtons()
	{
		isStop = true;
		isPause = false;
		startBtn.setText(R.string.start_btn);
		meetingStartInfo.setText("");
		pauseBtn.setEnabled(false);
		pauseBtn.setText(R.string.pause_btn);
		resetBtn.setEnabled(false);
	}
	
	/** Save the meeting time record into .csv file*/
	public void recordMeetingToFile()
	{
		String meetingRecord ="";
		String duration = "";
		if((countUp / 3600) > 0)
			duration += (countUp / 3600) + "h";
		if((countUp % 3600)/60 > 0)
			duration += (countUp % 3600)/60 + "min";
		if((countUp % 3600) % 60 > 0)
			duration += (countUp % 3600) % 60 + "s";
   	 	try
   	 	{
			 File file = new File(MainLogin.sdCardPath + "/" + MainLogin.appHomeFolder + "/" + username
	        		+ "/" + MainLogin.meetingTimeSubFolder + "/" + MainLogin.meetingTimeRecordsFile);
			 if(file.exists() == false)
			 {
				 meetingRecord += "Date,Begin Time,Meeting Duration\n";
			 }
			 meetingRecord += meetingBeginTime.monthDay + "/" + (meetingBeginTime.month+1) + "/" + 
			 	meetingBeginTime.year + "," + meetingBeginTime.hour + ":" + meetingBeginTime.minute +
			 	":" + meetingBeginTime.second + "," + duration + "\n";
			 fileOut = new FileOutputStream(file,true);
			 outWriter = new OutputStreamWriter(fileOut);
			 bfWriter = new BufferedWriter(outWriter);
			 bfWriter.write(meetingRecord);
			 bfWriter.close();
			 Toast toast = Toast.makeText(MeetingTimeRecorderActivity.this, R.string.meeting_time_saved, Toast.LENGTH_SHORT);
			 toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL,0,0);
			 toast.show();
		 }
		 catch (FileNotFoundException e1) {
			e1.printStackTrace();
		 } catch (IOException e) {
			e.printStackTrace();
		 }
		 
	}
}
