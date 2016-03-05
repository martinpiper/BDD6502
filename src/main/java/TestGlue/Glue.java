package TestGlue;

import com.loomcom.symon.Cpu;
import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.machines.Machine;
import com.loomcom.symon.machines.SimpleMachine;
import cucumber.api.Scenario;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class Glue {

	static private Machine machine = null;
	static private int writingAddress = 0;
	static private TreeMap labelMap = new TreeMap();
        static private TreeMap<Integer,String> reverseLabelMap = new TreeMap<Integer,String>();
	static private Map<String,Integer> calculationMap = new TreeMap<String,Integer>();
        static private boolean traceOveride = false;
	Scenario scenario = null;

	ScriptEngineManager manager = new ScriptEngineManager();
	ScriptEngine engine = manager.getEngineByName("JavaScript");

	@Before
	public void BeforeHook(Scenario scenario) {
		this.scenario = scenario;
	}

	public int valueToInt(String valueIn) throws ScriptException {
		if ( null == valueIn || valueIn.isEmpty() ) {
			return -1;
		}
		String origValueIn = valueIn;
		Integer cachedRet = calculationMap.get(origValueIn);
		if (null != cachedRet)
		{
			return cachedRet.intValue();
		}

		// Find any labels or numbers in the expression that can be substituted for known labels etc
		Pattern pattern = Pattern.compile("[\\w$%]+");
		Matcher matcher = pattern.matcher(valueIn);

		class ToReplace {
			public int start , end;
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
			valueIn = valueIn.substring(0 , entry.start) + entry.replacement + valueIn.substring(entry.end);
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
		Integer i = null;
		try
		{
			i = Integer.parseInt(t);
		}
		catch (Exception e)
		{
			i = (int) Math.round(Double.parseDouble(t));
		}
		calculationMap.put(origValueIn , i);
		return i.intValue();
	}

	// simple.feature
	@Given("^I have a simple 6502 system$")
	public void i_have_a_simple_6502_system() throws Throwable {
		machine = new SimpleMachine();
		machine.getCpu().reset();
	}
        
        @Given("^I have a simple overclocked 6502 system$")
	public void i_have_a_simple_overclocked_6502_system() throws Throwable {
		machine = new SimpleMachine();
		machine.getCpu().reset();
                machine.getCpu().setOverclock();
	}
        
	@Given("^I fill memory with (.+)$")
	public void i_fill_memory_with(String arg1) throws Throwable {
		Memory mem = machine.getRam();
		mem.fill(valueToInt(arg1));
	}

	@Given("^I start writing memory at (.+)$")
	public void i_start_writing_memory_at(String arg1) throws Throwable {
		writingAddress = valueToInt(arg1);
	}

	@Given("^I write the following hex bytes$")
	public void i_write_the_following_hex_bytes(List<String> arg1) throws Throwable {
		for ( String arg : arg1) {
			String[] values = arg.split(" ");
			int i;
			for ( i = 0 ; i < values.length ; i++ ) {
				if ( !values[i].isEmpty() ) {
					machine.getBus().write(writingAddress++ , Integer.parseInt(values[i], 16));
				}
			}
		}
	}

	@Given("^I write the following bytes$")
	public void i_write_the_following_bytes(List<String> arg1) throws Throwable {
		for ( String arg : arg1) {
			machine.getBus().write(writingAddress++ , valueToInt(arg));
		}
	}

	@Given("^I write memory at (.+) with (.+)$")
	public void i_write_memory_at_with(String arg1, String arg2) throws Throwable {
		machine.getBus().write(valueToInt(arg1) , valueToInt(arg2));
	}

	@Given("^I setup a (.+) byte stack slide$")
	public void i_setup_a_byte_stack_slide(String arg1) throws Throwable {
		int i;
		for ( i = 0 ; i < valueToInt(arg1) ; i++ ) {
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
        public void i_enable_trace() throws Throwable { traceOveride = true; }
        
        @Given("^I disable trace$")
        public void i_disable_trace() throws Throwable { traceOveride = false; } 
        
        public void internalCPUStep(boolean displayTrace) throws Throwable {
            
            Integer addr = new Integer(machine.getCpu().getCpuState().pc);
            
            machine.getCpu().step();
	    if ( displayTrace ) {
                Object label = reverseLabelMap.get(addr);
                if( label != null )
                {
                    scenario.write(label + ":");
                }
        	scenario.write(machine.getCpu().getCpuState().toTraceEvent());
	    }
            if( machine.getCpu().getFailOnBreak() == true)
            {
                if( machine.getCpu().getExtraStatus()== Cpu.Extra_BRK )
                {
                    throw new Exception("BRK Hit @" + machine.getCpu().getProgramCounter() );
                }
            }
            if( (machine.getCpu().getExtraStatus() & Cpu.Extra_ATest) == Cpu.Extra_ATest)
            {
                throw new Exception("A has changed @" + machine.getCpu().getProgramCounter());
            }
            if( (machine.getCpu().getExtraStatus() & Cpu.Extra_XTest) == Cpu.Extra_XTest)
            {
                throw new Exception("X has changed @" + machine.getCpu().getProgramCounter());
            }
            if( (machine.getCpu().getExtraStatus() & Cpu.Extra_YTest) == Cpu.Extra_YTest)
            {
                throw new Exception("Y has changed @" + machine.getCpu().getProgramCounter());
            }			
        }
        
        public int getNBytesValueAt(int addr, int count) throws Throwable
        {
            int ret = 0;
            int mul = 1;
            for( int j = 0 ; j < count ; ++j )
            {
                ret += machine.getBus().read(addr+j)*mul;
                mul *= 256;
            }
            return ret;            
        }
        
        public void executeProcedureAtUntilMemEquals(String arg1, String arg2, String arg3 ) throws Throwable {
            checkScenario();

            String output = String.format("Execute procedure (%s) until (%s) is (%s)" , arg1 , arg2 , arg3);
            scenario.write(output);
            
            boolean displayTrace = traceOveride;
            String trace = System.getProperty("bdd6502.trace");
            if ( null != trace && trace.indexOf("true") != -1 ) 
            {
		displayTrace = true;
            }

            if ( !arg1.isEmpty() ) 
            {
		machine.getCpu().setProgramCounter(valueToInt(arg1));
            }
            
            int addrToCheck = valueToInt(arg2);
            int valueToCheckFor = valueToInt(arg3);
            int numBytes = 1;
            if( valueToCheckFor > 0xFFFF ) numBytes++;
            if( valueToCheckFor > 0xFF) numBytes++;
            
            machine.getCpu().initRegisterTestStackIfNeeded();
            while(getNBytesValueAt(addrToCheck, numBytes) != valueToCheckFor)
            {
                internalCPUStep(displayTrace);
            }
        }
        
	public void executeProcedureAtForNoMoreThanInstructionsUntilPC(String arg1, String arg2, String arg3) throws Throwable {
		checkScenario();

		String output = String.format("Execute procedure (%s) for no more than (%s) instructions until pc (%s)" , arg1 , arg2 , arg3);
		scenario.write(output);
//		System.out.println(output);

		boolean displayTrace = traceOveride;
		String trace = System.getProperty("bdd6502.trace");
		if ( null != trace && trace.indexOf("true") != -1 ) {
			displayTrace = true;
		}

		if ( !arg1.isEmpty() ) {
			machine.getCpu().setProgramCounter(valueToInt(arg1));
		}

		int maxInstructions = valueToInt(arg2);
		int numInstructions = 0;
		int untilPC = valueToInt(arg3);

                machine.getCpu().initRegisterTestStackIfNeeded();
                
		// Pushing lots of 0 onto the stack will eventually return to address 1
		while (machine.getCpu().getProgramCounter() > 1) {
                    if ( untilPC == machine.getCpu().getProgramCounter() ) {
                        break;
                    }
                    internalCPUStep(displayTrace);
                    assertThat(++numInstructions , is(lessThanOrEqualTo(maxInstructions)));
		}

		output = String.format("Executed procedure (%s) for %d instructions" , arg1 , numInstructions);
		scenario.write(output);
//		System.out.println(output);
	}

	private void checkScenario()
	{
		assertThat("The Cucumber scenario is not set, use:\n@cucumber.api.java.Before\npublic void BeforeHook(cucumber.api.Scenario scenario) { c64.BeforeHook(scenario); }\n.", scenario, is(notNullValue()));
	}

	@When("^I execute the procedure at (.+) for no more than (.+) instructions until PC = (.+)$")
	public void i_execute_the_procedure_at_for_no_more_than_instructions_until_pc(String arg1, String arg2, String arg3) throws Throwable
	{
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
		executeProcedureAtForNoMoreThanInstructionsUntilPC("" , arg1 , arg2);
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
		assertThat(machine.getBus().read(valueToInt(arg1)), is(equalTo(valueToInt(arg2))));
	}

        @Then("^I expect to see (.+) less than (.+)$")
	public void i_expect_to_see_less_than(String arg1, String arg2) throws Throwable {
		assertThat(machine.getBus().read(valueToInt(arg1)), is(lessThan(valueToInt(arg2))));
	}
        
        @Then("^I expect to see (.+) greater than (.+)$")
	public void i_expect_to_see_greater_than(String arg1, String arg2) throws Throwable {
		assertThat(machine.getBus().read(valueToInt(arg1)), is(greaterThan(valueToInt(arg2))));
	}
        
	@Then("^I expect to see (.+) contain (.+)$")
	public void i_expect_to_see_contain(String arg1, String arg2) throws Throwable {
		assertThat(machine.getBus().read(valueToInt(arg1)) & valueToInt(arg2), is(equalTo(valueToInt(arg2))));
	}

	@Then("^I expect to see (.+) exclude (.+)$")
	public void i_expect_to_see_exclude(String arg1, String arg2) throws Throwable {
		assertThat(machine.getBus().read(valueToInt(arg1)) & valueToInt(arg2), is(equalTo(0)));
	}

	public int getRegValue(String arg1) throws Exception
	{
		int regValue;
		if (arg1.equalsIgnoreCase("a"))
		{
			regValue = machine.getCpu().getAccumulator();
		}
		else if (arg1.equalsIgnoreCase("x"))
		{
			regValue = machine.getCpu().getXRegister();
		}
		else if (arg1.equalsIgnoreCase("y"))
		{
			regValue = machine.getCpu().getYRegister();
		}
		else if (arg1.equalsIgnoreCase("st"))
		{
			regValue = machine.getCpu().getProcessorStatus() & (128 + 64 + 8 + 4 + 2 +1);
		}
		else
		{
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
		if (arg1.equalsIgnoreCase("a"))
		{
			machine.getCpu().setAccumulator(valueToInt(arg2));
		}
		else if (arg1.equalsIgnoreCase("x"))
		{
			machine.getCpu().setXRegister(valueToInt(arg2));
		}
		else if (arg1.equalsIgnoreCase("y"))
		{
			machine.getCpu().setYRegister(valueToInt(arg2));
		}
		else if (arg1.equalsIgnoreCase("st"))
		{
			machine.getCpu().setProcessorStatus(valueToInt(arg2));
		}
		else
		{
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
		// Write code here that turns the phrase above into concrete actions
		Process p = Runtime.getRuntime().exec(arg1);
		p.waitFor();

		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

		StringBuffer sb = new StringBuffer();
		String line = "";
		while ((line = reader.readLine())!= null) {
			sb.append(line + "\n");
		}

		reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		sb = new StringBuffer();
		while ((line = reader.readLine())!= null) {
			sb.append(line + "\n");
		}

		if ( p.exitValue() != 0 ) {
			throw new Exception(String.format("Return code: %d with message '%s'" , p.exitValue() , sb.toString() ) );
		}

		scenario.write(String.format("After executing command line '%s' return code: %d with message '%s'\n" , arg1 , p.exitValue() , sb.toString()));
	}

	@Given("^I load prg \"(.*?)\"$")
	public void i_load_prg(String arg1) throws Throwable {
		FileInputStream in = null;
		in = new FileInputStream(arg1);
		int addr = in.read() + (in.read() * 256);
		int c;
		while ((c = in.read()) != -1) {
			machine.getBus().write(addr++ , c);
		}
	}
        
        @Given("^I load bin \"(.*?)\" at (.+)$")
	public void i_load_bin_at(String arg1,String arg2) throws Throwable {
		FileInputStream in = null;
		in = new FileInputStream(arg1);
		int addr = valueToInt(arg2);
		int c;
		while ((c = in.read()) != -1) {
			machine.getBus().write(addr++ , c);
		}
	}
        
	@Given("^I load labels \"(.*?)\"$")
	public void i_load_labels(String arg1) throws Throwable {
		calculationMap.clear();
		BufferedReader br = new BufferedReader(new FileReader(arg1));
		String line;
		while ((line = br.readLine()) != null) {
			String[] splits = line.split("=");
			splits[0] = splits[0].trim();
			String[] splits2 = splits[1].split(";");
			splits2[0].trim();
			if (StringUtils.isNumeric(splits[0])) {
				// If the value is numeric this will affect the operation of valueToInt so we avoid this by adding a prefix underscore
				splits[0] = "_" + splits[0];
			}
			labelMap.put(splits[0], splits2[0]);
                        reverseLabelMap.put(new Integer(valueToInt(splits2[0])),splits[0]);
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
		while ( addrStart < addrEnd ) {
			if (cr == 0) {
				hexOutput += String.format("%2s:", Integer.toHexString(addrStart).replace(' ', '0'));
			}

			if (cr == 8)
			{
				hexOutput += " ";
			}

			int theByte = machine.getBus().read(addrStart);
			String hex = String.format("%2s", Integer.toHexString(theByte)).replace(' ', '0');

			if (CharUtils.isAsciiPrintable((char)theByte))
			{
				section += (char)theByte;
			}
			else
			{
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

		if (!section.isEmpty())
		{
			hexOutput += " : " + section;
		}

		scenario.write(hexOutput);
//		System.out.print(hexOutput);
	}

	@Then("^I expect memory (.+) to equal memory (.+)$")
	public void i_expect_to_see_memory_equal_memory(String arg1, String arg2) throws Throwable {
		assertThat(machine.getBus().read(valueToInt(arg1)), is(equalTo(machine.getBus().read(valueToInt(arg2)))));
	}

	@Then("^I expect memory (.+) to contain memory (.+)$")
	public void i_expect_to_see_memory_contain_memory(String arg1, String arg2) throws Throwable {
		assertThat(machine.getBus().read(valueToInt(arg1)) & machine.getBus().read(valueToInt(arg2)), is(equalTo(machine.getBus().read(valueToInt(arg2)))));
	}

	@Then("^I expect memory (.+) to exclude memory (.+)$")
	public void i_expect_to_see_memory_exclude_memory(String arg1, String arg2) throws Throwable {
		assertThat(machine.getBus().read(valueToInt(arg1)) & machine.getBus().read(valueToInt(arg2)), is(equalTo(0)));
	}

	@Given("^I set label (.+) equal to (.+)$")
	public void iSetLabelFooEqualTo(String arg1 , String arg2) throws Throwable
	{
		int val = valueToInt(arg2);
		String sval = Integer.toString(val);
		labelMap.put(arg1,sval);
                reverseLabelMap.put(new Integer(sval),arg1);
	}
        
        @Then("^Joystick (\\d+) is (NONE|U|D|L|R|UL|UR|DL|DR|FIRE|UFIRE|ULFIRE|URFIRE|DFIRE|DLFIRE|DRFIRE|LFIRE|RFIRE)$")
        public void joystick_X_is_DIR(int arg1, String arg2) throws Throwable {
            assertThat(arg1, is(lessThan(3)));
            assertThat(arg1, is(greaterThan(0)));
            int destMem = 0xDC00;
            if( arg1 == 1 )
            {
                destMem = 0xDC01;
            }
            if("NONE".equals(arg2))
            {
                machine.getBus().write(destMem, 255);                    
            }
            else if("U".equals(arg2))
            {
                machine.getBus().write(destMem, 254);  
            }
            else if("D".equals(arg2))
            {
                machine.getBus().write(destMem, 253);  
            }
            else if("L".equals(arg2))
            {
                machine.getBus().write(destMem, 251);  
            }
            else if("R".equals(arg2))
            {
                machine.getBus().write(destMem, 247);  
            }
            else if("UL".equals(arg2))
            {
                machine.getBus().write(destMem, 250);  
            }
            else if("UR".equals(arg2))
            {
                machine.getBus().write(destMem, 246);  
            }
            else if("DL".equals(arg2))
            {
                machine.getBus().write(destMem, 249);  
            }
            else if("DR".equals(arg2))
            {
                machine.getBus().write(destMem, 245);  
            }
            if("FIRE".equals(arg2))
            {
                machine.getBus().write(destMem, 239);                    
            }
            else if("UFIRE".equals(arg2))
            {
                machine.getBus().write(destMem, 238);  
            }
            else if("DFIRE".equals(arg2))
            {
                machine.getBus().write(destMem, 237);  
            }
            else if("LFIRE".equals(arg2))
            {
                machine.getBus().write(destMem, 235);  
            }
            else if("RFIRE".equals(arg2))
            {
                machine.getBus().write(destMem, 231);  
            }
            else if("ULFIRE".equals(arg2))
            {
                machine.getBus().write(destMem, 234);  
            }
            else if("URFIRE".equals(arg2))
            {
                machine.getBus().write(destMem, 230);  
            }
            else if("DLFIRE".equals(arg2))
            {
                machine.getBus().write(destMem, 233);  
            }
            else if("DRFIRE".equals(arg2))
            {
                machine.getBus().write(destMem, 229);  
            }
        }
}
