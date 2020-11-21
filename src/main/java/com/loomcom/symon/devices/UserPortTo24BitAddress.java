package com.loomcom.symon.devices;

import com.bdd6502.AudioExpansion;
import com.bdd6502.DisplayBombJack;
import com.bdd6502.MemoryBus;
import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;
import cucumber.api.Scenario;

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
    boolean vsyncTriggered = false;
    boolean simpleMode = false;
    boolean simpleModeLastMEWR = true;
    boolean simpleModeLastLatchCLK = true;

    public UserPortTo24BitAddress(Scenario scenario) throws MemoryRangeException {
        super(0xdd00, 0xdd0f, "UserPortTo24BitAddress");
        mScenario = scenario;
    }

    public void addDevice(MemoryBus device) {
        if (device != null) {
            externalDevices.add(device);
        }
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
        for (MemoryBus device : externalDevices) {
            device.writeData(bus24Bytes[1] | (bus24Bytes[2] << 8), bus24Bytes[0], data);
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
}
