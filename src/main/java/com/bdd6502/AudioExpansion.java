package com.bdd6502;

import javax.sound.sampled.*;

import java.io.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

// Any comment with "HW:" draws attention to hardware design considerations
public class AudioExpansion extends MemoryBus implements Runnable {
    int addressRegisters = 0x8000, addressExRegisters = 0x01;
    int addressExSampleBank = 0x04;

    // 2MHz /8 /8 gives 31250 Hz and ample time to latch, add, select, apply volume for 8 voices, accumulate and output in a cyclic pattern
    // The real hardware LOADOUTPUT period gives 25000 Hz
    public static final int sampleRate = 25000;
    static final int numVoices = 4;
    public static final int voiceSize = 11;
    static final int samplesToMix = 8;
    public static final int counterShift = 12;
    public static final int counterShiftValue = 1<<counterShift;
    public static final int counterShiftMask = counterShiftValue - 1;
    byte[] sampleBuffer = new byte[samplesToMix];

    SourceDataLine line = null;

    byte sampleRAM[] = new byte[0x10000];

    int voiceInternalCounter[] = new int[numVoices];    // Not addressable
    boolean voiceInternalChooseLoop[] = new boolean[numVoices];    // Not addressable

    byte voicesActiveMaskPrevious = 0;
    byte voicesActiveMask = 0;
    // Each voice is stored linearly in the address space, with 8 bytes per voice:
    // volume
    // voiceAddressLo , voiceAddressHi
    // voiceLengthLo , voiceLengthHi
    // voiceRateLo , voiceRateHi
    // spare
    int voiceVolume[] = new int[numVoices];
    int voiceAddress[] = new int[numVoices];
    int voiceLength[] = new int[numVoices];
    int voiceRate[] = new int[numVoices];
    int voiceLoopAddress[] = new int[numVoices];
    int voiceLoopLength[] = new int[numVoices];

    OutputStream outSamples;

    public static int calculateRateFromFrequency(int frequency) {
        return (AudioExpansion.counterShiftValue * frequency) / AudioExpansion.sampleRate;
    }

    public AudioExpansion() {
    }

    public AudioExpansion(int addressRegisters, int addressExSampleBank) {
        assertThat(addressRegisters, is(greaterThanOrEqualTo(0x8000)));
        assertThat(addressRegisters, is(lessThan(0xc000)));
        assertThat(addressRegisters & 0x7ff, is(equalTo(0x00)));
        this.addressRegisters = addressRegisters;
        this.addressExSampleBank = addressExSampleBank;
    }

    public void start() {
        try {
            outSamples = new BufferedOutputStream(new FileOutputStream("target/debugchannel.pcmu8"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // https://docs.oracle.com/javase/tutorial/sound/playing.html
        AudioFormat format = new AudioFormat(sampleRate, 8, 2, false, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        try {
            line = (SourceDataLine) AudioSystem.getLine(info);

            line.open(format, (sampleRate*2) / 60);    // At 60 fps we want this many samples
            Control[] controls = line.getControls();
            line.start();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void close()
    {
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        line.close();
    }

    Thread thread = null;
    public void startThread() {
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void writeData(int address, int addressEx, byte data) {
        // No contention, this will use latches, many of them
        if (MemoryBus.addressActive(addressEx, addressExRegisters) && (address >= addressRegisters) && (address < (addressRegisters + (numVoices * voiceSize)))) {
            int voice = (address - addressRegisters) / voiceSize;
            int voiceSection = (address - addressRegisters) % voiceSize;
            switch (voiceSection) {
                case 0:
                    voiceVolume[voice] = (data & 0xff);
                    break;
                case 1:
                    voiceAddress[voice] = (voiceAddress[voice] & 0xff00) | (data & 0xff);
                    break;
                case 2:
                    voiceAddress[voice] = (voiceAddress[voice] & 0x00ff) | ((data & 0xff) << 8);
                    break;
                case 3:
                    voiceLength[voice] = (voiceLength[voice] & 0xff00) | (data & 0xff);
                    break;
                case 4:
                    voiceLength[voice] = (voiceLength[voice] & 0x00ff) | ((data & 0xff) << 8);
                    break;
                case 5:
                    voiceRate[voice] = (voiceRate[voice] & 0xff00) | (data & 0xff);
                    break;
                case 6:
                    voiceRate[voice] = (voiceRate[voice] & 0x00ff) | ((data & 0xff) << 8);
                    break;
                case 7:
                    voiceLoopAddress[voice] = (voiceLoopAddress[voice] & 0xff00) | (data & 0xff);
                    break;
                case 8:
                    voiceLoopAddress[voice] = (voiceLoopAddress[voice] & 0x00ff) | ((data & 0xff) << 8);
                    break;
                case 9:
                    voiceLoopLength[voice] = (voiceLoopLength[voice] & 0xff00) | (data & 0xff);
                    break;
                case 10:
                    voiceLoopLength[voice] = (voiceLoopLength[voice] & 0x00ff) | ((data & 0xff) << 8);
                    break;
                default:
                    // Do nothing
                    break;
            }
        }

        if (MemoryBus.addressActive(addressEx, addressExRegisters) && address == addressRegisters + (numVoices * voiceSize) + 1) {
            for (int i = 0 ; i < numVoices ; i++) {
                // Reset clear in latches
                if ((data & (1 << i)) == 0) {
                    voicesActiveMask &= ~(1 << i);
                } else {
                    // Detect positive edge on active mask and set
                    if ( (voicesActiveMaskPrevious & (1 << i)) == 0 ) {
                        voicesActiveMask |= (1 << i);
                    }
                }
            }
            voicesActiveMaskPrevious = data;
            handleVoiceActiveMaskLatches();
        }

        // HW: Full 64K for sample memory, must use a proper selector
        // Some contention here as this uses banks of RAM
        if (MemoryBus.addressActive(addressEx, addressExSampleBank)) {
            busContention = 8;
            sampleRAM[address] = data;
        }
    }

    private void handleVoiceActiveMaskLatches() {
        for (int i = 0 ; i < numVoices ; i++) {
            if ((voicesActiveMask & (1 << i)) == 0) {
                // HW: Reset the latch on low. voiceInternalCounter is 24 bits
                voiceInternalCounter[i] = 0;
                voiceInternalChooseLoop[i] = false;
            }
        }
    }

    @Override
    public void setAddressBus(int address, int addressEx) {

    }

    public boolean calculateSamples() {
        if (line.available() < sampleBuffer.length) {
            return false;
        }
        calculateBalancedSamples(0);
        calculateBalancedSamples(1);
        line.write(sampleBuffer,0,sampleBuffer.length);
        try {
            outSamples.write(sampleBuffer,0,sampleBuffer.length);
        } catch (IOException e) {
        }
        return true;
    }

    public void calculateBalancedSamples(int offset) {
        for (int i = offset ; i < sampleBuffer.length ; i+=2) {
            ageContention();

            // Here accumulatedSample is signed, in hardware it is a two byte 16 bit lookup of two internal unsigned 8 bit data channels
            int accumulatedSample = 0;
            for (int index = 0 ; index < (numVoices/2) ; index++) {
                int voice = (offset * (numVoices/2)) + index;

                handleVoiceActiveMaskLatches();

                if ((voicesActiveMask & (1 << voice)) > 0) {
                    int address;
                    if (voiceInternalChooseLoop[voice]) {
                        // HW: Note accuracy shifting is just address line selection
                        address = voiceLoopAddress[voice] + (voiceInternalCounter[voice] >> counterShift);
                    } else {
                        // HW: Note accuracy shifting is just address line selection
                        address = voiceAddress[voice] + (voiceInternalCounter[voice] >> counterShift);
                    }

                    int sample = sampleRAM[address & 0xffff] & 0xff;
                    sample = sample - 0x80;
                    // HW: This will be implemented with a 0x10000 byte ROM containing a multiply/divide lookup table
                    sample = (sample * voiceVolume[voice]) / 255;

                    accumulatedSample += sample;

                    // HW: Note add is clocked after the sample read
                    voiceInternalCounter[voice] += voiceRate[voice];

                    // HW: Note selective comparison is just address line selection
                    if (voiceInternalChooseLoop[voice]) {
                        if (((voiceInternalCounter[voice] >> counterShift) & 0xffff) >= voiceLoopLength[voice]) {
                            // HW: Note selective reset of only some adders when length is reached
//                            voiceInternalCounter[voice] = voiceInternalCounter[voice] & counterShiftMask;
                            voiceInternalCounter[voice] = 0;
                        }
                    } else {
                        if (((voiceInternalCounter[voice] >> counterShift) & 0xffff) >= voiceLength[voice]) {
                            voiceInternalChooseLoop[voice] = true;
                            // HW: Note selective reset of only some adders when length is reached
//                            voiceInternalCounter[voice] = voiceInternalCounter[voice] & counterShiftMask;
                            voiceInternalCounter[voice] = 0;
                        }
                    }

                } else {
                    // HW: Add 0x80 as the middle part of 8 bit unsigned samples for inactive channels
                    // In emulation we do nothing...
//                    accumulatedSample += 0;
                }
            }

            // HW: Note voice division is just address line selection
            // HW: There will need to be an overflow and upper clamp applied
            accumulatedSample = accumulatedSample / (numVoices/2);
            // Convert from s8 to u8 sample
            accumulatedSample = accumulatedSample + 0x80;
            if (accumulatedSample > 255) {
                accumulatedSample = 255;
            } else if (accumulatedSample < 0) {
                accumulatedSample = 0;
            }
            sampleBuffer[i] = (byte)(accumulatedSample & 0xff);
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            if (!mute) {
                while (calculateSamples()) {
                }
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    volatile boolean mute = false;
    public void setMute(boolean mute) {
        this.mute = mute;
    }
}
