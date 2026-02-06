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
.set OS_ETRACE_POINT_BITSIZE = 7

.set OS_FT_STDOUT = 1
.set OS_FT_ETRACE = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "j8b/etrace_add.asm"
.include "math/mul16p2.asm"
.include "math/mul16.asm"
.include "math/mulq7n8.asm"
.include "math/mul32.asm"
.include "math/div16p2.asm"
.include "math/div16.asm"
.include "math/divq7n8.asm"
.include "stdio/out_num16.asm"
.include "stdio/out_cstr.asm"
.include "stdio/out_q7n8.asm"
.include "stdio/out_num32.asm"

j8bD160:
.db 0x0a,"s1/1.5=",0x00,0x00
j8bD161:
.db 0x0a,"f1/1.5=",0x00,0x00
j8bD162:
.db 0x0a,"s1%2=",0x00,0x00
j8bD163:
.db 0x0a,"f1+f2=",0x00
j8bD164:
.db 0x0a,"f1-f2=",0x00
j8bD165:
.db 0x0a,"f1*f2=",0x00
j8bD166:
.db 0x0a,"f1/f2=",0x00
j8bD167:
.db 0x0a,"s1*1000000000=",0x00
j8bD171:
.db "Math overflow exception",0x00
j8bD172:
.db 0x0a,"done",0x00
j8bD146:
.db 0x0a,"s1=",0x00,0x00
j8bD147:
.db 0x0a,"f1=",0x00,0x00
j8bD148:
.db 0x0a,"f2=",0x00,0x00
j8bD149:
.db 0x0a,0x00
j8bD150:
.db 0x0a,"s1+2=",0x00,0x00
j8bD151:
.db 0x0a,"s1+1.5=",0x00,0x00
j8bD152:
.db 0x0a,"f1+1.5=",0x00,0x00
j8bD153:
.db 0x0a,"s1-2=",0x00,0x00
j8bD154:
.db 0x0a,"s1-1.5=",0x00,0x00
j8bD155:
.db 0x0a,"f1-1.5=",0x00,0x00
j8bD156:
.db 0x0a,"s1*8=",0x00,0x00
j8bD157:
.db 0x0a,"s1*1.5=",0x00,0x00
j8bD158:
.db 0x0a,"f1*1.5=",0x00,0x00
j8bD159:
.db 0x0a,"s1/8=",0x00,0x00

Main:
	jmp j8b_CMainMmain
_j8b_meta_41:
	.db 22,0

j8b_CMainMmain:
	ldi r20,10
	ldi r21,0
	ldi r22,0
	ldi r23,6
	ldi r24,128
	ldi r25,1
	ldi r16,low(j8bD146*2)
	ldi r17,high(j8bD146*2)
	movw r30,r16
	call os_out_cstr
	movw r16,r20
	call os_out_num16
	ldi r16,low(j8bD147*2)
	ldi r17,high(j8bD147*2)
	movw r30,r16
	call os_out_cstr
	movw r16,r22
	call os_out_q7n8
	ldi r16,low(j8bD148*2)
	ldi r17,high(j8bD148*2)
	movw r30,r16
	call os_out_cstr
	movw r16,r24
	call os_out_q7n8
	ldi r16,low(j8bD149*2)
	ldi r17,high(j8bD149*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,low(j8bD150*2)
	ldi r17,high(j8bD150*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,2
	ldi r17,0
	add r16,r20
	adc r17,r21
	call os_out_num16
	ldi r16,low(j8bD151*2)
	ldi r17,high(j8bD151*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,128
	ldi r17,1
	add r17,r21
	call os_out_q7n8
	ldi r16,low(j8bD152*2)
	ldi r17,high(j8bD152*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,128
	ldi r17,1
	add r16,r22
	adc r17,r23
	call os_out_q7n8
	ldi r16,low(j8bD153*2)
	ldi r17,high(j8bD153*2)
	movw r30,r16
	call os_out_cstr
	movw r16,r20
	subi r16,2
	sbci r17,0
	call os_out_num16
	ldi r16,low(j8bD154*2)
	ldi r17,high(j8bD154*2)
	movw r30,r16
	call os_out_cstr
	movw r16,r20
	mov r17,r16
	ldi r16,0x00
	subi r16,128
	sbci r17,1
	call os_out_q7n8
	ldi r16,low(j8bD155*2)
	ldi r17,high(j8bD155*2)
	movw r30,r16
	call os_out_cstr
	movw r16,r22
	subi r16,128
	sbci r17,1
	call os_out_q7n8
	ldi r16,low(j8bD156*2)
	ldi r17,high(j8bD156*2)
	movw r30,r16
	call os_out_cstr
	movw r16,r20
	ldi r18,0x00
	call os_mul16p2_x8
	call os_out_num16
	ldi r16,low(j8bD157*2)
	ldi r17,high(j8bD157*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,128
	ldi r17,1
	movw r18,r20
	call os_mul16
	call os_out_q7n8
	ldi r16,low(j8bD158*2)
	ldi r17,high(j8bD158*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,128
	ldi r17,1
	movw r18,r22
	call os_mulq7n8
	call os_out_q7n8
	ldi r16,low(j8bD159*2)
	ldi r17,high(j8bD159*2)
	movw r30,r16
	call os_out_cstr
	movw r16,r20
	call os_div16p2_x8
	call os_out_num16
	ldi r16,low(j8bD160*2)
	ldi r17,high(j8bD160*2)
	movw r30,r16
	call os_out_cstr
	movw r16,r20
	mov r17,r16
	ldi r16,0x00
	push r24
	push r25
	ldi r18,128
	ldi r19,1
	call os_divq7n8
	pop r25
	pop r24
	call os_out_q7n8
	ldi r16,low(j8bD161*2)
	ldi r17,high(j8bD161*2)
	movw r30,r16
	call os_out_cstr
	movw r16,r22
	push r24
	push r25
	ldi r18,128
	ldi r19,1
	call os_divq7n8
	pop r25
	pop r24
	call os_out_q7n8
	ldi r16,low(j8bD162*2)
	ldi r17,high(j8bD162*2)
	movw r30,r16
	call os_out_cstr
	movw r16,r20
	push r24
	push r25
	ldi r18,2
	ldi r19,0
	call os_div16
	movw r16,r24
	pop r25
	pop r24
	call os_out_num16
	ldi r16,low(j8bD163*2)
	ldi r17,high(j8bD163*2)
	movw r30,r16
	call os_out_cstr
	movw r16,r24
	add r16,r22
	adc r17,r23
	call os_out_q7n8
	ldi r16,low(j8bD164*2)
	ldi r17,high(j8bD164*2)
	movw r30,r16
	call os_out_cstr
	movw r16,r22
	sub r16,r24
	sbc r17,r25
	call os_out_q7n8
	ldi r16,low(j8bD165*2)
	ldi r17,high(j8bD165*2)
	movw r30,r16
	call os_out_cstr
	movw r16,r24
	movw r18,r22
	call os_mulq7n8
	call os_out_q7n8
	ldi r16,low(j8bD166*2)
	ldi r17,high(j8bD166*2)
	movw r30,r16
	call os_out_cstr
	movw r16,r22
	movw r18,r24
	push r24
	push r25
	call os_divq7n8
	pop r25
	pop r24
	call os_out_q7n8
	ldi r16,low(j8bD167*2)
	ldi r17,high(j8bD167*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,0
	ldi r17,202
	ldi r18,154
	ldi r19,59
	push r24
	push r25
	push r22
	push r23
	movw r24,r20
	ldi r22,0x00
	ldi r23,0x00
	call os_mul32_nr
	pop r23
	pop r22
	pop r25
	pop r24
	brcc _j8b_throwskip_168
	ldi r16,6
	ldi r17,0x00
	ldi r18,1
	call j8bproc_etrace_addfirst
	rjmp _j8b_catch_45
_j8b_throwskip_168:
	call os_out_num32
	rjmp _j8b_catchskip_169
_j8b_catch_45:
	clt
	ldi r16,low(j8bD171*2)
	ldi r17,high(j8bD171*2)
	movw r30,r16
	call os_out_cstr
_j8b_catchskip_169:
	ldi r16,low(j8bD172*2)
	ldi r17,high(j8bD172*2)
	movw r30,r16
	call os_out_cstr
	jmp mcu_halt
