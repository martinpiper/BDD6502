Feature: Memory range checking

  Scenario: Memory range check 1
    Given I have a simple 6502 system
    Given I start writing memory at $e000
    And I write the following hex bytes
      | 00 01 02 03 04 05 06 07 |
      | 08 09 0a 0b 0c 0d 0e 0f |
      | 10 11 12 13 14 15 16 17 |

    When I start comparing memory at $e009
    And I assert the following hex bytes are the same
      | 09 0a 0b 0c 0d 0e |
      | 0f 10 11 12 13 14 |


    When I start comparing memory at $e009
    Then I assert the following bytes are the same
      | $09 | $0a | $0b | lo(0x400c) | hi(0x0d12) |
