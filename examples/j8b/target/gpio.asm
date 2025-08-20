; vm5277.avr_codegen v0.1 at Tue Aug 19 18:53:06 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_WELCOME = 1
.set OS_FT_DRAM = 1
.set OS_FT_TIMER1 = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "io/port_invert.asm"
.include "io/port_mode_out.asm"
.include "core/wait_ms.asm"

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
	ldi r16,14
	std z+4,r16
	movw r16,zl
	jmp j8bCMainMmain
;build class Main,id:0
;build method void 'Main.main(), id:81
j8bCmainMmain:
;build block Main.main,id:82
;invokeNative void System.setParam [byte, byte], params:LITERAL=0,LITERAL=16
;invokeNative void System.setParam [byte, byte], params:LITERAL=2,LITERAL=18
;invokeNative void System.setParam [byte, byte], params:LITERAL=3,LITERAL=1
;build var byte 'Main.main.port', allocated REG[20]
;const '17'->cells REG[20]
	ldi r20,17
;invokeNative void GPIO.modeOut [byte], params:LOCAL_RES=83
	;load method param
	mov r16,r20
	mcall port_mode_out
j8bCMainMmainB82lWHILEBODY16:
;build block Main.main,id:84
;invokeNative void Thread.waitMS [short], params:LITERAL=250
	;load method param
	ldi r16,250
	ldi r17,0
	mcall wait_ms
;invokeNative void GPIO.invert [byte], params:LOCAL_RES=83
	;load method param
	mov r16,r20
	mcall port_invert
;block end
	mjmp j8bCMainMmainB82lWHILEBODY16
j8bCMainMmainB82lWHILEEND17:
;block end
;restore regs:
;method end
	ret
;class end
