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
.set OS_STAT_POOL_SIZE = 3

.set OS_FT_STDOUT = 1

.include "devices/atmega168p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "stdio/out_char.asm"
.include "stdio/out_num16.asm"
.include "stdio/out_num8.asm"


Main:
	sts _os_stat_pool+0,c0x00
	ldi r19,128
	sts _os_stat_pool+1,r19
	ldi r19,2
	sts _os_stat_pool+2,r19
	jmp j8b_CMainMmain
_j8b_meta_41:
	.db 22,0

j8b_CMainMmain:
	lds r19,_OS_STAT_POOL+0
	add r19,c0x01
	sts _os_stat_pool+0,r19
	lds r19,_OS_STAT_POOL+1
	adc r19,c0x00
	sts _os_stat_pool+1,r19
	lds r16,_OS_STAT_POOL+0
	lds r17,_OS_STAT_POOL+1
	call os_out_num16
	ldi r19,110
	sts _os_stat_pool+2,r19
	lds r16,_OS_STAT_POOL+2
	mov r20,r16
	ldi r16,10
	call os_out_char
	mov r16,r20
	call os_out_num8
	ldi r16,10
	call os_out_char
	ldi r16,52
	call os_out_num8
	jmp mcu_halt
_j8b_meta_44:
	.db 24,0
