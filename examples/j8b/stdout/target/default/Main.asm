; vm5277.AVR v0.2
.equ CORE_FREQ = 16
.set STDIN_PORT_REGID = 11
.set STDIN_DDR_REGID = 10
.set STDIN_PIN_REGID = 9
.set STDIN_PORTNUM = 3
.set STDIN_PINNUM = 3
.set STDOUT_PORT_REGID = 11
.set STDOUT_DDR_REGID = 10
.set STDOUT_PIN_REGID = 9
.set STDOUT_PORTNUM = 3
.set STDOUT_PINNUM = 4

.set OS_FT_STDOUT = 1

.include "devices/atmega168p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "stdio/out_char.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_num16.asm"
.include "stdio/out_cstr.asm"
.include "stdio/out_bool.asm"
.include "stdio/out_num32.asm"
.include "stdio/out_q7n8.asm"

j8bD141:
.db "Constant string",0x00
j8bD142:
.db 0xeb,0xcf,0xce,0xd3,0xd4,0xc1,0xce,0xd4,0xce,0xc1,0xd1," ",0xd3,0xd4,0xd2,0xcf,0xcb,0xc1," ",0xd7," ",0xcb,0xcf,0xc4,0xc9,0xd2,0xcf,0xd7,0xcb,0xc5," KOI8-r",0x00

Main:
	jmp j8b_CMainMmain
_j8b_meta_41:
	.db 22,0

j8b_CMainMmain:
	ldi r16,35
	call os_out_char
	ldi r16,10
	call os_out_char
	ldi r16,low(j8bD141*2)
	ldi r17,high(j8bD141*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,10
	call os_out_char
	ldi r16,low(j8bD142*2)
	ldi r17,high(j8bD142*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,10
	call os_out_char
	ldi r16,255
	call os_out_num8
	ldi r16,10
	call os_out_char
	ldi r16,255
	ldi r17,255
	call os_out_num16
	ldi r16,10
	call os_out_char
	ldi r16,255
	ldi r17,255
	ldi r18,255
	ldi r19,255
	call os_out_num32
	ldi r16,10
	call os_out_char
	ldi r16,253
	ldi r17,1
	call os_out_q7n8
	ldi r16,10
	call os_out_char
	ldi r16,2
	ldi r17,1
	call os_out_q7n8
	ldi r16,10
	call os_out_char
	ldi r16,23
	ldi r17,1
	call os_out_q7n8
	ldi r16,10
	call os_out_char
	ldi r16,25
	ldi r17,1
	call os_out_q7n8
	ldi r16,10
	call os_out_char
	ldi r16,28
	ldi r17,1
	call os_out_q7n8
	ldi r16,10
	call os_out_char
	ldi r16,253
	ldi r17,127
	call os_out_q7n8
	ldi r16,10
	call os_out_char
	ldi r16,253
	ldi r17,10
	call os_out_q7n8
	ldi r16,10
	call os_out_char
	ldi r16,25
	ldi r17,254
	call os_out_q7n8
	ldi r16,10
	call os_out_char
	ldi r16,2
	ldi r17,128
	call os_out_q7n8
	ldi r16,10
	call os_out_char
	ldi r16,0
	ldi r17,128
	call os_out_q7n8
	ldi r16,10
	call os_out_char
	ldi r16,1
	call os_out_bool
	ldi r16,10
	call os_out_char
	ldi r16,0
	call os_out_bool
	ldi r16,10
	call os_out_char
	ldi r16,1
	call os_out_bool
	ldi r16,10
	call os_out_char
	jmp mcu_halt
