Feature: C64 ROM tests

  This checks the ROM addition syntax

  Scenario: Simple code test for C64 ROMs
    Given I have a simple overclocked 6502 system
    Given I am using C64 processor port options
    Given a ROM from file "..\..\VICE\C64\kernal" at $e000
    Given a ROM from file "..\..\VICE\C64\basic" at $a000
    Given add C64 hardware

#    And I enable trace with indent
    # This gets to repeatedly checking the keyboard buffer: E5CD  A5 C6     LDA $C6
    When I execute the indirect procedure at $fffc until return or until PC = $E5CF
    Then I continue executing the procedure until return or until PC = $E5CD

    # Using: https://sta.c64.org/cbm64pet.html
    # Run
    Then I write memory at $c6 with 1
    Then I write memory at $277 with $52
    Then I continue executing the procedure until return or until PC = $E5CF
    Then I continue executing the procedure until return or until PC = $E5CD

    Then I write memory at $c6 with 1
    Then I write memory at $277 with $55
    Then I continue executing the procedure until return or until PC = $E5CF
    Then I continue executing the procedure until return or until PC = $E5CD

    Then I write memory at $c6 with 1
    Then I write memory at $277 with $4e
    Then I continue executing the procedure until return or until PC = $E5CF
    Then I continue executing the procedure until return or until PC = $E5CD

    Then I write memory at $c6 with 1
    Then I write memory at $277 with $0d
    Then I continue executing the procedure until return or until PC = $E5CF
    Then I continue executing the procedure until return or until PC = $E5CD

    # Check for "run" and "ready." in screen memory
    When I hex dump memory between $400 and $800
    Then property "test.BDD6502.lastHexDump" must contain string "4f0: 12 15 0e 20"
    Then property "test.BDD6502.lastHexDump" must contain string "540: 12 05 01 04 19 2e 20"

    # Now explicitly check the various processor port expected behaviours
    And I create file "test.a" with
    """
    !source "../C64/stdlib/stdlib.a"
    !sal
    * = C64Cartridge_Lo_8K
    !word start
    !by $C3 , $C2 , $CD , $38 , $30	; CBM80 magic bytes
    start
      ; Copy into RAM under the ROM
      +MWordValueToAddress_A start , $fb
      ldy #0
    .cl1  lda ($fb),y
      sta ($fb),y
      +MIncAddr16 $fb , $fc
      lda $fc
      cmp #(>endCode)+1
      bcc .cl1

    .cl2
      lda FuncC000,y
      sta $c000,y
      dey
      bne .cl2

      ; Real code in ROM and in RAM so this is safe.
      ; NOTE: In Vice the EF3 cart type seems to map ROM all the time instead of RAM. But this emulation maps in RAM.
      lda #ProcessorPortAllRAM
      sta ZPProcessorPort

      ; Underlying RAM writes
      lda #$53
      sta $d800
      lda #$63
      sta $d000


      lda #ProcessorPortDefault
      sta ZPProcessorPort

      ; Underlying RAM writes through the ROMs
      lda #$23
      sta $e000
      lda #$13
      sta $a000
      ; To IO
      lda #$33
      sta $d800
      lda #$43
      sta $d000

      rts

    get
      sta ZPProcessorPort

      lda KERNALROM
      sta $400

      lda BASICROM
      sta $401

      lda VIC
      sta $402

      lda COLORRAM
      sta $403

      rts

    FuncC000
      sta ZPProcessorPort
      lda #42
      sta $400
      lda $8000
      sta $401
      stx $de00 ; Bank control
      lda $8000
      sta $402

      lda #ProcessorPortAllRAM
      sta ZPProcessorPort
      lda $8000
      sta $403

      rts
    endCode
    """
    And I run the command line: ..\C64\acme.exe -o test.prg --labeldump test.lbl -f cbm test.a
    And I run the command line: ..\C64\bin\MakeCart.exe -te -n -a $8000 -b 0 -r test.prg -c 0 2 $ffff -w -a $a000 -b 0 -c $1ffc 2 4 -w -a $8000 -b 1 -w  -b 2 -w -o test.crt

#    And I load prg "test.prg"
    And I load crt "test.crt"
    And I load labels "test.lbl"

    And I enable trace with indent

    When I execute the indirect procedure at $8000 until return

    When I set register A to ProcessorPortDefault
    When I execute the procedure at get until return
    When I hex dump memory between $400 and $407
    Then property "test.BDD6502.lastHexDump" must contain string "400: 85 94 43 03"

    When I set register A to ProcessorPortAllRAM
    When I execute the procedure at get until return
    When I hex dump memory between $400 and $407
    Then property "test.BDD6502.lastHexDump" must contain string "400: 23 13 63 53"

    When I set register A to ProcessorPortKERNALWithIO
    When I execute the procedure at get until return
    When I hex dump memory between $400 and $407
    Then property "test.BDD6502.lastHexDump" must contain string "400: 85 13 43 03"

    When I set register A to ProcessorPortDefault
    When I set register X to 1
    When I execute the procedure at $c000 until return
    When I hex dump memory between $400 and $407
    Then property "test.BDD6502.lastHexDump" must contain string "400: 2a 07 02 00"

    When I set register A to ProcessorPortDefault
    When I set register X to 2
    When I execute the procedure at $c000 until return
    When I hex dump memory between $400 and $407
    Then property "test.BDD6502.lastHexDump" must contain string "400: 2a 02 03 00"

    # Bank doesn't exist, so read RAM instead
    When I set register A to ProcessorPortDefault
    When I set register X to 3
    When I execute the procedure at $c000 until return
    When I hex dump memory between $400 and $407
    Then property "test.BDD6502.lastHexDump" must contain string "400: 2a 03 00 00"

    And I disable trace
