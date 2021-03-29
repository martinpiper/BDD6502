package com.loomcom.symon.devices;

import com.bdd6502.APUData;
import com.bdd6502.AudioExpansion;
import com.bdd6502.DisplayBombJack;
import com.bdd6502.MemoryBus;
import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;
import cucumber.api.Scenario;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

public class UserPortTo24BitAddress extends Device {
    private Scenario mScenario = null;
    int registerDDRPortA = 0;
    int registerDDRPortB = 0;
    int bus24State = 0;
    boolean bus24CountEnabled = false;
    int bus24Bytes[] = new int[4];
    List<MemoryBus> externalDevices = new LinkedList<>();
    boolean simpleMode = false;
    boolean simpleModeLastMEWR = true;
    boolean simpleModeLastLatchCLK = true;
    PrintWriter debugData = null;
    boolean enableAPU = false;
    DisplayBombJack displayBombJack = null;
    APUData apuData = null;
    final int kPixelsPerInstruction = 6;
    int apuInstuctionSchedule = 0;
    boolean apuIntercepting = false;

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
    int apuDataReg[] = new int[3];


    public UserPortTo24BitAddress(Scenario scenario) throws MemoryRangeException {
        super(0xdd00, 0xdd0f, "UserPortTo24BitAddress");
        mScenario = scenario;
    }

    public void addDevice(MemoryBus device) {
        if (device != null) {
            externalDevices.add(device);
        }
    }

    public void enableDebugData() throws IOException {
        debugData = new PrintWriter(new FileWriter("target/debugDataJustUserPort.txt"));
        debugData.println("; Automatically created by UserPortTo24BitAddress");
        debugData.println("d0");
    }

    public void setEnableAPU(DisplayBombJack display, APUData data) {
        enableAPU = true;
        displayBombJack = display;
        this.apuData = data;
        externalDevices.add(apuData);
    }

    public void setSimpleMode(boolean simpleMode) {
        this.simpleMode = simpleMode;
    }

    @Override
    public void write(int address, int data) throws MemoryAccessException {
        boolean busTrace = false;
        String trace = System.getProperty("bdd6502.bus24.trace");
        if (null != trace && trace.indexOf("true") != -1) {
            busTrace = true;
        }

        int register = address & 0x0f;
        switch (register) {
            case 0:
                if ((registerDDRPortA & 0x04) == 0x04) {
                    if ((data & 0x04) == 0) {
                        if (simpleMode) {
                            simpleModeLastMEWR = false;
                        } else {
                            bus24State = 0;
                            bus24CountEnabled = false;
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
                            bus24CountEnabled = true;
                            if (busTrace) {
                                System.out.println("Bus24 ready");
                            }
                        }
                    }
                }
                break;
            case 1:
                if (registerDDRPortB == 0xff) {
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
                }
                break;
            case 2:
                registerDDRPortA = data;
                break;
            case 3:
                registerDDRPortB = data;
                break;
            default:
                break;
        }
    }

    public void writeMemoryBusWithState(int data) {
        if (debugData != null) {
            debugData.printf("d$%04x%02x%02x\n", bus24Bytes[1] | (bus24Bytes[2] << 8), bus24Bytes[0], data);
        }


        for (MemoryBus device : externalDevices) {
            device.writeData(bus24Bytes[1] | (bus24Bytes[2] << 8), bus24Bytes[0], data);
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
        int register = address & 0x0f;
        switch (register) {
            default:
                return 0;

            case 0x0d:
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
    final int kAPU_Reset_ADDRB1		= 0b00000000000000000000000000000001;
    final int kAPU_Reset_PC			= 0b00000000000000000000000000000010;
    final int kAPU_InterceptBus		= 0b00000000000000000000000000000100;
    final int kAPU_WaitForEqualsHV	= 0b00000000000000000000000000001000;
    final int kAPU_Reset_EBSEADDR	= 0b00000000000000000000000000010000;
    final int kAPU_Incr_ADDRB1		= 0b00000000000000000000000000100000;
    final int kAPU_Incr_EADDR		= 0b00000000000000000000000001000000;
    final int kAPU_ExternalMEWR		= 0b00000000000000000000000010000000;		// ; This is timed to pulse low on the PCINCR (cycle 3)
    final int kAPU_Load_EBS			= 0b00000000000000000000000100000000;
    final int kAPU_Load_EADDRLo		= 0b00000000000000000000001000000000;
    final int kAPU_Load_EADDRHi		= 0b00000000000000000000010000000000;
    final int kAPU_Load_Wait24		= 0b00000000000000000000100000000000;
    final int kAPU_Load_Wait16		= 0b00000000000000000001000000000000;
    final int kAPU_Load_Wait8		= 0b00000000000000000010000000000000;

    // ; New instructions
    final int kAPU_SelectEBS2EADDR2	= 0b00000000000000000100000000000000;
    final int kAPU_Load_EBS2		= 0b00000000000000001000000000000000;
    // ; 16th bit
    final int kAPU_Load_EADDR2Lo	= 0b00000000000000010000000000000000;
    final int kAPU_Load_EADDR2Hi	= 0b00000000000000100000000000000000;
    final int kAPU_Incr_EADDR2		= 0b00000000000001000000000000000000;

    // ; Do not combine these IDataSelect values
    final int kAPU_IDataSelectRAM	= 0b00000000000000000000000000000000;
    final int kAPU_IDataSelectReg0	= 0b00000000000010000000000000000000;
    final int kAPU_IDataSelectReg1	= 0b00000000000100000000000000000000;
    final int kAPU_IDataSelectReg2	= 0b00000000000110000000000000000000;
    final int kAPU_IDataSelectMask	= 0b00000000000110000000000000000000;

    final int kAPU_IDataRegLoad0	= 0b00000000001000000000000000000000;
    final int kAPU_IDataRegLoad1	= 0b00000000010000000000000000000000;
    final int kAPU_IDataRegLoad2	= 0b00000000100000000000000000000000;

    final int kAPU_ADDRB2Select		= 0b00000001000000000000000000000000;
    final int kAPU_Incr_ADDRB2		= 0b00000010000000000000000000000000;
    final int kAPU_ADDRB1Load16		= 0b00000100000000000000000000000000;
    final int kAPU_ADDRB2Load16		= 0b00001000000000000000000000000000;



    public void calculatePixel() {
        if (!enableAPU || apuData == null) {
            return;
        }
        apuData.ageContention();

        int displayH = displayBombJack.getDisplayH();
        int displayV = displayBombJack.getDisplayV();
        if (apuWait24 == displayV && apuWait16 == (displayH & 0x100)>>8 && apuWait8 == (displayH & 0xff)) {
            apuHitWait = true;
        }

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
        }

        if (!apuEnable) {
            return;
        }

        if (apuData.hasContention()) {
            apuInstuctionSchedule = 0;
            return;
        }

        // Perform any memory writes at this time, for this emulation do the logic now
        if (apuInstuctionSchedule == 4) {
            int instruction = (apuData.getApuInstructions()[apuPC*4] & 0xff) | ((apuData.getApuInstructions()[(apuPC*4)+1] & 0xff) << 8) | ((apuData.getApuInstructions()[(apuPC*4)+2] & 0xff) << 16) | ((apuData.getApuInstructions()[(apuPC*4)+3] & 0xff) << 24);
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

            if (MemoryBus.addressActive(instruction , kAPU_Incr_ADDRB1)) {
                apuADDRB1++;
            }
            if (MemoryBus.addressActive(instruction , kAPU_Incr_ADDRB2)) {
                apuADDRB2++;
            }

            if (MemoryBus.addressActive(instruction , kAPU_Incr_EADDR)) {
                apuEADDR++;
            }

            if (MemoryBus.addressActive(instruction , kAPU_Incr_EADDR2)) {
                apuEADDR2++;
            }

            if (MemoryBus.addressActive(instruction , kAPU_Reset_EBSEADDR)) {
                apuEADDR = 0;
                apuEBS = 0;
            }

            if (MemoryBus.addressActive(instruction , kAPU_InterceptBus)) {
                apuIntercepting = true;
                for (MemoryBus device : externalDevices) {
                    if (MemoryBus.addressActive(instruction , kAPU_SelectEBS2EADDR2)) {
                        device.setAddressBus(apuEADDR2, apuEBS2);
                    } else {
                        device.setAddressBus(apuEADDR, apuEBS);
                    }
                }
            } else {
                if (apuIntercepting) {
                    apuIntercepting = false;
                    for (MemoryBus device : externalDevices) {
                        device.setAddressBus(0, 0);
                    }
                }
            }

            int iDataSelect = instruction & kAPU_IDataSelectMask;
            int gotByte;
            switch (iDataSelect) {
                default:
                case kAPU_IDataSelectRAM:
                    if (MemoryBus.addressActive(instruction , kAPU_ADDRB2Select)) {
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
            }

            if (MemoryBus.addressActive(instruction , kAPU_ExternalMEWR)) {
                if (!MemoryBus.addressActive(instruction , kAPU_InterceptBus)) {
                    System.out.println("kAPU_ExternalMEWR without kAPU_InterceptBus at address " + apuPC);
                }

                for (MemoryBus device : externalDevices) {
                    if (MemoryBus.addressActive(instruction , kAPU_SelectEBS2EADDR2)) {
                        device.writeData(apuEADDR2, apuEBS2, gotByte);
                    } else {
                        device.writeData(apuEADDR, apuEBS, gotByte);
                    }
                }
            }

            if (MemoryBus.addressActive(instruction , kAPU_IDataRegLoad0)) {
                apuDataReg[0] = gotByte;
            }
            if (MemoryBus.addressActive(instruction , kAPU_IDataRegLoad1)) {
                apuDataReg[1] = gotByte;
            }
            if (MemoryBus.addressActive(instruction , kAPU_IDataRegLoad2)) {
                apuDataReg[2] = gotByte;
            }

            if (MemoryBus.addressActive(instruction , kAPU_Load_EBS)) {
                apuEBS = gotByte;
            }

            if (MemoryBus.addressActive(instruction , kAPU_Load_EBS2)) {
                apuEBS2 = gotByte;
            }

            if (MemoryBus.addressActive(instruction , kAPU_Load_EADDRLo)) {
                apuEADDR = (apuEADDR & 0xff00) | gotByte;
            }

            if (MemoryBus.addressActive(instruction , kAPU_Load_EADDRHi)) {
                apuEADDR = (apuEADDR & 0xff) | (gotByte << 8);
            }

            if (MemoryBus.addressActive(instruction , kAPU_Load_EADDR2Lo)) {
                apuEADDR2 = (apuEADDR2 & 0xff00) | gotByte;
            }

            if (MemoryBus.addressActive(instruction , kAPU_Load_EADDR2Hi)) {
                apuEADDR2 = (apuEADDR2 & 0xff) | (gotByte << 8);
            }

            if (MemoryBus.addressActive(instruction , kAPU_Load_Wait24)) {
                apuWait24 = gotByte;
            }

            if (MemoryBus.addressActive(instruction , kAPU_Load_Wait16)) {
                apuWait16 = gotByte;
            }

            if (MemoryBus.addressActive(instruction , kAPU_Load_Wait8)) {
                apuWait8 = gotByte;
            }

            if (MemoryBus.addressActive(instruction , kAPU_ADDRB1Load16)) {
                apuADDRB1 = apuDataReg[0] | (apuDataReg[1] << 8);
            }
            if (MemoryBus.addressActive(instruction , kAPU_ADDRB2Load16)) {
                apuADDRB2 = apuDataReg[0] | (apuDataReg[1] << 8);
            }
        }

        if (!apuHitWait) {
            // Pause counting...
            apuInstuctionSchedule = 5;
            return;
        }

        apuInstuctionSchedule++;

        if (apuInstuctionSchedule >= kPixelsPerInstruction) {
            // Start whatever is the next instruction
            apuInstuctionSchedule = 0;
        }
    }
}
