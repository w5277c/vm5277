; vm5277.avr_codegen v0.1 at Thu Sep 11 04:00:14 VLAT 2025
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

Main:
	rjmp j8bCMainMmain
_j8b_meta18:
	.db 12,0
_j8b_meta20:
	.db 13,0
_j8b_finit19:
	ldi r16,10
	std z+6,r16
	ret

j8bC24CTestMTest:
	ldi r16,low(7)
	ldi r17,high(7)
	rcall os_dram_alloc
	std z+0,r16
	std z+1,r17
	std z+2,c0x00
	ldi r16,low(_j8b_meta20*2)
	std z+3,r16
	ldi r16,high(_j8b_meta20*2)
	std z+4,r16
	rcall j8bproc_clear_fields_nr
_j8b_cinit25:
	rcall _j8b_finit19
	push yl
	push yh
	lds yl,SPL
	lds yh,SPH
	ldd r16,y+5
	std z+5,r16
	ret

j8bC27CTestMsum:
	ldd r16,z+5
	push yl
	ldd yl,z+6
	add r16,yl
	pop yl
	ret

j8bC29CMainMgo:
	push yl
	push yh
	lds yl,SPL
	lds yh,SPH
	push r20
	push r21
	ldi r20,4
	ldi r21,5
	ldd r16,y+7
	rcall os_out_num8
	ldd r16,y+6
	rcall os_out_num8
	ldd r16,y+5
	rcall os_out_num8
	mov r16,r20
	rcall os_out_num8
	mov r16,r21
	rcall os_out_num8
	push r22
	ldi r22,6
	ldd r16,y+7
	rcall os_out_num8
	mov r16,r20
	rcall os_out_num8
	mov r16,r21
	rcall os_out_num8
	mov r16,r22
	rcall os_out_num8
	pop r22
	pop r21
	pop r20
	ret

j8bCMainMmain:
	push zl
	ldi r30,1
	push r30
	ldi r30,2
	push r30
	ldi r30,3
	push r30
	rcall j8bC29CMainMgo
	pop j8b_atom
	pop j8b_atom
	pop j8b_atom
	pop zl
	push zl
	push zh
	ldi r30,12
	push r30
	rcall j8bC24CTestMTest
	pop j8b_atom
	pop zh
	pop zl
	mov r20,r16
	mov r21,r17
	push zl
	push zh
	mov r30,r20
	mov r31,r21
	rcall j8bC27CTestMsum
	pop zh
	pop zl
	rcall os_out_num8
	ret
