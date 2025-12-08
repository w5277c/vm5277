; vm5277.avr_codegen v0.2
.equ STDOUT_PORT = 18

.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_bool.asm"
.include "stdio/out_num32.asm"

Main:
	jmp j8b_CMainMmain
_j8b_meta_32:
	.db 15,0

j8b_CMainMmain:
	ldi r20,1
	ldi r22,7
	ldi r23,6
	ldi r24,5
	ldi r25,4
	ldi r21,8
	mov r16,r20
	call os_out_bool
	movw r16,r22
	movw r18,r24
	call os_out_num32
	mov r16,r21
	call os_out_num8
	jmp mcu_halt
