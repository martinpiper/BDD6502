package com.loomcom.symon.devices;

import com.bdd6502.DisplayBombJack;
import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;
import cucumber.api.Scenario;

public class UserPortTo24BitAddress extends Device {
    private Scenario mScenario = null;
    int registerDDRPortA = 0;
    int registerDDRPortB = 0;
    int bus24State = 0;
    boolean bus24CountEnabled = false;
    int bus24Bytes[] = new int[3];
    DisplayBombJack displayBombJack;
    boolean vsyncTriggered = false;

    public UserPortTo24BitAddress(Scenario scenario, DisplayBombJack displayBombJack) throws MemoryRangeException {
        super(0xdd00, 0xdd0f, "UserPortTo24BitAddress");
        mScenario = scenario;
        this.displayBombJack = displayBombJack;
    }

    @Override
    public void write(int address, int data) throws MemoryAccessException {
        int register = address & 0x0f;
        switch (register) {
            case 0:
                if ((registerDDRPortA & 0x04) == 0x04) {
                    if ((data & 0x04) == 0) {
                        bus24State = 0;
                        bus24CountEnabled = false;
                        System.out.println("Bus24 reset");
                    } else {
                        bus24CountEnabled = true;
                        System.out.println("Bus24 ready");
                    }
                }
                break;
            case 1:
                if (registerDDRPortB == 0xff) {
                    if (bus24CountEnabled) {
                        if (bus24State == 3) {
                            displayBombJack.writeData(bus24Bytes[1] | (bus24Bytes[2] << 8), bus24Bytes[0], data);
                            bus24Bytes[1]++;
                            if (bus24Bytes[1] == 256) {
                                bus24Bytes[1] = 0;
                                bus24Bytes[2]++;
                            }
                        } else {
                            bus24Bytes[bus24State] = data;
                            System.out.println("Bus24 received " + bus24State + " : " + data);
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
                int ret;
                if (displayBombJack.getVSync()) {
                    ret = 0x10;
                } else {
                    ret = 0x00;
                }
                return ret;
        }
    }

    @Override
    public String toString() {
        return "";
    }
}
