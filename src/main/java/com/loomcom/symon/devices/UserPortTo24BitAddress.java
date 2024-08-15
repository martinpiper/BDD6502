package com.loomcom.symon.devices;

import com.bdd6502.APUData;
import com.bdd6502.DisplayBombJack;
import com.bdd6502.MemoryBus;
import com.bdd6502.MemoryInternal;
import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;
import com.loomcom.symon.util.HexUtil;
import cucumber.api.Scenario;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class UserPortTo24BitAddress extends Device {
    public static final String kAPUDEBUG = "APUDebug: ";
    private Scenario mScenario = null;
    int registerDDRPortA = 0;
    int registerDDRPortB = 0;
    int bus24State = 0;
    boolean bus24CountEnabled = false;
    int bus24Bytes[] = new int[4];
    final int kbus32_latch7_SelectMask=0x03;
    final int kbus32_latch7_Passthrough=0x00;
    final int kbus32_latch7_RAM=0x01;
    final int kbus32_latch7_PassthroughDisable=0x02;
    final int kbus32_latch7_Disabled=0x03;
    final int kbus32_latch7_InternalPA2=0x04;
    final int kbus32_latch7_FastDMAStart=0x08;
    final int kbus32_latch7_ResetDone=0x80;
    int bus32LatchAddress = 0;
    int bus32Latches[] = new int[16];
    int bus32FastDMACounter = 0;
    boolean bus32FastDMAStart = false;
    double bus32clockMultiplier = 1.0f;

    List<MemoryInternal> bus32MemoryBlocks = new LinkedList<>();
    List<MemoryBus> externalDevices = new LinkedList<>();
    boolean simpleMode = false;
    boolean simpleModeLastMEWR = true;
    boolean simpleModeLastLatchCLK = true;
    PrintWriter debugData = null;
    boolean add32Bit1Mode = false;
    private int bus24CIA2PortASerialBusVICBank = 0;
    private int bus32CurrentAddress;
    private int bus32AddAddress;

    public boolean isEnableAPU() {
        return enableAPU;
    }

    boolean enableAPU = false;
    DisplayBombJack displayBombJack = null;
    APUData apuData = null;
    final int kCyclesPerInstruction = 7;
    int apuInstuctionSchedule = 0;
    boolean apuIntercepting = false;
    int apuPreviousGotByte = 0;
    long apuPreviousGotIDataSelect = 0;

    // All of these are reset by _RESET
    boolean apuHitWait = true;
    int apuADDRB1 = 0;
    int apuADDRB2 = 0;
    int apuPC = 0;
    int apuEBS = 0;
    int apuEADDR = 0;
    int apuEBS2 = 0;
    int apuEADDR2 = 0;
    int apuWait8 = 0;
    int apuWait16 = 0;
    int apuWait24 = 0;
    int apuDataReg[] = new int[5];
    boolean busTrace = false;

    public static UserPortTo24BitAddress getThisInstance() {
        return thisInstance;
    }

    static UserPortTo24BitAddress thisInstance;

    public UserPortTo24BitAddress(Scenario scenario) throws MemoryRangeException {
        super(0xdc00, 0xddff, "UserPortTo24BitAddress");
        mScenario = scenario;

        propertiesUpdated();
        thisInstance = this;
    }

    @Override
    public void propertiesUpdated() {
        busTrace = false;
        if (System.getProperty("bdd6502.bus24.trace","").toLowerCase().equals("true")) {
            busTrace = true;
        }

        apuEnableDebug = false;
        if (System.getProperty("bdd6502.apu.trace","").toLowerCase().equals("true")) {
            apuEnableDebug = true;
        }
    }

    public void addDevice(MemoryBus device) {
        if (device != null) {
            externalDevices.add(device);
        }

        propertiesUpdated();
    }

    public void enableDebugData() throws IOException {
        debugData = new PrintWriter(new FileWriter("target/debugDataJustUserPort.txt"));
        debugData.println("; Automatically created by UserPortTo24BitAddress");
        debugData.println("d0");

        propertiesUpdated();
    }

    public void setEnableAPU(DisplayBombJack display, APUData data) {
        enableAPU = true;
        displayBombJack = display;
        this.apuData = data;
        externalDevices.add(apuData);

        propertiesUpdated();
    }

    public void setSimpleMode(boolean simpleMode) {
        this.simpleMode = simpleMode;
    }

    public void setAdd32Bit1Mode(boolean add32Bit1Mode) {
        this.add32Bit1Mode = add32Bit1Mode;
    }

    int CIA1Registers[] = new int[16];
    int CIA2TimerAControl = 0;
    @Override
    public void write(int address, int data) throws MemoryAccessException {
        int register = address & 0x10f;
        switch (register) {
            case 0x000:
            case 0x001:
            case 0x002:
            case 0x003:
            case 0x004:
            case 0x005:
            case 0x006:
            case 0x007:
            case 0x008:
            case 0x009:
            case 0x00a:
            case 0x00b:
            case 0x00c:
            case 0x00d:
            case 0x00e:
            case 0x00f:
                CIA1Registers[register] = data;
                break;
            case 0x100: // CIA2PortASerialBusVICBank
                if (add32Bit1Mode) {
                    bus24CIA2PortASerialBusVICBank = data & registerDDRPortA;

                    if ((bus32Latches[7] & kbus32_latch7_ResetDone) == kbus32_latch7_ResetDone) {
                        // Reset done
                        switch (bus32Latches[7] & kbus32_latch7_SelectMask) {
                            case kbus32_latch7_Passthrough:
                                break;
                            case kbus32_latch7_RAM:
                                return;
                            case kbus32_latch7_PassthroughDisable:
                                return;
                            case kbus32_latch7_Disabled:
                                return;
                        }
                    }
                }

                if ((registerDDRPortA & 0x04) == 0x04) {
                    if ((data & 0x04) == 0) {
                        if (simpleMode) {
                            simpleModeLastMEWR = false;
                        } else {
                            bus24SetPA2Low();
                            if (busTrace) {
                                System.out.println("Bus24 reset");
                            }
                        }
                    } else {
                        if (simpleMode) {
                            if (!simpleModeLastMEWR) {
                                simpleModeLastMEWR = true;
                                writeMemoryBusWithState(bus24Bytes[3]);

                                if (busTrace) {
                                    System.out.println("Bus24 simple write : ebs " + bus24Bytes[0] + " : addrl " + bus24Bytes[1] + " : addrh " + bus24Bytes[2] + " : data " + bus24Bytes[3]);
                                }
                            }
                        } else {
                            bus24SetPA2High();
                            if (busTrace) {
                                System.out.println("Bus24 ready");
                            }
                        }
                    }
                }
                break;
            case 0x101: // CIA2PortBRS232
                if (registerDDRPortB == 0xff) {
                    if (add32Bit1Mode) {
                        Bus32LatchAddressCalculate();

                        if ((bus32Latches[7] & kbus32_latch7_ResetDone) == kbus32_latch7_ResetDone) {
                            if (bus32LatchAddress == 7) {
                                if ((bus32Latches[bus32LatchAddress] & kbus32_latch7_FastDMAStart) == 0 && (data & kbus32_latch7_FastDMAStart) == kbus32_latch7_FastDMAStart) {
                                    // Only start on the positive edge
                                    bus32FastDMAStart = true;
                                }
                            }
                        }

                        bus32Latches[bus32LatchAddress] = data;

                        Bus32ApplyLogic();

                        if ((bus32Latches[7] & kbus32_latch7_ResetDone) == kbus32_latch7_ResetDone) {
                            // Reset done
                            if (bus32LatchAddress == 3) {
                                Bus32CalculateOffsets();
                                bus32CurrentAddress+=bus32AddAddress;
                                Bus32OffsetsToLatches();
                            }
                            else if (bus32LatchAddress == 6) {
                                Bus32CalculateOffsets();
                                for (MemoryInternal memory: bus32MemoryBlocks) {
                                    if (memory.includes(bus32CurrentAddress)) {
                                        memory.getMemory().write(bus32CurrentAddress - memory.startAddress,data);
                                    }
                                }
                                bus32CurrentAddress++;
                                Bus32OffsetsToLatches();
                            }
                            else if (bus32LatchAddress == 4) {
                                if ((bus32Latches[7] & kbus32_latch7_SelectMask) == kbus32_latch7_PassthroughDisable) {
                                    bus24WriteByte(data);
                                } else {
                                    throw new MemoryAccessException("Trying to write to latch 4 but kbus32_latch7_PassthroughDisable is not set");
                                }
                            }

                            switch (bus32Latches[7] & kbus32_latch7_SelectMask) {
                                case kbus32_latch7_Passthrough:
                                    break;
                                case kbus32_latch7_RAM:
                                case kbus32_latch7_PassthroughDisable:
                                    if ((bus32Latches[7] & kbus32_latch7_InternalPA2) == kbus32_latch7_InternalPA2) {
                                        bus24SetPA2High();
                                    } else {
                                        bus24SetPA2Low();
                                    }
                                    return;
                                case kbus32_latch7_Disabled:
                                    return;
                            }

                        }
                    }

                    if (simpleMode) {
                        boolean clock = (data & 0x08) == 0x08;
                        if (!clock) {
                            simpleModeLastLatchCLK = false;
                        } else {
                            if (!simpleModeLastLatchCLK) {
                                simpleModeLastLatchCLK = true;
                                switch (data & 0x07) {
                                    case 0x00:
                                        bus24Bytes[0] = (bus24Bytes[0] & 0xf0) | ((data >> 4) & 0x0f);
                                        break;
                                    case 0x01:
                                        bus24Bytes[0] = (bus24Bytes[0] & 0x0f) | (data & 0x0f0);
                                        break;

                                    case 0x02:
                                        bus24Bytes[1] = (bus24Bytes[1] & 0xf0) | ((data >> 4) & 0x0f);
                                        break;
                                    case 0x03:
                                        bus24Bytes[1] = (bus24Bytes[1] & 0x0f) | (data & 0x0f0);
                                        break;

                                    case 0x04:
                                        bus24Bytes[2] = (bus24Bytes[2] & 0xf0) | ((data >> 4) & 0x0f);
                                        break;
                                    case 0x05:
                                        bus24Bytes[2] = (bus24Bytes[2] & 0x0f) | (data & 0x0f0);
                                        break;

                                    case 0x06:
                                        bus24Bytes[3] = (bus24Bytes[3] & 0xf0) | ((data >> 4) & 0x0f);
                                        break;
                                    case 0x07:
                                        bus24Bytes[3] = (bus24Bytes[3] & 0x0f) | (data & 0x0f0);
                                        break;
                                }

                                setMemoryBusState();

                                if (busTrace) {
                                    System.out.println("Bus24 simple received : " + data);
                                }
                            }
                        }
                    } else {
                        bus24WriteByte(data);
                    }
                } else {
                    throw new MemoryAccessException("registerDDRPortB != 0xff on write");
                }
                break;
            case 0x102:
                registerDDRPortA = data;
                break;
            case 0x103:
                registerDDRPortB = data;
                break;
            case 0x10e:
                CIA2TimerAControl = data;
                break;
            default:
                break;
        }
    }

    private void bus24SetPA2High() {
        bus24CountEnabled = true;
    }

    private void bus24SetPA2Low() {
        bus24State = 0;
        bus24CountEnabled = false;
    }

    private void bus24WriteByte(int data) {
        if (bus24CountEnabled) {
            if (bus24State == 3) {
                writeMemoryBusWithState(data);
                bus24Bytes[1]++;
                if (bus24Bytes[1] == 256) {
                    bus24Bytes[1] = 0;
                    bus24Bytes[2]++;
                }
            } else {
                bus24Bytes[bus24State] = data;
                if (busTrace) {
                    System.out.println("Bus24 received " + bus24State + " : " + data);
                }
                bus24State++;
            }
        }
    }

    private void Bus32OffsetsToLatches() {
        bus32Latches[0] = bus32CurrentAddress & 0xff;
        bus32Latches[1] = (bus32CurrentAddress >> 8) & 0xff;
        bus32Latches[2] = (bus32CurrentAddress >> 16) & 0xff;
    }

    private void Bus32CalculateOffsets() {
        bus32CurrentAddress = bus32Latches[0] | (bus32Latches[1] << 8) | (bus32Latches[2] << 16);
        bus32AddAddress = bus32Latches[8] | (bus32Latches[9] << 8) | (bus32Latches[10] << 16);
    }

    private void Bus32CalculateDMACounter() {
        bus32FastDMACounter = bus32Latches[11] | (bus32Latches[12] << 8);
    }

    private void Bus32DMACounterToLatches() {
        bus32Latches[11] = bus32FastDMACounter & 0xff;
        bus32Latches[12] = (bus32FastDMACounter >> 8) & 0xff;
    }

    private void Bus32LatchAddressCalculate() {
        // Decode in LSB bit order:
        // .SP1 = $01
        // .SP2 = $02
        // .SERIALATN = $04
        // .PA2 = $08

        bus32LatchAddress = 0;
        // SP1
        if ((CIA1Registers[0x0e] & 0x40) == 0) {
            bus32LatchAddress |= 0x01;
        }
        // SP2
        if ((CIA2TimerAControl & 0x40) == 0) {
            bus32LatchAddress |= 0x02;
        }
        // SERIALATN
        if ((registerDDRPortA & 0x08) == 0x08) {
            if ((bus24CIA2PortASerialBusVICBank & 0x08) == 0) {
                bus32LatchAddress |= 0x04;
            }
        } else {
            // Input to tri-state = float high
            bus32LatchAddress |= 0x04;
        }
        // PA2
        if ((registerDDRPortA & 0x04) == 0x04) {
            if ((bus24CIA2PortASerialBusVICBank & 0x04) == 0x04) {
                bus32LatchAddress |= 0x08;
            }
        } else {
            // Input to tri-state = float high
            bus32LatchAddress |= 0x08;
        }
    }

    private void Bus32ApplyLogic() {
        // Check the internal logic based on latches
        if ( (bus32Latches[7] & kbus32_latch7_ResetDone) == 0) {
            bus32Latches[0] = 0;
            bus32Latches[1] = 0;
            bus32Latches[2] = 0;
            bus32Latches[8] = 0;
            bus32Latches[9] = 0;
            bus32Latches[10] = 0;
            bus32Latches[11] = 0;
            bus32Latches[12] = 0;
            bus32FastDMACounter = 0;
            bus32FastDMAStart = false;
        }
    }

    public void writeMemoryBusWithState(int data) {
        emitMemoryDebugForUserport(bus24Bytes[1] | (bus24Bytes[2] << 8) , bus24Bytes[0] , data);


        for (MemoryBus device : externalDevices) {
            device.writeData(bus24Bytes[1] | (bus24Bytes[2] << 8), bus24Bytes[0], data);
        }

        // To emulate the latched write passthrough from the APU its execution needs to be delayed, the RAM contention is used to emulate this
        if (apuData != null) {
            apuData.setBusContention(8 * setAPUMemoryClockDivider);
        }
    }

    public void emitMemoryDebugForUserport(int address, int addressEx , int data) {
        if (displayBombJack != null) {
            if (debugData != null && displayBombJack.isEnableDisplay() && !displayBombJack.getVBlank()) {
//                debugData.println("d$0");
                debugData.printf("w$ff03ff00,$%02x%02x%02x00\n", displayBombJack.getDisplayV() & 0xff, (displayBombJack.getDisplayH() >> 8) & 0x01, displayBombJack.getDisplayH() & 0xff);
//                debugData.println("d$0");
            }
        }

        if (debugData != null) {
            debugData.printf("d$%04x%02x%02x\n", address, addressEx, data);
            debugData.flush();
        }
    }

    // Only really used by the simple memory bus interface
    // The counting memory bus interface does not assert the memory bus when memory is not being accessed
    public void setMemoryBusState() {
        for (MemoryBus device : externalDevices) {
            device.setAddressBus(bus24Bytes[1] | (bus24Bytes[2] << 8), bus24Bytes[0]);
        }
    }

    @Override
    public int read(int address, boolean logRead) throws MemoryAccessException {
        int register = address & 0x10f;
        switch (register) {
            default:
                return 0;

            case 0x000:
            case 0x001:
            case 0x002:
            case 0x003:
            case 0x004:
            case 0x005:
            case 0x006:
            case 0x007:
            case 0x008:
            case 0x009:
            case 0x00a:
            case 0x00b:
            case 0x00c:
            case 0x00d:
            case 0x00e:
            case 0x00f:
                return CIA1Registers[register];
            case 0x101:
                if (!logRead) {
                    // CPU emulation does a fake read before a write. Which might be technically correct for RAM but this is the user port, which complicates error detection with the DDR.
                    return 0xff;
                }

                int toReturn = 0xff;
                if (registerDDRPortB == 0x00) {
                    if (add32Bit1Mode) {
                        Bus32LatchAddressCalculate();

                        Bus32ApplyLogic();

                        if ((bus32Latches[7] & kbus32_latch7_ResetDone) == kbus32_latch7_ResetDone) {
                            // Reset done
                            if (bus32LatchAddress == 3) {
                                Bus32CalculateOffsets();
                                bus32CurrentAddress+=bus32AddAddress;
                                Bus32OffsetsToLatches();
                            }
                            else if (bus32LatchAddress == 5) {
                                Bus32CalculateOffsets();
                                for (MemoryInternal memory : bus32MemoryBlocks) {
                                    if (memory.includes(bus32CurrentAddress)) {
                                        try {
                                            toReturn = memory.getMemory().read(bus32CurrentAddress - memory.startAddress(), false);
                                        } catch (MemoryAccessException ignored) {
                                        }
                                        break;
                                    }
                                }
                                bus32CurrentAddress++;
                                Bus32OffsetsToLatches();


                                if ((bus32Latches[7] & kbus32_latch7_SelectMask) == kbus32_latch7_RAM) {
                                    bus24WriteByte(toReturn);
                                }
                            }
                            else if (bus32LatchAddress == 13) {
                                toReturn = 0;
                                if (bus32FastDMAStart) {
                                    toReturn |= 0x01;
                                }
                                return toReturn;
                            }

                            switch (bus32Latches[7] & kbus32_latch7_SelectMask) {
                                case kbus32_latch7_Passthrough:
                                    break;
                                case kbus32_latch7_RAM:
                                    return 0xff;
                                case kbus32_latch7_PassthroughDisable:
                                    return 0xff;
                                case kbus32_latch7_Disabled:
                                    return 0xff;
                            }
                        }
                    }
                } else {
                    throw new MemoryAccessException("registerDDRPortB != 0 on read");
                }
                return toReturn;
            case 0x10d:
                // A double read can happen, once for the log, once for the CPU
                // We ignore the first read
                int ret = 0x0;
                for (MemoryBus device : externalDevices) {
                    if (device.extEXTWANTIRQ()) {
                        // Drive high
                        ret = 0x10;
                    }
                }
                if (logRead) {
                    for (MemoryBus device : externalDevices) {
                        device.resetExtEXTWANTIRQ();
                    }
                }
                return ret;
        }
    }

    @Override
    public String toString() {
        return "";
    }

    // From: Assembly
    // ; Instructions, can be combined
    final long kAPU_Reset_ADDRB1		= 0b00000000000000000000000000000001;
    final long kAPU_Reset_PC			= 0b00000000000000000000000000000010;
    final long kAPU_InternalMEWR		= 0b00000000000000000000000000000100;
    final long kAPU_WaitForEqualsHV 	= 0b00000000000000000000000000001000;
    final long kAPU_Incr_ADDRB1			= 0b00000000000000000000000000100000;
    final long kAPU_Incr_EADDR			= 0b00000000000000000000000001000000;
    final long kAPU_ExternalMEWR		= 0b00000000000000000000000010000000;		// ; This is timed to pulse low on the PCINCR (cycle 5)
    final long kAPU_Load_EBS			= 0b00000000000000000000000100000000;
    final long kAPU_Load_EADDRLo		= 0b00000000000000000000001000000000;
    final long kAPU_Load_EADDRHi		= 0b00000000000000000000010000000000;
    final long kAPU_Load_Wait24			= 0b00000000000000000000100000000000;
    final long kAPU_Load_Wait16			= 0b00000000000000000001000000000000;
    final long kAPU_Load_Wait8			= 0b00000000000000000010000000000000;

    // ; New instructions
    final long kAPU_SelectEBS2EADDR2	= 0b00000000000000000100000000000000;
    final long kAPU_Load_EBS2			= 0b00000000000000001000000000000000;
    // ; 16th bit
    final long kAPU_Load_EADDR2Lo		= 0b00000000000000010000000000000000;
    final long kAPU_Load_EADDR2Hi		= 0b00000000000000100000000000000000;
    final long kAPU_Incr_EADDR2			= 0b00000000000001000000000000000000;

    // ; Do not combine these IDataSelect values
    final int kAPU_IDataSelectRAM	        = 0b00000000000000000000000000000000;
    final int kAPU_IDataSelectReg0	        = 0b00000000000010000000000000000000;
    final int kAPU_IDataSelectReg1	        = 0b00000000000100000000000000000000;
    final int kAPU_IDataSelectReg2      	= 0b00000000000110000000000000000000;
    final int kAPU_IDataSelectReg3	    	= 0b00000000000000000000000000010000;
    final int kAPU_IDataSelectMemAddReg3	= 0b00000000000010000000000000010000;
    final int kAPU_IDataSelectReg3AddReg4	= 0b00000000000100000000000000010000;
    final int kAPU_IDataSelectReg3SubReg4	= 0b00000000000110000000000000010000;
    final int kAPU_IDataSelectMask	        = 0b00000000000110000000000000010000;

    final long kAPU_IDataRegLoad0	= 0b00000000001000000000000000000000L;
    final long kAPU_IDataRegLoad1	= 0b00000000010000000000000000000000L;
    final long kAPU_IDataRegLoad2	= 0b00000000100000000000000000000000L;

    final long kAPU_ADDRB2Select	= 0b00000001000000000000000000000000L;
    final long kAPU_Incr_ADDRB2		= 0b00000010000000000000000000000000L;
    final long kAPU_ADDRB1Load16	= 0b00000100000000000000000000000000L;
    final long kAPU_ADDRB2Load16	= 0b00001000000000000000000000000000L;
    final long kAPU_PCLoad16		= 0b00010000000000000000000000000000L;
    final long kAPU_SkipIfEQ		= 0b00100000000000000000000000000000L;
    final long kAPU_IDataRegLoad3	= 0b01000000000000000000000000000000L;
    final long kAPU_IDataRegLoad4	= 0b10000000000000000000000000000000L;

    public boolean isApuEnableDebug() {
        return apuEnableDebug;
    }

    public void setApuEnableDebug(boolean apuEnableDebug) {
        this.apuEnableDebug = apuEnableDebug;
    }

    boolean apuEnableDebug = false;

    public void setSetAPUClockDivider(int setAPUClockDivider) {
        this.setAPUClockDivider = setAPUClockDivider;
    }

    // These are the defaults from the hardware
    int setAPUClockDivider = 1; // VIDCLK
    int countAPUClockDivider = 0;

    public void setSetAPUMemoryClockDivider(int setAPUMemoryClockDivider) {
        this.setAPUMemoryClockDivider = setAPUMemoryClockDivider;
    }

    int setAPUMemoryClockDivider = 2; // VIDCLK / 2

    double clockAccumulator = 0.0;
    final double clockAccumulatorByteTime = 4.0;    // TODO: Adjust to match hardware
    public void calculatePixel() {
        if (add32Bit1Mode) {
            clockAccumulator += bus32clockMultiplier;
            while (clockAccumulator >= clockAccumulatorByteTime) {
                clockAccumulator -= clockAccumulatorByteTime;

                if (bus32FastDMAStart) {
                    int toReturn = 0xff;
                    if ((bus32Latches[7] & kbus32_latch7_SelectMask) == kbus32_latch7_RAM) {
                        // Read the internal RAM
                        Bus32CalculateOffsets();
                        for (MemoryInternal memory : bus32MemoryBlocks) {
                            if (memory.includes(bus32CurrentAddress)) {
                                try {
                                    toReturn = memory.getMemory().read(bus32CurrentAddress - memory.startAddress(), false);
                                    break;
                                } catch (MemoryAccessException ignored) {
                                }
                            }
                        }
                        bus32CurrentAddress++;
                        Bus32OffsetsToLatches();
                    } else {
                        assertThat("DMA in progress but kbus32_latch7_RAM is not set, so this DMA doesn't make sense!" , false);
                    }
                    bus24WriteByte(toReturn);
                    Bus32CalculateDMACounter();
                    bus32FastDMACounter++;
                    if ((bus32FastDMACounter & 0xffff) == 0) {
                        bus32FastDMAStart = false;
                    }
                    Bus32DMACounterToLatches();
                }
            }
        }
        if (!enableAPU || apuData == null) {
            return;
        }

        apuData.ageContention();

        countAPUClockDivider--;
        if (countAPUClockDivider <= 0) {
            countAPUClockDivider = setAPUClockDivider;
            calculatePixelInternal();
        }
    }
    public void calculatePixelInternal() {
        if (!enableAPU || apuData == null) {
            return;
        }

        apuCheckTriggers();

        boolean apuEnable = apuCheckRegisters();
        if (!apuEnable) {
            return;
        }

        // Emulate the instruction and data arbitration by pausing on this specific cycle
        if (apuInstuctionSchedule == 0 && apuData.hasContention()) {
            return;
        }

        // Perform any memory writes at this time, for this emulation do the logic now
        if (apuInstuctionSchedule == 4) {
            apuHandleInstructionSchedule4();
        }

        if (!apuHitWait) {
            // Pause counting...
            apuInstuctionSchedule = 5;
            return;
        }

        apuInstuctionSchedule++;

        if (apuInstuctionSchedule >= kCyclesPerInstruction) {
            // Start whatever is the next instruction
            apuInstuctionSchedule = 0;
        }
    }

    static public volatile boolean apuInstructionExecuted = false;

    private void apuHandleInstructionSchedule4() {
        apuInstructionExecuted = true;
        int currentPC = apuPC;
        long instruction = getAPUInstruction(apuPC);
        long originalInstruction = instruction;
        boolean wasSkipped = false;

        if (MemoryBus.addressActive(instruction , kAPU_InternalMEWR)) {
            assertThat("kAPU_InternalMEWR needs kAPU_IDataSelect* to be the same for the current and previous instruction" , apuPreviousGotIDataSelect , is(equalTo(instruction & kAPU_IDataSelectMask)));
            assertThat("kAPU_InternalMEWR should not select internal memory with kAPU_IDataSelectRAM while writing internal memory" , (int)(instruction & kAPU_IDataSelectMask), is(not(equalTo(kAPU_IDataSelectRAM))));
        }

        if (MemoryBus.addressActive(instruction , kAPU_SkipIfEQ)) {
            assertThat("kAPU_SkipIfEQ needs kAPU_IDataSelect* to be the same for the current and previous instruction" , apuPreviousGotIDataSelect , is(equalTo(instruction & kAPU_IDataSelectMask)));
            // The test in the schematic uses the pre-latch signal, so this occurs first
            if (apuPreviousGotByte == 0) {
                instruction = 0;
                wasSkipped = true;
            }
        }

        if (MemoryBus.addressActive(instruction , kAPU_WaitForEqualsHV)) {
            assertThat("kAPU_WaitForEqualsHV should not write internal or external memory in the current or previous instruction", !(MemoryBus.addressActive(last_instruction, kAPU_InternalMEWR | kAPU_ExternalMEWR) && MemoryBus.addressActive(instruction, kAPU_InternalMEWR | kAPU_ExternalMEWR)));
        }
        assertThat("kAPU_Incr_ADDRB1 should not be held high for more than one instruction", !(MemoryBus.addressActive(last_instruction , kAPU_Incr_ADDRB1) && MemoryBus.addressActive(instruction , kAPU_Incr_ADDRB1)));
        assertThat("kAPU_Incr_ADDRB2 should not be held high for more than one instruction", !(MemoryBus.addressActive(last_instruction , kAPU_Incr_ADDRB2) && MemoryBus.addressActive(instruction , kAPU_Incr_ADDRB2)));
        assertThat("kAPU_Incr_EADDR should not be held high for more than one instruction", !(MemoryBus.addressActive(last_instruction , kAPU_Incr_EADDR) && MemoryBus.addressActive(instruction , kAPU_Incr_EADDR)));
        assertThat("kAPU_Incr_EADDR2 should not be held high for more than one instruction", !(MemoryBus.addressActive(last_instruction , kAPU_Incr_EADDR2) && MemoryBus.addressActive(instruction , kAPU_Incr_EADDR2)));

        // Due to "Any Incr is timed at cycle 6+3" the increment is handled after the write and load pulses
        // Here we test the last_instruction and current instruction before the instruction is going to be executed
        if (MemoryBus.addressActive(last_instruction , kAPU_Incr_ADDRB1) && !MemoryBus.addressActive(instruction , kAPU_Incr_ADDRB1)) {
            apuADDRB1++;
            apuADDRB1 &= 0x3fff;
        }
        if (MemoryBus.addressActive(last_instruction , kAPU_Incr_ADDRB2) && !MemoryBus.addressActive(instruction , kAPU_Incr_ADDRB2)) {
            apuADDRB2++;
            apuADDRB2 &= 0x3fff;
        }

        if (MemoryBus.addressActive(last_instruction , kAPU_Incr_EADDR) && !MemoryBus.addressActive(instruction , kAPU_Incr_EADDR)) {
            apuEADDR++;
        }

        if (MemoryBus.addressActive(last_instruction , kAPU_Incr_EADDR2) && !MemoryBus.addressActive(instruction , kAPU_Incr_EADDR2)) {
            apuEADDR2++;
        }

        apuPC++;
        apuPC &= 0x07ff;

        if (MemoryBus.addressActive(instruction , kAPU_Reset_ADDRB1)) {
            apuADDRB1 = 0;
        }

        if (MemoryBus.addressActive(instruction , kAPU_Reset_PC)) {
            apuPC = 0;
        }

        if (MemoryBus.addressActive(instruction , kAPU_WaitForEqualsHV)) {
            apuHitWait = false;
        }

        int gotByte;
        gotByte = apuGetCurrentSelectedByte(instruction);

        apuHandleMemoryWrite(instruction, gotByte);

        apuHandleLoadPulses(instruction, gotByte);

        last_currentPC = currentPC;
        last_instruction = instruction;
        last_originalInstruction = originalInstruction;
        last_wasSkipped = wasSkipped;

        if (apuEnableDebug) {
            String output = getDebugOutputLastState();
            System.out.println(output);
        }
    }

    public void signalVBlank() {
        if (debugData != null && displayBombJack != null) {
            debugData.println("; Frame " + displayBombJack.getFrameNumberForSync());
            debugData.println("d$0");
            debugData.println("^-$01");
            debugData.println("d$0");
            debugData.flush();
        }
    }

    private int getAPUInstruction(int thePC) {
        thePC = thePC & 0x7ff;
        return (apuData.getApuInstructions()[thePC * 4] & 0xff) | ((apuData.getApuInstructions()[(thePC * 4) + 1] & 0xff) << 8) | ((apuData.getApuInstructions()[(thePC * 4) + 2] & 0xff) << 16) | ((apuData.getApuInstructions()[(thePC * 4) + 3] & 0xff) << 24);
    }

    int last_currentPC;
    long last_instruction;
    long last_originalInstruction;
    boolean last_wasSkipped;

    public String getDebugOutputLastState() {
        return apuEmitDebug(last_currentPC, last_instruction, last_originalInstruction, last_wasSkipped);
    }

    public String disassembleAPUInstructionAt(int currentPC) {
        long instruction = getAPUInstruction(currentPC);
        String instructionString = disassembleAPUInstruction(instruction, false);
        return HexUtil.intToHexSpaces((int) instruction) + "    " + instructionString;
    }

    public byte[] getAPUDataMemory() {
        return apuData.getApuData();
    }

    public boolean isWaitState() {
        if (apuPC == 0) {
            return true;
        }
        long instruction = getAPUInstruction(apuPC);
        if (MemoryBus.addressActive(instruction, kAPU_WaitForEqualsHV)) {
            return true;
        }
        return false;
    }

    private String apuEmitDebug(int currentPC, long instruction, long originalInstruction, boolean wasSkipped) {
        String instructionString = disassembleAPUInstruction(originalInstruction, wasSkipped);

        String ebs1Select = "";
        String ebs2Select = "";
        if (MemoryBus.addressActive(originalInstruction, kAPU_SelectEBS2EADDR2)) {
            ebs1Select = "";
            ebs2Select = "*";
        } else {
            ebs1Select = "*";
            ebs2Select = "";
        }

        String selectADDRB1 = "";
        String selectADDRB2 = "";
        String selectReg0 = "";
        String selectReg1 = "";
        String selectReg2 = "";
        String selectReg3 = "";
        String selectReg4 = "";

        // Switch doesn't work with long... sigh...
        int iDataSelectDebug = (int) instruction & kAPU_IDataSelectMask;
        switch (iDataSelectDebug) {
            default:
            case kAPU_IDataSelectRAM:
                if (MemoryBus.addressActive(instruction, kAPU_ADDRB2Select)) {
                    selectADDRB2 = "*";
                } else {
                    selectADDRB1 = "*";
                }
                break;
            case kAPU_IDataSelectReg0:
                selectReg0 = "*";
                break;
            case kAPU_IDataSelectReg1:
                selectReg1 = "*";
                break;
            case kAPU_IDataSelectReg2:
                selectReg2 = "*";
                break;
            case kAPU_IDataSelectReg3:
                selectReg3 = "*";
                break;
            case kAPU_IDataSelectMemAddReg3:
                selectReg3 = "*";
                break;
            case kAPU_IDataSelectReg3AddReg4:
                selectReg3 = "*";
                selectReg4 = "*";
                break;
            case kAPU_IDataSelectReg3SubReg4:
                selectReg3 = "*";
                selectReg4 = "*";
                break;
        }

        String output = "";
        String isSchedule = " : schedule ";
        if (apuData.hasContention()) {
            isSchedule = " : schedule <<Wait RAM>> ";
        }
        output += kAPUDEBUG + ">> PC: " + Integer.toHexString(currentPC) + isSchedule + apuInstuctionSchedule + " : " + instructionString.trim() + "\n";
        output += kAPUDEBUG + "Wait8: " + Integer.toHexString(apuWait8) + " Wait16: " + Integer.toHexString(apuWait16) + " Wait24: " + Integer.toHexString(apuWait24) + " : ";
        output += "RH8: " + Integer.toHexString(displayBombJack.getDisplayH() & 0xff) + " RH16: " + Integer.toHexString((displayBombJack.getDisplayH() >> 8) & 0xff) + " RV24: " + Integer.toHexString(displayBombJack.getDisplayV() & 0xff) + "\n";
        output += kAPUDEBUG + selectADDRB1 + "ADDRB1: " + Integer.toHexString(apuADDRB1) + " Contents: " + Integer.toHexString(apuData.getApuData()[(apuADDRB1-1) & 0x1fff] & 0xff) + " >" + Integer.toHexString(apuData.getApuData()[apuADDRB1 & 0x1fff] & 0xff) + "< " + Integer.toHexString(apuData.getApuData()[(apuADDRB1 + 1) & 0x1fff] & 0xff) + " : ";
        output += selectADDRB2 + "ADDRB2: " + Integer.toHexString(apuADDRB2) + " Contents: " + Integer.toHexString(apuData.getApuData()[(apuADDRB2-1) & 0x1fff] & 0xff) + " >" + Integer.toHexString(apuData.getApuData()[apuADDRB2 & 0x1fff] & 0xff) + "< " + Integer.toHexString(apuData.getApuData()[(apuADDRB2 + 1) & 0x1fff] & 0xff) + "\n";
        output += kAPUDEBUG + ebs1Select + "EBS: " + Integer.toHexString(apuEBS) + " "+ebs1Select+"EADDR: " + Integer.toHexString(apuEADDR) + " "+ebs2Select+"EBS2: " + Integer.toHexString(apuEBS2) + " "+ebs2Select+"EADDR2: " + Integer.toHexString(apuEADDR2) + "\n";
        output += kAPUDEBUG + selectReg0 + "DataReg0: " + Integer.toHexString(apuDataReg[0]) + " " + selectReg1 + "DataReg1: " + Integer.toHexString(apuDataReg[1]) + " " + selectReg2 +"DataReg2: " + Integer.toHexString(apuDataReg[2]) + " " + selectReg3 +"DataReg3: " + Integer.toHexString(apuDataReg[3]) + " " + selectReg4 + "DataReg4: " + Integer.toHexString(apuDataReg[4]) + "\n";
        return output;
    }

    private String disassembleAPUInstruction(long originalInstruction, boolean wasSkipped) {
        String instructionString = "";
        if (wasSkipped) {
            instructionString += "** Skipped ** ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_Reset_ADDRB1)) {
            instructionString += "Reset_ADDRB1 ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_Reset_PC)) {
            instructionString += "Reset_PC ";
        }

        if (MemoryBus.addressActive(originalInstruction, kAPU_InternalMEWR)) {
            instructionString += "InternalMEWR ";
        }

        if (MemoryBus.addressActive(originalInstruction, kAPU_WaitForEqualsHV)) {
            instructionString += "WaitForEqualsHV ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_Incr_ADDRB1)) {
            instructionString += "Incr_ADDRB1 ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_Incr_EADDR)) {
            instructionString += "Incr_EADDR ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_ExternalMEWR)) {
            instructionString += "ExternalMEWR ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_Load_EBS)) {
            instructionString += "Load_EBS ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_Load_EADDRLo)) {
            instructionString += "Load_EADDRLo ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_Load_EADDRHi)) {
            instructionString += "Load_EADDRHi ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_Load_Wait24)) {
            instructionString += "Load_Wait24 ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_Load_Wait16)) {
            instructionString += "Load_Wait16 ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_Load_Wait8)) {
            instructionString += "Load_Wait8 ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_SelectEBS2EADDR2)) {
            instructionString += "SelectEBS2EADDR2 ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_Load_EBS2)) {
            instructionString += "Load_EBS2 ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_Load_EADDR2Lo)) {
            instructionString += "Load_EADDR2Lo ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_Load_EADDR2Hi)) {
            instructionString += "Load_EADDR2Hi ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_Incr_EADDR2)) {
            instructionString += "Incr_EADDR2 ";
        }

        if (MemoryBus.addressActive(originalInstruction, kAPU_IDataRegLoad0)) {
            instructionString += "IDataRegLoad0 ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_IDataRegLoad1)) {
            instructionString += "IDataRegLoad1 ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_IDataRegLoad2)) {
            instructionString += "IDataRegLoad2 ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_IDataRegLoad3)) {
            instructionString += "IDataRegLoad3 ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_IDataRegLoad4)) {
            instructionString += "IDataRegLoad4 ";
        }


        if (MemoryBus.addressActive(originalInstruction, kAPU_ADDRB2Select)) {
            instructionString += "ADDRB2Select ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_Incr_ADDRB2)) {
            instructionString += "Incr_ADDRB2 ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_ADDRB1Load16)) {
            instructionString += "ADDRB1Load16 ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_ADDRB2Load16)) {
            instructionString += "ADDRB2Load16 ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_PCLoad16)) {
            instructionString += "PCLoad16 ";
        }
        if (MemoryBus.addressActive(originalInstruction, kAPU_SkipIfEQ)) {
            instructionString += "kAPU_SkipIfEQ ";
        }


        // Switch doesn't work with long... sigh...
        int iDataSelectDebug = (int) originalInstruction & kAPU_IDataSelectMask;
        switch (iDataSelectDebug) {
            default:
            case kAPU_IDataSelectRAM:
                // Default option, can be left silent
//                instructionString += "IDataSelectRAM ";
                break;
            case kAPU_IDataSelectReg0:
                instructionString += "IDataSelectReg0 ";
                break;
            case kAPU_IDataSelectReg1:
                instructionString += "IDataSelectReg1 ";
                break;
            case kAPU_IDataSelectReg2:
                instructionString += "IDataSelectReg2 ";
                break;
            case kAPU_IDataSelectReg3:
                instructionString += "IDataSelectReg3 ";
                break;
            case kAPU_IDataSelectMemAddReg3:
                instructionString += "IDataSelectMemAddReg3 ";
                break;
            case kAPU_IDataSelectReg3AddReg4:
                instructionString += "IDataSelectReg3AddReg4 ";
                break;
            case kAPU_IDataSelectReg3SubReg4:
                instructionString += "IDataSelectReg3SubReg4 ";
                break;
        }

        return instructionString;
    }

    private void apuHandleLoadPulses(long instruction, int gotByte) {
        // Due to the load pulses being timed later in the schematic, these loads are handled last
        if (MemoryBus.addressActive(instruction, kAPU_IDataRegLoad0)) {
            apuDataReg[0] = gotByte;
        }
        if (MemoryBus.addressActive(instruction, kAPU_IDataRegLoad1)) {
            apuDataReg[1] = gotByte;
        }
        if (MemoryBus.addressActive(instruction, kAPU_IDataRegLoad2)) {
            apuDataReg[2] = gotByte;
        }
        if (MemoryBus.addressActive(instruction, kAPU_IDataRegLoad3)) {
            apuDataReg[3] = gotByte;
        }
        if (MemoryBus.addressActive(instruction, kAPU_IDataRegLoad4)) {
            apuDataReg[4] = gotByte;
        }

        if (MemoryBus.addressActive(instruction, kAPU_Load_EBS)) {
            apuEBS = gotByte;
        }

        if (MemoryBus.addressActive(instruction, kAPU_Load_EBS2)) {
            apuEBS2 = gotByte;
        }

        if (MemoryBus.addressActive(instruction, kAPU_Load_EADDRLo)) {
            apuEADDR = (apuEADDR & 0xff00) | gotByte;
        }

        if (MemoryBus.addressActive(instruction, kAPU_Load_EADDRHi)) {
            apuEADDR = (apuEADDR & 0xff) | (gotByte << 8);
        }

        if (MemoryBus.addressActive(instruction, kAPU_Load_EADDR2Lo)) {
            apuEADDR2 = (apuEADDR2 & 0xff00) | gotByte;
        }

        if (MemoryBus.addressActive(instruction, kAPU_Load_EADDR2Hi)) {
            apuEADDR2 = (apuEADDR2 & 0xff) | (gotByte << 8);
        }

        if (MemoryBus.addressActive(instruction, kAPU_Load_Wait24)) {
            apuWait24 = gotByte;
        }

        if (MemoryBus.addressActive(instruction, kAPU_Load_Wait16)) {
            apuWait16 = gotByte;
        }

        if (MemoryBus.addressActive(instruction, kAPU_Load_Wait8)) {
            apuWait8 = gotByte;
        }

        if (MemoryBus.addressActive(instruction, kAPU_ADDRB1Load16)) {
            apuADDRB1 = apuDataReg[0] | (apuDataReg[1] << 8);
            apuADDRB1 &= 0x3fff;
        }
        if (MemoryBus.addressActive(instruction, kAPU_ADDRB2Load16)) {
            apuADDRB2 = apuDataReg[0] | (apuDataReg[1] << 8);
            apuADDRB2 &= 0x3fff;
        }

        if (MemoryBus.addressActive(instruction, kAPU_PCLoad16)) {
            // The PC load is latched on the clock input, so the address of the next instruction is the same as the loaded value
            apuPC = apuDataReg[0] | (apuDataReg[1] << 8);
            apuPC &= 0x07ff;
        }
    }

    private void apuHandleMemoryWrite(long instruction, int gotByte) {
        // This write is timed later in the schematic, so it handled later along with the loads
        if (MemoryBus.addressActive(instruction, kAPU_ExternalMEWR)) {
            for (MemoryBus device : externalDevices) {
                if (MemoryBus.addressActive(instruction, kAPU_SelectEBS2EADDR2)) {
                    device.writeData(apuEADDR2, apuEBS2, gotByte);
                } else {
                    device.writeData(apuEADDR, apuEBS, gotByte);
                }
            }
        }

        if (MemoryBus.addressActive(instruction, kAPU_InternalMEWR)) {
            if (MemoryBus.addressActive(instruction, kAPU_ADDRB2Select)) {
                apuData.getApuData()[apuADDRB2] = (byte) gotByte;
            } else {
                apuData.getApuData()[apuADDRB1] = (byte) gotByte;
            }
        }

    }

    private int apuGetCurrentSelectedByte(long instruction) {
        int gotByte;
        long iDataSelect = instruction & kAPU_IDataSelectMask;
        switch ((int)iDataSelect) {
            default:
            case kAPU_IDataSelectRAM:
                if (MemoryBus.addressActive(instruction, kAPU_ADDRB2Select)) {
                    gotByte = apuData.getApuData()[apuADDRB2] & 0xff;
                } else {
                    gotByte = apuData.getApuData()[apuADDRB1] & 0xff;
                }
                break;
            case kAPU_IDataSelectReg0:
                gotByte = apuDataReg[0];
                break;
            case kAPU_IDataSelectReg1:
                gotByte = apuDataReg[1];
                break;
            case kAPU_IDataSelectReg2:
                gotByte = apuDataReg[2];
                break;
            case kAPU_IDataSelectReg3:
                gotByte = apuDataReg[3];
                break;
            case kAPU_IDataSelectMemAddReg3:
                if (MemoryBus.addressActive(instruction, kAPU_ADDRB2Select)) {
                    gotByte = apuData.getApuData()[apuADDRB2] & 0xff;
                } else {
                    gotByte = apuData.getApuData()[apuADDRB1] & 0xff;
                }
                gotByte = (gotByte + apuDataReg[3]) & 0xff;
                break;
            case kAPU_IDataSelectReg3AddReg4:
                gotByte = (apuDataReg[3] + apuDataReg[4]) & 0xff;
                break;
            case kAPU_IDataSelectReg3SubReg4:
                gotByte = (apuDataReg[3] - apuDataReg[4]) & 0xff;
                break;
        }
        apuPreviousGotByte = gotByte;
        apuPreviousGotIDataSelect = iDataSelect;
        return gotByte;
    }

    private boolean apuCheckRegisters() {
        boolean apuEnable = false;
        byte controlRegister = apuData.getApuRegisters()[0];
        apuEnable = MemoryBus.addressActive(controlRegister , 0x02);

        if (!MemoryBus.addressActive(controlRegister , 0x01)) {
            apuInstuctionSchedule = 0;
            apuHitWait = true;
            apuIntercepting = false;

            apuADDRB1 = 0;
            apuADDRB2 = 0;
            apuPC = 0;
            apuEBS = 0;
            apuEADDR = 0;
            apuEBS2 = 0;
            apuEADDR2 = 0;
            apuWait8 = 0;
            apuWait16 = 0;
            apuWait24 = 0;
            apuDataReg[0] = 0;
            apuDataReg[1] = 0;
            apuDataReg[2] = 0;
            apuDataReg[3] = 0;
            apuDataReg[4] = 0;
        }
        return apuEnable;
    }

    private void apuCheckTriggers() {
        int displayH = displayBombJack.getDisplayH();
        int displayV = displayBombJack.getDisplayV();
        // Note:  For apuWait16 filter out _VIDCLK lo/hi values from just after RH8
        if (apuWait24 == displayV && (apuWait16 & 0x01) == (displayH & 0x100)>>8 && apuWait8 == (displayH & 0xff)) {
            apuHitWait = true;
        }
    }

    public void addMemoryAt(int iAddress, int iSize) throws MemoryRangeException {
        bus32MemoryBlocks.add(new MemoryInternal(iAddress,iAddress + iSize));
    }

    public void randomiseData(Random rand) {
        for (MemoryInternal ram : bus32MemoryBlocks) {
            MemoryBus.randomiseHelper(rand , ram.getMemory().getDmaAccess());
        }
    }

    public void setClockMultiplier(double clockMultiplier) {
        bus32clockMultiplier = clockMultiplier;
    }
}
