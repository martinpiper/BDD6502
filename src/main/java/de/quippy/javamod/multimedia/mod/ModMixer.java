/*
 * @(#) ModMixer.java
 * 
 * Created on 30.04.2006 by Daniel Becker (quippy@quippy.de)
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
package de.quippy.javamod.multimedia.mod;

import javax.sound.sampled.AudioFormat;

import de.quippy.javamod.mixer.BasicMixer;
import de.quippy.javamod.multimedia.mod.loader.Module;
import de.quippy.javamod.multimedia.mod.mixer.BasicModMixer;
import de.quippy.javamod.system.Helpers;
import de.quippy.javamod.system.Log;

import java.io.*;

/**
 * @author Daniel Becker
 * @since 30.04.2006
 */
public class ModMixer extends BasicMixer
{
	private static final int TMPBUFFERLENGTH = 2048;
	public static final String CHANNEL_EVENTS_TXT = "ChannelEvents.txt";
	public static final String EVENTS_BIN = "Events.bin";
	public static final String EVENTS_CMP = "Events.cmp";
	public static final String SAMPLES_BIN = "Samples.bin";


	private final Module mod;
	private final BasicModMixer modMixer;
	
	private int bufferSize, outputBufferSize;
	private int sampleSizeInBits;
	private int channels;
	private int sampleRate;
	private int doISP;
	private int doNoLoops;
	private int msBufferSize;
	private boolean doWideStereoMix;
	private boolean doNoiseReduction;
	private boolean doMegaBass;
	
	private int [] LBuffer;
	private int [] RBuffer;
	private byte [] output;
	
	private long currentSamplesWritten;
	
	// Wide Stereo Vars
	private int maxWideStereo;
	private int[] wideLBuffer;
	private int[] wideRBuffer;
	private int readPointer;
	private int writePointer;
	
	// Noise Reduction: simple low-pass filter
	private int nLeftNR;
	private int nRightNR;
	
	// Bass Expansion: low-pass filter
	private int nXBassSum;
	private int nXBassBufferPos;
	private int nXBassDlyPos;
	private int nXBassMask;
	private int nXBassDepth;
	private int [] XBassBuffer;
	private int [] XBassDelay;	
	
	/**
	 * Constructor for ModMixer
	 */
	public ModMixer(final Module mod, final int sampleSizeInBits, final int channels, final int sampleRate, final int doISP, final boolean doWideStereoMix, final boolean doNoiseReduction, final boolean doMegaBass, final int doNoLoops, final int msBufferSize)
	{
		super();
		this.mod = mod;
		this.sampleSizeInBits=sampleSizeInBits;
		this.channels=channels;
		this.sampleRate=sampleRate;
		this.doWideStereoMix = (channels<2)?false:doWideStereoMix;
		this.doNoiseReduction = doNoiseReduction;
		this.doMegaBass = doMegaBass;
		this.doISP = doISP;
		this.doNoLoops = doNoLoops;
		this.msBufferSize = msBufferSize;
		modMixer = this.mod.getModMixer(sampleRate, doISP, doNoLoops);
	}
	private void initialize()
	{
		bufferSize = msBufferSize * sampleRate / 1000;
		LBuffer = new int[bufferSize];
		RBuffer = new int[bufferSize];

		// For the DSP-Output
		outputBufferSize = bufferSize*channels; // For each channel!

		// Now for the bits (linebuffer):
		int bytesPerSample = sampleSizeInBits>>3; // DIV 8;
		outputBufferSize *= bytesPerSample;
		output = new byte[outputBufferSize];
		
		maxWideStereo = sampleRate / 50;
		wideLBuffer = new int [maxWideStereo];
		wideRBuffer = new int [maxWideStereo];
		readPointer = 0;
		writePointer=maxWideStereo-1;
		
		initMegaBass();
		
		nLeftNR = 0;
		nRightNR = 0;
		
		setAudioFormat(new AudioFormat(sampleRate, sampleSizeInBits, channels, true, false)); // signed, little endian
	}
	private void initMegaBass()
	{
		int nXBassSamples = (sampleRate * Helpers.XBASS_DELAY) / 10000;
		if (nXBassSamples > Helpers.XBASS_BUFFER) nXBassSamples = Helpers.XBASS_BUFFER;
		int mask = 2;
		while (mask <= nXBassSamples) mask <<= 1;
		
		XBassBuffer = new int [Helpers.XBASS_BUFFER];
		XBassDelay = new int [Helpers.XBASS_BUFFER];
		nXBassMask = ((mask >> 1) - 1);
		nXBassSum = 0;
		nXBassBufferPos = 0;
		nXBassDlyPos = 0;
		nXBassDepth = 6;
	}
	/**
	 * @param doNoiseReduction The doNoiseReduction to set.
	 */
	public void setDoNoiseReduction(boolean doNoiseReduction)
	{
		this.doNoiseReduction = doNoiseReduction;
	}
	/**
	 * @param doWideStereoMix The doWideStereoMix to set.
	 */
	public void setDoWideStereoMix(boolean doWideStereoMix)
	{
		this.doWideStereoMix = doWideStereoMix;
	}
	/**
	 * @param doMegaBass The doMegaBass to set.
	 */
	public void setDoMegaBass(boolean doMegaBass)
	{
		this.doMegaBass = doMegaBass;
	}
	/**
	 * @param doNoLoops the loop to set
	 */
	public void setDoNoLoops(int doNoLoops)
	{
		modMixer.changeDoNoLoops(doNoLoops);
	}
	/**
	 * @param doISP The doISP to set.
	 */
	public void setDoISP(int doISP)
	{
		modMixer.changeISP(doISP);
	}
	/**
	 * @param msBufferSize The buffer size to set.
	 */
	public void setBufferSize(int msBufferSize)
	{
		final int oldMsBufferSize = this.msBufferSize;

		final boolean wasPlaying = !isPaused();
		if (wasPlaying) pausePlayback();

		this.msBufferSize = msBufferSize;
		if (wasPlaying)
		{
			initialize();
			openAudioDevice();
			if (!isInitialized())
			{
				this.msBufferSize = oldMsBufferSize;
				initialize();
				openAudioDevice();
			}
	
			pausePlayback();
		}
	}
	/**
	 * @param sampleRate The sampleRate to set.
	 */
	public void setSampleRate(int sampleRate)
	{
		final int oldSampleRate = this.sampleRate;

		final boolean wasPlaying = !isPaused();
		if (wasPlaying) pausePlayback();

		this.sampleRate = sampleRate;
		if (wasPlaying)
		{
			modMixer.changeSampleRate(sampleRate);
			initialize();
			openAudioDevice();
			if (!isInitialized())
			{
				this.sampleRate = oldSampleRate;
				modMixer.changeSampleRate(oldSampleRate);
				initialize();
				openAudioDevice();
			}
	
			pausePlayback();
		}
	}
	/**
	 * @param sampleSizeInBits The sampleSizeInBits to set.
	 */
	public void setSampleSizeInBits(int sampleSizeInBits)
	{
		int oldsampleSizeInBits = this.sampleSizeInBits;

		boolean wasPlaying = !isPaused();
		if (wasPlaying) pausePlayback();

		this.sampleSizeInBits = sampleSizeInBits;
		initialize();
		openAudioDevice();
		if (!isInitialized())
		{
			this.sampleSizeInBits = oldsampleSizeInBits;
			initialize();
			openAudioDevice();
		}

		if (wasPlaying) pausePlayback();
	}
	/**
	 * @param channels The channels to set.
	 */
	public void setChannels(int channels)
	{
		int oldChannels = this.channels;

		boolean wasPlaying = !isPaused();
		if (wasPlaying) pausePlayback();

		this.channels = channels;
		initialize();
		openAudioDevice();
		if (!isInitialized())
		{
			this.channels = oldChannels;
			initialize();
			openAudioDevice();
		}

		if (wasPlaying) pausePlayback();
	}
	/**
	 * @return the mod
	 */
	public Module getMod()
	{
		return mod;
	}
	/**
	 * @return the modMixer
	 */
	public BasicModMixer getModMixer()
	{
		return modMixer;
	}
	/**
	 * 
	 * @see de.quippy.javamod.mixer.Mixer#isSeekSupported()
	 */
	@Override
	public boolean isSeekSupported()
	{
		return true;
	}
	/**
	 * 
	 * @see de.quippy.javamod.mixer.Mixer#getMillisecondPosition()
	 */
	@Override
	public long getMillisecondPosition()
	{
		return currentSamplesWritten * 1000L / (long)sampleRate;
	}
	/**
	 * @param milliseconds
	 * @see de.quippy.javamod.mixer.BasicMixer#seek(long)
	 * @since 13.02.2012
	 */
	@Override
	protected void seek(long milliseconds)
	{
		try
		{
			if (getMillisecondPosition() > milliseconds)
			{
				modMixer.initializeMixer(); // restart playback
				currentSamplesWritten = 0;
			}
			
			int [] tmpLBuffer = new int[TMPBUFFERLENGTH];
			int [] tmpRBuffer = new int[TMPBUFFERLENGTH];
			modMixer.changeISP(0);
			while (getMillisecondPosition() < milliseconds)
			{
				int sampleCount = modMixer.mixIntoBuffer(tmpLBuffer, tmpRBuffer, TMPBUFFERLENGTH);
				if (sampleCount <= 0) break;
				currentSamplesWritten += sampleCount;
			}
			modMixer.changeISP(doISP);
		}
		catch (Exception ex)
		{
			Log.error("[ModMixer]", ex);
		}
	}
	/**
	 * 
	 * @see de.quippy.javamod.mixer.Mixer#getLengthInMilliseconds()
	 */
	@Override
	public long getLengthInMilliseconds()
	{
		int [] tmpLBuffer = new int[TMPBUFFERLENGTH];
		int [] tmpRBuffer = new int[TMPBUFFERLENGTH];

		modMixer.changeSampleRate(22050);
		modMixer.changeISP(0);
		if (doNoLoops==Helpers.PLAYER_LOOP_DEACTIVATED)
			modMixer.changeDoNoLoops(Helpers.PLAYER_LOOP_FADEOUT);
		long fullLength = 0;
		while (fullLength < 60L*60L*22050L)
		{
			int sampleCount = modMixer.mixIntoBuffer(tmpLBuffer, tmpRBuffer, TMPBUFFERLENGTH);
			if (sampleCount <= 0) break;
			fullLength += (long)sampleCount;
		}
		modMixer.changeSampleRate(sampleRate);
		modMixer.changeISP(doISP);
		modMixer.changeDoNoLoops(doNoLoops);
		modMixer.initializeMixer();
		return fullLength * 1000L / 22050L;
	}
	/**
	 * @return
	 * @see de.quippy.javamod.mixer.Mixer#getChannelCount()
	 */
	@Override
	public int getChannelCount()
	{
		if (modMixer!=null)
			return modMixer.getCurrentUsedChannels();
		else
			return 0;
	}
	/**
	 * @return
	 * @see de.quippy.javamod.mixer.Mixer#getCurrentKBperSecond()
	 */
	@Override
	public int getCurrentKBperSecond()
	{
		return (getChannelCount()*sampleSizeInBits*sampleRate)/1000;
	}
	/**
	 * @return
	 * @see de.quippy.javamod.mixer.Mixer#getCurrentSampleFrequency()
	 */
	@Override
	public int getCurrentSampleFrequency()
	{
		return sampleRate/1000;
	}
	/**
	 * @since 22.06.2006
	 */
	@Override
	public void startPlayback()
	{
		initialize();
		currentSamplesWritten = 0; // not in initialize which is also called at freq. changes
		
		setIsPlaying();

		int xba = nXBassDepth+1;
		int xbamask = (1 << xba) - 1;
		
		if (getSeekPosition()>0) seek(getSeekPosition());
		
		try
		{
			openAudioDevice();
			if (!isInitialized()) return;

			int count;
			do
			{
				// get "count" values of 24 bit signed sampledata for mixing
				count = modMixer.mixIntoBuffer(LBuffer, RBuffer, bufferSize);
				if (count > 0)
				{
					int ox=0; int ix=0;
					while (ix < count)
					{
						// get Sample and reset to zero!
						int lsample = LBuffer[ix]; LBuffer[ix]=0;
						int rsample = RBuffer[ix]; RBuffer[ix]=0;
						ix++;
						
						// WideStrereo Mixing
						if (doWideStereoMix)
						{
							wideLBuffer[writePointer]=lsample;
							wideRBuffer[writePointer++]=rsample;
							if (writePointer>=maxWideStereo) writePointer=0;
	
							rsample+=(wideLBuffer[readPointer]>>1);
							lsample+=(wideRBuffer[readPointer++]>>1);
							if (readPointer>=maxWideStereo) readPointer=0;
						}
	
						// MegaBass
						if (doMegaBass)
						{
							nXBassSum -= XBassBuffer[nXBassBufferPos];
							int tmp0 = lsample + rsample;
							int tmp = (tmp0 + ((tmp0 >> 31) & xbamask)) >> xba;
							XBassBuffer[nXBassBufferPos] = tmp;
							nXBassSum += tmp;
							int v = XBassDelay[nXBassDlyPos];
							XBassDelay[nXBassDlyPos] = lsample;
							lsample = v + nXBassSum;
							v = XBassDelay[nXBassDlyPos+1];
							XBassDelay[nXBassDlyPos+1] = rsample;
							rsample = v + nXBassSum;
							nXBassDlyPos = (nXBassDlyPos + 2) & nXBassMask;
							nXBassBufferPos = (nXBassBufferPos+1) & nXBassMask;
						}
						
						// Noise Reduction:
						if (doNoiseReduction)
						{
							int vnr = lsample>>1;
							lsample = vnr + nLeftNR;
							nLeftNR = vnr;
							
							vnr = rsample>>1;
							rsample = vnr + nRightNR;
							nRightNR = vnr;
						}
						
						// Clip the values, just in case:
						if (lsample>8388607) lsample=8388607; // 0x7FFFFF
						else if (lsample<-8388608) lsample=-8388608; //0xFFFFFF
						
						if (rsample>8388607) rsample = 8388607;
						else if (rsample<-8388608) rsample = -8388608;
						
						
						// and after that into the outputbuffer to write to the soundstream
						if (channels==2)
						{
							// Now put theses values into the samplePipe, if set
							switch (sampleSizeInBits)
							{
								case 24:
									output[ox++] = (byte) (lsample);
									output[ox++] = (byte) (lsample >>  8);
									output[ox++] = (byte) (lsample >> 16);
									output[ox++] = (byte) (rsample);
									output[ox++] = (byte) (rsample >>  8);
									output[ox++] = (byte) (rsample >> 16);
									break;
								case 16:
									output[ox++] = (byte) (lsample >>  8);
									output[ox++] = (byte) (lsample >> 16);
									output[ox++] = (byte) (rsample >>  8);
									output[ox++] = (byte) (rsample >> 16);
									break;
								case 8:
								default:
									output[ox++] = (byte) (lsample >> 16);
									output[ox++] = (byte) (rsample >> 16);
									break;
							}
						}
						else
						{
							int sample = (lsample>>1) + (rsample>>1); 
							// Now put theses values into the samplePipe, if set
							switch (sampleSizeInBits)
							{
								case 24:
									output[ox++] = (byte) (sample);
									output[ox++] = (byte) (sample >>  8);
									output[ox++] = (byte) (sample >> 16);
									break;
								case 16:
									output[ox++] = (byte) (sample >>  8);
									output[ox++] = (byte) (sample >> 16);
									break;
								case 8:
								default:
									output[ox++] = (byte) (sample >> 16);
									break;
							}
						}
					}
					
					writeSampleDataToLine(output, 0, ox);

					currentSamplesWritten += count;
				}
				
				if (stopPositionIsReached()) setIsStopping();

				if (isStopping())
				{
					setIsStopped();
					break;
				}
				if (isPausing())
				{
					setIsPaused();
					while (isPaused())
					{
						try { Thread.sleep(1); } catch (InterruptedException ex) { /*noop*/ }
					}
				}
				if (isInSeeking())
				{
					setIsSeeking();
					while (isInSeeking())
					{
						try { Thread.sleep(1); } catch (InterruptedException ex) { /*noop*/ }
					}
				}
			}
			while (count!=-1);
			if (count<=0) setHasFinished(); // Piece was finished!
		}
		catch (Throwable ex)
		{
			throw new RuntimeException(ex);
		}
		finally
		{
			setIsStopped();
			closeAudioDevice();
		}
	}

	@Override
	public void fastExport(String filename, int ratio1, int ratio2) {
		PrintWriter debugData = null;
		DataOutputStream sampleData = null;
		DataOutputStream musicData = null;
		try {
			debugData = new PrintWriter(new FileWriter(filename + CHANNEL_EVENTS_TXT));
			sampleData = new DataOutputStream(new FileOutputStream(filename + SAMPLES_BIN));
			// To simulate a stopped voice the first exported sample is always 0x80
			sampleData.write(0x80);
			musicData = new DataOutputStream(new FileOutputStream(filename + EVENTS_BIN));
			modMixer.setDebugData(debugData);
			modMixer.setDebugSampleData(sampleData);
			modMixer.setDebugMusicData(musicData);
			modMixer.setSampleRatio1(ratio1);
			modMixer.setSampleRatio2(ratio2);
		} catch (IOException e) {
			e.printStackTrace();
		}
		initialize();
		currentSamplesWritten = 0; // not in initialize which is also called at freq. changes

		setIsPlaying();

		if (getSeekPosition()>0) seek(getSeekPosition());

		int count;
		do
		{
			// get "count" values of 24 bit signed sampledata for mixing
			count = modMixer.mixIntoBuffer(LBuffer, RBuffer, bufferSize);
			currentSamplesWritten += count;

			if (stopPositionIsReached()) setIsStopping();

			if (isStopping())
			{
				setIsStopped();
				break;
			}
			if (isPausing())
			{
				setIsPaused();
				while (isPaused())
				{
					try { Thread.sleep(1); } catch (InterruptedException ex) { /*noop*/ }
				}
			}
			if (isInSeeking())
			{
				setIsSeeking();
				while (isInSeeking())
				{
					try { Thread.sleep(1); } catch (InterruptedException ex) { /*noop*/ }
				}
			}
		}
		while (count!=-1);
		if (count<=0) setHasFinished(); // Piece was finished!
		setIsStopped();

		try {
			if (debugData != null) {
				debugData.flush();
				debugData.close();
			}
			if (sampleData != null) {
				sampleData.flush();
				System.out.println("sampleData length: " + sampleData.size());
				sampleData.close();
			}
			if (musicData != null) {
				musicData.write(Helpers.kMusicCommandStop);
				musicData.flush();
				musicData.flush();
				musicData.close();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
