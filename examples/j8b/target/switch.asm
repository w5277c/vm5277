; vm5277.avr_codegen v0.1 at Wed Nov 19 06:35:49 GMT+10:00 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/mfin.asm"
.include "stdio/out_num8.asm"
.include "sys/mcu_stop.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta21:
	.db 13,0

j8bCMainMmain:
	ldi r20,11
	add r20,C0x01
	mov r16,r20
	cpi r16,0
	breq _j8b_eoc27
	cpi r16,1
	brcs _j8b_eoc30
	cpi r16,11
	brcs _j8b_eoc28
_j8b_eoc30:
	cpi r16,15
	brcs _j8b_eoc31
	cpi r16,18
	brcs _j8b_eoc29
_j8b_eoc31:
	cpi r16,20
	brcs _j8b_eoc32
	cpi r16,26
	brcs _j8b_eoc29
_j8b_eoc32:
	cpi r16,30
	breq _j8b_eoc29
	cpi r16,32
	breq _j8b_eoc29
	rjmp _j8b_casedef_87
_j8b_eoc27:
	ldi r16,0
	rcall os_out_num8
	rjmp _j8b_caseend86
_j8b_eoc28:
	ldi r16,1
	rcall os_out_num8
	rjmp _j8b_caseend86
_j8b_eoc29:
	ldi r16,2
	rcall os_out_num8
	rjmp _j8b_caseend86
_j8b_casedef_87:
	ldi r16,3
	rcall os_out_num8
_j8b_caseend86:
	rcall mcu_stop
	rjmp j8bproc_mfin
