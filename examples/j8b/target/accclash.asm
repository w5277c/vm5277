; vm5277.avr_codegen v0.1 at Thu Sep 04 03:07:27 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_DRAM = 1
.set OS_FT_WELCOME = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "stdio/out_num16.asm"

Main:
	rjmp j8bCMainMmain

j8bCmainMmain:
	ldi r24,1
	ldi r25,0
	ldi r26,16
	ldi r27,0
	ldi r20,0
	ldi r21,1
	ldi r22,0
	ldi r23,16
	mov r16,r26
	mov r17,r27
	add r16,r24
	adc r17,r25
	mov r16,r22
	mov r17,r23
	add r16,r20
	adc r17,r21
	push r17
	push r16
	pop j8b_atom
	add r16,j8b_atom
	pop j8b_atom
	adc r17,j8b_atom
	rcall os_out_num16
	ret
