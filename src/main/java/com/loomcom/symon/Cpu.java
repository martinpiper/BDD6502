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

import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.util.HexUtil;

import java.util.Deque;
import java.util.LinkedList;

/**
 * This class provides a simulation of the MOS 6502 CPU's state machine.
 * A simple interface allows this 6502 to read and write to a simulated bus,
 * and exposes some of the internal state for inspection and debugging.
 */
public class Cpu implements InstructionTable {

    /* Process status register mnemonics */
    public static final int P_CARRY = 0x01;
    public static final int P_ZERO = 0x02;
    public static final int P_IRQ_DISABLE = 0x04;
    public static final int P_DECIMAL = 0x08;
    public static final int P_BREAK = 0x10;
    // Bit 5 is always '1'
    public static final int P_OVERFLOW = 0x40;
    public static final int P_NEGATIVE = 0x80;

    public static final int Extra_XTest = 0x01;
    public static final int Extra_YTest = 0x02;
    public static final int Extra_ATest = 0x04;
    public static final int Extra_BRK = 0xFF;

    // NMI vector
    public static final int NMI_VECTOR_L = 0xfffa;
    public static final int NMI_VECTOR_H = 0xfffb;
    // Reset vector
    public static final int RST_VECTOR_L = 0xfffc;
    public static final int RST_VECTOR_H = 0xfffd;
    // IRQ vector
    public static final int IRQ_VECTOR_L = 0xfffe;
    public static final int IRQ_VECTOR_H = 0xffff;
    /* The CPU state */
    private static final CpuState state = new CpuState();
    /* Simulated behavior */
    private static CpuBehavior behavior;
    /* The Bus */
    private Bus bus;
    /* Scratch space for addressing mode and effective address
     * calculations */
    private int irAddressMode; // Bits 3-5 of IR:  [ | | |X|X|X| | ]
    private int irOpMode;      // Bits 6-7 of IR:  [ | | | | | |X|X]
    private int effectiveAddress;

    /* Internal scratch space */
    private int lo = 0, hi = 0;  // Used in address calculation
    private int tmp; // Temporary storage

    /* start time of op execution, needed for speed simulation */
    private long opBeginTime;

    private int extraStatus;
    private boolean failOnBreak;
    private boolean exitOnBreak;
    private boolean overclock;
    private boolean dontPopTestStackNextRTS;

    private int clockCycles = 0;
    private Deque<RegisterStackSet> testStack;
    private Deque<Boolean> testStackNeedToPop;


    /**
     * Construct a new CPU.
     */
    public Cpu() {
        this(CpuBehavior.NMOS_WITH_INDIRECT_JMP_BUG);
    }

    public Cpu(CpuBehavior behavior) {
        this.behavior = behavior;
        failOnBreak = false;
        exitOnBreak = false;
        overclock = false;
        dontPopTestStackNextRTS = false;
        testStackNeedToPop = new LinkedList<Boolean>();
        testStack = new LinkedList<RegisterStackSet>();
        testStackNeedToPop.addFirst(Boolean.FALSE);
    }

    public void resetClockCycles() {
        clockCycles = 0;
    }

    public int getClockCycles() {
        return clockCycles;
    }

    /**
     * Return the Bus that this CPU is associated with.
     */
    public Bus getBus() {
        return bus;
    }

    /**
     * Set the bus reference for this CPU.
     */
    public void setBus(Bus bus) {
        this.bus = bus;
    }

    public CpuBehavior getBehavior() {
        return behavior;
    }

    public void setBehavior(CpuBehavior behavior) {
        this.behavior = behavior;
    }

    /**
     * Reset the CPU to known initial values.
     */
    public void reset() throws MemoryAccessException {
		/* TODO: In reality, the stack pointer could be anywhere
		   on the stack after reset. This non-deterministic behavior might be
           worth while to simulate. */
        state.sp = 0xff;

        // Set the PC to the address stored in the reset vector
        // In this incarnation we do not want to read RAM due to initialised memory checking
        // The CPU is "idle" until it is told to execute something
//		state.pc = address(bus.read(RST_VECTOR_L), bus.read(RST_VECTOR_H));
        // Instead set it to the bottom of memory
        state.pc = 0;

        // Clear instruction register.
        state.ir = 0;

        // Clear status register bits.
        state.carryFlag = false;
        state.zeroFlag = false;
        state.irqDisableFlag = false;
        state.decimalModeFlag = false;
        state.breakFlag = false;
        state.overflowFlag = false;
        state.negativeFlag = false;

        state.irqAsserted = false;

        // Clear illegal opcode trap.
        state.opTrap = false;

        // Reset step counter
        state.stepCounter = 0L;

        // Reset registers.
        state.a = 0;
        state.x = 0;
        state.y = 0;

        extraStatus = 0;
        overclock = false;
    }

    public void setFailOnBreak() {
        failOnBreak = true;
    }

    public void clearFailOnBreak() {
        failOnBreak = false;
    }

    public void setExitOnBreak() {
        exitOnBreak = true;
    }

    public void clearExitOnBreak() {
        exitOnBreak = false;
    }

    public boolean getFailOnBreak() {
        return failOnBreak;
    }

    public boolean getExitOnBreak() {
        return exitOnBreak;
    }

    public int getExtraStatus() {
        return extraStatus;
    }

    public void setOverclock() {
        overclock = true;
    }

    public void initRegisterTestStackIfNeeded() {
        if (testStackNeedToPop.size() == 0) {
            testStackNeedToPop.push(false);
        }
    }

    public void step(int num) throws MemoryAccessException {
        for (int i = 0; i < num; i++) {
            step();
        }
    }

    /**
     * Performs an individual instruction cycle.
     */
    public void step() throws MemoryAccessException {
        state.readByte = 0;
        state.readAddress = 0;
        state.traceShouldShowReadByte = false;
        state.traceShouldShowReadAddress = false;

        opBeginTime = System.nanoTime();
        // Store the address from which the IR was read, for debugging
        state.lastPc = state.pc;

        // Check for Interrupts before doing anything else.
        // This will set the PC and jump to the interrupt vector.
        if (state.nmiAsserted) {
            handleNmi();
        } else if (state.irqAsserted && !getIrqDisableFlag()) {
            handleIrq(state.pc);
        }

        // Fetch memory location for this instruction.
        state.ir = bus.read(state.pc);
        irAddressMode = (state.ir >> 2) & 0x07;
        irOpMode = state.ir & 0x03;

        incrementPC();

        clearOpTrap();

        // Decode the instruction and operands
        state.instSize = Cpu.instructionSizes[state.ir];
        int clockSteps = Cpu.instructionClocks[state.ir];
        clockCycles += clockSteps;
        for (int i = 0; i < state.instSize - 1; i++) {
            state.args[i] = bus.read(state.pc);
            // Increment PC after reading
            incrementPC();
        }

        state.stepCounter++;

        // Get the data from the effective address (if any)
        effectiveAddress = 0;

        switch (irOpMode) {
            case 0:
            case 2:
                switch (irAddressMode) {
                    case 0: // #Immediate
                        break;
                    case 1: // Zero Page
                        effectiveAddress = state.args[0];
                        break;
                    case 2: // Accumulator - ignored
                        break;
                    case 3: // Absolute
                        effectiveAddress = address(state.args[0], state.args[1]);
                        break;
                    case 5: // Zero Page,X / Zero Page,Y
                        if (state.ir == 0x96 || state.ir == 0xb6) {
                            effectiveAddress = zpyAddress(state.args[0]);
                        } else {
                            effectiveAddress = zpxAddress(state.args[0]);
                        }
                        state.traceShouldShowReadAddress = true;
                        break;
                    case 7: // Absolute,X / Absolute,Y
                        if (state.ir == 0xbe) {
                            effectiveAddress = yAddress(state.args[0], state.args[1]);
                        } else {
                            effectiveAddress = xAddress(state.args[0], state.args[1]);
                        }
                        state.traceShouldShowReadAddress = true;
                        break;
                }
                break;
            case 1:
                switch (irAddressMode) {
                    case 0: // (Zero Page,X)
                        tmp = (state.args[0] + state.x) & 0xff;
                        effectiveAddress = address(bus.read(tmp), bus.read(tmp + 1));
                        state.traceShouldShowReadAddress = true;
                        state.traceShouldShowReadByte = true;
                        break;
                    case 1: // Zero Page
                        effectiveAddress = state.args[0];
                        break;
                    case 2: // #Immediate
                        effectiveAddress = -1;
                        break;
                    case 3: // Absolute
                        effectiveAddress = address(state.args[0], state.args[1]);
                        break;
                    case 4: // (Zero Page),Y
                        tmp = address(bus.read(state.args[0]),
                                bus.read((state.args[0] + 1) & 0xff));
                        effectiveAddress = (tmp + state.y) & 0xffff;
                        state.traceShouldShowReadAddress = true;
                        state.traceShouldShowReadByte = true;
                        break;
                    case 5: // Zero Page,X
                        effectiveAddress = zpxAddress(state.args[0]);
                        state.traceShouldShowReadAddress = true;
                        break;
                    case 6: // Absolute, Y
                        effectiveAddress = yAddress(state.args[0], state.args[1]);
                        state.traceShouldShowReadAddress = true;
                        break;
                    case 7: // Absolute, X
                        effectiveAddress = xAddress(state.args[0], state.args[1]);
                        state.traceShouldShowReadAddress = true;
                        break;
                }
                break;
        }

        state.readAddress = effectiveAddress;
        if (state.readAddress >= 0) {
            state.readByte = bus.read(effectiveAddress, false);
        }

        // Execute
        switch (state.ir) {

            /** Single Byte Instructions; Implied and Relative **/
            case 0x00: // BRK - Force Interrupt - Implied
                if (failOnBreak) {
                    extraStatus = Extra_BRK;
                } else if (exitOnBreak) {
                    state.pc = 0;
                } else if (!getIrqDisableFlag()) {
                    handleIrq(state.pc + 1);
                }
                break;
            case 0x02: // PAT - Push A for Test
                if (testStackNeedToPop.peekFirst() == false) {
                    RegisterStackSet set = new RegisterStackSet();
                    set.a = state.a;
                    testStack.addFirst(set);
                    testStackNeedToPop.removeFirst();
                    testStackNeedToPop.addFirst(true);
                } else {
                    RegisterStackSet set = testStack.peekFirst();
                    set.a = state.a;
                }
                break;
            case 0x08: // PHP - Push Processor Status - Implied
                // Break flag is always set in the stack value.
                stackPush(state.getStatusFlag() | 0x10);
                break;
            case 0x10: // BPL - Branch if Positive - Relative
                if (!getNegativeFlag()) {
                    state.pc = relAddress(state.args[0]);
                }
                break;
            case 0x12: // PXT - Push X for Test
                if (testStackNeedToPop.peekFirst() == false) {
                    RegisterStackSet set = new RegisterStackSet();
                    set.x = state.x;
                    testStack.addFirst(set);
                    testStackNeedToPop.removeFirst();
                    testStackNeedToPop.addFirst(Boolean.TRUE);
                } else {
                    RegisterStackSet set = testStack.peekFirst();
                    set.x = state.x;
                }
                break;
            case 0x18: // CLC - Clear Carry Flag - Implied
                clearCarryFlag();
                break;
            case 0x20: // JSR - Jump to Subroutine - Implied
                stackPush((state.pc - 1 >> 8) & 0xff); // PC high byte
                stackPush(state.pc - 1 & 0xff);        // PC low byte
                state.pc = address(state.args[0], state.args[1]);
                testStackNeedToPop.addFirst(Boolean.FALSE);
                break;
            case 0x22: // PYT - Push Y for Test
                if (testStackNeedToPop.peekFirst() == false) {
                    RegisterStackSet set = new RegisterStackSet();
                    set.y = state.y;
                    testStack.addFirst(set);
                    testStackNeedToPop.removeFirst();
                    testStackNeedToPop.addFirst(Boolean.TRUE);
                } else {
                    RegisterStackSet set = testStack.peekFirst();
                    set.y = state.y;
                }
                break;
            case 0x28: // PLP - Pull Processor Status - Implied
                setProcessorStatus(stackPop());
                break;
            case 0x30: // BMI - Branch if Minus - Relative
                if (getNegativeFlag()) {
                    state.pc = relAddress(state.args[0]);
                }
                break;
            case 0x32: //TTA - Test Test A
            {
                RegisterStackSet set = testStack.peekFirst();
                if (set.a != state.a) {
                    extraStatus = Extra_ATest;
                }
            }
            break;
            case 0x38: // SEC - Set Carry Flag - Implied
                setCarryFlag();
                break;
            case 0x40: // RTI - Return from Interrupt - Implied
                setProcessorStatus(stackPop());
                lo = stackPop();
                hi = stackPop();
                setProgramCounter(address(lo, hi));
                break;
            case 0x42: //TTX - Test Test X
            {
                RegisterStackSet set = testStack.peekFirst();
                if (set.x != state.x) {
                    extraStatus = Extra_XTest;
                }
            }
            break;
            case 0x48: // PHA - Push Accumulator - Implied
                stackPush(state.a);
                break;
            case 0x50: // BVC - Branch if Overflow Clear - Relative
                if (!getOverflowFlag()) {
                    state.pc = relAddress(state.args[0]);
                }
                break;
            case 0x52: //TTY - Test Test Y
            {
                RegisterStackSet set = testStack.peekFirst();
                if (set.y != state.y) {
                    extraStatus = Extra_YTest;
                }
            }
            break;
            case 0x58: // CLI - Clear Interrupt Disable - Implied
                clearIrqDisableFlag();
                break;
            case 0x60: // RTS - Return from Subroutine - Implied
                lo = stackPop();
                hi = stackPop();
                setProgramCounter((address(lo, hi) + 1) & 0xffff);
                if (testStackNeedToPop.isEmpty() == false && dontPopTestStackNextRTS == false) {
                    if (testStackNeedToPop.peekFirst() == true) {
                        testStack.removeFirst();
                    }
                    testStackNeedToPop.removeFirst();
                }
                dontPopTestStackNextRTS = false;
                break;
            case 0x62:
                dontPopTestStackNextRTS = true;
                break;
            case 0x68: // PLA - Pull Accumulator - Implied
                state.a = stackPop();
                setArithmeticFlags(state.a);
                break;
            case 0x70: // BVS - Branch if Overflow Set - Relative
                if (getOverflowFlag()) {
                    state.pc = relAddress(state.args[0]);
                }
                break;
            case 0x78: // SEI - Set Interrupt Disable - Implied
                setIrqDisableFlag();
                break;
            case 0x88: // DEY - Decrement Y Register - Implied
                state.y = --state.y & 0xff;
                setArithmeticFlags(state.y);
                break;
            case 0x8a: // TXA - Transfer X to Accumulator - Implied
                state.a = state.x;
                setArithmeticFlags(state.a);
                break;
            case 0x90: // BCC - Branch if Carry Clear - Relative
                if (!getCarryFlag()) {
                    state.pc = relAddress(state.args[0]);
                }
                break;
            case 0x98: // TYA - Transfer Y to Accumulator - Implied
                state.a = state.y;
                setArithmeticFlags(state.a);
                break;
            case 0x9a: // TXS - Transfer X to Stack Pointer - Implied
                setStackPointer(state.x);
                break;
            case 0xa8: // TAY - Transfer Accumulator to Y - Implied
                state.y = state.a;
                setArithmeticFlags(state.y);
                break;
            case 0xaa: // TAX - Transfer Accumulator to X - Implied
                state.x = state.a;
                setArithmeticFlags(state.x);
                break;
            case 0xb0: // BCS - Branch if Carry Set - Relative
                if (getCarryFlag()) {
                    state.pc = relAddress(state.args[0]);
                }
                break;
            case 0xb8: // CLV - Clear Overflow Flag - Implied
                clearOverflowFlag();
                break;
            case 0xba: // TSX - Transfer Stack Pointer to X - Implied
                state.x = getStackPointer();
                setArithmeticFlags(state.x);
                break;
            case 0xc8: // INY - Increment Y Register - Implied
                state.y = ++state.y & 0xff;
                setArithmeticFlags(state.y);
                break;
            case 0xca: // DEX - Decrement X Register - Implied
                state.x = --state.x & 0xff;
                setArithmeticFlags(state.x);
                break;
            case 0xd0: // BNE - Branch if Not Equal to Zero - Relative
                if (!getZeroFlag()) {
                    state.pc = relAddress(state.args[0]);
                }
                break;
            case 0xd8: // CLD - Clear Decimal Mode - Implied
                clearDecimalModeFlag();
                break;
            case 0xe8: // INX - Increment X Register - Implied
                state.x = ++state.x & 0xff;
                setArithmeticFlags(state.x);
                break;
            case 0xea: // NOP
                // Do nothing.
                break;
            case 0xf0: // BEQ - Branch if Equal to Zero - Relative
                if (getZeroFlag()) {
                    state.pc = relAddress(state.args[0]);
                }
                break;
            case 0xf8: // SED - Set Decimal Flag - Implied
                setDecimalModeFlag();
                break;

            /** JMP *****************************************************************/
            case 0x4c: // JMP - Absolute
                state.pc = address(state.args[0], state.args[1]);
                break;
            case 0x6c: // JMP - Indirect
                lo = address(state.args[0], state.args[1]); // Address of low byte

                if (state.args[0] == 0xff &&
                        (behavior == CpuBehavior.NMOS_WITH_INDIRECT_JMP_BUG ||
                                behavior == CpuBehavior.NMOS_WITH_ROR_BUG)) {
                    hi = address(0x00, state.args[1]);
                } else {
                    hi = lo + 1;
                }

                state.pc = address(bus.read(lo), bus.read(hi));
                /* TODO: For accuracy, allow a flag to enable broken behavior of early 6502s:
                 *
                 * "An original 6502 has does not correctly fetch the target
                 * address if the indirect vector falls on a page boundary
                 * (e.g. $xxFF where xx is and value from $00 to $FF). In this
                 * case fetches the LSB from $xxFF as expected but takes the MSB
                 * from $xx00. This is fixed in some later chips like the 65SC02
                 * so for compatibility always ensure the indirect vector is not
                 * at the end of the page."
                 * (http://www.obelisk.demon.co.uk/6502/reference.html#JMP)
                 */
                break;


            /** ORA - Logical Inclusive Or ******************************************/
            case 0x09: // #Immediate
                state.a |= state.args[0];
                setArithmeticFlags(state.a);
                break;
            case 0x01: // (Zero Page,X)
            case 0x05: // Zero Page
            case 0x0d: // Absolute
            case 0x11: // (Zero Page),Y
            case 0x15: // Zero Page,X
            case 0x19: // Absolute,Y
            case 0x1d: // Absolute,X
                state.a |= bus.read(effectiveAddress);
                setArithmeticFlags(state.a);
                state.traceShouldShowReadByte = true;
                break;


            /** ASL - Arithmetic Shift Left *****************************************/
            case 0x0a: // Accumulator
                state.a = asl(state.a);
                setArithmeticFlags(state.a);
                break;
            case 0x06: // Zero Page
            case 0x0e: // Absolute
            case 0x16: // Zero Page,X
            case 0x1e: // Absolute,X
                tmp = asl(bus.read(effectiveAddress));
                bus.write(effectiveAddress, tmp);
                setArithmeticFlags(tmp);
                state.traceShouldShowReadByte = true;
                break;


            /** BIT - Bit Test ******************************************************/
            case 0x24: // Zero Page
            case 0x2c: // Absolute
                tmp = bus.read(effectiveAddress);
                setZeroFlag((state.a & tmp) == 0);
                setNegativeFlag((tmp & 0x80) != 0);
                setOverflowFlag((tmp & 0x40) != 0);
                state.traceShouldShowReadByte = true;
                break;


            /** AND - Logical AND ***************************************************/
            case 0x29: // #Immediate
                state.a &= state.args[0];
                setArithmeticFlags(state.a);
                break;
            case 0x21: // (Zero Page,X)
            case 0x25: // Zero Page
            case 0x2d: // Absolute
            case 0x31: // (Zero Page),Y
            case 0x35: // Zero Page,X
            case 0x39: // Absolute,Y
            case 0x3d: // Absolute,X
                state.a &= bus.read(effectiveAddress);
                setArithmeticFlags(state.a);
                state.traceShouldShowReadByte = true;
                break;


            /** ROL - Rotate Left ***************************************************/
            case 0x2a: // Accumulator
                state.a = rol(state.a);
                setArithmeticFlags(state.a);
                break;
            case 0x26: // Zero Page
            case 0x2e: // Absolute
            case 0x36: // Zero Page,X
            case 0x3e: // Absolute,X
                tmp = rol(bus.read(effectiveAddress));
                bus.write(effectiveAddress, tmp);
                setArithmeticFlags(tmp);
                state.traceShouldShowReadByte = true;
                break;


            /** EOR - Exclusive OR **************************************************/
            case 0x49: // #Immediate
                state.a ^= state.args[0];
                setArithmeticFlags(state.a);
                break;
            case 0x41: // (Zero Page,X)
            case 0x45: // Zero Page
            case 0x4d: // Absolute
            case 0x51: // (Zero Page,Y)
            case 0x55: // Zero Page,X
            case 0x59: // Absolute,Y
            case 0x5d: // Absolute,X
                state.a ^= bus.read(effectiveAddress);
                setArithmeticFlags(state.a);
                state.traceShouldShowReadByte = true;
                break;


            /** LSR - Logical Shift Right *******************************************/
            case 0x4a: // Accumulator
                state.a = lsr(state.a);
                setArithmeticFlags(state.a);
                break;
            case 0x46: // Zero Page
            case 0x4e: // Absolute
            case 0x56: // Zero Page,X
            case 0x5e: // Absolute,X
                tmp = lsr(bus.read(effectiveAddress));
                bus.write(effectiveAddress, tmp);
                setArithmeticFlags(tmp);
                state.traceShouldShowReadByte = true;
                break;


            /** ADC - Add with Carry ************************************************/
            case 0x69: // #Immediate
                if (state.decimalModeFlag) {
                    state.a = adcDecimal(state.a, state.args[0]);
                } else {
                    state.a = adc(state.a, state.args[0]);
                }
                break;
            case 0x61: // (Zero Page,X)
            case 0x65: // Zero Page
            case 0x6d: // Absolute
            case 0x71: // (Zero Page),Y
            case 0x75: // Zero Page,X
            case 0x79: // Absolute,Y
            case 0x7d: // Absolute,X
                if (state.decimalModeFlag) {
                    state.a = adcDecimal(state.a, bus.read(effectiveAddress));
                } else {
                    state.a = adc(state.a, bus.read(effectiveAddress));
                }
                state.traceShouldShowReadByte = true;
                break;


            /** ROR - Rotate Right **************************************************/
            case 0x6a: // Accumulator
                state.a = ror(state.a);
                setArithmeticFlags(state.a);
                break;
            case 0x66: // Zero Page
            case 0x6e: // Absolute
            case 0x76: // Zero Page,X
            case 0x7e: // Absolute,X
                tmp = ror(bus.read(effectiveAddress));
                bus.write(effectiveAddress, tmp);
                setArithmeticFlags(tmp);
                state.traceShouldShowReadByte = true;
                break;


            /** STA - Store Accumulator *********************************************/
            case 0x81: // (Zero Page,X)
            case 0x85: // Zero Page
            case 0x8d: // Absolute
            case 0x91: // (Zero Page),Y
            case 0x95: // Zero Page,X
            case 0x99: // Absolute,Y
            case 0x9d: // Absolute,X
                bus.write(effectiveAddress, state.a);
                break;


            /** STY - Store Y Register **********************************************/
            case 0x84: // Zero Page
            case 0x8c: // Absolute
            case 0x94: // Zero Page,X
                bus.write(effectiveAddress, state.y);
                break;


            /** STX - Store X Register **********************************************/
            case 0x86: // Zero Page
            case 0x8e: // Absolute
            case 0x96: // Zero Page,Y
                bus.write(effectiveAddress, state.x);
                break;


            /** LDY - Load Y Register ***********************************************/
            case 0xa0: // #Immediate
                state.y = state.args[0];
                setArithmeticFlags(state.y);
                break;
            case 0xa4: // Zero Page
            case 0xac: // Absolute
            case 0xb4: // Zero Page,X
            case 0xbc: // Absolute,X
                state.y = bus.read(effectiveAddress);
                setArithmeticFlags(state.y);
                break;


            /** LDX - Load X Register ***********************************************/
            case 0xa2: // #Immediate
                state.x = state.args[0];
                setArithmeticFlags(state.x);
                break;
            case 0xa6: // Zero Page
            case 0xae: // Absolute
            case 0xb6: // Zero Page,Y
            case 0xbe: // Absolute,Y
                state.x = bus.read(effectiveAddress);
                setArithmeticFlags(state.x);
                break;


            /** LDA - Load Accumulator **********************************************/
            case 0xa9: // #Immediate
                state.a = state.args[0];
                setArithmeticFlags(state.a);
                break;
            case 0xa1: // (Zero Page,X)
            case 0xa5: // Zero Page
            case 0xad: // Absolute
            case 0xb1: // (Zero Page),Y
            case 0xb5: // Zero Page,X
            case 0xb9: // Absolute,Y
            case 0xbd: // Absolute,X
                state.a = bus.read(effectiveAddress);
                setArithmeticFlags(state.a);
                break;


            /** CPY - Compare Y Register ********************************************/
            case 0xc0: // #Immediate
                cmp(state.y, state.args[0]);
                break;
            case 0xc4: // Zero Page
            case 0xcc: // Absolute
                cmp(state.y, bus.read(effectiveAddress));
                state.traceShouldShowReadByte = true;
                break;


            /** CMP - Compare Accumulator *******************************************/
            case 0xc9: // #Immediate
                cmp(state.a, state.args[0]);
                break;
            case 0xc1: // (Zero Page,X)
            case 0xc5: // Zero Page
            case 0xcd: // Absolute
            case 0xd1: // (Zero Page),Y
            case 0xd5: // Zero Page,X
            case 0xd9: // Absolute,Y
            case 0xdd: // Absolute,X
                cmp(state.a, bus.read(effectiveAddress));
                state.traceShouldShowReadByte = true;
                break;


            /** DEC - Decrement Memory **********************************************/
            case 0xc6: // Zero Page
            case 0xce: // Absolute
            case 0xd6: // Zero Page,X
            case 0xde: // Absolute,X
                tmp = (bus.read(effectiveAddress) - 1) & 0xff;
                bus.write(effectiveAddress, tmp);
                setArithmeticFlags(tmp);
                break;


            /** CPX - Compare X Register ********************************************/
            case 0xe0: // #Immediate
                cmp(state.x, state.args[0]);
                break;
            case 0xe4: // Zero Page
            case 0xec: // Absolute
                cmp(state.x, bus.read(effectiveAddress));
                state.traceShouldShowReadByte = true;
                break;


            /** SBC - Subtract with Carry (Borrow) **********************************/
            case 0xe9: // #Immediate
                if (state.decimalModeFlag) {
                    state.a = sbcDecimal(state.a, state.args[0]);
                } else {
                    state.a = sbc(state.a, state.args[0]);
                }
                break;
            case 0xe1: // (Zero Page,X)
            case 0xe5: // Zero Page
            case 0xed: // Absolute
            case 0xf1: // (Zero Page),Y
            case 0xf5: // Zero Page,X
            case 0xf9: // Absolute,Y
            case 0xfd: // Absolute,X
                if (state.decimalModeFlag) {
                    state.a = sbcDecimal(state.a, bus.read(effectiveAddress));
                } else {
                    state.a = sbc(state.a, bus.read(effectiveAddress));
                }
                state.traceShouldShowReadByte = true;
                break;


            /** INC - Increment Memory **********************************************/
            case 0xe6: // Zero Page
            case 0xee: // Absolute
            case 0xf6: // Zero Page,X
            case 0xfe: // Absolute,X
                tmp = (bus.read(effectiveAddress) + 1) & 0xff;
                bus.write(effectiveAddress, tmp);
                setArithmeticFlags(tmp);
                break;

            /** Unimplemented Instructions ****************************************/
            // TODO: Create a flag to enable highly-accurate emulation of unimplemented instructions.
            default:
                setOpTrap();
                break;
        }

        delayLoop(state.ir);
    }

    private void handleIrq(int returnPc) throws MemoryAccessException {
        handleInterrupt(returnPc, IRQ_VECTOR_L, IRQ_VECTOR_H);
        clearIrq();
    }

    private void handleNmi() throws MemoryAccessException {
        handleInterrupt(state.pc, NMI_VECTOR_L, NMI_VECTOR_H);
        clearNmi();
    }

    /**
     * Handle the common behavior of BRK, /IRQ, and /NMI
     *
     * @throws MemoryAccessException
     */
    private void handleInterrupt(int returnPc, int vectorLow, int vectorHigh) throws MemoryAccessException {
        // Set the break flag before pushing.
        // MPi: Not for the usual case of the C64 handling its IRQ
//        setBreakFlag();
        // Push program counter + 1 onto the stack
        stackPush((returnPc >> 8) & 0xff); // PC high byte
        stackPush(returnPc & 0xff);        // PC low byte
        stackPush(state.getStatusFlag());
        // Set the Interrupt Disabled flag.  RTI will clear it.
        setIrqDisableFlag();

        // Load interrupt vector address into PC
        state.pc = address(bus.read(vectorLow), bus.read(vectorHigh));
    }

    /**
     * Add with Carry, used by all addressing mode implementations of ADC.
     * As a side effect, this will set the overflow and carry flags as
     * needed.
     *
     * @param acc     The current value of the accumulator
     * @param operand The operand
     * @return
     */
    private int adc(int acc, int operand) {
        int result = (operand & 0xff) + (acc & 0xff) + getCarryBit();
        int carry6 = (operand & 0x7f) + (acc & 0x7f) + getCarryBit();
        setCarryFlag((result & 0x100) != 0);
        setOverflowFlag(state.carryFlag ^ ((carry6 & 0x80) != 0));
        result &= 0xff;
        setArithmeticFlags(result);
        return result;
    }

    /**
     * Add with Carry (BCD).
     */

    private int adcDecimal(int acc, int operand) {
        int l, h, result;
        l = (acc & 0x0f) + (operand & 0x0f) + getCarryBit();
        if ((l & 0xff) > 9) {
            l += 6;
        }
        h = (acc >> 4) + (operand >> 4) + (l > 15 ? 1 : 0);
        if ((h & 0xff) > 9) {
            h += 6;
        }
        result = (l & 0x0f) | (h << 4);
        result &= 0xff;
        setCarryFlag(h > 15);
        setZeroFlag(result == 0);
        setNegativeFlag(false); // BCD is never negative
        setOverflowFlag(false); // BCD never sets overflow flag
        return result;
    }

    /**
     * Common code for Subtract with Carry.  Just calls ADC of the
     * one's complement of the operand.  This lets the N, V, C, and Z
     * flags work out nicely without any additional logic.
     */
    private int sbc(int acc, int operand) {
        int result;
        result = adc(acc, ~operand);
        setArithmeticFlags(result);
        return result;
    }

    /**
     * Subtract with Carry, BCD mode.
     */
    private int sbcDecimal(int acc, int operand) {
        int l, h, result;
        l = (acc & 0x0f) - (operand & 0x0f) - (state.carryFlag ? 0 : 1);
        if ((l & 0x10) != 0) {
            l -= 6;
        }
        h = (acc >> 4) - (operand >> 4) - ((l & 0x10) != 0 ? 1 : 0);
        if ((h & 0x10) != 0) {
            h -= 6;
        }
        result = (l & 0x0f) | (h << 4);
        setCarryFlag((h & 0xff) < 15);
        setZeroFlag(result == 0);
        setNegativeFlag(false); // BCD is never negative
        setOverflowFlag(false); // BCD never sets overflow flag
        return (result & 0xff);
    }

    /**
     * Compare two values, and set carry, zero, and negative flags
     * appropriately.
     */
    private void cmp(int reg, int operand) {
        int tmp = (reg - operand) & 0xff;
        setCarryFlag(reg >= operand);
        setZeroFlag(tmp == 0);
        setNegativeFlag((tmp & 0x80) != 0); // Negative bit set
    }

    /**
     * Set the Negative and Zero flags based on the current value of the
     * register operand.
     */
    private void setArithmeticFlags(int reg) {
        state.zeroFlag = (reg == 0);
        state.negativeFlag = (reg & 0x80) != 0;
    }

    /**
     * Shifts the given value left by one bit, and sets the carry
     * flag to the high bit of the initial value.
     *
     * @param m The value to shift left.
     * @return the left shifted value (m * 2).
     */
    private int asl(int m) {
        setCarryFlag((m & 0x80) != 0);
        return (m << 1) & 0xff;
    }

    /**
     * Shifts the given value right by one bit, filling with zeros,
     * and sets the carry flag to the low bit of the initial value.
     */
    private int lsr(int m) {
        setCarryFlag((m & 0x01) != 0);
        return (m & 0xff) >>> 1;
    }

    /**
     * Rotates the given value left by one bit, setting bit 0 to the value
     * of the carry flag, and setting the carry flag to the original value
     * of bit 7.
     */
    private int rol(int m) {
        int result = ((m << 1) | getCarryBit()) & 0xff;
        setCarryFlag((m & 0x80) != 0);
        return result;
    }

    /**
     * Rotates the given value right by one bit, setting bit 7 to the value
     * of the carry flag, and setting the carry flag to the original value
     * of bit 1.
     */
    private int ror(int m) {
        int result = ((m >>> 1) | (getCarryBit() << 7)) & 0xff;
        setCarryFlag((m & 0x01) != 0);
        return result;
    }

    /**
     * Return the current Cpu State.
     *
     * @return the current Cpu State.
     */
    public CpuState getCpuState() {
        return state;
    }

    /**
     * @return the negative flag
     */
    public boolean getNegativeFlag() {
        return state.negativeFlag;
    }

    /**
     * @param negativeFlag the negative flag to set
     */
    public void setNegativeFlag(boolean negativeFlag) {
        state.negativeFlag = negativeFlag;
    }

    public void setNegativeFlag() {
        state.negativeFlag = true;
    }

    public void clearNegativeFlag() {
        state.negativeFlag = false;
    }

    /**
     * @return the carry flag
     */
    public boolean getCarryFlag() {
        return state.carryFlag;
    }

    /**
     * @param carryFlag the carry flag to set
     */
    public void setCarryFlag(boolean carryFlag) {
        state.carryFlag = carryFlag;
    }

    /**
     * @return 1 if the carry flag is set, 0 if it is clear.
     */
    public int getCarryBit() {
        return (state.carryFlag ? 1 : 0);
    }

    /**
     * Sets the Carry Flag
     */
    public void setCarryFlag() {
        state.carryFlag = true;
    }

    /**
     * Clears the Carry Flag
     */
    public void clearCarryFlag() {
        state.carryFlag = false;
    }

    /**
     * @return the zero flag
     */
    public boolean getZeroFlag() {
        return state.zeroFlag;
    }

    /**
     * @param zeroFlag the zero flag to set
     */
    public void setZeroFlag(boolean zeroFlag) {
        state.zeroFlag = zeroFlag;
    }

    /**
     * Sets the Zero Flag
     */
    public void setZeroFlag() {
        state.zeroFlag = true;
    }

    /**
     * Clears the Zero Flag
     */
    public void clearZeroFlag() {
        state.zeroFlag = false;
    }

    /**
     * @return the irq disable flag
     */
    public boolean getIrqDisableFlag() {
        return state.irqDisableFlag;
    }

    public void setIrqDisableFlag() {
        state.irqDisableFlag = true;
    }

    public void clearIrqDisableFlag() {
        state.irqDisableFlag = false;
    }

    /**
     * @return the decimal mode flag
     */
    public boolean getDecimalModeFlag() {
        return state.decimalModeFlag;
    }

    /**
     * Sets the Decimal Mode Flag to true.
     */
    public void setDecimalModeFlag() {
        state.decimalModeFlag = true;
    }

    /**
     * Clears the Decimal Mode Flag.
     */
    public void clearDecimalModeFlag() {
        state.decimalModeFlag = false;
    }

    /**
     * @return the break flag
     */
    public boolean getBreakFlag() {
        return state.breakFlag;
    }

    /**
     * Sets the Break Flag
     */
    public void setBreakFlag() {
        state.breakFlag = true;
    }

    /**
     * Clears the Break Flag
     */
    public void clearBreakFlag() {
        state.breakFlag = false;
    }

    /**
     * @return the overflow flag
     */
    public boolean getOverflowFlag() {
        return state.overflowFlag;
    }

    /**
     * @param overflowFlag the overflow flag to set
     */
    public void setOverflowFlag(boolean overflowFlag) {
        state.overflowFlag = overflowFlag;
    }

    /**
     * Sets the Overflow Flag
     */
    public void setOverflowFlag() {
        state.overflowFlag = true;
    }

    /**
     * Clears the Overflow Flag
     */
    public void clearOverflowFlag() {
        state.overflowFlag = false;
    }

    /**
     * Set the illegal instruction trap.
     */
    public void setOpTrap() {
        state.opTrap = true;
    }

    /**
     * Clear the illegal instruction trap.
     */
    public void clearOpTrap() {
        state.opTrap = false;
    }

    public int getAccumulator() {
        return state.a;
    }

    public void setAccumulator(int val) {
        state.a = val;
    }

    public int getXRegister() {
        return state.x;
    }

    public void setXRegister(int val) {
        state.x = val;
    }

    public int getYRegister() {
        return state.y;
    }

    public void setYRegister(int val) {
        state.y = val;
    }

    public int getProgramCounter() {
        return state.pc;
    }

    public void setProgramCounter(int addr) {
        state.pc = addr;
    }

    public int getStackPointer() {
        return state.sp;
    }

    public void setStackPointer(int offset) {
        state.sp = offset;
    }

    public int getInstruction() {
        return state.ir;
    }

    public String getAccumulatorStatus() {
        return "$" + HexUtil.byteToHex(state.a);
    }

    public String getXRegisterStatus() {
        return "$" + HexUtil.byteToHex(state.x);
    }

    public String getYRegisterStatus() {
        return "$" + HexUtil.byteToHex(state.y);
    }

    public String getProgramCounterStatus() {
        return "$" + HexUtil.wordToHex(state.pc);
    }

    public String getStackPointerStatus() {
        return "$" + HexUtil.byteToHex(state.sp);
    }

    public int getProcessorStatus() {
        return state.getStatusFlag();
    }

    /**
     * @value The value of the Process Status Register bits to be set.
     */
    public void setProcessorStatus(int value) {
        if ((value & P_CARRY) != 0) {
            setCarryFlag();
        } else {
            clearCarryFlag();
        }

        if ((value & P_ZERO) != 0) {
            setZeroFlag();
        } else {
            clearZeroFlag();
        }

        if ((value & P_IRQ_DISABLE) != 0) {
            setIrqDisableFlag();
        } else {
            clearIrqDisableFlag();
        }

        if ((value & P_DECIMAL) != 0) {
            setDecimalModeFlag();
        } else {
            clearDecimalModeFlag();
        }

        if ((value & P_BREAK) != 0) {
            setBreakFlag();
        } else {
            clearBreakFlag();
        }

        if ((value & P_OVERFLOW) != 0) {
            setOverflowFlag();
        } else {
            clearOverflowFlag();
        }

        if ((value & P_NEGATIVE) != 0) {
            setNegativeFlag();
        } else {
            clearNegativeFlag();
        }
    }

    /**
     * Simulate transition from logic-high to logic-low on the INT line.
     */
    public void assertIrq() {
        state.irqAsserted = true;
    }

    /**
     * Simulate transition from logic-low to logic-high of the INT line.
     */
    public void clearIrq() {
        state.irqAsserted = false;
    }

    /**
     * Simulate transition from logic-high to logic-low on the NMI line.
     */
    public void assertNmi() {
        state.nmiAsserted = true;
    }

    /**
     * Simulate transition from logic-low to logic-high of the NMI line.
     */
    public void clearNmi() {
        state.nmiAsserted = false;
    }

    /**
     * Push an item onto the stack, and decrement the stack counter.
     * Will wrap-around if already at the bottom of the stack (This
     * is the same behavior as the real 6502)
     */
    public void stackPush(int data) throws MemoryAccessException {
        bus.write(0x100 + state.sp, data);

        if (state.sp == 0) {
            state.sp = 0xff;
        } else {
            --state.sp;
        }
    }

    /**
     * Pre-increment the stack pointer, and return the top of the stack.
     * Will wrap-around if already at the top of the stack (This
     * is the same behavior as the real 6502)
     */
    public int stackPop() throws MemoryAccessException {
        if (state.sp == 0xff) {
            state.sp = 0x00;
        } else {
            ++state.sp;
        }

        return bus.read(0x100 + state.sp);
    }

    /**
     * Peek at the value currently at the top of the stack
     */
    public int stackPeek() throws MemoryAccessException {
        return bus.read(0x100 + state.sp + 1);
    }

    /*
     * Increment the PC, rolling over if necessary.
     */
    void incrementPC() {
        if (state.pc == 0xffff) {
            state.pc = 0;
        } else {
            ++state.pc;
        }
    }

    /**
     * Given two bytes, return an address.
     */
    int address(int lowByte, int hiByte) {
        return ((hiByte << 8) | lowByte) & 0xffff;
    }

    /**
     * Given a hi byte and a low byte, return the Absolute,X
     * offset address.
     */
    int xAddress(int lowByte, int hiByte) {
        return (address(lowByte, hiByte) + state.x) & 0xffff;
    }

    /**
     * Given a hi byte and a low byte, return the Absolute,Y
     * offset address.
     */
    int yAddress(int lowByte, int hiByte) {
        return (address(lowByte, hiByte) + state.y) & 0xffff;
    }

    /**
     * Given a single byte, compute the Zero Page,X offset address.
     */
    int zpxAddress(int zp) {
        return (zp + state.x) & 0xff;
    }

    /**
     * Given a single byte, compute the offset address.
     */
    int relAddress(int offset) {
        // Cast the offset to a signed byte to handle negative offsets
        return (state.pc + (byte) offset) & 0xffff;
    }

    /**
     * Given a single byte, compute the Zero Page,Y offset address.
     */
    int zpyAddress(int zp) {
        return (zp + state.y) & 0xff;
    }

    /*
     * Perform a busy-loop for CLOCK_IN_NS nanoseconds
     */
    private void delayLoop(int opcode) {
        if (overclock == true) {
            return; // don't delay
        }

        int clockSteps = Cpu.instructionClocks[0xff & opcode];
        // Just a precaution. This could be better.
        if (clockSteps == 0) {
            clockSteps = 1;
        }
        long opScheduledEnd = opBeginTime + clockSteps;
        long now = System.nanoTime();
        while (now < opScheduledEnd) {
            now = System.nanoTime();
        }
    }

    /**
     * A compact, struct-like representation of CPU state.
     */
    public static class CpuState {
        /**
         * Accumulator
         */
        public int a;
        /**
         * X index regsiter
         */
        public int x;
        /**
         * Y index register
         */
        public int y;
        /**
         * Stack Pointer
         */
        public int sp;
        /**
         * Program Counter
         */
        public int pc;
        /**
         * Instruction Register
         */
        public int ir;
        public int lastPc;
        public int[] args = new int[2];
        public int instSize;
        public boolean opTrap;
        public boolean irqAsserted;
        public boolean nmiAsserted;

        /* Status Flag Register bits */
        public boolean carryFlag;
        public boolean negativeFlag;
        public boolean zeroFlag;
        public boolean irqDisableFlag;
        public boolean decimalModeFlag;
        public boolean breakFlag;
        public boolean overflowFlag;
        public long stepCounter = 0L;

        /* Trace status */
        public int readByte = 0;
        public int readAddress = 0;
        public boolean traceShouldShowReadByte = false;
        public boolean traceShouldShowReadAddress = false;

        /**
         * Create an empty CPU State.
         */
        public CpuState() {
        }

        /**
         * Snapshot a copy of the CpuState.
         * <p>
         * (This is a copy constructor rather than an implementation of <code>Clonable</code>
         * based on Josh Bloch's recommendation)
         *
         * @param s The CpuState to copy.
         */
        public CpuState(CpuState s) {
            this.a = s.a;
            this.x = s.x;
            this.y = s.y;
            this.sp = s.sp;
            this.pc = s.pc;
            this.ir = s.ir;
            this.lastPc = s.lastPc;
            this.args[0] = s.args[0];
            this.args[1] = s.args[1];
            this.instSize = s.instSize;
            this.opTrap = s.opTrap;
            this.irqAsserted = s.irqAsserted;
            this.carryFlag = s.carryFlag;
            this.negativeFlag = s.negativeFlag;
            this.zeroFlag = s.zeroFlag;
            this.irqDisableFlag = s.irqDisableFlag;
            this.decimalModeFlag = s.decimalModeFlag;
            this.breakFlag = s.breakFlag;
            this.overflowFlag = s.overflowFlag;
            this.stepCounter = s.stepCounter;
        }

        /**
         * Returns a string formatted for the trace log.
         *
         * @return a string formatted for the trace log.
         */
        public String toTraceEvent() {
            String opcode = disassembleOp();
            StringBuilder sb = new StringBuilder(getInstructionByteStatus());
            sb.append("  ");
            sb.append(String.format("%-13s", opcode));
            String lookups = "  ";
            if (traceShouldShowReadByte) {
                lookups += ("[$" + HexUtil.byteToHex(readByte) + "]");
            }
            if (traceShouldShowReadAddress) {
                lookups += "@$" + HexUtil.wordToHex(readAddress);
            }
            sb.append(String.format("%-14s", lookups));
            sb.append("A:" + HexUtil.byteToHex(a) + " ");
            sb.append("X:" + HexUtil.byteToHex(x) + " ");
            sb.append("Y:" + HexUtil.byteToHex(y) + " ");
            sb.append("F:" + HexUtil.byteToHex(getStatusFlag()) + " ");
            sb.append("S:1" + HexUtil.byteToHex(sp) + " ");
            sb.append(getProcessorStatusString());
            return sb.toString();
        }

        // Returns a string formatted like Vice debugger
        public String toDebuggerTrace(int tir , int tpc , int targs0 , int targs1 , int stopwatch) {
            // .C:f6b0  A5 A0       LDA $A0        - A:E7 X:00 Y:0A SP:eb N.-..I..    7233411
            StringBuilder sb = new StringBuilder(".C:");
            sb.append(getInstructionByteStatusForAddress(tir , tpc , targs0 , targs1) + " ");
            sb.append(String.format("%-13s", disassembleOpForAddress(tir , tpc , targs0 , targs1)));

            sb.append("A:" + HexUtil.byteToHex(a) + " ");
            sb.append("X:" + HexUtil.byteToHex(x) + " ");
            sb.append("Y:" + HexUtil.byteToHex(y) + " ");
            sb.append("S:" + HexUtil.byteToHex(sp) + " ");
            sb.append(getProcessorStatusString().substring(1,9) + "    ");
            sb.append(String.format("%08d", stopwatch));
            return sb.toString();
        }

        /**
         * @returns The value of the Process Status Register, as a byte.
         */
        public int getStatusFlag() {
            int status = 0x20;
            if (carryFlag) {
                status |= P_CARRY;
            }
            if (zeroFlag) {
                status |= P_ZERO;
            }
            if (irqDisableFlag) {
                status |= P_IRQ_DISABLE;
            }
            if (decimalModeFlag) {
                status |= P_DECIMAL;
            }
            if (breakFlag) {
                status |= P_BREAK;
            }
            if (overflowFlag) {
                status |= P_OVERFLOW;
            }
            if (negativeFlag) {
                status |= P_NEGATIVE;
            }
            return status;
        }

        public String getInstructionByteStatus() {
            switch (Cpu.instructionSizes[ir]) {
                case 0:
                case 1:
                    return HexUtil.wordToHex(lastPc) + "  " +
                            HexUtil.byteToHex(ir) + "      ";
                case 2:
                    return HexUtil.wordToHex(lastPc) + "  " +
                            HexUtil.byteToHex(ir) + " " +
                            HexUtil.byteToHex(args[0]) + "   ";
                case 3:
                    return HexUtil.wordToHex(lastPc) + "  " +
                            HexUtil.byteToHex(ir) + " " +
                            HexUtil.byteToHex(args[0]) + " " +
                            HexUtil.byteToHex(args[1]);
                default:
                    return null;
            }
        }

        public String getInstructionByteStatusForAddress(int tir , int tpc , int targs0 , int targs1) {
            switch (Cpu.instructionSizes[tir]) {
                case 0:
                case 1:
                    return HexUtil.wordToHex(tpc) + "  " +
                            HexUtil.byteToHex(tir) + "      ";
                case 2:
                    return HexUtil.wordToHex(tpc) + "  " +
                            HexUtil.byteToHex(tir) + " " +
                            HexUtil.byteToHex(targs0) + "   ";
                case 3:
                    return HexUtil.wordToHex(tpc) + "  " +
                            HexUtil.byteToHex(tir) + " " +
                            HexUtil.byteToHex(targs0) + " " +
                            HexUtil.byteToHex(targs1);
                default:
                    return null;
            }
        }

        /**
         * Given an opcode and its operands, return a formatted name.
         *
         * @return A string representing the mnemonic and operands of the instruction
         */
        public String disassembleOp() {
            String mnemonic = opcodeNames[ir];

            if (mnemonic == null) {
                return "???";
            }

            StringBuilder sb = new StringBuilder(mnemonic);

            switch (instructionModes[ir]) {
                case ABS:
                    sb.append(" $" + HexUtil.wordToHex(address(args[0], args[1])));
                    break;
                case ABX:
                    sb.append(" $" + HexUtil.wordToHex(address(args[0], args[1])) + ",X");
                    break;
                case ABY:
                    sb.append(" $" + HexUtil.wordToHex(address(args[0], args[1])) + ",Y");
                    break;
                case IMM:
                    sb.append(" #$" + HexUtil.byteToHex(args[0]));
                    break;
                case IND:
                    sb.append(" ($" + HexUtil.wordToHex(address(args[0], args[1])) + ")");
                    break;
                case XIN:
                    sb.append(" ($" + HexUtil.byteToHex(args[0]) + ",X)");
                    break;
                case INY:
                    sb.append(" ($" + HexUtil.byteToHex(args[0]) + "),Y");
                    break;
                case REL:
                case ZPG:
                    sb.append(" $" + HexUtil.byteToHex(args[0]));
                    break;
                case ZPX:
                    sb.append(" $" + HexUtil.byteToHex(args[0]) + ",X");
                    break;
                case ZPY:
                    sb.append(" $" + HexUtil.byteToHex(args[0]) + ",Y");
                    break;
            }

            return sb.toString();
        }

        public String disassembleOpForAddress(int tir , int tpc , int targs0 , int targs1) {
            String mnemonic = opcodeNames[tir];

            if (mnemonic == null) {
                return "???";
            }

            StringBuilder sb = new StringBuilder(mnemonic);

            switch (instructionModes[tir]) {
                case ABS:
                    sb.append(" $" + HexUtil.wordToHex(address(targs0, targs1)));
                    break;
                case ABX:
                    sb.append(" $" + HexUtil.wordToHex(address(targs0, targs1)) + ",X");
                    break;
                case ABY:
                    sb.append(" $" + HexUtil.wordToHex(address(targs0, targs1)) + ",Y");
                    break;
                case IMM:
                    sb.append(" #$" + HexUtil.byteToHex(targs0));
                    break;
                case IND:
                    sb.append(" ($" + HexUtil.wordToHex(address(targs0, targs1)) + ")");
                    break;
                case XIN:
                    sb.append(" ($" + HexUtil.byteToHex(targs0) + ",X)");
                    break;
                case INY:
                    sb.append(" ($" + HexUtil.byteToHex(targs0) + "),Y");
                    break;
                case REL:
                case ZPG:
                    sb.append(" $" + HexUtil.byteToHex(targs0));
                    break;
                case ZPX:
                    sb.append(" $" + HexUtil.byteToHex(targs0) + ",X");
                    break;
                case ZPY:
                    sb.append(" $" + HexUtil.byteToHex(targs0) + ",Y");
                    break;
            }

            return sb.toString();
        }
        
        /**
         * Given two bytes, return an address.
         */
        private int address(int lowByte, int hiByte) {
            return ((hiByte << 8) | lowByte) & 0xffff;
        }


        /**
         * @return A string representing the current status register state.
         */
        public String getProcessorStatusString() {
            StringBuilder sb = new StringBuilder("[");
            sb.append(negativeFlag ? 'N' : '.');    // Bit 7
            sb.append(overflowFlag ? 'V' : '.');    // Bit 6
            sb.append("-");                         // Bit 5 (always 1)
            sb.append(breakFlag ? 'B' : '.');       // Bit 4
            sb.append(decimalModeFlag ? 'D' : '.'); // Bit 3
            sb.append(irqDisableFlag ? 'I' : '.');  // Bit 2
            sb.append(zeroFlag ? 'Z' : '.');        // Bit 1
            sb.append(carryFlag ? 'C' : '.');       // Bit 0
            sb.append("]");
            return sb.toString();
        }
    }

    protected class RegisterStackSet {
        public static final int kNotSet = 0xFFFF;
        public int a = kNotSet;
        public int x = kNotSet;
        public int y = kNotSet;
    }
}
