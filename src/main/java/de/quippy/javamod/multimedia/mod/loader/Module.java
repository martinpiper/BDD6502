/*
 * @(#) Module.java
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

import de.quippy.javamod.io.ModfileInputStream;
import de.quippy.javamod.multimedia.mod.loader.instrument.InstrumentsContainer;
import de.quippy.javamod.multimedia.mod.loader.instrument.Sample;
import de.quippy.javamod.multimedia.mod.loader.pattern.PatternContainer;
import de.quippy.javamod.multimedia.mod.mixer.BasicModMixer;
import de.quippy.javamod.system.Helpers;
import de.quippy.javamod.system.Log;

/**
 * @author Daniel Becker
 * @since 21.04.2006
 */
public abstract class Module
{
	private String fileName;
	private String trackerName;
	private String modID;
	
	private int modType;

	private String songName;
	private int nChannels;
	private int nInstruments;
	private int nSamples;
	private int nPattern;
	private int BPMSpeed;
	private int tempo;
	private InstrumentsContainer instrumentContainer;
	private PatternContainer patternContainer;
	private int songLength;
	private int [] arrangement;
	private boolean [] arrangementPositionPlayed;
	private int baseVolume;
	
	protected int songFlags;
	
	/**
	 * This class is used to decrompress the IT>=2.14 samples
	 * It is a mix from open cubic player and mod plug tracker adopted for
	 * Java by Daniel Becker
	 * 
	 * Read, what Tammo Hinrichs (OCP) wrote to this:
	 * ********************************************************
	 * And to make it even worse: A short (?) description of what the routines
	 * in this file do.
	 * 
	 * It's all about sample compression. Due to the rather "analog" behaviour
	 * of audio streams, it's not always possible to gain high reduction rates
	 * with generic compression algorithms. So the idea is to find an algorithm
	 * which is specialized for the kind of data we're actually dealing with:
	 * mono sample data.
	 * 
	 * in fact, PKZIP etc. is still somewhat better than this algorithm in most
	 * cases, but the advantage of this is it's decompression speed which might
	 * enable sometimes players or even synthesizer chips to decompress IT
	 * samples in real-time. And you can still pack these compressed samples with
	 * "normal" algorithms and get better results than these algorothms would
	 * ever achieve alone.
	 *
	 * some assumptions i made (and which also pulse made - and without which it
	 * would have been impossible for me to figure out the algorithm) :
	 *
	 * - it must be possible to find values which are found more often in the
	 *   file than others. Thus, it's possible to somehow encode the values
	 *   which we come across more often with less bits than the rest.
	 * - In general, you can say that low values (considering distance to
	 *   the null line) are found more often, but then, compression results
	 *   would heavily depend on signal amplitude and DC offsets and such.
	 * - But: ;)
	 * - higher frequencies have generally lower amplitudes than low ones, just
	 *   due to the nature of sound and our ears
	 * - so we could somehow filter the signal to decrease the low frequencies'
	 *   amplitude, thus resulting in lesser overall amplitude, thus again resul-
	 *   ting in better ratios, if we take the above thoughts into consideration.
	 * - every signal can be split into a sum of single frequencies, that is a
	 *   sum of a(f)*sin(f*t) terms (just believe me if you don't already know).
	 * - if we differentiate this sum, we get a sum of (a(f)*f)*cos(f*t). Due to
	 *   f being scaled to the nyquist of the sample frequency, it's always
	 *   between 0 and 1, and we get just what we want - we decrease the ampli-
	 *   tude of the low frequencies (and shift the signal's phase by 90°, but
	 *   that's just a side-effect that doesn't have to interest us)
	 * - the backwards way is simple integrating over the data and is completely
	 *   lossless. good.
	 * - so how to differentiate or integrate a sample stream? the solution is
	 *   simple: we simply use deltas from one sample to the next and have the
	 *   perfectly numerically differentiated curve. When we decompress, we
	 *   just add the value we get to the last one and thus restore the original
	 *   signal.
	 * - then, we assume that the "-1"st sample value is always 0 to avoid nasty
	 *   DC offsets when integrating.
	 *   
	 * ok. now we have a sample stream which definitely contains more low than
	 * high values. How do we compress it now?
	 * 
	 * Pulse had chosen a quite unusual, but effective solution: He encodes the
	 * values with a specific "bit width" and places markers between the values
	 * which indicate if this width would change. He implemented three different
	 * methods for that, depending on the bit width we actually have (i'll write
	 * it down for 8 bit samples, values which change for 16bit ones are in these
	 * brackets [] ;):
	 * 
	 * * method 1: 1 to 6 bits
	 *   there are two possibilities (example uses a width of 6)
	 *   - 100000 (a one with (width-1) zeroes ;) :
	 *     the next 3 [4] bits are read, incremented and used as new width...
	 *     and as it would be completely useless to switch to the same bit
	 *     width again, any value equal or greater the actual width is
	 *     incremented, thus resulting in a range from 1-9 [1-17] bits (which
	 *     we definitely need).
	 *   - any other value is expanded to a signed byte [word], integrated
	 *     and stored.
	 * * method 2: 7 to 8 [16] bits
	 *   again two possibilities (this time using a width of eg. 8 bits)
	 *   - 01111100 to 10000011 [01111000 to 10000111] :
	 *     this value will be subtracted by 01111011 [01110111], thus resulting
	 *     again in a 1-8 [1-16] range which will be expanded to 1-9 [1-17] in
	 *     the same manner as above
	 *   - any other value is again expanded (if necessary), integrated and
	 *     stored
	 * * method 3: 9 [17] bits
	 *   this time it depends on the highest bit:
	 *   - if 0, the last 8 [16] bits will be integrated and stored
	 *   - if 1, the last 8 [16] bits (+1) will be used as new bit width.
	 * any other width isnt supposed to exist and will result in a premature
	 * exit of the decompressor.
	 * 
	 * Few annotations:
	 * - The compressed data is processed in blocks of 0x8000 bytes. I dont
	 *   know the reason of this (it's definitely NOT better concerning compres-
	 *   sion ratio), i just think that it has got something to do with Pulse's
	 *   EMS memory handling or such. Anyway, this was really nasty to find
	 *   out ;)
	 * - The starting bit width is 9 [17]
	 * - IT2.15 compression simply doubles the differentiation/integration
	 *   of the signal, thus eliminating low frequencies some more and turning
	 *   the signal phase to 180° instead of 90° which can eliminate some sig-
	 *   nal peaks here and there - all resulting in a somewhat better ratio.
	 * 
	 * ok, but now lets start... but think before you easily somehow misuse
	 * this code, the algorithm is (C) Jeffrey Lim aka Pulse... and my only
	 * intention is to make IT's file format more open to the Tracker Community
	 * and especially the rest of the scene. Trackers ALWAYS were open standards,
	 * which everyone was able (and WELCOME) to adopt, and I don't think this
	 * should change. There are enough other things in the computer world
	 * which did, let's just not be mainstream, but open-minded. Thanks.
	 * 
	 *                     Tammo Hinrichs [ KB / T.O.M / PuRGE / Smash Designs ]
	 * 
	 * @author Daniel Becker
	 * @since 03.11.2007
	 */
	private static class ITDeCompressor
	{
		// StreamData
		private ModfileInputStream input;
		// Block of Data
		private byte[] sourceBuffer;
		private int sourceIndex;
		// Destination (24Bit signed mono!)
		private int[] destBuffer;
		private int destIndex;
		// Samples to fill
		private int anzSamples;
		// Bits remaining
		private int bitsRemain;
		// true, if we have IT Version >2.15 packed Data
		private boolean isIT215;

		public ITDeCompressor(Sample sample, boolean isIT215, ModfileInputStream inputStream)
		{
			this.input = inputStream;
			this.sourceBuffer = null;
			this.sourceIndex = 0;
			this.bitsRemain = 0;
			this.destBuffer = sample.sample;
			this.destIndex = 0;
			this.anzSamples = sample.length;
			this.isIT215 = isIT215;
		}

		/**
		 * reads b bits from the stream
		 * Works for 8 bit streams but 8 or 16 bit samples
		 * @since 03.11.2007
		 * @param b
		 * @return
		 */
		private int readbits(int b)
		{
			// Slow version but alwayes working and easy to understand
//			long value = 0;
//			int i = b;
//			while (i>0)
//			{
//				if (bitsRemain==0)
//				{
//					sourceIndex++;
//					bitsRemain = 8;
//				}
//				value >>= 1;
//				value |= (((long)sourceBuffer[sourceIndex] & 0x01) << 31) & 0xFFFFFFFF;
//				sourceBuffer[sourceIndex] >>= 1;
//				bitsRemain--;
//				i--;
//			}
//			return (int)((value >> (32 - b)) & 0xFFFFFFFF);
			// adopted version vom OCP - much faster
			long value = 0;
			if (b <= bitsRemain)
			{
				value = sourceBuffer[sourceIndex] & ((1 << b) - 1);
				sourceBuffer[sourceIndex] >>= b;
				bitsRemain -= b;
			}
			else
			{
				int nbits = b - bitsRemain;
				value = ((long)sourceBuffer[sourceIndex++]) & ((1 << bitsRemain) - 1);
				while (nbits>8)
				{
					value |= ((long)(sourceBuffer[sourceIndex++] & 0xFF)) << bitsRemain;
					nbits-=8; bitsRemain += 8;
				}
				value |= ((long)(sourceBuffer[sourceIndex] & ((1 << nbits) - 1))) << bitsRemain;
				sourceBuffer[sourceIndex] >>= nbits;
				bitsRemain = 8 - nbits;
			}
			return (int)(value & 0xFFFFFFFF);
		}

		/**
		 * gets block of compressed data from file
		 * 
		 * @since 03.11.2007
		 * @return
		 */
		private boolean readblock() throws IOException
		{
			if (input.available()==0) return false; // EOF?!
			int size = input.readIntelWord();
			if (size == 0) return false;
			if (input.available()<size) size = input.available(); // Dirty Hack - should never happen
			
			sourceBuffer = new byte[size];
			input.read(sourceBuffer, 0, size);
			sourceIndex = 0;
			bitsRemain = 8;
			return true;
		}

		/**
		 * This will decompress to 8 Bit samples
		 * @since 03.11.2007
		 * @return
		 */
		public boolean decompress8() throws IOException
		{
			int blklen;		// length of compressed data block in samples
			int blkpos;		// position in block
			int width;		// actual "bit width"
			int value;		// value read from file to be processed
			byte d1, d2;	// integrator buffers (d2 for it2.15)

			// now unpack data till the dest buffer is full
			while (anzSamples > 0)
			{
				// read a new block of compressed data and reset variables
				if (!readblock()) return false;
				blklen = (anzSamples < 0x8000) ? anzSamples : 0x8000;
				blkpos = 0;

				width = 9; // start with width of 9 bits
				d1 = d2 = 0; // reset integrator buffers
				// now uncompress the data block
				while (blkpos < blklen)
				{
					value = readbits(width); // read bits

					if (width < 7) // method 1 (1-6 bits)
					{
						if (value == (1 << (width - 1))) // check for "100..."
						{
							value = readbits(3) + 1; // yes -> read new width;
							width = (value < width) ? value : value + 1; // and expand it
							continue; // ... next value
						}
					}
					else if (width < 9) // method 2 (7-8 bits)
					{
						int border = (0xFF >> (9 - width)) - 4; // lower border for width chg

						if (value > border && value <= (border + 8))
						{
							value -= border; // convert width to 1-8
							width = (value < width) ? value : value + 1; // and expand it
							continue; // ... next value
						}
					}
					else if (width == 9) // method 3 (9 bits)
					{
						if ((value & 0x100) != 0) // bit 8 set?
						{
							width = (value + 1) & 0xFF; // new width...
							continue; // ... and next value
						}
					}
					else
					// illegal width, abort
					{
						return false;
					}

					// now expand value to signed byte
					byte v; // sample value
					if (width < 8)
					{
						int shift = 8 - width;
						v = (byte)((value << shift)&0xFF);
						v >>= shift;
					}
					else
						v = (byte)(value & 0xFF);

					// integrate upon the sample values
					d1 += v;
					d2 += d1;

					// ... and store it into the buffer
					this.destBuffer[destIndex++] = Helpers.promoteSigned8BitToSigned24Bit((isIT215) ? d2 : d1);
					blkpos++;
				}

				// now subtract block lenght from total length and go on
				anzSamples -= blklen;
			}

			return true;
		}
		/**
		 * This will decompress to 16 Bit samples
		 * @since 03.11.2007
		 * @return
		 */
		public boolean decompress16() throws IOException
		{
			int blklen;		// length of compressed data block in samples
			int blkpos;		// position in block
			int width;		// actual "bit width"
			int value;		// value read from file to be processed
			short d1, d2;	// integrator buffers (d2 for it2.15)

			// now unpack data till the dest buffer is full
			while (anzSamples > 0)
			{
				// read a new block of compressed data and reset variables
				if (!readblock()) return false;
				blklen = (anzSamples < 0x4000) ? anzSamples : 0x4000; // 0x4000 samples => 0x8000 bytes again
				blkpos = 0;

				width = 17; // start with width of 17 bits
				d1 = d2 = 0; // reset integrator buffers

				// now uncompress the data block
				while (blkpos < blklen)
				{
					value = readbits(width); // read bits

					if (width < 7) // method 1 (1-6 bits)
					{
						if (value == (1 << (width - 1))) // check for "100..."
						{
							value = readbits(4) + 1; // yes -> read new width;
							width = (value < width) ? value : value + 1; // and expand it
							continue; // ... next value
						}
					}
					else if (width < 17) // method 2 (7-16 bits)
					{
						int border = (0xFFFF >> (17 - width)) - 8; // lower border for width chg

						if (value > border && value <= (border + 16))
						{
							value -= border; // convert width to 1-8
							width = (value < width) ? value : value + 1; // and expand it
							continue; // ... next value
						}
					}
					else if (width == 17) // method 3 (17 bits)
					{
						if ((value & 0x10000) != 0) // bit 16 set?
						{
							width = (value + 1) & 0xFF; // new width...
							continue; // ... and next value
						}
					}
					else
					// illegal width, abort
					{
						return false;
					}

					// now expand value to signed word
					short v; // sample value
					if (width < 16)
					{
						int shift = 16 - width;
						v = (short) ((value << shift) & 0xFFFF);
						v >>= shift;
					}
					else
						v = (short) value;

					// integrate upon the sample values
					d1 += v;
					d2 += d1;

					// ... and store it into the buffer
					this.destBuffer[destIndex++] = Helpers.promoteSigned16BitToSigned24Bit((isIT215) ? d2 : d1);
					blkpos++;
				}

				// now subtract block lenght from total length and go on
				anzSamples -= blklen;
			}

			return true;
		}
	}
	
	/**
	 * Constructor for Module
	 */
	public Module()
	{
		super();
	}
	/**
	 * Constructor for Module
	 */
	protected Module(String fileName)
	{
		this();
		this.fileName = fileName;
	}
	/**
	 * Loads a Module. This Method will delegate the task to loadModFile(InputStream)
	 * 
	 * @param fileName
	 * @return
	 */
	public Module loadModFile(String fileName) throws IOException
	{
		return loadModFile(new File(fileName));
	}
	/**
	 * Loads a Module.
	 * This Method will delegate the task to loadModFile(URL)
	 * @param file
	 * @return
	 */
	public Module loadModFile(File file) throws IOException
	{
//		if (!file.exists()) // Wenn die Datei nicht existiert, werden wir es mit getRessource mal probieren...
//		{
//			ClassLoader classLoader = ModuleFactory.class.getClassLoader();
//			InputStream inputStream = classLoader.getResourceAsStream(file.getAbsolutePath());
//			int size = inputStream.available();
//		}
		return loadModFile(file.toURI().toURL());
	}
	/**
	 * @since 12.10.2007
	 * @param url
	 * @return
	 */
	public Module loadModFile(URL url) throws IOException
	{
		ModfileInputStream inputStream = null;
		try
		{
			inputStream = new ModfileInputStream(url);
			return loadModFile(inputStream);
		}
		finally
		{
			if (inputStream!=null) try { inputStream.close(); } catch (Exception ex) { Log.error("IGNORED", ex); }
		}
	}
	/**
	 * @since 31.12.2007
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	public Module loadModFile(ModfileInputStream inputStream) throws IOException
	{
		Module mod = this.getNewInstance(inputStream.getFileName());
		mod.loadModFileInternal(inputStream);
		return mod;
	}
	/**
	 * Returns true if the loader thinks this mod can be loaded by him
	 * @since 10.01.2010
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	public abstract boolean checkLoadingPossible(ModfileInputStream inputStream) throws IOException;
	/**
	 * Create an Instance of your own - is used by loadModFile before loadModFileInternal is called
	 * @since 10.01.2010
	 * @return
	 */
	protected abstract Module getNewInstance(String fileName);
	/**
	 * @since 31.12.2007
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	protected abstract void loadModFileInternal(ModfileInputStream inputStream) throws IOException;
	/**
	 * @return Returns the mixer.
	 */
	public abstract BasicModMixer getModMixer(int sampleRate, int doISP, int doNoLoops);
	/**
	 * Retrieve the file extension list this loader/player is used for
	 */
	public abstract String [] getFileExtensionList();
	/**
	 * Give panning value 0..256 (128 is center)
	 * @param channel
	 * @return
	 */
	public abstract int getPanningValue(int channel);
	/**
	 * Give the channel volume for this channel. 0->64
	 * @since 25.06.2006
	 * @param channel
	 * @return
	 */
	public abstract int getChannelVolume(int channel);
	/**
	 * Return 0: Amiga Mod Frequencytable (like mod, s3m, nst, wow...)
	 * Return 1: XM IT AmigaMod Table
	 * Return 2: XM IT Linear Frequency Table
	 * @return
	 */
	public abstract int getFrequencyTable();
	/**
	 * For s3m this is neccessary!
	 * @return
	 */
	public abstract boolean doFastSlides();
	
	/**
	 * @since 29.03.2010
	 * @return
	 */
	public String toShortInfoString()
	{
		StringBuilder modInfo = new StringBuilder(getTrackerName());
		modInfo.append(" mod with ").append(getNSamples()).append(" samples and ").append(getNChannels()).append(" channels using ");
		switch (getFrequencyTable())
		{
			case Helpers.AMIGA_TABLE: modInfo.append("Protracker"); break;
			case Helpers.STM_S3M_TABLE: modInfo.append("Scream Tracker"); break;
			case Helpers.XM_AMIGA_TABLE: modInfo.append("Fast Tracker log"); break;
			case Helpers.XM_LINEAR_TABLE: modInfo.append("Fast Tracker linear"); break;
			case Helpers.IT_LINEAR_TABLE: modInfo.append("Impuls Tracker linear"); break;
			case Helpers.IT_AMIGA_TABLE: modInfo.append("Impuls Tracker log"); break;
		}
		modInfo.append(" frequency table");
		return modInfo.toString();
	}
	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		StringBuilder modInfo = new StringBuilder(toShortInfoString());
		modInfo.append("\n\nSong named: ");
		modInfo.append(getSongName()).append('\n');
		modInfo.append(getInstrumentContainer().toString());
		return modInfo.toString();
	}
	protected void allocArrangement(int length)
	{
		arrangement = new int[length];
		arrangementPositionPlayed = new boolean[length];
	}
	/**
	 * @return Returns the arrangement.
	 */
	public int[] getArrangement()
	{
		return arrangement;
	}
	/**
	 * @param arrangement The arrangement to set.
	 */
	public void setArrangement(int[] arrangement)
	{
		this.arrangement = arrangement;
	}
	/**
	 * Automatically cleans up the arrangement data (if illegal pattnums
	 * are in there...)
	 * @since 03.10.2010
	 */
	public void cleanUpArrangement()
	{
		int illegalPatternNum = 0;
		for (int i=0; i<songLength; i++)
		{
			if (arrangement[i-illegalPatternNum]>=nPattern)
			{
				illegalPatternNum++;
				System.arraycopy(arrangement, i+1, arrangement, i, arrangement.length - i - 1);
			}
		}
		songLength -= illegalPatternNum;
	}
	public void resetLoopRecognition()
	{
		for (int i=0; i<arrangementPositionPlayed.length; i++) arrangementPositionPlayed[i] = false;
		getPatternContainer().resetRowsPlayed();
	}
	public boolean isArrangementPositionPlayed(int position)
	{
		return arrangementPositionPlayed[position];
	}
	public void setArrangementPositionPlayed(int position)
	{
		arrangementPositionPlayed[position] = true;
	}
	/**
	 * @return Returns the bPMSpeed.
	 */
	public int getBPMSpeed()
	{
		return BPMSpeed;
	}
	/**
	 * @param speed The bPMSpeed to set.
	 */
	protected void setBPMSpeed(int speed)
	{
		BPMSpeed = speed;
	}
	/**
	 * @return Returns the instruments.
	 */
	public InstrumentsContainer getInstrumentContainer()
	{
		return instrumentContainer;
	}
	/**
	 * @param instruments The instruments to set.
	 */
	protected void setInstrumentContainer(InstrumentsContainer instrumentContainer)
	{
		this.instrumentContainer = instrumentContainer;
	}
	/**
	 * @return Returns the nChannels.
	 */
	public int getNChannels()
	{
		return nChannels;
	}
	/**
	 * @param channels The nChannels to set.
	 */
	protected void setNChannels(int channels)
	{
		nChannels = channels;
	}
	/**
	 * @return Returns the nPattern.
	 */
	public int getNPattern()
	{
		return nPattern;
	}
	/**
	 * @param pattern The nPattern to set.
	 */
	protected void setNPattern(int pattern)
	{
		nPattern = pattern;
	}
	/**
	 * @return Returns the nInstruments.
	 */
	public int getNInstruments()
	{
		return nInstruments;
	}
	/**
	 * @param samples The nInstruments to set.
	 */
	protected void setNInstruments(int instruments)
	{
		nInstruments = instruments;
	}
	/**
	 * @return Returns the nSamples.
	 */
	public int getNSamples()
	{
		return nSamples;
	}
	/**
	 * @param samples The nSamples to set.
	 */
	protected void setNSamples(int samples)
	{
		nSamples = samples;
	}
	/**
	 * @return Returns the songLength.
	 */
	public int getSongLength()
	{
		return songLength;
	}
	/**
	 * @param songLength The songLength to set.
	 */
	protected void setSongLength(int songLength)
	{
		this.songLength = songLength;
	}
	/**
	 * @return Returns the songName.
	 */
	public String getSongName()
	{
		return songName;
	}
	/**
	 * @param songName The songName to set.
	 */
	protected void setSongName(String songName)
	{
		this.songName = songName;
	}
	/**
	 * @return Returns the tempo.
	 */
	public int getTempo()
	{
		return tempo;
	}
	/**
	 * @param tempo The tempo to set.
	 */
	protected void setTempo(int tempo)
	{
		this.tempo = tempo;
	}
	/**
	 * @return Returns the trackerName.
	 */
	public String getTrackerName()
	{
		return trackerName;
	}
	/**
	 * @param trackerName The trackerName to set.
	 */
	protected void setTrackerName(String trackerName)
	{
		this.trackerName = trackerName;
	}
	/**
	 * @return Returns the patternContainer.
	 */
	public PatternContainer getPatternContainer()
	{
		return patternContainer;
	}
	/**
	 * @param patternContainer The patternContainer to set.
	 */
	protected void setPatternContainer(PatternContainer patternContainer)
	{
		this.patternContainer = patternContainer;
	}
	/**
	 * @return the fileName
	 */
	public String getFileName()
	{
		return fileName;
	}
	/**
	 * @return Returns the modID.
	 */
	public String getModID()
	{
		return modID;
	}
	/**
	 * @param modID The modID to set.
	 */
	protected void setModID(String modID)
	{
		this.modID = modID;
	}
	/**
	 * @return Returns the baseVolume.
	 */
	public int getBaseVolume()
	{
		return baseVolume;
	}
	/**
	 * @param baseVolume The baseVolume to set.
	 */
	protected void setBaseVolume(int baseVolume)
	{
		this.baseVolume = baseVolume;
	}
	/**
	 * @return the songFlags
	 */
	public int getSongFlags()
	{
		return songFlags;
	}
	/**
	 * @param songFlags the songFlags to set
	 */
	protected void setSongFlags(int songFlags)
	{
		this.songFlags = songFlags;
	}
	/**
	 * @return Returns the modType.
	 */
	public int getModType()
	{
		return modType;
	}
	/**
	 * @param modType The modType to set.
	 */
	protected void setModType(int modType)
	{
		this.modType = modType;
	}
	/**
	 * Loads samples
	 * @since 03.11.2007
	 * @param current
	 * @param flags
	 * @param input
	 * @param offset
	 * @return the new offset after loading
	 */
	protected void readSampleData(Sample current, int flags, ModfileInputStream inputStream) throws IOException
	{
		if (current.length>0)
		{
			current.allocSampleData();
			if ((flags & Helpers.SM_IT21416)==Helpers.SM_IT21416 || (flags & Helpers.SM_IT21516)==Helpers.SM_IT21516)
			{
				ITDeCompressor reader = new ITDeCompressor(current, (flags & Helpers.SM_IT21516)==Helpers.SM_IT21516, inputStream);
				reader.decompress16();
			}
			else
			if ((flags & Helpers.SM_IT2148)==Helpers.SM_IT2148 || (flags & Helpers.SM_IT2158)==Helpers.SM_IT2158)
			{
				ITDeCompressor reader = new ITDeCompressor(current, (flags & Helpers.SM_IT2158)==Helpers.SM_IT2158, inputStream);
				reader.decompress8();
			}
			else
			{
				int old = 0;
				for (int s=0; s<current.length; s++)
				{
					if ((flags&Helpers.SM_16BIT)!=0) // 16 Bit Samples
					{
						short sample = (short)inputStream.readIntelWord();
						if ((flags&Helpers.SM_PCMD)!=0)
						{
							sample += old;
							old = sample;
						}
						
						if ((flags&Helpers.SM_PCMU)!=0) // unsigned
						{
							current.sample[s]=Helpers.promoteUnsigned16BitToSigned24Bit(sample);
						}
						else
						{
							current.sample[s]=Helpers.promoteSigned16BitToSigned24Bit(sample);
						}
					}
					else
					{
						byte sample = inputStream.readByte();
						if ((flags&Helpers.SM_PCMD)!=0)
						{
							sample += old;
							old = sample;
						}
						if ((flags&Helpers.SM_PCMU)!=0) // unsigned
						{
							current.sample[s]=Helpers.promoteUnsigned8BitToSigned24Bit(sample);
						}
						else
						{
							current.sample[s]=Helpers.promoteSigned8BitToSigned24Bit(sample);
						}
					}
				}
			}
			current.fixSampleLoops(getModType());
		}
	}
}
