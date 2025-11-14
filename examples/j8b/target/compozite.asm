; vm5277.avr_codegen v0.1 at Fri Nov 14 05:30:13 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_DRAM = 1
.set OS_FT_WELCOME = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/mfin.asm"
.include "stdio/out_char.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_cstr.asm"
.include "sys/mcu_stop.asm"
.include "stdio/out_num32.asm"

j8bD84:
.db "Hello world!",0x00,0x00

Main:
	rjmp j8bCMainMmain
_j8b_meta21:
	.db 13,0

j8bCMainMmain:
	ldi r16,1
	rcall os_out_num8
	ldi r16,2
	rcall os_out_num8
	ldi r16,3
	rcall os_out_num8
	ldi r16,low(j8bD84*2)
	ldi r17,high(j8bD84*2)
	movw r30,r16
	rcall os_out_cstr
	ldi r16,10
	rcall os_out_char
	ldi r20,65
	add r20,C0x01
	mov r16,r20
	rcall os_out_char
	ldi r16,33
	rcall os_out_char
	ldi r22,1
	ldi r23,2
	ldi r24,0
	ldi r25,0
	movw r16,r22
	movw r18,r24
	rcall os_out_num32
	add r22,C0x01
	adc r23,C0x00
	adc r24,C0x00
	adc r25,C0x00
	movw r16,r22
	movw r18,r24
	rcall os_out_num32
	rcall mcu_stop
	rjmp j8bproc_mfin
