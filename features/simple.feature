Feature: Simple 6502 code test

  Simple 6502 code test that runs a simple procedure (sei, inc $d020, rts, jmp $401 ) and tests expected memory.

  This ensures the BDD6502 library is included and initialised properly.

  Scenario: Simple code test
    Given clear all external devices
    Then assert that "($c100 - $c080) <= 160" is true
    Then assert that "($c100 - $c080) <= 60" is false

    Given I have a simple overclocked 6502 system
    When I fill memory with $00
    And I start writing memory at $400
    And I write the following hex bytes
      | 78 ee 20 d0 60 4c 01 04 |
      | 00 01 02 03 04 05 06 07 |
    And I start writing memory at $c100
    And I write the following bytes
      | 49152 + 12      |
      | low(49152 + 12) |
      | hi(49152 + 12)  |
      | 256 - 3         |
    And I write memory at $c000 with 12
    And I setup a 16 byte stack slide
    When I reset the cycle count
    When I execute the procedure at $400 for no more than 3 instructions
    Then I expect the cycle count to be no more than 14 cycles
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
    Then property "test.BDD6502.lastHexDump" must contain string "c000: ff ff ff ff ff ff ff ff  ff 13 ff ff ff ff ff ff"
    Then property "test.BDD6502.lastHexDump" must contain string ignoring whitespace "c000:ffffffffffffffff ff13ffffffffffff"
    When I hex dump memory between $c080 and $c100


  Scenario: Simple binary test code
    Given I have a simple 6502 system
    And I start writing memory at $400
    And I write the following hex bytes
      | 11 22 33 44 55 66 77 88 |

    Then I expect to see $400 equal %00010001
    And I expect to see $401 equal %00100010
    And I expect to see $402 equal %00110011
    And I expect to see $403 equal %01000100
    And I expect to see $404 equal %01010101
    And I expect to see $405 equal %01100110
    And I expect to see $406 equal %01110111
    And I expect to see $407 equal %10001000


  Scenario: Simple memory test code
    Given I have a simple 6502 system
    Given I set label foo equal to $400

    When I start writing memory at $400
    And I write the following hex bytes
      | 11 22 33 44 55 66 77 88 |
      | 11 22 33 44 55 66 77 88 |

    Then I expect memory $400+0 to equal memory $400+8+0
    Then I expect memory foo+1 to contain memory foo+8+1
    Then I expect memory $400+0 to exclude memory foo+8+1


  Scenario: Check property syntax
    When property "test.foo" is set to string "bar"
    Then property "test.foo" must contain string "a"
    And property "test.foo" must contain string "bar"



  @ignore @RemoteDebugger
  Scenario: Check remote debugger syntax
    Given I have a simple overclocked 6502 system
    And I enable trace
    And I start writing memory at $400
    And I write the following hex bytes
      | 78 ee 20 d0 20 10 04 4c 01 04 |
    And I start writing memory at $410
    And I write the following hex bytes
      | ee 21 d0 ee 20 d0 60 |

    When enable remote debugging
    And wait for debugger connection
    And wait for debugger command
    When I execute the procedure at $400 until return


  Scenario: Validates expected file data
    Given open file "Done.txt" for reading
    Then expect the next line to contain "; Done items"
    And skip line
    Then expect the next line to contain "Given I am using C64 processor port options"
    Then expect the line to contain "Added: "

  @TC-3-1
  Scenario: Saves memory and validates expected file data
    Given I have a simple overclocked 6502 system
    And I start writing memory at $400
    And I write the following hex bytes
      | 00 01 02 03 04 05 06 07 |
    And I start writing memory at $410
    And I write the following hex bytes
      | 00 01 02 03 04 05 06 07 |
    When save 6502 memory without two byte header from "$400" to "$408" to file "target/mem1.bin"
    When save 6502 memory without two byte header from "$410" to "$418" to file "target/mem2.bin"
    Then assert that file "target/mem1.bin" is binary equal to file "target/mem2.bin"
    Given set property "test.a" equal to the file size of "target/mem1.bin"
    Then assert that "${test.a}" is equal to "8"

    When save 6502 memory with two byte header from "$400" to "$408" to file "target/mem1.bin"
    When save 6502 memory with two byte header from "$400" to "$408" to file "target/mem2.bin"
    Then assert that file "target/mem1.bin" is binary equal to file "target/mem2.bin"
    Given set property "test.a" equal to the file size of "target/mem1.bin"
    Then assert that "${test.a}" is equal to "10"

