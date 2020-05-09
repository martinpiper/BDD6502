/*
 * @(#) MultimediaContainerEvent.java
 *
 * Created on 27.12.2008 by Daniel Becker
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
package de.quippy.javamod.multimedia;

import java.util.EventObject;

/**
 * @author Daniel Becker
 * @since 27.12.2008
 */
public class MultimediaContainerEvent extends EventObject
{
	private static final long serialVersionUID = 5125318726800658845L;

	public static int SONG_NAME_CHANGED = 1;
	public static int SONG_NAME_CHANGED_OLD_INVALID = 3;
	
	private int type;
	private Object event;
	/**
	 * Constructor for MultimediaContainerEvent
	 * @param source
	 */
	public MultimediaContainerEvent(Object source, int type, Object event)
	{
		super(source);
		this.type = type;
		this.event = event;
	}
	/**
	 * @return the type
	 */
	public int getType()
	{
		return type;
	}
	/**
	 * @return the event
	 */
	public Object getEvent()
	{
		return event;
	}
}
