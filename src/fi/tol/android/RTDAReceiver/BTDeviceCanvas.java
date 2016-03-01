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
 *  ImageView BTDeviceCanvas
 *
 *  Self-defined canvas to present Bluetooth inquiring result or
 *  to virtualize the measurement result,
 *
 */

package fi.tol.android.RTDAReceiver;

import java.io.InputStream;
import java.util.ArrayList;

import android.bluetooth.BluetoothClass;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class BTDeviceCanvas extends ImageView
{
    private Canvas mCanvas = null;
    private Bitmap bitmap;
    
    /** The Paint class holds the style and color information 
     *  about how to draw geometries, text and bitmaps. */
    private Paint paint = new Paint();
    
    private ArrayList<DeviceItem> btDevicesList;
    
    private int bitMapSize;
    
	public BTDeviceCanvas(Context context) {
		super(context);
	}
	public BTDeviceCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public BTDeviceCanvas(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @Override
    /** Do our drawing of the inquiring or measurement results */
    protected void onDraw(Canvas canvas) {
    	mCanvas = canvas;
    	bitMapSize = this.getWidth()/8;
        super.onDraw(mCanvas);
        
        int x = 0;
    	int y = 0;
    	
    	/** At most 10 device's drawing in one line */
    	int lineCount = 0;
    	int rowCount = 0;
    	paint.setTextSize(bitMapSize/4);
    	
    	/** If there are bluetooth devices in the list, then paint them */
    	if(btDevicesList != null && btDevicesList.isEmpty() != true)
        {
    		for(int i = 0; i<btDevicesList.size(); i++)
        	{
    			/** Calculate the position to paint the device */
    			x = lineCount*bitMapSize;
        		y = rowCount*bitMapSize;
        		
        		/** When in open state, paint white icons for found devices */
        		if(DevicesManaging.getInstance().getCanvasState() == DevicesManaging.ON_OPEN_STATE &&
    					btDevicesList.get(i).getIsInScope())
    			{
    				paintWhitePicture(btDevicesList.get(i).getBTMajorDeviceClass());
    				mCanvas.drawBitmap(bitmap, x, y, paint);
            		lineCount ++;
    			}
        		/** When in training state, paint white icons which numbers for allowed devices */
    			else if(DevicesManaging.getInstance().getCanvasState() == DevicesManaging.ON_START_STATE && 
        				btDevicesList.get(i).getIsAllowed() &&
        				btDevicesList.get(i).getIsInScope())
    			{
    				paintWhitePicture(btDevicesList.get(i).getBTMajorDeviceClass());
    				mCanvas.drawBitmap(bitmap, x, y, paint);
    				mCanvas.drawText(String.valueOf(btDevicesList.get(i).getBTDeviceNo()), 
    						(x + bitMapSize/2 - 5), (float) (y + bitMapSize*0.8), paint);
            		lineCount ++;
    			}
        		/** When in measuring state, paint green or red icons for allowed devices */
    			else if(DevicesManaging.getInstance().getCanvasState() == DevicesManaging.ON_RUN_STATE &&
    					btDevicesList.get(i).getIsAllowed())
    			{
    				/** If the allowed device is currently in range, then paint green icon */
    				if(btDevicesList.get(i).getIsInScope())
        			{
        				paintGreenPicture(btDevicesList.get(i).getBTMajorDeviceClass());
        			}
    				/** If the allowed device currently isn't in range, then paint red icon */
        			else if(!btDevicesList.get(i).getIsInScope())
        			{
        				paintRedPicture(btDevicesList.get(i).getBTMajorDeviceClass());
        			}
    				mCanvas.drawBitmap(bitmap, x, y, paint);
    				mCanvas.drawText(String.valueOf(btDevicesList.get(i).getBTDeviceNo()), 
    						(x + bitMapSize/2 - 5), (float) (y + bitMapSize*0.8), paint);
            		lineCount ++;
    			}
        		if(lineCount == 8)
        		{
        			lineCount = 0;
        			rowCount ++;
        		}   		
        	}
        }
    }
    	
    /** Paint Bluetooth device's icon in white 
     *  and depends on the device's type, will paint different shapes of icons*/
    public void paintWhitePicture(int deviceClass)
    {
    	switch(deviceClass)
		{
		case BluetoothClass.Device.Major.COMPUTER:
			bitmap = loadImage(R.drawable.computer_white, bitMapSize, bitMapSize);
			break;
		case BluetoothClass.Device.Major.PHONE:
			bitmap = loadImage(R.drawable.phone_white, bitMapSize, bitMapSize);
			break;
		case BluetoothClass.Device.Major.PERIPHERAL:
			bitmap = loadImage(R.drawable.mouse_white, bitMapSize, bitMapSize);
			break;
		default:
			bitmap = loadImage(R.drawable.others_white, bitMapSize, bitMapSize);
			break;
		}
    }
    
    /** Paint Bluetooth device's icon in green 
     *  and depends on the device's type, will paint different shapes of icons*/
    public void paintGreenPicture(int deviceClass)
    {
    	switch(deviceClass)
		{
		case BluetoothClass.Device.Major.COMPUTER:
			bitmap = loadImage(R.drawable.computer_green, bitMapSize, bitMapSize);
			break;
		case BluetoothClass.Device.Major.PHONE:
			bitmap = loadImage(R.drawable.phone_green, bitMapSize, bitMapSize);
			break;
		case BluetoothClass.Device.Major.PERIPHERAL:
			bitmap = loadImage(R.drawable.mouse_green, bitMapSize, bitMapSize);
			break;
		default:
			bitmap = loadImage(R.drawable.others_green, bitMapSize, bitMapSize);
			break;
		}
    }
    
    /** Paint Bluetooth device's icon in red
     * 	and depends on the device's type, will paint different shapes of icons*/
    public void paintRedPicture(int deviceClass)
    {
    	switch(deviceClass)
		{
		case BluetoothClass.Device.Major.COMPUTER:
			bitmap = loadImage(R.drawable.computer_red, bitMapSize, bitMapSize);
			break;
		case BluetoothClass.Device.Major.PHONE:
			bitmap = loadImage(R.drawable.phone_red, bitMapSize, bitMapSize);
			break;
		case BluetoothClass.Device.Major.PERIPHERAL:
			bitmap = loadImage(R.drawable.mouse_red, bitMapSize, bitMapSize);
			break;
		default:
			bitmap = loadImage(R.drawable.others_red, bitMapSize, bitMapSize);
			break;
		}
    }
    
    /** Load bitmap image */
    public Bitmap loadImage(int imageRes, int width, int height)
    {
    	Resources r = this.getContext().getResources();
    	InputStream is = r.openRawResource(imageRes);
    	BitmapDrawable  bmpDraw = new BitmapDrawable(is);
    	Bitmap bmp = Bitmap.createScaledBitmap(bmpDraw.getBitmap(), width, height, false);
    	return bmp;
    }
    
    /** Set the list of devices that are going to be painted */
    public void setBTDevicesList(ArrayList<DeviceItem> devices)
    {
    	btDevicesList = devices;
    }
}