Code originally came from: http://www.javamod.de/javamod.html
	It has been hacked around to:
		* Remove any GUI code, DSP effects, and other multimedia file formats not related to MOD/XM files
		* Export music file events, note play, volume, pitch etc
		* Export samples in PCM u8 format
	See code changes involving fastExport, debugData, previousChannelMemory
