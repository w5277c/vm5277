; vm5277.avr_codegen v0.1 at Thu Sep 04 03:08:50 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "stdio/out_char.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_cstr.asm"
.include "stdio/out_num32.asm"

j8bD80:
.db "orig num:",0x00

j8bD81:
.db "short:",0x00,0x00

j8bD82:
.db "byte:",0x00

Main:
	rjmp j8bCMainMmain

j8bCmainMmain:
	ldi r20,1
	ldi r21,2
	ldi r22,3
	ldi r23,4
	ldi r30,low(j8bD80*2)
	ldi r31,high(j8bD80*2)
	rcall os_out_cstr
	mov r16,r20
	mov r17,r21
	mov r18,r22
	mov r19,r23
	rcall os_out_num32
	ldi r16,10
	rcall os_out_char
	mov r16,r20
	mov r17,r21
	mov r18,r22
	mov r19,r23
	ldi r18,0x00
	ldi r19,0x00
	mov r20,r16
	mov r21,r17
	mov r22,r18
	mov r23,r19
	ldi r30,low(j8bD81*2)
	ldi r31,high(j8bD81*2)
	rcall os_out_cstr
	mov r16,r20
	mov r17,r21
	mov r18,r22
	mov r19,r23
	rcall os_out_num32
	ldi r16,10
	rcall os_out_char
	ldi r30,low(j8bD82*2)
	ldi r31,high(j8bD82*2)
	rcall os_out_cstr
	mov r16,r20
	mov r17,r21
	mov r18,r22
	mov r19,r23
	mov r16,r20
	rcall os_out_num8
	ldi r16,10
	rcall os_out_char
	ret
