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

j8bD163:
.db 0x0a,"done",0x00
j8bD149:
.db 0x0a,"b1=",0x00,0x00
j8bD150:
.db 0x0a,"b2=",0x00,0x00
j8bD151:
.db 0x0a,"if(b1==1)",0x00,0x00
j8bD153:
.db 0x0a,0xe2,0xcc,0xcf,0xcb," then(",0xc9,0xd3,0xd4,0xc9,0xce,0xc1,")",0x00,0x00
j8bD154:
.db 0x0a,0x09,0xd7,0xcc,0xcf,0xd6,0xc5,0xce,0xce,0xd9,0xca," if(b2==3)",0x00
j8bD156:
.db 0x0a,0x09,0xe2,0xcc,0xcf,0xcb," then(",0xc9,0xd3,0xd4,0xc9,0xce,0xc1,")",0x00
j8bD157:
.db 0x0a,0x09,0xe2,0xcc,0xcf,0xcb," else(",0xcc,0xcf,0xd6,0xd8,")",0x00
j8bD158:
.db 0x0a,0xe2,0xcc,0xcf,0xcb," else(",0xcc,0xcf,0xd6,0xd8,")",0x00,0x00
j8bD159:
.db 0x0a,0x09,0xd7,0xcc,0xcf,0xd6,0xc5,0xce,0xce,0xd9,0xca," if(b2==4)",0x00

Main:
	jmp j8b_CMainMmain
_j8b_meta_44:
	.db 22,0

j8b_CMainMmain:
	ldi r20,1
	ldi r21,2
	ldi r16,low(j8bD149*2)
	ldi r17,high(j8bD149*2)
	call os_out_cstr
	mov r16,r20
	call os_out_num8
	ldi r16,low(j8bD150*2)
	ldi r17,high(j8bD150*2)
	call os_out_cstr
	mov r16,r21
	call os_out_num8
	ldi r16,low(j8bD151*2)
	ldi r17,high(j8bD151*2)
	call os_out_cstr
	cpi r20,1
	brne _j8b_eoc_0
	ldi r16,low(j8bD153*2)
	ldi r17,high(j8bD153*2)
	call os_out_cstr
	ldi r16,low(j8bD154*2)
	ldi r17,high(j8bD154*2)
	call os_out_cstr
	cpi r21,3
	brne _j8b_eoc_1
	ldi r16,low(j8bD156*2)
	ldi r17,high(j8bD156*2)
	call os_out_cstr
	rjmp _j8b_eoc_155
_j8b_eoc_1:
	ldi r16,low(j8bD157*2)
	ldi r17,high(j8bD157*2)
	call os_out_cstr
_j8b_eoc_155:
	rjmp _j8b_eoc_152
_j8b_eoc_0:
	ldi r16,low(j8bD158*2)
	ldi r17,high(j8bD158*2)
	call os_out_cstr
	ldi r16,low(j8bD159*2)
	ldi r17,high(j8bD159*2)
	call os_out_cstr
	cpi r20,4
	brne _j8b_eoc_2
	ldi r16,low(j8bD156*2)
	ldi r17,high(j8bD156*2)
	call os_out_cstr
	rjmp _j8b_eoc_160
_j8b_eoc_2:
	ldi r16,low(j8bD157*2)
	ldi r17,high(j8bD157*2)
	call os_out_cstr
_j8b_eoc_160:
_j8b_eoc_152:
	ldi r16,low(j8bD163*2)
	ldi r17,high(j8bD163*2)
	call os_out_cstr
	jmp mcu_halt
