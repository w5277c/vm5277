; vm5277.avr_codegen v0.1 at Wed Nov 19 14:55:43 GMT+10:00 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/mfin.asm"
.include "stdio/out_num8.asm"
.include "sys/mcu_stop.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta31:
	.db 13,0

j8bCMainMmain:
	ldi r20,1
_j8b_loop34:
	cpi r20,10
	brcc _j8b_eoc0
	cpi r20,5
	brcs _j8b_loop34
	mov r16,r20
	rcall os_out_num8
	cpi r20,7
	breq _j8b_eol36
	add r20,C0x01
	rjmp _j8b_loop34
_j8b_eoc0:
_j8b_eol36:
	ldi r20,2
_j8b_loop41:
	cpi r20,10
	brcc _j8b_eoc3
	add r20,C0x01
	cpi r20,5
	brcs _j8b_loop41
	mov r16,r20
	rcall os_out_num8
	cpi r20,7
	brne _j8b_loop41
	rjmp _j8b_eol43
	rjmp _j8b_loop41
_j8b_eoc3:
_j8b_eol43:
	ldi r21,3
_j8b_loop48:
	cpi r21,10
	brcc _j8b_eoc6
	ldi r22,0
_j8b_loop53:
	cpi r22,10
	brcc _j8b_eoc7
	cpi r21,5
	brne _j8b_eoc8
	cpi r22,5
	breq _j8b_eol50
_j8b_eoc8:
	mov r16,r21
	rcall os_out_num8
	add r22,C0x01
	rjmp _j8b_loop53
_j8b_eoc7:
	add r21,C0x01
	rjmp _j8b_loop48
_j8b_eoc6:
_j8b_eol50:
	ldi r21,4
_j8b_loop59:
	cpi r21,10
	brcc _j8b_eoc9
	rjmp _j8b_loop59
	mov r16,r21
	rcall os_out_num8
	add r21,C0x01
	rjmp _j8b_loop59
_j8b_eoc9:
	rcall mcu_stop
	rjmp j8bproc_mfin
