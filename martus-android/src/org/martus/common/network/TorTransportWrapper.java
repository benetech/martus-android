/*

The Martus(tm) free, social justice documentation and
monitoring software. Copyright (C) 2013, Beneficent
Technology, Inc. (Benetech).

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
package org.martus.common.network;

import java.io.File;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcTransportFactory;
import org.martus.common.MartusLogger;
import org.martus.common.MartusUtilities;
import org.martus.common.ProgressMeterInterface;

import android.util.Log;
import com.subgraph.orchid.TorClient;
import com.subgraph.orchid.TorInitializationListener;
import com.subgraph.orchid.xmlrpc.OrchidXmlRpcTransportFactory;


public class TorTransportWrapper
{
	public static TorTransportWrapper create()
	{
		return new TorTransportWrapper();
	}
	
	private TorTransportWrapper()
	{
		isTorActive = false;
		isTorReady = false;

		createRealTorClient();
	}
	
	public void setTorDataDirectory(File directory)
	{
		tor.getConfig().setDataDirectory(directory);
	}

	public void setProgressMeter(ProgressMeterInterface initializationProgressMeterToUse)
	{
		progressMeter = initializationProgressMeterToUse;
	}

	public void start()
	{
		isTorActive = true;
		updateStatus();
		if(!isTorReady)
			new TorInitializer().start();
	}
	
	public void stop()
	{
		isTorActive = false;
		updateStatus();
	}
	
	protected class TorInitializer extends Thread
	{
		@Override
		public void run()
		{
			getTor().start();
		}

	}
	
	protected TorClient getTor()
	{
		return tor;
	}

	public boolean isEnabled()
	{
		return isTorActive;
	}
	
	public boolean isReady()
	{
		if(!isTorActive)
			return true;
		
		return isTorReady;
	}

	public void updateStatus()
	{
		if(progressMeter == null)
			return;
		
		if(isTorActive)
		{
			if(isTorReady)
			{
				progressMeter.setStatusMessage("TorStatusActive");
				progressMeter.hideProgressMeter();
			}
			else
			{
				progressMeter.setStatusMessage("TorStatusInitializing");
			}
		}
		else
		{
			progressMeter.setStatusMessage("TorStatusDisabled");
			progressMeter.hideProgressMeter();
		}
	}
	
	public XmlRpcTransportFactory createTransport(XmlRpcClient client, SimpleX509TrustManager tm)	throws Exception 
	{
		if(!isTorActive)
			return null;
		
		if(!isReady())
			throw new RuntimeException("Tor not initialized yet");
		
		return createRealTorTransportFactory(client, tm);
	}

	void updateProgress(String message, int percent)
	{
		Log.i("martus","Tor initialization: " + percent + "% - " + message);
		if(progressMeter != null)
			progressMeter.updateProgressMeter(percent, 100);
		updateStatus();
	}

	void updateProgressComplete()
	{
		Log.i("martus","Tor initialization complete");
		isTorReady = true;
		updateStatus();
	}

	private void createRealTorClient()
	{
		tor = new TorClient();

		class TorInitializationHandler implements TorInitializationListener
		{
			public void initializationProgress(String message, int percent)
			{
				updateProgress(message, percent);
			}
			
			public void initializationCompleted()
			{
				updateProgressComplete();
			}

		}
		
		tor.addInitializationListener(new TorInitializationHandler());
	}
	
	private XmlRpcTransportFactory createRealTorTransportFactory(XmlRpcClient client, SimpleX509TrustManager tm) throws Exception
	{
		XmlRpcTransportFactory factory = null;
		factory = new OrchidXmlRpcTransportFactory(client, tor, MartusUtilities.createSSLContext(tm));
		return factory;
	}

	private TorClient tor;
	private ProgressMeterInterface progressMeter;

	private boolean isTorActive;
	private boolean isTorReady;
}
