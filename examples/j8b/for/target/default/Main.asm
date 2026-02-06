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
.include "stdio/out_cstr.asm"

j8bD176:
.db 0x0a,0xe3,0xc9,0xcb,0xcc," for(byte i=0; i<10; i++), ",0xd3," ",0xc2,0xcc,0xcf,0xcb,0xcf,0xcd," else ",0xc9," break ",0xce,0xc1," 3: ",0x00,0x00
j8bD178:
.db "else ",0xc2,0xcc,0xcf,0xcb,": i=",0x00
j8bD179:
.db 0x0a,0xe3,0xc9,0xcb,0xcc," for(byte i=7; i<10; i++), ",0xd3," ",0xc2,0xcc,0xcf,0xcb,0xcf,0xcd," else ",0xc2,0xc5,0xda," break: ",0x00
j8bD181:
.db 0x0a,0xe3,0xc9,0xcb,0xcc," for(; i<10; i++), i-",0xd7,0xce,0xc5,0xdb,0xce,0xd1,0xd1," ",0xd0,0xc5,0xd2,0xc5,0xcd,0xc5,0xce,0xce,0xc1,0xd1," = 3:",0x00
j8bD182:
.db 0x0a,0xe3,0xc9,0xcb,0xcc," for(i=0; i<10; i++), i-",0xd7,0xce,0xc5,0xdb,0xce,0xd1,0xd1," ",0xd0,0xc5,0xd2,0xc5,0xcd,0xc5,0xce,0xce,0xc1,0xd1,": ",0x00
j8bD183:
.db 0x0a,0xe3,0xc9,0xcb,0xcc," for(byte i=0; i<10;), ",0xc2,0xc5,0xda," ",0xc9,0xce,0xcb,0xd2,0xc5,0xcd,0xc5,0xce,0xd4,0xc1,", ",0xd3," ",0xd7,0xd9,0xc8,0xcf,0xc4,0xcf,0xcd," ",0xd0,0xcf," break: ",0x00,0x00
j8bD185:
.db 0x0a,0xe3,0xc9,0xcb,0xcc," for(byte i=0; ; i++), ",0xc2,0xc5,0xda," ",0xd5,0xd3,0xcc,0xcf,0xd7,0xc9,0xd1,", ",0xd3," ",0xd7,0xd9,0xc8,0xcf,0xc4,0xcf,0xcd," ",0xd0,0xcf," break:",0x00,0x00
j8bD187:
.db 0x0a,0xe3,0xc9,0xcb,0xcc," for(byte i=0; i<10; i++), ",0xd3," continue ",0xd0,0xc5,0xd2,0xc5,0xc4," ",0xd7,0xd9,0xd7,0xcf,0xc4,0xcf,0xcd,", ",0xc5,0xd3,0xcc,0xc9," i<5: ",0x00,0x00
j8bD189:
.db 0x0a,0xe3,0xc9,0xcb,0xcc," for(byte i=0; i<5; i++), ",0xd3," ",0xd7,0xcc,0xcf,0xd6,0xc5,0xce,0xce,0xd9,0xcd," for(byte j=0; j<3; j++) ",0xd3," ",0xd7,0xd9,0xc8,0xcf,0xc4,0xcf,0xcd," ",0xc5,0xd3,0xcc,0xc9," i=2 ",0xc9," j=1: ",0x00
j8bD191:
.db 0x0a,"done",0x00

Main:
	jmp j8b_CMainMmain
_j8b_meta_55:
	.db 22,0

j8b_CMainMmain:
	ldi r16,low(j8bD176*2)
	ldi r17,high(j8bD176*2)
	movw r30,r16
	call os_out_cstr
	ldi r20,0
_j8b_loop_58:
	cpi r20,10
	brcc _j8b_eoc_0
	cpi r20,3
	breq _j8b_eol_60
	mov r16,r20
	call os_out_num8
	ldi r16,32
	call os_out_char
	add r20,c0x01
	rjmp _j8b_loop_58
_j8b_eoc_0:
	ldi r16,low(j8bD178*2)
	ldi r17,high(j8bD178*2)
	movw r30,r16
	call os_out_cstr
	mov r16,r20
	call os_out_num8
_j8b_eol_60:
	ldi r16,low(j8bD179*2)
	ldi r17,high(j8bD179*2)
	movw r30,r16
	call os_out_cstr
	ldi r20,7
_j8b_loop_65:
	cpi r20,10
	brcc _j8b_eoc_2
	mov r16,r20
	call os_out_num8
	ldi r16,32
	call os_out_char
	add r20,c0x01
	rjmp _j8b_loop_65
_j8b_eoc_2:
	ldi r16,low(j8bD178*2)
	ldi r17,high(j8bD178*2)
	movw r30,r16
	call os_out_cstr
	mov r16,r20
	call os_out_num8
	ldi r20,3
	ldi r16,low(j8bD181*2)
	ldi r17,high(j8bD181*2)
	movw r30,r16
	call os_out_cstr
_j8b_loop_71:
	cpi r20,10
	brcc _j8b_eoc_3
	mov r16,r20
	call os_out_num8
	ldi r16,32
	call os_out_char
	add r20,c0x01
	rjmp _j8b_loop_71
_j8b_eoc_3:
	ldi r16,low(j8bD182*2)
	ldi r17,high(j8bD182*2)
	movw r30,r16
	call os_out_cstr
	ldi r20,0
_j8b_loop_76:
	cpi r20,10
	brcc _j8b_eoc_4
	mov r16,r20
	call os_out_num8
	ldi r16,32
	call os_out_char
	add r20,c0x01
	rjmp _j8b_loop_76
_j8b_eoc_4:
	ldi r22,0
	ldi r16,low(j8bD183*2)
	ldi r17,high(j8bD183*2)
	movw r30,r16
	call os_out_cstr
	ldi r21,0
_j8b_loop_81:
	ldi r16,0
	call os_out_num8
	ldi r16,32
	call os_out_char
	add r22,c0x01
	cpi r22,20
	brne _j8b_loop_81
	ldi r22,0
	ldi r16,low(j8bD185*2)
	ldi r17,high(j8bD185*2)
	movw r30,r16
	call os_out_cstr
	ldi r21,0
_j8b_loop_87:
	mov r16,r21
	call os_out_num8
	ldi r16,32
	call os_out_char
	add r22,c0x01
	cpi r22,20
	breq _j8b_eol_89
	add r21,c0x01
	rjmp _j8b_loop_87
_j8b_eol_89:
	ldi r16,low(j8bD187*2)
	ldi r17,high(j8bD187*2)
	movw r30,r16
	call os_out_cstr
	ldi r21,0
_j8b_loop_93:
	cpi r21,10
	brcc _j8b_eoc_9
	cpi r21,5
	brcs _j8b_next_94
	mov r16,r21
	call os_out_num8
	ldi r16,32
	call os_out_char
_j8b_next_94:
	add r21,c0x01
	rjmp _j8b_loop_93
_j8b_eoc_9:
	ldi r16,low(j8bD189*2)
	ldi r17,high(j8bD189*2)
	movw r30,r16
	call os_out_cstr
	ldi r21,0
_j8b_loop_99:
	cpi r21,5
	brcc _j8b_eoc_11
	ldi r23,0
_j8b_loop_104:
	cpi r23,3
	brcc _j8b_eoc_12
	mov r16,r21
	call os_out_num8
	ldi r16,46
	call os_out_char
	mov r16,r23
	call os_out_num8
	ldi r16,32
	call os_out_char
	cpi r21,2
	brne _j8b_eoc_13
	cpi r23,1
	breq _j8b_eol_101
_j8b_eoc_13:
	add r23,c0x01
	rjmp _j8b_loop_104
_j8b_eoc_12:
	add r21,c0x01
	rjmp _j8b_loop_99
_j8b_eoc_11:
_j8b_eol_101:
	ldi r16,low(j8bD191*2)
	ldi r17,high(j8bD191*2)
	movw r30,r16
	call os_out_cstr
	jmp mcu_halt
