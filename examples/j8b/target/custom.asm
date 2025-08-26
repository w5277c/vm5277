; vm5277.avr_codegen v0.1 at Tue Aug 26 15:08:23 VLAT 2025
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "j8b/instanceof.asm"
.include "j8b/clear_fields.asm"
.include "j8b/invoke_method.asm"
.include "stdio/out_num8.asm"

Main:
	jmp j8bCMainMmain
;======== enter CLASS Main ========================
;======== enter CLASS Byte ========================
_j8b_meta10:
	.db 15,2,13,1,14,2
	.dw j8bC14CByteMtoByte,j8bC14CByteMtoByte,0
;build field byte 'Byte.value', allocated HEAP[0]

;build constructor 'Byte.Byte(byte)
j8bC11CByteMByte:
;eNewInstance 'Byte', heap size:6
	ldi r16,6
	ldi r17,0
	mcall os_dram_alloc
	std z+0,r16
	std z+1,r17
	std z+2,c0x00
	ldi r16,low(_j8b_meta10*2)
	std z+3,r16
	ldi r16,high(_j8b_meta10*2)
	std z+4,r16
	mcall j8bproc_clear_fields_nr
_j8b_cinit12:
;build block
;var 'value'->accum
	ldd r16,y+0
;accum->field value
	std z+5,r16
_j8b_methodend13:
;block end
	ret
;method end

;build method byte 'Byte.toByte()
j8bC14CByteMtoByte:
;build block
;field 'value'->accum
	ldd r16,z+5
;jump
	jmp _j8b_methodend15
_j8b_methodend15:
;block end
	ret
;method end
;======== leave CLASS Byte ========================

;build method void 'Main.main()
j8bCmainMmain:
;build block
;invokeNative void System.setParam [byte, byte], params:LITERAL=2,LITERAL=18
;push heap iReg
	push_z
	ldi zl,low(_j8b_retpoint20)
	push zl
	ldi zl,high(_j8b_retpoint20)
	push zl
;push const '8'
	ldi zl,8
	push zl
;invokeClassMethod Byte Byte.Byte
	jmp j8bC11CByteMByte
_j8b_retpoint20:
;pop heap iReg
	pop_z
;accum->var 'b1'
	mov r20,r16
	mov r21,r17
_j8b_ifbegin22:
;eIf
;push heap iReg
	push_z
;setHeap REG[20,21]
	mov r30,r20
	mov r31,r21
;eInstanceOf 'Byte'
	ldi r17,15
	mcall j8bproc_instanceof_nr
;pop heap iReg
	pop_z
	cpi r16,0x01
	brne _j8b_ifend25
_j8b_ifthen23:
;build block
;push heap iReg
	push_z
	ldi zl,low(_j8b_retpoint21)
	push zl
	ldi zl,high(_j8b_retpoint21)
	push zl
;setHeap REG[20,21]
	mov r30,r20
	mov r31,r21
;invokeInterfaceMethod byte Number.toByte
	ldi r16,13
	ldi r17,0
	jmp j8bproc_invoke_method_nr
_j8b_retpoint21:
;pop heap iReg
	pop_z
;invokeNative void System.out [byte], params:ACCUM
	;load method param
	mcall os_out_num8
_j8b_methodend19:
;block end
_j8b_ifend25:
;build var Number 'Main.main.b1', allocated REG[20,21]
_j8b_methodend18:
;block end
;restore regs:
	ret
;method end
;======== leave CLASS Main ========================
