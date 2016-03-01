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


/** MainPreferenceActivity
 *
 *  User Preference Setting page,
 *  For enable and disable modules
 */

package fi.tol.android.RTDAReceiver;


import java.util.Timer;
import java.util.TimerTask;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;

public class MainPreferenceActivity extends PreferenceActivity implements OnPreferenceChangeListener{
	
	private CheckBoxPreference pingICheckBoxPreference;
	private CheckBoxPreference pingIIincomingCheckBoxPreference;
	private CheckBoxPreference pingIIoutgoingCheckBoxPreference;
	
	
	
	/** Timer to delay broadcast preference changes to PhoneCallListenerService 
		 *  to ensure the service is successfully created before can receiver the broadcast */
	private Timer delayPreferenceChangeBroadcastTimer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		/** Inflates the given XML resource and adds the preference
		 *  hierarchy to the current preference hierarchy */
		addPreferencesFromResource(R.xml.preferences);
		this.setTitle(R.string.preference);
		
		/** Finds Checkbox Preferences based on the keys */
		pingICheckBoxPreference = (CheckBoxPreference) findPreference(getString(R.string.Ping_I_key));
		pingIIincomingCheckBoxPreference = (CheckBoxPreference) findPreference(getString(R.string.Ping_II_incoming_key));
		pingIIoutgoingCheckBoxPreference = (CheckBoxPreference) findPreference(getString(R.string.Ping_II_outgoing_key));
		
		/** Listen to preference changes*/
		pingICheckBoxPreference.setOnPreferenceChangeListener(this);
		pingIIincomingCheckBoxPreference.setOnPreferenceChangeListener(this);
		pingIIoutgoingCheckBoxPreference.setOnPreferenceChangeListener(this);
	}
	
	
	@Override
	protected void onDestroy() {
		
		/** Inform BroadcastReceiver in MainPage Activity to 
		 *  write the preference setting to currently login user's preference file*/
		Intent updateUserPreferenceIntent = new Intent();
		updateUserPreferenceIntent.setAction(MainPage.UPDATE_USER_PREFERENCE_SETTING);
		sendBroadcast(updateUserPreferenceIntent);

		super.onDestroy();
		
	}

	/** Listen to the preference setting change within this Activity life-cycle */
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if(preference.getKey().equals(getString(R.string.Ping_I_key)))
		{
			pingICheckBoxPreference.setChecked((Boolean)newValue);
			MainPage.toStartService(pingICheckBoxPreference.isChecked(),pingIIincomingCheckBoxPreference.isChecked(),
					pingIIoutgoingCheckBoxPreference.isChecked());
			if((Boolean)newValue == true)
			{
				/** Inform BroadcastReceiver in MainPage Activity to 
				 *  write the preference setting to currently login user's preference file*/
				delayPreferenceChangeBroadcastTimer = new Timer();
				delayPreferenceChangeBroadcastTimer.schedule(new TimerTask(){
		 	    	public void run() {
		 	    		Intent updateUserPreferenceIntent = new Intent();
						updateUserPreferenceIntent.setAction(PhoneCallListenerService.PINGI_ENABLED);
						sendBroadcast(updateUserPreferenceIntent);
					}
		 	    }, 3000);
				
			}	
		}
		else if(preference.getKey().equals(getString(R.string.Ping_II_incoming_key)))
		{
			pingIIincomingCheckBoxPreference.setChecked((Boolean)newValue);
			MainPage.toStartService(pingICheckBoxPreference.isChecked(),pingIIincomingCheckBoxPreference.isChecked(),
					pingIIoutgoingCheckBoxPreference.isChecked());
			if((Boolean)newValue == true)
			{
				
				/** Inform BroadcastReceiver in MainPage Activity to 
				 *  write the preference setting to currently login user's preference file*/
				delayPreferenceChangeBroadcastTimer = new Timer();
				delayPreferenceChangeBroadcastTimer.schedule(new TimerTask(){
		 	    	public void run() {
		 	    		Intent updateUserPreferenceIntent = new Intent();
						updateUserPreferenceIntent.setAction(PhoneCallListenerService.PINGII_INCOMMING_ENABLED);
						sendBroadcast(updateUserPreferenceIntent);
					}
		 	    }, 3000);
				
			}	
		}
		else if(preference.getKey().equals(getString(R.string.Ping_II_outgoing_key)))
		{
			pingIIoutgoingCheckBoxPreference.setChecked((Boolean)newValue);
			MainPage.toStartService(pingICheckBoxPreference.isChecked(),pingIIincomingCheckBoxPreference.isChecked(),
					pingIIoutgoingCheckBoxPreference.isChecked());
			if((Boolean)newValue == true)
			{
				/** Inform BroadcastReceiver in MainPage Activity to 
				 *  write the preference setting to currently login user's preference file*/
				delayPreferenceChangeBroadcastTimer = new Timer();
				delayPreferenceChangeBroadcastTimer.schedule(new TimerTask(){
		 	    	public void run() {
		 	    		Intent updateUserPreferenceIntent = new Intent();
						updateUserPreferenceIntent.setAction(PhoneCallListenerService.PINGII_OUTGOING_ENABLED);
						sendBroadcast(updateUserPreferenceIntent);
					}
		 	    }, 3000);
			}	
		}
		return false;
	}
	
}
