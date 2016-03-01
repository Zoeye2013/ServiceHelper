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
 *  Class PhoneCallRecordItem
 *
 *  Class for store the information of a phone call record
 */

package fi.tol.android.RTDAReceiver;


public class PhoneCallRecordItem{
	
	
	private String contactName;
	private String phoneNumber;
	private String beginTime;
	private String endTime;
	private long callDuration;
	private String date;
	private String timezone;
	private long beginDate;
	private String phoneType;
	private int type;
	private long outgoingCallWaitingTime;
	
	
	public void setPhoneType(String type)
	{
		phoneType = type;
	}
	public void setType(int tp)
	{
		type = tp;
	}
	public void setPhoneNumber(String number)
	{
		phoneNumber = number;
	}
	public void setContactName(String name)
	{
		contactName = name;
	}
	public void setBeginDate(long date)
	{
		beginDate = date;
	}
	public void setBeginTime(String begin)
	{
		beginTime = begin;
	}
	public void setEndTime(String end)
	{
		endTime = end;
	}
	public void setCallDuration(long duration)
	{
		callDuration = duration;
	}
	public void setOutgoingWaitingTime(long waiting)
	{
		outgoingCallWaitingTime = waiting;
	}
	public void setDate(String d)
	{
		date = d;
	}
	public void setTimezone(String timez)
	{
		timezone = timez;
	}
	
	public String getPhoneType()
	{
		return phoneType;
	}
	public int getType()
	{
		return type;
	}
	
	public String getPhoneNumber()
	{
		return phoneNumber;
	}
	public String getContactName()
	{
		return contactName;
	}
	public long getBeginDate()
	{
		return beginDate;
	}
	public String getBeginTime()
	{
		return beginTime;
	}
	public String getEndTime()
	{
		return endTime;
	}
	public long getCallDuration()
	{
		return callDuration;
	}
	public long getOutgoingWaitingTime()
	{
		return outgoingCallWaitingTime;
	}
	public String getTimezone()
	{
		return timezone;
	}
	public String getDate()
	{
		return date;
	}
}
