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
    Given a new video display with 16 colours
    And enable video display bus debug output
    Given video display processes 24 pixels per instruction
    Given video display refresh window every 32 instructions
    Given video display add joystick to port 2
    Given video display does not save debug BMP images
#    Given property "bdd6502.bus24.trace" is set to string "true"
    Given I have a simple overclocked 6502 system
    Given a user port to 24 bit bus is installed
    Given add a StaticColour layer for palette index '0x07'
    And the layer has 16 colours
#    Given add a Chars V4.0 layer with registers at '0x9000' and screen addressEx '0x80' and planes addressEx '0x20'
#    And the layer has 16 colours
    Given add a Sprites2 layer with registers at '0x9000' and addressEx '0x10'
    And the layer has 16 colours
    Given show video window

    # Palette
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanPaletteData.bin" to 24bit bus at '0x9c00' and addressEx '0x01'

    # Sprites2 data
    # --planestoscaled C:\Work\C64\VideoHardware\tmp\TurricanSprites_plane0.bin C:\Work\C64\VideoHardware\tmp\TurricanSprites_plane1.bin C:\Work\C64\VideoHardware\tmp\TurricanSprites_plane2.bin C:\Work\C64\VideoHardware\tmp\TurricanSprites_plane3.bin C:\Work\C64\VideoHardware\tmp\TurricanSprites_scaled
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanSprites_scaled0.bin" to 24bit bus at '0x2000' and addressEx '0x10'
    Given write data from file "C:\work\C64\VideoHardware\tmp\TurricanClouds_tiles.bin" to 24bit bus at '0x4000' and addressEx '0x10'
    Given write data from file "C:\work\C64\VideoHardware\tmp\TurricanClouds_tiles.bin2" to 24bit bus at '0x8000' and addressEx '0x10'
    Given write data from file "C:\work\BombJack\Mode7.bin" to 24bit bus at '0x0000' and addressEx '0x10'

    # Chars
    Given write data from file "C:\Work\ImageToBitplane\target\chars512_plane0.bin" to 24bit bus at '0x2000' and addressEx '0x20'
    Given write data from file "C:\Work\ImageToBitplane\target\chars512_plane1.bin" to 24bit bus at '0x4000' and addressEx '0x20'
    Given write data from file "C:\Work\ImageToBitplane\target\chars512_plane2.bin" to 24bit bus at '0x8000' and addressEx '0x20'
    Given write data from file "C:\Work\ImageToBitplane\target\chars512_plane3.bin" to 24bit bus at '0x0000' and addressEx '0x20'
    # Chars screen
    Given write data from file "C:\Work\ImageToBitplane\target\chars512_scr.bin" to 24bit bus at '0x4000' and addressEx '0x80'

    # Enable display
    Given write data byte '0x20' to 24bit bus at '0x9e00' and addressEx '0x01'
    # Default display priority
    Given write data byte '0xe4' to 24bit bus at '0x9e08' and addressEx '0x01'
    # Test the chars layer display enable
    Given write data byte '0x00' to 24bit bus at '0x9000' and addressEx '0x01'

    # Enable sprites2
    Given write data byte '0x01' to 24bit bus at '0x9100' and addressEx '0x01'

    # Sprites2 registers
    # Sprites support X and Y flips with X & Y repeating patterns
    # Palette | 0x10 =MSBY | 0x20 = MSBX | 0x40 = flipY | 0x80 = flipX
    # Y pos
    # Y size (in screen pixels, regardless of scale)
    # X pos
    # X scale extent (uses internal coordinates)
    # Y inv scale
    # X inv scale
    # Frame
    Given write data byte '0x01' to 24bit bus at '0x9200' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x9201' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9202' and addressEx '0x01'
    Given write data byte '0xf8' to 24bit bus at '0x9203' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9204' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9205' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9206' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9207' and addressEx '0x01'

    Given write data byte '0x00' to 24bit bus at '0x9208' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x9209' and addressEx '0x01'
    Given write data byte '0x30' to 24bit bus at '0x920a' and addressEx '0x01'
    Given write data byte '0xf0' to 24bit bus at '0x920b' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x920c' and addressEx '0x01'
    Given write data byte '0x08' to 24bit bus at '0x920d' and addressEx '0x01'
    Given write data byte '0x08' to 24bit bus at '0x920e' and addressEx '0x01'
    Given write data byte '0x44' to 24bit bus at '0x920f' and addressEx '0x01'

    # Use MSB X test
    Given write data byte '0xe0' to 24bit bus at '0x9210' and addressEx '0x01'
    Given write data byte '0xc0' to 24bit bus at '0x9211' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x9212' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9213' and addressEx '0x01'
    Given write data byte '0x18' to 24bit bus at '0x9214' and addressEx '0x01'
    Given write data byte '0x04' to 24bit bus at '0x9215' and addressEx '0x01'
    Given write data byte '0x04' to 24bit bus at '0x9216' and addressEx '0x01'
    Given write data byte '0xc2' to 24bit bus at '0x9217' and addressEx '0x01'

    # Use MSB Y test
    Given write data byte '0x11' to 24bit bus at '0x9218' and addressEx '0x01'
    Given write data byte '0xf0' to 24bit bus at '0x9219' and addressEx '0x01'
    Given write data byte '0x68' to 24bit bus at '0x921a' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x921b' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x921c' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x921d' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x921e' and addressEx '0x01'
    Given write data byte '0x03' to 24bit bus at '0x921f' and addressEx '0x01'

    Given write data byte '0x21' to 24bit bus at '0x9220' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9221' and addressEx '0x01'
    Given write data byte '0x08' to 24bit bus at '0x9222' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9223' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9224' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9225' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9226' and addressEx '0x01'
    Given write data byte '0x04' to 24bit bus at '0x9227' and addressEx '0x01'

    Given write data byte '0x21' to 24bit bus at '0x9228' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x9229' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x922a' and addressEx '0x01'
    Given write data byte '0x08' to 24bit bus at '0x922b' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x922c' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x922d' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x922e' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x922f' and addressEx '0x01'

    Given write data byte '0x01' to 24bit bus at '0x9230' and addressEx '0x01'
    Given write data byte '0x50' to 24bit bus at '0x9231' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9232' and addressEx '0x01'
    Given write data byte '0xf8' to 24bit bus at '0x9233' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9234' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9235' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9236' and addressEx '0x01'
    Given write data byte '0x0d' to 24bit bus at '0x9237' and addressEx '0x01'

    Given write data byte '0x21' to 24bit bus at '0x9238' and addressEx '0x01'
    Given write data byte '0x50' to 24bit bus at '0x9239' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x923a' and addressEx '0x01'
    Given write data byte '0x08' to 24bit bus at '0x923b' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x923c' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x923d' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x923e' and addressEx '0x01'
    Given write data byte '0x0e' to 24bit bus at '0x923f' and addressEx '0x01'

    # With scale * 2
    Given write data byte '0x01' to 24bit bus at '0x9240' and addressEx '0x01'
    Given write data byte '0xa0' to 24bit bus at '0x9241' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9242' and addressEx '0x01'
    Given write data byte '0x90' to 24bit bus at '0x9243' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9244' and addressEx '0x01'
    Given write data byte '0x08' to 24bit bus at '0x9245' and addressEx '0x01'
    Given write data byte '0x08' to 24bit bus at '0x9246' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9247' and addressEx '0x01'

    Given write data byte '0x01' to 24bit bus at '0x9248' and addressEx '0x01'
    Given write data byte '0xa0' to 24bit bus at '0x9249' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x924a' and addressEx '0x01'
    Given write data byte '0xb0' to 24bit bus at '0x924b' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x924c' and addressEx '0x01'
    Given write data byte '0x08' to 24bit bus at '0x924d' and addressEx '0x01'
    Given write data byte '0x08' to 24bit bus at '0x924e' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x924f' and addressEx '0x01'

    Given write data byte '0x01' to 24bit bus at '0x9250' and addressEx '0x01'
    Given write data byte '0xc0' to 24bit bus at '0x9251' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9252' and addressEx '0x01'
    Given write data byte '0x90' to 24bit bus at '0x9253' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9254' and addressEx '0x01'
    Given write data byte '0x08' to 24bit bus at '0x9255' and addressEx '0x01'
    Given write data byte '0x08' to 24bit bus at '0x9256' and addressEx '0x01'
    Given write data byte '0x0d' to 24bit bus at '0x9257' and addressEx '0x01'

    Given write data byte '0x01' to 24bit bus at '0x9258' and addressEx '0x01'
    Given write data byte '0xc0' to 24bit bus at '0x9259' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x925a' and addressEx '0x01'
    Given write data byte '0xb0' to 24bit bus at '0x925b' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x925c' and addressEx '0x01'
    Given write data byte '0x08' to 24bit bus at '0x925d' and addressEx '0x01'
    Given write data byte '0x08' to 24bit bus at '0x925e' and addressEx '0x01'
    Given write data byte '0x0e' to 24bit bus at '0x925f' and addressEx '0x01'

    # With scale / 2 and flipx
    Given write data byte '0x81' to 24bit bus at '0x9260' and addressEx '0x01'
    Given write data byte '0xa0' to 24bit bus at '0x9261' and addressEx '0x01'
    Given write data byte '0x08' to 24bit bus at '0x9262' and addressEx '0x01'
    Given write data byte '0xd8' to 24bit bus at '0x9263' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9264' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9265' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9266' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9267' and addressEx '0x01'

    Given write data byte '0x81' to 24bit bus at '0x9268' and addressEx '0x01'
    Given write data byte '0xa0' to 24bit bus at '0x9269' and addressEx '0x01'
    Given write data byte '0x08' to 24bit bus at '0x926a' and addressEx '0x01'
    Given write data byte '0xd0' to 24bit bus at '0x926b' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x926c' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x926d' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x926e' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x926f' and addressEx '0x01'

    Given write data byte '0x81' to 24bit bus at '0x9270' and addressEx '0x01'
    Given write data byte '0xa8' to 24bit bus at '0x9271' and addressEx '0x01'
    Given write data byte '0x08' to 24bit bus at '0x9272' and addressEx '0x01'
    Given write data byte '0xd8' to 24bit bus at '0x9273' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9274' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9275' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9276' and addressEx '0x01'
    Given write data byte '0x0d' to 24bit bus at '0x9277' and addressEx '0x01'

    Given write data byte '0x81' to 24bit bus at '0x9278' and addressEx '0x01'
    Given write data byte '0xa8' to 24bit bus at '0x9279' and addressEx '0x01'
    Given write data byte '0x08' to 24bit bus at '0x927a' and addressEx '0x01'
    Given write data byte '0xd0' to 24bit bus at '0x927b' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x927c' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x927d' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x927e' and addressEx '0x01'
    Given write data byte '0x0e' to 24bit bus at '0x927f' and addressEx '0x01'

    Given render a video display frame
    When rendering the video until window closed
