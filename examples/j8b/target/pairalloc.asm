; vm5277.avr_codegen v0.1 at Sat Sep 27 10:11:05 VLAT 2025
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "stdio/out_num16.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_bool.asm"
.include "stdio/out_num32.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta20:
	.db 12,0

j8bCMainMmain:
	ldi r20,0
	ldi r22,0
	ldi r23,0
	ldi r24,0
	ldi r25,0
	ldi r26,0
	ldi r27,0
	ldi r21,0
	ldi r20,1
	ldi r22,3
	ldi r23,2
	ldi r24,7
	ldi r25,6
	ldi r26,5
	ldi r27,4
	ldi r21,8
	mov r16,r20
	rcall os_out_bool
	movw r16,r22
	rcall os_out_num16
	movw r16,r24
	movw r18,r26
	rcall os_out_num32
	mov r16,r21
	rcall os_out_num8
	ret
