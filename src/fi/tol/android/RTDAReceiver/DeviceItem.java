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

/** Class DeviceItem
 *
 *  Class used to store information need in Bluetooth Process data acquisition module
 */

package fi.tol.android.RTDAReceiver;

import android.bluetooth.BluetoothDevice;

public class DeviceItem 
{
	/** The No. of the Bluetooth Device */
	private int btDeviceNo;
	
	/** Bluetooth Device Object */
	private BluetoothDevice btDevice;
	
	/** Whether this BT device is allowed device */
	private boolean isAllowedDevice;
	/** Whether this BT device is irrelevant device */
	private boolean isUselessDevice;
	/** Whether this BT device currently in range */
	private boolean isCurrentDevice;
	/** Whether this BT device is still in range */
	private boolean inScope;
	/** The type of this BT device */
	private int btMajorDeviceClass;
    
    public void setBTdeviceNo(int no)
    {
    	btDeviceNo = no;
    }
    public void setBluetoothDevice(BluetoothDevice device)
    {
    	btDevice = device;
    }
    public void setIsUseless(boolean isUseless)
    {
    	isUselessDevice = isUseless;
    }
    public void setIsAllowed(boolean isAllowed)
    {
    	isAllowedDevice = isAllowed;
    }
    public void setIsCurrent(boolean isCurrent)
    {
    	isCurrentDevice = isCurrent;
    }
    public void setIsInScope(boolean isInScope)
    {
    	inScope = isInScope;
    }

    public void setBTMajorDeviceClass(int majorClass)
    {
    	btMajorDeviceClass = majorClass;
    }
    public int getBTDeviceNo()
    {
    	return btDeviceNo;
    }
    public BluetoothDevice getBluetoothDevice()
    {
    	return btDevice;
    }
    public boolean getIsUseless()
    {
    	return isUselessDevice;
    }
    public boolean getIsAllowed()
    {
    	return isAllowedDevice;
    }
    public boolean getIsCurrent()
    {
    	return isCurrentDevice;
    }
    public boolean getIsInScope()
    {
    	return inScope;
    }
    public int getBTMajorDeviceClass()
    {
    	return btMajorDeviceClass;
    }
}
