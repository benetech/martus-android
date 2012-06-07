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
package org.martus.common.test;

import java.util.Vector;

import org.martus.common.HQKey;
import org.martus.common.HQKeys;
import org.martus.common.MartusXml;
import org.martus.util.TestCaseEnhanced;
import org.martus.util.xml.XmlUtilities;


public class testHQKeys extends TestCaseEnhanced
{
	public testHQKeys(String name)
	{
		super(name);
	}
	
	public void testBasics()
	{
		HQKeys hqKeys = new HQKeys();
		assertTrue(hqKeys.isEmpty());
		assertEquals(0, hqKeys.size());
		String publicKey1 = "123";
		HQKey key = new HQKey(publicKey1);
		hqKeys.add(key);
		assertEquals(1, hqKeys.size());
		assertTrue(hqKeys.containsKey(publicKey1));
		HQKey retrieved = hqKeys.get(0);
		assertEquals(key.getPublicKey(), retrieved.getPublicKey());
		assertEquals(key.getLabel(), retrieved.getLabel());
		hqKeys.remove(0);
		assertEquals(0, hqKeys.size());

		String publicKey2 = "123";
		String label2 = "abc";
		HQKey key2 = new HQKey(publicKey2, label2);
		hqKeys.add(key2);
		assertEquals(label2, hqKeys.getLabelIfPresent(key2));
	}
	
	public void testAddKeys()
	{
		HQKeys hqKeys = new HQKeys();
		String publicKey1 = "123";
		HQKey key = new HQKey(publicKey1);
		hqKeys.add(key);
		String publicKey2 = "123";
		String label2 = "abc";
		HQKey key2 = new HQKey(publicKey2, label2);
		hqKeys.add(key2);
		assertEquals(2, hqKeys.size());
		
		HQKeys newKeys = new HQKeys(hqKeys);
		assertEquals(2, newKeys.size());
		assertTrue(newKeys.containsKey(publicKey1));
		assertTrue(newKeys.containsKey(publicKey2));
		
		HQKeys newKeys2 = new HQKeys();
		newKeys2.add(hqKeys);
		assertEquals(2, newKeys.size());
		assertTrue(newKeys2.containsKey(publicKey1));
		assertTrue(newKeys2.containsKey(publicKey2));
		
	}
	
	public void testEmpty()
	{
		HQKeys hqKeys = new HQKeys();
		String xmlExpected = MartusXml.getTagStartWithNewline(HQKeys.HQ_KEYS_TAG) +
		MartusXml.getTagEnd(HQKeys.HQ_KEYS_TAG);
		assertEquals(xmlExpected, hqKeys.toString());
	}
	
	public void testXmlRepresentation()
	{
		Vector keys = new Vector();
		String key1 = "key 1";
		String label1 = "label 1";
		String key2 = "key 2";
		String label2 = "label 2 with <icky &xml stuff>";
		keys.add(new HQKey(key1, label1));
		keys.add(new HQKey(key2, label2));
		HQKeys hqKeys = new HQKeys(keys);
		String xmlExpected = MartusXml.getTagStartWithNewline(HQKeys.HQ_KEYS_TAG) +
		 MartusXml.getTagStart(HQKeys.HQ_KEY_TAG) + 
		 MartusXml.getTagStart(HQKeys.HQ_PUBLIC_KEY_TAG) + 
		 XmlUtilities.getXmlEncoded(key1) +
		 MartusXml.getTagEndWithoutNewline(HQKeys.HQ_PUBLIC_KEY_TAG) +
		 MartusXml.getTagEnd(HQKeys.HQ_KEY_TAG) +
		 MartusXml.getTagStart(HQKeys.HQ_KEY_TAG) + 
		 MartusXml.getTagStart(HQKeys.HQ_PUBLIC_KEY_TAG) + 
		 XmlUtilities.getXmlEncoded(key2) +
		 MartusXml.getTagEndWithoutNewline(HQKeys.HQ_PUBLIC_KEY_TAG) +
		 MartusXml.getTagEnd(HQKeys.HQ_KEY_TAG) +
		 MartusXml.getTagEnd(HQKeys.HQ_KEYS_TAG);
		
		assertEquals(xmlExpected, hqKeys.toString());
	}

	public void testXmlRepresentationWithLabels()
	{
		Vector keys = new Vector();
		String key1 = "key 1";
		String label1 = "label 1";
		String key2 = "key 2";
		String label2 = "label 2 with <icky &xml stuff>";
		keys.add(new HQKey(key1, label1));
		keys.add(new HQKey(key2, label2));
		HQKeys hqKeys = new HQKeys(keys);
		String xmlExpected = MartusXml.getTagStartWithNewline(HQKeys.HQ_KEYS_TAG) +
		 MartusXml.getTagStart(HQKeys.HQ_KEY_TAG) + 
		 MartusXml.getTagStart(HQKeys.HQ_PUBLIC_KEY_TAG) + 
		 XmlUtilities.getXmlEncoded(key1) +
		 MartusXml.getTagEndWithoutNewline(HQKeys.HQ_PUBLIC_KEY_TAG) +
		 MartusXml.getTagStart(HQKeys.HQ_LABEL_TAG) + 
		 XmlUtilities.getXmlEncoded(label1) +
		 MartusXml.getTagEndWithoutNewline(HQKeys.HQ_LABEL_TAG) +
		 MartusXml.getTagEnd(HQKeys.HQ_KEY_TAG) +
		 MartusXml.getTagStart(HQKeys.HQ_KEY_TAG) + 
		 MartusXml.getTagStart(HQKeys.HQ_PUBLIC_KEY_TAG) + 
		 XmlUtilities.getXmlEncoded(key2) +
		 MartusXml.getTagEndWithoutNewline(HQKeys.HQ_PUBLIC_KEY_TAG) +
		 MartusXml.getTagStart(HQKeys.HQ_LABEL_TAG) + 
		 XmlUtilities.getXmlEncoded(label2) +
		 MartusXml.getTagEndWithoutNewline(HQKeys.HQ_LABEL_TAG) +
		 MartusXml.getTagEnd(HQKeys.HQ_KEY_TAG) +
		 MartusXml.getTagEnd(HQKeys.HQ_KEYS_TAG);
		
		assertEquals(xmlExpected, hqKeys.toStringWithLabel());
	}

	
	public void testParseXml() throws Exception
	{
		Vector keys = new Vector();
		String key1 = "key 1";
		String label1 = "label 1";
		String key2 = "key 2";
		String label2 = "label 2";
		keys.add(new HQKey(key1, label1));
		keys.add(new HQKey(key2, label2));
		HQKeys hqKeys = new HQKeys(keys);
		
		Vector newKeys = HQKeys.parseXml(hqKeys.toString());
		HQKeys hqKeys2 = new HQKeys(newKeys);
		
		assertEquals(hqKeys.toString(), hqKeys2.toString());
	}

	
}
