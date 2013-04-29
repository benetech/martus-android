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

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcTransportFactory;
import org.martus.common.MartusLogger;
import org.martus.common.ProgressMeterInterface;
//import org.torproject.jtor.TorInitializationListener;


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

	public void setProgressMeter(ProgressMeterInterface initializationProgressMeterToUse)
	{
		initializationProgressMeter = initializationProgressMeterToUse;
	}

	public void start()
	{
		enableRealTorClient();
	}
	
	public void stop()
	{
		isTorActive = false;
	}
	
	public boolean isReady()
	{
		if(!isTorActive)
			return true;
		
		return isTorReady;
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
		if(initializationProgressMeter != null)
			initializationProgressMeter.updateProgressMeter(percent, 100);
		
		MartusLogger.log("JTor initialization: " + percent + "% - " + message);
	}

	void updateProgressComplete()
	{
		if(initializationProgressMeter != null)
			initializationProgressMeter.updateProgressMeter(100, 100);
		
		isTorReady = true;
	}

	private void createRealTorClient()
	{
//		tor = new TorClient();
//
//		class TorInitializationHandler implements TorInitializationListener
//		{
//			public void initializationProgress(String message, int percent)
//			{
//				updateProgress(message, percent);
//			}
//			
//			public void initializationCompleted()
//			{
//				updateProgressComplete();
//			}
//
//		}
		
//		tor.addInitializationListener(new TorInitializationHandler());
	}
	
	private void enableRealTorClient()
	{
//		isTorActive = true;
//		tor.start();
	}

	private XmlRpcTransportFactory createRealTorTransportFactory(XmlRpcClient client, SimpleX509TrustManager tm) throws Exception
	{
		XmlRpcTransportFactory factory = null;
//		factory = new JTorXmlRpcTransportFactory(client, tor, MartusUtilities.createSSLContext(tm));
		return factory;
	}

//	private TorClient tor;
	private ProgressMeterInterface initializationProgressMeter;

	private boolean isTorActive;
	private boolean isTorReady;
}
