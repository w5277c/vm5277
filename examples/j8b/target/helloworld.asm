; vm5277.avr_codegen v0.1 at Tue Aug 19 18:53:59 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_WELCOME = 1
.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "stdio/out_cstr.asm"
.include "sys/mcu_stop.asm"

j8bD79:
.db "Hello world!",0x0d,0x0a,0xf0,0xd2,0xc9,0xd7,0xc5,0xd4,"!",0x0d,0x0a,0x00

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
;invokeNative void System.outCStr [cstr], params:FLASH_RES=79
	ldi r30,low(j8bD79*2)
	ldi r31,high(j8bD79*2)
	mcall os_out_cstr
;invokeNative void System.stop
	mcall mcu_stop
;block end
;method end
	ret
;class end
