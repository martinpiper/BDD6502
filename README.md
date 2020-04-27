BDD6502
=======

A project to allow Behaviour Driven Development (BDD) to be used with 6502 code.

The idea is to allow 6502 code to be tested with human readable automated tests.
The tests are located in feature files that describe expected software behaviour.
The feature files are parsed and executed with Cucumber-JVM. See http://cukes.info/
This uses a 6502 simulator (Symon from https://github.com/sethm/symon) in the JVM to allow very low level control over the virtual machine test environment.
It has some minor tweaks to allow greater access to the simulator.

Usually with BDD each test scenario in a feature file will be entirely separate and not maintain state from previous scenarios.
In other words a scenario should contain all the state needed for a test and test code usually enforces this rule.
However in this implementation this is only optional behaviour, by default the 6502 simulator state will be kept across scenario and feature file boundaries.
This means scenarios and features can rely on the 6502 simulator state from scenarios and states in the current execution run.
The scenario step "* I have a simple 6502 system" will initialise the 6502 simulator state and fill memory to 0.
The step "* I fill memory with <value>" will fill memory with a different value.


Setup:

Build the jar or download it from: https://github.com/martinpiper/BDD6502/releases
If you are running the examples, you will need various files from inside this repository.
	Get the repository https://github.com/martinpiper/C64Public and make sure the contents are place in a "C64" directory at the same level as this BDD6502 directory.
	Note: Many of the examples expect to find ../C64/ and use ../C64/ACME.exe to compile example assembly code, as well as reference ../C64/stdlib/ for library functions, macros, and defines

The video hardware example also uses files from: https://github.com/martinpiper/BombJack
	Get the repository and make sure the directory "BombJack" is at the same level as this BDD6502 directory.
	

To run the test code execute the command line:

	java --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.desktop/java.awt.font=ALL-UNNAMED -jar target\BDD6502-1.0.9-SNAPSHOT-jar-with-dependencies.jar --monochrome --plugin pretty --plugin html:target/cucumber --plugin json:target/report1.json --glue TestGlue features

If the Java property "bdd6502.trace" is set to be "true" then the simulator state is dumped for each cycle.
This can be set on the command java line before the -jar option: jar -Dbdd6502.trace=true
Or when using an IDE: -Dbdd6502.trace=true


To use the feature editor and debugger:

	java -Dcom.replicanet.cukesplus.server.featureEditor -Dcom.replicanet.ACEServer.debug.requests= --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.desktop/java.awt.font=ALL-UNNAMED -jar target\BDD6502-1.0.9-SNAPSHOT-jar-with-dependencies.jar --monochrome --plugin pretty --plugin html:target/cucumber --plugin json:target/report1.json --glue TestGlue features
