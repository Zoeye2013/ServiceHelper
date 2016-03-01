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
 *  User Account Information page
 *
 *  For presenting currently login user account's information
 *  and user password setting
 */

package fi.tol.android.RTDAReceiver;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class AccountInfoActivity extends Activity {
	
	/** For presenting user name*/
	private String username;
	private TextView usernameText;
	
	/** Button for changing password */
	private Button changeBtn;
	
	/** For changing password */
	/** Old password */
	private TextView oldPwdText;
	private TextView oldPwdWarningText;
	private EditText oldPwdEdit;
	/** New password */
	private TextView newPwdText;
	private TextView newPwdWarningText;
	private EditText newPwdEdit;
	/** Confirm new password */
	private TextView reNewPwdText;
	private TextView reNewPwdWarningText;
	private EditText reNewPwdEdit;
	private boolean oldPwdOK = false;
	private boolean newPwdOK = false;
	
	/** Button for saving changes of account info */
	private Button saveChangeBtn;
	//private Button deleteAccountBtn;
	
	/** SharedPreference for account info of all registered users */
	private SharedPreferences accountPreference;
	private SharedPreferences.Editor accountPreferenceEditor;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.account_info_page);
		this.setTitle(R.string.account);
		
		/** Get SharedPreference for account info of all registered users */
		accountPreference = getSharedPreferences("Account",Context.MODE_PRIVATE);
		accountPreferenceEditor = accountPreference.edit();
		
		/** Get currently login user name */
		username = accountPreference.getString("last_log_in", null);
		
		/** Initiate the elements on the account page */
		usernameText = (TextView) findViewById(R.id.account_name);
		usernameText.setText(username);
		changeBtn = (Button) findViewById(R.id.button_change_password);
		//deleteAccountBtn = (Button) findViewById(R.id.button_delete_account);
		oldPwdText = (TextView) findViewById(R.id.old_password);
		oldPwdWarningText = (TextView) findViewById(R.id.warning_old_password);
		oldPwdEdit = (EditText) findViewById(R.id.old_password_edit);
		oldPwdEdit.addTextChangedListener(new TextWatcherForMultipleEdit(oldPwdEdit));
		newPwdText = (TextView) findViewById(R.id.new_password);
		newPwdWarningText = (TextView) findViewById(R.id.warning_new_password);
		newPwdEdit = (EditText) findViewById(R.id.new_password_edit);
		newPwdEdit.addTextChangedListener(new TextWatcherForMultipleEdit(newPwdEdit));
		reNewPwdText = (TextView) findViewById(R.id.re_new_password);
		reNewPwdWarningText = (TextView) findViewById(R.id.warning_re_new_password);
		reNewPwdEdit = (EditText) findViewById(R.id.re_new_password_edit);
		reNewPwdEdit.addTextChangedListener(new TextWatcherForMultipleEdit(reNewPwdEdit));
		saveChangeBtn = (Button) findViewById(R.id.btn_save_password_change);
		
		/** Set click listener for the button to change password */
		changeBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				/**  Elements for changing password is invisible by default
				 *  will be visible once the user click the change password button */
				oldPwdText.setVisibility(TextView.VISIBLE);
				oldPwdWarningText.setVisibility(TextView.VISIBLE);
				oldPwdEdit.setVisibility(EditText.VISIBLE);
				newPwdText.setVisibility(TextView.VISIBLE);
				newPwdWarningText.setVisibility(TextView.VISIBLE);
				newPwdEdit.setVisibility(EditText.VISIBLE);
				reNewPwdText.setVisibility(TextView.VISIBLE);
				reNewPwdWarningText.setVisibility(TextView.VISIBLE);
				reNewPwdEdit.setVisibility(EditText.VISIBLE);
				saveChangeBtn.setVisibility(Button.VISIBLE);
				
				/** Button for save account changes will be enabled 
				 *  only when user set correct new password */
				saveChangeBtn.setEnabled(false);
			}});
		
		/** Set click listener for the button to save changes of the account */
		saveChangeBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				if(oldPwdOK && newPwdOK){
					String password = reNewPwdEdit.getText().toString();
					
					/** Save account information into SharedPreference */
					accountPreferenceEditor.putString(username, password);
					accountPreferenceEditor.commit();
					
					/** Set elements for changing password to invisible */
					oldPwdText.setVisibility(TextView.INVISIBLE);
					oldPwdWarningText.setVisibility(TextView.INVISIBLE);
					oldPwdEdit.setVisibility(EditText.INVISIBLE);
					newPwdText.setVisibility(TextView.INVISIBLE);
					newPwdWarningText.setVisibility(TextView.INVISIBLE);
					newPwdEdit.setVisibility(EditText.INVISIBLE);
					reNewPwdText.setVisibility(TextView.INVISIBLE);
					reNewPwdWarningText.setVisibility(TextView.INVISIBLE);
					reNewPwdEdit.setVisibility(EditText.INVISIBLE);
					saveChangeBtn.setVisibility(Button.INVISIBLE);
					//toster notify user;
					
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
			/** If the content of the EditText for entering old password changed */
			case R.id.old_password_edit:
				if(s.length() > 0 && s.toString().equals(accountPreference.getString(username, null))){
					oldPwdWarningText.setText("");
					oldPwdOK = true;
					if(oldPwdOK && newPwdOK)
						saveChangeBtn.setEnabled(true);
					}
				else{
					oldPwdWarningText.setTextColor(Color.RED);
					oldPwdWarningText.setText(R.string.password_wrong);
					oldPwdOK = false;
					saveChangeBtn.setEnabled(false);
				}
				break;
			/** If the content of the EditText for entering new password changed */
			case R.id.new_password_edit:
				if(s.length() > 0){
					if(s.length() < 6){
						newPwdWarningText.setTextColor(Color.RED);
						newPwdWarningText.setText(R.string.password_short);
						newPwdOK = false;
						saveChangeBtn.setEnabled(false);
					}
					else if(s.length() >= 6){
						newPwdWarningText.setText("");
					}
				}
				else{
					newPwdWarningText.setTextColor(Color.RED);
					newPwdWarningText.setText(R.string.password_null);
					newPwdOK = false;
					saveChangeBtn.setEnabled(false);
				}
				break;
			/** If the content of the EditText for confirming new password changed */
			case R.id.re_new_password_edit:
				if(s.length() > 0 && s.toString().equals(newPwdEdit.getText().toString())){
					reNewPwdWarningText.setTextColor(Color.GREEN);
					reNewPwdWarningText.setText(R.string.password_ok);
					newPwdOK = true;
					if(oldPwdOK && newPwdOK)
						saveChangeBtn.setEnabled(true);
				}
				else{
					reNewPwdWarningText.setTextColor(Color.RED);
					reNewPwdWarningText.setText(R.string.password_dismatch);
					saveChangeBtn.setEnabled(false);
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
