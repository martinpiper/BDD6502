/*
 * @(#) ModfileInputStream.java
 *
 * Created on 31.12.2007 by Daniel Becker
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
package de.quippy.javamod.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import de.quippy.javamod.system.Helpers;
import de.quippy.javamod.system.Log;

/**
 * We here add special read methods for ModFiles ;)
 * @author Daniel Becker
 * @since 31.12.2007
 */
public class ModfileInputStream extends RandomAccessInputStreamImpl
{
	private String fileName;
	
	/**
	 * Constructor for ModfileInputStream
	 * @param file
	 * @throws FileNotFoundException
	 */
	public ModfileInputStream(File file) throws FileNotFoundException, IOException
	{
		super(file);
		this.fileName = file.getAbsolutePath();
		checkForPackedFiles();
	}
	/**
	 * Constructor for ModfileInputStream
	 * @param fileName
	 * @throws FileNotFoundException
	 */
	public ModfileInputStream(String fileName) throws FileNotFoundException, IOException
	{
		super(fileName);
		this.fileName = fileName;
		checkForPackedFiles();
	}
	/**
	 * Constructor for ModfileInputStream
	 * @param fromUrl
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws MalformedURLException
	 */
	public ModfileInputStream(URL fromUrl) throws IOException, FileNotFoundException, MalformedURLException
	{
		super(fromUrl);
		this.fileName = Helpers.createLocalFileStringFromURL(fromUrl, false);
		checkForPackedFiles();
	}
	/**
	 * @return the fileName
	 */
	public String getFileName()
	{
		return fileName;
	}
	private void checkForPackedFiles()
	{
		try
		{
			if (PowerPackerFile.isPowerPacker(this))
			{
				PowerPackerFile ppFile = new PowerPackerFile(this);
				close();
				tmpFile = null;
				raFile = null;
				buffer = ppFile.getBuffer();
				bufferLength = buffer.length;
				readPointer = 0;
			}
			else
			if (XpkSqsh.isXPK_SQSH(this))
			{
				XpkSqsh xpkSqshFile = new XpkSqsh(this);
				close();
				tmpFile = null;
				raFile = null;
				buffer = xpkSqshFile.getBuffer();
				bufferLength = buffer.length;
				readPointer = 0;
			}
		}
		catch (IOException ex)
		{
			Log.error("ModfileInputStream::checkForPowerPackerFile", ex);
		}
	}
	/**************************************************************************/
	/**
	 * @since 31.12.2007
	 * @param strLength
	 * @return a String
	 * @throws IOException
	 */
	public String readString(int strLength) throws IOException
	{
		byte [] buffer = new byte[strLength];
		int read = read(buffer, 0, strLength);
		return Helpers.retrieveAsString(buffer, 0, read);
	}
	/**
	 * @since 31.12.2007
	 */
	public int readByteAsInt() throws IOException
	{
		return ((int)readByte())&0xFF;
	}
	/**
	 * @since 31.12.2007
	 */
	public int readMotorolaWord() throws IOException
	{
		return ((readByte()&0xFF)<<8) | (readByte()&0xFF);
	}
	/**
	 * @since 31.12.2007
	 */
	public int readIntelWord() throws IOException
	{
		return (readByte()&0xFF) | ((readByte()&0xFF)<<8);
	}
	/**
	 * @since 31.12.2007
	 */
	public int readMotorolaDWord() throws IOException
	{
		return ((readByte()&0xFF)<<24) | ((readByte()&0xFF)<<16) | ((readByte()&0xFF)<<8) | (readByte()&0xFF);
	}
	/**
	 * @since 31.12.2007
	 */
	public int readIntelDWord() throws IOException
	{
		return (readByte()&0xFF) | ((readByte()&0xFF)<<8) | ((readByte()&0xFF)<<16) | ((readByte()&0xFF)<<24);
	}
}
