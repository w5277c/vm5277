; vm5277.avr_codegen v0.1 at Wed Oct 08 15:23:24 VLAT 2025
.equ stdout_port = 18

.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "stdio/out_num8.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta20:
	.db 12,0

j8bCMainMmain:
	ldi r20,0
_j8b_loop55:
	cpi r20,10
	brcc _j8b_eoc24
	cpi r20,1
	breq _j8b_eol23
	mov r16,r20
	rcall os_out_num8
	add r20,C0x01
	rjmp _j8b_loop55
_j8b_eoc24:
	ldi r16,33
	rcall os_out_num8
_j8b_eol23:
	ldi r20,8
_j8b_loop57:
	mov r16,r20
	rcall os_out_num8
	add r20,C0x01
	rjmp _j8b_loop57
	ldi r20,0
_j8b_loop58:
	mov r16,r20
	rcall os_out_num8
	add r20,C0x01
	rjmp _j8b_loop58
	ldi r20,3
_j8b_loop59:
	cpi r20,10
	brcc _j8b_eoc42
	mov r16,r20
	rcall os_out_num8
	add r20,C0x01
	rjmp _j8b_loop59
_j8b_eoc42:
	ldi r20,0
_j8b_loop60:
	cpi r20,10
	brcc _j8b_eoc46
	mov r16,r20
	rcall os_out_num8
	add r20,C0x01
	rjmp _j8b_loop60
_j8b_eoc46:
	ldi r21,0
_j8b_loop61:
	ldi r16,0
	rcall os_out_num8
	rjmp _j8b_loop61
	ret
