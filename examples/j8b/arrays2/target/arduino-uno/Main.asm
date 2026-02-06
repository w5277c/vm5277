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
.set OS_FT_DRAM = 1
.set OS_ARRAY_1D = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "dmem/dram.asm"
.include "j8b/new_array.asm"
.include "j8b/arr_celladdr.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_bool.asm"
.include "stdio/out_num32.asm"


Main:
	jmp j8b_CMainMmain
_j8b_meta_42:
	.db 22,0

j8b_CMainMmain:
	ldi r16,13
	ldi r17,0
	call j8bproc_new_array
	ldi r19,32
	st x+,r19
	st x+,c0x01
	ldi r19,9
	st x+,r19
	ldi r19,2
	st x+,r19
	st x+,c0x00
	movw r20,r16
	ldi r16,21
	ldi r17,0
	call j8bproc_new_array
	ldi r19,32
	st x+,r19
	st x+,c0x01
	ldi r19,9
	st x+,r19
	ldi r19,4
	st x+,r19
	st x+,c0x00
	movw r22,r16
	movw r26,r22
	adiw r26,17
	ld r16,x+
	ld r17,x+
	ld r18,x+
	ld r19,x+
	push r17
	push r16
	movw r26,r20
	call j8bproc_arr_celladdr
	ld r16,x+
	ld r17,x+
	ld r18,x+
	ld r19,x+
	call os_out_num32
	movw r26,r22
	adiw r26,9
	ld r16,x+
	ld r17,x+
	ld r18,x+
	ld r19,x+
	movw r26,r20
	adiw r26,5
	st x+,r16
	st x+,r17
	st x+,r18
	st x+,r19
	ldi r16,32
	call os_out_num8
	movw r26,r20
	adiw r26,13
	ld r16,-x
	cpi r16,0
	brcs _j8b_eoc_0
	brne _j8b_mcpe_45
	ld r16,-x
	cpi r16,0
	brcs _j8b_eoc_0
	brne _j8b_mcpe_45
	ld r16,-x
	cpi r16,0
	brcs _j8b_eoc_0
	brne _j8b_mcpe_45
	ld r16,-x
	cpi r16,2
	brcs _j8b_eoc_0
	breq _j8b_eoc_0
_j8b_mcpe_45:
	ldi r16,33
	call os_out_num8
_j8b_eoc_0:
	ldi r24,3
	movw r26,r20
	adiw r26,5
	ld r16,x+
	ld r17,x+
	ld r18,x+
	ld r19,x+
	cpi r19,0x00
	brcs _j8b_eoc_46
	brne _j8b_mcpe_47
	cpi r18,0x00
	brcs _j8b_eoc_46
	brne _j8b_mcpe_47
	cpi r17,0x00
	brcs _j8b_eoc_46
	brne _j8b_mcpe_47
	cp r16,r24
	brcs _j8b_eoc_46
	breq _j8b_eoc_46
_j8b_mcpe_47:
	cpi r24,2
	brne _j8b_eoc_46
	ldi r16,1
	rjmp _j8b_eolb_48
_j8b_eoc_46:
	ldi r16,0
_j8b_eolb_48:
	mov r25,r16
	mov r25,r16
	call os_out_bool
	jmp mcu_halt
