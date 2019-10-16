Feature:  Machine state test

  This tests expected machine state

  Scenario: Simple machine state test
    Given I have a simple 6502 system
    And I create file "test.a" with
  """
  !sal
  *=$400
  start
    lda #0
    php
    lda #12
    ldx #14
    ldy #17
    plp
    rts
  """
    And I run the command line: ..\C64\acme.exe -o test.prg --labeldump test.lbl -f cbm test.a
    And I load prg "test.prg"
    And I load labels "test.lbl"

    When I execute the procedure at start for no more than 100 instructions

    # Note how the label "start" is used below and correctly resolves to be $400 when checking memory
    Then I expect register A equal 12
    And I expect register X equal 14
    And I expect register Y equal 17
    # stC = 1
    # stZ = 2
    # stI = 4
    # stD = 8
    # stV = 64
    # stN = 128
    And I expect register ST equal stZ
    # Performs a logical bit set test
    And I expect register ST contain stZ
    And I expect register ST exclude stI


  Scenario: Simple machine state write test
    Given I have a simple 6502 system
    And I create file "test.a" with
  """
  !sal
  *=$400
  start
    sta $500
    stx $501
    sty $502
    php
    pla
    sta $503
    rts
  """
    And I run the command line: ..\C64\acme.exe -o test.prg --labeldump test.lbl -f cbm test.a
    And I load prg "test.prg"
    And I load labels "test.lbl"

    When I set register A to 1
    And I set register X to 2
    And I set register Y to 3
    And I set register ST to stZ
    And I execute the procedure at start for no more than 100 instructions

    # Note how the label "start" is used below and correctly resolves to be $400 when checking memory
    Then I expect to see $500 equal 1
    And I expect to see $501 equal 2
    And I expect to see $502 equal 3
    And I expect to see $503 contain stZ
    And I expect to see $503 equal $32


  Scenario: Machine status register tests
    Given I have a simple 6502 system

    When I set register ST to 0
    Then I expect register ST equal 0

    When I set register ST to 1
    Then I expect register ST equal stC

    When I set register ST to 2
    Then I expect register ST equal stZ

    When I set register ST to 4
    Then I expect register ST equal stI

    When I set register ST to 8
    Then I expect register ST equal stD

    When I set register ST to 64
    Then I expect register ST equal stV

    When I set register ST to 128
    Then I expect register ST equal stN

    When I set register ST to 128+64
    Then I expect register ST equal stN+stV
    Then I expect register ST contain stN+stV
    Then I expect register ST contain stN
    Then I expect register ST contain stV


    When I set register ST to 2+4
    Then I expect register ST contain stZ
    Then I expect register ST contain stI
    Then I expect register ST equal stI+stZ
    Then I expect register ST contain stI+stZ
