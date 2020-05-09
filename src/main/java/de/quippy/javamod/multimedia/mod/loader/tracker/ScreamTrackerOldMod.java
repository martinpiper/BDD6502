/*
 * @(#) ScreamTrackerOldMod.java
 * 
 * Created on 07.05.2006 by Daniel Becker (quippy@quippy.de)
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
import de.quippy.javamod.multimedia.mod.mixer.BasicModMixer;
import de.quippy.javamod.multimedia.mod.mixer.ScreamTrackerMixer;
import de.quippy.javamod.system.Helpers;

/**
 * @author Daniel Becker
 * @since 07.05.2006
 */
public class ScreamTrackerOldMod extends Module
{
	private static final String[] MODFILEEXTENSION = new String [] 
	{
		"stm"
	};
	/**
	 * Will be executed during class load
	 */
	static
	{
		ModuleFactory.registerModule(new ScreamTrackerOldMod());
	}

	private int vHi, vLow;
	private int playBackTempo;
	private int STMType;
	
	/**
	 * Constructor for ScreamTrackerOldMod
	 */
	public ScreamTrackerOldMod()
	{
		super();
	}
	/**
	 * Constructor for ScreamTrackerOldMod
	 */
	protected ScreamTrackerOldMod(String fileName)
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
		if ((channel%3)!=0)
			return 256;
		else
			return 0;
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
	 * TODO: Read this!!!
	 * @return the Playback tempo
	 */
	public int getPlayBackTempo()
	{
		return playBackTempo;
	}
	/**
	 * @return
	 * @see de.quippy.javamod.multimedia.mod.loader.Module#doFastSlides()
	 */
	@Override
	public boolean doFastSlides()
	{
		return false;
	}
	/**
	 * Get the current modtype
	 * @param kennung
	 * @return
	 */
	private String getModType(String kennung) throws IOException
	{
		if (!kennung.equals("!Scream!")) 
			throw new IOException("Mod id: " + kennung + ": this is not a screamtracker mod");

		setNSamples(31);
		setNChannels(4);
		return "ScreamTracker";
	}
	/**
	 * Read the STM pattern data
	 * @param pattNum
	 * @param row
	 * @param channel
	 * @param note
	 * @return
	 */
	private PatternElement createNewPatternElement(int pattNum, int row, int channel, int note)
	{
		PatternElement pe = new PatternElement(pattNum, row, channel);
		
		pe.setInstrument((note&0xF80000)>>19);

		int oktave = (note&0xF0000000)>>28;
		if (oktave!=-1)
		{
			int ton = (note&0x0F000000)>>24;
			int index = (oktave+3)*12+ton; // fit to it octaves
			pe.setPeriod((index<Helpers.noteValues.length) ? Helpers.noteValues[index] : 0);
			pe.setNoteIndex(index+1);
		}
		else
		{
			pe.setPeriod(0);
			pe.setNoteIndex(0);
		}
	
		pe.setEffekt((note&0xF00)>>8);
		pe.setEffektOp(note&0xFF);
		if (pe.getEffekt()==0x01) // set Tempo needs correction. Do not ask why!
		{
			int effektOp = pe.getEffektOp();
			pe.setEffektOp(((effektOp&0x0F)<<4) | ((effektOp&0xF0)>>4));
		}
		
		int volume =((note&0x70000)>>16) | ((note&0xF000)>>9);
		if (volume<=64)
		{
			pe.setVolumeEffekt(1);
			pe.setVolumeEffektOp(volume);
		}
		
		return pe;
	}
	/**
	 * @param inputStream
	 * @return true, if this is a protracker mod, false if this is not clear
	 * @see de.quippy.javamod.multimedia.mod.loader.Module#checkLoadingPossible(de.quippy.javamod.io.ModfileInputStream)
	 */
	@Override
	public boolean checkLoadingPossible(ModfileInputStream inputStream) throws IOException
	{
		inputStream.seek(0x14);
		String stmID = inputStream.readString(8);
		inputStream.seek(0);
		return stmID.equals("!SCREAM!") || stmID.equals("!Scream!"); 
	}
	/**
	 * @param fileName
	 * @return
	 * @see de.quippy.javamod.multimedia.mod.loader.Module#getNewInstance(java.lang.String)
	 */
	@Override
	protected Module getNewInstance(String fileName)
	{
		return new ScreamTrackerOldMod(fileName);
	}
	/**
	 * @param inputStream
	 * @return
	 * @see de.quippy.javamod.multimedia.mod.loader.Module#loadModFile(byte[])
	 */
	@Override
	public void loadModFileInternal(ModfileInputStream inputStream) throws IOException
	{
		inputStream.seek(0x14);
		setTrackerName(getModType(inputStream.readString(8)));
		inputStream.seek(0);
		
		// Songname
		setSongName(inputStream.readString(20));

		// ID. Should be "!SCREAM!"
		setModID(inputStream.readString(8));
		
		// 0x1A as file end signal... overread
		inputStream.skip(1);
		// Type: 1=Song 2=MOD
		STMType = inputStream.readByteAsInt();
		if (STMType!=2) throw new IOException("Unsupported STM MOD (ID!=0x02)");
		
		// Version
		vHi = inputStream.readByteAsInt();
		vLow = inputStream.readByteAsInt();
		setTrackerName(getTrackerName() + " V" + vHi + '.' + vLow);
		
		// PlaybackTemp (???)
		playBackTempo = inputStream.readByteAsInt();
		setTempo(6);
		setBPMSpeed(125);
		
		// count of pattern in arrangement
		int patternCount = inputStream.readByteAsInt();
		if (patternCount>64) patternCount = 64;
		setNPattern(patternCount);
		
		// Base volume
		setBaseVolume(inputStream.readByteAsInt()<<1);
		
		// Skip these reserverd bytes
		inputStream.skip(13);

		// Instruments
		setNInstruments(getNSamples());
		InstrumentsContainer instrumentContainer = new InstrumentsContainer(this, 0, getNSamples());
		this.setInstrumentContainer(instrumentContainer);
		for (int i=0; i<getNSamples(); i++)
		{
			Sample current = new Sample();
			// Samplename
			current.setName(inputStream.readString(12));
			
			// reserved
			inputStream.skip(1);
			
			// instrument Disk number, if song (not supported)
			int diskNumber = inputStream.readByteAsInt();
			if (STMType==1) current.setName(current.name+" #"+diskNumber);
			
			// Reserved (Sample Beginning Offset?!)
			inputStream.skip(2);
			
			// Length
			current.setLength(inputStream.readIntelWord());
			
			// Repeat start and stop
			int repeatStart = inputStream.readIntelWord();
			int repeatStop = inputStream.readIntelWord();
			
			if (repeatStart<repeatStop && repeatStop!=0xFFFF) 
				current.setLoopType(Helpers.LOOP_ON);
			else
				current.setLoopType(0);
			
			current.setRepeatStart(repeatStart);
			current.setRepeatStop(repeatStop);
			current.setRepeatLength(repeatStop-repeatStart);

			// volume 64 is maximum
			int vol  = inputStream.readByteAsInt() & 0x7F;
			current.setVolume(vol>64?64:vol);
			
			// reserved
			inputStream.skip(1);

			current.setPanning(-1);
			
			// Base Frequency
			current.setFineTune(0);
			current.setTranspose(0);
			current.setBaseFrequency(inputStream.readIntelWord());

			// Reserved
			inputStream.skip(4);

			// Length in Paragraphs. Ignoring:
			inputStream.skip(2);
			instrumentContainer.setSample(i, current);
		}

		// always space for 128 pattern... With STMs we need to guess the arrangement length
		allocArrangement(128);
		int currentSongLenth = -1;
		for (int i=0; i<128; i++) 
		{
			int nextPatternIndex = inputStream.readByteAsInt(); 
			getArrangement()[i]=nextPatternIndex;
			if (currentSongLenth==-1 && nextPatternIndex==99) currentSongLenth = i;
		}
		while (getArrangement()[currentSongLenth-1]>=getNPattern()) currentSongLenth--;
		setSongLength(currentSongLenth);

		PatternContainer patternContainer = new PatternContainer(getNPattern(), 64, getNChannels());
		setPatternContainer(patternContainer);
		for (int pattNum=0; pattNum<getNPattern(); pattNum++)
		{
			for (int row=0; row<64; row++)
			{
				for (int channel=0; channel<getNChannels(); channel++)
				{
					int value = inputStream.readMotorolaDWord();
					patternContainer.setPatternElement(createNewPatternElement(pattNum, row, channel, value));
				}
			}
		}
		
		for (int i=0; i<getNSamples(); i++)
		{
			Sample current = getInstrumentContainer().getSample(i);
			readSampleData(current, Helpers.SM_PCMS, inputStream);
		}

		cleanUpArrangement();
	}
}
