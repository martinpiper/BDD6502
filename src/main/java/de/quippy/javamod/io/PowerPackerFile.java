/*
 * @(#) PowerPackerFile.java
 *
 * Created on 06.01.2010 by Daniel Becker
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

/* The C Routine for PowerPacking
typedef struct _PPBITBUFFER
{
	UINT bitcount;
	ULONG bitbuffer;
	LPCBYTE pStart;
	LPCBYTE pSrc;

	ULONG GetBits(UINT n);
} PPBITBUFFER;


ULONG PPBITBUFFER::GetBits(UINT n)
{
	ULONG result = 0;

	for (UINT i=0; i<n; i++)
	{
		if (!bitcount)
		{
			bitcount = 8;
			if (pSrc != pStart) pSrc--;
			bitbuffer = *pSrc;
		}
		result = (result<<1) | (bitbuffer&1);
		bitbuffer >>= 1;
		bitcount--;
    }
    return result;
}


VOID PP20_DoUnpack(const BYTE *pSrc, UINT nSrcLen, BYTE *pDst, UINT nDstLen)
{
	PPBITBUFFER BitBuffer;
	ULONG nBytesLeft;

	BitBuffer.pStart = pSrc;
	BitBuffer.pSrc = pSrc + nSrcLen - 4;
	BitBuffer.bitbuffer = 0;
	BitBuffer.bitcount = 0;
	BitBuffer.GetBits(pSrc[nSrcLen-1]);
	nBytesLeft = nDstLen;
	while (nBytesLeft > 0)
	{
		if (!BitBuffer.GetBits(1))
		{
			UINT n = 1;
			while (n < nBytesLeft)
			{
				UINT code = BitBuffer.GetBits(2);
				n += code;
				if (code != 3) break;
			}
			for (UINT i=0; i<n; i++)
			{
				pDst[--nBytesLeft] = (BYTE)BitBuffer.GetBits(8);
			}
			if (!nBytesLeft) break;
		}
		{
			UINT n = BitBuffer.GetBits(2)+1;
			UINT nbits = pSrc[n-1];
			UINT nofs;
			if (n==4)
			{
				nofs = BitBuffer.GetBits( (BitBuffer.GetBits(1)) ? nbits : 7 );
				while (n < nBytesLeft)
				{
					UINT code = BitBuffer.GetBits(3);
					n += code;
					if (code != 7) break;
				}
			} else
			{
				nofs = BitBuffer.GetBits(nbits);
			}
			for (UINT i=0; i<=n; i++)
			{
				pDst[nBytesLeft-1] = (nBytesLeft+nofs < nDstLen) ? pDst[nBytesLeft+nofs] : 0;
				if (!--nBytesLeft) break;
			}
		}
	}
}


BOOL PP20_Unpack(LPCBYTE *ppMemFile, LPDWORD pdwMemLength)
{
	DWORD dwMemLength = *pdwMemLength;
	LPCBYTE lpMemFile = *ppMemFile;
	DWORD dwDstLen;
	LPBYTE pBuffer;

	if ((!lpMemFile) || (dwMemLength < 256) || (*(DWORD *)lpMemFile != '02PP')) return FALSE;
	dwDstLen = (lpMemFile[dwMemLength-4]<<16) | (lpMemFile[dwMemLength-3]<<8) | (lpMemFile[dwMemLength-2]);
	//Log("PP20 detected: Packed length=%d, Unpacked length=%d\n", dwMemLength, dwDstLen);
	if ((dwDstLen < 512) || (dwDstLen > 0x400000) || (dwDstLen > 16*dwMemLength)) return FALSE;
	if ((pBuffer = (LPBYTE)GlobalAllocPtr(GHND, (dwDstLen + 31) & ~15)) == NULL) return FALSE;
	PP20_DoUnpack(lpMemFile+4, dwMemLength-4, pBuffer, dwDstLen);
	*ppMemFile = pBuffer;
	*pdwMemLength = dwDstLen;
	return TRUE;
}
 
 */
package de.quippy.javamod.io;

import java.io.IOException;

import de.quippy.javamod.system.Helpers;

/**
 * This class will decompress the input from any inputStream into an internal
 * buffer with the powerpacker algorithem and give access to this buffer
 * as an RandomAccessInputStream
 * @author Daniel Becker
 * @since 06.01.2010
 */
public class PowerPackerFile
{
	private byte [] buffer;
	/**
	 * Will read n bits from a file
	 * @author Daniel Becker
	 * @since 06.01.2010
	 */
	private static class BitBuffer
	{
		private RandomAccessInputStream source;
		private int filePointer;
		private int bitCount;
		private int bitBuffer;
		
		public BitBuffer(RandomAccessInputStream source, int filePointer)
		{
			this.source = source;
			this.filePointer = filePointer;
			bitCount = 0;
			bitBuffer = 0;
		}

		public int getBits(int n) throws IOException
		{
			int result = 0;

			for (int i=0; i<n; i++)
			{
				if (bitCount == 0)
				{
					bitCount = 8;
					if (filePointer>3) filePointer--;
					source.seek(filePointer);
					bitBuffer = source.read();
				}
				result = (result<<1) | (bitBuffer & 1);
				bitBuffer >>= 1;
				bitCount--;
		    }
		    return result;
		}
	}
	/**
	 * Constructor for PowerPackerInputStream
	 */
	public PowerPackerFile(RandomAccessInputStream input) throws IOException
	{
		buffer = readAndUnpack(input);
	}
	/**
	 * @since 04.01.2011
	 * @return
	 */
	public byte [] getBuffer()
	{
		return buffer;
	}
	/**
	 * Will check for a power packer file
	 * @since 04.01.2011
	 * @param input
	 * @return true if this file is a powerpacker file
	 * @throws IOException
	 */
	public static boolean isPowerPacker(RandomAccessInputStream input) throws IOException
	{
		long pos = input.getFilePointer();
		input.seek(0);
		byte [] ppId = new byte [4];
		input.read(ppId, 0, 4);
		input.seek(pos);
		return Helpers.retrieveAsString(ppId, 0, 4).equals("PP20");
	}
	/**
	 * Will unpack powerpacker 2.0 packed contend while reading from the packed Stream
	 * and unpacking into memory
	 * @since 06.01.2010
	 * @param source
	 * @param buffer
	 * @throws IOException
	 */
	private void pp20DoUnpack(RandomAccessInputStream source, byte [] buffer) throws IOException
	{
		BitBuffer bitBuffer = new BitBuffer(source, (int)source.getLength()-4);
		source.seek(source.getLength()-1);
		int skip = source.read();
		bitBuffer.getBits(skip);
		int nBytesLeft = buffer.length;
		while (nBytesLeft > 0)
		{
			if (bitBuffer.getBits(1) == 0)
			{
				int n = 1;
				while (n < nBytesLeft)
				{
					int code = (int)bitBuffer.getBits(2);
					n += code;
					if (code != 3) break;
				}
				for (int i=0; i<n; i++)
				{
					buffer[--nBytesLeft] = (byte)bitBuffer.getBits(8);
				}
				if (nBytesLeft == 0) break;
			}
			
			int n = bitBuffer.getBits(2)+1;
			source.seek(n+3);
			int nbits = source.read();
			int nofs;
			if (n==4)
			{
				nofs = bitBuffer.getBits( (bitBuffer.getBits(1)!=0) ? nbits : 7 );
				while (n < nBytesLeft)
				{
					int code = bitBuffer.getBits(3);
					n += code;
					if (code != 7) break;
				}
			} 
			else
			{
				nofs = bitBuffer.getBits(nbits);
			}
			for (int i=0; i<=n; i++)
			{
				buffer[nBytesLeft-1] = (nBytesLeft+nofs < buffer.length) ? buffer[nBytesLeft+nofs] : 0;
				if ((--nBytesLeft)==0) break;
			}
		}
	}
	private byte[] readAndUnpack(RandomAccessInputStream source) throws IOException
	{
		source.seek(0); // Just in case...
		final int PP20ID = source.read()<<24 | source.read()<<16 | source.read()<<8 | source.read();
		final int length = (int)source.getLength();
		if (length<256 || PP20ID != 0x50503230) throw new IOException("Not a powerpacker file!");
		// Destination Length at the end of file:
		source.seek(length - 4);
		final int destLen = source.read()<<16 | source.read() << 8 | source.read();
		if (destLen < 512 || destLen > 0x400000 || destLen > (length<<3)) throw new IOException("Length of " + length + " is not supported!");
		final byte [] dstBuffer = new byte[destLen];
		pp20DoUnpack(source, dstBuffer);
		return dstBuffer;
	}
}
