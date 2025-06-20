package com.bdd6502;

import com.loomcom.symon.util.HexUtil;
import org.apache.commons.lang.ArrayUtils;

import javax.sound.sampled.*;
import java.io.*;
import java.util.Arrays;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

// Any comment with "HW:" draws attention to hardware design considerations
public class AudioExpansion2 extends MemoryBus implements Runnable {
    int addressRegisters = 0x8000, addressExRegisters = 0x01;
    int addressExSampleBank = 0x06;

    // 2MHz /8 /8 gives 31250 Hz and ample time to latch, add, select, apply volume for 8 voices, accumulate and output in a cyclic pattern
    // The real hardware LOADOUTPUT period gives 25000 Hz
    public static final int sampleRate = 25000;
    static final int samplesToMix = 8;
    public static final int counterShift = 12;
    public static final int counterShiftValue = 1<<counterShift;
    public static final int counterShiftMask = counterShiftValue - 1;
    byte[] sampleBuffer = new byte[samplesToMix];

    SourceDataLine line = null;

    byte sampleRAM[] = new byte[0x100000];
    byte sampleRAMBank = 0;

    int voiceInternalCounter;    // Not addressable
    int currentSample = 0x80;
    byte voiceControl = 0;
    byte voiceVolume;
    int voiceAddress;
    int voiceAddressAdd;
    int voiceLength;
    int voiceRate;

    OutputStream outSamples;

    public static int calculateRateFromFrequency(int frequency) {
        return (AudioExpansion2.counterShiftValue * frequency) / AudioExpansion2.sampleRate;
    }

    public AudioExpansion2() {
    }

    int channel = 0;
    public AudioExpansion2(int addressRegisters, int addressExSampleBank, int channel) {
        assertThat(addressRegisters, is(greaterThanOrEqualTo(0x8000)));
        assertThat(addressRegisters, is(lessThan(0xc000)));
        assertThat(addressRegisters & 0xff, is(equalTo(0x00)));
        this.addressRegisters = addressRegisters;
        this.addressExSampleBank = addressExSampleBank;
        this.channel = channel;
    }

    public void start() {
        Arrays.fill(sampleBuffer , (byte) 0x80);
        try {
            outSamples = new BufferedOutputStream(new FileOutputStream("target/debugchannel2.pcmu8"));
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
        if (addressExActive(addressEx, addressExRegisters) && (address >= addressRegisters + 0x30) && (address < (addressRegisters + 0x40))) {
            int voiceSection = address - (addressRegisters + 0x30);
            switch (voiceSection) {
                case 0:
                    sampleRAMBank = data;
                    break;
                case 1:
                    voiceControl = data;
                    handleVoiceActiveMaskLatches();
                    break;
                case 2:
                    voiceVolume = data;
                    break;
                case 3:
                    voiceAddress = (voiceAddress & 0xffff00) | (data & 0xff);
                    break;
                case 4:
                    voiceAddress = (voiceAddress & 0xff00ff) | ((data & 0xff) << 8);
                    break;
                case 5:
                    voiceAddress = (voiceAddress & 0x00ffff) | ((data & 0xff) << 16);
                    break;
                case 6:
                    voiceLength = (voiceLength & 0xffff00) | (data & 0xff);
                    break;
                case 7:
                    voiceLength = (voiceLength & 0xff00ff) | ((data & 0xff) << 8);
                    break;
                case 8:
                    voiceLength = (voiceLength & 0x00ffff) | ((data & 0xff) << 16);
                    break;
                case 9:
                    voiceRate = (voiceRate & 0xff00) | (data & 0xff);
                    break;
                case 10:
                    voiceRate = (voiceRate & 0x00ff) | ((data & 0xff) << 8);
                    break;
                default:
                    // Do nothing
                    break;
            }
        }

        // Some contention here as this uses banks of RAM
        if (addressExActive(addressEx, addressExSampleBank)) {
            busContention = 8;
            sampleRAM[address | ((sampleRAMBank & 0xff)<<16)] = data;
        }
    }

    private void handleVoiceActiveMaskLatches() {
        if ((voiceControl & 0x01) == 0) {
            // HW: Reset the latch on low. voiceInternalCounter is 32 bits
            voiceInternalCounter = 0;
            currentSample = 0x80;
            voiceAddressAdd = 0;
            bitsRemaining = 0;
        }
    }

    @Override
    public void setAddressBus(int address, int addressEx) {

    }


    public boolean calculateSamples() {
        if (line.available() < sampleBuffer.length) {
            return false;
        }
        calculateBalancedSamples(channel);
        line.write(sampleBuffer,0,sampleBuffer.length);
        try {
            outSamples.write(sampleBuffer,0,sampleBuffer.length);
        } catch (IOException e) {
        }
        return true;
    }

    int bitsRemaining = 0;
    int currentByte = 0;
    int getNextBit() {
        if (bitsRemaining <= 0) {
            int address = voiceAddress + voiceAddressAdd;
            currentByte = sampleRAM[address & 0xfffff] & 0xff;
            bitsRemaining = 8;
            voiceAddressAdd++;
        }
        int ret = 0;
        if ((currentByte & 0x80) != 0) {
            ret = 1;
        }
        bitsRemaining--;
        currentByte <<= 1;
        return ret;
    }
    public void calculateBalancedSamples(int channel) {
        for (int i = channel ; i < sampleBuffer.length ; i+=2) {
            ageContention();

            handleVoiceActiveMaskLatches();

            // HW: Here sample is signed, in hardware it is unsigned
            int sample = currentSample - 0x80;

            if ((voiceControl & 0x01) != 0) {
                // HW: Note add is clocked before any sample read
                voiceInternalCounter += voiceRate;
                if (voiceInternalCounter >= counterShiftValue) {
                    voiceInternalCounter -= counterShiftValue;

                    // Decode the delta
                    int delta = 0;
                    int numBits = 0;
                    int gotBit = 1;
                    while(getNextBit() == 0) {
                        numBits++;
                    }
                    if (numBits > 0) {
                        // Anything else except the 0 special case...
                        while (numBits > 0) {
                            delta <<= 1;
                            delta |= gotBit;
                            gotBit = getNextBit();
                            numBits--;
                        }
                        if (gotBit != 0) {
                            delta = -delta;
                        }
                    }

                    // Testing the result of signed int maths with unsigned byte two's complement based maths
                    byte testSample = (byte)currentSample;
                    testSample += (byte) (delta & 0xff);

                    sample += delta;
                    currentSample = sample + 0x80;

                    if ((currentSample & 0xff) != (testSample & 0xff)) {
                        int z=0;
                    }

                    if (voiceAddressAdd >= voiceLength) {
                        voiceAddressAdd = 0;
                        // HW: Note the comparison result should only be used when the offset adders are stable
                        // HW: Note the active flag is a reset only if it is previously active, do not use a level to clear this flag
                        if ((voiceControl & 0x02) > 0) {
                            voiceAddressAdd = 0;
                            currentSample = 0x80;
                            bitsRemaining = 0;
                        } else {
                            voiceControl = 0;
                        }
                    }
                }
            } else {
                // HW: Add 0x80 as the middle part of 8 bit unsigned samples for inactive channels
                // In emulation we do nothing...
                sample += 0;
            }

            // HW: This will be implemented with a 0x10000 byte ROM containing a multiply/divide lookup table
            int sampleAfterVolume = (sample * (voiceVolume & 0xff)) / 255;
            sampleAfterVolume += 0x80;

            if (sampleAfterVolume > 255) {
                sampleAfterVolume = 255;
            } else if (sampleAfterVolume < 0) {
                sampleAfterVolume = 0;
            }
            sampleBuffer[i] = (byte)(sampleAfterVolume & 0xff);
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

    public void randomiseData(Random rand) {
        randomiseHelper(rand , sampleRAM);
        voiceInternalCounter = rand.nextInt();

        // To avoid killing ears/speakers/headphones use a quiet volume
        voiceVolume = 32;

        voiceAddress = rand.nextInt();
        voiceLength = rand.nextInt();
        voiceRate = rand.nextInt();

        voiceControl = (byte) rand.nextInt();
    }

    public String getDebug() {
        String debug = "";
        debug += "Audio2: ";
        if ( (voiceControl & 0x01) != 0 ) {
            debug += " Active";
        } else {
            debug += "       ";
        }
        if ( (voiceControl & 0x02) != 0 ) {
            debug += " Loop";
        } else {
            debug += "     ";
        }
        debug += " Addr:" + HexUtil.wordToHex(voiceAddress) + " Len:" + HexUtil.wordToHex(voiceLength) + " Rate:" + HexUtil.wordToHex(voiceRate) + " Counter:" + HexUtil.wordToHex(voiceInternalCounter) + " AddrAdd:" + HexUtil.wordToHex(voiceAddressAdd);
        debug += "\r";
        return debug;
    }
}
