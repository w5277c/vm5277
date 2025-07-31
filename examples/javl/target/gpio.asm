; vm5277.avr_codegen v0.1 at Fri Aug 01 00:28:36 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_WELCOME = 1
.set OS_FT_TIMER1 = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "io/port_invert.asm"
.include "io/port_mode_out.asm"
.include "core/wait_ms.asm"

MAIN:
JavlCMainMmain15:
	ldi r20,17
	mov r16,r20
	call port_mode_out
JavlCMainMmainB81lWHILEBODY17:
	ldi r16,250
	ldi r17,0
	call wait_ms
	mov r16,r20
	call port_invert
	jmp JavlCMainMmainB81lWHILEBODY17
JavlCMainMmainB81lWHILEEND18:
	ret
