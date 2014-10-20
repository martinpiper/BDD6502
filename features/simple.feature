Feature: Simple 6502 code test

  Simple 6502 code test that runs a simple procedure (sei, inc $d020, rts ) and tests expected memory

Scenario: Simple code test
  Given I have a simple 6502 system
  And I start writing memory at $400
  And I write the following hex bytes
      | 78 ee 20 d0 60 4c 01 04 |
      | 00 01 02 03 04 05 06 07 |
  And I write memory at $c000 with 12
  And I setup a 16 byte stack slide
  When I execute the procedure at $400 for no more than 3 instructions
  Then I expect to see $d020 contain 1
  And I expect to see $409 contain 1
  And I expect to see $40f contain 7
  And I expect to see 49152 contain $c


Scenario: Demonstrate the 6502 simulator state is preserved between scenarios
  # The previous scenario's state is preserved here so test it again
  Then I expect to see $d020 contain 1
  And I expect to see $409 contain 1
  And I expect to see $40f contain 7
  When I fill memory with $ff
  Then I expect to see $c000 contain $ff
