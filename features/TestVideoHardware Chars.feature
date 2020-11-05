Feature: Tests the videocharacter screen data conversion

  Uses ImageToBitplane config: oldbridge char screen
  Use ImageToBitplane config to improve clouds conversion as it priortises their colours: oldbridge char screen with rgbfactor
  Or uses ImageToBitplane config: map_9 - Copy - chars.png char screen


  @TC-2
  Scenario: Chars display test
    Given clear all external devices
    Given a new video display
    And enable video display bus debug output
    Given video display processes 8 pixels per instruction
    Given video display refresh window every 32 instructions
    Given video display does not save debug BMP images
    Given property "bdd6502.bus24.trace" is set to string "true"
    Given add a Chars layer with registers at '0x9000' and addressEx '0x20'
    Given show video window

    # Palette
    Given write data from file "C:\Work\ImageToBitplane\target\chars_paletteData.bin" to 24bit bus at '0x9c00' and addressEx '0x01'
    # Chars
    Given write data from file "C:\Work\ImageToBitplane\target\chars_plane0.bin" to 24bit bus at '0x2000' and addressEx '0x20'
    Given write data from file "C:\Work\ImageToBitplane\target\chars_plane1.bin" to 24bit bus at '0x4000' and addressEx '0x20'
    Given write data from file "C:\Work\ImageToBitplane\target\chars_plane2.bin" to 24bit bus at '0x8000' and addressEx '0x20'
    # Chars screen
    Given write data from file "C:\Work\ImageToBitplane\target\chars_scr.bin" to 24bit bus at '0x9000' and addressEx '0x01'

    # Enable display with all borders
    Given write data byte '0xf0' to 24bit bus at '0x9e00' and addressEx '0x01'


    Given render a video display frame

    When rendering the video until window closed


  @TC-2
  Scenario: Chars display test with simple user port bus code
    Given clear all external devices
    Given a new video display
    And enable video display bus debug output
    Given video display processes 8 pixels per instruction
    Given video display refresh window every 32 instructions
    Given video display does not save debug BMP images
    Given property "bdd6502.bus24.trace" is set to string "true"
    Given I have a simple overclocked 6502 system
    Given a simple user port to 24 bit bus is installed
    Given add a Chars layer with registers at '0x9000' and addressEx '0x20'
    Given show video window

    And I run the command line: ..\C64\acme.exe -v3 --lib ../ -o test.prg --labeldump test.lbl -f cbm "features/TestVideoHardware Chars.a"
    And I run the command line: ..\C64\bin\LZMPi.exe -c64mbe test.prg testcmp.prg $200
    And I load prg "test.prg"
    And I load labels "test.lbl"
#    And I enable trace with indent

#    When I execute the procedure at start for no more than 99999999 instructions
    When I execute the procedure at start until return
    Given render a video display frame

    When rendering the video until window closed
