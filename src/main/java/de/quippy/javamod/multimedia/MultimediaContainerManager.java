/*
 * @(#)MultimediaContainerManager.java
 *
 * Created on 12.10.2007 by Daniel Becker
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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import javax.sound.sampled.UnsupportedAudioFileException;

import de.quippy.javamod.system.Helpers;
import de.quippy.javamod.system.Log;

/**
 * @author: Daniel Becker
 * @since: 12.10.2007
 */
public class MultimediaContainerManager
{
	private static HashMap<String, MultimediaContainer> fileExtensionMap;
	private static ArrayList<MultimediaContainer> containerArray;
	private static Properties containerConfigs;
	/**
	 * @since: 12.10.2007
	 */
	private MultimediaContainerManager()
	{
		super();
	}
	public static HashMap<String, MultimediaContainer> getFileExtensionMap()
	{
		if (fileExtensionMap==null)
			fileExtensionMap= new HashMap<String, MultimediaContainer>();
		
		return fileExtensionMap;
	}
	public static ArrayList<MultimediaContainer> getContainerArray()
	{
		if (containerArray==null)
			containerArray = new ArrayList<MultimediaContainer>();
		return containerArray;
	}
	public static Properties getContainerConfigs()
	{
		if (containerConfigs==null) containerConfigs = new Properties();
		return containerConfigs;
	}
	public static void getContainerConfigs(Properties intoProps)
	{
		fireConfiggurationSave();
		Enumeration<Object> propertyEnum = getContainerConfigs().keys();
		while (propertyEnum.hasMoreElements())
		{
			Object key = propertyEnum.nextElement();
			Object value = getContainerConfigs().get(key);
			intoProps.put(key, value);
		}
	}
	public static void configureContainer(Properties fromProps)
	{
		Enumeration<Object> propertyEnum = fromProps.keys();
		while (propertyEnum.hasMoreElements())
		{
			Object key = propertyEnum.nextElement();
			Object value = fromProps.get(key);
			/*if (getContainerConfigs().contains(key))*/ getContainerConfigs().put(key, value);
		}
		fireConfiggurationChanged();
	}
	private static void fireConfiggurationChanged()
	{
		ArrayList<MultimediaContainer> listeners = getContainerArray();
		for (int i=0; i<listeners.size(); i++)
			listeners.get(i).configurationChanged(getContainerConfigs());
	}
	private static void fireConfiggurationSave()
	{
		ArrayList<MultimediaContainer> listeners = getContainerArray();
		for (int i=0; i<listeners.size(); i++)
			listeners.get(i).configurationSave(getContainerConfigs());
	}
	public static void registerContainer(MultimediaContainer container)
	{
		getContainerArray().add(container);
		String [] extensions = container.getFileExtensionList();
		for (int i=0; i<extensions.length; i++)
			getFileExtensionMap().put(extensions[i], container);
	}
	public static void deregisterContainer(MultimediaContainer container)
	{
		getContainerArray().remove(container);
		String [] extensions = container.getFileExtensionList();
		for (int i=0; i<extensions.length; i++)
			getFileExtensionMap().remove(extensions[i]);
	}
	public static void updateLookAndFeel()
	{
		ArrayList<MultimediaContainer> listeners = getContainerArray();
		for (int i=0; i<listeners.size(); i++)
			listeners.get(i).updateLookAndFeel();
	}
	public static String[] getSupportedFileExtensions()
	{
		Set<String> keys = getFileExtensionMap().keySet();
		String[] result = new String[keys.size()];
		return keys.toArray(result);
	}
	public static HashMap<String, String[]>getSupportedFileExtensionsPerContainer()
	{
		ArrayList<MultimediaContainer> listeners = getContainerArray();
		HashMap<String, String[]> result = new HashMap<String, String []>(listeners.size());
		for (int i=0; i<listeners.size(); i++)
			result.put(listeners.get(i).getName(), listeners.get(i).getFileExtensionList());
		return result;
	}
	public static MultimediaContainer getMultimediaContainerForType(String type) throws UnsupportedAudioFileException
	{
		MultimediaContainer container = getFileExtensionMap().get(type.toLowerCase());
		if (container==null) 
			throw new UnsupportedAudioFileException(type);
		else
			return container;
	}
	public static MultimediaContainer getMultimediaContainerSingleton(URL url) throws UnsupportedAudioFileException
	{
		String fileName = url.getPath();

		// we default to mp3 with wrong extensions
		MultimediaContainer baseContainer = getFileExtensionMap().get(Helpers.getExtensionFrom(fileName));
		if (baseContainer==null) baseContainer = getFileExtensionMap().get(Helpers.getPreceedingExtensionFrom(fileName));
		if (baseContainer==null) // no extensions found?!
		{
			if (url.getProtocol().equalsIgnoreCase("file")) 
				throw new UnsupportedAudioFileException(fileName); // in Filemode we are ready now
			else
				baseContainer = getFileExtensionMap().get("mp3"); // otherwise we try a streaming protocol!
		}

		return baseContainer;
	}
	public static MultimediaContainer getMultimediaContainer(URL url) throws UnsupportedAudioFileException
	{
		MultimediaContainer baseContainer = getMultimediaContainerSingleton(url);
		MultimediaContainer container = baseContainer.getInstance(url);
		if (container==null) 
			throw new UnsupportedAudioFileException(url.getPath());
		else
			return container;
	}
	public static MultimediaContainer getMultimediaContainer(URI uri) throws MalformedURLException, UnsupportedAudioFileException
	{
		return getMultimediaContainer(uri.toURL()); 
	}
	public static MultimediaContainer getMultimediaContainer(File file) throws MalformedURLException, UnsupportedAudioFileException
	{
		return getMultimediaContainer(file.toURI()); 
	}
	public static MultimediaContainer getMultimediaContainer(String fileName) throws MalformedURLException, UnsupportedAudioFileException
	{
		return getMultimediaContainer(new File(fileName));
	}
	public static void addMultimediaContainerEventListener(MultimediaContainerEventListener listener)
	{
		ArrayList<MultimediaContainer> containers = getContainerArray();
		for (int i=0; i<containers.size(); i++)
			containers.get(i).addListener(listener);
	}
	public static void removeMultimediaContainerEventListener(MultimediaContainerEventListener listener)
	{
		ArrayList<MultimediaContainer> containers = getContainerArray();
		for (int i=0; i<containers.size(); i++)
			containers.get(i).removeListener(listener);
	}
	public static String getSongNameFromURL(URL url)
	{
		if (url==null) return "";

		String result = Helpers.createStringFomURL(url);
		final int lastSlash = result.lastIndexOf('/');
		int dot = result.lastIndexOf('.');
		if (dot == -1 || dot<lastSlash) dot = result.length();
		return result.substring(lastSlash + 1, dot); 
	}
	public static String getSongNameFromFile(File fileName)
	{
		if (fileName==null) return "";

		String result = fileName.getAbsolutePath();
		final int lastSlash = result.lastIndexOf(File.separatorChar);
		int dot = result.lastIndexOf('.');
		if (dot == -1 || dot<lastSlash) dot = result.length();
		return result.substring(lastSlash + 1, dot); 
	}
	/**
	 * This method will only do (!)localy(!) what is needed to pick up
	 * the song name String at [0] and time in milliseconds as Long at [1]
	 * @param url
	 * @return
	 * @since 12.02.2011
	 */
	public static Object [] getSongInfosFor(URL url)
	{
		try
		{
			MultimediaContainer container = getMultimediaContainerSingleton(url);
			if (container!=null) 
				return container.getSongInfosFor(url);
		}
		catch (UnsupportedAudioFileException ex)
		{
			Log.error("IGNORED", ex);
		}
		return new Object[] { getSongNameFromURL(url) + " UNSUPPORTED FILE", Long.valueOf(-1) };
	}
}
