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
 * Activity SaveAllowedDevicesDialog
 *
 * A popup dialog to save newly trained bluetooth devices as a group
 *
 */
package fi.tol.android.RTDAReceiver;

import java.io.File;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SaveAllowedDevicesDialog extends Activity {
	private Button okButton;
	private EditText siteNameEditText;
	private TextView warningTextView;
	private String allowedFileName;
	private String username;
	//private DevicesManaging deviceMag;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.save_allowed_dialog);
        
        this.setTitle(R.string.save_info);
        
        okButton = (Button)findViewById(R.id.button_ok);
        siteNameEditText = (EditText)findViewById(R.id.name_editText);
        warningTextView = (TextView)findViewById(R.id.site_save_warning_message);
        username = RTDAReceiver.username;
        okButton.setOnClickListener(new View.OnClickListener() 
    	{
    	    public void onClick(View v) {
    	    	allowedFileName = siteNameEditText.getText().toString();
    	    	Log.i("Allowed file", allowedFileName);
    	        File file = new File(Environment.getExternalStorageDirectory() + "/" + MainLogin.appHomeFolder +
    	        		"/" + username + "/" + MainLogin.rtdaSubFolder + "/" +
    	        		MainLogin.rtdaAllowedFolder + "/" + allowedFileName + ".csv");
    	        if(allowedFileName.length() <= 0)
    	        {
    	        	warningTextView.setText(R.string.null_warning);
    	        }
    	        else if (file.exists())
    	        {
    	        	warningTextView.setText(R.string.exist_file_warning);
    	        }
    	        else
    	        {
    	        	DevicesManaging.getInstance().writeAllowedDevicesFile(allowedFileName);
    				/*if(deviceMag.isAnyAllowedDevices())
    				{
    					btnRun.setEnabled(true);
    				}*/
    	        	finish();
    	        }
    	    }
    	});
	}
}