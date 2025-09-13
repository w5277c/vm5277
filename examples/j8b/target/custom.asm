; vm5277.avr_codegen v0.1 at Sun Sep 14 04:16:21 VLAT 2025
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "math/mul16.asm"
.include "math/div16.asm"
.include "stdio/out_num16.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta18:
	.db 12,0

j8bCMainMmain:
	ldi r20,10
	ldi r21,0
	ldi r22,2
	ldi r23,0
	mov r16,r20
	mov r17,r21
	subi r16,254
	sbci r17,255
	rcall os_out_num16
	mov r16,r20
	mov r17,r21
	subi r16,2
	sbci r17,0
	rcall os_out_num16
	mov r16,r20
	mov r17,r21
	ldi ACCUM_EL,2
	ldi ACCUM_EH,0
	rcall os_mul16
	rcall os_out_num16
	mov r16,r20
	mov r17,r21
	push TEMP_L
	push TEMP_H
	ldi ACCUM_EL,2
	ldi ACCUM_EH,0
	rcall os_div16
	pop TEMP_H
	pop TEMP_L
	rcall os_out_num16
	mov r16,r20
	mov r17,r21
	push TEMP_L
	push TEMP_H
	ldi ACCUM_EL,2
	ldi ACCUM_EH,0
	rcall os_div16
	movw ACCUM_L,TEMP_L
	pop TEMP_H
	pop TEMP_L
	rcall os_out_num16
	mov r16,r22
	mov r17,r23
	add r16,r20
	adc r17,r21
	rcall os_out_num16
	mov r16,r20
	mov r17,r21
	sub r16,r22
	sbc r17,r23
	rcall os_out_num16
	mov r16,r22
	mov r17,r23
	mov ACCUM_EL,r20
	mov ACCUM_EH,r21
	rcall os_mul16
	rcall os_out_num16
	mov r16,r20
	mov r17,r21
	mov ACCUM_EL,r22
	mov ACCUM_EH,r23
	tst r17
	brne _j8b_nediv24
	tst r18
	brne _j8b_nediv24
;TODO Division by zero
	ldi r16,0xff
	ldi r17,0xff
	rjmp _j8b_ediv23
_j8b_nediv24:
	push TEMP_L
	push TEMP_H
	rcall os_div16
	pop TEMP_H
	pop TEMP_L
_j8b_ediv23:
	rcall os_out_num16
	mov r16,r20
	mov r17,r21
	mov ACCUM_EL,r22
	mov ACCUM_EH,r23
	tst r17
	brne _j8b_nediv26
	tst r18
	brne _j8b_nediv26
;TODO Division by zero
	ldi r16,0xff
	ldi r17,0xff
	rjmp _j8b_ediv25
_j8b_nediv26:
	push TEMP_L
	push TEMP_H
	rcall os_div16
	movw ACCUM_L,TEMP_L
	pop TEMP_H
	pop TEMP_L
_j8b_ediv25:
	rcall os_out_num16
	ret
