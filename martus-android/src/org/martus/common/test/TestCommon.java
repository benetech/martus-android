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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.martus.common.bulletin.TestBulletin;
import org.martus.common.bulletinstore.TestBulletinStore;


public class TestCommon
{
	public static void main (String[] args)
	{
		runTests();
	}

	public static void runTests ()
	{
		//todo removed
//		junit.textui.TestRunner.run (suite());
	}

	public static Test suite ( )
	{
		TestSuite suite= new TestSuite("All Common Martus Tests");

		// common stuff
		suite.addTest(new TestSuite(TestBulletin.class));
		suite.addTest(new TestSuite(TestBulletinStore.class));
		suite.addTest(new TestSuite(TestClientFileDatabase.class));
		suite.addTest(new TestSuite(TestFileDatabase.class));
		suite.addTest(new TestSuite(TestMartusKeyPair.class));
		suite.addTest(new TestSuite(TestMartusSecurity.class));

		return suite;
	}
}
