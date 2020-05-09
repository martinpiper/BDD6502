/*
 * @(#) FileOrPackedInputStream.java
 *
 * Created on 04.01.2011 by Daniel Becker
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.quippy.javamod.system.Helpers;

/**
 * This FileInputStream will also handle files that are in zip files
 * Use a path like "C:/someDir/zipFile.zip/path/to/file
 * @author Daniel Becker
 * @since 04.01.2011
 */
public class FileOrPackedInputStream extends InputStream
{
	protected InputStream stream;
	private ZipEntry entry;
	
	/**
	 * Constructor for FileOrPackedInputStream
	 */
	public FileOrPackedInputStream(File file) throws IOException, FileNotFoundException
	{
		super();
		if (!file.exists()) 
		{
			try
			{
				stream = tryForZippedFile(file.toURI().toURL());
			}
			catch (MalformedURLException ex)
			{
				throw new FileNotFoundException(file.getCanonicalPath());
			}
		}
		else
			stream = new FileInputStream(file);
	}
	/**
	 * Constructor for FileOrPackedInputStream
	 */
	public FileOrPackedInputStream(String fileName) throws IOException, FileNotFoundException
	{
		this(new File(fileName));
	}
	/**
	 * Constructor for FileOrPackedInputStream
	 */
	public FileOrPackedInputStream(URL fromUrl) throws IOException, FileNotFoundException
	{
		super();
		try
		{
			stream = fromUrl.openStream();
		}
		catch (AccessControlException ex) // This happens with applets if controlled from outside
		{
			throw new AccessControlException("[FileOrPackedInputStream] Access denied: "+fromUrl.toString());
		}
		catch (Exception ex)
		{
			//Log.error("[FileOrPackedInputStream] Checking if "+fromUrl.toString()+" is a zipped file location", ex);
			stream = tryForZippedFile(fromUrl);
		}
	}
	private InputStream tryForZippedFile(URL fromUrl) throws IOException
	{
		String path = fromUrl.toString();
		String fileNamePortion = "";
		while (path!=null && path.length()!=0)
		{
			int slashIndex = path.lastIndexOf('/');
			if (slashIndex<0) break;
			fileNamePortion = Helpers.createStringFromURLString(path.substring(slashIndex)) + fileNamePortion;
			path = path.substring(0, slashIndex);
			URL newUrl = new URL(path);
			ZipInputStream input = null;
			try
			{
				input = new ZipInputStream(newUrl.openStream());
			}
			catch (Throwable e)
			{
				continue;
			}
			String zipEntryName = fileNamePortion.substring(1);
			ZipEntry entry;
			while ((entry = input.getNextEntry())!=null)
			{
				if (entry.isDirectory()) continue;
				if (entry.getName().equals(zipEntryName))
				{
					this.entry = entry;
					return input;
				}
			}
		}
		throw new FileNotFoundException(fromUrl.toString());
	}
	/**
	 * returns null if this stream is not a zip stream
	 * @return the entry
	 */
	public ZipEntry getEntry()
	{
		return entry;
	}
	/**
	 * @return
	 * @throws IOException
	 * @see java.io.InputStream#available()
	 */
	@Override
	public int available() throws IOException
	{
		if (entry==null) 
			return stream.available();
		else
			return (int)entry.getSize();
	}
	/**
	 * @throws IOException
	 * @see java.io.InputStream#close()
	 */
	@Override
	public void close() throws IOException
	{
		stream.close();
		super.close();
	}
	/**
	 * @param readlimit
	 * @see java.io.InputStream#mark(int)
	 */
	@Override
	public synchronized void mark(int readlimit)
	{
		stream.mark(readlimit);
	}
	/**
	 * @return
	 * @see java.io.InputStream#markSupported()
	 */
	@Override
	public boolean markSupported()
	{
		return stream.markSupported();
	}
	/**
	 * @throws IOException
	 * @see java.io.InputStream#reset()
	 */
	@Override
	public synchronized void reset() throws IOException
	{
		stream.reset();
	}
	/**
	 * @param n
	 * @return
	 * @throws IOException
	 * @see java.io.InputStream#skip(long)
	 */
	@Override
	public long skip(long n) throws IOException
	{
		return stream.skip(n);
	}
	/**
	 * @return
	 * @throws IOException
	 * @see java.io.InputStream#read()
	 */
	@Override
	public int read() throws IOException
	{
		return stream.read();
	}
	/**
	 * @param b
	 * @return
	 * @throws IOException
	 * @see java.io.InputStream#read(byte[])
	 */
	@Override
	public int read(byte[] b) throws IOException
	{
		return stream.read(b);
//		return read(b, 0, b.length);
	}
	/**
	 * @param b
	 * @param off
	 * @param len
	 * @return
	 * @throws IOException
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		return stream.read(b, off, len);
//		int fullSize = 0;
//		while (fullSize < len)
//		{
//			int readLength = stream.read(b, off + fullSize, len - fullSize);
//			if (readLength==-1)
//			{
//				if (fullSize>0)
//					return fullSize;
//				else
//					return -1;
//			}
//			fullSize += readLength;
//		}
//		return fullSize;
	}
}
