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
.set OS_STAT_POOL_SIZE = 3

.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "dmem/dram.asm"
.include "j8b/clear_fields.asm"
.include "j8b/mfin.asm"
.include "stdio/out_num16.asm"
.include "stdio/out_num8.asm"


Main:
	sts _os_stat_pool+0,c0x01
	ldi r19,2
	sts _os_stat_pool+1,r19
	sts _os_stat_pool+2,c0x00
	std z+5,c0x00
	jmp j8b_CMainMmain
_j8b_meta_41:
	.db 22,0
	sts _os_stat_pool+0,c0x01
	ldi r19,2
	sts _os_stat_pool+1,r19
	sts _os_stat_pool+2,c0x00
	ret
_j8b_meta_42:
	.db 23,0

j8b_CClazzMClazz_49:
	ldi r16,low(6)
	ldi r17,high(6)
	call os_dram_alloc
	std z+0,r16
	std z+1,r17
	std z+2,c0xff
	ldi r16,low(_j8b_meta_42*2)
	std z+3,r16
	ldi r16,high(_j8b_meta_42*2)
	std z+4,r16
	call j8bproc_clear_fields_nr
	jmp j8bproc_mfin

j8b_CMainMmain:
	ldi r19,15
	sts _os_stat_pool+0,r19
	ldi r19,14
	sts _os_stat_pool+1,r19
	sts _os_stat_pool+2,c0x00
	lds r16,_OS_STAT_POOL+1
	lds r17,_OS_STAT_POOL+2
	call os_out_num16
	push r30
	push r31
	rcall j8b_CClazzMClazz_49
	movw r20,r16
	movw r26,r20
	adiw r26,5
	ldi r19,16
	st x+,r19
	movw r26,r20
	adiw r26,5
	ld r16,x+
	call os_out_num8
	jmp mcu_halt
