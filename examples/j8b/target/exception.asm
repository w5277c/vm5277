; vm5277.avr_codegen v0.2
.equ CORE_FREQ = 16
.equ STDOUT_PORT = 18
.equ ACTLED_PORT = 17
.set OS_ETRACE_POINT_BITSIZE = 7

.set OS_FT_STDOUT = 1
.set OS_FT_WELCOME = 1
.set OS_FT_HEARTBEAT = 1
.set OS_FT_ETRACE = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "sys/mcu_blink_n_reset.asm"
.include "j8b/etrace_out.asm"
.include "j8b/etrace_add.asm"
.include "j8b/mfin.asm"
.include "stdio/out_num8.asm"

Main:
	jmp j8b_CMainMmain
_j8b_meta_32:
	.db 15,0

j8b_CMainMruntimeTest_38:
	ldi r16,1
	ldi r17,0x00
	ldi r18,1
	call j8bproc_etrace_addfirst
	set
	jmp j8bproc_mfin

j8b_CMainMmain:
	push r30
	push r31
	rcall j8b_CMainMruntimeTest_38
	brtc _j8b_catchskip_114
	cpi r16,1
	breq _j8b_catch_45
	rjmp _j8b_catch_46
	ldi r16,64
	call os_out_num8
_j8b_catchskip_114:
	rjmp _j8b_catchskip_116
_j8b_catch_45:
	ldi r16,32
	call os_out_num8
_j8b_catchskip_116:
	rjmp _j8b_catchskip_118
_j8b_catch_46:
	ldi r16,33
	call os_out_num8
_j8b_catchskip_118:
	ldi r16,6
	ldi r18,3
	call j8bproc_etrace_addfirst
	rjmp _j8b_catch_49
_j8b_catch_49:
	ldi r16,1
	call os_out_num8
	brts _j8b_skip_122
	jmp mcu_halt
_j8b_skip_122:
	call j8bproc_etrace_out
	jmp mcu_blink_n_reset
