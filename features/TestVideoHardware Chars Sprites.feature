Feature: Tests the video character screen data conversion and sprites

  Uses ImageToBitplane config: oldbridge char screen
  >>> Use ImageToBitplane config to improve clouds conversion as it priortises their colours: oldbridge char screen with rgbfactor
  Use ImageToBitplane config to add use of colour 0, since if it is the back most layer then then it can use all 8 colours in the character block: oldbridge char screen with rgbfactor no force colour 0
  Or uses ImageToBitplane config: map_9 - Copy - chars.png char screen


  @TC-5
  Scenario: Chars and sprites display test with full user port bus code
    Given clear all external devices
    Given a new video display
    Given set the video display to RGB colour 5 6 5
    Given set the video display with 32 palette banks
    And enable video display bus debug output
    Given video display processes 24 pixels per instruction
    Given video display refresh window every 32 instructions
    Given video display add joystick to port 2
    Given video display does not save debug BMP images
#    Given property "bdd6502.bus24.trace" is set to string "true"
    Given I have a simple overclocked 6502 system
    Given a user port to 24 bit bus is installed
    Given add a StaticColour layer for palette index '0x7f'
    Given add a Chars layer with registers at '0x9000' and addressEx '0x20'
    Given add a Sprites layer with registers at '0x9800' and addressEx '0x10'

    Given randomly initialise all memory using seed 1234
    Given show video window

    And I run the command line: ..\C64\acme.exe -v3 --lib ../ -o test.prg --labeldump test.lbl -f cbm "features/TestVideoHardware Chars Sprites full.a"
    And I run the command line: ..\C64\bin\LZMPi.exe -c64mbe test.prg testcmp.prg $200
    And I load prg "test.prg"
    And I load labels "test.lbl"
#    And I enable trace with indent

    When enable remote debugging
#    And wait for debugger connection

#    When I execute the procedure at start for no more than 99999999 instructions
    When I execute the procedure at DisplayScreen until return
    Given render a video display frame

    When I execute the procedure at mainLoop until return

    When rendering the video until window closed




  @TC-7
  Scenario: Testing Sprites2 layer
    Given clear all external devices
    Given a new video display with overscan and 16 colours
    Given set the video display to RGB colour 5 6 5
    Given set the video display with 32 palette banks
    And enable video display bus debug output
    Given video display processes 24 pixels per instruction
    Given video display refresh window every 32 instructions
    Given video display saves debug BMP images to leaf filename "target/frames/TC-7-"
    Given video display add joystick to port 2
#    Given video display does not save debug BMP images
#    Given property "bdd6502.bus24.trace" is set to string "true"
    Given I have a simple overclocked 6502 system
    Given a user port to 24 bit bus is installed
    Given add a StaticColour layer for palette index '0x07'
    And the layer has 16 colours
    And the layer has overscan
    Given add a Chars V4.0 layer with registers at '0x9000' and screen addressEx '0x80' and planes addressEx '0x20'
    And the layer has 16 colours
    And the layer has overscan
    Given add a Sprites2 layer with registers at '0x9200' and addressEx '0x10' and running at 14.31818MHz
    And the layer has 16 colours
    And the layer has overscan
    Given show video window
    Given limit video display to 60 fps

    # Palette
    Given write data from file "C:\Work\ImageToBitplane\target\chars512_paletteData.bin" to 24bit bus at '0x9c00' and addressEx '0x01'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanScaledPaletteData.bin" to 24bit bus at '0x9d60' and addressEx '0x01'
    Given write data from file "C:\Work\ImageToBitplane\target\testconcat4PaletteData.bin" to 24bit bus at '0x9d80' and addressEx '0x01'


    # Sprites2 data
    # Turrican Scaled
    # buildings concat
    # Building Scaled
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanScaledSprites0.bin" to 24bit bus at '0x2000' and addressEx '0x10'
    Given write data from file "C:\work\C64\VideoHardware\tmp\TurricanScaledSprites1.bin" to 24bit bus at '0x4000' and addressEx '0x10'
    Given write data from file "C:\work\C64\VideoHardware\tmp\TurricanClouds_tiles.bin2" to 24bit bus at '0x8000' and addressEx '0x10'
    Given write data from file "C:\Work\ImageToBitplane\target\testconcat4ScaledSprites0.bin" to 24bit bus at '0x0000' and addressEx '0x10'

    # Chars
    # oldbridge char screen with rgbfactor 512
    Given write data from file "C:\Work\ImageToBitplane\target\chars1024_plane0.bin" to 24bit bus at '0x2000' and addressEx '0x20'
    Given write data from file "C:\Work\ImageToBitplane\target\chars1024_plane1.bin" to 24bit bus at '0x4000' and addressEx '0x20'
    Given write data from file "C:\Work\ImageToBitplane\target\chars1024_plane2.bin" to 24bit bus at '0x8000' and addressEx '0x20'
    Given write data from file "C:\Work\ImageToBitplane\target\chars1024_plane3.bin" to 24bit bus at '0x0000' and addressEx '0x20'
    # Chars screen
    Given write data from file "C:\Work\ImageToBitplane\target\chars1024_scr.bin" to 24bit bus at '0x4000' and addressEx '0x80'
    Given write data from file "C:\Work\ImageToBitplane\target\chars1024_scr.bin2" to 24bit bus at '0x8000' and addressEx '0x80'

    # Enable display
    Given write data byte '0x20' to 24bit bus at '0x9e00' and addressEx '0x01'
    # Default display priority
    Given write data byte '0xe4' to 24bit bus at '0x9e08' and addressEx '0x01'
    # Overscan control
    Given write data byte '0x29' to 24bit bus at '0x9e09' and addressEx '0x01'
    # Just enable Sprites2, not the chars
    Given write data byte '0x01' to 24bit bus at '0x9e0a' and addressEx '0x01'

    # Sprites2 registers
    # Sprites support X and Y flips with X & Y repeating patterns
    # Palette | 0x10 = MSBX | 0x20 = MSBY | 0x40 = flipX | 0x80 = flipY
    # Y pos
    # Y size (in screen pixels, regardless of scale)
    # X pos
    # X scale extent (uses internal coordinates)
    # Y inv scale (*32)
    # X inv scale (*32)
    # Sprite frame (index) | 0x40 = halfX | 0x80 = halfY
    Given write data byte '0x8b' to 24bit bus at '0x9200' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x9201' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9202' and addressEx '0x01'
    Given write data byte '0xf8' to 24bit bus at '0x9203' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9204' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9205' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9206' and addressEx '0x01'
    Given write data byte '0xc0' to 24bit bus at '0x9207' and addressEx '0x01'

    Given write data byte '0x4b' to 24bit bus at '0x9208' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x9209' and addressEx '0x01'
    Given write data byte '0x30' to 24bit bus at '0x920a' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x920b' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x920c' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x920d' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x920e' and addressEx '0x01'
    Given write data byte '0x04' to 24bit bus at '0x920f' and addressEx '0x01'

    # Use MSB X test
    Given write data byte '0xdb' to 24bit bus at '0x9210' and addressEx '0x01'
#    Given write data byte '0x0b' to 24bit bus at '0x9210' and addressEx '0x01'
    Given write data byte '0xc0' to 24bit bus at '0x9211' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x9212' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x9213' and addressEx '0x01'
#    Given write data byte '0x40' to 24bit bus at '0x9213' and addressEx '0x01'
    Given write data byte '0x18' to 24bit bus at '0x9214' and addressEx '0x01'
    Given write data byte '0x08' to 24bit bus at '0x9215' and addressEx '0x01'
    Given write data byte '0x08' to 24bit bus at '0x9216' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9217' and addressEx '0x01'

    # Use MSB Y test
    Given write data byte '0x2b' to 24bit bus at '0x9218' and addressEx '0x01'
    Given write data byte '0xf0' to 24bit bus at '0x9219' and addressEx '0x01'
    Given write data byte '0x68' to 24bit bus at '0x921a' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x921b' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x921c' and addressEx '0x01'
    Given write data byte '0x06' to 24bit bus at '0x921d' and addressEx '0x01'
    Given write data byte '0x06' to 24bit bus at '0x921e' and addressEx '0x01'
    Given write data byte '0x02' to 24bit bus at '0x921f' and addressEx '0x01'

    # /2
    Given write data byte '0x0c' to 24bit bus at '0x9220' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x9221' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9222' and addressEx '0x01'
    Given write data byte '0x90' to 24bit bus at '0x9223' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9224' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x9225' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x9226' and addressEx '0x01'
    Given write data byte '0x30' to 24bit bus at '0x9227' and addressEx '0x01'

    Given write data byte '0x0c' to 24bit bus at '0x9228' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x9229' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x922a' and addressEx '0x01'
    Given write data byte '0xa0' to 24bit bus at '0x922b' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x922c' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x922d' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x922e' and addressEx '0x01'
    Given write data byte '0x31' to 24bit bus at '0x922f' and addressEx '0x01'

    Given write data byte '0x0c' to 24bit bus at '0x9230' and addressEx '0x01'
    Given write data byte '0x90' to 24bit bus at '0x9231' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9232' and addressEx '0x01'
    Given write data byte '0x90' to 24bit bus at '0x9233' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9234' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x9235' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x9236' and addressEx '0x01'
    Given write data byte '0x32' to 24bit bus at '0x9237' and addressEx '0x01'

    Given write data byte '0x0c' to 24bit bus at '0x9238' and addressEx '0x01'
    Given write data byte '0x90' to 24bit bus at '0x9239' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x923a' and addressEx '0x01'
    Given write data byte '0xa0' to 24bit bus at '0x923b' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x923c' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x923d' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x923e' and addressEx '0x01'
    Given write data byte '0x33' to 24bit bus at '0x923f' and addressEx '0x01'

    # *2
    Given write data byte '0x0c' to 24bit bus at '0x9240' and addressEx '0x01'
    Given write data byte '0xa0' to 24bit bus at '0x9241' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x9242' and addressEx '0x01'
    Given write data byte '0xb0' to 24bit bus at '0x9243' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9244' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9245' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9246' and addressEx '0x01'
    Given write data byte '0x30' to 24bit bus at '0x9247' and addressEx '0x01'

    Given write data byte '0x0c' to 24bit bus at '0x9248' and addressEx '0x01'
    Given write data byte '0xa0' to 24bit bus at '0x9249' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x924a' and addressEx '0x01'
    Given write data byte '0xf0' to 24bit bus at '0x924b' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x924c' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x924d' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x924e' and addressEx '0x01'
    Given write data byte '0x31' to 24bit bus at '0x924f' and addressEx '0x01'

    Given write data byte '0x0c' to 24bit bus at '0x9250' and addressEx '0x01'
    Given write data byte '0xe0' to 24bit bus at '0x9251' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9252' and addressEx '0x01'
    Given write data byte '0xb0' to 24bit bus at '0x9253' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9254' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9255' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9256' and addressEx '0x01'
    Given write data byte '0x32' to 24bit bus at '0x9257' and addressEx '0x01'

    Given write data byte '0x0c' to 24bit bus at '0x9258' and addressEx '0x01'
    Given write data byte '0xe0' to 24bit bus at '0x9259' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x925a' and addressEx '0x01'
    Given write data byte '0xf0' to 24bit bus at '0x925b' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x925c' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x925d' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x925e' and addressEx '0x01'
    Given write data byte '0x33' to 24bit bus at '0x925f' and addressEx '0x01'

    # Will be end of list, but temporarily set this to be a valid but vertically very small sprite
    Given write data byte '0x01' to 24bit bus at '0x9262' and addressEx '0x01'
    # After the end of list, this large sprite should not be displayed
    Given write data byte '0x0c' to 24bit bus at '0x9268' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9269' and addressEx '0x01'
    Given write data byte '0xf0' to 24bit bus at '0x926a' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x926b' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x926c' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x926d' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x926e' and addressEx '0x01'
    Given write data byte '0x33' to 24bit bus at '0x926f' and addressEx '0x01'

    Given render a video display frame
#    When display until window closed

    # Now test the end of list
    Given write data byte '0x00' to 24bit bus at '0x9262' and addressEx '0x01'

    Given render a video display frame
#    When display until window closed

    Then expect image "testdata/TC-7-000000.bmp" to be identical to "target/frames/TC-7-000000.bmp"
    Then expect image "testdata/TC-7-000001.bmp" to be identical to "target/frames/TC-7-000001.bmp"

    Given video display does not save debug BMP images

    And I run the command line: ..\C64\acme.exe -v3 --lib ../ -o test.prg --labeldump test.lbl -f cbm "features/TestVideoHardware Sprites2.a"
    And I run the command line: ..\C64\bin\LZMPi.exe -c64mbe test.prg testcmp.prg $200
    And I load prg "test.prg"
    And I load labels "test.lbl"
#    And I enable trace with indent

    When enable remote debugging
#    And wait for debugger connection

    When I execute the procedure at DisplayScreen until return
    When I execute the procedure at mainLoop until return

    When rendering the video until window closed




  @TC-10
  Scenario: Testing Sprites3 layer
    Given clear all external devices
    Given a new video display with overscan and 16 colours
    Given set the video display to RGB colour 5 6 5
    Given set the video display with 32 palette banks
    And enable video display bus debug output
    Given video display processes 24 pixels per instruction
    Given video display refresh window every 32 instructions
    Given video display saves debug BMP images to leaf filename "target/frames/TC-10-"
    Given video display add joystick to port 2
#    Given video display does not save debug BMP images
#    Given property "bdd6502.bus24.trace" is set to string "true"
    Given I have a simple overclocked 6502 system
    Given a user port to 24 bit bus is installed
    Given add a StaticColour layer for palette index '0x07'
    And the layer has 16 colours
    And the layer has overscan
    Given add a Chars V4.0 layer with registers at '0x9000' and screen addressEx '0x80' and planes addressEx '0x20'
    And the layer has 16 colours
    And the layer has overscan
    Given add a Sprites3 layer with registers at '0x9200' and addressEx '0x10'
    And the layer has 16 colours
    And the layer has overscan
    Given show video window
    Given limit video display to 60 fps

    # Palette
    Given write data from file "C:\Work\ImageToBitplane\target\chars512_paletteData.bin" to 24bit bus at '0x9c00' and addressEx '0x01'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanScaledPaletteData.bin" to 24bit bus at '0x9d60' and addressEx '0x01'
    Given write data from file "C:\Work\ImageToBitplane\target\testconcat4PaletteData.bin" to 24bit bus at '0x9d80' and addressEx '0x01'


    # Sprites2 data
    # Turrican Scaled
    # buildings concat
    # Building Scaled
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanScaledSprites0.bin" to 24bit bus at '0x2000' and addressEx '0x10'
    Given write data from file "C:\work\C64\VideoHardware\tmp\TurricanScaledSprites1.bin" to 24bit bus at '0x4000' and addressEx '0x10'
    Given write data from file "C:\work\C64\VideoHardware\tmp\TurricanClouds_tiles.bin2" to 24bit bus at '0x8000' and addressEx '0x10'
    Given write data from file "C:\Work\ImageToBitplane\target\testconcat4ScaledSprites0.bin" to 24bit bus at '0x0000' and addressEx '0x10'

    # Chars
    # oldbridge char screen with rgbfactor 512
    Given write data from file "C:\Work\ImageToBitplane\target\chars1024_plane0.bin" to 24bit bus at '0x2000' and addressEx '0x20'
    Given write data from file "C:\Work\ImageToBitplane\target\chars1024_plane1.bin" to 24bit bus at '0x4000' and addressEx '0x20'
    Given write data from file "C:\Work\ImageToBitplane\target\chars1024_plane2.bin" to 24bit bus at '0x8000' and addressEx '0x20'
    Given write data from file "C:\Work\ImageToBitplane\target\chars1024_plane3.bin" to 24bit bus at '0x0000' and addressEx '0x20'
    # Chars screen
    Given write data from file "C:\Work\ImageToBitplane\target\chars1024_scr.bin" to 24bit bus at '0x4000' and addressEx '0x80'
    Given write data from file "C:\Work\ImageToBitplane\target\chars1024_scr.bin2" to 24bit bus at '0x8000' and addressEx '0x80'

    # Enable display
    Given write data byte '0x20' to 24bit bus at '0x9e00' and addressEx '0x01'
    # Default display priority
    Given write data byte '0xe4' to 24bit bus at '0x9e08' and addressEx '0x01'
    # Overscan control
    Given write data byte '0x29' to 24bit bus at '0x9e09' and addressEx '0x01'
    # Just enable Sprites2, not the chars
    Given write data byte '0x01' to 24bit bus at '0x9e0a' and addressEx '0x01'

    # Sprites2 registers
    # Sprites support X and Y flips with X & Y repeating patterns
    # Palette | 0x10 = MSBX | 0x20 = MSBY | 0x40 = flipX | 0x80 = flipY
    # Y pos
    # Y size (in screen pixels, regardless of scale)
    # X pos
    # X scale extent (uses internal coordinates)
    # Y inv scale (*32)
    # X inv scale (*32)
    # Sprite frame (index) | 0x40 = halfX | 0x80 = halfY
    Given write data byte '0x8b' to 24bit bus at '0x9200' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x9201' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9202' and addressEx '0x01'
    Given write data byte '0xf8' to 24bit bus at '0x9203' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9204' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9205' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9206' and addressEx '0x01'
    Given write data byte '0xc0' to 24bit bus at '0x9207' and addressEx '0x01'

#    Given write data byte '0x00' to 24bit bus at '0x9208' and addressEx '0x01'
#    Given write data byte '0x00' to 24bit bus at '0x9209' and addressEx '0x01'
#    Given write data byte '0x00' to 24bit bus at '0x920a' and addressEx '0x01'

    Given write data byte '0x4b' to 24bit bus at '0x9208' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x9209' and addressEx '0x01'
    Given write data byte '0x30' to 24bit bus at '0x920a' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x920b' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x920c' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x920d' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x920e' and addressEx '0x01'
    Given write data byte '0x04' to 24bit bus at '0x920f' and addressEx '0x01'

    # Use MSB X test
    Given write data byte '0xdb' to 24bit bus at '0x9210' and addressEx '0x01'
#    Given write data byte '0x0b' to 24bit bus at '0x9210' and addressEx '0x01'
    Given write data byte '0xc0' to 24bit bus at '0x9211' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x9212' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x9213' and addressEx '0x01'
#    Given write data byte '0x40' to 24bit bus at '0x9213' and addressEx '0x01'
    Given write data byte '0x18' to 24bit bus at '0x9214' and addressEx '0x01'
    Given write data byte '0x08' to 24bit bus at '0x9215' and addressEx '0x01'
    Given write data byte '0x08' to 24bit bus at '0x9216' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9217' and addressEx '0x01'

    # Use MSB Y test
    Given write data byte '0x2b' to 24bit bus at '0x9218' and addressEx '0x01'
    Given write data byte '0xf0' to 24bit bus at '0x9219' and addressEx '0x01'
    Given write data byte '0x68' to 24bit bus at '0x921a' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x921b' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x921c' and addressEx '0x01'
    Given write data byte '0x06' to 24bit bus at '0x921d' and addressEx '0x01'
    Given write data byte '0x06' to 24bit bus at '0x921e' and addressEx '0x01'
    Given write data byte '0x02' to 24bit bus at '0x921f' and addressEx '0x01'

    # /2
    Given write data byte '0x0c' to 24bit bus at '0x9220' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x9221' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9222' and addressEx '0x01'
    Given write data byte '0x90' to 24bit bus at '0x9223' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9224' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x9225' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x9226' and addressEx '0x01'
    Given write data byte '0x30' to 24bit bus at '0x9227' and addressEx '0x01'

    Given write data byte '0x0c' to 24bit bus at '0x9228' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x9229' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x922a' and addressEx '0x01'
    Given write data byte '0xa0' to 24bit bus at '0x922b' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x922c' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x922d' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x922e' and addressEx '0x01'
    Given write data byte '0x31' to 24bit bus at '0x922f' and addressEx '0x01'

    Given write data byte '0x0c' to 24bit bus at '0x9230' and addressEx '0x01'
    Given write data byte '0x90' to 24bit bus at '0x9231' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9232' and addressEx '0x01'
    Given write data byte '0x90' to 24bit bus at '0x9233' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9234' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x9235' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x9236' and addressEx '0x01'
    Given write data byte '0x32' to 24bit bus at '0x9237' and addressEx '0x01'

    Given write data byte '0x0c' to 24bit bus at '0x9238' and addressEx '0x01'
    Given write data byte '0x90' to 24bit bus at '0x9239' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x923a' and addressEx '0x01'
    Given write data byte '0xa0' to 24bit bus at '0x923b' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x923c' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x923d' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x923e' and addressEx '0x01'
    Given write data byte '0x33' to 24bit bus at '0x923f' and addressEx '0x01'

    # *2
    Given write data byte '0x0c' to 24bit bus at '0x9240' and addressEx '0x01'
    Given write data byte '0xa0' to 24bit bus at '0x9241' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x9242' and addressEx '0x01'
    Given write data byte '0xb0' to 24bit bus at '0x9243' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9244' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9245' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9246' and addressEx '0x01'
    Given write data byte '0x30' to 24bit bus at '0x9247' and addressEx '0x01'

    Given write data byte '0x0c' to 24bit bus at '0x9248' and addressEx '0x01'
    Given write data byte '0xa0' to 24bit bus at '0x9249' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x924a' and addressEx '0x01'
    Given write data byte '0xf0' to 24bit bus at '0x924b' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x924c' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x924d' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x924e' and addressEx '0x01'
    Given write data byte '0x31' to 24bit bus at '0x924f' and addressEx '0x01'

    Given write data byte '0x0c' to 24bit bus at '0x9250' and addressEx '0x01'
    Given write data byte '0xe0' to 24bit bus at '0x9251' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9252' and addressEx '0x01'
    Given write data byte '0xb0' to 24bit bus at '0x9253' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9254' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9255' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9256' and addressEx '0x01'
    Given write data byte '0x32' to 24bit bus at '0x9257' and addressEx '0x01'

    Given write data byte '0x0c' to 24bit bus at '0x9258' and addressEx '0x01'
    Given write data byte '0xe0' to 24bit bus at '0x9259' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x925a' and addressEx '0x01'
    Given write data byte '0xf0' to 24bit bus at '0x925b' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x925c' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x925d' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x925e' and addressEx '0x01'
    Given write data byte '0x33' to 24bit bus at '0x925f' and addressEx '0x01'

    # Will be end of list, but temporarily set this to be a valid but vertically very small sprite
    Given write data byte '0x01' to 24bit bus at '0x9262' and addressEx '0x01'
    # After the end of list, this large sprite should not be displayed
    Given write data byte '0x0c' to 24bit bus at '0x9268' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9269' and addressEx '0x01'
    Given write data byte '0xf0' to 24bit bus at '0x926a' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x926b' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x926c' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x926d' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x926e' and addressEx '0x01'
    Given write data byte '0x33' to 24bit bus at '0x926f' and addressEx '0x01'

    Given render a video display frame
#    When display until window closed

    # Now test the end of list
    Given write data byte '0x00' to 24bit bus at '0x9262' and addressEx '0x01'

    Given render a video display frame
    Given render 256 video display frames
#    When display until window closed

#    Then expect image "testdata/TC-10-000000.bmp" to be identical to "target/frames/TC-10-000000.bmp"
#    Then expect image "testdata/TC-10-000001.bmp" to be identical to "target/frames/TC-10-000001.bmp"



  @TC-17
  Scenario: Testing Sprites4 layer
    Given clear all external devices
    Given a new video display with overscan and 16 colours
    And the display uses exact address matching
    Given set the video display to RGB colour 5 6 5
    Given set the video display with 32 palette banks
    And enable video display bus debug output
    Given video display processes 24 pixels per instruction
    Given video display refresh window every 32 instructions
    Given video display saves debug BMP images to leaf filename "target/frames/TC-17-"
    Given video display add joystick to port 2
#    Given video display does not save debug BMP images
#    Given property "bdd6502.bus24.trace" is set to string "true"
    Given I have a simple overclocked 6502 system
    Given a user port to 24 bit bus is installed
    Given add a StaticColour layer for palette index '0x07'
    And the layer has 16 colours
    And the layer has overscan
    And the layer uses exact address matching
    Given add a Chars V4.0 layer with registers at '0x9000' and screen addressEx '0x80' and planes addressEx '0x20'
    And the layer has 16 colours
    And the layer has overscan
    And the layer uses exact address matching
    Given add a Sprites4 layer with registers at '0x8800' and addressEx '0x04' and running at 14.31818MHz
    And the layer has 16 colours
    And the layer has overscan
    And the layer uses exact address matching
    Given randomly initialise all memory using seed 1234
    Given show video window
    Given limit video display to 60 fps

    # Palette
    Given write data from file "C:\Work\ImageToBitplane\target\chars512_paletteData.bin" to 24bit bus at '0x9c00' and addressEx '0x01'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanScaledPaletteData4.bin" to 24bit bus at '0x9d60' and addressEx '0x01'
    Given write data from file "C:\Work\ImageToBitplane\target\testconcat4PaletteData4.bin" to 24bit bus at '0x9d80' and addressEx '0x01'


    # Sprites4 data
    # Turrican Scaled
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanScaledSprites4.bin" to 24bit bus at '0x0000' and addressEx '0x04'
    Given write data from file "C:\Work\ImageToBitplane\target\testconcat4ScaledSprites4.bin" to 24bit bus at '0x4000' and addressEx '0x04'

    # Chars
    # oldbridge char screen with rgbfactor 512
    Given write data from file "C:\Work\ImageToBitplane\target\chars1024_plane0.bin" to 24bit bus at '0x2000' and addressEx '0x20'
    Given write data from file "C:\Work\ImageToBitplane\target\chars1024_plane1.bin" to 24bit bus at '0x4000' and addressEx '0x20'
    Given write data from file "C:\Work\ImageToBitplane\target\chars1024_plane2.bin" to 24bit bus at '0x8000' and addressEx '0x20'
    Given write data from file "C:\Work\ImageToBitplane\target\chars1024_plane3.bin" to 24bit bus at '0x0000' and addressEx '0x20'
    # Chars screen
    Given write data from file "C:\Work\ImageToBitplane\target\chars1024_scr.bin" to 24bit bus at '0x4000' and addressEx '0x80'
    Given write data from file "C:\Work\ImageToBitplane\target\chars1024_scr.bin2" to 24bit bus at '0x8000' and addressEx '0x80'

    # Enable display
    Given write data byte '0x20' to 24bit bus at '0x9e00' and addressEx '0x01'
    # Default display priority
    Given write data byte '0xe4' to 24bit bus at '0x9e08' and addressEx '0x01'
    # Overscan control
    Given write data byte '0x29' to 24bit bus at '0x9e09' and addressEx '0x01'
    # Just enable Sprites4, not the chars
    Given write data byte '0x01' to 24bit bus at '0x9e0a' and addressEx '0x01'
    # All layers
#    Given write data byte '0x07' to 24bit bus at '0x9e0a' and addressEx '0x01'

    # Sprites4 registers
    # Zero flag
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    # Zero the X/Y border adjustments
    Given write data byte '0x00' to 24bit bus at '0x8801' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8802' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8803' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8804' and addressEx '0x01'
    # Extent X/Y values
    # Improves from 126 to 190, with Y extent
    Given write data byte '0xa8' to 24bit bus at '0x8805' and addressEx '0x01'
    # Improves from 111 to 126, without X extent
    Given write data byte '0x70' to 24bit bus at '0x8806' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8807' and addressEx '0x01'

    # Sprites support X and Y flips
    # Palette | 0x10 = MSBX | 0x20 = MSBY | 0x40 = flipX | 0x80 = flipY
    # Y pos
    # Y size (in screen pixels, regardless of scale)
    # X pos
    # X size (in screen pixels, regardless of scale)
    # Sprite address (24 bits)
    # Y inv scale (*32)
    # X inv scale (*32)
    # Sprite stride-1
    # Middle, right, crouching left, no scale
    Given write data byte '0x1b' to 24bit bus at '0x8808' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x8809' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x880a' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x880b' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x880c' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880d' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x880e' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880f' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x8810' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x8811' and addressEx '0x01'
    Given write data byte '0x3f' to 24bit bus at '0x8812' and addressEx '0x01'

    # Top left, 0,0, standing left, double size
    Given write data byte '0x0b' to 24bit bus at '0x8813' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8814' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x8815' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8816' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x8817' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8818' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8819' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x881a' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x881b' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x881c' and addressEx '0x01'
    Given write data byte '0x3f' to 24bit bus at '0x881d' and addressEx '0x01'

    # Middle middle, building top, double size, flip X and Y
    Given write data byte '0xcc' to 24bit bus at '0x881e' and addressEx '0x01'
    Given write data byte '0x50' to 24bit bus at '0x881f' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x8820' and addressEx '0x01'
    Given write data byte '0x88' to 24bit bus at '0x8821' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x8822' and addressEx '0x01'
    # Note the sprite address starts on the "right" and "bottom"
    Given write data byte '0xff' to 24bit bus at '0x8823' and addressEx '0x01'
    Given write data byte '0x8f' to 24bit bus at '0x8824' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8825' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x8826' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x8827' and addressEx '0x01'
    Given write data byte '0x3f' to 24bit bus at '0x8828' and addressEx '0x01'

    # Terminate the sprite list
    Given write data byte '0x00' to 24bit bus at '0x8829' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x882a' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x882b' and addressEx '0x01'

    # Signal flag ready
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'

    Given render a video display frame
#    When display until window closed

    Given render a video display frame
    Given render a video display frame

    Given write data byte '0xff' to 24bit bus at '0x8801' and addressEx '0x01'
    Given write data byte '0xff' to 24bit bus at '0x8802' and addressEx '0x01'
    Given render a video display frame
    Given render a video display frame
    Given write data byte '0xfe' to 24bit bus at '0x8801' and addressEx '0x01'
    Given write data byte '0xff' to 24bit bus at '0x8802' and addressEx '0x01'
    Given render a video display frame
    Given render a video display frame
    Given write data byte '0xfd' to 24bit bus at '0x8801' and addressEx '0x01'
    Given write data byte '0xff' to 24bit bus at '0x8802' and addressEx '0x01'
    Given render a video display frame
    Given render a video display frame
    Given write data byte '0xf0' to 24bit bus at '0x8801' and addressEx '0x01'
    Given write data byte '0xff' to 24bit bus at '0x8802' and addressEx '0x01'
    Given render a video display frame
    Given render a video display frame
    Given write data byte '0xf0' to 24bit bus at '0x8803' and addressEx '0x01'
    Given write data byte '0xff' to 24bit bus at '0x8804' and addressEx '0x01'
    Given render a video display frame
    Given render a video display frame
    Given render a video display frame
    # Pixel scroll test, note double buffered draw buffers
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    # Top left, 0,0, standing left, double size
    Given write data byte '0x0b' to 24bit bus at '0x8808' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8809' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x880a' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880b' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x880c' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880d' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880e' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880f' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x8810' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x8811' and addressEx '0x01'
    Given write data byte '0x3f' to 24bit bus at '0x8812' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8813' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8814' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8815' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'
    Given render a video display frame
    # Pixel scroll test, note double buffered draw buffers
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    # Top left, 0,0, standing left, double size
    Given write data byte '0x0b' to 24bit bus at '0x8808' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8809' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x880a' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880b' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x880c' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x880d' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880e' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880f' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x8810' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x8811' and addressEx '0x01'
    Given write data byte '0x3f' to 24bit bus at '0x8812' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8813' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8814' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8815' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'
    Given render a video display frame
    # Pixel scroll test, note double buffered draw buffers
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    # Top left, 0,0, standing left, double size
    Given write data byte '0x0b' to 24bit bus at '0x8808' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8809' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x880a' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880b' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x880c' and addressEx '0x01'
    Given write data byte '0x02' to 24bit bus at '0x880d' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880e' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880f' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x8810' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x8811' and addressEx '0x01'
    Given write data byte '0x3f' to 24bit bus at '0x8812' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8813' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8814' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8815' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'
    Given render a video display frame
    # Pixel scroll test, note double buffered draw buffers
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    # Top left, 0,0, standing left, double size
    Given write data byte '0x0b' to 24bit bus at '0x8808' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8809' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x880a' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880b' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x880c' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x880d' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880e' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880f' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x8810' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x8811' and addressEx '0x01'
    Given write data byte '0x3f' to 24bit bus at '0x8812' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8813' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8814' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8815' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'
    Given render a video display frame
    # Pixel scroll test, note double buffered draw buffers
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    # Top left, 0,0, standing left, double size
    Given write data byte '0x0b' to 24bit bus at '0x8808' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8809' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x880a' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880b' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x880c' and addressEx '0x01'
    Given write data byte '0x04' to 24bit bus at '0x880d' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880e' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880f' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x8810' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x8811' and addressEx '0x01'
    Given write data byte '0x3f' to 24bit bus at '0x8812' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8813' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8814' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8815' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'
    Given render a video display frame
    # Pixel scroll test, note double buffered draw buffers
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    # Top left, 0,0, standing left, double size
    Given write data byte '0x0b' to 24bit bus at '0x8808' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8809' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x880a' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880b' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x880c' and addressEx '0x01'
    Given write data byte '0x05' to 24bit bus at '0x880d' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880e' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880f' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x8810' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x8811' and addressEx '0x01'
    Given write data byte '0x3f' to 24bit bus at '0x8812' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8813' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8814' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8815' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'
    Given render a video display frame



    Given render a video display frame
    Given render a video display frame
    Given render a video display frame
    Given render a video display frame

    And I run the command line: ..\C64\acme.exe -v3 --lib ../ -o test.prg --labeldump test.lbl -f cbm "features/Test Sprites4.a"
#    And I run the command line: ..\C64\bin\LZMPi.exe -c64mbe test.prg testcmp.prg $200
    And I load prg "test.prg"
    And I load labels "test.lbl"
#    And I enable trace with indent

    When enable remote debugging
#    And wait for debugger connection

#    When I execute the procedure at start for no more than 99999999 instructions
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'

    When I execute the procedure at start until return

    # Signal flag ready
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'
    Given render a video display frame
    Given render a video display frame
    Given render a video display frame

    Given video display does not save debug BMP images
    When I execute the procedure at frames until return

      #    Given render 256 video display frames
#    When display until window closed

    Then expect image "testdata/TC-17-000002.bmp" to be identical to "target/frames/TC-17-000002.bmp"
    Then expect image "testdata/TC-17-000003.bmp" to be identical to "target/frames/TC-17-000003.bmp"
    Then expect image "testdata/TC-17-000004.bmp" to be identical to "target/frames/TC-17-000004.bmp"
    Then expect image "testdata/TC-17-000005.bmp" to be identical to "target/frames/TC-17-000005.bmp"
    Then expect image "testdata/TC-17-000006.bmp" to be identical to "target/frames/TC-17-000006.bmp"
    Then expect image "testdata/TC-17-000007.bmp" to be identical to "target/frames/TC-17-000007.bmp"
    Then expect image "testdata/TC-17-000008.bmp" to be identical to "target/frames/TC-17-000008.bmp"
    Then expect image "testdata/TC-17-000009.bmp" to be identical to "target/frames/TC-17-000009.bmp"
    Then expect image "testdata/TC-17-000010.bmp" to be identical to "target/frames/TC-17-000010.bmp"
    Then expect image "testdata/TC-17-000011.bmp" to be identical to "target/frames/TC-17-000011.bmp"
    Then expect image "testdata/TC-17-000012.bmp" to be identical to "target/frames/TC-17-000012.bmp"
    Then expect image "testdata/TC-17-000013.bmp" to be identical to "target/frames/TC-17-000013.bmp"
    Then expect image "testdata/TC-17-000014.bmp" to be identical to "target/frames/TC-17-000014.bmp"
    Then expect image "testdata/TC-17-000015.bmp" to be identical to "target/frames/TC-17-000015.bmp"
    Then expect image "testdata/TC-17-000016.bmp" to be identical to "target/frames/TC-17-000016.bmp"
    Then expect image "testdata/TC-17-000017.bmp" to be identical to "target/frames/TC-17-000017.bmp"
    Then expect image "testdata/TC-17-000018.bmp" to be identical to "target/frames/TC-17-000018.bmp"
    Then expect image "testdata/TC-17-000019.bmp" to be identical to "target/frames/TC-17-000019.bmp"
    Then expect image "testdata/TC-17-000020.bmp" to be identical to "target/frames/TC-17-000020.bmp"
    Then expect image "testdata/TC-17-000021.bmp" to be identical to "target/frames/TC-17-000021.bmp"
    Then expect image "testdata/TC-17-000027.bmp" to be identical to "target/frames/TC-17-000027.bmp"




  @TC-17-2
  Scenario: Testing Sprites4 layer - 2
    Given clear all external devices
    Given a new video display with overscan and 16 colours
    And the display uses exact address matching
    Given set the video display to RGB colour 5 6 5
    Given set the video display with 32 palette banks
    And enable video display bus debug output
    Given video display processes 24 pixels per instruction
    Given video display refresh window every 32 instructions
    Given video display saves debug BMP images to leaf filename "target/frames/TC-17-2-"
    Given video display add joystick to port 2
#    Given video display does not save debug BMP images
    Given property "bdd6502.bus24.trace" is set to string "true"
    Given I have a simple overclocked 6502 system
    Given a user port to 24 bit bus is installed
    # Layer 0
    Given add a 2-to-1 merge layer with registers at '0xa200'
    And the layer has 16 colours
    And the layer has overscan
      # Layer 0-1
#      Given add a Sprites4 layer with registers at '0x8800' and addressEx '0x04' and running at 14.31818MHz
    Given add a Sprites4 layer with registers at '0x8800' and addressEx '0x04' and running at 12.096MHz
    And the layer has 16 colours
    And the layer has overscan
    And the layer uses exact address matching
      # Layer 0-0
    Given add a Sprites V9.5 layer with registers at '0x9800' and addressEx '0x10' and running at 16MHz
    And the layer has 16 colours
    And the layer has overscan
    And the layer uses exact address matching
    Given randomly initialise all memory using seed 1234
    Given show video window
    Given limit video display to 60 fps

    # Palette
    Given write data from file "C:\temp\t.pal" to 24bit bus at '0x9c00' and addressEx '0x01'


    Given write data from file "C:\temp\TScaledSprites4.bin" to 24bit bus at '0x0000' and addressEx '0x04'

    # Chars
    # oldbridge char screen with rgbfactor 512
    Given write data from file "C:\Work\ImageToBitplane\target\chars1024_plane0.bin" to 24bit bus at '0x2000' and addressEx '0x20'
    Given write data from file "C:\Work\ImageToBitplane\target\chars1024_plane1.bin" to 24bit bus at '0x4000' and addressEx '0x20'
    Given write data from file "C:\Work\ImageToBitplane\target\chars1024_plane2.bin" to 24bit bus at '0x8000' and addressEx '0x20'
    Given write data from file "C:\Work\ImageToBitplane\target\chars1024_plane3.bin" to 24bit bus at '0x0000' and addressEx '0x20'
    # Chars screen
    Given write data from file "C:\Work\ImageToBitplane\target\chars1024_scr.bin" to 24bit bus at '0x4000' and addressEx '0x80'
    Given write data from file "C:\Work\ImageToBitplane\target\chars1024_scr.bin2" to 24bit bus at '0x8000' and addressEx '0x80'

    # Enable display
    Given write data byte '0x30' to 24bit bus at '0x9e00' and addressEx '0x01'
    # Default display priority
    Given write data byte '0xe4' to 24bit bus at '0x9e08' and addressEx '0x01'
    # Overscan control
#    Given write data byte '0x29' to 24bit bus at '0x9e09' and addressEx '0x01'
    Given write data byte '0x0f' to 24bit bus at '0x9e09' and addressEx '0x01'
    # Just enable Sprites4, not the chars
    Given write data byte '0x01' to 24bit bus at '0x9e0a' and addressEx '0x01'
    # Background colour
    Given write data byte '0x07' to 24bit bus at '0x9e0b' and addressEx '0x01'

    # Init combiners
    Given write data byte '0x60' to 24bit bus at '0xa200' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0xa201' and addressEx '0x01'
    Given write data byte '0x60' to 24bit bus at '0xa202' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0xa203' and addressEx '0x01'


    # All layers
#    Given write data byte '0x07' to 24bit bus at '0x9e0a' and addressEx '0x01'

    # Sprites4 registers
    # Zero flag
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    # Zero the X/Y border adjustments
    Given write data byte '0x00' to 24bit bus at '0x8801' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8802' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8803' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8804' and addressEx '0x01'
    # Extent X/Y values
    Given write data byte '0xa8' to 24bit bus at '0x8805' and addressEx '0x01'
    Given write data byte '0x70' to 24bit bus at '0x8806' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8807' and addressEx '0x01'

    # Sprites support X and Y flips
    # Palette | 0x10 = MSBX | 0x20 = MSBY | 0x40 = flipX | 0x80 = flipY
    # Y pos
    # Y size (in screen pixels, regardless of scale)
    # X pos
    # X size (in screen pixels, regardless of scale)
    # Sprite address (24 bits)
    # Y inv scale (*32)
    # X inv scale (*32)
    # Sprite stride-1
    # Middle, right, crouching left, no scale
    Given write data byte '0x00' to 24bit bus at '0x8808' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x8809' and addressEx '0x01'
    Given write data byte '30' to 24bit bus at '0x880a' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x880b' and addressEx '0x01'
    Given write data byte '116' to 24bit bus at '0x880c' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880d' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880e' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880f' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x8810' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x8811' and addressEx '0x01'
    Given write data byte '115' to 24bit bus at '0x8812' and addressEx '0x01'

    Given write data byte '0x00' to 24bit bus at '0x8813' and addressEx '0x01'
    Given write data byte '0x60' to 24bit bus at '0x8814' and addressEx '0x01'
    Given write data byte '39' to 24bit bus at '0x8815' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x8816' and addressEx '0x01'
    Given write data byte '116' to 24bit bus at '0x8817' and addressEx '0x01'
    Given write data byte '0x98' to 24bit bus at '0x8818' and addressEx '0x01'
    Given write data byte '0x0d' to 24bit bus at '0x8819' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x881a' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x881b' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x881c' and addressEx '0x01'
    Given write data byte '115' to 24bit bus at '0x881d' and addressEx '0x01'

    Given write data byte '0x01' to 24bit bus at '0x881e' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x881f' and addressEx '0x01'
    Given write data byte '102' to 24bit bus at '0x8820' and addressEx '0x01'
    Given write data byte '0xa0' to 24bit bus at '0x8821' and addressEx '0x01'
    Given write data byte '105' to 24bit bus at '0x8822' and addressEx '0x01'
    Given write data byte '0x44' to 24bit bus at '0x8823' and addressEx '0x01'
    Given write data byte '0x1f' to 24bit bus at '0x8824' and addressEx '0x01'
    Given write data byte '0x0' to 24bit bus at '0x8825' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x8826' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x8827' and addressEx '0x01'
    Given write data byte '104' to 24bit bus at '0x8828' and addressEx '0x01'

    # Terminate the sprite list
    Given write data byte '0x00' to 24bit bus at '0x8829' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x882a' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x882b' and addressEx '0x01'

    # Signal flag ready
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'

    Given render a video display frame
    Given render a video display frame
    Given render a video display frame
    When display until window closed

    Then expect image "testdata/TC-17-2-000002.bmp" to be identical to "target/frames/TC-17-2-000002.bmp"


  @TC-17-3
  Scenario: Testing Sprites4 layer - 3 - Specific scaled data and extents
    Given clear all external devices
    Given a new video display with overscan and 16 colours
    And the display uses exact address matching
    Given set the video display to RGB colour 5 6 5
    Given set the video display with 32 palette banks
    And enable video display bus debug output
    Given video display processes 24 pixels per instruction
    Given video display refresh window every 32 instructions
    Given video display saves debug BMP images to leaf filename "target/frames/TC-17-3-"
    Given video display add joystick to port 2
#    Given video display does not save debug BMP images
#    Given property "bdd6502.bus24.trace" is set to string "true"
    Given I have a simple overclocked 6502 system
    Given a user port to 24 bit bus is installed
    # Layer 0
    Given add a 2-to-1 merge layer with registers at '0xa200'
    And the layer has 16 colours
    And the layer has overscan
      # Layer 0-1
#      Given add a Sprites4 layer with registers at '0x8800' and addressEx '0x04' and running at 14.31818MHz
    Given add a Sprites4 layer with registers at '0x8800' and addressEx '0x04' and running at 12.096MHz
    And the layer has 16 colours
    And the layer has overscan
    And the layer uses exact address matching
      # Layer 0-0
    Given add a Sprites V9.5 layer with registers at '0x9800' and addressEx '0x10' and running at 16MHz
    And the layer has 16 colours
    And the layer has overscan
    And the layer uses exact address matching
    Given show video window
    Given limit video display to 60 fps

    # Palette
    # Mid grey
    Given write data byte '0xe7' to 24bit bus at '0x9c00' and addressEx '0x01'
    Given write data byte '0x38' to 24bit bus at '0x9c01' and addressEx '0x01'
    # Bright red
    Given write data byte '0x1f' to 24bit bus at '0x9c02' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9c03' and addressEx '0x01'
    # Bright green
    Given write data byte '0xe0' to 24bit bus at '0x9c04' and addressEx '0x01'
    Given write data byte '0x07' to 24bit bus at '0x9c05' and addressEx '0x01'
    # Bright blue
    Given write data byte '0x00' to 24bit bus at '0x9c06' and addressEx '0x01'
    Given write data byte '0xf8' to 24bit bus at '0x9c07' and addressEx '0x01'
    # White
    Given write data byte '0xff' to 24bit bus at '0x9c08' and addressEx '0x01'
    Given write data byte '0xff' to 24bit bus at '0x9c09' and addressEx '0x01'
    # Bright greeny yellow
    Given write data byte '0x77' to 24bit bus at '0x9c0a' and addressEx '0x01'
    Given write data byte '0x77' to 24bit bus at '0x9c0b' and addressEx '0x01'
    # Bright darker greeny yellow
    Given write data byte '0x55' to 24bit bus at '0x9c0c' and addressEx '0x01'
    Given write data byte '0x55' to 24bit bus at '0x9c0d' and addressEx '0x01'

    # Sprites4 Specific pattern to aid debugging of scale up and down behaviour
    Given start writing data to 24bit bus at '0x0000' and addressEx '0x04'
    # Guard data
    Given write data '0x55 0x55 0x55 0x55 0x55 0x55 0x55 0x55' to 24bit bus
    # Real data
    Given write data '0x21 0x43 0x23 0x01' to 24bit bus
    Given write data '0x10 0x32 0x21 0x43' to 24bit bus
    # Guard data
    Given write data '0x66 0x66 0x66 0x66 0x66 0x66 0x66 0x66' to 24bit bus

    # Enable display
    Given write data byte '0x30' to 24bit bus at '0x9e00' and addressEx '0x01'
    # Default display priority
    Given write data byte '0xe4' to 24bit bus at '0x9e08' and addressEx '0x01'
    # Overscan control
#    Given write data byte '0x29' to 24bit bus at '0x9e09' and addressEx '0x01'
    Given write data byte '0x0f' to 24bit bus at '0x9e09' and addressEx '0x01'
    # Just enable Sprites4
    Given write data byte '0x01' to 24bit bus at '0x9e0a' and addressEx '0x01'
    # Background colour
    Given write data byte '0x00' to 24bit bus at '0x9e0b' and addressEx '0x01'

    # Init combiners
    Given write data byte '0x60' to 24bit bus at '0xa200' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0xa201' and addressEx '0x01'
    Given write data byte '0x60' to 24bit bus at '0xa202' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0xa203' and addressEx '0x01'

    # Sprites4 registers
    # Zero flag
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    # Zero the X/Y border adjustments
    Given write data byte '0xf0' to 24bit bus at '0x8801' and addressEx '0x01'
    Given write data byte '0xff' to 24bit bus at '0x8802' and addressEx '0x01'
    Given write data byte '0xf8' to 24bit bus at '0x8803' and addressEx '0x01'
    Given write data byte '0xff' to 24bit bus at '0x8804' and addressEx '0x01'
    # Extent X/Y values
    Given write data byte '0xa8' to 24bit bus at '0x8805' and addressEx '0x01'
    Given write data byte '0x70' to 24bit bus at '0x8806' and addressEx '0x01'
    # Memory bank for sprite RAM load
    Given write data byte '0x00' to 24bit bus at '0x8807' and addressEx '0x01'

    # Sprites support X and Y flips
    # Palette | 0x10 = MSBX | 0x20 = MSBY | 0x40 = flipX | 0x80 = flipY
    # Y pos
    # Y size (in screen pixels, regardless of scale)
    # X pos
    # X size (in screen pixels, regardless of scale)
    # Sprite address (24 bits)
    # Y inv scale (*32)
    # X inv scale (*32)
    # Sprite stride-1
    Given start writing data to 24bit bus at '0x8808' and addressEx '0x01'
    Given write data '0x00 0x10 16 0x10 16 0x10 0x00 0x00 0x08 0x08 3' to 24bit bus
    Given write data '0x00 0x40 4 0x40 4 0x10 0x00 0x00 0x20 0x20 3' to 24bit bus
    Given write data '0x00 0x80 2 0x80 2 0x10 0x00 0x00 0x40 0x40 3' to 24bit bus
    # Testing width limits
    # The minimum width is 2 pixels
    Given write data '0x00 0x90 1 0x08 0x02 0x10 0x00 0x00 0x00 0x00 3' to 24bit bus
    # This actually gives 256 pixels width
    Given write data '0x00 0x92 1 0x08 0x00 0x10 0x00 0x00 0x00 0x00 3' to 24bit bus
    # This actually gives 257 pixels width
    Given write data '0x00 0x94 1 0x08 0x01 0x11 0x00 0x00 0x00 0x00 3' to 24bit bus
    # 254 pixels width, as expected
    Given write data '0x00 0x96 1 0x08 0xfe 0x12 0x00 0x00 0x00 0x00 3' to 24bit bus
    # 255 pixels width, as expected
    Given write data '0x00 0x98 1 0x08 0xff 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus

    # Terminate the sprite list
    Given write data '0x00 0x00 0x00' to 24bit bus

    # Signal flag ready
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'

    Given enable debug pixel picking
    Given render a video display frame

    Given write data byte '0x29' to 24bit bus at '0x9e09' and addressEx '0x01'
    Given render a video display frame
    Given render a video display frame

    # Fill rate test. Moving sprites off the edges of the screen
    Given start writing data to 24bit bus at '0x8808' and addressEx '0x01'
    Given write data '0x00 0x08 20 0x10 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x0c 200 0x10 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x10 200 0x14 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    # Will partially render
    Given write data '0x00 0x14 200 0x18 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x18 200 0x1c 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0xc8 20 0x0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    # Terminate the sprite list
    Given write data '0x00 0x00 0x00' to 24bit bus
    # Signal flag ready
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'
    Given render a video display frame

    Given start writing data to 24bit bus at '0x8808' and addressEx '0x01'
    Given write data '0x00 0x08 20 0x10 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    # Moving this sprite down, should cause the later sprite to render more lines
    Given write data '0x00 0x8c 200 0x10 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x10 200 0x14 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    # Will fully render
    Given write data '0x00 0x14 200 0x18 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    # Will partially render
    Given write data '0x00 0x18 200 0x1c 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0xc8 20 0x0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    # Terminate the sprite list
    Given write data '0x00 0x00 0x00' to 24bit bus
    # Signal flag ready
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'
    Given render a video display frame

    Given start writing data to 24bit bus at '0x8808' and addressEx '0x01'
    # Moving this sprite off the left edge (or far right) causes the later sprite to render more
    Given write data '0x10 0x08 20 0x80 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x8c 200 0x10 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x10 200 0x14 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x14 200 0x18 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    # Will partially render, but slightly more
    Given write data '0x00 0x18 200 0x1c 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0xc8 20 0x0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    # Terminate the sprite list
    Given write data '0x00 0x00 0x00' to 24bit bus
    # Signal flag ready
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'
    Given render a video display frame

    Given start writing data to 24bit bus at '0x8808' and addressEx '0x01'
    # Moving this sprite off the top edge (or far bottom) causes the later sprite to render more
    Given write data '0x20 0x88 20 0x10 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x8c 200 0x10 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x10 200 0x14 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x14 200 0x18 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    # Will partially render, but slightly more as above
    Given write data '0x00 0x18 200 0x1c 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0xc8 20 0x0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    # Terminate the sprite list
    Given write data '0x00 0x00 0x00' to 24bit bus
    # Signal flag ready
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'
    Given render a video display frame

    Given start writing data to 24bit bus at '0x8808' and addressEx '0x01'
    Given write data '0x00 0x08 20 0x10 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    # Moving this sprite almost off the bottom, causes a later sprite to render more
    Given write data '0x00 0xcc 200 0x10 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x10 200 0x14 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x14 200 0x18 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    # Will partially render, but much more
    Given write data '0x00 0x18 200 0x1c 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0xc8 20 0x0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    # Terminate the sprite list
    Given write data '0x00 0x00 0x00' to 24bit bus
    # Signal flag ready
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'
    Given render a video display frame

    Given start writing data to 24bit bus at '0x8808' and addressEx '0x01'
    Given write data '0x00 0x08 20 0x10 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0xcc 200 0x10 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    # Moving this sprite half off the right, causes a later sprite to render more
    Given write data '0x00 0x10 200 0xa4 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x14 200 0x18 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    # Will partially render, but much more
    Given write data '0x00 0x18 200 0x1c 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0xc8 20 0x0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    # Terminate the sprite list
    Given write data '0x00 0x00 0x00' to 24bit bus
    # Signal flag ready
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'
    Given render a video display frame

    Given start writing data to 24bit bus at '0x8808' and addressEx '0x01'
    Given write data '0x00 0x08 20 0x10 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0xcc 200 0x10 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    # Moving this sprite almost off the right, causes a later sprite to render more
    Given write data '0x10 0x10 200 0x34 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x14 200 0x18 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    # Will fully render
    Given write data '0x00 0x18 200 0x1c 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    # Will fully render
    Given write data '0x00 0xc8 20 0x0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    # Test any remaining draw time
    Given write data '0x00 0x0c 1 0x08 0xff 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    # Terminate the sprite list
    Given write data '0x00 0x00 0x00' to 24bit bus
    # Signal flag ready
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'
    Given render a video display frame
    Given render a video display frame

    # Extent X/Y values checks
    Given write data byte '0xa8' to 24bit bus at '0x8805' and addressEx '0x01'
    Given write data byte '0x50' to 24bit bus at '0x8806' and addressEx '0x01'
    Given start writing data to 24bit bus at '0x8808' and addressEx '0x01'
    Given write data '0x00 0x08 20 0x10 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x30 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x50 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x70 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x90 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0xb0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0xd0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0xcc 200 0x10 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x10 0x10 200 0x34 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x14 200 0x18 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x18 200 0x1c 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0xc8 20 0x0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x0c 1 0x08 0xff 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    # Checks that the logic for handling 256 and 257 pixel wide sprites works with clipping
    Given write data '0x00 0x0e 1 0x48 0x00 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    Given write data '0x00 0x0f 1 0x48 0x01 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    # Terminate the sprite list
    Given write data '0x00 0x00 0x00' to 24bit bus
    # Signal flag ready
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'
    Given render a video display frame
    Given render a video display frame
    Given render a video display frame

    # Extent X/Y values checks
    Given write data byte '0x90' to 24bit bus at '0x8805' and addressEx '0x01'
    Given write data byte '0x50' to 24bit bus at '0x8806' and addressEx '0x01'
    Given start writing data to 24bit bus at '0x8808' and addressEx '0x01'
    Given write data '0x00 0x08 20 0x10 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x30 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x50 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x70 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x90 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0xb0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0xd0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0xcc 200 0x10 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x10 0x10 200 0x34 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x14 200 0x18 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x18 200 0x1c 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0xc8 20 0x0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x0c 1 0x08 0xff 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    Given write data '0x00 0x0e 1 0x48 0x00 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    Given write data '0x00 0x0f 1 0x48 0x01 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    # Terminate the sprite list
    Given write data '0x00 0x00 0x00' to 24bit bus
    # Signal flag ready
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'
    Given render a video display frame
    Given render a video display frame
    Given render a video display frame

    # Extent X/Y values checks
    Given write data byte '0x80' to 24bit bus at '0x8805' and addressEx '0x01'
    Given write data byte '0x50' to 24bit bus at '0x8806' and addressEx '0x01'
    Given start writing data to 24bit bus at '0x8808' and addressEx '0x01'
    Given write data '0x00 0x08 20 0x10 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x30 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x50 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x70 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x90 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0xb0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0xd0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0xcc 200 0x10 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x10 0x10 200 0x34 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x14 200 0x18 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x18 200 0x1c 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0xc8 20 0x0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x0c 1 0x08 0xff 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    Given write data '0x00 0x0e 1 0x48 0x00 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    Given write data '0x00 0x0f 1 0x48 0x01 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    # Terminate the sprite list
    Given write data '0x00 0x00 0x00' to 24bit bus
    # Signal flag ready
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'
    Given render a video display frame
    Given render a video display frame
    Given render a video display frame

    # Extent X/Y values checks
    Given write data byte '0x70' to 24bit bus at '0x8805' and addressEx '0x01'
    Given write data byte '0x50' to 24bit bus at '0x8806' and addressEx '0x01'
    Given start writing data to 24bit bus at '0x8808' and addressEx '0x01'
    Given write data '0x00 0x08 20 0x10 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x30 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x50 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x70 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x90 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0xb0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0xd0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0xcc 200 0x10 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x10 0x10 200 0x34 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x14 200 0x18 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x18 200 0x1c 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0xc8 20 0x0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x0c 1 0x08 0xff 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    Given write data '0x00 0x0e 1 0x48 0x00 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    Given write data '0x00 0x0f 1 0x48 0x01 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    # Terminate the sprite list
    Given write data '0x00 0x00 0x00' to 24bit bus
    # Signal flag ready
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'
    Given render a video display frame
    Given render a video display frame
    Given render a video display frame

    # Extent X/Y values checks
    Given write data byte '0x60' to 24bit bus at '0x8805' and addressEx '0x01'
    Given write data byte '0x50' to 24bit bus at '0x8806' and addressEx '0x01'
    Given start writing data to 24bit bus at '0x8808' and addressEx '0x01'
    Given write data '0x00 0x08 20 0x10 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x30 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x50 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x70 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x90 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0xb0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0xd0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0xcc 200 0x10 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x10 0x10 200 0x34 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x14 200 0x18 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x18 200 0x1c 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0xc8 20 0x0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x0c 1 0x08 0xff 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    Given write data '0x00 0x0e 1 0x48 0x00 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    Given write data '0x00 0x0f 1 0x48 0x01 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    # Terminate the sprite list
    Given write data '0x00 0x00 0x00' to 24bit bus
    # Signal flag ready
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'
    Given render a video display frame
    Given render a video display frame
    Given render a video display frame

    # Extent X/Y values checks
    Given write data byte '0x50' to 24bit bus at '0x8805' and addressEx '0x01'
    Given write data byte '0x50' to 24bit bus at '0x8806' and addressEx '0x01'
    Given start writing data to 24bit bus at '0x8808' and addressEx '0x01'
    Given write data '0x00 0x08 20 0x10 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x30 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x50 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x70 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x90 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0xb0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0xd0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0xcc 200 0x10 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x10 0x10 200 0x34 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x14 200 0x18 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x18 200 0x1c 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0xc8 20 0x0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x0c 1 0x08 0xff 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    Given write data '0x00 0x0e 1 0x48 0x00 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    Given write data '0x00 0x0f 1 0x48 0x01 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    # Terminate the sprite list
    Given write data '0x00 0x00 0x00' to 24bit bus
    # Signal flag ready
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'
    Given render a video display frame
    Given render a video display frame
    Given render a video display frame

    # Extent X/Y values checks
    Given write data byte '0x40' to 24bit bus at '0x8805' and addressEx '0x01'
    Given write data byte '0x50' to 24bit bus at '0x8806' and addressEx '0x01'
    Given start writing data to 24bit bus at '0x8808' and addressEx '0x01'
    Given write data '0x00 0x08 20 0x10 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x30 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x50 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x70 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x90 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0xb0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0xd0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0xcc 200 0x10 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x10 0x10 200 0x34 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x14 200 0x18 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x18 200 0x1c 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0xc8 20 0x0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x0c 1 0x08 0xff 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    Given write data '0x00 0x0e 1 0x48 0x00 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    Given write data '0x00 0x0f 1 0x48 0x01 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    # Terminate the sprite list
    Given write data '0x00 0x00 0x00' to 24bit bus
    # Signal flag ready
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'
    Given render a video display frame
    Given render a video display frame
    Given render a video display frame

    # Extent X/Y values checks
    Given write data byte '0x30' to 24bit bus at '0x8805' and addressEx '0x01'
    Given write data byte '0x50' to 24bit bus at '0x8806' and addressEx '0x01'
    Given start writing data to 24bit bus at '0x8808' and addressEx '0x01'
    Given write data '0x00 0x08 20 0x10 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x30 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x50 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x70 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x90 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0xb0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0xd0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0xcc 200 0x10 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x10 0x10 200 0x34 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x14 200 0x18 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x18 200 0x1c 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0xc8 20 0x0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x0c 1 0x08 0xff 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    Given write data '0x00 0x0e 1 0x48 0x00 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    Given write data '0x00 0x0f 1 0x48 0x01 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    # Terminate the sprite list
    Given write data '0x00 0x00 0x00' to 24bit bus
    # Signal flag ready
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'
    Given render a video display frame
    Given render a video display frame
    Given render a video display frame

    # Extent X/Y values checks
    Given write data byte '0x20' to 24bit bus at '0x8805' and addressEx '0x01'
    Given write data byte '0x50' to 24bit bus at '0x8806' and addressEx '0x01'
    Given start writing data to 24bit bus at '0x8808' and addressEx '0x01'
    Given write data '0x00 0x08 20 0x10 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x30 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x50 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x70 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x90 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0xb0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0xd0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0xcc 200 0x10 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x10 0x10 200 0x34 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x14 200 0x18 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x18 200 0x1c 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0xc8 20 0x0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x0c 1 0x08 0xff 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    Given write data '0x00 0x0e 1 0x48 0x00 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    Given write data '0x00 0x0f 1 0x48 0x01 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    # Terminate the sprite list
    Given write data '0x00 0x00 0x00' to 24bit bus
    # Signal flag ready
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'
    Given render a video display frame
    Given render a video display frame
    Given render a video display frame

    # Extent X/Y values checks
    Given write data byte '0x10' to 24bit bus at '0x8805' and addressEx '0x01'
    Given write data byte '0x50' to 24bit bus at '0x8806' and addressEx '0x01'
    Given start writing data to 24bit bus at '0x8808' and addressEx '0x01'
    Given write data '0x00 0x08 20 0x10 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x30 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x50 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x70 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0x90 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0xb0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x08 20 0xd0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0xcc 200 0x10 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x10 0x10 200 0x34 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x14 200 0x18 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0x18 200 0x1c 200 0x08 0x00 0x00 0x01 0x01 1' to 24bit bus
    Given write data '0x00 0xc8 20 0x0 20 0x08 0x00 0x00 0x10 0x10 1' to 24bit bus
    Given write data '0x00 0x0c 1 0x08 0xff 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    Given write data '0x00 0x0e 1 0x48 0x00 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    Given write data '0x00 0x0f 1 0x48 0x01 0x13 0x00 0x00 0x00 0x00 3' to 24bit bus
    # Terminate the sprite list
    Given write data '0x00 0x00 0x00' to 24bit bus
    # Signal flag ready
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'
    Given render a video display frame
    Given render a video display frame
    Given render a video display frame

    Given render a video display frame
    Given render a video display frame

#    When display until window closed

    Then expect image "testdata/TC-17-3-000003.bmp" to be identical to "target/frames/TC-17-3-000003.bmp"
    Then expect image "testdata/TC-17-3-000005.bmp" to be identical to "target/frames/TC-17-3-000005.bmp"
    Then expect image "testdata/TC-17-3-000006.bmp" to be identical to "target/frames/TC-17-3-000006.bmp"
    Then expect image "testdata/TC-17-3-000007.bmp" to be identical to "target/frames/TC-17-3-000007.bmp"
    Then expect image "testdata/TC-17-3-000008.bmp" to be identical to "target/frames/TC-17-3-000008.bmp"
    Then expect image "testdata/TC-17-3-000009.bmp" to be identical to "target/frames/TC-17-3-000009.bmp"
    Then expect image "testdata/TC-17-3-000010.bmp" to be identical to "target/frames/TC-17-3-000010.bmp"
    Then expect image "testdata/TC-17-3-000011.bmp" to be identical to "target/frames/TC-17-3-000011.bmp"

    Then expect image "testdata/TC-17-3-000013.bmp" to be identical to "target/frames/TC-17-3-000013.bmp"
    Then expect image "testdata/TC-17-3-000016.bmp" to be identical to "target/frames/TC-17-3-000016.bmp"
    Then expect image "testdata/TC-17-3-000019.bmp" to be identical to "target/frames/TC-17-3-000019.bmp"
    Then expect image "testdata/TC-17-3-000022.bmp" to be identical to "target/frames/TC-17-3-000022.bmp"
    Then expect image "testdata/TC-17-3-000025.bmp" to be identical to "target/frames/TC-17-3-000025.bmp"
    Then expect image "testdata/TC-17-3-000028.bmp" to be identical to "target/frames/TC-17-3-000028.bmp"
    Then expect image "testdata/TC-17-3-000031.bmp" to be identical to "target/frames/TC-17-3-000031.bmp"
    Then expect image "testdata/TC-17-3-000034.bmp" to be identical to "target/frames/TC-17-3-000034.bmp"
    Then expect image "testdata/TC-17-3-000037.bmp" to be identical to "target/frames/TC-17-3-000037.bmp"
    Then expect image "testdata/TC-17-3-000040.bmp" to be identical to "target/frames/TC-17-3-000040.bmp"



  @TC-17-4
  Scenario: Testing Sprites4 layer - 4 - Flips
    Given clear all external devices
    Given a new video display with overscan and 16 colours
    And the display uses exact address matching
    Given set the video display to RGB colour 5 6 5
    Given set the video display with 32 palette banks
    And enable video display bus debug output
    Given video display processes 24 pixels per instruction
    Given video display refresh window every 32 instructions
    Given video display saves debug BMP images to leaf filename "target/frames/TC-17-4-"
    Given video display add joystick to port 2
#    Given video display does not save debug BMP images
#    Given property "bdd6502.bus24.trace" is set to string "true"
    Given I have a simple overclocked 6502 system
    Given a user port to 24 bit bus is installed
    # Layer 0
    Given add a 2-to-1 merge layer with registers at '0xa200'
    And the layer has 16 colours
    And the layer has overscan
      # Layer 0-1
#      Given add a Sprites4 layer with registers at '0x8800' and addressEx '0x04' and running at 14.31818MHz
    Given add a Sprites4 layer with registers at '0x8800' and addressEx '0x04' and running at 12.096MHz
    And the layer has 16 colours
    And the layer has overscan
    And the layer uses exact address matching
      # Layer 0-0
    Given add a Sprites V9.5 layer with registers at '0x9800' and addressEx '0x10' and running at 16MHz
    And the layer has 16 colours
    And the layer has overscan
    And the layer uses exact address matching
    Given show video window
    Given limit video display to 60 fps

    # Palette
    # Mid grey
    Given write data byte '0xe7' to 24bit bus at '0x9c00' and addressEx '0x01'
    Given write data byte '0x38' to 24bit bus at '0x9c01' and addressEx '0x01'
    # Bright red
    Given write data byte '0x1f' to 24bit bus at '0x9c02' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9c03' and addressEx '0x01'
    # Bright green
    Given write data byte '0xe0' to 24bit bus at '0x9c04' and addressEx '0x01'
    Given write data byte '0x07' to 24bit bus at '0x9c05' and addressEx '0x01'
    # Bright blue
    Given write data byte '0x00' to 24bit bus at '0x9c06' and addressEx '0x01'
    Given write data byte '0xf8' to 24bit bus at '0x9c07' and addressEx '0x01'
    # White
    Given write data byte '0xff' to 24bit bus at '0x9c08' and addressEx '0x01'
    Given write data byte '0xff' to 24bit bus at '0x9c09' and addressEx '0x01'
    # Bright greeny yellow
    Given write data byte '0x77' to 24bit bus at '0x9c0a' and addressEx '0x01'
    Given write data byte '0x77' to 24bit bus at '0x9c0b' and addressEx '0x01'
    # Bright darker greeny yellow
    Given write data byte '0x55' to 24bit bus at '0x9c0c' and addressEx '0x01'
    Given write data byte '0x55' to 24bit bus at '0x9c0d' and addressEx '0x01'

    # Sprites4 Specific pattern to aid debugging of scale up and down behaviour
    Given start writing data to 24bit bus at '0x0000' and addressEx '0x04'
    # Guard data
    Given write data '0x55 0x55 0x55 0x55 0x55 0x55 0x55 0x55' to 24bit bus
    # Real data
    Given write data '0x21 0x43 0x23 0x01' to 24bit bus
    Given write data '0x10 0x32 0x21 0x43' to 24bit bus
    # Guard data
    Given write data '0x66 0x66 0x66 0x66 0x66 0x66 0x66 0x66' to 24bit bus

    # Enable display
    Given write data byte '0x30' to 24bit bus at '0x9e00' and addressEx '0x01'
    # Default display priority
    Given write data byte '0xe4' to 24bit bus at '0x9e08' and addressEx '0x01'
    # Overscan control
    Given write data byte '0x29' to 24bit bus at '0x9e09' and addressEx '0x01'
    # Just enable Sprites4
    Given write data byte '0x01' to 24bit bus at '0x9e0a' and addressEx '0x01'
    # Background colour
    Given write data byte '0x00' to 24bit bus at '0x9e0b' and addressEx '0x01'

    # Init combiners
    Given write data byte '0x60' to 24bit bus at '0xa200' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0xa201' and addressEx '0x01'
    Given write data byte '0x60' to 24bit bus at '0xa202' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0xa203' and addressEx '0x01'

    # Sprites4 registers
    # Zero flag
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    # Zero the X/Y border adjustments
    Given write data byte '0xf0' to 24bit bus at '0x8801' and addressEx '0x01'
    Given write data byte '0xff' to 24bit bus at '0x8802' and addressEx '0x01'
    Given write data byte '0xf8' to 24bit bus at '0x8803' and addressEx '0x01'
    Given write data byte '0xff' to 24bit bus at '0x8804' and addressEx '0x01'
    # Extent X/Y values
    Given write data byte '0xa8' to 24bit bus at '0x8805' and addressEx '0x01'
    Given write data byte '0x70' to 24bit bus at '0x8806' and addressEx '0x01'
    # Memory bank for sprite RAM load
    Given write data byte '0x00' to 24bit bus at '0x8807' and addressEx '0x01'

    # Sprites support X and Y flips
    # Palette | 0x10 = MSBX | 0x20 = MSBY | 0x40 = flipX | 0x80 = flipY
    # Y pos
    # Y size (in screen pixels, regardless of scale)
    # X pos
    # X size (in screen pixels, regardless of scale)
    # Sprite address (24 bits)
    # Y inv scale (*32)
    # X inv scale (*32)
    # Sprite stride-1
    Given start writing data to 24bit bus at '0x8808' and addressEx '0x01'
    # Note: These do not adjust the starting address, so will appear to be "wrong", but it is sometimes valid to not adjust the starting address for certain graphical effects
    Given write data '0x00 0x10 16 0x10 16 0x10 0x00 0x00 0x08 0x08 3' to 24bit bus
    Given write data '0x40 0x10 16 0x30 16 0x10 0x00 0x00 0x08 0x08 3' to 24bit bus
    Given write data '0x80 0x10 16 0x50 16 0x10 0x00 0x00 0x08 0x08 3' to 24bit bus
    Given write data '0xc0 0x10 16 0x70 16 0x10 0x00 0x00 0x08 0x08 3' to 24bit bus

    # Note: These do adjust the starting address, so will be correct
    Given write data '0x00 0x30 16 0x10 16 0x10 0x00 0x00 0x08 0x08 3' to 24bit bus
    Given write data '0x40 0x30 16 0x30 16 0x13 0x00 0x00 0x08 0x08 3' to 24bit bus
    Given write data '0x80 0x30 16 0x50 16 0x1c 0x00 0x00 0x08 0x08 3' to 24bit bus
    Given write data '0xc0 0x30 16 0x70 16 0x1f 0x00 0x00 0x08 0x08 3' to 24bit bus

    # As above but with different scales and sizes, to test stride and overflow
    Given write data '0x00 0x50 4 0x10 16 0x10 0x00 0x00 0x08 0x20 3' to 24bit bus
    Given write data '0x40 0x50 4 0x30 16 0x10 0x00 0x00 0x08 0x20 3' to 24bit bus
    Given write data '0x80 0x50 4 0x50 16 0x10 0x00 0x00 0x08 0x20 3' to 24bit bus
    Given write data '0xc0 0x50 4 0x70 16 0x10 0x00 0x00 0x08 0x20 3' to 24bit bus

    Given write data '0x00 0x60 4 0x10 16 0x10 0x00 0x00 0x08 0x20 3' to 24bit bus
    Given write data '0x40 0x60 4 0x30 16 0x13 0x00 0x00 0x08 0x20 3' to 24bit bus
    Given write data '0x80 0x60 4 0x50 16 0x1c 0x00 0x00 0x08 0x20 3' to 24bit bus
    Given write data '0xc0 0x60 4 0x70 16 0x1f 0x00 0x00 0x08 0x20 3' to 24bit bus

    Given write data '0x00 0x70 16 0x10 4 0x10 0x00 0x00 0x20 0x08 3' to 24bit bus
    Given write data '0x40 0x70 16 0x30 4 0x13 0x00 0x00 0x20 0x08 3' to 24bit bus
    Given write data '0x80 0x70 16 0x50 4 0x1c 0x00 0x00 0x20 0x08 3' to 24bit bus
    Given write data '0xc0 0x70 16 0x70 4 0x1f 0x00 0x00 0x20 0x08 3' to 24bit bus

    # Terminate the sprite list
    Given write data '0x00 0x00 0x00' to 24bit bus

    # Signal flag ready
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'

    Given enable debug pixel picking
    Given render a video display frame

    Given render a video display frame
    Given render a video display frame

    When display until window closed

    Then expect image "testdata/TC-17-4-000002.bmp" to be identical to "target/frames/TC-17-4-000002.bmp"
