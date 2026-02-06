; vm5277.AVR v0.2
.equ CORE_FREQ = 16
.set STDIN_PORT_REGID = 11
.set STDIN_DDR_REGID = 10
.set STDIN_PIN_REGID = 9
.set STDIN_PORTNUM = 3
.set STDIN_PINNUM = 0
.set STDOUT_PORT_REGID = 11
.set STDOUT_DDR_REGID = 10
.set STDOUT_PIN_REGID = 9
.set STDOUT_PORTNUM = 3
.set STDOUT_PINNUM = 1

.set OS_FT_STDOUT = 1
.set OS_FT_STDIN = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "stdio/out_char.asm"
.include "stdio/out_cstr.asm"
.include "j8b/stdin_read.asm"

j8bD141:
.db "Ready!",0x0a,0x00
j8bD142:
.db "got:",0x00,0x00

Main:
	jmp j8b_CMainMmain
_j8b_meta_42:
	.db 22,0

j8b_CMainMmain:
	ldi r16,low(j8bD141*2)
	ldi r17,high(j8bD141*2)
	movw r30,r16
	call os_out_cstr
_j8b_loop_45:
	call j8bproc_stdin_read
	mov r20,r16
	ldi r16,low(j8bD142*2)
	ldi r17,high(j8bD142*2)
	movw r30,r16
	call os_out_cstr
	mov r16,r20
	call os_out_char
	ldi r16,10
	call os_out_char
	rjmp _j8b_loop_45
