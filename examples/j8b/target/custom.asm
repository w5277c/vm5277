; vm5277.avr_codegen v0.1 at Fri Sep 26 15:28:10 VLAT 2025
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "math/mul8.asm"
.include "math/mulq7n8.asm"
.include "math/div8.asm"
.include "math/divq7n8.asm"
.include "stdio/out_num16.asm"
.include "stdio/out_q7n8.asm"

Main:
	rjmp j8bCMainMmain
;======== enter CLASS Main ========================
_j8b_meta20:
	.db 12,0

;build method void 'Main.main()
j8bCMainMmain:
;build block
;invokeNative void System.setParam [byte, byte], params:LITERAL=2,LITERAL=18
;const '0.0'->cells REG[20,21]
	ldi r20,0
	ldi r21,0
;const '10.0'->cells REG[20,21]
	ldi r20,10
	ldi r21,0
;const '0.0'->cells REG[22,23]
	ldi r22,0
	ldi r23,0
;const '6.0'->cells REG[22,23]
	ldi r22,0
	ldi r23,6
;const '0.0'->cells REG[24,25]
	ldi r24,0
	ldi r25,0
;const '1.5'->cells REG[24,25]
	ldi r24,128
	ldi r25,1
;var 's1'->accum
	movw r16,r20
;acc cast short[2]->byte[1]
;accum PLUS 2.0 -> accum
	subi r16,254
;invokeNative void System.out [short], params:ACCUM
	;load method param
	rcall os_out_num16
;var 's1'->accum
	movw r16,r20
;acc cast short[2]->fixed[2]
	mov r17,r16
	clr r16
;accum PLUS 1.5 -> accum
	subi r16,128
	sbci r17,254
;invokeNative void System.out [fixed], params:ACCUM
	;load method param
	rcall os_out_q7n8
;var 'f1'->accum
	movw r16,r22
;accum PLUS 1.5 -> accum
	subi r16,128
	sbci r17,254
;invokeNative void System.out [fixed], params:ACCUM
	;load method param
	rcall os_out_q7n8
;var 's1'->accum
	movw r16,r20
;acc cast short[2]->byte[1]
;accum MINUS 2.0 -> accum
	subi r16,2
;invokeNative void System.out [short], params:ACCUM
	;load method param
	rcall os_out_num16
;var 's1'->accum
	movw r16,r20
;acc cast short[2]->fixed[2]
	mov r17,r16
	clr r16
;accum MINUS 1.5 -> accum
	subi r16,128
	sbci r17,1
;invokeNative void System.out [fixed], params:ACCUM
	;load method param
	rcall os_out_q7n8
;var 'f1'->accum
	movw r16,r22
;accum MINUS 1.5 -> accum
	subi r16,128
	sbci r17,1
;invokeNative void System.out [fixed], params:ACCUM
	;load method param
	rcall os_out_q7n8
;var 's1'->accum
	movw r16,r20
;acc cast short[2]->byte[1]
;accum MULT 2.0 -> accum
	ldi ACCUM_H,2
	rcall os_mul8
;invokeNative void System.out [short], params:ACCUM
	;load method param
	rcall os_out_num16
;var 's1'->accum
	movw r16,r20
;acc cast short[2]->fixed[2]
	mov r17,r16
	clr r16
;accum MULT 1.5 -> accum
	ldi ACCUM_EL,128
	ldi ACCUM_EH,1
	rcall os_mulq7n8
;invokeNative void System.out [fixed], params:ACCUM
	;load method param
	rcall os_out_q7n8
;var 'f1'->accum
	movw r16,r22
;accum MULT 1.5 -> accum
	ldi ACCUM_EL,128
	ldi ACCUM_EH,1
	rcall os_mulq7n8
;invokeNative void System.out [fixed], params:ACCUM
	;load method param
	rcall os_out_q7n8
;var 's1'->accum
	movw r16,r20
;acc cast short[2]->byte[1]
;accum DIV 2.0 -> accum
	push TEMP_L
	ldi ACCUM_H,2
	rcall os_div8
	pop TEMP_L
;invokeNative void System.out [short], params:ACCUM
	;load method param
	rcall os_out_num16
;var 's1'->accum
	movw r16,r20
;acc cast short[2]->fixed[2]
	mov r17,r16
	clr r16
;accum DIV 1.5 -> accum
	push TEMP_L
	push TEMP_H
	ldi ACCUM_EL,128
	ldi ACCUM_EH,1
	rcall os_divq7n8
	pop TEMP_H
	pop TEMP_L
;invokeNative void System.out [fixed], params:ACCUM
	;load method param
	rcall os_out_q7n8
;var 'f1'->accum
	movw r16,r22
;accum DIV 1.5 -> accum
	push TEMP_L
	push TEMP_H
	ldi ACCUM_EL,128
	ldi ACCUM_EH,1
	rcall os_divq7n8
	pop TEMP_H
	pop TEMP_L
;invokeNative void System.out [fixed], params:ACCUM
	;load method param
	rcall os_out_q7n8
;var 's1'->accum
	movw r16,r20
;acc cast short[2]->byte[1]
;accum MOD 2.0 -> accum
	push TEMP_L
	ldi ACCUM_H,2
	rcall os_div8
	mov ACCUM_L,TEMP_L
	pop TEMP_L
;invokeNative void System.out [short], params:ACCUM
	;load method param
	rcall os_out_num16
;var 'f2'->accum
	movw r16,r24
;accum PLUS cells REG[22,23] -> accum[FIXED]
	add r16,r22
	adc r17,r23
;invokeNative void System.out [fixed], params:ACCUM
	;load method param
	rcall os_out_q7n8
;var 'f1'->accum
	movw r16,r22
;accum MINUS cells REG[24,25] -> accum[FIXED]
	sub r16,r24
	sbc r17,r25
;invokeNative void System.out [fixed], params:ACCUM
	;load method param
	rcall os_out_q7n8
;var 'f2'->accum
	movw r16,r24
;accum MULT cells REG[22,23] -> accum[FIXED]
	movw r18,r22
	rcall os_mulq7n8
;invokeNative void System.out [fixed], params:ACCUM
	;load method param
	rcall os_out_q7n8
;var 'f1'->accum
	movw r16,r22
;accum DIV cells REG[24,25] -> accum[FIXED]
	movw r18,r24
	tst r18
	brne _j8b_nediv26
	tst r19
	brne _j8b_nediv26
;TODO Division by zero
	ldi r16,0xff
	ldi r17,0xff
	rjmp _j8b_ediv25
_j8b_nediv26:
	push TEMP_L
	push TEMP_H
	rcall os_divq7n8
	pop TEMP_H
	pop TEMP_L
_j8b_ediv25:
;invokeNative void System.out [fixed], params:ACCUM
	;load method param
	rcall os_out_q7n8
;build var short 'Main.main.s1', allocated REG[20,21]
;build var fixed 'Main.main.f1', allocated REG[22,23]
;build var fixed 'Main.main.f2', allocated REG[24,25]
;acc cast null[2]->void[0]
;return, argsSize:0, varsSize:0, retSize:0
	ret
;block end
;method end
;======== leave CLASS Main ========================
