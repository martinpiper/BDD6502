/*
 * @(#) PatternElement.java
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
public class PatternElement
{
	private int patternIndex;
	private int row;
	private int channel;
	private int period;
	private int noteIndex;
	private int instrument;
	private int effekt;
	private int effektOp;
	private int volumeEffekt;
	private int volumeEffektOp;

	/**
	 * Constructor for PatternElement
	 */
	public PatternElement(int patternIndex, int patternRow, int channel)
	{
		super();
		this.patternIndex = patternIndex;
		this.row = patternRow;
		this.channel = channel;
		this.period = 0;
		this.noteIndex = 0;
		this.instrument = 0;
		this.volumeEffekt = 0;
		this.volumeEffektOp = 0;
		this.effekt = 0;
		this.effektOp = 0;
	}
	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder(Helpers.getNoteNameToPeriod(noteIndex));
		if ((period==0 && noteIndex!=0) || (period!=0 && noteIndex==0))
			sb.append('!');
		else
			sb.append(' ');
		if (instrument!=0) sb.append(Helpers.getAsHex(instrument, 2)); else sb.append("..");
		if (volumeEffekt!=0)
		{
			switch (volumeEffekt)
			{
				case 0x01: sb.append('v'); break;
				case 0x02: sb.append('d'); break;
				case 0x03: sb.append('c'); break;
				case 0x04: sb.append('b'); break;
				case 0x05: sb.append('a'); break;
				case 0x06: sb.append('u'); break;
				case 0x07: sb.append('h'); break;
				case 0x08: sb.append('p'); break;
				case 0x09: sb.append('l'); break;
				case 0x0A: sb.append('r'); break;
				case 0x0B: sb.append('g'); break;
			}
			sb.append(Helpers.getAsHex(volumeEffektOp, 2));
		}
		else 
			sb.append(" ..");
		
		sb.append(' ');
		if (effekt!=0)
		{
			if (effekt<0x0F)
				sb.append(Helpers.getAsHex(effekt, 1));
			else
				sb.append((char)('F' + effekt - 0x0F));
			sb.append(Helpers.getAsHex(effektOp, 2));
		}
		else 
			sb.append("...");
		
		return sb.toString();
	}
	/**
	 * @return Returns the channel.
	 */
	public int getChannel()
	{
		return channel;
	}
	/**
	 * @param channel The channel to set.
	 */
	public void setChannel(int channel)
	{
		this.channel = channel;
	}
	/**
	 * @return Returns the effekt.
	 */
	public int getEffekt()
	{
		return effekt;
	}
	/**
	 * @param effekt The effekt to set.
	 */
	public void setEffekt(int effekt)
	{
		this.effekt = effekt;
	}
	/**
	 * @return Returns the effektOp.
	 */
	public int getEffektOp()
	{
		return effektOp;
	}
	/**
	 * @param effektOp The effektOp to set.
	 */
	public void setEffektOp(int effektOp)
	{
		this.effektOp = effektOp;
	}
	/**
	 * @return Returns the instrument.
	 */
	public int getInstrument()
	{
		return instrument;
	}
	/**
	 * @param instrument The instrument to set.
	 */
	public void setInstrument(int instrument)
	{
		this.instrument = instrument;
	}
	/**
	 * @return Returns the noteIndex.
	 */
	public int getNoteIndex()
	{
		return noteIndex;
	}
	/**
	 * @param noteIndex The noteIndex to set.
	 */
	public void setNoteIndex(int noteIndex)
	{
		this.noteIndex = noteIndex;
	}
	/**
	 * @return Returns the patternIndex.
	 */
	public int getPatternIndex()
	{
		return patternIndex;
	}
	/**
	 * @param patternIndex The patternIndex to set.
	 */
	public void setPatternIndex(int patternIndex)
	{
		this.patternIndex = patternIndex;
	}
	/**
	 * @return Returns the period.
	 */
	public int getPeriod()
	{
		return period;
	}
	/**
	 * @param period The period to set.
	 */
	public void setPeriod(int period)
	{
		this.period = period;
	}
	/**
	 * @return Returns the row.
	 */
	public int getRow()
	{
		return row;
	}
	/**
	 * @param row The row to set.
	 */
	public void setRow(int row)
	{
		this.row = row;
	}
	/**
	 * @return Returns the volume.
	 */
	public int getVolumeEffekt()
	{
		return volumeEffekt;
	}
	/**
	 * @param volume The volume to set.
	 */
	public void setVolumeEffekt(int volumeEffekt)
	{
		this.volumeEffekt = volumeEffekt;
	}
	/**
	 * @return Returns the volumeEffektOp.
	 */
	public int getVolumeEffektOp()
	{
		return volumeEffektOp;
	}
	/**
	 * @param volumeEffektOp The volumeEffektOp to set.
	 */
	public void setVolumeEffektOp(int volumeEffektOp)
	{
		this.volumeEffektOp = volumeEffektOp;
	}
}
