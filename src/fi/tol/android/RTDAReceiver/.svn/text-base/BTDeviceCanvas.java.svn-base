package fi.tol.android.RTDAReceiver;

import java.io.InputStream;
import java.util.ArrayList;

import android.bluetooth.BluetoothAdapter;
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
    private Paint paint = new Paint();
    private ArrayList<DeviceItem> btDevicesList;
    private int canvasState = RTDAReceiver.ON_OPEN_STATE;
    
    private final int bitMapSize = 25;
    
	public BTDeviceCanvas(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	public BTDeviceCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public BTDeviceCanvas(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
    	mCanvas = canvas;
        super.onDraw(mCanvas);
        
        int x = 0;
    	int y = 0;
    	int lineCount = 0;//At most 10 in one line
    	int rowCount = 0;
    	paint.setTextSize(8);
    	if(btDevicesList != null && btDevicesList.isEmpty() != true)
        {
    		for(int i = 0; i<btDevicesList.size(); i++)
        	{
    			x = lineCount*bitMapSize;
        		y = rowCount*bitMapSize;
        		if(canvasState == RTDAReceiver.ON_OPEN_STATE &&
    					btDevicesList.get(i).getIsInScope())
    			{
    				paintWhitePicture(btDevicesList.get(i).getBTMajorDeviceClass());
    				mCanvas.drawBitmap(bitmap, x, y, paint);
            		lineCount ++;
    			}
    			else if(canvasState == RTDAReceiver.ON_START_STATE && 
        				btDevicesList.get(i).getIsAllowed() &&
        				btDevicesList.get(i).getIsInScope())
    			{
    				paintWhitePicture(btDevicesList.get(i).getBTMajorDeviceClass());
    				mCanvas.drawBitmap(bitmap, x, y, paint);
    				mCanvas.drawText(String.valueOf(btDevicesList.get(i).getBTDeviceNo()), 
    						(x + bitMapSize/2 - 5), (float) (y + bitMapSize*0.8), paint);
            		lineCount ++;
    			}
    			else if(canvasState == RTDAReceiver.ON_RUN_STATE &&
    					btDevicesList.get(i).getIsAllowed())
    			{
    				if(btDevicesList.get(i).getIsInScope())
        			{
        				paintGreenPicture(btDevicesList.get(i).getBTMajorDeviceClass());
        			}
        			else if(!btDevicesList.get(i).getIsInScope())
        			{
        				paintRedPicture(btDevicesList.get(i).getBTMajorDeviceClass());
        			}
    				mCanvas.drawBitmap(bitmap, x, y, paint);
    				mCanvas.drawText(String.valueOf(btDevicesList.get(i).getBTDeviceNo()), 
    						(x + bitMapSize/2 - 5), (float) (y + bitMapSize*0.8), paint);
            		lineCount ++;
    			}
        		if(lineCount == 9)
        		{
        			lineCount = 0;
        			rowCount ++;
        		}   		
        	}
        }
    }
    	
    
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
    
    public Bitmap loadImage(int imageRes, int width, int height)
    {
    	Resources r = this.getContext().getResources();
    	InputStream is = r.openRawResource(imageRes);
    	BitmapDrawable  bmpDraw = new BitmapDrawable(is);
    	Bitmap bmp = Bitmap.createScaledBitmap(bmpDraw.getBitmap(), width, height, false);
    	return bmp;
    }
    public void setBTDevicesList(ArrayList<DeviceItem> devices)
    {
    	btDevicesList = devices;
    }
    public void setCanvasState(int state)
    {
    	canvasState = state;
    }
}