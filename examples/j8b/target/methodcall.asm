; vm5277.avr_codegen v0.1 at Sat Sep 27 09:23:32 VLAT 2025
.equ stdout_port = 18

.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "stdio/out_num8.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta30:
	.db 13,0

j8bCMainMmain:
	push zl
	push zl
	ldi r30,3
	push r30
	ldi r30,8
	push r30
	rcall j8bC32CMainMadd
	pop j8b_atom
	pop j8b_atom
	pop zl
	ldi r30,3
	push r30
	push r16
	rcall j8bC32CMainMadd
	pop j8b_atom
	pop j8b_atom
	pop zl
	rcall os_out_num8
	ret

j8bC32CMainMadd:
	push yl
	push yh
	lds yl,SPL
	lds yh,SPH
	ldd r16,y+5
	push zl
	ldd zl,y+6
	add r16,zl
	pop zl
	ret
