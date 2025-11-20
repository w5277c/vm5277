; vm5277.avr_codegen v0.1 at Tue Nov 18 14:49:08 GMT+10:00 2025
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
_j8b_meta22:
	.db 12,0

j8bCMainMmain:
	ldi r20,3
_j8b_loop46:
	cpi r20,10
	brcc _j8b_eoc0
	mov r16,r20
	rcall os_out_num8
	add r20,C0x01
	cpi r20,5
	brne _j8b_loop46
_j8b_eoc0:
_j8b_loop47:
	mov r16,r20
	rcall os_out_num8
	rjmp _j8b_loop47
_j8b_loop49:
	rjmp _j8b_loop49
_j8b_loop50:
	ldi r16,33
	rcall os_out_num8
	add r20,C0x01
	cpi r20,20
	brcs _j8b_loop50
_j8b_loop51:
	ldi r16,32
	rcall os_out_num8
	add r20,C0x01
	rjmp _j8b_loop51
