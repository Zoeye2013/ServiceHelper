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
 *  Activity RegisterDialog
 *
 *  For register New User
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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class RegisterDialog extends Activity{
	
	/** Elements on register page */
	private EditText userNameEdit;
	private TextView warningMsgName;
	private EditText passwordEdit;
	private TextView warningMsgPassword;
	private EditText repasswordEdit;
	private TextView warningMsgRepassword;
	private Button registerAndLoginBtn;
	
	private String userName;
	private String password;
	private boolean userNameOK = false;
	private boolean passwordOK = false;
	
	/** Get accounts information from SharedPreferences */
	private SharedPreferences accountPreference;
	private SharedPreferences.Editor accountPreferenceEditor;
	
	private Context context;
	private Activity activity;
	private String userFolderPath;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.register_new_user);
		
		this.setTitle(R.string.register_title);
		context = this;
		activity = this;
		
		/** Initiate elements on register page */
		userNameEdit = (EditText)findViewById(R.id.register_user_name);
		/** Register customized Text Change Listener */
		userNameEdit.addTextChangedListener(new TextWatcherForMultipleEdit(userNameEdit));
		warningMsgName = (TextView)findViewById(R.id.warning_username);
		
		passwordEdit = (EditText)findViewById(R.id.register_password);
		/** Register customized Text Change Listener */
		passwordEdit.addTextChangedListener(new TextWatcherForMultipleEdit(passwordEdit));
		warningMsgPassword = (TextView)findViewById(R.id.warning_password);
		
		repasswordEdit = (EditText)findViewById(R.id.register_re_password);
		/** Register customized Text Change Listener */
		repasswordEdit.addTextChangedListener(new TextWatcherForMultipleEdit(repasswordEdit));
		warningMsgRepassword = (TextView)findViewById(R.id.warning_re_password);
		
		registerAndLoginBtn = (Button)findViewById(R.id.btn_register_and_login);
		registerAndLoginBtn.setEnabled(false);
		
		/** Get accounts information from SharedPreferences */
		accountPreference = getSharedPreferences("Account",Context.MODE_PRIVATE);
		accountPreferenceEditor = accountPreference.edit();
		
		/** Listen to 'Register' Button Click */
		registerAndLoginBtn.setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) {
				if(userNameOK && passwordOK){
					
					userName = userNameEdit.getText().toString();
					password = repasswordEdit.getText().toString();
					
					/** Save new user information into account SharedPreference */
					accountPreferenceEditor.putString(userName, password);
					accountPreferenceEditor.commit();
					
					/** Create new user folder under the Application's Home folder for new user */
					if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
					{
						userFolderPath = Environment.getExternalStorageDirectory() + "/" + MainLogin.appHomeFolder + "/" + userName;
						if(!MainLogin.isFileExist(userFolderPath))
						{
							File homeDir = new File(userFolderPath);
							homeDir.mkdir();
						}
					}
					/** Successful registration will lead to automatic login,
					 *  So will save current user as 'last_log_in' user
					 */
					accountPreferenceEditor.putString("last_log_in", userName);
					accountPreferenceEditor.commit();
					
					/** Login automatically, direct to MainPage */
					Intent loginIntent = new Intent(context, MainPage.class);
					loginIntent.putExtra("username", userNameEdit.getText().toString());
					loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					/** Return successful register result back to MainLogin Activity */
					setResult(1);
					context.startActivity(loginIntent);
					activity.finish();
				}
			}
		});
	}

	/** Inner Class to create customized rules for checking user's EditText input */
	private class TextWatcherForMultipleEdit implements TextWatcher{
		
		private EditText editText;
		
		public TextWatcherForMultipleEdit(EditText edit)
		{
			editText = edit;
		}
		
		/** Called after the text inside a EditText is changed */
		public void afterTextChanged(Editable s) {
			switch(editText.getId())
			{
			/** Check user name (cannot be null, cannot be existing user names) */
			case R.id.register_user_name:
				
				/** User name cannot be null */
				if(s.length() > 0){
					String tempUser = s.toString();
					
					/** Check shared preference whether the user name exist */
					String tempPwd = accountPreference.getString(tempUser, null);
					
					/** If user name doesn't exist */
					if(tempPwd == null){
						/** Inform user that the user name is OK */
						warningMsgName.setTextColor(Color.GREEN);
						warningMsgName.setText(R.string.username_ok);
						userNameOK = true;
						if(userNameOK && passwordOK)
							registerAndLoginBtn.setEnabled(true);
					}
					
					/** If user name already exist */
					else if(tempPwd != null){
						warningMsgName.setTextColor(Color.RED);
						warningMsgName.setText(R.string.username_exist);
						userNameOK = false;
						registerAndLoginBtn.setEnabled(false);
					}
				}
				else{
					warningMsgName.setTextColor(Color.RED);
					warningMsgName.setText(R.string.username_null);
					userNameOK = false;
					registerAndLoginBtn.setEnabled(false);
				}
				break;
			/** Check password (longer than 6 char, enter password twice and same) */
			case R.id.register_password:
				if(repasswordEdit.getText().length() <=0 )
				{
					if(s.length() < 6){
						warningMsgPassword.setTextColor(Color.RED);
						warningMsgPassword.setText(R.string.password_short);
						passwordOK = false;
						registerAndLoginBtn.setEnabled(false);
					}
					else if(s.length() >= 6){
						warningMsgPassword.setText("");
					}
				}
				else
				{
					if(s.toString().equals(repasswordEdit.getText().toString()))
					{
						warningMsgPassword.setTextColor(Color.GREEN);
						warningMsgPassword.setText(R.string.password_ok);
						warningMsgRepassword.setTextColor(Color.GREEN);
						warningMsgRepassword.setText(R.string.password_ok);
						passwordOK = true;
						if(userNameOK && passwordOK)
							registerAndLoginBtn.setEnabled(true);
					}
					else
					{
						warningMsgPassword.setTextColor(Color.RED);
						warningMsgPassword.setText(R.string.password_dismatch);
						warningMsgRepassword.setTextColor(Color.RED);
						warningMsgRepassword.setText(R.string.password_dismatch);
						registerAndLoginBtn.setEnabled(false);
					}
				}
				break;
			/** Check password (longer than 6 char, enter password twice and same) */
			case R.id.register_re_password:
				if(s.length() > 0 && s.toString().equals(passwordEdit.getText().toString())){
					warningMsgRepassword.setTextColor(Color.GREEN);
					warningMsgRepassword.setText(R.string.password_ok);
					warningMsgPassword.setText("");
					passwordOK = true;
					if(userNameOK && passwordOK)
						registerAndLoginBtn.setEnabled(true);
				}
				else{
					warningMsgRepassword.setTextColor(Color.RED);
					warningMsgRepassword.setText(R.string.password_dismatch);
					registerAndLoginBtn.setEnabled(false);
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
