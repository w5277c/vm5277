; vm5277.avr_codegen v0.1 at Fri Nov 14 05:22:28 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/mfin_sf.asm"
.include "stdio/out_num16.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta21:
	.db 13,0

j8bCMainMmain:
	push yl
	push yh
	lds yl,SPL
	lds yh,SPH
	push c0x00
	push c0x00
	ldi r20,0
	ldi r21,0
	add r20,C0x01
	adc r21,C0x00
	ldi r22,15
	ldi r23,0
	add r22,C0x01
	adc r23,C0x00
	ldi r24,255
	ldi r25,0
	add r24,C0x01
	adc r25,C0x00
	std y+0,c0xff
	ldi r19,15
	subi yl,low(33)
	sbci yh,high(33)
	std y+32,r19
	ldd r19,y+33
	add r19,C0x01
	std y+33,r19
	ldd r19,y+32
	adc r19,C0x00
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
	rcall os_out_num16
	ldi r30,0
	rjmp j8bproc_mfin_sf
