; vm5277.avr_codegen v0.1 at Fri Aug 01 00:14:19 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_WELCOME = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "stdio/out_cstr.asm"
.include "sys/mcu_stop.asm"

JavlD78:
.db "Hello world!",0x0d,0x0a,0xf0,0xd2,0xc9,0xd7,0xc5,0xd4,"!",0x0d,0x0a,0x00

MAIN:
JavlCMainMmain12:
	ldi r30,low(JavlD78*2)
	ldi r31,high(JavlD78*2)
	call os_out_cstr
	call mcu_stop
	ret
