Feature: Continue and stop execution test

  This assembles simple code and executes the code until a specific program counter value is reached.
  It then tests continuing the execution and stopping again.

  Scenario: Execute until PC and continue test
    Given I have a simple 6502 system
    And I create file "test.a" with
  """
  !sal
  *=$400
  start
    lda #0      ; 1024
    ldx #$20    ; 1026
  stopHere
    sta $400    ; 1028
    nop
    nop
    nop
  stopHere2
  .l1
    inc $400
    dex
    bne .l1
    rts
  """
    And I run the command line: ..\C64\acme.exe -o test.prg --labeldump test.lbl -f cbm test.a
    And I load prg "test.prg"
    And I load labels "test.lbl"
    Then I expect to see $400 equal $a9
    # The above code is actually 103 instructions long when executing
    When I execute the procedure at start for no more than 2 instructions until PC = stopHere
    # Note how the label "start" is used below and correctly resolves to be $400 when checking memory
    Then I expect to see start equal $a9
    When I continue executing the procedure for no more than 4 instructions until PC = stopHere2
    Then I expect to see start equal 0
    When I continue executing the procedure until return
    Then I expect to see start equal 32
    And I expect to see $402 equal $a2



  Scenario: Execute until PC and continue test2
    Given I have a simple 6502 system
    And I create file "test.a" with
  """
  !sal
  *=$400
  start
    lda #0      ; 1024
    ldx #$20    ; 1026
  stopHere
    sta $400    ; 1028
    nop
    nop
    nop
  stopHere2
  .l1
    inc $400
    dex
    bne .l1
    rts
  """
    And I run the command line: ..\C64\acme.exe -o test.prg --labeldump test.lbl -f cbm test.a
    And I load prg "test.prg"
    And I load labels "test.lbl"
    Then I expect to see $400 equal $a9
    # The above code is actually 103 instructions long when executing
    When I execute the procedure at start for no more than 2 instructions until PC = stopHere
    # Note how the label "start" is used below and correctly resolves to be $400 when checking memory
    Then I expect to see start equal $a9
    When I continue executing the procedure for no more than 4 instructions until PC = stopHere2
    Then I expect to see start equal 0
    When I continue executing the procedure until return
    Then I expect to see start equal 32
    And I expect to see $402 equal $a2

    When I execute the procedure at start for no more than 2 instructions until PC = stopHere
    When I continue executing the procedure for no more than 4 instructions until PC = stopHere2
    # PC never reaches $1000, testing the syntax
    When I continue executing the procedure until return or until PC = $1000
    Then I expect to see start equal 32
