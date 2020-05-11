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
    int bus24Bytes[] = new int[3];
    List<MemoryBus> externalDevices = new LinkedList<>();
    boolean vsyncTriggered = false;

    public UserPortTo24BitAddress(Scenario scenario) throws MemoryRangeException {
        super(0xdd00, 0xdd0f, "UserPortTo24BitAddress");
        mScenario = scenario;
    }

    public void addDevice(MemoryBus device) {
        if (device != null) {
            externalDevices.add(device);
        }
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
                        bus24State = 0;
                        bus24CountEnabled = false;
                        if (busTrace) {
                            System.out.println("Bus24 reset");
                        }
                    } else {
                        bus24CountEnabled = true;
                        if (busTrace) {
                            System.out.println("Bus24 ready");
                        }
                    }
                }
                break;
            case 1:
                if (registerDDRPortB == 0xff) {
                    if (bus24CountEnabled) {
                        if (bus24State == 3) {
                            for (MemoryBus device : externalDevices) {
                                device.writeData(bus24Bytes[1] | (bus24Bytes[2] << 8), bus24Bytes[0], data);
                            }
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

    @Override
    public int read(int address, boolean logRead) throws MemoryAccessException {
        int register = address & 0x0f;
        switch (register) {
            default:
                return 0;

            case 0x0d:
                // Float high...
                int ret = 0x10;
                for (MemoryBus device : externalDevices) {
                    if (!device.extEXTWANTIRQ()) {
                        // Drive low
                        ret = 0x00;
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
