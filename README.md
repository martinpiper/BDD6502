BDD6502
=======

A project to allow Behaviour Driven Development (BDD) testing to be used with 6502 code. As code gets more complex, reliable easy to execute comprehensive tests helps reduce the amount of time spent debugging and checking existing code. This helps improve the efficiency of future code change because you're worrying less about checking for changes breaking existing code. 

This framework allows 6502 code to be tested with human readable automated tests.
The tests are located in feature files that describe expected software behaviour.
The feature files are parsed and executed with Cucumber-JVM. See http://cukes.info/
This uses a 6502 simulator (Symon from https://github.com/sethm/symon) in the JVM to allow very low level control over the virtual machine test environment.
It has some minor tweaks to allow greater access to the simulator.

Test cases can also enable unitialised memory access protection, this is a very powerful feature that allows unintended memory access to be detected while running tests. Combined with the full trace output this can show exactly which instruction is at fault, which reduces debugging time.

Usually with BDD each test scenario in a feature file will be entirely separate and not maintain state from previous scenarios.
In other words a scenario should contain all the state needed for a test and test code usually enforces this rule. However in this implementation this is only optional behaviour, by default the 6502 simulator state will be kept across scenario and feature file boundaries. This means scenarios and features can rely on the 6502 simulator state from scenarios and states in the current execution run.
The scenario step "* I have a simple 6502 system" will initialise the 6502 simulator state and fill memory to 0.
The step "* I fill memory with <value>" will fill memory with a different value.


Example test case
-----------------

This example [test case](https://github.com/martinpiper/BDD6502/blob/master/features/assemble.feature) describes a short piece of code, how to assemble it, and what values are expected in memory before and after executing the procedure under test.  
```
   Given I have a simple 6502 system
    And I create file "test.a" with
      """
      !sal
      *=$400
      start
        lda #0
        sta $400
        ldx #$20
      .l1
        inc $400
        dex
        bne .l1
        rts
      """
    And I run the command line: ..\C64\acme.exe -o test.prg --labeldump test.lbl -f cbm test.a
    And I load prg "test.prg"
    And I load labels "test.lbl"
    Then I expect to see $400 equal 0xa9
    Then I expect to see $400 contain $81
    Then I expect to see $400 exclude $40
    # The above code is actually 100 instructions long when executing
    When I execute the procedure at start until return
    Then I assert the unitialised memory read flag is clear
    # Note how the label "start" is used below and correctly resolves to be $400 when checking memory
    Then I expect to see start equal 32
    And I expect to see $402 equal $8d
    And I expect to see $400 equal 32
```

Code can of course be included from existing source code and tested, as this example from a [game project demonstrates](https://github.com/martinpiper/C64Public/blob/master/Citadel2/features/Score.feature) where its score routine is tested.
[This video](https://youtu.be/-Ptq6ZY3Kxk?t=1253) demonstrates unit testing 6502 code.

Syntax also exists to interface with the remote monitor in [Vice emulator](https://vice-emu.sourceforge.io/).
This allows completed game code to be accurately emulated and verified, for example by comparing screenshots of game code as it [executes frame by frame](https://github.com/martinpiper/C64Public/blob/master/Scroller/features/VerifyByScreenshots.feature).

Syntax for test cases
---------------------

Syntax included in [source control](https://github.com/martinpiper/BDD6502/blob/master/target/syntax.html) along with the [built jars](https://github.com/martinpiper/BDD6502/tree/master/target).

Syntax can also be extended by using a [java maven build](https://github.com/martinpiper/C64Public/blob/master/Citadel2/pom.xml) to add [extension syntax](https://github.com/martinpiper/C64Public/blob/master/Citadel2/src/test/java/MazeGlue6502/Memory.java#L54-L59). 
[This video](https://youtu.be/-Ptq6ZY3Kxk?t=356) gives an example of extending syntax with java code.

Setup
-----

Build the jar, or use the built version in the "[target](https://github.com/martinpiper/BDD6502/tree/master/target)" directory.

If you are running the examples, you will need various files from inside this repository.
	Get the repository https://github.com/martinpiper/C64Public and make sure the contents are place in a "C64" directory at the same level as this BDD6502 directory.
	Note: Many of the examples expect to find ../C64/ and use ../C64/ACME.exe to compile example assembly code, as well as reference ../C64/stdlib/ for library functions, macros, and defines

The video hardware example also uses files from: https://github.com/martinpiper/BombJack
	Get the repository and make sure the directory "BombJack" is at the same level as this BDD6502 directory.
	

* To run the test code execute the command line:

(If your java complains about --add-opens then use the second command line)

	java --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.desktop/java.awt.font=ALL-UNNAMED -jar target\BDD6502-1.0.9-SNAPSHOT-jar-with-dependencies.jar --monochrome --plugin pretty --plugin html:target/cucumber --plugin json:target/report1.json --glue TestGlue features

	java -jar target\BDD6502-1.0.9-SNAPSHOT-jar-with-dependencies.jar --monochrome --plugin pretty --plugin html:target/cucumber --plugin json:target/report1.json --glue TestGlue features


* If the Java property "bdd6502.trace" is set to be "true" then the simulator state is dumped for each cycle.
This can be set on the command java line before the -jar option: jar -Dbdd6502.trace=true
Or when using an IDE: -Dbdd6502.trace=true



* To use the feature editor and debugger, this will display a URL to open in your browser: http://127.0.0.1:8001/ace-builds-master/demo/autocompletion.html

(If your java complains about --add-opens then use the second command line)

	java -Dcom.replicanet.cukesplus.server.featureEditor -Dcom.replicanet.ACEServer.debug.requests= --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.desktop/java.awt.font=ALL-UNNAMED -jar target\BDD6502-1.0.9-SNAPSHOT-jar-with-dependencies.jar --monochrome --plugin pretty --plugin html:target/cucumber --plugin json:target/report1.json --glue TestGlue features

	java -Dcom.replicanet.cukesplus.server.featureEditor -Dcom.replicanet.ACEServer.debug.requests= -jar target\BDD6502-1.0.9-SNAPSHOT-jar-with-dependencies.jar --monochrome --plugin pretty --plugin html:target/cucumber --plugin json:target/report1.json --glue TestGlue features





Music conversion
----------------

The music conversion from MOD/XM files, commonly used on the Amiga, is mostly used with my [modular audio and video hardware](https://www.youtube.com/watch?v=MLVZav7mVcI) project. 

* Convert: java -jar target\BDD6502-1.0.9-SNAPSHOT-jar-with-dependencies.jar --exportmod "C:\Users\Martin Piper\Downloads\asikwp_-_twistmachine.mod" "target/exportedMusic" 1 1
    Use "-Dmusic.volume=1" to include channel volume changes that are not part of frequency changes. This can produce large files, so use only when really needed.
    

* Playback: java -jar target\BDD6502-1.0.9-SNAPSHOT-jar-with-dependencies.jar --playmod target\exportedMusic




Remote debugger commands
------------------------

cpu 6502
cpu apu

display full
display cls
display ahead
