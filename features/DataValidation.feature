Feature: Tests data validation syntax

  Given various input files and validation steps, this tests expected output.

  Scenario: Tests file truncation syntax

    Given I create file "target/in.txt" with
      """
      line 1
      line 2
      line 3
      line 4
      line 5
      line 6
      """

    When processing each line in file "target/in.txt" and only output to file "target/out.txt" lines after finding a line containing "line 3"

    Given open file "target/out.txt" for reading
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
