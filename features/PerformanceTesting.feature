Feature:  Code performance test

  This tests expected performance of code

  Scenario: Simple performance test
    Given I have a simple overclocked 6502 system
    And I create file "test.a" with
      """
      !sal
      *=$400
      start
        dex
        bne start
        rts
      """
    And I run the command line: ..\C64\acme.exe -v3 -o test.prg --labeldump test.lbl -f cbm test.a
    Then property "test.BDD6502.lastProcessOutput" must contain string "Parsing source file 'test.a'"


    And I load prg "test.prg"
    And I load labels "test.lbl"

    Given I enable trace with indent

    Given I reset the cycle count
    Given I set register X to 2
    When I execute the procedure at start until return
    Then I expect register X equal 0
    Then I expect the cycle count to be no more than 14 cycles


    Given I reset the cycle count
    Given I set register X to 3
    When I execute the procedure at start until return
    Then I expect register X equal 0
    Then I expect the cycle count to be no more than 18 cycles


    # Cumulative cycle count check
    Given I reset the cycle count
    Given I set register X to 2
    When I execute the procedure at start until return
    Given I set register X to 3
    When I execute the procedure at start until return
    Then I expect the cycle count to be no more than 32 cycles
    Given set the variable "test.cycles" equal to the cycle count
    Then assert that "${test.cycles}" contains text "32"

    Given I disable trace
