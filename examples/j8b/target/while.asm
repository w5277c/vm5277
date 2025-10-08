; vm5277.avr_codegen v0.1 at Wed Oct 08 19:44:57 VLAT 2025
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "stdio/out_num8.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta20:
	.db 12,0

j8bCMainMmain:
	ldi r20,3
_j8b_loop43:
	cpi r20,10
	brcc _j8b_eoc24
	cpi r20,5
	breq _j8b_eol23
	mov r16,r20
	rcall os_out_num8
	add r20,C0x01
	rjmp _j8b_loop43
_j8b_eoc24:
_j8b_eol23:

_j8b_loop44:
	mov r16,r20
	rcall os_out_num8
	rjmp _j8b_loop44

_j8b_loop46:
	rjmp _j8b_loop46
	ret
