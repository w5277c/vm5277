; vm5277.avr_codegen v0.1 at Sun Aug 10 03:21:17 VLAT 2025
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
	ldi r30,low(0)
	ldi r31,high(0)
	ldi r16,0
	std z+0,r16
JavlCByteMconstr12:
	ldd r16,y+0
	ldi zl,low(0)
	ldi zh,high(0)
	st z,r16
	ret
JavlCByteMtoByte13:
	ldi zl,low(0)
	ldi zh,high(0)
	ldd r16,z+0
	mov r30,r16
	ret
JavlCMainMmain14:
	ldi r16,8
	ldi r17,0
	mcall os_dram_alloc
	ldi r30,low(JavlCMainMmainRP15)
	push r30
	ldi r30,high(JavlCMainMmainRP15)
	push r30
	ldi r30,1
	push r30
	jmp JavlCByteMconstr12
JavlCMainMmainRP15:
	jmp JavlCByteMtoByte13
	mov r16,zl
	call os_out_num8
	ret
