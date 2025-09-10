; vm5277.avr_codegen v0.1 at Wed Sep 10 10:13:05 VLAT 2025
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
_j8b_meta15:
	.db 12,0

j8bC16CMainMgo:
	push yl
	push yh
	lds yl,SPL
	lds yh,SPH
	push c0x00
	push c0x00
	push zl
	ldi zl,4
	std y+0,zl
	ldi zl,5
	subi yl,low(33)
	sbci yh,high(33)
	std y+32,zl
	pop zl
	ldd r16,y+40
	rcall os_out_num8
	ldd r16,y+39
	rcall os_out_num8
	ldd r16,y+38
	rcall os_out_num8
	ldd r16,y+33
	rcall os_out_num8
	ldd r16,y+32
	rcall os_out_num8
	push c0x00
	push zl
	ldi zl,6
	std y+31,zl
	pop zl
	ldd r16,y+40
	rcall os_out_num8
	ldd r16,y+33
	rcall os_out_num8
	ldd r16,y+32
	rcall os_out_num8
	ldd r16,y+31
	rcall os_out_num8
_j8b_eob18:
	pop j8b_atom
_j8b_eob17:
	pop j8b_atom
	pop j8b_atom
	pop yh
	pop yl
	ret

j8bCMainMmain:
	push zl
	ldi r30,1
	push r30
	ldi r30,2
	push r30
	ldi r30,3
	push r30
	rcall j8bC16CMainMgo
	pop j8b_atom
	pop j8b_atom
	pop j8b_atom
	pop zl
_j8b_eob19:
