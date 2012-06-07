/*

The Martus(tm) free, social justice documentation and
monitoring software. Copyright (C) 2001-2007, Beneficent
Technology, Inc. (The Benetech Initiative).

Martus is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later
version with the additions and exceptions described in the
accompanying Martus license file entitled "license.txt".

It is distributed WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, including warranties of fitness of purpose or
merchantability.  See the accompanying Martus License and
GPL license for more details on the required license terms
for this software.

You should have received a copy of the GNU General Public
License along with this program; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA 02111-1307, USA.

*/
package org.martus.common;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MartusLogger 
{
	public static void disableLogging()
	{
		destination = null;
	}
	
	public static void logBeginProcess(String text)
	{
		log("Begin " + text);
	}
	
	public static void logEndProcess(String text)
	{
		log("End " + text);
	}

	public synchronized static void log(String text)
	{
		if(destination != null)
		{
			Date now = new Date();
			DateFormat df = new SimpleDateFormat("EEE MM/dd HH:mm:ss zzz");
			destination.println(df.format(now) + " " + text);
		}
	}
	
	public synchronized static void logCurrentStack()
	{
		if(destination == null)
			return;

		try
		{
			throw new Throwable("Current Stack");
		}
		catch(Throwable t)
		{
			t.printStackTrace(destination);
		}
	}
	
	public synchronized static void logException(Exception e)
	{
		if(destination == null)
			return;
		
		destination.println(e.getMessage());
		e.printStackTrace(destination);
	}
	
	public static void logError(String errorText)
	{
		log("ERROR: " + errorText);
	}
	
	public static void logWarning(String errorText)
	{
		log("WARNING: " + errorText);
	}
	
	public static PrintStream getDestination()
	{
		return destination;
	}
	
	private static PrintStream destination = System.out;
}
