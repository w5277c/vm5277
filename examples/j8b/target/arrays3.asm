; vm5277.avr_codegen v0.1 at Tue Oct 07 00:21:51 VLAT 2025
.equ stdout_port = 18

.set OS_ARRAY_1D = 1
.set OS_FT_DRAM = 1
.set OS_ARRAY_3D = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/arr_celladdr.asm"
.include "stdio/out_num8.asm"

Main:
	rjmp j8bCMainMmain
;======== enter CLASS Main ========================
_j8b_meta20:
	.db 12,0

;build method void 'Main.main()
j8bCMainMmain:
;build block
;invokeNative void System.setParam [byte, byte], params:LITERAL=2,LITERAL=18
;eNewArray byte[]
	ldi r16,15
	ldi r17,0
	push r30
	push r31
	rcall os_dram_alloc
	movw r26,r30
	pop r31
	pop r30
	movw r16,xl
	st x+,c0x00
	st x+,C0x01
	ldi r19,9
	st x+,r19
	ldi r19,10
	st x+,r19
	st x+,c0x00
;accum->var 'arr'
	movw r20,r16
;push cells: REG[20,21]
	push r20
	push r21
;invokeClassMethod byte Main.method1
	rcall j8bC22CMainMmethod1
;block free, size:2
	pop j8b_atom
	pop j8b_atom
;invokeNative void System.out [byte], params:ACCUM
	;load method param
	rcall os_out_num8
;push const '5.0'
	ldi r19,5
	push r19
;invokeClassMethod byte[] Main.method2
	rcall j8bC24CMainMmethod2
;block free, size:1
	pop j8b_atom
;push const '0.0'
	push c0x00
	push c0x00
;cells 'ACC[]'->ArrReg
	movw r26,r16
;compute array addr null->X
	rcall j8bproc_arr_celladdr
;arr 'ARRAY[]'->acc
	ld r16,x
;push accum(BE)
	push r16
;push cells: REG[20,21]
	push r20
	push r21
;invokeClassMethod byte Main.method1
	rcall j8bC22CMainMmethod1
;block free, size:2
	pop j8b_atom
	pop j8b_atom
;acc cast null[1]->short[2]
	ldi r17,0x00
;push accum(BE)
	push r17
	push r16
;cells 'REG[20,21]'->ArrReg
	movw r26,r20
;compute array addr null->X
	rcall j8bproc_arr_celladdr
;arr 'ARRAY[]'->acc
	ld r16,x
;accum PLUS cells STACK[x1] -> accum
	pop r19
	add r16,r19
;push accum(BE)
	push r16
;push const '0.0'
	push c0x00
	push c0x00
;cells 'REG[20,21]'->ArrReg
	movw r26,r20
;compute array addr null->X
	rcall j8bproc_arr_celladdr
;push cells: ARRAY[]
	ld r16,x+
	push r16
	push c0x00
;cells 'REG[20,21]'->ArrReg
	movw r26,r20
;compute array addr null->X
	rcall j8bproc_arr_celladdr
;push cells: ARRAY[]
	ld r16,x+
	push r16
	push c0x00
;cells 'REG[20,21]'->ArrReg
	movw r26,r20
;compute array addr null->X
	rcall j8bproc_arr_celladdr
;pop accum(BE)
	pop r16
;accum->arr ARRAY[]
;acc ->arr'ARRAY[]'
	st x,r16
;build var byte[] 'Main.main.arr', allocated REG[20,21]
;return, argsSize:0, varsSize:0, retSize:0
	ret
;block end
;method end

;build method byte 'Main.method1(byte[])
j8bC22CMainMmethod1:
;build block
;alloc stack, size:0
	push yl
	push yh
	lds yl,SPL
	lds yh,SPH
;push const '1.0'
	push c0x01
	push c0x00
;cells 'ARGS[0,1]'->ArrReg
	ldd r26,y+6
	ldd r27,y+5
;compute array addr null->X
	rcall j8bproc_arr_celladdr
;arr 'ARRAY[]'->acc
	ld r16,x
;return, argsSize:2, varsSize:0, retSize:1
	ret
;block end
;build var byte[] 'Main.method1.barr', allocated ARGS[0,1]
;method end
;build var byte[] 'Main.method1.barr', allocated ARGS[0,1]

;build method byte[] 'Main.method2(byte)
j8bC24CMainMmethod2:
;build block
;alloc stack, size:0
	push yl
	push yh
	lds yl,SPL
	lds yh,SPH
;var 'b'->accum
	ldd r16,y+5
;push stack iReg
	push r28
	push r29
;acc cast null[1]->short[2]
	ldi r17,0x00
;push accum(BE)
	push r17
	push r16
;eNewArray byte[]
	lds yl,SPL
	lds yh,SPH
	adiw yl,0x01
	ld r16,y+
	ld r17,y+
	subi r16,low(-5)
	sbci r17,high(-5)
	push r30
	push r31
	rcall os_dram_alloc
	movw r26,r30
	pop r31
	pop r30
	movw r16,xl
	st x+,c0x00
	st x+,C0x01
	ldi r19,9
	st x+,r19
	pop r19
	st x+,r19
	pop r19
	st x+,r19
;pop stack iReg
	pop r29
	pop r28
;return, argsSize:1, varsSize:0, retSize:2
	ret
;block end
;build var byte 'Main.method2.b', allocated ARGS[0]
;method end
;======== leave CLASS Main ========================
