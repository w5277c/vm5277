; vm5277.avr_codegen v0.1 at Mon Aug 18 02:27:05 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_DRAM = 1
.set OS_FT_WELCOME = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "stdio/out_num16.asm"

Main:
	ldi r16,5
	ldi r17,0
	mcall os_dram_alloc
	std z+0,r16
	std z+1,r17
	std z+2,c0xff
	ldi r16,1
	std z+3,r16
	ldi r16,13
	std z+4,r16
	movw r16,zl
	jmp JavlCMainMmain
JavlCmainMmain:
	ldi r26,1
	ldi r27,0
	ldi r24,16
	ldi r25,0
	ldi r22,0
	ldi r23,1
	ldi r20,0
	ldi r21,16
	mov r16,r22
	mov r17,r23
	add r16,r20
	adc r17,r21
	push r17
	push r16
	mov r16,r26
	mov r17,r27
	add r16,r24
	adc r17,r25
	pop javl_atom
	add r16,javl_atom
	pop javl_atom
	adc r17,javl_atom
	mcall os_out_num16
	ret
