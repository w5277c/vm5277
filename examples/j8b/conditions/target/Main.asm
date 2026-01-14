; vm5277.avr_codegen v0.2
.equ CORE_FREQ = 16
.equ STDOUT_PORT = 18

.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "j8b/mfin_sf.asm"
.include "stdio/out_char.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_bool.asm"

Main:
	jmp j8b_CMainMmain
_j8b_meta_43:
	.db 15,0

j8b_CMainMmain:
	ldi r16,65
	call os_out_char
	ldi r16,67
	call os_out_char
	ldi r16,68
	call os_out_char
	ldi r20,1
	cpi r20,3
	brne _j8b_eoc_4
	ldi r16,32
	call os_out_char
_j8b_eoc_4:
	ldi r21,0
	mov r16,r21
	sbrs r16,0x00
	rjmp _j8b_eoc_5
	ldi r16,50
	call os_out_num8
_j8b_eoc_5:
	cpi r21,0
	brne _j8b_eoc_6
	ldi r16,51
	call os_out_num8
_j8b_eoc_6:
	ldi r22,1
	add r22,c0x01
	ldi r23,2
	add r23,c0x01
	mov r16,r23
	add r16,r22
	cpi r16,2
	brcc _j8b_eoc_7
	ldi r16,70
	call os_out_char
	rjmp _j8b_eoc_117
_j8b_eoc_7:
	ldi r16,71
	call os_out_char
_j8b_eoc_117:
	mov r16,r22
	cp r16,r23
	brcs _j8b_eoc_8
	breq _j8b_eoc_8
	ldi r16,89
	rjmp _j8b_tere_118
_j8b_eoc_8:
	ldi r16,78
_j8b_tere_118:
	call os_out_num8
	ldi r16,89
	call os_out_num8
	ldi r16,78
	call os_out_num8
	push r30
	push r31
	push c0x00
	rcall j8b_CMainMmethod_56
	sbrs r16,0x00
	rjmp _j8b_eoc_9
	ldi r16,89
	rjmp _j8b_tere_120
_j8b_eoc_9:
	ldi r16,78
_j8b_tere_120:
	call os_out_num8
	push r30
	push r31
	push c0x00
	rcall j8b_CMainMmethod_56
	sbrs r16,0x00
	rjmp _j8b_eoc_10
	push r30
	push r31
	push c0x01
	rcall j8b_CMainMmethod_56
	rjmp _j8b_tere_122
_j8b_eoc_10:
	push r30
	push r31
	push c0x00
	rcall j8b_CMainMmethod_56
_j8b_tere_122:
	call os_out_bool
	jmp mcu_halt

j8b_CMainMmethod_56:
	push r28
	push r29
	lds r28,SPL
	lds r29,SPH
	ldd r16,y+5
	com r16
	ldi r30,1
	jmp j8bproc_mfin_sf
