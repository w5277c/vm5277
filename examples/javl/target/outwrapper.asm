; vm5277.avr_codegen v0.1 at Fri Aug 01 00:09:52 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_WELCOME = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "stdio/out_char.asm"
.include "sys/mcu_stop.asm"

MAIN:
JavlCMainMmain12:
	ldi r16,35
	call os_out_char
	call mcu_stop
	ret
