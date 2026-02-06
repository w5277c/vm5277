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
.include "math/mulq7n8.asm"
.include "math/divq7n8.asm"
.include "stdio/out_char.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_num16.asm"
.include "stdio/out_cstr.asm"
.include "stdio/out_bool.asm"
.include "stdio/out_num32.asm"
.include "stdio/out_q7n8.asm"

j8bD151:
.db 0x0a,"1. bool: ",0x00,0x00
j8bD152:
.db "True=",0x00
j8bD153:
.db ",False=",0x00
j8bD154:
.db ",!True=",0x00
j8bD155:
.db ",True&&False=",0x00
j8bD156:
.db ",True||False=",0x00
j8bD157:
.db 0x0a,"2. byte: ",0x00,0x00
j8bD158:
.db "Min[0]=",0x00
j8bD159:
.db ", Max[255]=",0x00
j8bD160:
.db ", Val[125]=",0x00
j8bD161:
.db ", Val+1[126]=",0x00
j8bD162:
.db ", Min-1[255]=",0x00
j8bD163:
.db ", Max+1[0]=",0x00
j8bD164:
.db 0x0a,"3. short: ",0x00
j8bD166:
.db ", Max[65535]=",0x00
j8bD167:
.db ", Val[32767]=",0x00
j8bD168:
.db ", Val+1[32768]=",0x00
j8bD169:
.db ", Min-1[65535]=",0x00
j8bD171:
.db 0x0a,"4. int: ",0x00
j8bD173:
.db ", Max[4294967295]=",0x00,0x00
j8bD174:
.db ", Val[2147483647]=",0x00,0x00
j8bD175:
.db ", Val+1[2147483648]=",0x00,0x00
j8bD176:
.db ", Min-1[4294967295]=",0x00,0x00
j8bD178:
.db 0x0a,"5. fixed: ",0x00
j8bD179:
.db "Min[-128.00]=",0x00
j8bD180:
.db ", Max[127.99]=",0x00,0x00
j8bD181:
.db ", Val[-1.56]=",0x00
j8bD182:
.db ", Val+1[-0.56]=",0x00
j8bD183:
.db ", Min-1[127.00]=",0x00,0x00
j8bD184:
.db ", Max+1[-127.00]=",0x00
j8bD185:
.db 0x0a,"6. ",0xf1,0xd7,0xce,0xd9,0xc5," ",0xd0,0xd2,0xc9,0xd7,0xc5,0xc4,0xc5,0xce,0xc9,0xd1,":",0x00
j8bD186:
.db "(byte)",0x00,0x00
j8bD187:
.db ", (short)",0x00
j8bD188:
.db ", (fixed)",0x00
j8bD189:
.db ", (byte)",0x00,0x00
j8bD192:
.db 0x0a,"7. ",0xf3,0xcd,0xc5,0xdb,0xc1,0xce,0xce,0xd9,0xc5," ",0xd7,0xd9,0xd2,0xc1,0xd6,0xc5,0xce,0xc9,0xd1,":",0x00,0x00
j8bD193:
.db "byte+short[32892]=",0x00,0x00
j8bD194:
.db ", short+int[2147516414]=",0x00,0x00
j8bD195:
.db ", byte+fixed[123.44]=",0x00
j8bD196:
.db 0x0a,"8. ",0xe1,0xd2,0xc9,0xc6,0xcd,0xc5,0xd4,0xc9,0xcb,0xc1," fixed:",0x00
j8bD197:
.db "fx1=",0x00,0x00
j8bD198:
.db ", fx2=",0x00,0x00
j8bD199:
.db ", fx1+fx2[14.25]=",0x00
j8bD200:
.db ", fx1-fx2[7.25]=",0x00,0x00
j8bD201:
.db ", fx1*fx2[37.63]=",0x00
j8bD202:
.db ", fx1/fx2[3.07]=",0x00,0x00
j8bD203:
.db 0x0a,"9. ",0xf3,0xd2,0xc1,0xd7,0xce,0xc5,0xce,0xc9,0xc5," ",0xd2,0xc1,0xda,0xce,0xd9,0xc8," ",0xd4,0xc9,0xd0,0xcf,0xd7,":",0x00
j8bD204:
.db "fx3=",0x00,0x00
j8bD205:
.db ", fx1>10=",0x00
j8bD206:
.db ", bVal==fx3=",0x00,0x00
j8bD207:
.db ", fx1<=-1=",0x00,0x00
j8bD208:
.db 0x0a,"10. ",0xf3,0xcc,0xcf,0xd6,0xce,0xcf,0xc5," ",0xd5,0xd3,0xcc,0xcf,0xd7,0xc9,0xc5,":",0x00
j8bD209:
.db "((bVal>100) && ((fVal+1)>0) || (sVal==32767) && !bFalse)[true]=",0x00
j8bD210:
.db 0x0a,"done",0x00

Main:
	jmp j8b_CMainMmain
_j8b_meta_41:
	.db 22,0

j8b_CMainMmain:
	push r28
	push r29
	lds r28,SPL
	lds r29,SPH
	ldi r16,low(12)
	ldi r17,high(12)
	push c0x00
	dec r16
	brne pc-0x02
	dec r17
	brne pc-0x04
	ldi r20,1
	ldi r21,0
	ldi r22,125
	ldi r24,255
	ldi r25,127
	mov j8b_atom,r30
	std y+0,c0xff
	subi yl,low(33)
	sbci yh,high(33)
	std y+32,c0xff
	std y+31,c0xff
	ldi r30,127
	std y+30,r30
	ldi r19,112
	std y+29,r19
	ldi r19,254
	std y+28,r19
	ldi r19,192
	std y+27,r19
	ldi r19,10
	std y+26,r19
	ldi r19,128
	std y+25,r19
	ldi r19,3
	std y+24,r19
	std y+23,c0x00
	ldi r19,125
	std y+22,r19
	ldi r16,low(j8bD151*2)
	ldi r17,high(j8bD151*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,low(j8bD152*2)
	ldi r17,high(j8bD152*2)
	movw r30,r16
	call os_out_cstr
	mov r16,r20
	call os_out_bool
	ldi r16,low(j8bD153*2)
	ldi r17,high(j8bD153*2)
	movw r30,r16
	call os_out_cstr
	mov r16,r21
	call os_out_bool
	ldi r16,low(j8bD154*2)
	ldi r17,high(j8bD154*2)
	movw r30,r16
	call os_out_cstr
	cpi r20,0
	brne _j8b_eoc_43
	ldi r16,1
	rjmp _j8b_eolb_44
_j8b_eoc_43:
	ldi r16,0
_j8b_eolb_44:
	call os_out_bool
	ldi r16,low(j8bD155*2)
	ldi r17,high(j8bD155*2)
	movw r30,r16
	call os_out_cstr
	cpi r20,0
	breq _j8b_eoc_45
	cpi r21,0
	breq _j8b_eoc_45
	ldi r16,1
	rjmp _j8b_eolb_46
_j8b_eoc_45:
	ldi r16,0
_j8b_eolb_46:
	call os_out_bool
	ldi r16,low(j8bD156*2)
	ldi r17,high(j8bD156*2)
	movw r30,r16
	call os_out_cstr
	cpi r20,0
	brne _j8b_eolb_48
	cpi r21,0
	breq _j8b_eoc_47
_j8b_eolb_48:
	ldi r16,1
	rjmp _j8b_eolb_49
_j8b_eoc_47:
	ldi r16,0
_j8b_eolb_49:
	call os_out_bool
	ldi r16,low(j8bD157*2)
	ldi r17,high(j8bD157*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,low(j8bD158*2)
	ldi r17,high(j8bD158*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,0
	call os_out_num8
	ldi r16,low(j8bD159*2)
	ldi r17,high(j8bD159*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,255
	call os_out_num8
	ldi r16,low(j8bD160*2)
	ldi r17,high(j8bD160*2)
	movw r30,r16
	call os_out_cstr
	mov r16,r22
	call os_out_num8
	ldi r16,low(j8bD161*2)
	ldi r17,high(j8bD161*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,1
	add r16,r22
	call os_out_num8
	ldi r16,low(j8bD162*2)
	ldi r17,high(j8bD162*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,255
	call os_out_num8
	ldi r16,low(j8bD163*2)
	ldi r17,high(j8bD163*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,0
	call os_out_num8
	ldi r16,low(j8bD164*2)
	ldi r17,high(j8bD164*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,low(j8bD158*2)
	ldi r17,high(j8bD158*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,0
	ldi r17,0
	call os_out_num16
	ldi r16,low(j8bD166*2)
	ldi r17,high(j8bD166*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,255
	ldi r17,255
	call os_out_num16
	ldi r16,low(j8bD167*2)
	ldi r17,high(j8bD167*2)
	movw r30,r16
	call os_out_cstr
	movw r16,r24
	call os_out_num16
	ldi r16,low(j8bD168*2)
	ldi r17,high(j8bD168*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,1
	ldi r17,0
	add r16,r24
	adc r17,r25
	call os_out_num16
	ldi r16,low(j8bD169*2)
	ldi r17,high(j8bD169*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,255
	ldi r17,255
	call os_out_num16
	ldi r16,low(j8bD163*2)
	ldi r17,high(j8bD163*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,0
	ldi r17,0
	call os_out_num16
	ldi r16,low(j8bD171*2)
	ldi r17,high(j8bD171*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,low(j8bD158*2)
	ldi r17,high(j8bD158*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,0
	ldi r17,0
	ldi r18,0
	ldi r19,0
	call os_out_num32
	ldi r16,low(j8bD173*2)
	ldi r17,high(j8bD173*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,255
	ldi r17,255
	ldi r18,255
	ldi r19,255
	call os_out_num32
	ldi r16,low(j8bD174*2)
	ldi r17,high(j8bD174*2)
	movw r30,r16
	call os_out_cstr
	ldd r16,y+33
	ldd r17,y+32
	ldd r18,y+31
	ldd r19,y+30
	call os_out_num32
	ldi r16,low(j8bD175*2)
	ldi r17,high(j8bD175*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,1
	ldi r17,0
	ldi r18,0
	ldi r19,0
	ldd j8b_atom,y+33
	add r16,j8b_atom
	ldd j8b_atom,y+32
	adc r17,j8b_atom
	ldd j8b_atom,y+31
	adc r18,j8b_atom
	ldd j8b_atom,y+30
	adc r19,j8b_atom
	call os_out_num32
	ldi r16,low(j8bD176*2)
	ldi r17,high(j8bD176*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,255
	ldi r17,255
	ldi r18,255
	ldi r19,255
	call os_out_num32
	ldi r16,low(j8bD163*2)
	ldi r17,high(j8bD163*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,0
	ldi r17,0
	ldi r18,0
	ldi r19,0
	call os_out_num32
	ldi r16,low(j8bD178*2)
	ldi r17,high(j8bD178*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,low(j8bD179*2)
	ldi r17,high(j8bD179*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,0
	ldi r17,128
	call os_out_q7n8
	ldi r16,low(j8bD180*2)
	ldi r17,high(j8bD180*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,255
	ldi r17,127
	call os_out_q7n8
	ldi r16,low(j8bD181*2)
	ldi r17,high(j8bD181*2)
	movw r30,r16
	call os_out_cstr
	ldd r16,y+29
	ldd r17,y+28
	call os_out_q7n8
	ldi r16,low(j8bD182*2)
	ldi r17,high(j8bD182*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,1
	ldi r17,0
	mov r17,r16
	ldi r16,0x00
	ldd j8b_atom,y+29
	add r16,j8b_atom
	ldd j8b_atom,y+28
	adc r17,j8b_atom
	call os_out_q7n8
	ldi r16,low(j8bD183*2)
	ldi r17,high(j8bD183*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,0
	ldi r17,127
	call os_out_q7n8
	ldi r16,low(j8bD184*2)
	ldi r17,high(j8bD184*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,255
	ldi r17,128
	call os_out_q7n8
	ldi r16,low(j8bD185*2)
	ldi r17,high(j8bD185*2)
	movw r30,r16
	call os_out_cstr
	movw r16,r24
	mov r23,r16
	ldi r16,low(j8bD186*2)
	ldi r17,high(j8bD186*2)
	movw r30,r16
	call os_out_cstr
	movw r16,r24
	call os_out_num16
	ldi r16,61
	call os_out_char
	mov r16,r23
	call os_out_num8
	ldi r16,low(j8bD187*2)
	ldi r17,high(j8bD187*2)
	movw r30,r16
	call os_out_cstr
	ldd r16,y+33
	ldd r17,y+32
	ldd r18,y+31
	ldd r19,y+30
	call os_out_num32
	ldi r16,61
	call os_out_char
	ldd r16,y+33
	ldd r17,y+32
	ldd r18,y+31
	ldd r19,y+30
	call os_out_num16
	ldi r16,low(j8bD188*2)
	ldi r17,high(j8bD188*2)
	movw r30,r16
	call os_out_cstr
	mov r16,r22
	call os_out_num8
	ldi r16,61
	call os_out_char
	mov r16,r22
	mov r17,r16
	ldi r16,0x00
	call os_out_q7n8
	ldi r16,low(j8bD189*2)
	ldi r17,high(j8bD189*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,168
	ldi r17,243
	call os_out_q7n8
	ldi r16,61
	call os_out_char
	ldi r16,243
	call os_out_num8
	ldi r16,low(j8bD189*2)
	ldi r17,high(j8bD189*2)
	movw r30,r16
	call os_out_cstr
	ldd r16,y+29
	ldd r17,y+28
	call os_out_q7n8
	ldi r16,61
	call os_out_char
	ldd r16,y+29
	ldd r17,y+28
	mov r16,r17
	ldi r17,0x00
	sbrc r16,0x07
	inc r16
	call os_out_num8
	ldi r16,low(j8bD189*2)
	ldi r17,high(j8bD189*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,128
	ldi r17,127
	call os_out_q7n8
	ldi r16,61
	call os_out_char
	ldi r16,127
	call os_out_num8
	ldi r16,low(j8bD192*2)
	ldi r17,high(j8bD192*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,low(j8bD193*2)
	ldi r17,high(j8bD193*2)
	movw r30,r16
	call os_out_cstr
	movw r16,r24
	add r16,r22
	adc r17,c0x00
	call os_out_num16
	ldi r16,low(j8bD194*2)
	ldi r17,high(j8bD194*2)
	movw r30,r16
	call os_out_cstr
	ldd r16,y+33
	ldd r17,y+32
	ldd r18,y+31
	ldd r19,y+30
	add r16,r24
	adc r17,r25
	adc r18,c0x00
	adc r19,c0x00
	call os_out_num32
	ldi r16,low(j8bD195*2)
	ldi r17,high(j8bD195*2)
	movw r30,r16
	call os_out_cstr
	ldd r16,y+29
	ldd r17,y+28
	add r17,r22
	call os_out_q7n8
	ldi r16,low(j8bD196*2)
	ldi r17,high(j8bD196*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,low(j8bD197*2)
	ldi r17,high(j8bD197*2)
	movw r30,r16
	call os_out_cstr
	ldd r16,y+27
	ldd r17,y+26
	call os_out_q7n8
	ldi r16,low(j8bD198*2)
	ldi r17,high(j8bD198*2)
	movw r30,r16
	call os_out_cstr
	ldd r16,y+25
	ldd r17,y+24
	call os_out_q7n8
	ldi r16,low(j8bD199*2)
	ldi r17,high(j8bD199*2)
	movw r30,r16
	call os_out_cstr
	ldd r16,y+25
	ldd r17,y+24
	ldd j8b_atom,y+27
	add r16,j8b_atom
	ldd j8b_atom,y+26
	adc r17,j8b_atom
	call os_out_q7n8
	ldi r16,low(j8bD200*2)
	ldi r17,high(j8bD200*2)
	movw r30,r16
	call os_out_cstr
	ldd r16,y+27
	ldd r17,y+26
	ldd j8b_atom,y+25
	sub r16,j8b_atom
	ldd j8b_atom,y+24
	sbc r17,j8b_atom
	call os_out_q7n8
	ldi r16,low(j8bD201*2)
	ldi r17,high(j8bD201*2)
	movw r30,r16
	call os_out_cstr
	ldd r16,y+25
	ldd r17,y+24
	ldd r18,y+27
	ldd r19,y+26
	call os_mulq7n8
	call os_out_q7n8
	ldi r16,low(j8bD202*2)
	ldi r17,high(j8bD202*2)
	movw r30,r16
	call os_out_cstr
	ldd r16,y+27
	ldd r17,y+26
	ldd r18,y+25
	ldd r19,y+24
	push r24
	push r25
	call os_divq7n8
	pop r25
	pop r24
	call os_out_q7n8
	ldi r16,low(j8bD203*2)
	ldi r17,high(j8bD203*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,low(j8bD204*2)
	ldi r17,high(j8bD204*2)
	movw r30,r16
	call os_out_cstr
	ldd r16,y+23
	ldd r17,y+22
	call os_out_q7n8
	ldi r16,low(j8bD205*2)
	ldi r17,high(j8bD205*2)
	movw r30,r16
	call os_out_cstr
	ldd r16,y+26
	cpi r16,10
	brcs _j8b_eoc_62
	brne _j8b_mcpe_63
	ldd r16,y+27
	cpi r16,0
	brcs _j8b_eoc_62
	breq _j8b_eoc_62
_j8b_mcpe_63:
	ldi r16,1
	rjmp _j8b_eolb_64
_j8b_eoc_62:
	ldi r16,0
_j8b_eolb_64:
	call os_out_bool
	ldi r16,low(j8bD206*2)
	ldi r17,high(j8bD206*2)
	movw r30,r16
	call os_out_cstr
	mov r16,r22
	mov r17,r16
	ldi r16,0x00
	ldd r19,y+22
	cp r17,r19
	brne _j8b_eoc_65
	ldd r19,y+23
	cp r16,r19
	brne _j8b_eoc_65
	ldi r16,1
	rjmp _j8b_eolb_67
_j8b_eoc_65:
	ldi r16,0
_j8b_eolb_67:
	call os_out_bool
	ldi r16,low(j8bD207*2)
	ldi r17,high(j8bD207*2)
	movw r30,r16
	call os_out_cstr
	ldd r16,y+26
	cpi r16,255
	breq pc+0x03
	brcc _j8b_eoc_68
	brne _j8b_mcpe_69
	ldd r16,y+27
	cpi r16,0
	breq pc+0x02
	brcc _j8b_eoc_68
_j8b_mcpe_69:
	ldi r16,1
	rjmp _j8b_eolb_70
_j8b_eoc_68:
	ldi r16,0
_j8b_eolb_70:
	call os_out_bool
	ldi r16,low(j8bD208*2)
	ldi r17,high(j8bD208*2)
	movw r30,r16
	call os_out_cstr
	ldi r16,low(j8bD209*2)
	ldi r17,high(j8bD209*2)
	movw r30,r16
	call os_out_cstr
	cpi r21,0
	brne _j8b_eoc_71
	cpi r22,100
	brcs _j8b_eolb_73
	breq _j8b_eolb_73
	ldi r16,1
	mov r17,r16
	ldi r16,0x00
	ldd j8b_atom,y+29
	add r16,j8b_atom
	ldd j8b_atom,y+28
	adc r17,j8b_atom
	cpi r17,0
	brcs _j8b_eolb_73
	brne _j8b_mcpe_74
	cpi r16,0
	brcs _j8b_eolb_73
	breq _j8b_eolb_73
_j8b_mcpe_74:
	rjmp _j8b_eolb_72
_j8b_eolb_73:
	cpi r25,127
	brne _j8b_eoc_71
	cpi r24,255
	brne _j8b_eoc_71
_j8b_eolb_72:
	ldi r16,1
	rjmp _j8b_eolb_77
_j8b_eoc_71:
	ldi r16,0
_j8b_eolb_77:
	call os_out_bool
	ldi r16,low(j8bD210*2)
	ldi r17,high(j8bD210*2)
	movw r30,r16
	call os_out_cstr
	jmp mcu_halt
