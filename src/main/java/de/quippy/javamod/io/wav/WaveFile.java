/*
 * @(#) WaveFile.java
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
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *----------------------------------------------------------------------
 */

package de.quippy.javamod.io.wav;

import java.io.File;

import javax.sound.sampled.AudioFormat;

/**
 * Class allowing WaveFormat Access
 */
public class WaveFile extends RiffFile
{
	private static class WaveFormat_ChunkData
	{
		public short wFormatTag; // Format category (PCM=1)
		public short nChannels; // Number of channels (mono=1, stereo=2)
		public int nSamplesPerSec; // Sampling rate [Hz]
		public int nAvgBytesPerSec;
		public short nBlockAlign;
		public short nBitsPerSample;

		public WaveFormat_ChunkData()
		{
			wFormatTag = 1; // PCM
			config(44100, (short) 16, (short) 1);
		}

		public void config(int NewSamplingRate, short NewBitsPerSample, short NewNumChannels)
		{
			nSamplesPerSec = NewSamplingRate;
			nChannels = NewNumChannels;
			nBitsPerSample = NewBitsPerSample;
			nAvgBytesPerSec = (nChannels * nSamplesPerSec * nBitsPerSample) / 8;
			nBlockAlign = (short) ((nChannels * nBitsPerSample) / 8);
		}
	}
	private static class WaveFormat_Chunk
	{
		public RiffChunkHeader header;
		public WaveFormat_ChunkData data;

		public WaveFormat_Chunk()
		{
			header = new RiffChunkHeader();
			header.ckID = fourCC("fmt ");
			header.ckSize = 16;
			data = new WaveFormat_ChunkData();
		}
	}

	private WaveFormat_Chunk wave_format;
	private RiffChunkHeader pcm_data;
	private long pcm_data_offset;
	/**
	 * Constructs a new WaveFile instance. 
	 */
	public WaveFile()
	{
		super();
		wave_format = new WaveFormat_Chunk();
		pcm_data = new RiffChunkHeader();
		pcm_data.ckID = fourCC("data");
		pcm_data.ckSize = 0;
	}

	public int openForWrite(File file, AudioFormat format)
	{
		return openForWrite(file.getAbsolutePath(), (int)format.getSampleRate(), (short)format.getSampleSizeInBits(), (short)format.getChannels());
	}
	/**
	 * Open for write using another wave file's parameters...
	 */
	public int openForWrite(String Filename, WaveFile OtherWave)
	{
		return openForWrite(Filename, OtherWave.getSamplingRate(), OtherWave.getBitsPerSample(), OtherWave.getNumChannels());
	}
	/**
	 *
	 */
	public int openForWrite(String Filename, int SamplingRate, short BitsPerSample, short NumChannels)
	{
		// Verify parameters...
		if (Filename == null)
		{
			return DDC_INVALID_CALL;
		}

		wave_format.data.config(SamplingRate, BitsPerSample, NumChannels);

		int retcode = open(Filename, RFM_WRITE);

		if (retcode == DDC_SUCCESS)
		{
			byte[] theWave =
			{
					(byte) 'W', (byte) 'A', (byte) 'V', (byte) 'E'
			};
			retcode = write(theWave, 4);

			if (retcode == DDC_SUCCESS)
			{
				writeHeader(wave_format.header);
				write(wave_format.data.wFormatTag, 2);
				write(wave_format.data.nChannels, 2);
				write(wave_format.data.nSamplesPerSec, 4);
				write(wave_format.data.nAvgBytesPerSec, 4);
				write(wave_format.data.nBlockAlign, 2);
				write(wave_format.data.nBitsPerSample, 2);

				if (retcode == DDC_SUCCESS)
				{
					pcm_data_offset = currentFilePosition();
					retcode = writeHeader(pcm_data);
				}
			}
		}

		return retcode;
	}

	/**
	 * @param data
	 * @param start
	 * @param numBytes
	 * @return
	 * @see javazoom.jl.converter.RiffFile#write(byte[], int, int)
	 */
	public int writeSamples(byte[] data, int start, int numBytes)
	{
		pcm_data.ckSize += numBytes;
		return write(data, start, numBytes);
	}

	/**
	 * @param data
	 * @param numBytes
	 * @return
	 * @see javazoom.jl.converter.RiffFile#write(byte[], int)
	 */
	public int writeSamples(byte[] data, int numBytes)
	{
		pcm_data.ckSize += numBytes;
		return write(data, numBytes);
	}
	/**
	 * Write 16-bit audio
	 */
	public int writeSamples(short[] data, int numSamples)
	{
		int numBytes = numSamples<<1;
		byte[] theData = new byte[numBytes];
		for (int y = 0, yc=0; y<numBytes; y+=2)
		{
			theData[y] = (byte) (data[yc] & 0x00FF);
			theData[y + 1] = (byte) ((data[yc++] >>> 8) & 0x00FF);
		}
		return write(theData, numBytes);
	}
	/**
	 *
	 */
	public int close()
	{
		int rc = DDC_SUCCESS;

		if (fmode == RFM_WRITE) rc = backpatchHeader(pcm_data_offset, pcm_data);
		rc = super.close();
		return rc;
	}
	public int getSamplingRate()
	{
		return wave_format.data.nSamplesPerSec;
	}
	public short getBitsPerSample()
	{
		return wave_format.data.nBitsPerSample;
	}
	public short getNumChannels()
	{
		return wave_format.data.nChannels;
	}
}