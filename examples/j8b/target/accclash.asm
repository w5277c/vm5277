; vm5277.avr_codegen v0.1 at Thu Sep 25 13:42:36 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "stdio/out_num16.asm"

Main:
	rjmp j8bCMainMmain
;======== enter CLASS Main ========================
_j8b_meta30:
	.db 13,0

;build method void 'Main.main()
j8bCMainMmain:
;build block
;invokeNative void System.setParam [byte, byte], params:LITERAL=0,LITERAL=16
;invokeNative void System.setParam [byte, byte], params:LITERAL=2,LITERAL=18
;invokeNative void System.setParam [byte, byte], params:LITERAL=3,LITERAL=1
;const '0.0'->cells REG[20,21]
	ldi r20,0
	ldi r21,0
;eUnary POST_INC cells REG[20,21]
	add r20,C0x01
	adc r21,C0x00
;const '15.0'->cells REG[22,23]
	ldi r22,15
	ldi r23,0
;eUnary POST_INC cells REG[22,23]
	add r22,C0x01
	adc r23,C0x00
;const '255.0'->cells REG[24,25]
	ldi r24,255
	ldi r25,0
;eUnary POST_INC cells REG[24,25]
	add r24,C0x01
	adc r25,C0x00
;const '4095.0'->cells REG[26,27]
	ldi r26,255
	ldi r27,15
;eUnary POST_INC cells REG[26,27]
	add r26,C0x01
	adc r27,C0x00
;var 's4'->accum
	movw r16,r26
;accum PLUS cells REG[24,25] -> accum
	add r16,r24
	adc r17,r25
;push accum(BE)
	push r17
	push r16
;var 's2'->accum
	movw r16,r22
;accum PLUS cells REG[20,21] -> accum
	add r16,r20
	adc r17,r21
;accum PLUS cells STACK[x2] -> accum
	pop j8b_atom
	add r16,j8b_atom
	pop j8b_atom
	adc r17,j8b_atom
;invokeNative void System.out [short], params:ACCUM
	;load method param
	rcall os_out_num16
;build var short 'Main.main.s1', allocated REG[20,21]
;build var short 'Main.main.s2', allocated REG[22,23]
;build var short 'Main.main.s3', allocated REG[24,25]
;build var short 'Main.main.s4', allocated REG[26,27]
;acc cast null[2]->void[0]
;return, argsSize:0, varsSize:0, retSize:0
	ret
;block end
;method end
;======== leave CLASS Main ========================
