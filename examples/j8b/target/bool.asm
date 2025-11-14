; vm5277.avr_codegen v0.1 at Fri Nov 14 05:28:38 VLAT 2025
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/mfin_sf.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_bool.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta20:
	.db 12,0

j8bC22CMainMtest:
	push yl
	push yh
	lds yl,SPL
	lds yh,SPH
	ldd r16,y+6
	push r16
	ldd r16,y+5
	pop j8b_atom
	and r16,j8b_atom
	ldi r30,2
	rjmp j8bproc_mfin_sf

j8bCMainMmain:
	push yl
	push yh
	lds yl,SPL
	lds yh,SPH
	push c0x00
	ldi r20,0
	com r20
	ldi r21,0
	com r21
	ldi r22,1
	com r22
	ldi r16,1
	rcall os_out_bool
	ldi r16,0
	rcall os_out_bool
	ldi r16,0
	rcall os_out_bool
	ldi r16,1
	rcall os_out_bool
	mov r16,r21
	rcall os_out_bool
	mov r16,r22
	rcall os_out_bool
	mov r16,r21
	com r16
	rcall os_out_bool
	mov r16,r22
	com r16
	rcall os_out_bool
	mov r16,r21
	com r16
	mov r23,r16
	rcall os_out_bool
	mov r16,r21
	cp r16,r22
	brne _j8b_eoc0
	ldi r16,48
	rcall os_out_num8
_j8b_eoc0:
	mov r16,r21
	sbrs r16,0x00
	rjmp _j8b_eoc1
	ldi r16,49
	rcall os_out_num8
_j8b_eoc1:
	mov r16,r22
	sbrs r16,0x00
	rjmp _j8b_eoc2
	ldi r16,50
	rcall os_out_num8
_j8b_eoc2:
	cpi r22,0
	brne _j8b_eoc3
	ldi r16,51
	rcall os_out_num8
_j8b_eoc3:
	cpi r21,0
	brne _j8b_eolb30
	cpi r22,0
	breq _j8b_eoc4
_j8b_eolb30:
	ldi r16,52
	rcall os_out_num8
_j8b_eoc4:
	cpi r21,0
	breq _j8b_eoc5
	cpi r22,0
	breq _j8b_eoc5
	ldi r16,53
	rcall os_out_num8
_j8b_eoc5:
	push r30
	push r31
	push r21
	push r22
	rcall j8bC22CMainMtest
	rcall os_out_bool
	cpi r21,0
	brne _j8b_eolb32
	cpi r22,0
	brne _j8b_eolb32
	cpi r20,0
	breq _j8b_eoc31
_j8b_eolb32:
	ldi r16,1
	rjmp _j8b_eolb33
_j8b_eoc31:
	ldi r16,0
_j8b_eolb33:
	mov r24,r16
	rcall os_out_bool
	cpi r21,0
	breq _j8b_eoc34
	cpi r22,0
	breq _j8b_eoc34
	ldi r16,1
	rjmp _j8b_eolb35
_j8b_eoc34:
	ldi r16,0
_j8b_eolb35:
	mov r25,r16
	rcall os_out_bool
	mov r16,r21
	com r16
	std y+0,r16
	rcall os_out_bool
	ldi r30,0
	rjmp j8bproc_mfin_sf
