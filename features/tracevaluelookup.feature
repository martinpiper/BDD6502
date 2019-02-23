Feature: Does tests to show the extra trace format

  Which there is no way to verify alas...

  Scenario: I have nonsensical trace 
  	Given I have a simple 6502 system
  	Given I am using C64 processor port options
    And I create file "test.a" with
      """
      *=$400
      start
        lda #$37
        sta 1
        sta $d020
        lda #$00
        sta $02
        lda #$50
        sta $03
        lda #$80
        sta $5000
        lda #$40
        sta $04
        ldy #0
        lda ($02),y
        and $04
        sta ($02),y
        rts
      """
	And I run the command line: ../C64/acme.exe -o test.prg --labeldump test.lbl -f cbm test.a
	And I load prg "test.prg"
	And I load labels "test.lbl"
	And I enable trace with indent
	When I execute the procedure at 1024 for no more than 5000 instructions
	