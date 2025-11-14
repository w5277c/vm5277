; vm5277.avr_codegen v0.1 at Fri Nov 14 05:51:27 VLAT 2025
.equ stdout_port = 18
.set OS_STAT_POOL_SIZE = 3

.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/mfin.asm"
.include "stdio/out_num16.asm"
.include "stdio/out_num8.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta14:
	.db 12,0
_j8b_finit13:
	sts _OS_STAT_POOL+0,c0x00
	ldi r19,128
	sts _OS_STAT_POOL+1,r19
	ret

j8bCMainMmain:
	lds r19,_OS_STAT_POOL+0
	add r19,C0x01
	sts _OS_STAT_POOL+0,r19
	lds r19,_OS_STAT_POOL+1
	adc r19,C0x00
	sts _OS_STAT_POOL+1,r19
	lds r16,_OS_STAT_POOL+0
	lds r17,_OS_STAT_POOL+1
	rcall os_out_num16
	ldi r19,110
	sts _OS_STAT_POOL+2,r19
	lds r16,_OS_STAT_POOL+2
	mov r20,r16
	rcall os_out_num8
	ldi r16,52
	rcall os_out_num8
	rjmp j8bproc_mfin
_j8b_meta19:
	.db 14,0
_j8b_finit18:
	ldi r19,2
	sts _OS_STAT_POOL+2,r19
	ret
