Feature: Tests the video hardware expansion

  @TC-1
  Scenario: Full display test with sprites, borders, contention, chars, tiles, and mode7
    Given a new video display
    Given video display processes 8 pixels per instruction
    Given video display refresh window every 32 instructions
    Given video display saves debug BMP images to leaf filename "target/frames/TC-1-"
    Given I have a simple overclocked 6502 system
    Given a user port to 24 bit bus is installed
    Given add a Mode7 layer with registers at '0xa000' and addressEx '0x08'
    Given add a Tiles layer with registers at '0x9e00' and screen addressEx '0x80' and planes addressEx '0x40'
    Given add a Chars layer with registers at '0x9000' and addressEx '0x20'
    Given add a Sprites layer with registers at '0x9800' and addressEx '0x10'
    Given show video window

    # Instead of writing this data via the 6502 CPU, just send it straight to memory
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

    Given render a video display frame

    And I run the command line: ..\C64\acme.exe -v3 --lib ../ -o test.prg --labeldump test.lbl -f cbm features/TestVideoHardware.a
    And I load prg "test.prg"
    And I load labels "test.lbl"
#    And I enable trace with indent

    When I execute the procedure at start for no more than 75652 instructions
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

    When I execute the procedure at start4 for no more than 50687 instructions
    Then expect image "testdata/TC-1-000015.bmp" to be identical to "target/frames/TC-1-000015.bmp"
    Then expect image "testdata/TC-1-000016.bmp" to be identical to "target/frames/TC-1-000016.bmp"
    Then expect image "testdata/TC-1-000017.bmp" to be identical to "target/frames/TC-1-000017.bmp"
    Then expect image "testdata/TC-1-000018.bmp" to be identical to "target/frames/TC-1-000018.bmp"

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
    Then expect image "testdata/TC-1-000028.bmp" to be identical to "target/frames/TC-1-000028.bmp"
    Then expect image "testdata/TC-1-000029.bmp" to be identical to "target/frames/TC-1-000029.bmp"
    Then expect image "testdata/TC-1-000030.bmp" to be identical to "target/frames/TC-1-000030.bmp"
    Then expect image "testdata/TC-1-000031.bmp" to be identical to "target/frames/TC-1-000031.bmp"

#    When rendering the video until window closed
