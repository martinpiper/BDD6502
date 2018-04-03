Feature: PrintIO Device code test

  This checks the PrintIO device is working

  Scenario: Simple code test for PrintIO
    Given I have a simple 6502 system
    Given I install PrintIODevice at $d700
    And I create file "test.a" with
  """
  *=$400
  start
	sta $d705 ; flush
    lda #0
    sta $D701
    lda #$AA
   sta $D702
    lda #$12
    sta $D703
    lda #$34
    sta $d704
    sta $d705 ; flush
    ldx #0
loop
	lda textStart,x
	sta $D700
	inx
	cpx #textEnd-textStart
	bne loop
	sta $d705 ; flush
    rts
textStart
!tx "hello world"
!by 13
textEnd
  """
    And I run the command line: ../C64/acme.exe -o test.prg --labeldump test.lbl -f cbm test.a
    And I load prg "test.prg"
    And I load labels "test.lbl"
   When I execute the procedure at 1024 for no more than 1000 instructions