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

import org.martus.common.bulletin.BulletinConstants;
import org.martus.common.database.DatabaseKey;
import org.martus.common.packet.UniversalId;
import org.martus.util.*;



public class TestDatabaseKey extends TestCaseEnhanced
{
	public TestDatabaseKey(String name)
	{
		super(name);
	}

	public void TRACE(String text)
	{
		//System.out.println(text);
	}

	public void testConstructors() throws Exception
	{
		UniversalId uid1 = UniversalId.createDummyUniversalId();
		DatabaseKey key1 = DatabaseKey.createDraftKey(uid1);
		assertEquals("bad uid1?", uid1, key1.getUniversalId());
		assertEquals("not draft?", true, key1.isDraft());

		DatabaseKey key2 = DatabaseKey.createSealedKey(uid1);
		assertEquals("bad uid2?", uid1, key2.getUniversalId());
		assertEquals("not sealed?", true, key2.isSealed());

		DatabaseKey key3 = DatabaseKey.createLegacyKey(uid1);
		assertEquals("bad uid3?", uid1, key3.getUniversalId());
		assertEquals("draft?", false, key3.isDraft());
		assertEquals("not sealed?", true, key3.isSealed());

		DatabaseKey keySealed = DatabaseKey.createKey(uid1, BulletinConstants.STATUSSEALED);
		assertEquals("bad keySealed?", uid1, keySealed.getUniversalId());
		assertEquals("draft?", false, keySealed.isDraft());
		assertEquals("not sealed?", true, keySealed.isSealed());

		DatabaseKey keyDraft = DatabaseKey.createKey(uid1, BulletinConstants.STATUSDRAFT);
		assertEquals("bad keyDraft?", uid1, keyDraft.getUniversalId());
		assertEquals("not draft?", true, keyDraft.isDraft());
		assertEquals("sealed?", false, keyDraft.isSealed());
	}

	public void testEqualsStrings() throws Exception
	{
		UniversalId uid1 = UniversalId.createDummyUniversalId();
		UniversalId uid2 = UniversalId.createDummyUniversalId();

		DatabaseKey key1 = DatabaseKey.createSealedKey(uid1);
		DatabaseKey key2 = DatabaseKey.createSealedKey(UniversalId.createFromAccountAndLocalId(uid1.getAccountId(), uid1.getLocalId()));
		DatabaseKey key3 = DatabaseKey.createSealedKey(uid2);
		DatabaseKey key4 = DatabaseKey.createSealedKey(uid1);
		key4.setDraft();
		assertEquals("self should match", key1, key1);
		assertEquals("never match null", false, key1.equals(null));
		assertEquals("never match uid", false, key1.equals(uid1));
		assertEquals("Keys should match", key1, key2);
		assertEquals("symmetrical equals", key2, key1);
		assertEquals("should not match", false, key1.equals(key3));
		assertEquals("symmetrical not equals", false, key3.equals(key1));
		assertNotEquals("status ignored?", key4, key1);

		assertEquals("hash self should match", key1.hashCode(), key1.hashCode());
		assertEquals("hash Keys should match", key1.hashCode(), key2.hashCode());
		assertNotEquals("hash didn't use status?", key1.hashCode(), key4.hashCode());
	}

	public void testEquals() throws Exception
	{
		UniversalId uid = UniversalId.createDummyUniversalId();

		DatabaseKey key1 = DatabaseKey.createSealedKey(uid);
		DatabaseKey key2 = DatabaseKey.createSealedKey(uid);
		DatabaseKey key3 = DatabaseKey.createSealedKey(UniversalId.createDummyUniversalId());
		assertEquals("self should match", key1, key1);
		assertEquals("never match null", false, key1.equals(null));
		assertEquals("never match string", false, key1.equals(uid));
		assertEquals("Keys should match", key1, key2);
		assertEquals("symmetrical equals", key2, key1);
		assertEquals("should not match", false, key1.equals(key3));
		assertEquals("symmetrical not equals", false, key3.equals(key1));

		assertEquals("hash self should match", key1.hashCode(), key1.hashCode());
		assertEquals("hash Keys should match", key1.hashCode(), key2.hashCode());
	}

	public void testGetAccount() throws Exception
	{
		UniversalId uid = UniversalId.createDummyUniversalId();
		DatabaseKey key = DatabaseKey.createSealedKey(uid);
		assertEquals("wrong account?", uid.getAccountId(), key.getAccountId());
	}

	public void testStatus() throws Exception
	{
		UniversalId uid = UniversalId.createDummyUniversalId();
		DatabaseKey key = DatabaseKey.createSealedKey(uid);
		assertEquals("Default not sealed?", true, key.isSealed());
		assertEquals("Default was draft?", false, key.isDraft());
		key.setDraft();
		assertEquals("Sealed still set?", false, key.isSealed());
		assertEquals("Draft not set?", true, key.isDraft());
		key.setSealed();
		assertEquals("Sealed not set?", true, key.isSealed());
		assertEquals("Draft still set?", false, key.isDraft());

	}
}
