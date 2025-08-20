; vm5277.avr_codegen v0.1 at Tue Aug 19 18:51:11 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_WELCOME = 1
.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "stdio/out_num16.asm"

Main:
;Launch 
	ldi r16,5
	ldi r17,0
	mcall os_dram_alloc
	std z+0,r16
	std z+1,r17
	std z+2,c0xff
	ldi r16,1
	std z+3,r16
	ldi r16,13
	std z+4,r16
	movw r16,zl
	jmp j8bCMainMmain
;build class Main,id:0
;build method void 'Main.main(), id:77
j8bCmainMmain:
;build block Main.main,id:78
;invokeNative void System.setParam [byte, byte], params:LITERAL=0,LITERAL=16
;invokeNative void System.setParam [byte, byte], params:LITERAL=2,LITERAL=18
;invokeNative void System.setParam [byte, byte], params:LITERAL=3,LITERAL=1
;build var short 'Main.main.s1', allocated REG[24,25]
;const '1'->cells REG[24,25]
	ldi r24,1
	ldi r25,0
;build var short 'Main.main.s2', allocated REG[26,27]
;const '16'->cells REG[26,27]
	ldi r26,16
	ldi r27,0
;build var short 'Main.main.s3', allocated REG[20,21]
;const '256'->cells REG[20,21]
	ldi r20,0
	ldi r21,1
;build var short 'Main.main.s4', allocated REG[22,23]
;const '4096'->cells REG[22,23]
	ldi r22,0
	ldi r23,16
;var 's4'->accum
	mov r16,r22
	mov r17,r23
;accum PLUS cells REG[20,21] -> accum
	add r16,r20
	adc r17,r21
;push accum(BE)
	push r17
	push r16
;var 's2'->accum
	mov r16,r26
	mov r17,r27
;accum PLUS cells REG[24,25] -> accum
	add r16,r24
	adc r17,r25
;accum PLUS cells STACK[x2] -> accum
	pop j8b_atom
	add r16,j8b_atom
	pop j8b_atom
	adc r17,j8b_atom
;invokeNative void System.out [short], params:ACCUM
	;load method param
	mcall os_out_num16
;block end
;restore regs:
;method end
	ret
;class end
