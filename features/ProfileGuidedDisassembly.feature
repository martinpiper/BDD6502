Feature:  Profile guided disassembly

  This tests the profile guided disassembly generation.
  This executes 6502/6510 code with specific entry parameters, notes memory access for execution and read/write, and then uses this information to generate assembly code.
  Only memory that is accessed is output during the disassembly phase.
  This is useful for reverse engineering code.

  @TC-21
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
      ; Test generate label for branch never taken
      bne .neverTaken
      rts
    .neverTaken inc $1234
    selfModifyTest2
      jsr .other2
    .other2
      lda #0
      sta selfModifyTest2
      rts
    UseJumpTableSelfModify
      lda .loAddr,x
      sta .smJ+1
      lda .hiAddr,x
      sta .smJ+2
    .smJ jmp $1234
    .loAddr !by <.code1 , <.code2 , <.code3 , <.code4
    .hiAddr !by >.code1 , >.code2 , >.code3 , >.code4
    .code1
      inc $fb
      rts
    .code2
      inc $fc
      rts
    .code3
      inc $fd
      rts
    .code4
      inc $fe
      rts


    """
    And I run the command line: ..\C64\acme.exe -o test.prg --labeldump test.lbl -f cbm target\test1.a
    And I load prg "test.prg"
    And I load labels "test.lbl"

    Given I enable trace with indent

    Given enable memory profiling
    Given I set register X to 0x1
    When I execute the procedure at UseJumpTableSelfModify until return
    Given I set register X to 0x3
    When I execute the procedure at UseJumpTableSelfModify until return
    When I execute the procedure at selfModifyTest until return
    When I execute the procedure at selfModifyTest2 until return
    When I execute the procedure at start until return
    Given I disable trace
    When I execute the procedure at start until return for 1000 iterations

#    Then include profile last access
#    Then include profile index register type
#    Then include profile index range
#    Then include profile write hint
    Then include profile branch not taken
    Then output profile disassembly to file "target\test1dis.a"

    Given open file "target\test1dis.a" for reading
#    When ignoring lines that contain ";"
    When ignoring empty lines
    Then expect the next line to contain "label_fc = $fc"
    Then expect the next line to contain "label_fe = $fe"
    Then expect the next line to contain "label_0463 = label_0464 - 1 ; Table start skipped"
    Then expect the next line to contain "label_e8 = $e8"
    Then expect the next line to contain "label_a9 = $a9"
    Then expect the next line to contain "label_042e = label_0431 - 3 ; Table start skipped"
    Then expect the next line to contain "label_045f = label_0460 - 1 ; Table start skipped"
    Then expect the next line to contain "* = $03ff"
    Then expect the next line to contain "label_03ff	!by $20"
    Then expect the next line to contain "* = $0400"
    Then expect the next line to contain "label_0400	lda #$00"
    Then expect the next line to contain "sta label_03ff"
    Then expect the next line to contain "bne label_0408"
    Then expect the next line to contain "; Opcode multiple entry code : label_0407	bit label_00a9"
    Then expect the next line to contain "label_0407	!by $2c"
    Then expect the next line to contain "label_0408	!by $a9"
    Then expect the next line to contain "label_0409	!by $00"
    Then expect the next line to contain "bne label_040d"
    Then expect the next line to contain "; Opcode multiple entry code : label_040c	bit label_e8"
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
    Then expect the next line to contain "* = $0431"
    Then expect the next line to contain "label_0431	!by $03"
    Then expect the next line to contain "label_0432	!by $04 ; Never accessed"
    Then expect the next line to contain "label_0433	!by $05"
    Then expect the next line to contain "label_0434	!by $06 ; Never accessed"
    Then expect the next line to contain "label_0435	!by $07"
    Then expect the next line to contain "* = $0438"
    Then expect the next line to contain "label_0438	jsr label_043c ; Self modified parameters"
    Then expect the next line to contain "label_0439 = label_0438 + 1"
    Then expect the next line to contain "label_043a = label_0438 + 2"
    Then expect the next line to contain "label_043b	rts"
    Then expect the next line to contain "label_043c	lda #$00"
    Then expect the next line to contain "label_043e	sta label_0439"
    Then expect the next line to contain "label_0444 ; Branch not taken here"
    Then expect the next line to contain "bne label_0444 ; Branch not taken"
    Then expect the next line to contain "rts"
    Then expect the next line to contain "* = $0447"
    Then expect the next line to contain "; Self modified code : label_0447	jsr label_044a"
    Then expect the next line to contain "label_0447	!by $00"
    Then expect the next line to contain "label_0448	!by $4a"
    Then expect the next line to contain "label_0449	!by $04"
    Then expect the next line to contain "label_044a	lda #$00"
    Then expect the next line to contain "label_044c	sta label_0447"
    Then expect the next line to contain "rts"
    Then expect the next line to contain "label_0450	lda label_045f,x"
    Then expect the next line to contain "label_0453	sta label_045d"
    Then expect the next line to contain "label_0456	lda label_0463,x"
    Then expect the next line to contain "label_0459	sta label_045e"
    Then expect the next line to contain "label_045c	jmp label_0470 ; Self modified parameters"
    Then expect the next line to contain "label_045d = label_045c + 1"
    Then expect the next line to contain "label_045e = label_045c + 2"
    Then expect the next line to contain "* = $0460"
    Then expect the next line to contain "label_0460	!by <label_046a"
    Then expect the next line to contain "label_0461	!by $6d ; Never accessed"
    Then expect the next line to contain "label_0462	!by <label_0470"
    Then expect the next line to contain "* = $0464"
    Then expect the next line to contain "label_0464	!by >label_046a"
    Then expect the next line to contain "label_0465	!by $04 ; Never accessed"
    Then expect the next line to contain "label_0466	!by >label_0470"
    Then expect the next line to contain "* = $046a"
    Then expect the next line to contain "label_046a	inc label_fc"
    Then expect the next line to contain "rts"
    Then expect the next line to contain "* = $0470"
    Then expect the next line to contain "label_0470	inc label_fe"
    Then expect the next line to contain "rts"
    Then expect end of file
    Given close current file

    And I run the command line: ..\C64\acme.exe -o test.prg -f cbm target\test1dis.a
    Given I have a simple overclocked 6502 system
    And I load prg "test.prg"
    When I execute the procedure at start until return


  @TC-22
  Scenario: Complex binary only profile guided disassembly
    Given I have a simple overclocked 6502 system

    And I load prg "..\DebuggingDetails\MusicSelectSystemPatched_a000.prg"

    Given I enable trace with indent

    Given enable memory profiling

    # Note the minimal play routine code: ..\DebuggingDetails\RetrogradeMusicSelectSystem.a
    Given I set register A to 0x7d
    Given I set register X to 0x83
    # This register might not be needed...
    Given I set register Y to 0x01
    When I execute the procedure at 0x8ce until return

    Given I disable trace
    When I execute the procedure at 0x93d until return for 10000 iterations

#    Then include profile last access
#    Then include profile index register type
#    Then include profile index range
#    Then include profile write hint
    Then include profile branch not taken
    Then profile exclude memory range from 0xd400 to 0xd4ff
    Then output profile disassembly to file "target\temp.a"

    # cls && c:\work\c64\acme.exe --cpu 6502 -o c:\temp\t.prg -f cbm -v9 c:\work\BDD6502\target\temp.a c:\work\BDD6502\features\MinPlay.a && c:\work\c64\bin\LZMPi.exe -pp $37 -c64mbu c:\temp\t.prg c:\temp\tcmp.prg $c000 && c:\temp\tcmp.prg


  @TC-23
  Scenario: Complex binary only profile guided disassembly 2
    Given I have a simple overclocked 6502 system

    And I load prg "c:\temp\mus1000.prg"

    Given I enable trace with indent

    Given enable memory profiling

    Given I set register A to 0x00
    When I execute the procedure at 0x1000 until return

    Given I disable trace
    When I execute the procedure at 0x1003 until return for 10000 iterations
     # Stop music
    When I execute the procedure at 0x1006 until return

#    Then include profile last access
#    Then include profile index register type
#    Then include profile index range
#    Then include profile write hint
    Then include profile branch not taken
    Then profile exclude memory range from 0xd400 to 0xd4ff
    Then output profile disassembly to file "target\temp2.a"

