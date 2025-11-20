; vm5277.avr_codegen v0.1 at Fri Nov 21 08:02:30 GMT+10:00 2025
.equ stdout_port = 18

.set OS_ARRAY_1D = 1
.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/new_array.asm"
.include "j8b/arr_celladdr.asm"
.include "j8b/arr_refcount.asm"
.include "j8b/mfin.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_bool.asm"
.include "stdio/out_num32.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta15:
	.db 12,0

j8bCMainMmain:
	ldi r16,13
	ldi r17,0
	rcall j8bproc_new_array
	ldi r19,32
	st x+,r19
	st x+,C0x01
	ldi r19,9
	st x+,r19
	ldi r19,2
	st x+,r19
	st x+,c0x00
	movw r20,r16
	ldi r16,21
	ldi r17,0
	rcall j8bproc_new_array
	ldi r19,32
	st x+,r19
	st x+,C0x01
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
	rcall j8bproc_arr_celladdr
	ld r16,x+
	ld r17,x+
	ld r18,x+
	ld r19,x+
	rcall os_out_num32
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
	rcall os_out_num8
	movw r26,r20
	adiw r26,13
	ld r16,-x
	cpi r16,0
	brcs _j8b_eoc0
	brne _j8b_mcpe18
	ld r16,-x
	cpi r16,0
	brcs _j8b_eoc0
	brne _j8b_mcpe18
	ld r16,-x
	cpi r16,0
	brcs _j8b_eoc0
	brne _j8b_mcpe18
	ld r16,-x
	cpi r16,2
	brcs _j8b_eoc0
	breq _j8b_eoc0
_j8b_mcpe18:
	ldi r16,33
	rcall os_out_num8
_j8b_eoc0:
	ldi r24,3
	movw r26,r20
	adiw r26,5
	ld r16,x+
	ld r17,x+
	ld r18,x+
	ld r19,x+
	cpi r19,0x00
	brcs _j8b_eoc19
	brne _j8b_mcpe20
	cpi r18,0x00
	brcs _j8b_eoc19
	brne _j8b_mcpe20
	cpi r17,0x00
	brcs _j8b_eoc19
	brne _j8b_mcpe20
	cp r16,r24
	brcs _j8b_eoc19
	breq _j8b_eoc19
_j8b_mcpe20:
	cpi r24,2
	brne _j8b_eoc19
	ldi r16,1
	rjmp _j8b_eolb21
_j8b_eoc19:
	ldi r16,0
_j8b_eolb21:
	mov r25,r16
	rcall os_out_bool
	movw r26,r20
	rcall j8bproc_arr_refcount_dec
	movw r26,r22
	rcall j8bproc_arr_refcount_dec
	rjmp j8bproc_mfin
