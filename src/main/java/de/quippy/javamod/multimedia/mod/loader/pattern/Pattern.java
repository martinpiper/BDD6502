/*
 * @(#) Pattern.java
 * 
 * Created on 28.04.2006 by Daniel Becker (quippy@quippy.de)
 * 
 *-----------------------------------------------------------------------
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *----------------------------------------------------------------------
 */
package de.quippy.javamod.multimedia.mod.loader.pattern;

import de.quippy.javamod.system.Helpers;

/**
 * @author Daniel Becker
 * @since 28.04.2006
 */
public class Pattern
{
	private PatternRow [] patternRow;
	/**
	 * Constructor for Pattern
	 */
	public Pattern(int rows)
	{
		super();
		patternRow = new PatternRow[rows];
	}
	public Pattern(int rows, int channels)
	{
		this(rows);
		for (int i=0; i<rows; i++) patternRow[i]= new PatternRow(channels);
	}
	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<patternRow.length; i++)
			sb.append(Helpers.getAsHex(i, 2)).append(": ").append(patternRow[i].toString()).append('\n');
		return sb.toString();
	}
	/**
	 * @since 23.08.2008
	 * @return
	 */
	public int getRowCount()
	{
		return patternRow.length;
	}
	/**
	 * @since 23.08.2008
	 */
	public void resetRowsPlayed()
	{
		for (int i=0; i<patternRow.length; i++)
		{
			PatternRow row = patternRow[i];
			if (row!=null) row.resetRowPlayed();
		}
	}
	/**
	 * @return Returns the patternRow.
	 */
	public PatternRow[] getPatternRow()
	{
		return patternRow;
	}
	/**
	 * @return Returns the patternRow.
	 */
	public PatternRow getPatternRow(int row)
	{
		return patternRow[row];
	}
	/**
	 * @return Returns the patternElement.
	 */
	public PatternElement getPatternElement(int row, int channel)
	{
		return patternRow[row].getPatternElement(channel);
	}
	/**
	 * @param patternRow The patternRow to set.
	 */
	public void setPatternRow(PatternRow[] patternRow)
	{
		this.patternRow = patternRow;
	}
	/**
	 * @param patternRow The patternRow to set.
	 */
	public void setPatternRow(int row, PatternRow patternRow)
	{
		this.patternRow[row] = patternRow;
	}
	/**
	 * @param patternElement The patternElement to set.
	 */
	public void setPatternElement(int row, int channel, PatternElement patternElement)
	{
		this.patternRow[row].setPatternElement(channel, patternElement);
	}
}
