; vm5277.avr_codegen v0.1 at Tue Oct 07 00:37:35 VLAT 2025
.equ stdout_port = 18

.set OS_ARRAY_1D = 1
.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/arr_celladdr.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_bool.asm"
.include "stdio/out_num32.asm"

Main:
	rjmp j8bCMainMmain
;======== enter CLASS Main ========================
_j8b_meta20:
	.db 12,0

;build method void 'Main.main()
j8bCMainMmain:
;build block
;invokeNative void System.setParam [byte, byte], params:LITERAL=2,LITERAL=18
;eNewArray int[]
	ldi r16,13
	ldi r17,0
	push r30
	push r31
	rcall os_dram_alloc
	movw r26,r30
	pop r31
	pop r30
	movw r16,xl
	ldi r19,32
	st x+,r19
	st x+,C0x01
	ldi r19,9
	st x+,r19
	ldi r19,2
	st x+,r19
	st x+,c0x00
;accum->var 'bArray1'
	movw r20,r16
;eNewArray int[]
	ldi r16,21
	ldi r17,0
	push r30
	push r31
	rcall os_dram_alloc
	movw r26,r30
	pop r31
	pop r30
	movw r16,xl
	ldi r19,32
	st x+,r19
	st x+,C0x01
	ldi r19,9
	st x+,r19
	ldi r19,4
	st x+,r19
	st x+,c0x00
;accum->var 'bArray2'
	movw r22,r16
;push const '3.0'
	ldi r19,3
	push r19
	push c0x00
;cells 'REG[22,23]'->ArrReg
	movw r26,r22
;compute array addr null->X
	rcall j8bproc_arr_celladdr
;arr 'ARRAY[]'->acc
	ld r16,x+
	ld r17,x+
	ld r18,x+
	ld r19,x
;acc cast int[4]->short[2]
;push accum(BE)
	push r17
	push r16
;cells 'REG[20,21]'->ArrReg
	movw r26,r20
;compute array addr null->X
	rcall j8bproc_arr_celladdr
;invokeNative void System.out [int], params:ARRAY
	ld r16,x+
	ld r17,x+
	ld r18,x+
	ld r19,x
	rcall os_out_num32
;push const '1.0'
	push c0x01
	push c0x00
;cells 'REG[22,23]'->ArrReg
	movw r26,r22
;compute array addr null->X
	rcall j8bproc_arr_celladdr
;arr 'ARRAY[]'->acc
	ld r16,x+
	ld r17,x+
	ld r18,x+
	ld r19,x
;push const '0.0'
	push c0x00
	push c0x00
;cells 'REG[20,21]'->ArrReg
	movw r26,r20
;compute array addr null->X
	rcall j8bproc_arr_celladdr
;acc ->arr'ARRAY[]'
	st x+,r16
	st x+,r17
	st x+,r18
	st x,r19
;invokeNative void System.out [byte], params:LITERAL=32
	;load method param
	ldi r16,32
	rcall os_out_num8
;eIf
;push const '1.0'
	push c0x01
	push c0x00
;cells 'REG[20,21]'->ArrReg
	movw r26,r20
;compute array addr null->X
	rcall j8bproc_arr_celladdr
;cells ARRAY[] GT 2 -> accum (isOr:false, isNot:false)
	adiw r26,4
	ld r16,-x
	cpi r16,0
	brcs _j8b_eoc22
	brne _j8b_mcpe27
	ld r16,-x
	cpi r16,0
	brcs _j8b_eoc22
	brne _j8b_mcpe27
	ld r16,-x
	cpi r16,0
	brcs _j8b_eoc22
	brne _j8b_mcpe27
	ld r16,-x
	cpi r16,2
	brcs _j8b_eoc22
	breq _j8b_eoc22
_j8b_mcpe27:
;build block
;invokeNative void System.out [byte], params:LITERAL=33
	;load method param
	ldi r16,33
	rcall os_out_num8
;block end
_j8b_eoc22:
;const '0.0'->cells REG[24]
;const '3.0'->cells REG[24]
	ldi r24,3
;push const '0.0'
	push c0x00
	push c0x00
;cells 'REG[20,21]'->ArrReg
	movw r26,r20
;compute array addr null->X
	rcall j8bproc_arr_celladdr
;arr 'ARRAY[]'->acc
	ld r16,x+
	ld r17,x+
	ld r18,x+
	ld r19,x
;accum GT REG[24] -> accum (isOr:false, isNot:false)
	cpi r19,0x00
	brcs _j8b_eoc28
	brne _j8b_mcpe29
	cpi r18,0x00
	brcs _j8b_eoc28
	brne _j8b_mcpe29
	cpi r17,0x00
	brcs _j8b_eoc28
	brne _j8b_mcpe29
	cp r16,r24
	brcs _j8b_eoc28
	breq _j8b_eoc28
_j8b_mcpe29:
;cells REG[24] EQ 2 -> accum (isOr:false, isNot:false)
	cpi r24,2
	brne _j8b_eoc28
;const '1.0'->accum 
	ldi r16,1
	rjmp _j8b_eolb30
_j8b_eoc28:
;const '0.0'->accum 
	ldi r16,0
_j8b_eolb30:
;accum->var 'bl'
	mov r25,r16
;invokeNative void System.out [bool], params:LOCAL_RES=32
	;load method param
	mov r16,r25
	rcall os_out_bool
;build var int[] 'Main.main.bArray1', allocated REG[20,21]
;build var int[] 'Main.main.bArray2', allocated REG[22,23]
;build var byte 'Main.main.bt', allocated REG[24]
;build var bool 'Main.main.bl', allocated REG[25]
;return, argsSize:0, varsSize:0, retSize:0
	ret
;block end
;method end
;======== leave CLASS Main ========================
