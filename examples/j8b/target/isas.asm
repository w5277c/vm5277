; vm5277.avr_codegen v0.1 at Fri Nov 14 05:44:38 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/class_refcount.asm"
.include "j8b/instanceof.asm"
.include "j8b/clear_fields.asm"
.include "j8b/mfin.asm"
.include "j8b/mfin_sf.asm"
.include "stdio/out_num16.asm"
.include "stdio/out_cstr.asm"

j8bD98:
.db " is short",0x0a,0x00,0x00

j8bD99:
.db "Is short too",0x0a,0x00

Main:
	rjmp j8bCMainMmain
_j8b_meta23:
	.db 13,0
_j8b_meta25:
	.db 15,1,14,2
	.dw 0,0

j8bC28CShortMShort:
	ldi r16,low(7)
	ldi r17,high(7)
	rcall os_dram_alloc
	std z+0,r16
	std z+1,r17
	std z+2,c0x00
	ldi r16,low(_j8b_meta25*2)
	std z+3,r16
	ldi r16,high(_j8b_meta25*2)
	std z+4,r16
	rcall j8bproc_clear_fields_nr
_j8b_cinit26:
	push yl
	push yh
	lds yl,SPL
	lds yh,SPH
	ldd r16,y+6
	ldd r17,y+5
	std z+5,r16
	std z+6,r17
	ldi r30,2
	rjmp j8bproc_mfin_sf

j8bCMainMmain:
	push r30
	push r31
	ldi r19,2
	push r19
	push c0x01
	rcall j8bC28CShortMShort
	movw r20,r16
	push r30
	push r31
	movw r30,r20
	ldi r19,15
	rcall j8bproc_instanceof_nr
	pop r31
	pop r30
	sbrs r16,0x00
	rjmp _j8b_eoc0
	movw r26,r20
	ld r16,x+
	ld r17,x+
	rcall os_out_num16
	ldi r16,low(j8bD98*2)
	ldi r17,high(j8bD98*2)
	movw r30,r16
	rcall os_out_cstr
	ldi r16,low(j8bD99*2)
	ldi r17,high(j8bD99*2)
	movw r30,r16
	rcall os_out_cstr
_j8b_eoc0:
	movw r30,r20
	rcall j8bproc_class_refcount_dec
	rjmp j8bproc_mfin
