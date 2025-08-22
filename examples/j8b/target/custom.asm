; vm5277.avr_codegen v0.1 at Sat Aug 23 02:40:52 VLAT 2025
.equ stdout_port = 18

.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "j8b/clear_heap.asm"
.include "stdio/out_num8.asm"

Main:
	jmp j8bCMainMmain
j8bI0CMain: .db 1,13
j8bI10CByte: .db 2,14,12,0
j8bC11CByteMByte:
	ldi r16,6
	ldi r17,0
	mcall os_dram_alloc
	std z+0,r16
	std z+1,r17
	std z+2,c0x00
	ldi r16,low(j8bI10CByte*2)
	std z+3,r16
	ldi r16,high(j8bI10CByte*2)
	std z+4,r16
	mcall j8bproc_clear_heap_nr
j8bCI12CByteMByte:
	ldd r16,y+0
	std z+5,r16
j8bE13CByteMByteB19:
	ret
j8bC14CByteMtoByte:
	ldi r20,0
	mov r16,r20
	push yl
	ldd yl,z+5
	add r16,yl
	pop yl
	jmp j8bE15CByteMtoByteB21
j8bE15CByteMtoByteB21:
	ret
j8bCmainMmain:
	push_z
	ldi zl,8
	push zl
	mcall j8bC11CByteMByte
	pop_z
	mov r20,r16
	mov r21,r17
	push_z
	mov r30,r20
	mov r31,r21
	mcall j8bC14CByteMtoByte
	pop_z
	mcall os_out_num8
j8bE16CMainMmainB24:
	ret
