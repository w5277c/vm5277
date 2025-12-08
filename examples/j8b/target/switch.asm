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
	ldi r20,11
	add r20,c0x01
	mov r16,r20
	cpi r16,0
	breq _j8b_eoc_38
	cpi r16,1
	brcs _j8b_eoc_41
	cpi r16,11
	brcs _j8b_eoc_39
_j8b_eoc_41:
	cpi r16,15
	brcs _j8b_eoc_42
	cpi r16,18
	brcs _j8b_eoc_40
_j8b_eoc_42:
	cpi r16,20
	brcs _j8b_eoc_43
	cpi r16,26
	brcs _j8b_eoc_40
_j8b_eoc_43:
	cpi r16,30
	breq _j8b_eoc_40
	cpi r16,32
	breq _j8b_eoc_40
	rjmp _j8b_casedef__103
_j8b_eoc_38:
	ldi r16,0
	call os_out_num8
	rjmp _j8b_caseend_102
_j8b_eoc_39:
	ldi r16,1
	call os_out_num8
	rjmp _j8b_caseend_102
_j8b_eoc_40:
	ldi r16,2
	call os_out_num8
	rjmp _j8b_caseend_102
_j8b_casedef__103:
	ldi r16,3
	call os_out_num8
_j8b_caseend_102:
	jmp mcu_halt
