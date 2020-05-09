/*
 * @(#) ScreamTrackerMod.java
 * 
 * Created on 09.05.2006 by Daniel Becker (quippy@quippy.de)
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
package de.quippy.javamod.multimedia.mod.loader.tracker;

import java.io.IOException;

import de.quippy.javamod.io.ModfileInputStream;
import de.quippy.javamod.multimedia.mod.loader.Module;
import de.quippy.javamod.multimedia.mod.loader.ModuleFactory;
import de.quippy.javamod.multimedia.mod.loader.instrument.InstrumentsContainer;
import de.quippy.javamod.multimedia.mod.loader.instrument.Sample;
import de.quippy.javamod.multimedia.mod.loader.pattern.PatternContainer;
import de.quippy.javamod.multimedia.mod.loader.pattern.PatternElement;
import de.quippy.javamod.multimedia.mod.loader.pattern.PatternRow;
import de.quippy.javamod.multimedia.mod.mixer.BasicModMixer;
import de.quippy.javamod.multimedia.mod.mixer.ScreamTrackerMixer;
import de.quippy.javamod.system.Helpers;

/**
 * @author Daniel Becker
 * @since 09.05.2006
 */
public class ScreamTrackerMod extends Module
{
	private static final String[] MODFILEEXTENSION = new String [] 
	{
		"s3m"
	};
	/**
	 * Will be executed during class load
	 */
	static
	{
		ModuleFactory.registerModule(new ScreamTrackerMod());
	}

	protected int version;
	protected int flags;
	protected int samplesType;
	protected boolean isStereo;
	protected boolean usePanningValues;
	protected int [] channelSettings;
	protected int [] panningValue;
	/** Due to deactivated Channels, we need to remap: */
	private int[] channelMap;

	/**
	 * Constructor for ScreamTrackerMod
	 */
	public ScreamTrackerMod()
	{
		super();
	}
	/**
	 * Constructor for ScreamTrackerMod
	 * @param fileExtension
	 */
	protected ScreamTrackerMod(String fileName)
	{
		super(fileName);
	}
	/**
	 * @return the Fileextensions this loader is suitable for
	 * @see de.quippy.javamod.multimedia.mod.loader.Module#getFileExtensionList()
	 */
	@Override
	public String [] getFileExtensionList()
	{
		return MODFILEEXTENSION;
	}
	/**
	 * @param sampleRate
	 * @param doISP
	 * @return
	 * @see de.quippy.javamod.multimedia.mod.loader.Module#getModMixer(int, boolean)
	 */
	@Override
	public BasicModMixer getModMixer(int sampleRate, int doISP, int doNoLoops)
	{
		return new ScreamTrackerMixer(this, sampleRate, doISP, doNoLoops);
	}
	/**
	 * @param channel
	 * @return
	 * @see de.quippy.javamod.multimedia.mod.loader.Module#getPanningValue(int)
	 */
	@Override
	public int getPanningValue(int channel)
	{
		return panningValue[channel];
	}
	/**
	 * @param channel
	 * @return
	 * @see de.quippy.javamod.multimedia.mod.loader.Module#getChannelVolume(int)
	 */
	@Override
	public int getChannelVolume(int channel)
	{
		return 64;
	}
	/**
	 * @return
	 * @see de.quippy.javamod.multimedia.mod.loader.Module#getFrequencyTable()
	 */
	@Override
	public int getFrequencyTable()
	{
		return Helpers.STM_S3M_TABLE;
	}
	/**
	 * @return
	 * @see de.quippy.javamod.multimedia.mod.loader.Module#doFastSlides()
	 */
	@Override
	public boolean doFastSlides()
	{
		return ((flags&64)!=0) || (version < 0x1300);
	}
	/**
	 * Set a Pattern by interpreting
	 * @param input
	 * @param offset
	 * @param pattNum
	 */
	private void setPattern(int pattNum, ModfileInputStream inputStream) throws IOException
	{
		int row=0;
		PatternRow currentRow = getPatternContainer().getPatternRow(pattNum, row);

		int count = inputStream.readIntelWord()-2; // this read byte also counts
		while (count>=0)
		{
			int packByte = inputStream.readByteAsInt(); count--;
			if (packByte==0)
			{
				row++;
				if (row>=64) 
					break; // Maximum. But do we have to break?! Donnow...
				else
					currentRow = getPatternContainer().getPatternRow(pattNum, row);
			}
			else
			{
				int channel = packByte&31; // there is the channel
				channel = channelMap[channel];
				
				int period = 0;
				int noteIndex = 0;
				int instrument = 0;
				int volume = -1;
				int effekt = 0;
				int effektOp = 0;
				
				if ((packByte&32)!=0) // Note and Sample follow
				{
					int ton = inputStream.readByteAsInt(); count--;
					if (ton==254)
					{
						noteIndex = period = Helpers.NOTE_CUT; // This is our NoteCutValue!
					}
					else 
					{
						// calculate the new note
						noteIndex = ((ton>>4)+1)*12+(ton&0xF); // fit to it octacves
						if (noteIndex>=Helpers.noteValues.length)
						{
							period = 0;
							noteIndex = 0;
						}
						else
						{
							period = Helpers.noteValues[noteIndex];
							noteIndex++;
						}
					}
					
					instrument = inputStream.readByteAsInt(); count--;
				}
				
				if ((packByte&64)!=0) // volume following
				{
					volume = inputStream.readByteAsInt(); count--;
				}
				
				if ((packByte&128)!=0) // Effekts!
				{
					effekt = inputStream.readByteAsInt(); count--;
					effektOp = inputStream.readByteAsInt(); count--;
				}
				
				if (channel!=-1)
				{
					PatternElement currentElement = currentRow.getPatternElement(channel);
					currentElement.setNoteIndex(noteIndex);
					currentElement.setPeriod(period);
					currentElement.setInstrument(instrument);
					if (volume!=-1)
					{
						currentElement.setVolumeEffekt(1);
						currentElement.setVolumeEffektOp(volume);
					}
					currentElement.setEffekt(effekt);
					currentElement.setEffektOp(effektOp);
				}
			}
		}
	}
	/**
	 * @param inputStream
	 * @return true, if this is a protracker mod, false if this is not clear
	 * @see de.quippy.javamod.multimedia.mod.loader.Module#checkLoadingPossible(de.quippy.javamod.io.ModfileInputStream)
	 */
	@Override
	public boolean checkLoadingPossible(ModfileInputStream inputStream) throws IOException
	{
		inputStream.seek(0x2C);
		String s3mID = inputStream.readString(4);
		inputStream.seek(0);
		return s3mID.equals("SCRM"); 
	}
	/**
	 * @param fileName
	 * @return
	 * @see de.quippy.javamod.multimedia.mod.loader.Module#getNewInstance(java.lang.String)
	 */
	@Override
	protected Module getNewInstance(String fileName)
	{
		return new ScreamTrackerMod(fileName);
	}
	/**
	 * @param inputStream
	 * @return
	 * @see de.quippy.javamod.multimedia.mod.loader.Module#loadModFile(byte[])
	 */
	@Override
	public void loadModFileInternal(ModfileInputStream inputStream) throws IOException
	{
		setModType(Helpers.MODTYPE_S3M);
		
		inputStream.seek(0x1D);
		int id = inputStream.readByteAsInt(); 
		if (id!=0x10) throw new IOException("Unsupported S3M MOD (ID!=0x10)");
		inputStream.seek(0);
		
		// Songname
		setSongName(inputStream.readString(28));
		
		// Skip arbitrary data...
		inputStream.seek(0x20);

		setSongLength(inputStream.readIntelWord());
		setNSamples(inputStream.readIntelWord());
		setNInstruments(getNSamples());
		setNPattern(inputStream.readIntelWord());
		setNChannels(32);
		
		// Flags... Skip
		flags = inputStream.readIntelWord();
		
		// Version number
		version = inputStream.readIntelWord();
		
		// Samples Type
		samplesType = inputStream.readIntelWord();

		// ModID
		setModID(inputStream.readString(4));
		setTrackerName("ScreamTracker V" + ((version>>8)&0x0F) + '.' + (version&0xFF));

		// Global Volume
		setBaseVolume(inputStream.readByteAsInt()<<1);
		
		// Tempo
		setTempo(inputStream.readByteAsInt());
		
		// BPM
		setBPMSpeed(inputStream.readByteAsInt());
		
		// MasterVolume (mv&0x80)!=0 --> Stereo else Mono, MasterVolume is SoundBlaster specific
		isStereo = ((inputStream.readByteAsInt() & 0x80)!=0);
		// UltraClick removal --> ignored
		/*int uc = */inputStream.readByteAsInt();
		// DefaultPanning
		usePanningValues = inputStream.readByteAsInt()==0xFC;
		
		// skip again arbitrary data (8Byte unused, 2Byte is pointer to special data, if "special data flag" at offset 0x26 is set
		inputStream.skip(10);
		
		// PanningValues and active or unactive Channels
		channelSettings = new int[32];
		channelMap = new int[32];
		int anzChannel = 0;
		for (int i=0; i<32; i++)
		{
			int readByte = inputStream.readByteAsInt(); 
			if (readByte!=255)
			{
				channelMap[i]=anzChannel;
				channelSettings[anzChannel++] = readByte;
			}
			else
				channelMap[i]=-1;
		}
		setNChannels(anzChannel);
		
		// Song Arrangement
		allocArrangement(getSongLength());
		for (int i=0; i<getSongLength(); i++)  getArrangement()[i]=inputStream.readByteAsInt();
		
		// read the samples
		InstrumentsContainer instrumentContainer = new InstrumentsContainer(this, 0, getNSamples());
		this.setInstrumentContainer(instrumentContainer);
		for (int i=0; i<getNSamples(); i++)
		{
			inputStream.seek(96L+getSongLength()+(i<<1));
			long instrumentOffset = inputStream.readIntelWord();
			inputStream.seek(instrumentOffset<<4);
			
			Sample current = new Sample();
			
			current.setType(inputStream.readByteAsInt());
			// Samplename
			current.setDosFileName(inputStream.readString(13));
			
			long sampleOffset = inputStream.readIntelWord();
			
			// Length
			current.setLength(inputStream.readIntelDWord());
			
			// Repeat start and stop
			int repeatStart = inputStream.readIntelDWord();
			int repeatStop  = inputStream.readIntelDWord();

			current.setRepeatStart(repeatStart);
			current.setRepeatStop(repeatStop);
			current.setRepeatLength(repeatStop-repeatStart);

			// volume 
			current.setVolume(inputStream.readByteAsInt());
			
			// Reserved (Sample Beginning Offset?!)
			inputStream.skip(2);

			// Flags: 1:Loop 2:Stereo 4:16Bit-Sample...
			current.setFlags(inputStream.readByteAsInt());
			current.setLoopType(((current.flags&0x01)==0x01) ? Helpers.LOOP_ON : 0);
			
			// C4SPD
			current.setFineTune(0);
			current.setTranspose(0);
			current.setBaseFrequency(inputStream.readIntelDWord());
			
			// Again reserved data...
			inputStream.skip(12);
			
			// SampleName
			current.setName(inputStream.readString(28));
			
			// Key
			inputStream.skip(4);
			
			current.setPanning(-1);
			
			// SampleData
			int flags = (samplesType==2)?Helpers.SM_PCMU:Helpers.SM_PCMS;
			if ((current.flags&0x04)!=0) flags|=Helpers.SM_16BIT;
			inputStream.seek(sampleOffset<<4);
			readSampleData(current, flags, inputStream);

			instrumentContainer.setSample(i, current);
		}
		
		// Pattern data
		PatternContainer patternContainer = new PatternContainer(getNPattern(), 64, getNChannels());
		setPatternContainer(patternContainer);
		for (int pattNum=0; pattNum<getNPattern(); pattNum++)
		{
			// First, clear them:
			for (int row=0; row<64; row++)
			{
				for (int channel=0; channel<getNChannels(); channel++)
				{
					PatternElement currentElement = new PatternElement(pattNum, row, channel);
					patternContainer.setPatternElement(currentElement);
				}
			}

			inputStream.seek(96L+getSongLength()+(getNSamples()<<1)+(pattNum<<1));
			long patternPosition = inputStream.readIntelWord();
			inputStream.seek(patternPosition<<4);
			setPattern(pattNum, inputStream);
		}

		// If there is Panning...
		panningValue = new int[getNChannels()];
		if (this.usePanningValues)
		{
			inputStream.seek(96L+getSongLength()+(getNSamples()<<1)+(getNPattern()<<1));
			for (int i=0; i<getNChannels(); i++)
			{
				int readByte = inputStream.readByteAsInt() & 0x0F; 
				int ch = channelMap[i];
				if (ch!=-1)
				{
					int val = readByte<<4;
					if (this.channelSettings[ch]<=7 || readByte!=0)
					{
						panningValue[ch]=val;
					}
					else
					{
						panningValue[ch]=256-val;
					}
				}
			}
		}
		else
		if (!isStereo)
		{
			for (int i=0; i<getNChannels(); i++)
			{
				panningValue[i]=128;
			}
		}
		else
		{
			for (int i=0; i<getNChannels(); i++)
			{
				if (this.channelSettings[i]<=7)
				{
					panningValue[i]=256;
				}
				else
				{
					panningValue[i]=0;
				}
			}
		}

		// Correct the songlength for playing, skip markerpattern... (do not want to skip them during playing!)
		int realLen = 0;
		for (int i=0; i<getSongLength(); i++)
		{
			if (getArrangement()[i]<254 && getArrangement()[i]<getNPattern())
				getArrangement()[realLen++]=getArrangement()[i];
		}
		setSongLength(realLen);
		cleanUpArrangement();
	}
}
