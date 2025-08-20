; vm5277.avr_codegen v0.1 at Thu Aug 21 02:32:44 VLAT 2025
.equ stdout_port = 18

.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "stdio/out_num8.asm"
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
	ldi r16,12
	std z+4,r16
	movw r16,zl
	jmp j8bCMainMmain
;build class Main,id:0
;build class Byte,id:16
;build field byte 'Byte.value', allocated HEAP[0]
;const '0'->cells HEAP[0]
	push yl
	ldi yl,0
	std z+6,yl
	pop yl
;build method null 'Byte.constr(byte), id:18
j8bCByteMconstr7:
;build block Byte.constr,id:19
;var 'value'->accum
	ldd r16,y+0
;accum->field value
	std z+6,r16
;block end
;build var byte 'Byte.constr.value', allocated STACK_FRAME[0]
;method end
	ret
;build var byte 'Byte.constr.value', allocated STACK_FRAME[1]
;build method byte 'Byte.toByte(), id:20
j8bCByteMtoByte8:
;build block Byte.toByte,id:21
;field 'value'->accum
	ldd r16,z+6
	ret
;block end
;method end
;class end
;build method void 'Main.main(), id:24
j8bCmainMmain:
;build block Main.main,id:25
;invokeNative void System.setParam [byte, byte], params:LITERAL=2,LITERAL=18
;eNew [Byte,Number], heap size:6
	ldi r16,6
	ldi r17,0
	mcall os_dram_alloc
	std z+0,r16
	std z+1,r17
	std z+2,c0x00
	ldi r16,2
	std z+3,r16
	ldi r16,14
	std z+4,r16
	ldi r16,13
	std z+5,r16
	movw r16,zl
;accum->var 'b1'
	mov r20,r16
	mov r21,r17
;refCount++ for REG[20,21]
	push_z
	mov zl,r20
	mov zh,r21
	mcall j8bproc_inc_refcount
	pop_z
;push stack iReg
	push yl
	push yh
;push const '1'
	ldi yl,1
	push yl
;stack addr to stack iReg
	lds yl,SPL
	lds yh,SPH
;invokeMethod Byte Byte.Byte
	mcall j8bCByteMconstr7
;free stack, size:1
	pop yl
;pop stack iReg
	pop yh
	pop yl
;build var Byte 'Main.main.b1', allocated REG[20,21]
;var 'b1'->accum
	mov r16,r20
	mov r17,r21
;push heap iReg
	push zl
	push zh
;setHeap REG[20,21]
	mov r30,r20
	mov r31,r21
;stack addr to stack iReg
	lds yl,SPL
	lds yh,SPH
;invokeMethod byte Byte.toByte
	mcall j8bCByteMtoByte8
;pop heap iReg
	pop zh
	pop zl
;invokeNative void System.out [byte], params:ACCUM
	;load method param
	mcall os_out_num8
;const '0'->cells REG[20,21]
	ldi r20,0
	ldi r21,0
;refCount-- for REG[20,21]
	push_z
	mov zl,r20
	mov zh,r21
	mcall j8bproc_dec_refcount
	pop_z
;eNew [Byte,Number], heap size:6
	ldi r16,6
	ldi r17,0
	mcall os_dram_alloc
	std z+0,r16
	std z+1,r17
	std z+2,c0x00
	ldi r16,2
	std z+3,r16
	ldi r16,14
	std z+4,r16
	ldi r16,13
	std z+5,r16
	movw r16,zl
;accum->var 'b2'
	mov r22,r16
	mov r23,r17
;refCount++ for REG[22,23]
	push_z
	mov zl,r22
	mov zh,r23
	mcall j8bproc_inc_refcount
	pop_z
;push stack iReg
	push yl
	push yh
;push const '1'
	ldi yl,1
	push yl
;stack addr to stack iReg
	lds yl,SPL
	lds yh,SPH
;invokeMethod Byte Byte.Byte
	mcall j8bCByteMconstr7
;free stack, size:1
	pop yl
;pop stack iReg
	pop yh
	pop yl
;build var Byte 'Main.main.b2', allocated REG[22,23]
;var 'b2'->accum
	mov r16,r22
	mov r17,r23
;push heap iReg
	push zl
	push zh
;setHeap REG[22,23]
	mov r30,r22
	mov r31,r23
;stack addr to stack iReg
	lds yl,SPL
	lds yh,SPH
;invokeMethod byte Byte.toByte
	mcall j8bCByteMtoByte8
;pop heap iReg
	pop zh
	pop zl
;invokeNative void System.out [byte], params:ACCUM
	;load method param
	mcall os_out_num8
;var 'b2'->accum
	mov r16,r22
	mov r17,r23
;invokeMethod short Byte.getClassTypeId
;const '14'->accum 
	ldi r16,14
	ldi r17,0
;invokeNative void System.out [short], params:ACCUM
	;load method param
	mcall os_out_num16
;var 'b2'->accum
	mov r16,r22
	mov r17,r23
;invokeMethod short Byte.getClassId
;var 'b2'->accum
	mov r16,r22
	mov r17,r23
;invokeNative void System.out [short], params:ACCUM
	;load method param
	mcall os_out_num16
;build block Main.main,id:28
;invokeNative void System.out [byte], params:LITERAL=33
	;load method param
	ldi r16,33
	mcall os_out_num8
;block end
;block end
;restore regs:
;method end
	ret
;class end
