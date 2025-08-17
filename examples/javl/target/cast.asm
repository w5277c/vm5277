; vm5277.avr_codegen v0.1 at Mon Aug 18 02:27:42 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "stdio/out_char.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_cstr.asm"
.include "stdio/out_num32.asm"

JavlD80:
.db "orig num:",0x00

JavlD81:
.db "short:",0x00,0x00

JavlD82:
.db "byte:",0x00

Main:
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
	jmp JavlCMainMmain
JavlCmainMmain:
	ldi r20,1
	ldi r21,2
	ldi r22,3
	ldi r23,4
	ldi r30,low(JavlD80*2)
	ldi r31,high(JavlD80*2)
	mcall os_out_cstr
	mov r16,r20
	mov r17,r21
	mov r18,r22
	mov r19,r23
	mcall os_out_num32
	ldi r16,10
	mcall os_out_char
	mov r16,r20
	mov r17,r21
	mov r18,r22
	mov r19,r23
	mov r16,r20
	mov r17,r21
	mov r18,r22
	mov r19,r23
	ldi r18,0x00
	ldi r19,0x00
	mov r20,r16
	mov r21,r17
	mov r22,r18
	mov r23,r19
	ldi r30,low(JavlD81*2)
	ldi r31,high(JavlD81*2)
	mcall os_out_cstr
	mov r16,r20
	mov r17,r21
	mov r18,r22
	mov r19,r23
	mcall os_out_num32
	ldi r16,10
	mcall os_out_char
	ldi r30,low(JavlD82*2)
	ldi r31,high(JavlD82*2)
	mcall os_out_cstr
	mov r16,r20
	mov r17,r21
	mov r18,r22
	mov r19,r23
	mov r16,r20
	mcall os_out_num8
	ldi r16,10
	mcall os_out_char
	ret
