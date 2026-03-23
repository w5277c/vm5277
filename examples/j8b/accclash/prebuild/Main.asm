; vm5277.AVR v0.3
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
.set OS_FT_WELCOME = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_cstr.asm"

j8bD145:
.db 0x0a,0xfa,0xce,0xc1,0xde,0xc5,0xce,0xc9,0xc5," ",0xd7,0xd9,0xd2,0xc1,0xd6,0xc5,0xce,0xc9,0xd1," (b1+b2) + (b3+b4), ",0xc7,0xc4,0xc5," b1=",0x00,0x00
j8bD146:
.db ",b2=",0x00,0x00
j8bD147:
.db ",b3=",0x00,0x00
j8bD148:
.db ",b4=",0x00,0x00
j8bD149:
.db ":",0x00
j8bD150:
.db 0x0a,"done",0x00

Main:
	jmp j8b_CMainMmain
_j8b_meta_41:
	.db 22,0

j8b_CMainMmain:
	ldi r20,1
	ldi r21,2
	ldi r22,3
	ldi r23,4
	ldi r16,low(j8bD145*2)
	ldi r17,high(j8bD145*2)
	call os_out_cstr
	mov r16,r20
	call os_out_num8
	ldi r16,low(j8bD146*2)
	ldi r17,high(j8bD146*2)
	call os_out_cstr
	mov r16,r21
	call os_out_num8
	ldi r16,low(j8bD147*2)
	ldi r17,high(j8bD147*2)
	call os_out_cstr
	mov r16,r22
	call os_out_num8
	ldi r16,low(j8bD148*2)
	ldi r17,high(j8bD148*2)
	call os_out_cstr
	mov r16,r23
	call os_out_num8
	ldi r16,low(j8bD149*2)
	ldi r17,high(j8bD149*2)
	call os_out_cstr
	mov r16,r21
	add r16,r20
	push r16
	mov r16,r23
	add r16,r22
	pop j8b_atom
	add r16,j8b_atom
	call os_out_num8
	ldi r16,low(j8bD150*2)
	ldi r17,high(j8bD150*2)
	call os_out_cstr
	jmp mcu_halt
