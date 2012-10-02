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

package org.martus.client.bulletinstore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.martus.common.FieldSpecCollection;
import org.martus.common.MartusLogger;
import org.martus.common.MartusUtilities.FileVerificationException;
import org.martus.common.bulletin.Bulletin;
import org.martus.common.bulletinstore.BulletinStore;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusCrypto.MartusSignatureException;
import org.martus.common.database.ClientFileDatabase;
import org.martus.common.database.Database;
import org.martus.common.database.DatabaseKey;
import org.martus.common.database.FileDatabase.MissingAccountMapException;
import org.martus.common.database.FileDatabase.MissingAccountMapSignatureException;
import org.martus.common.fieldspec.StandardFieldSpecs;
import org.martus.common.packet.UniversalId;


/*
	This class represents a collection of bulletins
	(and also a collection of folders) stored on the
	client pc.

	It is responsible for managing the lifetimes of
	both bulletins and folders, including saving and
	loading them to/from disk.
*/
public class ClientBulletinStore extends BulletinStore
{
	public ClientBulletinStore(MartusCrypto cryptoToUse)
	{
		setSignatureGenerator(cryptoToUse);
	}

	public void doAfterSigninInitialization(File dataRootDirectory) throws FileVerificationException, MissingAccountMapException, MissingAccountMapSignatureException
	{
		File dbDirectory = new File(dataRootDirectory, "packets");
		Database db = new ClientFileDatabase(dbDirectory, getSignatureGenerator());
		doAfterSigninInitialization(dataRootDirectory, db);
	}

	public void doAfterSigninInitialization(File dataRootDirectory, Database db) throws FileVerificationException, MissingAccountMapException, MissingAccountMapSignatureException
	{
		super.doAfterSigninInitialization(dataRootDirectory, db);


		topSectionFieldSpecs = StandardFieldSpecs.getDefaultTopSetionFieldSpecs();
		bottomSectionFieldSpecs = StandardFieldSpecs.getDefaultBottomSectionFieldSpecs();

		loadCache();

		File obsoleteCacheFile = new File(getStoreRootDir(), OBSOLETE_CACHE_FILE_NAME);
		obsoleteCacheFile.delete();

	}

	public void prepareToExitNormally() throws Exception
	{
		MartusLogger.logBeginProcess("saveSessionKeyCache");
		saveBulletinDataCache();
		MartusLogger.logEndProcess("saveSessionKeyCache");

		MartusLogger.logBeginProcess("saveFieldSpecCache");
		MartusLogger.logEndProcess("saveFieldSpecCache");
	}


	public boolean mustEncryptPublicData()
	{
		return getDatabase().mustEncryptLocalData();
	}

	public boolean isMyBulletin(UniversalId uid)
	{
		return(uid.getAccountId().equals(getAccountId()));
	}


	public void deleteAllData() throws Exception
	{
		super.deleteAllData();
	}


	public void scrubAllData() throws Exception
	{
		class PacketScrubber implements Database.PacketVisitor
		{
			PacketScrubber(Database databaseToUse)
			{
				db = databaseToUse;
			}

			public void visit(DatabaseKey key)
			{
				try
				{
					db.scrubRecord(key);
					db.discardRecord(key);
					revisionWasRemoved(key.getUniversalId());
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}

			Database db;
		}

		PacketScrubber ac = new PacketScrubber(getWriteableDatabase());
		getDatabase().visitAllRecords(ac);
	}

	public void signAccountMap() throws MartusSignatureException, IOException
	{
		getWriteableDatabase().signAccountMap();
	}

	public FieldSpecCollection getBottomSectionFieldSpecs()
	{
		return bottomSectionFieldSpecs;
	}

	public FieldSpecCollection getTopSectionFieldSpecs()
	{
		return topSectionFieldSpecs;
	}


	public void setTopSectionFieldSpecs(FieldSpecCollection newFieldSpecs)
	{
		topSectionFieldSpecs = newFieldSpecs;
	}

	public void setBottomSectionFieldSpecs(FieldSpecCollection newFieldSpecs)
	{
		bottomSectionFieldSpecs = newFieldSpecs;
	}


	protected void loadCache()
	{
		//System.out.println("BulletinStore.loadCache");
		File cacheFile = getCacheFileForAccount(getStoreRootDir());
		if(!cacheFile.exists())
			return;

		byte[] sessionKeyCache = new byte[(int)cacheFile.length()];
		try
		{
			FileInputStream in = new FileInputStream(cacheFile);
			in.read(sessionKeyCache);
			in.close();
			getSignatureGenerator().setSessionKeyCache(sessionKeyCache);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			cacheFile.delete();
		}
	}

	protected void saveBulletinDataCache()
	{
		//System.out.println("BulletinStore.saveCache");
		try
		{
			byte[] sessionKeyCache = getSignatureGenerator().getSessionKeyCache();
			File cacheFile = new File(getStoreRootDir(), CACHE_FILE_NAME);
			FileOutputStream out = new FileOutputStream(cacheFile);
			out.write(sessionKeyCache);
			out.close();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public static File getCacheFileForAccount(File accountDir)
	{
		return new File(accountDir, CACHE_FILE_NAME);
	}

	private File getFieldSpecCacheFile()
	{
		return new File(getStoreRootDir(), FIELD_SPEC_CACHE_FILE_NAME);
	}

	public boolean bulletinHasCurrentFieldSpecs(Bulletin b)
	{
		return (b.getTopSectionFieldSpecs().equals(getTopSectionFieldSpecs()) &&
				b.getBottomSectionFieldSpecs().equals(getBottomSectionFieldSpecs()) );
	}

	public Bulletin createEmptyBulletin() throws Exception
	{
		return createEmptyBulletin(getTopSectionFieldSpecs(), getBottomSectionFieldSpecs());
	}

	public Bulletin createEmptyBulletin(FieldSpecCollection topSectionSpecs, FieldSpecCollection bottomSectionSpecs) throws Exception
	{
		Bulletin b = new Bulletin(getSignatureGenerator(), topSectionSpecs, bottomSectionSpecs);
		return b;
	}

	public Bulletin createEmptyClone(Bulletin original) throws Exception
	{
		FieldSpecCollection topSectionSpecs = original.getTopSectionFieldSpecs();
		FieldSpecCollection bottomSectionSpecs = original.getBottomSectionFieldSpecs();
		return createEmptyCloneWithFields(original, topSectionSpecs, bottomSectionSpecs);
	}

	public Bulletin createEmptyCloneWithFields(Bulletin original, FieldSpecCollection publicSpecs, FieldSpecCollection privateSpecs) throws Exception
	{
		UniversalId headerUid = original.getUniversalId();
		UniversalId publicDataUid = original.getFieldDataPacket().getUniversalId();
		UniversalId privateDataUid = original.getPrivateFieldDataPacket().getUniversalId();
		return new Bulletin(getSignatureGenerator(), headerUid, publicDataUid, privateDataUid, publicSpecs, privateSpecs);
	}

	public Bulletin createNewDraft(Bulletin original, FieldSpecCollection topSectionFieldSpecsToUse, FieldSpecCollection bottomSectionFieldSpecsToUse) throws Exception
	{
		Bulletin newDraftBulletin = createEmptyBulletin(topSectionFieldSpecsToUse, bottomSectionFieldSpecsToUse);
		newDraftBulletin.createDraftCopyOf(original, getDatabase());
		return newDraftBulletin;
	}

	public Bulletin createDraftClone(Bulletin original, FieldSpecCollection topSectionFieldSpecsToUse, FieldSpecCollection bottomSectionFieldSpecsToUse) throws Exception
	{
		Bulletin clone = createEmptyCloneWithFields(original, topSectionFieldSpecsToUse, bottomSectionFieldSpecsToUse);
		clone.createDraftCopyOf(original, getDatabase());
		return clone;
	}

	private static final String CACHE_FILE_NAME = "skcache.dat";
	private static final String OBSOLETE_CACHE_FILE_NAME = "sfcache.dat";
	private static final String FIELD_SPEC_CACHE_FILE_NAME = "fscache.dat";

	private FieldSpecCollection topSectionFieldSpecs;
	private FieldSpecCollection bottomSectionFieldSpecs;
}
