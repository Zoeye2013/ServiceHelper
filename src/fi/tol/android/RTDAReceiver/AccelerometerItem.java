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
 *  Class AccelerometerItem
 *
 *  For storing Accelerometer sensor data
 *  and calculate average values and variations of the sensor data
 *  We record three dimensions of Accelerometer sensor data: x, y, z
 **/

package fi.tol.android.RTDAReceiver;

import java.util.ArrayList;

public class AccelerometerItem {
	
	/** Average value of accelerometer sensor in three dimensions */
	private double averageX;
	private double averageY;
	private double averageZ;
	
	/** Absolute average value of accelerometer sensor in three dimensions */
	private double absAverageX;
	private double absAverageY;
	private double absAverageZ;
	
	/** Variation of sensor values in three dimensions */
	private double variationX;
	private double variationY;
	private double variationZ;
	
	/** Sum of sensor values in three dimensions */
	private double sumOne;
	private double absSum;
	private double sqSum;
	private double sumTwo;
	private double sumThree;
	
	/** Point set of sensor values in three dimensions */
	private ArrayList<Float> seriesX;
	private ArrayList<Float> seriesY;
	private ArrayList<Float> seriesZ;
	private ArrayList<Long> measurementTimes;
	private int acceNum;
	
	
	
	public AccelerometerItem()
	{
		seriesX = new ArrayList<Float>();
		seriesY = new ArrayList<Float>();
		seriesZ = new ArrayList<Float>();
		measurementTimes = new ArrayList<Long>();
	}
	
	public String getColumns()
	{
		/*String columns = ",averageX,averageY,averageZ,absAverageX,absAverageY,absAverageZ,variationX,variationY,variationZ" +
			",sumOne,absSum,sqSum,sumTwo,sumThree";*/
		String columns = ",X,Y,Z";
		return columns;
	}
	/** After a time period, clear serial sensor values
	 *  ready for recording data in next time period */
	public void initSerialValues()
	{
		averageX =0; 
		averageY = 0; 
		averageZ = 0;
		absAverageX = 0;
		absAverageY = 0;
		absAverageZ = 0;
		variationX = 0;
		variationY = 0;
		variationZ = 0;
		
		seriesX.clear();
		seriesY.clear();
		seriesZ.clear();
		measurementTimes.clear();
		acceNum = 0;
	}
	
	/** Add newest sensor values to the list */
	public void addSerialValues(long time,float x, float y, float z)
	{
		seriesX.add(x);
		seriesY.add(y);
		seriesZ.add(z);
		measurementTimes.add(time);
		acceNum ++;
	}
	public String getAccelerometerValues(int index)
	{
		String values = "," + seriesX.get(index) + "," + seriesY.get(index) + "," + seriesZ.get(index) + "\n";
		return values;
	}
	public Long getAcceValueTime(int index)
	{
		Long time = measurementTimes.get(index);
		return time;
	}
	public int getAccelerometerNum()
	{
		return acceNum;
	}
	
	/** Calculate the average values of sensor data during a time period */
	public void calculateAverageValues()
	{
		double xSumTemp = 0, ySumTemp = 0, zSumTemp = 0;
		double absXSumTemp = 0, absYSumTemp = 0, absZSumTemp = 0;
		double sqSumTemp = 0, sumTwoTemp = 0, sumThreeTemp = 0;
		int num = seriesX.size();
		for(int i = 0; i < num; i++)
		{
			float x = seriesX.get(i), y = seriesY.get(i), z = seriesZ.get(i);
			xSumTemp += x;
			absXSumTemp += Math.abs(x);
			sqSumTemp += Math.pow(x, 2);
			
			ySumTemp += y;
			absYSumTemp += Math.abs(y);
			sqSumTemp += Math.pow(y, 2);
			
			zSumTemp += z;
			absZSumTemp += Math.abs(z);
			sqSumTemp += Math.pow(z, 2);
			
			sumTwoTemp += x*y + y*z + z*x;
			sumThreeTemp += x*y*z;
		}
		averageX = xSumTemp/num;
		averageY = ySumTemp/num;
		averageZ = zSumTemp/num;
		
		sumOne = averageX + averageY + averageZ;
		
		absAverageX = absXSumTemp/num;
		absAverageY = absYSumTemp/num;
		absAverageZ = absZSumTemp/num;
		
		absSum = absAverageX + absAverageY + absAverageZ;
		sqSum = sqSumTemp/num;
		sumTwo = sumTwoTemp/num;
		sumThree = sumThreeTemp/num;
	}
	
	/** Calculate the variations of sensor data during a time period */
	public void calculateVariations()
	{
		double xVariationTemp = 0, yVariationTemp = 0, zVariationTemp = 0;
		int xNum = seriesX.size(), yNum = seriesY.size(), zNum = seriesZ.size();
		for(int i = 0; i < xNum; i++)
		{
			xVariationTemp += Math.pow((seriesX.get(i) - averageX), 2);
		}
		for(int i = 0; i < yNum; i++)
		{
			yVariationTemp += Math.pow((seriesY.get(i) - averageY), 2);
		}
		for(int i = 0; i < zNum; i++)
		{
			zVariationTemp += Math.pow((seriesZ.get(i) - averageZ), 2);
		}
		variationX = xVariationTemp/xNum;
		variationY = yVariationTemp/yNum;
		variationZ = zVariationTemp/zNum;
	}
	
	public void calculateSums()
	{
	}
	
	/** Get average values of sensor data during a time period */
	public double getAverageX()
	{
		return averageX;
	}
	public double getAverageY()
	{
		return averageY;
	}
	public double getAverageZ()
	{
		return averageZ;
	}
	
	/** Get absolute average values of sensor data during a time period */
	public double getAbsAverageX()
	{
		return absAverageX;
	}
	public double getAbsAverageY()
	{
		return absAverageY;
	}
	public double getAbsAverageZ()
	{
		return absAverageZ;
	}
	
	/** Get variations of sensor data during a time period */
	public double getVariationX()
	{
		return variationX;
	}
	public double getVariationY()
	{
		return variationY;
	}
	public double getVariationZ()
	{
		return variationZ;
	}
	public double getSumOne()
	{
		return sumOne;
	}
	public double getAbsSum()
	{
		return absSum;
	}
	public double getsqSum()
	{
		return sqSum;
	}
	public double getSumTwo()
	{
		return sumTwo;
	}
	public double getSumThree()
	{
		return sumThree;
	}
}
