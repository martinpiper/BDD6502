package com.loomcom.symon.devices;


import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;
import cucumber.api.Scenario;

public class PrintIODevice extends Device
{
    private String mBuffer = new String();
    private Scenario mScenario = null;
    private int upperStore = 0;

    public PrintIODevice(int startAddress, int endAddress, String name, Scenario scenario) throws MemoryRangeException {
        super(startAddress, endAddress, name);
        mScenario = scenario;
    }

    @Override
    public void write(int address, int data) throws MemoryAccessException {
        int register = address & 7;
        switch(register)
        {
            case 0: //ouput ascii
                mBuffer += Character.toString((char) data);
                break;
            case 1: //output decimal byte
                mBuffer += Integer.toString(data) + " ";
                break;
            case 2: //output byte hex
                mBuffer += Integer.toHexString(data) + " ";
                break;
            case 3: //save hi byte
                upperStore = data;
                break;
            case 4: //output word hex
                mBuffer += Integer.toHexString((upperStore*256+data)) + " ";
                break;
            case 5: //flush line
                mScenario.write(mBuffer + "\n");
                mBuffer = "";
                break;
            case 6: //clear buffer
                mBuffer ="";
                break;
            default:
                break;
        }
    }

    @Override
    public int read(int address) throws MemoryAccessException {
        return 0;
    }

    @Override
    public String toString() {
        return mBuffer;
    }
}
