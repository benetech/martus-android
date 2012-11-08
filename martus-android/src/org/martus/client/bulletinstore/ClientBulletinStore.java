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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.zip.ZipFile;

import org.martus.common.FieldSpecCollection;
import org.martus.common.MartusUtilities.FileVerificationException;
import org.martus.common.bulletin.Bulletin;
import org.martus.common.bulletin.BulletinLoader;
import org.martus.common.bulletin.BulletinZipImporter;
import org.martus.common.bulletinstore.BulletinStore;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusCrypto.MartusSignatureException;
import org.martus.common.database.ClientFileDatabase;
import org.martus.common.database.Database;
import org.martus.common.database.DatabaseKey;
import org.martus.common.database.FileDatabase.MissingAccountMapException;
import org.martus.common.database.FileDatabase.MissingAccountMapSignatureException;
import org.martus.common.field.MartusField;
import org.martus.common.fieldspec.StandardFieldSpecs;
import org.martus.common.packet.BulletinHeaderPacket;
import org.martus.common.packet.BulletinHistory;
import org.martus.common.packet.UniversalId;
import org.martus.util.inputstreamwithseek.FileInputStreamWithSeek;
import org.martus.util.inputstreamwithseek.InputStreamWithSeek;



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
		
		initializeFolders();

		topSectionFieldSpecs = StandardFieldSpecs.getDefaultTopSetionFieldSpecs();
		bottomSectionFieldSpecs = StandardFieldSpecs.getDefaultBottomSectionFieldSpecs();
		
		loadCache();
		
		File obsoleteCacheFile = new File(getStoreRootDir(), OBSOLETE_CACHE_FILE_NAME);
		obsoleteCacheFile.delete();

	}

	
	public void prepareToExitWithoutSavingState()
	{
		getSignatureGenerator().flushSessionKeyCache();
	}


	
	public boolean mustEncryptPublicData()
	{
		return getDatabase().mustEncryptLocalData();
	}
	
	public boolean isMyBulletin(UniversalId uid)
	{
		return(uid.getAccountId().equals(getAccountId()));
	}

	public synchronized Set getSetOfBulletinUniversalIdsInFolders()
	{
		Set setOfUniversalIds = new HashSet();

		Vector visibleFolders = getAllVisibleFolders();
        for (Object visibleFolder : visibleFolders) {
            BulletinFolder folder = (BulletinFolder) visibleFolder;
            setOfUniversalIds.addAll(folder.getAllUniversalIdsUnsorted());
        }
		return setOfUniversalIds;
	}

	public synchronized void destroyBulletin(Bulletin b) throws IOException
	{
		removeBulletinFromAllFolders(b);
		removeBulletinFromStore(b);
	}

	public void removeBulletinFromAllFolders(Bulletin b) throws IOException
	{
		BulletinHistory history = b.getHistory();
		for(int i = 0; i < history.size(); ++i)
		{
			String localId = history.get(i);
			UniversalId uidOfAncestor = UniversalId.createFromAccountAndLocalId(b.getAccount(), localId);
			removeRevisionFromAllFolders(uidOfAncestor);
		}
		
		removeRevisionFromAllFolders(b.getUniversalId());
	}

	private void removeRevisionFromAllFolders(UniversalId id)
	{
		for(int f = 0; f < getFolderCount(); ++f)
		{
			removeBulletinFromFolder(getFolder(f), id);
		}
	}
	


	public Bulletin getBulletinRevision(UniversalId uid)
	{
		DatabaseKey key = DatabaseKey.createLegacyKey(uid);
		if(!doesBulletinRevisionExist(key))
		{
			//System.out.println("BulletinStore.findBulletinByUniversalId: !doesRecordExist");
			return null;
		}

		try
		{
			Bulletin b = loadFromDatabase(key);
			return b;
		}
		catch(NullPointerException e)
		{
			e.printStackTrace();
			return null;
		}
		catch(Exception e)
		{
			//TODO: Better error handling
			System.out.println("BulletinStore.findBulletinByUniversalId: " + e);
			e.printStackTrace();
			return null;
		}
	}

	public String getSentTag(UniversalId uid)
	{
		boolean knownNotOnServer = isProbablyNotOnServer(uid);

		if(getFolderDraftOutbox().contains(uid))
		{
			if(isMyBulletin(uid))
				return WAS_SENT_NO;
			if(!knownNotOnServer)
				return null;
		}

		if(knownNotOnServer)
			return WAS_SENT_NO;

		if(isProbablyOnServer(uid))
			return WAS_SENT_YES;
		
		return null;
	}

	public String getFieldData(UniversalId uid, String fieldTag)
	{
		if(fieldTag.equals(Bulletin.TAGWASSENT) || fieldTag.equals(Bulletin.PSEUDOFIELD_WAS_SENT))
		{
			String tag = getSentTag(uid);
			if(tag == null)
				return "";
			return tag;
		}

		
		Bulletin b = getBulletinRevision(uid);
		MartusField field = b.getField(fieldTag);
		if(field == null)
			return "";
		return field.getData();
	}

	public Bulletin loadFromDatabase(DatabaseKey key) throws
		Exception
	{
		Bulletin b = BulletinLoader.loadFromDatabase(getDatabase(), key, getSignatureVerifier());
		return b;
	}

	public void saveBulletin(Bulletin b) throws Exception
	{
		saveBulletin(b, mustEncryptPublicData());
	}

	public synchronized void discardBulletin(BulletinFolder f, UniversalId uid) throws IOException
	{
		try
		{
			if(!f.equals(folderDiscarded))
				folderDiscarded.add(uid);
		}
		catch (BulletinAlreadyExistsException saveToIgnoreException)
		{
		}
		removeBulletinFromFolder(f, uid);
		if(isOrphan(uid))
			destroyBulletin(getBulletinRevision(uid));
	}


	
	private boolean isDiscarded(UniversalId uid)
	{
		return getFolderDiscarded().contains(uid);
	}

	public synchronized BulletinFolder createFolder(String name)
	{
		BulletinFolder folder = rawCreateFolder(name);
		return folder;
	}



	public synchronized int getFolderCount()
	{
		return folders.size();
	}

	private synchronized BulletinFolder getFolder(int index)
	{
		if(index < 0 || index >= folders.size())
			return null;

		return (BulletinFolder)folders.get(index);
	}

	public synchronized BulletinFolder findFolder(String name)
	{
		for(int index=0; index < getFolderCount(); ++index)
		{
			BulletinFolder folder = getFolder(index);
			if(name.equals(folder.getName()))
				return folder;
		}
		return null;
	}

	public synchronized Vector getAllFolders()
	{
		Vector allFolders = new Vector();
		for(int f = 0; f < getFolderCount(); ++f)
		{
			BulletinFolder folder = getFolder(f);
			allFolders.add(folder);
		}
		return allFolders;
	}
	
	public synchronized Vector getAllVisibleFolders()
	{
		Vector allFolders = getAllFolders();
		Vector visibleFolders = new Vector();
		for(Iterator f = allFolders.iterator(); f.hasNext();)
		{
			BulletinFolder folder = (BulletinFolder) f.next();
			if(folder.isVisible())
				visibleFolders.add(folder);
		}
		return visibleFolders;
	}
	


	public synchronized Vector getVisibleFolderNames()
	{
		Vector names = new Vector();
		Vector visibleFolders = getAllVisibleFolders();
		for(Iterator f = visibleFolders.iterator(); f.hasNext();)
		{
			BulletinFolder folder = (BulletinFolder) f.next();
			String folderName = folder.getName();
			names.add(folderName);
		}
		return names;
	}




	public BulletinFolder getFolderDiscarded()
	{
		return folderDiscarded;
	}


	public BulletinFolder getFolderDraftOutbox()
	{
		return folderDraftOutbox;
	}

	
	private BulletinFolder getFolderOnServer()
	{
		return createOrFindFolder(ON_SERVER_FOLDER);
	}
	
	private BulletinFolder getFolderNotOnServer()
	{
		return createOrFindFolder(NOT_ON_SERVER_FOLDER);
	}


	public void createSystemFolders()
	{
		folderSaved = createSystemFolder(SAVED_FOLDER);
		folderDiscarded = createSystemFolder(DISCARDED_FOLDER);
		folderDraftOutbox = createSystemFolder(DRAFT_OUTBOX);
		
		folderSealedOutbox = createSystemFolder(SEALED_OUTBOX);
	}

	public BulletinFolder createSystemFolder(String name)
	{
		BulletinFolder folder = rawCreateFolder(name);
		if(folder == null)
			folder = findFolder(name);
		folder.preventRename();
		folder.preventDelete();
		return folder;
	}
	
	// synchronized because updateOnServerLists is called from background thread
	public synchronized boolean isProbablyOnServer(UniversalId uid)
	{
		return getFolderOnServer().contains(uid);
	}
	
	// synchronized because updateOnServerLists is called from background thread
	public synchronized boolean isProbablyNotOnServer(UniversalId uid)
	{
		return getFolderNotOnServer().contains(uid);
	}
	
	public  void setIsOnServer(Bulletin b)
	{
		setIsOnServer(b.getUniversalId());
	}

	// synchronized because updateOnServerLists is called from background thread
	public synchronized void setIsOnServer(UniversalId uid)
	{
		removeBulletinFromFolder(getFolderNotOnServer(), uid);
		try
		{
			getFolderOnServer().add(uid);
		}
		catch(BulletinAlreadyExistsException harmless)
		{
		}
		catch(Exception ignoreForNow)
		{
			// TODO: Figure out if this should be propagated
			ignoreForNow.printStackTrace();
		}
	}

	public  void setIsNotOnServer(Bulletin b)
	{
		setIsNotOnServer(b.getUniversalId());
	}
	
	// synchronized because updateOnServerLists is called from background thread
	public synchronized void setIsNotOnServer(UniversalId uid)
	{
		removeBulletinFromFolder(getFolderOnServer(), uid);
		try
		{
			getFolderNotOnServer().add(uid);
		}
		catch(BulletinAlreadyExistsException harmless)
		{
		}
		catch(Exception ignoreForNow)
		{
			// TODO: Figure out if this should be propagated
			ignoreForNow.printStackTrace();
		}
	}


	public synchronized void removeBulletinFromFolder(BulletinFolder from, UniversalId uid)
	{
		from.remove(uid);
	}


	public void deleteAllData() throws Exception
	{
		super.deleteAllData();
		deleteFoldersDatFile();
		resetFolders();
	}			
	
	public void deleteFoldersDatFile()
	{
		getFoldersFile().delete();
	}	
	
	public void resetFolders()
	{
		initializeFolders();
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
		deleteFoldersDatFile();
	}	

	public void signAccountMap() throws MartusSignatureException, IOException
	{
		getWriteableDatabase().signAccountMap();
	}

	public synchronized void loadFolders()
	{
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			InputStreamWithSeek in = new FileInputStreamWithSeek(getFoldersFile());
			getSignatureVerifier().decrypt(in, out);
			in.close();

			String folderXml = new String(out.toByteArray(), "UTF-8");
			internalLoadFolders(folderXml);
		}
		catch(UnsupportedEncodingException e)
		{
			System.out.println("BulletinStore.loadFolders: " + e);
		}
		catch(FileNotFoundException expectedIfFoldersDontExistYet)
		{
		}
		catch(Exception e)
		{
			// TODO: Improve error handling!!!
			System.out.println("BulletinStore.loadFolders: " + e);
			e.printStackTrace();
		}
	}



	public File getFoldersFile()
	{
		return getFoldersFileForAccount(getStoreRootDir());
	}

	static public File getFoldersFileForAccount(File AccountDir)
	{
		return new File(AccountDir, "MartusFolders.dat");
	}

	public FieldSpecCollection getBottomSectionFieldSpecs()
	{
		return bottomSectionFieldSpecs;
	}

	public FieldSpecCollection getTopSectionFieldSpecs()
	{
		return topSectionFieldSpecs;
	}

	public synchronized BulletinFolder createOrFindFolder(String name)
	{
		BulletinFolder result = findFolder(name);
		if(result != null)
			return result;
		return createFolder(name);
	}

	public void ensureBulletinIsInFolder(BulletinFolder folder, UniversalId uid) throws IOException, AddOlderVersionToFolderFailedException
	{
		try
		{
			addBulletinToFolder(folder, uid);
		}
		catch (BulletinAlreadyExistsException ignoreHarmless)
		{
		}
	}
	
	public synchronized void addBulletinToFolder(BulletinFolder folder, UniversalId uidToAdd) throws BulletinAlreadyExistsException, IOException, AddOlderVersionToFolderFailedException
	{
		Bulletin b = getBulletinRevision(uidToAdd);
		if(b == null)
			return;
		
		if(folder.isVisible() && !isLeaf(uidToAdd))
			throw new AddOlderVersionToFolderFailedException();
		
		folder.add(uidToAdd);

		String accountId = uidToAdd.getAccountId();
		Vector visibleFolders = getAllVisibleFolders();
		BulletinHistory history = b.getHistory();
		for(int i = 0; i < history.size(); ++i)
		{
			String localId = history.get(i);
			UniversalId uidToRemove = UniversalId.createFromAccountAndLocalId(accountId, localId);
			for(Iterator f = visibleFolders.iterator(); f.hasNext();)
			{
				BulletinFolder folderToFix = (BulletinFolder) f.next();
				if( folderToFix.contains(uidToRemove))
				{
					try
					{
						folderToFix.add(uidToAdd);
					}
					catch (BulletinAlreadyExistsException ignoreHarmless)
					{
					}
					removeBulletinFromFolder(folderToFix, uidToRemove);
				}
			}
		}
	}

	

	

	private void initializeFolders()
	{
		folders = new Vector();
		createSystemFolders();
	}
	
	public void setTopSectionFieldSpecs(FieldSpecCollection newFieldSpecs)
	{
		topSectionFieldSpecs = newFieldSpecs;
	}
	
	public void setBottomSectionFieldSpecs(FieldSpecCollection newFieldSpecs)
	{
		bottomSectionFieldSpecs = newFieldSpecs;
	}


	public synchronized boolean isOrphan(UniversalId uid)
	{
		Vector allFolders= getVisibleFolderNames();
		for(int i = 0; i < allFolders.size(); ++i)
		{
			BulletinFolder folder = findFolder((String)allFolders.get(i));
			if(folder != null && folder.contains(uid))
				return false;
		}

		return true;
	}

	private synchronized BulletinFolder rawCreateFolder(String name)
	{
		if(findFolder(name) != null)
			return null;

		BulletinFolder folder = new BulletinFolder(this, name);
		folders.add(folder);
		return folder;
	}




	public synchronized void internalLoadFolders(String folderXml)
	{
		folders.clear();
		loadedLegacyFolders = false;

        createSystemFolders();
	}


	public static class BulletinAlreadyExistsException extends Exception 
	{
	}
	
	public static class AddOlderVersionToFolderFailedException extends Exception 
	{
	}

	public void importZipFileBulletin(File zipFile, BulletinFolder toFolder, boolean forceSameUids) throws
			Exception
	{
		ZipFile zip = new ZipFile(zipFile);
		try
		{
			BulletinHeaderPacket bhp = BulletinHeaderPacket.loadFromZipFile(zip, getSignatureVerifier());
			UniversalId uid = bhp.getUniversalId();

			boolean isSealed = bhp.getStatus().equals(Bulletin.STATUSSEALED);
			if(forceSameUids || !isMyBulletin(uid) || isSealed)
			{
				importZipFileToStoreWithSameUids(zipFile);
			}
			else
			{
				uid = importZipFileToStoreWithNewUids(zipFile);
			}

			if(!toFolder.contains(uid))
				addBulletinToFolder(toFolder, uid);
		}
		finally
		{
			zip.close();
		}

	}

	public UniversalId importZipFileToStoreWithNewUids(File inputFile) throws
		Exception
	{
		final MartusCrypto security = getSignatureGenerator();
		Bulletin imported = BulletinZipImporter.loadFromFileAsNewDraft(security, inputFile);
		saveBulletin(imported);
		return imported.getUniversalId();
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

	public Vector getUidsOfAllBulletinRevisions()
	{
		class UidCollector implements Database.PacketVisitor
		{
			public void visit(DatabaseKey key)
			{
				uidList.add(key.getUniversalId());
			}
			Vector uidList = new Vector();
		}
	
		UidCollector uidCollector = new UidCollector();
		visitAllBulletinRevisions(uidCollector);
		return uidCollector.uidList;
	}


	public static final String SAVED_FOLDER = "%Sent";
	public static final String DISCARDED_FOLDER = "%Discarded";
	public static final String SEARCH_RESULTS_BULLETIN_FOLDER = "%SearchResults";
	public static final String RECOVERED_BULLETIN_FOLDER = "%RecoveredBulletins";
	public static final String RETRIEVE_SEALED_BULLETIN_FOLDER = "%RetrievedMyBulletin";
	public static final String RETRIEVE_SEALED_FIELD_OFFICE_BULLETIN_FOLDER = "%RetrievedFieldOfficeBulletin";
	public static final String RETRIEVE_DRAFT_BULLETIN_FOLDER = "%RetrievedMyBulletinDraft";
	public static final String RETRIEVE_DRAFT_FIELD_OFFICE_BULLETIN_FOLDER = "%RetrievedFieldOfficeBulletinDraft";
	public static final String DAMAGED_BULLETIN_FOLDER = "%DamagedBulletins";
	private static final String DRAFT_OUTBOX = "*DraftOutbox";
	private static final String SEALED_OUTBOX = "*SealedOutbox";
	private static final String ON_SERVER_FOLDER = "*OnServer";
	private static final String NOT_ON_SERVER_FOLDER = "*NotOnServer";

	public static final String OBSOLETE_OUTBOX_FOLDER = "%OutBox";
	public static final String OBSOLETE_DRAFT_FOLDER = "%Draft";
	public static final String WAS_SENT_YES = "WasSentYes";
	public static final String WAS_SENT_NO = "WasSentNo";

	private static final String CACHE_FILE_NAME = "skcache.dat";
	private static final String OBSOLETE_CACHE_FILE_NAME = "sfcache.dat";
	private static final String FIELD_SPEC_CACHE_FILE_NAME = "fscache.dat";

	private Vector folders;
	private BulletinFolder folderSaved;
	private BulletinFolder folderDiscarded;
	private BulletinFolder folderDraftOutbox;
	private BulletinFolder folderSealedOutbox;
	private boolean loadedLegacyFolders;

	private FieldSpecCollection topSectionFieldSpecs;
	private FieldSpecCollection bottomSectionFieldSpecs;
}
