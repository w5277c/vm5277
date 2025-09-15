; vm5277.avr_codegen v0.1 at Tue Sep 16 04:53:07 VLAT 2025
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_bool.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta20:
	.db 12,0

j8bC21CMainMtest:
	push yl
	push yh
	lds yl,SPL
	lds yh,SPH
	ldd r16,y+6
	push r16
	ldd r16,y+5
	pop j8b_atom
	and r16,j8b_atom
	ret

j8bCMainMmain:
	ldi r24,1
	ldi r20,1
	ldi r21,0
	ldi r16,1
	rcall os_out_bool
	ldi r16,0
	rcall os_out_bool
	ldi r16,0
	rcall os_out_bool
	ldi r16,1
	rcall os_out_bool
	mov r16,r20
	rcall os_out_bool
	mov r16,r21
	rcall os_out_bool
	mov r16,r20
	com r16
	rcall os_out_bool
	mov r16,r21
	com r16
	rcall os_out_bool
	mov r16,r20
	com r16
	mov r22,r16
	mov r16,r22
	rcall os_out_bool
	mov r16,r21
	cp r16,r20
	brne _j8b_eoc24
	ldi r16,48
	rcall os_out_num8
_j8b_eoc24:
	mov r16,r20
	tst r16
	breq  _j8b_eoc26
	ldi r16,49
	rcall os_out_num8
_j8b_eoc26:
	mov r16,r21
	tst r16
	breq  _j8b_eoc28
	ldi r16,50
	rcall os_out_num8
_j8b_eoc28:
	mov r16,r21
	com r16
	tst r16
	breq  _j8b_eoc30
	ldi r16,51
	rcall os_out_num8
_j8b_eoc30:
	cpi r20,0
	brne _j8b_eolb39
	cpi r21,0
	breq _j8b_eoc32
_j8b_eolb39:
	ldi r16,52
	rcall os_out_num8
_j8b_eoc32:
	cpi r20,0
	breq _j8b_eoc34
	cpi r21,0
	breq _j8b_eoc34
	ldi r16,53
	rcall os_out_num8
_j8b_eoc34:
	push zl
	push r20
	push r21
	rcall j8bC21CMainMtest
	pop j8b_atom
	pop j8b_atom
	pop zl
	rcall os_out_bool
	mov r16,r20
	push r16
	mov r16,r21
	pop j8b_atom
	or r16,j8b_atom
	push r16
	mov r16,r24
	pop j8b_atom
	or r16,j8b_atom
	mov r23,r16
	mov r16,r23
	rcall os_out_bool
	mov r16,r20
	push r16
	mov r16,r21
	pop j8b_atom
	and r16,j8b_atom
	mov r25,r16
	mov r16,r25
	rcall os_out_bool
	mov r16,r20
	com r16
	mov r26,r16
	mov r16,r26
	rcall os_out_bool
	ret
