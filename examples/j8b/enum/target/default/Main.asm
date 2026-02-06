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
.include "stdio/out_num8.asm"
.include "stdio/out_cstr.asm"
.include "stdio/out_bool.asm"

j8bD142:
.db 0x0a,"Enum EDirection, size:",0x00
j8bD143:
.db 0x0a,"LEFT id:",0x00
j8bD144:
.db 0x0a,"RIGHT id:",0x00,0x00
j8bD145:
.db 0x0a,"DOWN id:",0x00
j8bD146:
.db 0x0a,"UP id:",0x00
j8bD147:
.db 0x0a,0x0a,"Enum EStatus, size:",0x00
j8bD148:
.db 0x0a,"OK id:",0x00
j8bD149:
.db 0x0a,"ERROR id:",0x00,0x00
j8bD150:
.db 0x0a,0x0a,"Index for variable dir:",0x00
j8bD151:
.db 0x0a,"Edirection.UP==dir:",0x00,0x00
j8bD152:
.db 0x0a,"Index for EDirection.UP:",0x00
j8bD153:
.db 0x0a,"Index for EStatus.item(EStatus.ERROR.index()):",0x00
j8bD154:
.db 0x0a,"Index for EStatus.item(1):",0x00
j8bD155:
.db 0x0a,0x0a,"done",0x00,0x00

Main:
	jmp j8b_CMainMmain
_j8b_meta_41:
	.db 22,0

j8b_CMainMmain:
	ldi r16,low(j8bD142*2)
	ldi r17,high(j8bD142*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,4
	call os_out_num8
	ldi r16,low(j8bD143*2)
	ldi r17,high(j8bD143*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,0
	call os_out_num8
	ldi r16,low(j8bD144*2)
	ldi r17,high(j8bD144*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,1
	call os_out_num8
	ldi r16,low(j8bD145*2)
	ldi r17,high(j8bD145*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,2
	call os_out_num8
	ldi r16,low(j8bD146*2)
	ldi r17,high(j8bD146*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,3
	call os_out_num8
	ldi r16,low(j8bD147*2)
	ldi r17,high(j8bD147*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,2
	call os_out_num8
	ldi r16,low(j8bD148*2)
	ldi r17,high(j8bD148*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,0
	call os_out_num8
	ldi r16,low(j8bD149*2)
	ldi r17,high(j8bD149*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,1
	call os_out_num8
	ldi r16,low(j8bD150*2)
	ldi r17,high(j8bD150*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,1
	call os_out_num8
	ldi r16,low(j8bD151*2)
	ldi r17,high(j8bD151*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,0
	call os_out_bool
	ldi r16,low(j8bD152*2)
	ldi r17,high(j8bD152*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,3
	call os_out_num8
	ldi r16,low(j8bD153*2)
	ldi r17,high(j8bD153*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,1
	call os_out_num8
	ldi r16,low(j8bD154*2)
	ldi r17,high(j8bD154*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,1
	call os_out_num8
	ldi r16,low(j8bD155*2)
	ldi r17,high(j8bD155*2)
	movw r30,r16
	call os_out_cstr
	jmp mcu_halt
