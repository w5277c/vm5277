; vm5277.avr_codegen v0.2
.equ CORE_FREQ = 16
.equ STDOUT_PORT = 18

.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1
.set OS_ARRAY_2D = 1
.set OS_ARRAY_3D = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "dmem/dram.asm"
.include "j8b/class_refcount.asm"
.include "j8b/new_array.asm"
.include "j8b/arr_celladdr.asm"
.include "mem/rom_read16.asm"
.include "stdio/out_num16.asm"
.include "stdio/out_q7n8.asm"

j8bD99:
.dw 128,0,0

j8bD100:
.dw 0,25853

Main:
	jmp j8b_CMainMmain
_j8b_meta_33:
	.db 15,0

j8b_CMainMmain:
	push r28
	push r29
	lds r28,SPL
	lds r29,SPH
	push c0x00
	push c0x00
	ldi r20,254
	add r20,c0x01
	ldi r16,19
	ldi r17,0
	call j8bproc_new_array
	ldi r19,17
	st x+,r19
	st x+,c0x01
	ldi r19,9
	st x+,r19
	ldi r19,3
	st x+,r19
	st x+,c0x00
	ldi r19,2
	st x+,r19
	st x+,c0x00
	push r17
	push r16
	push zl
	push zh
	ldi zl,low(j8bD99*2)
	ldi zh,high(j8bD99*2)
	ldi r16,low(6)
	ldi r17,high(6)
	call os_rom_read16_nr
	pop zh
	pop zl
	mov r17,r16
	ldi r16,0x00
	mov r16,r20
	ldi r17,0x00
	st x+,r16
	st x+,r17
	push zl
	push zh
	ldi zl,low(j8bD100*2)
	ldi zh,high(j8bD100*2)
	ldi r16,low(4)
	ldi r17,high(4)
	call os_rom_read16_nr
	pop zh
	pop zl
	pop r16
	pop r17
	movw r22,r16
	movw r26,r22
	adiw r26,7
	ld r16,x+
	ld r17,x+
	call os_out_q7n8
	ldi r21,8
	mov r16,r21
	ldi r17,0x00
	push r28
	push r29
	push r17
	push r16
	lds r28,SPL
	lds r29,SPH
	adiw r28,0x01
	ld r16,y+
	ld r17,y+
	lsl r16
	rol r17
	subi r16,low(-5)
	sbci r17,high(-5)
	call j8bproc_new_array
	ldi r19,16
	st x+,r19
	st x+,c0x01
	ldi r19,9
	st x+,r19
	pop r19
	st x+,r19
	pop r19
	st x+,r19
	pop r29
	pop r28
	movw r24,r16
	push c0x01
	push c0x00
	movw r26,r24
	call j8bproc_arr_celladdr
	ldi r19,3
	st x+,r19
	st x+,c0x00
	std y+0,c0x01
	subi yl,low(33)
	sbci yh,high(33)
	std y+32,c0x01
	ldd r19,y+33
	add r19,c0x01
	std y+33,r19
	ldd r19,y+32
	adc r19,c0x00
	std y+32,r19
	ldd r16,y+33
	ldd r17,y+32
	ldi r19,3
	push r19
	push c0x00
	movw r26,r24
	call j8bproc_arr_celladdr
	ld r19,-x
	cp r17,r19
	breq pc+0x02
	brcc _j8b_eoc_0
	ld r19,-x
	cp r16,r19
	brcc _j8b_eoc_0
	ldi r19,3
	push r19
	push c0x00
	movw r26,r24
	call j8bproc_arr_celladdr
	ld r16,x+
	ld r17,x+
	call os_out_num16
_j8b_eoc_0:
	push c0x01
	push c0x00
	movw r26,r24
	call j8bproc_arr_celladdr
	ld r16,x+
	ld r17,x+
	mov r17,r16
	ldi r16,0x00
	push r17
	push r16
	movw r26,r22
	adiw r26,7
	pop r16
	pop r17
	st x+,r16
	st x+,r17
	push r30
	push r31
	movw r30,r22
	call j8bproc_class_refcount_dec
	pop r31
	pop r30
	ldi r22,0
	ldi r23,0
	push r30
	push r31
	movw r30,r24
	call j8bproc_class_refcount_dec
	pop r31
	pop r30
	ldi r24,0
	ldi r25,0
	jmp mcu_halt
