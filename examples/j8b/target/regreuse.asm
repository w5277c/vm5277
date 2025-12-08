; vm5277.avr_codegen v0.2
.equ CORE_FREQ = 16
.equ STDOUT_PORT = 18

.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "stdio/out_num8.asm"

Main:
	jmp j8b_CMainMmain
_j8b_meta_32:
	.db 15,0

j8b_CMainMmain:
	ldi r20,100
	ldi r21,10
	ldi r22,20
	mov r16,r22
	add r16,r21
	add r16,r20
	call os_out_num8
	ldi r21,0
	ldi r22,0
	ldi r23,0
	ldi r21,30
	ldi r22,40
	mov r16,r22
	add r16,r21
	call os_out_num8
	ldi r23,50
	mov r16,r23
	call os_out_num8
	ldi r21,60
	ldi r22,70
	mov r16,r21
	add r16,r20
	add r16,r22
	call os_out_num8
	jmp mcu_halt
