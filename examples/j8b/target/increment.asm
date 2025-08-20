; vm5277.avr_codegen v0.1 at Tue Aug 19 18:54:55 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_DRAM = 1
.set OS_FT_WELCOME = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "stdio/out_char.asm"
.include "sys/mcu_stop.asm"

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
;build var byte 'Main.main.num', allocated REG[21]
;const '32'->cells REG[21]
	ldi r21,32
;build var byte 'Main.main.ch', allocated REG[20]
;const '65'->cells REG[20]
	ldi r20,65
;invokeNative void System.outChar [byte], params:LOCAL_RES=80
	;load method param
	mov r16,r20
	mcall os_out_char
;var 'ch'->accum
	mov r16,r20
;eUnary POST_INC cells REG[20] -> accum
	add r20,C0x01
;invokeNative void System.outChar [byte], params:LOCAL_RES=80
	;load method param
	mov r16,r20
	mcall os_out_char
;var 'ch'->accum
	mov r16,r20
;eUnary POST_INC cells REG[20] -> accum
	add r20,C0x01
;invokeNative void System.outChar [byte], params:LOCAL_RES=80
	;load method param
	mov r16,r20
	mcall os_out_char
;var 'ch'->accum
	mov r16,r20
;eUnary POST_INC cells REG[20] -> accum
	add r20,C0x01
;var 'num'->accum
	mov r16,r21
;accum PLUS cells REG[20] -> accum
	add r16,r20
;accum->var 'ch'
	mov r20,r16
;invokeNative void System.outChar [byte], params:LOCAL_RES=80
	;load method param
	mov r16,r20
	mcall os_out_char
;invokeNative void System.stop
	mcall mcu_stop
;block end
;restore regs:
;method end
	ret
;class end
