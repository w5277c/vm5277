; vm5277.avr_codegen v0.1 at Sun Sep 28 10:25:31 VLAT 2025
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "math/mul16.asm"
.include "math/mulq7n8.asm"
.include "math/div16.asm"
.include "math/divq7n8.asm"
.include "stdio/out_num16.asm"
.include "stdio/out_q7n8.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta20:
	.db 12,0

j8bCMainMmain:
	ldi r20,0
	ldi r21,0
	ldi r20,10
	ldi r21,0
	ldi r22,0
	ldi r23,0
	ldi r22,0
	ldi r23,6
	ldi r24,0
	ldi r25,0
	ldi r24,128
	ldi r25,1
	movw r16,r20
	subi r16,254
	sbci r17,255
	rcall os_out_num16
	movw r16,r20
	mov r17,r16
	clr r16
	subi r16,128
	sbci r17,254
	rcall os_out_q7n8
	movw r16,r22
	subi r16,128
	sbci r17,254
	rcall os_out_q7n8
	movw r16,r20
	subi r16,2
	sbci r17,0
	rcall os_out_num16
	movw r16,r20
	mov r17,r16
	clr r16
	subi r16,128
	sbci r17,1
	rcall os_out_q7n8
	movw r16,r22
	subi r16,128
	sbci r17,1
	rcall os_out_q7n8
	movw r16,r20
	ldi r18,2
	ldi r19,0
	rcall os_mul16
	rcall os_out_num16
	movw r16,r20
	mov r17,r16
	clr r16
	ldi r18,128
	ldi r19,1
	rcall os_mulq7n8
	rcall os_out_q7n8
	movw r16,r22
	ldi r18,128
	ldi r19,1
	rcall os_mulq7n8
	rcall os_out_q7n8
	movw r16,r20
	push r24
	push r25
	ldi r18,2
	ldi r19,0
	rcall os_div16
	pop r25
	pop r24
	rcall os_out_num16
	movw r16,r20
	mov r17,r16
	clr r16
	push r24
	push r25
	ldi r18,128
	ldi r19,1
	rcall os_divq7n8
	pop r25
	pop r24
	rcall os_out_q7n8
	movw r16,r22
	push r24
	push r25
	ldi r18,128
	ldi r19,1
	rcall os_divq7n8
	pop r25
	pop r24
	rcall os_out_q7n8
	movw r16,r20
	push r24
	push r25
	ldi r18,2
	ldi r19,0
	rcall os_div16
	movw r16,r24
	pop r25
	pop r24
	rcall os_out_num16
	movw r16,r24
	add r16,r22
	adc r17,r23
	rcall os_out_q7n8
	movw r16,r22
	sub r16,r24
	sbc r17,r25
	rcall os_out_q7n8
	movw r16,r24
	movw r18,r22
	rcall os_mulq7n8
	rcall os_out_q7n8
	movw r16,r22
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
	push r24
	push r25
	rcall os_divq7n8
	pop r25
	pop r24
_j8b_ediv25:
	rcall os_out_q7n8
	ret
