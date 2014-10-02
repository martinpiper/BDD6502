Feature: Simple 6502 code test

    Simple 6502 code test that runs a simple procedure (sei, inc $d020, rts ) and tests expected memory

Scenario: Bar
    Given I have a simple 6502 system
    And I fill memory with $00
    And I start writing memory at $400
    And I write the following hex bytes
        | 78 ee 20 d0 60 4c 01 04 |
        | 00 01 02 03 04 05 06 07 |
    And I setup a 16 byte stack slide
    When I execute the procedure at $400
    Then I expect to see $d020 contain 1
    And I expect to see $409 contain 1
    And I expect to see $40f contain 7
