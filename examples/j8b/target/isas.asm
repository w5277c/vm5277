; vm5277.avr_codegen v0.1 at Tue Aug 19 18:57:20 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_WELCOME = 1
.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "stdio/out_num16.asm"
.include "stdio/out_cstr.asm"

j8bD91:
.db " is short",0x0a,0x00,0x00

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
;build class Short,id:78
;build field short 'Short.value', allocated HEAP[0,1]
;const '0'->cells HEAP[0,1]
	push yl
	ldi yl,0
	std z+6,yl
	ldi yl,0
	std z+7,yl
	pop yl
;build method null 'Short.constr(short), id:80
j8bCShortMconstr12:
;build block Short.constr,id:81
;var 'value'->accum
	ldd r16,y+0
	ldd r17,y+1
;accum->field value
	std z+6,r16
	std z+7,r17
;block end
;build var short 'Short.constr.value', allocated STACK_FRAME[0,1]
;method end
	ret
;build method short 'Short.toShort(), id:84
j8bCShortMtoShort14:
;build block Short.toShort,id:85
;field 'value'->accum
	ldd r16,z+6
	ldd r17,z+7
	ret
;block end
;method end
;class end
;build method void 'Main.main(), id:86
j8bCmainMmain:
;build block Main.main,id:87
;invokeNative void System.setParam [byte, byte], params:LITERAL=0,LITERAL=16
;invokeNative void System.setParam [byte, byte], params:LITERAL=2,LITERAL=18
;invokeNative void System.setParam [byte, byte], params:LITERAL=3,LITERAL=1
;eNew [Short,Number], heap size:6
	ldi r16,6
	ldi r17,0
	mcall os_dram_alloc
	std z+0,r16
	std z+1,r17
	std z+2,c0x00
	ldi r16,2
	std z+3,r16
	ldi r16,15
	std z+4,r16
	ldi r16,14
	std z+5,r16
	movw r16,zl
;accum->var 'num2'
	mov r20,r16
	mov r21,r17
;refCount++ for REG[20,21]
	push r16
	push yl
	push yh
	mov yl,r20
	mov yh,r21
	ldd r16,y+0x02
	cpse r16,c0xff
	inc r16
	std y+0x02,r16
	pop yh
	pop yl
	pop r16
;push stack iReg
	push yl
	push yh
;push const '258'
	ldi yl,2
	push yl
	ldi yl,1
	push yl
;stack addr to stack iReg
	lds yl,SPL
	lds yh,SPH
;invokeMethod Short Short.Short
	mcall j8bCShortMconstr12
;free stack, size:2
	pop yl
	pop yl
;pop stack iReg
	pop yh
	pop yl
;build var Object 'Main.main.num2', allocated REG[20,21]
if_begin15:
;eIf
;var 'num2'->accum
	mov r16,r20
	mov r17,r21
;push heap iReg
	push zl
	push zh
;setHeap REG[20,21]
	mov r30,r20
	mov r31,r21
;eInstanceOf Short
	push r17
	adiw zl,0x03
	ld r16,z+
	ld r17,z+
	cpi r17,15
	breq pc+0x04
	dec r16
	brne pc-0x04
	rjmp pc+0x02
	ldi r16,0x01
	pop r17
;pop heap iReg
	pop zh
	pop zl
	cpi r16,0x01
	brne if_end18
if_then16:
;build block Main.main,id:89
;var 'num2'->accum
	mov r16,r20
	mov r17,r21
;push heap iReg
	push zl
	push zh
;setHeap REG[20,21]
	mov r30,r20
	mov r31,r21
;stack addr to stack iReg
	lds yl,SPL
	lds yh,SPH
;invokeMethod short Short.toShort
	mcall j8bCShortMtoShort14
;pop heap iReg
	pop zh
	pop zl
;invokeNative void System.out [short], params:ACCUM
	;load method param
	mcall os_out_num16
;invokeNative void System.outCStr [cstr], params:FLASH_RES=91
	ldi r30,low(j8bD91*2)
	ldi r31,high(j8bD91*2)
	mcall os_out_cstr
;block end
if_end18:
;block end
;restore regs:
;method end
	ret
;class end
