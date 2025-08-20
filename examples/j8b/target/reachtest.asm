; vm5277.avr_codegen v0.1 at Tue Aug 19 19:13:56 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_WELCOME = 1
.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1

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
;build class A,id:77
;build method byte 'A.x(), id:78
j8bCAMx12:
;build block A.x,id:79
;const '10'->accum 
	ldi r16,10
	ret
;block end
;method end
;class end
;build method void 'Main.main(), id:80
j8bCmainMmain:
;build block Main.main,id:81
;invokeNative void System.setParam [byte, byte], params:LITERAL=0,LITERAL=16
;invokeNative void System.setParam [byte, byte], params:LITERAL=2,LITERAL=18
;invokeNative void System.setParam [byte, byte], params:LITERAL=3,LITERAL=1
;stack addr to stack iReg
	lds yl,SPL
	lds yh,SPH
;invokeMethod byte A.x
	mcall j8bCAMx12
;invokeNative void System.out [byte], params:ACCUM
	;load method param
	mcall os_out_num8
;invokeNative void System.stop
	mcall mcu_stop
;block end
;method end
	ret
;class end
