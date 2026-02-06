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
.set OS_FT_WELCOME = 1

.include "devices/atmega168p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "stdio/out_num8.asm"
.include "stdio/out_cstr.asm"

j8bD162:
.db 0x0a,0xf3,0xd2,0xc1,0xc2,0xcf,0xd4,0xc1,0xcc," 'case 0'",0x00,0x00
j8bD163:
.db 0x0a,0xf3,0xd2,0xc1,0xc2,0xcf,0xd4,0xc1,0xcc," 'case 1..10'",0x00,0x00
j8bD164:
.db 0x0a,0xf3,0xd2,0xc1,0xc2,0xcf,0xd4,0xc1,0xcc," 'case 15,20..25,16,17,30,32'",0x00,0x00
j8bD165:
.db 0x0a,0xf3,0xd2,0xc1,0xc2,0xcf,0xd4,0xc1,0xcc," 'default'",0x00
j8bD166:
.db 0x0a,"switch, b=",0x00
j8bD158:
.db 0x0a,0xf0,0xd2,0xc9,0xcd,0xc5,0xd2," switch, b=",0x00,0x00
j8bD190:
.db 0x0a,"done",0x00
j8bD159:
.db ": ",0x00,0x00

Main:
	jmp j8b_CMainMmain
_j8b_meta_41:
	.db 22,0

j8b_CMainMmain:
	ldi r20,0
	ldi r16,low(j8bD158*2)
	ldi r17,high(j8bD158*2)
	movw r30,r16
	call os_out_cstr
	mov r16,r20
	call os_out_num8
	ldi r16,low(j8bD159*2)
	ldi r17,high(j8bD159*2)
	movw r30,r16
	call os_out_cstr
	mov r16,r20
	cpi r16,0
	breq _j8b_eoc_59
	cpi r16,1
	brcs _j8b_eoc_62
	cpi r16,11
	brcs _j8b_eoc_60
_j8b_eoc_62:
	cpi r16,15
	brcs _j8b_eoc_63
	cpi r16,18
	brcs _j8b_eoc_61
_j8b_eoc_63:
	cpi r16,20
	brcs _j8b_eoc_64
	cpi r16,26
	brcs _j8b_eoc_61
_j8b_eoc_64:
	cpi r16,30
	breq _j8b_eoc_61
	cpi r16,32
	breq _j8b_eoc_61
	rjmp _j8b_casedef__161
_j8b_eoc_59:
	ldi r16,low(j8bD162*2)
	ldi r17,high(j8bD162*2)
	movw r30,r16
	call os_out_cstr
	rjmp _j8b_caseend_160
_j8b_eoc_60:
	ldi r16,low(j8bD163*2)
	ldi r17,high(j8bD163*2)
	movw r30,r16
	call os_out_cstr
	rjmp _j8b_caseend_160
_j8b_eoc_61:
	ldi r16,low(j8bD164*2)
	ldi r17,high(j8bD164*2)
	movw r30,r16
	call os_out_cstr
	rjmp _j8b_caseend_160
_j8b_casedef__161:
	ldi r16,low(j8bD165*2)
	ldi r17,high(j8bD165*2)
	movw r30,r16
	call os_out_cstr
_j8b_caseend_160:
	ldi r20,2
	ldi r16,low(j8bD166*2)
	ldi r17,high(j8bD166*2)
	movw r30,r16
	call os_out_cstr
	mov r16,r20
	call os_out_num8
	ldi r16,low(j8bD159*2)
	ldi r17,high(j8bD159*2)
	movw r30,r16
	call os_out_cstr
	mov r16,r20
	cpi r16,0
	breq _j8b_eoc_65
	cpi r16,1
	brcs _j8b_eoc_68
	cpi r16,11
	brcs _j8b_eoc_66
_j8b_eoc_68:
	cpi r16,15
	brcs _j8b_eoc_69
	cpi r16,18
	brcs _j8b_eoc_67
_j8b_eoc_69:
	cpi r16,20
	brcs _j8b_eoc_70
	cpi r16,26
	brcs _j8b_eoc_67
_j8b_eoc_70:
	cpi r16,30
	breq _j8b_eoc_67
	cpi r16,32
	breq _j8b_eoc_67
	rjmp _j8b_casedef__169
_j8b_eoc_65:
	ldi r16,low(j8bD162*2)
	ldi r17,high(j8bD162*2)
	movw r30,r16
	call os_out_cstr
	rjmp _j8b_caseend_168
_j8b_eoc_66:
	ldi r16,low(j8bD163*2)
	ldi r17,high(j8bD163*2)
	movw r30,r16
	call os_out_cstr
	rjmp _j8b_caseend_168
_j8b_eoc_67:
	ldi r16,low(j8bD164*2)
	ldi r17,high(j8bD164*2)
	movw r30,r16
	call os_out_cstr
	rjmp _j8b_caseend_168
_j8b_casedef__169:
	ldi r16,low(j8bD165*2)
	ldi r17,high(j8bD165*2)
	movw r30,r16
	call os_out_cstr
_j8b_caseend_168:
	ldi r20,17
	ldi r16,low(j8bD166*2)
	ldi r17,high(j8bD166*2)
	movw r30,r16
	call os_out_cstr
	mov r16,r20
	call os_out_num8
	ldi r16,low(j8bD159*2)
	ldi r17,high(j8bD159*2)
	movw r30,r16
	call os_out_cstr
	mov r16,r20
	cpi r16,0
	breq _j8b_eoc_71
	cpi r16,1
	brcs _j8b_eoc_74
	cpi r16,11
	brcs _j8b_eoc_72
_j8b_eoc_74:
	cpi r16,15
	brcs _j8b_eoc_75
	cpi r16,18
	brcs _j8b_eoc_73
_j8b_eoc_75:
	cpi r16,20
	brcs _j8b_eoc_76
	cpi r16,26
	brcs _j8b_eoc_73
_j8b_eoc_76:
	cpi r16,30
	breq _j8b_eoc_73
	cpi r16,32
	breq _j8b_eoc_73
	rjmp _j8b_casedef__177
_j8b_eoc_71:
	ldi r16,low(j8bD162*2)
	ldi r17,high(j8bD162*2)
	movw r30,r16
	call os_out_cstr
	rjmp _j8b_caseend_176
_j8b_eoc_72:
	ldi r16,low(j8bD163*2)
	ldi r17,high(j8bD163*2)
	movw r30,r16
	call os_out_cstr
	rjmp _j8b_caseend_176
_j8b_eoc_73:
	ldi r16,low(j8bD164*2)
	ldi r17,high(j8bD164*2)
	movw r30,r16
	call os_out_cstr
	rjmp _j8b_caseend_176
_j8b_casedef__177:
	ldi r16,low(j8bD165*2)
	ldi r17,high(j8bD165*2)
	movw r30,r16
	call os_out_cstr
_j8b_caseend_176:
	ldi r20,18
	ldi r16,low(j8bD166*2)
	ldi r17,high(j8bD166*2)
	movw r30,r16
	call os_out_cstr
	mov r16,r20
	call os_out_num8
	ldi r16,low(j8bD159*2)
	ldi r17,high(j8bD159*2)
	movw r30,r16
	call os_out_cstr
	mov r16,r20
	cpi r16,0
	breq _j8b_eoc_77
	cpi r16,1
	brcs _j8b_eoc_80
	cpi r16,11
	brcs _j8b_eoc_78
_j8b_eoc_80:
	cpi r16,15
	brcs _j8b_eoc_81
	cpi r16,18
	brcs _j8b_eoc_79
_j8b_eoc_81:
	cpi r16,20
	brcs _j8b_eoc_82
	cpi r16,26
	brcs _j8b_eoc_79
_j8b_eoc_82:
	cpi r16,30
	breq _j8b_eoc_79
	cpi r16,32
	breq _j8b_eoc_79
	rjmp _j8b_casedef__185
_j8b_eoc_77:
	ldi r16,low(j8bD162*2)
	ldi r17,high(j8bD162*2)
	movw r30,r16
	call os_out_cstr
	rjmp _j8b_caseend_184
_j8b_eoc_78:
	ldi r16,low(j8bD163*2)
	ldi r17,high(j8bD163*2)
	movw r30,r16
	call os_out_cstr
	rjmp _j8b_caseend_184
_j8b_eoc_79:
	ldi r16,low(j8bD164*2)
	ldi r17,high(j8bD164*2)
	movw r30,r16
	call os_out_cstr
	rjmp _j8b_caseend_184
_j8b_casedef__185:
	ldi r16,low(j8bD165*2)
	ldi r17,high(j8bD165*2)
	movw r30,r16
	call os_out_cstr
_j8b_caseend_184:
	ldi r16,low(j8bD190*2)
	ldi r17,high(j8bD190*2)
	movw r30,r16
	call os_out_cstr
	jmp mcu_halt
