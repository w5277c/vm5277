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
.include "j8b/instanceof.asm"
.include "j8b/clear_fields.asm"
.include "j8b/mfin.asm"
.include "j8b/mfin_sf.asm"
.include "stdio/out_num16.asm"
.include "stdio/out_cstr.asm"

j8bD116:
.db " is short",0x0a,0x00,0x00

j8bD117:
.db "Is short too",0x0a,0x00

Main:
	jmp j8b_CMainMmain
_j8b_meta_34:
	.db 15,0
_j8b_meta_36:
	.db 17,1,16,2
	.dw 0,j8b_CShortMtoShort_45

j8b_CShortMShort_39:
	ldi r16,low(7)
	ldi r17,high(7)
	call os_dram_alloc
	brcc _j8b_throwskip_113
	ldi r16,4
	call j8bproc_etrace_addfirst
	set
	rjmp _j8b_eob_38
_j8b_throwskip_113:
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
	ldd r16,y+6
	ldd r17,y+5
	std z+5,r16
	std z+6,r17
_j8b_eob_38:
	ldi r30,2
	jmp j8bproc_mfin_sf

j8b_CShortMtoShort_45:
	ldd r16,z+5
	ldd r17,z+6
	jmp j8bproc_mfin

j8b_CMainMmain:
	push r30
	push r31
	ldi r19,2
	push r19
	push c0x01
	rcall j8b_CShortMShort_39
	movw r20,r16
	push r30
	push r31
	movw r30,r20
	ldi r19,17
	call j8bproc_instanceof_nr
	pop r31
	pop r30
	sbrs r16,0x00
	rjmp _j8b_eoc_0
	push r30
	push r31
	movw r30,r20
	rcall j8b_CShortMtoShort_45
	brtc _j8b_throwskip_115
	ldi r18,1
	call j8bproc_etrace_add
	rjmp _j8b_eob_46
_j8b_throwskip_115:
	call os_out_num16
	ldi r16,low(j8bD116*2)
	ldi r17,high(j8bD116*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,low(j8bD117*2)
	ldi r17,high(j8bD117*2)
	movw r30,r16
	call os_out_cstr
_j8b_eoc_0:
_j8b_eob_46:
	brts _j8b_skip_118
	jmp mcu_halt
_j8b_skip_118:
	call j8bproc_etrace_out
	jmp mcu_halt
