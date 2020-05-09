/*
 * @(#) PatternRow.java
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

/**
 * @author Daniel Becker
 * @since 28.04.2006
 */
public class PatternRow
{
	private PatternElement [] patternElement;
	private boolean rowPlayed;
	
	/**
	 * Constructor for PatternRow
	 */
	public PatternRow(int channels)
	{
		super();
		patternElement = new PatternElement[channels];
		resetRowPlayed();
	}
	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(Boolean.toString(rowPlayed)).append(" | ");
		for (int i=0; i<patternElement.length; i++)
			sb.append(patternElement[i].toString()).append(" | ");
		return sb.toString();
	}
	/**
	 * @since 23.08.2008
	 */
	public void resetRowPlayed()
	{
		rowPlayed = false;
	}
	/**
	 * @since 23.08.2008
	 */
	public void setRowPlayed()
	{
		rowPlayed = true;
	}
	/**
	 * @since 23.08.2008
	 * @return
	 */
	public boolean isRowPlayed()
	{
		return rowPlayed;
	}
	/**
	 * @return Returns the patternElement.
	 */
	public PatternElement[] getPatternElement()
	{
		return patternElement;
	}
	/**
	 * @return Returns the patternElement.
	 */
	public PatternElement getPatternElement(int channel)
	{
		return patternElement[channel];
	}
	/**
	 * @param patternElement The patternElement to set.
	 */
	public void setPatternElement(PatternElement[] patternElement)
	{
		this.patternElement = patternElement;
	}
	/**
	 * @param patternElement The patternElement to set.
	 */
	public void setPatternElement(int channel, PatternElement patternElement)
	{
		this.patternElement[channel] = patternElement;
	}
}
