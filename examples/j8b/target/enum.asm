; vm5277.avr_codegen v0.1 at Fri Nov 14 05:33:09 VLAT 2025
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/mfin.asm"
.include "j8b/mfin_sf.asm"
.include "stdio/out_num8.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta15:
	.db 12,0

j8bCMainMmain:
	ldi r16,1
	rcall os_out_num8
	ldi r16,2
	rcall os_out_num8
	ldi r16,1
	mov r20,r16
	ldi r16,3
	cp r16,r20
	brne _j8b_eoc0
	ldi r16,1
	rcall os_out_num8
	rjmp _j8b_eoc27
_j8b_eoc0:
	ldi r16,0
	rcall os_out_num8
_j8b_eoc27:
	push r30
	push r31
	ldi r19,3
	push r19
	rcall j8bC20CMainMprintDirection
	ldi r21,0
	mov r16,r21
	rcall os_out_num8
	ldi r16,1
	mov r22,r16
	mov r16,r22
	rcall os_out_num8
	ldi r16,1
	rcall os_out_num8
	rjmp j8bproc_mfin

j8bC20CMainMprintDirection:
	push yl
	push yh
	lds yl,SPL
	lds yh,SPH
	ldd r16,y+5
	rcall os_out_num8
	ldi r30,1
	rjmp j8bproc_mfin_sf
