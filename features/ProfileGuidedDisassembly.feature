Feature:  Profile guided disassembly

  This tests the profile guided disassembly generation.
  This executes 6502/6510 code with specific entry parameters, notes memory access for execution and read/write, and then uses this information to generate assembly code.
  Only memory that is accessed is output during the disassembly phase.
  This is useful for reverse engineering code.

  Scenario: Simple profile guided disassembly
    Given I have a simple overclocked 6502 system

    And I load prg "..\DebuggingDetails\MusicSelectSystemPatched_a000.prg"
#    And I load prg "c:\temp\mus1000.prg"

    Given I enable trace with indent

    Given enable memory profiling

    # Note the minimal play routine code: ..\DebuggingDetails\RetrogradeMusicSelectSystem.a
    Given I set register A to 0x7d
    Given I set register X to 0x83
    # This register might not be needed...
    Given I set register Y to 0x01
    When I execute the procedure at 0x8ce until return

#    Given I set register A to 0x00
#    When I execute the procedure at 0x1000 until return

    Given I disable trace
    When I execute the procedure at 0x93d until return for 1000 iterations

#    When I execute the procedure at 0x1003 until return for 1000 iterations
#     # Stop music
#    When I execute the procedure at 0x1006 until return for 1000 iterations

#    Then include profile last access
#    Then include profile index register type
#    Then include profile index range
#    Then include profile write hint
    Then output profile disassembly to file "target\temp.a"

    # cls && c:\work\c64\acme.exe --cpu 6502 -o c:\temp\t.prg -f cbm -v9 c:\work\BDD6502\target\temp.a c:\work\BDD6502\features\MinPlay.a && c:\work\c64\bin\LZMPi.exe -pp $37 -c64mbu c:\temp\t.prg c:\temp\tcmp.prg $c000 && c:\temp\tcmp.prg

