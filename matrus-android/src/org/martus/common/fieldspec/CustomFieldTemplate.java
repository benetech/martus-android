/*

The Martus(tm) free, social justice documentation and
monitoring software. Copyright (C) 2005-2007, Beneficent
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
package org.martus.common.fieldspec;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import org.martus.common.FieldCollection;
import org.martus.common.FieldSpecCollection;
import org.martus.common.FieldCollection.CustomFieldsParseException;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusCrypto.AuthorizationFailedException;
import org.martus.common.crypto.MartusCrypto.MartusSignatureException;

public class CustomFieldTemplate
{
	public CustomFieldTemplate()
	{
		super();
		clearData();
	}

	private void clearData()
	{
		errors = new Vector();
		xmlImportedTopSectionText = "";
		xmlImportedBottomSectionText = "";
	}
	
	public class FutureVersionException extends Exception
	{
		
	}

	
	public boolean importTemplate(MartusCrypto security, File fileToImport, Vector authroizedKeys) throws FutureVersionException
	{
		try
		{
			clearData();
			String templateXMLToImportTopSection = "";
			String templateXMLToImportBottomSection = "";
			FileInputStream in = new FileInputStream(fileToImport);
			byte[] dataBundle = new byte[(int)fileToImport.length()];
			in.read(dataBundle);
			in.close();
			byte[] dataBundleTopSection;
			if(isLegacyTemplateFile(fileToImport))
			{
				dataBundleTopSection = dataBundle;
				FieldSpecCollection defaultBottomFields = StandardFieldSpecs.getDefaultBottomSectionFieldSpecs();
				templateXMLToImportBottomSection = defaultBottomFields.toXml();
			}
			else
			{
				ByteArrayInputStream bIn = new ByteArrayInputStream(dataBundle);
				DataInputStream bundleIn = new DataInputStream(bIn);
				bundleIn.skip(versionHeader.length()); //ignore header
				int templateVersion = bundleIn.readInt();
				if(templateVersion > exportVersionNumber)
					throw new FutureVersionException();
				int topSectionBundleLength = bundleIn.readInt();
				int bottomSectionBundleLength = bundleIn.readInt();
				dataBundleTopSection = new byte[topSectionBundleLength];
				byte[] dataBundleBottomSection = new byte[bottomSectionBundleLength];
				bundleIn.read(dataBundleTopSection,0, topSectionBundleLength);
				bundleIn.read(dataBundleBottomSection,0, bottomSectionBundleLength);
				byte[] xmlBytesBottomSection = security.extractFromSignedBundle(dataBundleBottomSection, authroizedKeys);
				templateXMLToImportBottomSection = new String(xmlBytesBottomSection, "UTF-8");
			}

			byte[] xmlBytesTopSection = security.extractFromSignedBundle(dataBundleTopSection, authroizedKeys);
			templateXMLToImportTopSection = new String(xmlBytesTopSection, "UTF-8");
			
			if(isvalidTemplateXml(templateXMLToImportTopSection, templateXMLToImportBottomSection))
			{
				xmlImportedTopSectionText = templateXMLToImportTopSection;
				xmlImportedBottomSectionText = templateXMLToImportBottomSection;
				return true;
			}
		}
		catch(IOException e)
		{
			errors.add(CustomFieldError.errorIO(e.getMessage()));
			
		}
		catch(MartusSignatureException e)
		{
			errors.add(CustomFieldError.errorSignature());
		}
		catch(AuthorizationFailedException e)
		{
			errors.add(CustomFieldError.errorUnauthorizedKey());
		}
		return false;
	}
	
	public boolean isLegacyTemplateFile(File fileToImport) throws IOException
	{
		FileInputStream in = new FileInputStream(fileToImport);
		byte[] versionHeaderInBytes = new byte[versionHeader.length()];
		in.read(versionHeaderInBytes);
		in.close();
		String versionHeaderInString = new String(versionHeaderInBytes);
		return !versionHeaderInString.equals(versionHeader);
	}
	
	public boolean ExportTemplate(MartusCrypto security, File fileToExportXml, String xmlToExportTopSection, String xmlToExportBottomSection)
	{
		clearData();
		if(!isvalidTemplateXml(xmlToExportTopSection, xmlToExportBottomSection))
			return false;
		try
		{
			FileOutputStream out = new FileOutputStream(fileToExportXml);
			DataOutputStream dataOut = new DataOutputStream(out);
			dataOut.write(versionHeader.getBytes());
			dataOut.writeInt(exportVersionNumber);
			byte[] signedBundleTopSection = security.createSignedBundle(xmlToExportTopSection.getBytes("UTF-8"));
			byte[] signedBundleBottomSection = security.createSignedBundle(xmlToExportBottomSection.getBytes("UTF-8"));
			dataOut.writeInt(signedBundleTopSection.length);
			dataOut.writeInt(signedBundleBottomSection.length);
			dataOut.write(signedBundleTopSection);
			dataOut.write(signedBundleBottomSection);
			dataOut.flush();
			dataOut.close();
			out.flush();
			out.close();
			return true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean isvalidTemplateXml(String xmlToValidateTopSection, String xmlToValidateBottomSection)
	{
		try
		{
			FieldSpecCollection newSpecsTopSection = FieldCollection.parseXml(xmlToValidateTopSection);
			FieldSpecCollection newSpecsBottomSection = FieldCollection.parseXml(xmlToValidateBottomSection);
			CustomFieldSpecValidator checker = new CustomFieldSpecValidator(newSpecsTopSection, newSpecsBottomSection);
			if(checker.isValid())
				return true;
			errors.addAll(checker.getAllErrors());
		}
		catch (InvalidIsoDateException e)
		{
			System.out.println("isValidTemplateXml");
			e.printStackTrace();
			errors.add(CustomFieldError.errorInvalidIsoDate(e.getTag(), e.getLabel(), e.getType()));
		}
		catch (CustomFieldsParseException e)
		{
			System.out.println("isValidTemplateXml");
			e.printStackTrace();
			errors.add(CustomFieldError.errorParseXml(e.getMessage()));
		}
		return false;
	}
	
	public Vector getErrors()
	{
		return errors;
	}
	
	public String getImportedTopSectionText()
	{
		return xmlImportedTopSectionText;
	}
	
	public String getImportedBottomSectionText()
	{
		return xmlImportedBottomSectionText;
	}

	private Vector errors;
	private String xmlImportedTopSectionText;
	private String xmlImportedBottomSectionText;
	public static final String versionHeader = "Export Version Number:";
	public static final int exportVersionNumber = 2; 
	
}
