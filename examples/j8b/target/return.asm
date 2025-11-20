; vm5277.avr_codegen v0.1 at Wed Nov 19 17:51:48 GMT+10:00 2025
.equ core_freq = 16
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1
.set OS_FT_WELCOME = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/mfin.asm"
.include "sys/mcu_stop.asm"
.include "stdio/out_num32.asm"

Main:
	rjmp j8bCMainMmain
;======== enter CLASS Main ========================
_j8b_meta22:
	.db 13,0

;build method int 'Main.someMethod()
j8bC25CMainMsomeMethod:
;build block
;const '1.690906E7'->cells REG[20,21,22,23]
	ldi r20,4
	ldi r21,3
	ldi r22,2
	ldi r23,1
;build block
;block end
;build var int 'Main.someMethod.i1', allocated REG[20,21,22,23]
;eUnary POST_INC cells REG[20,21,22,23]
	add r20,C0x01
	adc r21,C0x00
	adc r22,C0x00
	adc r23,C0x00
;cells REG[20,21,22,23] EQ 0 -> accum (isOr:false, isNot:false)
	cpi r23,0
	brne _j8b_eoc0
	cpi r22,0
	brne _j8b_eoc0
	cpi r21,0
	brne _j8b_eoc0
	cpi r20,0
	brne _j8b_eoc0
;var 'i1'->accum
	movw r16,r20
	movw r18,r22
	rjmp _j8b_eob23
_j8b_eoc0:
;var 'i1'->accum
	movw r16,r20
	movw r18,r22
;accum PLUS 4.5495938E8 -> accum
	subi r16,236
	sbci r17,222
	sbci r18,225
	sbci r19,228
_j8b_eob23:
;finish method, type:int, args size:0, vars size:0
	rjmp j8bproc_mfin
;block end
;restore regs:
;method end

;build method void 'Main.main()
j8bCMainMmain:
;build block
	push r30
	push r31
	rcall j8bC25CMainMsomeMethod
;invokeNative System.out [int]
	rcall os_out_num32
;invokeNative System.stop []
	rcall mcu_stop
;finish method, type:void, args size:0, vars size:0
	rjmp j8bproc_mfin
;block end
;method end
;======== leave CLASS Main ========================
