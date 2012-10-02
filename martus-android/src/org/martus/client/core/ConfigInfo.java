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

package org.martus.client.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.martus.common.LegacyCustomFields;
import org.martus.common.fieldspec.StandardFieldSpecs;

public class ConfigInfo
{
	public ConfigInfo()
	{
		clear();
	}

	public boolean hasEnoughContactInfo()
	{
		if(author != null && author.length() > 0)
			return true;

		if(organization != null && organization.length() > 0)
			return true;

		return false;
	}

	public void setAuthor(String newSource)		{ author = newSource; }
	public void setOrganization(String newOrg)		{ organization = newOrg; }
	public void setEmail(String newEmail)			{ email = newEmail; }
	public void setWebPage(String newWebPage)		{ webPage = newWebPage; }
	public void setPhone(String newPhone)			{ phone = newPhone; }
	public void setAddress(String newAddress)		{ address = newAddress; }
	public void setServerName(String newServerName){ serverName = newServerName; }
	public void setServerPublicKey(String newServerPublicKey){serverPublicKey = newServerPublicKey; }
	public void setTemplateDetails(String newTemplateDetails){ templateDetails = newTemplateDetails; }
	public void setLegacyHQKey(String newHQKey)			{ legacyHQKey = newHQKey; }
	public void setSendContactInfoToServer(boolean newSendContactInfoToServer) {sendContactInfoToServer = newSendContactInfoToServer; }
	public void setServerCompliance(String newCompliance) {serverCompliance = newCompliance;}
	public void setCustomFieldLegacySpecs(String newSpecs)	{customFieldLegacySpecs = newSpecs;}
	public void setForceBulletinsAllPrivate(boolean newForceBulletinsAllPrivate)	{forceBulletinsAllPrivate = newForceBulletinsAllPrivate; }
	public void setBackedUpKeypairEncrypted(boolean newBackedUpKeypairEncrypted)	{backedUpKeypairEncrypted = newBackedUpKeypairEncrypted; }
	public void setBackedUpKeypairShare(boolean newBackedUpKeypairShare)	{backedUpKeypairShare = newBackedUpKeypairShare; }
	public void setAllHQKeysXml(String allHQKeysXml){this.allHQKeysXml = allHQKeysXml;}
	public void setBulletinVersioningAware(boolean newBulletinVersioningAware){this.bulletinVersioningAware = newBulletinVersioningAware;}
	public void setDefaultHQKeysXml(String defaultHQKeysXml){this.defaultHQKeysXml = defaultHQKeysXml;}
	public void setCheckForFieldOfficeBulletins(boolean newCheckForBulletins){checkForFieldOfficeBulletins = newCheckForBulletins;}
	public void setCustomFieldTopSectionXml(String newXml)	{customFieldTopSectionXml = newXml;}
	public void setCustomFieldBottomSectionXml(String newXml)	{customFieldBottomSectionXml = newXml;}

	public void clearHQKey()						{ legacyHQKey = ""; }
	public void clearPromptUserRequestSendToServer() { mustAskUserToSendToServer = false; }

	public short getVersion()			{ return version; }
	public String getAuthor()			{ return author; }
	public String getOrganization()	{ return organization; }
	public String getEmail()			{ return email; }
	public String getWebPage()			{ return webPage; }
	public String getPhone()			{ return phone; }
	public String getAddress()			{ return address; }
	public String getServerName()		{ return serverName; }
	public String getServerPublicKey()	{ return serverPublicKey; }
	public String getTemplateDetails() { return templateDetails; }
	public String getLegacyHQKey() 			{ return legacyHQKey; }
	public boolean shouldContactInfoBeSentToServer() { return sendContactInfoToServer; }
	public boolean promptUserRequestSendToServer() { return mustAskUserToSendToServer; }
	public String getServerCompliance() {return serverCompliance;}
	public String getCustomFieldLegacySpecs() {return customFieldLegacySpecs;}
	public boolean shouldForceBulletinsAllPrivate()	{ return forceBulletinsAllPrivate;}
	public boolean hasUserBackedUpKeypairEncrypted()	{ return backedUpKeypairEncrypted;}
	public boolean hasUserBackedUpKeypairShare()	{ return backedUpKeypairShare;}
	public String getAllHQKeysXml()		{return allHQKeysXml;}
	public boolean isBulletinVersioningAware()	{return bulletinVersioningAware;}
	public String getDefaultHQKeysXml()		{return defaultHQKeysXml;}
	public boolean getCheckForFieldOfficeBulletins() {return checkForFieldOfficeBulletins;}
	public String getCustomFieldTopSectionXml()	{return customFieldTopSectionXml;}
	public String getCustomFieldBottomSectionXml() {return customFieldBottomSectionXml;}
	
	public boolean isServerConfigured()
	{
		return (serverName.length()>0 && serverPublicKey.length()>0);
	}
	
	public boolean isNewVersion()
	{
		return version > VERSION;
	}
	
	public void clear()
	{
		version = VERSION;
		author = "";
		organization = "";
		email = "";
		webPage = "";
		phone = "";
		address = "";
		serverName = "";
		serverPublicKey="";
		templateDetails = "";
		legacyHQKey = "";
		sendContactInfoToServer = false;
		mustAskUserToSendToServer = false;
		serverCompliance = "";
		customFieldLegacySpecs = LegacyCustomFields.buildFieldListString(StandardFieldSpecs.getDefaultTopSetionFieldSpecs());
		forceBulletinsAllPrivate = false;
		backedUpKeypairEncrypted = false;
		backedUpKeypairShare = false;
		allHQKeysXml = "";
		bulletinVersioningAware = true;
		defaultHQKeysXml = "";
		customFieldTopSectionXml = "";
		customFieldBottomSectionXml = "";
	}

	public static ConfigInfo load(InputStream inputStream) throws IOException
	{
		ConfigInfo loaded =  new ConfigInfo();

		DataInputStream in = new DataInputStream(inputStream);
		try
		{
			loaded.version = in.readShort();
			loaded.author = in.readUTF();
			loaded.organization = in.readUTF();
			loaded.email = in.readUTF();
			loaded.webPage = in.readUTF();
			loaded.phone = in.readUTF();
			loaded.address = in.readUTF();
			loaded.serverName = in.readUTF();
			loaded.templateDetails = in.readUTF();
			loaded.legacyHQKey = in.readUTF();
			loaded.serverPublicKey = in.readUTF();
						
			if(loaded.version >= 2)
				loaded.sendContactInfoToServer = in.readBoolean();
			else
				loaded.mustAskUserToSendToServer = true;
				
			if(loaded.version >= 4)
				loaded.serverCompliance = in.readUTF();
				
			if(loaded.version >= 5)
				loaded.customFieldLegacySpecs = in.readUTF();
				
			if(loaded.version >= 6)
				loaded.customFieldTopSectionXml = in.readUTF();

			if(loaded.version >= 7)
				loaded.forceBulletinsAllPrivate = in.readBoolean();

			if(loaded.version >= 8)
			{
				loaded.backedUpKeypairEncrypted = in.readBoolean();
				loaded.backedUpKeypairShare = in.readBoolean();
			}
			if(loaded.version >= 9)
				loaded.allHQKeysXml = in.readUTF();
			
			if(loaded.version >= 10)
				loaded.bulletinVersioningAware = in.readBoolean();
			else
				loaded.bulletinVersioningAware = false;

			if(loaded.version >= 11)
				loaded.defaultHQKeysXml = in.readUTF();

			if(loaded.version >= 12)
				loaded.customFieldBottomSectionXml = in.readUTF();

			if(loaded.version >= 13)
				loaded.checkForFieldOfficeBulletins = in.readBoolean();

			if(loaded.version >= 14)
			{
				loaded.customFieldTopSectionXml = readLongString(in);
				loaded.customFieldBottomSectionXml = readLongString(in);
			}
		}
		finally
		{
			in.close();
		}
		return loaded;
	}

	public void save(OutputStream outputStream) throws IOException
	{
		DataOutputStream out = new DataOutputStream(outputStream);
		try
		{
			out.writeShort(VERSION);
			out.writeUTF(author);
			out.writeUTF(organization);
			out.writeUTF(email);
			out.writeUTF(webPage);
			out.writeUTF(phone);
			out.writeUTF(address);
			out.writeUTF(serverName);
			out.writeUTF(templateDetails);
			out.writeUTF(legacyHQKey);
			out.writeUTF(serverPublicKey);
			out.writeBoolean(sendContactInfoToServer);
			out.writeUTF(serverCompliance);
			out.writeUTF(customFieldLegacySpecs);
			out.writeUTF("");
			out.writeBoolean(forceBulletinsAllPrivate);
			out.writeBoolean(backedUpKeypairEncrypted);
			out.writeBoolean(backedUpKeypairShare);
			out.writeUTF(allHQKeysXml);
			out.writeBoolean(bulletinVersioningAware);
			out.writeUTF(defaultHQKeysXml);
			out.writeUTF("");
			out.writeBoolean(checkForFieldOfficeBulletins);
			writeLongString(out, customFieldTopSectionXml);
			writeLongString(out, customFieldBottomSectionXml);
		}
		finally
		{
			out.close();
		}
	}
	
	public static void writeLongString(DataOutputStream out, String data) throws IOException
	{
		byte[] bytes = data.getBytes("UTF-8");
		out.writeInt(bytes.length);
		for(int i = 0; i < bytes.length; ++i)
			out.writeByte(bytes[i]);
	}
	
	public static String readLongString(DataInputStream in) throws IOException
	{
		int length = in.readInt();
		byte[] bytes = new byte[length];
		for(int i = 0; i < bytes.length; ++i)
			bytes[i] = in.readByte();
		return new String(bytes, "UTF-8");
	}
	
	private boolean mustAskUserToSendToServer;

	public static final short VERSION = 14;
	//Version 1
	private short version;
	private String author;
	private String organization;
	private String email;
	private String webPage;
	private String phone;
	private String address;
	private String serverName;
	private String serverPublicKey;
	private String templateDetails;
	private String legacyHQKey;
	//Version 2
	private boolean sendContactInfoToServer;
	//Version 3 flag to indicate AccountMap.txt is signed.
	//Version 4
	private String serverCompliance;
	//Version 5
	private String customFieldLegacySpecs;
	//Version 6
		// was: private String legacyCustomFieldTopSectionXml;
	//Version 7
	private boolean forceBulletinsAllPrivate;
	//Version 8
	private boolean backedUpKeypairEncrypted;
	private boolean backedUpKeypairShare;
	//Version 9
	private String allHQKeysXml;
	//Version 10 
	private boolean bulletinVersioningAware;
	//Version 11
	private String defaultHQKeysXml;
	//Version 12
		// was: private String legacyCustomFieldBottomSectionXml;
	//Version 13
	private boolean checkForFieldOfficeBulletins;
	//Version 14
	private String customFieldTopSectionXml;
	private String customFieldBottomSectionXml;

}
