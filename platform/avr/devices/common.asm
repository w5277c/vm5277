;-----------------------------------------------------------------------------------------------------------------------
;Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
;-----------------------------------------------------------------------------------------------------------------------
;30.05.2025	konstantin@5277.ru		Взят с проекта core5277
;-----------------------------------------------------------------------------------------------------------------------
	.EQU	TIMERS_SPEED							= (3125*CORE_FREQ*2)/1000

	.EQU	PINx									= 0x00
	.EQU	DDRx									= 0x01
	.EQU	PORTx									= 0x02

	.EQU	PA0										= (0x00<<4)|0x00
	.EQU	PA1										= (0x00<<4)|0x01
	.EQU	PA2										= (0x00<<4)|0x02
	.EQU	PA3										= (0x00<<4)|0x03
	.EQU	PA4										= (0x00<<4)|0x04
	.EQU	PA5										= (0x00<<4)|0x05
	.EQU	PA6										= (0x00<<4)|0x06
	.EQU	PA7										= (0x00<<4)|0x07
	.EQU	PB0										= (0x01<<4)|0x00
	.EQU	PB1										= (0x01<<4)|0x01
	.EQU	PB2										= (0x01<<4)|0x02
	.EQU	PB3										= (0x01<<4)|0x03
	.EQU	PB4										= (0x01<<4)|0x04
	.EQU	PB5										= (0x01<<4)|0x05
	.EQU	PB6										= (0x01<<4)|0x06
	.EQU	PB7										= (0x01<<4)|0x07
	.EQU	PC0										= (0x02<<4)|0x00
	.EQU	PC1										= (0x02<<4)|0x01
	.EQU	PC2										= (0x02<<4)|0x02
	.EQU	PC3										= (0x02<<4)|0x03
	.EQU	PC4										= (0x02<<4)|0x04
	.EQU	PC5										= (0x02<<4)|0x05
	.EQU	PC6										= (0x02<<4)|0x06
	.EQU	PC7										= (0x02<<4)|0x07
	.EQU	PD0										= (0x03<<4)|0x00
	.EQU	PD1										= (0x03<<4)|0x01
	.EQU	PD2										= (0x03<<4)|0x02
	.EQU	PD3										= (0x03<<4)|0x03
	.EQU	PD4										= (0x03<<4)|0x04
	.EQU	PD5										= (0x03<<4)|0x05
	.EQU	PD6										= (0x03<<4)|0x06
	.EQU	PD7										= (0x03<<4)|0x07
	.EQU	PE0										= (0x04<<4)|0x00
	.EQU	PE1										= (0x04<<4)|0x01
	.EQU	PE2										= (0x04<<4)|0x02
	.EQU	PE3										= (0x04<<4)|0x03
	.EQU	PE4										= (0x04<<4)|0x04
	.EQU	PE5										= (0x04<<4)|0x05
	.EQU	PE6										= (0x04<<4)|0x06
	.EQU	PE7										= (0x04<<4)|0x07
	.EQU	PF0										= (0x05<<4)|0x00
	.EQU	PF1										= (0x05<<4)|0x01
	.EQU	PF2										= (0x05<<4)|0x02
	.EQU	PF3										= (0x05<<4)|0x03
	.EQU	PF4										= (0x05<<4)|0x04
	.EQU	PF5										= (0x05<<4)|0x05
	.EQU	PF6										= (0x05<<4)|0x06
	.EQU	PF7										= (0x05<<4)|0x07
	.EQU	PG0										= (0x06<<4)|0x00
	.EQU	PG1										= (0x06<<4)|0x01
	.EQU	PG2										= (0x06<<4)|0x02
	.EQU	PG3										= (0x06<<4)|0x03
	.EQU	PG4										= (0x06<<4)|0x04
	.EQU	PG5										= (0x06<<4)|0x05
	.EQU	PG6										= (0x06<<4)|0x06
	.EQU	PG7										= (0x06<<4)|0x07

	.EQU	ISC_LOW_LEVEL							= 0x00
	.EQU	ISC_ANY_CHANGE							= 0x01
	.EQU	ISC_FALLING_EDGE						= 0x02
	.EQU	ISC_RISING_EDGE							= 0x03

	.SET	INT0_PORT								= 0xff
	.SET	INT1_PORT								= 0xff
	.SET	INT2_PORT								= 0xff
	.SET	INT3_PORT								= 0xff
	.SET	INT4_PORT								= 0xff
	.SET	INT5_PORT								= 0xff
	.SET	INT6_PORT								= 0xff
	.SET	INT7_PORT								= 0xff


	.DEF	_CORE_REG_L								= r8
	.DEF	_CORE_REG_H								= r9
	.DEF	C0x00									= r10	;Константа 0x00
	.DEF	C0xff									= r11	;Константа 0xff
	.DEF	ID_L									= r12
	.DEF	ID_H									= r13
	.DEF	FLAGS									= r14	;Регистр флагов
	.DEF	LOOP_CNTR								= r15	;Регистр цикла
	.DEF	TEMP_L									= r16
	.DEF	TEMP_H									= r17
	.DEF	TEMP_EL									= r18
	.DEF	TEMP_EH									= r19
	.DEF	ACCUM_L									= r20
	.DEF	ACCUM_H									= r21
	.DEF	ACCUM_EL								= r22
	.DEF	ACCUM_EH								= r23
	.DEF	WL										= r24
	.DEF	W										= r25
	.DEF	WH										= r25
	.DEF	XL										= r26
	.DEF	X										= r27
	.DEF	XH										= r27
	.DEF	YL										= r28
	.DEF	Y										= r29
	.DEF	YH										= r29
	.DEF	ZL										= r30
	.DEF	Z										= r31
	.DEF	ZH										= r31

	;---Часто-используемые-константы-для-частот-программного-таймера---
;TODO делить на 2?
	.EQU	TIMER_FREQ_20KHz						= 1
	.EQU	TIMER_FREQ_10KHz						= 2
	.EQU	TIMER_FREQ_5KHz							= 4
	.EQU	TIMER_FREQ_4KHz							= 5
	.EQU	TIMER_FREQ_2KHz							= 10
	.EQU	TIMER_FREQ_1KHz							= 20
	.EQU	TIMER_FREQ_500Hz						= 40
	.EQU	TIMER_FREQ_250Hz						= 80
	.EQU	TIMER_FREQ_100Hz						= 128+5
	.EQU	TIMER_FREQ_50Hz							= 128+10
	.EQU	TIMER_FREQ_10Hz							= 128+50

	;---Часто-используемые-константы-для-частот-таймера-С---
	.EQU	TIMER_C_BAUDRATE_115200					= CORE_FREQ*125000/115200-1
	.EQU	TIMER_C_BAUDRATE_76800					= CORE_FREQ*125000/76800-1
	.EQU	TIMER_C_BAUDRATE_74880 					= CORE_FREQ*125000/74880-1
	.EQU	TIMER_C_BAUDRATE_57600					= CORE_FREQ*125000/57600-1
	.EQU	TIMER_C_BAUDRATE_38400					= CORE_FREQ*125000/38400-1
	.EQU	TIMER_C_BAUDRATE_28800					= CORE_FREQ*125000/28800-1
	.EQU	TIMER_C_BAUDRATE_19200					= CORE_FREQ*125000/19200-1
	.EQU	TIMER_C_BAUDRATE_14400					= CORE_FREQ*125000/14400-1
	.EQU	TIMER_C_BAUDRATE_9600					= CORE_FREQ*125000/9600-1
	;---
	.EQU	TIMER_C_FREQ_40KHz						= (CORE_FREQ*125000/(40000*2))-1
	.EQU	TIMER_C_FREQ_38KHz						= (CORE_FREQ*125000/(38000*2))-1
	.EQU	TIMER_C_FREQ_36KHz						= (CORE_FREQ*125000/(36000*2))-1
	.EQU	TIMER_C_FREQ_20KHz						= (CORE_FREQ*125000/(20000*2))-1
	.EQU	TIMER_C_FREQ_10KHz						= (CORE_FREQ*125000/(10000*2))-1
	.EQU	TIMER_C_FREQ_5KHz						= (CORE_FREQ*125000/(5000*2))-1
.IF CORE_FREQ < 20
	.EQU	TIMER_C_FREQ_4800Hz						= (CORE_FREQ*125000/(4800*2))-1
.ENDIF
	;---

.MACRO PUSH_X
	PUSH XH
	PUSH XL
.ENDMACRO
.MACRO POP_X
	POP XL
	POP XH
.ENDMACRO
.MACRO PUSH_Y
	PUSH YH
	PUSH YL
.ENDMACRO
.MACRO POP_Y
	POP YL
	POP YH
.ENDMACRO
.MACRO PUSH_Z
	PUSH ZH
	PUSH ZL
.ENDMACRO
.MACRO POP_Z
	POP ZL
	POP ZH
.ENDMACRO

.ifdef CORE_FREQ
.MACRO C5_WAIT_500NS
	PUSH TEMP
	LDI TEMP,low((@0-36)/(16/CORE_FREQ))
	.if ((@0-36)/(16/CORE_FREQ)) > 0
		MCALL _C5_WAIT_500NS
	.endif
	POP TEMP
.ENDMACRO
.endif

.MACRO LDI_X
	LDI XH,high(@0)
	LDI XL,low(@0)
.ENDMACRO
.MACRO LDI_Y
	LDI YH,high(@0)
	LDI YL,low(@0)
.ENDMACRO
.MACRO LDI_Z
	LDI ZH,high(@0)
	LDI ZL,low(@0)
.ENDMACRO
.MACRO LDI_T16
	LDI TEMP_H,high(@0)
	LDI TEMP_L,low(@0)
.ENDMACRO
.MACRO LDI_T32
	LDI TEMP_EH,byte4(@0)
	LDI TEMP_EL,byte3(@0)
	LDI TEMP_H,byte2(@0)
	LDI TEMP_L,byte1(@0)
.ENDMACRO
.MACRO PUSH_T16
	PUSH TEMP_H
	PUSH TEMP_L
.ENDMACRO
.MACRO POP_T16
	POP TEMP_L
	POP TEMP_H
.ENDMACRO
.MACRO PUSH_T32
	PUSH TEMP_EH
	PUSH TEMP_EL
	PUSH TEMP_H
	PUSH TEMP_L
.ENDMACRO
.MACRO POP_T32
	POP TEMP_L
	POP TEMP_H
	POP TEMP_EL
	POP TEMP_EH
.ENDMACRO
.MACRO PUSH_FT
	PUSH TEMP_EH
	PUSH TEMP_EL
	PUSH TEMP_H
	PUSH TEMP_L
	PUSH TEMP
.ENDMACRO
.MACRO POP_FT
	POP TEMP
	POP TEMP_L
	POP TEMP_H
	POP TEMP_EL
	POP TEMP_EH
.ENDMACRO
