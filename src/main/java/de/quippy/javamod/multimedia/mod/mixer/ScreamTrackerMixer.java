/*
 * @(#) ScreamTrackerMixer.java
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
package de.quippy.javamod.multimedia.mod.mixer;

import de.quippy.javamod.multimedia.mod.loader.Module;
import de.quippy.javamod.multimedia.mod.loader.pattern.PatternElement;
import de.quippy.javamod.system.Helpers;

/**
 * This is the screamtracker mixing routine with all special mixing
 * on typical screamtracker events
 * @author Daniel Becker
 * @since 07.05.2006
 */
public class ScreamTrackerMixer extends BasicModMixer
{
	/**
	 * Constructor for ScreamTrackerMixer
	 * @param mod
	 * @param sampleRate
	 * @param doISP
	 */
	public ScreamTrackerMixer(Module mod, int sampleRate, int doISP, int doNoLoops)
	{
		super(mod, sampleRate, doISP, doNoLoops);
	}
	/**
	 * @param channel
	 * @param aktMemo
	 * @see de.quippy.javamod.multimedia.mod.mixer.BasicModMixer#initializeMixer(int, de.quippy.javamod.multimedia.mod.mixer.BasicModMixer.ChannelMemory)
	 */
	@Override
	protected void initializeMixer(int channel, ChannelMemory aktMemo)
	{
		if (mod.getModType()==Helpers.MODTYPE_IT)
		{
			aktMemo.muted = ((mod.getPanningValue(channel) & 0x200)!=0); // 0x80<<2
			aktMemo.doSurround = (mod.getPanningValue(channel) == 400); // 100<<2 - ist in der Tat kein HEX!!!
		}
	}
	/**
	 * Clear all effekts. Sometimes, if Effekts do continue, they are not stopped.
	 * @param aktMemo
	 * @param nextElement
	 */
	@Override
	protected void resetAllEffects(ChannelMemory aktMemo, PatternElement nextElement, boolean forced)
	{
		if (aktMemo.arpegioIndex>=0)
		{
			aktMemo.arpegioIndex=-1;
			int nextNotePeriod = aktMemo.arpegioNote[0];
			if (nextNotePeriod!=0)
			{
				setNewPlayerTuningFor(aktMemo, aktMemo.currentNotePeriod = nextNotePeriod);
			}
		}
		if (aktMemo.vibratoOn) // We have a vibrato for reset
		{
			if (forced || (nextElement.getEffekt()!=0x08 && nextElement.getEffekt()!=0x0B && nextElement.getEffekt()!=0x15)) // but only, if there is no vibrato following 
			{
				aktMemo.vibratoOn = false;
				if (!aktMemo.vibratoNoRetrig) aktMemo.vibratoTablePos = 0;
				setNewPlayerTuningFor(aktMemo);
			}
		}
		if (aktMemo.tremoloOn) // We have a tremolo for reset
		{
			if (forced || nextElement.getEffekt()!=0x12) //but only, if there is no tremolo following
			{
				aktMemo.tremoloOn = false;
				if (!aktMemo.tremoloNoRetrig) aktMemo.tremoloTablePos = 0;
			}
		}
		if (aktMemo.panbrelloOn) // We have a panbrello for reset
		{
			if (forced || nextElement.getEffekt()!=0x19) //but only, if there is no panbrello following
			{
				aktMemo.panbrelloOn = false;
				if (!aktMemo.panbrelloNoRetrig) aktMemo.panbrelloTablePos = 0;
			}
		}
	}
	/**
	 * @param aktMemo
	 * @see de.quippy.javamod.multimedia.mod.mixer.BasicModMixer#doRowEffects(de.quippy.javamod.multimedia.mod.mixer.BasicModMixer.ChannelMemory)
	 */
	@Override
	protected void doRowEffects(final ChannelMemory aktMemo)
	{
		if (aktMemo.effekt==0 && aktMemo.effektParam==0) return;

		switch (aktMemo.effekt)
		{
			case 0x01 :			// SET SPEED
				currentTick = currentTempo = aktMemo.effektParam;
				break;
			case 0x02 :			// Pattern position jump
				patternBreakJumpPatternIndex = aktMemo.effektParam;
				break;
			case 0x03 :			// Pattern break
				patternBreakRowIndex = ((aktMemo.effektParam>>4)*10)+(aktMemo.effektParam&0x0F);
				break;
			case 0x04 :			// Volume Slide
				if (aktMemo.effektParam!=0) aktMemo.volumSlideValue = aktMemo.effektParam;
				// Fine Volume Up/Down and FastSlides
				if (isFineVolumeSlide(aktMemo.volumSlideValue) || useFastSlides)
					doVolumeSlideEffekt(aktMemo);
				break;
			case 0x05 :			// Porta Down
				if (aktMemo.effektParam!=0) aktMemo.portaStepDown=aktMemo.effektParam;
					
				int indicatorPortaDown = aktMemo.portaStepDown&0xF0;
				if (indicatorPortaDown==0xE0 || indicatorPortaDown==0xF0) // (Extra) Fine Porta Down
				{
					int effektOp = aktMemo.portaStepDown&0xF;
					aktMemo.currentNotePeriod += (indicatorPortaDown==0xE0)?effektOp<<2:effektOp<<4;
					setNewPlayerTuningFor(aktMemo);
				}
				break;
			case 0x06 :			// Porta Up
				if (aktMemo.effektParam!=0) aktMemo.portaStepUp=aktMemo.effektParam;

				int indicatorPortaUp = aktMemo.portaStepUp&0xF0;
				if (indicatorPortaUp==0xE0 || indicatorPortaUp==0xF0) // Extra Fine Porta Up
				{
					int effektOp = aktMemo.portaStepUp&0xF;
					aktMemo.currentNotePeriod -= (indicatorPortaUp==0xE0)?effektOp<<2:effektOp<<4;
					setNewPlayerTuningFor(aktMemo);
				}
				break;
			case 0x07 : 		// Porta To Note
				if (aktMemo.assignedNotePeriod!=0) aktMemo.portaTargetNotePeriod = getFineTunePeriod(aktMemo);
				if (aktMemo.effektParam!=0) aktMemo.portaNoteStep = aktMemo.effektParam<<4;
				break;
			case 0x15 :			// Fine Vibrato
				// This effect is identicatal to the vibrato, but has a 4x smaller amplitude (more precise).
				// But that is decided during tick effekts.
			case 0x08 :			// Vibrato
				if ((aktMemo.effektParam>>4)!=0) aktMemo.vibratoStep = aktMemo.effektParam>>4;
				if ((aktMemo.effektParam&0xF)!=0) aktMemo.vibratoAmplitude = aktMemo.effektParam&0xF;
				aktMemo.vibratoOn = true;
				break;
			case 0x09 :			// Tremor
				if (aktMemo.effektParam!=0)
				{
					aktMemo.tremorCount = 0;
					aktMemo.tremorOntime = (aktMemo.effektParam>>4)+1; 
					aktMemo.tremorOfftime = (aktMemo.effektParam&0xF)+1;
				}
				doTremorEffekt(aktMemo);
				break;
			case 0x0A :			// Arpeggio
				if (aktMemo.assignedNotePeriod!=0)
				{
					int currentIndex = aktMemo.assignedNoteIndex-1; // Index into noteValues Table
					aktMemo.arpegioNote[0] = getFineTunePeriod(aktMemo);
					aktMemo.arpegioNote[1] = getFineTunePeriod(aktMemo, currentIndex+(aktMemo.effektParam >>4));
					aktMemo.arpegioNote[2] = getFineTunePeriod(aktMemo, currentIndex+(aktMemo.effektParam&0xF));
					aktMemo.arpegioIndex=0;
				}
				break;
			case 0x0B :			// Vibrato + Volume Slide
				aktMemo.vibratoOn = true;
				if (aktMemo.effektParam!=0) aktMemo.volumSlideValue = aktMemo.effektParam;
				// Fine Volume Up/Down and FastSlides
				if (isFineVolumeSlide(aktMemo.volumSlideValue) || useFastSlides)
					doVolumeSlideEffekt(aktMemo);
				break;
			case 0x0C :			// Porta To Note + VolumeSlide
				if (aktMemo.assignedNotePeriod!=0) aktMemo.portaTargetNotePeriod = getFineTunePeriod(aktMemo);
				if (aktMemo.effektParam!=0) aktMemo.volumSlideValue = aktMemo.effektParam;
				// Fine Volume Up/Down and FastSlides
				if (isFineVolumeSlide(aktMemo.volumSlideValue) || useFastSlides)
					doVolumeSlideEffekt(aktMemo);
				break;
			case 0x0D :			// Set Channel Volume
				aktMemo.channelVolume = aktMemo.effektParam;
				if (aktMemo.channelVolume>64) aktMemo.channelVolume = 64;
				break;
			case 0x0E :			// Channel Volume Slide
				if (aktMemo.effektParam!=0) aktMemo.channelVolumSlideValue = aktMemo.effektParam;
				// Fine Volume Up/Down and FastSlides
				if (isFineVolumeSlide(aktMemo.channelVolumSlideValue) || useFastSlides)
				{
					doChannelVolumeSlideEffekt(aktMemo);
				}
				break;
			case 0x0F : 		// Sample Offset
				final PatternElement element = aktMemo.currentElement;
				if ((element.getPeriod()!=0 || element.getNoteIndex()!=0) && aktMemo.currentSample!=null) 
				{
					if (aktMemo.effektParam!=0)
					{
						aktMemo.sampleOffset = aktMemo.highSampleOffset<<16 | aktMemo.effektParam<<8;
						aktMemo.highSampleOffset = 0;
						if (aktMemo.sampleOffset>=aktMemo.currentSample.length)
							aktMemo.sampleOffset = aktMemo.currentSample.length-1;
					}
					aktMemo.currentSamplePos = aktMemo.sampleOffset; 
					aktMemo.currentDirection = aktMemo.currentTuningPos = 0;
				}
				break;
			case 0x10 :			// Panning Slide
				if (aktMemo.effektParam!=0)
				{
					if ((aktMemo.effektParam>>4)!=0)
						aktMemo.panningSlideValue = (aktMemo.effektParam>>4)<<2;
					else
						aktMemo.panningSlideValue = -((aktMemo.effektParam&0xF)<<2);
				}
				break;
			case 0x11:			// Retrig Note
				if ((aktMemo.effektParam&0xF)!=0)
				{
					aktMemo.retrigCount = aktMemo.retrigMemo = aktMemo.effektParam&0xF;
					aktMemo.retrigVolSlide = aktMemo.effektParam>>4;
				}
				doRetrigNote(aktMemo);
				break;
			case 0x12 :			// Tremolo
				if ((aktMemo.effektParam>>4)!=0) aktMemo.tremoloStep = aktMemo.effektParam>>4;
				if ((aktMemo.effektParam&0xF)!=0) aktMemo.tremoloAmplitude = aktMemo.effektParam&0xF;
				aktMemo.tremoloOn = true;
				break;
			case 0x13 : 		// Extended
				final int effektOpEx = aktMemo.effektParam&0x0F;
				switch (aktMemo.effektParam>>4)
				{
					case 0x1:	// Glissando
						aktMemo.glissando = effektOpEx!=0;
						break;
					case 0x2:	// Set FineTune
						aktMemo.currentFineTune = Helpers.it_fineTuneTable[effektOpEx];
						aktMemo.currentFinetuneFrequency = Helpers.it_fineTuneTable[effektOpEx];
						setNewPlayerTuningFor(aktMemo);
						break;
					case 0x3:	// Set Vibrato Type
						aktMemo.vibratoType=effektOpEx&0x3;
						aktMemo.vibratoNoRetrig = ((effektOpEx&0x04)!=0);
						break;
					case 0x4:	// Set Tremolo Type
						aktMemo.tremoloType=effektOpEx&0x3;
						aktMemo.tremoloNoRetrig = ((effektOpEx&0x04)!=0);
						break;
					case 0x5:	// Set Panbrello Type
						aktMemo.panbrelloType=effektOpEx&0x3;
						aktMemo.panbrelloNoRetrig = ((effektOpEx&0x04)!=0);
						break;
					case 0x6:	// Pattern Delay Frame
						if (patternTicksDelayCount<=0) patternTicksDelayCount = effektOpEx;
						break;
					case 0x7:	// set NNA TODO
						break;
					case 0x8:	// Set Fine Panning
						aktMemo.panning = effektOpEx<<4;
						break;
					case 0x9:	// TODO Extended Channel Effekt
						switch (effektOpEx)
						{
							case 0x0: // Disable surround for the current channel
								aktMemo.doSurround = false; 
								break;
							case 0x1: //  Enable surround for the current channel. Note that a panning effect will automatically desactive the surround, unless the 4-way (Quad) surround mode has been activated with the S9B effect.
								aktMemo.doSurround = true; 
								break;
							case 0x8: // Disable reverb for this channel
								break;
							case 0x9: // Force reverb for this channel
								break;
							case 0xA: // Select mono surround mode (center channel). This is the default
								break;
							case 0xB: // Select quad surround mode: this allows you to pan in the rear channels, especially useful for 4-speakers playback. Note that S9A and S9B do not activate the surround for the current channel, it is a global setting that will affect the behavior of the surround for all channels. You can enable or disable the surround for individual channels by using the S90 and S91 effects. In quad surround mode, the channel surround will stay active until explicitely disabled by a S90 effect
								break;
							case 0xC: // Select global filter mode (IT compatibility). This is the default, when resonant filters are enabled with a Zxx effect, they will stay active until explicitely disabled by setting the cutoff frequency to the maximum (Z7F), and the resonance to the minimum (Z80).
								break;
							case 0xD: // Select local filter mode (MPT beta compatibility): when this mode is selected, the resonant filter will only affect the current note. It will be deactivated when a new note is being played.
								break;
							case 0xE: // Play forward. You may use this to temporarily force the direction of a bidirectional loop to go forward.
								break;
							case 0xF: // Play backward. The current instrument will be played backwards, or it will temporarily set the direction of a loop to go backward. 									
								break;
						}
						break;
					case 0xA:	// set High Offset
						aktMemo.highSampleOffset = aktMemo.effektParam&0x0F;
						break;
					case 0xB :	// JumpLoop
						if (effektOpEx==0) // Set a marker for loop
						{
							aktMemo.jumpLoopPatternRow = currentRow;
							aktMemo.jumpLoopPositionSet = true;
							break;
						}
						if (aktMemo.jumpLoopRepeatCount==-1) // was not set!
						{
							aktMemo.jumpLoopRepeatCount=effektOpEx;
							if (!aktMemo.jumpLoopPositionSet) // if not set, pattern Start is default!
							{
								aktMemo.jumpLoopPatternRow = 0;
								aktMemo.jumpLoopPositionSet = true;
							}
						}
						if (aktMemo.jumpLoopRepeatCount>0 && aktMemo.jumpLoopPositionSet)
						{
							aktMemo.jumpLoopRepeatCount--;
							patternJumpPatternIndex = aktMemo.jumpLoopPatternRow;
						}
						else
						{
							aktMemo.jumpLoopPositionSet = false;
							aktMemo.jumpLoopRepeatCount = -1;
						}
						break;
					case 0xC :	// Note Cut
						aktMemo.noteCutCount = effektOpEx;
						break;
					case 0xD :	// Note Delay
						aktMemo.noteDelayCount = effektOpEx;
						break;
					case 0xE :	// Pattern Delay
						if (patternDelayCount<=0) patternDelayCount=effektOpEx; // if currently in the patternDelay, do NOT reset the value. We would wait forever!!!
						break;
					case 0xF:	// TODO Set Active Macro
						break;
				}
				break;
			case 0x14 :			// set Tempo
				if (aktMemo.effektParam>>4==0) 			// 0x0X
					currentBPM -= aktMemo.effektParam&0xF;
				else
				if (aktMemo.effektParam>>4==1) 			// 0x1X
					currentBPM += aktMemo.effektParam&0xF;
				else
					currentBPM = aktMemo.effektParam;	// 0x2X
				samplePerTicks = calculateSamplesPerTick();
				break;
			case 0x16 :			// Set Global Volume
				if (mod.getModType()==Helpers.MODTYPE_IT)
					globalVolume = aktMemo.effektParam;
				else
					globalVolume = aktMemo.effektParam<<1;
				if (globalVolume>128) globalVolume = 128;
				break;
			case 0x17 :			// Global Volume Slide
				if ((aktMemo.effektParam>>4)!=0)
					globalVolumSlideValue = aktMemo.effektParam>>4;
				else
				//if ((aktMemo.effektParam&0xF)!=0)
					globalVolumSlideValue = -(aktMemo.effektParam&0xF);

				if (mod.getModType()!=Helpers.MODTYPE_IT)
					globalVolumSlideValue<<=1;
				doGlobalVolumeSlideEffekt();
				break;
			case 0x18 :			// Set Panning
				if (aktMemo.effektParam==0xA4)
				{
					aktMemo.doSurround = true;
					aktMemo.panning = 0x80;
				}
				else
				{
					aktMemo.doSurround = false;
					aktMemo.panning = aktMemo.effektParam;
				}
				break;
			case 0x19 :			// Panbrello
				if ((aktMemo.effektParam>>4)!=0) aktMemo.panbrelloStep = aktMemo.effektParam>>4;
				if ((aktMemo.effektParam&0xF)!=0) aktMemo.panbrelloAmplitude = aktMemo.effektParam&0xF;
				aktMemo.panbrelloOn = true;
				break;
			case 0x20 :			// Makros TODO
				break;
		}
	}
	/**
	 * Convenient Method for the Porta to note Effekt
	 * @param aktMemo
	 */
	private void doPortaToNoteEffekt(ChannelMemory aktMemo)
	{
		if (aktMemo.portaTargetNotePeriod!=aktMemo.currentNotePeriod)
		{
			if (aktMemo.portaTargetNotePeriod<aktMemo.currentNotePeriod)
			{
				aktMemo.currentNotePeriod -= aktMemo.portaNoteStep;
				if (aktMemo.currentNotePeriod<aktMemo.portaTargetNotePeriod)
					aktMemo.currentNotePeriod=aktMemo.portaTargetNotePeriod;
			}
			else
			{
				aktMemo.currentNotePeriod += aktMemo.portaNoteStep;
				if (aktMemo.currentNotePeriod>aktMemo.portaTargetNotePeriod)
					aktMemo.currentNotePeriod=aktMemo.portaTargetNotePeriod;
			}
			setNewPlayerTuningFor(aktMemo);
		}
	}
	/**
	 * Convenient Method for the vibrato effekt
	 * @param aktMemo
	 */
	protected void doVibratoEffekt(ChannelMemory aktMemo, boolean doFineVibrato)
	{
		int periodAdd;
		switch (aktMemo.vibratoType & 0x03)
		{
			case 1: periodAdd = (Helpers.ModRampDownTable[aktMemo.vibratoTablePos]);	//Sawtooth
					break;
			case 2: periodAdd = (Helpers.ModSquareTable  [aktMemo.vibratoTablePos]);	//Squarewave
					break;
			case 3:	periodAdd = (Helpers.ModRandomTable  [aktMemo.vibratoTablePos]);	//Random.
					break;
			default:periodAdd = (Helpers.ModSinusTable   [aktMemo.vibratoTablePos]);	//Sinus
					break;
		}
		
		periodAdd = ((periodAdd<<4)*aktMemo.vibratoAmplitude) >> ((doFineVibrato)?9:7);
		setNewPlayerTuningFor(aktMemo, aktMemo.currentNotePeriod + periodAdd);
		
		aktMemo.vibratoTablePos = (aktMemo.vibratoTablePos + aktMemo.vibratoStep) & 0x3F;
	}
	/**
	 * Convenient Method for the tremolo effekt 
	 * @param aktMemo
	 */
	protected void doTremoloEffekt(ChannelMemory aktMemo)
	{
		int volumeAdd;
		switch (aktMemo.tremoloType & 0x03)
		{
			case 1: volumeAdd = (Helpers.ModRampDownTable[aktMemo.tremoloTablePos]);	//Sawtooth
					break;
			case 2: volumeAdd = (Helpers.ModSquareTable  [aktMemo.tremoloTablePos]);	//Squarewave
					break;
			case 3:	volumeAdd = (Helpers.ModRandomTable  [aktMemo.tremoloTablePos]);	//Random.
					break;
			default:volumeAdd = (Helpers.ModSinusTable   [aktMemo.tremoloTablePos]);	//Sinus
					break;
		}

		volumeAdd = (volumeAdd * aktMemo.tremoloAmplitude) >> 7;
		aktMemo.currentVolume = aktMemo.currentSetVolume + volumeAdd;
		
		aktMemo.tremoloTablePos = (aktMemo.tremoloTablePos + aktMemo.tremoloStep) & 0x3F;
	}
	/**
	 * The tremor effekt
	 * @param aktMemo
	 */
	protected void doTremorEffekt(ChannelMemory aktMemo)
	{
		if (aktMemo.tremorCount<aktMemo.tremorOntime)
			aktMemo.currentVolume = aktMemo.currentSetVolume;
		else
			aktMemo.currentVolume = 0;
		
		aktMemo.tremorCount++;
		if (aktMemo.tremorCount>(aktMemo.tremorOntime + aktMemo.tremorOfftime)) aktMemo.tremorCount=0;
	}
	/**
	 * Check if effekt is a fine volume slide
	 * @since 04.04.2020
	 * @param volumeSlideValue
	 * @return
	 */
	private boolean isFineVolumeSlide(final int volumeSlideValue)
	{
		return ((volumeSlideValue>>4)==0xF && (volumeSlideValue&0xF)!=0x0) ||
			   ((volumeSlideValue>>4)!=0x0 && (volumeSlideValue&0xF)==0xF);
	}
	/**
	 * Convenient Method for the VolumeSlide Effekt
	 * @param aktMemo
	 */
	protected void doVolumeSlideEffekt(final ChannelMemory aktMemo)
	{
		final int x = aktMemo.volumSlideValue>>4;
		final int y = aktMemo.volumSlideValue&0xF;
		
		if (x!=0)
		{
			if (x==0xF && y!=0) // Fine Slide Down
				aktMemo.currentVolume -= y;
			else
				aktMemo.currentVolume += x;
		}
		else
		if (y!=0)
		{
			if (x!=0 && y==0xF) // Fine Slide Up
				aktMemo.currentVolume += x;
			else
				aktMemo.currentVolume -= y;
		}
		
		if (aktMemo.currentVolume>64) aktMemo.currentVolume = 64;
		else
		if (aktMemo.currentVolume<0) aktMemo.currentVolume = 0;

		aktMemo.currentSetVolume = aktMemo.currentVolume;
	}
	/**
	 * Same as the volumeSlide, but affects the channel volume
	 * @since 21.06.2006
	 * @param aktMemo
	 */
	protected void doChannelVolumeSlideEffekt(final ChannelMemory aktMemo)
	{
		int x = aktMemo.channelVolumSlideValue>>4;
		int y = aktMemo.channelVolumSlideValue&0xF;
		if (x!=0)
		{
			if (x==0xF && y!=0)
				aktMemo.channelVolume -= y;
			else
				aktMemo.channelVolume += x;
		}
		else
		if (y!=0)
		{
			if (x!=0 && y==0xF)
				aktMemo.channelVolume += x;
			else
				aktMemo.channelVolume -= y;
		}
		
		if (aktMemo.channelVolume>64) aktMemo.channelVolume = 64;
		else
		if (aktMemo.channelVolume<0) aktMemo.channelVolume = 0;
	}
	/**
	 * Convenient Method for the Global VolumeSlideEffekt
	 * @since 21.06.2006
	 * @param aktMemo
	 */
	protected void doGlobalVolumeSlideEffekt()
	{
		globalVolume+=globalVolumSlideValue;
		if (globalVolume>128) globalVolume = 128;
		else
		if (globalVolume<0) globalVolume = 0;
	}
	/**
	 * Convenient Method for the panning slide Effekt
	 * @param aktMemo
	 */
	protected void doPanningSlideEffekt(ChannelMemory aktMemo)
	{
		aktMemo.doSurround = false;
		aktMemo.panning += aktMemo.panningSlideValue;
	}
	/**
	 * Retriggers the note and does volume slide
	 * @since 04.04.2020
	 * @param aktMemo
	 */
	protected void doRetrigNote(final ChannelMemory aktMemo)
	{
		aktMemo.retrigCount--;
		if (aktMemo.retrigCount<=0)
		{
			aktMemo.retrigCount = aktMemo.retrigMemo;
			resetInstrument(aktMemo);
			if (aktMemo.retrigVolSlide>0)
			{
				switch (aktMemo.retrigVolSlide)
				{
					case 0x1: aktMemo.currentVolume--; break;
					case 0x2: aktMemo.currentVolume-=2; break;
					case 0x3: aktMemo.currentVolume-=4; break;
					case 0x4: aktMemo.currentVolume-=8; break;
					case 0x5: aktMemo.currentVolume-=16; break;
					case 0x6: aktMemo.currentVolume=Helpers.ft2TwoThirds[aktMemo.currentVolume<0?0:aktMemo.currentVolume>63?63:aktMemo.currentVolume]; /*(aktMemo.currentVolume<<1)/3;*/ break;
					case 0x7: aktMemo.currentVolume>>=1; break;
					case 0x8: /* Documentary says ? */ break;
					case 0x9: aktMemo.currentVolume++; break;
					case 0xA: aktMemo.currentVolume+=2; break;
					case 0xB: aktMemo.currentVolume+=4; break;
					case 0xC: aktMemo.currentVolume+=8; break;
					case 0xD: aktMemo.currentVolume+=16; break;
					case 0xE: aktMemo.currentVolume=(aktMemo.currentVolume*3)>>1; break;
					case 0xF: aktMemo.currentVolume<<=1; break;
				}
				if (aktMemo.currentVolume>64) aktMemo.currentVolume = 64;
				else
				if (aktMemo.currentVolume<0) aktMemo.currentVolume = 0;
				aktMemo.currentSetVolume = aktMemo.currentVolume;
				System.out.println(aktMemo.currentSetVolume);
			}
		}
	}
	/**
	 * @param aktMemo
	 * @see de.quippy.javamod.multimedia.mod.mixer.BasicModMixer#doTickEffekts(de.quippy.javamod.multimedia.mod.mixer.BasicModMixer.ChannelMemory)
	 */
	@Override
	protected void doTickEffekts(ChannelMemory aktMemo)
	{
		if (aktMemo.effekt==0 && aktMemo.effektParam==0) return;
		
		switch (aktMemo.effekt)
		{
			case 0x04 : 		// VolumeSlide, if *NOT* Fine Slide
				if (!isFineVolumeSlide(aktMemo.volumSlideValue))
					doVolumeSlideEffekt(aktMemo);
				break;
			case 0x05: 			// Porta Down
				int indicatorPortaDown = aktMemo.portaStepDown&0xF0;
				if (indicatorPortaDown!=0xE0 && indicatorPortaDown!=0xF0) // (Extra) Fine Porta Down
				{
					aktMemo.currentNotePeriod += (aktMemo.portaStepDown<<4);
					if (aktMemo.glissando) aktMemo.currentNotePeriod = Helpers.getRoundedPeriod(aktMemo.currentNotePeriod>>4)<<4;
					setNewPlayerTuningFor(aktMemo);
				}
				break;
			case 0x06: 			// Porta Up
				int indicatorPortaUp = aktMemo.portaStepUp&0xF0;
				if (indicatorPortaUp!=0xE0 && indicatorPortaUp!=0xF0) // (Extra) Fine Porta Down
				{
					aktMemo.currentNotePeriod -= (aktMemo.portaStepUp<<4);
					if (aktMemo.glissando) aktMemo.currentNotePeriod = Helpers.getRoundedPeriod(aktMemo.currentNotePeriod>>4)<<4;
					setNewPlayerTuningFor(aktMemo);
				}
				break;
			case 0x07 :			// Porta to Note
				doPortaToNoteEffekt(aktMemo);
				break;
			case 0x08 :			// Vibrato
				doVibratoEffekt(aktMemo, false);
				break;
			case 0x09 :			// Tremor
				doTremorEffekt(aktMemo);
				break;
			case 0x0A :			// Arpeggio
				aktMemo.arpegioIndex = (aktMemo.arpegioIndex+1)%3;
				int nextNotePeriod = aktMemo.arpegioNote[aktMemo.arpegioIndex];
				if (nextNotePeriod!=0)
				{
					aktMemo.currentNotePeriod = nextNotePeriod;
					setNewPlayerTuningFor(aktMemo);
				}
				break;
			case 0x0B:			// Vibrato + VolumeSlide
				doVibratoEffekt(aktMemo, false);
				doVolumeSlideEffekt(aktMemo);
				break;
			case 0x0C :			// Porta to Note + VolumeSlide
				doPortaToNoteEffekt(aktMemo);
				doVolumeSlideEffekt(aktMemo);
				break;
			case 0x0E :			// Channel Volume Slide, if *NOT* Fine Slide
				if (!isFineVolumeSlide(aktMemo.channelVolumSlideValue))
					doChannelVolumeSlideEffekt(aktMemo);
				break;
			case 0x10 :			// Panning Slide
				doPanningSlideEffekt(aktMemo);
				break;
			case 0x11 :			// Retrig Note
				doRetrigNote(aktMemo);
				break;
			case 0x12 :			// Tremolo
				doTremoloEffekt(aktMemo);
				break;
			case 0x13 :			// Extended
				switch (aktMemo.effektParam>>4)
				{
					case 0xC :	// Note Cut
						if (aktMemo.noteCutCount>0)
						{
							aktMemo.noteCutCount--;
							if (aktMemo.noteCutCount<=0)
							{
								aktMemo.noteCutCount=-1;
								aktMemo.currentVolume = 0;
							}
						}
						break;
					case 0xD:	// Note Delay
						if (aktMemo.noteDelayCount>0)
						{
							aktMemo.noteDelayCount--;
							if (aktMemo.noteDelayCount<=0)
							{
								aktMemo.noteDelayCount = -1;
								setNewInstrumentAndPeriod(aktMemo);
							}
						}
						break;
				}
				break;
			case 0x15 :			// Fine Vibrato
				doVibratoEffekt(aktMemo, true);
				break;
			case 0x17 :			// Global Volume Slide
				doGlobalVolumeSlideEffekt();
				break;
		}
	}
	/**
	 * @param aktMemo
	 * @see de.quippy.javamod.multimedia.mod.mixer.BasicModMixer#doVolumeColumnRowEffekt(de.quippy.javamod.multimedia.mod.mixer.BasicModMixer.ChannelMemory)
	 */
	@Override
	protected void doVolumeColumnRowEffekt(ChannelMemory aktMemo)
	{
		if (aktMemo.volumeEffekt==0) return;
		
		switch (aktMemo.volumeEffekt)
		{
			case 0x01: // Set Volume
				aktMemo.currentSetVolume = aktMemo.currentVolume = aktMemo.volumeEffektOp;
				break;
			case 0x02: // Volslide down
				aktMemo.volumSlideValue = -aktMemo.volumeEffektOp;
				break;
			case 0x03: // Volslide up
				aktMemo.volumSlideValue = aktMemo.volumeEffektOp;
				break;
			case 0x04: // Fine Volslide down
				aktMemo.currentSetVolume = aktMemo.currentVolume -= aktMemo.volumeEffektOp;
				break;
			case 0x05: // Fine Volslide up
				aktMemo.currentSetVolume = aktMemo.currentVolume += aktMemo.volumeEffektOp;
				break;
			case 0x06: // vibrato speed
				if (aktMemo.volumeEffektOp!=0) aktMemo.vibratoStep = aktMemo.volumeEffektOp;
				break;
			case 0x07: // vibrato
				if (aktMemo.volumeEffektOp!=0) aktMemo.vibratoAmplitude = aktMemo.volumeEffektOp;
				break;
			case 0x08: // Set Panning
				aktMemo.doSurround = false;
				aktMemo.panning = aktMemo.volumeEffektOp<<4;
				break;
			case 0x09: // Panning Slide Left
				aktMemo.panningSlideValue = -aktMemo.volumeEffektOp;
				break;
			case 0x0A: // Panning Slide Right
				aktMemo.panningSlideValue = aktMemo.volumeEffektOp;
				break;
			case 0x0B: // Tone Porta
				if (aktMemo.assignedNotePeriod!=0) aktMemo.portaTargetNotePeriod = getFineTunePeriod(aktMemo);
				if (aktMemo.volumeEffektOp!=0) aktMemo.portaNoteStep = aktMemo.volumeEffektOp;
				break;
			case 0x0C: // Porta Down
				aktMemo.portaStepDown = aktMemo.volumeEffektOp<<4;
				break;
			case 0x0D: // Porta Up
				aktMemo.portaStepUp = aktMemo.volumeEffektOp<<4;
				break;
		}
	}
	/**
	 * @param aktMemo
	 * @see de.quippy.javamod.multimedia.mod.mixer.BasicModMixer#doVolumeColumnTickEffekt(de.quippy.javamod.multimedia.mod.mixer.BasicModMixer.ChannelMemory)
	 */
	@Override
	protected void doVolumeColumnTickEffekt(ChannelMemory aktMemo)
	{
		if (aktMemo.volumeEffekt==0) return;
		
		switch (aktMemo.volumeEffekt)
		{
			case 0x02: // Volslide down
			case 0x03: // Volslide up
				doVolumeSlideEffekt(aktMemo);
				break;
			case 0x06: // vibrato speed
			case 0x07: // vibrato
				doVibratoEffekt(aktMemo, false);
				break;
			case 0x09: // Panning Slide Left
			case 0x0A: // Panning Slide Right
				doPanningSlideEffekt(aktMemo);
				break;
			case 0x0B: // Tone Porta
				doPortaToNoteEffekt(aktMemo);
				break;
		}
	}
	/**
	 * @param aktMemo
	 * @return true, if the Effekt and EffektOp indicate a NoteDelayEffekt
	 * @see de.quippy.javamod.multimedia.mod.mixer.BasicModMixer#isNoteDelayEffekt(de.quippy.javamod.multimedia.mod.mixer.BasicModMixer.ChannelMemory)
	 */
	@Override
	protected boolean isNoteDelayEffekt(ChannelMemory aktMemo)
	{
		return aktMemo.effekt==0x13 && aktMemo.effektParam>>4==0x0D;
	}
	/**
	 * @param aktMemo
	 * @return true, if the Effekt and EffektOp indicate a PortaToNoteEffekt AND there was a Note set
	 * @see de.quippy.javamod.multimedia.mod.mixer.BasicModMixer#isPortaToNoteEffekt(de.quippy.javamod.multimedia.mod.mixer.BasicModMixer.ChannelMemory)
	 */
	@Override
	protected boolean isPortaToNoteEffekt(ChannelMemory aktMemo)
	{
		return (aktMemo.effekt==0x07 || aktMemo.effekt==0x0C) && aktMemo.currentNotePeriod!=0;
	}
	/**
	 * @param aktMemo
	 * @return true, if the effekt indicates a SampleOffsetEffekt
	 * @see de.quippy.javamod.multimedia.mod.mixer.BasicModMixer#isSampleOffsetEffekt(de.quippy.javamod.multimedia.mod.mixer.BasicModMixer.ChannelMemory)
	 */
	@Override
	protected boolean isSampleOffsetEffekt(ChannelMemory aktMemo)
	{
		return aktMemo.effekt==0x0F;
	}
}
