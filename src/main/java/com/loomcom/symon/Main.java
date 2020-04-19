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


package com.loomcom.symon;

import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.machines.Machine;
import com.loomcom.symon.machines.SimpleMachine;

public class Main {

    static private Machine machine;

    /**
     * Main entry point to the simulator.
     *
     * @param args
     */
    public static void main(String args[]) throws Exception {

        machine = new SimpleMachine();
        Memory mem = machine.getRam();
        mem.fill(0x00);
        machine.getBus().write(Cpu.RST_VECTOR_L, 0x00);
        machine.getBus().write(Cpu.RST_VECTOR_H, 0x04);

        // Some simple code for sei, inc $d020, jmp $401
        int addr = 0x400;
        machine.getBus().write(addr++, 0x78);
        machine.getBus().write(addr++, 0xee);
        machine.getBus().write(addr++, 0x20);
        machine.getBus().write(addr++, 0xd0);
        machine.getBus().write(addr++, 0x4c);
        machine.getBus().write(addr++, 0x01);
        machine.getBus().write(addr++, 0x04);

        machine.getCpu().reset();

        // Add some blank stack slide data so any RTI or RTS will eventually get to $0000 and get detected
        int i;
        for (i = 0; i < 16; i++) {
            machine.getCpu().stackPush(0);
        }

        for (i = 0; i < 10; i++) {
            machine.getCpu().step();
            System.out.println(machine.getCpu().getCpuState().toTraceEvent());
        }
    }
}
