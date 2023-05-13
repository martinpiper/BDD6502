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
    When automation click current menu item "Run Simulation.*F12"
    Then automation wait for window close
