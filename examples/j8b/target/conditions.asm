; vm5277.avr_codegen v0.1 at Thu Sep 04 03:28:35 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_WELCOME = 1
.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "stdio/out_char.asm"

Main:
	rjmp j8bCMainMmain

j8bCmainMmain:
	ldi r16,65
	rcall os_out_char
	ldi r16,67
	rcall os_out_char
	ldi r16,68
	rcall os_out_char
	ldi r20,1
	ldi r21,2
	mov r16,r21
	add r16,r20
	cpi r16,2
	brcc _j8b_eoc26
	ldi r16,70
	rcall os_out_char
	rjmp _j8b_eob28
_j8b_eoc26:
	ldi r16,71
	rcall os_out_char
_j8b_eob28:
	ret
