package com.loomcom.symon.devices;

import com.bdd6502.APUData;
import com.bdd6502.DisplayBombJack;
import com.bdd6502.DisplayLayer;
import com.bdd6502.MemoryBus;
import com.loomcom.symon.exceptions.MemoryRangeException;
import javafx.util.Pair;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class UserPortTo24BitAddressTest {

    static int fullAddressFromAddrEx(int address , int addressEx) {
        return address | (addressEx << 16);
    }

    List<Pair<Integer,Byte>> memoryAddressByteSequence;

    public DisplayBombJack getDisplayBombJack() throws IOException, MemoryRangeException {
        memoryAddressByteSequence = new ArrayList<Pair<Integer,Byte>>();

        DisplayBombJack display = new DisplayBombJack();
        display.enableDebugData();

        // Add a profiling display layer
        DisplayLayer debugLayer = new DisplayLayer() {
            @Override
            public void writeData(int address, int addressEx, byte data) {
                memoryAddressByteSequence.add(new Pair<Integer, Byte>(fullAddressFromAddrEx(address,addressEx) , data));
            }

            @Override
            public void setAddressBus(int address, int addressEx) {
            }

            @Override
            public int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync) {
                return 0;
            }
        };
        display.addLayer(debugLayer);

        UserPortTo24BitAddress apu = new UserPortTo24BitAddress(null);
        apu.addDevice(display);
        APUData apuData = new APUData();
        apu.setEnableAPU(display , apuData);
        display.setCallbackAPU(apu);

        return display;
    }


    @Test
    public void checkAPU1() throws IOException, MemoryRangeException {
        DisplayBombJack display = getDisplayBombJack();

        display.calculateAFrame();

        assertThat(memoryAddressByteSequence, is(empty()));
    }
}