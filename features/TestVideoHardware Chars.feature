Feature: Tests the video character screen data conversion

  Uses ImageToBitplane config: oldbridge char screen
  Use ImageToBitplane config to improve clouds conversion as it priortises their colours: oldbridge char screen with rgbfactor
  Use ImageToBitplane config to add use of colour 0, since if it is the back most layer then then it can use all 8 colours in the character block: oldbridge char screen with rgbfactor no force colour 0
  Or uses ImageToBitplane config: map_9 - Copy - chars.png char screen


  @TC-2
  Scenario: Chars display test
    Given clear all external devices
    Given a new video display
    Given set the video display to RGB colour 5 6 5
    Given set the video display with 32 palette banks
    And enable video display bus debug output
    Given video display processes 8 pixels per instruction
    Given video display refresh window every 32 instructions
    Given video display saves debug BMP images to leaf filename "target/frames/TC-2-"
    Given property "bdd6502.bus24.trace" is set to string "true"
#    Given add a GetBackground layer fetching from layer index '0'
#    Given add a StaticColour layer for palette index '1'
    Given add a Chars layer with registers at '0x9000' and addressEx '0x20'
    # Enable display
    Given write data byte '0x20' to 24bit bus at '0x9e00' and addressEx '0x01'
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
    Given write data byte '0x20' to 24bit bus at '0x9e00' and addressEx '0x01'

    Given render a video display frame
    Then expect image "testdata/TC-2-000000.bmp" to be identical to "target/frames/TC-2-000000.bmp"

#    When rendering the video until window closed


  @TC-3
  Scenario: Chars display test with simple user port bus code
    Given clear all external devices
    Given a new video display
    Given set the video display to RGB colour 5 6 5
    Given set the video display with 32 palette banks
    And enable video display bus debug output
    Given video display processes 24 pixels per instruction
    Given video display refresh window every 32 instructions
    Given video display add joystick to port 2
    Given video display does not save debug BMP images
    Given property "bdd6502.bus24.trace" is set to string "true"
    Given I have a simple overclocked 6502 system
    Given a simple user port to 24 bit bus is installed
    Given add a StaticColour layer for palette index '0x7f'
    Given add a Chars layer with registers at '0x9000' and addressEx '0x20'
    # Enable display, since the Video_EnableDisplay code dos not properly use the simple bus, however it is deprecated
    Given write data byte '0x20' to 24bit bus at '0x9e00' and addressEx '0x01'
    Given show video window

    And I run the command line: ..\C64\acme.exe -v3 --lib ../ -o test.prg --labeldump test.lbl -f cbm "features/TestVideoHardware Chars.a"
    And I run the command line: ..\C64\bin\LZMPi.exe -c64mbe test.prg testcmp.prg $200
    And I load prg "test.prg"
    And I load labels "test.lbl"
#    And I enable trace with indent

#    When I execute the procedure at start for no more than 99999999 instructions
    When I execute the procedure at DisplayScreen until return
    Given render a video display frame

    When I execute the procedure at mainLoop until return

    When rendering the video until window closed




  @TC-4
  Scenario: Chars display test with full user port bus code
    Given clear all external devices
    Given a new video display
    Given set the video display to RGB colour 5 6 5
    Given set the video display with 32 palette banks
    And enable video display bus debug output
    And disable debug pixel picking
    And enable debug pixel picking
    Given video display processes 24 pixels per instruction
    Given video display refresh window every 32 instructions
    Given video display add joystick to port 2
    Given video display does not save debug BMP images
    Given property "bdd6502.bus24.trace" is set to string "true"
    Given I have a simple overclocked 6502 system
    Given a user port to 24 bit bus is installed
    Given add a StaticColour layer for palette index '0x7f'
    Given add a Chars layer with registers at '0x9000' and addressEx '0x20'
    Given show video window

    And I run the command line: ..\C64\acme.exe -v3 --lib ../ -o test.prg --labeldump test.lbl -f cbm "features/TestVideoHardware Chars full.a"
    And I run the command line: ..\C64\bin\LZMPi.exe -c64mbe test.prg testcmp.prg $200
    And I load prg "test.prg"
    And I load labels "test.lbl"
#    And I enable trace with indent

#    When I execute the procedure at start for no more than 99999999 instructions
    When I execute the procedure at DisplayScreen until return
    Given render a video display frame

    When I execute the procedure at mainLoop until return

    When rendering the video until window closed



  # Use: oldbridge char screen with rgbfactor 512
  @TC-2 @TC-2-1
  Scenario: Chars V4.0+ display test
    Given clear all external devices
    Given a new video display with 16 colours
    Given set the video display to RGB colour 5 6 5
    Given set the video display with 32 palette banks
    And enable video display bus debug output
    Given video display processes 8 pixels per instruction
    Given video display refresh window every 32 instructions
    Given video display saves debug BMP images to leaf filename "target/frames/TC-2-1-"
    Given property "bdd6502.bus24.trace" is set to string "true"
#    Given add a GetBackground layer fetching from layer index '0'
#    Given add a StaticColour layer for palette index '1'
    Given add a Chars V4.0 layer with registers at '0x9000' and screen addressEx '0x80' and planes addressEx '0x20'
    And the layer has 16 colours

    # Enable display
    Given write data byte '0x20' to 24bit bus at '0x9e00' and addressEx '0x01'
    Given show video window

    # Palette
    Given write data from file "C:\Work\ImageToBitplane\target\chars512_paletteData.bin" to 24bit bus at '0x9c00' and addressEx '0x01'
    # Chars
    Given write data from file "C:\Work\ImageToBitplane\target\chars512_plane0.bin" to 24bit bus at '0x2000' and addressEx '0x20'
    Given write data from file "C:\Work\ImageToBitplane\target\chars512_plane1.bin" to 24bit bus at '0x4000' and addressEx '0x20'
    Given write data from file "C:\Work\ImageToBitplane\target\chars512_plane2.bin" to 24bit bus at '0x8000' and addressEx '0x20'
    Given write data from file "C:\Work\ImageToBitplane\target\chars512_plane3.bin" to 24bit bus at '0x0000' and addressEx '0x20'
    # Chars screen
    Given write data from file "C:\Work\ImageToBitplane\target\chars512_scr.bin" to 24bit bus at '0x4000' and addressEx '0x80'

    # Test the chars layer display disable
    Given write data byte '0x02' to 24bit bus at '0x9000' and addressEx '0x01'
    Given render a video display frame

    # Test the chars layer display enable
    Given write data byte '0x00' to 24bit bus at '0x9000' and addressEx '0x01'
    Given render a video display frame

    # Test scrolling
    Given write data byte '0x01' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x02' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x03' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x04' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x05' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x06' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x07' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x08' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x09' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x0a' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x0b' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x0c' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x0d' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x0e' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x0f' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame

    Given write data byte '0x20' to 24bit bus at '0x9001' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9002' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9003' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9004' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x40' to 24bit bus at '0x9001' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9002' and addressEx '0x01'
    Given write data byte '0x30' to 24bit bus at '0x9003' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9004' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x60' to 24bit bus at '0x9001' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9002' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x9003' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9004' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x80' to 24bit bus at '0x9001' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9002' and addressEx '0x01'
    Given write data byte '0x50' to 24bit bus at '0x9003' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9004' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0xa0' to 24bit bus at '0x9001' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9002' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x9003' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9004' and addressEx '0x01'
    Given render a video display frame

    Given write data byte '0x20' to 24bit bus at '0x9001' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x9002' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9003' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x9004' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x40' to 24bit bus at '0x9001' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x9002' and addressEx '0x01'
    Given write data byte '0x30' to 24bit bus at '0x9003' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x9004' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x60' to 24bit bus at '0x9001' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x9002' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x9003' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x9004' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x80' to 24bit bus at '0x9001' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x9002' and addressEx '0x01'
    Given write data byte '0x50' to 24bit bus at '0x9003' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x9004' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0xa0' to 24bit bus at '0x9001' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x9002' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x9003' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x9004' and addressEx '0x01'
    Given render a video display frame

    Given write data byte '0x00' to 24bit bus at '0x9001' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9002' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9003' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9004' and addressEx '0x01'
    # Test scrolling with shrunk borders
    Given write data byte '0xe0' to 24bit bus at '0x9e00' and addressEx '0x01'
    Given show video window

    Given write data byte '0x01' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x02' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x03' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x04' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x05' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x06' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x07' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x08' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x09' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x0a' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x0b' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x0c' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x0d' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x0e' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x0f' to 24bit bus at '0x9001' and addressEx '0x01'
    Given render a video display frame

    Then expect image "testdata/TC-2-1-000000.bmp" to be identical to "target/frames/TC-2-1-000000.bmp"
    Then expect image "testdata/TC-2-1-000001.bmp" to be identical to "target/frames/TC-2-1-000001.bmp"
    Then expect image "testdata/TC-2-1-000002.bmp" to be identical to "target/frames/TC-2-1-000002.bmp"
    Then expect image "testdata/TC-2-1-000003.bmp" to be identical to "target/frames/TC-2-1-000003.bmp"
    Then expect image "testdata/TC-2-1-000004.bmp" to be identical to "target/frames/TC-2-1-000004.bmp"
    Then expect image "testdata/TC-2-1-000005.bmp" to be identical to "target/frames/TC-2-1-000005.bmp"
    Then expect image "testdata/TC-2-1-000006.bmp" to be identical to "target/frames/TC-2-1-000006.bmp"
    Then expect image "testdata/TC-2-1-000007.bmp" to be identical to "target/frames/TC-2-1-000007.bmp"
    Then expect image "testdata/TC-2-1-000008.bmp" to be identical to "target/frames/TC-2-1-000008.bmp"
    Then expect image "testdata/TC-2-1-000009.bmp" to be identical to "target/frames/TC-2-1-000009.bmp"

    Then expect image "testdata/TC-2-1-000010.bmp" to be identical to "target/frames/TC-2-1-000010.bmp"
    Then expect image "testdata/TC-2-1-000011.bmp" to be identical to "target/frames/TC-2-1-000011.bmp"
    Then expect image "testdata/TC-2-1-000012.bmp" to be identical to "target/frames/TC-2-1-000012.bmp"
    Then expect image "testdata/TC-2-1-000013.bmp" to be identical to "target/frames/TC-2-1-000013.bmp"
    Then expect image "testdata/TC-2-1-000014.bmp" to be identical to "target/frames/TC-2-1-000014.bmp"
    Then expect image "testdata/TC-2-1-000015.bmp" to be identical to "target/frames/TC-2-1-000015.bmp"
    Then expect image "testdata/TC-2-1-000016.bmp" to be identical to "target/frames/TC-2-1-000016.bmp"
    Then expect image "testdata/TC-2-1-000017.bmp" to be identical to "target/frames/TC-2-1-000017.bmp"
    Then expect image "testdata/TC-2-1-000018.bmp" to be identical to "target/frames/TC-2-1-000018.bmp"
    Then expect image "testdata/TC-2-1-000019.bmp" to be identical to "target/frames/TC-2-1-000019.bmp"

    Then expect image "testdata/TC-2-1-000020.bmp" to be identical to "target/frames/TC-2-1-000020.bmp"
    Then expect image "testdata/TC-2-1-000021.bmp" to be identical to "target/frames/TC-2-1-000021.bmp"
    Then expect image "testdata/TC-2-1-000022.bmp" to be identical to "target/frames/TC-2-1-000022.bmp"
    Then expect image "testdata/TC-2-1-000023.bmp" to be identical to "target/frames/TC-2-1-000023.bmp"
    Then expect image "testdata/TC-2-1-000024.bmp" to be identical to "target/frames/TC-2-1-000024.bmp"
    Then expect image "testdata/TC-2-1-000025.bmp" to be identical to "target/frames/TC-2-1-000025.bmp"
    Then expect image "testdata/TC-2-1-000026.bmp" to be identical to "target/frames/TC-2-1-000026.bmp"
    Then expect image "testdata/TC-2-1-000027.bmp" to be identical to "target/frames/TC-2-1-000027.bmp"
    Then expect image "testdata/TC-2-1-000028.bmp" to be identical to "target/frames/TC-2-1-000028.bmp"
    Then expect image "testdata/TC-2-1-000029.bmp" to be identical to "target/frames/TC-2-1-000029.bmp"

    Then expect image "testdata/TC-2-1-000030.bmp" to be identical to "target/frames/TC-2-1-000030.bmp"
    Then expect image "testdata/TC-2-1-000031.bmp" to be identical to "target/frames/TC-2-1-000031.bmp"
    Then expect image "testdata/TC-2-1-000032.bmp" to be identical to "target/frames/TC-2-1-000032.bmp"
    Then expect image "testdata/TC-2-1-000033.bmp" to be identical to "target/frames/TC-2-1-000033.bmp"
    Then expect image "testdata/TC-2-1-000034.bmp" to be identical to "target/frames/TC-2-1-000034.bmp"
    Then expect image "testdata/TC-2-1-000035.bmp" to be identical to "target/frames/TC-2-1-000035.bmp"
    Then expect image "testdata/TC-2-1-000036.bmp" to be identical to "target/frames/TC-2-1-000036.bmp"
    Then expect image "testdata/TC-2-1-000037.bmp" to be identical to "target/frames/TC-2-1-000037.bmp"
    Then expect image "testdata/TC-2-1-000038.bmp" to be identical to "target/frames/TC-2-1-000038.bmp"
    Then expect image "testdata/TC-2-1-000039.bmp" to be identical to "target/frames/TC-2-1-000039.bmp"

    Then expect image "testdata/TC-2-1-000040.bmp" to be identical to "target/frames/TC-2-1-000040.bmp"
    Then expect image "testdata/TC-2-1-000041.bmp" to be identical to "target/frames/TC-2-1-000041.bmp"

#    When rendering the video until window closed
