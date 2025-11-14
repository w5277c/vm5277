; vm5277.avr_codegen v0.1 at Fri Nov 14 05:33:38 VLAT 2025
.equ stdout_port = 18

.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/mfin.asm"
.include "stdio/out_num8.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta22:
	.db 12,0

j8bCMainMmain:
	ldi r20,0
_j8b_loop48:
	cpi r20,10
	brcc _j8b_eoc0
	cpi r20,1
	breq _j8b_eol25
	mov r16,r20
	rcall os_out_num8
	add r20,C0x01
	rjmp _j8b_loop48
_j8b_eoc0:
	ldi r16,33
	rcall os_out_num8
_j8b_eol25:
	ldi r20,7
	ldi r16,64
	rcall os_out_num8
	ldi r20,8
_j8b_loop50:
	mov r16,r20
	rcall os_out_num8
	add r20,C0x01
	rjmp _j8b_loop50
	ldi r20,0
_j8b_loop51:
	mov r16,r20
	rcall os_out_num8
	add r20,C0x01
	rjmp _j8b_loop51
	ldi r20,3
_j8b_loop52:
	cpi r20,10
	brcc _j8b_eoc5
	mov r16,r20
	rcall os_out_num8
	add r20,C0x01
	rjmp _j8b_loop52
_j8b_eoc5:
	ldi r20,0
_j8b_loop53:
	cpi r20,10
	brcc _j8b_eoc6
	mov r16,r20
	rcall os_out_num8
	add r20,C0x01
	rjmp _j8b_loop53
_j8b_eoc6:
	ldi r21,0
_j8b_loop54:
	cpi r21,10
	brcc _j8b_eoc7
	mov r16,r21
	rcall os_out_num8
	rjmp _j8b_loop54
_j8b_eoc7:
	rjmp j8bproc_mfin
