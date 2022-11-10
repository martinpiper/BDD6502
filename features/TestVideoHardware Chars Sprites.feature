Feature: Tests the video character screen data conversion and sprites

  Uses ImageToBitplane config: oldbridge char screen
  >>> Use ImageToBitplane config to improve clouds conversion as it priortises their colours: oldbridge char screen with rgbfactor
  Use ImageToBitplane config to add use of colour 0, since if it is the back most layer then then it can use all 8 colours in the character block: oldbridge char screen with rgbfactor no force colour 0
  Or uses ImageToBitplane config: map_9 - Copy - chars.png char screen


  @TC-5
  Scenario: Chars and sprites display test with full user port bus code
    Given clear all external devices
    Given a new video display
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

