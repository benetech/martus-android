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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Vector;

import javax.net.ssl.HttpsURLConnection;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

//import org.apache.xmlrpc.XmlRpcClient;
//import org.apache.xmlrpc.XmlRpcException;
//import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.martus.common.MartusLogger;
import org.martus.common.MartusUtilities;
import org.martus.common.network.NetworkInterface;
import org.martus.common.network.NetworkInterfaceConstants;
import org.martus.common.network.NetworkInterfaceXmlRpcConstants;
import org.martus.common.network.SimpleHostnameVerifier;
import org.martus.common.network.SimpleX509TrustManager;
import org.martus.util.Stopwatch;

public class ClientSideNetworkHandlerUsingXmlRpc
	implements NetworkInterfaceConstants, NetworkInterfaceXmlRpcConstants, NetworkInterface
{

	public static class SSLSocketSetupException extends Exception 
	{
	}

	public ClientSideNetworkHandlerUsingXmlRpc(String serverName, int[] portsToUse) throws SSLSocketSetupException
	{
		server = serverName;
		ports = portsToUse;

		RESULT_NO_SERVER = new Vector();
		RESULT_NO_SERVER.add(NetworkInterfaceConstants.NO_SERVER);
		
		try
		{
			tm = new SimpleX509TrustManager();
			HttpsURLConnection.setDefaultSSLSocketFactory(MartusUtilities.createSocketFactory(tm));
			HttpsURLConnection.setDefaultHostnameVerifier(new SimpleHostnameVerifier());
		}
		catch (Exception e)
		{
			throw new SSLSocketSetupException();
		}
	}

	// begin ServerInterface
	public Vector getServerInfo(Vector reservedForFuture)
	{
		Vector params = new Vector();
		params.add(reservedForFuture);
		Caller caller = new CallerWithTimeout(cmdGetServerInfo, params, secondsToTimeoutGetServerInfo);
		return (Vector)callServer(server, caller);
	}

	public Vector getUploadRights(String myAccountId, Vector parameters, String signature)
	{
		Vector params = new Vector();
		params.add(myAccountId);
		params.add(parameters);
		params.add(signature);
		return (Vector)callServer(server, cmdGetUploadRights, params);
	}

	public Vector getSealedBulletinIds(String myAccountId, Vector parameters, String signature)
	{
		Vector params = new Vector();
		params.add(myAccountId);
		params.add(parameters);
		params.add(signature);
		return (Vector)callServer(server, cmdGetSealedBulletinIds, params);
	}

	public Vector getDraftBulletinIds(String myAccountId, Vector parameters, String signature)
	{
		Vector params = new Vector();
		params.add(myAccountId);
		params.add(parameters);
		params.add(signature);
		return (Vector)callServer(server, cmdGetDraftBulletinIds, params);
	}

	public Vector getFieldOfficeAccountIds(String myAccountId, Vector parameters, String signature)
	{
		Vector params = new Vector();
		params.add(myAccountId);
		params.add(parameters);
		params.add(signature);
		return (Vector)callServer(server, cmdGetFieldOfficeAccountIds, params);
	}

	public Vector putBulletinChunk(String myAccountId, Vector parameters, String signature)
	{
		Vector params = new Vector();
		params.add(myAccountId);
		params.add(parameters);
		params.add(signature);
		return (Vector)callServer(server, cmdPutBulletinChunk, params);
	}

	public Vector getBulletinChunk(String myAccountId, Vector parameters, String signature)
	{
		Vector params = new Vector();
		params.add(myAccountId);
		params.add(parameters);
		params.add(signature);
		return (Vector)callServer(server, cmdGetBulletinChunk, params);
	}

	public Vector getPacket(String myAccountId, Vector parameters, String signature)
	{
		Vector params = new Vector();
		params.add(myAccountId);
		params.add(parameters);
		params.add(signature);
		return (Vector)callServer(server, cmdGetPacket, params);
	}

	public Vector deleteDraftBulletins(String myAccountId, Vector parameters, String signature)
	{
		Vector params = new Vector();
		params.add(myAccountId);
		params.add(parameters);
		params.add(signature);
		return (Vector)callServer(server, cmdDeleteDrafts, params);
	}

	public Vector putContactInfo(String myAccountId, Vector parameters, String signature)
	{
		Vector params = new Vector();
		params.add(myAccountId);
		params.add(parameters);
		params.add(signature);
		return (Vector)callServer(server, cmdPutContactInfo, params);
	}

	public Vector getNews(String myAccountId, Vector parameters, String signature)
	{
		Vector params = new Vector();
		params.add(myAccountId);
		params.add(parameters);
		params.add(signature);
		return (Vector)callServer(server, cmdGetNews, params);
	}
	
	public Vector getServerCompliance(String myAccountId, Vector parameters, String signature)
	{
		Vector params = new Vector();
		params.add(myAccountId);
		params.add(parameters);
		params.add(signature);
		return (Vector)callServer(server, cmdGetServerCompliance, params);
	}

	public Object callServer(String serverName, String method, Vector params)
	{
		Caller caller = new Caller(method, params);
		return callServer(serverName, caller);
	}
	
	private Object callServer(String serverName, Caller caller)
	{
		int numPorts = ports.length;
		int portIndexToTryNext = indexOfPortThatWorkedLast;
		for(int i=0; i < numPorts; ++i)
		{
			Vector result = caller.call(this, serverName, ports[portIndexToTryNext]);
			
			if(result == null || !result.equals(RESULT_NO_SERVER))
			{
				indexOfPortThatWorkedLast = portIndexToTryNext;
				return result;
			}

			portIndexToTryNext = (portIndexToTryNext+1) % numPorts;
		}
		return null;
	}
	
	static class Caller
	{
		public Caller(String methodToCall, Vector paramsToUse)
		{
			method = methodToCall;
			params = paramsToUse;
		}
		
		public Vector call(ClientSideNetworkHandlerUsingXmlRpc handler, String serverName, int port)
		{
			return internalCall(handler, serverName, port);
		}

		Vector internalCall(ClientSideNetworkHandlerUsingXmlRpc handler, String serverName, int port)
		{
			return handler.callServerAtPort(serverName, method, params, port);
		}
		
		String method;
		Vector params;
	}
	
	static class CallerWithTimeout extends Caller
	{
		public CallerWithTimeout(String methodToCall, Vector paramsToUse, long secondsToWait)
		{
			super(methodToCall, paramsToUse);
			timeoutMillis = secondsToWait * 1000;
		}
		
		public Vector call(ClientSideNetworkHandlerUsingXmlRpc handler, String serverName, int port)
		{
			
			BackgroundCallerTask task = new BackgroundCallerTask(this, handler, serverName, port);
			Thread background = new Thread(task);
			try
			{
				background.start();
				background.join(timeoutMillis);
			}
			catch (InterruptedException e)
			{
				System.out.println("ERROR: Server timeout");
			}
			background.interrupt();
			return task.response;
		}
		
		long timeoutMillis;
	}
	
	static class BackgroundCallerTask implements Runnable
	{
		BackgroundCallerTask(Caller callerTouse, ClientSideNetworkHandlerUsingXmlRpc handlerToUse, String serverNameToUse, int portToUse)
		{
			caller = callerTouse;
			handler = handlerToUse;
			serverName = serverNameToUse;
			port = portToUse;
			response = RESULT_NO_SERVER;
		}
			
		public void run()
		{
			response = caller.internalCall(handler, serverName, port);
		}

		Caller caller;
		ClientSideNetworkHandlerUsingXmlRpc handler;
		String serverName;
		int port;
		Vector response;
	}
	
	Vector callServerAtPort(
		String serverName,
		String method,
		Vector params,
		int port)
	{
		try
		{
			Object xmldata = executeXmlRpc(serverName, method, params, port);
			Vector result = new Vector();//(Vector)executeXmlRpc(serverName, method, params, port);
			if(xmldata instanceof Object[]){
				Object[] dataToconvert = (Object[]) xmldata;
				Vector data = new Vector();
				for (int j = 0; j < dataToconvert.length; j++){
					data.add(dataToconvert[j]);
				}
				result = data;
			}
			if(tm.getExpectedPublicKey() == null)
				throw new Exception("Trust Manager never called");
			return result;
		}
		catch (IOException e)
		{
			if(e.getMessage().startsWith("Connection"))
				return RESULT_NO_SERVER;

			if(e.getMessage().contains("RSA premaster"))
			{
				MartusLogger.log("Possible problem with RSA key size limitations");
				MartusLogger.logException(e);
				return null;
			}
			//TODO throw IOExceptions so caller can decide what to do.
			//This was added for connection refused: connect (no server connected)
			//System.out.println("ServerInterfaceXmlRpcHandler:callServer Exception=" + e);
			//e.printStackTrace();
		}
		catch (XmlRpcException e)
		{
			if(e.getMessage().indexOf("NoSuchMethodException") < 0)
			{
				System.out.println("ServerInterfaceXmlRpcHandler:callServer XmlRpcException=" + e);
				e.printStackTrace();
			}
		}
		catch (Exception e)
		{
			System.out.println("ServerInterfaceXmlRpcHandler:callServer Exception=" + e);
			e.printStackTrace();
		}
		return null;
	}
	
	public Object executeXmlRpc(String serverName, String method,
									Vector params, int port)
		throws MalformedURLException, XmlRpcException, IOException 
	{
		final String serverUrl = "https://" + serverName + ":" + port + "/RPC2";
		//System.out.println("ServerInterfaceXmlRpcHandler:callServer serverUrl=" + serverUrl);
		
		// NOTE: We **MUST** create a new XmlRpcClient for each call, because
		// there is a memory leak in apache xmlrpc 1.1 that will cause out of 
		// memory exceptions if we reuse an XmlRpcClient object
//		XmlRpcClient client = new XmlRpcClient(serverUrl);
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setServerURL(new URL(serverUrl));
		XmlRpcClient client = new XmlRpcClient();
		client.setConfig(config);
		Stopwatch sw = new Stopwatch();
		Object result = client.execute("MartusServer." + method, params);
		sw.stop();
		final int MAX_EXPECTED_TIME_MILLIS = 60 * 1000;
		if(sw.elapsed() > MAX_EXPECTED_TIME_MILLIS)
			System.out.println("SLOW SERVER: " + new Date() + " " + serverUrl + " " + method + " " + sw.elapsed()/1000 + " seconds");
		return result;
	}

	public SimpleX509TrustManager getSimpleX509TrustManager()
	{
		return tm;
	}

	static int indexOfPortThatWorkedLast = 0;
	SimpleX509TrustManager tm;
	String server;
	int[] ports;
	
	static Vector RESULT_NO_SERVER;

	private static final int secondsToTimeoutGetServerInfo = 15;
}
