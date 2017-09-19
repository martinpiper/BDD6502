* = $0400
ldx #1
ldy #1
lda #1
.byte $2,$12,$22 ; PAT,PXT,PYT
jsr level1
.byte $32,$42,$52 ; TTA,TTX,TTY
rts

level1
ldx #2
ldy #2
lda #2
.byte $2,$12,$22 ; PAT,PXT,PYT
.byte $62 ; INR

lda #>(level2-1)
pha
lda #<(level2-1)
pha
lda #2
rts

level2
.byte $32,$42,$52 ; TTA,TTX,TTY
ldx #1
ldy #1
lda #1
rts