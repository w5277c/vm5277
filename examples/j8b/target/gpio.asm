; vm5277.avr_codegen v0.1 at Thu Nov 20 07:17:52 GMT+10:00 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_WELCOME = 1
.set OS_FT_TIMER1 = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/mfin.asm"
.include "io/port_invert.asm"
.include "io/port_mode_out.asm"
.include "core/wait_ms.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta27:
	.db 14,0

j8bCMainMmain:
	ldi r16,17
	rcall port_mode_out
_j8b_loop30:
	ldi r16,250
	ldi r17,0x00
	rcall wait_ms
	ldi r16,17
	rcall port_invert
	rjmp _j8b_loop30
	rjmp j8bproc_mfin
