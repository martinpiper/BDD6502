package com.bdd6502;

import javax.sound.sampled.*;

public class AudioExpansion extends MemoryBus {
    final int sampleRate = 44100;
    SourceDataLine line = null;

    public AudioExpansion() {

    }

    public void start() {
        // https://docs.oracle.com/javase/tutorial/sound/playing.html
        AudioFormat format = new AudioFormat(sampleRate, 8, 1, false, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format , sampleRate / 60);    // At 60 fps we want this many samples
            line.start();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void writeSamples(byte[] b) {
        line.write(b,0,b.length);
    }

    public int availableSamples() {
        return line.available();
    }

    public void close() {
        line.close();
    }

    @Override
    void writeData(int address, int addressEx, byte data) {

    }
}
