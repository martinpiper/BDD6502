package com.bdd6502;

import com.loomcom.symon.devices.UserPortTo24BitAddress;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class RemoteDebugger implements Runnable {
    private RemoteDebugger() {
        setReplyReg(0,0,0,0,0,0,0,0,0,0,0);
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
        if (receivedNext) {
            suspendCPU = true;
            receivedNext = false;
            return true;
        }
        return false;
    }

    volatile boolean receivedNext = false;

    public boolean isSuspendCPU() {
        return suspendCPU;
    }

    volatile  boolean suspendCPU = false;

    volatile String replyReg;
    volatile String currentReplyPrefix;

    public void setReplyNext(String replyNext) {
        this.replyNext = replyNext;
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

    public void setReplyReg(int addr, int a , int x , int y , int sp , int mem0 , int mem1 , int st , int lin , int cycle , int stopwatch) {
        receivedReg = false;

        replyReg = "  ADDR A  X  Y  SP 00 01 NV-BDIZC LIN CYC  STOPWATCH\n";
        String hex = String.format("%4s", Integer.toHexString(addr)).replace(' ', '0');
        replyReg +=".;" + hex + " ";
        currentReplyPrefix = "(C:$" + hex + ") ";

        hex = String.format("%2s", Integer.toHexString(a)).replace(' ', '0');
        replyReg += hex + " ";
        hex = String.format("%2s", Integer.toHexString(x)).replace(' ', '0');
        replyReg += hex + " ";
        hex = String.format("%2s", Integer.toHexString(y)).replace(' ', '0');
        replyReg += hex + " ";

        hex = String.format("%2s", Integer.toHexString(sp)).replace(' ', '0');
        replyReg += hex + " ";

        hex = String.format("%2s", Integer.toHexString(mem0)).replace(' ', '0');
        replyReg += hex + " ";
        hex = String.format("%2s", Integer.toHexString(mem1)).replace(' ', '0');
        replyReg += hex + " ";

        String binary = String.format("%8s", Integer.toBinaryString(st)).replace(' ', '0');
        replyReg += binary + " ";

        String decimal = String.format("%3d", lin).replace(' ', '0');
        replyReg += decimal + " ";
        decimal = String.format("%3d", cycle).replace(' ', '0');
        replyReg += decimal + "   ";

        decimal = String.format("%8d", stopwatch).replace(' ', '0');
        replyReg += decimal + "\n";

        if (UserPortTo24BitAddress.getThisInstance() != null) {
            if (UserPortTo24BitAddress.getThisInstance().isEnableAPU()) {
                replyReg += UserPortTo24BitAddress.getThisInstance().getDebugOutputLastState();
            }
        }
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
                                suspendCPU = true;
                                receivedCommand = true;

                                disassembleStart = sentCommand[1] + (sentCommand[2] << 8);
                                disassembleEnd = sentCommand[3] + (sentCommand[4] << 8);

                                replyDump = null;
                                receivedDump = true;

                                System.out.println("RDEBUG: BIN: dump " + disassembleStart + " " + disassembleEnd);

                                while (replyDump == null) {
                                    Thread.sleep(10);
                                }
                                output.write(0x02);
                                output.write(replyDump.length);
                                output.write(replyDump.length >> 8);
                                output.write(replyDump.length >> 16);
                                output.write(replyDump.length >> 24);
                                output.write(0x00);
                                output.write(replyDump);
                                output.flush();
                            }
                            continue;
                        }
                        // No we don't use the buffered input stream reader and read a line because we want to have raw unadulterated bytes
                        String line = "";
                        do {
                            line += (char) nextByte;
                            nextByte = input.read();
                        } while (!(nextByte == 0x0d || nextByte == 0x0a));

                        suspendCPU = true;
                        receivedCommand = true;

                        line = line.trim();

                        System.out.println("RDEBUG: " + line);

                        if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("x") || line.equalsIgnoreCase("goto") || line.equalsIgnoreCase("g")) {
                            suspendCPU = false;
                            continue;
                        }

                        if (line.equalsIgnoreCase("break") || line.equalsIgnoreCase("bk")) {
                            // TODO: Respond with current break points
                            writer.print("No breakpoints are set\n" + currentReplyPrefix);
                            writer.flush();
                            continue;
                        } else if (line.equalsIgnoreCase("reg") || line.equalsIgnoreCase("r")) {
                            replyReg = null;

                            receivedReg = true;

                            while (replyReg == null) {
                                Thread.sleep(10);
                            }

                            writer.print(replyReg + currentReplyPrefix);
                            writer.flush();
                            continue;
                        } else if (line.equalsIgnoreCase("next") || line.equalsIgnoreCase("n")) {
                            receivedCommand = true;

                            replyNext = null;
                            receivedNext = true;
                            suspendCPU = false;
                            while (replyNext == null) {
                                Thread.sleep(10);
                            }
                            writer.print(replyNext + currentReplyPrefix);
                            writer.flush();
                            suspendCPU = true;
                            continue;
                        } else if (line.startsWith("disass ") || line.startsWith("d ")) {
                            String startAddr = line.substring(line.indexOf(' '));
                            startAddr = startAddr.substring(0, startAddr.lastIndexOf(' '));
                            startAddr = startAddr.trim();
                            String endAddr = line.substring(line.lastIndexOf(' '));
                            endAddr = endAddr.trim();

                            try {
                                disassembleStart = Integer.parseInt(startAddr, 16);
                                disassembleEnd = Integer.parseInt(endAddr, 16);

                                replyDisassemble = null;
                                receivedDisassemble = true;
                                while (replyDisassemble == null) {
                                    Thread.sleep(10);
                                }

                                writer.print(replyDisassemble + currentReplyPrefix);
                            } catch (Exception e2) {
                                writer.print(currentReplyPrefix);
                            }
                            writer.flush();

                        }
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                // Any broken connection and the state resets itself
                numConnections--;
                suspendCPU = false;
                receivedCommand = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
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
