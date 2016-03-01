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
 *  Activity SelectMeasureSiteDialog
 *
 *  A pop up dialog Activity for showing the list of sets of allowed devices
 *  that are trained by users for different measurement sites
 *  Contains a List View
 */

package fi.tol.android.RTDAReceiver;

import java.io.File;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;


public class SelectMeasureSiteDialog extends Activity {
	
	/** Elements on the UI */
	private Button okButton;
	private Button backButton;
	private ListView siteListView;
	
	/** The content of the List View */
	private ArrayAdapter<String> siteAdapter;
	private String[] siteArray;
	
	private File siteDir;
	private String selectedSite;
	private String username;
	
	/** Action name for the action user select a site to run the measuring App */
	protected static final String MEASUREMENT_SITE_SELECTED = "MEASUREMENT_SITE_SELECTED";

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.select_measure_site_dialog);
		username = RTDAReceiver.username;
        siteDir = new File(Environment.getExternalStorageDirectory() + "/" + MainLogin.appHomeFolder +
        		"/" + username + "/" + MainLogin.rtdaSubFolder + "/" + MainLogin.rtdaAllowedFolder);
        siteArray = siteDir.list();
        okButton = (Button)findViewById(R.id.button_select_site_ok);
        backButton = (Button)findViewById(R.id.button_no_site_back);
        
        siteListView = (ListView)findViewById(R.id.sites_list);
        siteAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_single_choice,siteArray);
        siteListView.setAdapter(siteAdapter);
        siteListView.setItemsCanFocus(false);
        siteListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        
        /** There isn't any set of allowed devices */
        if(siteArray.length <=0)
        {
        	this.setTitle(R.string.no_allowed_device);
        	okButton.setVisibility(Button.INVISIBLE);
        	backButton.setVisibility(Button.VISIBLE);
        }
        else
        {
        	this.setTitle(R.string.select_allowed_device);
        	okButton.setVisibility(Button.VISIBLE);
        	backButton.setVisibility(Button.INVISIBLE);
        	siteListView.setItemChecked(0, true);
        	selectedSite = siteArray[0].toString();
        }
        
        /** When user select a set of allowed devices for measurement */
        siteListView.setOnItemClickListener(new OnItemClickListener(){

			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {

				selectedSite = siteArray[position].toString();
			}
        	
        });
        
        /** When user confirm his/her selectioin */
        okButton.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {

				DevicesManaging.getInstance().setCanvasState(DevicesManaging.ON_RUN_STATE);
				DevicesManaging.getInstance().readAllowedDevicesFile(selectedSite);
				
				Intent intent=new Intent();
				intent.setAction(MEASUREMENT_SITE_SELECTED);
				intent.putExtra("SelectedSite",selectedSite);
				sendBroadcast(intent);
				finish();
			} 	
        });
        
        /** When user click 'Back' button, just close the pop up dialog */
        backButton.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				finish();
			}
        });
	}
}
