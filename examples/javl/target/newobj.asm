; vm5277.avr_codegen v0.1 at Wed Aug 13 01:03:04 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "stdio/out_num8.asm"

MAIN:
;build class Main,id:0
;build class Byte,id:77
;build field byte 'Byte.value', id:78, size:1
;alloc Byte.value [HEAP:0]
;const '0'->cells[HEAP:0]
	ldi r30,low(0)
	ldi r31,high(0)
	ldi r16,0
	std z+0,r16
;build method null 'Byte.constr(byte), id:79
JavlCByteMconstr12:
;build block Byte.constr,id:80
;var 'value'->accum
	ldd r16,y+0
;accum->field value
	ldi zl,low(0)
	ldi zh,high(0)
	st z,r16
;block end
;build var byte 'Byte.constr.value', id:88, size:1
;alloc Byte.constr.value [STACK:0]
;method end
	ret
;build method byte 'Byte.toByte(), id:81
JavlCByteMtoByte13:
;build block Byte.toByte,id:82
;field 'value'->accum
	ldi zl,low(0)
	ldi zh,high(0)
	ldd r16,z+0
	mov r30,r16
	ret
;block end
;method end
;class end
;build method void 'Main.main(), id:85
JavlCMainMmain15:
;build block Main.main,id:86
;invokeNative void System.setParam [byte, byte], params:[LITERAL=0, LITERAL=16]
;invokeNative void System.setParam [byte, byte], params:[LITERAL=2, LITERAL=18]
;invokeNative void System.setParam [byte, byte], params:[LITERAL=3, LITERAL=1]
;eNew [Byte Number], heap size:6
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
	movw r16,r30
;accum->var 'b'
	mov r20,r16
	mov r21,r17
;refCount++ for [REG:20,REG:21]
	push r16
	push_y
	mov yl,r20
	mov yh,r21
	ldd r16,y+0x02
	cpse r16,c0xff
	inc r16
	std y+0x02,r16
	pop_y
	pop r16
	;push ret point
	ldi yl,low(JavlCMainMmainRP16)
	push yl
	ldi yl,high(JavlCMainMmainRP16)
	push yl
	ldi yl,1
	push yl
;invokeMethod Byte Byte.Byte
	jmp JavlCByteMconstr12
JavlCMainMmainRP16:
;build var Byte 'Main.main.b', id:87, size:2
;alloc Main.main.b [REG:20, REG:21]
;var 'b'->accum
	mov r16,r20
	mov r17,r21
;cellsToRet [REG:20,REG:21]
	mov zl,r20
	mov zh,r21
;invokeMethod byte Byte.toByte
	call JavlCByteMtoByte13
;invokeNative void System.out [byte], params:[RETURN]
	mov r16,zl
	call os_out_num8
;block end
;restore regs: 
;method end
	ret
;class end
