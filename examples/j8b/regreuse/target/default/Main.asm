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
.set OS_FT_WELCOME = 1

.include "devices/atmega168p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "stdio/out_char.asm"
.include "stdio/out_num8.asm"


Main:
	jmp j8b_CMainMmain
_j8b_meta_41:
	.db 22,0

j8b_CMainMmain:
	ldi r20,100
	ldi r21,10
	ldi r22,20
	ldi r16,10
	call os_out_char
	mov r16,r22
	add r16,r21
	add r16,r20
	call os_out_num8
	ldi r23,0
	ldi r21,30
	ldi r22,40
	ldi r16,10
	call os_out_char
	mov r16,r22
	add r16,r21
	call os_out_num8
	ldi r23,50
	ldi r16,10
	call os_out_char
	mov r16,r23
	call os_out_num8
	ldi r21,60
	ldi r22,70
	ldi r16,10
	call os_out_char
	mov r16,r21
	add r16,r20
	add r16,r22
	call os_out_num8
	jmp mcu_halt
