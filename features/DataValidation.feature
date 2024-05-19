Feature: Tests data validation syntax

  Given various input files and validation steps, this tests expected output.

  Scenario: Tests file truncation syntax

    Given I create file "target/in.txt" with
      """
      line 1
      line 2
      line 3
      line 4
      ignore this
      line 5
      ignore this as well
      line 6
      """

    When processing each line in file "target/in.txt" and only output to file "target/out.txt" lines after finding a line containing "line 3"

    Given open file "target/out.txt" for reading
    When ignoring lines that contain "ignore"
    When ignoring empty lines
    Then expect the next line to contain "line 4"
    Then expect the next line to contain "line 5"
    Then expect the next line to contain "line 6"
    Then expect end of file
    Given close current file


  Scenario: Tests file reconciliation syntax

    Given I create file "target/in.txt" with
      """
      line 1
      line 2
      line 3
      line 4
      line 5
      line 6
      """

    Given I create file "target/toMatch.txt" with
      """
      line 1
      line 4
       2
      line 6
      line 3
      """


    When processing each line in file "target/in.txt" and only output to file "target/out.txt" lines that do not contain any lines from "target/toMatch.txt"

    Given open file "target/out.txt" for reading
    Then expect the next line to contain "line 5"
    Then expect end of file


  Scenario: Tests file writing syntax

    Given open file "target/out.txt" for writing
    When write to the file a line "line 1"
    When write to the file a line "line 2"
    When write to the file a line "line 3"
    When close the writing file

    Given open file "target/out.txt" for reading
    Then expect the next line to contain "line 1"
    Then expect the next line to contain "line 2"
    Then expect the next line to contain "line 3"
    Then expect end of file
    Given close current file

  Scenario: Tests syntax for handling typical captured transition signals from the DigitalData model
    Given I create file "target/in.txt" with
      """
      ;@time:0.000096
      ;recordSenseChange:$00000051
      d=PTPA2 | PT_PC | $000000d7
      ;@time:0.000096
      ;recordSenseChange:$00000082
      d=PTPA2 | PT_PC | $00000055
      ;@time:0.000097
      ;recordSenseChange:$00010000
      d=PTPA2 | $00000055
      ;@time:0.000098
      ;recordSenseChange:$00010000
      d=PTPA2 | PT_PC | $00000055
      ;@time:0.000099
      ;recordSenseChange:$00000022
      d=PTPA2 | PT_PC | $00000077
      ;@time:0.000099
      ;recordSenseChange:$00000011
      d=PTPA2 | PT_PC | $00000066
      ;@time:0.000100
      ;recordSenseChange:$00010000
      d=PTPA2 | $00000066
      ;@time:0.000101
      ;recordSenseChange:$00010000
      d=PTPA2 | PT_PC | $00000066
      ;@time:0.000102
      ;recordSenseChange:$00000011
      d=PTPA2 | PT_PC | $00000077
      ;@time:0.000103
      ;recordSenseChange:$00010000
      d=PTPA2 | $00000077
      ;@time:0.000104
      ;recordSenseChange:$00010000
      d=PTPA2 | PT_PC | $00000077
      ;@time:0.000105
      ;recordSenseChange:$00000088
      d=PTPA2 | PT_PC | $000000ff
      ;@time:0.000105
      ;recordSenseChange:$00000077
      d=PTPA2 | PT_PC | $00000088
      ;@time:0.000106
      ;recordSenseChange:$00010000
      d=PTPA2 | $00000088
      ;@time:0.000107
      ;recordSenseChange:$00010000
      d=PTPA2 | PT_PC | $00000088
      """

    Given open file "target/in.txt" for reading
    When ignoring lines that contain ";"
    When ignoring lines that contain "."
    When ignoring empty lines
    Then accounting for transient values expect the next line to contain "d=PTPA2 | PT_PC | $00000055"
    Then accounting for transient values expect the next line to contain "d=PTPA2 | $00000055"
    Then accounting for transient values expect the next line to contain "d=PTPA2 | PT_PC | $00000055"
    Then accounting for transient values expect the next line to contain "d=PTPA2 | PT_PC | $00000066"
    Then accounting for transient values expect the next line to contain "d=PTPA2 | $00000066"
    Then accounting for transient values expect the next line to contain "d=PTPA2 | PT_PC | $00000066"
    Then accounting for transient values expect the next line to contain "d=PTPA2 | PT_PC | $00000077"
    Then accounting for transient values expect the next line to contain "d=PTPA2 | $00000077"
    Then accounting for transient values expect the next line to contain "d=PTPA2 | PT_PC | $00000077"
    Then accounting for transient values expect the next line to contain "d=PTPA2 | PT_PC | $00000088"
    Then accounting for transient values expect the next line to contain "d=PTPA2 | $00000088"
    Then accounting for transient values expect the next line to contain "d=PTPA2 | PT_PC | $00000088"

