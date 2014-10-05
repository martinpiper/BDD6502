import cucumber.api.cli.Main;

public class TestRunner {
	public static void main(String args[]) throws Exception {
		if ( args.length == 0 ) {
			// Use a default command line if it's missing one
			String temp = "--monochrome --format pretty --glue TestGlue features";
			System.out.println("Using default command line options: " + temp);
			args = temp.split(" ");
		}
		try {
			Main.main(args);
		} catch (Throwable throwable) {
			throwable.printStackTrace();
		}
	}
}
