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

import java.util.Iterator;
import java.util.Vector;

import org.martus.util.xml.SimpleXmlDefaultLoader;
import org.martus.util.xml.SimpleXmlMapLoader;
import org.martus.util.xml.SimpleXmlParser;
import org.martus.util.xml.XmlUtilities;
import org.xml.sax.SAXParseException;


public class HQKeys
{
	public HQKeys()
	{
		hqKeys = new Vector();
	}
	
	public HQKeys(Vector keysToUse)
	{
		hqKeys = keysToUse;
	}
	
	public HQKeys(HQKey key) 
	{
		hqKeys = new Vector();
		add(key);
	}
	
	public HQKeys(HQKeys keys) 
	{
		hqKeys = new Vector();
		add(keys);
	}
	

	public HQKeys(String xml) throws HQsException
	{
		hqKeys = parseXml(xml);	
	}
	
	public boolean isEmpty()
	{
		return hqKeys.isEmpty();
	}
	
	public int size()
	{
		return hqKeys.size();
	}
	
	public void add(HQKey keyToAdd)
	{
		hqKeys.add(keyToAdd);
	}
	
	public void add(HQKeys keysToAdd)
	{
		for(int i = 0; i < keysToAdd.size(); ++i)
		{
			add(keysToAdd.get(i));
		}
	}
	
	public void remove(int index)
	{
		hqKeys.remove(index);
	}
	
	public void clear()
	{
		hqKeys.clear();
	}
	
	public HQKey get(int index)
	{
		return (HQKey)hqKeys.get(index);
	}
	
	public String toString()
	{
		return getXMLRepresntation(DONT_INCLUDE_LABEL);
	}

	public String toStringWithLabel()
	{
		return getXMLRepresntation(INCLUDE_LABEL);
	}
	
	private String getXMLRepresntation(boolean includeLabel)
	{
		String xmlRepresentation = MartusXml.getTagStartWithNewline(HQ_KEYS_TAG);
		for(int i = 0; i < hqKeys.size(); ++i)
		{
			xmlRepresentation += MartusXml.getTagStart(HQ_KEY_TAG);
			xmlRepresentation += MartusXml.getTagStart(HQ_PUBLIC_KEY_TAG);
			xmlRepresentation += ((HQKey)hqKeys.get(i)).getPublicKey();
			xmlRepresentation += MartusXml.getTagEndWithoutNewline(HQ_PUBLIC_KEY_TAG);
			if(includeLabel)
			{
				xmlRepresentation += MartusXml.getTagStart(HQ_LABEL_TAG);
				xmlRepresentation += XmlUtilities.getXmlEncoded(((HQKey)hqKeys.get(i)).getLabel());
				xmlRepresentation += MartusXml.getTagEndWithoutNewline(HQ_LABEL_TAG);
			}
			xmlRepresentation += MartusXml.getTagEnd(HQ_KEY_TAG);
		}
		xmlRepresentation += MartusXml.getTagEnd(HQ_KEYS_TAG);
		
		return xmlRepresentation;
	}

	public static class HQsException extends Exception 
	{
	}

	public boolean containsKey(String publicKey)
	{
		for (Iterator iter = hqKeys.iterator(); iter.hasNext();)
		{
			HQKey key = (HQKey) iter.next();
			if(key.getPublicKey().equals(publicKey))
				return true;
		}
		return false;
	}

	public String getLabelIfPresent(HQKey hqKey)
	{
		String publicKey = hqKey.getPublicKey();
		for (Iterator iter = hqKeys.iterator(); iter.hasNext();)
		{
			HQKey key = (HQKey) iter.next();
			if(key.getPublicKey().equals(publicKey))
				return key.getLabel();
		}
		return "";
	}
	
	public boolean contains(HQKey key)
	{
		return hqKeys.contains(key);
	}

	public static Vector parseXml(String xml) throws HQsException
	{
		Vector hQs = new Vector();
		if(xml.length() == 0)
			return hQs;
		XmlHQsLoader loader = new XmlHQsLoader(hQs);
		try
		{
			SimpleXmlParser.parse(loader, xml);
			return hQs;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new HQsException();
		}
	}
	
	public static class XmlHQsLoader extends SimpleXmlDefaultLoader
	{
		public XmlHQsLoader(Vector hqKeys)
		{
			super(HQ_KEYS_TAG);
			keys = hqKeys;
		}
		
		public SimpleXmlDefaultLoader startElement(String tag)
			throws SAXParseException
		{
			if(tag.equals(HQ_KEY_TAG))
				return new SimpleXmlMapLoader(tag);
			return super.startElement(tag);
		}

		public void endElement(String tag, SimpleXmlDefaultLoader ended)
			throws SAXParseException
		{
			SimpleXmlMapLoader loader = (SimpleXmlMapLoader)ended;
			String publicCode = loader.get(HQ_PUBLIC_KEY_TAG);
			String label = loader.get(HQ_LABEL_TAG);
			HQKey key = new HQKey(publicCode, label);
			keys.add(key);
		}
		Vector keys;
	}

	public static final String HQ_KEYS_TAG = "HQs";
	public static final String HQ_KEY_TAG = "HQ";
	public static final String HQ_PUBLIC_KEY_TAG = "PublicKey";
	public static final String HQ_LABEL_TAG = "Label";
	private final boolean DONT_INCLUDE_LABEL = false;
	private final boolean INCLUDE_LABEL = true;
	Vector hqKeys;
}
