@TC-16
Feature: C64 with new UserPort20To32Bit1 and old UserPortTo24 interfaces

  This checks the C64 behaviour with the new and old user port interfaces. Write and reads data.

  Scenario: Simple C64 and user port interface test

    And I run the command line: ..\C64\acme.exe -v3 --lib ../ -o test.prg --labeldump test.lbl -f cbm "features/C64WithNewUserPort20to32Bit1.a"
    And I run the command line: ..\C64\bin\LZMPi.exe -c64mbu test.prg testcmp.prg $800

    Given clear all external devices
    Given a new C64 video display
    And show C64 video window
    And force C64 displayed bank to 3
    Given add C64 display window to C64 keyboard buffer hook
#    Given a new audio expansion
#    And audio mix 85
    Given a new video display with overscan and 16 colours
    Given set the video display to RGB colour 5 6 5
    Given set the video display with 32 palette banks
	And enable video display bus debug output
    Given video display processes 24 pixels per instruction
    Given video display refresh window every 32 instructions
#    And audio refresh window every 0 instructions
#    And audio refresh is independent
    Given video display add joystick to port 1
    Given video display add CIA1 timers with raster offset 0 , 0
    Given video display saves debug BMP images to leaf filename "target/frames/TC-16-"
    And C64 video display saves debug BMP images to leaf filename "target/frames/TC-16-C64-"
    Given property "bdd6502.bus24.trace" is set to string "true"
    Given I enable trace
    Given I have a simple overclocked 6502 system
    Given I am using C64 processor port options
    Given a ROM from file "C:\VICE\C64\kernal" at $e000
    Given a ROM from file "C:\VICE\C64\basic" at $a000
    Given a CHARGEN ROM from file "C:\VICE\C64\chargen"
    Given add C64 hardware
#    When I enable uninitialised memory read protection with immediate fail
    * That does fail on BRK
    Given a user port to 32 bit interface and 24 bit bus is installed
    And add to the 32 bit interface a bank of memory at address '0x0' and size '0x100000'
    And add to the 32 bit interface a bank of memory at address '0x100000' and size '0x100000'
    And enable user port bus debug output
    Given add a Chars V4.0 layer with registers at '0x9000' and screen addressEx '0x80' and planes addressEx '0x20'
    And the layer has 16 colours
    And the layer has overscan
    Given add a Tiles layer with registers at '0x9e00' and screen addressEx '0x80' and planes addressEx '0x40'
    And the layer has 16 colours
    And the layer has overscan
    Given add a Sprites2 layer with registers at '0x9200' and addressEx '0x08' and running at 14.31818MHz
    And the layer has 16 colours
    And the layer has overscan
    Given add a Sprites layer with registers at '0x9800' and addressEx '0x10'
    And the layer has 16 colours
    And the layer has overscan

    When enable remote debugging
#    And wait for debugger connection
#    And wait for debugger command

    Given limit video display to 60 fps
    Given add C64 regular IRQ trigger of "100000" cycles

    Given show video window
    Given randomly initialise all memory using seed 4321

    And render a video display frame
    And render a C64 video display frame

    And I load prg "test.prg"
    And I load labels "test.lbl"
    And I enable trace with indent
    When ignore address Video_WaitVBlank_startGuard to Video_WaitVBlank_endGuard for trace

    When I execute the procedure at TestInterface_Passthrough until return

    And render a video display frame
    And render a C64 video display frame

    # Validate the expected data writes going to the video hardware, indicating the interface behaved as expected
    Given open file "target/debugData.txt" for reading
    When ignoring lines that contain ";"
    When ignoring empty lines
    Then expect the next line to contain "d0"
    Then expect the next line to contain "d$00000000"
    Then expect the next line to contain "d$20000200"
    Then expect the next line to contain "d$00000000"
    Then expect the next line to contain "d$9a000100"
    Then expect the next line to contain "d$9a010100"
    Given close current file

    When I execute the procedure at Bus20To32Bit1_Init until return
    When I execute the procedure at TestInterface_WriteRAM until return
    When I execute the procedure at TestInterface_ReadRAM until return
    When I execute the procedure at TestInterface_ReadRAMWithOffset until return
    When I execute the procedure at TestInterface_ScrollScreen until return
    And render a C64 video display frame
    When I execute the procedure at TestInterface_ScrollScreen until return
    And render a C64 video display frame
    When I execute the procedure at TestInterface_ScrollScreen until return
    And render a C64 video display frame
    When I execute the procedure at TestInterface_ScrollScreen until return
    And render a C64 video display frame

    And render a video display frame
    And render a C64 video display frame

#    When rendering the video until window closed

