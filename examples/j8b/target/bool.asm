; vm5277.avr_codegen v0.1 at Fri Sep 26 15:20:13 VLAT 2025
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_bool.asm"

Main:
	rjmp j8bCMainMmain
;======== enter CLASS Main ========================
_j8b_meta20:
	.db 12,0

;build method bool 'Main.test(bool,bool)
;build block
;alloc stack, size:0
	push yl
	push yh
	lds yl,SPL
	lds yh,SPH
;var '_b1'->accum
	ldd r16,y+6
;push accum(BE)
	push r16
;var '_b2'->accum
	ldd r16,y+5
;accum AND cells STACK[x1] -> accum
	pop j8b_atom
	and r16,j8b_atom
;return, argsSize:0, varsSize:0, retSize:1
	ret
;block end
;method end

;build method void 'Main.main()
j8bCMainMmain:
;build block
;invokeNative void System.setParam [byte, byte], params:LITERAL=2,LITERAL=18
;const '0.0'->cells REG[20]
	ldi r20,0
;eUnary NOT cells REG[20]
	com r20
;const '0.0'->cells REG[21]
	ldi r21,0
;eUnary NOT cells REG[21]
	com r21
;const '1.0'->cells REG[22]
	ldi r22,1
;eUnary NOT cells REG[22]
	com r22
;invokeNative void System.out [bool], params:LITERAL=1
	;load method param
	ldi r16,1
	rcall os_out_bool
;invokeNative void System.out [bool], params:LITERAL=0
	;load method param
	ldi r16,0
	rcall os_out_bool
;invokeNative void System.out [bool], params:LITERAL=0
	;load method param
	ldi r16,0
	rcall os_out_bool
;invokeNative void System.out [bool], params:LITERAL=1
	;load method param
	ldi r16,1
	rcall os_out_bool
;invokeNative void System.out [bool], params:LOCAL_RES=26
	;load method param
	mov r16,r21
	rcall os_out_bool
;invokeNative void System.out [bool], params:LOCAL_RES=27
	;load method param
	mov r16,r22
	rcall os_out_bool
;eUnary NOT cells REG[21] -> accum
	mov r16,r21
	com r16
;invokeNative void System.out [bool], params:ACCUM
	;load method param
	rcall os_out_bool
;eUnary NOT cells REG[22] -> accum
	mov r16,r22
	com r16
;invokeNative void System.out [bool], params:ACCUM
	;load method param
	rcall os_out_bool
;eUnary NOT cells REG[21] -> accum
	mov r16,r21
	com r16
;accum->var 'b3'
	mov r23,r16
;invokeNative void System.out [bool], params:LOCAL_RES=28
	;load method param
	mov r16,r23
	rcall os_out_bool
;eIf
;var 'b2'->accum
	mov r16,r22
;accum EQ REG[21] -> accum (isOr:false, isNot:false)
	cp r16,r21
	brne _j8b_eoc24
;build block
;invokeNative void System.out [byte], params:LITERAL=48
	;load method param
	ldi r16,48
	rcall os_out_num8
;block end
_j8b_eoc24:
;eIf
;var 'b1'->accum
	mov r16,r21
;accum bool is true
	tst r16
	breq  _j8b_eoc26
;build block
;invokeNative void System.out [byte], params:LITERAL=49
	;load method param
	ldi r16,49
	rcall os_out_num8
;block end
_j8b_eoc26:
;eIf
;var 'b2'->accum
	mov r16,r22
;accum bool is true
	tst r16
	breq  _j8b_eoc28
;build block
;invokeNative void System.out [byte], params:LITERAL=50
	;load method param
	ldi r16,50
	rcall os_out_num8
;block end
_j8b_eoc28:
;eIf
;cells REG[22] NEQ 0 -> accum (isOr:true, isNot:true)
	cpi r22,0
	brne _j8b_eoc30
;build block
;invokeNative void System.out [byte], params:LITERAL=51
	;load method param
	ldi r16,51
	rcall os_out_num8
;block end
_j8b_eoc30:
;eIf
;cells REG[21] NEQ 0 -> accum (isOr:true, isNot:false)
	cpi r21,0
	brne _j8b_eolb39
;cells REG[22] NEQ 0 -> accum (isOr:true, isNot:false)
	cpi r22,0
	breq _j8b_eoc32
_j8b_eolb39:
;build block
;invokeNative void System.out [byte], params:LITERAL=52
	;load method param
	ldi r16,52
	rcall os_out_num8
;block end
_j8b_eoc32:
;eIf
;cells REG[21] NEQ 0 -> accum (isOr:false, isNot:false)
	cpi r21,0
	breq _j8b_eoc34
;cells REG[22] NEQ 0 -> accum (isOr:false, isNot:false)
	cpi r22,0
	breq _j8b_eoc34
;build block
;invokeNative void System.out [byte], params:LITERAL=53
	;load method param
	ldi r16,53
	rcall os_out_num8
;block end
_j8b_eoc34:
;push heap iReg
	push zl
;push cells: REG[21]
	push r21
;push cells: REG[22]
	push r22
;invokeClassMethod bool Main.test
	rcall j8bCMainMmain
;block free, size:2
	pop j8b_atom
	pop j8b_atom
;pop heap iReg
	pop zl
;invokeNative void System.out [bool], params:ACCUM
	;load method param
	rcall os_out_bool
;var 'b1'->accum
	mov r16,r21
;push accum(BE)
	push r16
;var 'b2'->accum
	mov r16,r22
;accum OR cells STACK[x1] -> accum
	pop j8b_atom
	or r16,j8b_atom
;push accum(BE)
	push r16
;var 'b10'->accum
	mov r16,r20
;accum OR cells STACK[x1] -> accum
	pop j8b_atom
	or r16,j8b_atom
;accum->var 'b4'
	mov r24,r16
;invokeNative void System.out [bool], params:LOCAL_RES=35
	;load method param
	mov r16,r24
	rcall os_out_bool
;var 'b1'->accum
	mov r16,r21
;push accum(BE)
	push r16
;var 'b2'->accum
	mov r16,r22
;accum AND cells STACK[x1] -> accum
	pop j8b_atom
	and r16,j8b_atom
;accum->var 'b5'
	mov r25,r16
;invokeNative void System.out [bool], params:LOCAL_RES=36
	;load method param
	mov r16,r25
	rcall os_out_bool
;eUnary NOT cells REG[21] -> accum
	mov r16,r21
	com r16
;accum->var 'b6'
	mov r26,r16
;invokeNative void System.out [bool], params:LOCAL_RES=37
	;load method param
	mov r16,r26
	rcall os_out_bool
;build var bool 'Main.main.b10', allocated REG[20]
;build var bool 'Main.main.b1', allocated REG[21]
;build var bool 'Main.main.b2', allocated REG[22]
;build var bool 'Main.main.b3', allocated REG[23]
;build var bool 'Main.main.b4', allocated REG[24]
;build var bool 'Main.main.b5', allocated REG[25]
;build var bool 'Main.main.b6', allocated REG[26]
;acc cast null[1]->void[0]
;return, argsSize:2, varsSize:0, retSize:0
	ret
;block end
;build var bool 'Main.main._b1', allocated ARGS[0]
;build var bool 'Main.main._b2', allocated ARGS[1]
;method end
;======== leave CLASS Main ========================
