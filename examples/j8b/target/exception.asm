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
.include "math/div8.asm"
.include "j8b/mfin.asm"
.include "j8b/mfin_sf.asm"
.include "stdio/out_num8.asm"
.include "j8b/etrace_out.asm"

Main:
	jmp j8b_CMainMmain
_j8b_meta_33:
	.db 15,0

j8b_CMainMdivTest_35:
	push r28
	push r29
	lds r28,SPL
	lds r29,SPH
	ldi r20,1
	sub r20,c0x01
	brcc _j8b_throwskip_142
	ldi r16,4
	ldi r18,7
	call j8bproc_etrace_add
	rjmp _j8b_eob_34
_j8b_throwskip_142:
	ldd r16,y+5
	mov r17,r20
	tst r17
	brne _j8b_throwskip_143
	ldi r16,3
	ldi r18,8
	call j8bproc_etrace_add
	rjmp _j8b_eob_34
_j8b_throwskip_143:
	push r24
	call os_div8
	pop r24
_j8b_eob_34:
	ldi r30,1
	jmp j8bproc_mfin_sf

j8b_CMainMtest_37:
	ldi r16,3
	ldi r17,0x00
	ldi r18,1
	call j8bproc_etrace_addfirst
	set
	jmp j8bproc_mfin

j8b_CMainMruntimeTest_39:
	ldi r16,1
	ldi r17,0x00
	ldi r18,3
	call j8bproc_etrace_addfirst
	set
	jmp j8bproc_mfin

j8b_CMainMmain:
	push r30
	push r31
	rcall j8b_CMainMtest_37
	brtc _j8b_throwskip_125
	cpi r16,3
	breq _j8b_catch_48
	ldi r18,2
	call j8bproc_etrace_add
	rjmp _j8b_eob_40
_j8b_throwskip_125:
	rjmp _j8b_catchskip_127
_j8b_catch_48:
	ldi r16,2
	call os_out_num8
_j8b_catchskip_127:
	push r30
	push r31
	rcall j8b_CMainMruntimeTest_39
	brtc _j8b_throwskip_132
	cpi r16,1
	breq _j8b_catch_57
	rjmp _j8b_catch_58
_j8b_throwskip_132:
	rjmp _j8b_catchskip_134
_j8b_catch_57:
	ldi r16,32
	call os_out_num8
_j8b_catchskip_134:
_j8b_catch_58:
	ldi r17,2
	ldi r16,6
	ldi r18,5
	call j8bproc_etrace_addfirst
	rjmp _j8b_catch_61
_j8b_catch_61:
	ldi r16,1
	call os_out_num8
	ldi r20,0
_j8b_loop_65:
	mov r16,r20
	call os_out_num8
	add r20,c0x01
	brcc _j8b_loop_65
	ldi r16,4
	ldi r18,6
	call j8bproc_etrace_add
	lds r16,_os_etrace_buffer+0x00
	call os_out_num8
	lds r16,_os_etrace_buffer+0x01
	call os_out_num8
	call j8bproc_etrace_out
	push r30
	push r31
	ldi r19,10
	push r19
	rcall j8b_CMainMdivTest_35
	brtc _j8b_throwskip_145
	cpi r16,3
	breq _j8b_catch_73
	cpi r16,4
	breq _j8b_catch_73
	ldi r18,9
	call j8bproc_etrace_add
	rjmp _j8b_eob_40
_j8b_throwskip_145:
	call os_out_num8
	ldi r16,4
	ldi r17,0x00
	ldi r18,10
	call j8bproc_etrace_addfirst
	rjmp _j8b_catch_74
_j8b_catch_73:
	ldi r16,10
	call os_out_num8
	rjmp _j8b_catchskip_148
_j8b_catch_74:
	ldi r16,11
	call os_out_num8
_j8b_catchskip_148:
_j8b_eob_40:
	brts _j8b_skip_150
	jmp mcu_halt
_j8b_skip_150:
	call j8bproc_etrace_out
	jmp mcu_blink_n_reset
