package com.loomcom.symon.devices;

import TestGlue.Glue;
import com.bdd6502.APUData;
import com.bdd6502.DisplayBombJack;
import com.bdd6502.DisplayLayer;
import com.loomcom.symon.exceptions.MemoryRangeException;
import javafx.util.Pair;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class UserPortTo24BitAddressTest {

    UserPortTo24BitAddress apu;

    static int fullAddressFromAddrEx(int address , int addressEx) {
        return address | (addressEx << 16);
    }

    static MemoryItem fromAddrValue(int address , int addressEx, int value) {
        return new MemoryItem(fullAddressFromAddrEx(address,addressEx), value);
    }

    static class MemoryItem extends Pair<Integer,Integer> {

        public MemoryItem(Integer key, Byte value) {
            super(key, value & 0xff);
        }
        public MemoryItem(Integer key, Integer value) {
            super(key, value.byteValue() & 0xff);
        }

        @Override
        public boolean equals(Object o) {
            if (super.equals(o)) {
                return true;
            }
            if (o instanceof MemoryItem) {
                if (((MemoryItem) o).getKey().compareTo(getKey()) == 0) {
                    if (((MemoryItem) o).getValue().compareTo(getValue()) == 0) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
    List<MemoryItem> memoryAddressByteSequence;

    public DisplayBombJack getDisplayBombJack() throws IOException, MemoryRangeException {
        memoryAddressByteSequence = new ArrayList<MemoryItem>();

        DisplayBombJack display = new DisplayBombJack();
        display.enableDebugData();

        // Add a profiling display layer
        DisplayLayer debugLayer = new DisplayLayer() {
            @Override
            public void writeData(int address, int addressEx, byte data) {
                memoryAddressByteSequence.add(new MemoryItem(fullAddressFromAddrEx(address,addressEx) , data));
            }

            @Override
            public void setAddressBus(int address, int addressEx) {
            }

            @Override
            public void randomiseData(Random rand) {

            }

            @Override
            public int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync, boolean _doLineStart, boolean enableLayer) {
                return 0;
            }
        };
        display.addLayer(debugLayer);

        apu = new UserPortTo24BitAddress(null);
        apu.setApuEnableDebug(true);
        apu.enableDebugData();
        apu.addDevice(display);
        APUData apuData = new APUData();
        apuData.enableDebugData();
        apu.setEnableAPU(display , apuData);
        display.setCallbackAPU(apu);

        return display;
    }

    public void addAPUCodeData(String filename) throws Exception {
        String returnString = Glue.runProcessWithOutput("..\\C64\\acme.exe -v3 --lib ../ -o target/apu.bin --labeldump test.lbl -f plain " + filename , true);
        System.out.println(returnString);
        Glue.loadLabels("test.lbl");

        int apuCodeStart = Glue.valueToInt("APUCode_Start");
        int apuCodeSize = Glue.valueToInt("APUCode_Size");
        int apuDataStart = Glue.valueToInt("APUData_Start");
        int apuDataSize = Glue.valueToInt("APUData_Size");


        apu.apuData.writeDataFromFile(0x8000, 0x02, "target/apu.bin", apuCodeStart, apuCodeSize);
        apu.apuData.writeDataFromFile(0x4000, 0x02, "target/apu.bin", apuDataStart, apuDataSize);
    }

    @After
    public void tearDown() throws Exception {
        apu.apuData.closeDebugData();
    }

    @Test
    public void checkAPU1() throws Exception {

        DisplayBombJack display = getDisplayBombJack();

        addAPUCodeData("features/checkAPU1.a");

        display.calculateAFrame();

        assertThat(memoryAddressByteSequence, is(empty()));

        // No APU reset
        apu.apuData.writeData(0x2000, 0x02, 0x01);

        display.calculateAFrame();

        assertThat(memoryAddressByteSequence, is(empty()));

        // Enable APU
        apu.apuData.writeData(0x2000, 0x02, 0x03);


        // Render for display position and use memory checks to validate wait for position logic
        display.calculatePixelsUntil(0x188, 0x40);
        assertThat(memoryAddressByteSequence, is(empty()));

        display.calculatePixelsUntil(0x00, 0x40);
        assertThat(memoryAddressByteSequence, contains(
                fromAddrValue(0x9c00,0x01,0x11) ,
                fromAddrValue(0x9c01,0x01,0x22) ,
                fromAddrValue(0x9c1c,0x01,0x11) ,
                fromAddrValue(0x9c1d,0x01,0x22)
        ));
        memoryAddressByteSequence.clear();

        // Check APU pause while instruction or data address set
        apu.apuData.setAddressBus(0x8000, 0x02);
        display.calculatePixelsUntil(0x188, 0x50);
        assertThat(memoryAddressByteSequence, is(empty()));

        display.calculatePixelsUntil(0x00, 0x50);
        assertThat(memoryAddressByteSequence, is(empty()));
        apu.apuData.setAddressBus(0x0000, 0x00);
        display.calculatePixelsUntil(0xf0, 0x50);
        assertThat(memoryAddressByteSequence, contains(
                fromAddrValue(0x9c00,0x01,0x33) ,
                fromAddrValue(0x9c01,0x01,0x44) ,
                fromAddrValue(0x9c1c,0x01,0x33) ,
                fromAddrValue(0x9c1d,0x01,0x44)
        ));
        memoryAddressByteSequence.clear();



        display.calculatePixelsUntil(0x188, 0x60);
        assertThat(memoryAddressByteSequence, is(empty()));

        display.calculatePixelsUntil(0x00, 0x60);
        assertThat(memoryAddressByteSequence, contains(
                fromAddrValue(0x9c00,0x01,0x55) ,
                fromAddrValue(0x9c01,0x01,0x66) ,
                fromAddrValue(0x9c1c,0x01,0x55) ,
                fromAddrValue(0x9c1d,0x01,0x66)
        ));

    }


    @Test
    public void checkAPU2() throws Exception {

        DisplayBombJack display = getDisplayBombJack();

        addAPUCodeData("features/checkAPU2.a");

        display.calculateAFrame();

        assertThat(memoryAddressByteSequence, is(empty()));

        // No APU reset
        apu.apuData.writeData(0x2000, 0x02, 0x01);

        display.calculateAFrame();

        assertThat(memoryAddressByteSequence, is(empty()));

        // Enable APU
        apu.apuData.writeData(0x2000, 0x02, 0x03);

        display.calculatePixelsUntil(0, 0);

        assertThat(memoryAddressByteSequence, contains(
                fromAddrValue(0x9c01,0x01,0x11) ,
                fromAddrValue(0x9c00,0x01,0x22) ,

                fromAddrValue(0x9c00,0x01,0x01) ,
                fromAddrValue(0x9c01,0x01,0x00) ,
                fromAddrValue(0x9c00,0x01,0x9c) ,

                fromAddrValue(0x9c00,0x01,0x11) ,
                fromAddrValue(0x9c02,0x01,0x11) ,
                fromAddrValue(0x9c00,0x01,0x22) ,

                fromAddrValue(0x9c00,0x01,0x56) ,
                fromAddrValue(0x9c00,0x01,0x11) ,
                fromAddrValue(0x9c00,0x01,0x78) ,
                fromAddrValue(0x9c00,0x01,0x22)


        ));

    }


    @Test
    public void checkAPU3() throws Exception {

        DisplayBombJack display = getDisplayBombJack();

        addAPUCodeData("features/checkAPU3.a");

        display.calculateAFrame();

        assertThat(memoryAddressByteSequence, is(empty()));

        // No APU reset
        apu.apuData.writeData(0x2000, 0x02, 0x01);

        display.calculateAFrame();

        assertThat(memoryAddressByteSequence, is(empty()));

        // Enable APU
        apu.apuData.writeData(0x2000, 0x02, 0x03);

        display.calculatePixelsUntil(0, 0);

        assertThat(memoryAddressByteSequence, contains(
                fromAddrValue(0x9c00,0x01,0x11) ,
                fromAddrValue(0x9c01,0x01,0x22) ,
                fromAddrValue(0x9c02,0x01,0x33) ,
                fromAddrValue(0x9c03,0x01,0x44)
        ));

    }


    @Test
    public void checkAPU4() throws Exception {

        DisplayBombJack display = getDisplayBombJack();

        addAPUCodeData("features/checkAPU4.a");

        display.calculateAFrame();

        assertThat(memoryAddressByteSequence, is(empty()));

        // No APU reset
        apu.apuData.writeData(0x2000, 0x02, 0x01);
        apu.apuData.writeData(0x2000, 0x02, 0x03);

        display.calculatePixelsUntil(0, 0);

        assertThat(memoryAddressByteSequence, contains(
                fromAddrValue(0x9c00,0x01,0x73) ,
                fromAddrValue(0x9c01,0x01,0x11) ,
                fromAddrValue(0x9c02,0x01,0x22) ,
                fromAddrValue(0x9c03,0x01,0x33) ,
                fromAddrValue(0x9c04,0x01,0x44) ,
                fromAddrValue(0x9c05,0x01,0xc7) ,
                fromAddrValue(0x9c06,0x01,0x99) ,
                fromAddrValue(0x9c07,0x01,0xef)
        ));

    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void checkAPU5() throws Exception {

        DisplayBombJack display = getDisplayBombJack();

        addAPUCodeData("features/checkAPU5.a");

        display.calculateAFrame();

        assertThat(memoryAddressByteSequence, is(empty()));

        // No APU reset
        apu.apuData.writeData(0x2000, 0x02, 0x01);
        apu.apuData.writeData(0x2000, 0x02, 0x03);

        exceptionRule.expect(AssertionError.class);
        exceptionRule.expectMessage("kAPU_SkipIfEQ needs kAPU_IDataSelect* to be the same for the current and previous instruction");
        display.calculatePixelsUntil(0, 0);

        assertThat(memoryAddressByteSequence, is(empty()));

    }

    @Test
    public void checkAPU6() throws Exception {

        DisplayBombJack display = getDisplayBombJack();

        addAPUCodeData("features/checkAPU6.a");

        assertThat(apu.apuData.getApuData()[0] , is(equalTo((byte)0x10)));
        assertThat(apu.apuData.getApuData()[1] , is(equalTo((byte)0x11)));
        assertThat(apu.apuData.getApuData()[2] , is(equalTo((byte)0x12)));
        assertThat(apu.apuData.getApuData()[3] , is(equalTo((byte)0x13)));
        assertThat(apu.apuData.getApuData()[4] , is(equalTo((byte)0x14)));

        assertThat(apu.apuData.getApuData()[9] , is(equalTo((byte)0x00)));
        assertThat(apu.apuData.getApuData()[10] , is(equalTo((byte)0x00)));


        display.calculateAFrame();
        assertThat(memoryAddressByteSequence, is(empty()));


        apu.apuData.writeData(0x2000, 0x02, 0x01);
        apu.apuData.writeData(0x2000, 0x02, 0x03);

        // Check APU step by step execution behaviour
        apu.calculatePixel();
        assertThat(apu.apuInstuctionSchedule , is(equalTo(1)));
        assertThat(apu.apuPC , is(equalTo(0)));
        apu.calculatePixel();
        assertThat(apu.apuInstuctionSchedule , is(equalTo(2)));
        assertThat(apu.apuPC , is(equalTo(0)));
        apu.calculatePixel();
        assertThat(apu.apuInstuctionSchedule , is(equalTo(3)));
        assertThat(apu.apuPC , is(equalTo(0)));
        apu.calculatePixel();
        assertThat(apu.apuInstuctionSchedule , is(equalTo(4)));
        assertThat(apu.apuPC , is(equalTo(0)));
        apu.calculatePixel();
        assertThat(apu.apuInstuctionSchedule , is(equalTo(5)));
        assertThat(apu.apuPC , is(equalTo(1)));
        apu.calculatePixel();
        assertThat(apu.apuInstuctionSchedule , is(equalTo(6)));
        assertThat(apu.apuPC , is(equalTo(1)));
        String debugState = apu.getDebugOutputLastState();
        assertThat(debugState , not(containsString("<<Wait RAM>>")));

        // Simulate a write from the user port to somewhere in the 24 bit address space, to cause APU and data bus contention
        apu.writeMemoryBusWithState(0);
        // And then the schedule loops
        apu.calculatePixel();
        assertThat(apu.apuInstuctionSchedule , is(equalTo(0)));
        assertThat(apu.apuPC , is(equalTo(1)));
        debugState = apu.getDebugOutputLastState();
        assertThat(debugState , containsString("<<Wait RAM>>"));

        apu.calculatePixel();
        assertThat(apu.apuInstuctionSchedule , is(equalTo(0)));
        assertThat(apu.apuPC , is(equalTo(1)));
        debugState = apu.getDebugOutputLastState();
        assertThat(debugState , containsString("<<Wait RAM>>"));

        display.calculatePixelsUntil(0, 0);

        assertThat(memoryAddressByteSequence, is(contains(new Pair<Integer,Integer>(0,0))));

        assertThat(apu.apuData.getApuData()[0] , is(equalTo((byte)0x10)));
        assertThat(apu.apuData.getApuData()[1] , is(equalTo((byte)0x10)));
        assertThat(apu.apuData.getApuData()[2] , is(equalTo((byte)0x11)));
        assertThat(apu.apuData.getApuData()[3] , is(equalTo((byte)0x12)));
        assertThat(apu.apuData.getApuData()[4] , is(equalTo((byte)0x13)));
        assertThat(apu.apuData.getApuData()[5] , is(equalTo((byte)0x00)));

        assertThat(apu.apuData.getApuData()[9] , is(equalTo((byte)0x00)));
        assertThat(apu.apuData.getApuData()[10] , is(equalTo((byte)0x10)));
        assertThat(apu.apuData.getApuData()[11] , is(equalTo((byte)0x11)));
        assertThat(apu.apuData.getApuData()[12] , is(equalTo((byte)0x12)));
        assertThat(apu.apuData.getApuData()[13] , is(equalTo((byte)0x13)));
        assertThat(apu.apuData.getApuData()[14] , is(equalTo((byte)0x00)));
    }

    @Test
    public void checkAPU7() throws Exception {

        DisplayBombJack display = getDisplayBombJack();

        addAPUCodeData("features/checkAPU7.a");

        display.calculateAFrame();

        assertThat(memoryAddressByteSequence, is(empty()));

        // No APU reset
        apu.apuData.writeData(0x2000, 0x02, 0x01);
        apu.apuData.writeData(0x2000, 0x02, 0x03);

        exceptionRule.expect(AssertionError.class);
        exceptionRule.expectMessage("kAPU_InternalMEWR needs kAPU_IDataSelect* to be the same for the current and previous instruction");
        display.calculatePixelsUntil(0, 0);

        assertThat(memoryAddressByteSequence, is(empty()));
    }

    @Test
    public void checkAPU8() throws Exception {

        DisplayBombJack display = getDisplayBombJack();

        addAPUCodeData("features/checkAPU8.a");

        display.calculateAFrame();

        assertThat(memoryAddressByteSequence, is(empty()));

        // No APU reset
        apu.apuData.writeData(0x2000, 0x02, 0x01);
        apu.apuData.writeData(0x2000, 0x02, 0x03);

        exceptionRule.expect(AssertionError.class);
        exceptionRule.expectMessage("kAPU_InternalMEWR should not select internal memory with kAPU_IDataSelectRAM while writing internal memory");
        display.calculatePixelsUntil(0, 0);

        assertThat(memoryAddressByteSequence, is(empty()));
    }
}
