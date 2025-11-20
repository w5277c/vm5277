; vm5277.avr_codegen v0.1 at Fri Nov 21 07:26:06 GMT+10:00 2025
.equ core_freq = 16
.equ stdout_port = 18
.set OS_STAT_POOL_SIZE = 3

.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/class_refcount.asm"
.include "j8b/clear_fields.asm"
.include "j8b/mfin.asm"
.include "stdio/out_num16.asm"
.include "stdio/out_num8.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta21:
	.db 13,0
_j8b_finit20:
	sts _OS_STAT_POOL+0,c0x01
	ldi r19,2
	sts _OS_STAT_POOL+1,r19
	sts _OS_STAT_POOL+2,c0x00
	ret
_j8b_meta23:
	.db 14,0
_j8b_finit22:
	std z+5,c0x00
	ret

j8bC30CClazzMClazz:
	ldi r16,low(6)
	ldi r17,high(6)
	rcall os_dram_alloc
	std z+0,r16
	std z+1,r17
	std z+2,c0x00
	ldi r16,low(_j8b_meta23*2)
	std z+3,r16
	ldi r16,high(_j8b_meta23*2)
	std z+4,r16
	rcall j8bproc_clear_fields_nr
_j8b_cinit28:
	rcall _j8b_finit22
	rjmp j8bproc_mfin

j8bCMainMmain:
	ldi r19,15
	sts _OS_STAT_POOL+0,r19
	ldi r19,14
	sts _OS_STAT_POOL+1,r19
	sts _OS_STAT_POOL+2,c0x00
	lds r16,_OS_STAT_POOL+1
	lds r17,_OS_STAT_POOL+2
	rcall os_out_num16
	push r30
	push r31
	rcall j8bC30CClazzMClazz
	movw r20,r16
	movw r26,r20
	adiw r26,5
	ldi r19,16
	st x+,r19
	movw r26,r20
	adiw r26,5
	ld r16,x+
	rcall os_out_num8
	movw r30,r20
	rcall j8bproc_class_refcount_dec
	rjmp j8bproc_mfin
