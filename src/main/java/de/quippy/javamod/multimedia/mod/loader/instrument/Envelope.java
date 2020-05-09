/*
 * @(#) Envelope.java
 * 
 * Created on 19.06.2006 by Daniel Becker
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
package de.quippy.javamod.multimedia.mod.loader.instrument;

/**
 * @author Daniel Becker
 * @since 19.06.2006
 */
public class Envelope
{
	private int [] position;
	private int [] value;
	private int nPoints;
	private int sustainStartPoint;
	private int sustainEndPoint;
	private int loopStartPoint;
	private int loopEndPoint;
	public boolean on, sustain, loop, carry, filter;
	
	private static final int SHIFT = 3;
	private static final int MAXVALUE = 64<<SHIFT;

	/**
	 * Constructor for Envelope
	 */
	public Envelope()
	{
		super();
		on=sustain=loop=carry=filter=false;
	}
	/**
	 * Get the new position
	 * @since 19.06.2006
	 * @param p
	 * @param keyOff
	 * @return
	 */
	public int updatePosition(int p, boolean keyOff)
	{
		p++;
		if (loop && p>=position[loopEndPoint]) p=position[loopStartPoint];
		if (sustain && p>=position[sustainEndPoint] && !keyOff) p=position[sustainStartPoint];
		return p;
	}
	/**
	 * get the value at the position
	 * Returns values between 0 and 512
	 * @since 19.06.2006
	 * @param p
	 * @return
	 */
	public int getValueForPosition(int p)
	{
		int pt = nPoints - 1;
		for (int i=0; i<pt; i++)
		{
			if (p <= position[i]) 
			{ 
				pt = i; 
				break; 
			}
		}
		int x2 = position[pt];
		int x1, v;
		if (p>=x2)
		{
			v = value[pt]<<SHIFT;
			x1 = x2;
		}
		else
		if (pt>0)
		{
			v = value[pt-1]<<SHIFT;
			x1 = position[pt-1];
		}
		else
		{
			v = x1 = 0;
		}
		
		if (p>x2) p=x2;
		if ((x2>x1) && (p>x1))
		{
			v += ((p - x1) * ((value[pt]<<SHIFT) - v)) / (x2 - x1);
		}
		if (v<0) v=0;
		else
		if (v>MAXVALUE) v = MAXVALUE;
		
		return v;
	}
	/**
	 * Sets the boolean values corresponding to the flag value
	 * XM-Version
	 * @since 19.06.2006
	 * @param flag
	 */
	public void setXMType(int flag)
	{
		on = (flag&0x01)!=0;
		sustain = (flag&0x02)!=0;
		loop = (flag&0x04)!=0;
		carry = filter = false;
	}
	/**
	 * Sets the boolean values corresponding to the flag value
	 * IT-Version (why on earth needed this to be swaped?!)
	 * @since 12.11.2006
	 * @param flag
	 */
	public void setITType(int flag)
	{
		on = (flag&0x01)!=0;
		loop = (flag&0x02)!=0;
		sustain = (flag&0x04)!=0;
		carry = (flag&0x08)!=0;
		filter = (flag&0x80)!=0;
	}
	/**
	 * @param loopEndPoint The loopEndPoint to set.
	 */
	public void setLoopEndPoint(int loopEndPoint)
	{
		this.loopEndPoint = loopEndPoint;
	}
	/**
	 * @param loopStartPoint The loopStartPoint to set.
	 */
	public void setLoopStartPoint(int loopStartPoint)
	{
		this.loopStartPoint = loopStartPoint;
	}
	/**
	 * @param points The nPoints to set.
	 */
	public void setNPoints(int points)
	{
		nPoints = points;
	}
	/**
	 * @param position The position to set.
	 */
	public void setPosition(int[] position)
	{
		this.position = position;
	}
	/**
	 * @param value The value to set.
	 */
	public void setValue(int[] value)
	{
		this.value = value;
	}
	/**
	 * @param sustainPoint The sustainPoint to set. (XM-Version)
	 */
	public void setSustainPoint(int sustainPoint)
	{
		this.sustainStartPoint = this.sustainEndPoint = sustainPoint;
	}
	/**
	 * @param sustainEndPoint the sustainEndPoint to set (IT-Version)
	 */
	public void setSustainEndPoint(int sustainEndPoint)
	{
		this.sustainEndPoint = sustainEndPoint;
	}
	/**
	 * @param sustainStartPoint the sustainStartPoint to set (IT-Version)
	 */
	public void setSustainStartPoint(int sustainStartPoint)
	{
		this.sustainStartPoint = sustainStartPoint;
	}
}
