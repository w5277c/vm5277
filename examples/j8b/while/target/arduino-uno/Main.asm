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
.include "stdio/out_char.asm"
.include "core/wait_s.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_cstr.asm"

j8bD151:
.db 0x0a,"byte i=",0x00,0x00
j8bD152:
.db 0x0a,0xe3,0xc9,0xcb,0xcc," while(i<10), ",0xd3," ",0xce,0xcb,0xd2,0xc5,0xcd,0xc5,0xce,0xd4,0xcf,0xcd," i ",0xc9," break ",0xce,0xc1," 5: ",0x00,0x00
j8bD154:
.db 0x0a,0xe3,0xc9,0xcb,0xcc," while(false)",0x00,0x00
j8bD155:
.db 0x0a,0xe3,0xc9,0xcb,0xcc," do{...}while(i<20), ",0xd3," ",0xc9,0xce,0xcb,0xd2,0xc5,0xcd,0xc5,0xce,0xd4,0xcf,0xcd," i: ",0x00
j8bD156:
.db 0x0a,0xe2,0xc5,0xd3,0xcb,0xcf,0xce,0xc5,0xde,0xce,0xd9,0xca," ",0xc3,0xc9,0xcb,0xcc," while(true), ",0xd3," ",0xd0,0xc1,0xd5,0xda,0xcf,0xca," ",0xd7," 1 ",0xd3,0xc5,0xcb,": ",0x00
j8bD157:
.db 0x0a,"done",0x00

Main:
	jmp j8b_CMainMmain
_j8b_meta_46:
	.db 22,0

j8b_CMainMmain:
	ldi r20,3
	ldi r16,low(j8bD151*2)
	ldi r17,high(j8bD151*2)
	movw r30,r16
	call os_out_cstr
	mov r16,r20
	call os_out_num8
	ldi r16,low(j8bD152*2)
	ldi r17,high(j8bD152*2)
	movw r30,r16
	call os_out_cstr
_j8b_loop_49:
	cpi r20,10
	brcc _j8b_eoc_0
	mov r16,r20
	call os_out_num8
	ldi r16,32
	call os_out_char
	add r20,c0x01
	cpi r20,5
	brne _j8b_loop_49
_j8b_eoc_0:
	ldi r16,low(j8bD154*2)
	ldi r17,high(j8bD154*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,low(j8bD155*2)
	ldi r17,high(j8bD155*2)
	movw r30,r16
	call os_out_cstr
_j8b_loop_60:
	mov r16,r20
	call os_out_num8
	ldi r16,32
	call os_out_char
	add r20,c0x01
	cpi r20,20
	brcs _j8b_loop_60
	ldi r16,low(j8bD156*2)
	ldi r17,high(j8bD156*2)
	movw r30,r16
	call os_out_cstr
_j8b_loop_65:
	ldi r16,1
	ldi r17,0
	call os_wait_s
	ldi r16,35
	call os_out_char
	rjmp _j8b_loop_65
