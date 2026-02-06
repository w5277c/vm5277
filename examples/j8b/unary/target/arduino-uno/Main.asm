; vm5277.AVR v0.2
.equ CORE_FREQ = 16
.set STDIN_PORT_REGID = 11
.set STDIN_DDR_REGID = 10
.set STDIN_PIN_REGID = 9
.set STDIN_PORTNUM = 3
.set STDIN_PINNUM = 0
.set STDOUT_PORT_REGID = 11
.set STDOUT_DDR_REGID = 10
.set STDOUT_PIN_REGID = 9
.set STDOUT_PORTNUM = 3
.set STDOUT_PINNUM = 1

.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "stdio/out_char.asm"
.include "stdio/out_num16.asm"
.include "stdio/out_bool.asm"
.include "stdio/out_q7n8.asm"


Main:
	jmp j8b_CMainMmain
_j8b_meta_41:
	.db 22,0

j8b_CMainMmain:
	push r28
	push r29
	lds r28,SPL
	lds r29,SPH
	push c0x00
	push c0x00
	ldi r20,0
	ldi r21,0
	ldi r22,0
	ldi r23,0
	ldi r24,2
	ldi r25,1
	ldi r19,4
	std y+0,r19
	ldi r19,3
	subi yl,low(33)
	sbci yh,high(33)
	std y+32,r19
	add r24,c0x01
	adc r25,c0x00
	sub r24,c0x01
	sbc r25,c0x00
	add r24,c0x01
	adc r25,c0x00
	sub r24,c0x01
	sbc r25,c0x00
	movw r16,r24
	add r24,c0x01
	adc r25,c0x00
	std y+33,r16
	std y+32,r17
	movw r16,r24
	sub r24,c0x01
	sbc r25,c0x00
	std y+33,r16
	std y+32,r17
	add r24,c0x01
	adc r25,c0x00
	movw r16,r24
	std y+33,r16
	std y+32,r17
	sub r24,c0x01
	sbc r25,c0x00
	movw r16,r24
	std y+33,r16
	std y+32,r17
	cpi r20,0
	brne _j8b_eoc_43
	ldi r16,1
	rjmp _j8b_eolb_44
_j8b_eoc_43:
	ldi r16,0
_j8b_eolb_44:
	mov r16,r24
	com r16
	mov r17,r25
	com r17
	add r16,c0x01
	adc r17,c0x00
	std y+33,r16
	std y+32,r17
	cpi r20,0
	brne _j8b_eoc_45
	ldi r16,1
	rjmp _j8b_eolb_46
_j8b_eoc_45:
	ldi r16,0
_j8b_eolb_46:
	cpi r20,0
	brne _j8b_eoc_47
	ldi r16,1
	rjmp _j8b_eolb_48
_j8b_eoc_47:
	ldi r16,0
_j8b_eolb_48:
	mov r16,r24
	com r16
	mov r17,r25
	com r17
	add r16,c0x01
	adc r17,c0x00
	mov r17,r16
	ldi r16,0x00
	movw r22,r16
	add r24,c0x01
	adc r25,c0x00
	movw r16,r24
	mov r17,r16
	ldi r16,0x00
	movw r22,r16
	sub r24,c0x01
	sbc r25,c0x00
	movw r16,r24
	mov r17,r16
	ldi r16,0x00
	movw r22,r16
	movw r16,r24
	add r24,c0x01
	adc r25,c0x00
	mov r17,r16
	ldi r16,0x00
	movw r22,r16
	movw r16,r24
	sub r24,c0x01
	sbc r25,c0x00
	mov r17,r16
	ldi r16,0x00
	movw r22,r16
	add r24,c0x01
	adc r25,c0x00
	movw r16,r24
	sub r24,c0x01
	sbc r25,c0x00
	movw r16,r24
	cpi r20,0
	breq _j8b_eolb_50
	cpi r21,0
	breq _j8b_eoc_49
_j8b_eolb_50:
	ldi r16,1
	rjmp _j8b_eolb_51
_j8b_eoc_49:
	ldi r16,0
_j8b_eolb_51:
	call os_out_bool
	ldi r16,10
	call os_out_char
	movw r16,r24
	call os_out_num16
	ldi r16,10
	call os_out_char
	ldd r16,y+33
	ldd r17,y+32
	call os_out_num16
	ldi r16,10
	call os_out_char
	movw r16,r22
	call os_out_q7n8
	ldi r16,10
	call os_out_char
	mov r16,r21
	call os_out_bool
	jmp mcu_halt
