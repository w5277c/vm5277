; vm5277.avr_codegen v0.1 at Fri Sep 26 17:38:53 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_WELCOME = 1
.set OS_FT_TIMER1 = 1
.set OS_FT_DRAM = 1

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
_j8b_meta38:
	.db 14,0

j8bCMainMmain:
	ldi r16,17
	rcall port_mode_out
_j8b_loop44:
	ldi r16,250
	ldi r17,0
	rcall wait_ms
	ldi r16,17
	rcall port_invert
	rjmp _j8b_loop44
	ret
