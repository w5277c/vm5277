; vm5277.avr_codegen v0.2
.equ CORE_FREQ = 4
.equ STDOUT_PORT = 18

.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "j8b/mfin_sf.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_bool.asm"

Main:
	jmp j8b_CMainMmain
_j8b_meta_38:
	.db 15,0

j8b_CMainMtest_40:
	push r28
	push r29
	lds r28,SPL
	lds r29,SPH
	ldd r16,y+6
	push r16
	ldd r16,y+5
	pop j8b_atom
	and r16,j8b_atom
	ldi r30,2
	jmp j8bproc_mfin_sf

j8b_CMainMmain:
	push r28
	push r29
	lds r28,SPL
	lds r29,SPH
	push c0x00
	ldi r20,0
	com r20
	ldi r21,0
	com r21
	ldi r22,1
	com r22
	ldi r16,1
	call os_out_bool
	ldi r16,0
	call os_out_bool
	ldi r16,0
	call os_out_bool
	ldi r16,1
	call os_out_bool
	mov r16,r21
	call os_out_bool
	mov r16,r22
	call os_out_bool
	mov r16,r21
	com r16
	call os_out_bool
	mov r16,r22
	com r16
	call os_out_bool
	mov r16,r21
	com r16
	mov r23,r16
	call os_out_bool
	mov r16,r21
	cp r16,r22
	brne _j8b_eoc_0
	ldi r16,48
	call os_out_num8
_j8b_eoc_0:
	mov r16,r21
	sbrs r16,0x00
	rjmp _j8b_eoc_1
	ldi r16,49
	call os_out_num8
_j8b_eoc_1:
	mov r16,r22
	sbrs r16,0x00
	rjmp _j8b_eoc_2
	ldi r16,50
	call os_out_num8
_j8b_eoc_2:
	cpi r22,0
	brne _j8b_eoc_3
	ldi r16,51
	call os_out_num8
_j8b_eoc_3:
	cpi r21,0
	brne _j8b_eolb_48
	cpi r22,0
	breq _j8b_eoc_4
_j8b_eolb_48:
	ldi r16,52
	call os_out_num8
_j8b_eoc_4:
	cpi r21,0
	breq _j8b_eoc_5
	cpi r22,0
	breq _j8b_eoc_5
	ldi r16,53
	call os_out_num8
_j8b_eoc_5:
	push r30
	push r31
	push r21
	push r22
	rcall j8b_CMainMtest_40
	call os_out_bool
	cpi r21,0
	brne _j8b_eolb_50
	cpi r22,0
	brne _j8b_eolb_50
	cpi r20,0
	breq _j8b_eoc_49
_j8b_eolb_50:
	ldi r16,1
	rjmp _j8b_eolb_51
_j8b_eoc_49:
	ldi r16,0
_j8b_eolb_51:
	mov r24,r16
	call os_out_bool
	cpi r21,0
	breq _j8b_eoc_52
	cpi r22,0
	breq _j8b_eoc_52
	ldi r16,1
	rjmp _j8b_eolb_53
_j8b_eoc_52:
	ldi r16,0
_j8b_eolb_53:
	mov r25,r16
	call os_out_bool
	mov r16,r21
	com r16
	std y+0,r16
	call os_out_bool
	jmp mcu_halt
