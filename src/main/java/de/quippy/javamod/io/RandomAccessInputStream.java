/*
 * @(#) RandomAccessInputStream.java
 *
 * Created on 10.09.2009 by Daniel Becker
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
import java.io.IOException;

/**
 * Interface for the RandomAccessInputStream that is used by ModfileInputStream
 * @author Daniel Becker
 * @since 10.09.2009
 */
public interface RandomAccessInputStream
{
	public File getFile();
	// InputStream functions - normally implemented due to extending from InputStream
	public int available() throws IOException;
	public void close() throws IOException;
	public void mark(int readlimit);
	public boolean markSupported();
	public int read() throws IOException;
	public int read(byte[] b, int off, int len) throws IOException;
	public int read(byte[] b) throws IOException;
	public void reset() throws IOException;
	public long skip(long n) throws IOException;
	// New functions
	public long getFilePointer() throws IOException;
	public void seek(long pos) throws IOException;
	public byte readByte() throws IOException;
	public long getLength() throws IOException;
	// RandomAccessFile functions
	public int skipBytes(int n) throws IOException;
	public long length() throws IOException;
	public int readFully(byte[] b) throws IOException;
	public int readFully(byte[] b, int offs, int len) throws IOException;
	public boolean readBoolean() throws IOException;
	public char readChar() throws IOException;
	public short readShort() throws IOException;
	public double readDouble() throws IOException;
	public float readFloat() throws IOException;
	public int readInt() throws IOException;
	public String readLine() throws IOException;
	public long readLong() throws IOException;
	public int readUnsignedByte() throws IOException;
	public int readUnsignedShort() throws IOException;
	public String readUTF() throws IOException;
}
