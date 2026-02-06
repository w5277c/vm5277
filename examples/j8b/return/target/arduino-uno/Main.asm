; vm5277.AVR v0.2
.equ CORE_FREQ = 16
.set STDIN_PORT_REGID = 11
.set STDIN_DDR_REGID = 10
.set STDIN_PIN_REGID = 9
.set STDIN_PORTNUM = 3
.set STDIN_PINNUM = 0
.set STDOUT_PORT_REGID = 11
.set STDOUT_DDR_REGID = 10
.set STDOUT_PIN_REGID = 9
.set STDOUT_PORTNUM = 3
.set STDOUT_PINNUM = 1

.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "j8b/mfin.asm"
.include "stdio/out_num32.asm"


Main:
	jmp j8b_CMainMmain
_j8b_meta_42:
	.db 22,0

j8b_CMainMsomeMethod_45:
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
	rjmp _j8b_eob_43
_j8b_eoc_0:
	ldi r16,20
	ldi r17,33
	ldi r18,30
	ldi r19,27
	add r16,r20
	adc r17,r21
	adc r18,r22
	adc r19,r23
_j8b_eob_43:
	jmp j8bproc_mfin

j8b_CMainMmain:
	push r30
	push r31
	rcall j8b_CMainMsomeMethod_45
	call os_out_num32
	jmp mcu_halt
