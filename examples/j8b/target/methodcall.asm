; vm5277.avr_codegen v0.1 at Sun Sep 07 03:25:17 VLAT 2025
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "stdio/out_num8.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta24:
	.db 13,0

j8bCMainMmain:
	ldi r20,1
	ldi r21,2
	ldi r22,3
	push zl
	mov r16,r21
	add r16,r20
	push r16
	push zl
	push r22
	ldi r30,8
	push r30
	rcall j8bC26CMainMinc
	lds zl,SREG
	cli
	sts SPL,yl
	sts SPH,yh
	sts SREG,zl
	pop zl
	push r16
	rcall j8bC26CMainMinc
	lds zl,SREG
	cli
	sts SPL,yl
	sts SPH,yh
	sts SREG,zl
	pop zl
	rcall os_out_num8
	movw zl,yl
	subi yl,low(-2)
	sbci yh,high(-2)
	ijmp

j8bC26CMainMinc:
	lds yl,SPL
	lds yh,SPH
	ldd r16,y+0
	push zl
	ldd zl,y+1
	add r16,zl
	pop zl
	movw zl,yl
	subi yl,low(-4)
	sbci yh,high(-4)
	ijmp
