; vm5277.AVR v0.3
.equ CORE_FREQ = 16
.set STDIO_PORT_REGID = 8
.set STDIO_DDR_REGID = 7
.set STDIO_PIN_REGID = 6
.set STDIO_PORTNUM = 2
.set STDIO_PINNUM = 4

.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
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
