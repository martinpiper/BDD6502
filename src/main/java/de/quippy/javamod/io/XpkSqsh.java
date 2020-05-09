/*
 * @(#) XpkSqsh.java
 * Created on 01.04.2013 by Daniel Becker
 * -----------------------------------------------------------------------
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * ----------------------------------------------------------------------
 */
package de.quippy.javamod.io;

import java.io.IOException;

/**
 * This code is from the class gamod.unpack.XpkSqsh
 * by Josef Jelinek
 * 
 * @author Josef Jelinek, Daniel Becker
 * @since 01.04.2013
 */
public class XpkSqsh
{
	private static final int HEADER_LENGTH = 36;
	private static final byte[] OCTETS =
	{
	    2, 3, 4, 5, 6, 7, 8, 0,
	    3, 2, 4, 5, 6, 7, 8, 0,
	    4, 3, 5, 2, 6, 7, 8, 0,
	    5, 4, 6, 2, 3, 7, 8, 0,
	    6, 5, 7, 2, 3, 4, 8, 0,
	    7, 6, 8, 2, 3, 4, 5, 0,
	    8, 7, 6, 2, 3, 4, 5, 0,
	};

	private byte[] buffer;

	private class BitData
	{
		private final byte[] p;
		private final int base;
		private int i;

		public BitData(byte[] data, int index)
		{
			p = data;
			base = index;
			i = 0;
		}

		public int getBit()
		{
			int r = (p[base + i / 8] >> 7 - i % 8) & 1;
			i++;
			return r;
		}

		public int getBits(int count)
		{
			int b = 0;
			for (int k = 0; k < count; k++)
				b = b << 1 | getBit();
			return b;
		}

		public int getSignBits(int count)
		{
			return getBits(count) << (32 - count) >> (32 - count);
		}
	}

	/**
	 * Constructor for XpkSqsh
	 */
	public XpkSqsh(RandomAccessInputStream input) throws IOException
	{
		buffer = readAndUnpack(input);
	}
	/**
	 * @since 01.04.2013
	 * @return
	 */
	public byte[] getBuffer()
	{
		return buffer;
	}
	/**
	 * @since 01.04.2013
	 * @param input
	 * @return
	 * @throws IOException
	 */
	public static boolean isXPK_SQSH(RandomAccessInputStream input) throws IOException
	{
		long pos = input.getFilePointer();
		input.seek(0);
		byte[] xpkSqshId = new byte[12];
		input.read(xpkSqshId, 0, 12);
		input.seek(pos);
		if (xpkSqshId[0] != 'X' || xpkSqshId[1] != 'P' || xpkSqshId[2] != 'K' || xpkSqshId[3] != 'F') return false;
		if (xpkSqshId[8] != 'S' || xpkSqshId[9] != 'Q' || xpkSqshId[10] != 'S' || xpkSqshId[11] != 'H') return false;
		return true;
	}
	private byte[] readAndUnpack(RandomAccessInputStream source) throws IOException
	{
		source.seek(0); // Just in case...
		return unpackData(source);
	}

	/*** Jelineks Coding *****/
	private boolean testData(byte[] a)
	{
		if (a.length < 12) return false;
		if ('X' != a[0] || 'P' != a[1] || 'K' != a[2] || 'F' != a[3]) return false;
		if ('S' != a[8] || 'Q' != a[9] || 'S' != a[10] || 'H' != a[11]) return false;
		return true;
	}
	private int getLength(byte[] a, int i)
	{
		return (a[i] & 0x7F) << 24 | (a[i + 1] & 0xFF) << 16 | (a[i + 2] & 0xFF) << 8 | a[i + 3] & 0xFF;
	}
	public byte[] unpackData(RandomAccessInputStream source) throws IOException
	{
		byte[] data = new byte[16];
		source.read(data, 0, 16);
		final int orig_length = getLength(data, 4);
		final int target_length = getLength(data, 12);
		if (testData(data) && orig_length == source.getLength() - 8)
		{
			final byte[] dst = new byte[target_length];
			if (unpack(source, dst, HEADER_LENGTH)) return dst;
		}
		return null;
	}

	private boolean unpack(RandomAccessInputStream source, byte[] dst, final int srcPos) throws IOException
	{
		int dstPos = 0;
		source.seek(srcPos);
		while (dstPos < dst.length)
		{
			int chunkType = source.read() & 0xFF;
			int headerChecksum = source.read() & 0xFF;
			int dataChecksum = (source.read() & 0xFF) << 8 | source.read() & 0xFF;
			int packedLength = (source.read() & 0xFF) << 8 | source.read() & 0xFF;
			int unpackedLength = (source.read() & 0xFF) << 8 | source.read() & 0xFF;
			headerChecksum ^= chunkType;
			headerChecksum ^= dataChecksum >> 8 ^ dataChecksum & 255;
			headerChecksum ^= packedLength >> 8 ^ packedLength & 255;
			headerChecksum ^= unpackedLength >> 8 ^ unpackedLength & 255;
			if (headerChecksum != 0) return false;
			packedLength = (packedLength + 3) & 0xFFFC;
			if (source.getFilePointer() + packedLength + 1 > source.getLength()) return false;
			
			final byte[] src = new byte[packedLength];
			if (source.read(src, 0, packedLength) != packedLength) return false;
			
			for (int i = 0; i < packedLength; i += 2)
				dataChecksum ^= (src[i] & 0xFF) << 8 | src[i + 1] & 0xFF;
			if (dataChecksum != 0) return false;
			if (dstPos + unpackedLength > dst.length) return false;
			
			if (chunkType == 0)
			{
				System.arraycopy(src, 0, dst, dstPos, unpackedLength);
			}
			else 
			if (chunkType == 1)
			{
				if (!unsqsh(src, 0, dst, dstPos, dstPos + unpackedLength)) return false;
			}
			else
			{
				return false;
			}
			dstPos += unpackedLength;
		}
		return true;
	}

	private boolean unsqsh(byte[] src, int srcPos, byte[] dst, int dstPos, int dstEnd)
	{
		if (dstEnd - dstPos != ((src[srcPos] & 255) << 8 | src[srcPos + 1] & 255)) return false;
		int expandCounter = 0, expandCount = 0, bitCount = 0;
		if (dstPos >= dst.length) return false;
		int last = dst[dstPos++] = src[srcPos + 2];
		BitData data = new BitData(src, srcPos + 3);
		while (dstPos < dstEnd)
		{
			boolean b1 = data.getBit() == 1;
			if (b1 && expandCounter < 8 || !b1 && expandCounter >= 8 && data.getBit() == 0)
			{
				int count = getCopyCount(data);
				dstPos = copyBlock(data, dst, dstPos, count);
				last = dst[dstPos - 1];
				expandCounter -= count < 3 || expandCounter == 0 ? 0 : count == 3 || expandCounter == 1 ? 1 : 2;
			}
			else
			{
				bitCount = expandCounter < 8 ? 8 : b1 ? bitCount : getExpandBitCount(data, bitCount);
				if (expandCounter >= 8 && (bitCount != 8 || expandCount >= 20))
				{
					if (bitCount != 8)
					{
						if (dstPos < dst.length) last = dst[dstPos++] = (byte) (last - data.getSignBits(bitCount));
						if (dstPos < dst.length) last = dst[dstPos++] = (byte) (last - data.getSignBits(bitCount));
						if (dstPos < dst.length) last = dst[dstPos++] = (byte) (last - data.getSignBits(bitCount));
					}
					if (dstPos < dst.length) last = dst[dstPos++] = (byte) (last - data.getSignBits(bitCount));
					expandCount += 8;
				}
				if (dstPos < dst.length) last = dst[dstPos++] = (byte) (last - data.getSignBits(bitCount));
				expandCounter += expandCounter < 31 ? 1 : 0;
			}
			expandCount -= expandCount / 8;
		}
		return true;
	}

	private int copyBlock(BitData data, byte[] dst, int dstPos, int count)
	{
		int bitCount = getCopyOffsetBitCount(data);
		int offsetBase = getCopyOffsetBase(bitCount);
		int winPos = dstPos - 1 - offsetBase - data.getBits(bitCount);
		for (int i = 0; i < count; i++)
			if (dstPos + i < dst.length) dst[dstPos + i] = dst[winPos + i];
		return Math.min(dst.length, dstPos + count);
	}

	private int getCopyCount(BitData data)
	{
		if (data.getBit() == 0) return 2 + data.getBit();
		if (data.getBit() == 0) return 4 + data.getBit();
		if (data.getBit() == 0) return 6 + data.getBit();
		if (data.getBit() == 0) return 8 + data.getBits(3);
		return 16 + data.getBits(5);
	}

	private int getExpandBitCount(BitData data, int bitCount)
	{
		if (data.getBit() == 0) return OCTETS[8 * bitCount - 15];
		if (data.getBit() == 0) return OCTETS[8 * bitCount - 14];
		return OCTETS[8 * bitCount + data.getBits(2) - 13];
	}

	private int getCopyOffsetBitCount(BitData data)
	{
		if (data.getBit() == 1) return 12;
		if (data.getBit() == 1) return 14;
		return 8;
	}

	private int getCopyOffsetBase(int bitCount)
	{
		if (bitCount == 12) return 0x100;
		if (bitCount == 14) return 0x1100;
		return 0;
	}
}
