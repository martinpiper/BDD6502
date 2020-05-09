/*
 * @(#) Log.java
 * 
 * Created on 21.04.2006 by Daniel Becker (quippy@quippy.de)
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
package de.quippy.javamod.system;

import java.util.ArrayList;

/**
 * A simple Logger
 * @author Daniel Becker
 * @since 21.04.2006
 */
public class Log
{
	public static final int LOGLEVEL_NONE = 0;
	public static final int LOGLEVEL_INFO = 1;
	public static final int LOGLEVEL_ERROR = 2;
	public static final int LOGLEVEL_DEBUG = 4;
	public static final int LOGLEVEL_ALL = LOGLEVEL_DEBUG | LOGLEVEL_ERROR | LOGLEVEL_INFO;

	private static ArrayList<LogMessageCallBack> logReceiver = new ArrayList<LogMessageCallBack>();
	private static int currentLogLevel = LOGLEVEL_ALL;

	private Log()
	{
		super();
	}
	
	public static void setLogLevel(final int newLogLevel)
	{
		currentLogLevel = newLogLevel;
	}
	public static boolean isLogLevel(final int whatLogLevel)
	{
		return (currentLogLevel & whatLogLevel) != 0;
	}
	
	public static synchronized void addLogListener(final LogMessageCallBack receiver)
	{
		if (!logReceiver.contains(receiver)) logReceiver.add(receiver);
	}
	public static synchronized void removeLogListener(final LogMessageCallBack receiver)
	{
		logReceiver.remove(receiver);
	}
	public static synchronized void error(final String message)
	{
		error(message, null);
	}
	public static synchronized void error(final String message, final Throwable ex)
	{
		if (isLogLevel(LOGLEVEL_ERROR))
		{
			final int size = logReceiver.size();
			for (int i=0; i<size; i++) logReceiver.get(i).error(message, ex);
			System.err.println(message);
			if (ex!=null) 
			{
				ex.printStackTrace(System.err);
				System.err.print('\n');
			}
		}
	}
	public static synchronized void info(final String message)
	{
		if (isLogLevel(LOGLEVEL_INFO))
		{
			final int size = logReceiver.size();
			for (int i=0; i<size; i++) logReceiver.get(i).info(message);
			System.out.println(message);
		}
	}
	public static synchronized void debug(final String message)
	{
		if (isLogLevel(LOGLEVEL_DEBUG))
		{
			final int size = logReceiver.size();
			for (int i=0; i<size; i++) logReceiver.get(i).debug(message);
			System.out.println(message);
		}
	}
}