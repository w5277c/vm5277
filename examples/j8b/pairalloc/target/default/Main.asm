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

.set OS_FT_STDOUT = 1

.include "devices/atmega168p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "stdio/out_char.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_bool.asm"
.include "stdio/out_num32.asm"


Main:
	jmp j8b_CMainMmain
_j8b_meta_41:
	.db 22,0

j8b_CMainMmain:
	ldi r20,1
	ldi r22,7
	ldi r23,6
	ldi r24,5
	ldi r25,4
	ldi r21,8
	ldi r16,10
	call os_out_char
	mov r16,r20
	call os_out_bool
	ldi r16,10
	call os_out_char
	movw r16,r22
	movw r18,r24
	call os_out_num32
	ldi r16,10
	call os_out_char
	mov r16,r21
	call os_out_num8
	jmp mcu_halt
