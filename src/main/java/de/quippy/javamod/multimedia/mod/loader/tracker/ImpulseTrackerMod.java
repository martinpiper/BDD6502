/*
 * @(#) ImpulseTrackerMod.java
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

/**
 * @author Daniel Becker
 * @since 26.05.2006
 */
public class ImpulseTrackerMod extends ScreamTrackerMod
{
	private static final int [] autovibit2xm = new int [] { 0, 3, 1, 4, 2, 0, 0, 0 };
	private static final String[] MODFILEEXTENSION = new String [] 
 	{
 		"it"
 	};
	/**
	 * Will be executed during class load
	 */
	static
	{
		ModuleFactory.registerModule(new ImpulseTrackerMod());
	}

	private int special;
	private int [] channelVolume;
	
	/**
	 * Constructor for ImpulseTrackerMod
	 */
	public ImpulseTrackerMod()
	{
		super();
	}
	/**
	 * Constructor for ImpulseTrackerMod
	 * @param fileExtension
	 */
	protected ImpulseTrackerMod(String fileName)
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
	 * @see de.quippy.javamod.multimedia.mod.loader.tracker.ScreamTrackerMod#getFrequencyTable()
	 */
	@Override
	public int getFrequencyTable()
	{
		return ((songFlags & Helpers.SONG_LINEARSLIDES)!=0)? Helpers.IT_LINEAR_TABLE : Helpers.IT_AMIGA_TABLE;
	}
	/**
	 * @param channel
	 * @return
	 * @see de.quippy.javamod.multimedia.mod.loader.tracker.ScreamTrackerMod#getChannelVolume(int)
	 */
	@Override
	public int getChannelVolume(int channel)
	{
		return channelVolume[channel];
	}
	/**
	 * TODO: Read this!!!
	 * @return a special field with params
	 */
	public int getSpecial()
	{
		return special;
	}
	private void readEnvelopeData(Envelope env, int add, ModfileInputStream inputStream) throws IOException
	{
		long pos = inputStream.getFilePointer();
		
		env.setITType(inputStream.readByteAsInt());
		int nPoints = inputStream.readByteAsInt();
		if (nPoints>25) nPoints=25;
		env.setNPoints(nPoints);
		env.setLoopStartPoint(inputStream.readByteAsInt());
		env.setLoopEndPoint(inputStream.readByteAsInt());
		env.setSustainStartPoint(inputStream.readByteAsInt());
		env.setSustainEndPoint(inputStream.readByteAsInt());
		
		int [] values = new int[nPoints];
		int [] points = new int[nPoints];
		
		for (int i=0; i<nPoints; i++)
		{
			values[i] = (inputStream.readByteAsInt() + add)&0xFF;
			points[i] = inputStream.readIntelWord();
		}
		
		env.setPosition(points);
		env.setValue(values);
		
		inputStream.seek(pos+82L);
	}
	/**
	 * @param inputStream
	 * @return true, if this is a protracker mod, false if this is not clear
	 * @see de.quippy.javamod.multimedia.mod.loader.Module#checkLoadingPossible(de.quippy.javamod.io.ModfileInputStream)
	 */
	@Override
	public boolean checkLoadingPossible(ModfileInputStream inputStream) throws IOException
	{
		byte[] id = new byte[4];
		inputStream.read(id, 0, 4);
		inputStream.seek(0);
		return Helpers.convertDWordToInt(id, 0)==0x494D504D /*IMPM*/;
	}
	/**
	 * @param fileName
	 * @return
	 * @see de.quippy.javamod.multimedia.mod.loader.Module#getNewInstance(java.lang.String)
	 */
	@Override
	protected Module getNewInstance(String fileName)
	{
		return new ImpulseTrackerMod(fileName);
	}
	/**
	 * @param inputStream
	 * @return
	 * @see de.quippy.javamod.multimedia.mod.loader.Module#loadModFile(java.io.DataInputStream)
	 */
	@Override
	public void loadModFileInternal(ModfileInputStream inputStream) throws IOException
	{
		setModType(Helpers.MODTYPE_IT);

		// IT-ID:
		byte[] id = new byte[4];
		inputStream.read(id, 0, 4);
		setModID(Helpers.retrieveAsString(id, 0, 4));
		if (Helpers.convertDWordToInt(id, 0)!=0x494D504D /*IMPM*/) throw new IOException("Unsupported IT Module!");

		// Songname
		setSongName(inputStream.readString(26));
		
		//PHiliht = Pattern row hilight information. Only relevant for pattern editing situations.
		inputStream.skip(2);
		
		//OrdNum:   Number of orders in song
		setSongLength(inputStream.readIntelWord());
		//InsNum:   Number of instruments in song
		setNInstruments(inputStream.readIntelWord());
		//SmpNum:   Number of samples in song
		setNSamples(inputStream.readIntelWord());
	    //PatNum:   Number of patterns in song
		setNPattern(inputStream.readIntelWord());
		//Cwt:      Created with tracker.
		version = inputStream.readIntelWord();
		setTrackerName("Impulse Tracker V" + Helpers.getAsHex((version>>8)&0xF, 1) + "." + Helpers.getAsHex(version&0xFF, 2));
		//Cmwt:     Compatible with tracker with version greater than value. (ie. format version)
		int cmwt = inputStream.readIntelWord();
		setTrackerName(getTrackerName() + " (cmwt: " + Helpers.getAsHex((cmwt>>8)&0xF, 1) + "." + Helpers.getAsHex(cmwt&0xFF, 2) + ")");
		//Flags:    Bit 0: On = Stereo, Off = Mono
		//          Bit 1: Vol0MixOptimizations - If on, no mixing occurs if
		//                 the volume at mixing time is 0 (redundant v1.04+)
		//          Bit 2: On = Use instruments, Off = Use samples.
		//          Bit 3: On = Linear slides, Off = Amiga slides.
		//          Bit 4: On = Old Effects, Off = IT Effects
		//                  Differences:
		//                 - Vibrato is updated EVERY frame in IT mode, whereas
		//                    it is updated every non-row frame in other formats.
		//                    Also, it is two times deeper with Old Effects ON
		//                 - Command Oxx will set the sample offset to the END
		//                   of a sample instead of ignoring the command under
		//                   old effects mode.
		//                 - (More to come, probably)
		//          Bit 5: On = Link Effect G's memory with Effect E/F. Also
		//                      Gxx with an instrument present will cause the
		//                      envelopes to be retriggered. If you change a
		//                      sample on a row with Gxx, it'll adjust the
		//                      frequency of the current note according to:
		//                        NewFrequency = OldFrequency * NewC5 / OldC5;
		//          Bit 6: Use MIDI pitch controller, Pitch depth given by PWD
		//          Bit 7: Request embedded MIDI configuration
		//                 (Coded this way to permit cross-version saving)
		flags = inputStream.readIntelWord();
		if ((flags & 0x08)!=0) 		songFlags |= Helpers.SONG_LINEARSLIDES;
		if ((flags & 0x10)!=0) 		songFlags |= Helpers.SONG_ITOLDEFFECTS;
		if ((flags & 0x20)!=0) 		songFlags |= Helpers.SONG_ITCOMPATMODE;
		if ((flags & 0x80)!=0) 		songFlags |= Helpers.SONG_EMBEDMIDICFG;
		if ((flags & 0x1000)!=0) 	songFlags |= Helpers.SONG_EXFILTERRANGE;
		//Special:  Bit 0: On = song message attached.
		//                 Song message:
		//                  Stored at offset given by "Message Offset" field.
		//                  Length = MsgLgth.
		//                  NewLine = 0Dh (13 dec)
		//                  EndOfMsg = 0
		//
		//                 Note: v1.04+ of IT may have song messages of up to
		//                       8000 bytes included.
		//          Bit 1: Reserved
		//          Bit 2: Reserved
		//          Bit 3: MIDI configuration embedded
		//          Bit 4-15: Reserved
		special = inputStream.readIntelWord();
		
		//GV:       Global volume. (0->128) All volumes are adjusted by this
		setBaseVolume(inputStream.readByteAsInt());
		//MV:       Mix volume (0->128) During mixing, this value controls the magnitude of the wave being mixed.
		inputStream.skip(1);
		//IS:       Initial Speed of song.
		setTempo(inputStream.readByteAsInt());
		//IT:       Initial Tempo of song
		setBPMSpeed(inputStream.readByteAsInt());
		//Sep:      Panning separation between channels (0->128, 128 is max sep.)
		inputStream.skip(1);
		//PWD:      Pitch wheel depth for MIDI controllers
		inputStream.skip(1);
		//MsgLgth
		inputStream.skip(2);
		//MsgOffset
		inputStream.skip(4);
		//4 Byte reserved
		inputStream.skip(4);
		//Chnl Pan: Each byte contains a panning value for a channel. Ranges from
		//           0 (absolute left) to 64 (absolute right). 32 = central pan,
		//           100 = Surround sound.
		//           +128 = disabled channel (notes will not be played, but note
		//                                    that effects in muted channels are
		//                                    still processed)
		isStereo = true;
		usePanningValues = true;
		panningValue = new int[64];
		for (int i=0; i<64; i++)
		{
			int panValue = inputStream.readByteAsInt();
			if (panValue==100 || (panValue & 0x80)!=0)
			{
				panningValue[i] = panValue<<2;
			}
			else
			{
				panningValue[i]=(panValue&0x7F)<<2; 
			}
		}
		//Chnl Vol: Volume for each channel. Ranges from 0->64
		channelVolume = new int[64];
		for (int i=0; i<64; i++)
		{
			channelVolume[i] = inputStream.readByteAsInt();
			if (channelVolume[i]>64) channelVolume[i]=64;
		}
		//Orders:   This is the order in which the patterns are played.
		//           Valid values are from 0->199.
		//           255 = "---", End of song marker
		//           254 = "+++", Skip to next order
		// Song Arrangement
		allocArrangement(getSongLength());
		for (int i=0; i<getSongLength(); i++) getArrangement()[i]=inputStream.readByteAsInt();
		
		InstrumentsContainer instrumentContainer = new InstrumentsContainer(this, getNInstruments(), getNSamples());
		this.setInstrumentContainer(instrumentContainer);
		for (int i=0; i<getNInstruments(); i++)
		{
			inputStream.seek(0xC0L+getSongLength()+(i<<2));
			inputStream.seek(inputStream.readIntelDWord());

			if (inputStream.readMotorolaDWord()!=0x494D5049 /*IMPI*/) throw new IOException("Unsupported IT Instrument Header!");
			
			Instrument currentIns = new Instrument();
			currentIns.setDosFileName(inputStream.readString(13));

			Envelope volumeEnvelope = new Envelope();
			currentIns.setVolumeEnvelope(volumeEnvelope);
			Envelope panningEnvelope = new Envelope();
			currentIns.setPanningEnvelope(panningEnvelope);
			Envelope pitchEnvelope = new Envelope();
			currentIns.setPitchEnvelope(pitchEnvelope);

			// Depending on cmwt:
			if (cmwt<0x200) // Old Instrument format
			{
				volumeEnvelope.setITType(inputStream.readByteAsInt());
				volumeEnvelope.setLoopStartPoint(inputStream.readByteAsInt());
				volumeEnvelope.setLoopEndPoint(inputStream.readByteAsInt());
				volumeEnvelope.setSustainStartPoint(inputStream.readByteAsInt());
				volumeEnvelope.setSustainEndPoint(inputStream.readByteAsInt());
				inputStream.skip(2);
				currentIns.setVolumeFadeOut(inputStream.readIntelWord() << 6);
				currentIns.setGlobalVolume(64);
				currentIns.setNNA(inputStream.readByteAsInt());
				currentIns.setDublicateNoteCheck(inputStream.readByteAsInt());
				inputStream.skip(4);
			}
			else
			{
				currentIns.setNNA(inputStream.readByteAsInt());
				currentIns.setDublicateNoteCheck(inputStream.readByteAsInt());
				currentIns.setDublicateNoteAction(inputStream.readByteAsInt());
				currentIns.setVolumeFadeOut(inputStream.readIntelWord() << 6);
				currentIns.setPitchPanSeparation(inputStream.readByteAsInt());
				currentIns.setPitchPanCenter(inputStream.readByteAsInt());
				currentIns.setGlobalVolume(inputStream.readByteAsInt() >> 1);
				currentIns.setDefaultPan(inputStream.readByteAsInt());
				currentIns.setRandomVolumeVariation(inputStream.readByteAsInt());
				currentIns.setRandomPanningVariation(inputStream.readByteAsInt());
				inputStream.skip(4);
			}
			
			currentIns.setName(inputStream.readString(26));
			if (cmwt<0x200) // Old Instrument format
			{
				inputStream.skip(6);
			}
			else
			{
				currentIns.setInitialFilterCutoff(inputStream.readByteAsInt());
				currentIns.setInitialFilterResonance(inputStream.readByteAsInt());
				inputStream.skip(4);
			}
			
			int [] sampleIndex = new int[120];
			int [] noteIndex = new int[120];
			for (int j=0; j<120; j++)
			{
				noteIndex[j] = inputStream.readByteAsInt();
				sampleIndex[j] = inputStream.readByteAsInt();
			}
			currentIns.setIndexArray(sampleIndex);
			currentIns.setNoteArray(noteIndex);
			
			if (cmwt<0x200) // Old Instrument format
			{
				int [] volumeEnvelopePosition = new int[25];
				int [] volumeEnvelopeValue = new int[25];
				int nPoints = 0;
				for (; nPoints<25; nPoints++)
				{
					volumeEnvelopePosition[nPoints] = inputStream.readByteAsInt();
					volumeEnvelopeValue[nPoints] = inputStream.readByteAsInt();
				}
				volumeEnvelope.setNPoints(nPoints);
				volumeEnvelope.setPosition(volumeEnvelopePosition);
				volumeEnvelope.setValue(volumeEnvelopeValue);
			}
			else
			{
				
				readEnvelopeData(volumeEnvelope, 0, inputStream);
				readEnvelopeData(panningEnvelope, 32, inputStream);
				readEnvelopeData(pitchEnvelope, 32, inputStream);
			}
			
			instrumentContainer.setInstrument(i, currentIns);
		}
		
		for (int i=0; i<getNSamples(); i++)
		{
			inputStream.seek(0xC0L+getSongLength()+(getNInstruments()<<2)+(i<<2));
			inputStream.seek(inputStream.readIntelDWord());

			if (inputStream.readMotorolaDWord()!=0x494D5053 /*IMPI*/) throw new IOException("Unsupported IT Sample Header!");
			
			Sample currentSample = new Sample();

			currentSample.setDosFileName(inputStream.readString(13));
			currentSample.setGlobalVolume(inputStream.readByteAsInt());
			int flags = inputStream.readByteAsInt();
			currentSample.setFlags(flags);
            /*Bit 4. On = Use loop
            Bit 5. On = Use sustain loop
            Bit 6. On = Ping Pong loop, Off = Forwards loop
            Bit 7. On = Ping Pong Sustain loop, Off = Forwards Sustain loop*/
			int loopType = 0;
			if ((flags&0x10)==0x10) loopType |= Helpers.LOOP_ON; 
			if ((flags&0x20)==0x20) loopType |= Helpers.LOOP_SUSTAIN_ON; 
			if ((flags&0x40)==0x40) loopType |= Helpers.LOOP_IS_PINGPONG; 
			if ((flags&0x80)==0x80) loopType |= Helpers.LOOP_SUSTAIN_IS_PINGPONG; 
			currentSample.setLoopType(loopType);

			currentSample.setVolume(inputStream.readByteAsInt());
			currentSample.setName(inputStream.readString(26));
			int CvT = inputStream.readByteAsInt();
			currentSample.setCvT(CvT);
			int panning = inputStream.readByteAsInt();
			if ((panning&0x80)==0) 
				panning=-1;
			else
			{
				panning=(panning&0x7F)<<1;
				if (panning>128) panning=128;
			}
			currentSample.setPanning(panning);
			currentSample.setLength(inputStream.readIntelDWord());

			int repeatStart = inputStream.readIntelDWord();
			int repeatStop = inputStream.readIntelDWord();
			currentSample.setRepeatStart(repeatStart);
			currentSample.setRepeatStop(repeatStop);
			currentSample.setRepeatLength(repeatStop-repeatStart);

			currentSample.setFineTune(0);
			currentSample.setTranspose(0);
			int c4Speed = inputStream.readIntelDWord();
			if (c4Speed==0) c4Speed = 8363;
			else
			if (c4Speed<256) c4Speed = 256;
			currentSample.setBaseFrequency(c4Speed); 
			
			currentSample.setSustainLoopStart(inputStream.readIntelDWord());
			currentSample.setSustainLoopEnd(inputStream.readIntelDWord());
			
			int sampleOffset = inputStream.readIntelDWord();
			
			currentSample.setVibratoRate(inputStream.readByteAsInt());
			currentSample.setVibratoDepth(inputStream.readByteAsInt() & 0x7F);
			currentSample.setVibratoSweep((inputStream.readByteAsInt() + 3) >> 2);
			currentSample.setVibratoType(autovibit2xm[inputStream.readByteAsInt() & 0x07]);

			if (sampleOffset>0 && currentSample.length>0)
			{
				int loadFlag = 0;
				if (CvT==0xFF && (flags&0x02)!=0x02) loadFlag = Helpers.SM_ADPCM4; // No 16 Bit flag set!
				else
				{
					loadFlag = ((CvT&0x1)==0x1)?Helpers.SM_PCMS:Helpers.SM_PCMU;
					if ((flags&0x2)==0x2) loadFlag |= Helpers.SM_16BIT;
					if ((flags&0x4)==0x4) loadFlag |= Helpers.SM_STEREO;
					if ((flags&0x8)==0x8) loadFlag |= (version>=0x215 && (CvT&0x4)==0x4)?Helpers.SM_IT2158:Helpers.SM_IT2148;
				}
				inputStream.seek(sampleOffset);
				readSampleData(currentSample, loadFlag, inputStream);
			}
			
			instrumentContainer.setSample(i, currentSample);
		}

		PatternContainer patternContainer = new PatternContainer(getNPattern());
		setPatternContainer(patternContainer);
		int maxChannels = 0;
		for (int pattNum=0; pattNum<getNPattern(); pattNum++)
		{
			inputStream.seek(0xC0+getSongLength()+(getNInstruments()<<2)+(getNSamples()<<2)+(pattNum<<2));
			inputStream.seek(inputStream.readIntelDWord());
			
			int patternDataLength = inputStream.readIntelWord();
			int rows = inputStream.readIntelWord();
			
			inputStream.skip(4); // RESERVED
			
			// First, clear them:
			patternContainer.setPattern(pattNum, new Pattern(rows));
			for (int row=0; row<rows; row++)
			{
				patternContainer.setPatternRow(pattNum, row, new PatternRow(64));
				for (int channel=0; channel<64; channel++)
				{
					PatternElement currentElement = new PatternElement(pattNum, row, channel);
					patternContainer.setPatternElement(currentElement);
				}
			}
			int row = 0;
			int [] lastMask= new int[64];
			int [] lastNote= new int[64];
			int [] lastIns= new int[64];
			int [] lastVolCmd = new int[64];
			int [] lastVolOp = new int[64];
			int [] lastCmd= new int[64];
			int [] lastData= new int[64];
			while (patternDataLength>0) 
			{
				int channelByte = inputStream.readByteAsInt(); patternDataLength--;
				if (channelByte==0)
				{
					row++;
					continue;
				}
				int channel = (channelByte - 1) & 0x3F;
				if (channel>maxChannels) maxChannels = channel;
				PatternElement element = patternContainer.getPatternElement(pattNum, row, channel);
				
				if ((channelByte & 0x80)!=0)
				{
					lastMask[channel] = inputStream.readByteAsInt(); patternDataLength--;
				}
				if ((lastMask[channel]&0x01)!=0 || (lastMask[channel]&0x10)!=0)
				{
					if ((lastMask[channel]&0x01)!=0)
					{
						lastNote[channel] = inputStream.readByteAsInt(); patternDataLength--;
					}
					int noteIndex = lastNote[channel]; 
					int period;
					if (noteIndex==0xFF) // Note Off!
					{
						noteIndex = period = Helpers.KEY_OFF;
					}
					else
					if (noteIndex==0xFE) // Volume Off!
					{
						noteIndex = period = Helpers.NOTE_CUT;
					}
					else
					{
						if (noteIndex<Helpers.noteValues.length) period = Helpers.noteValues[noteIndex]; else period = 1;
						noteIndex++;
					}
					element.setNoteIndex(noteIndex);
					element.setPeriod(period);
				}
				if ((lastMask[channel]&0x02)!=0 || (lastMask[channel]&0x20)!=0)
				{
					if ((lastMask[channel]&0x02)!=0)
					{
						lastIns[channel] = inputStream.readByteAsInt(); patternDataLength--;
					}
					element.setInstrument(lastIns[channel]);
				}
				if ((lastMask[channel]&0x04)!=0 || (lastMask[channel]&0x40)!=0)
				{
					if ((lastMask[channel]&0x04)!=0)
					{
						int vol = inputStream.readByteAsInt(); patternDataLength--;
						int volCmd=0, volOp=0;

						// 0-64: Set Volume
						if (vol <= 64) { volCmd = 0x01; volOp = vol; } else
						// 128-192: Set Panning
						if ((vol >= 128) && (vol <= 192)) { volCmd = 0x08; volOp = vol - 128; } else
						// 65-74: Fine Volume Up
						if (vol < 75) {volCmd = 0x05; volOp = vol - 65; } else
						// 75-84: Fine Volume Down
						if (vol < 85) { volCmd = 0x04; volOp = vol - 75; } else
						// 85-94: Volume Slide Up
						if (vol < 95) { volCmd = 0x03; volOp = vol - 85; } else
						// 95-104: Volume Slide Down
						if (vol < 105) { volCmd = 0x02; volOp = vol - 95; } else
						// 105-114: Pitch Slide Up
						if (vol < 115) { volCmd = 0x0D; volOp = vol - 105; } else
						// 115-124: Pitch Slide Down
						if (vol < 125) { volCmd = 0x0C; volOp = vol - 115; } else
						// 193-202: Portamento To
						if ((vol >= 193) && (vol <= 202)) { volCmd = 0x0B; volOp = vol - 193; } else
						// 203-212: Vibrato
						if ((vol >= 203) && (vol <= 212)) { volCmd = 0x06; volOp = vol - 203; }
						lastVolCmd[channel] = volCmd;
						lastVolOp[channel] = volOp;
					}
					element.setVolumeEffekt(lastVolCmd[channel]);
					element.setVolumeEffektOp(lastVolOp[channel]);
				}
				if ((lastMask[channel]&0x08)!=0 || (lastMask[channel]&0x80)!=0)
				{
					if ((lastMask[channel]&0x08)!=0)
					{
						lastCmd[channel] = inputStream.readByteAsInt(); patternDataLength--;
						lastData[channel] = inputStream.readByteAsInt(); patternDataLength--;
					}
					element.setEffekt(lastCmd[channel]);
					element.setEffektOp(lastData[channel]);
				}
			}
		}
		if (maxChannels<4) maxChannels=4;
		setNChannels(maxChannels+1);
		
		// Correct the songlength for playing, skip markerpattern... (do not want to skip them during playing!)
		int realLen = 0;
		for (int i=0; i<getSongLength(); i++)
		{
			if (getArrangement()[i]==255) // end of Song:
				break;
			else
			if (getArrangement()[i]<254 && getArrangement()[i]<getNPattern())
				getArrangement()[realLen++]=getArrangement()[i];
		}
		setSongLength(realLen);
		cleanUpArrangement();
	}
}