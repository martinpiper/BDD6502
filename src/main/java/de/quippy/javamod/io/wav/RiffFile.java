/*
 * @(#) RiffFile.java
 *
 * Created on 31.12.2008 by Daniel Becker
 * 11/19/04 1.0 moved to LGPL.
 * 02/23/99 JavaConversion by E.B
 * Don Cross, April 1993.
 * RIFF file format classes.
 * See Chapter 8 of "Multimedia Programmer's Reference" in
 * the Microsoft Windows SDK.
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

package de.quippy.javamod.io.wav;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Class to manage RIFF files
 */
public class RiffFile
{
	protected static class RiffChunkHeader
	{
		public int ckID = 0; // Four-character chunk ID
		public int ckSize = 0; // Length of data in chunk

		public RiffChunkHeader()
		{
		}
	}

	// DDCRET
	public static final int DDC_SUCCESS = 0; // The operation succeded
	public static final int DDC_FAILURE = 1; // The operation failed for unspecified reasons
	public static final int DDC_OUT_OF_MEMORY = 2; // Operation failed due to running out of memory
	public static final int DDC_FILE_ERROR = 3; // Operation encountered file I/O error
	public static final int DDC_INVALID_CALL = 4; // Operation was called with invalid parameters
	public static final int DDC_USER_ABORT = 5; // Operation was aborted by the user
	public static final int DDC_INVALID_FILE = 6; // File format does not match

	// RiffFileMode
	public static final int RFM_UNKNOWN = 0; // undefined type (can use to mean "N/A" or "not open")
	public static final int RFM_WRITE = 1; // open for write
	public static final int RFM_READ = 2; // open for read

	private RiffChunkHeader riff_header; // header for whole file
	protected int fmode; // current file I/O mode
	protected RandomAccessFile file; // I/O stream to use

	/**
	 * Dummy Constructor
	 */
	public RiffFile()
	{
		super();
		file = null;
		fmode = RFM_UNKNOWN;
		riff_header = new RiffChunkHeader();
		riff_header.ckID = fourCC("RIFF");
		riff_header.ckSize = 0;
	}

	/**
	 * Return File Mode.
	 */
	public int currentFileMode()
	{
		return fmode;
	}

	/**
	 * Open a RIFF file.
	 */
	public int open(String Filename, int NewMode)
	{
		int retcode = DDC_SUCCESS;

		if (fmode != RFM_UNKNOWN)
		{
			retcode = close();
		}

		if (retcode == DDC_SUCCESS)
		{
			switch (NewMode)
			{
				case RFM_WRITE:
					try
					{
						file = new RandomAccessFile(Filename, "rw");
						fmode = RFM_WRITE;
						// Write the RIFF header...
						// We will have to come back later and patch it!
						if (writeHeader_internally(riff_header)!=DDC_SUCCESS)
						{
							file.close();
							fmode = RFM_UNKNOWN;
						}
					}
					catch (IOException ioe)
					{
						fmode = RFM_UNKNOWN;
						retcode = DDC_FILE_ERROR;
					}
					break;

				case RFM_READ:
					try
					{
						file = new RandomAccessFile(Filename, "r");
						try
						{
							// Try to read the RIFF header...   				   
							byte[] br = new byte[8];
							file.read(br, 0, 8);
							fmode = RFM_READ;
							riff_header.ckID = ((br[0] << 24) & 0xFF000000) | ((br[1] << 16) & 0x00FF0000) | ((br[2] << 8) & 0x0000FF00) | (br[3] & 0x000000FF);
							riff_header.ckSize = ((br[7] << 24) & 0xFF000000) | ((br[6] << 16) & 0x00FF0000) | ((br[5] << 8) & 0x0000FF00) | (br[4] & 0x000000FF);
						}
						catch (IOException ioe)
						{
							file.close();
							fmode = RFM_UNKNOWN;
						}
					}
					catch (IOException ioe)
					{
						fmode = RFM_UNKNOWN;
						retcode = DDC_FILE_ERROR;
					}
					break;
				default:
					retcode = DDC_INVALID_CALL;
			}
		}
		return retcode;
	}

	public int write(byte[] data, int numBytes)
	{
		return write(data, 0, numBytes);
	}
	/**
	 * Write NumBytes data.
	 */
	public int write(byte[] data, int start, int numBytes)
	{
		if (fmode != RFM_WRITE)
		{
			return DDC_INVALID_CALL;
		}
		try
		{
			file.write(data, start, numBytes);
			fmode = RFM_WRITE;
		}
		catch (IOException ioe)
		{
			return DDC_FILE_ERROR;
		}
		riff_header.ckSize += numBytes-start;
		return DDC_SUCCESS;
	}
	public int writeHeader_internally(RiffChunkHeader hriff_header)
	{
		byte[] br = new byte[8];
		br[0] = (byte) ((hriff_header.ckID >>> 24) & 0x000000FF);
		br[1] = (byte) ((hriff_header.ckID >>> 16) & 0x000000FF);
		br[2] = (byte) ((hriff_header.ckID >>> 8) & 0x000000FF);
		br[3] = (byte) ( hriff_header.ckID & 0x000000FF);

		br[7] = (byte) ((hriff_header.ckSize >>> 24) & 0x000000FF);
		br[6] = (byte) ((hriff_header.ckSize >>> 16) & 0x000000FF);
		br[5] = (byte) ((hriff_header.ckSize >>> 8) & 0x000000FF);
		br[4] = (byte) ( hriff_header.ckSize & 0x000000FF);

		try
		{
			file.write(br, 0, 8);
		}
		catch (IOException ioe)
		{
			return DDC_FILE_ERROR;
		}
		return DDC_SUCCESS;
	}
	/**
	 * Write NumBytes data.
	 */
	public int writeHeader(RiffChunkHeader hriff_header)
	{
		if (fmode != RFM_WRITE)
		{
			return DDC_INVALID_CALL;
		}
		
		int status = writeHeader_internally(hriff_header);
		if (status==DDC_SUCCESS) riff_header.ckSize+=8;
		return status;
	}

	/**
	 * Write NumBytes data.
	 */
	public int write(short Data, int NumBytes)
	{
		short theData = (short) (((Data >>> 8) & 0x00FF) | ((Data << 8) & 0xFF00));
		if (fmode != RFM_WRITE)
		{
			return DDC_INVALID_CALL;
		}
		try
		{
			file.writeShort(theData);
			fmode = RFM_WRITE;
		}
		catch (IOException ioe)
		{
			return DDC_FILE_ERROR;
		}
		riff_header.ckSize += NumBytes;
		return DDC_SUCCESS;
	}
	/**
	 * Write NumBytes data.
	 */
	public int write(int Data, int NumBytes)
	{
		short theDataL = (short) ((Data >>> 16) & 0x0000FFFF);
		short theDataR = (short) (Data & 0x0000FFFF);
		short theDataLI = (short) (((theDataL >>> 8) & 0x00FF) | ((theDataL << 8) & 0xFF00));
		short theDataRI = (short) (((theDataR >>> 8) & 0x00FF) | ((theDataR << 8) & 0xFF00));
		int theData = ((theDataRI << 16) & 0xFFFF0000) | (theDataLI & 0x0000FFFF);
		if (fmode != RFM_WRITE)
		{
			return DDC_INVALID_CALL;
		}
		try
		{
			file.writeInt(theData);
			fmode = RFM_WRITE;
		}
		catch (IOException ioe)
		{
			return DDC_FILE_ERROR;
		}
		riff_header.ckSize += NumBytes;
		return DDC_SUCCESS;
	}

	/**
	 * Read NumBytes data.
	 */
	public int read(byte[] Data, int NumBytes)
	{
		int retcode = DDC_SUCCESS;
		try
		{
			file.read(Data, 0, NumBytes);
		}
		catch (IOException ioe)
		{
			retcode = DDC_FILE_ERROR;
		}
		return retcode;
	}

	/**
	 * Expect NumBytes data.
	 */
	public int expect(String Data, int NumBytes)
	{
		byte target = 0;
		int cnt = 0;
		try
		{
			while ((NumBytes--) != 0)
			{
				target = file.readByte();
				if (target != Data.charAt(cnt++)) return DDC_FILE_ERROR;
			}
		}
		catch (IOException ioe)
		{
			return DDC_FILE_ERROR;
		}
		return DDC_SUCCESS;
	}

	/**
	 * Close Riff File.
	 * Length is written too.
	 */
	public int close()
	{
		int retcode = DDC_SUCCESS;

		switch (fmode)
		{
			case RFM_WRITE:
				try
				{
					file.seek(0);
					try
					{
						writeHeader_internally(riff_header);
						file.close();
					}
					catch (IOException ioe)
					{
						retcode = DDC_FILE_ERROR;
					}
				}
				catch (IOException ioe)
				{
					retcode = DDC_FILE_ERROR;
				}
				break;

			case RFM_READ:
				try
				{
					file.close();
				}
				catch (IOException ioe)
				{
					retcode = DDC_FILE_ERROR;
				}
				break;
		}
		file = null;
		fmode = RFM_UNKNOWN;
		return retcode;
	}

	/**
	 * Return File Position.
	 */
	public long currentFilePosition()
	{
		long position;
		try
		{
			position = file.getFilePointer();
		}
		catch (IOException ioe)
		{
			position = -1;
		}
		return position;
	}

	/**
	 * Write Data to specified offset.
	 */
	public int backpatchHeader(long FileOffset, RiffChunkHeader data)
	{
		if (file == null)
		{
			return DDC_INVALID_CALL;
		}
		try
		{
			file.seek(FileOffset);
		}
		catch (IOException ioe)
		{
			return DDC_FILE_ERROR;
		}
		return writeHeader_internally(data);
	}

	public int backpatch(long FileOffset, byte[] Data, int NumBytes)
	{
		if (file == null)
		{
			return DDC_INVALID_CALL;
		}
		try
		{
			file.seek(FileOffset);
		}
		catch (IOException ioe)
		{
			return DDC_FILE_ERROR;
		}
		return write(Data, NumBytes);
	}

	/**
	 * Seek in the File.
	 */
	protected int seek(long offset)
	{
		int rc;
		try
		{
			file.seek(offset);
			rc = DDC_SUCCESS;
		}
		catch (IOException ioe)
		{
			rc = DDC_FILE_ERROR;
		}
		return rc;
	}

	/**
	 * Error Messages.
	 */
	//   private String DDCRET_String(int retcode)
	//   {
	//   	 switch ( retcode )
	//   	 {
	//	  case DDC_SUCCESS:          return "DDC_SUCCESS";
	//	  case DDC_FAILURE:          return "DDC_FAILURE";
	//	  case DDC_OUT_OF_MEMORY:    return "DDC_OUT_OF_MEMORY";
	//	  case DDC_FILE_ERROR:       return "DDC_FILE_ERROR";
	//	  case DDC_INVALID_CALL:     return "DDC_INVALID_CALL";
	//	  case DDC_USER_ABORT:       return "DDC_USER_ABORT";
	//	  case DDC_INVALID_FILE:     return "DDC_INVALID_FILE";
	//     }
	//     return "Unknown Error";   
	//   }
	/**
	 * Fill the header.
	 */
	public static int fourCC(String ChunkName)
	{
		byte[] p =
		{
				0x20, 0x20, 0x20, 0x20
		};
		System.arraycopy(ChunkName.getBytes(), 0, p, 0, 4);
		int ret = (((p[0] << 24) & 0xFF000000) | ((p[1] << 16) & 0x00FF0000) | ((p[2] << 8) & 0x0000FF00) | (p[3] & 0x000000FF));
		return ret;
	}

}
