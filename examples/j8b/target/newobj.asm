; vm5277.avr_codegen v0.2
.equ CORE_FREQ = 16
.equ STDOUT_PORT = 18
.set OS_ETRACE_POINT_BITSIZE = 7

.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1
.set OS_FT_DRAM = 1
.set OS_FT_ETRACE = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "j8b/etrace_out.asm"
.include "j8b/etrace_add.asm"
.include "dmem/dram.asm"
.include "j8b/class_refcount.asm"
.include "j8b/clear_fields.asm"
.include "j8b/mfin.asm"
.include "j8b/mfin_sf.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_num16.asm"

Main:
	jmp j8b_CMainMmain
_j8b_meta_34:
	.db 15,0
_j8b_meta_36:
	.db 17,1,16,1
	.dw j8b_CByteMtoByte_41

j8b_CByteMByte_39:
	ldi r16,low(6)
	ldi r17,high(6)
	call os_dram_alloc
	brcc _j8b_throwskip_112
	ldi r16,4
	call j8bproc_etrace_addfirst
	set
	rjmp _j8b_eob_38
_j8b_throwskip_112:
	std z+0,r16
	std z+1,r17
	std z+2,c0xff
	ldi r16,low(_j8b_meta_36*2)
	std z+3,r16
	ldi r16,high(_j8b_meta_36*2)
	std z+4,r16
	call j8bproc_clear_fields_nr
	push r28
	push r29
	lds r28,SPL
	lds r29,SPH
	ldd r16,y+5
	std z+5,r16
_j8b_eob_38:
	ldi r30,1
	jmp j8bproc_mfin_sf

j8b_CByteMtoByte_41:
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
	rcall j8b_CByteMByte_39
	movw r20,r16
	push r30
	push r31
	movw r30,r20
	rcall j8b_CByteMtoByte_41
	brtc _j8b_throwskip_113
	ldi r18,1
	call j8bproc_etrace_add
	rjmp _j8b_eob_42
_j8b_throwskip_113:
	call os_out_num8
	push r30
	push r31
	ldi r19,2
	push r19
	rcall j8b_CByteMByte_39
	movw r22,r16
	push r30
	push r31
	movw r30,r22
	rcall j8b_CByteMtoByte_41
	brtc _j8b_throwskip_115
	ldi r18,2
	call j8bproc_etrace_add
	rjmp _j8b_eob_42
_j8b_throwskip_115:
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
	rcall j8b_CByteMByte_39
	movw r24,r16
	push r30
	push r31
	movw r30,r24
	rcall j8b_CByteMtoByte_41
	brtc _j8b_throwskip_117
	ldi r18,3
	call j8bproc_etrace_add
	rjmp _j8b_eob_42
_j8b_throwskip_117:
	call os_out_num8
	ldi r16,17
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
_j8b_eob_42:
	brts _j8b_skip_120
	jmp mcu_halt
_j8b_skip_120:
	call j8bproc_etrace_out
	jmp mcu_halt
