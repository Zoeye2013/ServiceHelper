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
 *  Class SignalVectorItem
 *
 *  To store items of a signal vector record,
 *  which is a vector of Bluetooth RSSI value
 *  Each item in the vector is the RSSI value of one of current Bluetooth allowed devices */

package fi.tol.android.RTDAReceiver;

import java.util.ArrayList;

public class SignalVectorItem {
	
	/** List stores Bluetooth RSSI value of specific Bluetooth allowed devices */
	private ArrayList<Short> signalVector;
	
	public SignalVectorItem()
	{
		signalVector = new ArrayList<Short>();
	}

	/** Record Bluetooth RSSI value of current set of Bluetooth allowed devices */
	public void setSignalVector(ArrayList<Short> rssi)
	{
		for(int i = 0; i < rssi.size(); i ++)
		{
			signalVector.add(rssi.get(i));
		}
	}
	
	public void setSignal(int index, short rssi)
	{
		signalVector.set(index, rssi);
	}
	
	public void initSignalVector(int num)
	{
		/** Initiate the signal vector, set each allowed device's RSSI value to 0 */
		for(int i = 0; i < num; i ++)
		{
			signalVector.add((short) 0);
		}
	}
	
	/** Get Bluetooth RSSI value of current set of Bluetooth allowed devices */
	public ArrayList<Short> getSignalVector()
	{
		return signalVector;
	}
	
	/** Clear signal vector data preparing for receiving new data */
	public void clearSignalVector()
	{
		for(int i = 0; i < signalVector.size(); i++)
		{
			signalVector.set(i, (short)(0));
		}
	}
}
