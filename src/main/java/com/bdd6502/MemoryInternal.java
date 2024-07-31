package com.bdd6502;

import com.loomcom.symon.MemoryRange;
import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.MemoryRangeException;

public class MemoryInternal extends MemoryRange {
    private Memory memory = null;
    public MemoryInternal(int startAddress, int endAddress) throws MemoryRangeException {
        super(startAddress, endAddress);
        memory = new Memory(startAddress , endAddress);
    }

    public Memory getMemory() {
        return memory;
    }
}
