; vm5277.avr_codegen v0.1 at Thu Sep 04 13:55:48 VLAT 2025
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "stdio/out_num8.asm"

Main:
	rjmp j8bCMainMmain

j8bC10CMainMgo:
	lds yl,SPL
	lds yh,SPH
	ldd r16,y+0
	rcall os_out_num8
	ret

j8bCMainMmain:
	lds yl,SPL
	lds yh,SPH
	push_z
	ldi zl,low(_j8b_rp13*2)
	push zl
	ldi zl,high(_j8b_rp13*2)
	push zl
	ldi zl,1
	push zl
	rjmp j8bC10CMainMgo
_j8b_rp13:
	pop_z
	ret
