Feature:  Profile guided disassembly

  This tests the profile guided disassembly generation.
  This executes 6502/6510 code with specific entry parameters, notes memory access for execution and read/write, and then uses this information to generate assembly code.
  Only memory that is accessed is output during the disassembly phase.
  This is useful for reverse engineering code.

  Scenario: Simple profile guided disassembly
    Given I have a simple overclocked 6502 system
    And I create file "target\test1.a" with
    """
    !sal
    *=$400
    start
      lda #0
      sta $3ff
      bne .bitSkip+1
      .bitSkip bit $00a9
      bne .bitSkip2+1
      .bitSkip2 bit $e8
      ldx #$20
    .l1
      inc $3ff
      dex
      bne .l1
      ; Check for sparse index access...
      ldy #3
      lda .someData,y
      ldy #5
      lda .someData,y
      ldy #7
      lda .someData,y
      ldy #8
      rts
      ; This code won't be included in the disassembly because it's not executed or referenced
    .unusedCode
      inc .unusedCode
      jmp .unusedCode
    .someData
      !by 0,1,2,3,4,5,6,7,8,9
    selfModifyTest
      jsr .other
      rts
    .other
      lda #0
      sta selfModifyTest+1
      rts
    """
    And I run the command line: ..\C64\acme.exe -o test.prg --labeldump test.lbl -f cbm target\test1.a
    And I load prg "test.prg"
    And I load labels "test.lbl"

    Given I enable trace with indent

    Given enable memory profiling
    When I execute the procedure at selfModifyTest until return
    When I execute the procedure at start until return
    Given I disable trace
    When I execute the procedure at start until return for 1000 iterations

    Then include profile last access
    Then include profile index register type
    Then include profile index range
    Then include profile write hint
    Then output profile disassembly to file "target\test1dis.a"

    Given open file "target\test1dis.a" for reading
    When ignoring lines that contain ";"
    When ignoring empty lines
    Then expect the next line to contain "label_e8 = $e8"
    Then expect the next line to contain "label_a9 = $a9"
    Then expect the next line to contain "* = $03ff"
    Then expect the next line to contain "label_03ff	!by $20"
    Then expect the next line to contain "* = $0400"
    Then expect the next line to contain "label_0400	lda #$00"
    Then expect the next line to contain "sta label_03ff"
    Then expect the next line to contain "bne label_0408"
    Then expect the next line to contain "label_0407	!by $2c"
    Then expect the next line to contain "label_0408	!by $a9"
    Then expect the next line to contain "label_0409	!by $00"
    Then expect the next line to contain "bne label_040d"
    Then expect the next line to contain "label_040c	!by $24"
    Then expect the next line to contain "label_040d	!by $e8"
    Then expect the next line to contain "ldx #$20"
    Then expect the next line to contain "label_0410	inc label_03ff"
    Then expect the next line to contain "dex"
    Then expect the next line to contain "bne label_0410"
    Then expect the next line to contain "ldy #$03"
    Then expect the next line to contain "label_0418	lda label_042e,y"
    Then expect the next line to contain "ldy #$05"
    Then expect the next line to contain "label_041d	lda label_042e,y"
    Then expect the next line to contain "ldy #$07"
    Then expect the next line to contain "label_0422	lda label_042e,y"
    Then expect the next line to contain "ldy #$08"
    Then expect the next line to contain "label_0427	rts"
    Then expect the next line to contain "* = $042e"
    Then expect the next line to contain "label_042e	!by $00"
    Then expect the next line to contain "* = $0431"
    Then expect the next line to contain "label_0431	!by $03"
    Then expect the next line to contain "label_0432	!by $04"
    Then expect the next line to contain "label_0433	!by $05"
    Then expect the next line to contain "label_0434	!by $06"
    Then expect the next line to contain "label_0435	!by $07"
    Then expect the next line to contain "* = $0438"
    Then expect the next line to contain "label_0438	!by $20"
    Then expect the next line to contain "label_0439	!by $00"
    Then expect the next line to contain "label_043a	!by $04"
    Then expect the next line to contain "label_043b	rts"
    Then expect the next line to contain "label_043c	lda #$00"
    Then expect the next line to contain "label_043e	sta label_0439"
    Then expect the next line to contain "rts"
    Then expect end of file
    Given close current file

    And I run the command line: ..\C64\acme.exe -o test.prg -f cbm target\test1dis.a
    Given I have a simple overclocked 6502 system
    And I load prg "test.prg"
    When I execute the procedure at start until return


  Scenario: Complex binary only profile guided disassembly
    Given I have a simple overclocked 6502 system

    And I load prg "..\DebuggingDetails\MusicSelectSystemPatched_a000.prg"
#    And I load prg "c:\temp\mus1000.prg"

    Given I enable trace with indent

    Given enable memory profiling

    # Note the minimal play routine code: ..\DebuggingDetails\RetrogradeMusicSelectSystem.a
    Given I set register A to 0x7d
    Given I set register X to 0x83
    # This register might not be needed...
    Given I set register Y to 0x01
    When I execute the procedure at 0x8ce until return

#    Given I set register A to 0x00
#    When I execute the procedure at 0x1000 until return

    Given I disable trace
    When I execute the procedure at 0x93d until return for 10000 iterations

#    When I execute the procedure at 0x1003 until return for 1000 iterations
#     # Stop music
#    When I execute the procedure at 0x1006 until return for 1000 iterations

#    Then include profile last access
#    Then include profile index register type
#    Then include profile index range
#    Then include profile write hint
    Then output profile disassembly to file "target\temp.a"

    # cls && c:\work\c64\acme.exe --cpu 6502 -o c:\temp\t.prg -f cbm -v9 c:\work\BDD6502\target\temp.a c:\work\BDD6502\features\MinPlay.a && c:\work\c64\bin\LZMPi.exe -pp $37 -c64mbu c:\temp\t.prg c:\temp\tcmp.prg $c000 && c:\temp\tcmp.prg

