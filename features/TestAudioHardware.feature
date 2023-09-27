Feature: Tests the video and audio hardware expansion together

  @TC-1-a
  Scenario: Full display test with sprites, borders, contention, chars, tiles, and mode7, and sample play
    Given I run the command line: ..\C64\acme.exe -v3 --lib ../ -o test.prg --labeldump test.lbl -f cbm features/TestAudioHardware.a

    Given clear all external devices
    Given a new video display
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
