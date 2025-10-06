; vm5277.avr_codegen v0.1 at Tue Oct 07 00:34:54 VLAT 2025
.equ stdout_port = 18

.set OS_ARRAY_2D = 1
.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1
.set OS_ARRAY_1D = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/arr_celladdr.asm"
.include "j8b/arr_refcount.asm"
.include "mem/rom_read16.asm"
.include "stdio/out_num16.asm"
.include "stdio/out_q7n8.asm"

j8bD25:
.dw 128,0,0

j8bD26:
.dw 0,25853

Main:
	rjmp j8bCMainMmain
;======== enter CLASS Main ========================
_j8b_meta20:
	.db 12,0

;build method void 'Main.main()
j8bCMainMmain:
;build block
;alloc stack, size:2
	push yl
	push yh
	lds yl,SPL
	lds yh,SPH
	push c0x00
	push c0x00
;invokeNative void System.setParam [byte, byte], params:LITERAL=2,LITERAL=18
;const '254.0'->cells REG[20]
	ldi r20,254
;eUnary POST_INC cells REG[20]
	add r20,C0x01
;eNewArray fixed[][]
	ldi r16,19
	ldi r17,0
	push r30
	push r31
	rcall os_dram_alloc
	movw r26,r30
	pop r31
	pop r30
	movw r16,xl
	ldi r19,17
	st x+,r19
	st x+,C0x01
	ldi r19,9
	st x+,r19
	ldi r19,3
	st x+,r19
	st x+,c0x00
	ldi r19,2
	st x+,r19
	st x+,c0x00
;push accum(BE)
	push r17
	push r16
	push zl
	push zh
	ldi zl,low(j8bD25*2)
	ldi zh,high(j8bD25*2)
	ldi r16,low(6)
	ldi r17,high(6)
	rcall os_rom_read16_nr
	pop zh
	pop zl
;var 'b1'->accum
	mov r16,r20
;acc cast byte[1]->fixed[2]
	mov r17,r16
	clr r16
;acc ->arr'ARRAY[]'
	st x+,r16
	st x,r17
	push zl
	push zh
	ldi zl,low(j8bD26*2)
	ldi zh,high(j8bD26*2)
	ldi r16,low(4)
	ldi r17,high(4)
	rcall os_rom_read16_nr
	pop zh
	pop zl
;pop accum(BE)
	pop r16
	pop r17
;accum->var 'arr'
	movw r22,r16
;push const '0.0'
	push c0x00
	push c0x00
;push const '0.0'
	push c0x00
	push c0x00
;cells 'REG[22,23]'->ArrReg
	movw r26,r22
;compute array addr null->X
	rcall j8bproc_arr_celladdr
;invokeNative void System.out [fixed], params:ARRAY
	ld r16,x+
	ld r17,x
	rcall os_out_q7n8
;eNewArray short[]
	ldi r16,21
	ldi r17,0
	push r30
	push r31
	rcall os_dram_alloc
	movw r26,r30
	pop r31
	pop r30
	movw r16,xl
	ldi r19,16
	st x+,r19
	st x+,C0x01
	ldi r19,9
	st x+,r19
	ldi r19,8
	st x+,r19
	st x+,c0x00
;accum->var 'arr2'
	movw r24,r16
;push const '1.0'
	push c0x01
	push c0x00
;cells 'REG[24,25]'->ArrReg
	movw r26,r24
;compute array addr null->X
	rcall j8bproc_arr_celladdr
;const '3.0'->cells ARRAY[]
	ldi r16,3
	st x+,r16
	st x,c0x00
;const '257.0'->cells STACK_FRAME[0,1]
	std y+0,c0x01
	subi yl,low(33)
	sbci yh,high(33)
	std y+32,c0x01
;eUnary POST_INC cells STACK_FRAME[0,1]
	push zl
	ldd zl,y+33
	add zl,C0x01
	std y+33,zl
	ldd zl,y+32
	adc zl,C0x00
	std y+32,zl
	pop zl
;eIf
;var 's'->accum
	ldd r16,y+33
	ldd r17,y+32
;push const '3.0'
	ldi r19,3
	push r19
	push c0x00
;cells 'REG[24,25]'->ArrReg
	movw r26,r24
;compute array addr null->X
	rcall j8bproc_arr_celladdr
;accum LT ARRAY[] -> accum (isOr:false, isNot:false)
	ld r19,-x
	cp r17,r19
	breq pc+0x02
	brcc _j8b_eoc22
	ld r19,-x
	cp r16,r19
	brcc _j8b_eoc22
;build block
;push const '3.0'
	ldi r19,3
	push r19
	push c0x00
;cells 'REG[24,25]'->ArrReg
	movw r26,r24
;compute array addr null->X
	rcall j8bproc_arr_celladdr
;invokeNative void System.out [short], params:ARRAY
	ld r16,x+
	ld r17,x
	rcall os_out_num16
;block end
_j8b_eoc22:
;push const '1.0'
	push c0x01
	push c0x00
;cells 'REG[24,25]'->ArrReg
	movw r26,r24
;compute array addr null->X
	rcall j8bproc_arr_celladdr
;arr 'ARRAY[]'->acc
	ld r16,x+
	ld r17,x
;push const '0.0'
	push c0x00
	push c0x00
;push const '0.0'
	push c0x00
	push c0x00
;cells 'REG[22,23]'->ArrReg
	movw r26,r22
;compute array addr null->X
	rcall j8bproc_arr_celladdr
;acc cast short[2]->fixed[2]
	mov r17,r16
	clr r16
;acc ->arr'ARRAY[]'
	st x+,r16
	st x,r17
;arr refCount-- for REG[22,23]
	movw r26,r22
	rcall j8bproc_arr_refcount_dec
;const '0.0'->cells REG[22,23]
	ldi r22,0
	ldi r23,0
;arr refCount-- for REG[24,25]
	movw r26,r24
	rcall j8bproc_arr_refcount_dec
;const '0.0'->cells REG[24,25]
	ldi r24,0
	ldi r25,0
;build var byte 'Main.main.b1', allocated REG[20]
;build var fixed[][] 'Main.main.arr', allocated REG[22,23]
;build var short[] 'Main.main.arr2', allocated REG[24,25]
;build var short 'Main.main.s', allocated STACK_FRAME[0,1]
;block free, size:2
	pop j8b_atom
	pop j8b_atom
;return, argsSize:0, varsSize:2, retSize:0
	pop yh
	pop yl
	ret
;block end
;method end
;======== leave CLASS Main ========================
