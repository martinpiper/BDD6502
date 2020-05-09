/*
 * @(#) XMMod.java
 * 
 * Created on 26.05.2006 by Daniel Becker
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
import de.quippy.javamod.multimedia.mod.loader.instrument.Envelope;
import de.quippy.javamod.multimedia.mod.loader.instrument.Instrument;
import de.quippy.javamod.multimedia.mod.loader.instrument.InstrumentsContainer;
import de.quippy.javamod.multimedia.mod.loader.instrument.Sample;
import de.quippy.javamod.multimedia.mod.loader.pattern.Pattern;
import de.quippy.javamod.multimedia.mod.loader.pattern.PatternContainer;
import de.quippy.javamod.multimedia.mod.loader.pattern.PatternElement;
import de.quippy.javamod.multimedia.mod.loader.pattern.PatternRow;
import de.quippy.javamod.system.Helpers;
import de.quippy.javamod.system.Log;

/**
 * @author Daniel Becker
 * @since 26.05.2006
 */
public class XMMod extends ProTrackerMod
{
	private static final String[] MODFILEEXTENSION = new String [] 
   	{
   		"xm"
   	};
	/**
	 * Will be executed during class load
	 */
	static
	{
		ModuleFactory.registerModule(new XMMod());
	}

	private int version;
	private int headerSize;
	private int songRestart;
	private int flag;
	
	/**
	 * Constructor for XMMod
	 */
	public XMMod()
	{
		super();
	}
	/**
	 * Constructor for XMMod
	 * @param fileExtension
	 */
	protected XMMod(String fileName)
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
	 * @return
	 * @see de.quippy.javamod.multimedia.mod.loader.Module#getFrequencyTable()
	 */
	@Override
	public int getFrequencyTable()
	{
		return ((songFlags & Helpers.SONG_LINEARSLIDES)!=0)?Helpers.XM_LINEAR_TABLE:Helpers.XM_AMIGA_TABLE;
	}
	public int getSongRestart()
	{
		return songRestart;
	}
	private void setIntoPatternElement(PatternElement currentElement, ModfileInputStream inputStream) throws IOException
	{
		long pos = inputStream.getFilePointer();
		int lookahead = inputStream.readByteAsInt();
		inputStream.seek(pos);
		int flags = ((lookahead&0x80)!=0)? inputStream.readByteAsInt() : 0x1F; // Packed or not packed...
		if( (flags&0x01)!=0)
		{
			int period = 0;
			int noteIndex = inputStream.readByteAsInt();
			if (noteIndex==97) // Key Off!
			{
				noteIndex = period = Helpers.KEY_OFF;
			}
			else
			if (noteIndex!=0)
			{
				if (noteIndex<97) noteIndex +=12;
				noteIndex -= 12;
				period = Helpers.noteValues[noteIndex - 1];
			}
			currentElement.setNoteIndex(noteIndex);
			currentElement.setPeriod(period);
		}
		if( (flags&0x02)!=0 ) currentElement.setInstrument(inputStream.readByteAsInt()); // Inst
		if( (flags&0x04)!=0 )
		{
			int volume = inputStream.readByteAsInt();
			if (volume!=0)
			{
				if (volume<=0x50)
				{
					currentElement.setVolumeEffekt(1);
					currentElement.setVolumeEffektOp(volume-0x10);
				}
				else
				{
					currentElement.setVolumeEffekt((volume>>4)-0x4);
					currentElement.setVolumeEffektOp(volume&0x0F);
				}
			}
		}
		if( (flags&0x08)!=0 ) currentElement.setEffekt(inputStream.readByteAsInt()); // FX
		if( (flags&0x10)!=0 ) currentElement.setEffektOp(inputStream.readByteAsInt()); // FXP
	}
	/**
	 * Get the ModType
	 * @param kennung
	 * @return
	 */
	private boolean isXMMod(String kennung)
	{
		if (kennung.equals("Extended Module: ")) return true;
		if (kennung.toLowerCase().equals("extended module: ")) return true;
		return false;
	}
	/**
	 * @param inputStream
	 * @return true, if this is a protracker mod, false if this is not clear
	 * @see de.quippy.javamod.multimedia.mod.loader.Module#checkLoadingPossible(de.quippy.javamod.io.ModfileInputStream)
	 */
	@Override
	public boolean checkLoadingPossible(ModfileInputStream inputStream) throws IOException
	{
		String xmID = inputStream.readString(17);
		inputStream.seek(0);
		return isXMMod(xmID);
	}
	/**
	 * @param fileName
	 * @return
	 * @see de.quippy.javamod.multimedia.mod.loader.Module#getNewInstance(java.lang.String)
	 */
	@Override
	protected Module getNewInstance(String fileName)
	{
		return new XMMod(fileName);
	}
	/**
	 * @param inputStream
	 * @return
	 * @see de.quippy.javamod.multimedia.mod.loader.Module#loadModFile(java.io.DataInputStream)
	 */
	@Override
	public void loadModFileInternal(ModfileInputStream inputStream) throws IOException
	{
		setModType(Helpers.MODTYPE_XM);
		setBaseVolume(128);
	
		// XM-ID:
		setModID(inputStream.readString(17));
		if (!isXMMod(getModID())) throw new IOException("Unsupported XM Module!");

		// Songname
		setSongName(inputStream.readString(20));
		// 0x1A:
		inputStream.skip(1);

		// Trackername
		String trackerName = inputStream.readString(20); 
		setTrackerName(trackerName.trim());
		
		// Version
		version = inputStream.readIntelWord();
		if (version<0x0104) Log.info("XM-Version is below 0x0104... ");
		
		long LSEEK = inputStream.getFilePointer();
		// Header Size
		headerSize = inputStream.readIntelDWord();
		
		// OrderNum:
		setSongLength(inputStream.readIntelWord());
		
		//SongRestart
		songRestart = inputStream.readIntelWord();
		
		// NChannels
		setNChannels(inputStream.readIntelWord());
		
		// NPattern
		setNPattern(inputStream.readIntelWord());
		
		// Insttruments
		setNInstruments(inputStream.readIntelWord());
		
		// a Flag
		flag = inputStream.readIntelWord();
		if ((flag & 0x0001)!=0) songFlags |= Helpers.SONG_LINEARSLIDES;
		if ((flag & 0x1000)!=0) songFlags |= Helpers.SONG_EXFILTERRANGE;

		
		// Tempo
		setTempo(inputStream.readIntelWord());
		
		// BPMSpeed
		setBPMSpeed(inputStream.readIntelWord());
		
		// always space for 256 pattern...
		int illegalPatternNum = 0;
		allocArrangement(256);
		for (int i=0; i<256; i++) getArrangement()[i-illegalPatternNum]=inputStream.readByteAsInt();
		
		inputStream.seek(LSEEK + headerSize);
		
		// Read the patternData
		PatternContainer patternContainer = new PatternContainer(getNPattern());
		for (int pattNum=0; pattNum<getNPattern(); pattNum++)
		{
			LSEEK = inputStream.getFilePointer();
			int patternHeaderSize = inputStream.readIntelDWord();
			int packingType = inputStream.readByteAsInt();
			if (packingType!=0) throw new IOException("Unknown pattern packing type: " + packingType);
			int rows = inputStream.readIntelWord();
			int packedPatternDataSize = inputStream.readIntelWord();
			inputStream.seek(LSEEK + patternHeaderSize);
			
			Pattern currentPattern = new Pattern(rows);
			if (packedPatternDataSize>0)
			{
				for (int row=0; row<rows; row++)
				{
					PatternRow currentRow = new PatternRow(getNChannels());
					for (int channel=0; channel<getNChannels(); channel++)
					{
						PatternElement currentElement = new PatternElement(pattNum, row, channel);
						setIntoPatternElement(currentElement, inputStream);
						currentRow.setPatternElement(channel, currentElement);
					}
					currentPattern.setPatternRow(row, currentRow);
				}
			}
			patternContainer.setPattern(pattNum, currentPattern);
		}
		setPatternContainer(patternContainer);
		
		InstrumentsContainer instrumentContainer = new InstrumentsContainer(this, getNInstruments(), 0);
		this.setInstrumentContainer(instrumentContainer);

		int sampleOffsetIndex = 0;
		// Read the instrument data
		for (int ins=0; ins<getNInstruments(); ins++)
		{
			int vibratoType = 0; 
			int vibratoSweep = 0;
			int vibratoDepth = 0;
			int vibratoRate = 0; 

			LSEEK = inputStream.getFilePointer();
			
			Instrument currentIns = new Instrument();
			int instrumentHeaderSize = inputStream.readIntelDWord();
			currentIns.setName(inputStream.readString(22));
			/*int insType = */inputStream.readByteAsInt();
			int anzSamples = inputStream.readIntelWord();
			
			if (anzSamples>0)
			{
				setNSamples(getNSamples()+anzSamples);
				/*int sampleHeaderSize = */inputStream.readIntelDWord();
				int [] sampleIndex = new int[96];
				int [] noteIndex = new int[96];
				for (int i=0; i<96; i++)
				{
					sampleIndex[i] = inputStream.readByteAsInt() + sampleOffsetIndex + 1;
					noteIndex[i] = i;
				}
				currentIns.setIndexArray(sampleIndex);
				currentIns.setNoteArray(noteIndex);
				
				int [] volumeEnvelopePosition = new int[12];
				int [] volumeEnvelopeValue = new int[12];
				for (int i=0; i<12; i++)
				{
					volumeEnvelopePosition[i] = inputStream.readIntelWord();
					volumeEnvelopeValue[i] = inputStream.readIntelWord();
				}
				Envelope volumeEnvelope = new Envelope();
				volumeEnvelope.setPosition(volumeEnvelopePosition);
				volumeEnvelope.setValue(volumeEnvelopeValue);
				currentIns.setVolumeEnvelope(volumeEnvelope);
				
				int [] panningEnvelopePosition = new int[12];
				int [] panningEnvelopeValue = new int[12];
				for (int i=0; i<12; i++)
				{
					panningEnvelopePosition[i] = inputStream.readIntelWord();
					panningEnvelopeValue[i] = inputStream.readIntelWord();
				}
				Envelope panningEnvelope = new Envelope();
				panningEnvelope.setPosition(panningEnvelopePosition);
				panningEnvelope.setValue(panningEnvelopeValue);
				currentIns.setPanningEnvelope(panningEnvelope);
				
				volumeEnvelope.setNPoints(inputStream.readByteAsInt());
				panningEnvelope.setNPoints(inputStream.readByteAsInt());
				
				volumeEnvelope.setSustainPoint(inputStream.readByteAsInt());
				volumeEnvelope.setLoopStartPoint(inputStream.readByteAsInt());
				volumeEnvelope.setLoopEndPoint(inputStream.readByteAsInt());
				
				panningEnvelope.setSustainPoint(inputStream.readByteAsInt());
				panningEnvelope.setLoopStartPoint(inputStream.readByteAsInt());
				panningEnvelope.setLoopEndPoint(inputStream.readByteAsInt());

				volumeEnvelope.setXMType(inputStream.readByteAsInt());
				panningEnvelope.setXMType(inputStream.readByteAsInt());
				
				vibratoType = inputStream.readByteAsInt();
				vibratoSweep = inputStream.readByteAsInt();
				vibratoDepth = inputStream.readByteAsInt();
				vibratoRate = inputStream.readByteAsInt();
				
				currentIns.setVolumeFadeOut(inputStream.readIntelWord());
				
				// Reserved
				inputStream.skip(2);
			}
			inputStream.seek(LSEEK+instrumentHeaderSize);
			
			instrumentContainer.reallocSampleSpace(getNSamples());
			for (int samIndex=0; samIndex<anzSamples; samIndex++)
			{
				Sample current = new Sample();
				
				current.setVibratoType(vibratoType);
				current.setVibratoSweep(vibratoSweep);
				current.setVibratoDepth(vibratoDepth);
				current.setVibratoRate(vibratoRate);
				
				// Length
				current.setLength(inputStream.readIntelDWord());
				
				// Repeat start and stop
				int repeatStart  = inputStream.readIntelDWord();
				int repeatLength = inputStream.readIntelDWord();
				int repeatStop = repeatStart+repeatLength;
				
				// volume 64 is maximum
				int vol  = inputStream.readByteAsInt() & 0x7F;
				current.setVolume(vol>64?64:vol);
				
				// finetune Value>0x7F means negative
				int fine = inputStream.readByteAsInt();
				fine = fine>0x7F?fine-0x100:fine;
				current.setFineTune(fine);
				current.setBaseFrequency(Helpers.it_fineTuneTable[(fine>>4)+8]);
				
				current.setFlags(inputStream.readByteAsInt());
				int loopType = 0;
				if ((current.flags&0x03)!=0x00) loopType |= Helpers.LOOP_ON;
				if ((current.flags&0x02)==0x02) loopType |= Helpers.LOOP_IS_PINGPONG;
				current.setLoopType(loopType);

				if ((current.flags&0x10)!=0)
				{
					current.length>>=1;
					repeatStart>>=1;
					repeatStop>>=1;
				}

				current.setRepeatStart(repeatStart);
				current.setRepeatStop(repeatStop);
				current.setRepeatLength(repeatStop-repeatStart);

				current.setPanning(inputStream.readByteAsInt());
				
				int transpose = inputStream.readByteAsInt();
				current.setTranspose((transpose>0x7F)?transpose-0x100:transpose);
				
				// Reserved
				inputStream.skip(1);
				
				// Samplename
				current.setName(inputStream.readString(22));
				
				instrumentContainer.setSample(samIndex + sampleOffsetIndex, current);
			}
			
			for (int samIndex=0; samIndex<anzSamples; samIndex++)
			{
				Sample current = instrumentContainer.getSample(samIndex + sampleOffsetIndex); 
				int flags = Helpers.SM_PCMD;
				if ((current.flags&0x10)!=0) flags |= Helpers.SM_16BIT;
				readSampleData(current, flags, inputStream);
			}
			instrumentContainer.setInstrument(ins, currentIns);
			sampleOffsetIndex += anzSamples;
		}
		cleanUpArrangement();
	}
}
