; vm5277.avr_codegen v0.1 at Mon Aug 18 03:25:34 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "stdio/out_char.asm"

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
	ldi r16,65
	mcall os_out_char
	ldi r16,67
	mcall os_out_char
	ldi r16,68
	mcall os_out_char
	ldi r20,70
	mov r16,r20
	mcall os_out_char
	ldi r22,1
	ldi r21,2
	mov r16,r22
	add r16,r21
	cpi r16,2
	rol r16
	ldi r16,71
	mcall os_out_char
	ldi r16,72
	mcall os_out_char
	ret
if_begin:
	brne if_else
if_then:
	rjmp if_end
if_else:
if_end:
