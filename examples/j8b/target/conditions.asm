; vm5277.avr_codegen v0.1 at Fri Sep 26 15:18:00 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_DRAM = 1
.set OS_FT_WELCOME = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "stdio/out_char.asm"
.include "stdio/out_num8.asm"

Main:
	rjmp j8bCMainMmain
;======== enter CLASS Main ========================
_j8b_meta30:
	.db 13,0

;build method void 'Main.main()
j8bCMainMmain:
;build block
;invokeNative void System.setParam [byte, byte], params:LITERAL=0,LITERAL=16
;invokeNative void System.setParam [byte, byte], params:LITERAL=2,LITERAL=18
;invokeNative void System.setParam [byte, byte], params:LITERAL=3,LITERAL=1
;build block
;invokeNative void System.outChar [byte], params:LITERAL=65
	;load method param
	ldi r16,65
	rcall os_out_char
;block end
;build block
;invokeNative void System.outChar [byte], params:LITERAL=67
	;load method param
	ldi r16,67
	rcall os_out_char
;block end
;build block
;invokeNative void System.outChar [byte], params:LITERAL=68
	;load method param
	ldi r16,68
	rcall os_out_char
;block end
;const '0.0'->cells REG[20]
	ldi r20,0
;const '1.0'->cells REG[20]
	ldi r20,1
;eIf
;cells REG[20] EQ 3 -> accum (isOr:false, isNot:false)
	cpi r20,3
	brne _j8b_eoc41
;build block
;invokeNative void System.outChar [byte], params:LITERAL=32
	;load method param
	ldi r16,32
	rcall os_out_char
;block end
_j8b_eoc41:
;const '0.0'->cells REG[21]
	ldi r21,0
;const '0.0'->cells REG[21]
	ldi r21,0
;eIf
;var 'b1'->accum
	mov r16,r21
;accum bool is true
	tst r16
	breq  _j8b_eoc43
;build block
;invokeNative void System.out [byte], params:LITERAL=50
	;load method param
	ldi r16,50
	rcall os_out_num8
;block end
_j8b_eoc43:
;eIf
;cells REG[21] NEQ 0 -> accum (isOr:true, isNot:true)
	cpi r21,0
	brne _j8b_eoc45
;build block
;invokeNative void System.out [byte], params:LITERAL=51
	;load method param
	ldi r16,51
	rcall os_out_num8
;block end
_j8b_eoc45:
;const '1.0'->cells REG[22]
	ldi r22,1
;eUnary POST_INC cells REG[22]
	add r22,C0x01
;const '2.0'->cells REG[23]
	ldi r23,2
;eUnary POST_INC cells REG[23]
	add r23,C0x01
;eIf
;var 'j'->accum
	mov r16,r23
;accum PLUS cells REG[22] -> accum
	add r16,r22
;cells ACC[] LT 2 -> accum (isOr:false, isNot:false)
	cpi r16,2
	brcc _j8b_eoc47
;build block
;invokeNative void System.outChar [byte], params:LITERAL=70
	;load method param
	ldi r16,70
	rcall os_out_char
;block end
	rjmp _j8b_eob49
_j8b_eoc47:
;build block
;invokeNative void System.outChar [byte], params:LITERAL=71
	;load method param
	ldi r16,71
	rcall os_out_char
_j8b_eob49:
;block end
;build var byte 'Main.main.s1', allocated REG[20]
;build var bool 'Main.main.b1', allocated REG[21]
;build var byte 'Main.main.i', allocated REG[22]
;build var byte 'Main.main.j', allocated REG[23]
;acc cast null[1]->void[0]
;return, argsSize:0, varsSize:0, retSize:0
	ret
;block end
;method end
;======== leave CLASS Main ========================
