package com.bdd6502;

import TestGlue.Glue;
import com.loomcom.symon.devices.UserPortTo24BitAddress;

import javax.script.ScriptException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class RemoteDebugger implements Runnable {
    private RemoteDebugger() {
        setReplyReg(0,0,0,0,0,0,0,0,0,0,0, "");
    }

    public int getNumConnections() {
        return numConnections;
    }

    volatile int numConnections = 0;

    public boolean isReceivedCommand() {
        return receivedCommand;
    }

    volatile boolean receivedCommand = false;

    public boolean isReceivedNext() {
        return receivedNext;
    }

    volatile boolean receivedNext = false;

    volatile boolean receivedStep = false;

    public boolean isReceivedStep() {
        return receivedStep;
    }

    volatile boolean receivedReturn = false;

    public boolean isReceivedReturn() {
        return receivedReturn;
    }

    volatile Set<Integer> receivedBreakAt = new HashSet<>();

    public boolean isReceivedBreakAt(int address) {
        return receivedBreakAt.contains(address);
    }

    public void clearStepNextReturn() {
        receivedNext = false;
        receivedStep = false;
        receivedReturn = false;
    }

    public enum DisplayType {kDisplay_Full , kDisplay_Clear , kDisplay_Ahead};

    public DisplayType getDebuggerDisplay() {
        return debuggerDisplay;
    }

    public boolean isDebuggerDisplayChanged() {
        boolean ret = debuggerDisplayChanged;
        debuggerDisplayChanged = false;
        return ret;
    }

    boolean debuggerDisplayChanged = false;
    public DisplayType debuggerDisplay = DisplayType.kDisplay_Full;

    public void signalSuspendDevice(int deviceFlags) {
        this.suspendCPU |= deviceFlags;
    }

    public void clearSuspendDevice(int deviceFlags) {
        this.suspendCPU &= ~deviceFlags;
    }

    public static final int kDeviceFlags_CPU = 0b01;
    public static final int kDeviceFlags_APU = 0b10;

    int currentDevice = kDeviceFlags_CPU;

    public boolean isCurrentDevice(int device) {
        return (currentDevice & device) == device;
    }

    public boolean isSuspendDevice(int flags) {
        return (suspendCPU & flags) != 0;
    }

    public void suspendCurrentDevice() { suspendCPU |= currentDevice; }

    volatile int suspendCPU = 0;

    volatile String replyReg = "<undefined>";
    volatile String currentReplyPrefix = "<undefined>";

    public void setReplyNext(String replyNext) {
        this.replyNext = replyNext;
        clearStepNextReturn();
    }

    volatile String replyNext;

    public void setReplyDisassemble(String replyDisassemble) {
        this.replyDisassemble = replyDisassemble;
        this.receivedDisassemble = false;
    }

    volatile String replyDisassemble;

    public boolean isReceivedDisassemble() {
        return receivedDisassemble;
    }

    boolean receivedDisassemble = false;

    public int getDisassembleStart() {
        return disassembleStart;
    }

    int disassembleStart;

    public int getDisassembleEnd() {
        return disassembleEnd;
    }

    int disassembleEnd;

    public int getDumpStart() {
        return dumpStart;
    }

    int dumpStart;

    public int getDumpEnd() {
        return dumpEnd;
    }

    int dumpEnd;

    public void setReplyDump(byte[] replyDump) {
        this.replyDump = replyDump;
        this.receivedDump = false;
    }

    volatile byte[] replyDump = null;

    public boolean isReceivedDump() {
        return receivedDump;
    }

    volatile boolean receivedDump = false;

    public boolean isReceivedReg() {
        return receivedReg;
    }

    volatile boolean receivedReg = false;

    public void setReplyReg(int addr, int a , int x , int y , int sp , int mem0 , int mem1 , int st , int lin , int cycle , int stopwatch, String optionalExtras) {
        setCurrentPrefix(addr);
        receivedReg = false;

        String newReplyReg = "  ADDR A  X  Y  SP 00 01 NV-BDIZC LIN CYC  STOPWATCH\n";
        String hex = String.format("%4s", Integer.toHexString(addr)).replace(' ', '0');
        newReplyReg +=".;" + hex + " ";

        hex = String.format("%2s", Integer.toHexString(a)).replace(' ', '0');
        newReplyReg += hex + " ";
        hex = String.format("%2s", Integer.toHexString(x)).replace(' ', '0');
        newReplyReg += hex + " ";
        hex = String.format("%2s", Integer.toHexString(y)).replace(' ', '0');
        newReplyReg += hex + " ";

        hex = String.format("%2s", Integer.toHexString(sp)).replace(' ', '0');
        newReplyReg += hex + " ";

        hex = String.format("%2s", Integer.toHexString(mem0)).replace(' ', '0');
        newReplyReg += hex + " ";
        hex = String.format("%2s", Integer.toHexString(mem1)).replace(' ', '0');
        newReplyReg += hex + " ";

        String binary = String.format("%8s", Integer.toBinaryString(st)).replace(' ', '0');
        newReplyReg += binary + " ";

        String decimal = String.format("%3d", lin).replace(' ', '0');
        newReplyReg += decimal + " ";
        decimal = String.format("%3d", cycle).replace(' ', '0');
        newReplyReg += decimal + "   ";

        decimal = String.format("%8d", stopwatch).replace(' ', '0');
        newReplyReg += decimal + "\n";

        if (optionalExtras != null) {
            newReplyReg += optionalExtras;
        }

        this.replyReg = newReplyReg;
    }

    public void setCurrentPrefix(int addr) {
        String hex = String.format("%4s", Integer.toHexString(addr)).replace(' ', '0');
        currentReplyPrefix = "(C:$" + hex + ") ";
    }

    public void setDisassembleStart(int disassembleStart) {
        this.disassembleStart = disassembleStart;
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(6510);

            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                InputStream input = socket.getInputStream();

                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);
                numConnections++;
                try {
                    String potentialLine = "";
                    while (!socket.isClosed()) {
                        handleReplies(output, writer);

                        if (input.available() <= 0) {
                            Thread.sleep(10);
                            continue;
                        }
                        int nextByte = input.read();
                        if (nextByte < 0) {
                            break;
                        }
                        if (nextByte == 0x02) {
                            // Binary protocol
                            int length = input.read();
                            int sentCommand[] = new int[length];
                            int i = 0;
                            while (i < length) {
                                sentCommand[i++] = input.read() & 0xff;
                            }
                            if (sentCommand[0] == 0x01) {
                                // Dump command
                                suspendCurrentDevice();
                                receivedCommand = true;

                                dumpStart = sentCommand[1] + (sentCommand[2] << 8);
                                dumpEnd = sentCommand[3] + (sentCommand[4] << 8);

                                replyDump = null;
                                receivedDump = true;

                                System.out.println("RDEBUG: BIN: dump " + dumpStart + " " + dumpEnd);
                            }
                            continue;
                        }
                        // No we don't use the buffered input stream reader and read a line because we want to have raw unadulterated bytes
                        String line = "";
                        do {
                            handleReplies(output, writer);
                            if (socket.isClosed()) {
                                break;
                            }
                            if (input.available() <= 0) {
                                Thread.sleep(10);
                                continue;
                            }
                            line += (char) nextByte;
                            nextByte = input.read();
                        } while (!(nextByte == 0x0d || nextByte == 0x0a));

                        receivedCommand = true;

                        line = line.trim();

                        System.out.println("RDEBUG: '" + line + "'");
                        if (line.isEmpty()) {
                            continue;
                        }

                        if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("x") || line.equalsIgnoreCase("goto") || line.equalsIgnoreCase("g")) {
                            suspendCPU = 0;
                            continue;
                        }

                        if (line.startsWith("display")) {
                            try {
                                String[] splits = line.split(" ");

                                if (splits.length >= 2) {
                                    if (splits[1].compareToIgnoreCase("full") == 0) {
                                        debuggerDisplay = DisplayType.kDisplay_Full;
                                        debuggerDisplayChanged = true;
                                        writer.print(currentReplyPrefix);
                                    } else if (splits[1].compareToIgnoreCase("cls") == 0) {
                                        debuggerDisplay = DisplayType.kDisplay_Clear;
                                        debuggerDisplayChanged = true;
                                        writer.print(currentReplyPrefix);
                                    } else if (splits[1].compareToIgnoreCase("ahead") == 0) {
                                        debuggerDisplay = DisplayType.kDisplay_Ahead;
                                        debuggerDisplayChanged = true;
                                        writer.print(currentReplyPrefix);
                                    }
                                } else {
                                    writer.print("display <full> <cls> <ahead>\n" + currentReplyPrefix);
                                }
                            } catch (Exception e2) {
                                writer.print(currentReplyPrefix);
                            }
                            writer.flush();
                        } else if (line.startsWith("break") || line.startsWith("bk")) {
                            String[] splits = line.split(" ");
                            suspendCurrentDevice();
                            if (splits.length > 1) {
                                int address = 0;
                                try {
                                    address = Glue.valueToInt(splits[1]);
                                    receivedBreakAt.add(address);
                                } catch (ScriptException e) {
                                    writer.println("Couldn't parse: " + splits[1]);
                                }
                            }
                            // TODO: Respond with current break points
                            if (receivedBreakAt.isEmpty()) {
                                writer.print("No breakpoints are set\n" + currentReplyPrefix);
                            } else {
                                writer.print(receivedBreakAt.size() + " breakpoints set\n");
                                for (Integer address: receivedBreakAt
                                     ) {
                                    writer.println("   " + String.format("$%04x", address));
                                }
                                writer.print(currentReplyPrefix);
                            }
                            writer.flush();
                            continue;
                        } else if (line.startsWith("delete") || line.startsWith("del")) {
                            suspendCurrentDevice();
                            receivedBreakAt.clear();
                            writer.print("All breakpoints deleted\n" + currentReplyPrefix);
                            writer.flush();
                            continue;
                        } else if (line.equalsIgnoreCase("reg") || line.equalsIgnoreCase("r")) {
                            suspendCurrentDevice();
                            replyReg = null;
                            receivedReg = true;
                            continue;
                        } else if (line.equalsIgnoreCase("next") || line.equalsIgnoreCase("n")) {
                            replyNext = null;
                            receivedNext = true;
                            suspendCPU = 0;
                            continue;
                        } else if (line.equalsIgnoreCase("step") || line.equalsIgnoreCase("z")) {
                            replyNext = null;
                            receivedStep = true;
                            suspendCPU = 0;
                            continue;
                        } else if (line.equalsIgnoreCase("return") || line.equalsIgnoreCase("ret")) {
                            replyNext = null;
                            receivedReturn = true;
                            suspendCPU = 0;
                            continue;
                        } else if (line.startsWith("disass") || line.startsWith("d ") || line.equalsIgnoreCase("d")) {
                            try {
                                String[] splits = line.split(" ");
                                disassembleEnd = disassembleStart + 0x30;
                                if (splits.length >= 2) {
                                    disassembleStart = Glue.valueToInt(splits[1], 16);
                                    disassembleEnd = disassembleStart + 0x30;
                                }
                                if (splits.length >= 3) {
                                    disassembleEnd = Glue.valueToInt(splits[2], 16);
                                }

                                suspendCurrentDevice();
                                replyDisassemble = null;
                                receivedDisassemble = true;
                            } catch (Exception e2) {
                                writer.print(currentReplyPrefix);
                            }
                            writer.flush();
                        } else if (line.startsWith("cpu")) {
                            try {
                                String[] splits = line.split(" ");

                                if (splits.length >= 2) {
                                    if (splits[1].compareToIgnoreCase("6502") == 0) {
                                        currentDevice = kDeviceFlags_CPU;
                                        writer.print(currentReplyPrefix);
                                    } else if (splits[1].compareToIgnoreCase("apu") == 0) {
                                        currentDevice = kDeviceFlags_APU;
                                        writer.print(currentReplyPrefix);
                                    }
                                } else {
                                    writer.print("This device (Computer) supports the following CPU types: 6502 apu\n" + currentReplyPrefix);
                                }
                            } catch (Exception e2) {
                                writer.print(currentReplyPrefix);
                            }
                            writer.flush();
                        } else {
                            // Always the last case
                            writer.print("Unknown command: " + line + "\n" + currentReplyPrefix);
                            writer.flush();
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                // Any broken connection and the state resets itself
                numConnections--;
                suspendCPU = 0;
                receivedCommand = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleReplies(OutputStream output, PrintWriter writer) throws IOException {
        if (replyReg != null) {
            writer.print(replyReg + currentReplyPrefix);
            writer.flush();
            replyReg = null;
        }
        if (replyNext != null) {
            writer.print(replyNext + currentReplyPrefix);
            writer.flush();
            replyNext = null;
        }
        if (replyDisassemble != null) {
            writer.print(replyDisassemble + currentReplyPrefix);
            writer.flush();
            replyDisassemble = null;
        }
        if (replyDump != null) {
            output.write(0x02);
            output.write(replyDump.length);
            output.write(replyDump.length >> 8);
            output.write(replyDump.length >> 16);
            output.write(replyDump.length >> 24);
            output.write(0x00);
            output.write(replyDump);
            output.flush();
            replyDump = null;
        }
    }

    public static RemoteDebugger getRemoteDebugger() {
        return remoteDebugger;
    }

    static RemoteDebugger remoteDebugger;
    static Thread remoteDebuggerThread;

    public static void startRemoteDebugger() {
        if (remoteDebugger != null) {
            return;
        }
        remoteDebugger = new RemoteDebugger();

        remoteDebuggerThread = new Thread(remoteDebugger);
        remoteDebuggerThread.start();
    }

    static void stopRemoteDebugger() {
        if (remoteDebugger == null) {
            return;
        }
        remoteDebuggerThread.interrupt();
        try {
            remoteDebuggerThread.wait(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        remoteDebuggerThread = null;
        remoteDebugger = null;
    }
}
