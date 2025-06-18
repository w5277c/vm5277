;-----------------------------------------------------------------------------------------------------------------------
;Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
;-----------------------------------------------------------------------------------------------------------------------
;17.06.2025	konstantin@5277.ru		Взят из проекта core5277
;-----------------------------------------------------------------------------------------------------------------------
.IFNDEF OS_OUT_CHAR
;-----------------------------------------------------------
OS_OUT_CHAR:
;-----------------------------------------------------------
;Вывод символа
;IN: ACCUM_L-байт
;-----------------------------------------------------------
	PUSH_Z
	PUSH ACCUM_L
	PUSH LOOP_CNTR
	PUSH TEMP_L
	PUSH FLAGS

	LDI_Z PORTS_TABLE*2+(STDOUT_PORT>>4)					;PINx
	LPM ZL,Z
	ADIW ZL,0x02											;PORTx
	CLR ZH

	LD TEMP_L,Z
	ORI TEMP_L,(EXP2 (STDOUT_PORT & 0x0f))
	ST Z,TEMP_L
	LDI TEMP_L,0x14
	DEC TEMP_L
	BRNE PC-0x01

	LDS FLAGS,SREG
	CLI
	LD TEMP_L,Z
	ANDI TEMP_L,low(~(EXP2 (STDOUT_PORT & 0x0f)))
	ST Z,TEMP_L
	LDI TEMP_L,0x14;0x08
	DEC TEMP_L
	BRNE PC-0x01

	;DATA BITS
	LDI LOOP_CNTR,0x08
_OS_OUT_CHAR__LOOP:

	LD TEMP_L,Z
	SBRC ACCUM_L,0x00
	ORI TEMP_L,(EXP2 (STDOUT_PORT & 0x0f))
	SBRS ACCUM_L,0x00
	ANDI TEMP_L,low(~(EXP2 (STDOUT_PORT & 0x0f)))
	LSR ACCUM_L
	ST Z,TEMP_L

	NOP
	LDI TEMP_L,0x13
	DEC TEMP_L
	BRNE PC-0x01
	DEC LOOP_CNTR
	BRNE _OS_OUT_CHAR__LOOP

	;STOP
	LD TEMP_L,Z
	ORI TEMP_L,(EXP2 (STDOUT_PORT & 0x0f))
	ST Z,TEMP_L

	STS SREG,FLAGS
	POP FLAGS
	POP TEMP_L
	POP LOOP_CNTR
	POP ACCUM_L
	POP_Z
	RET
.ENDIF

