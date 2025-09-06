; vm5277.avr_codegen v0.1 at Thu Sep 04 03:09:59 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "stdio/out_char.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_cstr.asm"
.include "sys/mcu_stop.asm"
.include "stdio/out_num32.asm"

j8bD82:
.db "Hello world!",0x00,0x00

Main:
	rjmp j8bCMainMmain

j8bCmainMmain:
	ldi r20,1
	ldi r21,2
	mov r16,r21
	add r16,r20
	mov r22,r16
	mov r16,r20
	rcall os_out_num8
	mov r16,r21
	rcall os_out_num8
	mov r16,r22
	rcall os_out_num8
	ldi r30,low(j8bD82*2)
	ldi r31,high(j8bD82*2)
	rcall os_out_cstr
	ldi r16,10
	rcall os_out_char
	ldi r23,65
	mov r16,r23
	mov r16,r23
	rcall os_out_char
	ldi r16,33
	rcall os_out_char
	ldi r24,1
	ldi r25,2
	ldi r26,0
	ldi r27,0
	mov r16,r24
	mov r17,r25
	mov r18,r26
	mov r19,r27
	rcall os_out_num32
	mov r16,r24
	mov r17,r25
	mov r18,r26
	mov r19,r27
	mov r16,r24
	mov r17,r25
	mov r18,r26
	mov r19,r27
	rcall os_out_num32
	rcall mcu_stop
	ret
