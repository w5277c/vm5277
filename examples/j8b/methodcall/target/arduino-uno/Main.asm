; vm5277.AVR v0.2
.equ CORE_FREQ = 16
.set STDIN_PORT_REGID = 11
.set STDIN_DDR_REGID = 10
.set STDIN_PIN_REGID = 9
.set STDIN_PORTNUM = 3
.set STDIN_PINNUM = 0
.set STDOUT_PORT_REGID = 11
.set STDOUT_DDR_REGID = 10
.set STDOUT_PIN_REGID = 9
.set STDOUT_PORTNUM = 3
.set STDOUT_PINNUM = 1

.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "math/mul32.asm"
.include "j8b/mfin_sf.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_cstr.asm"
.include "stdio/out_num32.asm"

j8bD163:
.db 0x0a,0x0a,0xf2,0xc5,0xcb,0xd5,0xd2,0xd3,0xc9,0xd1,", ",0xc6,0xc1,0xcb,0xd4,0xcf,0xd2,0xc9,0xc1,0xcc," ",0xc9,0xda," 12[479001600]:",0x00
j8bD167:
.db 0x0a,"done",0x00
j8bD154:
.db 0x0a,0xf7,0xd9,0xda,0xcf,0xd7," ",0xcd,0xc5,0xd4,0xcf,0xc4,0xc1," add(a+b, add(c, 8)):",0x00,0x00
j8bD155:
.db 0x0a,0xf7,0xd9,0xdb,0xcc,0xc9," ",0xc9,0xda," ",0xcd,0xc5,0xd4,0xcf,0xc4,0xc1,", ",0xd0,0xcf,0xcc,0xd5,0xde,0xc9,0xcc,0xc9," ",0xda,0xce,0xc1,0xde,0xc5,0xce,0xc9,0xc5,":",0x00,0x00
j8bD156:
.db 0x0a,0xf7,0xcf,0xdb,0xcc,0xc9," ",0xd7," ",0xcd,0xc5,0xd4,0xcf,0xc4," add, ",0xc1,0xd2,0xc7,"1:",0x00
j8bD157:
.db ", ",0xc1,0xd2,0xc7,"2:",0x00
j8bD158:
.db 0x0a,0xf7,0xd9,0xc8,0xcf,0xc4,0xc9,0xcd," ",0xc9,0xda," ",0xcd,0xc5,0xd4,0xcf,0xc4,0xc1,", ",0xd2,0xc5,0xda,0xd5,0xcc,0xd8,0xd4,0xc1,0xd4,":",0x00,0x00

Main:
	jmp j8b_CMainMmain
_j8b_meta_42:
	.db 22,0

j8b_CMainMmain:
	ldi r16,low(j8bD154*2)
	ldi r17,high(j8bD154*2)
	movw r30,r16
	call os_out_cstr
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
	rcall j8b_CMainMadd_45
	push r16
	rcall j8b_CMainMadd_45
	mov r20,r16
	ldi r16,low(j8bD155*2)
	ldi r17,high(j8bD155*2)
	movw r30,r16
	call os_out_cstr
	mov r16,r20
	call os_out_num8
	ldi r16,low(j8bD163*2)
	ldi r17,high(j8bD163*2)
	movw r30,r16
	call os_out_cstr
	push r30
	push r31
	ldi r30,12
	push r30
	push c0x00
	push c0x00
	push c0x00
	rcall j8b_CMainMfactorial_48
	call os_out_num32
	ldi r16,low(j8bD167*2)
	ldi r17,high(j8bD167*2)
	movw r30,r16
	call os_out_cstr
	jmp mcu_halt

j8b_CMainMadd_45:
	push r28
	push r29
	lds r28,SPL
	lds r29,SPH
	ldd r16,y+5
	ldd j8b_atom,y+6
	add r16,j8b_atom
	mov r20,r16
	ldi r16,low(j8bD156*2)
	ldi r17,high(j8bD156*2)
	movw r30,r16
	call os_out_cstr
	ldd r16,y+6
	call os_out_num8
	ldi r16,low(j8bD157*2)
	ldi r17,high(j8bD157*2)
	movw r30,r16
	call os_out_cstr
	ldd r16,y+5
	call os_out_num8
	ldi r16,low(j8bD158*2)
	ldi r17,high(j8bD158*2)
	movw r30,r16
	call os_out_cstr
	mov r16,r20
	call os_out_num8
	mov r16,r20
	ldi r30,2
	jmp j8bproc_mfin_sf

j8b_CMainMfactorial_48:
	push r28
	push r29
	lds r28,SPL
	lds r29,SPH
	ldd r16,y+5
	cpi r16,0
	breq pc+0x03
	brcc _j8b_eoc_0
	brne _j8b_mcpe_50
	ldd r16,y+6
	cpi r16,0
	breq pc+0x03
	brcc _j8b_eoc_0
	brne _j8b_mcpe_50
	ldd r16,y+7
	cpi r16,0
	breq pc+0x03
	brcc _j8b_eoc_0
	brne _j8b_mcpe_50
	ldd r16,y+8
	cpi r16,1
	breq pc+0x02
	brcc _j8b_eoc_0
_j8b_mcpe_50:
	ldi r16,1
	ldi r17,0
	ldi r18,0
	ldi r19,0
	rjmp _j8b_eob_46
_j8b_eoc_0:
	push r30
	push r31
	ldd r16,y+8
	ldd r17,y+7
	ldd r18,y+6
	ldd r19,y+5
	subi r16,1
	sbci r17,0
	sbci r18,0
	sbci r19,0
	push r16
	push r17
	push r18
	push r19
	rcall j8b_CMainMfactorial_48
	push r24
	push r25
	push r22
	push r23
	ldd r24,y+8
	ldd r25,y+7
	ldd r22,y+6
	ldd r23,y+5
	call os_mul32_nr
	pop r23
	pop r22
	pop r25
	pop r24
_j8b_eob_46:
	ldi r30,4
	jmp j8bproc_mfin_sf
