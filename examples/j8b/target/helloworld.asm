; vm5277.avr_codegen v0.1 at Fri Nov 14 05:34:40 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_WELCOME = 1
.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/mfin.asm"
.include "stdio/out_cstr.asm"
.include "sys/mcu_stop.asm"

j8bD81:
.db "Hello world!",0x0d,0x0a,0xf0,0xd2,0xc9,0xd7,0xc5,0xd4,"!",0x0d,0x0a,0x00

Main:
	rjmp j8bCMainMmain
_j8b_meta21:
	.db 13,0

j8bCMainMmain:
	ldi r16,low(j8bD81*2)
	ldi r17,high(j8bD81*2)
	movw r30,r16
	rcall os_out_cstr
	rcall mcu_stop
	rjmp j8bproc_mfin
