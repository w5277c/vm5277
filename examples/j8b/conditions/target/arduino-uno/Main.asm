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
.include "math/mul8.asm"
.include "math/div8.asm"
.include "stdio/out_cstr.asm"
.include "stdio/out_bool.asm"

j8bD147:
.db 0x0a,"1. ",0xe3,0xc5,0xd0,0xcf,0xde,0xcb,0xc1," AND ",0xd3," ",0xd2,0xc1,0xda,0xce,0xd9,0xcd,0xc9," ",0xd5,0xd3,0xcc,0xcf,0xd7,0xc9,0xd1,0xcd,0xc9,"[true]:",0x00,0x00
j8bD148:
.db 0x0a,"2. ",0xe3,0xc5,0xd0,0xcf,0xde,0xcb,0xc1," OR ",0xd3," ",0xd2,0xc1,0xda,0xce,0xd9,0xcd,0xc9," ",0xd5,0xd3,0xcc,0xcf,0xd7,0xc9,0xd1,0xcd,0xc9,"[true]:",0x00
j8bD149:
.db 0x0a,"3. ",0xeb,0xcf,0xcd,0xc2,0xc9,0xce,0xc1,0xc3,0xc9,0xd1," AND/OR ",0xd3," ",0xd0,0xd2,0xc9,0xcf,0xd2,0xc9,0xd4,0xc5,0xd4,0xc1,0xcd,0xc9,"[true]:",0x00
j8bD150:
.db 0x0a,"4. ",0xf7,0xcc,0xcf,0xd6,0xc5,0xce,0xce,0xd9,0xc5," ",0xd7,0xd9,0xd2,0xc1,0xd6,0xc5,0xce,0xc9,0xd1,"[true]:",0x00,0x00
j8bD151:
.db 0x0a,"5. ",0xf3," ",0xcf,0xd4,0xd2,0xc9,0xc3,0xc1,0xce,0xc9,0xd1,0xcd,0xc9,"[true]:",0x00,0x00
j8bD152:
.db 0x0a,"6. ",0xf3,0xd2,0xc1,0xd7,0xce,0xc5,0xce,0xc9,0xd1," ",0xd3," ",0xc1,0xd2,0xc9,0xc6,0xcd,0xc5,0xd4,0xc9,0xcb,0xcf,0xca,"[true]:",0x00,0x00
j8bD153:
.db 0x0a,"7. ",0xf3,0xcd,0xc5,0xdb,0xc1,0xce,0xce,0xd9,0xc5," ",0xd4,0xc9,0xd0,0xd9," ",0xd3,0xd2,0xc1,0xd7,0xce,0xc5,0xce,0xc9,0xca,"[true]:",0x00
j8bD154:
.db 0x0a,"8. ",0xf4,0xd2,0xcf,0xca,0xce,0xc1,0xd1," ",0xd7,0xcc,0xcf,0xd6,0xc5,0xce,0xce,0xcf,0xd3,0xd4,0xd8,"[true]:",0x00,0x00
j8bD155:
.db 0x0a,"9. ",0xeb,0xcf,0xd2,0xcf,0xd4,0xcb,0xcf,0xc5," ",0xda,0xc1,0xcd,0xd9,0xcb,0xc1,0xce,0xc9,0xc5," (",0xc4,0xc5,0xcc,0xc5,0xce,0xc9,0xc5," ",0xce,0xc1," ",0xce,0xcf,0xcc,0xd8," ",0xd7," false ",0xd7,0xc5,0xd4,0xcb,0xc5,")[false]:",0x00,0x00
j8bD156:
.db 0x0a,"10. ",0xed,0xce,0xcf,0xc7,0xcf,0xd5,0xd2,0xcf,0xd7,0xce,0xc5,0xd7,0xcf,0xc5," ",0xd5,0xd3,0xcc,0xcf,0xd7,0xc9,0xc5,"[true]:",0x00,0x00
j8bD157:
.db 0x0a,"11. ",0xe2,0xcf,0xcc,0xc5,0xc5," ",0xd3,0xcc,0xcf,0xd6,0xce,0xcf,0xc5," ",0xd5,0xd3,0xcc,0xcf,0xd7,0xc9,0xc5,"[true]:",0x00

Main:
	jmp j8b_CMainMmain
_j8b_meta_41:
	.db 22,0

j8b_CMainMmain:
	ldi r20,0
	ldi r21,1
	ldi r22,2
	ldi r23,3
	ldi r24,4
	ldi r25,5
	ldi r16,low(j8bD147*2)
	ldi r17,high(j8bD147*2)
	movw r30,r16
	call os_out_cstr
	cpi r21,1
	brne _j8b_eoc_43
	cpi r22,1
	breq _j8b_eoc_43
	cpi r23,4
	brcc _j8b_eoc_43
	cpi r24,3
	brcs _j8b_eoc_43
	breq _j8b_eoc_43
	cpi r25,5
	breq pc+0x02
	brcc _j8b_eoc_43
	ldi r16,1
	rjmp _j8b_eolb_44
_j8b_eoc_43:
	ldi r16,0
_j8b_eolb_44:
	call os_out_bool
	ldi r16,low(j8bD148*2)
	ldi r17,high(j8bD148*2)
	movw r30,r16
	call os_out_cstr
	cpi r21,0
	breq _j8b_eolb_46
	cpi r22,2
	breq _j8b_eolb_46
	cpi r23,5
	breq pc+0x02
	brcc _j8b_eolb_46
	cpi r24,4
	brne _j8b_eolb_46
	cpi r25,0
	brcc _j8b_eoc_45
_j8b_eolb_46:
	ldi r16,1
	rjmp _j8b_eolb_47
_j8b_eoc_45:
	ldi r16,0
_j8b_eolb_47:
	call os_out_bool
	ldi r16,low(j8bD149*2)
	ldi r17,high(j8bD149*2)
	movw r30,r16
	call os_out_cstr
	cpi r21,1
	brne _j8b_eoc_48
	cpi r22,2
	breq _j8b_eolb_49
	cpi r23,0
	brne _j8b_eoc_48
_j8b_eolb_49:
	cpi r24,4
	brne _j8b_eoc_48
	ldi r16,1
	rjmp _j8b_eolb_50
_j8b_eoc_48:
	ldi r16,0
_j8b_eolb_50:
	call os_out_bool
	ldi r16,low(j8bD150*2)
	ldi r17,high(j8bD150*2)
	movw r30,r16
	call os_out_cstr
	cpi r21,1
	brne _j8b_eolb_53
	cpi r22,2
	breq _j8b_eolb_52
_j8b_eolb_53:
	cpi r23,3
	brne _j8b_eoc_51
	cpi r24,4
	brne _j8b_eoc_51
_j8b_eolb_52:
	ldi r16,1
	rjmp _j8b_eolb_55
_j8b_eoc_51:
	ldi r16,0
_j8b_eolb_55:
	call os_out_bool
	ldi r16,low(j8bD151*2)
	ldi r17,high(j8bD151*2)
	movw r30,r16
	call os_out_cstr
	cpi r21,0
	breq _j8b_eoc_56
	cpi r22,2
	brne _j8b_eoc_56
	ldi r16,1
	rjmp _j8b_eolb_57
_j8b_eoc_56:
	ldi r16,0
_j8b_eolb_57:
	call os_out_bool
	ldi r16,low(j8bD152*2)
	ldi r17,high(j8bD152*2)
	movw r30,r16
	call os_out_cstr
	mov r16,r22
	add r16,r21
	cpi r16,3
	brne _j8b_eoc_58
	mov r16,r24
	mov r17,r23
	call os_mul8
	cpi r16,12
	brne _j8b_eoc_58
	ldi r16,1
	rjmp _j8b_eolb_59
_j8b_eoc_58:
	ldi r16,0
_j8b_eolb_59:
	call os_out_bool
	ldi r16,low(j8bD153*2)
	ldi r17,high(j8bD153*2)
	movw r30,r16
	call os_out_cstr
	mov r16,r21
	cp r16,r22
	brcc _j8b_eoc_60
	mov r16,r22
	cp r16,r23
	breq pc+0x02
	brcc _j8b_eoc_60
	mov r16,r23
	cp r16,r21
	brcs _j8b_eoc_60
	breq _j8b_eoc_60
	mov r16,r24
	cp r16,r22
	brcs _j8b_eoc_60
	ldi r16,1
	rjmp _j8b_eolb_61
_j8b_eoc_60:
	ldi r16,0
_j8b_eolb_61:
	call os_out_bool
	ldi r16,low(j8bD154*2)
	ldi r17,high(j8bD154*2)
	movw r30,r16
	call os_out_cstr
	cpi r21,1
	breq _j8b_eolb_63
	cpi r22,0
	brne _j8b_eoc_62
_j8b_eolb_63:
	cpi r23,3
	breq _j8b_eolb_64
	cpi r24,4
	brne _j8b_eoc_62
	cpi r25,5
	brne _j8b_eoc_62
_j8b_eolb_64:
	ldi r16,1
	rjmp _j8b_eolb_66
_j8b_eoc_62:
	ldi r16,0
_j8b_eolb_66:
	call os_out_bool
	ldi r16,low(j8bD155*2)
	ldi r17,high(j8bD155*2)
	movw r30,r16
	call os_out_cstr
	cpi r21,1
	breq _j8b_eoc_67
	mov r16,r22
	mov r17,r20
	push r24
	call os_div8
	pop r24
	cp r16,r20
	brne _j8b_eoc_67
	ldi r16,1
	rjmp _j8b_eolb_69
_j8b_eoc_67:
	ldi r16,0
_j8b_eolb_69:
	call os_out_bool
	ldi r16,low(j8bD156*2)
	ldi r17,high(j8bD156*2)
	movw r30,r16
	call os_out_cstr
	mov r16,r21
	cp r16,r22
	breq _j8b_eoc_70
	cpi r21,1
	brne _j8b_eoc_70
	cpi r22,2
	breq _j8b_eolb_71
	cpi r23,3
	brne _j8b_eoc_70
_j8b_eolb_71:
	cpi r24,4
	breq _j8b_eolb_72
	cpi r25,5
	brne _j8b_eoc_70
_j8b_eolb_72:
	ldi r16,1
	rjmp _j8b_eolb_73
_j8b_eoc_70:
	ldi r16,0
_j8b_eolb_73:
	call os_out_bool
	ldi r16,low(j8bD157*2)
	ldi r17,high(j8bD157*2)
	movw r30,r16
	call os_out_cstr
	cpi r25,1
	breq pc+0x02
	brcc _j8b_eolb_76
	cpi r21,0
	brne _j8b_eolb_75
	cpi r22,2
	brne _j8b_eoc_74
_j8b_eolb_75:
	mov r16,r23
	cp r16,r24
	brcs _j8b_eoc_74
_j8b_eolb_76:
	mov r16,r21
	cp r16,r22
	breq _j8b_eolb_77
	cpi r23,0
	breq _j8b_eoc_74
	cpi r24,0
	breq _j8b_eoc_74
_j8b_eolb_77:
	ldi r16,1
	rjmp _j8b_eolb_79
_j8b_eoc_74:
	ldi r16,0
_j8b_eolb_79:
	call os_out_bool
	jmp mcu_halt
