; vm5277.avr_codegen v0.2
.equ CORE_FREQ = 16
.equ STDOUT_PORT = 18
.set OS_STAT_POOL_SIZE = 3

.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "stdio/out_num16.asm"
.include "stdio/out_num8.asm"

Main:
	jmp j8b_CMainMmain
_j8b_meta_32:
	.db 15,0
_j8b_finit_31:
	sts _os_stat_pool+0,c0x00
	ldi r19,128
	sts _os_stat_pool+1,r19
	ret

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
	call os_out_num8
	ldi r16,52
	call os_out_num8
	jmp mcu_halt
_j8b_meta_37:
	.db 17,0
_j8b_finit_36:
	ldi r19,2
	sts _os_stat_pool+2,r19
	ret
