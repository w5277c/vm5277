; vm5277.avr_codegen v0.1 at Tue Aug 19 19:00:57 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "stdio/out_num8.asm"
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
;build var byte 'Main.main.src1', allocated REG[21]
;const '1'->cells REG[21]
	ldi r21,1
;build var short 'Main.main.src2', allocated REG[22,23]
;const '2'->cells REG[22,23]
	ldi r22,2
	ldi r23,0
;push stack iReg
	push yl
	push yh
;push cells: REG[21]
	push r21
;push cells: REG[22,23]
	push r22
	push r23
;stack addr to stack iReg
	lds yl,SPL
	lds yh,SPH
;invokeMethod byte Main.inc
	mcall j8bCMainMinc12
;free stack, size:3
	pop yl
	pop yl
	pop yl
;pop stack iReg
	pop yh
	pop yl
;build var byte 'Main.main.result', allocated REG[20]
;accum->var 'result'
	mov r20,r16
;invokeNative void System.out [byte], params:LOCAL_RES=81
	;load method param
	mov r16,r20
	mcall os_out_num8
;invokeNative void System.stop
	mcall mcu_stop
;block end
;restore regs:
;method end
	ret
;build method byte 'Main.inc(byte,short), id:82
j8bCMainMinc12:
;build block Main.inc,id:83
;build var byte 'Main.inc.tmp', allocated REG[20]
;const '3'->cells REG[20]
	ldi r20,3
;var 'val2'->accum
	ldd r16,y+1
	ldd r17,y+2
;accum PLUS cells STACK_FRAME[0] -> accum
	push zl
	ldd zl,y+0
	add r16,zl
	pop zl
;accum PLUS cells REG[20] -> accum
	add r16,r20
;acc cast 2->1
	ret
;block end
;restore regs:
;build var byte 'Main.inc.val1', allocated STACK_FRAME[0]
;build var short 'Main.inc.val2', allocated STACK_FRAME[1,2]
;method end
;class end
