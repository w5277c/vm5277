; vm5277.avr_codegen v0.1 at Fri Nov 14 14:54:52 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/mfin.asm"
.include "j8b/mfin_sf.asm"
.include "stdio/out_char.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_bool.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta32:
	.db 13,0

j8bCMainMmain:
	ldi r16,65
	rcall os_out_char
	ldi r16,67
	rcall os_out_char
	ldi r16,68
	rcall os_out_char
	ldi r20,1
	cpi r20,3
	brne _j8b_eoc4
	ldi r16,32
	rcall os_out_char
_j8b_eoc4:
	ldi r21,0
	mov r16,r21
	sbrs r16,0x00
	rjmp _j8b_eoc5
	ldi r16,50
	rcall os_out_num8
_j8b_eoc5:
	cpi r21,0
	brne _j8b_eoc6
	ldi r16,51
	rcall os_out_num8
_j8b_eoc6:
	ldi r22,1
	add r22,C0x01
	ldi r23,2
	add r23,C0x01
	mov r16,r23
	add r16,r22
	cpi r16,2
	brcc _j8b_eoc7
	ldi r16,70
	rcall os_out_char
	rjmp _j8b_eoc101
_j8b_eoc7:
	ldi r16,71
	rcall os_out_char
_j8b_eoc101:
	mov r16,r22
	cp r16,r23
	brcs _j8b_eoc8
	breq _j8b_eoc8
	ldi r16,89
	rjmp _j8b_tere102
_j8b_eoc8:
	ldi r16,78
_j8b_tere102:
	rcall os_out_num8
	ldi r16,89
	rcall os_out_num8
	ldi r16,78
	rcall os_out_num8
	push r30
	push r31
	push c0x00
	rcall j8bC45CMainMmethod
	sbrs r16,0x00
	rjmp _j8b_eoc9
	ldi r16,89
	rjmp _j8b_tere104
_j8b_eoc9:
	ldi r16,78
_j8b_tere104:
	rcall os_out_num8
	push r30
	push r31
	push c0x00
	rcall j8bC45CMainMmethod
	sbrs r16,0x00
	rjmp _j8b_eoc10
	push r30
	push r31
	push c0x01
	rcall j8bC45CMainMmethod
	rjmp _j8b_tere106
_j8b_eoc10:
	push r30
	push r31
	push c0x00
	rcall j8bC45CMainMmethod
_j8b_tere106:
	rcall os_out_bool
	rjmp j8bproc_mfin

j8bC45CMainMmethod:
	push yl
	push yh
	lds yl,SPL
	lds yh,SPH
	ldd r16,y+5
	com r16
	ldi r30,1
	rjmp j8bproc_mfin_sf
