/*
 * @(#) ModuleFactory.java
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
package de.quippy.javamod.multimedia.mod.loader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import de.quippy.javamod.io.ModfileInputStream;
import de.quippy.javamod.system.Log;

/**
 * Returns the appropiate ModuleClass for the desired ModFile
 * @author Daniel Becker
 * @since 21.04.2006
 */
public class ModuleFactory
{
	private static HashMap<String, Module> fileExtensionMap;
	private static ArrayList<Module> modulesArray;
	/**
	 * Constructor for ModuleFactory - This Class Is A Singleton
	 */
	private ModuleFactory()
	{
		super();
	}
	
	/**
	 * Lazy instantiation access method
	 * @since 04.01.2010
	 * @return
	 */
	private static HashMap<String, Module> getFileExtensionMap()
	{
		if (fileExtensionMap==null)
			fileExtensionMap= new HashMap<String, Module>();
		
		return fileExtensionMap;
	}
	/**
	 * Lazy instantiation access method
	 * @since 04.01.2010
	 * @return
	 */
	private static ArrayList<Module> getModulesArray()
	{
		if (modulesArray==null)
			modulesArray = new ArrayList<Module>();
		return modulesArray;
	}
	public static void registerModule(Module mod)
	{
		getModulesArray().add(mod);
		String [] extensions = mod.getFileExtensionList();
		for (int i=0; i<extensions.length; i++)
			getFileExtensionMap().put(extensions[i], mod);
	}
	public static void deregisterModule(Module mod)
	{
		getModulesArray().remove(mod);
		String [] extensions = mod.getFileExtensionList();
		for (int i=0; i<extensions.length; i++)
			getFileExtensionMap().remove(extensions[i]);
	}
	public static String [] getSupportedFileExtensions()
	{
		Set<String> keys = getFileExtensionMap().keySet();
		String[] result = new String[keys.size()];
		return keys.toArray(result);
	}
	public static Module getModuleFromExtension(String extension)
	{
		return getFileExtensionMap().get(extension.toLowerCase());
	}
	/**
	 * Finds the appropriate loader through the IDs
	 * @since 04.01.2010
	 * @param input
	 * @return
	 */
	private static Module getModuleFromStreamByID(ModfileInputStream input)
	{
		Iterator<Module> iter = getModulesArray().iterator();
		while (iter.hasNext())
		{
			Module mod = iter.next();
			try 
			{
				if (mod.checkLoadingPossible(input)) return mod;
			}
			catch (IOException ex)
			{
				/* Ignoring */
			}
		}
		return null;
	}
	/**
	 * Finds the appropriate loader through simply loading it!
	 * @since 13.06.2010
	 * @param input
	 * @return
	 */
	private static Module getModuleFromStream(ModfileInputStream input)
	{
		Iterator<Module> iter = getModulesArray().iterator();
		while (iter.hasNext())
		{
			Module mod = iter.next();
			try 
			{
				Module result = mod.loadModFile(input);
				input.seek(0);
				return result; // <-- here this loading was a success!
			}
			catch (Throwable ex)
			{
				/* Ignoring */
			}
		}
		return null;
	}
	/**
	 * Uses the File-Extension to find a suitable loader.
	 * @param fileName The Filename of the mod
	 * @return null, if fails
	 */
	public static Module getInstance(String fileName) throws IOException
	{
		return getInstance(new File(fileName));
	}
	/**
	 * Uses the File-Extension to find a suitable loader.
	 * @param file The File-Instance of the modfile
	 * @return null, if fails
	 */
	public static Module getInstance(File file) throws IOException
	{
		return getInstance(file.toURI().toURL());
	}
	/**
	 * Uses the File-Extension to find a suitable loader.
	 * @param url URL-Instance of the path to the modfile
	 * @return null, if fails
	 */
	public static Module getInstance(URL url) throws IOException
	{
		ModfileInputStream inputStream = null;
		try
		{
			inputStream = new ModfileInputStream(url);
			Module mod = getModuleFromStreamByID(inputStream);
			// If the header gives no infos, it's obviously a Noise Tracker file
			// So let's try all loaders
			if (mod!=null) 
				return mod.loadModFile(inputStream);
			else
			{
				mod = getModuleFromStream(inputStream);
				if (mod!=null)
					return mod;
				else
					throw new IOException("Unsupported MOD-Type: " + inputStream.getFileName());
			}
		}
		catch (Exception ex)
		{
			Log.error("[ModuleFactory] Failed with loading " + url.toString(), ex);
			return null;
		}
		finally
		{
			if (inputStream!=null) try { inputStream.close(); } catch (IOException ex) { Log.error("IGNORED", ex); }
		}
	}
}
