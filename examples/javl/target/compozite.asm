; vm5277.avr_codegen v0.1 at Mon Aug 18 03:02:44 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1
.set OS_FT_WELCOME = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "stdio/out_char.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_cstr.asm"
.include "sys/mcu_stop.asm"
.include "stdio/out_num32.asm"

JavlD82:
.db "Hello world!",0x00,0x00

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
	mov r16,r20
	add r16,r21
	mov r22,r16
	mov r16,r20
	mcall os_out_num8
	mov r16,r21
	mcall os_out_num8
	mov r16,r22
	mcall os_out_num8
	ldi r30,low(JavlD82*2)
	ldi r31,high(JavlD82*2)
	mcall os_out_cstr
	ldi r16,10
	mcall os_out_char
	ldi r23,65
	mov r16,r23
	add r23,C0x01
	mov r16,r23
	mcall os_out_char
	ldi r16,33
	mcall os_out_char
	ldi r24,1
	ldi r25,2
	ldi r26,0
	ldi r27,0
	mov r16,r24
	mov r17,r25
	mov r18,r26
	mov r19,r27
	mcall os_out_num32
	mov r16,r24
	mov r17,r25
	mov r18,r26
	mov r19,r27
	add r24,C0x01
	adc r25,C0x00
	adc r26,C0x00
	adc r27,C0x00
	mov r16,r24
	mov r17,r25
	mov r18,r26
	mov r19,r27
	mcall os_out_num32
	mcall mcu_stop
	ret
