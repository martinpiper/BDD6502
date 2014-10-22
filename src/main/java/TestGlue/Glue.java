package TestGlue;

import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.machines.Machine;
import com.loomcom.symon.machines.SimpleMachine;
import cucumber.api.Scenario;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.apache.commons.lang3.StringUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class Glue {

	static private Machine machine = null;
	static private int writingAddress = 0;
	static private TreeMap labelMap = new TreeMap();
	Scenario scenario = null;

	@Before
	public void BeforeHook(Scenario scenario) {
		this.scenario = scenario;
	}

	private int valueToInt(String valueIn) throws ScriptException {
		if ( null == valueIn || valueIn.isEmpty() ) {
			return -1;
		}

		// Find any labels or numbers in the expression that can be substituted for known labels etc
		Pattern pattern = Pattern.compile("[\\w$]+");
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
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByName("JavaScript");
		String functions = "function low(i) { return i & 255; }";
		functions += "function hi(i) { return ~~(i/256); }";
		Object temp = engine.eval(functions + valueIn);
		String t = temp.toString();
		Integer i = Integer.parseInt(t);
		return i.intValue();
	}

	// simple.feature
	@Given("^I have a simple 6502 system$")
	public void i_have_a_simple_6502_system() throws Throwable {
		machine = new SimpleMachine();
		machine.getCpu().reset();
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

	@When("^I execute the procedure at (.+) for no more than (.+) instructions until PC = (.+)$")
	public void i_execute_the_procedure_at_for_no_more_than_instructions_until_pc(String arg1, String arg2, String arg3) throws Throwable {
		boolean displayTrace = false;
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

		// Pushing lots of 0 onto the stack will eventually return to address 1
		while (machine.getCpu().getProgramCounter() > 1) {
			if ( untilPC == machine.getCpu().getProgramCounter() ) {
				break;
			}
			machine.getCpu().step();
			if ( displayTrace ) {
				scenario.write(machine.getCpu().getCpuState().toTraceEvent());
			}
			assertThat(++numInstructions , is(lessThanOrEqualTo(maxInstructions)));
		}

		String output = String.format("Executed procedure %s for %d instructions" , arg1 , numInstructions);
		scenario.write(output);
//		System.out.println(output);
	}

	@When("^I execute the procedure at (.+) for no more than (.+) instructions$")
	public void i_execute_the_procedure_at_for_no_more_than_instructions(String arg1, String arg2) throws Throwable {
		machine.getCpu().setStackPointer(0xff);
		i_execute_the_procedure_at_for_no_more_than_instructions_until_pc(arg1 , arg2 , "");
	}

	@When("^I continue executing the procedure for no more than (.+) instructions$")
	public void i_continue_executing_the_procedure_at_for_no_more_than_instructions(String arg1) throws Throwable {
		i_execute_the_procedure_at_for_no_more_than_instructions_until_pc("" , arg1 , "");
	}

	@When("^I continue executing the procedure for no more than (.+) instructions until PC = (.+)$")
	public void i_continue_executing_the_procedure_at_for_no_more_than_instructions_until_pc(String arg1, String arg2) throws Throwable {
		i_execute_the_procedure_at_for_no_more_than_instructions_until_pc("" , arg1 , arg2);
	}

	@Then("^I expect to see (.+) contain (.+)$")
	public void i_expect_to_see_contain(String arg1, String arg2) throws Throwable {
		assertThat(machine.getBus().read(valueToInt(arg1)), is(equalTo(valueToInt(arg2))));
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

	@Given("^I load labels \"(.*?)\"$")
	public void i_load_labels(String arg1) throws Throwable {
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
		}
		br.close();
	}

	@When("^I hex dump memory between (.+) and (.+)$")
	public void i_hex_dump_memory_between_$c_and_$c(String start, String end) throws Throwable {
		int addrStart = valueToInt(start);
		int addrEnd = valueToInt(end);
		int cr = 0;
		String hexOutput = "";
		while ( addrStart < addrEnd ) {
			if (cr == 0) {
				hexOutput += String.format("%2s:", Integer.toHexString(addrStart).replace(' ', '0'));
			}

			String hex = String.format("%2s", Integer.toHexString(machine.getBus().read(addrStart))).replace(' ', '0');

			hexOutput += " " + hex;
			cr += 1;
			if (cr >= 16) {
				hexOutput += "\n";
				cr = 0;
			}

			addrStart += 1;
		}
		scenario.write(hexOutput);
//		System.out.print(hexOutput);
	}
}
