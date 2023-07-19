Feature: Test with the simulation setup

  This tests the emulation output with the simulation output, using the same layer setup as the main schematic.

  @TC-14
  Scenario: Using the simulation layer setup
    Given clear all external devices
    Given a new video display with overscan and 16 colours
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
      Given add a Sprites layer with registers at '0x9800' and addressEx '0x10'
      And the layer has 16 colours
      And the layer has overscan
      Given add a Sprites2 layer with registers at '0x9200' and addressEx '0x04' and running at 14.31818MHz
      And the layer has 16 colours
      And the layer has overscan


    Given show video window

    # Use: convert4.bat
    # Palette
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanPaletteData.bin" to 24bit bus at '0x9c00' and addressEx '0x01'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanScaledPaletteData.bin" to 24bit bus at '0x9d00' and addressEx '0x01'
    # Sprites
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanSprites_plane0.bin" to 24bit bus at '0x2000' and addressEx '0x10'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanSprites_plane1.bin" to 24bit bus at '0x4000' and addressEx '0x10'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanSprites_plane2.bin" to 24bit bus at '0x8000' and addressEx '0x10'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanSprites_plane3.bin" to 24bit bus at '0x0000' and addressEx '0x10'
    # Sprites2 data
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanScaledSprites0.bin" to 24bit bus at '0x2000' and addressEx '0x04'
    Given write data from file "C:\work\C64\VideoHardware\tmp\TurricanScaledSprites1.bin" to 24bit bus at '0x4000' and addressEx '0x04'
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

    # Sprites2 registers
    Given write data byte '0x08' to 24bit bus at '0x9200' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x9201' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x9202' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x9203' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x9204' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9205' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x9206' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9207' and addressEx '0x01'
    # Second scaled sprite
    Given write data byte '0x08' to 24bit bus at '0x9208' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x9209' and addressEx '0x01'
    Given write data byte '0x40' to 24bit bus at '0x920a' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x920b' and addressEx '0x01'
    Given write data byte '0x20' to 24bit bus at '0x920c' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x920d' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x920e' and addressEx '0x01'
    Given write data byte '0x10' to 24bit bus at '0x920f' and addressEx '0x01'

    # End of list
    Given write data byte '0x00' to 24bit bus at '0x9212' and addressEx '0x01'

    # Default combine setup
    Given write data byte '0x60' to 24bit bus at '0xa200' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0xa201' and addressEx '0x01'
    Given write data byte '0x60' to 24bit bus at '0xa202' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0xa203' and addressEx '0x01'

    # Enable all layers
    Given write data byte '0x0f' to 24bit bus at '0x9e0a' and addressEx '0x01'
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
