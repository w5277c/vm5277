
	.SET	CORE_FREQ = 8

	.include "devices/atmega328.def"
	.include "core/core.asm"
	.include "io/port_mode_out.asm"
	.include "io/port_set_hi.asm"
	.include "io/port_set_lo.asm"
	.include "core/wait_ms.asm"

	.EQU LED_PORT = PB0

MAIN:
	LDI ACCUM_L,LED_PORT
	CALL PORT_MODE_OUT

LOOP:
	LDI ACCUM_L,LED_PORT
	CALL PORT_SET_LO

	LDI ACCUM_H,0x00
	LDI ACCUM_L,0x64
	CALL WAIT_MS

	LDI ACCUM_L,LED_PORT
	CALL PORT_SET_HI

	LDI ACCUM_H,0x00
	LDI ACCUM_L,0x64
	CALL WAIT_MS

	RJMP LOOP
