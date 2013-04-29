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

package org.martus.clientside;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Vector;
import javax.xml.parsers.SAXParserFactory;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.util.SAXParsers;
import org.martus.common.MartusLogger;
import org.martus.common.network.NetworkInterfaceConstants;
import org.martus.common.network.NetworkInterfaceXmlRpcConstants;
import org.martus.common.network.NonSSLNetworkAPI;
import org.martus.common.network.NonSSLNetworkAPIWithHelpers;
import org.martus.common.network.TorTransportWrapper;

public class ClientSideNetworkHandlerUsingXmlRpcForNonSSL extends NonSSLNetworkAPIWithHelpers implements NetworkInterfaceConstants, NetworkInterfaceXmlRpcConstants
	
{
	public ClientSideNetworkHandlerUsingXmlRpcForNonSSL(String serverName)
	{
		this(serverName, (TorTransportWrapper)null);
	}
	
	public ClientSideNetworkHandlerUsingXmlRpcForNonSSL(String serverName, TorTransportWrapper transportToUse)
	{
		this(serverName, NetworkInterfaceXmlRpcConstants.defaultNonSSLPorts, transportToUse);
	}
	
	public ClientSideNetworkHandlerUsingXmlRpcForNonSSL(String serverName, int[] portsToUse)
	{
		this(serverName, portsToUse, null);
	}

	public ClientSideNetworkHandlerUsingXmlRpcForNonSSL(String serverName, int[] portsToUse, TorTransportWrapper transportToUse)
	{
		server = serverName;
		ports = portsToUse;
		transport = transportToUse;
	}

	// begin MartusXmlRpc interface
	public String ping()
	{
		Vector params = new Vector();
		return (String)callServer(server, CMD_PING, params);
	}

	public Vector getServerInformation()
	{
		logging("MartusServerProxyViaXmlRpc:getServerInformation");
		Vector params = new Vector();
		Object[] result = (Object[]) callServer(server, CMD_SERVER_INFO, params);
		return new Vector(Arrays.asList(result));
	}

	// end MartusXmlRpc interface

	public Object callServer(String serverName, String method, Vector params)
	{
		int numPorts = ports.length;
		for(int i=0; i < numPorts; ++i)
		{
			int port = ports[indexOfPortThatWorkedLast];
			try
			{
				return callServerAtPort(serverName, method, params, port);
			}
			catch(ConnectException e)
			{
				indexOfPortThatWorkedLast = (indexOfPortThatWorkedLast+1)%numPorts;
				continue;
			}
			catch(Exception e)
			{
				logging("MartusServerProxyViaXmlRpc:callServer Exception=" + e);
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}
	
	public Object callServerAtPort(String serverName, String method, Vector params, int port)
		throws MalformedURLException, XmlRpcException, IOException
	{
		if(ClientPortOverride.useInsecurePorts)
			port += 9000;
		
		final String serverUrl = "http://" + serverName + ":" + port + "/RPC2";
		MartusLogger.logVerbose("MartusServerProxyViaXmlRpc:callServer serverUrl=" + serverUrl);

		// NOTE: We **MUST** create a new XmlRpcClient for each call, because
		// there is a memory leak in apache xmlrpc 1.1 that will cause out of 
		// memory exceptions if we reuse an XmlRpcClient object
		XmlRpcClient client = new XmlRpcClient();
		
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setServerURL(new URL(serverUrl));
		SAXParsers.setSAXParserFactory(SAXParserFactory.newInstance());
		client.setConfig(config);
		
		return client.execute("MartusServer." + method, params);
	}

	private void logging(String message)
	{
		Timestamp stamp = new Timestamp(System.currentTimeMillis());
		System.out.println(stamp + " " + message);
	}

	public static boolean isNonSSLServerAvailable(NonSSLNetworkAPI server)
	{
		String result = server.ping();
		if(result == null)
			return false;
	
		if(result.indexOf(MARTUS_SERVER_PING_RESPONSE) != 0)
			return false;
	
		return true;
	}

	public static final String MARTUS_SERVER_PING_RESPONSE = "MartusServer";

	String server;
	int[] ports;
	private TorTransportWrapper transport;
	static int indexOfPortThatWorkedLast = 0;
	boolean debugMode;
}
