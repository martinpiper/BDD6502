Feature: Tests the video and audio hardware expansion together

  @TC-1
  Scenario: Full display test with sprites, borders, contention, chars, tiles, and mode7, and sample play
    Given clear all external devices
    Given a new video display
    And enable video display bus debug output
    Given a new audio expansion
    Given video display processes 8 pixels per instruction
    Given video display refresh window every 32 instructions
    Given video display does not save debug BMP images
    Given video display add joystick to port 1
    Given video display saves debug BMP images to leaf filename "target/frames/TC-1-"
    Given property "bdd6502.bus24.trace" is set to string "true"
    Given property "bdd6502.apu.trace" is set to string "true"
    Given I have a simple overclocked 6502 system
    And That does fail on BRK
    And I enable unitialised memory read protection with immediate fail
    Given a user port to 24 bit bus is installed
    And enable user port bus debug output
    And enable APU mode
    Given add a Mode7 layer with registers at '0xa000' and addressEx '0x08'
    Given add a Tiles layer with registers at '0x9e00' and screen addressEx '0x80' and planes addressEx '0x40'
    Given add a Chars layer with registers at '0x9000' and addressEx '0x20'
    Given add a Sprites layer with registers at '0x9800' and addressEx '0x10'
    Given show video window

    # Instead of writing this data via the 6502 CPU, just send it straight to memory
    # Audio
    Given write data from file "testdata/sample.pcmu8" to 24bit bus at '0x0000' and addressEx '0x04'
    # Disabled the test write of the video during the frame unlimited code
#    Given write data byte '0x00' to 24bit bus at '0x802d' and addressEx '0x01'
#    Given write data byte '0xff' to 24bit bus at '0x8000' and addressEx '0x01'
#    Given write data byte '0xff' to 24bit bus at '0x8003' and addressEx '0x01'
#    Given write data byte '0xff' to 24bit bus at '0x8004' and addressEx '0x01'
#    Given write data byte '0x4a' to 24bit bus at '0x8005' and addressEx '0x01'
#    Given write data byte '0x0b' to 24bit bus at '0x8006' and addressEx '0x01'
#    Given write data byte '0xff' to 24bit bus at '0x8009' and addressEx '0x01'
#    Given write data byte '0xff' to 24bit bus at '0x800a' and addressEx '0x01'
#    Given write data byte '0x01' to 24bit bus at '0x802c' and addressEx '0x01'
#    Given write data byte '0x01' to 24bit bus at '0x802d' and addressEx '0x01'
#    When rendering the video until window closed

    # Palette
    Given write data from file "C:\work\BombJack\PaletteData.bin" to 24bit bus at '0x9c00' and addressEx '0x01'
    # Sprites
    Given write data from file "C:\work\BombJack\14_j07b.bin" to 24bit bus at '0x2000' and addressEx '0x10'
    Given write data from file "C:\work\BombJack\15_l07b.bin" to 24bit bus at '0x4000' and addressEx '0x10'
    Given write data from file "C:\work\BombJack\16_m07b.bin" to 24bit bus at '0x8000' and addressEx '0x10'
    # Tiles
    Given write data from file "C:\work\BombJack\08_r08t.bin" to 24bit bus at '0x2000' and addressEx '0x40'
    Given write data from file "C:\work\BombJack\07_n08t.bin" to 24bit bus at '0x4000' and addressEx '0x40'
    Given write data from file "C:\work\BombJack\06_l08t.bin" to 24bit bus at '0x8000' and addressEx '0x40'
    # Chars
    Given write data from file "C:\work\BombJack\05_k08t.bin" to 24bit bus at '0x2000' and addressEx '0x20'
    Given write data from file "C:\work\BombJack\04_h08t.bin" to 24bit bus at '0x4000' and addressEx '0x20'
    Given write data from file "C:\work\BombJack\03_e08t.bin" to 24bit bus at '0x8000' and addressEx '0x20'
    # Mode7
    Given write data from file "C:\work\BombJack\map.bin" to 24bit bus at '0x2000' and addressEx '0x08'
    Given write data from file "C:\work\BombJack\Mode7.bin" to 24bit bus at '0x4000' and addressEx '0x08'
    Given write data from file "C:\work\BombJack\Mode7B.bin" to 24bit bus at '0x8000' and addressEx '0x08'

    # Setup bright yellow as background colour
    Given write data byte '0xff' to 24bit bus at '0x9c00' and addressEx '0x01'
    # Enable display with all borders
    Given write data byte '0xf0' to 24bit bus at '0x9e00' and addressEx '0x01'
    # Default layer priority
    Given write data byte '0xe4' to 24bit bus at '0x9e08' and addressEx '0x01'

    Given render a video display frame

    # Disable the display again, to prepare for the code to turn it back on again
    Given write data byte '0x00' to 24bit bus at '0x9e00' and addressEx '0x01'

    And I run the command line: ..\C64\acme.exe -v3 --lib ../ -o test.prg --labeldump test.lbl -f cbm features/TestVideoHardware.a
    And I load prg "test.prg"
    And I load labels "test.lbl"
#    And I enable trace with indent

    When I execute the procedure at start for no more than 76036 instructions
    Given render a video display frame
    Then expect image "testdata/TC-1-000000.bmp" to be identical to "target/frames/TC-1-000000.bmp"
    Then expect image "testdata/TC-1-000001.bmp" to be identical to "target/frames/TC-1-000001.bmp"
    Then expect image "testdata/TC-1-000002.bmp" to be identical to "target/frames/TC-1-000002.bmp"
    Then expect image "testdata/TC-1-000003.bmp" to be identical to "target/frames/TC-1-000003.bmp"
    Then expect image "testdata/TC-1-000004.bmp" to be identical to "target/frames/TC-1-000004.bmp"
    Then expect image "testdata/TC-1-000005.bmp" to be identical to "target/frames/TC-1-000005.bmp"
    Then expect image "testdata/TC-1-000006.bmp" to be identical to "target/frames/TC-1-000006.bmp"

    When I execute the procedure at start2 for no more than 50688 instructions
    Then expect image "testdata/TC-1-000007.bmp" to be identical to "target/frames/TC-1-000007.bmp"
    Then expect image "testdata/TC-1-000008.bmp" to be identical to "target/frames/TC-1-000008.bmp"
    Then expect image "testdata/TC-1-000009.bmp" to be identical to "target/frames/TC-1-000009.bmp"
    Then expect image "testdata/TC-1-000010.bmp" to be identical to "target/frames/TC-1-000010.bmp"

    When I execute the procedure at start3 for no more than 50688 instructions
    Then expect image "testdata/TC-1-000011.bmp" to be identical to "target/frames/TC-1-000011.bmp"
    Then expect image "testdata/TC-1-000012.bmp" to be identical to "target/frames/TC-1-000012.bmp"
    Then expect image "testdata/TC-1-000013.bmp" to be identical to "target/frames/TC-1-000013.bmp"
    Then expect image "testdata/TC-1-000014.bmp" to be identical to "target/frames/TC-1-000014.bmp"

    When I execute the procedure at start4 for no more than 50689 instructions
    Then expect image "testdata/TC-1-000015.bmp" to be identical to "target/frames/TC-1-000015.bmp"
    Then expect image "testdata/TC-1-000016.bmp" to be identical to "target/frames/TC-1-000016.bmp"
    Then expect image "testdata/TC-1-000017.bmp" to be identical to "target/frames/TC-1-000017.bmp"
    Then expect image "testdata/TC-1-000018.bmp" to be identical to "target/frames/TC-1-000018.bmp"

#    Given I enable trace with indent
    When I execute the procedure at start5 for no more than 114050 instructions
    Then expect image "testdata/TC-1-000019.bmp" to be identical to "target/frames/TC-1-000019.bmp"
    Then expect image "testdata/TC-1-000020.bmp" to be identical to "target/frames/TC-1-000020.bmp"
    Then expect image "testdata/TC-1-000021.bmp" to be identical to "target/frames/TC-1-000021.bmp"
    Then expect image "testdata/TC-1-000022.bmp" to be identical to "target/frames/TC-1-000022.bmp"
    Then expect image "testdata/TC-1-000023.bmp" to be identical to "target/frames/TC-1-000023.bmp"
    Then expect image "testdata/TC-1-000024.bmp" to be identical to "target/frames/TC-1-000024.bmp"
    Then expect image "testdata/TC-1-000025.bmp" to be identical to "target/frames/TC-1-000025.bmp"
    Then expect image "testdata/TC-1-000026.bmp" to be identical to "target/frames/TC-1-000026.bmp"
    Then expect image "testdata/TC-1-000027.bmp" to be identical to "target/frames/TC-1-000027.bmp"

    When I execute the procedure at start6 for no more than 50686 instructions
    Given render a video display frame
    Then expect image "testdata/TC-1-000028.bmp" to be identical to "target/frames/TC-1-000028.bmp"
    Then expect image "testdata/TC-1-000029.bmp" to be identical to "target/frames/TC-1-000029.bmp"
    Then expect image "testdata/TC-1-000030.bmp" to be identical to "target/frames/TC-1-000030.bmp"
    Then expect image "testdata/TC-1-000031.bmp" to be identical to "target/frames/TC-1-000031.bmp"
    Then expect image "testdata/TC-1-000032.bmp" to be identical to "target/frames/TC-1-000032.bmp"

#    When rendering the video until window closed

    # This goes on for longer, due to the counter going through 0 to 255
#    And I enable trace with indent
    Given property "bdd6502.bus24.trace" is set to string "false"
    Given video display does not save debug BMP images
    Given limit video display to 60 fps
    When I execute the procedure at PlaySample for no more than 50686 instructions
    When I execute the procedure at start5 until return



  @TC-6
  Scenario: Full 16 colour display test with sprites, borders, contention, chars, tiles, and mode7
    Given clear all external devices
    Given a new video display with 16 colours
    And enable video display bus debug output
    Given a new audio expansion
    Given video display processes 8 pixels per instruction
    Given video display refresh window every 32 instructions
    Given video display does not save debug BMP images
    Given video display add joystick to port 1
    Given video display saves debug BMP images to leaf filename "target/frames/TC-6-"
    Given property "bdd6502.bus24.trace" is set to string "true"
    Given I have a simple overclocked 6502 system
    And That does fail on BRK
    And I enable unitialised memory read protection with immediate fail
    Given a user port to 24 bit bus is installed
#    Given add a GetBackground layer fetching from layer index '1'
    Given add a Mode7 layer with registers at '0xa000' and addressEx '0x08'
    Given add a Tiles layer with registers at '0x9e00' and screen addressEx '0x80' and planes addressEx '0x40'
    And the layer has 16 colours
    Given add a Chars V4.0 layer with registers at '0x9000' and screen addressEx '0x80' and planes addressEx '0x20'
    And the layer has 16 colours
    Given add a Sprites layer with registers at '0x9800' and addressEx '0x10'
    And the layer has 16 colours
    Given show video window

    # Use: convert4.bat
    # Palette
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanPaletteData.bin" to 24bit bus at '0x9c00' and addressEx '0x01'
    # Clear first palette entry to black
#    Given write data byte '0x00' to 24bit bus at '0x9c00' and addressEx '0x01'
#    Given write data byte '0x00' to 24bit bus at '0x9c01' and addressEx '0x01'
    # Sprites
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanSprites_plane0.bin" to 24bit bus at '0x2000' and addressEx '0x10'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanSprites_plane1.bin" to 24bit bus at '0x4000' and addressEx '0x10'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanSprites_plane2.bin" to 24bit bus at '0x8000' and addressEx '0x10'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanSprites_plane3.bin" to 24bit bus at '0x0000' and addressEx '0x10'
    # Tiles
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanTiles_map.bin" to 24bit bus at '0x2000' and addressEx '0x80'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanTiles_plane0.bin" to 24bit bus at '0x2000' and addressEx '0x40'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanTiles_plane1.bin" to 24bit bus at '0x4000' and addressEx '0x40'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanTiles_plane2.bin" to 24bit bus at '0x8000' and addressEx '0x40'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanTiles_plane3.bin" to 24bit bus at '0x0000' and addressEx '0x40'
    # Chars
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanStatus_map.bin" to 24bit bus at '0x4000' and addressEx '0x80'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanStatus_plane0.bin" to 24bit bus at '0x2000' and addressEx '0x20'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanStatus_plane1.bin" to 24bit bus at '0x4000' and addressEx '0x20'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanStatus_plane2.bin" to 24bit bus at '0x8000' and addressEx '0x20'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanStatus_plane3.bin" to 24bit bus at '0x0000' and addressEx '0x20'
    # Mode7
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanClouds_screen.bin" to 24bit bus at '0x2000' and addressEx '0x08'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanClouds_tiles.bin" to 24bit bus at '0x4000' and addressEx '0x08'
    Given write data from file "C:\Work\C64\VideoHardware\tmp\TurricanClouds_tiles.bin2" to 24bit bus at '0x8000' and addressEx '0x08'

    # Enable display with tiles and borders
    Given write data byte '0xf0' to 24bit bus at '0x9e00' and addressEx '0x01'
    Given write data byte '0x70' to 24bit bus at '0x9e01' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x9e02' and addressEx '0x01'
    Given write data byte '0x8a' to 24bit bus at '0x9e03' and addressEx '0x01'
    Given write data byte '0x02' to 24bit bus at '0x9e04' and addressEx '0x01'
    # Layer priority, mode7 in front of sprites and tiles
    Given write data byte '0x8d' to 24bit bus at '0x9e08' and addressEx '0x01'
    # Enable chars display
    Given write data byte '0x00' to 24bit bus at '0x9000' and addressEx '0x01'
    # Enable sprites display
    Given write data byte '0x10' to 24bit bus at '0x9a00' and addressEx '0x01'
    Given write data byte '0x00' to 24bit bus at '0x9a01' and addressEx '0x01'

    # Mode7 registers
    Given write data byte '0x01' to 24bit bus at '0xa001' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0xa007' and addressEx '0x01'
    Given write data byte '0x1f' to 24bit bus at '0xa015' and addressEx '0x01'


    # Setup some graphics
    # While it would be possible to use 32x32 sprite mode for the top half of the player, there are 16x16 sprite tile optimisations that reduce duplicate tiles
    # So use 16x16 sprites instead, 6 of them!
    Given write data byte '0x00' to 24bit bus at '0x9820' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x9821' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x9822' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x9823' and addressEx '0x01'

    Given write data byte '0x01' to 24bit bus at '0x9824' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x9825' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x9826' and addressEx '0x01'
    Given write data byte '0x90' to 24bit bus at '0x9827' and addressEx '0x01'

    Given write data byte '0x0d' to 24bit bus at '0x9828' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x9829' and addressEx '0x01'
    Given write data byte '0x70' to 24bit bus at '0x982a' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x982b' and addressEx '0x01'

    Given write data byte '0x0e' to 24bit bus at '0x982c' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x982d' and addressEx '0x01'
    Given write data byte '0x70' to 24bit bus at '0x982e' and addressEx '0x01'
    Given write data byte '0x90' to 24bit bus at '0x982f' and addressEx '0x01'

    Given write data byte '0x2d' to 24bit bus at '0x9830' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x9831' and addressEx '0x01'
    Given write data byte '0x60' to 24bit bus at '0x9832' and addressEx '0x01'
    Given write data byte '0x80' to 24bit bus at '0x9833' and addressEx '0x01'

    Given write data byte '0x2e' to 24bit bus at '0x9834' and addressEx '0x01'
    Given write data byte '0x01' to 24bit bus at '0x9835' and addressEx '0x01'
    Given write data byte '0x60' to 24bit bus at '0x9836' and addressEx '0x01'
    Given write data byte '0x90' to 24bit bus at '0x9837' and addressEx '0x01'

    # Quickly verify mode7 register latch reset behaviour
    Given render a video display until H=80 and V=128
    Given write data byte '0x01' to 24bit bus at '0xa015' and addressEx '0x01'
    Given write data byte '0x1f' to 24bit bus at '0xa015' and addressEx '0x01'
    Given render a video display until vsync

#    When display until window closed

    Then expect image "testdata/TC-6-000000.bmp" to be identical to "target/frames/TC-6-000000.bmp"
