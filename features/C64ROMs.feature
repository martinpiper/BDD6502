Feature: C64 ROM tests

  This checks the ROM addition syntax

  Scenario: Simple code test for C64 ROMs
    Given I have a simple overclocked 6502 system
    Given a ROM from file "..\..\VICE\C64\kernal" at $e000
    Given a ROM from file "..\..\VICE\C64\basic" at $a000
    Given add a C64 VIC

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
