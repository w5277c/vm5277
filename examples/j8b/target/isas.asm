; vm5277.avr_codegen v0.1 at Sat Sep 27 09:20:38 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "j8b/instanceof.asm"
.include "j8b/clear_fields.asm"
.include "stdio/out_num16.asm"
.include "stdio/out_cstr.asm"

j8bD101:
.db " is short",0x0a,0x00,0x00

Main:
	rjmp j8bCMainMmain
_j8b_meta30:
	.db 13,0
_j8b_meta32:
	.db 15,1,14,2
	.dw 0,j8bC38CShortMtoShort

j8bC33CShortMShort:
	ldi r16,low(7)
	ldi r17,high(7)
	rcall os_dram_alloc
	std z+0,r16
	std z+1,r17
	std z+2,c0x00
	ldi r16,low(_j8b_meta32*2)
	std z+3,r16
	ldi r16,high(_j8b_meta32*2)
	std z+4,r16
	rcall j8bproc_clear_fields_nr
_j8b_cinit34:
	push yl
	push yh
	lds yl,SPL
	lds yh,SPH
	ldd r16,y+6
	ldd r17,y+5
	std z+5,r16
	std z+6,r17
	ret

j8bC38CShortMtoShort:
	ldd r16,z+5
	ldd r17,z+6
	ret

j8bCMainMmain:
	push zl
	push zh
	ldi r30,2
	push r30
	ldi r30,1
	push r30
	rcall j8bCMainMmain
	pop j8b_atom
	pop j8b_atom
	pop zh
	pop zl
	movw r20,r16
	push zl
	push zh
	movw r30,r20
	ldi r17,15
	rcall j8bproc_instanceof_nr
	pop zh
	pop zl
	tst r16
	breq  _j8b_eoc41
	push zl
	push zh
	movw r30,r20
	rcall j8bCMainMmain
	pop zh
	pop zl
	rcall os_out_num16
	ldi r30,low(j8bD101*2)
	ldi r31,high(j8bD101*2)
	rcall os_out_cstr
_j8b_eoc41:
	ret
