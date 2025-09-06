; vm5277.avr_codegen v0.1 at Thu Sep 04 03:40:44 VLAT 2025
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
.include "stdio/out_cstr.asm"
.include "sys/mcu_stop.asm"

j8bD79:
.db "Hello world!",0x0d,0x0a,0xf0,0xd2,0xc9,0xd7,0xc5,0xd4,"!",0x0d,0x0a,0x00

Main:
	rjmp j8bCMainMmain

j8bCmainMmain:
	ldi r30,low(j8bD79*2)
	ldi r31,high(j8bD79*2)
	rcall os_out_cstr
	rcall mcu_stop
	ret
