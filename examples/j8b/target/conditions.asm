; vm5277.avr_codegen v0.1 at Tue Aug 19 18:52:47 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_DRAM = 1
.set OS_FT_WELCOME = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "stdio/out_char.asm"

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
;build block Main.main,id:79
;invokeNative void System.outChar [byte], params:LITERAL=65
	;load method param
	ldi r16,65
	mcall os_out_char
;block end
;build block Main.main,id:81
;invokeNative void System.outChar [byte], params:LITERAL=67
	;load method param
	ldi r16,67
	mcall os_out_char
;block end
;build block Main.main,id:82
;invokeNative void System.outChar [byte], params:LITERAL=68
	;load method param
	ldi r16,68
	mcall os_out_char
;block end
;build var byte 'Main.main.i', allocated REG[20]
;const '1'->cells REG[20]
	ldi r20,1
;build var byte 'Main.main.j', allocated REG[21]
;const '2'->cells REG[21]
	ldi r21,2
if_begin13:
;eIf
;var 'j'->accum
	mov r16,r21
;accum PLUS cells REG[20] -> accum
	add r16,r20
;accum LT 2 -> accum
	cpi r16,2
	rol r16
	cpi r16,0x01
	brne if_else15
if_then14:
;build block Main.main,id:86
;invokeNative void System.outChar [byte], params:LITERAL=70
	;load method param
	ldi r16,70
	mcall os_out_char
;block end
	rjmp if_end16
if_else15:
;build block Main.main,id:87
;invokeNative void System.outChar [byte], params:LITERAL=71
	;load method param
	ldi r16,71
	mcall os_out_char
;block end
if_end16:
;block end
;restore regs:
;method end
	ret
;class end
