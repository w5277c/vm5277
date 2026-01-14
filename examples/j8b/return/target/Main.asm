; vm5277.avr_codegen v0.2
.equ CORE_FREQ = 16
.equ STDOUT_PORT = 18

.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "j8b/mfin.asm"
.include "stdio/out_num32.asm"

Main:
	jmp j8b_CMainMmain
_j8b_meta_33:
	.db 15,0

j8b_CMainMsomeMethod_36:
	ldi r20,4
	ldi r21,3
	ldi r22,2
	ldi r23,1
	add r20,c0x01
	adc r21,c0x00
	adc r22,c0x00
	adc r23,c0x00
	cpi r23,0
	brne _j8b_eoc_0
	cpi r22,0
	brne _j8b_eoc_0
	cpi r21,0
	brne _j8b_eoc_0
	cpi r20,0
	brne _j8b_eoc_0
	movw r16,r20
	movw r18,r22
	rjmp _j8b_eob_34
_j8b_eoc_0:
	movw r16,r20
	movw r18,r22
	subi r16,236
	sbci r17,222
	sbci r18,225
	sbci r19,228
_j8b_eob_34:
	jmp j8bproc_mfin

j8b_CMainMmain:
	push r30
	push r31
	rcall j8b_CMainMsomeMethod_36
	call os_out_num32
	jmp mcu_halt
