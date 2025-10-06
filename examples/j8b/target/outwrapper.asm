; vm5277.avr_codegen v0.1 at Sat Sep 27 10:01:14 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_WELCOME = 1
.set OS_FT_STDOUT = 1
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
_j8b_meta30:
	.db 13,0

j8bCMainMmain:
	ldi r16,35
	rcall os_out_char
	rcall mcu_stop
	ret
