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
 *  Activity MainLogin
 *
 *  Login Page for Hospital Helper Application
 *  It's mandatory, after login user can customize the applications they want to use
 *  Successful login will lead to MainPage, shows icons that represent different modules
 *
 */

package fi.tol.android.RTDAReceiver;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainLogin extends Activity implements OnClickListener{
	
	private EditText userNameEdit;
	private EditText passwordEdit;
	private Button registerBtn;
	private Button loginBtn;
	private Button cancelBtn;
	
	/** Give warning if user name doesn't exist */
	private TextView warningMsgUserName;
	/** Give warning if password doesn't match user name */
	private TextView warningMsgPassword;
	
	/** Get accounts information from SharedPreferences */
	private SharedPreferences accountPreference;
	private SharedPreferences.Editor accountPreferenceEditor;
	
	
	private boolean userNameOK = false;
	private boolean passwordOK = false;
	
	private Context context;
	private Activity activity;
	
	/** Folder or file names of modules in the App */
	private String appHomePath;
	/** Phone SDcard Path */
	public static final String sdCardPath = Environment.getExternalStorageDirectory().toString();
	/** Hospital Helper Home Folder */
	public static final String appHomeFolder = "PalveluApu";
	
	/** Folders for Bluetooth Process Data Acquisition Module */
	public static final String rtdaSubFolder = "RTDA";
	public static final String rtdaAllowedFolder = "Allowed_Devices";
	public static final String rtdaSignalVectorFolder = "Signal_Vectors";
	
	/** Folders for PingI,PingII(incoming), and PingII(outgoing) modules */
	public static final String pingISubFolder = "PingI";
	public static final String pingICallConsultationFolder = "Call_Consultation";
	public static final String questionnaireFile = "questionnaire.csv";
	public static final String uncompletedCallLogFile = "uncompletedCallRecords.csv";
	
	public static final String pingIIincomingFolder = "PingII_incoming";
	public static final String pingIIincomingCallRecordsFolder = "Incoming_Call_Records";
	
	public static final String pingIIoutgoingFolder = "PingII_outgoing";
	public static final String pingIIoutgoingCallRecordsFolder = "Outgoing_Call_Records";
	
	/** Folders for Task Recorder module */
	public static final String tasksSubFolder = "Tasks";
	public static final String taskRecordsFolder = "Tasks_Records";
	public static final String sensorDataFolder = "Sensor_Data";
	public static final String tasksListFile = "tasksList.csv";
	
	/** Folder for Meeting Timer Recorder module */
	public static final String meetingTimeSubFolder = "Meeting_Time";
	public static final String meetingTimeRecordsFile = "meetingTimeRecords.csv";
	
	//public static final String rtdaLogFolder = "Logs"; //1.28

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;
		activity = this;
		
		/** Get SharedPreference for accounts information */
		accountPreference = getSharedPreferences("Account",Context.MODE_PRIVATE);
		accountPreferenceEditor = accountPreference.edit();
		
		
		/** Create Application Home folder if it doesn't exist 
		 *  If phone's SDcard is not available, the application will be ended */
		createAppHomeFolder();
		
		/** Get the last login user name */
		String lastLogInUser = accountPreference.getString("last_log_in", null);
		/** If last login user name doesn't exist, initiate the login page elements */
		if( lastLogInUser == null)
		{
			setContentView(R.layout.main_login);
			this.setTitle(R.string.login_title);
			
			userNameEdit = (EditText)findViewById(R.id.username_input);
			/** Register customized Text Change Listener */
			userNameEdit.addTextChangedListener(new TextWatcherForMultipleEdit(userNameEdit));
			
			passwordEdit = (EditText)findViewById(R.id.password_input);
			/** Register customized Text Change Listener */
			passwordEdit.addTextChangedListener(new TextWatcherForMultipleEdit(passwordEdit));
			
			registerBtn = (Button)findViewById(R.id.btn_register);
			loginBtn = (Button)findViewById(R.id.btn_login);
			loginBtn.setEnabled(false);
			cancelBtn = (Button)findViewById(R.id.btn_cancel_login);
			
			warningMsgUserName = (TextView)findViewById(R.id.login_warning_username);
			warningMsgPassword = (TextView)findViewById(R.id.login_warning_password);
			
			/** Register to Button click listener */
			registerBtn.setOnClickListener(this);
			loginBtn.setOnClickListener(this);
			cancelBtn.setOnClickListener(this);
			
		}
		/** If last login user name exists, 
		 *  will automatically login with the user name */
		else if((lastLogInUser != null) && (lastLogInUser.length() != 0))
		{
			Intent loginIntent = new Intent(context, MainPage.class);
			loginIntent.putExtra("username", lastLogInUser);
			//loginIntent.putExtra("password", passwordEdit.getText().toString());
			loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			
			context.startActivity(loginIntent);
			activity.finish();
		}
	}
	
	
	
	@Override
	/** Activity start the RegisterDialog for result, 
	 * 	when the dialog is finished, application will automatically login
	 * 	use currently registered user, so this MainLogin page will be closed */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode==1){
	        activity.finish();
	    }
	}
	
	/** Create Application Home folder if it doesn't exist */
	public void createAppHomeFolder()
	{
		/** If Phone's SDcard is available, then create Home folder for the application */
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
		{
			appHomePath = Environment.getExternalStorageDirectory() + "/" + appHomeFolder;
			if(!MainLogin.isFileExist(appHomePath))
			{
				Log.i("RTDA", "create App home folder");
				File userDir = new File(appHomePath);
				userDir.mkdir();
			}
		}
		/** If SDcard is not available, inform user 
		 * 	that data will not be saved and end the application */
		else
		{
			Toast toast = Toast.makeText(MainLogin.this, R.string.sdcard_is_not_available, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER_VERTICAL| Gravity.CENTER_HORIZONTAL,0,0);
			toast.show();
			activity.finish();
		}
	}
	
	/** Check if files or directories exist*/
    public static boolean isFileExist(String fileName)
	{
    	Log.i("RTDA", "check is file exist");
    	File logFile = new File(fileName);
		return logFile.exists();
	}
    
    /** Activity implements OnClickListener Interface,
	 *  Here listen to button click */
	public void onClick(View v) {
		/** Register Button is clicked, direct to RegisterDialog */
		if(v.equals(registerBtn))
		{
			Intent registerIntent = new Intent(context, RegisterDialog.class);
			/** Activity start the RegisterDialog for result, 
			 * 	when the dialog is finished, application will automatically login
			 * 	use currently registered user 
			 */
			activity.startActivityForResult(registerIntent, 0);
		}
		/** Cancel Button is clicked, cancel login */
		else if(v.equals(cancelBtn))
		{
			activity.finish();
		}
		/** Login Button is enabled when user enter correct user name and password
		 *  successful login direct to MainPage
		 */
		else if(v.equals(loginBtn))
		{
			accountPreferenceEditor.putString("last_log_in", userNameEdit.getText().toString());
			accountPreferenceEditor.commit();
			Intent loginIntent = new Intent(context, MainPage.class);
			loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			activity.startActivity(loginIntent);
			activity.finish();
		}
	}


	/** Inner Class to create customized rules for checking user's EditText input */
	class TextWatcherForMultipleEdit implements TextWatcher{
		
		private EditText editText;
		
		public TextWatcherForMultipleEdit(EditText edit)
		{
			editText = edit;
		}

		/** Called after the text inside a EditText is changed */
		public void afterTextChanged(Editable s) {
			switch(editText.getId())
			{
			/** Check if user name exist */
			case R.id.username_input:
				String tempUser = s.toString();
				if(s.length() > 0 && accountPreference != null && accountPreference.getString(tempUser, null) != null)
				{
					warningMsgUserName.setText("");
					userNameOK = true;
					if(userNameOK && passwordOK)
						loginBtn.setEnabled(true);
				}
				else{
					warningMsgUserName.setTextColor(Color.RED);
					warningMsgUserName.setText(R.string.user_not_exist);
					userNameOK = false;
					loginBtn.setEnabled(false);
				}
				break;
			/** Check if password matches the user name */
			case R.id.password_input:
				if(s.length() > 0 && s.toString().equals(accountPreference.getString(userNameEdit.getText().toString(), null))){
					warningMsgPassword.setText("");
					passwordOK = true;
					if(userNameOK && passwordOK)
						loginBtn.setEnabled(true);
						
					}
				else{
					warningMsgPassword.setTextColor(Color.RED);
					warningMsgPassword.setText(R.string.password_wrong);
					passwordOK = false;
					loginBtn.setEnabled(false);
				}
				break;
			}
		}

		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}


		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
		}
	}
}
