; vm5277.avr_codegen v0.1 at Fri Nov 14 05:48:49 VLAT 2025
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/mfin.asm"
.include "j8b/mfin_sf.asm"
.include "stdio/out_num8.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta21:
	.db 13,0

j8bCMainMmain:
	push r30
	push r31
	ldi r19,3
	push r19
	push r30
	push r31
	ldi r19,3
	push r19
	ldi r19,8
	push r19
	rcall j8bC24CMainMadd
	push r16
	rcall j8bC24CMainMadd
	rcall os_out_num8
	rjmp j8bproc_mfin

j8bC24CMainMadd:
	push yl
	push yh
	lds yl,SPL
	lds yh,SPH
	ldd r16,y+5
	ldd j8b_atom,y+6
	add r16,j8b_atom
	ldi r30,2
	rjmp j8bproc_mfin_sf
