import cucumber.api.cli.Main;

public class TestRunner {
	// Run with: --monochrome --format pretty --glue TestGlue features
	public static void main(String args[]) throws Exception {
		try {
			Main.main(args);
		} catch (Throwable throwable) {
			throwable.printStackTrace();
		}
	}
}
