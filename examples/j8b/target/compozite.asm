; vm5277.avr_codegen v0.2
.equ CORE_FREQ = 16
.equ STDOUT_PORT = 18

.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "stdio/out_char.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_cstr.asm"
.include "stdio/out_num32.asm"

j8bD100:
.db "Hello world!",0x00,0x00

Main:
	jmp j8b_CMainMmain
_j8b_meta_32:
	.db 15,0

j8b_CMainMmain:
	ldi r16,1
	call os_out_num8
	ldi r16,2
	call os_out_num8
	ldi r16,3
	call os_out_num8
	ldi r16,low(j8bD100*2)
	ldi r17,high(j8bD100*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,10
	call os_out_char
	ldi r20,65
	add r20,c0x01
	mov r16,r20
	call os_out_char
	ldi r16,33
	call os_out_char
	ldi r22,1
	ldi r23,2
	ldi r24,0
	ldi r25,0
	movw r16,r22
	movw r18,r24
	call os_out_num32
	add r22,c0x01
	adc r23,c0x00
	adc r24,c0x00
	adc r25,c0x00
	movw r16,r22
	movw r18,r24
	call os_out_num32
	jmp mcu_halt
