; vm5277.avr_codegen v0.2
.equ CORE_FREQ = 16
.equ STDOUT_PORT = 18
.set OS_STAT_POOL_SIZE = 3
.set OS_ETRACE_POINT_BITSIZE = 7

.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1
.set OS_FT_DRAM = 1
.set OS_FT_ETRACE = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "j8b/etrace_out.asm"
.include "j8b/etrace_add.asm"
.include "dmem/dram.asm"
.include "j8b/clear_fields.asm"
.include "j8b/mfin.asm"
.include "stdio/out_num16.asm"
.include "stdio/out_num8.asm"

Main:
	jmp j8b_CMainMmain
_j8b_meta_32:
	.db 15,0
_j8b_finit_31:
	sts _os_stat_pool+0,c0x01
	ldi r19,2
	sts _os_stat_pool+1,r19
	sts _os_stat_pool+2,c0x00
	ret
_j8b_meta_34:
	.db 16,0
_j8b_finit_33:
	std z+5,c0x00
	ret

j8b_CClazzMClazz_41:
	ldi r16,low(6)
	ldi r17,high(6)
	call os_dram_alloc
	brcc _j8b_throwskip_115
	ldi r16,4
	call j8bproc_etrace_addfirst
	set
	rjmp _j8b_eob_40
_j8b_throwskip_115:
	std z+0,r16
	std z+1,r17
	std z+2,c0xff
	ldi r16,low(_j8b_meta_34*2)
	std z+3,r16
	ldi r16,high(_j8b_meta_34*2)
	std z+4,r16
	call j8bproc_clear_fields_nr
	call _j8b_finit_33
_j8b_eob_40:
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
	rcall j8b_CClazzMClazz_41
	movw r20,r16
	movw r26,r20
	adiw r26,5
	ldi r19,16
	st x+,r19
	movw r26,r20
	adiw r26,5
	ld r16,x+
	call os_out_num8
	brts _j8b_skip_116
	jmp mcu_halt
_j8b_skip_116:
	call j8bproc_etrace_out
	jmp mcu_halt
