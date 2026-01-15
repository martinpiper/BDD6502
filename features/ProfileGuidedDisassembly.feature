Feature:  Profile guided disassembly

  This tests the profile guided disassembly generation.
  This executes 6502/6510 code with specific entry parameters, notes memory access for execution and read/write, and then uses this information to generate assembly code.
  Only memory that is accessed is output during the disassembly phase.
  This is useful for reverse engineering code.

  @TC-21
  Scenario: Profile guided disassembly integration
    Given I have a simple overclocked 6502 system
    When I enable uninitialised memory read protection with immediate fail
    # Stops the memory protection triggering on these temporary locations
    Given I write memory at $fb with 0
    Given I write memory at $fc with 0
    Given I write memory at $fd with 0
    Given I write memory at $fe with 0
    # Due to the bit $00a9
    Given I write memory at $a9 with 0
    # Due to the bit $e8
    Given I write memory at $e8 with 0

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
    Then profile use fill instead of PC adjust
    Then profile set PC adjust limit to 256 bytes
    Then include profile branch not taken
    Then profile exclude branches not taken
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
    Then expect the next line to contain "!by $20"
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
    Then expect the next line to contain "!fill 9"
    Then expect the next line to contain "label_0431	!by $03"
    Then expect the next line to contain "!by $04 ; Never accessed"
    Then expect the next line to contain "label_0433	!by $05"
    Then expect the next line to contain "!by $06 ; Never accessed"
    Then expect the next line to contain "label_0435	!by $07"
    Then expect the next line to contain "!fill 2"
    Then expect the next line to contain "label_0438	jsr label_043c ; Self modified parameters"
    Then expect the next line to contain "label_0439 = label_0438 + 1"
    Then expect the next line to contain "label_043a = label_0438 + 2"
    Then expect the next line to contain "label_043b	rts"
    Then expect the next line to contain "label_043c	lda #$00"
    Then expect the next line to contain "label_043e	sta label_0439"
    Then expect the next line to contain "; Excluded: 	bne label_0444 ; Branch not taken"
    Then expect the next line to contain "rts"
    Then expect the next line to contain "!fill 3"
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
    Then expect the next line to contain "!fill 1"
    Then expect the next line to contain "label_0460	!by <label_046a"
    Then expect the next line to contain "!by $6d ; Never accessed"
    Then expect the next line to contain "label_0462	!by <label_0470"
    Then expect the next line to contain "!fill 1"
    Then expect the next line to contain "label_0464	!by >label_046a"
    Then expect the next line to contain "!by $04 ; Never accessed"
    Then expect the next line to contain "label_0466	!by >label_0470"
    Then expect the next line to contain "!fill 3"
    Then expect the next line to contain "label_046a	inc label_fc"
    Then expect the next line to contain "rts"
    Then expect the next line to contain "!fill 3"
    Then expect the next line to contain "label_0470	inc label_fe"
    Then expect the next line to contain "rts"
    Then expect end of file
    Given close current file

    And I run the command line: ..\C64\acme.exe -o test.prg --labeldump test.lbl -f cbm target\test1dis.a
    Given I have a simple overclocked 6502 system
    When I enable uninitialised memory read protection with immediate fail
    # Due to the bit $00a9
    Given I write memory at $a9 with 0
    # Due to the bit $e8
    Given I write memory at $e8 with 0
    And I load prg "test.prg"
    And I load labels "test.lbl"
    When I execute the procedure at label_0400 until return


  @TC-22
  Scenario: Complex binary only profile guided disassembly
    Given I have a simple overclocked 6502 system
    # Due to the bit $00a9
    Given I write memory at $a9 with 0
    When I enable uninitialised memory read protection with immediate fail

    And I load prg "..\DebuggingDetails\MusicSelectSystemPatched_a000.prg"

    Given I enable trace with indent

    Given enable memory profiling
    Given memory profile record writes from 0xd400 to 0xd4ff


    # Note the minimal play routine code: ..\DebuggingDetails\RetrogradeMusicSelectSystem.a
    # 0x837d points to music data
    Given I set register A to 0x7d
    Given I set register X to 0x83
    When I execute the procedure at 0x8ce until return

    Given I disable trace
    When I execute the procedure at 0x93d until return for 10000 iterations

#    Then include profile last access
#    Then include profile index register type
#    Then include profile index range
#    Then include profile write hint
    Then include profile branch not taken
    Then profile use fill instead of PC adjust
    Then profile set PC adjust limit to 256 bytes
    Then profile avoid PC set in code
    Then profile avoid PC adjust in code
    Then profile avoid PC adjust in data
    # Removing spaces in data is quite aggressive and can introduce problems
    Then profile avoid PC set in data
    Then profile preserve data spacing from 0x837e to 0xffff
    # This the data spacing is needed, so we protect it
    Then profile preserve data spacing from 0x0ac4 to 0x0ac8
    Then profile exclude branches not taken
    Then profile output never accessed as a 0 byte
    Then profile exclude memory range from 0xd400 to 0xd4ff
    Then profile optimise labels
    # Reload the data so the memory is exactly the same before profiled execution
    And I load prg "..\DebuggingDetails\MusicSelectSystemPatched_a000.prg"
    Then output profile disassembly to file "target\temp.a"

    # Now validate the writes
    Given I have a simple overclocked 6502 system
    # Due to the bit $00a9
    Given I write memory at $a9 with 0
    And I run the command line: ..\C64\acme.exe -o test.prg --labeldump test.lbl -f cbm -v9 features\MinPlay.a
    And I load prg "test.prg"
    And I load labels "test.lbl"
    Given enable memory profiling
    Given memory profile record writes from 0xd400 to 0xd4ff
    # Validates, per iteration, the recorded memory writes with the previous execution
    Given enable memory profiling validation
    Given I set register A to lo(label_837d)
    Given I set register X to hi(label_837d)
    When I execute the procedure at label_08ce until return
    When I execute the procedure at label_093d until return for 10000 iterations

    # cls && c:\work\c64\acme.exe --cpu 6502 -o c:\temp\t.prg --labeldump test.lbl -f cbm -v9 features\MinPlay.a && c:\work\c64\bin\LZMPi.exe -pp $37 -c64mbu c:\temp\t.prg c:\temp\tcmp.prg $cf80 && c:\temp\tcmp.prg


  @TC-23
  Scenario: Complex binary only profile guided disassembly 2
    Given I have a simple overclocked 6502 system
    When I enable uninitialised memory read protection with immediate fail
    Given I write memory at $fb with 0
    Given I write memory at $fc with 0

    And I load prg "testdata\mus1000.prg"

    Given I enable trace with indent

    Given enable memory profiling
    Given memory profile record writes from 0xd400 to 0xd4ff

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
    Then profile use fill instead of PC adjust
    Then profile set PC adjust limit to 256 bytes
    Then profile avoid PC set in code
#    Then profile avoid PC adjust in code
#    Then profile preserve code spacing from 0x1000 to 0x17ff
#    Then profile avoid PC adjust in data
#    Then profile preserve data spacing from 0x1000 to 0x17ff

    Then profile exclude memory range from 0xd400 to 0xd4ff
    Then profile optimise labels
    # Reload the data so the memory is exactly the same before profiled execution
    And I load prg "testdata\mus1000.prg"
    Then output profile disassembly to file "target\temp2.a"

    Given I have a simple overclocked 6502 system
    When I enable uninitialised memory read protection with immediate fail
    Given I write memory at $fb with 0
    Given I write memory at $fc with 0

    And I run the command line: ..\C64\acme.exe --setpc $1000 --cpu 6502 -o test.prg --labeldump test.lbl -f cbm -v9 features\MinPlay_SID.a target\temp2.a
    And I load prg "test.prg"
    And I load labels "test.lbl"
    Given enable memory profiling
    Given memory profile record writes from 0xd400 to 0xd4ff
    # Validates, per iteration, the recorded memory writes with the previous execution
    Given enable memory profiling validation

    Given I set register A to 0x00
    When I execute the procedure at label_1000 until return
    When I execute the procedure at label_1003 until return for 10000 iterations
    When I execute the procedure at label_1006 until return



  @TC-24
  Scenario: Complex binary only profile guided disassembly
    Given I have a simple overclocked 6502 system
    When I enable uninitialised memory read protection with immediate fail
    Given I disable trace


    And I load prg "testdata\mwmusb000_b059.prg"

#    Given I enable trace with indent

    Given enable memory profiling
    Given memory profile record writes from 0xd400 to 0xd4ff

#    Given I enable trace with indent at iteration 1598

    Given I set register A to 0x00
    When I execute the procedure at 0xb000 until return

    Given I disable trace
    When I execute the procedure at 0xb059 until return for 10000 iterations

#    Then include profile last access
#    Then include profile index register type
#    Then include profile index range
#    Then include profile write hint
    Then include profile branch not taken
    Then profile use fill instead of PC adjust
    Then profile set PC adjust limit to 256 bytes
    Then profile avoid PC set in code
#    Then profile always set PC when moving from code to data
#    Then profile always set PC when moving from data to code
    Then profile avoid PC adjust in code
    # This piece of music uses pointers, these can be relocated
    Then profile avoid PC adjust in data
    # Removing spaces in data is quite aggressive and can introduce problems
    Then profile avoid PC set in data
#    Then profile preserve data spacing from 0xb4c2 to 0xffff
    Then profile exclude branches not taken
    Then profile output never accessed as a 0 byte
    Then profile exclude memory range from 0xd400 to 0xd4ff
    Then profile optimise labels
    # Reload the data so the memory is exactly the same before profiled execution
    And I load prg "testdata\mwmusb000_b059.prg"
    Then output profile disassembly to file "target\temp3.a"

    # Now validate the writes
    Given I have a simple overclocked 6502 system
    When I enable uninitialised memory read protection with immediate fail
    Given I disable trace
    And I run the command line: ..\C64\acme.exe --setpc $c000 -o test.prg --labeldump test.lbl -f cbm -v9 features\MinPlay_SID.a target\temp3.a
    And I load prg "test.prg"
    And I load labels "test.lbl"
    Given enable memory profiling
    Given memory profile record writes from 0xd400 to 0xd4ff
    # Validates, per iteration, the recorded memory writes with the previous execution
    Given enable memory profiling validation

#    Given I enable trace with indent
#    Given I enable trace with indent at iteration 1598

    Given I set register A to 0x00
    When I execute the procedure at label_b000 until return
    When I execute the procedure at label_b059 until return for 10000 iterations
#    When I execute the procedure at $b000 until return
#    When I execute the procedure at $b059 until return for 10000 iterations

    # cls && c:\work\c64\acme.exe --cpu 6502 -o c:\temp\t.prg --labeldump test.lbl -f cbm -v9 features\MinPlay3.a && c:\work\c64\bin\LZMPi.exe -pp $35 -c64mbu c:\temp\t.prg c:\temp\tcmp.prg $cf80 && c:\temp\tcmp.prg



  # Using information from here: https://github.com/martinpiper/DebuggingDetails/blob/main/Elite.txt
  @TC-25
  Scenario: Analyse Elite's line draw
    Given clear all external devices
    Given a new C64 video display
    And show C64 video window
    And C64 video display saves debug BMP images to leaf filename "target/frames/TC-25-C64-1-"
    And force C64 displayed bank to 2

    Given I have a simple overclocked 6502 system
    Given I am using C64 processor port options
    Given add C64 hardware
    When I enable uninitialised memory read protection with immediate fail
    Given I disable trace
    And I load prg "testdata\eld.prg"
    # Same as the game code initialisation
    Given I write memory at $01 with $35
    Given I write memory at $d011 with $3b
    Given I write memory at $d018 with $81
    Given I write memory at $d020 with 2

    And render a C64 video display frame
    Given I fill memory from $4000 to $6000 exclusive with $00
    Given I fill memory from $6000 to $6400 exclusive with $10
    And render a C64 video display frame

    Given enable memory profiling
    Given memory profile record writes from 0x4000 to 0x5fff

    And I start writing memory at $6b
    And I write the following hex bytes
      | 10 20 30 40 |
    When I execute the procedure at $b49d until return
    And render a C64 video display frame
    And I start writing memory at $6b
    And I write the following hex bytes
      | 0 0 ff c7 |
    When I execute the procedure at $b49d until return
    And render a C64 video display frame

    # This aims to exercise combinations of line drawing operations to get maximum code coverage
    And I create file "target\eliteinit.a" with
      """
      !sal
      *=$400
      excludeStart
      coords
        !by 0 , 0 , 255, 199
      coords2
        !by 0, 199 , 255 , 0
      start
        ldx #3
      .cl1
        lda coords,x
        sta $6b,x
        dex
        bpl .cl1
        jsr $b49d
        inc coords
        bne start
        inc coords+1
        lda coords+1
        cmp #$c8
        bne start

        ; And now the opposite case
      start2
        ldx #3
      .cl2
        lda coords2,x
        sta $6b,x
        dex
        bpl .cl2
        jsr $b49d
        inc coords2
        bne start2
        inc coords2+3
        lda coords2+3
        cmp #$c8
        bne start2
        rts
      excludeEnd
      """
    And I run the command line: ..\C64\acme.exe -o target\eliteinit.prg --labeldump target\eliteinit.lbl -f cbm target\eliteinit.a
    And I load prg "target\eliteinit.prg"
    And I load labels "target\eliteinit.lbl"
    Then profile exclude memory range from excludeStart to excludeEnd
    Given C64 video display does not save debug BMP images
    When I execute the procedure at start until return
    And C64 video display saves debug BMP images to leaf filename "target/frames/TC-25-C64-1-"
    And render a C64 video display frame

       # This aims to exercise combinations of line drawing operations to get maximum code coverage
    And I create file "target\elitedemo_includes.a" with
      """
      label_6b = $6b
      label_b49d = $b49d
      """
    And I create file "target\elitedemo.a" with
      """
      !sal
      excludeStart2
      start2
        jsr DoDraw
        jsr DoDraw

      ; Apply velocity
        ldx #3
      .cl1
        lda coords,x
        clc
        adc velocity,x
        sta coords,x
        dex
        bpl .cl1

      !macro MDoTest .i , .max {
        lda coords + .i
        cmp #4
        bcs .o1
        lda velocity + .i
        bpl .o1
        eor #$ff
        clc
        adc #1
        sta velocity + .i
        jmp .o2
      .o1
        cmp #.max
        bcc .o2
        lda velocity + .i
        bmi .o2
        eor #$ff
        clc
        adc #1
        sta velocity + .i
      .o2
        }
        +MDoTest 0 , 250
        +MDoTest 1 , 195
        +MDoTest 2 , 250
        +MDoTest 3 , 195
        rts

      DoDraw
        ldx #3
      .cl2
        lda coords,x
        sta+1 label_6b,x
        dex
        bpl .cl2
        jsr label_b49d
        rts

      coords
        !by 100 , 100 , 100, 100
      velocity
        !by 1 , 2 , 3 , 4
      excludeEnd2
      """
    And I run the command line: ..\C64\acme.exe --setpc $400 -o target\elitedemo.prg --labeldump target\elitedemo.lbl -f cbm target\elitedemo.a target\elitedemo_includes.a
    And I load prg "target\elitedemo.prg"
    And I load labels "target\elitedemo.lbl"
    Given I fill memory from $4000 to $6000 exclusive with $00
    Given I fill memory from $6000 to $6400 exclusive with $10
    Then profile exclude memory range from excludeStart2 to excludeEnd2
    Given C64 video display does not save debug BMP images
    When I execute the procedure at start2 until return for 10000 iterations
    And C64 video display saves debug BMP images to leaf filename "target/frames/TC-25-C64-1-"
    And render a C64 video display frame

#    Then include profile last access
#    Then include profile index register type
#    Then include profile index range
#    Then include profile write hint

    Then include profile branch not taken
    Then profile use fill instead of PC adjust
    Then profile set PC adjust limit to 256 bytes
#    Then profile always set PC when moving from code to data
#    Then profile always set PC when moving from data to code
    Then profile avoid PC set in code
    Then profile avoid PC adjust in code
    Then profile avoid PC set in data
    Then profile avoid PC adjust in data
#    Then profile preserve data spacing from 0x9c21 to 0x9dff
#    Then profile preserve data spacing from 0xa000 to 0xa1ff
#    Then profile exclude branches not taken
    # Less aggressive than all not taken branches
#    Then profile exclude blind branches not taken
#    Then profile output never accessed as a 0 byte
    Then profile exclude memory range from 0x4000 to 0x5fff
    # This is because none of the tests was able to exercise this part of the code
    Then profile mark as code from 0xb501 to 0xb504 inclusive
    Then profile optimise labels
    # Reload the data so the memory is exactly the same before profiled execution
    And I load prg "testdata\eld.prg"
    Then output profile disassembly to file "target\elitelinedraw.a"


    # Now test the disassembled code to make sure it works when relocated elsewhere
    Given clear all external devices
    Given a new C64 video display
    And show C64 video window
    And C64 video display saves debug BMP images to leaf filename "target/frames/TC-25-C64-2-"
    And force C64 displayed bank to 2

    Given I have a simple overclocked 6502 system
    Given I am using C64 processor port options
    Given add C64 hardware
    When I enable uninitialised memory read protection with immediate fail
    Given I enable trace with indent

    Given I write memory at $01 with $35
    Given I write memory at $d011 with $3b
    Given I write memory at $d018 with $81
    Given I write memory at $d020 with 2

    # Set this memory as written because the draw code uses EOR which reads "uninitialised" memory
    Given I fill memory from $4000 to $6000 exclusive with $00
    Given I fill memory from $6000 to $6400 exclusive with $10
    And render a C64 video display frame
    And render a C64 video display frame

    And I run the command line: ..\C64\acme.exe --setpc $ba00 -o target\elitelinedraw.prg --labeldump target\elitelinedraw.lbl -f cbm target\elitelinedraw.a
    And I load prg "target\elitelinedraw.prg"
    And I load labels "target\elitelinedraw.lbl"

    Given enable memory profiling
    Given memory profile record writes from 0x4000 to 0x5fff
    Given enable memory profiling validation

#    Given I enable trace with indent
    And I start writing memory at label_6b
    And I write the following hex bytes
      | 10 20 30 40 |
    When I execute the procedure at label_b49d until return
    And I start writing memory at label_6b
    And I write the following hex bytes
      | 0 0 ff c7 |
    When I execute the procedure at label_b49d until return
    And render a C64 video display frame
    And render a C64 video display frame

    Then expect image "target/frames/TC-25-C64-1-000003.bmp" to be identical to "target/frames/TC-25-C64-2-000003.bmp"

    # This aims to exercise combinations of line drawing operations to get maximum code coverage
    And I create file "target\eliteinit.a" with
      """
      !source "target\elitelinedraw.lbl"
      !sal
      excludeStart
      coords
        !by 0 , 0 , 255, 199
      coords2
        !by 0, 199 , 255 , 0
      start
        ldx #3
      .cl1
        lda coords,x
        sta+1 label_6b,x
        dex
        bpl .cl1
        jsr label_b49d
        inc coords
        bne start
        inc coords+1
        lda coords+1
        cmp #$c8
        bne start

        ; And now the opposite case
      start2
        ldx #3
      .cl2
        lda coords2,x
        sta label_6b,x
        dex
        bpl .cl2
        jsr label_b49d
        inc coords2
        bne start2
        inc coords2+3
        lda coords2+3
        cmp #$c8
        bne start2
        rts
      excludeEnd
      """
    And I run the command line: ..\C64\acme.exe --setpc $400 -o target\eliteinit.prg --labeldump target\eliteinit.lbl -f cbm target\eliteinit.a
    And I load prg "target\eliteinit.prg"
    And I load labels "target\eliteinit.lbl"
    Then profile exclude memory range from excludeStart to excludeEnd
    Given C64 video display does not save debug BMP images
    Given I disable trace
    When I execute the procedure at start until return
    And C64 video display saves debug BMP images to leaf filename "target/frames/TC-25-C64-2-"
    And render a C64 video display frame
    Then expect image "target/frames/TC-25-C64-1-029720.bmp" to be identical to "target/frames/TC-25-C64-2-029720.bmp"


  @TC-26
  Scenario: Run Elite's line draw
    Given clear all external devices
    Given a new C64 video display
    And show C64 video window
    And C64 video display saves debug BMP images to leaf filename "target/frames/TC-26-C64-"
    And force C64 displayed bank to 2

    Given I have a simple overclocked 6502 system
    Given I am using C64 processor port options
    Given add C64 hardware
    When I enable uninitialised memory read protection with immediate fail
    Given I disable trace

    # Same as the game code initialisation
    Given I write memory at $01 with $35
    Given I write memory at $d011 with $3b
    Given I write memory at $d018 with $81
    Given I write memory at $d020 with 2

    And I create file "target\elitedemo2.a" with
      """
      !sal
        jmp RealStart
      !source "target\elitelinedraw.a"
      RealStart
        sei
        ldx #$ff
        txs
        lda #$35
        sta $01
        lda #$3b
        sta $d011
        lda #$81
        sta $d018
        lda #2
        sta $dd00
        lda #2
        sta $d020
      .l1
        jsr start2
        jmp .l1
      !source "target\elitedemo.a"


      * = $4000
        !fill $2000 , 0
      * = $6000
        !fill (40*25) , $10
      """
    And I run the command line: ..\C64\acme.exe -v9 --setpc $400 -o target\elitedemo.prg --labeldump target\elitedemo.lbl -f cbm target\elitedemo2.a
    And I load prg "target\elitedemo.prg"
    And I load labels "target\elitedemo.lbl"
#    Given C64 video display does not save debug BMP images
    Given I disable trace
#    When I execute the procedure at start until return
    When I execute the procedure at start2 until return for 1000 iterations

    # c:\work\c64\bin\LZMPi.exe -c64mbu c:\work\BDD6502\target\elitedemo.prg c:\temp\ed.prg $400


    @TC-27
      Scenario: Convert simple Acme source to Kick Assembler source syntax
        Given I create file "target\test-TC-26-1.a" with
        """
        label_6c = $6c
        label_9d00 = label_9c01 - 1 ; Table start skipped
        * = $1234
        label_9c01	!by $00
          ; Note spaces and tabs
          !by $20
        	!by $21
        	!fill 9
        """
      When converting simple ACME syntax in file "target\test-TC-26-1.a" to Kick Assembler output file "target\test-TC-26-1.asm"
      Then open file "target\test-TC-26-1.asm" for reading
      And ignoring empty lines
      # https://www.theweb.dk/KickAssembler/webhelp/content/ch03s05.html
      Then expect the next line to contain ".label label_6c = $6c"
      Then expect the next line to contain ".label label_9d00 = label_9c01 - 1 // Table start skipped"
      Then expect the next line to contain "* = $1234"
      Then expect the next line to contain "label_9c01: .byte $00"
      Then expect the next line to contain "// Note spaces and tabs"
      Then expect the next line to contain ".byte $20"
      Then expect the next line to contain ".byte $21"
      Then expect the next line to contain ".fill 9,0"




  # Using information from here: https://github.com/martinpiper/DebuggingDetails/blob/main/Last%20Ninja%203.txt
  @TC-28
  Scenario: Analyse Last Ninja 3 map draw

    And I create file "target\test1.a" with
    """
    !sal
    *=$5600
    start
    ;  jsr $7047
      jsr $6e47
      ; Copy the background colour
      lda $c0
      sta $d021
      rts
    end
    """
    And I run the command line: ..\C64\acme.exe -o test.prg --labeldump test.lbl -f cbm target\test1.a

    Given clear all external devices
    Given a new C64 video display
    And show C64 video window
    And C64 video display saves debug BMP images to leaf filename "target/frames/TC-28-C64-1-"
    And force C64 displayed bank to 0

    Given I have a simple overclocked 6502 system
    Given I am using C64 processor port options
    Given add C64 hardware
    When I enable uninitialised memory read protection with immediate fail
    Given I disable trace
    And I load prg "C:\temp\ln3.prg"
    Given I fill memory from $e000 to $10000 exclusive with $00
    And I load prg "test.prg"
    And I load labels "test.lbl"

    # Same as the game code initialisation
    Given I write memory at $01 with $35
    Given I write memory at $d011 with $3b
    Given I write memory at $d016 with $18
    Given I write memory at $d018 with $39
    Given I write memory at $d020 with 2
    Given I write memory at $d021 with 0

    Given enable memory profiling
    Given memory profile record writes from 0xe000 to 0xffff

    Given C64 cycles to pixels multiplier is 0

    # For debugging purposes
#    Given disable memory profiling
#    Given disable memory profiling validation
#    Given I enable trace with indent
    Given I write memory at $e3 with 0
#    Given I write memory at $0803 with $6f

    Given I write memory at $e3 with 0
    When I execute the procedure at start until return
    And render a C64 video display frame
    Given I write memory at $e3 with 1
    When I execute the procedure at start until return
    And render a C64 video display frame
    Given I write memory at $e3 with 2
    When I execute the procedure at start until return
    And render a C64 video display frame
    Given I write memory at $e3 with 3
    When I execute the procedure at start until return
    And render a C64 video display frame
    Given I write memory at $e3 with 4
    When I execute the procedure at start until return
    And render a C64 video display frame
    Given I write memory at $e3 with 5
    When I execute the procedure at start until return
    And render a C64 video display frame
    Given I write memory at $e3 with 6
    When I execute the procedure at start until return
    And render a C64 video display frame
    Given I write memory at $e3 with 7
    When I execute the procedure at start until return
    And render a C64 video display frame
    Given I write memory at $e3 with 8
    When I execute the procedure at start until return
    And render a C64 video display frame
    Given I write memory at $e3 with 9
    When I execute the procedure at start until return
    And render a C64 video display frame
    Given I write memory at $e3 with $a
    When I execute the procedure at start until return
    And render a C64 video display frame
    # Causes memory at $f0bb to be accessed during the map draw
#    Given I write memory at $e3 with $d
#    When I execute the procedure at start until return
#    And render a C64 video display frame


#    Then include profile last access
#    Then include profile index register type
#    Then include profile index range
#    Then include profile write hint

    Then include profile branch not taken
#    Then profile use fill instead of PC adjust
    Then profile set PC adjust limit to 256 bytes
    Then profile always set PC when moving from code to data
    Then profile always set PC when moving from data to code
    Then profile avoid PC set in code
    Then profile avoid PC adjust in code
    Then profile avoid PC set in data
    Then profile avoid PC adjust in data
    Then profile preserve data spacing from 0x0800 to 0x5900
    Then profile exclude branches not taken
    # Less aggressive than all not taken branches
#    Then profile exclude blind branches not taken
#    Then profile output never accessed as a 0 byte
    Then profile exclude memory range from start to end
    # Exclude graphics memory in the last VIC bank
    Then profile exclude memory range from $c000 to $ffff
    Then profile optimise labels
    # Reload the data so the memory is exactly the same before profiled execution
    And I load prg "C:\temp\ln3.prg"
    Then output profile disassembly to file "target\LastNinja3MapDraw.a"

    # Now test the disassembled code to make sure it works when relocated elsewhere
    Given clear all external devices
    Given a new C64 video display
    And show C64 video window
    And C64 video display saves debug BMP images to leaf filename "target/frames/TC-28-C64-2-"
    And force C64 displayed bank to 0

    Given I have a simple overclocked 6502 system
    Given I am using C64 processor port options
    Given add C64 hardware
    When I enable uninitialised memory read protection with immediate fail
#    And I load prg "C:\temp\ln3.prg"

    And I create file "target\test1.a" with
    """
    !sal
    *=$200
    label_e000 = $e000
    label_d000 = $d000
    !source "target\LastNinja3MapDraw.a"
    *=$5600
    start
    ;  jsr $7047
      jsr label_6e47
      ; Copy the background colour
      lda label_c0
      sta $d021
      rts
    """

    And I run the command line: ..\C64\acme.exe -o target\LastNinja3MapDraw.prg --labeldump target\LastNinja3MapDraw.lbl -f cbm target\test1.a
    And I load prg "target\LastNinja3MapDraw.prg"
    And I load labels "target\LastNinja3MapDraw.lbl"
    Given I fill memory from $e000 to $10000 exclusive with $00

#    Given I enable with indent

    # Same as the game code initialisation
    Given I write memory at $01 with $35
    Given I write memory at $d011 with $3b
    Given I write memory at $d016 with $18
    Given I write memory at $d018 with $39
    Given I write memory at $d020 with 2
    Given I write memory at $d021 with 0

    Given I write memory at $c2 with $00
#    Given I write memory at $c3 with $0f
#    Given I write memory at $c4 with $0b
#    Given I write memory at $c5 with $0c

    Given enable memory profiling
    Given memory profile record writes from 0xe000 to 0xffff
    Given enable memory profiling validation

#    Given I enable trace with indent
    Given I write memory at label_e3 with 0
    When I execute the procedure at start until return
    And render a C64 video display frame
    Given I write memory at label_e3 with 1
    When I execute the procedure at start until return
    And render a C64 video display frame
    Given I write memory at label_e3 with 2
    When I execute the procedure at start until return
    And render a C64 video display frame
    Given I write memory at label_e3 with 3
    When I execute the procedure at start until return
    And render a C64 video display frame

    Then expect image "target/frames/TC-28-C64-1-000001.bmp" to be identical to "target/frames/TC-28-C64-2-000001.bmp"
    Then expect image "target/frames/TC-28-C64-1-000003.bmp" to be identical to "target/frames/TC-28-C64-2-000003.bmp"
    Then expect image "target/frames/TC-28-C64-1-000005.bmp" to be identical to "target/frames/TC-28-C64-2-000005.bmp"
    Then expect image "target/frames/TC-28-C64-1-000007.bmp" to be identical to "target/frames/TC-28-C64-2-000007.bmp"

  # Using information from here: https://github.com/martinpiper/DebuggingDetails/blob/main/Last%20Ninja%203.txt
  @TC-28-2
  Scenario: Last Ninja 3 map draw with some custom data

    And I create file "target\test1.a" with
    """
    !sal
    *=$5600
    start
    ;  jsr $7047
      jsr $6e47
      ; Copy the background colour
      lda $c0
      sta $d021
      rts
    end
    """
    And I run the command line: ..\C64\acme.exe -o test.prg --labeldump test.lbl -f cbm target\test1.a

    Given clear all external devices
    Given a new C64 video display
    And show C64 video window
    And C64 video display saves debug BMP images to leaf filename "target/frames/TC-28-2-C64-1-"
    And force C64 displayed bank to 0

    Given I have a simple overclocked 6502 system
    Given I am using C64 processor port options
    Given add C64 hardware
    When I enable uninitialised memory read protection with immediate fail
    Given I disable trace
    And I load prg "C:\temp\ln3.prg"
    Given I fill memory from $e000 to $10000 exclusive with $00
    And I load prg "test.prg"
    And I load labels "test.lbl"

    # Same as the game code initialisation
    Given I write memory at $01 with $35
    Given I write memory at $d011 with $3b
    Given I write memory at $d016 with $18
    Given I write memory at $d018 with $39
    Given I write memory at $d020 with 2
    Given I write memory at $d021 with 0

    Given C64 cycles to pixels multiplier is 0

    # For debugging purposes
#    Given disable memory profiling
#    Given disable memory profiling validation
    Given I enable trace with indent
    Given I write memory at $e3 with 0
#    Given I write memory at $0803 with $6f

    And I start writing memory at $2d24
    # Two paths and two walls...
    # 6a = path
    # 6f = wall
    And I write the following hex bytes
      | 6a 10 0f 6a 10 14 6f 0f 0e 6f 16 0d ff |

    When I execute the procedure at start until return
    And render a C64 video display frame

    And I start writing memory at $2d24
    # Remaps the black colour, in the second object, to white
    And I write the following hex bytes
      | 65 0e 0f 65 1e 2f 10 ff |

    When I execute the procedure at start until return
    And render a C64 video display frame

    And I start writing memory at $2d24
    # Three gravel ($17), second one X flip, third using $39 remaps colour $f to $0 (light grey to black)
    And I write the following hex bytes
      | 17 10 19 17 98 19 17 1c 39 0f ff |

    When I execute the procedure at start until return
    And render a C64 video display frame
