; vm5277.avr_codegen v0.1 at Tue Aug 19 18:52:09 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_WELCOME = 1
.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "stdio/out_char.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_cstr.asm"
.include "sys/mcu_stop.asm"
.include "stdio/out_num32.asm"

j8bD82:
.db "Hello world!",0x00,0x00

Main:
;Launch 
	ldi r16,5
	ldi r17,0
	mcall os_dram_alloc
	std z+0,r16
	std z+1,r17
	std z+2,c0xff
	ldi r16,1
	std z+3,r16
	ldi r16,13
	std z+4,r16
	movw r16,zl
	jmp j8bCMainMmain
;build class Main,id:0
;build method void 'Main.main(), id:77
j8bCmainMmain:
;build block Main.main,id:78
;invokeNative void System.setParam [byte, byte], params:LITERAL=0,LITERAL=16
;invokeNative void System.setParam [byte, byte], params:LITERAL=2,LITERAL=18
;invokeNative void System.setParam [byte, byte], params:LITERAL=3,LITERAL=1
;build var byte 'Main.main.i1', allocated REG[20]
;const '1'->cells REG[20]
	ldi r20,1
;build var byte 'Main.main.i2', allocated REG[21]
;const '2'->cells REG[21]
	ldi r21,2
;var 'i2'->accum
	mov r16,r21
;accum PLUS cells REG[20] -> accum
	add r16,r20
;build var byte 'Main.main.sum', allocated REG[22]
;accum->var 'sum'
	mov r22,r16
;invokeNative void System.out [byte], params:LOCAL_RES=79
	;load method param
	mov r16,r20
	mcall os_out_num8
;invokeNative void System.out [byte], params:LOCAL_RES=80
	;load method param
	mov r16,r21
	mcall os_out_num8
;invokeNative void System.out [byte], params:LOCAL_RES=81
	;load method param
	mov r16,r22
	mcall os_out_num8
;invokeNative void System.outCStr [cstr], params:FLASH_RES=82
	ldi r30,low(j8bD82*2)
	ldi r31,high(j8bD82*2)
	mcall os_out_cstr
;invokeNative void System.outChar [byte], params:LITERAL=10
	;load method param
	ldi r16,10
	mcall os_out_char
;build var byte 'Main.main.num', allocated REG[23]
;const '65'->cells REG[23]
	ldi r23,65
;var 'num'->accum
	mov r16,r23
;eUnary POST_INC cells REG[23] -> accum
	add r23,C0x01
;invokeNative void System.outChar [byte], params:LOCAL_RES=87
	;load method param
	mov r16,r23
	mcall os_out_char
;invokeNative void System.outChar [byte], params:LITERAL=33
	;load method param
	ldi r16,33
	mcall os_out_char
;build var int 'Main.main.i', allocated REG[24,25,26,27]
;const '513'->cells REG[24,25,26,27]
	ldi r24,1
	ldi r25,2
	ldi r26,0
	ldi r27,0
;invokeNative void System.out [int], params:LOCAL_RES=88
	;load method param
	mov r16,r24
	mov r17,r25
	mov r18,r26
	mov r19,r27
	mcall os_out_num32
;var 'i'->accum
	mov r16,r24
	mov r17,r25
	mov r18,r26
	mov r19,r27
;eUnary POST_INC cells REG[24,25,26,27] -> accum
	add r24,C0x01
	adc r25,C0x00
	adc r26,C0x00
	adc r27,C0x00
;invokeNative void System.out [int], params:LOCAL_RES=88
	;load method param
	mov r16,r24
	mov r17,r25
	mov r18,r26
	mov r19,r27
	mcall os_out_num32
;invokeNative void System.stop
	mcall mcu_stop
;block end
;restore regs:
;method end
	ret
;class end
