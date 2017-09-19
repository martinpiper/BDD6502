Feature: Extra opcodes test

  This puts some code that jumps through multiples levels and test a,x,y are the right values.
  It also does a rts push call to test the INR opcode.

  Scenario: test extra opcodes
    Given I have a simple overclocked 6502 system
    And I start writing memory at $400
    And I write the following hex bytes
        | a2 01 a0 01 a9 01 02 12 |
        | 22 20 10 04 32 42 52 60 |
        | a2 02 a0 02 a9 02 02 12 |
        | 22 62 a9 04 48 a9 22 48 |
        | a9 02 60 32 42 52 a2 01 |
        | a0 01 a9 01 60 |  
    When I execute the procedure at $400 for no more than 31 instructions
    