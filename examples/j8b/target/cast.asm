; vm5277.avr_codegen v0.1 at Tue Aug 19 18:51:40 VLAT 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
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
;build var int 'Main.main.i', allocated REG[20,21,22,23]
;const '67305985'->cells REG[20,21,22,23]
	ldi r20,1
	ldi r21,2
	ldi r22,3
	ldi r23,4
;invokeNative void System.outCStr [cstr], params:FLASH_RES=80
	ldi r30,low(j8bD80*2)
	ldi r31,high(j8bD80*2)
	mcall os_out_cstr
;invokeNative void System.out [int], params:LOCAL_RES=79
	;load method param
	mov r16,r20
	mov r17,r21
	mov r18,r22
	mov r19,r23
	mcall os_out_num32
;invokeNative void System.outChar [byte], params:LITERAL=10
	;load method param
	ldi r16,10
	mcall os_out_char
;var 'i'->accum
	mov r16,r20
	mov r17,r21
	mov r18,r22
	mov r19,r23
;acc cast 4->2
;acc cast 2->4
	ldi r18,0x00
	ldi r19,0x00
;accum->var 'i'
	mov r20,r16
	mov r21,r17
	mov r22,r18
	mov r23,r19
;invokeNative void System.outCStr [cstr], params:FLASH_RES=81
	ldi r30,low(j8bD81*2)
	ldi r31,high(j8bD81*2)
	mcall os_out_cstr
;invokeNative void System.out [int], params:LOCAL_RES=79
	;load method param
	mov r16,r20
	mov r17,r21
	mov r18,r22
	mov r19,r23
	mcall os_out_num32
;invokeNative void System.outChar [byte], params:LITERAL=10
	;load method param
	ldi r16,10
	mcall os_out_char
;invokeNative void System.outCStr [cstr], params:FLASH_RES=82
	ldi r30,low(j8bD82*2)
	ldi r31,high(j8bD82*2)
	mcall os_out_cstr
;var 'i'->accum
	mov r16,r20
	mov r17,r21
	mov r18,r22
	mov r19,r23
;acc cast 4->1
;invokeNative void System.out [byte], params:LOCAL_RES=79
	;load method param
	mov r16,r20
	mcall os_out_num8
;invokeNative void System.outChar [byte], params:LITERAL=10
	;load method param
	ldi r16,10
	mcall os_out_char
;block end
;restore regs:
;method end
	ret
;class end
