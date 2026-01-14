; vm5277.avr_codegen v0.2
.equ CORE_FREQ = 16


.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "io/port_invert.asm"
.include "io/port_mode_out.asm"
.include "core/wait_ms.asm"

Main:
	jmp j8b_CMainMmain
_j8b_meta_33:
	.db 15,0

j8b_CMainMmain:
	ldi r16,17
	call port_mode_out
_j8b_loop_36:
	ldi r16,250
	ldi r17,0x00
	call wait_ms
	ldi r16,17
	call port_invert
	rjmp _j8b_loop_36
	jmp mcu_halt
