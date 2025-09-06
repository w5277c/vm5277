; vm5277.avr_codegen v0.1 at Thu Sep 04 11:11:23 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "stdio/out_char.asm"
.include "sys/mcu_stop.asm"

Main:
	rjmp j8bCMainMmain

j8bCmainMmain:
	ldi r21,32
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
	mov r16,r21
	add r16,r20
	mov r20,r16
	mov r16,r20
	rcall os_out_char
	rcall mcu_stop
	ret
