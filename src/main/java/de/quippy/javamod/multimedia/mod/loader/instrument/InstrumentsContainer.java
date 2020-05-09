/*
 * @(#) InstrumentContainer.java
 * 
 * Created on 28.04.2006 by Daniel Becker (quippy@quippy.de)
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

import de.quippy.javamod.multimedia.mod.loader.Module;

/**
 * @author Daniel Becker
 * @since 28.04.2006
 */
public class InstrumentsContainer
{
	private Module parent;
	private Instrument [] instruments;
	private Sample [] samples;

	/**
	 * Constructor for InstrumentsContainer
	 */
	public InstrumentsContainer(Module module, int anzInstruments, int anzSamples)
	{
		super();
		this.parent = module;
		
		if (anzInstruments!=0) 
			this.instruments = new Instrument[anzInstruments]; 
		else 
			this.instruments = null;
		
		if (anzSamples!=0)
			this.samples = new Sample[anzSamples];
		else
			this.samples = null;
	}
	/**
	 * With XM-Mods we do not know the real ammount
	 * of samples at startup
	 * @since 01.11.2007
	 * @param newAmount
	 */
	public void reallocSampleSpace(int newAmount)
	{
		Sample [] newSamples = new Sample[newAmount];
		if (this.samples!=null)
		{
			//System.arraycopy(this.samples, 0, newSamples, this.samples.length);
			for (int i=0; i<this.samples.length; i++)
				newSamples[i] = this.samples[i];
		}
		this.samples = newSamples;
	}
	public void setInstrument(int index, Instrument instrument)
	{
		instruments[index] = instrument;
	}
	/**
	 * Stores the sample with desired index
	 * @since 19.06.2006
	 * @param index
	 * @param sample
	 */
	public void setSample(int index, Sample sample)
	{
		this.samples[index] = sample;
	}
	/**
	 * returns the sample with the index
	 * @since 19.06.2006
	 * @param sampleIndex
	 * @return
	 */
	public Sample getSample(int sampleIndex)
	{
		if (samples==null || sampleIndex>=samples.length || sampleIndex<0)
			return null;
		else
			return samples[sampleIndex];
	}
	/**
	 * Add all sample length values to retrive the complete
	 * amount. Is used only by the ProtrackerMods
	 * @since 19.06.2006
	 * @return
	 */
	public int getFullSampleLength()
	{
		int fullSampleLength=0;
		for (int i=0; i<samples.length; i++)
			fullSampleLength+=samples[i].length;
		return fullSampleLength;
	}
	/**
	 * returns the instrument with the index
	 * @since 19.06.2006
	 * @param sampleIndex
	 * @return
	 */
	public Instrument getInstrument(int index)
	{
		if (instruments==null)
			return null;
		else
		if (index>=instruments.length) 
			return null;
		else
			return instruments[index];
	}
	/**
	 * @return the instruments
	 */
	public Instrument[] getInstruments()
	{
		return instruments;
	}
	/**
	 * @return the samples
	 */
	public Sample[] getSamples()
	{
		return samples;
	}
	/**
	 * @return the parent
	 */
	public Module getParentModule()
	{
		return parent;
	}
	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		StringBuilder bf = new StringBuilder();
		
		if (instruments!=null)
		{
			for (int i=0; i<instruments.length; i++)
			{
				bf.append(instruments[i].toString());
				bf.append('\n');
			}
		}
		if (samples!=null)
		{
			for (int i=0; i<samples.length; i++)
			{
				bf.append(samples[i].toShortString());
				bf.append('\n');
			}
		}
		return bf.toString();
	}
}
