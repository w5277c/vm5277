; vm5277.avr_codegen v0.2
.equ CORE_FREQ = 16
.equ STDOUT_PORT = 18

.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "math/mul16p2.asm"
.include "math/mulq7n8.asm"
.include "math/div16p2.asm"
.include "math/div16.asm"
.include "math/divq7n8.asm"
.include "stdio/out_num16.asm"
.include "stdio/out_q7n8.asm"

Main:
	jmp j8b_CMainMmain
_j8b_meta_32:
	.db 15,0

j8b_CMainMmain:
	ldi r20,10
	ldi r21,0
	ldi r22,0
	ldi r23,6
	ldi r24,128
	ldi r25,1
	movw r16,r20
	subi r16,254
	sbci r17,255
	call os_out_num16
	movw r16,r20
	mov r17,r16
	ldi r16,0x00
	subi r16,128
	sbci r17,254
	call os_out_q7n8
	movw r16,r22
	subi r16,128
	sbci r17,254
	call os_out_q7n8
	movw r16,r20
	subi r16,2
	sbci r17,0
	call os_out_num16
	movw r16,r20
	mov r17,r16
	ldi r16,0x00
	subi r16,128
	sbci r17,1
	call os_out_q7n8
	movw r16,r22
	subi r16,128
	sbci r17,1
	call os_out_q7n8
	movw r16,r20
	call os_mul16p2_x8
	call os_out_num16
	movw r16,r20
	mov r17,r16
	ldi r16,0x00
	ldi r18,128
	ldi r19,1
	call os_mulq7n8
	call os_out_q7n8
	movw r16,r22
	ldi r18,128
	ldi r19,1
	call os_mulq7n8
	call os_out_q7n8
	movw r16,r20
	call os_div16p2_x8
	call os_out_num16
	movw r16,r20
	mov r17,r16
	ldi r16,0x00
	push r24
	push r25
	ldi r18,128
	ldi r19,1
	call os_divq7n8
	pop r25
	pop r24
	call os_out_q7n8
	movw r16,r22
	push r24
	push r25
	ldi r18,128
	ldi r19,1
	call os_divq7n8
	pop r25
	pop r24
	call os_out_q7n8
	movw r16,r20
	push r24
	push r25
	ldi r18,2
	ldi r19,0
	call os_div16
	movw r16,r24
	pop r25
	pop r24
	call os_out_num16
	movw r16,r24
	add r16,r22
	adc r17,r23
	call os_out_q7n8
	movw r16,r22
	sub r16,r24
	sbc r17,r25
	call os_out_q7n8
	movw r16,r24
	movw r18,r22
	call os_mulq7n8
	call os_out_q7n8
	movw r16,r22
	movw r18,r24
	push r24
	push r25
	call os_divq7n8
	pop r25
	pop r24
	call os_out_q7n8
	jmp mcu_halt
