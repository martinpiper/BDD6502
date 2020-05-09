/*
 * @(#)ModContainer.java
 *
 * Created on 12.10.2007 by Daniel Becker
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

import java.net.URL;
import java.util.Properties;

import de.quippy.javamod.mixer.Mixer;
import de.quippy.javamod.multimedia.MultimediaContainer;
import de.quippy.javamod.multimedia.MultimediaContainerManager;
import de.quippy.javamod.multimedia.mod.loader.Module;
import de.quippy.javamod.multimedia.mod.loader.ModuleFactory;
import de.quippy.javamod.system.Helpers;
import de.quippy.javamod.system.Log;

/**
 * @author: Daniel Becker
 * @since: 12.10.2007
 */
public class ModContainer extends MultimediaContainer
{
	public static final String PROPERTY_PLAYER_BITSPERSAMPLE = "javamod.player.bitspersample"; 
	public static final String PROPERTY_PLAYER_STEREO = "javamod.player.stereo"; 
	public static final String PROPERTY_PLAYER_FREQUENCY = "javamod.player.frequency"; 
	public static final String PROPERTY_PLAYER_MSBUFFERSIZE = "javamod.player.msbuffersize"; 
	public static final String PROPERTY_PLAYER_ISP = "javamod.player.ISP"; 
	public static final String PROPERTY_PLAYER_WIDESTEREOMIX = "javamod.player.widestereomix"; 
	public static final String PROPERTY_PLAYER_NOISEREDUCTION = "javamod.player.noisereduction"; 
	public static final String PROPERTY_PLAYER_MEGABASS = "javamod.player.megabass"; 
	public static final String PROPERTY_PLAYER_NOLOOPS = "javamod.player.noloops"; 
	/* GUI Constants ---------------------------------------------------------*/
	public static final String DEFAULT_WIDESTEREOMIX = "false";
	public static final String DEFAULT_NOISEREDUCTION = "false";
	public static final String DEFAULT_MEGABASS = "false";
	public static final String DEFAULT_NOLOOPS = "0";
	public static final String DEFAULT_SAMPLERATE = "48000";
   	public static final String DEFAULT_CHANNEL = "2";
	public static final String DEFAULT_BITSPERSAMPLE = "16";
	public static final String DEFAULT_MSBUFFERSIZE = "250";
	public static final String DEFAULT_INTERPOLATION_INDEX = "1";
	public static final String[] SAMPLERATE = new String[]
	{
		"8000", "11025", "16000", "22050", "33075", "44100", DEFAULT_SAMPLERATE, "96000", "192000"
	};
	public static final String[] CHANNELS = new String[]
   	{
   		"1", DEFAULT_CHANNEL
   	};
	public static final String[] BITSPERSAMPLE = new String[]
	{
		"8", DEFAULT_BITSPERSAMPLE, "24"
	};
	public static final String[] INTERPOLATION = new String[]
  	{
  		"none", "linear", "cubic spline", "windowed FIR"
  	};
	public static final String[] BUFFERSIZE = new String[]
  	{
  		"30", "50", "75", "100", "125", "150", "175", "200", "225", "250"
  	};

	private Module currentMod;
	private ModMixer currentMixer;

	/**
	 * Will be executed during class load
	 */
	static
	{
		MultimediaContainerManager.registerContainer(new ModContainer());
	}
	
	/**
	 * @since: 12.10.2007
	 */
	public ModContainer()
	{
		super();
	}
	/**
	 * @param url
	 * @return
	 * @see de.quippy.javamod.multimedia.MultimediaContainer#getInstance(java.net.URL)
	 */
	@Override
	public MultimediaContainer getInstance(URL url)
	{
		MultimediaContainer result = super.getInstance(url); 
		try
		{
			currentMod = ModuleFactory.getInstance(url);
			if (currentMod==null)
			{
				Log.error("[ModContainer] Failed with loading of " + url.toString(), new Exception("Modfile "+url.toString()+" is obviously corrupt!"));
				result = null;
			}
			else
			{
			}
		}
		catch (Exception ex)
		{
			currentMod = null;
			throw new RuntimeException(ex);
		}
		return result;
	}
	/**
	 * @return
	 * @see de.quippy.javamod.multimedia.MultimediaContainer#getSongName()
	 */
	@Override
	public String getSongName()
	{
		if (currentMod!=null)
		{
			String songName = currentMod.getSongName();
			if (songName!=null && songName.trim().length()!=0)
				return songName;
		}
		return super.getSongName();
	}
	/**
	 * @param url
	 * @return
	 * @see de.quippy.javamod.multimedia.MultimediaContainer#getSongInfosFor(java.net.URL)
	 */
	@Override
	public Object[] getSongInfosFor(URL url)
	{
		String songName = MultimediaContainerManager.getSongNameFromURL(url);
		Long duration = Long.valueOf(-1);
		try
		{
			final Module theMod = ModuleFactory.getInstance(url);
			final String modSongName = theMod.getSongName();
			if (modSongName!=null && modSongName.trim().length()!=0) songName = modSongName;
			int loopValue = 0;
			if (loopValue == Helpers.PLAYER_LOOP_DEACTIVATED) loopValue = Helpers.PLAYER_LOOP_IGNORE;
			ModMixer theMixer = new ModMixer(theMod, 8, 1, 1000, 0, false, false, false, loopValue, 25);
			duration = Long.valueOf(theMixer.getLengthInMilliseconds());
		}
		catch (Throwable ex)
		{
		}
		return new Object[] { songName, duration };
	}
	/**
	 * @return
	 * @see de.quippy.javamod.multimedia.MultimediaContainer#canExport()
	 */
	@Override
	public boolean canExport()
	{
		return true;
	}
	/**
	 * @return
	 * @since 13.10.2007
	 * @see de.quippy.javamod.multimedia.MultimediaContainer#getInfoPanel()
	 */
	/**
	 * @see de.quippy.javamod.multimedia.MultimediaContainerInterface#getFileExtensionList()
	 * @since: 12.10.2007
	 * @return
	 */
	@Override
	public String[] getFileExtensionList()
	{
		return ModuleFactory.getSupportedFileExtensions();
	}
	/**
	 * @return the name of the group of files this container knows
	 * @see de.quippy.javamod.multimedia.MultimediaContainer#getName()
	 */
	@Override
	public String getName()
	{
		return "Mod-File";
	}
	/**
	 * @param newProps
	 * @see de.quippy.javamod.multimedia.MultimediaContainer#configurationChanged(java.util.Properties)
	 */
	@Override
	public void configurationChanged(Properties props)
	{
	}
	/**
	 * Get the values from the gui and store them into the main Propertys
	 * @since 13.10.2007
	 */
	@Override
	public void configurationSave(Properties props)
	{
	}
	/**
	 * @see de.quippy.javamod.multimedia.MultimediaContainer#createNewMixer()
	 * @since: 12.10.2007
	 * @return
	 */
	@Override
	public Mixer createNewMixer()
	{
		if (currentMod==null) return null;

		Properties props = new Properties();
		configurationSave(props);
		
		int frequency = Integer.parseInt(props.getProperty(PROPERTY_PLAYER_FREQUENCY, DEFAULT_SAMPLERATE)); 
		int bitsPerSample = Integer.parseInt(props.getProperty(PROPERTY_PLAYER_BITSPERSAMPLE, DEFAULT_BITSPERSAMPLE)); 
		int channels = Integer.parseInt(props.getProperty(PROPERTY_PLAYER_STEREO, DEFAULT_CHANNEL));
		int isp = Integer.parseInt(props.getProperty(PROPERTY_PLAYER_ISP, DEFAULT_INTERPOLATION_INDEX)); 
		boolean wideStereoMix = Boolean.parseBoolean(props.getProperty(PROPERTY_PLAYER_WIDESTEREOMIX, DEFAULT_WIDESTEREOMIX)); 
		boolean noiseReduction = Boolean.parseBoolean(props.getProperty(PROPERTY_PLAYER_NOISEREDUCTION, DEFAULT_NOISEREDUCTION));
		boolean megaBass = Boolean.parseBoolean(props.getProperty(PROPERTY_PLAYER_MEGABASS, DEFAULT_MEGABASS));
		int loopValue = Integer.parseInt(props.getProperty(PROPERTY_PLAYER_NOLOOPS, DEFAULT_NOLOOPS));
		int msBufferSize = Integer.parseInt(props.getProperty(PROPERTY_PLAYER_MSBUFFERSIZE, DEFAULT_MSBUFFERSIZE));
		currentMixer = new ModMixer(currentMod, bitsPerSample, channels, frequency, isp, wideStereoMix, noiseReduction, megaBass, loopValue, msBufferSize);
		return currentMixer;
	}
	/**
	 * @since 14.10.2007
	 * @return
	 */
	public ModMixer getCurrentMixer()
	{
		return currentMixer;
	}
	public Module getCurrentMod()
	{
		return currentMod;
	}
}
