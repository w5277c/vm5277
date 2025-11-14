; vm5277.avr_codegen v0.1 at Fri Nov 14 05:50:10 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_WELCOME = 1
.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/mfin.asm"
.include "stdio/out_num8.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta21:
	.db 13,0

j8bCMainMmain:
	ldi r20,100
	ldi r21,0
	ldi r22,0
	ldi r21,10
	ldi r22,20
	mov r16,r22
	add r16,r21
	add r16,r20
	rcall os_out_num8
	ldi r21,0
	ldi r22,0
	ldi r23,0
	ldi r21,30
	ldi r22,40
	mov r16,r22
	add r16,r21
	rcall os_out_num8
	ldi r23,50
	mov r16,r23
	rcall os_out_num8
	ldi r21,60
	ldi r22,70
	mov r16,r21
	add r16,r20
	add r16,r22
	rcall os_out_num8
	rjmp j8bproc_mfin
