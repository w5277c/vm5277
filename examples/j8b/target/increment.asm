; vm5277.avr_codegen v0.2
.equ CORE_FREQ = 16
.equ STDOUT_PORT = 18

.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "stdio/out_char.asm"

Main:
	jmp j8b_CMainMmain
_j8b_meta_32:
	.db 15,0

j8b_CMainMmain:
	ldi r20,65
	mov r16,r20
	call os_out_char
	add r20,c0x01
	mov r16,r20
	call os_out_char
	add r20,c0x01
	mov r16,r20
	call os_out_char
	add r20,c0x01
	mov r16,r20
	subi r16,224
	mov r20,r16
	mov r16,r20
	call os_out_char
	jmp mcu_halt
