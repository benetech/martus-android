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

import org.martus.common.MiniLocalization;
import org.martus.util.xml.XmlUtilities;


abstract public class FieldType
{
	public static FieldType createFromTypeName(String name)
	{
		if(FieldTypeNormal.getTypeNameString().equals(name))
			return new FieldTypeNormal();
		if(FieldTypeMultiline.getTypeNameString().equals(name))
			return new FieldTypeMultiline();
		if(FieldTypeDate.getTypeNameString().equals(name))
			return new FieldTypeDate();
		if(FieldTypeDateRange.getTypeNameString().equals(name))
			return new FieldTypeDateRange();
		if(FieldTypeBoolean.getTypeNameString().equals(name))
			return new FieldTypeBoolean();
		if(FieldTypeLanguage.getTypeNameString().equals(name))
			return new FieldTypeLanguage();
		if(FieldTypeGrid.getTypeNameString().equals(name))
			return new FieldTypeGrid();
		if(FieldTypeDropdown.getTypeNameString().equals(name))
			return new FieldTypeDropdown();
		if(FieldTypeMessage.getTypeNameString().equals(name))
			return new FieldTypeMessage();
		if(FieldTypeSectionStart.getTypeNameString().equals(name))
			return new FieldTypeSectionStart();
		
		return new FieldTypeUnknown();
	}
	
	abstract public String getTypeName();
	
	public String convertStoredToSearchable(String storedData, MiniLocalization localization)
	{
		return storedData;
	}
	
	public String convertStoredToHtml(String storedData, MiniLocalization localization)
	{
		return XmlUtilities.getXmlEncoded(storedData);
	}
	
	public String convertStoredToExportable(String storedData, MiniLocalization localization)
	{
		return storedData;
	}
	
	public boolean equals(Object other)
	{
		return (this.getClass().equals(other.getClass()));
	}
	
	public int hashCode()
	{
		// hashcodes don't have to be unique--just consistent
		return 0;
	}

	public boolean isDate()
	{
		return false;
	}
	
	public boolean isDateRange()
	{
		return false;
	}
	
	public boolean isString()
	{
		return false;
	}
	
	public boolean isMultiline()
	{
		return false;
	}
	
	public boolean isBoolean()
	{
		return false;
	}
	
	public boolean isDropdown()
	{
		return false;
	}
	
	public boolean isNestedDropdown()
	{
		return false;
	}
	
	public boolean isMessage()
	{
		return false;
	}
	
	public boolean isSectionStart()
	{
		return false;
	}
	
	public boolean isLanguageDropdown()
	{
		return false;
	}
	
	public boolean isAnyField()
	{
		return false;
	}
	
	public boolean isPopUpTree()
	{
		return false;
	}
	
	public boolean isGrid()
	{
		return false;
	}
	
	public boolean isUnknown()
	{
		return false;
	}
	
	public String getDefaultValue()
	{
		return "";
	}
	
	public FieldSpec createEmptyFieldSpec()
	{
		return new FieldSpec(this);
	}
	
	public static String UNKNOWN_TYPE_STRING = "UNKNOWN";

}
