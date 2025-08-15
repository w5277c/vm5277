; vm5277.avr_codegen v0.1 at Sat Aug 16 00:17:41 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "stdio/out_num8.asm"

MAIN:
	push yl
	ldi yl,0
	std z+6,yl
	pop yl
JavlCByteMconstr12:
	ldd r16,y+0
	std z+6,r16
	ret
JavlCByteMtoByte13:
	ldd r16,z+6
	mov r30,r16
	ret
JavlCMainMmain15:
	ldi r16,6
	ldi r17,0
	mcall os_dram_alloc
	std z+0,r16
	std z+1,r17
	std z+2,c0x00
	ldi r16,2
	std z+3,r16
	ldi r16,15
	std z+4,r16
	ldi r16,14
	std z+5,r16
	movw r16,zl
	mov r20,r16
	mov r21,r17
	push r16
	push yl
	push yh
	mov yl,r20
	mov yh,r21
	ldd r16,z+0x02
	cpse r16,c0xff
	inc r16
	std z+0x02,r16
	pop zh
	pop zl
	pop r16
	push yl
	push yh
	ldi yl,1
	push yl
	lds yl,SPL
	lds yh,SPH
	mcall JavlCByteMconstr12
	pop yl
	pop yh
	pop yl
	mov r16,r20
	mov r17,r21
	mov r30,r20
	mov r31,r21
	lds yl,SPL
	lds yh,SPH
	mcall JavlCByteMtoByte13
	mov r16,zl
	mcall os_out_num8
	ret
