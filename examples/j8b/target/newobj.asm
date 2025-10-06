; vm5277.avr_codegen v0.1 at Sat Sep 27 09:58:23 VLAT 2025
.equ stdout_port = 18

.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "j8b/clear_fields.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_num16.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta20:
	.db 12,0
_j8b_meta22:
	.db 14,1,13,1
	.dw j8bC26CByteMtoByte

j8bC23CByteMByte:
	ldi r16,low(6)
	ldi r17,high(6)
	rcall os_dram_alloc
	std z+0,r16
	std z+1,r17
	std z+2,c0x00
	ldi r16,low(_j8b_meta22*2)
	std z+3,r16
	ldi r16,high(_j8b_meta22*2)
	std z+4,r16
	rcall j8bproc_clear_fields_nr
_j8b_cinit24:
	push yl
	push yh
	lds yl,SPL
	lds yh,SPH
	ldd r16,y+5
	std z+5,r16
	ret

j8bC26CByteMtoByte:
	ldd r16,z+5
	ret

j8bCMainMmain:
	push zl
	push zh
	ldi r30,1
	push r30
	rcall j8bC23CByteMByte
	pop j8b_atom
	pop zh
	pop zl
	movw r20,r16
	push zl
	push zh
	movw r30,r20
	rcall j8bC26CByteMtoByte
	pop zh
	pop zl
	rcall os_out_num8
	push zl
	push zh
	movw r30,r20
	rcall j8bproc_dec_refcount
	pop zh
	pop zl
	ldi r20,0
	ldi r21,0
	push zl
	push zh
	ldi r30,1
	push r30
	rcall j8bC23CByteMByte
	pop j8b_atom
	pop zh
	pop zl
	movw r22,r16
	push zl
	push zh
	movw r30,r22
	rcall j8bC26CByteMtoByte
	pop zh
	pop zl
	rcall os_out_num8
	ldi r16,14
	ldi r17,0
	rcall os_out_num16
	movw r16,r22
	rcall os_out_num16
	ldi r16,33
	rcall os_out_num8
	ret
