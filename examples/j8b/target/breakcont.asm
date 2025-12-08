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
_j8b_meta_42:
	.db 15,0

j8b_CMainMmain:
	ldi r20,1
_j8b_loop_45:
	cpi r20,10
	brcc _j8b_eoc_0
	cpi r20,5
	brcs _j8b_next_46
	mov r16,r20
	call os_out_num8
	cpi r20,7
	breq _j8b_eol_47
_j8b_next_46:
	add r20,c0x01
	rjmp _j8b_loop_45
_j8b_eoc_0:
_j8b_eol_47:
	ldi r20,2
_j8b_loop_52:
	cpi r20,10
	brcc _j8b_eoc_3
	add r20,c0x01
	cpi r20,5
	brcs _j8b_loop_52
	mov r16,r20
	call os_out_num8
	cpi r20,7
	brne _j8b_loop_52
_j8b_eoc_3:
	ldi r21,3
_j8b_loop_59:
	cpi r21,10
	brcc _j8b_eoc_6
	ldi r22,0
_j8b_loop_64:
	cpi r22,10
	brcc _j8b_eoc_7
	cpi r21,5
	brne _j8b_eoc_8
	cpi r22,5
	breq _j8b_eol_61
_j8b_eoc_8:
	mov r16,r21
	call os_out_num8
	add r22,c0x01
	rjmp _j8b_loop_64
_j8b_eoc_7:
	add r21,c0x01
	rjmp _j8b_loop_59
_j8b_eoc_6:
_j8b_eol_61:
	ldi r21,4
_j8b_loop_70:
	cpi r21,10
	brcc _j8b_eoc_9
	add r21,c0x01
	rjmp _j8b_loop_70
_j8b_eoc_9:
	jmp mcu_halt
