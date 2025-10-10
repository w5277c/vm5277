; vm5277.avr_codegen v0.1 at Sat Oct 11 01:40:29 VLAT 2025
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
	ldi r16,1
	rcall os_out_num8
	ldi r16,2
	rcall os_out_num8
	ldi r16,1
	mov r20,r16
	ldi r16,3
	cp r16,r20
	brne _j8b_eoc22
	ldi r16,1
	rcall os_out_num8
_j8b_eob23:
	rjmp _j8b_eob24
_j8b_eoc22:
	ldi r16,0
	rcall os_out_num8
_j8b_eob24:
	ldi r19,3
	push r19
	rcall j8bC25CMainMprintDirection
	pop j8b_atom
	ldi r21,0
	mov r16,r21
	rcall os_out_num8
	ldi r16,1
	mov r22,r16
	mov r16,r22
	rcall os_out_num8
	ldi r16,1
	rcall os_out_num8
_j8b_eob21:
	ret

j8bC25CMainMprintDirection:
	push yl
	push yh
	lds yl,SPL
	lds yh,SPH
	ldd r16,y+5
	rcall os_out_num8
_j8b_eob26:
	ret
