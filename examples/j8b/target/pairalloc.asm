; vm5277.avr_codegen v0.1 at Fri Nov 14 05:49:45 VLAT 2025
.equ stdout_port = 18

.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/mfin.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_bool.asm"
.include "stdio/out_num32.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta14:
	.db 12,0

j8bCMainMmain:
	ldi r20,0
	ldi r22,0
	ldi r23,0
	ldi r24,0
	ldi r25,0
	ldi r21,0
	ldi r20,1
	ldi r22,7
	ldi r23,6
	ldi r24,5
	ldi r25,4
	ldi r21,8
	mov r16,r20
	rcall os_out_bool
	movw r16,r22
	movw r18,r24
	rcall os_out_num32
	mov r16,r21
	rcall os_out_num8
	rjmp j8bproc_mfin
