; vm5277.avr_codegen v0.2
.equ CORE_FREQ = 16
.equ STDOUT_PORT = 18

.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "stdio/out_char.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_cstr.asm"
.include "stdio/out_num32.asm"

j8bD98:
.db "orig num:",0x00

j8bD99:
.db "short:",0x00,0x00

j8bD100:
.db "byte:",0x00

Main:
	jmp j8b_CMainMmain
_j8b_meta_32:
	.db 15,0

j8b_CMainMmain:
	ldi r20,1
	ldi r21,2
	ldi r22,3
	ldi r23,4
	ldi r16,low(j8bD98*2)
	ldi r17,high(j8bD98*2)
	movw r30,r16
	call os_out_cstr
	movw r16,r20
	movw r18,r22
	call os_out_num32
	ldi r16,10
	call os_out_char
	movw r16,r20
	ldi r18,0x00
	ldi r19,0x00
	movw r20,r16
	movw r22,r18
	ldi r16,low(j8bD99*2)
	ldi r17,high(j8bD99*2)
	movw r30,r16
	call os_out_cstr
	movw r16,r20
	movw r18,r22
	call os_out_num32
	ldi r16,10
	call os_out_char
	ldi r16,low(j8bD100*2)
	ldi r17,high(j8bD100*2)
	movw r30,r16
	call os_out_cstr
	mov r16,r20
	call os_out_num8
	ldi r16,10
	call os_out_char
	jmp mcu_halt
