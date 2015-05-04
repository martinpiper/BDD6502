Feature: Simple 6502 code test

  Simple 6502 code test that runs a simple procedure (sei, inc $d020, rts, jmp $401 ) and tests expected memory.

  This ensures the BDD6502 library is included and initialised properly.

Scenario: Simple code test
  Given I have a simple 6502 system
  And I start writing memory at $400
  And I write the following hex bytes
      | 78 ee 20 d0 60 4c 01 04 |
      | 00 01 02 03 04 05 06 07 |
  And I start writing memory at $c100
  And I write the following bytes
    | 49152 + 12 |
    | low(49152 + 12) |
    | hi(49152 + 12) |
    | 256 - 3 |
  And I write memory at $c000 with 12
  And I setup a 16 byte stack slide
  When I execute the procedure at $400 for no more than 3 instructions
  Then I expect to see $d020 equal 1
  And I expect to see $409 equal 1
  And I expect to see $40f equal 7
  And I expect to see 49152 equal $c
  And I expect to see $c100 equal 12
  And I expect to see $c101 equal 12
  And I expect to see $c102 equal 192
  And I expect to see $c103 equal 253



Scenario: Demonstrate the 6502 simulator state is preserved between scenarios
  # The previous scenario's state is preserved here so test it again
  Then I expect to see $d020 equal 1
  And I expect to see $409 equal 1
  And I expect to see $40f equal 7
  When I fill memory with $ff
  Then I expect to see $c000 equal $ff



Scenario: Demonstrate evaluation of parameters
  When I write memory at $c000 + 12 - 3 with 12 + 7
  Then I expect to see 49161 equal 19




Scenario: Demonstrate hex dump of specific memory
  When I hex dump memory between $c000 and $c020
  When I hex dump memory between $c080 and $c100
