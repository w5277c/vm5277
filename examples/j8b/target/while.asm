; vm5277.avr_codegen v0.1 at Fri Nov 14 05:52:18 VLAT 2025
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/mfin.asm"
.include "stdio/out_num8.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta19:
	.db 12,0

j8bCMainMmain:
	ldi r20,3
_j8b_loop34:
	cpi r20,10
	brcc _j8b_eoc0
	mov r16,r20
	rcall os_out_num8
	add r20,C0x01
	cpi r20,5
	brne _j8b_loop34
_j8b_eoc0:
_j8b_loop35:
	mov r16,r20
	rcall os_out_num8
	rjmp _j8b_loop35
_j8b_loop37:
	rjmp _j8b_loop37
