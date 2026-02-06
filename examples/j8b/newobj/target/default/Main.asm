; vm5277.AVR v0.2
.equ CORE_FREQ = 16
.set STDIN_PORT_REGID = 11
.set STDIN_DDR_REGID = 10
.set STDIN_PIN_REGID = 9
.set STDIN_PORTNUM = 3
.set STDIN_PINNUM = 3
.set STDOUT_PORT_REGID = 11
.set STDOUT_DDR_REGID = 10
.set STDOUT_PIN_REGID = 9
.set STDOUT_PORTNUM = 3
.set STDOUT_PINNUM = 4

.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1
.set OS_FT_DRAM = 1

.include "devices/atmega168p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "dmem/dram.asm"
.include "j8b/class_refcount.asm"
.include "j8b/clear_fields.asm"
.include "j8b/mfin.asm"
.include "j8b/mfin_sf.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_num16.asm"


Main:
	jmp j8b_CMainMmain
_j8b_meta_43:
	.db 22,0
_j8b_meta_44:
	.db 12,1,23,1
	.dw j8b_CByteMtoByte_49

j8b_CByteMByte_47:
	ldi r16,low(6)
	ldi r17,high(6)
	call os_dram_alloc
	std z+0,r16
	std z+1,r17
	std z+2,c0xff
	ldi r16,low(_j8b_meta_44*2)
	std z+3,r16
	ldi r16,high(_j8b_meta_44*2)
	std z+4,r16
	call j8bproc_clear_fields_nr
	push r28
	push r29
	lds r28,SPL
	lds r29,SPH
	ldd r16,y+5
	std z+5,r16
	ldi r30,1
	jmp j8bproc_mfin_sf

j8b_CByteMtoByte_49:
	ldd r16,z+5
	jmp j8bproc_mfin

j8b_CMainMmain:
	push r28
	push r29
	lds r28,SPL
	lds r29,SPH
	push c0x00
	push c0x00
	push r30
	push r31
	push c0x01
	rcall j8b_CByteMByte_47
	movw r20,r16
	push r30
	push r31
	movw r30,r20
	rcall j8b_CByteMtoByte_49
	call os_out_num8
	push r30
	push r31
	ldi r19,2
	push r19
	rcall j8b_CByteMByte_47
	movw r22,r16
	push r30
	push r31
	movw r30,r22
	rcall j8b_CByteMtoByte_49
	call os_out_num8
	push r30
	push r31
	movw r30,r20
	call j8bproc_class_refcount_dec
	pop r31
	pop r30
	ldi r20,0
	ldi r21,0
	push r30
	push r31
	push c0x01
	rcall j8b_CByteMByte_47
	movw r24,r16
	push r30
	push r31
	movw r30,r24
	rcall j8b_CByteMtoByte_49
	call os_out_num8
	ldi r16,12
	call os_out_num8
	movw r16,r24
	call os_out_num16
	mov r16,r20
	std y+0,r16
	subi yl,low(33)
	sbci yh,high(33)
	std y+32,r17
	movw r16,r20
	cp r17,r23
	brne _j8b_eoc_0
	cp r16,r22
	brne _j8b_eoc_0
	ldi r16,1
	call os_out_num8
_j8b_eoc_0:
	mov r16,r20
	ldd r19,y+33
	cp r16,r19
	brne _j8b_eoc_1
	ldi r16,2
	call os_out_num8
_j8b_eoc_1:
	jmp mcu_halt
