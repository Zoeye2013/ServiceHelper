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
 *  BootBroadcastReceiver
 *
 *  Broadcast receiver for listening phone's shut down and boot actions
 *  If any Ping Apps is enabled, then need this broadcast receiver to
 *  start or stop the background service for Ping Apps when the phone
 *  is powered off or re-booted
 */


package fi.tol.android.RTDAReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootBroadcastReceiver extends BroadcastReceiver{
	
	/** Listen to two actions: phone shun down action and phone boot completed action */
	static final String BOOT_ACTION = "android.intent.action.BOOT_COMPLETED";
	static final String SHUTDOWN_ACTION = "android.intent.action.ACTION_SHUTDOWN";
	
	/** Preference settings own by specific username 
	 *  here used to get the info about enabled modules */
	private SharedPreferences userPreference;
	
	/** SharedPreference for account info of all registered users 
	 *  here used to get the info of currently login user */
	private SharedPreferences accountSharedPreference;
	
	private String lastLogInUserName;
	private boolean enablePing_I = false;
	private boolean enablePingIIin = false;
	private boolean enablePingIIout = false;

	@Override
	public void onReceive(Context context, Intent intent) {
		
		/** Phone is booted */
		if (intent.getAction().equals(BOOT_ACTION))
        {
    	    accountSharedPreference = context.getSharedPreferences("Account",Context.MODE_PRIVATE);
    	    lastLogInUserName = accountSharedPreference.getString("last_log_in", null);

    	    /** last login user isn't null */
    	    if(lastLogInUserName != null)
    	    {
    	    	Log.i("BOOT_SHUT", "BOOT_last log in is null");
    	    	userPreference = context.getSharedPreferences(lastLogInUserName, Context.MODE_PRIVATE);
    	    	enablePing_I = userPreference.getBoolean(context.getString(R.string.Ping_I_key), false);
    	    	enablePingIIin = userPreference.getBoolean(context.getString(R.string.Ping_II_incoming_key), false);
    	    	enablePingIIout = userPreference.getBoolean(context.getString(R.string.Ping_II_outgoing_key), false);
    	    	
    	    	/** Service will be started if at least one of Ping Apps is enabled */
    	    	if(enablePing_I == true || enablePingIIin == true || enablePingIIout == true)
    	    	{
    	    		context.startService(new Intent(context,PhoneCallListenerService.class));
    	    	}
    	    }
        }
		/** Stop background service for Ping Apps */
        else if(intent.getAction().equals(SHUTDOWN_ACTION))
        {
        	context.stopService(new Intent(context,PhoneCallListenerService.class));
        }
	}
}
