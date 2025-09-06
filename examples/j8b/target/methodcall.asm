; vm5277.avr_codegen v0.1 at Sun Sep 07 03:00:47 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_WELCOME = 1
.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "stdio/out_num8.asm"
.include "sys/mcu_stop.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta24:
	.db 13,0
_j8b_meta25:
	.db 14,0

j8bC26CTestMtest:
	ldi r16,33
	rcall os_out_num8
	movw zl,yl
	subi yl,low(-2)
	sbci yh,high(-2)
	ijmp

j8bC28CTestMTest:
	ldi r16,5
	ldi r17,0
	rcall os_dram_alloc
	std z+0,r16
	std z+1,r17
	std z+2,c0x00
	ldi r16,low(_j8b_meta25*2)
	std z+3,r16
	ldi r16,high(_j8b_meta25*2)
	std z+4,r16
_j8b_cinit29:
	movw zl,yl
	subi yl,low(-2)
	sbci yh,high(-2)
	ijmp

j8bCMainMmain:
	ldi r21,1
	ldi r20,2
	push zl
	push r20
	ldi r30,2
	push r30
	rcall j8bC32CMainMinc
	lds zl,SREG
	cli
	sts SPL,yl
	sts SPH,yh
	sts SREG,zl
	pop zl
	rcall os_out_num8
	push zl
	push r20
	push r20
	rcall j8bC32CMainMinc
	lds zl,SREG
	cli
	sts SPL,yl
	sts SPH,yh
	sts SREG,zl
	pop zl
	rcall os_out_num8
	push zl
	ldi r30,2
	push r30
	ldi r30,2
	push r30
	rcall j8bC32CMainMinc
	lds zl,SREG
	cli
	sts SPL,yl
	sts SPH,yh
	sts SREG,zl
	pop zl
	rcall os_out_num8
	push zl
	mov r16,r20
	add r16,r21
	push r16
	push zl
	ldi r30,1
	push r30
	push zl
	ldi r30,2
	push r30
	ldi r30,3
	push r30
	rcall j8bC32CMainMinc
	lds zl,SREG
	cli
	sts SPL,yl
	sts SPH,yh
	sts SREG,zl
	pop zl
	push r16

	rcall j8bC32CMainMinc
	pop zl


	push r16
	rcall j8bC32CMainMinc
	lds zl,SREG
	cli
	sts SPL,yl
	sts SPH,yh
	sts SREG,zl
	pop zl
	rcall os_out_num8
	push zl
	push zh
	rcall j8bC28CTestMTest
	lds zl,SREG
	cli
	sts SPL,yl
	sts SPH,yh
	sts SREG,zl
	pop zh
	pop zl
	mov r22,r16
	mov r23,r17
	push zl
	push zh
	mov r30,r22
	mov r31,r23
	rcall j8bC26CTestMtest
	lds zl,SREG
	cli
	sts SPL,yl
	sts SPH,yh
	sts SREG,zl
	pop zh
	pop zl
	rcall mcu_stop
	movw zl,yl
	subi yl,low(-2)
	sbci yh,high(-2)
	ijmp

j8bC32CMainMinc:
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
	lds zl,SREG
	cli
	sts SPL,yl
	sts SPH,yh
	sts SREG,zl
	ret
