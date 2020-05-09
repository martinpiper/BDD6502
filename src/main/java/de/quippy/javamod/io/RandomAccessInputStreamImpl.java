/*
 * @(#) RandomAccessInputStreamImpl.java
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

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UTFDataFormatException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import de.quippy.javamod.system.Log;

/**
 * This class mappes the RandomAccessFile to an InputStream type of class.
 * You can also instantiate this class with an URL. If this URL is not of
 * protocol type "file://" the ressource will get downloaded and written to
 * a tmp file. The tempfile will be deleted with calling of "close".
 * Furthermore this input stream will also handle zip compressed content
 * If nothing else works the fallback strategy is to use an internal buffer.
 * This will be also used by PowerPacked Modes (see ModfileInputSteam)
 * @author Daniel Becker
 * @since 31.12.2007
 */
public class RandomAccessInputStreamImpl extends InputStream implements RandomAccessInputStream
{
	protected RandomAccessFile raFile = null;
	protected File localFile = null;
	protected File tmpFile = null;
	protected int mark = 0;
	
	protected byte[] buffer = null;
	protected int readPointer = 0;
	protected int bufferLength = 0;
	
	/**
	 * Constructor for RandomAccessInputStreamImpl
	 * @param file
	 * @throws FileNotFoundException
	 */
	public RandomAccessInputStreamImpl(File file) throws IOException, FileNotFoundException
	{
		super();
		if (!file.exists())
		{
			file = unpackFromZIPFile(file.toURI().toURL());
		}
		raFile = new RandomAccessFile(localFile = file, "r");
	}
	/**
	 * Constructor for RandomAccessInputStreamImpl
	 * @param fileName
	 * @throws FileNotFoundException
	 */
	public RandomAccessInputStreamImpl(String fileName) throws IOException, FileNotFoundException
	{
		this(new File(fileName));
	}
	public RandomAccessInputStreamImpl(URL fromUrl) throws IOException, FileNotFoundException
	{
		super();
		if (fromUrl.getProtocol().equalsIgnoreCase("file"))
		{
			try
			{
				File file = new File(fromUrl.toURI());
				if (!file.exists())
				{
					file = unpackFromZIPFile(fromUrl);
				}
				raFile = new RandomAccessFile(localFile = file, "r");
			}
			catch (URISyntaxException uriEx)
			{
				throw new MalformedURLException(uriEx.getMessage());
			}
		}
		else
		{
			InputStream inputStream = new FileOrPackedInputStream(fromUrl);

			try
			{
				tmpFile = copyFullStream(inputStream);
				try { inputStream.close(); } catch (IOException ex) { Log.error("IGNORED", ex); }
				raFile = new RandomAccessFile(localFile = tmpFile, "r");
			}
			catch (Throwable ex)
			{
				int size = inputStream.available();
				if (size<1024) size = 1024;
				ByteArrayOutputStream out = new ByteArrayOutputStream(size);
				
				copyFullStream(inputStream, out);
				
				inputStream.close();
				out.close();
				
				buffer = out.toByteArray();
				bufferLength = buffer.length;
				readPointer = 0;
				raFile = null;
				localFile = null;
			}
		}
	}
	/**
	 * @since 04.01.2011
	 * @param file
	 * @return
	 * @throws IOException
	 */
	private File unpackFromZIPFile(URL fromUrl) throws IOException, FileNotFoundException
	{
		InputStream input = new FileOrPackedInputStream(fromUrl);
		return copyFullStream(input);
	}
	/**
	 * @since 02.01.2011
	 * @param input
	 * @return
	 * @throws IOException
	 */
	private File copyFullStream(InputStream input) throws IOException
	{
		tmpFile = File.createTempFile("JavaMod", "ReadFile");
		tmpFile.deleteOnExit();

		FileOutputStream out = new FileOutputStream(tmpFile);
		copyFullStream(input, out);
		out.close();
		
		return tmpFile;
	}
	/**
	 * @since 02.01.2008
	 * @param inputStream
	 * @param out
	 * @throws IOException
	 */
	private void copyFullStream(InputStream inputStream, OutputStream out) throws IOException
	{
		byte[] input = new byte[8192];
		int len;
		while ((len = inputStream.read(input, 0, 8192))!=-1)
		{
			out.write(input, 0, len);
		}
	}
	/**
	 * Will return the local file this RandomAccessFile works on
	 * or null using local buffer and no file
	 * @since 09.01.2011
	 * @return
	 */
	public File getFile()
	{
		return localFile;
	}
	/**
	 * @return
	 * @throws IOException
	 * @see java.io.InputStream#available()
	 */
	@Override
	public int available() throws IOException
	{
		if (raFile!=null)
			return (int)(raFile.length() - raFile.getFilePointer());
		else
			return bufferLength-readPointer;
	}
	/**
	 * @throws IOException
	 * @see java.io.InputStream#close()
	 */
	@Override
	public void close() throws IOException
	{
		if (raFile!=null) raFile.close();
		super.close();
		if (tmpFile!=null)
		{
			boolean ok = tmpFile.delete();
			if (!ok) Log.error("Could not delete temporary file: " + tmpFile.getCanonicalPath());
		}
		raFile = null;
		buffer = null;
		bufferLength = 0;
		readPointer = 0;
	}
	/**
	 * @param readlimit
	 * @see java.io.InputStream#mark(int)
	 */
	@Override
	public synchronized void mark(int readlimit)
	{
		try
		{
			if (raFile!=null) 
				mark = (int)raFile.getFilePointer();
			else
				mark = readPointer;
		}
		catch (IOException ex)
		{
		}
	}
	/**
	 * @return
	 * @see java.io.InputStream#markSupported()
	 */
	@Override
	public boolean markSupported()
	{
		return true;
	}
	/**
	 * @return
	 * @throws IOException
	 * @see java.io.InputStream#read()
	 */
	@Override
	public int read() throws IOException
	{
		if (raFile!=null)
			return raFile.read();
		else
			return (readPointer<bufferLength)? (int)(buffer[readPointer++] & 0xFF) : -1;
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
		if (raFile!=null)
			return raFile.read(b, off, len);
		else
		{
			if (b == null) 
				throw new NullPointerException();
			if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0))
				throw new IndexOutOfBoundsException();
			if (readPointer >= bufferLength)
				return -1;
			if (readPointer + len > bufferLength)
				len = bufferLength - readPointer;
			if (len <= 0)
				return 0;
			System.arraycopy(buffer, readPointer, b, off, len);
			readPointer += len;
			return len;
		}
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
		return read(b, 0, b.length);
	}
	/**
	 * @throws IOException
	 * @see java.io.InputStream#reset()
	 */
	@Override
	public synchronized void reset() throws IOException
	{
		if (raFile!=null)
			raFile.seek(mark);
		else
			readPointer = mark;
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
		if (raFile!=null)
			return (long)(raFile.skipBytes((int)n));
		else
		{
			if (readPointer + n > bufferLength)
			    n = bufferLength - readPointer;
			if (n < 0) return 0;
			readPointer += n;
			return n;
		}
	}
	/********************* Mapping to RandomAccessFile ************************/
	public int skipBytes(int n) throws IOException
	{
		return (int)skip(n);
	}
	public long getFilePointer() throws IOException
	{
		if (raFile!=null)
			return raFile.getFilePointer();
		else
			return readPointer;
	}
	public void seek(long pos) throws IOException
	{
		if (raFile!=null)
			raFile.seek(pos);
		else
			readPointer = (int)pos;
	}
	public byte readByte() throws IOException
	{
		if (raFile!=null)
			return raFile.readByte();
		else
			return (byte)(read());
	}
	public long getLength() throws IOException
	{
		if (raFile!=null)
			return raFile.length();
		else
			return bufferLength;
	}
	public long length() throws IOException
	{
		return getLength();
	}
	public int readFully(byte[] b) throws IOException
	{
		if (raFile != null)
		{
			raFile.readFully(b);
			return b.length;
		}
		else
			return read(b);
	}
	public int readFully(byte[] b, int offs, int len) throws IOException
	{
		if (raFile != null)
		{
			raFile.readFully(b, offs, len);
			return len;
		}
		else
			return read(b, offs, len);
	}
	public boolean readBoolean() throws IOException
	{
		if (raFile != null)
			return raFile.readBoolean();
		else
		{
			int ch = this.read();
			if (ch < 0) throw new EOFException();
			return (ch != 0);
		}
	}
	public char readChar() throws IOException
	{
		if (raFile != null) 
			return raFile.readChar();
		else
		{
			int ch1 = this.read();
			int ch2 = this.read();
			if ((ch1 | ch2) < 0) throw new EOFException();
			return (char)((ch1 << 8) | (ch2));
		}
	}
	public short readShort() throws IOException
	{
		if (raFile != null) 
			return raFile.readShort();
		else
		{
			int ch1 = this.read();
			int ch2 = this.read();
			if ((ch1 | ch2) < 0)
			    throw new EOFException();
			return (short)((ch1 << 8) | (ch2));
		}
	}
	public double readDouble() throws IOException
	{
		if (raFile != null)
			return raFile.readDouble();
		else
			return Double.longBitsToDouble(readLong());
	}
	public float readFloat() throws IOException
	{
		if (raFile != null) 
			return raFile.readFloat();
		else
			return Float.intBitsToFloat(readInt());
	}
	public int readInt() throws IOException
	{
		if (raFile != null) 
			return raFile.readInt();
		else
		{
			int ch1 = this.read();
			int ch2 = this.read();
			int ch3 = this.read();
			int ch4 = this.read();
			if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
			return ((ch1 << 24) | (ch2 << 16) | (ch3 << 8) | (ch4));
		}
	}
	public String readLine() throws IOException
	{
		if (raFile != null) 
			return raFile.readLine();
		else
		{
			StringBuffer input = new StringBuffer();
			int c = -1;
			boolean eol = false;

			while (!eol)
			{
				switch (c = read())
				{
					case -1:
					case '\n':
						eol = true;
						break;
					case '\r':
						eol = true;
						long cur = getFilePointer();
						if ((read()) != '\n')
						{
							seek(cur);
						}
						break;
					default:
						input.append((char) c);
						break;
				}
			}

			if ((c == -1) && (input.length() == 0))
			{
				return null;
			}
			return input.toString();
		}
	}
	public long readLong() throws IOException
	{
		if (raFile != null) 
			return raFile.readLong();
		else
			return ((long)(readInt()) << 32) + (readInt() & 0xFFFFFFFFL);
	}
	public int readUnsignedByte() throws IOException
	{
		if (raFile != null) 
			return raFile.readUnsignedByte();
		else
		{
			int ch = this.read();
			if (ch < 0) throw new EOFException();
			return ch;
		}
	}
	public int readUnsignedShort() throws IOException
	{
		if (raFile != null) 
			return raFile.readUnsignedShort();
		else
		{
			int ch1 = this.read();
			int ch2 = this.read();
			if ((ch1 | ch2) < 0) throw new EOFException();
			return (ch1 << 8) | (ch2);
		}
	}
	public String readUTF() throws IOException
	{
		if (raFile != null) 
			return raFile.readUTF();
		else
		{
			int utflen = readUnsignedShort();
			byte[] bytearr = new byte[utflen];
			char[] chararr = new char[utflen];

			int c, char2, char3;
			int count = 0;
			int chararr_count = 0;

			readFully(bytearr, 0, utflen);

			while (count < utflen)
			{
				c = (int) bytearr[count] & 0xff;
				if (c > 127) break;
				count++;
				chararr[chararr_count++] = (char) c;
			}

			while (count < utflen)
			{
				c = (int) bytearr[count] & 0xff;
				switch (c >> 4)
				{
					case 0:
					case 1:
					case 2:
					case 3:
					case 4:
					case 5:
					case 6:
					case 7:
						/* 0xxxxxxx */
						count++;
						chararr[chararr_count++] = (char) c;
						break;
					case 12:
					case 13:
						/* 110x xxxx 10xx xxxx */
						count += 2;
						if (count > utflen) throw new UTFDataFormatException("malformed input: partial character at end");
						char2 = (int) bytearr[count - 1];
						if ((char2 & 0xC0) != 0x80) throw new UTFDataFormatException("malformed input around byte " + count);
						chararr[chararr_count++] = (char) (((c & 0x1F) << 6) | (char2 & 0x3F));
						break;
					case 14:
						/* 1110 xxxx 10xx xxxx 10xx xxxx */
						count += 3;
						if (count > utflen) throw new UTFDataFormatException("malformed input: partial character at end");
						char2 = (int) bytearr[count - 2];
						char3 = (int) bytearr[count - 1];
						if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80)) throw new UTFDataFormatException("malformed input around byte " + (count - 1));
						chararr[chararr_count++] = (char) (((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | ((char3 & 0x3F) << 0));
						break;
					default:
						/* 10xx xxxx, 1111 xxxx */
						throw new UTFDataFormatException("malformed input around byte " + count);
				}
			}
			// The number of chars produced may be less than utflen
			return new String(chararr, 0, chararr_count);
		}
	}
}
