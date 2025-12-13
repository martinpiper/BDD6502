Feature: Tests the video and audio hardware expansion together

  @TC-1-a
  Scenario: Full display test with sprites, borders, contention, chars, tiles, and mode7, and sample play
    Given I run the command line: ..\C64\acme.exe -v3 --lib ../ -o test.prg --labeldump test.lbl -f cbm features/TestAudioHardware.a

    Given clear all external devices
    Given a new video display
    Given set the video display to RGB colour 5 6 5
    Given set the video display with 32 palette banks
    And enable video display bus debug output
    Given a new audio expansion
    And audio mix 85
    And audio refresh window every 32 instructions
    And audio refresh window every 0 instructions
    And audio refresh is independent
    Given video display does not save debug BMP images
    Given property "bdd6502.bus24.trace" is set to string "true"
    Given I have a simple overclocked 6502 system
    And That does fail on BRK
    And I enable uninitialised memory read protection with immediate fail
    Given a user port to 24 bit bus is installed
#    And I enable trace with indent
    Given show video window


    # Instead of writing this data via the 6502 CPU, just send it straight to memory
    # Audio
    Given write data from file "target/exportedMusicSamples.bin" to 24bit bus at '0x0000' and addressEx '0x04'

    And I load prg "test.prg"
    And I load labels "test.lbl"

    Given property "bdd6502.bus24.trace" is set to string "false"
    When I execute the procedure at start for no more than 1000000 instructions
    When I execute the procedure at MusicInit for no more than 1000000 instructions
#    When I execute the procedure at MusicDecompressTest for no more than 1000000 instructions
    And I disable trace
#    Then I hex dump memory between $ff00 and $ffff
#    When I execute the procedure at PlaySample for no more than 1000000 instructions
    Given limit video display to 60 fps
    When I execute the procedure at MusicPlay until return



  @TC-19
  Scenario: Test Audio2
    Given clear all external devices
    Given a new video display with overscan and 16 colours
    Given set the video display to RGB colour 5 6 5
    Given set the video display with 32 palette banks
    Given the display uses exact address matching
    Given the display has RGB background functionality
    And enable video display bus debug output

    Given a new audio2 1 expansion with registers at '0x8000' and addressEx '0x06'
    And the audio2 1 expansion uses exact address matching
#    Given a new audio2 2 expansion with registers at '0x8100' and addressEx '0x07'
#    And the audio2 2 expansion uses exact address matching
    # Emulate a single board with stereo mix
#    Given a new audio2 2 expansion with registers at '0x8000' and addressEx '0x06'
#    And the audio2 2 expansion uses exact address matching

    And audio refresh window every 32 instructions
    And audio refresh window every 0 instructions
    And audio refresh is independent
    Given video display does not save debug BMP images
    Given property "bdd6502.bus24.trace" is set to string "true"
    Given property "bdd6502.apu.trace" is set to string "true"
    Given I have a simple overclocked 6502 system
    And That does fail on BRK
    And I enable uninitialised memory read protection with immediate fail
    Given a user port to 24 bit bus is installed
    And enable user port bus debug output
    Given add a Chars V4.0 layer with registers at '0x9000' and screen addressEx '0x80' and planes addressEx '0x20'
    And the layer has 16 colours
    And the layer has overscan
    And the layer uses exact address matching
    Given show video window

    And I start writing memory at $400
    And I write the following hex bytes
      | 4c 00 04 |


    # Palette
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanPaletteData.bin" to 24bit bus at '0x9c00' and addressEx '0x01'
    # Chars
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanStatus_map.bin" to 24bit bus at '0x4000' and addressEx '0x80'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanStatus_map.bin2" to 24bit bus at '0x8000' and addressEx '0x80'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanStatus_plane0.bin" to 24bit bus at '0x2000' and addressEx '0x20'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanStatus_plane1.bin" to 24bit bus at '0x4000' and addressEx '0x20'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanStatus_plane2.bin" to 24bit bus at '0x8000' and addressEx '0x20'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanStatus_plane3.bin" to 24bit bus at '0x0000' and addressEx '0x20'

    # Wide overscan can use 0x2b which has a couple of chars on the left masked for scrolling and hits the right edge _HSYNC
    # Use the 320 wide settings
    Given write data byte '0x29' to 24bit bus at '0x9e09' and addressEx '0x01'
    # Enable all layers
    Given write data byte '0x0f' to 24bit bus at '0x9e0a' and addressEx '0x01'

    # Enable display
    Given write data byte '0x20' to 24bit bus at '0x9e00' and addressEx '0x01'
    # Layer priority all
    Given write data byte '0x00' to 24bit bus at '0x9e08' and addressEx '0x01'
    # Change video background colour
    Given write data byte '0x00' to 24bit bus at '0x9e0b' and addressEx '0x01'

    # Audio2 1 data
    Given load binary file "c:\temp\aburner1_mono.bin" into temporary memory
    Given write data from temporary memory to address24 '0' and addressEx '0x06' using bank switch register at '0x8030' and addressEx '0x01'


    # Audio2 1 registers
    # Control
    Given write data byte '0x00' to 24bit bus at '0x8031' and addressEx '0x01'
    # Volume
    Given write data byte '0xff' to 24bit bus at '0x8032' and addressEx '0x01'
    # Address
    Given write data byte '0x00' to 24bit bus at '0x8033' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8034' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8035' and addressEx '0x01'
    # Length
    Given write data byte '0x1c' to 24bit bus at '0x8036' and addressEx '0x01'
    Given write data byte '0x34' to 24bit bus at '0x8037' and addressEx '0x01'
    Given write data byte '0x16' to 24bit bus at '0x8038' and addressEx '0x01'
    # Rate
    # 12500 Hz
    Given write data byte '0x3d' to 24bit bus at '0x8039' and addressEx '0x01'
    Given write data byte '0x0a' to 24bit bus at '0x803a' and addressEx '0x01'
    # Control
    Given write data byte '0x03' to 24bit bus at '0x8031' and addressEx '0x01'

    Given video display does not save debug BMP images
    Given limit video display to 60 fps
#    When I execute the procedure at $400 until return
    When render 100 video display frames
    When rendering the video until window closed



  @TC-20
  Scenario: Test Audio3
    Given clear all external devices
    Given a new video display with overscan and 16 colours
    Given set the video display to RGB colour 5 6 5
    Given set the video display with 32 palette banks
    Given the display uses exact address matching
    Given the display has RGB background functionality
    And enable video display bus debug output

    Given a new audio3 1 expansion with registers at '0x8000' and addressEx '0x06'
    And the audio3 1 expansion uses exact address matching
    Given a new audio3 2 expansion with registers at '0x8100' and addressEx '0x07'
    And the audio3 2 expansion uses exact address matching

    And audio refresh window every 32 instructions
    And audio refresh window every 0 instructions
    And audio refresh is independent
    Given video display does not save debug BMP images
    Given property "bdd6502.bus24.trace" is set to string "true"
    Given property "bdd6502.apu.trace" is set to string "true"
    Given I have a simple overclocked 6502 system
    And That does fail on BRK
    And I enable uninitialised memory read protection with immediate fail
    Given a user port to 24 bit bus is installed
    And enable user port bus debug output
    Given add a Chars V4.0 layer with registers at '0x9000' and screen addressEx '0x80' and planes addressEx '0x20'
    And the layer has 16 colours
    And the layer has overscan
    And the layer uses exact address matching
    Given show video window

    And I start writing memory at $400
    And I write the following hex bytes
      | 4c 00 04 |


    # Palette
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanPaletteData.bin" to 24bit bus at '0x9c00' and addressEx '0x01'
    # Chars
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanStatus_map.bin" to 24bit bus at '0x4000' and addressEx '0x80'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanStatus_map.bin2" to 24bit bus at '0x8000' and addressEx '0x80'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanStatus_plane0.bin" to 24bit bus at '0x2000' and addressEx '0x20'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanStatus_plane1.bin" to 24bit bus at '0x4000' and addressEx '0x20'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanStatus_plane2.bin" to 24bit bus at '0x8000' and addressEx '0x20'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanStatus_plane3.bin" to 24bit bus at '0x0000' and addressEx '0x20'

    # Wide overscan can use 0x2b which has a couple of chars on the left masked for scrolling and hits the right edge _HSYNC
    # Use the 320 wide settings
    Given write data byte '0x29' to 24bit bus at '0x9e09' and addressEx '0x01'
    # Enable all layers
    Given write data byte '0x0f' to 24bit bus at '0x9e0a' and addressEx '0x01'

    # Enable display
    Given write data byte '0x20' to 24bit bus at '0x9e00' and addressEx '0x01'
    # Layer priority all
    Given write data byte '0x00' to 24bit bus at '0x9e08' and addressEx '0x01'
    # Change video background colour
    Given write data byte '0x00' to 24bit bus at '0x9e0b' and addressEx '0x01'

    # Audio3 1 data
    Given load binary file "C:\temp\aburner_left.vcd" into temporary memory
    Given write data from temporary memory to address24 '0' and addressEx '0x06' using bank switch register at '0x8030' and addressEx '0x01'
    Given load binary file "C:\temp\aburner_left3.vcd" into temporary memory
    Given write data from temporary memory to address24 '896987' and addressEx '0x06' using bank switch register at '0x8030' and addressEx '0x01'
    Given load binary file "C:\temp\aburner_left6.vcd" into temporary memory
    Given write data from temporary memory to address24 '896987+1343276' and addressEx '0x06' using bank switch register at '0x8030' and addressEx '0x01'

    # Audio3 2 data
    Given load binary file "C:\temp\aburner_right.vcd" into temporary memory
    Given write data from temporary memory to address24 '0' and addressEx '0x07' using bank switch register at '0x8130' and addressEx '0x01'
    Given load binary file "C:\temp\aburner_right3.vcd" into temporary memory
    Given write data from temporary memory to address24 '873536' and addressEx '0x06' using bank switch register at '0x8030' and addressEx '0x01'
    Given load binary file "C:\temp\aburner_right6.vcd" into temporary memory
    Given write data from temporary memory to address24 '873536+1326593' and addressEx '0x06' using bank switch register at '0x8030' and addressEx '0x01'


    # Audio3 1 registers
    # Control
    Given write data byte '0x00' to 24bit bus at '0x8031' and addressEx '0x01'
    # Volume
    Given write data byte '0xff' to 24bit bus at '0x8032' and addressEx '0x01'
    # Address
    Given write data byte '0x00' to 24bit bus at '0x8033' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8034' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8035' and addressEx '0x01'
    # Length
    Given write data byte '0xd8' to 24bit bus at '0x8036' and addressEx '0x01'
    Given write data byte '0xaf' to 24bit bus at '0x8037' and addressEx '0x01'
    Given write data byte '0x0d' to 24bit bus at '0x8038' and addressEx '0x01'
    # Rate
    # 25000 Hz
    Given write data byte '0x00' to 24bit bus at '0x8039' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x803a' and addressEx '0x01'
    # 12500 Hz
    Given write data byte '0x00' to 24bit bus at '0x8039' and addressEx '0x01'
    Given write data byte '0x08' to 24bit bus at '0x803a' and addressEx '0x01'
    # Control
    Given write data byte '0x03' to 24bit bus at '0x8031' and addressEx '0x01'

    # Audio3 2 registers
    # Control
    Given write data byte '0x00' to 24bit bus at '0x8131' and addressEx '0x01'
    # Volume
    Given write data byte '0xff' to 24bit bus at '0x8132' and addressEx '0x01'
    # Address
    Given write data byte '0x00' to 24bit bus at '0x8133' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8134' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8135' and addressEx '0x01'
    # Length
    Given write data byte '0x40' to 24bit bus at '0x8136' and addressEx '0x01'
    Given write data byte '0x54' to 24bit bus at '0x8137' and addressEx '0x01'
    Given write data byte '0x0d' to 24bit bus at '0x8138' and addressEx '0x01'
    # Rate
    # 25000 Hz
    Given write data byte '0x00' to 24bit bus at '0x8139' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x813a' and addressEx '0x01'
    # 12500 Hz
    Given write data byte '0x00' to 24bit bus at '0x8139' and addressEx '0x01'
    Given write data byte '0x08' to 24bit bus at '0x813a' and addressEx '0x01'
    # Control
    Given write data byte '0x03' to 24bit bus at '0x8131' and addressEx '0x01'

    Given video display does not save debug BMP images
    Given limit video display to 60 fps
#    When I execute the procedure at $400 until return
    When render 100 video display frames
    When rendering the video until window closed
