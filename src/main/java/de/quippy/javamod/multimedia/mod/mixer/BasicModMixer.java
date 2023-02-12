/*
 * @(#) BasicModMixer.java
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
package de.quippy.javamod.multimedia.mod.mixer;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.bdd6502.AudioExpansion;
import de.quippy.javamod.multimedia.mod.loader.Module;
import de.quippy.javamod.multimedia.mod.loader.instrument.Envelope;
import de.quippy.javamod.multimedia.mod.loader.instrument.Instrument;
import de.quippy.javamod.multimedia.mod.loader.instrument.Sample;
import de.quippy.javamod.multimedia.mod.loader.pattern.Pattern;
import de.quippy.javamod.multimedia.mod.loader.pattern.PatternElement;
import de.quippy.javamod.multimedia.mod.loader.pattern.PatternRow;
import de.quippy.javamod.system.Helpers;

/**
 * @author Daniel Becker
 * @since 30.04.2006
 */
public abstract class BasicModMixer
{
	protected class ChannelMemory
	{
		public PatternElement currentElement;
		
		public boolean muted;

		public int assignedNotePeriod, currentNotePeriod, currentFinetuneFrequency;
		public int assignedNoteIndex, currentFineTune, currentTranspose;
		public int currentVolume, currentSetVolume, channelVolume, fadeOutVolume, panning, actVolumeLeft, actVolumeRight;
		public int actRampVolLeft, actRampVolRight, deltaVolLeft, deltaVolRight;
		
		public int noteDelayCount, noteCutCount;

		public Instrument assignedInstrument;
		public int assignedInstrumentIndex;
		public Sample currentSample;
		public int currentTuning, currentTuningPos, currentSamplePos, currentDirection;
		public int volEnvPos, panEnvPos, pitchEnvPos;
		public boolean instrumentFinished, keyOff;
		public int keyOffCounter;
		
		public int effekt, effektParam;
		public int volumeEffekt, volumeEffektOp;
		
		public int channelVolumSlideValue;
		
		public boolean glissando;
		public int arpegioIndex, arpegioNote[];
		public int portaStepUp, portaStepUpEnd, portaStepDown, portaStepDownEnd;
		public int finePortaUp, finePortaDown, finePortaUpEx, finePortaDownEx; 
		public int portaNoteStep, portaTargetNotePeriod;
		public int volumSlideValue;
		public int panningSlideValue;
		public int vibratoTablePos, vibratoStep, vibratoAmplitude, vibratoType;
		public boolean vibratoOn, vibratoNoRetrig;
		public int autoVibratoTablePos, autoVibratoAmplitude;
		public int tremoloTablePos, tremoloStep, tremoloAmplitude, tremoloType;
		public boolean tremoloOn, tremoloNoRetrig;
		public int panbrelloTablePos, panbrelloStep, panbrelloAmplitude, panbrelloType;
		public boolean panbrelloOn, panbrelloNoRetrig;
		public int tremorCount, tremorOntime, tremorOfftime;
		public int retrigCount, retrigMemo, retrigVolSlide;
		public int sampleOffset, highSampleOffset;
		
		public int jumpLoopPatternRow, jumpLoopRepeatCount;
		public boolean jumpLoopPositionSet;

		public boolean doSurround;
		
		// Resonance Filter
		public boolean filterOn;
		public int nFilterMode;
		public int nResonance, nCutOff, nCutSwing, nResSwing;
		public long nFilter_A0, nFilter_B0, nFilter_B1, nFilter_HP;
		public long nFilter_Y1, nFilter_Y2;

		public boolean newInstrumentSet;
		int previousRealFrequency;
		int previousRealVolume;

		public ChannelMemory()
		{
			panning = 128; // 0-256, this is therefore center
			actRampVolLeft = 
			actRampVolRight =
			deltaVolLeft =
			deltaVolRight =
			currentVolume =
			currentSetVolume =
			channelVolume = 
			channelVolumSlideValue = 0;
			fadeOutVolume = 65536;
			
			muted = false;
			assignedNotePeriod = currentNotePeriod =    
			currentFinetuneFrequency = currentFineTune = 0;
			currentTuning = currentTuningPos = currentSamplePos = currentDirection = 0;
			instrumentFinished = true;
			keyOffCounter = -1;
			keyOff = false;
			volEnvPos = panEnvPos = pitchEnvPos = -1;
			
			arpegioIndex = noteDelayCount = noteCutCount = -1;
			arpegioNote = new int[3];
			portaStepUp = portaStepDown = portaStepUpEnd = portaStepDownEnd = 0; 
			finePortaDown = finePortaUp = 0; 
			finePortaDownEx = finePortaUpEx = 0; 
			portaNoteStep = portaTargetNotePeriod = volumSlideValue = 0;
			vibratoTablePos = vibratoStep = vibratoAmplitude = vibratoType = 0; 
			vibratoOn = vibratoNoRetrig = false;
			autoVibratoTablePos = autoVibratoAmplitude = 0;
			tremoloTablePos = tremoloStep = tremoloAmplitude = tremoloType = 0; 
			tremoloOn = tremoloNoRetrig = false;
			panbrelloTablePos = panbrelloStep = panbrelloAmplitude = panbrelloType = 0; 
			panbrelloOn = panbrelloNoRetrig = false;
			glissando=false;
			tremorCount = tremorOntime = tremorOfftime = 0;
			retrigCount = retrigMemo = retrigVolSlide = sampleOffset = highSampleOffset = 0;
			
			doSurround = false;
			
			filterOn = false;
			nFilterMode = 0;
			nResonance = nCutOff = nCutSwing = nResSwing = 0;
			nFilter_A0 = nFilter_B0 = nFilter_B1 = nFilter_HP = 0;
			nFilter_Y1 = nFilter_Y2 = 0; 

			jumpLoopPositionSet = false;
			patternJumpPatternIndex = jumpLoopPatternRow = jumpLoopRepeatCount = -1;

			newInstrumentSet = false;
			previousRealFrequency = 0;
			previousRealVolume = 0;
		}
		/**
		 * DeepCopy by Reflection - iterate through all fields and copy the values
		 * @since 29.03.2010
		 * @param fromMe
		 */
		public void deepCopy(ChannelMemory fromMe)
		{
			try
			{
				Field [] fields = ChannelMemory.class.getDeclaredFields();
				for (int i=0; i<fields.length; i++)
				{
					Field f = fields[i];
					if (Modifier.isFinal(f.getModifiers())) continue;
					f.set(this, f.get(fromMe));
				}
			}
			catch (Exception ex)
			{
				throw new RuntimeException(ex);
			}
		}
	}

	protected ChannelMemory [] channelMemory;
	protected int maxChannels;
	protected static final int MAX_NNA_CHANNELS = 200;
	
	protected int currentTempo, currentBPM;
	protected int globalTuning;
	protected int globalVolume, globalVolumSlideValue;
	protected boolean useFastSlides; // This is s3m specific and results from a bug in st3
	protected int frequencyTableType; // XM and IT Mods support this! Look at the constants
	protected int currentTick, currentRow, currentArrangement, currentPatternIndex;
	protected int samplePerTicks;
	protected int patternDelayCount, patternTicksDelayCount;
	protected Pattern currentPattern;
	protected int volRampLen;
	
	protected int patternBreakRowIndex; // -1== no pattern break, otherwise >=0
	protected int patternBreakJumpPatternIndex; // -1== no pattern pos jump
	protected int patternJumpPatternIndex;
	
	protected final Module mod;
	protected int sampleRate;
	protected int doISP; // 0: no ISP; 1:linear; 2:Cubic Spline; 3:Windowed FIR
	protected int doNoLoops; // aktivates infinit loop recognition
	
	protected boolean modFinished;
	
	// FadeOut
	protected boolean doFadeOut; // means we are in a loop condition and do a fadeout now. 0: deaktivated, 1: fade out, 2: just ignore loop
	protected int fadeOutValue;
	protected int fadeOutFac;
	protected int fadeOutSub;
	
	// RAMP volume interweaving
	protected int [] vRampL;
	protected int [] vRampR;
	protected int [] nvRampL;
	protected int [] nvRampR;

	public void setDebugData(PrintWriter debugData) {
		this.debugData = debugData;
	}
	public void setDebugSampleData(DataOutputStream debugSampleData) {
		this.debugSampleData = debugSampleData;
	}

	public void setDebugMusicData(DataOutputStream debugMusicData) {
		this.debugMusicData = debugMusicData;
	}

	PrintWriter debugData = null;
	DataOutputStream debugSampleData = null;
	DataOutputStream debugMusicData = null;

	public void setSampleRatio1(int sampleRatio1) {
		this.sampleRatio1 = sampleRatio1;
	}

	public void setSampleRatio2(int sampleRatio2) {
		this.sampleRatio2 = sampleRatio2;
	}

	int sampleRatio1 = 1, sampleRatio2 = 1;

	/**
	 * Constructor for BasicModMixer
	 */
	public BasicModMixer(Module mod, int sampleRate, int doISP, int doNoLoops)
	{
		super();
		this.mod = mod;
		this.sampleRate = sampleRate;
		this.doISP = doISP;
		this.doNoLoops = doNoLoops;

		this.maxChannels = mod.getNChannels();
		if (mod.getModType() == Helpers.MODTYPE_IT) maxChannels += MAX_NNA_CHANNELS;
		this.channelMemory = new ChannelMemory[maxChannels];

		this.vRampL = new int [Helpers.VOL_RAMP_LEN];
		this.vRampR = new int [Helpers.VOL_RAMP_LEN];
		this.nvRampL = new int [Helpers.VOL_RAMP_LEN];
		this.nvRampR = new int [Helpers.VOL_RAMP_LEN];
		
		initializeMixer();
	}
	/**
	 * BE SHURE TO STOP PLAYBACK! Changing this during playback may (will!)
	 * cause crappy playback!
	 * @since 09.07.2006
	 * @param newSampleRate
	 */
	public void changeSampleRate(int newSampleRate)
	{
		this.sampleRate = newSampleRate;
		this.samplePerTicks = calculateSamplesPerTick();
		calculateGlobalTuning();
		calculateVolRampLen();
		for (int c=0; c<maxChannels; c++)
		{
			final ChannelMemory aktMemo = channelMemory[c]; 
			setNewPlayerTuningFor(aktMemo);
		}
	}
	/**
	 * Changes the interpolation routine. This can be done at any time
	 * @since 09.07.2006
	 * @param newISP
	 */
	public void changeISP(int newISP)
	{
		this.doISP = newISP;
	}
	/**
	 * Changes the interpolation routine. This can be done at any time
	 * @since 09.07.2006
	 * @param newDoNoLoops
	 */
	public void changeDoNoLoops(int newDoNoLoops)
	{
		this.doNoLoops = newDoNoLoops;
	}
	/**
	 * Do own inits
	 * Espezially do the init of the panning depending
	 * on ModType
	 * @return
	 */
	protected abstract void initializeMixer(int channel, ChannelMemory aktMemo);
	
	/**
	 * Call this first!
	 */
	public void initializeMixer()
	{
		// to be a bit faster, we do some precalculations
		calculateGlobalTuning();
		
		// get Mod specific values
		frequencyTableType = mod.getFrequencyTable();
		currentTempo = mod.getTempo();
		currentBPM = mod.getBPMSpeed();
		globalVolume = mod.getBaseVolume();
		globalVolumSlideValue = 0;
		useFastSlides = mod.doFastSlides();
		
		samplePerTicks = calculateSamplesPerTick();
		
		currentTick = currentArrangement = currentRow = patternDelayCount = patternTicksDelayCount = 0;
		currentArrangement = 0;
		currentPatternIndex = mod.getArrangement()[currentArrangement];
		currentPattern = mod.getPatternContainer().getPattern(currentPatternIndex);
		
		patternJumpPatternIndex = patternBreakRowIndex = patternBreakJumpPatternIndex = -1;
		
		modFinished = false;
		
		calculateVolRampLen();
		
		// Reset all rowes played to false
		mod.resetLoopRecognition();

		// Reset FadeOut
		doFadeOut = false;
		fadeOutFac = 8;
		fadeOutValue = 1<<fadeOutFac;
		fadeOutSub = 0x01;
		
		final int nChannels = mod.getNChannels();
		// Now initialize every used channel
		for (int c=0; c<maxChannels; c++)
		{
			channelMemory[c] = new ChannelMemory();
			
			if (c<nChannels)
			{
				final ChannelMemory aktMemo = channelMemory[c];
				aktMemo.panning = mod.getPanningValue(c);
				aktMemo.channelVolume = mod.getChannelVolume(c); 
				initializeMixer(c, aktMemo);
			}
		}
	}
	/**
	 * Normally you would use the formula (25*samplerate)/(bpm*10)
	 * which is (2.5*sampleRate)/bpm. But (2.5 * sampleRate) is the same
	 * as (sampleRate*2) + (sampleRate/2)
	 * @return
	 */
	protected int calculateSamplesPerTick()
	{
		// return (25*sampleRate)/(currentBPM*10); // old fashioned
		return ((sampleRate<<1) + (sampleRate>>1)) / currentBPM;
	}
	/**
	 * For faster tuning calculations, this is precalced 
	 */
	private void calculateGlobalTuning()
	{
		this.globalTuning = (int)((((((long)Helpers.BASEPERIOD)<<4) * ((long)Helpers.BASEFREQUENCY))<<Helpers.SHIFT) / ((long)sampleRate));
	}
	/**
	 * The size of Volumeramping we intend to use
	 */
	private void calculateVolRampLen()
	{
		volRampLen = sampleRate * Helpers.VOLRAMPLEN_MS / 100000;
		if (volRampLen < 8) volRampLen = 8;
	}
	/**
	 * Retrievs a Periodvalue (see Helpers.noteValues) shifted by 4 (*16)
	 * XM_LINEAR_TABLE and XM_AMIGA_TABLE is for XM-Mods,
	 * AMIGA_TABLE is for ProTrackerMods only (XM_AMIGA_TABLE is about the same though)
	 * With Mods the AMIGA_TABLE, IT_AMIGA_TABLE and XM_AMIGA_TABLE result in 
	 * the approximate same values, but to be purly compatible and correct,
	 * we use the protracker fintune period tables!
	 * The IT_AMIGA_TABLE is for STM and S3M and IT...
	 * Be careful: if XM_* is used, we expect a noteIndex (0..119), no period!
	 * @param aktMemo
	 * @param period or noteIndex
	 * @return
	 */
	protected int getFineTunePeriod(final ChannelMemory aktMemo, final int period)
	{
		int noteIndex = period - 1; // Period was both, a mod period or xm/it noteIndex - this changed (look at default!)
		switch (frequencyTableType)
		{
			case Helpers.STM_S3M_TABLE:
			case Helpers.IT_LINEAR_TABLE:
				int s3mNote=Helpers.FreqS3MTable[noteIndex%12];
				int s3mOctave=noteIndex/12;
				if (aktMemo.currentFinetuneFrequency <= 0) aktMemo.currentFinetuneFrequency = Helpers.BASEFREQUENCY;
				return (int) ((long) Helpers.BASEFREQUENCY * ((long)s3mNote << 7) / ((long) aktMemo.currentFinetuneFrequency << (s3mOctave)));

			case Helpers.AMIGA_TABLE:
				return Helpers.protracker_fineTunedPeriods[(aktMemo.currentFineTune>>4)+8][period-25]; // We have less Octaves!
			
			case Helpers.IT_AMIGA_TABLE:
				noteIndex -= 12;
				if (noteIndex < 0) noteIndex = 0;
			case Helpers.XM_AMIGA_TABLE:
				int fineTune=aktMemo.currentFineTune;
				int rFine=fineTune>>4;

				final int note=((noteIndex%12)<<3)+8; // !negativ finetune values! -8..+7 Therefore add 8
				final int octave=noteIndex/12;
				
				int logIndex = note + rFine; 
				if (logIndex<0) logIndex=0; else if (logIndex>103) logIndex=103;
				int v1=Helpers.logtab[logIndex];
				if (fineTune<0)
				{
					rFine--;
					fineTune = -fineTune;
				} 
				else 
					rFine++;
				
				logIndex = note + rFine;
				if (logIndex<0) logIndex=0; else if (logIndex>103) logIndex=103;
				int v2=Helpers.logtab[logIndex];
				rFine = fineTune & 0x0F;
				return ((v1*(16-rFine)) + (v2*rFine)) >> (octave+4);

// 				MikMod does it like this:
//				int noteIndex = period-1;
//				int note=noteIndex%12;
//				int octave=noteIndex/12;
//				
//				int fineTune=aktMemo.currentFineTune>>4;
//				int logIndex = (note<<3)+fineTune+8; // !negativ finetune values! -8..+7
//				int v1=Helpers.logtab[logIndex];
//				if (fineTune==0) return v1>>octave;
//				int v2=Helpers.logtab[logIndex+1];
//				return (v1+((fineTune*(v2-v1))/15))>>octave;

			case Helpers.XM_LINEAR_TABLE:
				int p = 7680 - (noteIndex<<6) - (aktMemo.currentFineTune>>1);
				if (p<1) return (1<<2);
				return p<<2;

			default: // Period is not a noteindex - this will never happen, but I once used it with protracker mods
				return (int)((long)Helpers.BASEFREQUENCY*(long)period/(long)aktMemo.currentFinetuneFrequency);
		}
	}
	/**
	 * Calls getFineTunePeriod(ChannelMemory, int Period) with the aktual Period assigend.
	 * All Effekts changing the period need to call this
	 * @param aktMemo
	 * @return
	 */
	protected int getFineTunePeriod(final ChannelMemory aktMemo)
	{
		if (frequencyTableType>=Helpers.AMIGA_TABLE)
			return (aktMemo.assignedNoteIndex==0)?0:getFineTunePeriod(aktMemo, aktMemo.assignedNoteIndex + aktMemo.currentTranspose);
		else
		if (frequencyTableType==Helpers.IT_LINEAR_TABLE || frequencyTableType==Helpers.STM_S3M_TABLE)
			return (aktMemo.assignedNoteIndex==0)?0:getFineTunePeriod(aktMemo, aktMemo.assignedNoteIndex);
		else
			return (aktMemo.assignedNotePeriod==0)?0:getFineTunePeriod(aktMemo, aktMemo.assignedNotePeriod<<4);
	}
	/**
	 * This Method now takes the current Period (e.g. 856<<4) and calculates the
	 * playerTuning to be used. I.e. a value like 2, which means every second sample in the
	 * current instrument is to be played. A value of 0.5 means, every sample is played twice.
	 * As we use int-values, this again is shiftet. 
	 * @param aktMemo
	 * @param newPeriod
	 */
	protected void setNewPlayerTuningFor(final ChannelMemory aktMemo, final int newPeriod)
	{
		if (newPeriod<=0) {
			aktMemo.currentTuning = 0;
		} else if (frequencyTableType==Helpers.XM_LINEAR_TABLE) {
			final int xm_period_value = newPeriod>>2;
			int newFrequency = Helpers.lintab[xm_period_value % 768] >> (xm_period_value / 768);
			aktMemo.currentTuning = (int)(((long)newFrequency<<Helpers.SHIFT) / sampleRate);
		} else {
			aktMemo.currentTuning = globalTuning / newPeriod; // in globalTuning, all constant values are already calculated. (see above)
		}
	}
	/**
	 * Set the current tuning for the player
	 * @param aktMemo
	 */
	protected void setNewPlayerTuningFor(final ChannelMemory aktMemo)
	{
		setNewPlayerTuningFor(aktMemo, aktMemo.currentNotePeriod);
	}
	/**
	 * calc the cut off
	 * @param cutOff
	 * @param flt_modifier
	 * @param isExFilterRange
	 * @return
	 */
	protected long cutOffToFrequency(long cutOff, int flt_modifier, boolean isExFilterRange)
	{
		double fac = (isExFilterRange)?(20.0d*512.0d):(24.0d*512.0d);
		double fc = 110.0d * Math.pow(2.0d, 0.25d + ((double)(cutOff*(flt_modifier+256))) / fac);
		long freq = (long)fc;
		if (freq<120) return 120;
		if (freq>20000) return 20000;
		if ((freq<<1) > sampleRate) return ((long)sampleRate)>>1;
		return freq;
	}
	/**
	 * Simple 2-poles resonant filter 
	 * @since 31.03.2010
	 * @param aktMemo
	 * @param bReset
	 * @param flt_modifier
	 */
	protected void setupChannelFilter(ChannelMemory aktMemo, boolean bReset, int flt_modifier)
	{
		double cutOff = (double)((aktMemo.nCutOff + aktMemo.nCutSwing)&0x7F);
		double resonance = (double)((aktMemo.nResonance + aktMemo.nResSwing)&0x7F);
		
		double fc = cutOffToFrequency((long)cutOff, flt_modifier, (mod.getSongFlags()&Helpers.SONG_EXFILTERRANGE)!=0);
		fc *= 2.0d * 3.14159265358 / ((double)sampleRate);

		double dmpfac = Math.pow(10.0d, -((24.0d / 128.0d) * resonance) / 20.0d);
		
		double d = (1.0d - 2.0d * dmpfac) * fc;
		if (d > 2.0d) d = 2.0d;
		d = (2.0d * dmpfac - d) / fc;
		double e = Math.pow(1.0d / fc, 2.0d);

		double fg  = 1.0d / (1.0d + d + e);
		double fg1 = -e * fg;
		double fg0 = 1.0d - fg - fg1;
			
		switch(aktMemo.nFilterMode)
		{
			case Helpers.FLTMODE_HIGHPASS:
				aktMemo.nFilter_A0 = (long)((1.0d-fg) * Helpers.FILTER_PRECISION);
				aktMemo.nFilter_B0 = (long)(fg0 * Helpers.FILTER_PRECISION);
				aktMemo.nFilter_B1 = (long)(fg1 * Helpers.FILTER_PRECISION);
				aktMemo.nFilter_HP = -1;
				break;
			default:
				aktMemo.nFilter_A0 = (long)(fg * Helpers.FILTER_PRECISION);
				aktMemo.nFilter_B0 = (long)(fg0 * Helpers.FILTER_PRECISION);
				aktMemo.nFilter_B1 = (long)(fg1 * Helpers.FILTER_PRECISION);
				aktMemo.nFilter_HP = 0;
				break;
		}
		
		if (bReset) aktMemo.nFilter_Y1 = aktMemo.nFilter_Y2 = 0;
		
		aktMemo.filterOn = true;
	}
	/**
	 * @since 31.03.2010
	 * @param actMemo
	 * @param s
	 * @return
	 */
	private long doResonance(final ChannelMemory actMemo, final long s)
	{
		long fy = ((s * actMemo.nFilter_A0) + (actMemo.nFilter_Y1 * actMemo.nFilter_B0) + (actMemo.nFilter_Y2 * actMemo.nFilter_B1) + 4096L) >> Helpers.FILTER_SHIFT_BITS;

		actMemo.nFilter_Y2 = actMemo.nFilter_Y1;
		actMemo.nFilter_Y1 = fy - (s & actMemo.nFilter_HP);
		
		return fy;
	}
	
	/**
	 * Do the effects of a row. This is mostly the setting of effekts
	 * @param aktMemo
	 */
	protected abstract void doRowEffects(ChannelMemory aktMemo);
	/**
	 * Do the Effekts during Ticks
	 * @param aktMemo
	 */
	protected abstract void doTickEffekts(ChannelMemory aktMemo);
	/**
	 * Used to precess the volume column
	 * @param aktMemo
	 */
	protected abstract void doVolumeColumnRowEffekt(ChannelMemory aktMemo);
	/**
	 * do the volume column tick effekts 
	 * @param aktMemo
	 */
	protected abstract void doVolumeColumnTickEffekt(ChannelMemory aktMemo);
	/**
	 * Clear all effekts. Sometimes, if Effekts do continue, they are not stopped.
	 * @param aktMemo
	 * @param nextElement
	 */
	protected abstract void resetAllEffects(ChannelMemory aktMemo, PatternElement nextElement, boolean forced);
	/**
	 * Returns true, if the Effekt and EffektOp indicate a NoteDelayEffekt
	 * @param aktMemo
	 * @param aktMemo
	 * @return
	 */
	protected abstract boolean isNoteDelayEffekt(final ChannelMemory aktMemo);
	/**
	 * Returns true, if the Effekt and EffektOp indicate a PortaToNoteEffekt
	 * @param aktMemo
	 * @param aktMemo
	 * @return
	 */
	protected abstract boolean isPortaToNoteEffekt(final ChannelMemory aktMemo);
	/**
	 * Return true, if the effekt and effektop indicate the sample offset effekt
	 * @since 19.06.2006
	 * @param aktMemo
	 * @param aktMemo
	 * @return
	 */
	protected abstract boolean isSampleOffsetEffekt(final ChannelMemory aktMemo);
	/**
	 * Processes the Envelopes
	 * This function now sets the volume - alwayes!!
	 * @since 19.06.2006
	 * @param aktMemo
	 */
	protected void processEnvelopes(ChannelMemory aktMemo)
	{
		int currentVolume = aktMemo.currentVolume << Helpers.VOLUMESHIFT;
		int currentPanning = aktMemo.panning; 

		Instrument currentInstrument = aktMemo.assignedInstrument;
		if (currentInstrument!=null)
		{
			Envelope volumeEnv  = currentInstrument.volumeEnvelope;
			if (volumeEnv!=null && volumeEnv.on)
			{
				aktMemo.volEnvPos = volumeEnv.updatePosition(aktMemo.volEnvPos, aktMemo.keyOff);
				int newVol = volumeEnv.getValueForPosition(aktMemo.volEnvPos);
				currentVolume = (currentVolume * newVol) >> 9;
			}
			Envelope panningEnv = currentInstrument.panningEnvelope;
			if (panningEnv!=null && panningEnv.on)
			{
				aktMemo.panEnvPos = panningEnv.updatePosition(aktMemo.panEnvPos, aktMemo.keyOff);
				currentPanning += panningEnv.getValueForPosition(aktMemo.panEnvPos)-256;
			}

			if (aktMemo.keyOff)
			{
				if (volumeEnv!=null && volumeEnv.on)
				{
					aktMemo.fadeOutVolume -= currentInstrument.volumeFadeOut<<1;
					if (aktMemo.fadeOutVolume<0) aktMemo.fadeOutVolume = 0;
				}
				else
					aktMemo.fadeOutVolume = 0;
				currentVolume = (currentVolume * aktMemo.fadeOutVolume) >> 16; // max: 65536
			}
			
			Envelope pitchEnv = currentInstrument.pitchEnvelope;
			if (pitchEnv!=null && pitchEnv.on)
			{
				aktMemo.pitchEnvPos = pitchEnv.updatePosition(aktMemo.pitchEnvPos, aktMemo.keyOff);
				int pitchValue = pitchEnv.getValueForPosition(aktMemo.pitchEnvPos) - 256;
				if (pitchEnv.filter)
					setupChannelFilter(aktMemo, !aktMemo.filterOn, pitchValue);
				else
				{
					long a = aktMemo.currentNotePeriod;
					long b = 0;
					if (pitchValue < 0)
					{
						pitchValue = -pitchValue;
						if (pitchValue > 255) pitchValue = 255;
						b = Helpers.LinearSlideUpTable[pitchValue];
					} 
					else
					{
						if (pitchValue > 255) pitchValue = 255;
						b = Helpers.LinearSlideDownTable[pitchValue];
					}
					setNewPlayerTuningFor(aktMemo, (int)((a*b)>>16));
				}
			}
		}
		// do Panbrello
		if (aktMemo.panbrelloOn)
		{
			final int panningPos = (aktMemo.panbrelloTablePos>>2) & 0x3F; // MPTrack starts +0x10 (high amplitude)
			int newPanning;
			switch (aktMemo.panbrelloType & 0x03)
			{
				case 1: newPanning = (Helpers.ModRampDownTable[panningPos]);	// Sawtooth
						break;
				case 2: newPanning = (Helpers.ModSquareTable  [panningPos]);	// Squarewave
						break;
				case 3:	newPanning = (Helpers.ModRandomTable  [panningPos]);	// Random.
						break;
				default:newPanning = (Helpers.ModSinusTable   [panningPos]);	// Sinus
						break;
			}

			aktMemo.panbrelloTablePos += aktMemo.panbrelloStep;
			newPanning = ((newPanning * aktMemo.panbrelloAmplitude) + 2) >> 3;
			newPanning += currentPanning;
			currentPanning = (newPanning<0)?0:((newPanning>255)?255:newPanning);
		}
		
		currentVolume = (currentVolume*aktMemo.channelVolume)>>6;		// max: 64
		// Global Volumes
		currentVolume = (currentVolume * globalVolume) >> 7;			// max: 128
		currentVolume = (currentVolume * fadeOutValue) >> fadeOutFac;	// max: 255
		
		if (currentVolume<0) currentVolume=0;
		else
		if (currentVolume>Helpers.MAXVOLUME) currentVolume=Helpers.MAXVOLUME;

		if (currentPanning<0) currentPanning=0;
		else
		if (currentPanning>256) currentPanning=256;
		
		aktMemo.actRampVolLeft = aktMemo.actVolumeLeft;
		aktMemo.actRampVolRight = aktMemo.actVolumeRight;
		
		aktMemo.actVolumeLeft  = currentVolume*((256-currentPanning)<<Helpers.VOLUMESHIFT_FULL); 
		aktMemo.actVolumeRight = currentVolume*((    currentPanning)<<Helpers.VOLUMESHIFT_FULL);
		
		if (aktMemo.doSurround) aktMemo.actVolumeLeft = -aktMemo.actVolumeLeft; 
		
		if (aktMemo.actVolumeLeft != aktMemo.actRampVolLeft)
		{
			aktMemo.deltaVolLeft = aktMemo.actVolumeLeft - aktMemo.actRampVolLeft;
			if (aktMemo.deltaVolLeft > volRampLen) 
				aktMemo.deltaVolLeft /= volRampLen; 
			else
			if (aktMemo.deltaVolLeft!=0)
				aktMemo.deltaVolLeft /= aktMemo.deltaVolLeft;
		}
		else
			aktMemo.deltaVolLeft = 0;
		if (aktMemo.actVolumeRight != aktMemo.actRampVolRight)
		{
			aktMemo.deltaVolRight = aktMemo.actVolumeRight - aktMemo.actRampVolRight;
			if (aktMemo.deltaVolRight > volRampLen) 
				aktMemo.deltaVolRight /= volRampLen; 
			else 
			if (aktMemo.deltaVolRight!=0)
				aktMemo.deltaVolRight /= aktMemo.deltaVolRight;
		}
		else
			aktMemo.deltaVolRight = 0;

		// AutoVibrato
		Sample currentSample = aktMemo.currentSample;
		if (currentSample!=null && currentSample.vibratoDepth>0 && aktMemo.currentNotePeriod>0)
		{
			if (currentSample.vibratoSweep == 0)
				aktMemo.autoVibratoAmplitude = currentSample.vibratoDepth << 8;
			else
			{
				if (!aktMemo.keyOff)
				{
					aktMemo.autoVibratoAmplitude += (currentSample.vibratoDepth << 8) / currentSample.vibratoSweep;
					if ((aktMemo.autoVibratoAmplitude >> 8) > currentSample.vibratoDepth)
						aktMemo.autoVibratoAmplitude = currentSample.vibratoDepth << 8;
				}
			}
			
			aktMemo.autoVibratoTablePos += currentSample.vibratoRate;
			int periodAdd;
			switch (currentSample.vibratoType & 0x07)
			{
				default:
				case 0:	periodAdd = Helpers.ft2VibratoTable[aktMemo.autoVibratoTablePos & 0xFF];	// Sine
						break;
				case 1:	periodAdd = ((aktMemo.autoVibratoTablePos & 0x80)==0x80) ? +64 : -64;			// Square
						break;
				case 2:	periodAdd = ((0x40 + (aktMemo.autoVibratoTablePos >> 1)) & 0x7f) - 0x40;	// Ramp Up
						break;
				case 3:	periodAdd = ((0x40 - (aktMemo.autoVibratoTablePos >> 1)) & 0x7F) - 0x40;	// Ramp Down
						break;
				case 4:	periodAdd = (Helpers.ModRandomTable[aktMemo.autoVibratoTablePos & 0x3F]);	// Random.
						break;
			}
			periodAdd =	(periodAdd * aktMemo.autoVibratoAmplitude) >> 8;
			setNewPlayerTuningFor(aktMemo, aktMemo.currentNotePeriod + (periodAdd>>4));
		}
	}
	/**
	 * Set all index values back to zero!
	 * @since 19.06.2006
	 * @param aktMemo
	 */
	protected void resetInstrument(ChannelMemory aktMemo)
	{
		aktMemo.autoVibratoTablePos =
		aktMemo.autoVibratoAmplitude =
		aktMemo.currentDirection = 
		aktMemo.currentTuningPos = 
		aktMemo.currentSamplePos = 0;
		aktMemo.volEnvPos = 
		aktMemo.panEnvPos =
		aktMemo.pitchEnvPos = -1; 
		aktMemo.filterOn = 
		aktMemo.instrumentFinished = false;
	}
	public int getCurrentPatternPosition()
	{
		return ((currentArrangement&0xFF)<<24) | ((currentPatternIndex&0xFF)<<16) | ((currentRow&0xFF)<<8) | ((currentTempo - currentTick)&0xFF);
	}
	/**
	 * @since 30.03.2010
	 * @return
	 */
	public int getCurrentUsedChannels()
	{
		int result = 0;
		for (int i=0; i<maxChannels; i++)
		{
			if (isChannelActive(channelMemory[i])) result++;
		}
		return result;
	}
	/**
	 * @since 30.03.2010
	 * @param actMemo
	 * @return
	 */
	private boolean isChannelActive(ChannelMemory actMemo)
	{
		return (actMemo.currentSample!=null && actMemo.currentSample.sample!=null &&
				actMemo.currentTuning!=0 && !actMemo.instrumentFinished);
	}
//	/**
//	 * @since 29.03.2010
//	 * @param aktMemo
//	 * @return
//	 */
//	protected ChannelMemory getNNAChannelFor(ChannelMemory aktMemo)
//	{
//		int lowVol = Helpers.MAXVOLUME;
//		ChannelMemory result = null;
//		// Pick a Channel with lowest volume or silence
//		for (int c=mod.getNChannels(); c<maxChannels; c++)
//		{
//			final ChannelMemory memo = channelMemory[c];
//			if (!isChannelActive(memo)) return memo;
//			
//			final int currentVolume = memo.currentVolume;
//			if (currentVolume==0) return memo;
//			if (currentVolume<lowVol)
//			{
//				lowVol = currentVolume;
//				result = memo;
//			}
//		}
//		return result;
//	}
//	/**
//	 * @since 29.03.2010
//	 * @param aktMemo
//	 */
//	protected void checkForNNA(ChannelMemory aktMemo)
//	{
//		if (mod.getModType() != Helpers.MODTYPE_IT || !isChannelActive(aktMemo)) return;
//		Instrument currentInstrument = aktMemo.assignedInstrument;
//		if (currentInstrument!=null)
//		{
//			ChannelMemory newChannel = getNNAChannelFor(aktMemo);
//			if (newChannel!=null)
//			{
//				newChannel.deepCopy(aktMemo);
//				switch (currentInstrument.NNA)
//				{
//					case Helpers.NNA_CUT: newChannel.currentVolume = setNewVolume(newChannel, newChannel.panning, 0); break;
//					case Helpers.NNA_CONTINUE: break;
//					case Helpers.NNA_FADE: 
//					case Helpers.NNA_OFF: newChannel.keyOff = true; break;
//				}
//			}
//		}
//	}
	protected void setNewInstrumentAndPeriod(final ChannelMemory aktMemo)
	{
		final PatternElement element = aktMemo.currentElement;
//		if ((element.getPeriod()>0 || element.getNoteIndex()>0) && !isPorta) // New Note Effekt
//		{
//			checkForNNA(aktMemo);
//		}
		Sample newSample = null;
		// If we have an instrument setting here, set it's volume and panning values
		if (element.getInstrument()>0)
		{
			// Get the correct sample from the mapping table
			if (aktMemo.assignedInstrument!=null)
			{
				final Instrument inst = aktMemo.assignedInstrument; 
				newSample = mod.getInstrumentContainer().getSample(inst.getSampleIndex(aktMemo.assignedNoteIndex-1));
				aktMemo.assignedNoteIndex = inst.getNoteIndex(aktMemo.assignedNoteIndex-1)+1;
				if ((inst.initialFilterCutoff & 0x80)!=0) aktMemo.nCutOff = inst.initialFilterCutoff & 0x7F;
				if ((inst.initialFilterResonance & 0x80)!=0) aktMemo.nResonance = inst.initialFilterResonance & 0x7F;
			}
			else
				newSample = mod.getInstrumentContainer().getSample(aktMemo.assignedInstrumentIndex-1);

			if (newSample!=null)
			{
				aktMemo.newInstrumentSet = true;

				// If this sample uses panning, set the panning
				int pan = newSample.panning;
				if (pan!=-1)
				{
					aktMemo.doSurround = false;
					aktMemo.panning = pan;
				}
				aktMemo.fadeOutVolume = 65536;
				aktMemo.currentSetVolume = aktMemo.currentVolume = newSample.volume;
			}
		}
		// if there is a note, we need to calc the new tuning and activate a previous set instrument
		if (element.getPeriod()>0 || element.getNoteIndex()>0)
		{
			resetAllEffects(aktMemo, element, true); // Reset Tremolo and such things...
			// Key Off is off per definition
			aktMemo.keyOff = false;
			// We have an instrument assignment, so there was (once) an instrument set!
			if (aktMemo.assignedInstrument!=null || aktMemo.assignedInstrumentIndex>0) 
			{
				// Get the correct sample from the mapping table (if not yet set (speedup))
				if (newSample==null)
				{
					if (aktMemo.assignedInstrument!=null)
					{
						final Instrument inst = aktMemo.assignedInstrument; 
						newSample = mod.getInstrumentContainer().getSample(inst.getSampleIndex(aktMemo.assignedNoteIndex-1));
						aktMemo.assignedNoteIndex = inst.getNoteIndex(aktMemo.assignedNoteIndex-1)+1;
						if ((inst.initialFilterCutoff & 0x80)!=0) aktMemo.nCutOff = inst.initialFilterCutoff & 0x7F;
						if ((inst.initialFilterResonance & 0x80)!=0) aktMemo.nResonance = inst.initialFilterResonance & 0x7F;
					}
					else
						newSample = mod.getInstrumentContainer().getSample(aktMemo.assignedInstrumentIndex-1);
				}
				
				// and reset all pointers, if it's a new one...
				if (aktMemo.currentSample!=newSample)
				{
					// Now activate new Instrument...
					aktMemo.currentSample = newSample; 
					if (newSample!=null)
					{
						resetInstrument(aktMemo);
						aktMemo.currentFinetuneFrequency = newSample.baseFrequency;
						aktMemo.currentFineTune = newSample.fineTune;
						aktMemo.currentTranspose = newSample.transpose;
					}
				}
			}
			// Set the note, if this is not a porta to note effekt
			if (aktMemo.currentSample!=null && !isPortaToNoteEffekt(aktMemo))
			{
				resetInstrument(aktMemo);
				setNewPlayerTuningFor(aktMemo, aktMemo.portaTargetNotePeriod = aktMemo.currentNotePeriod = getFineTunePeriod(aktMemo));

				final Instrument inst = aktMemo.assignedInstrument;
				if (inst!=null)
				{
					boolean makeFilter = false;
					if ((inst.initialFilterCutoff & 0x80)!=0) { aktMemo.nCutOff = inst.initialFilterCutoff & 0x7F; makeFilter = true; }
					if ((inst.initialFilterResonance & 0x80)!=0) { aktMemo.nResonance = inst.initialFilterResonance & 0x7F; makeFilter = true; }
					if (aktMemo.nCutOff<0x7F && makeFilter) setupChannelFilter(aktMemo, true, 256);
				}
			}
		}
	}
	/**
	 * @since 18.09.2010
	 * @param aktMemo
	 */
	protected void processEffekts(boolean inTick, final ChannelMemory aktMemo)
	{
		if (inTick)
		{
			doVolumeColumnTickEffekt(aktMemo);
			doTickEffekts(aktMemo);
		}
		else
		{
			doVolumeColumnRowEffekt(aktMemo);
			doRowEffects(aktMemo);
		}
		processEnvelopes(aktMemo);
	}
	protected boolean isInfinitLoop(final int currentArrangement, final PatternRow patternRow)
	{
		return (mod.isArrangementPositionPlayed(currentArrangement) && patternRow.isRowPlayed());
	}
	protected boolean isInfinitLoop(final int currentArrangement, final int currentRow)
	{
		return isInfinitLoop(currentArrangement, currentPattern.getPatternRow(currentRow));
	}
	/**
	 * Do the Events of a new Row!
	 * @return true, if finished! 
	 */
	protected void doRowEvent()
	{
		final PatternRow patternRow = currentPattern.getPatternRow(currentRow);
		if (patternRow==null) return;
		
		patternRow.setRowPlayed();
		final int nChannels = mod.getNChannels();
		for (int c=0; c<nChannels; c++)
		{
			// get pattern and channel memory data for current channel
			final PatternElement element = patternRow.getPatternElement(c);
			final ChannelMemory aktMemo = channelMemory[c];
			
			// reset all effects on this channel
			resetAllEffects(aktMemo, element, false);
			
			// Now copy the pattern data but remain old values for note and instrument
			aktMemo.currentElement = element;
			if (element.getPeriod()>0) aktMemo.assignedNotePeriod = element.getPeriod();
			if (element.getNoteIndex()>0) aktMemo.assignedNoteIndex = element.getNoteIndex();
			if (element.getInstrument()>0)
			{
				aktMemo.assignedInstrumentIndex = element.getInstrument();
				aktMemo.assignedInstrument = mod.getInstrumentContainer().getInstrument(element.getInstrument()-1);
			}
			aktMemo.effekt = element.getEffekt(); 
			aktMemo.effektParam = element.getEffektOp();
			aktMemo.volumeEffekt = element.getVolumeEffekt();
			aktMemo.volumeEffektOp = element.getVolumeEffektOp();
			
			// Key Off?
			if (element.getPeriod()==Helpers.KEY_OFF || element.getNoteIndex()==Helpers.KEY_OFF)
			{
				aktMemo.keyOff = true;
			}
			else
			if (element.getPeriod()==Helpers.NOTE_CUT || element.getNoteIndex()==Helpers.NOTE_CUT)
			{
				aktMemo.fadeOutVolume = 0;
			}
			else
			if (!isNoteDelayEffekt(aktMemo)) // If this is a noteDelay, we lose for now, this is all done later!
			{
				setNewInstrumentAndPeriod(aktMemo);
			}
			
			processEffekts(false, aktMemo);
		}
	}
	/**
	 * when stepping to a new Pattern - Position needs new set...
	 * @since 21.01.2014
	 */
	private void resetJumpPositionSet()
	{
		for (int c=0; c<maxChannels; c++)
		{
			channelMemory[c].jumpLoopPositionSet = false;
		}
	}
	public static int currentFrame = 0;
	public static int lastFrameOutput = 0;
	/**
	 * Do the events during a Tick.
	 * @return true, if finished! 
	 */
	protected boolean doTickEvents()
	{
		if (debugData != null) {
			debugData.println("Frame: " + currentFrame);
			debugData.flush();
			if (currentFrame > (50 * 60 * 10)) {
				debugData.println("Aborted file export due to time limit");
				debugData.flush();
				System.out.println("Very long music detected, aborting");
//				System.exit(-1);
				return true;
			}
		}
			// Global Fade Out
		if (doFadeOut)
		{
			fadeOutValue-=fadeOutSub;
			if (fadeOutValue <= 0) return true; // We did a fadeout and are finished now
		}
		final int nChannels = maxChannels;
		if (patternTicksDelayCount>0)
		{
			for (int c=0; c<nChannels; c++)
			{
				final ChannelMemory aktMemo = channelMemory[c];
				processEffekts(true, aktMemo);
			}
			patternTicksDelayCount--; 
		}
		else
		{
			currentTick--;
			if (currentTick<=0)
			{
				currentTick = currentTempo;
	
				// if PatternDelay, do it and return
				if (patternDelayCount>0)
				{
					for (int c=0; c<nChannels; c++)
					{
						final ChannelMemory aktMemo = channelMemory[c];
						processEffekts(false, aktMemo);
					}
					patternDelayCount--;
				}
				else
				if (currentArrangement>=mod.getSongLength())
				{
					return true; // Ticks finished and no new row --> FINITO!
				}
				else	
				{
					// Do the row events
					doRowEvent();
					// and step to the next row... Even if there are no more -  we will find out later!
					currentRow++;
					if (patternJumpPatternIndex!=-1) // Do not check infinit Loops here, this is never infinit
					{
						currentRow = patternJumpPatternIndex;
						patternJumpPatternIndex = -1;
					}
					if (currentRow>=currentPattern.getRowCount() || 
						patternBreakRowIndex!=-1 || patternBreakJumpPatternIndex!=-1)
					{
						mod.setArrangementPositionPlayed(currentArrangement);
						if (patternBreakJumpPatternIndex!=-1)
						{
							final int checkRow = (patternBreakRowIndex!=-1)?patternBreakRowIndex:currentRow-1;
							final boolean infinitLoop = isInfinitLoop(patternBreakJumpPatternIndex, checkRow);
							if (infinitLoop && doNoLoops==Helpers.PLAYER_LOOP_IGNORE)
							{
								patternBreakRowIndex = patternBreakJumpPatternIndex = -1;
								resetJumpPositionSet();
								currentArrangement++;
							}
							else
							{
								currentArrangement = patternBreakJumpPatternIndex;
							}
							patternBreakJumpPatternIndex = -1;
							// and activate fadeout, if wished
							if (infinitLoop && doNoLoops == Helpers.PLAYER_LOOP_FADEOUT)
								doFadeOut = true;
						}
						else
						{
							resetJumpPositionSet();
							currentArrangement++;
						}
						
						if (patternBreakRowIndex!=-1)
						{
							currentRow = patternBreakRowIndex;
							patternBreakRowIndex = -1;
						}
						else
							currentRow = 0;
						// End of song? Fetch new pattern if not...
						if (currentArrangement<mod.getSongLength())
						{
							currentPatternIndex = mod.getArrangement()[currentArrangement];
							currentPattern = mod.getPatternContainer().getPattern(currentPatternIndex);
						}
						else
						{
							currentPatternIndex = -1;
							currentPattern = null;
						}
					}
				}
			}
			else
			{
				// Do all Tickevents, 'cause we are in a Tick...
				for (int c=0; c<nChannels; c++)
				{
					final ChannelMemory aktMemo = channelMemory[c];
					processEffekts(true, aktMemo);
				}
			}
		}
		return false;
	}
	/**
	 * Add current speed to samplepos and
	 * fit currentSamplePos into loop values
	 * or signal Sample finished
	 * @since 18.06.2006
	 * @param actMemo
	 */
	protected void fitIntoLoops(final ChannelMemory actMemo)
	{
		final Sample ins = actMemo.currentSample;
		
		// If Forward direction:
		if (actMemo.currentDirection>=0)
		{
			actMemo.currentTuningPos += actMemo.currentTuning;
			if (actMemo.currentTuningPos >= Helpers.SHIFT_ONE)
			{
				actMemo.currentSamplePos += (actMemo.currentTuningPos >> Helpers.SHIFT);
				actMemo.currentTuningPos &= Helpers.SHIFT_MASK;
				if ((ins.loopType & Helpers.LOOP_ON)==0) // NoLoop
				{
					actMemo.instrumentFinished = actMemo.currentSamplePos>=ins.length;
				}
				else // loop is On
				{
					if ((ins.loopType & Helpers.LOOP_IS_PINGPONG)==0) // No Ping Pong
					{
						if (actMemo.currentSamplePos >= ins.repeatStop)
							actMemo.currentSamplePos = ins.repeatStart + ((actMemo.currentSamplePos-ins.repeatStart)%ins.repeatLength);
					}
					else // is PingPong
					{
						if (actMemo.currentSamplePos >= ins.repeatStop)
						{
							actMemo.currentDirection = -1;
							actMemo.currentSamplePos = ins.repeatStop - ((actMemo.currentSamplePos-ins.repeatStart)%ins.repeatLength) - 1;
						}
					}
				}
			}
		}
		else // Loop is on and we have ping pong!
		{
			actMemo.currentTuningPos -= actMemo.currentTuning;
			if (actMemo.currentTuningPos <= 0)
			{
				int hi = ((-actMemo.currentTuningPos) >> Helpers.SHIFT) + 1;
				actMemo.currentSamplePos -= hi;
				actMemo.currentTuningPos += hi<<Helpers.SHIFT;
				if (actMemo.currentSamplePos <= ins.repeatStart)
				{
					actMemo.currentDirection = 1;
					actMemo.currentSamplePos = ins.repeatStart + ((ins.repeatStart-actMemo.currentSamplePos)%ins.repeatLength);
				}
			}
		}
	}
	ChannelMemory previousChannelMemory[] = new ChannelMemory[128];

	// Using the Sample.index
	boolean sampleExported[] = new boolean[256];
	int sampleStarts[] = new int[256];
	int sampleLengths[] = new int[256];
	int sampleLoopStarts[] = new int[256];
	int sampleLoopLengths[] = new int[256];
	int sampleFinalRatio1[] = new int[256];
	int sampleFinalRatio2[] = new int[256];
	/**
	 * Fill the buffers with channel data
	 * @since 18.06.2006
	 * @param leftBuffer
	 * @param rightBuffer
	 * @param startIndex
	 * @param endIndex
	 * @param actMemo
	 */
	private void mixChannelIntoBuffers(final int[] leftBuffer, final int[] rightBuffer, final int startIndex, final int endIndex, final ChannelMemory actMemo)
	{
		if (debugData != null) {
			int channel = actMemo.currentElement.getChannel();
			if (previousChannelMemory[channel] == null) {
				previousChannelMemory[channel] = new ChannelMemory();
			}
			if (actMemo.currentSample != previousChannelMemory[channel].currentSample) {
				outputChannelHeader(channel);
				debugData.println("currentSample: " + actMemo.currentSample.toString());
				debugData.flush();
			}
			if (actMemo.currentNotePeriod != previousChannelMemory[channel].currentNotePeriod) {
				outputChannelHeader(channel);
				debugData.println("currentNotePeriod: " + actMemo.currentNotePeriod);
				debugData.flush();
			}
			if (actMemo.instrumentFinished != previousChannelMemory[channel].instrumentFinished) {
				outputChannelHeader(channel);
				debugData.println("instrumentFinished: " + actMemo.instrumentFinished);
				debugData.flush();
			}
			if (((actMemo.actRampVolLeft + actMemo.actRampVolRight) >> 22) != ((previousChannelMemory[channel].actRampVolLeft + previousChannelMemory[channel].actRampVolRight) >> 22)) {
				outputChannelHeader(channel);
				debugData.println("actRampVolCombined: " + ((actMemo.actRampVolLeft + actMemo.actRampVolRight) >> 22));
				debugData.flush();
			}

			// A trigger
			if (actMemo.newInstrumentSet == true) {
				int sampleIndex = actMemo.currentSample.index;
				exportSample(actMemo);
				outputChannelHeader(channel);
				debugData.println("newInstrumentSet: " + sampleIndex);
				debugData.flush();

				outputWaitFrames();
				try {
					debugMusicData.write(Helpers.kMusicCommandPlayNote | channel);

					outputVolume(actMemo);
					debugMusicData.write(sampleIndex);

					outputNote(actMemo, sampleIndex);

					debugMusicData.flush();
				} catch (IOException e) {
				}

				actMemo.newInstrumentSet = false;
			}

			if (!actMemo.instrumentFinished) {
				if (getRealFrequency(actMemo, actMemo.currentSample.index) != actMemo.previousRealFrequency) {
					int sampleIndex = actMemo.currentSample.index;
					exportSample(actMemo);
					outputChannelHeader(channel);
					debugData.println("newNoteSet: " + sampleIndex);
					debugData.flush();

					outputWaitFrames();
					try {
						debugMusicData.write(Helpers.kMusicCommandAdjustNote | channel);

						outputVolume(actMemo);
						outputNote(actMemo, sampleIndex);

						debugMusicData.flush();
					} catch (IOException e) {
					}
				}

				if (getRealVolume(actMemo) != actMemo.previousRealVolume) {
					if (!System.getProperty("music.volume","").isEmpty()) {
						int sampleIndex = actMemo.currentSample.index;
						exportSample(actMemo);
						outputChannelHeader(channel);
						debugData.println("newVolumeSet: " + sampleIndex);
						debugData.flush();

						outputWaitFrames();
						try {
							debugMusicData.write(Helpers.kMusicCommandAdjustVolume | channel);

							outputVolume(actMemo);

							debugMusicData.flush();
						} catch (IOException e) {
						}
					}
				}


			}


			previousChannelMemory[channel].deepCopy(actMemo);
		}
		for (int i=startIndex; i<endIndex; i++)
		{
			// Retrieve the Sampledata for this point (interpolated, if necessary)
			long sample = actMemo.currentSample.getInterpolatedSample(doISP, actMemo.currentSamplePos, actMemo.currentTuningPos);

			// Resonance Filters
			if (actMemo.filterOn) sample = doResonance(actMemo, sample);

			// Volume Ramping (deClick) Left!
			long volL = actMemo.actRampVolLeft;
			if ((actMemo.deltaVolLeft>0 && volL>actMemo.actVolumeLeft) ||
				(actMemo.deltaVolLeft<0 && volL<actMemo.actVolumeLeft))
			{
				volL = actMemo.actRampVolLeft = actMemo.actVolumeLeft;
				actMemo.deltaVolLeft = 0;
			}
			else
			{
				actMemo.actRampVolLeft += actMemo.deltaVolLeft;
			}
			// Volume Ramping (deClick) Right!
			long volR = actMemo.actRampVolRight;
			if ((actMemo.deltaVolRight>0 && volR>actMemo.actVolumeRight) ||
				(actMemo.deltaVolRight<0 && volR<actMemo.actVolumeRight))
			{
				volR = actMemo.actRampVolRight = actMemo.actVolumeRight;
				actMemo.deltaVolRight = 0;
			}
			else
			{
				actMemo.actRampVolRight += actMemo.deltaVolRight;
			}
			
			// Fit into volume for the two channels
			leftBuffer[i] += (int)((sample * volL) >> Helpers.MAXVOLUMESHIFT);
			rightBuffer[i]+= (int)((sample * volR) >> Helpers.MAXVOLUMESHIFT);

			// Now fit the loops of the current Sample
			fitIntoLoops(actMemo);
			if (actMemo.instrumentFinished) break;
		}
	}

	private void outputNote(ChannelMemory actMemo, int sampleIndex) throws IOException {
		// Convert internal frequency to hardware values
		int realFrequency = getRealFrequency(actMemo, sampleIndex);
		debugMusicData.write(realFrequency);
		debugMusicData.write(realFrequency >> 8);
		actMemo.previousRealFrequency = realFrequency;
	}

	private int getRealFrequency(ChannelMemory actMemo, int sampleIndex) {
		int frequency = (actMemo.currentTuning * sampleRate) / (1 << Helpers.SHIFT);
		frequency = applySampleRatioForIndex(frequency, sampleIndex);
		int realFrequency = AudioExpansion.calculateRateFromFrequency(frequency);
		return realFrequency;
	}

	private void outputVolume(ChannelMemory actMemo) throws IOException {
		int volume = getRealVolume(actMemo);
		debugMusicData.write(volume);
		actMemo.previousRealVolume = volume;
	}

	private int getRealVolume(ChannelMemory actMemo) {
		int volume = actMemo.currentVolume * 4;
		if (volume > 255) {
			volume = 255;
		}
		return volume;
	}

	int applySampleRatioForIndex(int value , int sampleIndex) {
		if (sampleFinalRatio2[sampleIndex] == 0) {
			return 0;
		}
		return (int)(((long)value * (long)sampleFinalRatio1[sampleIndex]) / (long)sampleFinalRatio2[sampleIndex]);
	}


	private void exportSample(ChannelMemory actMemo) {
		int sampleIndex = actMemo.currentSample.index;
		if (sampleExported[sampleIndex]) {
			return;
		}

		sampleStarts[sampleIndex] = debugSampleData.size();

		int realLength = actMemo.currentSample.length;
		sampleFinalRatio1[sampleIndex] = 1;
		sampleFinalRatio2[sampleIndex] = 1;
		if (realLength > 256) {
			sampleFinalRatio1[sampleIndex] = sampleRatio1;
			sampleFinalRatio2[sampleIndex] = sampleRatio2;
		}
		sampleLengths[sampleIndex] = applySampleRatioForIndex(realLength  , sampleIndex);

		int loopLength = actMemo.currentSample.repeatLength;
		if (loopLength == 0) {
			// The first exported sample is always going to be 0x80 (a silent sample, basically 0)
			// This is used to simulate a voice off for non-looping voices
			sampleLoopStarts[sampleIndex] = 0;
			sampleLoopLengths[sampleIndex] = 0;
		} else {
			sampleLoopStarts[sampleIndex] = sampleStarts[sampleIndex] + applySampleRatioForIndex(actMemo.currentSample.repeatStart , sampleIndex);
			sampleLoopLengths[sampleIndex] = applySampleRatioForIndex(loopLength ,sampleIndex);
		}

		byte[] buffer = new byte[applySampleRatioForIndex(realLength ,sampleIndex)];
		for (int i = 0; i < realLength ; i++) {
			int sourceSample = actMemo.currentSample.sample[i+5] >> 16;
			if (sourceSample < -127) {
				sourceSample = -127;
			}
			if (sourceSample > 127) {
				sourceSample = 127;
			}
			// Debug: Reduce overall volume
//			sourceSample = sourceSample / 2;
			// Convert 8 bit signed sample data to unsigned 8 bit
			int sample = 0x80 + sourceSample;
			if (sample < 0) {
				sample = 0;
			}
			if (sample > 255) {
				sample = 255;
			}
			int realI = applySampleRatioForIndex(i , sampleIndex);
			if (realI < buffer.length) {
				buffer[realI] =(byte) sample;
			}
		}
		try {
			debugSampleData.write(buffer);
			debugSampleData.flush();

			// And write the sample data
			debugMusicData.write(Helpers.kMusicCommandSetSampleData);
			debugMusicData.write(sampleIndex);

			debugMusicData.write(sampleStarts[sampleIndex]);
			debugMusicData.write(sampleStarts[sampleIndex]>>8);
			debugMusicData.write(sampleLengths[sampleIndex]);
			debugMusicData.write(sampleLengths[sampleIndex]>>8);

			debugMusicData.write(sampleLoopStarts[sampleIndex]);
			debugMusicData.write(sampleLoopStarts[sampleIndex]>>8);
			debugMusicData.write(sampleLoopLengths[sampleIndex]);
			debugMusicData.write(sampleLoopLengths[sampleIndex]>>8);
			debugMusicData.flush();

		} catch (IOException e) {
		}

		sampleExported[sampleIndex] = true;
	}

	private void outputChannelHeader(int channel) {
		debugData.println("Channel: " + channel);
	}

	private void outputWaitFrames() {
		int delta = currentFrame - lastFrameOutput;
		while (delta >= 256) {
			int buffer = Helpers.kMusicCommandWaitFrames;
			try {
				debugMusicData.write(buffer);
				debugMusicData.write(255);
			} catch (IOException e) {
			}
			delta -= 255;
		}
		if (delta > 0) {
			int buffer = Helpers.kMusicCommandWaitFrames;
			try {
				debugMusicData.write(buffer);
				debugMusicData.write(delta);
			} catch (IOException e) {
			}
		}
		lastFrameOutput = currentFrame;
	}

	/**
	 * Retrieves Sample Data without manipulating the currentSamplePos and currentTuningPos and currentDirection
	 * (a kind of read ahead)
	 * @since 18.06.2006
	 * @param leftBuffer
	 * @param rightBuffer
	 * @param aktMemo
	 */
	private void fillRampDataIntoBuffers(final int[] leftBuffer, final int[] rightBuffer, final ChannelMemory aktMemo)
	{
		// Remember changeable values
		final int currentTuningPos = aktMemo.currentTuningPos;
		final int currentSamplePos = aktMemo.currentSamplePos;
		final int currentDirection = aktMemo.currentDirection;
		final boolean instrumentFinished = aktMemo.instrumentFinished;
		final int actRampVolLeft = aktMemo.actRampVolLeft;
		final int actRampVolRight = aktMemo.actRampVolRight;
		
		mixChannelIntoBuffers(leftBuffer, rightBuffer, 0, Helpers.VOL_RAMP_LEN, aktMemo);
		
		// set them back
		aktMemo.currentTuningPos = currentTuningPos;
		aktMemo.currentSamplePos = currentSamplePos;
		aktMemo.instrumentFinished = instrumentFinished;
		aktMemo.currentDirection = currentDirection;
		aktMemo.actRampVolLeft = actRampVolLeft;
		aktMemo.actRampVolRight = actRampVolRight;
	}
	/**
	 * Will mix #count 24bit signed samples in stereo into the two buffer.
	 * The buffers will contain 24Bit signed samples.
	 * @param leftBuffer
	 * @param rightBuffer
	 * @param count
	 * @return #of samples mixed, -1 if mixing finished
	 */
	long songTotalSamplePos = 0;
	public int mixIntoBuffer(final int[] leftBuffer, final int[] rightBuffer, final int count)
	{
//		try
//		{
			if (modFinished) return -1;
			
			int bufferIdx = 0; // Index into the buffer
			int endIndex = samplePerTicks; // where !will! we be after next mixing (will this still fit?!)
			final int maxEndIndex = count - Helpers.VOL_RAMP_LEN;
			if (maxEndIndex < samplePerTicks) throw new RuntimeException("The mixing buffer is too small. Minimum are " + samplePerTicks + " sample frames");

			while (endIndex < maxEndIndex && !modFinished)
			{
				for (int c = 0; c < maxChannels; c++)
				{
//					if (c!=1) continue; //TODO: COMMENT THIS OUT AGAIN IF FINISHED WITH DEBUGGING
					
					ChannelMemory actMemo = channelMemory[c];

					// Mix this channel?
					if (!actMemo.muted && isChannelActive(actMemo))
					{
						// fill in those samples
						mixChannelIntoBuffers(leftBuffer, rightBuffer, bufferIdx, endIndex, actMemo);
						
						// and get the ramp data for interweaving, if there is something left
// MPi: Disabled this as it was causing actRampVolCombined to appear after newInstrumentSet:
						if (!actMemo.instrumentFinished) fillRampDataIntoBuffers(nvRampL, nvRampR, actMemo);
					}
				}
	
				// Now Interweave with last ticks ramp buffer data
				for (int n=0; n<Helpers.VOL_RAMP_LEN; n++)
				{
					final int difFade = Helpers.VOL_RAMP_LEN - n;
					
					leftBuffer [bufferIdx + n] = ((leftBuffer [bufferIdx + n] * n) + (vRampL[n] * difFade))>>Helpers.VOL_RAMP_FRAC;
					rightBuffer[bufferIdx + n] = ((rightBuffer[bufferIdx + n] * n) + (vRampR[n] * difFade))>>Helpers.VOL_RAMP_FRAC;
					
					// and copy in one step...
					vRampL[n] = nvRampL[n]; vRampR[n] = nvRampR[n];
					nvRampL[n] = nvRampR[n] = 0;
				}

				// Convert into 60 fps time signature for the C64
				currentFrame = (int) ((60 * songTotalSamplePos) / sampleRate);
				songTotalSamplePos += samplePerTicks;

				bufferIdx += samplePerTicks;
				modFinished = doTickEvents();
				endIndex += samplePerTicks; // tickevents can change samplePerTicks
			}
	
			return bufferIdx;
//		}
//		catch (Throwable ex)
//		{
//			//This is only needed for debugging porposes during playback, so
//			//we know, where the error happend...
//			throw new RuntimeException(this.getClass().getName() + " " + currentPatternIndex + "[" + currentRow + "]", ex);
//		}
	}
}
