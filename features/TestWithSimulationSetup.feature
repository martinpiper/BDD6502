Feature: Test with the simulation setup

  This tests the emulation output with the simulation output, using the same layer setup as the main schematic.

  @TC-14
  Scenario: Using the simulation layer setup
    Given clear all external devices
    Given a new video display with overscan and 16 colours
    Given set the video display to RGB colour 5 6 5
    Given set the video display with 32 palette banks
    And enable video display bus debug output
    Given video display processes 8 pixels per instruction
    Given video display refresh window every 32 instructions
    Given video display does not save debug BMP images
    Given video display add joystick to port 1
    Given video display saves debug BMP images to leaf filename "target/frames/TC-14-"
    Given property "bdd6502.bus24.trace" is set to string "true"
    Given I have a simple overclocked 6502 system
    And That does fail on BRK
    And I enable uninitialised memory read protection with immediate fail
    Given a user port to 24 bit bus is installed

    Given add a 2-to-1 merge layer with registers at '0xa202'
    And the layer has 16 colours
    And the layer has overscan
      Given add a Mode7 layer with registers at '0xa000' and addressEx '0x08'
      And the layer has 16 colours
      And the layer has overscan
      Given add a Vector layer with registers at '0xa100' and addressEx '0x02'
      And the layer has 16 colours
      And the layer has overscan


    Given add a Tiles layer with registers at '0x9e00' and screen addressEx '0x80' and planes addressEx '0x40'
    And the layer has 16 colours
    And the layer has overscan


    Given add a Chars V4.0 layer with registers at '0x9000' and screen addressEx '0x80' and planes addressEx '0x20'
    And the layer has 16 colours
    And the layer has overscan


    Given add a 2-to-1 merge layer with registers at '0xa200'
    And the layer has 16 colours
    And the layer has overscan
      Given add a Sprites V9.5 layer with registers at '0x9800' and addressEx '0x10' and running at 16MHz
      And the layer has 16 colours
      And the layer has overscan
      And the layer uses exact address matching
      Given add a Sprites4 layer with registers at '0x8800' and addressEx '0x05' and running at 12.096MHz
      And the layer has 16 colours
      And the layer has overscan
      And the layer uses exact address matching


    Given show video window

    # Use: convert4.bat
    # Palette
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanPaletteData.bin" to 24bit bus at '0x9c00' and addressEx '0x01'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanScaledPaletteData.bin" to 24bit bus at '0x9d00' and addressEx '0x01'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanScaledPaletteData4.bin" to 24bit bus at '0x9d60' and addressEx '0x01'
    # Sprites
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanSprites_plane0.bin" to 24bit bus at '0x2000' and addressEx '0x10'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanSprites_plane1.bin" to 24bit bus at '0x4000' and addressEx '0x10'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanSprites_plane2.bin" to 24bit bus at '0x8000' and addressEx '0x10'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanSprites_plane3.bin" to 24bit bus at '0x0000' and addressEx '0x10'
    # Sprites4 data
    # Turrican Scaled
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanScaledSprites4.bin" to 24bit bus at '0x0000' and addressEx '0x05'
    # Tiles
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanTiles_map.bin" to 24bit bus at '0x2000' and addressEx '0x80'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanTiles_plane0.bin" to 24bit bus at '0x2000' and addressEx '0x40'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanTiles_plane1.bin" to 24bit bus at '0x4000' and addressEx '0x40'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanTiles_plane2.bin" to 24bit bus at '0x8000' and addressEx '0x40'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanTiles_plane3.bin" to 24bit bus at '0x0000' and addressEx '0x40'
    # Chars
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanStatus_map.bin" to 24bit bus at '0x4000' and addressEx '0x80'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanStatus_map.bin2" to 24bit bus at '0x8000' and addressEx '0x80'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanStatus_plane0.bin" to 24bit bus at '0x2000' and addressEx '0x20'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanStatus_plane1.bin" to 24bit bus at '0x4000' and addressEx '0x20'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanStatus_plane2.bin" to 24bit bus at '0x8000' and addressEx '0x20'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanStatus_plane3.bin" to 24bit bus at '0x0000' and addressEx '0x20'
    # Mode7
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanClouds_screen.bin" to 24bit bus at '0x2000' and addressEx '0x08'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanClouds_tiles.bin" to 24bit bus at '0x4000' and addressEx '0x08'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanClouds_tiles.bin2" to 24bit bus at '0x8000' and addressEx '0x08'
    # Vectors
    # Fill both banks with transparent lines
    Given fill data byte '0x00' to 24bit bus at '0x0000' to '0x3fff' stride '0x02' and addressEx '0x02'
    Given fill data byte '0xfe' to 24bit bus at '0x0001' to '0x3fff' stride '0x02' and addressEx '0x02'
    Given write data from file "C:\Work\ImageToBitplane\target\vectors_Data.bin" to 24bit bus at '0x0000' and addressEx '0x02'


    # Wide overscan can use 0x2b which has a couple of chars on the left masked for scrolling and hits the right edge _HSYNC
    # Use the 320 wide settings
    Given write data byte '0x29' to 24bit bus at '0x9e09' and addressEx '0x01'
    # Disable all layers
    Given write data byte '0x00' to 24bit bus at '0x9e0a' and addressEx '0x01'

    # Enable display with tiles and borders
    Given write data byte '0x20' to 24bit bus at '0x9e00' and addressEx '0x01'
    Given write data byte '0x70' to 24bit bus at '0x9e01' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x9e02' and addressEx '0x01'
    Given write data byte '0x8a' to 24bit bus at '0x9e03' and addressEx '0x01'
    Given write data byte '0x02' to 24bit bus at '0x9e04' and addressEx '0x01'
    # Layer priority
    Given write data byte '0x9c' to 24bit bus at '0x9e08' and addressEx '0x01'
    # Sprites size
    Given write data byte '0x00' to 24bit bus at '0x9a00' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9a01' and addressEx '0x01'

    # Mode7 registers
    Given write data byte '0x01' to 24bit bus at '0xa001' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0xa007' and addressEx '0x01'
    Given write data byte '0x1f' to 24bit bus at '0xa015' and addressEx '0x01'

    # Setup some graphics
    # While it would be possible to use 32x32 sprite mode for the top half of the player, there are 16x16 sprite tile optimisations that reduce duplicate tiles
    # So use 16x16 sprites instead, 6 of them!
    Given write data byte '0x00' to 24bit bus at '0x9800' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x9801' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x9802' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x9803' and addressEx '0x01'

    Given write data byte '0x01' to 24bit bus at '0x9804' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x9805' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x9806' and addressEx '0x01'
    Given write data byte '0x90' to 24bit bus at '0x9807' and addressEx '0x01'

    Given write data byte '0x0d' to 24bit bus at '0x9808' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x9809' and addressEx '0x01'
    Given write data byte '0x70' to 24bit bus at '0x980a' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x980b' and addressEx '0x01'

    Given write data byte '0x0e' to 24bit bus at '0x980c' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x980d' and addressEx '0x01'
    Given write data byte '0x70' to 24bit bus at '0x980e' and addressEx '0x01'
    Given write data byte '0x90' to 24bit bus at '0x980f' and addressEx '0x01'

    Given write data byte '0x2d' to 24bit bus at '0x9810' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x9811' and addressEx '0x01'
    Given write data byte '0x60' to 24bit bus at '0x9812' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x9813' and addressEx '0x01'

    Given write data byte '0x2e' to 24bit bus at '0x9814' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x9815' and addressEx '0x01'
    Given write data byte '0x60' to 24bit bus at '0x9816' and addressEx '0x01'
    Given write data byte '0x90' to 24bit bus at '0x9817' and addressEx '0x01'

    Given write data byte '0x00' to 24bit bus at '0xa800' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0xa801' and addressEx '0x01'
    Given write data byte '0x70' to 24bit bus at '0xa802' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0xa803' and addressEx '0x01'

    Given write data byte '0x01' to 24bit bus at '0xa804' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0xa805' and addressEx '0x01'
    Given write data byte '0x70' to 24bit bus at '0xa806' and addressEx '0x01'
    Given write data byte '0x90' to 24bit bus at '0xa807' and addressEx '0x01'

    Given write data byte '0x0d' to 24bit bus at '0xa808' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0xa809' and addressEx '0x01'
    Given write data byte '0x60' to 24bit bus at '0xa80a' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0xa80b' and addressEx '0x01'

    Given write data byte '0x0e' to 24bit bus at '0xa80c' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0xa80d' and addressEx '0x01'
    Given write data byte '0x60' to 24bit bus at '0xa80e' and addressEx '0x01'
    Given write data byte '0x90' to 24bit bus at '0xa80f' and addressEx '0x01'

    Given write data byte '0x2d' to 24bit bus at '0xa810' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0xa811' and addressEx '0x01'
    Given write data byte '0x50' to 24bit bus at '0xa812' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0xa813' and addressEx '0x01'

    Given write data byte '0x2e' to 24bit bus at '0xa814' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0xa815' and addressEx '0x01'
    Given write data byte '0x50' to 24bit bus at '0xa816' and addressEx '0x01'
    Given write data byte '0x90' to 24bit bus at '0xa817' and addressEx '0x01'

    # Vectors bank
    Given write data byte '0x00' to 24bit bus at '0xa100' and addressEx '0x01'

    # Sprites4 registers
    # Zero flag
    Given write data byte '0x02' to 24bit bus at '0x8800' and addressEx '0x01'
    # X/Y adjustments
    Given write data byte '0xf2' to 24bit bus at '0x8801' and addressEx '0x01'
    Given write data byte '0xff' to 24bit bus at '0x8802' and addressEx '0x01'
    Given write data byte '0xf4' to 24bit bus at '0x8803' and addressEx '0x01'
    Given write data byte '0xff' to 24bit bus at '0x8804' and addressEx '0x01'
    # Extent X/Y values
    Given write data byte '0xa8' to 24bit bus at '0x8805' and addressEx '0x01'
    Given write data byte '0x70' to 24bit bus at '0x8806' and addressEx '0x01'
    # Extra address
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
    Given write data byte '0x70' to 24bit bus at '0x8809' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x880a' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x880b' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x880c' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880d' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x880e' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x880f' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x8810' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x8811' and addressEx '0x01'
    Given write data byte '0x3f' to 24bit bus at '0x8812' and addressEx '0x01'

    # Standing left, double size
    Given write data byte '0x0b' to 24bit bus at '0x8813' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x8814' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x8815' and addressEx '0x01'
    Given write data byte '0x48' to 24bit bus at '0x8816' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x8817' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8818' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x8819' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x881a' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x881b' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x881c' and addressEx '0x01'
    Given write data byte '0x3f' to 24bit bus at '0x881d' and addressEx '0x01'

    # Terminate the sprite list
    Given write data byte '0x00' to 24bit bus at '0x8829' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x882a' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x882b' and addressEx '0x01'

    # Signal flag ready
    Given write data byte '0x03' to 24bit bus at '0x8800' and addressEx '0x01'

    # Default combine setup
    Given write data byte '0x60' to 24bit bus at '0xa200' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0xa201' and addressEx '0x01'
    Given write data byte '0x60' to 24bit bus at '0xa202' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0xa203' and addressEx '0x01'

    # Enable all layers
    Given write data byte '0x0f' to 24bit bus at '0x9e0a' and addressEx '0x01'
    Given render a video display frame
    Given render a video display frame
    Given render a video display frame

    # Visibility layer control
    Given write data byte '0x00' to 24bit bus at '0xa200' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x20' to 24bit bus at '0xa200' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x40' to 24bit bus at '0xa200' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x60' to 24bit bus at '0xa200' and addressEx '0x01'
    Given render a video display frame

    Given write data byte '0x00' to 24bit bus at '0xa202' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x20' to 24bit bus at '0xa202' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x40' to 24bit bus at '0xa202' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x60' to 24bit bus at '0xa202' and addressEx '0x01'
    Given render a video display frame

    # Now verify combination layer behaviour
    Given write data byte '0x60' to 24bit bus at '0xa200' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x61' to 24bit bus at '0xa200' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x62' to 24bit bus at '0xa200' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x63' to 24bit bus at '0xa200' and addressEx '0x01'
    Given render a video display frame

    Given write data byte '0x30' to 24bit bus at '0xa201' and addressEx '0x01'
    Given write data byte '0x60' to 24bit bus at '0xa200' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x61' to 24bit bus at '0xa200' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x62' to 24bit bus at '0xa200' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x63' to 24bit bus at '0xa200' and addressEx '0x01'
    Given render a video display frame

    Given write data byte '0x60' to 24bit bus at '0xa200' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0xa201' and addressEx '0x01'

    Given write data byte '0x60' to 24bit bus at '0xa200' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x61' to 24bit bus at '0xa200' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x62' to 24bit bus at '0xa200' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x63' to 24bit bus at '0xa200' and addressEx '0x01'
    Given render a video display frame

    Given write data byte '0x30' to 24bit bus at '0xa203' and addressEx '0x01'
    Given write data byte '0x60' to 24bit bus at '0xa202' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x61' to 24bit bus at '0xa202' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x62' to 24bit bus at '0xa202' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x63' to 24bit bus at '0xa202' and addressEx '0x01'
    Given render a video display frame

    Given write data byte '0x60' to 24bit bus at '0xa202' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0xa203' and addressEx '0x01'

    # Dithering
    Given write data byte '0x00' to 24bit bus at '0xa201' and addressEx '0x01'
    Given write data byte '0x64' to 24bit bus at '0xa200' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0xe4' to 24bit bus at '0xa200' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x65' to 24bit bus at '0xa200' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x66' to 24bit bus at '0xa200' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x67' to 24bit bus at '0xa200' and addressEx '0x01'
    Given render a video display frame

    Given write data byte '0x00' to 24bit bus at '0xa203' and addressEx '0x01'
    Given write data byte '0x64' to 24bit bus at '0xa202' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0xe4' to 24bit bus at '0xa202' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x65' to 24bit bus at '0xa202' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x66' to 24bit bus at '0xa202' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x67' to 24bit bus at '0xa202' and addressEx '0x01'
    Given render a video display frame

    # Combiner force out
    Given write data byte '0x60' to 24bit bus at '0xa200' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0xa201' and addressEx '0x01'
    Given write data byte '0x60' to 24bit bus at '0xa202' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0xa203' and addressEx '0x01'

    Given write data byte '0x68' to 24bit bus at '0xa200' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x70' to 24bit bus at '0xa200' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x78' to 24bit bus at '0xa200' and addressEx '0x01'
    Given render a video display frame

    Given write data byte '0x68' to 24bit bus at '0xa202' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x70' to 24bit bus at '0xa202' and addressEx '0x01'
    Given render a video display frame
    Given write data byte '0x78' to 24bit bus at '0xa202' and addressEx '0x01'
    Given render a video display frame

    # About 0.8 seconds
#    When display until window closed

    Then expect image "testdata/TC-14-000000.bmp" to be identical to "target/frames/TC-14-000000.bmp"
    Then expect image "testdata/TC-14-000001.bmp" to be identical to "target/frames/TC-14-000001.bmp"
    Then expect image "testdata/TC-14-000002.bmp" to be identical to "target/frames/TC-14-000002.bmp"
    Then expect image "testdata/TC-14-000003.bmp" to be identical to "target/frames/TC-14-000003.bmp"
    Then expect image "testdata/TC-14-000004.bmp" to be identical to "target/frames/TC-14-000004.bmp"
    Then expect image "testdata/TC-14-000005.bmp" to be identical to "target/frames/TC-14-000005.bmp"
    Then expect image "testdata/TC-14-000006.bmp" to be identical to "target/frames/TC-14-000006.bmp"
    Then expect image "testdata/TC-14-000007.bmp" to be identical to "target/frames/TC-14-000007.bmp"
    Then expect image "testdata/TC-14-000008.bmp" to be identical to "target/frames/TC-14-000008.bmp"
    Then expect image "testdata/TC-14-000009.bmp" to be identical to "target/frames/TC-14-000009.bmp"

    Then expect image "testdata/TC-14-000010.bmp" to be identical to "target/frames/TC-14-000010.bmp"
    Then expect image "testdata/TC-14-000011.bmp" to be identical to "target/frames/TC-14-000011.bmp"
    Then expect image "testdata/TC-14-000012.bmp" to be identical to "target/frames/TC-14-000012.bmp"
    Then expect image "testdata/TC-14-000013.bmp" to be identical to "target/frames/TC-14-000013.bmp"
    Then expect image "testdata/TC-14-000014.bmp" to be identical to "target/frames/TC-14-000014.bmp"
    Then expect image "testdata/TC-14-000015.bmp" to be identical to "target/frames/TC-14-000015.bmp"
    Then expect image "testdata/TC-14-000016.bmp" to be identical to "target/frames/TC-14-000016.bmp"
    Then expect image "testdata/TC-14-000017.bmp" to be identical to "target/frames/TC-14-000017.bmp"
    Then expect image "testdata/TC-14-000018.bmp" to be identical to "target/frames/TC-14-000018.bmp"
    Then expect image "testdata/TC-14-000019.bmp" to be identical to "target/frames/TC-14-000019.bmp"

    Then expect image "testdata/TC-14-000020.bmp" to be identical to "target/frames/TC-14-000020.bmp"
    Then expect image "testdata/TC-14-000021.bmp" to be identical to "target/frames/TC-14-000021.bmp"
    Then expect image "testdata/TC-14-000022.bmp" to be identical to "target/frames/TC-14-000022.bmp"
    Then expect image "testdata/TC-14-000023.bmp" to be identical to "target/frames/TC-14-000023.bmp"
    Then expect image "testdata/TC-14-000024.bmp" to be identical to "target/frames/TC-14-000024.bmp"
    Then expect image "testdata/TC-14-000025.bmp" to be identical to "target/frames/TC-14-000025.bmp"
    Then expect image "testdata/TC-14-000026.bmp" to be identical to "target/frames/TC-14-000026.bmp"
    Then expect image "testdata/TC-14-000027.bmp" to be identical to "target/frames/TC-14-000027.bmp"
    Then expect image "testdata/TC-14-000028.bmp" to be identical to "target/frames/TC-14-000028.bmp"
    Then expect image "testdata/TC-14-000029.bmp" to be identical to "target/frames/TC-14-000029.bmp"

    Then expect image "testdata/TC-14-000030.bmp" to be identical to "target/frames/TC-14-000030.bmp"
    Then expect image "testdata/TC-14-000031.bmp" to be identical to "target/frames/TC-14-000031.bmp"
    Then expect image "testdata/TC-14-000032.bmp" to be identical to "target/frames/TC-14-000032.bmp"
    Then expect image "testdata/TC-14-000033.bmp" to be identical to "target/frames/TC-14-000033.bmp"
    Then expect image "testdata/TC-14-000034.bmp" to be identical to "target/frames/TC-14-000034.bmp"
    Then expect image "testdata/TC-14-000035.bmp" to be identical to "target/frames/TC-14-000035.bmp"
    Then expect image "testdata/TC-14-000036.bmp" to be identical to "target/frames/TC-14-000036.bmp"
    Then expect image "testdata/TC-14-000037.bmp" to be identical to "target/frames/TC-14-000037.bmp"
    Then expect image "testdata/TC-14-000038.bmp" to be identical to "target/frames/TC-14-000038.bmp"
    Then expect image "testdata/TC-14-000039.bmp" to be identical to "target/frames/TC-14-000039.bmp"

    Then expect image "testdata/TC-14-000040.bmp" to be identical to "target/frames/TC-14-000040.bmp"
    Then expect image "testdata/TC-14-000041.bmp" to be identical to "target/frames/TC-14-000041.bmp"
    Then expect image "testdata/TC-14-000042.bmp" to be identical to "target/frames/TC-14-000042.bmp"



  Scenario: Validate with simulation output
    Then expect image "C:\work\BombJack\output\debug00000000.bmp" to be identical to "target/frames/TC-14-000000.bmp"
    Then expect image "C:\work\BombJack\output\debug00000001.bmp" to be identical to "target/frames/TC-14-000001.bmp"
    Then expect image "C:\work\BombJack\output\debug00000002.bmp" to be identical to "target/frames/TC-14-000002.bmp"
    Then expect image "C:\work\BombJack\output\debug00000003.bmp" to be identical to "target/frames/TC-14-000003.bmp"
    Then expect image "C:\work\BombJack\output\debug00000004.bmp" to be identical to "target/frames/TC-14-000004.bmp"
    Then expect image "C:\work\BombJack\output\debug00000005.bmp" to be identical to "target/frames/TC-14-000005.bmp"
    Then expect image "C:\work\BombJack\output\debug00000006.bmp" to be identical to "target/frames/TC-14-000006.bmp"
    Then expect image "C:\work\BombJack\output\debug00000007.bmp" to be identical to "target/frames/TC-14-000007.bmp"
    Then expect image "C:\work\BombJack\output\debug00000008.bmp" to be identical to "target/frames/TC-14-000008.bmp"
    Then expect image "C:\work\BombJack\output\debug00000009.bmp" to be identical to "target/frames/TC-14-000009.bmp"

    Then expect image "C:\work\BombJack\output\debug00000010.bmp" to be identical to "target/frames/TC-14-000010.bmp"
    Then expect image "C:\work\BombJack\output\debug00000011.bmp" to be identical to "target/frames/TC-14-000011.bmp"
    Then expect image "C:\work\BombJack\output\debug00000012.bmp" to be identical to "target/frames/TC-14-000012.bmp"
    Then expect image "C:\work\BombJack\output\debug00000013.bmp" to be identical to "target/frames/TC-14-000013.bmp"
    Then expect image "C:\work\BombJack\output\debug00000014.bmp" to be identical to "target/frames/TC-14-000014.bmp"
    Then expect image "C:\work\BombJack\output\debug00000015.bmp" to be identical to "target/frames/TC-14-000015.bmp"
    Then expect image "C:\work\BombJack\output\debug00000016.bmp" to be identical to "target/frames/TC-14-000016.bmp"
    Then expect image "C:\work\BombJack\output\debug00000017.bmp" to be identical to "target/frames/TC-14-000017.bmp"
    Then expect image "C:\work\BombJack\output\debug00000018.bmp" to be identical to "target/frames/TC-14-000018.bmp"
    Then expect image "C:\work\BombJack\output\debug00000019.bmp" to be identical to "target/frames/TC-14-000019.bmp"

    Then expect image "C:\work\BombJack\output\debug00000020.bmp" to be identical to "target/frames/TC-14-000020.bmp"
    Then expect image "C:\work\BombJack\output\debug00000021.bmp" to be identical to "target/frames/TC-14-000021.bmp"
    Then expect image "C:\work\BombJack\output\debug00000022.bmp" to be identical to "target/frames/TC-14-000022.bmp"
    Then expect image "C:\work\BombJack\output\debug00000023.bmp" to be identical to "target/frames/TC-14-000023.bmp"
    Then expect image "C:\work\BombJack\output\debug00000024.bmp" to be identical to "target/frames/TC-14-000024.bmp"
    Then expect image "C:\work\BombJack\output\debug00000025.bmp" to be identical to "target/frames/TC-14-000025.bmp"
    Then expect image "C:\work\BombJack\output\debug00000026.bmp" to be identical to "target/frames/TC-14-000026.bmp"
    Then expect image "C:\work\BombJack\output\debug00000027.bmp" to be identical to "target/frames/TC-14-000027.bmp"
    Then expect image "C:\work\BombJack\output\debug00000028.bmp" to be identical to "target/frames/TC-14-000028.bmp"
    Then expect image "C:\work\BombJack\output\debug00000029.bmp" to be identical to "target/frames/TC-14-000029.bmp"

    Then expect image "C:\work\BombJack\output\debug00000030.bmp" to be identical to "target/frames/TC-14-000030.bmp"
    Then expect image "C:\work\BombJack\output\debug00000031.bmp" to be identical to "target/frames/TC-14-000031.bmp"
    Then expect image "C:\work\BombJack\output\debug00000032.bmp" to be identical to "target/frames/TC-14-000032.bmp"
    Then expect image "C:\work\BombJack\output\debug00000033.bmp" to be identical to "target/frames/TC-14-000033.bmp"
    Then expect image "C:\work\BombJack\output\debug00000034.bmp" to be identical to "target/frames/TC-14-000034.bmp"
    Then expect image "C:\work\BombJack\output\debug00000035.bmp" to be identical to "target/frames/TC-14-000035.bmp"
    Then expect image "C:\work\BombJack\output\debug00000036.bmp" to be identical to "target/frames/TC-14-000036.bmp"
    Then expect image "C:\work\BombJack\output\debug00000037.bmp" to be identical to "target/frames/TC-14-000037.bmp"
    Then expect image "C:\work\BombJack\output\debug00000038.bmp" to be identical to "target/frames/TC-14-000038.bmp"
    Then expect image "C:\work\BombJack\output\debug00000039.bmp" to be identical to "target/frames/TC-14-000039.bmp"

    Then expect image "C:\work\BombJack\output\debug00000040.bmp" to be identical to "target/frames/TC-14-000040.bmp"
