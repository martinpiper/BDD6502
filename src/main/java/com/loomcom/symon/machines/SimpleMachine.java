/*
 * Copyright (c) 2014 Seth J. Morabito <web@loomcom.com>
 *                    Maik Merten <maikmerten@googlemail.com>
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

package com.loomcom.symon.machines;

import com.loomcom.symon.Bus;
import com.loomcom.symon.Cpu;
import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.MemoryRangeException;

/**
 * A SimpleMachine is the simplest 6502 implementation possible - it
 * consists solely of RAM and a CPU. This machine is primarily useful
 * for running 6502 functional tests or debugging by hand.
 */
public class SimpleMachine implements Machine {

    private static final int BUS_BOTTOM = 0x0000;
    private static final int BUS_TOP    = 0xffff;

    private final Bus bus;
    private final Memory ram;
    private final Cpu cpu;

    public SimpleMachine() throws MemoryRangeException {
        this.bus = new Bus(BUS_BOTTOM, BUS_TOP);
        this.ram = new Memory(BUS_BOTTOM, BUS_TOP, false);
        this.cpu = new Cpu();

        bus.addCpu(cpu);
        bus.addDevice(ram);
    }

    public Bus getBus() {
        return bus;
    }

    public Cpu getCpu() {
        return cpu;
    }

    public Memory getRam() {
        return ram;
    }

    public Memory getRom() {
        return null;
    }

    public void setRom(Memory rom) throws MemoryRangeException {
        // No-op
    }

    public int getRomBase() {
        return 0;
    }

    public int getRomSize() {
        return 0;
    }

    public int getMemorySize() {
        return BUS_TOP + 1;
    }

    public String getName() {
        return "Simple";
    }
}
