; vm5277.avr_codegen v0.1 at Fri Sep 26 15:12:49 VLAT 2025
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
.include "stdio/out_cstr.asm"
.include "sys/mcu_stop.asm"
.include "stdio/out_num32.asm"

j8bD90:
.db "Hello world!",0x00,0x00

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
;invokeNative void System.out [byte], params:LITERAL=1
	;load method param
	ldi r16,1
	rcall os_out_num8
;invokeNative void System.out [byte], params:LITERAL=2
	;load method param
	ldi r16,2
	rcall os_out_num8
;invokeNative void System.out [byte], params:LITERAL=3
	;load method param
	ldi r16,3
	rcall os_out_num8
;invokeNative void System.outCStr [cstr], params:FLASH_RES=90
	ldi r30,low(j8bD90*2)
	ldi r31,high(j8bD90*2)
	rcall os_out_cstr
;invokeNative void System.outChar [byte], params:LITERAL=10
	;load method param
	ldi r16,10
	rcall os_out_char
;const '65.0'->cells REG[20]
	ldi r20,65
;eUnary POST_INC cells REG[20]
	add r20,C0x01
;invokeNative void System.outChar [byte], params:LOCAL_RES=95
	;load method param
	mov r16,r20
	rcall os_out_char
;invokeNative void System.outChar [byte], params:LITERAL=33
	;load method param
	ldi r16,33
	rcall os_out_char
;const '513.0'->cells REG[22,23,24,25]
	ldi r22,1
	ldi r23,2
	ldi r24,0
	ldi r25,0
;invokeNative void System.out [int], params:LOCAL_RES=96
	;load method param
	movw r16,r22
	movw r18,r24
	rcall os_out_num32
;eUnary POST_INC cells REG[22,23,24,25]
	add r22,C0x01
	adc r23,C0x00
	adc r24,C0x00
	adc r25,C0x00
;invokeNative void System.out [int], params:LOCAL_RES=96
	;load method param
	movw r16,r22
	movw r18,r24
	rcall os_out_num32
;invokeNative void System.stop
	rcall mcu_stop
;build var byte 'Main.main.num', allocated REG[20]
;build var int 'Main.main.i', allocated REG[22,23,24,25]
;acc cast null[1]->void[0]
;return, argsSize:0, varsSize:0, retSize:0
	ret
;block end
;method end
;======== leave CLASS Main ========================
