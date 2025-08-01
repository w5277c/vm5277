; vm5277.avr_codegen v0.1 at Sat Aug 02 05:10:15 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_WELCOME = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "stdio/out_num8.asm"

	ldi r30,low(0)
	ldi r31,high(0)
	ldi r16,0
	std z+0,r16
JavlCByteMtoByte13:
	mov r30,r16
	ret
MAIN:
JavlCMainMmain14:
	jmp JavlCByteMtoByte13
	mov r16,zl
	call os_out_num8
	ret
