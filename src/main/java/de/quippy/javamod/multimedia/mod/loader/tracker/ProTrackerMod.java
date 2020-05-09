/*
 * @(#) ProTrackerMod.java
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
import de.quippy.javamod.multimedia.mod.mixer.ProTrackerMixer;
import de.quippy.javamod.system.Helpers;

/**
 * @author Daniel Becker
 * @since 21.04.2006
 */
public class ProTrackerMod extends Module
{
	private static final String[] MODFILEEXTENSION = new String [] 
  	{
  		"nst", "mod", "wow"
  	};
	/**
	 * Will be executed during class load
	 */
	static
	{
		ModuleFactory.registerModule(new ProTrackerMod());
	}

	private boolean isAmigaLike;
	
	/**
	 * Constructor for ProTrackerMod
	 */
	public ProTrackerMod()
	{
		super();
	}
	/**
	 * Constructor for ProTrackerMod
	 */
	protected ProTrackerMod(String fileName)
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
	 * @return
	 * @see de.quippy.javamod.multimedia.mod.loader.Module#getModMixer(int)
	 */
	@Override
	public BasicModMixer getModMixer(int sampleRate, int doISP, int doNoLoops)
	{
		return new ProTrackerMixer(this, sampleRate, doISP, doNoLoops);
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
		return (isAmigaLike)?Helpers.AMIGA_TABLE:Helpers.XM_AMIGA_TABLE;
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
	 * Get the ModType
	 * @param kennung
	 * @return
	 */
	private String getModType(String kennung)
	{
		if (kennung.length()==4)
		{
			setNSamples(31);
			if (kennung.equals("M.K.") || kennung.equals("M!K!") || kennung.equals("M&K!") || kennung.equals("N.T."))
			{
				isAmigaLike = true;
				setNChannels(4);
				return "ProTracker";
			}
			if (kennung.startsWith("FLT"))
			{
				isAmigaLike = false;
				setNChannels(Integer.parseInt(Character.toString(kennung.charAt(3))));
				return "StarTrekker";
			}
			if (kennung.startsWith("TDZ"))
			{
				isAmigaLike = false;
				setNChannels(Integer.parseInt(Character.toString(kennung.charAt(3))));
				return "StarTrekker";
			}
			if (kennung.endsWith("CHN"))
			{
				isAmigaLike = false;
				setNChannels(Integer.parseInt(Character.toString(kennung.charAt(0))));
				return "StarTrekker";
			}
			if (kennung.equals("CD81") || kennung.equals("OKTA"))
			{
				isAmigaLike = false;
				setNChannels(8);
				return "Atari Oktalyzer";
			}
	
			String firstKennung = kennung.substring(0,2);
			String lastKennung = kennung.substring(2,4);
	
			if (lastKennung.equals("CH") || lastKennung.equals("CN"))
			{
				isAmigaLike = false;
				setNChannels(Integer.parseInt(firstKennung));
				return "TakeTracker";
			}	 
		}

		// Noise Tracker is the rest...
		isAmigaLike = true;
		setNSamples(15);
		setNChannels(4);
		setModID("NONE");
		return "NoiseTracker";
	}
	/**
	 * Many modfiles are too short or too long.
	 * Here we try to find out about this, as the real
	 * saved count of pattern is not saved anywhere.
	 * @param fileSize
	 * @return
	 */
	private int calculatePatternCount(int fileSize)
	{
		int headerLen = 150; // Name+SongLen+CIAA+SongArrangement
		if (getNSamples()>15) headerLen += 4L;  // Kennung

		int sampleLen = 0;
		for (int i=0; i<getNSamples(); i++)
			sampleLen += 30L + getInstrumentContainer().getSample(i).length;

		int spaceForPattern = fileSize - headerLen - sampleLen;
		
		// Lets find out about the highest Patternnumber used
		// in the song arrangement
		int maxPatternNumber=0;
		for (int i=0; i<getSongLength(); i++)
		{
			int patternNumber = getArrangement()[i];
			if (patternNumber > maxPatternNumber && patternNumber < 0x80)
				maxPatternNumber=getArrangement()[i];
		}
		maxPatternNumber++; // Highest number becomes highest count 

		// It could be the WOW-Format:
		if (getModID().equals("M.K."))
		{
			// so check for 8 channels:
			int totalPatternBytes = maxPatternNumber * (64*4*8);
			// This mod has 8 channels! --> WOW
			if (totalPatternBytes == spaceForPattern)
			{
				isAmigaLike = true;
				setNChannels(8);
				setTrackerName("Grave Composer");
			}
		}

		int bytesPerPattern=64*4*getNChannels();
		setNPattern(spaceForPattern / bytesPerPattern);
		int bytesLeft = spaceForPattern % bytesPerPattern;

		if (bytesLeft>0) // It does not fit!
		{
			if (maxPatternNumber>getNPattern())
			{
				// The modfile is too short. The highest pattern is reaching into
				// the sampledata, but it has to be read!
				bytesLeft-=bytesPerPattern;
				setNPattern(maxPatternNumber+1);
			}
			else
			{
				// The modfile is too long. Sometimes this happens if composer
				// add additional data to the modfile.
				bytesLeft+=(getNPattern()-maxPatternNumber)*bytesPerPattern;
				setNPattern(maxPatternNumber);
			}

			return bytesLeft;
		}

		return 0;
	}
	/**
	 * Create the new Pattern element
	 * @param pattNum
	 * @param row
	 * @param channel
	 * @param note
	 * @return
	 */
	private PatternElement createNewPatternElement(int pattNum, int row, int channel, int note)
	{
		PatternElement pe = new PatternElement(pattNum, row, channel);
	
		if (getNSamples()>15)
		{
			pe.setInstrument((((note&0xF0000000)>>24) | ((note&0xF000)>>12))&getNSamples()); // & 0x1F 
			pe.setPeriod((note&0x0FFF0000)>>16);
		}
		else
		{
			pe.setInstrument(((note&0xF000)>>12)&getNSamples()); // &0x0F
			pe.setPeriod((note&0xFFFF0000)>>16);
		}

		if (pe.getPeriod()>0)
		{
			pe.setNoteIndex(Helpers.getNoteIndexForPeriod(pe.getPeriod())+1);
		}

		pe.setEffekt((note&0xF00)>>8);
		pe.setEffektOp(note&0xFF);
		if (pe.getEffekt()==12 && pe.getEffektOp()>64) pe.setEffektOp(64);
		
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
		inputStream.seek(1080);
		String modID = inputStream.readString(4);
		inputStream.seek(0);
		return 	modID.equals("M.K.") || modID.equals("M!K!")  || modID.equals("M&K!") || modID.equals("N.T.") || 
				modID.startsWith("FLT") || modID.startsWith("TDZ") || 
				modID.endsWith("CHN") ||
				modID.equals("CD81") || modID.equals("OKTA") ||
				modID.endsWith("CH") || modID.endsWith("CN");
	}
	/**
	 * @param fileName
	 * @return
	 * @see de.quippy.javamod.multimedia.mod.loader.Module#getNewInstance(java.lang.String)
	 */
	@Override
	protected Module getNewInstance(String fileName)
	{
		return new ProTrackerMod(fileName);
	}
	/**
	 * @param inputStream
	 * @return
	 * @see de.quippy.javamod.multimedia.mod.loader.Module#loadModFile(java.io.DataInputStream)
	 */
	@Override
	public void loadModFileInternal(ModfileInputStream inputStream) throws IOException
	{
		setModType(Helpers.MODTYPE_MOD);
		setBaseVolume(128);
		setBPMSpeed(125);
		setTempo(6);
	
		inputStream.seek(1080);
		setModID(inputStream.readString(4));
		inputStream.seek(0);
		setTrackerName(getModType(getModID()));
		
		setSongName(inputStream.readString(20));

		setNInstruments(getNSamples());
		InstrumentsContainer instrumentContainer = new InstrumentsContainer(this, 0, getNSamples());
		this.setInstrumentContainer(instrumentContainer);
		for (int i=0; i<getNSamples(); i++)
		{
			Sample current = new Sample();
			// Samplename
			current.setName(inputStream.readString(22));

			// Length
			current.setLength(inputStream.readMotorolaWord() << 1);
			
			// finetune Value>7 means negative 8..15= -8..-1
			int fine = inputStream.readByteAsInt() & 0xF;
			fine = fine>7?fine-16:fine;
			// BaseFrequenzy from Table: FineTune is -8...+7
			current.setFineTune(fine<<4);
			current.setTranspose(0);
			current.setBaseFrequency(Helpers.it_fineTuneTable[fine+8]);

			// volume 64 is maximum
			int vol  = inputStream.readByteAsInt() & 0x7F;
			current.setVolume(vol>64?64:vol);
			
			// Repeat start and stop
			int repeatStart  = inputStream.readMotorolaWord()<< 1;
			int repeatLength = inputStream.readMotorolaWord()<< 1;
			int repeatStop = repeatStart+repeatLength;
			
			if (current.length<4) current.length=0;
			if (current.length>0)
			{
				if (repeatStart > current.length) repeatStart=current.length-1;
				if (repeatStop > current.length) repeatStop=current.length;
				if (repeatStart>=repeatStop || repeatStop<=8 || (repeatStop-repeatStart)<=4)
				{
					repeatStart = repeatStop = 0;
					current.setLoopType(0);
				}
				if (repeatStart<repeatStop) 
					current.setLoopType(Helpers.LOOP_ON);
			}
			else
				current.setLoopType(0);

			current.setRepeatStart(repeatStart);
			current.setRepeatStop(repeatStop);
			current.setRepeatLength(repeatStop-repeatStart);
			
			current.setPanning(-1);

			instrumentContainer.setSample(i, current);
		}
		
		// count of pattern in arrangement
		setSongLength(inputStream.readByteAsInt());
		// good old CIAA
		inputStream.skip(1);
		
		// always space for 128 pattern...
		allocArrangement(128);
		for (int i=0; i<128; i++) getArrangement()[i]=inputStream.readByteAsInt();
		
		// skip ModID, if not NoiseTracker:
		if (getNSamples()>15) inputStream.skip(4);
		
		// Read the patterndata
		int bytesLeft = calculatePatternCount((int)inputStream.getLength()); // Get the amount of pattern and keep "bytesLeft" in mind!
		
		PatternContainer patternContainer = new PatternContainer(getNPattern(), 64, getNChannels());
		setPatternContainer(patternContainer);
		for (int pattNum=0; pattNum<getNPattern(); pattNum++)
		{
			if (getModID().equals("FLT8")) // StarTrekker is slightly different
			{
				for (int row=0; row<64; row++)
				{
					for (int channel=0; channel<4; channel++)
					{
						int value = inputStream.readMotorolaDWord();
						patternContainer.setPatternElement(createNewPatternElement(pattNum, row, channel, value));
					}
				}
				for (int row=0; row<64; row++)
				{
					for (int channel=4; channel<8; channel++)
					{
						int value = inputStream.readMotorolaDWord();
						patternContainer.setPatternElement(createNewPatternElement(pattNum, row, channel, value));
					}
				}
			}
			else
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
		}
		// Sampledata: If the modfile was too short, we need to recalculate:
		if (bytesLeft<0)
		{
			int calcSamplePos=getInstrumentContainer().getFullSampleLength();
			calcSamplePos=(int)inputStream.getLength()-calcSamplePos;
			// do this only, if needed!
			if (calcSamplePos<inputStream.getFilePointer()) inputStream.seek(calcSamplePos);
		}

		for (int i=0; i<getNSamples(); i++)
		{
			Sample current = getInstrumentContainer().getSample(i);
			readSampleData(current, Helpers.SM_PCMS, inputStream);
		}
		cleanUpArrangement();
	}
}
