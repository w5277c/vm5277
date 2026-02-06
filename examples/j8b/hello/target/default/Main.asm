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
.include "stdio/out_cstr.asm"

j8bD141:
.db "Hello world!",0x0a,0xf0,0xd2,0xc9,0xd7,0xc5,0xd4," ",0xcd,0xc9,0xd2,"!",0x0a,0x00

Main:
	jmp j8b_CMainMmain
_j8b_meta_41:
	.db 22,0

j8b_CMainMmain:
	ldi r16,low(j8bD141*2)
	ldi r17,high(j8bD141*2)
	movw r30,r16
	call os_out_cstr
	jmp mcu_halt
