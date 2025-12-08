; vm5277.avr_codegen v0.2
.equ STDOUT_PORT = 18

.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "j8b/mfin_sf.asm"
.include "stdio/out_num8.asm"

Main:
	jmp j8b_CMainMmain
_j8b_meta_32:
	.db 15,0

j8b_CMainMmain:
	push r30
	push r31
	ldi r19,3
	push r19
	push r30
	push r31
	ldi r19,3
	push r19
	ldi r19,8
	push r19
	rcall j8b_CMainMadd_35
	push r16
	rcall j8b_CMainMadd_35
	call os_out_num8
	jmp mcu_halt

j8b_CMainMadd_35:
	push r28
	push r29
	lds r28,SPL
	lds r29,SPH
	ldd r16,y+5
	ldd j8b_atom,y+6
	add r16,j8b_atom
	ldi r30,2
	jmp j8bproc_mfin_sf
