Feature: Tests the video hardware expansion

  @TC-1
  Scenario: Display test with sprites, borders and contention
    Given a new video display
    Given video display saves debug BMP images to leaf filename "target/frames/TC-1-"
    Given I have a simple overclocked 6502 system
    Given a user port to 24 bit bus is installed
    Given add a Mode7 layer with registers at '0xa000' and addressEx '0x08'
    Given add a Tiles layer with registers at '0x9e00' and screen addressEx '0x80' and planes addressEx '0x40'
    Given add a Chars layer with registers at '0x9000' and addressEx '0x20'
    Given add a Sprites layer with registers at '0x9800' and addressEx '0x10'
    Given show video window

    # Instead of writing this data via the 6502 CPU, just send it straight to memory
    Given write data from file "C:\work\BombJack\PaletteData.bin" to 24bit bus at '0x9c00' and addressEx '0x01'
    Given write data from file "C:\work\BombJack\14_j07b.bin" to 24bit bus at '0x2000' and addressEx '0x10'
    Given write data from file "C:\work\BombJack\15_l07b.bin" to 24bit bus at '0x4000' and addressEx '0x10'
    Given write data from file "C:\work\BombJack\16_m07b.bin" to 24bit bus at '0x8000' and addressEx '0x10'

    # Setup bright yellow as background colour
    Given write data byte '0xff' to 24bit bus at '0x9c00' and addressEx '0x01'
    # Enable display with all borders
    Given write data byte '0xf0' to 24bit bus at '0x9e00' and addressEx '0x01'

    Given render a video display frame
    Given render a video display frame

    And I run the command line: ..\C64\acme.exe -v3 --lib ../C64/ -o test.prg --labeldump test.lbl -f cbm features/TestVideoHardware.a
    And I load prg "test.prg"
    And I load labels "test.lbl"
#    And I enable trace with indent
    When I execute the procedure at start for no more than 62980 instructions

    Then expect image "testdata/TC-1-000001.bmp" to be identical to "target/frames/TC-1-000001.bmp"
    Then expect image "testdata/TC-1-000002.bmp" to be identical to "target/frames/TC-1-000002.bmp"
    Then expect image "testdata/TC-1-000003.bmp" to be identical to "target/frames/TC-1-000003.bmp"
    Then expect image "testdata/TC-1-000004.bmp" to be identical to "target/frames/TC-1-000004.bmp"
    Then expect image "testdata/TC-1-000005.bmp" to be identical to "target/frames/TC-1-000005.bmp"
    Then expect image "testdata/TC-1-000006.bmp" to be identical to "target/frames/TC-1-000006.bmp"

#    When rendering the video until window closed
