; vm5277.avr_codegen v0.1 at Fri Nov 14 05:29:19 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/mfin.asm"
.include "stdio/out_char.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_cstr.asm"
.include "stdio/out_num32.asm"

j8bD82:
.db "orig num:",0x00

j8bD83:
.db "short:",0x00,0x00

j8bD84:
.db "byte:",0x00

Main:
	rjmp j8bCMainMmain
_j8b_meta21:
	.db 13,0

j8bCMainMmain:
	ldi r20,1
	ldi r21,2
	ldi r22,3
	ldi r23,4
	ldi r16,low(j8bD82*2)
	ldi r17,high(j8bD82*2)
	movw r30,r16
	rcall os_out_cstr
	movw r16,r20
	movw r18,r22
	rcall os_out_num32
	ldi r16,10
	rcall os_out_char
	movw r16,r20
	ldi r18,0x00
	ldi r19,0x00
	movw r20,r16
	movw r22,r18
	ldi r16,low(j8bD83*2)
	ldi r17,high(j8bD83*2)
	movw r30,r16
	rcall os_out_cstr
	movw r16,r20
	movw r18,r22
	rcall os_out_num32
	ldi r16,10
	rcall os_out_char
	ldi r16,low(j8bD84*2)
	ldi r17,high(j8bD84*2)
	movw r30,r16
	rcall os_out_cstr
	mov r16,r20
	rcall os_out_num8
	ldi r16,10
	rcall os_out_char
	rjmp j8bproc_mfin
