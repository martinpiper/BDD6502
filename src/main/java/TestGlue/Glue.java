package TestGlue;

import com.bdd6502.*;
import com.loomcom.symon.Cpu;
import com.loomcom.symon.devices.*;
import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;
import com.loomcom.symon.machines.Machine;
import com.loomcom.symon.machines.SimpleMachine;
import com.loomcom.symon.util.HexUtil;
import cucumber.api.PendingException;
import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import mmarquee.automation.AutomationException;
import mmarquee.automation.UIAutomation;
import mmarquee.automation.controls.Application;
import mmarquee.automation.controls.ElementBuilder;
import mmarquee.automation.controls.MenuItem;
import mmarquee.automation.controls.Window;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class Glue {

    Window currentWindow;
    Application application;
    @Given("^starting an automation process \"([^\"]*)\" with parameters \"([^\"]*)\"$")
    public void startingAnAutomationProcessWithParameters(String name, String parameters) throws Throwable {
        // https://mmarquee.github.io/ui-automation/docs/developer.html
        UIAutomation automation = UIAutomation.getInstance();
        if (parameters.isEmpty()) {
            // Empty parameters seems to generate an error internally?
            application = new Application(new ElementBuilder().automation(automation).applicationPath(name));
            application.launchOrAttach();
        } else {
            application = new Application(new ElementBuilder().automation(automation).applicationPath(name).applicationArguments(parameters));
            application.launchOrAttach();
        }
    }

    @Given("^starting an automation process \"([^\"]*)\" with parameters: (.*)$")
    public void startingAnAutomationProcessWithParametersC(String arg0, String arg1) throws Throwable {
        startingAnAutomationProcessWithParameters(arg0, arg1);
    }

    @When("^automation wait for idle$")
    public void automationWaitForIdle() {
        application.waitForInputIdle();
    }

    @When("^automation find window from pattern \"([^\"]*)\"$")
    public void automationFindWindowFromPattern(String arg0) throws Throwable {
        UIAutomation automation = UIAutomation.getInstance();
        currentWindow = automation.getDesktopWindow(Pattern.compile(arg0));
    }

    @When("^automation focus window$")
    public void automationFocusWindow() {
        currentWindow.focus();
    }

    @Then("^automation wait for window close$")
    public void automationWaitForWindowClose() throws InterruptedException {

        while (true) {
            try {
                if (!currentWindow.isEnabled()) break;
            } catch (AutomationException e) {
                System.out.println("Window closed!");
                return;
            }
            Thread.sleep(1000);
            System.out.println("Waiting for window...");
        }
    }

    MenuItem currentMenuItem;
    @When("^automation expand main menu item \"([^\"]*)\"$")
    public void automationExpandMainMenuItem(String name) throws Throwable {
        // Write code here that turns the phrase above into concrete actions
        currentMenuItem = currentWindow.getMainMenu().getMenuItem(name);
        currentMenuItem.expand();
    }

    @When("^automation click current menu item \"([^\"]*)\"$")
    public void automationClickCurrentMenuItem(String pattern) throws Throwable {
        List<MenuItem> items = currentMenuItem.getItems();
        for (MenuItem item : items) {
            System.out.println(item.getName());
        }
        currentMenuItem = currentMenuItem.getMenuItem(Pattern.compile(pattern));
        currentMenuItem.click();
    }

    private class ProfileData {
        boolean isSEI = false;
        int targetAddress;
    }
    static private TreeMap<Integer, ProfileData> profileDataByAddress = new TreeMap<>();
    static private HashSet<Integer> profileDataTargets = new HashSet<>();
    static private int[] profileDataCycles = new int[65536];
    static private int[] profileDataCalls = new int[65536];
    static private Glue glue = null;
    static private Machine machine = null;
    static private int writingAddress = 0;
    static private int comparingAddress = 0;
    static private TreeMap labelMap = new TreeMap();
    static private TreeMap<Integer, String> reverseLabelMap = new TreeMap<Integer, String>();
    static private Map<String, Integer> calculationMap = new TreeMap<String, Integer>();
    static private Map<Integer, Integer> traceMapByte = new TreeMap<Integer, Integer>();
    static private Map<Integer, Integer> traceMapWord = new TreeMap<Integer, Integer>();
    static private Map<Integer, Integer> traceMapByteUpdate = new TreeMap<Integer, Integer>();
    static private Map<Integer, Integer> traceMapWordUpdate = new TreeMap<Integer, Integer>();
    static private List<MemoryBus> devices = new LinkedList<MemoryBus>();
    static private TreeSet ignoreTraceForAddress = new TreeSet();

    static private boolean traceOveride = false;
    static private boolean indentTrace = false;
    static private int lastStackValue = -1;
    static private boolean enableuninitialisedReadProtection = false;
    static private boolean enableuninitialisedReadProtectionWithFail = false;
    static private DisplayBombJack displayBombJack = null;
    static private DisplayC64 displayC64 = null;
    static private AudioExpansion audioExpansion = null;
    static private UserPortTo24BitAddress userPort24BitAddress = null;
    private int pixelsPerInstruction = 8;
    private int instructionsPerDisplayRefresh = 32;
    private int instructionsPerDisplayRefreshCount = 0;

    private int instructionsPerAudioRefresh = 32;
    private int instructionsPerAudioRefreshCount = 0;

    Scenario scenario = null;
    static ScriptEngineManager manager = new ScriptEngineManager();
    static ScriptEngine engine = manager.getEngineByName("JavaScript");

    public static Machine getMachine() {
        return machine;
    }

    public static Glue getGlue() {
        return glue;
    }

    @Before
    public void BeforeHook(Scenario scenario) {
        glue = this;
        this.scenario = scenario;
    }

    @After
    public void AfterHook(Scenario scenario) {
        try {
            if (remoteMonitor != null) {
                sendMonitorCommand("quit");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    static public int valueToIntFast(String valueIn) throws ScriptException {
        return valueToIntFast(valueIn , 10);
    }

    static public int valueToIntFast(String valueIn, int radix) throws ScriptException {
        String origValueIn = valueIn.trim();
        Object found = labelMap.get(origValueIn);
        if (null != found) {
            origValueIn = (String) found;
        }
        if (origValueIn.charAt(0) == '$') {
            Integer ivalue = Integer.parseInt(origValueIn.substring(1), 16);
            return ivalue.intValue();
        }
        if (origValueIn.length() > 2 && origValueIn.charAt(0) == '0' && origValueIn.charAt(1) == 'x') {
            Integer ivalue = Integer.parseInt(origValueIn.substring(2), 16);
            return ivalue.intValue();
        }
        if (origValueIn.charAt(0) == '%') {
            Integer ivalue = Integer.parseInt(origValueIn.substring(1), 2);
            return ivalue.intValue();
        }
        try {
            Integer ivalue = Integer.parseInt(origValueIn, radix);
            return ivalue.intValue();
        } catch (Exception e) {
            throw new ScriptException("Fast fail " + origValueIn);
        }

    }
    static public int valueToInt(String valueIn) throws ScriptException {
        return valueToInt(valueIn , 10);
    }
    static public int valueToInt(String valueIn , int radix) throws ScriptException {
        if (null == valueIn || valueIn.isEmpty()) {
            return -1;
        }
        if ((valueIn.contains("+") == false) && (valueIn.contains("}") == false) && (valueIn.contains("{") == false) && (valueIn.contains(")") == false) && (valueIn.contains("(") == false) && (valueIn.contains("-") == false) && (valueIn.contains("st") == false) && (valueIn.contains("*") == false) && (valueIn.contains("/") == false)) {
            return valueToIntFast(valueIn , radix);
        }

        String origValueIn = radix + ":" + valueIn;
        Integer cachedRet = calculationMap.get(origValueIn);
        if (null != cachedRet) {
            return cachedRet.intValue();
        }

        // Find any labels or numbers in the expression that can be substituted for known labels etc
        Pattern pattern = Pattern.compile("[\\w$%]+");
        Matcher matcher = pattern.matcher(valueIn);

        class ToReplace {
            public int start, end;
            public String replacement;
        }

        List<ToReplace> toReplace = new ArrayList<ToReplace>();

        while (matcher.find()) {
            String value = matcher.group();
            Object found = labelMap.get(value);
            if (null != found) {
                value = (String) found;
            }
            if (value.charAt(0) == '$') {
                Integer ivalue = Integer.parseInt(value.substring(1), 16);
                value = ivalue.toString();
            }
            if (value.length() > 2 && value.charAt(0) == '0' && value.charAt(1) == 'x') {
                Integer ivalue = Integer.parseInt(value.substring(2), 16);
                value = ivalue.toString();
            }
            if (value.charAt(0) == '%') {
                Integer ivalue = Integer.parseInt(value.substring(1), 2);
                value = ivalue.toString();
            }

            if (!value.equals(matcher.group())) {
                ToReplace entry = new ToReplace();
                entry.start = matcher.start();
                entry.end = matcher.end();
                entry.replacement = value;
                toReplace.add(entry);
            }
        }

        // Build the new string in reverse, so it keeps the indexes
        Collections.reverse(toReplace);
        for (ToReplace entry : toReplace) {
            valueIn = valueIn.substring(0, entry.start) + entry.replacement + valueIn.substring(entry.end);
        }

        // Evaluate the expression
        String functions = "function low(i) { return i & 255; }";
        functions += "function lo(i) { return low(i); }";
        functions += "function high(i) { return ~~(i/256); }";
        functions += "function hi(i) { return high(i); }";
        functions += "stC = 1;";
        functions += "stZ = 2;";
        functions += "stI = 4;";
        functions += "stD = 8;";
//		functions += "stB = 16;";
//		functions += "stS = 32;";
        functions += "stV = 64;";
        functions += "stN = 128;";
        Object temp = engine.eval(functions + valueIn);
        String t = temp.toString();
        if (t.compareToIgnoreCase("true") == 0) {
            return 1;
        } else if (t.compareToIgnoreCase("false") == 0) {
            return 0;
        }
        Integer i = null;
        try {
            i = Integer.parseInt(t , radix);
        } catch (Exception e) {
            i = (int) Math.round(Double.parseDouble(t));
        }
        calculationMap.put(origValueIn, i);
        return i.intValue();
    }

    // simple.feature
    @Given("^I have a simple 6502 system$")
    public void i_have_a_simple_6502_system() throws Throwable {
        initMachine();
    }

    @Given("^I have a simple overclocked 6502 system$")
    public void i_have_a_simple_overclocked_6502_system() throws Throwable {
        initMachine();
        machine.getCpu().setOverclock();
        if (displayC64 != null) {
            displayC64.setTheRAM(machine.getRam());
        }
    }

    @Given("^clear all external devices$")
    public void clearDevices() {
        devices.clear();
        if (displayBombJack != null) {
            displayBombJack.getWindow().dispatchEvent(new WindowEvent(displayBombJack.getWindow(), WindowEvent.WINDOW_CLOSING));
        }
        displayBombJack = null;
        if (audioExpansion != null) {
            audioExpansion.close();
        }
        audioExpansion = null;
        userPort24BitAddress = null;
    }

    public void initMachine() throws MemoryRangeException, MemoryAccessException {
        machine = new SimpleMachine();
        machine.getCpu().reset();
        // When the "procedure" returns then the 0 address contains an initialised byte of memory
        machine.getCpu().getBus().write(0, 0);
        labelMap.clear();
        reverseLabelMap.clear();
        traceMapByte.clear();
        traceMapWord.clear();
        traceMapByteUpdate.clear();
        traceMapWordUpdate.clear();
        lastStackValue = -1;
        enableuninitialisedReadProtection = false;
        enableuninitialisedReadProtectionWithFail = false;
        ignoreTraceForAddress.clear();

        profileEnable = false;
        profileClear = false;
        initProfile();
    }

    @Given("^I am using C64 processor port options$")
    public void i_am_using_C_processor_port_options() throws Throwable {
        machine.getBus().setProcessorPort();
    }


    @Given("^I fill memory with (.+)$")
    public void i_fill_memory_with(String arg1) throws Throwable {
        Memory mem = machine.getRam();
        mem.fill(valueToInt(arg1));
    }

    @When("^I enable uninitialised memory read protection$")
    public void i_enable_uninitialised_memory_read_protection() throws Throwable {
        enableuninitialisedReadProtection = true;
    }

    @When("^I enable uninitialised memory read protection with immediate fail$")
    public void i_enable_uninitialised_memory_read_protection_with_immediate_fail() throws Throwable {
        enableuninitialisedReadProtection = true;
        enableuninitialisedReadProtectionWithFail = true;
    }

    @When("^I disable uninitialised memory read protection$")
    public void i_disable_uninitialised_memory_read_protection() throws Throwable {
        enableuninitialisedReadProtection = false;
    }

    @When("^I reset the uninitialised memory read flag$")
    public void i_reset_uninitialised_memory_read_flag() throws Throwable {
        machine.getRam().resetuninitialisedReadOccured();
    }

    @Then("^I assert the uninitialised memory read flag is set$")
    public void i_assert_the_uninitialised_memory_read_flag_is_set() throws Throwable {
        assertThat(machine.getRam().isuninitialisedReadOccured(), is(equalTo(true)));
    }

    @Then("^I assert the uninitialised memory read flag is clear$")
    public void i_assert_the_uninitialised_memory_read_flag_is_clear() throws Throwable {
        assertThat(machine.getRam().isuninitialisedReadOccured(), is(equalTo(false)));
    }

    @Given("^assert on read memory from (.+) to (.+)$")
    public void assertOnReadMemory(String arg1 , String arg2) throws Throwable {
        int start = valueToInt(arg1);
        int end = valueToInt(arg2);
        machine.getRam().setAssertOnRead(start,end,true);
    }

    @Given("^assert on write memory from (.+) to (.+)$")
    public void assertOnWriteMemory(String arg1 , String arg2) throws Throwable {
        int start = valueToInt(arg1);
        int end = valueToInt(arg2);
        machine.getRam().setAssertOnWrite(start,end,true);
    }

    @Given("^assert on exec memory from (.+) to (.+)$")
    public void assertOnExecMemory(String arg1 , String arg2) throws Throwable {
        int start = valueToInt(arg1);
        int end = valueToInt(arg2);
        machine.getRam().setAssertOnExec(start,end,true);
    }

    @Given("^I start writing memory at (.+)$")
    public void i_start_writing_memory_at(String arg1) throws Throwable {
        writingAddress = valueToInt(arg1);
    }

    @When("^I start comparing memory at (.+)$")
    public void i_start_comparing_memory_at(String arg1) throws Throwable {
        comparingAddress = valueToInt(arg1);
    }

    @Given("^I write the following hex bytes$")
    public void i_write_the_following_hex_bytes(List<String> arg1) throws Throwable {
        for (String arg : arg1) {
            String[] values = arg.split(" ");
            int i;
            for (i = 0; i < values.length; i++) {
                if (!values[i].isEmpty()) {
                    machine.getBus().write(writingAddress++, Integer.parseInt(values[i], 16));
                }
            }
        }
    }

    @When("^I assert the following hex bytes are the same$")
    public void i_assert_the_following_hex_bytes_are_the_same(List<String> arg1) throws Throwable {
        for (String arg : arg1) {
            String[] values = arg.split(" ");
            int i;
            for (i = 0; i < values.length; i++) {
                if (!values[i].isEmpty()) {
                    int readValue = machine.getBus().read(comparingAddress, false);
                    int expectedValue = Integer.parseInt(values[i], 16);
                    assertThat("Address is $" + Integer.toHexString(comparingAddress), readValue, is(equalTo(expectedValue)));
                    comparingAddress++;
                }
            }
        }
    }

    @Given("^I write the following bytes$")
    public void i_write_the_following_bytes(List<String> arg1) throws Throwable {
        for (String arg : arg1) {
            machine.getBus().write(writingAddress++, valueToInt(arg));
        }
    }

    @Given("^I assert the following bytes are the same$")
    public void i_assert_the_following_bytes_are_the_same(List<String> arg1) throws Throwable {
        for (String arg : arg1) {
            int readValue = machine.getBus().read(comparingAddress, false);
            int expectedValue = valueToInt(arg);
            assertThat("Address is $" + Integer.toHexString(comparingAddress), readValue, is(equalTo(expectedValue)));
            comparingAddress++;
        }
    }

    @Given("^I write memory at (.+) with (.+)$")
    public void i_write_memory_at_with(String arg1, String arg2) throws Throwable {
        machine.getBus().write(valueToInt(arg1), valueToInt(arg2));
    }

    @Given("^I push a (.+) byte to the stack$")
    public void i_push_byte_to_the_stack(String arg1) throws Throwable {
        machine.getCpu().stackPush(valueToInt(arg1));
    }

    @Given("^I setup a (.+) byte stack slide$")
    public void i_setup_a_byte_stack_slide(String arg1) throws Throwable {
        int i;
        for (i = 0; i < valueToInt(arg1); i++) {
            machine.getCpu().stackPush(0);
        }
    }

    @Given("^That does fail on BRK$")
    public void that_does_fail_on_brk() throws Throwable {
        machine.getCpu().setFailOnBreak();
    }

    @Given("^That does exit on BRK$")
    public void that_does_exit_on_brk() throws Throwable {
        machine.getCpu().setExitOnBreak();
    }

    @Given("^I enable trace$")
    public void i_enable_trace() throws Throwable {
        traceOveride = true;
        indentTrace = false;
        lastStackValue = -1;
    }

    @Given("^I enable trace with indent$")
    public void i_enable_trace_with_indent() throws Throwable {
        traceOveride = true;
        indentTrace = true;
        lastStackValue = -1;
    }

    @Given("^I disable trace$")
    public void i_disable_trace() throws Throwable {
        traceOveride = false;
        indentTrace = false;
        lastStackValue = -1;
    }

    @Given("^ignore address (.+) to (.+) for trace$")
    public void ignoreAddressForTrace(String addressStart, String addressEnd) throws Throwable {
        for (int address = valueToInt(addressStart) ; address < valueToInt(addressEnd) ; address++) {
            ignoreTraceForAddress.add(address);
        }
    }


    int wantCPUPCSuspendHere = -1;
    boolean wantCPUSuspendNext = false;
    int wantCPUSuspendNextReturn = -1;
    boolean wantAPUStep = false;
    boolean wantAPUBreakOnWaitOrPC0 = false;
    public void internalCPUStep(boolean displayTrace) throws Throwable {

        int beforeCycles = machine.getCpu().getClockCycles();
        int beforeAddr = machine.getCpu().getCpuState().pc;

        RemoteDebugger remoteDebugger = RemoteDebugger.getRemoteDebugger();
        if (remoteDebugger != null) {

            testExecutionBreakPoints(remoteDebugger);

            if (remoteDebugger.isReceivedReg()) {
                debuggerUpdateRegs(remoteDebugger);
            }

            handleSuspendLoop(remoteDebugger , RemoteDebugger.kDeviceFlags_CPU);

            int gotoAddr = remoteDebugger.getReceivedGotoAddress();
            if (gotoAddr >= 0) {
                remoteDebugger.clearReceivedGotoAddress();
                machine.getCpu().setProgramCounter(gotoAddr);
            }
        }

        if ((remoteDebugger != null && remoteDebugger.isClearProfiling()) || profileClear) {
            profileClear = false;
            initProfile();
        }

        if (wantCPUSuspendNextReturn >= 0) {
            int tpc = machine.getCpu().getProgramCounter();
            int tir = machine.getRam().safeInvisibleRead(tpc);
            if (tir == 0x60 /*rts opcode*/ && machine.getCpu().getStackPointer() >= wantCPUSuspendNextReturn) {
                wantCPUSuspendNextReturn = -1;
                wantCPUSuspendNext = true;
            }
        }

        if (remoteDebugger != null) {
            if (remoteDebugger.isCurrentDevice(RemoteDebugger.kDeviceFlags_CPU) && remoteDebugger.isReceivedNext()) {
                int tpc = machine.getCpu().getProgramCounter();
                int tir = machine.getRam().safeInvisibleRead(tpc);

                remoteDebugger.clearStepNextReturn();

                if (tir == 0x20 /*jsr opcode*/) {
                    // If jsr is next then calculate the next PC to stop after the jsr
                    tpc += 3; // Skip the opcode
                    wantCPUPCSuspendHere = tpc;
                } else {
                    wantCPUSuspendNext = true;
                }
            }
        }

        // Execute CPU step!
        machine.getCpu().step();

        if (displayTrace) {
            if (!ignoreTraceForAddress.contains(beforeAddr)) {
                String traceLine = getTraceLine(beforeAddr);
                scenario.write(traceLine);
            }
        }

        int beforeOpcode = machine.getCpu().getInstruction();

        if ((remoteDebugger != null && remoteDebugger.isEnableProfiling()) || profileEnable) {
            // Don't count the jsr in the cycles count...
            int cyclesDelta = machine.getCpu().getClockCycles() - beforeCycles;
            for (Map.Entry<Integer, ProfileData> entry : profileDataByAddress.entrySet()) {
                Integer key = entry.getKey();
                ProfileData value = entry.getValue();
                if (value.isSEI == machine.getCpu().getIrqDisableFlag()) {
                    profileDataCycles[value.targetAddress] += cyclesDelta;
                }
            }

            // Handle instruction states
            if (beforeOpcode == 0x20) {
                // Note before the CPU step...
                if (!profileDataByAddress.containsKey(beforeAddr)) {
                    ProfileData profileData = new ProfileData();
                    profileData.isSEI = machine.getCpu().getIrqDisableFlag();
                    profileData.targetAddress = machine.getCpu().getProgramCounter();
                    profileDataByAddress.put(beforeAddr, profileData);
                    profileDataCalls[profileData.targetAddress]++;
                    profileDataTargets.add(profileData.targetAddress);
                }
            }

            if (beforeOpcode == 0x60) {
                int afterAddr = machine.getCpu().getCpuState().pc - 3;  // Try to find the previous jsr...
                profileDataByAddress.remove(afterAddr);
                profileCreateDebug(remoteDebugger);
            }
        }

        if (remoteDebugger != null) {
            if (remoteDebugger.isCurrentDevice(RemoteDebugger.kDeviceFlags_CPU) && remoteDebugger.isReceivedStep()) {
                remoteDebugger.clearStepNextReturn();
                wantCPUSuspendNextReturn = -1;
                wantCPUPCSuspendHere = -1;
                wantCPUSuspendNext = true;
            }

            if (remoteDebugger.isCurrentDevice(RemoteDebugger.kDeviceFlags_CPU) && remoteDebugger.isReceivedReturn()) {
                remoteDebugger.clearStepNextReturn();
                wantCPUSuspendNextReturn = machine.getCpu().getStackPointer();
                wantCPUPCSuspendHere = -1;
                wantCPUSuspendNext = false;
            }
        }


        traceMapByteUpdate.clear();
        traceMapByte.forEach((k, v) -> {
            try {
                int newValue = machine.getRam().read(k, false);
                if (newValue != v) {
                    String foundLabel = reverseLabelMap.get(k);
                    if (StringUtils.isEmpty(foundLabel)) {
                        foundLabel = String.format("$%04x", k);
                    }

                    scenario.write(foundLabel + " set to " + String.format("%3d @ PC:$%04X", newValue, machine.getCpu().getProgramCounter()));
                    traceMapByteUpdate.put(k, newValue);
                }
            } catch (Exception e) {
            }
        });

        traceMapByteUpdate.forEach((k, v) -> {
            traceMapByte.put(k, v);
        });

        traceMapWordUpdate.clear();
        traceMapWord.forEach((k, v) -> {
            try {
                int newValue = machine.getRam().read(k, false) + (256 * machine.getRam().read(k + 1, false));
                if (newValue != v) {
                    String foundLabel = reverseLabelMap.get(k);
                    if (StringUtils.isEmpty(foundLabel)) {
                        foundLabel = String.format("$%04x", k);
                    }
                    scenario.write(foundLabel + " set to " + String.format("$%04X @ PC:$%04X", newValue, machine.getCpu().getProgramCounter()));
                    traceMapWordUpdate.put(k, newValue);
                }
            } catch (Exception e) {
            }
        });

        traceMapWordUpdate.forEach((k, v) -> {
            traceMapWord.put(k, v);
        });

        if (remoteDebugger != null) {
            remoteDebugger.setDisassembleStart(machine.getCpu().getProgramCounter());
        }

        if (machine.getCpu().getFailOnBreak() == true) {
            if (machine.getCpu().getExtraStatus() == Cpu.Extra_BRK) {
                throw new Exception("BRK Hit @" + machine.getCpu().getProgramCounter());
            }
        }
        if ((machine.getCpu().getExtraStatus() & Cpu.Extra_ATest) == Cpu.Extra_ATest) {
            throw new Exception("A has changed @" + machine.getCpu().getProgramCounter());
        }
        if ((machine.getCpu().getExtraStatus() & Cpu.Extra_XTest) == Cpu.Extra_XTest) {
            throw new Exception("X has changed @" + machine.getCpu().getProgramCounter());
        }
        if ((machine.getCpu().getExtraStatus() & Cpu.Extra_YTest) == Cpu.Extra_YTest) {
            throw new Exception("Y has changed @" + machine.getCpu().getProgramCounter());
        }
    }

    private static void initProfile() {
        debugProfile = "";
        profileDataByAddress.clear();
        profileDataTargets.clear();
        Arrays.setAll(profileDataCycles, (index)->0);
        Arrays.setAll(profileDataCalls, (index)->0);
    }

    static String debugProfile = "";
    private static void profileCreateDebug(RemoteDebugger remoteDebugger) {
        debugProfile = "";
        for (int theAddress : profileDataTargets) {
            debugProfile += "$" + HexUtil.wordToHex(theAddress) + " : ";
            String foundLabel = reverseLabelMap.get(theAddress);
            if (StringUtils.isNotEmpty(foundLabel)) {
                debugProfile += foundLabel;
            }
            debugProfile += " : calls " + profileDataCalls[theAddress] + " : cycles " + profileDataCycles[theAddress];
            if (profileDataCalls[theAddress] > 0) {
                debugProfile += " : cycles per call " + ((float)profileDataCycles[theAddress] / profileDataCalls[theAddress]);
            }
            debugProfile += "\n";
        }
        System.setProperty("test.BDD6502.lastProfile", debugProfile);
        if (remoteDebugger != null) {
            remoteDebugger.setProfileLastResult(debugProfile);
        }
    }

    private void handleSuspendLoop(RemoteDebugger remoteDebugger, int deviceFlags) throws MemoryAccessException, InterruptedException {
        boolean wasSuspended = false;
        while (remoteDebugger.isSuspendDevice(deviceFlags)) {
            if (!wasSuspended) {
                handleDisplayEvents(remoteDebugger);
                if (audioExpansion != null) {
                    audioExpansion.setMute(true);
                }
                wasSuspended = true;
            }
            remoteDebugger.setCurrentPrefix(machine.getCpu().getProgramCounter());

            if (remoteDebugger.isDebuggerDisplayChanged()) {
                handleDisplayEvents(remoteDebugger);
            }

            if (remoteDebugger.isReceivedReg()) {
                debuggerUpdateRegs(remoteDebugger);
            }

            if (remoteDebugger.isReceivedDump()) {
                int start = remoteDebugger.getDumpStart();
                int end = remoteDebugger.getDumpEnd();

                byte returnMemory[] = new byte[(end - start) + 1];

                if (remoteDebugger.isCurrentDevice(RemoteDebugger.kDeviceFlags_CPU)) {
                    int i = 0;
                    while (start < end) {
                        returnMemory[i++] = (byte) machine.getRam().safeInvisibleRead(start);
                        start++;
                    }
                }
                if (userPort24BitAddress.isEnableAPU() && remoteDebugger.isCurrentDevice(RemoteDebugger.kDeviceFlags_APU)) {
                    byte[] memory = userPort24BitAddress.getAPUDataMemory();
                    int i = 0;
                    while (start <= end) {
                        returnMemory[i++] = memory[start % memory.length];  // Clamp for the APU memory size
                        start++;
                    }
                }

                remoteDebugger.setReplyDump(returnMemory);
                continue;
            }

            if (remoteDebugger.isReceivedDisassemble()) {
                int start = remoteDebugger.getDisassembleStart();
                int end = remoteDebugger.getDisassembleEnd();
                StringBuilder sb = new StringBuilder();
                if (remoteDebugger.isCurrentDevice(RemoteDebugger.kDeviceFlags_CPU)) {
                    // .C:f6b0  A5 A0       LDA $A0
                    while (start <= end) {
                        sb.append(".C:");
                        int tir = machine.getRam().safeInvisibleRead(start);
                        int targs0 = machine.getRam().safeInvisibleRead(start + 1);
                        int targs1 = machine.getRam().safeInvisibleRead(start + 2);

                        sb.append(machine.getCpu().getCpuState().getInstructionByteStatusForAddress(tir, start, targs0, targs1) + " ");
                        sb.append(String.format("%-13s\n", machine.getCpu().getCpuState().disassembleOpForAddress(tir, start, targs0, targs1)));

                        // Undefined or invalid instructions should still progress the disassembly by 1 byte
                        start += Math.max(machine.getCpu().instructionSizes[tir], 1);
                    }
                }
                if (userPort24BitAddress != null && userPort24BitAddress.isEnableAPU() && remoteDebugger.isCurrentDevice(RemoteDebugger.kDeviceFlags_APU)) {
                    while (start <= end) {
                        sb.append(".C:");
                        String line = userPort24BitAddress.disassembleAPUInstructionAt(start);
                        sb.append(HexUtil.wordToHex(start) + "  " + line + "\n");
                        start += 1;
                    }
                }
                // Ready for the next page
                remoteDebugger.setDisassembleStart(end);
                remoteDebugger.setReplyDisassemble(sb.toString());
                continue;
            }

            Thread.sleep(10);
        }

        if (wasSuspended) {
            // Reset any frame rate compensation
            if (audioExpansion != null) {
                audioExpansion.setMute(false);
            }

            timeSinceLastDebugDisplayTimes = System.currentTimeMillis();
            if (displayBombJack != null) {
                displaySyncFrame = displayBombJack.getFrameNumberForSync();
            }
            lastFramesCount = displaySyncFrame;
            startTime = System.currentTimeMillis() - (1000/60);
        }
    }

    private void handleDisplayEvents(RemoteDebugger remoteDebugger) {
        if (displayBombJack != null && displayBombJack.isVisible()) {
            if (remoteDebugger.getDebuggerDisplay() == RemoteDebugger.DisplayType.kDisplay_Clear) {
                displayBombJack.displayClear();
            } else if (remoteDebugger.getDebuggerDisplay() == RemoteDebugger.DisplayType.kDisplay_Ahead) {
                displayBombJack.displayClearAhead();
            }
            displayBombJack.RepaintWindow();
        }
        if (displayC64 != null && displayC64.isVisible()) {
            displayC64.RepaintWindow();
        }
    }

    private boolean testExecutionBreakPoints(RemoteDebugger remoteDebugger) {
        if (machine.getCpu().getProgramCounter() == wantCPUPCSuspendHere) {
            wantCPUPCSuspendHere = -1;
            remoteDebugger.signalSuspendDevice(RemoteDebugger.kDeviceFlags_CPU);
            remoteDebugger.setCurrentPrefix(machine.getCpu().getProgramCounter());
            String debug = getNextInstructionForDebugger();
            remoteDebugger.setReplyNext(debug);
            return true;
        }

        if (wantCPUSuspendNext) {
            wantCPUSuspendNext = false;
            remoteDebugger.signalSuspendDevice(RemoteDebugger.kDeviceFlags_CPU);
            remoteDebugger.setCurrentPrefix(machine.getCpu().getProgramCounter());
            String debug = getNextInstructionForDebugger();
            remoteDebugger.setReplyNext(debug);
            return true;
        }

        if (remoteDebugger.isReceivedBreakAt(machine.getCpu().getProgramCounter())) {
            wantCPUSuspendNext = false;
            wantCPUPCSuspendHere = -1;
            remoteDebugger.signalSuspendDevice(RemoteDebugger.kDeviceFlags_CPU);
            remoteDebugger.setCurrentPrefix(machine.getCpu().getProgramCounter());
            String debug = getNextInstructionForDebugger();
            remoteDebugger.setReplyNext(debug);
            return true;
        }

        return false;
    }

    private String getNextInstructionForDebugger() {
        int tpc = machine.getCpu().getProgramCounter();
        int tir = machine.getRam().safeInvisibleRead(tpc);
        int targs0 = machine.getRam().safeInvisibleRead(tpc + 1);
        int targs1 = machine.getRam().safeInvisibleRead(tpc + 2);
        return machine.getCpu().getCpuState().toDebuggerTrace(tir, tpc, targs0, targs1, machine.getCpu().getClockCycles());
    }

    private void debuggerUpdateRegs(RemoteDebugger remoteDebugger) throws MemoryAccessException {
        String optionalExtras = "";
        if (UserPortTo24BitAddress.getThisInstance() != null) {
            if (UserPortTo24BitAddress.getThisInstance().isEnableAPU()) {
                optionalExtras += UserPortTo24BitAddress.getThisInstance().getDebugOutputLastState();
            }
        }

        int displayH = 0 , displayV = 0;
        if (displayBombJack != null) {
            displayH = displayBombJack.getDisplayH();
            displayV = displayBombJack.getDisplayV();
            optionalExtras += displayBombJack.getDebug();
        }

        if (audioExpansion != null) {
            optionalExtras += audioExpansion.getDebug();
        }

        remoteDebugger.setReplyReg(machine.getCpu().getCpuState().pc, machine.getCpu().getCpuState().a, machine.getCpu().getCpuState().x, machine.getCpu().getCpuState().y,
                machine.getCpu().getCpuState().sp, machine.getRam().read(0, false), machine.getRam().read(1, false),
                machine.getCpu().getCpuState().getStatusFlag(), displayV, displayH,
                machine.getCpu().getClockCycles(),
                optionalExtras);
    }

    public String getTraceLine(Integer addr) {
        Object label = reverseLabelMap.get(addr);
        if (label != null) {
            scenario.write(label + ":");
        }
        String traceLine = machine.getCpu().getCpuState().toTraceEvent();

        String decorated = traceLine.substring(16, 30);
        boolean changed = false;
        for (int i = 0; i < decorated.length(); i++) {
            int pos = decorated.indexOf("$", i);
            if (pos == -1) {
                break;
            }
            try {
                String hex = decorated.substring(pos + 1, pos + 5);

                int testAddr = Integer.parseInt(hex, 16);

                String foundLabel = reverseLabelMap.get(testAddr);
                if (!StringUtils.isEmpty(foundLabel)) {
                    decorated = decorated.substring(0, pos) + foundLabel + decorated.substring(pos + 5);
                    i += foundLabel.length();
                    changed = true;
                    continue;
                }
            } catch (Exception e) {
            }

            try {
                String hex = decorated.substring(pos + 1, pos + 3);

                int testAddr = Integer.parseInt(hex, 16);

                String foundLabel = reverseLabelMap.get(testAddr);
                if (!StringUtils.isEmpty(foundLabel)) {
                    decorated = decorated.substring(0, pos) + foundLabel + decorated.substring(pos + 5);
                    i += foundLabel.length();
                    changed = true;
                    continue;
                }
            } catch (Exception e) {
            }
        }

        if (changed) {
            traceLine += "    " + decorated;
        }

        if (indentTrace) {
            int indent = 0xff - machine.getCpu().getStackPointer();
            if (lastStackValue == -1) {
                lastStackValue = indent;
            }
            for (int i = 0; i < lastStackValue; ++i) {
                traceLine = " " + traceLine;
            }
            lastStackValue = indent;
        }
        return traceLine;
    }

    public int getNBytesValueAt(int addr, int count) throws Throwable {
        int ret = 0;
        int mul = 1;
        for (int j = 0; j < count; ++j) {
            ret += machine.getBus().read(addr + j, false) * mul;
            mul *= 256;
        }
        return ret;
    }

    public void executeProcedureAtUntilMemEquals(String arg1, String arg2, String arg3) throws Throwable {
        checkScenario();

        String output = String.format("Execute procedure (%s) until (%s) is (%s)", arg1, arg2, arg3);
        scenario.write(output);

        boolean displayTrace = traceOveride;
        String trace = System.getProperty("bdd6502.trace");
        if (null != trace && trace.indexOf("true") != -1) {
            displayTrace = true;
        }

        if (!arg1.isEmpty()) {
            // Set the return address to location 0 if we execute a "procedure"
            machine.getCpu().stackPush(0xff);
            machine.getCpu().stackPush(0xff);
            machine.getCpu().setProgramCounter(valueToInt(arg1));
        }

        int addrToCheck = valueToInt(arg2);
        int valueToCheckFor = valueToInt(arg3);
        int numBytes = 1;
        if (valueToCheckFor > 0xFFFF) {
            numBytes++;
        }
        if (valueToCheckFor > 0xFF) {
            numBytes++;
        }

        machine.getCpu().initRegisterTestStackIfNeeded();
        while (getNBytesValueAt(addrToCheck, numBytes) != valueToCheckFor) {
            Integer addr = machine.getCpu().getCpuState().pc;
            internalCPUStep(displayTrace);
            if (enableuninitialisedReadProtection && machine.getRam().isuninitialisedReadOccured()) {
                processuninitialisedMemoryRead(addr);
                break;
            }
        }
    }

    Set<Integer> pauseUntilNewVBlank = null;

    public void executeProcedureAtForNoMoreThanInstructionsUntilPC(String arg1, String arg2, String arg3) throws Throwable {
        checkScenario();

        String output = String.format("Execute procedure (%s) for no more than (%s) instructions until pc (%s)", arg1, arg2, arg3);
        scenario.write(output);
//		System.out.println(output);

        boolean displayTrace = traceOveride;
        String trace = System.getProperty("bdd6502.trace");
        if (null != trace && trace.indexOf("true") != -1) {
            displayTrace = true;
        }

        displayStateToScenario();

        if (!arg1.isEmpty()) {
            // Set the return address to location 0 if we execute a "procedure"
            machine.getCpu().stackPush(0xff);
            machine.getCpu().stackPush(0xff);
            machine.getCpu().setProgramCounter(valueToInt(arg1));
        }

        int maxInstructions = 0;
        if (!arg2.isEmpty()) {
            maxInstructions = valueToInt(arg2);
        }
        int numInstructions = 0;
        int untilPC = valueToInt(arg3);

        machine.getCpu().initRegisterTestStackIfNeeded();

        long instructionsThisPeriod = 0;
        timeSinceLastDebugDisplayTimes = System.currentTimeMillis();
        if (displayBombJack != null) {
            lastFramesCount = displayBombJack.getFrameNumberForSync();
        } else {
            lastFramesCount = 0;
        }

        long frameDelta = 0;
        int instructionsAvoided = 0;
        // Pushing lots of 0 onto the stack will eventually return to address 1
        while (machine.getCpu().getProgramCounter() > 1) {
            frameDelta = 0;
            if (displayBombJack != null) {
                int joystickBits = 0;
                if (!displayBombJack.getWindow().isPressedUp()) {
                    joystickBits |= 0x1;
                }
                if (!displayBombJack.getWindow().isPressedDown()) {
                    joystickBits |= 0x2;
                }
                if (!displayBombJack.getWindow().isPressedLeft()) {
                    joystickBits |= 0x4;
                }
                if (!displayBombJack.getWindow().isPressedRight()) {
                    joystickBits |= 0x8;
                }
                if (!displayBombJack.getWindow().isPressedFire()) {
                    joystickBits |= 0x10;
                }
                if (enableJoystick1) {
                    machine.getBus().write(0xdc00, joystickBits);
                }
                if (enableJoystick2) {
                    machine.getBus().write(0xdc01, joystickBits);
                }
                if (displayBombJack != null && enableCIA1RasterTimer) {
                    machine.getBus().write(0xdc04, (displayBombJack.getDisplayX(cia1RasterOffsetX) * 62) / 384); // CIA1TimerALo
                    machine.getBus().write(0xdc05, 0); // CIA1TimerAHi
                    machine.getBus().write(0xdc06, displayBombJack.getDisplayYForCIA(cia1RasterOffsetY)); // CIA1TimerBLo
                    machine.getBus().write(0xdc07, displayBombJack.getDisplayYForCIA(cia1RasterOffsetY) >> 8); // CIA1TimerBHi
                }

                // Execute video clocks while running the CPU
                long targetNumberFrames = System.currentTimeMillis() - startTime;
                targetNumberFrames *= displayLimitFPS;
                targetNumberFrames /= 1000;
                int displayFrame = displayBombJack.getFrameNumberForSync() - displaySyncFrame;

                frameDelta = targetNumberFrames - displayFrame;
                if (displayBombJack != null && displayLimitFPS > 0 && System.currentTimeMillis() > (timeSinceLastDebugDisplayTimes + 1000)) {
                    long timeNowMillis = System.currentTimeMillis();
                    long periodMillis = timeNowMillis - timeSinceLastDebugDisplayTimes;
                    if (periodMillis > 0 && pixelsPerInstruction > 0) {
                        long instructionsPerFrame = (1000 * instructionsThisPeriod) / periodMillis;
                        instructionsPerFrame /= displayLimitFPS;
                        long pixelsShortfall = displayBombJack.pixelsInWholeFrame() - (instructionsPerFrame * pixelsPerInstruction);
                        long instructionsShortfall = pixelsShortfall / pixelsPerInstruction;
                        System.out.println("Rendered FPS = " + (displayBombJack.getFrameNumberForSync() - lastFramesCount) + " frameDelta = " + frameDelta + " period=" + periodMillis + " instructionsThisPeriod=" + instructionsThisPeriod + " instructionsPerFrame=" + instructionsPerFrame + " instructionsShortfall=" + instructionsShortfall + " instructionsAvoided=" + instructionsAvoided);
                        instructionsAvoided = 0;
                    }
                    lastFramesCount = displayBombJack.getFrameNumberForSync();
                    instructionsThisPeriod = 0;
                    timeSinceLastDebugDisplayTimes = timeNowMillis;
                }

                if (displayLimitFPS == 0 || frameDelta > 0) {
                    for (int i = 0; i < pixelsPerInstruction; i++) {
                        displayBombJack.calculatePixel();

                        if (UserPortTo24BitAddress.apuInstructionExecuted) {
                            userPort24BitAddress.apuInstructionExecuted = false;
                            RemoteDebugger remoteDebugger = RemoteDebugger.getRemoteDebugger();
                            if (remoteDebugger != null) {
                                if (wantAPUStep) {
                                    wantAPUStep = false;
                                    remoteDebugger.signalSuspendDevice(RemoteDebugger.kDeviceFlags_APU);
                                    remoteDebugger.setCurrentPrefix(machine.getCpu().getProgramCounter());
//                                    String debug = getNextInstructionForDebugger();
                                    String debug = "APU step";
                                    remoteDebugger.setReplyNext(debug);
                                }

                                handleSuspendLoop(remoteDebugger , RemoteDebugger.kDeviceFlags_APU);

                                if (wantAPUBreakOnWaitOrPC0) {
                                    if (userPort24BitAddress.isWaitState()) {
                                        wantAPUBreakOnWaitOrPC0 = false;
                                        wantAPUStep = true;
                                    }
                                }
                            }

                            if (remoteDebugger != null && remoteDebugger.isCurrentDevice(RemoteDebugger.kDeviceFlags_APU) && remoteDebugger.isReceivedNext()) {
                                remoteDebugger.clearStepNextReturn();
                                wantAPUStep = true;
                            }

                            if (remoteDebugger != null && remoteDebugger.isCurrentDevice(RemoteDebugger.kDeviceFlags_APU) && remoteDebugger.isReceivedStep()) {
                                remoteDebugger.clearStepNextReturn();
                                wantAPUStep = true;
                            }

                            if (remoteDebugger != null && remoteDebugger.isCurrentDevice(RemoteDebugger.kDeviceFlags_APU) && remoteDebugger.isReceivedReturn()) {
                                remoteDebugger.clearStepNextReturn();
                                wantAPUBreakOnWaitOrPC0 = true;
                            }
                        }
                    }
                    // If we are limiting FPS then the frame update logic changes
                    if (displayLimitFPS > 0) {
                        if (displayBombJack.getFrameNumberForSync() != lastRepaintedFrame) {
                            displayBombJack.RepaintWindow();
                            lastRepaintedFrame = displayBombJack.getFrameNumberForSync();
                        }
                    } else {
                        instructionsPerDisplayRefreshCount--;
                        if (instructionsPerDisplayRefreshCount <= 0) {
                            displayBombJack.RepaintWindow();
                            instructionsPerDisplayRefreshCount = instructionsPerDisplayRefresh;
                        }
                    }
                }

                if (!displayBombJack.isVisible())
                {
                    break;
                }
            }

            /// Ensure that closing the display causes the execution to stop
            if (displayC64 != null && !displayC64.isVisible())
            {
                break;
            }


            if (audioExpansion != null) {
                if (instructionsPerAudioRefresh > 0) {
                    instructionsPerAudioRefreshCount--;
                    if (instructionsPerAudioRefreshCount <= 0) {
                        audioExpansion.calculateSamples();
                        instructionsPerAudioRefreshCount = instructionsPerAudioRefresh;
                    }
                }
            }

            if (untilPC == machine.getCpu().getProgramCounter()) {
                break;
            }

            int beforeCycles = machine.getCpu().getClockCycles();

            // Try to avoid CPU wasting time
            Integer addr = machine.getCpu().getCpuState().pc;

            if (displayLimitFPS > 0 && frameDelta <= 0) {
                // If we are limiting the frames and no frame needs to be rendered then don't execute instructions
            } else {
                boolean skipInstruction = false;
                if (pauseUntilNewVBlank != null && displayBombJack != null && !displayBombJack.extEXTWANTIRQ() && !pauseUntilNewVBlank.isEmpty()) {
                    if (pauseUntilNewVBlank.contains(addr)) {
                        skipInstruction = true;
//                        displayBombJack.calculatePixelsUntilEXTWANTIRQ();
                    }
                }
                if (!skipInstruction) {
                    internalCPUStep(displayTrace);
                } else {
                    instructionsAvoided++;
                }
            }

            int deltaCycles = machine.getCpu().getClockCycles() - beforeCycles;
            if (deltaCycles > 0) {
                if (displayC64 != null && displayC64.isVisible()) {
                    displayC64.calculatePixelsFor(deltaCycles * 8);
                }
            }

            instructionsThisPeriod++;
            numInstructions++;
            if (maxInstructions > 0) {
                assertThat(numInstructions, is(lessThanOrEqualTo(maxInstructions)));
            }
            if (enableuninitialisedReadProtection && machine.getRam().isuninitialisedReadOccured()) {
                processuninitialisedMemoryRead(addr);
                break;
            }
        }

        displayStateToScenario();

        output = String.format("Executed procedure (%s) for %d instructions", arg1, numInstructions);
        scenario.write(output);
//		System.out.println(output);
    }

    public void displayStateToScenario() {
        if (displayBombJack != null) {
            scenario.write("display timing=" + displayBombJack.getDisplayH() + " , " +  displayBombJack.getDisplayV() + " vblank=" + displayBombJack.getVBlank() + " hsync=" + !displayBombJack.is_hSync() + " vsync=" + !displayBombJack.is_vSync());
        }
    }

    public void processuninitialisedMemoryRead(Integer addr) throws Exception {
        String fullDebug = "uninitialised memory read: " + getTraceLine(addr);
        scenario.write(fullDebug);
        if (enableuninitialisedReadProtectionWithFail) {
            throw new MemoryAccessException(fullDebug);
        }
    }

    private void checkScenario() {
        assertThat("The Cucumber scenario is not set, use:\n@cucumber.api.java.Before\npublic void BeforeHook(cucumber.api.Scenario scenario) { c64.BeforeHook(scenario); }\n.", scenario, is(notNullValue()));
    }

    @When("^I execute the procedure at (.+) for no more than (.+) instructions until PC = (.+)$")
    public void i_execute_the_procedure_at_for_no_more_than_instructions_until_pc(String arg1, String arg2, String arg3) throws Throwable {
        machine.getCpu().setStackPointer(0xff);
        executeProcedureAtForNoMoreThanInstructionsUntilPC(arg1, arg2, arg3);
    }

    @When("^I execute the procedure at (.+) for no more than (.+) instructions$")
    public void i_execute_the_procedure_at_for_no_more_than_instructions(String arg1, String arg2) throws Throwable {
        machine.getCpu().setStackPointer(0xff);
        executeProcedureAtForNoMoreThanInstructionsUntilPC(arg1, arg2, "");
    }

    @When("^I continue executing the procedure for no more than (.+) instructions$")
    public void i_continue_executing_the_procedure_at_for_no_more_than_instructions(String arg1) throws Throwable {
        executeProcedureAtForNoMoreThanInstructionsUntilPC("", arg1, "");
    }

    @When("^I continue executing the procedure for no more than (.+) instructions until PC = (.+)$")
    public void i_continue_executing_the_procedure_at_for_no_more_than_instructions_until_pc(String arg1, String arg2) throws Throwable {
        executeProcedureAtForNoMoreThanInstructionsUntilPC("", arg1, arg2);
    }

    @When("^I execute the procedure at (.+) until return$")
    public void i_execute_the_procedure_at(String arg1) throws Throwable {
        machine.getCpu().setStackPointer(0xff);
        executeProcedureAtForNoMoreThanInstructionsUntilPC(arg1, "", "");
    }

    @When("^I execute the indirect procedure at (.+) until return$")
    public void i_execute_the_indirect_procedure_at(String arg1) throws Throwable {
        machine.getCpu().setStackPointer(0xff);
        int addr = valueToInt(arg1);
        int code = machine.getBus().read(addr) | (machine.getBus().read(addr + 1) << 8);
        executeProcedureAtForNoMoreThanInstructionsUntilPC(Integer.toString(code), "", "");
    }

    @When("^I execute the indirect procedure at (.+) until return or until PC = (.+)$")
    public void i_execute_the_indirect_procedure_at_until_pc(String arg1 , String arg2) throws Throwable {
        machine.getCpu().setStackPointer(0xff);
        int addr = valueToInt(arg1);
        int code = machine.getBus().read(addr) | (machine.getBus().read(addr + 1) << 8);
        executeProcedureAtForNoMoreThanInstructionsUntilPC(Integer.toString(code), "", arg2);
    }

    @When("^I continue executing the procedure until return$")
    public void i_continue_executing_the_procedure_at_until_return() throws Throwable {
        executeProcedureAtForNoMoreThanInstructionsUntilPC("", "", "");
    }

    @When("^I continue executing the procedure until return or until PC = (.+)$")
    public void i_continue_executing_the_procedure_at_until_return_instructions_until_pc(String arg2) throws Throwable {
        executeProcedureAtForNoMoreThanInstructionsUntilPC("", "", arg2);
    }

    @When("^Until (.+) = (.+) execute from (.+)$")
    public void until_equals_execute_from(String arg1, String arg2, String arg3) throws Throwable {
        machine.getCpu().setStackPointer(0xff);
        executeProcedureAtUntilMemEquals(arg3, arg1, arg2);
    }

    @When("^I continue executing until (.+) = (.+)$")
    public void i_continue_executing_until(String arg1, String arg2) throws Throwable {
        executeProcedureAtUntilMemEquals("", arg1, arg2);
    }

    @Then("^I expect to see (.+) equal (.+)$")
    public void i_expect_to_see_equal(String arg1, String arg2) throws Throwable {
        assertThat(machine.getBus().read(valueToInt(arg1), false), is(equalTo(valueToInt(arg2))));
    }

    @Then("^I expect to see (.+) less than (.+)$")
    public void i_expect_to_see_less_than(String arg1, String arg2) throws Throwable {
        assertThat(machine.getBus().read(valueToInt(arg1), false), is(lessThan(valueToInt(arg2))));
    }

    @Then("^I expect to see (.+) greater than (.+)$")
    public void i_expect_to_see_greater_than(String arg1, String arg2) throws Throwable {
        assertThat(machine.getBus().read(valueToInt(arg1), false), is(greaterThan(valueToInt(arg2))));
    }

    @Then("^I expect to see (.+) contain (.+)$")
    public void i_expect_to_see_contain(String arg1, String arg2) throws Throwable {
        assertThat(machine.getBus().read(valueToInt(arg1), false) & valueToInt(arg2), is(equalTo(valueToInt(arg2))));
    }

    @Then("^I expect to see (.+) exclude (.+)$")
    public void i_expect_to_see_exclude(String arg1, String arg2) throws Throwable {
        assertThat(machine.getBus().read(valueToInt(arg1), false) & valueToInt(arg2), is(equalTo(0)));
    }

    public int getRegValue(String arg1) throws Exception {
        int regValue;
        if (arg1.equalsIgnoreCase("a")) {
            regValue = machine.getCpu().getAccumulator();
        } else if (arg1.equalsIgnoreCase("x")) {
            regValue = machine.getCpu().getXRegister();
        } else if (arg1.equalsIgnoreCase("y")) {
            regValue = machine.getCpu().getYRegister();
        } else if (arg1.equalsIgnoreCase("st")) {
            regValue = machine.getCpu().getProcessorStatus() & (128 + 64 + 8 + 4 + 2 + 1);
        } else if (arg1.equalsIgnoreCase("pc")) {
            regValue = machine.getCpu().getProgramCounter();
        } else {
            throw new Exception("Unknown register " + arg1);
        }
        return regValue;
    }

    @Then("^I expect register (.+) equal (.+)$")
    public void i_expect_register_equal(String arg1, String arg2) throws Throwable {
        int regValue = 0;

        regValue = getRegValue(arg1);
        assertThat(regValue, is(equalTo(valueToInt(arg2))));
    }

    @Then("^I expect register (.+) to be less than (.+)$")
    public void i_expect_register_to_be_less_than(String arg1, String arg2) throws Throwable {
        int regValue = 0;

        regValue = getRegValue(arg1);
        assertThat(regValue, is(lessThan(valueToInt(arg2))));
    }

    @Then("^I expect register (.+) to be greater than (.+)$")
    public void i_expect_register_to_be_greater_than(String arg1, String arg2) throws Throwable {
        int regValue = 0;

        regValue = getRegValue(arg1);
        assertThat(regValue, is(greaterThan(valueToInt(arg2))));
    }

    @Then("^I expect register (.+) contain (.+)$")
    public void i_expect_register_contain(String arg1, String arg2) throws Throwable {
        int regValue = 0;

        regValue = getRegValue(arg1);
        assertThat(regValue & valueToInt(arg2), is(equalTo(valueToInt(arg2))));
    }

    @Then("^I expect register (.+) exclude (.+)$")
    public void i_expect_register_exclude(String arg1, String arg2) throws Throwable {
        int regValue = 0;

        regValue = getRegValue(arg1);
        assertThat(regValue & valueToInt(arg2), is(equalTo(0)));
    }

    @When("^I set register (.+) to (.+)$")
    public void i_set_register_to(String arg1, String arg2) throws Throwable {
        if (arg1.equalsIgnoreCase("a")) {
            machine.getCpu().setAccumulator(valueToInt(arg2));
        } else if (arg1.equalsIgnoreCase("x")) {
            machine.getCpu().setXRegister(valueToInt(arg2));
        } else if (arg1.equalsIgnoreCase("y")) {
            machine.getCpu().setYRegister(valueToInt(arg2));
        } else if (arg1.equalsIgnoreCase("st")) {
            machine.getCpu().setProcessorStatus(valueToInt(arg2));
        } else {
            throw new Exception("Unknown register " + arg1);
        }
    }

    // assemble.feature
    @Given("^I create file \"(.*?)\" with$")
    public void i_create_file_with(String arg1, String arg2) throws Throwable {
        BufferedWriter out = new BufferedWriter(new FileWriter(arg1));
        out.write(arg2);
        out.close();
    }

    @Given("^I run the command line: (.*)$")
    public void i_run_the_command_line(String arg1) throws Throwable {
        checkScenario();
        String returnString = runProcessWithOutput(arg1 , true);
        scenario.write(returnString);
    }

    @Given("^I run the command line ignoring return code: (.*)$")
    public void i_run_the_command_line_ignore(String arg1) throws Throwable {
        checkScenario();
        String returnString = runProcessWithOutput(arg1 , false);
        scenario.write(returnString);
    }

    public static String runProcessWithOutput(String arg1 , boolean doThrow) throws Exception {
        StringBuffer sb = new StringBuffer();
        // Write code here that turns the phrase above into concrete actions
        Process p = Runtime.getRuntime().exec(arg1);
        updateStringBufferFromProcess(p, sb);
        p.waitFor();

        if (doThrow) {
            if (p.exitValue() != 0) {
                throw new Exception(String.format("Return code: %d with message '%s'", p.exitValue(), sb.toString()));
            }
        }
        String returnString = String.format("After executing command line '%s' return code: %d with message '%s'\n", arg1, p.exitValue(), sb.toString());
        System.setProperty("test.BDD6502.lastProcessOutput", returnString);
        return returnString;
    }

    public static void updateStringBufferFromProcess(Process p, StringBuffer sb) throws IOException {
        final StringBuffer[] temp = {sb};
        Runnable getter = new Runnable() {
            @Override
            public void run() {
                StringBuffer sb = temp[0];
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

                    String line = "";
                    while (true) {
                        if (!((line = reader.readLine()) != null)) break;
                        sb.append(line + "\n");
                    }

                    reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                    while ((line = reader.readLine()) != null) {
                        sb.append(line + "\n");
                    }

                } catch (IOException e) {
                }
                System.out.println("Process completed");
            }
        };
        Thread thread = new Thread(getter);
        thread.start();
    }

    @Given("^I load prg \"(.*?)\"$")
    public void i_load_prg(String arg1) throws Throwable {
        FileInputStream in = null;
        in = new FileInputStream(arg1);
        int addr = in.read() + (in.read() * 256);
        int c;
        while ((c = in.read()) != -1) {
            machine.getBus().write(addr++, c);
        }
        in.close();
    }

    @Given("^I load crt \"(.*?)\"$")
    public void i_load_crt(String arg1) throws Throwable {
        FileInputStream in = null;
        in = new FileInputStream(arg1);
        in.skip(0x14);
        int version = (in.read() * 256) + in.read();
        int type = (in.read() * 256) + in.read();
        int exrom = in.read();
        int game = in.read();
        in.skip(0x06);  // Unused
        in.skip(0x20);  // Name

        machine.getBus().addCRTInfo(type , exrom , game);

        int c;
        while ((c = in.read()) != -1) {
            in.skip(0x03);
            in.skip(0x02);
            int chipLength = (in.read() * 256) + in.read();
            int chipType = (in.read() * 256) + in.read();
            int chipBank = (in.read() * 256) + in.read();
            int chipAddress = (in.read() * 256) + in.read();
            int chipSize = (in.read() * 256) + in.read();
            Device chip = new Device(chipAddress , chipAddress + chipSize - 1 , "CRTChip " + chipBank + ":$" + Integer.toHexString(chipAddress)) {
                int chipData[] = new int[chipSize];
                @Override
                public void write(int address, int data) throws MemoryAccessException {
                    chipData[address] = data;
                }

                @Override
                public int read(int address, boolean logRead) throws MemoryAccessException {
                    return chipData[address];
                }

                @Override
                public String toString() {
                    return null;
                }
            };
            int addr = 0;
            while (addr < chipSize) {
                chip.write(addr,in.read());
                addr++;
            }
            machine.getBus().addCRTChip(chip,chipBank);
        }
        in.close();
    }

    @Given("^I load bin \"(.*?)\" at (.+)$")
    public void i_load_bin_at(String arg1, String arg2) throws Throwable {
        FileInputStream in = null;
        in = new FileInputStream(arg1);
        int addr = valueToInt(arg2);
        int c;
        while ((c = in.read()) != -1) {
            machine.getBus().write(addr++, c);
        }
        in.close();
    }

    @Given("^I load labels \"(.*?)\"$")
    public void i_load_labels(String arg1) throws Throwable {
        loadLabels(arg1);
    }

    public static void loadLabels(String arg1) throws IOException {
        calculationMap.clear();
        BufferedReader br = new BufferedReader(new FileReader(arg1));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) continue;
            String[] splits = line.split("=");
            splits[0] = splits[0].trim();
            String[] splits2 = splits[1].split(";");
            splits2[0] = splits2[0].trim();
            if (StringUtils.isNumeric(splits[0])) {
                // If the value is numeric this will affect the operation of valueToInt so we avoid this by adding a prefix underscore
                splits[0] = "_" + splits[0];
            }
            // Ignore some reserved keywords
            if (splits[0].compareToIgnoreCase("lo") == 0) {
                continue;
            }
            if (splits[0].compareToIgnoreCase("low") == 0) {
                continue;
            }
            if (splits[0].compareToIgnoreCase("hi") == 0) {
                continue;
            }
            if (splits[0].compareToIgnoreCase("high") == 0) {
                continue;
            }
            labelMap.put(splits[0], splits2[0]);
            try {
                reverseLabelMap.put(new Integer(valueToInt(splits2[0])), splits[0]);
            } catch (Exception e) {
                // Ignore
            }
        }
        br.close();
    }

    @When("^I hex dump memory between (.+) and (.+)$")
    public void i_hex_dump_memory_between_$c_and_$c(String start, String end) throws Throwable {
        checkScenario();
        int addrStart = valueToInt(start);
        int addrEnd = valueToInt(end);
        int cr = 0;
        String hexOutput = "";
        String section = "";
        while (addrStart < addrEnd) {
            if (cr == 0) {
                hexOutput += String.format("%2s:", Integer.toHexString(addrStart).replace(' ', '0'));
            }

            if (cr == 8) {
                hexOutput += " ";
            }

            int theByte = machine.getBus().read(addrStart, false);
            String hex = String.format("%2s", Integer.toHexString(theByte)).replace(' ', '0');

            if (CharUtils.isAsciiPrintable((char) theByte)) {
                section += (char) theByte;
            } else {
                section += '.';
            }

            hexOutput += " " + hex;
            cr += 1;
            if (cr >= 16) {
                hexOutput += " : " + section;
                hexOutput += "\n";
                cr = 0;
                section = "";
            }

            addrStart += 1;
        }

        if (!section.isEmpty()) {
            hexOutput += " : " + section;
        }

        System.setProperty("test.BDD6502.lastHexDump", hexOutput);
        scenario.write(hexOutput);
//		System.out.print(hexOutput);
    }

    @Then("^I expect memory (.+) to equal memory (.+)$")
    public void i_expect_to_see_memory_equal_memory(String arg1, String arg2) throws Throwable {
        assertThat(machine.getBus().read(valueToInt(arg1), false), is(equalTo(machine.getBus().read(valueToInt(arg2)))));
    }

    @Then("^I expect memory (.+) to contain memory (.+)$")
    public void i_expect_to_see_memory_contain_memory(String arg1, String arg2) throws Throwable {
        assertThat(machine.getBus().read(valueToInt(arg1), false) & machine.getBus().read(valueToInt(arg2), false), is(equalTo(machine.getBus().read(valueToInt(arg2), false))));
    }

    @Then("^I expect memory (.+) to exclude memory (.+)$")
    public void i_expect_to_see_memory_exclude_memory(String arg1, String arg2) throws Throwable {
        assertThat(machine.getBus().read(valueToInt(arg1), false) & machine.getBus().read(valueToInt(arg2), false), is(equalTo(0)));
    }

    @Given("^I set label (.+) equal to (.+)$")
    public void iSetLabelFooEqualTo(String arg1, String arg2) throws Throwable {
        int val = valueToInt(arg2);
        String sval = Integer.toString(val);
        labelMap.put(arg1, sval);
        reverseLabelMap.put(new Integer(sval), arg1);
    }

    @Then("^Joystick (\\d+) is (NONE|U|D|L|R|UL|UR|DL|DR|FIRE|UFIRE|ULFIRE|URFIRE|DFIRE|DLFIRE|DRFIRE|LFIRE|RFIRE)$")
    public void joystick_X_is_DIR(int arg1, String arg2) throws Throwable {
        assertThat(arg1, is(lessThan(3)));
        assertThat(arg1, is(greaterThan(0)));
        int destMem = 0xDC00;
        if (arg1 == 1) {
            destMem = 0xDC01;
        }
        if ("NONE".equals(arg2)) {
            machine.getBus().write(destMem, 255);
        } else if ("U".equals(arg2)) {
            machine.getBus().write(destMem, 254);
        } else if ("D".equals(arg2)) {
            machine.getBus().write(destMem, 253);
        } else if ("L".equals(arg2)) {
            machine.getBus().write(destMem, 251);
        } else if ("R".equals(arg2)) {
            machine.getBus().write(destMem, 247);
        } else if ("UL".equals(arg2)) {
            machine.getBus().write(destMem, 250);
        } else if ("UR".equals(arg2)) {
            machine.getBus().write(destMem, 246);
        } else if ("DL".equals(arg2)) {
            machine.getBus().write(destMem, 249);
        } else if ("DR".equals(arg2)) {
            machine.getBus().write(destMem, 245);
        }

        if ("FIRE".equals(arg2)) {
            machine.getBus().write(destMem, 239);
        } else if ("UFIRE".equals(arg2)) {
            machine.getBus().write(destMem, 238);
        } else if ("DFIRE".equals(arg2)) {
            machine.getBus().write(destMem, 237);
        } else if ("LFIRE".equals(arg2)) {
            machine.getBus().write(destMem, 235);
        } else if ("RFIRE".equals(arg2)) {
            machine.getBus().write(destMem, 231);
        } else if ("ULFIRE".equals(arg2)) {
            machine.getBus().write(destMem, 234);
        } else if ("URFIRE".equals(arg2)) {
            machine.getBus().write(destMem, 230);
        } else if ("DLFIRE".equals(arg2)) {
            machine.getBus().write(destMem, 233);
        } else if ("DRFIRE".equals(arg2)) {
            machine.getBus().write(destMem, 229);
        }
    }

    @When("^I reset the cycle count$")
    public void iResetTheCycleCount() throws Throwable {
        machine.getCpu().resetClockCycles();
    }

    @Then("^I expect the cycle count to be no more than (.+) cycles$")
    public void iExpectTheCycleCountToBeNoMoreThanCycles(String arg0) throws Throwable {
        int iarg0 = valueToInt(arg0);
        scenario.write(String.format("Checking cycles %d with %s delta %d percent %f%%\n", machine.getCpu().getClockCycles(), arg0, machine.getCpu().getClockCycles() - iarg0, 100.0f * machine.getCpu().getClockCycles() / iarg0));
        assertThat(machine.getCpu().getClockCycles(), is(lessThanOrEqualTo(iarg0)));
    }

    @Then("^assert that \"([^\"]*)\" is true$")
    public void assert_that_true(String arg1) throws Throwable {
        assertThat(valueToInt(arg1), is(equalTo(1)));
    }

    @Then("^assert that \"([^\"]*)\" is false$")
    public void assert_that_false(String arg1) throws Throwable {
        assertThat(valueToInt(arg1), is(equalTo(0)));
    }

    @Given("^I enable trace (byte|word) at (.+)$")
    public void iEnableTraceByte_Word_atMem(String mode, String location) throws Throwable {
        int address = valueToInt(location);
        int currentValue = 0;
        if (mode.equals("byte")) {
            currentValue = machine.getRam().read(address, false);
            traceMapByte.put(address, currentValue);
            scenario.write("initial value of trace " + location + " = " + currentValue);
        } else if (mode.equals("word")) {
            currentValue = machine.getRam().read(address, false) + (256 * machine.getRam().read(address + 1, false));

            traceMapWord.put(address, currentValue);
            scenario.write("initial value of trace " + location + " = " + String.format("$%04X", currentValue));
        }

    }

    @Given("^I disable trace (byte|word) at (.+)$")
    public void iDisableTraceByte_Word_atMem(String mode, String location) throws Throwable {
        int address = valueToInt(location);
        if (mode.equals("byte")) {
            traceMapByte.remove(address);
        } else if (mode.equals("word")) {
            traceMapWord.remove(address);
        }
    }

    @Then("^I compare memory range (.+)-(.+) to (.+)$")
    public void iCompareMemoryRangeTo(String start, String end, String other) throws Throwable {
        int startAddr = valueToInt(start);
        int endAddr = valueToInt(end);
        int compareAddr = valueToInt(other);
        Memory ram = machine.getRam();
        for (int addr = startAddr; addr <= endAddr; addr++, compareAddr++) {
            assertThat(ram.read(addr, false), is(equalTo(ram.read(compareAddr, false))));
        }
    }

    @Given("^I install PrintIODevice at (.+)$")
    public void iInstallPrintIODeviceAt(String location) throws Throwable {
        int startAddr = valueToInt(location);
        machine.getBus().addDevice(new PrintIODevice(startAddr, startAddr + 0xFF, "IOPrint", scenario));
    }

    @Then("^I expect IODevice buffer to equal \"(.+)\"$")
    public void iExpectIODeviceBufferToEqual(String value) throws Throwable {
        SortedSet<Device> devices = machine.getBus().getDevices();
        assertThat(devices.isEmpty(), is(equalTo(false)));
        Iterator<Device> it = devices.iterator();
        boolean found = false;
        while (it.hasNext()) {
            Device d = it.next();
            if (d.getName() == "IOPrint") {
                found = true;
                String buffer = d.toString();
                assertThat(value, is(equalTo(buffer)));
                break;
            }
        }
        if (found == false) {
            scenario.write("Unable to find IOPrint device");
            assertThat(0, is(not(1))); // force a fail
        }

    }

    @When("^property \"([^\"]*)\" is set to string \"([^\"]*)\"$")
    public void property_is_set_to(String arg1, String arg2) throws Throwable {
        System.setProperty(arg1, arg2);
        devicesUpdateProperties();
    }

    void devicesUpdateProperties() {
        if (userPort24BitAddress != null) {
            userPort24BitAddress.propertiesUpdated();
        }
    }

    @Then("^property \"([^\"]*)\" must contain string \"([^\"]*)\"$")
    public void property_must_contain(String arg1, String arg2) throws Throwable {
        assertThat(System.getProperty(arg1), containsString(arg2));
    }

    @Then("^property \"([^\"]*)\" must contain string ignoring whitespace \"([^\"]*)\"$")
    public void property_must_contain_ignoring_whitespace(String arg1, String arg2) throws Throwable {
        assertThat(System.getProperty(arg1).replaceAll("\\s+", ""), containsString(arg2.replaceAll("\\s+", "")));
    }

    @Given("^a new video display$")
    public void aNewVideoDisplay() throws IOException {
        if (displayBombJack != null) {
            displayBombJack.getWindow().dispatchEvent(new WindowEvent(displayBombJack.getWindow(), WindowEvent.WINDOW_CLOSING));
        }
        displayBombJack = new DisplayBombJack();
        devices.add(displayBombJack);
    }

    @Given("^a new C64 video display$")
    public void aNewC64VideoDisplay() throws IOException {
        if (displayC64 != null) {
            displayC64.getWindow().dispatchEvent(new WindowEvent(displayC64.getWindow(), WindowEvent.WINDOW_CLOSING));
        }
        displayC64 = new DisplayC64();
    }

    @Given("^a new video display with 16 colours$")
    public void aNewVideoDisplay16Colours() throws IOException {
        if (displayBombJack != null) {
            displayBombJack.getWindow().dispatchEvent(new WindowEvent(displayBombJack.getWindow(), WindowEvent.WINDOW_CLOSING));
        }
        displayBombJack = new DisplayBombJack();
        displayBombJack.make16Colours();
        devices.add(displayBombJack);
    }

    @Given("^a new video display with overscan and 16 colours$")
    public void aNewVideoOverscanDisplay16Colours() throws IOException {
        if (displayBombJack != null) {
            displayBombJack.getWindow().dispatchEvent(new WindowEvent(displayBombJack.getWindow(), WindowEvent.WINDOW_CLOSING));
        }
        displayBombJack = new DisplayBombJack();
        displayBombJack.setWithOverscan(true);
        displayBombJack.make16Colours();
        devices.add(displayBombJack);
    }

    @Given("^enable video display bus debug output$")
    public void enableVideoBusDebug() throws IOException {
        displayBombJack.enableDebugData();
    }

    @Given("^enable user port bus debug output$")
    public void enableUserPortBusDebug() throws IOException {
        userPort24BitAddress.enableDebugData();
    }

    @Given("^a new audio expansion$")
    public void aNewAudioExpansion() throws IOException {
        if (audioExpansion != null) {
            audioExpansion.close();
        }
        audioExpansion = new AudioExpansion();
        audioExpansion.start();
        devices.add(audioExpansion);
    }

    @Given("^enable APU mode$")
    public void enableAPUMode() throws IOException {
        APUData apuData = new APUData();
        devices.add(apuData);
        userPort24BitAddress.setEnableAPU(displayBombJack , apuData);
        displayBombJack.setCallbackAPU(userPort24BitAddress);
    }

    @Given("^APU clock divider (\\d+)$")
    public void apu_clock_divider(int divider) throws Throwable {
        userPort24BitAddress.setSetAPUClockDivider(divider);
    }

    @Given("^APU memory clock divider (\\d+)$")
    public void apu_memory_clock_divider(int divider) throws Throwable {
        userPort24BitAddress.setSetAPUMemoryClockDivider(divider);
    }

    @Given("^show video window$")
    public void showVideoWindow() {
        displayBombJack.InitWindow();
    }

    @Given("^show C64 video window$")
    public void showC64VideoWindow() {
        displayC64.InitWindow();
    }

    @Given("^render a video display frame$")
    public void renderAVideoDisplayFrame() {
        displayBombJack.calculateAFrame();
        displayBombJack.RepaintWindow();
    }

    @Given("^render a C64 video display frame$")
    public void renderAC64VideoDisplayFrame() {
        displayC64.calculateAFrame();
        displayC64.RepaintWindow();
    }

    @Given("^render (.*) video display frames$")
    public void renderVideoDisplayFrames(Integer numFrames) {
        while (numFrames-- > 0) {
            displayBombJack.calculateAFrame();
            displayBombJack.RepaintWindow();
        }
    }

    @Given("^render a video display until H=(.*) and V=(.*)$")
    public void renderAVideoDisplayUntilHV(String h, String v) throws ScriptException {
        displayBombJack.calculatePixelsUntil(valueToInt(h),valueToInt(v));
        displayBombJack.RepaintWindow();
    }

    @Given("^render a video display until vsync$")
    public void renderAVideoDisplayUntilVSync() throws ScriptException {
        displayBombJack.calculatePixelsUntilVSync();
        displayBombJack.RepaintWindow();
    }

    @Given("^a user port to 24 bit bus is installed$")
    public void aUserportToBitBusIsInstalled() throws MemoryRangeException {
        userPort24BitAddress = new UserPortTo24BitAddress(scenario);
        userPort24BitAddress.addDevice(displayBombJack);
        userPort24BitAddress.addDevice(audioExpansion);
        machine.getBus().addDevice(userPort24BitAddress);
    }

    @Given("^a simple user port to 24 bit bus is installed$")
    public void aSimpleUserportToBitBusIsInstalled() throws MemoryRangeException {
        aUserportToBitBusIsInstalled();
        userPort24BitAddress.setSimpleMode(true);
    }

    @Given("^the layer has overscan$")
    public void theLayerHasOverscan() {
        displayBombJack.getLastLayerAdded().setWithOverscan(true);
    }

    @Given("^add a StaticColour layer for palette index '(.*)'$")
    public void addAStaticColourLayerForPaletteIndex(String paletteIndex) throws ScriptException {
        displayBombJack.addLayer((new StaticColour(valueToInt(paletteIndex))));
    }

    @Given("^add a GetBackground layer fetching from layer index '(.*)'$")
    public void addAGetBackgroundLayerFetchingFromLayerIndex(String layerIndex) throws ScriptException {
        displayBombJack.addLayer((new GetBackground(valueToInt(layerIndex))));
    }

    @Given("^add a Mode7 layer with registers at '(.*)' and addressEx '(.*)'$")
    public void addAModeDisplayWithRegistersAtXaAndAddressExX(String addressRegisters, String addressExMap) throws ScriptException {
        displayBombJack.addLayer(new Mode7(valueToInt(addressRegisters), valueToInt(addressExMap)));
    }

    @Given("^add a Tiles layer with registers at '(.*)' and screen addressEx '(.*)' and planes addressEx '(.*)'$")
    public void addATilesDisplayWithRegistersAtXaAndAddressExX(String addressRegisters, String addressExScreen, String addressExPlanes) throws ScriptException {
        displayBombJack.addLayer(new Tiles(valueToInt(addressRegisters), valueToInt(addressExScreen), valueToInt(addressExPlanes)));
    }

    @Given("^add a Chars layer with registers at '(.*)' and addressEx '(.*)'$")
    public void addACharsDisplayWithRegistersAtXaAndAddressExX(String addressRegisters, String addressEx) throws ScriptException {
        displayBombJack.addLayer(new Chars(valueToInt(addressRegisters), valueToInt(addressEx)));
    }

    @Given("^add a Chars V4.0 layer with registers at '(.*)' and screen addressEx '(.*)' and planes addressEx '(.*)'$")
    public void addACharsV4_0DisplayWithRegistersAtXaAndAddressExX(String addressRegisters, String addressExScreen, String addressExPlanes) throws ScriptException {
        displayBombJack.addLayer(new Chars(valueToInt(addressRegisters), valueToInt(addressExScreen), valueToInt(addressExPlanes)));
    }

    @Given("^add a Sprites layer with registers at '(.*)' and addressEx '(.*)'$")
    public void addASpritesLayerWithRegistersAtXAndAddressExX(String addressRegisters, String addressEx) throws ScriptException {
        displayBombJack.addLayer(new Sprites(valueToInt(addressRegisters), valueToInt(addressEx)));
    }

    @Given("^add a Sprites2 layer with registers at '(.*)' and addressEx '(.*)'$")
    public void addASprites2LayerWithRegistersAtXAndAddressExX(String addressRegisters, String addressEx) throws ScriptException {
        displayBombJack.addLayer(new Sprites2(valueToInt(addressRegisters), valueToInt(addressEx)));
    }

    @Given("^add a Sprites2 layer with registers at '(.*)' and addressEx '(.*)' and running at (.*)MHz$")
    public void addASprites2LayerWithRegistersAtXAndAddressExXMHz(String addressRegisters, String addressEx , String MHz) throws ScriptException {
        double clockMultiplier = Double.parseDouble(MHz) / 12.096;
        displayBombJack.addLayer(new Sprites2(valueToInt(addressRegisters), valueToInt(addressEx) , clockMultiplier));
    }

    @Given("^add a Sprites3 layer with registers at '(.*)' and addressEx '(.*)'$")
    public void addASprites3LayerWithRegistersAtXAndAddressExX(String addressRegisters, String addressEx) throws ScriptException {
        displayBombJack.addLayer(new Sprites3(valueToInt(addressRegisters), valueToInt(addressEx)));
    }

    @Given("^add a Vector layer with registers at '(.*)' and addressEx '(.*)'$")
    public void addAVectorDisplayWithRegistersAtXaAndAddressExX(String addressRegisters, String addressExMap) throws ScriptException {
        displayBombJack.addLayer(new VectorPlane(valueToInt(addressRegisters), valueToInt(addressExMap)));
    }

    @Given("^add a 2-to-1 merge layer with registers at '(.*)'$")
    public void add2to1MergeLayerWithRegistersAt(String addressRegisters) throws ScriptException {
        displayBombJack.addLayer(new MergeNTo1(2,valueToInt(addressRegisters)));
    }

    @Given("^the layer uses exact address matching$")
    public void makeExactEBBSAddress() throws ScriptException {
        displayBombJack.getLastLayerAdded().makeExactEBBSAddress();
    }

    @Given("^the layer has 16 colours$")
    public void make16Colours() throws ScriptException {
        displayBombJack.getLastLayerAdded().make16Colours();
    }

    @Given("^write data from file \"([^\"]*)\" to 24bit bus at '(.*)' and addressEx '(.*)'$")
    public void writeDataFromFileToBitBusAtXCAndAddressExX(String filename, String address, String addressEx) throws Throwable {
        for (MemoryBus device : devices) {
            device.writeDataFromFile(valueToInt(address), valueToInt(addressEx), filename);
        }
    }

    @Given("^write data byte '(.*)' to 24bit bus at '(.*)' and addressEx '(.*)'$")
    public void writeDataByteToBitBusAtXCAndAddressExX(String value, String address, String addressEx) throws Throwable {
        for (MemoryBus device : devices) {
            device.writeData(valueToInt(address), valueToInt(addressEx), valueToInt(value));
        }
    }

    @Given("^fill data byte '(.*)' to 24bit bus at '(.*)' to '(.*)' stride '(.*)' and addressEx '(.*)'$")
    public void fillDataByteToBitBusAtXCAndAddressExX(String value, String address, String addressTo, String stride, String addressEx) throws Throwable {
        for (MemoryBus device : devices) {
            for (int addr = valueToInt(address) ; addr < valueToInt(addressTo) ; addr += valueToInt(stride)) {
                device.writeData(addr, valueToInt(addressEx), valueToInt(value));
            }
        }
    }

    @When("^rendering the video until window closed$")
    public void renderingTheVideoUntilWindowClosed() throws InterruptedException {
        displayBombJack.writeDebugBMPsToLeafFilename(null);
        renderAVideoDisplayFrame();
        displayUntilWindowClosed();
    }

    @When("^display until window closed$")
    public void displayUntilWindowClosed() throws InterruptedException {
        while (displayBombJack.isVisible()) {
            displayBombJack.RepaintWindow();

            if (audioExpansion != null) {
                if (instructionsPerAudioRefresh > 0) {
                    for (int i = 0; i < 100; i++) {
                        audioExpansion.calculateSamples();
                    }
                }
            }

            Thread.sleep(1);
        }
    }

    @Given("^video display saves debug BMP images to leaf filename \"([^\"]*)\"$")
    public void videoDisplaySavesDebugBMPImagesToLeafFilename(String leafFilename) throws Throwable {
        displayBombJack.writeDebugBMPsToLeafFilename(leafFilename);
    }

    @Given("^video display does not save debug BMP images$")
    public void videoDisplayDoesNotSaveDebugBMPImages() throws Throwable {
        displayBombJack.writeDebugBMPsToLeafFilename(null);
    }

    @Given("^C64 video display saves debug BMP images to leaf filename \"([^\"]*)\"$")
    public void C64videoDisplaySavesDebugBMPImagesToLeafFilename(String leafFilename) throws Throwable {
        displayC64.writeDebugBMPsToLeafFilename(leafFilename);
    }

    @Given("^C64 video display does not save debug BMP images$")
    public void C64videoDisplayDoesNotSaveDebugBMPImages() throws Throwable {
        displayC64.writeDebugBMPsToLeafFilename(null);
    }

    static boolean bufferedImagesEqual(BufferedImage img1, BufferedImage img2) {
        if (img1.getWidth() == img2.getWidth() && img1.getHeight() == img2.getHeight()) {
            for (int x = 0; x < img1.getWidth(); x++) {
                for (int y = 0; y < img1.getHeight(); y++) {
                    if (img1.getRGB(x, y) != img2.getRGB(x, y))
                        return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    @Then("^expect image \"([^\"]*)\" to be identical to \"([^\"]*)\"$")
    public void expectImageToBeIdenticalTo(String expectedFilename, String testFilename) throws Throwable {
        BufferedImage expected = ImageIO.read(new File(expectedFilename));
        BufferedImage test = ImageIO.read(new File(testFilename));
        // Vice "scrsh" command seems to lock the output file for a while?
        // So we will retry
        if (expected == null) {
            Thread.sleep(500);
            expected = ImageIO.read(new File(expectedFilename));
        }
        if (test == null) {
            Thread.sleep(500);
            test = ImageIO.read(new File(testFilename));
        }

        assertThat("Images differ",bufferedImagesEqual(expected,test),is(true));
    }

    @Given("^video display processes (.*) pixels per instruction$")
    public void videoDisplayProcessesPixelsPerInstruction(String numPixels) throws ScriptException {
        pixelsPerInstruction = valueToInt(numPixels);
    }

    @Given("^video display refresh window every (.*) instructions$")
    public void videoDisplayRefreshWindowEveryInstructions(String refreshEvery) throws ScriptException {
        instructionsPerDisplayRefresh = valueToInt(refreshEvery);
    }

    @Given("^audio refresh window every (.*) instructions$")
    public void audioRefreshWindowEveryInstructions(String refreshEvery) throws ScriptException {
        instructionsPerAudioRefresh = valueToInt(refreshEvery);
    }

    @Given("^audio refresh is independent$")
    public void audioRefreshIsIndependent() throws ScriptException {
        audioExpansion.startThread();
    }

    long startTime  = 0;
    double displayLimitFPS = 0;
    int displaySyncFrame = 0;
    long timeSinceLastDebugDisplayTimes = 0;
    long lastFramesCount = 0;
    long lastRepaintedFrame = 0;
    @Given("^limit video display to (.*) fps$")
    public void limitVideoDisplayToFps(double arg0) {
        displaySyncFrame = displayBombJack.getFrameNumberForSync();
        lastFramesCount = displaySyncFrame;
        startTime = System.currentTimeMillis();
        displayLimitFPS = arg0;
    }

    @Given("^unlimit video display fps$")
    public void unlimitVideoDisplayToFps() {
        displayLimitFPS = 0;
    }

    boolean enableJoystick1 = false;
    @Given("^video display add joystick to port 1$")
    public void addJoystickPort1() {
        enableJoystick1 = true;
    }

    boolean enableJoystick2 = false;
    @Given("^video display add joystick to port 2$")
    public void addJoystickPort2() {
        enableJoystick2 = true;
    }

    boolean enableCIA1RasterTimer = false;
    int cia1RasterOffsetX = 0;
    int cia1RasterOffsetY = 0;
    @Given("^video display add CIA1 timers with raster offset (\\d+) , (\\d+)$")
    public void video_display_add_CIA_timers_with_raster_offset(int offsetX, int offsetY) throws Throwable {
        enableCIA1RasterTimer = true;
        cia1RasterOffsetX = offsetX;
        cia1RasterOffsetY = cia1RasterOffsetY;
    }

    Socket remoteMonitor;
    @Given("^connect to remote monitor at TCP \"([^\"]*)\" port \"([^\"]*)\"$")
    public void connect_to_remote_monitor_at_TCP_port(String host, Integer port) throws Throwable {
        remoteMonitor = new Socket(host, port);
    }

    public void sendMonitorCommand(String command) throws IOException, InterruptedException {
        command = command.replace("%WCD%",System.getProperty("user.dir").replace("/", "\\"));
        // Make sure the input is empty first
        getMonitorReply();
        // This space padding is needed because of a bug in Vice remote monitor, the EOL at the end is important for commands with different length to be parsed correctly
        String realCommand = command + "                                                                                                                                                                      \n";
        OutputStream os = remoteMonitor.getOutputStream();
        os.write(realCommand.getBytes());
    }

    public void waitMonitorReply() throws IOException, InterruptedException {
        Thread.sleep(100);  // Urghh, why do we have to? :D
        InputStream is = remoteMonitor.getInputStream();

        while (is.available() == 0) {
            Thread.sleep(10);
        }
    }

    public String getMonitorReply() throws IOException, InterruptedException {
        String reply = "";
        InputStream is = remoteMonitor.getInputStream();

        while (is.available() > 0) {
            int toRead = is.available();
            byte buffer[] = new byte[toRead];
            is.read(buffer);

            String fragment = new String(buffer);
            reply += fragment;
        }

        if (!reply.isEmpty()) {
            scenario.write("Got monitor reply: " + reply);
        }

        System.setProperty("test.BDD6502.previousMonitorReply", System.getProperty("test.BDD6502.lastMonitorReply", ""));

        System.setProperty("test.BDD6502.lastMonitorReply", reply);

        return reply;
    }

    @When("^send remote monitor command \"([^\"]*)\"$")
    public void send_remote_monitor_command(String command) throws Throwable {
        sendMonitorCommand(command);
        waitMonitorReply();
        getMonitorReply();
    }

    @When("^send remote monitor command without parsing \"(.*)\"$")
    public void send_remote_monitor_command_without_parsing(String command) throws Throwable {
        sendMonitorCommand(command);
        waitMonitorReply();
        getMonitorReply();
    }

    @When("^send remote monitor command \"([^\"]*)\" \"([^\"]*)\"$")
    public void send_remote_monitor_command2(String command, String param) throws Throwable {
        command += " $";
        command += Integer.toHexString(valueToInt(param));
        sendMonitorCommand(command);
        waitMonitorReply();
        getMonitorReply();
    }

    @When("^remote monitor wait for hit$")
    public void remote_monitor_wait_for_hit() throws Throwable {
        sendMonitorCommand("x");
        waitMonitorReply();
        getMonitorReply();
        // Then send a command to get a reply and sync the remote monitor
        sendMonitorCommand("r");
        waitMonitorReply();
        getMonitorReply();
    }

    @When("^remote monitor wait for (.*) hits$")
    public void remote_monitor_wait_for_hit(Integer numHits) throws Throwable {
        while (numHits-- > 0) {
            remote_monitor_wait_for_hit();
        }
    }

    @When("^remote monitor continue without waiting$")
    public void remote_monitor_continue_without_waiting() throws Throwable {
        sendMonitorCommand("x");
        getMonitorReply();
    }

    @When("^disconnect remote monitor$")
    public void disconnect_remote_monitor() throws Throwable {
        remoteMonitor.close();
        remoteMonitor = null;
    }

    @Given("^wait for (\\d+) milliseconds$")
    public void waitForMilliseconds(int milliseconds) throws InterruptedException {
        Thread.sleep(milliseconds);
    }

    @When("^enable remote debugging$")
    public void enable_remote_debugging() {
        RemoteDebugger.startRemoteDebugger();
    }

    @When("^wait for debugger connection$")
    public void wait_for_debugger_connection() throws InterruptedException {
        System.out.println(">> Waiting for debugger connection");
        while (RemoteDebugger.getRemoteDebugger().getNumConnections() <= 0) {
            Thread.sleep(100);
        }
        System.out.println("<< Got debugger connection");
    }

    @And("^wait for debugger command$")
    public void waitForDebuggerCommand() throws InterruptedException {
        System.out.println(">> Waiting for debugger command");
        while (!RemoteDebugger.getRemoteDebugger().isReceivedCommand()) {
            Thread.sleep(100);
        }
        System.out.println("<< Got debugger command");
    }

    BufferedReader fileReading;
    @Given("^open file \"([^\"]*)\" for reading$")
    public void open_file_for_reading(String filePath) throws FileNotFoundException {
        fileReading = new BufferedReader(new FileReader(filePath));
    }

    @Given("^close current file$")
    public void close_current_file() throws IOException {
        fileReading.close();
    }

    String currentLineRead;
    @Then("^expect the next line to contain \"([^\"]*)\"$")
    public void expect_the_next_line_to_contain(String subString) throws IOException {
        currentLineRead = fileReading.readLine();
        expectTheLineToContain(subString);
    }

    @And("^skip line$")
    public void skipLine() throws IOException {
        currentLineRead = fileReading.readLine();
    }

    @Then("^expect end of file$")
    public void expectEndOfFile() throws IOException {
        currentLineRead = fileReading.readLine();
        assertThat(currentLineRead, is(equalTo(null)));
    }

    @Then("^expect the line to contain \"([^\"]*)\"$")
    public void expectTheLineToContain(String subString) {
        assertThat(currentLineRead, containsString(subString));
    }

    @When("^processing each line in file \"([^\"]*)\" and only output to file \"([^\"]*)\" lines after finding a line containing \"([^\"]*)\"$")
    public void processingEachLineInFileAndOnlyOutputToFileLinesAfterFindingALineContaining(String inFilePath, String outFilePath, String lineToMatch) throws Throwable {
        BufferedReader fileReading = new BufferedReader(new FileReader(inFilePath));
        BufferedWriter fileWriting = new BufferedWriter(new FileWriter(outFilePath));
        String currentLine = "";
        do {
            currentLine = fileReading.readLine();
        } while (currentLine != null && !currentLine.contains(lineToMatch));

        // Skip this line
        currentLine = fileReading.readLine();
        while(currentLine != null) {
            fileWriting.write(currentLine);
            currentLine = fileReading.readLine();
            fileWriting.newLine();
        }
        fileReading.close();
        fileWriting.flush();
        fileWriting.close();
    }

    @When("^processing each line in file \"([^\"]*)\" and only output to file \"([^\"]*)\" lines that do not contain any lines from \"([^\"]*)\"$")
    public void processingEachLineInFileAndOnlyOutputToFileLinesThatDoNotContainAnyLinesFrom(String inFilePath, String outFilePath, String matchFilePath) throws Throwable {
        String currentLine = "";
        BufferedReader fileReading = new BufferedReader(new FileReader(matchFilePath));
        Set<String> toMatch = new HashSet<String>();
        do {
            currentLine = fileReading.readLine();
            if (currentLine != null && !currentLine.isEmpty()) {
                toMatch.add(currentLine);
            }
        } while (currentLine != null);
        fileReading.close();

        fileReading = new BufferedReader(new FileReader(inFilePath));
        BufferedWriter fileWriting = new BufferedWriter(new FileWriter(outFilePath));
        do {
            currentLine = fileReading.readLine();
            if (currentLine != null) {
                boolean matched = false;
                for (String line : toMatch) {
                    if (currentLine.contains(line)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    fileWriting.write(currentLine);
                    currentLine = fileReading.readLine();
                    fileWriting.newLine();
                }
            }
        } while (currentLine != null);

        fileReading.close();
        fileWriting.flush();
        fileWriting.close();
    }

    @Given("^a ROM from file \"([^\"]*)\" at (.*)$")
    public void a_ROM_from_file_at_$a(String filename, String address) throws Throwable {
        File file = new File(filename);
        int iaddr = valueToInt(address);
        int flen = (int) file.length();
        Memory memory = Memory.makeROM(iaddr , iaddr + flen - 1 , file);

        machine.getCpu().getBus().addDevice(memory);
    }

    @Given("^a CHARGEN ROM from file \"([^\"]*)\"$")
    public void a_CHARGEN_ROM_from_file(String filename) throws Throwable {
        File file = new File(filename);
        int flen = (int) file.length();
        Memory memory = Memory.makeROM(0 , flen - 1 , file);

        if (displayC64 != null) {
            displayC64.setTheCHARGEN(memory);
        }
        // Not going to add this to the device memory map
    }

    @Given("^add C64 hardware$")
    public void addC64Hardware() throws MemoryRangeException {
        Device device = new C64VICII(scenario);
        if (displayC64 != null) {
            displayC64.setTheVICII(device);
        }
        machine.getCpu().getBus().addDevice(device , 1);

        machine.getCpu().getBus().addDevice(new C64SID(scenario) , 1);

        device = new C64ColourRAM(scenario);
        if (displayC64 != null) {
            displayC64.setTheColourRAM(device);
        }
        machine.getCpu().getBus().addDevice(device , 1);

        device = new C64IO(scenario);
        if (displayC64 != null) {
            displayC64.setTheIO(device);
        }
        machine.getCpu().getBus().addDevice(device , 1);
    }

    @Given("^randomly initialise all memory using seed (.*)$")
    public void randomlyInitialiseAllMemoryUsingSeed(String seedValue) throws ScriptException {
        int seed = valueToInt(seedValue);

        Random rand = new Random(seed);

        for (MemoryBus device : devices) {
            device.randomiseData(rand);
        }

        if (displayBombJack != null) {
            displayBombJack.randomiseData(rand);
        }
        if (audioExpansion != null) {
            audioExpansion.randomiseData(rand);
        }
    }

    @And("^enable debug pixel picking$")
    public void enableDebugPixelPicking() {
        if (displayBombJack != null) {
            displayBombJack.setDebugDisplayPixels(true);
        }
    }
    @And("^disable debug pixel picking$")
    public void disableDebugPixelPicking() {
        if (displayBombJack != null) {
            displayBombJack.setDebugDisplayPixels(false);
        }
    }

    static boolean profileEnable = false;
    static boolean profileClear = false;

    @Given("^profile start$")
    public void profileStart() {
        profileEnable = true;
    }

    @Given("^profile stop$")
    public void profileStop() {
        profileEnable = false;
    }

    @Given("^profile clear$")
    public void profileClear() {
        profileClear = true;
    }

    @Given("^profile print$")
    public void profilePrint() {
        profileCreateDebug(null);
        scenario.write(debugProfile);
    }

    @Given("^force C64 displayed bank to (\\d+)$")
    public void force_C64_displayed_bank_to(int bank) throws Throwable {
        displayC64.setForceBank(bank);
    }

    @Given("^avoid CPU wait during VBlank for address \"([^\"]*)\"$")
    public void avoid_CPU_wait_during_VBlank_for_address(String arg1) throws Throwable {
        int potential = valueToInt(arg1);
        if (potential >= 0) {
            if (pauseUntilNewVBlank == null) {
                pauseUntilNewVBlank = new HashSet<>();
            }
            pauseUntilNewVBlank.add(potential);
        }
    }
}
