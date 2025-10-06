; vm5277.avr_codegen v0.1 at Sat Sep 20 04:36:08 VLAT 2025
.equ stdout_port = 18

.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "stdio/out_num16.asm"
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
;const '0.0'->cells REG[20]
	ldi r20,0
;const '0.0'->cells REG[21,22]
	ldi r21,0
	ldi r22,0
;const '0.0'->cells REG[23,24,25,26]
	ldi r23,0
	ldi r24,0
	ldi r25,0
	ldi r26,0
;const '0.0'->cells REG[27]
	ldi r27,0
;const '1.0'->cells REG[20]
	ldi r20,1
;const '515.0'->cells REG[21,22]
	ldi r21,3
	ldi r22,2
;const '6.7438087E7'->cells REG[23,24,25,26]
	ldi r23,7
	ldi r24,6
	ldi r25,5
	ldi r26,4
;const '8.0'->cells REG[27]
	ldi r27,8
;invokeNative void System.out [bool], params:LOCAL_RES=23
	;load method param
	mov r16,r20
	rcall os_out_bool
;invokeNative void System.out [short], params:LOCAL_RES=24
	;load method param
	mov r16,r21
	mov r17,r22
	rcall os_out_num16
;invokeNative void System.out [int], params:LOCAL_RES=25
	;load method param
	mov r16,r23
	mov r17,r24
	mov r18,r25
	mov r19,r26
	rcall os_out_num32
;invokeNative void System.out [byte], params:LOCAL_RES=26
	;load method param
	mov r16,r27
	rcall os_out_num8
;build var bool 'Main.main.b1', allocated REG[20]
;build var short 'Main.main.s1', allocated REG[21,22]
;build var int 'Main.main.i1', allocated REG[23,24,25,26]
;build var byte 'Main.main.t1', allocated REG[27]
;acc cast null[1]->void[0]
;return, argsSize:0, varsSize:0, retSize:0
	ret
;block end
;method end
;======== leave CLASS Main ========================
