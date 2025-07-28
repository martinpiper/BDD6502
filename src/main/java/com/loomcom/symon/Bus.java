/*
 * Copyright (c) 2014 Seth J. Morabito <web@loomcom.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.loomcom.symon;

import com.loomcom.symon.devices.Device;
import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;

import java.util.*;

/**
 * The Bus ties the whole thing together, man.
 */
public class Bus {

    // The default address at which to load programs
    public static int DEFAULT_LOAD_ADDRESS = 0x0200;

    // By default, our bus starts at 0, and goes up to 64K
    private int startAddress = 0x0000;
    private int endAddress = 0xffff;

    // The CPU
    private Cpu cpu;

    private int theProcessorPort;

    private boolean processorPort;
    // Ordered sets of IO devices, associated with their priority
    private Map<Integer, SortedSet<Device>> deviceMap;
    // an array for quick lookup of adresses, brute-force style
    private Device[][] deviceAddressArrayRead;
    private Device[][] deviceAddressArrayWrite;

    public Bus(int size) {
        this(0, size - 1);
    }


    public Bus(int startAddress, int endAddress) {
        this.deviceMap = new HashMap<Integer, SortedSet<Device>>();
        this.startAddress = startAddress;
        this.endAddress = endAddress;
        processorPort = false;
        theProcessorPort = 0x17;    // Default value from the C64
    }

    public void setProcessorPort() {
        processorPort = true;
    }

    public boolean getProcessorPort() {
        return processorPort;
    }

    public int startAddress() {
        return startAddress;
    }

    public int endAddress() {
        return endAddress;
    }

    private void buildDeviceAddressArray() {
        int size = (this.endAddress - this.startAddress) + 1;
        deviceAddressArrayRead = new Device[16][size];
        deviceAddressArrayWrite = new Device[16][size];

        // getDevices() provides an OrderedSet with devices ordered by priorities
        for (int pp = 0 ; pp < 16 ; pp++) {
            for (Device device : getDevices()) {
                MemoryRange range = device.getMemoryRange();
                for (int address = range.startAddress; address <= range.endAddress; ++address) {
                    if (processorPort) {
                        switch (pp) {
                            case 0b000:
                                // Choose RAM only
                                if (range.startAddress == 0) {
                                    deviceAddressArrayRead[pp][address - this.startAddress] = device;
                                    deviceAddressArrayWrite[pp][address - this.startAddress] = device;
                                }
                                break;
                            case 0b001:
                                // Choose RAM only, ignore char ROM
                                if (range.startAddress == 0) {
                                    deviceAddressArrayRead[pp][address - this.startAddress] = device;
                                    deviceAddressArrayWrite[pp][address - this.startAddress] = device;
                                }
                                break;
                            case 0b010:
                                if (range.startAddress == 0 || range.startAddress == 0xe000) {
                                    deviceAddressArrayRead[pp][address - this.startAddress] = device;
                                }
                                if (range.startAddress == 0) {
                                    deviceAddressArrayWrite[pp][address - this.startAddress] = device;
                                }
                                break;
                            case 0b011:
                                if (range.startAddress == 0 || range.startAddress == 0xa000 || range.startAddress == 0xe000) {
                                    deviceAddressArrayRead[pp][address - this.startAddress] = device;
                                }
                                if (range.startAddress == 0) {
                                    deviceAddressArrayWrite[pp][address - this.startAddress] = device;
                                }
                                break;
                            case 0b100:
                                // Choose RAM only
                                if (range.startAddress == 0) {
                                    deviceAddressArrayRead[pp][address - this.startAddress] = device;
                                    deviceAddressArrayWrite[pp][address - this.startAddress] = device;
                                }
                                break;
                            case 0b101:
                                if (range.startAddress == 0 || (range.startAddress >= 0xd000 && range.startAddress < 0xe000)) {
                                    deviceAddressArrayRead[pp][address - this.startAddress] = device;
                                    deviceAddressArrayWrite[pp][address - this.startAddress] = device;
                                }
                                break;
                            case 0b110:
                                if (range.startAddress == 0 || (range.startAddress >= 0xd000 && range.startAddress < 0xe000) || range.startAddress == 0xe000) {
                                    deviceAddressArrayRead[pp][address - this.startAddress] = device;
                                }
                                if (range.startAddress == 0 || (range.startAddress >= 0xd000 && range.startAddress < 0xe000)) {
                                    deviceAddressArrayWrite[pp][address - this.startAddress] = device;
                                }
                                break;
                            case 0b111:
                                if (range.startAddress == 0 || range.startAddress == 0xa000  || (range.startAddress >= 0xd000 && range.startAddress < 0xe000) || range.startAddress == 0xe000) {
                                    deviceAddressArrayRead[pp][address - this.startAddress] = device;
                                }
                                if (range.startAddress == 0 || (range.startAddress >= 0xd000 && range.startAddress < 0xe000)) {
                                    deviceAddressArrayWrite[pp][address - this.startAddress] = device;
                                }
                                break;
                        }
                    } else {
                        deviceAddressArrayRead[pp][address - this.startAddress] = device;
                        deviceAddressArrayWrite[pp][address - this.startAddress] = device;
                    }
                }
            }
        }

    }

    boolean crtActive = false;
    int crtType , crtExrom , crtGame;

    public void addCRTInfo(int type, int exrom, int game) {
        crtActive = true;
        crtType = type;
        crtExrom = exrom;
        crtGame = game;
    }
    ArrayList<Device> crtChips8000 = new ArrayList<>();
    ArrayList<Device> crtChipsA000 = new ArrayList<>();
    ArrayList<Device> crtChipsE000 = new ArrayList<>();
    public void addCRTChip(Device device , int bank) {
        if (device.getMemoryRange().startAddress == 0x8000) {
            crtChips8000.add(bank, device);
        }
        if (device.getMemoryRange().startAddress == 0xa000) {
            crtChipsA000.add(bank, device);
        }
        if (device.getMemoryRange().startAddress == 0xe000) {
            crtChipsE000.add(bank, device);
        }
    }

    /**
     * Add a device to the bus.
     *
     * @param device
     * @param priority
     * @throws MemoryRangeException
     */
    public void addDevice(Device device, int priority) throws MemoryRangeException {

        MemoryRange range = device.getMemoryRange();
        if (range.startAddress() < this.startAddress || range.startAddress() > this.endAddress) {
            throw new MemoryRangeException("start address of device " + device.getName() + " does not fall within the address range of the bus");
        }
        if (range.endAddress() < this.startAddress || range.endAddress() > this.endAddress) {
            throw new MemoryRangeException("end address of device " + device.getName() + " does not fall within the address range of the bus");
        }


        SortedSet<Device> deviceSet = deviceMap.get(priority);
        if (deviceSet == null) {
            deviceSet = new TreeSet<Device>();
            deviceMap.put(priority, deviceSet);
        }

        device.setBus(this);
        deviceSet.add(device);
        buildDeviceAddressArray();
    }

    /**
     * Add a device to the bus. Throws a MemoryRangeException if the device overlaps with any others.
     *
     * @param device
     * @throws MemoryRangeException
     */
    public void addDevice(Device device) throws MemoryRangeException {
        addDevice(device, 0);
    }


    /**
     * Remove a device from the bus.
     *
     * @param device
     */
    public void removeDevice(Device device) {
        for (SortedSet<Device> deviceSet : deviceMap.values()) {
            deviceSet.remove(device);
        }
        buildDeviceAddressArray();
    }

    public void addCpu(Cpu cpu) {
        this.cpu = cpu;
        cpu.setBus(this);
    }

    /**
     * Returns true if the memory map is full, i.e., there are no
     * gaps between any IO devices.  All memory locations map to some
     * device.
     */
    public boolean isComplete() {
        if (deviceAddressArrayRead == null) {
            buildDeviceAddressArray();
        }

        for (int address = startAddress; address <= endAddress; ++address) {
            if (deviceAddressArrayRead[address - startAddress] == null) {
                return false;
            }
            if (deviceAddressArrayWrite[address - startAddress] == null) {
                return false;
            }
        }

        return true;
    }

    public int read(int address) throws MemoryAccessException {
        return read(address, true);
    }

    int crtBank = 0;
    int crtEasyFlashControl = 0;
    public int read(int address, boolean logRead) throws MemoryAccessException {
        if (processorPort && 0x0001 == address) {
            // Return the contents of the processor port if it is active
            return theProcessorPort;
        }
        if (processorPort && crtActive) {
            int pp = theProcessorPort & 0b111;
            if (0b111 == pp || 0b011 == pp) {
                if (address >= 0x8000 && address <= 0x9fff) {
                    if (crtBank < crtChips8000.size()) {
                        Device chip = crtChips8000.get(crtBank);
                        if (null != chip) {
                            return chip.read(address - 0x8000, logRead);
                        }
                    }
                }
            }
        }
        Device d = deviceAddressArrayRead[theProcessorPort & 0b111][address - this.startAddress];
        if (d != null) {
            MemoryRange range = d.getMemoryRange();
            int devAddr = address - range.startAddress();
            return d.read(devAddr, logRead) & 0xff;
        }

        throw new MemoryAccessException("Bus read failed. No device at address " + String.format("$%04X", address));
    }

    public void write(int address, int value) throws MemoryAccessException {
        if (processorPort) {
            if (0x0001 == address) {
//                System.out.println("Processor port Write detected: of " + String.format("$%02X", value));
                theProcessorPort = value;
                return;
            }
            int pp = theProcessorPort & 0b111;
            if (0b111 == pp || 0b110 == pp || 0b101 == pp) {
                if (0xd000 <= address && address <= 0xdfff) {
//                    System.out.println("IO Write detected: " + String.format("$%04X", address) + " of " + String.format("$%02X", value));
                    // gmod2
                    if (crtActive && crtType == 0x13 && address == 0xde00) {
                        crtBank = value;
                    }
                    // EF3
                    if (crtActive && crtType == 0x20 && address == 0xde00) {
                        crtBank = value;
                    }
                    if (crtActive && crtType == 0x20 && address == 0xde02) {
                        crtEasyFlashControl = value;
                    }
                }
            }
        }
        Device d = deviceAddressArrayWrite[theProcessorPort & 0b111][address - this.startAddress];
        if (d != null) {
            MemoryRange range = d.getMemoryRange();
            int devAddr = address - range.startAddress();
            d.write(devAddr, value);
            return;
        }

        throw new MemoryAccessException("Bus write failed. No device at address " + String.format("$%04X", address));
    }

    public void assertIrq() {
        if (cpu != null) {
            cpu.assertIrq();
        }
    }

    public void clearIrq() {
        if (cpu != null) {
            cpu.clearIrq();
        }
    }

    public void assertNmi() {
        if (cpu != null) {
            cpu.assertNmi();
        }
    }

    public void clearNmi() {
        if (cpu != null) {
            cpu.clearNmi();
        }
    }

    public SortedSet<Device> getDevices() {
        // create an ordered set of devices, ordered by device priorities
        SortedSet<Device> devices = new TreeSet<Device>();

        List<Integer> priorities = new ArrayList<Integer>(deviceMap.keySet());
        Collections.sort(priorities);

        for (int priority : priorities) {
            SortedSet<Device> deviceSet = deviceMap.get(priority);
            for (Device device : deviceSet) {
                devices.add(device);
            }
        }

        return devices;
    }

    public Cpu getCpu() {
        return cpu;
    }

    public void loadProgram(int... program) throws MemoryAccessException {
        int address = getCpu().getProgramCounter();
        int i = 0;
        for (int d : program) {
            write(address + i++, d);
        }
    }
}
