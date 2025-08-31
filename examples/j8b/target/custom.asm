; vm5277.avr_codegen v0.1 at Sun Aug 31 15:55:41 VLAT 2025
.equ stdout_port = 18

.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "stdio/out_num16.asm"

Main:
	jmp j8bCMainMmain

j8bCmainMmain:
	ldi r20,2
	ldi r21,0
	ldi r22,3
	ldi r23,0
_j8b_ifbegin11:
	mov r16,r20
	mov r17,r21
	cpi r17,0
	brne _j8b_ifelse13
	cpi r16,2
	brne _j8b_ifelse13
	mov r16,r22
	mov r17,r23
	cpi r17,0
	brne _j8b_cppoint17
	cpi r16,3
	breq _j8b_ifelse13
_j8b_cppoint17:
	mov r16,r20
	mov r17,r21
	cpi r17,0
	brcs _j8b_ifthen12
	brne _j8b_cppoint18
	cpi r16,1
	brcs _j8b_ifthen12
_j8b_cppoint18:
	mov r16,r22
	mov r17,r23
	cpi r17,0
	breq pc+0x03
	brcc _j8b_ifthen12
	brne _j8b_cppoint19
	cpi r16,5
	breq pc+0x02
	brcc _j8b_ifthen12
_j8b_cppoint19:
	jmp _j8b_ifelse13
_j8b_ifthen12:
	mov r16,r20
	mov r17,r21
	mcall os_out_num16
_j8b_methodend15:
_j8b_ifelse13:
_j8b_ifend14:
_j8b_methodend10:
	ret
