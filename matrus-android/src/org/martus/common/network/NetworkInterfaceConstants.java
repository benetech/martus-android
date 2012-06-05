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

package org.martus.common.network;


public interface NetworkInterfaceConstants
{

	public static final String OK = "ok";
	public static final String CHUNK_OK = "chunk ok";
	public static final String NOT_FOUND = "not found";
	public static final String REJECTED = "account rejected";
	public static final String NOTYOURBULLETIN = "not your bulletin";
	public static final String DUPLICATE = "duplicate";
	public static final String SEALED_EXISTS = "sealed bulletin exists";
	public static final String INVALID_DATA = "invalid data";
	public static final String NO_SERVER = "no server";
	public static final String SERVER_DOWN = "server is down";
	public static final String SERVER_ERROR = "server error";
	public static final String SIG_ERROR = "signature error";
	public static final String INCOMPLETE = "incomplete result";
	public static final String VERSION = "MartusServer v0.30";
	public static final String TAG_BULLETIN_SIZE = "bulletin size";
	public static final String TAG_BULLETIN_DATE_SAVED = "bulletin date saved";
	public static final String TAG_BULLETIN_HISTORY = "bulletin history";

	public static final String UNKNOWN_COMMAND = "unknown command";
	public static final String NOT_AUTHORIZED = "not authorized";
	public static final String UNKNOWN = "unknown";

	public static final int CLIENT_MAX_CHUNK_SIZE = 100 * 1024;
	public static final int MAXIMUM_CLIENT_MAX_CHUNK_SIZE = 1024*1024;
	public static final String BASE_64_ENCODED = "Base64Encoded";
	
}
