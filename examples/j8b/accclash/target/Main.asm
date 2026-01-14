; vm5277.avr_codegen v0.2
.equ CORE_FREQ = 16
.equ STDOUT_PORT = 18

.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "stdio/out_num16.asm"

Main:
	jmp j8b_CMainMmain
_j8b_meta_32:
	.db 15,0

j8b_CMainMmain:
	push r28
	push r29
	lds r28,SPL
	lds r29,SPH
	push c0x00
	push c0x00
	ldi r20,0
	ldi r21,0
	add r20,c0x01
	adc r21,c0x00
	ldi r22,15
	ldi r23,0
	add r22,c0x01
	adc r23,c0x00
	ldi r24,255
	ldi r25,0
	add r24,c0x01
	adc r25,c0x00
	std y+0,c0xff
	ldi r19,15
	subi yl,low(33)
	sbci yh,high(33)
	std y+32,r19
	ldd r19,y+33
	add r19,c0x01
	std y+33,r19
	ldd r19,y+32
	adc r19,c0x00
	std y+32,r19
	ldd r16,y+33
	ldd r17,y+32
	add r16,r24
	adc r17,r25
	push r17
	push r16
	movw r16,r22
	add r16,r20
	adc r17,r21
	pop j8b_atom
	add r16,j8b_atom
	pop j8b_atom
	adc r17,j8b_atom
	call os_out_num16
	jmp mcu_halt
