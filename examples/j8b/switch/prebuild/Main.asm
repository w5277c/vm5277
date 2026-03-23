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

j8bD176:
.db 0x0a,0xf3,0xd2,0xc1,0xc2,0xcf,0xd4,0xc1,0xcc," 'case 15,20..25,16,17,30,32'",0x00,0x00
j8bD177:
.db 0x0a,0xf3,0xd2,0xc1,0xc2,0xcf,0xd4,0xc1,0xcc," 'default'",0x00
j8bD178:
.db 0x0a,"switch, b=",0x00
j8bD170:
.db 0x0a,0xf0,0xd2,0xc9,0xcd,0xc5,0xd2," switch, b=",0x00,0x00
j8bD202:
.db 0x0a,"done",0x00
j8bD171:
.db ": ",0x00,0x00
j8bD174:
.db 0x0a,0xf3,0xd2,0xc1,0xc2,0xcf,0xd4,0xc1,0xcc," 'case 0'",0x00,0x00
j8bD175:
.db 0x0a,0xf3,0xd2,0xc1,0xc2,0xcf,0xd4,0xc1,0xcc," 'case 1..10'",0x00,0x00

Main:
	jmp j8b_CMainMmain
_j8b_meta_41:
	.db 22,0

j8b_CMainMmain:
	ldi r20,0
	ldi r16,low(j8bD170*2)
	ldi r17,high(j8bD170*2)
	call os_out_cstr
	mov r16,r20
	call os_out_num8
	ldi r16,low(j8bD171*2)
	ldi r17,high(j8bD171*2)
	call os_out_cstr
	mov r16,r20
	cpi r16,0
	brne _j8b_eoc_60
	ldi r16,low(j8bD174*2)
	ldi r17,high(j8bD174*2)
	call os_out_cstr
	rjmp _j8b_switchend_172
_j8b_eoc_60:
	cpi r16,1
	brcs _j8b_eoc_64
	cpi r16,11
	brcs _j8b_eoc_63
_j8b_eoc_64:
	rjmp _j8b_eoc_61
_j8b_eoc_63:
	ldi r16,low(j8bD175*2)
	ldi r17,high(j8bD175*2)
	call os_out_cstr
	rjmp _j8b_switchend_172
_j8b_eoc_61:
	cpi r16,15
	brcs _j8b_eoc_66
	cpi r16,18
	brcs _j8b_eoc_65
_j8b_eoc_66:
	cpi r16,20
	brcs _j8b_eoc_67
	cpi r16,26
	brcs _j8b_eoc_65
_j8b_eoc_67:
	cpi r16,30
	breq _j8b_eoc_65
	cpi r16,32
	brne _j8b_casedef__173
_j8b_eoc_65:
	ldi r16,low(j8bD176*2)
	ldi r17,high(j8bD176*2)
	call os_out_cstr
	rjmp _j8b_switchend_172
_j8b_casedef__173:
	ldi r16,low(j8bD177*2)
	ldi r17,high(j8bD177*2)
	call os_out_cstr
_j8b_switchend_172:
	ldi r20,2
	ldi r16,low(j8bD178*2)
	ldi r17,high(j8bD178*2)
	call os_out_cstr
	mov r16,r20
	call os_out_num8
	ldi r16,low(j8bD171*2)
	ldi r17,high(j8bD171*2)
	call os_out_cstr
	mov r16,r20
	cpi r16,0
	brne _j8b_eoc_69
	ldi r16,low(j8bD174*2)
	ldi r17,high(j8bD174*2)
	call os_out_cstr
	rjmp _j8b_switchend_180
_j8b_eoc_69:
	cpi r16,1
	brcs _j8b_eoc_73
	cpi r16,11
	brcs _j8b_eoc_72
_j8b_eoc_73:
	rjmp _j8b_eoc_70
_j8b_eoc_72:
	ldi r16,low(j8bD175*2)
	ldi r17,high(j8bD175*2)
	call os_out_cstr
	rjmp _j8b_switchend_180
_j8b_eoc_70:
	cpi r16,15
	brcs _j8b_eoc_75
	cpi r16,18
	brcs _j8b_eoc_74
_j8b_eoc_75:
	cpi r16,20
	brcs _j8b_eoc_76
	cpi r16,26
	brcs _j8b_eoc_74
_j8b_eoc_76:
	cpi r16,30
	breq _j8b_eoc_74
	cpi r16,32
	brne _j8b_casedef__181
_j8b_eoc_74:
	ldi r16,low(j8bD176*2)
	ldi r17,high(j8bD176*2)
	call os_out_cstr
	rjmp _j8b_switchend_180
_j8b_casedef__181:
	ldi r16,low(j8bD177*2)
	ldi r17,high(j8bD177*2)
	call os_out_cstr
_j8b_switchend_180:
	ldi r20,17
	ldi r16,low(j8bD178*2)
	ldi r17,high(j8bD178*2)
	call os_out_cstr
	mov r16,r20
	call os_out_num8
	ldi r16,low(j8bD171*2)
	ldi r17,high(j8bD171*2)
	call os_out_cstr
	mov r16,r20
	cpi r16,0
	brne _j8b_eoc_78
	ldi r16,low(j8bD174*2)
	ldi r17,high(j8bD174*2)
	call os_out_cstr
	rjmp _j8b_switchend_188
_j8b_eoc_78:
	cpi r16,1
	brcs _j8b_eoc_82
	cpi r16,11
	brcs _j8b_eoc_81
_j8b_eoc_82:
	rjmp _j8b_eoc_79
_j8b_eoc_81:
	ldi r16,low(j8bD175*2)
	ldi r17,high(j8bD175*2)
	call os_out_cstr
	rjmp _j8b_switchend_188
_j8b_eoc_79:
	cpi r16,15
	brcs _j8b_eoc_84
	cpi r16,18
	brcs _j8b_eoc_83
_j8b_eoc_84:
	cpi r16,20
	brcs _j8b_eoc_85
	cpi r16,26
	brcs _j8b_eoc_83
_j8b_eoc_85:
	cpi r16,30
	breq _j8b_eoc_83
	cpi r16,32
	brne _j8b_casedef__189
_j8b_eoc_83:
	ldi r16,low(j8bD176*2)
	ldi r17,high(j8bD176*2)
	call os_out_cstr
	rjmp _j8b_switchend_188
_j8b_casedef__189:
	ldi r16,low(j8bD177*2)
	ldi r17,high(j8bD177*2)
	call os_out_cstr
_j8b_switchend_188:
	ldi r20,18
	ldi r16,low(j8bD178*2)
	ldi r17,high(j8bD178*2)
	call os_out_cstr
	mov r16,r20
	call os_out_num8
	ldi r16,low(j8bD171*2)
	ldi r17,high(j8bD171*2)
	call os_out_cstr
	mov r16,r20
	cpi r16,0
	brne _j8b_eoc_87
	ldi r16,low(j8bD174*2)
	ldi r17,high(j8bD174*2)
	call os_out_cstr
	rjmp _j8b_switchend_196
_j8b_eoc_87:
	cpi r16,1
	brcs _j8b_eoc_91
	cpi r16,11
	brcs _j8b_eoc_90
_j8b_eoc_91:
	rjmp _j8b_eoc_88
_j8b_eoc_90:
	ldi r16,low(j8bD175*2)
	ldi r17,high(j8bD175*2)
	call os_out_cstr
	rjmp _j8b_switchend_196
_j8b_eoc_88:
	cpi r16,15
	brcs _j8b_eoc_93
	cpi r16,18
	brcs _j8b_eoc_92
_j8b_eoc_93:
	cpi r16,20
	brcs _j8b_eoc_94
	cpi r16,26
	brcs _j8b_eoc_92
_j8b_eoc_94:
	cpi r16,30
	breq _j8b_eoc_92
	cpi r16,32
	brne _j8b_casedef__197
_j8b_eoc_92:
	ldi r16,low(j8bD176*2)
	ldi r17,high(j8bD176*2)
	call os_out_cstr
	rjmp _j8b_switchend_196
_j8b_casedef__197:
	ldi r16,low(j8bD177*2)
	ldi r17,high(j8bD177*2)
	call os_out_cstr
_j8b_switchend_196:
	ldi r16,low(j8bD202*2)
	ldi r17,high(j8bD202*2)
	call os_out_cstr
	jmp mcu_halt
