; vm5277.avr_codegen v0.1 at Thu Sep 04 03:38:11 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_DRAM = 1
.set OS_FT_TIMER1 = 1
.set OS_FT_WELCOME = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "io/port_invert.asm"
.include "io/port_mode_out.asm"
.include "core/wait_ms.asm"

Main:
	rjmp j8bCMainMmain

j8bCmainMmain:
	ldi r20,17
	mov r16,r20
	rcall port_mode_out
_j8b_loop22:
	ldi r16,250
	ldi r17,0
	rcall wait_ms
	mov r16,r20
	rcall port_invert
	rjmp _j8b_loop22
	ret
