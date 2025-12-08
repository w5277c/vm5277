; vm5277.avr_codegen v0.2
.equ STDOUT_PORT = 18

.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "stdio/out_num8.asm"

Main:
	jmp j8b_CMainMmain
_j8b_meta_40:
	.db 15,0

j8b_CMainMmain:
	ldi r20,3
_j8b_loop_43:
	cpi r20,10
	brcc _j8b_eoc_0
	mov r16,r20
	call os_out_num8
	add r20,c0x01
	cpi r20,5
	brne _j8b_loop_43
_j8b_eoc_0:
_j8b_loop_49:
	mov r16,r20
	call os_out_num8
	rjmp _j8b_loop_49
_j8b_loop_59:
	rjmp _j8b_loop_59
_j8b_loop_64:
	ldi r16,33
	call os_out_num8
	add r20,c0x01
	cpi r20,20
	brcs _j8b_loop_64
_j8b_loop_69:
	ldi r16,32
	call os_out_num8
	add r20,c0x01
	rjmp _j8b_loop_69
	jmp mcu_halt
