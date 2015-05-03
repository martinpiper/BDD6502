Feature:  Maxchine state test

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
  Then I expect register A to equal 12
  And I expect register X to equal 14
  And I expect register Y to equal 17
  # stC = 1
  # stZ = 2
  # stI = 4
  # stD = 8
  # stV = 64
  # stN = 128
  And I expect register ST to equal stZ
  # Performs a logical bit set test
  And I expect register ST to contain stZ
