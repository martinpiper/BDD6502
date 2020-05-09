/*
 * @(#) Sample.java
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
package de.quippy.javamod.multimedia.mod.loader.instrument;

import de.quippy.javamod.multimedia.mod.mixer.interpolation.CubicSpline;
import de.quippy.javamod.multimedia.mod.mixer.interpolation.WindowedFIR;
import de.quippy.javamod.system.Helpers;

/**
 * Used to store the Instruments
 * @author Daniel Becker
 * @since 21.04.2006
 */
public class Sample
{
	private static int staticIndex = 0;
	public int index = staticIndex++;
	public String name;			// Name of the sample
	public int length;			// full length (already *2 --> Mod-Fomat)
	public int fineTune;		// Finetuning -8..+8
	public int volume;			// Basisvolume
	public int repeatStart;		// # of the loop start (already *2 --> Mod-Fomat)
	public int repeatStop;		// # of the loop end   (already *2 --> Mod-Fomat)
	public int repeatLength;	// length of the loop
	public int loopType;		// 0: no Looping, 1: normal, 2: pingpong, 3: backwards
	public int transpose;		// PatternNote + transpose
	public int baseFrequency;	// BaseFrequency

	//S3M:
	public int type;			// always 1 for a sample
	public String dosFileName;	// DOS File-Name
	public int flags;			// flag: 1:Looping sample 2:Stereo 4:16Bit-Sample...
	
	// XM
	public int panning;			// default Panning
	public int vibratoType;		// Vibrato Type 
	public int vibratoSweep;	// Vibrato Sweep
	public int vibratoDepth;	// Vibrato Depth
	public int vibratoRate;		// Vibrato Rate 

	// IT
	public int sustainLoopStart;// SustainLoopStart
	public int sustainLoopEnd;  // SustainLoopEnd
	public int flag_CvT;				// Flag for Instrument Save
	public int globalVolume;	// GlobalVolume

	public int [] sample;		// The sampledata, already converted to 16 bit (always)
								// 8Bit: 0..127,128-255; 16Bit: -32768..0..+32767
	/**
	 * Constructor for Sample
	 */
	public Sample()
	{
		super();
	}
	public void allocSampleData()
	{
		sample = new int[length+5];
	}
	/**
	 * Fits the loop-data given in instruments loaded
	 * These values are often not correkt
	 * @since 27.08.2006
	 * @param modType
	 */
	public void fixSampleLoops(int modType)
	{
		if (sample==null || length==0) return;
		if (repeatStop>length)
		{
			repeatStop = length;
			repeatLength = repeatStop - repeatStart;
		}
		if (repeatStart+2>repeatStop)
		{
			repeatStop = repeatStart = 0;
			repeatLength = repeatStop - repeatStart;
			loopType = 0;
		}
		sample[length+4] = sample[length+3] = sample[length+2] = sample[length+1] = sample[length] = sample[length-1];
		if (loopType==1 && (repeatStop+4>repeatLength || modType==Helpers.MODTYPE_MOD || modType==Helpers.MODTYPE_S3M))
		{
			sample[repeatStop  ] = sample[repeatStart  ];
			sample[repeatStop+1] = sample[repeatStart+1];
			sample[repeatStop+2] = sample[repeatStart+2];
			sample[repeatStop+3] = sample[repeatStart+3];
			sample[repeatStop+4] = sample[repeatStart+4];
		}
	}
	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		StringBuilder bf = new StringBuilder(this.name);
		bf.append('(').
			append("index:").append(index).append(',').
			append("length:").append(length).append(',').
			append("fineTune:").append(fineTune).append(',').
			append("transpose:").append(transpose).append(',').
			append("baseFrequency:").append(baseFrequency).append(',').
			append("volume:").append(volume).append(',').
			append("panning:").append(panning).append(',').
			append("repeatStart:").append(repeatStart).append(',').
			append("repeatLength:").append(repeatLength).append(',').
			append("repeatStop:").append(repeatStop).append(')');
		
		return bf.toString();
	}
	public String toShortString()
	{
		return this.name;
	}
	/**
	 * Does the linear interpolation with the next sample
	 * @since 06.06.2006
	 * @param currentTuningPos
	 * @return
	 */
	private int getLinearInterpolated(final int currentSamplePos, final int currentTuningPos)
	{
		final long s1 = ((long)sample[currentSamplePos  ])<<Helpers.SAMPLE_SHIFT;
		final long s2 = ((long)sample[currentSamplePos+1])<<Helpers.SAMPLE_SHIFT;
		return (int)((s1 + (((s2-s1)*((long)currentTuningPos))>>Helpers.SHIFT))>>Helpers.SAMPLE_SHIFT);
	}
	/**
	 * does cubic interpolation with the next sample
	 * @since 06.06.2006
	 * @param currentTuningPos
	 * @return
	 */
	private int getCubicInterpolated(final int currentSamplePos, final int currentTuningPos)
	{
		final int poslo = (currentTuningPos >> CubicSpline.SPLINE_FRACSHIFT) & CubicSpline.SPLINE_FRACMASK;
		
		final long v1 = (((currentSamplePos-1)<0)?0L:	(long)CubicSpline.lut[poslo  ]*(long)sample[currentSamplePos-1]) +
				  		(								(long)CubicSpline.lut[poslo+1]*(long)sample[currentSamplePos  ]) +
				  		(								(long)CubicSpline.lut[poslo+2]*(long)sample[currentSamplePos+1]) +
				  		(								(long)CubicSpline.lut[poslo+3]*(long)sample[currentSamplePos+2]);
		
		return (int)(v1 >> CubicSpline.SPLINE_QUANTBITS);
	}
	/**
	 * does a windowed fir interploation with the next sample
	 * @since 15.06.2006
	 * @param currentTuningPos
	 * @return
	 */
	private int getFIRInterpolated(final int currentSamplePos, final int  currentTuningPos)
	{
		final int poslo  = currentTuningPos & WindowedFIR.WFIR_POSFRACMASK;
		final int firidx = ((poslo+WindowedFIR.WFIR_FRACHALVE)>>WindowedFIR.WFIR_FRACSHIFT) & WindowedFIR.WFIR_FRACMASK;
		final long v1 = (((currentSamplePos-3)<0)?0L:	(long)WindowedFIR.lut[firidx  ]*(long)sample[currentSamplePos-3]) +
				  		(((currentSamplePos-2)<0)?0L:	(long)WindowedFIR.lut[firidx+1]*(long)sample[currentSamplePos-2]) +
				  		(((currentSamplePos-1)<0)?0L:	(long)WindowedFIR.lut[firidx+2]*(long)sample[currentSamplePos-1]) +
				  		(								(long)WindowedFIR.lut[firidx+3]*(long)sample[currentSamplePos  ]);
		final long v2 = (								(long)WindowedFIR.lut[firidx+4]*(long)sample[currentSamplePos+1]) +
				  		(								(long)WindowedFIR.lut[firidx+5]*(long)sample[currentSamplePos+2]) +
				  		(								(long)WindowedFIR.lut[firidx+6]*(long)sample[currentSamplePos+3]) +
				  		(								(long)WindowedFIR.lut[firidx+7]*(long)sample[currentSamplePos+4]);
		return (int)(((v1>>1) + (v2>>1)) >> WindowedFIR.WFIR_16BITSHIFT);
	}
	/**
	 * @since 15.06.2006
	 * @return Returns the sample using desired interploation.
	 */
	public int getInterpolatedSample(final int doISP, final int currentSamplePos, final int currentTuningPos)
	{
		// Shit happens... indeed!
		if (sample==null) return 0;
		if (currentSamplePos>length) return 0;

		// Now return correct sample
		switch (doISP)
		{
			case 0: 
				return sample[currentSamplePos];
			case 1:
				return getLinearInterpolated(currentSamplePos, currentTuningPos);
			case 2:
				return getCubicInterpolated(currentSamplePos, currentTuningPos);
			case 3:
				return getFIRInterpolated(currentSamplePos, currentTuningPos);
			default:
				return 0;
		}
	}
	/**
	 * @param baseFrequency The baseFrequency to set.
	 */
	public void setBaseFrequency(int baseFrequency)
	{
		this.baseFrequency = baseFrequency;
	}
	/**
	 * @param dosFileName The dosFileName to set.
	 */
	public void setDosFileName(String dosFileName)
	{
		this.dosFileName = dosFileName;
	}
	/**
	 * @param fineTune The fineTune to set.
	 */
	public void setFineTune(int fineTune)
	{
		this.fineTune = fineTune;
	}
	/**
	 * @param flags The flags to set.
	 */
	public void setFlags(int newFlags)
	{
		flags = newFlags;
	}
	/**
	 * @param length The length to set.
	 */
	public void setLength(int length)
	{
		this.length = length;
	}
	/**
	 * @param loopType The loopType to set.
	 */
	public void setLoopType(int loopType)
	{
		this.loopType = loopType;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name)
	{
		this.name = name;
	}
	/**
	 * @param repeatLength The repeatLength to set.
	 */
	public void setRepeatLength(int repeatLength)
	{
		this.repeatLength = repeatLength;
	}
	/**
	 * @param repeatStart The repeatStart to set.
	 */
	public void setRepeatStart(int repeatStart)
	{
		this.repeatStart = repeatStart;
	}
	/**
	 * @param repeatStop The repeatStop to set.
	 */
	public void setRepeatStop(int repeatStop)
	{
		this.repeatStop = repeatStop;
	}
	/**
	 * @param sample The sample to set.
	 */
	public void setSample(int[] sample)
	{
		this.sample = sample;
	}
	/**
	 * @param transpose The transpose to set.
	 */
	public void setTranspose(int transpose)
	{
		this.transpose = transpose;
	}
	/**
	 * @param type The type to set.
	 */
	public void setType(int type)
	{
		this.type = type;
	}
	/**
	 * @param volume The volume to set.
	 */
	public void setVolume(int volume)
	{
		this.volume = volume;
	}
	/**
	 * @param panning The panning to set.
	 */
	public void setPanning(int panning)
	{
		this.panning = panning;
	}
	/**
	 * @param cvT the cvT to set
	 */
	public void setCvT(int flag_CvT)
	{
		this.flag_CvT = flag_CvT;
	}
	/**
	 * @param sustainLoopStart the sustainLoopStart to set
	 */
	public void setSustainLoopStart(int sustainLoopStart)
	{
		this.sustainLoopStart = sustainLoopStart;
	}
	/**
	 * @param sustainLoopEnd the sustainLoopEnd to set
	 */
	public void setSustainLoopEnd(int sustainLoopEnd)
	{
		this.sustainLoopEnd = sustainLoopEnd;
	}
	/**
	 * @param vibratoDepth The vibratoDepth to set.
	 */
	public void setVibratoDepth(int vibratoDepth)
	{
		this.vibratoDepth = vibratoDepth;
	}
	/**
	 * @param vibratoRate The vibratoRate to set.
	 */
	public void setVibratoRate(int vibratoRate)
	{
		this.vibratoRate = vibratoRate;
	}
	/**
	 * @param vibratoSweep The vibratoSweep to set.
	 */
	public void setVibratoSweep(int vibratoSweep)
	{
		this.vibratoSweep = vibratoSweep;
	}
	/**
	 * @param vibratoType The vibratoType to set.
	 */
	public void setVibratoType(int vibratoType)
	{
		this.vibratoType = vibratoType;
	}
	/**
	 * @param globalVolume the globalVolume to set
	 */
	public void setGlobalVolume(int globalVolume)
	{
		this.globalVolume = globalVolume;
	}
}
