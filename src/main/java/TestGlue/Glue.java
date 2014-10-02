package TestGlue;

import java.util.List;

import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.machines.Machine;
import com.loomcom.symon.machines.SimpleMachine;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class Glue {

	private Machine machine;
	private int writingAddress = 0;

	private int valueToInt(String value) {
		if (value.charAt(0) == '$') {
			return Integer.parseInt(value.substring(1) , 16);
		}
		return Integer.parseInt(value);
	}

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

	@Given("^I setup a (.+) byte stack slide$")
	public void i_setup_a_byte_stack_slide(String arg1) throws Throwable {
		int i;
		for ( i = 0 ; i < valueToInt(arg1) ; i++ ) {
			machine.getCpu().stackPush(0);
		}
	}

	@When("^I execute the procedure at (.+)$")
	public void i_execute_the_procedure_at(String arg1) throws Throwable {
		machine.getCpu().setProgramCounter(valueToInt(arg1));

		// Pushing lots of 0 onto the stack will eventually return to address 1
		while (machine.getCpu().getProgramCounter() > 1) {
			machine.getCpu().step();
			System.out.print(machine.getCpu().getCpuState().toTraceEvent());
		}
	}

	@Then("^I expect to see (.+) contain (.+)$")
	public void i_expect_to_see_contain(String arg1, String arg2) throws Throwable {
//		assertEquals(machine.getBus().read(valueToInt(arg1)) , valueToInt(arg2));
		assertThat(machine.getBus().read(valueToInt(arg1)), is(equalTo(valueToInt(arg2))));
	}
}
