Feature: Automates Windows processes

  Scenario: Test syntax with notepad
#    Given starting an automation process "notepad.exe" with parameters "c:\temp\t.txt"
#    When automation wait for idle
#    When automation find window from pattern ".* Notepad"
#    Given starting an automation process "cmd" with parameters "/c C:\work\BombJack\APU.pdsprj"
    Given starting an automation process "cmd" with parameters: /c "C:\work\BombJack\APU.pdsprj"
    When automation find window from pattern ".*APU.*Proteus.*"
    When automation focus window
    When automation expand main menu item "Debug"
#    When automation scan the entire desktop
    When I run the command line ignoring return code: java.exe -jar target\BDD6502-1.0.9-SNAPSHOT-jar-with-dependencies.jar --scan
    When automation click current menu item "Run Simulation"
    Then automation wait for window close
