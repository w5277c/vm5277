; vm5277.avr_codegen v0.1 at Fri Nov 14 05:35:04 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1
.set OS_FT_WELCOME = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/mfin.asm"
.include "stdio/out_char.asm"
.include "sys/mcu_stop.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta21:
	.db 13,0

j8bCMainMmain:
	ldi r20,65
	mov r16,r20
	rcall os_out_char
	add r20,C0x01
	mov r16,r20
	rcall os_out_char
	add r20,C0x01
	mov r16,r20
	rcall os_out_char
	add r20,C0x01
	mov r16,r20
	subi r16,224
	mov r20,r16
	mov r16,r20
	rcall os_out_char
	rcall mcu_stop
	rjmp j8bproc_mfin
