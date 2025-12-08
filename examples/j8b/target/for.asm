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
	ldi r20,0
_j8b_loop_43:
	cpi r20,10
	brcc _j8b_eoc_0
	cpi r20,1
	breq _j8b_eol_45
	mov r16,r20
	call os_out_num8
	add r20,c0x01
	rjmp _j8b_loop_43
_j8b_eoc_0:
	ldi r16,33
	call os_out_num8
_j8b_eol_45:
	ldi r20,7
	ldi r16,64
	call os_out_num8
	ldi r20,8
_j8b_loop_56:
	mov r16,r20
	call os_out_num8
	add r20,c0x01
	rjmp _j8b_loop_56
	ldi r20,0
_j8b_loop_61:
	mov r16,r20
	call os_out_num8
	add r20,c0x01
	rjmp _j8b_loop_61
	ldi r20,3
_j8b_loop_66:
	cpi r20,10
	brcc _j8b_eoc_5
	mov r16,r20
	call os_out_num8
	add r20,c0x01
	rjmp _j8b_loop_66
_j8b_eoc_5:
	ldi r20,0
_j8b_loop_71:
	cpi r20,10
	brcc _j8b_eoc_6
	mov r16,r20
	call os_out_num8
	add r20,c0x01
	rjmp _j8b_loop_71
_j8b_eoc_6:
	ldi r21,0
_j8b_loop_76:
	ldi r16,0
	call os_out_num8
	rjmp _j8b_loop_76
	jmp mcu_halt
