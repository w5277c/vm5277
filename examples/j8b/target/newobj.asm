; vm5277.avr_codegen v0.1 at Fri Nov 14 05:49:18 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_WELCOME = 1
.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/class_refcount.asm"
.include "j8b/clear_fields.asm"
.include "j8b/mfin_sf.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_num16.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta23:
	.db 13,0
_j8b_meta25:
	.db 15,1,14,1
	.dw 0

j8bC28CByteMByte:
	ldi r16,low(6)
	ldi r17,high(6)
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
	ldd r16,y+5
	std z+5,r16
	ldi r30,1
	rjmp j8bproc_mfin_sf

j8bCMainMmain:
	push yl
	push yh
	lds yl,SPL
	lds yh,SPH
	push c0x00
	push c0x00
	push r30
	push r31
	push c0x01
	rcall j8bC28CByteMByte
	movw r20,r16
	movw r26,r20
	ld r16,x+
	rcall os_out_num8
	push r30
	push r31
	ldi r19,2
	push r19
	rcall j8bC28CByteMByte
	movw r22,r16
	movw r26,r22
	ld r16,x+
	rcall os_out_num8
	push r30
	push r31
	movw r30,r20
	rcall j8bproc_class_refcount_dec
	pop r31
	pop r30
	ldi r20,0
	ldi r21,0
	push r30
	push r31
	push c0x01
	rcall j8bC28CByteMByte
	movw r24,r16
	movw r26,r24
	ld r16,x+
	rcall os_out_num8
	ldi r16,15
	rcall os_out_num8
	movw r16,r24
	rcall os_out_num16
	mov r16,r20
	std y+0,r16
	subi yl,low(33)
	sbci yh,high(33)
	std y+32,r17
	movw r16,r20
	cp r17,r23
	brne _j8b_eoc0
	cp r16,r22
	brne _j8b_eoc0
	ldi r16,1
	rcall os_out_num8
_j8b_eoc0:
	mov r16,r20
	ldd r19,y+33
	cp r16,r19
	brne _j8b_eoc1
	ldi r16,2
	rcall os_out_num8
_j8b_eoc1:
	movw r30,r20
	rcall j8bproc_class_refcount_dec
	movw r30,r22
	rcall j8bproc_class_refcount_dec
	movw r30,r24
	rcall j8bproc_class_refcount_dec
	ldd zl,y+33
	ldd zh,y+30
	rcall j8bproc_class_refcount_dec
	ldi r30,0
	rjmp j8bproc_mfin_sf
