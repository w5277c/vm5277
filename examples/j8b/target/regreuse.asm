; vm5277.avr_codegen v0.1 at Tue Aug 19 19:02:52 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_WELCOME = 1
.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "stdio/out_num8.asm"

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
;build var byte 'Main.main.a', allocated REG[20]
;const '100'->cells REG[20]
	ldi r20,100
;build block Main.main,id:80
;build var byte 'Main.main.b', allocated REG[21]
;const '10'->cells REG[21]
	ldi r21,10
;build var byte 'Main.main.c', allocated REG[22]
;const '20'->cells REG[22]
	ldi r22,20
;var 'c'->accum
	mov r16,r22
;accum PLUS cells REG[21] -> accum
	add r16,r21
;accum PLUS cells REG[20] -> accum
	add r16,r20
;invokeNative void System.out [byte], params:ACCUM
	;load method param
	mcall os_out_num8
;block end
;restore regs:
;build block Main.main,id:83
;build var byte 'Main.main.d', allocated REG[21]
;const '30'->cells REG[21]
	ldi r21,30
;build var byte 'Main.main.e', allocated REG[22]
;const '40'->cells REG[22]
	ldi r22,40
;var 'e'->accum
	mov r16,r22
;accum PLUS cells REG[21] -> accum
	add r16,r21
;invokeNative void System.out [byte], params:ACCUM
	;load method param
	mcall os_out_num8
;build block Main.main,id:86
;build var byte 'Main.main.f', allocated REG[23]
;const '50'->cells REG[23]
	ldi r23,50
;invokeNative void System.out [byte], params:LOCAL_RES=87
	;load method param
	mov r16,r23
	mcall os_out_num8
;block end
;restore regs:
;block end
;restore regs:
;build var byte 'Main.main.g', allocated REG[22]
;const '60'->cells REG[22]
	ldi r22,60
;build var byte 'Main.main.h', allocated REG[21]
;const '70'->cells REG[21]
	ldi r21,70
;var 'g'->accum
	mov r16,r22
;accum PLUS cells REG[20] -> accum
	add r16,r20
;accum PLUS cells REG[21] -> accum
	add r16,r21
;invokeNative void System.out [byte], params:ACCUM
	;load method param
	mcall os_out_num8
;block end
;restore regs:
;method end
	ret
;class end
