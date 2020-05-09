/*
 * @(#) PatternContainer.java
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
public class PatternContainer
{
	private Pattern [] pattern;
	/**
	 * Constructor for PatternContainer
	 */
	public PatternContainer(int anzPattern)
	{
		super();
		pattern = new Pattern[anzPattern];
	}
	public PatternContainer(int anzPattern, int row)
	{
		this(anzPattern);
		for (int i=0; i<anzPattern; i++) pattern[i] = new Pattern(row);
	}
	public PatternContainer(int anzPattern, int row, int channels)
	{
		this(anzPattern);
		for (int i=0; i<anzPattern; i++) pattern[i] = new Pattern(row, channels);
	}
	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<pattern.length; i++)
			sb.append(i).append(". Pattern:\n").append(pattern[i].toString()).append('\n');
		return sb.toString();
	}
	/**
	 * @since 23.08.2008
	 */
	public void resetRowsPlayed()
	{
		for (int i=0; i<pattern.length; i++)
			pattern[i].resetRowsPlayed();
	}
	/**
	 * @return Returns the pattern.
	 */
	public Pattern[] getPattern()
	{
		return pattern;
	}
	/**
	 * @return Returns the pattern.
	 */
	public Pattern getPattern(int patternIndex)
	{
		return pattern[patternIndex];
	}
	/**
	 * @return Returns the pattern.
	 */
	public PatternRow getPatternRow(int patternIndex, int row)
	{
		return pattern[patternIndex].getPatternRow(row);
	}
	/**
	 * @return Returns the pattern.
	 */
	public PatternElement getPatternElement(int patternIndex, int row, int channel)
	{
		return pattern[patternIndex].getPatternElement(row, channel);
	}
	/**
	 * @param pattern The pattern to set.
	 */
	public void setPattern(Pattern[] pattern)
	{
		this.pattern = pattern;
	}
	/**
	 * @param pattern The pattern to set.
	 */
	public void setPattern(int patternIndex, Pattern pattern)
	{
		this.pattern[patternIndex] = pattern;
	}
	/**
	 * @param pattern The pattern to set.
	 */
	public void setPatternRow(int patternIndex, int row, PatternRow patternRow)
	{
		this.pattern[patternIndex].setPatternRow(row, patternRow);
	}
	/**
	 * @param pattern The pattern to set.
	 */
	public void setPatternElement(int patternIndex, int row, int channel, PatternElement patternElement)
	{
		this.pattern[patternIndex].setPatternElement(row, channel, patternElement);
	}
	/**
	 * @param pattern The pattern to set.
	 */
	public void setPatternElement(PatternElement patternElement)
	{
		this.pattern[patternElement.getPatternIndex()].setPatternElement(patternElement.getRow(), patternElement.getChannel(), patternElement);
	}
}
