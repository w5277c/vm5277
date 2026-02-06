; vm5277.AVR v0.2
.equ CORE_FREQ = 16
.set STDIN_PORT_REGID = 11
.set STDIN_DDR_REGID = 10
.set STDIN_PIN_REGID = 9
.set STDIN_PORTNUM = 3
.set STDIN_PINNUM = 3
.set STDOUT_PORT_REGID = 11
.set STDOUT_DDR_REGID = 10
.set STDOUT_PIN_REGID = 9
.set STDOUT_PORTNUM = 3
.set STDOUT_PINNUM = 4

.set OS_FT_WELCOME = 1

.include "devices/atmega168p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "io/port_invert.asm"
.include "io/port_mode_out.asm"
.include "core/wait_ms.asm"


Main:
	jmp j8b_CMainMmain
_j8b_meta_42:
	.db 22,0

j8b_CMainMmain:
	ldi r16,21
	call port_mode_out
_j8b_loop_45:
	ldi r16,50
	ldi r17,0
	call os_wait_ms
	ldi r16,21
	call port_invert
	rjmp _j8b_loop_45
