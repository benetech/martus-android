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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;
import org.martus.common.ProgressMeterInterface;
import org.martus.common.VersionBuildDate;
import org.martus.common.MartusUtilities.BulletinNotFoundException;
import org.martus.common.MartusUtilities.NotYourBulletinErrorException;
import org.martus.common.MartusUtilities.ServerErrorException;
import org.martus.common.bulletin.BulletinZipUtilities;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusCrypto.MartusSignatureException;
import org.martus.common.network.BulletinRetrieverGatewayInterface;
import org.martus.common.network.NetworkInterface;
import org.martus.common.network.NetworkInterfaceConstants;
import org.martus.common.network.NetworkInterfaceXmlRpcConstants;
import org.martus.common.network.NetworkResponse;
import org.martus.common.packet.UniversalId;
import org.martus.util.StreamableBase64.InvalidBase64Exception;

public class ClientSideNetworkGateway implements BulletinRetrieverGatewayInterface
{
	public ClientSideNetworkGateway(NetworkInterface serverToUse)
	{
		server = serverToUse;
	}

	public NetworkResponse getServerInfo()
	{
		Vector parameters = new Vector();
		Vector response = server.getServerInfo(parameters);
		return new NetworkResponse(response);
	}

	public NetworkResponse getUploadRights(MartusCrypto signer, String tryMagicWord) throws
			MartusCrypto.MartusSignatureException
	{
		Vector parameters = new Vector();
		parameters.add(tryMagicWord);
		String signature = signer.createSignatureOfVectorOfStrings(parameters);
		return new NetworkResponse(server.getUploadRights(signer.getPublicKeyString(), parameters, signature));
	}

	public NetworkResponse getSealedBulletinIds(MartusCrypto signer, String authorAccountId, Vector retrieveTags) throws
			MartusCrypto.MartusSignatureException
	{
		Vector parameters = new Vector();
		parameters.add(authorAccountId);
		parameters.add(retrieveTags);
		String signature = signer.createSignatureOfVectorOfStrings(parameters);
		return new NetworkResponse(server.getSealedBulletinIds(signer.getPublicKeyString(), parameters, signature));
	}

	public NetworkResponse getDraftBulletinIds(MartusCrypto signer, String authorAccountId, Vector retrieveTags) throws
			MartusCrypto.MartusSignatureException
	{
		Vector parameters = new Vector();
		parameters.add(authorAccountId);
		parameters.add(retrieveTags);
		String signature = signer.createSignatureOfVectorOfStrings(parameters);
		return new NetworkResponse(server.getDraftBulletinIds(signer.getPublicKeyString(), parameters, signature));
	}

	public NetworkResponse getFieldOfficeAccountIds(MartusCrypto signer, String hqAccountId) throws
			MartusCrypto.MartusSignatureException
	{
		Vector parameters = new Vector();
		parameters.add(hqAccountId);
		String signature = signer.createSignatureOfVectorOfStrings(parameters);
		return new NetworkResponse(server.getFieldOfficeAccountIds(signer.getPublicKeyString(), parameters, signature));
	}

	public NetworkResponse putBulletinChunk(MartusCrypto signer, String authorAccountId, String bulletinLocalId,
			int totalSize, int chunkOffset, int chunkSize, String data) throws
			MartusCrypto.MartusSignatureException
	{
		Vector parameters = new Vector();
		parameters.add(authorAccountId);
		parameters.add(bulletinLocalId);
		parameters.add(new Integer(totalSize));
		parameters.add(new Integer(chunkOffset));
		parameters.add(new Integer(chunkSize));
		parameters.add(data);
		String signature = signer.createSignatureOfVectorOfStrings(parameters);
		return new NetworkResponse(server.putBulletinChunk(signer.getPublicKeyString(), parameters, signature));
	}

	public NetworkResponse getBulletinChunk(MartusCrypto signer, String authorAccountId, String bulletinLocalId,
					int chunkOffset, int maxChunkSize) throws
			MartusCrypto.MartusSignatureException
	{
		Vector parameters = new Vector();
		parameters.add(authorAccountId);
		parameters.add(bulletinLocalId);
		parameters.add(new Integer(chunkOffset));
		parameters.add(new Integer(maxChunkSize));
		String signature = signer.createSignatureOfVectorOfStrings(parameters);
		return new NetworkResponse(server.getBulletinChunk(signer.getPublicKeyString(), parameters, signature));
	}

	public NetworkResponse getPacket(MartusCrypto signer, String authorAccountId, String bulletinLocalId,
					String packetLocalId) throws
			MartusCrypto.MartusSignatureException
	{
		Vector parameters = new Vector();
		parameters.add(authorAccountId);
		parameters.add(bulletinLocalId);
		parameters.add(packetLocalId);
		parameters.add(NetworkInterfaceConstants.BASE_64_ENCODED);
		String signature = signer.createSignatureOfVectorOfStrings(parameters);
		return new NetworkResponse(server.getPacket(signer.getPublicKeyString(), parameters, signature));
	}

	public NetworkResponse deleteServerDraftBulletins(MartusCrypto signer,
					String authorAccountId, String[] bulletinLocalIds) throws
			MartusCrypto.MartusSignatureException
	{
		Vector parameters = new Vector();
		parameters.add(new Integer(bulletinLocalIds.length));
		for (int i = 0; i < bulletinLocalIds.length; i++)
		{
			parameters.add(bulletinLocalIds[i]);
		}
		String signature = signer.createSignatureOfVectorOfStrings(parameters);
		return new NetworkResponse(server.deleteDraftBulletins(signer.getPublicKeyString(), parameters, signature));
	}

	public NetworkResponse	putContactInfo(MartusCrypto signer, String authorAccountId, Vector parameters) throws
			MartusCrypto.MartusSignatureException
	{
		String signature = signer.createSignatureOfVectorOfStrings(parameters);
		return new NetworkResponse(server.putContactInfo(signer.getPublicKeyString(), parameters, signature));
	}

	public NetworkResponse	getNews(MartusCrypto signer, String versionLabel) throws
			MartusCrypto.MartusSignatureException
	{
		Vector parameters = new Vector();
		parameters.add(versionLabel);
		parameters.add(VersionBuildDate.getVersionBuildDate());
		String signature = signer.createSignatureOfVectorOfStrings(parameters);
		return new NetworkResponse(server.getNews(signer.getPublicKeyString(), parameters, signature));
	}

	public NetworkResponse	getServerCompliance(MartusCrypto signer) throws
			MartusCrypto.MartusSignatureException
	{
		Vector parameters = new Vector();
		String signature = signer.createSignatureOfVectorOfStrings(parameters);
		return new NetworkResponse(server.getServerCompliance(signer.getPublicKeyString(), parameters, signature));
	}
	
	static public ClientSideNetworkGateway buildGateway(String serverName, String serverPublicKey)
	{
		NetworkInterface server = buildNetworkInterface(serverName, serverPublicKey);
		if(server == null)
			return null;
		
		return new ClientSideNetworkGateway(server);
	}

	public static NetworkInterface buildNetworkInterface(String serverName, String serverPublicKey)
	{
		if(serverName.length() == 0)
			return null;
	
		try
		{
			int[] ports = NetworkInterfaceXmlRpcConstants.defaultSSLPorts;
			ClientSideNetworkHandlerUsingXmlRpc handler = new ClientSideNetworkHandlerUsingXmlRpc(serverName, ports);
			handler.getSimpleX509TrustManager().setExpectedPublicKey(serverPublicKey);
			return handler;
		}
		catch (ClientSideNetworkHandlerUsingXmlRpc.SSLSocketSetupException e)
		{
			//TODO propagate to UI and needs a test.
			e.printStackTrace();
			return null;
		}
	}

	public Vector downloadFieldOfficeAccountIds(MartusCrypto security, String myAccountId) throws ServerErrorException
	{
		try
		{
			NetworkResponse response = getFieldOfficeAccountIds(security, myAccountId);
			String resultCode = response.getResultCode();
			if(!resultCode.equals(NetworkInterfaceConstants.OK))
				throw new ServerErrorException(resultCode);
			return response.getResultVector();
		}
		catch(MartusCrypto.MartusSignatureException e)
		{
			System.out.println("ServerUtilities.getFieldOfficeAccounts: " + e);
			throw new ServerErrorException();
		}
	}

	public File retrieveBulletin(UniversalId uid, MartusCrypto security,
			int chunkSize, ProgressMeterInterface progressMeter)
			throws IOException, FileNotFoundException,
			MartusSignatureException, ServerErrorException,
			InvalidBase64Exception, NotYourBulletinErrorException,
			BulletinNotFoundException
	{
		File tempFile = File.createTempFile("$$$MartusRetrievedBulletin", null);
		tempFile.deleteOnExit();
		FileOutputStream outputStream = new FileOutputStream(tempFile);

		int masterTotalSize = BulletinZipUtilities.retrieveBulletinZipToStream(
				uid, outputStream, chunkSize, this, security, progressMeter);
		outputStream.close();
		if (tempFile.length() != masterTotalSize)
			throw new ServerErrorException(
					"bulletin totalSize didn't match data length");
		return tempFile;
	}

	final static String defaultReservedString = "";

	NetworkInterface server;
}
